package net.takasha.rpg.mythic;

import net.minecraft.world.phys.Vec3;
import net.takasha.rpg.modelengine.ActiveModelInstance;
import net.takasha.rpg.modelengine.TakashaModelEngine;

import java.util.Map;

/**
 * Homing missile mechanic that steers towards target entity with inertia curve.
 */
public class MissileMechanic implements MythicMechanic {

    private final String inertiaFormula;
    private final String velocityFormula;
    private final String hrFormula;
    private final String vrFormula;
    private final String mdFormula;
    private final String onTickSkill;
    private final String onHitSkill;
    private final String ssoFormula;
    private final String syoFormula;
    private final String mobType;
    private final boolean stopOnBlock;
    private final boolean stopOnEntity;

    public MissileMechanic(Map<String, String> params) {
        this.inertiaFormula = params.getOrDefault("in", params.getOrDefault("i", "0.5"));
        this.velocityFormula = params.getOrDefault("v", "3.0");
        this.hrFormula = params.getOrDefault("hr", "0.3");
        this.vrFormula = params.getOrDefault("vr", "0.3");
        this.mdFormula = params.getOrDefault("md", "80");
        this.onTickSkill = params.getOrDefault("ot", params.getOrDefault("ontick", ""));
        this.onHitSkill = params.getOrDefault("onhit", params.getOrDefault("oh", params.getOrDefault("ohb", "")));
        this.ssoFormula = params.getOrDefault("sso", "0");
        this.syoFormula = params.getOrDefault("syo", "0");
        this.mobType = params.getOrDefault("mob", "");
        this.stopOnBlock = !"false".equalsIgnoreCase(params.getOrDefault("sb", "true"));
        this.stopOnEntity = !"false".equalsIgnoreCase(params.getOrDefault("se", "true"));
    }

    @Override
    public void execute(SkillExecutionContext context) {
        Vec3 start = context.getOrigin();
        double sso = context.parseDouble(ssoFormula, 0.0);
        double syo = context.parseDouble(syoFormula, 0.0);

        if (context.getCaster() != null) {
            Vec3 look = context.getCaster().getLookAngle();
            Vec3 side = new Vec3(-look.z, 0, look.x).normalize();
            start = start.add(side.scale(sso)).add(0, syo, 0);
        }

        double inertia = context.parseDouble(inertiaFormula, 0.5);
        double velocity = context.parseDouble(velocityFormula, 3.0);
        double hr = context.parseDouble(hrFormula, 0.3);
        double vr = context.parseDouble(vrFormula, 0.3);
        int md = context.parseInt(mdFormula, 80);

        Vec3 initDir = context.getCaster() != null ? context.getCaster().getLookAngle() : new Vec3(0, 0, 1);

        ActiveModelInstance modelInstance = null;
        if (!mobType.isBlank()) {
            MythicMobDef mobDef = MythicMobManager.getInstance().get(mobType);
            String modelId = (mobDef != null && mobDef.getModelId() != null) ? mobDef.getModelId() : mobType.toLowerCase();
            modelInstance = TakashaModelEngine.spawnStaticModel(context.getLevel(), modelId, start);
            if (modelInstance != null) {
                String state = mobDef != null ? mobDef.getDefaultState() : "animation";
                float speed = mobDef != null ? mobDef.getAnimationSpeed() : 1.0f;
                modelInstance.playAnimation(state, speed, true);
                modelInstance.setLifetime(md + 10);
            }
        }

        if (context.getCaster() != null) {
            start = new Vec3(start.x, Math.max(context.getCaster().getY() + 0.2, start.y), start.z);
        }

        ActiveProjectile missile = new ActiveProjectile(
                context.getLevel(),
                context.getCaster(),
                (context.getTarget() != null && context.getTarget() != context.getCaster()) ? context.getTarget() : null,
                start,
                initDir,
                velocity,
                inertia,
                hr, vr, md,
                onTickSkill, onHitSkill,
                modelInstance,
                stopOnBlock,
                stopOnEntity
        );
        missile.setVariables(context.getVariables());
        if (context.getTargetLocation() != null) {
            missile.setTargetLocation(context.getTargetLocation());
        }

        MythicSkillExecutor.getInstance().addProjectile(missile);
    }
}
