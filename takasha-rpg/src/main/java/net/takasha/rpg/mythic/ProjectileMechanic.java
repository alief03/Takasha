package net.takasha.rpg.mythic;

import net.minecraft.world.phys.Vec3;
import net.takasha.rpg.modelengine.ActiveModelInstance;
import net.takasha.rpg.modelengine.TakashaModelEngine;

import java.util.Map;

/**
 * Straight line or targeted ballistic projectile.
 */
public class ProjectileMechanic implements MythicMechanic {

    private final String velocityFormula;
    private final String hrFormula;
    private final String vrFormula;
    private final String mdFormula;
    private final String onHitSkill;
    private final String onTickSkill;
    private final String onStartSkill;
    private final String syoFormula;
    private final String sfoFormula;
    private final String ssoFormula;
    private final String mobType;
    private final boolean stopOnBlock;
    private final boolean stopOnEntity;

    public ProjectileMechanic(Map<String, String> params) {
        this.velocityFormula = params.getOrDefault("v", params.getOrDefault("velocity", "20"));
        this.hrFormula = params.getOrDefault("hr", "1.0");
        this.vrFormula = params.getOrDefault("vr", "1.0");
        this.mdFormula = params.getOrDefault("md", "40");
        this.onHitSkill = params.getOrDefault("onhit", params.getOrDefault("oh", params.getOrDefault("ohb", "")));
        this.onTickSkill = params.getOrDefault("ot", params.getOrDefault("ontick", ""));
        this.onStartSkill = params.getOrDefault("os", params.getOrDefault("onstart", ""));
        this.syoFormula = params.getOrDefault("syo", "0");
        this.sfoFormula = params.getOrDefault("sfo", "0");
        this.ssoFormula = params.getOrDefault("sso", "0");
        this.mobType = params.getOrDefault("mob", "");
        this.stopOnBlock = !"false".equalsIgnoreCase(params.getOrDefault("sb", "true"));
        this.stopOnEntity = !"false".equalsIgnoreCase(params.getOrDefault("se", "true"));
    }

    @Override
    public void execute(SkillExecutionContext context) {
        Vec3 startPos = context.getOrigin();
        double syo = context.parseDouble(syoFormula, 0.0);
        double sfo = context.parseDouble(sfoFormula, 0.0);
        double sso = context.parseDouble(ssoFormula, 0.0);

        if (context.getCaster() != null && (syo != 0 || sfo != 0 || sso != 0)) {
            Vec3 look = context.getCaster().getLookAngle();
            Vec3 side = new Vec3(-look.z, 0, look.x).normalize();
            startPos = startPos.add(look.scale(sfo)).add(side.scale(sso)).add(0, syo, 0);
        }

        Vec3 targetLoc = context.getTargetLocation();
        Vec3 direction = (targetLoc != null && !targetLoc.equals(startPos)) ?
                targetLoc.subtract(startPos).normalize() :
                (context.getCaster() != null ? context.getCaster().getLookAngle() : new Vec3(0, 0, 1));

        double velocity = context.parseDouble(velocityFormula, 20.0);
        double hr = context.parseDouble(hrFormula, 1.0);
        double vr = context.parseDouble(vrFormula, 1.0);
        int md = context.parseInt(mdFormula, 40);

        double vx = direction.x;
        double vy = direction.y;
        double vz = direction.z;
        double horizDist = Math.sqrt(vx * vx + vz * vz);
        float initialYaw = (float) Math.toDegrees(Math.atan2(-vx, vz));
        float initialPitch = (float) Math.toDegrees(Math.atan2(-vy, horizDist));

        ActiveModelInstance modelInstance = null;
        net.takasha.rpg.modelengine.TransientTextDisplay textDisplay = null;
        if (!mobType.isBlank()) {
            MythicMobDef mobDef = MythicMobManager.getInstance().get(mobType);
            if (mobDef != null && "text_display".equalsIgnoreCase(mobDef.getMobType())) {
                java.util.List<String> frames = mobDef.getTextFrames();
                textDisplay = TakashaModelEngine.spawnTextDisplay(context.getLevel(), startPos, frames);
            } else {
                String modelId = (mobDef != null && mobDef.getModelId() != null) ? mobDef.getModelId() : mobType.toLowerCase();
                modelInstance = TakashaModelEngine.spawnStaticModel(context.getLevel(), modelId, startPos, initialYaw, initialPitch);
                if (modelInstance != null) {
                    if (mobDef != null && mobDef.isLockPitch()) {
                        modelInstance.setLockPitch(true);
                    }
                    String state = mobDef != null ? mobDef.getDefaultState() : "animation";
                    float speed = mobDef != null ? mobDef.getAnimationSpeed() : 1.0f;
                    modelInstance.playAnimation(state, speed, true);
                    modelInstance.setLifetime(md + 10);
                }
            }
        }

        if (context.getCaster() != null) {
            startPos = new Vec3(startPos.x, Math.max(context.getCaster().getY() + 0.2, startPos.y), startPos.z);
        }

        ActiveProjectile projectile = new ActiveProjectile(
                context.getLevel(),
                context.getCaster(),
                (context.getTarget() != null && context.getTarget() != context.getCaster()) ? context.getTarget() : null,
                startPos,
                direction,
                velocity,
                0.0, // no homing inertia for standard projectile
                hr, vr, md,
                onTickSkill, onHitSkill,
                modelInstance,
                textDisplay,
                stopOnBlock,
                stopOnEntity
        );
        projectile.setVariables(context.getVariables());

        if (onStartSkill != null && !onStartSkill.isBlank()) {
            SkillExecutionContext startCtx = context.copy();
            startCtx.setActiveProjectile(projectile);
            MythicSkillExecutor.getInstance().executeSkill(onStartSkill, startCtx);
        }

        MythicSkillExecutor.getInstance().addProjectile(projectile);
    }
}
