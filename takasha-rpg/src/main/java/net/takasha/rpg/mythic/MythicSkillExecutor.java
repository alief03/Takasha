package net.takasha.rpg.mythic;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global executor and tick manager for MythicMobs skills, ballistics, and scheduled delayed mechanics.
 */
public class MythicSkillExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger("takasha-mythic-executor");
    private static final MythicSkillExecutor INSTANCE = new MythicSkillExecutor();

    public static MythicSkillExecutor getInstance() {
        return INSTANCE;
    }

    private final Map<String, MythicSkillDef> registeredSkills = new ConcurrentHashMap<>();
    private final List<ActiveProjectile> activeProjectiles = new ArrayList<>();
    private final List<ActiveOrbital> activeOrbitals = new ArrayList<>();
    private final List<ActiveTotem> activeTotems = new ArrayList<>();
    private final List<DelayedMechanicTask> delayedTasks = new ArrayList<>();

    public record DelayedMechanicTask(int delayTicksRemaining, MythicMechanic mechanic, SkillExecutionContext context) {
        public boolean tick() {
            return delayTicksRemaining <= 1;
        }

        public DelayedMechanicTask decrement() {
            return new DelayedMechanicTask(delayTicksRemaining - 1, mechanic, context);
        }
    }

    private MythicSkillExecutor() {}

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            INSTANCE.tick();
        });
        LOGGER.info("MythicSkillExecutor initialized.");
    }

    public void registerSkill(MythicSkillDef skill) {
        if (skill != null) {
            registeredSkills.put(skill.getId().toUpperCase(), skill);
        }
    }

    public void registerSkills(Map<String, MythicSkillDef> map) {
        if (map != null) {
            for (Map.Entry<String, MythicSkillDef> entry : map.entrySet()) {
                registeredSkills.put(entry.getKey().toUpperCase(), entry.getValue());
            }
        }
    }

    public MythicSkillDef getSkill(String id) {
        if (id == null) return null;
        return registeredSkills.get(id.toUpperCase());
    }

    public int getRegisteredSkillsCount() {
        return registeredSkills.size();
    }

    public void addProjectile(ActiveProjectile projectile) {
        synchronized (activeProjectiles) {
            activeProjectiles.add(projectile);
        }
    }

    public void addOrbital(ActiveOrbital orbital) {
        synchronized (activeOrbitals) {
            activeOrbitals.add(orbital);
        }
    }

    public void addTotem(ActiveTotem totem) {
        synchronized (activeTotems) {
            activeTotems.add(totem);
        }
    }

    public ActiveOrbital getOrbital(LivingEntity owner, String auraName) {
        synchronized (activeOrbitals) {
            for (ActiveOrbital o : activeOrbitals) {
                if (o.getOwner() == owner && (auraName == null || auraName.equalsIgnoreCase(o.getAuraName()))) {
                    return o;
                }
            }
        }
        return null;
    }

    public void scheduleMechanic(MythicMechanic mechanic, SkillExecutionContext context, int delayTicks) {
        synchronized (delayedTasks) {
            delayedTasks.add(new DelayedMechanicTask(delayTicks, mechanic, context));
        }
    }

    public boolean executeSkill(String skillName, SkillExecutionContext context) {
        if (skillName == null || skillName.isBlank()) return false;
        MythicSkillDef def = getSkill(skillName);
        if (def == null) {
            LOGGER.debug("Mythic skill not found: {}", skillName);
            return false;
        }

        // Evaluate skill-level conditions: in MythicMobs contract, skill conditions evaluate on CASTER
        SkillExecutionContext casterCtx = context.copy();
        casterCtx.setTarget(context.getCaster());
        for (SkillCondition condition : def.getConditions()) {
            SkillCondition.ConditionResult result = condition.evaluate(casterCtx);
            if (!result.passes()) {
                if (result.castInsteadSkill() != null) {
                    return executeSkill(result.castInsteadSkill(), context);
                }
                return false;
            }
        }

        // Execute mechanics
        for (MythicSkillDef.MechanicEntry entry : def.getMechanics()) {
            List<SkillTargeter.TargetResult> targets = entry.targeter().resolve(context);
            if (targets.isEmpty()) {
                continue;
            }

            for (SkillTargeter.TargetResult target : targets) {
                SkillExecutionContext targetCtx = context.copy();
                targetCtx.setTarget(target.entity());
                if (target.position() != null) {
                    targetCtx.setTargetLocation(target.position());
                }
                if (target.hasRotation()) {
                    targetCtx.setTargetRotation(target.yaw(), target.pitch());
                }

                // Check target conditions
                boolean targetCondsPassed = true;
                for (SkillCondition tc : def.getTargetConditions()) {
                    SkillCondition.ConditionResult tcRes = tc.evaluate(targetCtx);
                    if (!tcRes.passes()) {
                        if (tcRes.castInsteadSkill() != null) {
                            executeSkill(tcRes.castInsteadSkill(), targetCtx);
                            return true;
                        }
                        targetCondsPassed = false;
                        break;
                    }
                }
                if (!targetCondsPassed) continue;

                // Check inline condition
                if (entry.inlineCondition() != null) {
                    SkillCondition.ConditionResult inlineRes = entry.inlineCondition().evaluate(targetCtx);
                    if (!inlineRes.passes()) {
                        if (inlineRes.castInsteadSkill() != null) {
                            executeSkill(inlineRes.castInsteadSkill(), targetCtx);
                        }
                        continue;
                    }
                }

                // Handle delay & repeat
                int reps = Math.max(1, entry.repeatCount());
                int interval = Math.max(1, entry.repeatInterval());
                for (int r = 0; r < reps; r++) {
                    int taskDelay = entry.delayTicks() + (r * (reps > 1 ? interval : 0));
                    if (taskDelay > 0) {
                        scheduleMechanic(entry.mechanic(), targetCtx, taskDelay);
                    } else {
                        entry.mechanic().execute(targetCtx);
                    }
                }
            }
        }

        return true;
    }

    public void tick() {
        // 1. Tick Auras
        SkillAuraManager.getInstance().tick();

        // 2. Tick Delayed Tasks (Thread-safe Snapshot Queue)
        List<DelayedMechanicTask> tasksToRun;
        synchronized (delayedTasks) {
            tasksToRun = new ArrayList<>(delayedTasks);
            delayedTasks.clear();
        }

        List<DelayedMechanicTask> pendingTasks = new ArrayList<>();
        for (DelayedMechanicTask task : tasksToRun) {
            if (task.tick()) {
                try {
                    task.mechanic().execute(task.context());
                } catch (Exception e) {
                    LOGGER.error("Error executing delayed mechanic", e);
                }
            } else {
                pendingTasks.add(task.decrement());
            }
        }

        synchronized (delayedTasks) {
            delayedTasks.addAll(pendingTasks);
        }

        // 3. Tick Projectiles (Snapshot Safe Removal)
        List<ActiveProjectile> projsToTick;
        synchronized (activeProjectiles) {
            projsToTick = new ArrayList<>(activeProjectiles);
        }
        List<ActiveProjectile> expiredProjs = new ArrayList<>();
        for (ActiveProjectile p : projsToTick) {
            try {
                if (!p.tick()) {
                    expiredProjs.add(p);
                }
            } catch (Exception e) {
                LOGGER.error("Error ticking projectile", e);
                expiredProjs.add(p);
            }
        }
        synchronized (activeProjectiles) {
            activeProjectiles.removeAll(expiredProjs);
        }

        // 4. Tick Orbitals (Snapshot Safe Removal)
        List<ActiveOrbital> orbsToTick;
        synchronized (activeOrbitals) {
            orbsToTick = new ArrayList<>(activeOrbitals);
        }
        List<ActiveOrbital> expiredOrbs = new ArrayList<>();
        for (ActiveOrbital o : orbsToTick) {
            try {
                if (!o.tick()) {
                    expiredOrbs.add(o);
                }
            } catch (Exception e) {
                LOGGER.error("Error ticking orbital", e);
                expiredOrbs.add(o);
            }
        }
        synchronized (activeOrbitals) {
            activeOrbitals.removeAll(expiredOrbs);
        }

        // 5. Tick Totems (Snapshot Safe Removal)
        List<ActiveTotem> totemsToTick;
        synchronized (activeTotems) {
            totemsToTick = new ArrayList<>(activeTotems);
        }
        List<ActiveTotem> expiredTotems = new ArrayList<>();
        for (ActiveTotem t : totemsToTick) {
            try {
                if (!t.tick()) {
                    expiredTotems.add(t);
                }
            } catch (Exception e) {
                LOGGER.error("Error ticking totem", e);
                expiredTotems.add(t);
            }
        }
        synchronized (activeTotems) {
            activeTotems.removeAll(expiredTotems);
        }
    }

    public void clearAll() {
        synchronized (activeProjectiles) {
            for (ActiveProjectile p : activeProjectiles) p.remove();
            activeProjectiles.clear();
        }
        synchronized (activeOrbitals) {
            for (ActiveOrbital o : activeOrbitals) o.remove();
            activeOrbitals.clear();
        }
        synchronized (activeTotems) {
            for (ActiveTotem t : activeTotems) t.remove();
            activeTotems.clear();
        }
        synchronized (delayedTasks) {
            delayedTasks.clear();
        }
        SkillAuraManager.getInstance().clear();
        registeredSkills.clear();
    }
}
