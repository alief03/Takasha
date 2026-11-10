package net.takasha.rpg.mythic;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.takasha.rpg.modelengine.ActiveModelInstance;
import net.takasha.rpg.modelengine.TakashaModelEngine;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Spawns VFX or custom model entities at target location.
 */
public class SummonMechanic implements MythicMechanic {

    private final String mobType;
    private final String amountFormula;
    private final String radiusFormula;
    private final String yRadiusFormula;
    private final boolean onSurface;

    public SummonMechanic(Map<String, String> params) {
        this.mobType = params.getOrDefault("type", params.getOrDefault("mob", ""));
        this.amountFormula = params.getOrDefault("amount", "1");
        this.radiusFormula = params.getOrDefault("radius", "0");
        this.yRadiusFormula = params.getOrDefault("yradius", "0");
        this.onSurface = Boolean.parseBoolean(params.getOrDefault("onsurface", "false"));
    }

    @Override
    public void execute(SkillExecutionContext context) {
        if (mobType.isBlank()) return;

        Vec3 basePos = context.getTargetLocation();
        if (basePos == null) basePos = context.getOrigin();

        MythicMobDef mobDef = MythicMobManager.getInstance().get(mobType);
        if (mobDef != null && "text_display".equalsIgnoreCase(mobDef.getMobType())) {
            int amount = Math.max(1, context.parseInt(amountFormula, 1));
            double radius = context.parseDouble(radiusFormula, 0.0);
            double yRadius = context.parseDouble(yRadiusFormula, 0.0);
            for (int i = 0; i < amount; i++) {
                Vec3 spawnPos = basePos;
                if (radius > 0 || yRadius > 0) {
                    double rx = (ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0) * radius;
                    double rz = (ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0) * radius;
                    double ry = (ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0) * yRadius;
                    spawnPos = spawnPos.add(rx, ry, rz);
                }
                TakashaModelEngine.spawnTextDisplay(context.getLevel(), spawnPos, mobDef.getTextFrames());
            }
            return;
        }
        String modelId = (mobDef != null && mobDef.getModelId() != null) ? mobDef.getModelId() : mobType.toLowerCase();

        int amount = Math.max(1, context.parseInt(amountFormula, 1));
        double radius = context.parseDouble(radiusFormula, 0.0);
        double yRadius = context.parseDouble(yRadiusFormula, 0.0);

        float yaw = context.getTargetYaw();
        float pitch = context.getTargetPitch();
        boolean lockPitch = mobDef != null && mobDef.isLockPitch();
        if (lockPitch) {
            pitch = 0.0f;
        }

        for (int i = 0; i < amount; i++) {
            Vec3 spawnPos = basePos;
            if (radius > 0 || yRadius > 0) {
                double rx = (ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0) * radius;
                double rz = (ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0) * radius;
                double ry = (ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0) * yRadius;
                spawnPos = spawnPos.add(rx, ry, rz);
            }

            if (onSurface && context.getLevel() != null) {
                int blockX = (int) Math.floor(spawnPos.x);
                int blockZ = (int) Math.floor(spawnPos.z);
                int surfaceY = context.getLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
                if (surfaceY > context.getLevel().getMinY()) {
                    spawnPos = new Vec3(spawnPos.x, surfaceY, spawnPos.z);
                }
            }

            ActiveModelInstance instance = TakashaModelEngine.spawnStaticModel(context.getLevel(), modelId, spawnPos, yaw, pitch);
            if (instance != null) {
                instance.setLockPitch(lockPitch);
                String state = mobDef != null ? mobDef.getDefaultState() : "animation";
                float speed = mobDef != null ? mobDef.getAnimationSpeed() : 1.0f;
                int lifetime = mobDef != null ? mobDef.getLifetimeTicks() : 40;

                instance.playAnimation(state, speed, false);
                instance.setLifetime(lifetime);

                // Auto-configure flipbook sequences for multi-frame Blockbench models
                String lowerMob = mobType.toLowerCase(java.util.Locale.ROOT);
                String lowerModel = modelId.toLowerCase(java.util.Locale.ROOT);
                if (lowerMob.contains("volcanic_spear") || lowerModel.contains("volcanic_spear")) {
                    instance.setupSequence("volcanic_spear", 1, 8, 1, 0, 4);
                } else if (lowerMob.contains("explosion") || lowerModel.contains("vfx_explosion")) {
                    instance.setupSequence("vfx_explosion", 1, 12, 1, 0, 2);
                } else if (lowerMob.contains("rupture") || lowerModel.contains("vfx_earthquake_rupture")) {
                    instance.setupSequence("vfx_earthquake_rupture", 1, 5, 2, 44, 4);
                }
            }
        }
    }
}
