package net.takasha.rpg.mythic;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.takasha.rpg.modelengine.ActiveModelInstance;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks an active orbital model revolving around an entity with onTick skill and stack sync support.
 */
public class ActiveOrbital {

    private final LivingEntity owner;
    private final LivingEntity originCaster;
    private final ActiveModelInstance modelInstance;
    private final String auraName;
    private final double radius;
    private final double yOffset;
    private final double angularVelocity;
    private final String onTickSkill;
    private final int intervalTicks;
    private final Map<String, Object> variables = new HashMap<>();

    private double angle = 0.0;
    private int ticksRemaining;
    private int lastKnownStacks = 1;

    public ActiveOrbital(LivingEntity owner, LivingEntity originCaster, ActiveModelInstance modelInstance, String auraName,
                         double radius, double yOffset, double angularVelocity, int durationTicks,
                         String onTickSkill, int intervalTicks) {
        this.owner = owner;
        this.originCaster = originCaster;
        this.modelInstance = modelInstance;
        this.auraName = auraName;
        this.radius = radius;
        this.yOffset = yOffset;
        this.angularVelocity = angularVelocity;
        this.ticksRemaining = durationTicks;
        this.onTickSkill = onTickSkill != null ? onTickSkill.trim() : "";
        this.intervalTicks = Math.max(1, intervalTicks);
        if (auraName != null && !auraName.isEmpty()) {
            this.lastKnownStacks = Math.max(1, SkillAuraManager.getInstance().getStacks(owner, auraName));
        }
    }

    public ActiveOrbital(LivingEntity owner, ActiveModelInstance modelInstance, String auraName,
                         double radius, double yOffset, double angularVelocity, int durationTicks) {
        this(owner, owner, modelInstance, auraName, radius, yOffset, angularVelocity, durationTicks, "", 1);
    }

    public void setVariables(Map<String, Object> vars) {
        if (vars != null) {
            this.variables.putAll(vars);
        }
    }

    public void syncStacks(int stacks) {
        if (stacks <= 0) {
            remove();
            return;
        }
        this.lastKnownStacks = stacks;
        if ("HEAT".equalsIgnoreCase(auraName)) {
            if (stacks == 1) {
                modelInstance.playAnimation("stack_black1", 1.0, true);
                playStackSound(owner, 0.75f);
            } else if (stacks == 2) {
                modelInstance.playAnimation("stack_black2", 1.0, true);
                playStackSound(owner, 1.0f);
            } else if (stacks >= 3) {
                modelInstance.playAnimation("stack_black3", 1.0, true);
                playStackSound(owner, 1.25f);
            }
        }
    }

    public boolean tick() {
        if (owner == null || !owner.isAlive() || ticksRemaining <= 0 || modelInstance.isRemoved()) {
            modelInstance.remove();
            return false;
        }

        // Aura stacks lifecycle check (e.g. HEAT stacks consumed by Ignition)
        if (auraName != null && !auraName.isEmpty()) {
            int stacks = SkillAuraManager.getInstance().getStacks(owner, auraName);
            if (stacks <= 0) {
                remove();
                return false;
            }

            if (stacks != lastKnownStacks) {
                syncStacks(stacks);
            }
        }

        // Periodic onTick skill (e.g. FlameDebuff_BURN_DAMAGE on burning mob)
        if (!onTickSkill.isEmpty() && (ticksRemaining % intervalTicks == 0)) {
            if (owner.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                SkillExecutionContext tickCtx = new SkillExecutionContext(
                        serverLevel,
                        originCaster != null ? originCaster : owner,
                        owner,
                        owner.position().add(0, yOffset, 0)
                );
                tickCtx.getVariables().putAll(this.variables);
                MythicSkillExecutor.getInstance().executeSkill(onTickSkill, tickCtx);
            }
        }

        ticksRemaining--;

        if (radius <= 0.001) {
            modelInstance.setOffset(new Vec3(0, yOffset, 0));
        } else {
            angle += angularVelocity;
            Vec3 offset = new Vec3(Math.cos(angle) * radius, yOffset, Math.sin(angle) * radius);
            modelInstance.setOffset(offset);
        }

        return true;
    }

    private void playStackSound(LivingEntity entity, float pitch) {
        if (entity.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    net.minecraft.sounds.SoundEvents.TRIAL_SPAWNER_SPAWN_ITEM,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, pitch);
        }
    }

    public LivingEntity getOwner() {
        return owner;
    }

    public String getAuraName() {
        return auraName;
    }

    public void refreshDuration(int durationTicks) {
        this.ticksRemaining = Math.max(this.ticksRemaining, durationTicks);
    }

    public void remove() {
        modelInstance.remove();
        ticksRemaining = 0;
    }
}
