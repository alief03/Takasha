package net.takasha.rpg.mythic;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.takasha.rpg.modelengine.ActiveModelInstance;
import net.takasha.rpg.modelengine.TakashaModelEngine;

import java.util.Map;

/**
 * Creates an orbiting VFX model revolving around the target or caster with dual anchor support.
 */
public class OrbitalMechanic implements MythicMechanic {

    private final String auraName;
    private final String mobType;
    private final String rFormula;
    private final String hrFormula;
    private final String oyFormula;
    private final String avFormula;
    private final String durationFormula;
    private final String maxStacksFormula;
    private final String onTickSkill;
    private final String intervalFormula;

    public OrbitalMechanic(Map<String, String> params) {
        this.auraName = params.getOrDefault("auraname", params.getOrDefault("aura", "ORBITAL"));
        this.mobType = params.getOrDefault("mob", params.getOrDefault("type", ""));
        this.rFormula = params.getOrDefault("r", params.getOrDefault("radius", "-1"));
        this.hrFormula = params.getOrDefault("hr", "1.0");
        this.oyFormula = params.getOrDefault("oy", params.getOrDefault("yoffset", "2.0"));
        this.avFormula = params.getOrDefault("avy", params.getOrDefault("av", "0"));
        this.durationFormula = params.getOrDefault("duration", "200");
        this.maxStacksFormula = params.getOrDefault("maxstacks", "3");
        this.onTickSkill = params.getOrDefault("ot", params.getOrDefault("ontick", ""));
        this.intervalFormula = params.getOrDefault("i", params.getOrDefault("interval", "1"));
    }

    @Override
    public void execute(SkillExecutionContext context) {
        // Bind to target if explicitly targeted (e.g. FlameDebuff @TargetedTarget), else caster (@self)
        LivingEntity anchor = (context.getTarget() != null && context.getTarget() != context.getCaster())
                ? context.getTarget()
                : context.getCaster();
        if (anchor == null || !anchor.isAlive()) return;

        double parsedR = context.parseDouble(rFormula, -1.0);
        double hr = context.parseDouble(hrFormula, 1.0);
        double radius = parsedR >= 0 ? parsedR : hr;

        double oy = context.parseDouble(oyFormula, 2.0);
        double av = context.parseDouble(avFormula, radius > 0 ? 0.08 : 0.0);
        int duration = context.parseInt(durationFormula, 200);
        int maxStacks = context.parseInt(maxStacksFormula, 3);
        int interval = context.parseInt(intervalFormula, 1);

        // Add aura stack to anchor
        SkillAuraManager.getInstance().addAura(anchor, auraName, duration, maxStacks);

        // Spawn visual model
        if (!mobType.isBlank()) {
            MythicMobDef mobDef = MythicMobManager.getInstance().get(mobType);
            String modelId = (mobDef != null && mobDef.getModelId() != null) ? mobDef.getModelId() : mobType.toLowerCase();

            ActiveOrbital existingOrbital = MythicSkillExecutor.getInstance().getOrbital(anchor, auraName);
            if (existingOrbital != null) {
                existingOrbital.refreshDuration(duration);
                existingOrbital.syncStacks(SkillAuraManager.getInstance().getStacks(anchor, auraName));
                return;
            }

            ActiveModelInstance instance = TakashaModelEngine.spawnModel(context.getLevel(), modelId, anchor, new Vec3(0, oy, 0));
            if (instance != null) {
                if (mobDef != null && mobDef.isLockPitch()) {
                    instance.setLockPitch(true);
                }
                String state = mobDef != null ? mobDef.getDefaultState() : "stack_black1";
                instance.playAnimation(state, 1.0, true);

                ActiveOrbital orbital = new ActiveOrbital(anchor, context.getCaster(), instance, auraName, radius, oy, av, duration, onTickSkill, interval);
                orbital.setVariables(context.getVariables());
                orbital.syncStacks(SkillAuraManager.getInstance().getStacks(anchor, auraName));
                MythicSkillExecutor.getInstance().addOrbital(orbital);
            }
        }
    }
}
