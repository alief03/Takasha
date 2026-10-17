package net.sakura.weapons.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.sakura.weapons.client.config.TakashaMasterConfig;

import java.util.Comparator;
import java.util.List;

/**
 * Auto Bow combat assistant with ballistic auto-aim calculation.
 * 100% FREE PLAYER MOVEMENT: Never locks or modifies WASD, sprint, jump, or sneak keys.
 * Player can freely run and maneuver while the bow draws and fires at whitelisted targets.
 */
public class TakashaAutoBowHandler {

    private static volatile TakashaAutoBowHandler INSTANCE;

    private LivingEntity currentTarget = null;
    private int drawTicks = 0;
    private boolean isDrawing = false;
    private int shootCooldownTicks = 0;

    public static TakashaAutoBowHandler getInstance() {
        if (INSTANCE == null) {
            synchronized (TakashaAutoBowHandler.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TakashaAutoBowHandler();
                }
            }
        }
        return INSTANCE;
    }

    public void tick(Minecraft client) {
        TakashaMasterConfig config = TakashaMasterConfig.getInstance();
        if (!config.autoBowEnabled) {
            reset(client);
            return;
        }

        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            reset(client);
            return;
        }

        if (client.gui.screen() != null) {
            stopDrawing(client);
            return;
        }

        // Must be holding a bow
        if (!isHoldingBow(player)) {
            stopDrawing(client);
            currentTarget = null;
            return;
        }

        if (shootCooldownTicks > 0) {
            shootCooldownTicks--;
            return;
        }

        double maxRange = config.autoBowRange;
        AABB scanBox = player.getBoundingBox().inflate(maxRange);

        List<LivingEntity> targets = client.level.getEntitiesOfClass(
                LivingEntity.class,
                scanBox,
                entity -> entity != null && entity.isAlive() && entity != player && isValidTarget(entity, config)
        );

        if (targets.isEmpty()) {
            stopDrawing(client);
            currentTarget = null;
            return;
        }

        // Select closest valid target with line of sight
        currentTarget = targets.stream()
                .filter(e -> hasLineOfSight(player, e))
                .min(Comparator.comparingDouble(player::distanceTo))
                .orElse(null);

        if (currentTarget == null) {
            stopDrawing(client);
            return;
        }

        // Calculate ballistic aim
        Vec3 aimTarget = calculateBallisticAim(player, currentTarget);
        if (config.autoBowAimAssist) {
            applyAim(player, aimTarget);
        }

        // Auto Draw & Release
        if (!isDrawing) {
            isDrawing = true;
            drawTicks = 0;
            if (client.options != null) {
                client.options.keyUse.setDown(true);
            }
        } else {
            drawTicks++;
            if (drawTicks >= config.autoBowPullTicks) {
                // Fully charged shot: release arrow!
                if (client.options != null) {
                    client.options.keyUse.setDown(false);
                }
                isDrawing = false;
                drawTicks = 0;
                shootCooldownTicks = 8; // Small cooldown between shots
            }
        }
    }

    /**
     * Calculates ballistic arc compensation for arrow gravity (g = 0.05).
     */
    private Vec3 calculateBallisticAim(LocalPlayer player, LivingEntity target) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 targetPos = target.position().add(0, target.getEyeHeight() * 0.7, 0);

        double dx = targetPos.x - eyePos.x;
        double dy = targetPos.y - eyePos.y;
        double dz = targetPos.z - eyePos.z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // Approximate arrow flight time (velocity ~ 3.0 blocks/tick at full draw)
        double flightTicks = horizontalDist / 3.0;
        // Gravity compensation: drop = 0.5 * g * t^2
        double dropCompensation = 0.5 * 0.05 * (flightTicks * flightTicks);

        return targetPos.add(0, dropCompensation, 0);
    }

    private void applyAim(LocalPlayer player, Vec3 aimTarget) {
        Vec3 eyePos = player.getEyePosition();
        double dx = aimTarget.x - eyePos.x;
        double dy = aimTarget.y - eyePos.y;
        double dz = aimTarget.z - eyePos.z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float targetPitch = (float) (-(Mth.atan2(dy, horizontalDist) * (180.0 / Math.PI)));

        // Smoothly guide crosshair toward target
        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();

        float yawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        float stepYaw = Mth.clamp(yawDiff, -35.0f, 35.0f);
        float stepPitch = Mth.clamp(pitchDiff, -25.0f, 25.0f);

        player.setYRot(currentYaw + stepYaw);
        player.setXRot(Mth.clamp(currentPitch + stepPitch, -90.0f, 90.0f));
    }

    private boolean hasLineOfSight(LocalPlayer player, LivingEntity target) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 targetCenter = target.position().add(0, target.getEyeHeight() * 0.7, 0);
        HitResult hit = player.level().clip(new ClipContext(
                eyePos, targetCenter, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    private boolean isValidTarget(LivingEntity entity, TakashaMasterConfig config) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return config.bowTargetWhitelist.contains(id.toString()) || config.bowTargetWhitelist.contains(id.getPath());
    }

    private boolean isHoldingBow(LocalPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.getItem() instanceof BowItem || off.getItem() instanceof BowItem;
    }

    private void stopDrawing(Minecraft client) {
        if (isDrawing && client.options != null) {
            client.options.keyUse.setDown(false);
            isDrawing = false;
            drawTicks = 0;
        }
    }

    public void reset(Minecraft client) {
        stopDrawing(client);
        currentTarget = null;
        shootCooldownTicks = 0;
    }
}
