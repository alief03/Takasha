package net.sakura.weapons.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
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
 * High-performance Auto Kill engine with KAIZEN vs VANILLA specialization.
 * - KAIZEN: Anti-AFK micro oscillation, strict half-slab grinder LOS, weapon cooldown sync, anti-aura cone.
 * - VANILLA: Pure stationary attack, hyper-spam or turbo rates, custom reach up to 6.0m, up to 360° FOV.
 */
public class TakashaMobKillHandler {

    private static volatile TakashaMobKillHandler INSTANCE;

    private final TakashaMobKillAntiAfk antiAfk = new TakashaMobKillAntiAfk();
    private LivingEntity currentTarget = null;
    private long lastAttackTime = 0L;
    private long lastEquipAttemptTime = 0L;
    private long lastBreakWarningTime = 0L;
    private float lockedGrinderYaw = Float.NaN;
    private float lockedGrinderPitch = Float.NaN;

    public static TakashaMobKillHandler getInstance() {
        if (INSTANCE == null) {
            synchronized (TakashaMobKillHandler.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TakashaMobKillHandler();
                }
            }
        }
        return INSTANCE;
    }

    public TakashaMobKillAntiAfk getAntiAfk() {
        return antiAfk;
    }

    public void tick(Minecraft client) {
        TakashaMasterConfig config = TakashaMasterConfig.getInstance();
        if (!config.autoKillEnabled) {
            reset(client);
            return;
        }

        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            reset(client);
            return;
        }

        // ABSOLUTE MUTEX: Pause combat & release all keys if Capcay solver is active, eating food, or any GUI screen is open
        boolean capcayBusy = net.sakura.weapons.client.fishing.CapcayManager.getInstance().isBusy();
        boolean autoEating = net.sakura.weapons.client.food.TakashaAutoEatHandler.getInstance().isEating();
        boolean screenOpen = client.gui.screen() != null;

        if (capcayBusy || autoEating || screenOpen) {
            currentTarget = null;
            antiAfk.releaseMovementKeys(client);
            return;
        }

        // Anti-AFK behavior: Active only in KAIZEN mode if enabled
        if (config.mobKillMode == TakashaMasterConfig.MobKillMode.KAIZEN && config.antiAfkEnabled) {
            antiAfk.tick(client, player);
        } else {
            antiAfk.reset(client);
        }

        double reach = config.killReach;
        AABB scanBox = player.getBoundingBox().inflate(reach + 1.5D);

        List<LivingEntity> candidates = client.level.getEntitiesOfClass(
                LivingEntity.class,
                scanBox,
                entity -> entity != null && entity.isAlive() && entity != player && isValidTarget(entity, config)
        );

        if (candidates.isEmpty()) {
            currentTarget = null;
            // In Kaizen mode: if no candidate mob is currently in reach, restore camera to locked grinder angle if drifted
            if (config.mobKillMode == TakashaMasterConfig.MobKillMode.KAIZEN && config.autoCameraFix && !Float.isNaN(lockedGrinderYaw)) {
                restoreLockedGrinderAngle(player, config);
            }
            return;
        }

        // Keep current target if still valid and within reach
        if (currentTarget == null || !currentTarget.isAlive() || !isValidTarget(currentTarget, config) || player.distanceTo(currentTarget) > reach) {
            currentTarget = selectBestTarget(player, candidates, config);
        }

        if (currentTarget == null) {
            if (config.mobKillMode == TakashaMasterConfig.MobKillMode.KAIZEN && config.autoCameraFix && !Float.isNaN(lockedGrinderYaw)) {
                restoreLockedGrinderAngle(player, config);
            }
            return;
        }

        // Kaizen Camera Auto-Fix: automatically and smoothly steer crosshair towards target/grinder opening
        if (config.mobKillMode == TakashaMasterConfig.MobKillMode.KAIZEN && config.autoCameraFix) {
            applyKaizenCameraFix(player, currentTarget, config);
        }

        // Check weapon validity and break protection
        if (config.autoSwitchWeapon) {
            ensureWeaponEquipped(client, player, config);
        }

        ItemStack mainHand = player.getMainHandItem();
        if (config.weaponBreakProtection && mainHand.isDamageableItem()) {
            int remaining = mainHand.getMaxDamage() - mainHand.getDamageValue();
            if (remaining <= config.weaponBreakThreshold) {
                handleWeaponBreakWarning(client, player, remaining);
                return;
            }
        }

        // FOV cone check (Anti-Aura)
        if (config.maxFovAngle < 360.0 && !isInFrontalCone(player, currentTarget, config.maxFovAngle)) {
            return;
        }

        // Reach distance check
        if (player.distanceTo(currentTarget) > reach) {
            return;
        }

        // Line-Of-Sight (LOS) check
        if (config.mobKillMode == TakashaMasterConfig.MobKillMode.KAIZEN) {
            // Kaizen mode: strict raycast through half-slab grinder openings
            if (!hasLineOfSight(player, currentTarget)) {
                return;
            }
        } else {
            // Vanilla mode: fast line-of-sight check
            if (!player.hasLineOfSight(currentTarget) && !hasLineOfSight(player, currentTarget)) {
                return;
            }
        }

        // Smart Critical check (ensures 150% critical damage by striking during fall)
        if (config.smartCritical) {
            boolean isFalling = player.fallDistance > 0.0F && !player.onGround() && !player.isInWater() && !player.isPassenger();
            if (!isFalling) {
                return;
            }
        }

        // Attack Rate Mode enforcement
        long minIntervalMs = 250L;
        if (config.attackRateMode == TakashaMasterConfig.AttackRateMode.COOLDOWN_SYNC) {
            if (player.getAttackStrengthScale(0.5f) < 0.95f) {
                return;
            }
            minIntervalMs = 250L;
        } else if (config.attackRateMode == TakashaMasterConfig.AttackRateMode.TURBO) {
            if (player.getAttackStrengthScale(0.5f) < 0.70f) {
                return;
            }
            minIntervalMs = 100L;
        } else if (config.attackRateMode == TakashaMasterConfig.AttackRateMode.HYPER_SPAM) {
            // 0ms delay, attack as fast as every tick
            minIntervalMs = 0L;
        }

        long now = Util.getMillis();
        if (minIntervalMs > 0L && now - lastAttackTime < minIntervalMs) {
            return;
        }
        lastAttackTime = now;

        // Execute Attack
        if (client.gameMode != null) {
            client.gameMode.attack(player, currentTarget);
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

    public double getViewDotProduct(LocalPlayer player, Entity target) {
        Vec3 lookVec = player.getViewVector(1.0f);
        Vec3 targetCenter = target.position().add(0, target.getEyeHeight() * 0.5, 0);
        Vec3 toTarget = targetCenter.subtract(player.getEyePosition()).normalize();
        return lookVec.dot(toTarget);
    }

    public boolean isInFrontalCone(LocalPlayer player, Entity target, double maxFovDeg) {
        if (maxFovDeg >= 360.0) return true;
        double dot = getViewDotProduct(player, target);
        double minDot = Math.cos(Math.toRadians(maxFovDeg));
        return dot >= minDot;
    }

    /**
     * Raycast LOS check supporting half-slab grinder openings.
     */
    private boolean hasLineOfSight(LocalPlayer player, LivingEntity target) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 targetCenter = target.position().add(0, target.getEyeHeight() * 0.5, 0);

        // First check center
        HitResult hitCenter = player.level().clip(new ClipContext(
                eyePos, targetCenter, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        if (hitCenter.getType() == HitResult.Type.MISS) {
            return true;
        }

        // Second check feet (for half-slab mob farm gaps where feet are exposed)
        Vec3 targetFeet = target.position().add(0, 0.2, 0);
        HitResult hitFeet = player.level().clip(new ClipContext(
                eyePos, targetFeet, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        return hitFeet.getType() == HitResult.Type.MISS;
    }

    private boolean isValidTarget(LivingEntity entity, TakashaMasterConfig config) {
        if (entity instanceof Player) {
            if (!config.attackPlayers) return false;
        } else if (config.attackHostileOnly) {
            if (!(entity instanceof Monster)) {
                // If not monster, check if specifically whitelisted
                Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                if (!config.killMobWhitelist.contains(id.toString()) && !config.killMobWhitelist.contains(id.getPath())) {
                    return false;
                }
            }
        }

        if (!config.killMobWhitelist.isEmpty()) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            String idStr = id.toString();
            String path = id.getPath();
            return config.killMobWhitelist.contains(idStr) || config.killMobWhitelist.contains(path);
        }
        return true;
    }

    private LivingEntity selectBestTarget(LocalPlayer player, List<LivingEntity> list, TakashaMasterConfig config) {
        // Prioritize dangerous igniting creepers first
        for (LivingEntity entity : list) {
            if (entity instanceof Creeper creeper && creeper.isIgnited()) {
                if (config.maxFovAngle >= 360.0 || isInFrontalCone(player, creeper, config.maxFovAngle)) {
                    return creeper;
                }
            }
        }

        var stream = list.stream();
        if (config.maxFovAngle < 360.0) {
            if (!(config.mobKillMode == TakashaMasterConfig.MobKillMode.KAIZEN && config.autoCameraFix)) {
                stream = stream.filter(e -> isInFrontalCone(player, e, config.maxFovAngle));
            } else {
                stream = stream.filter(e -> hasLineOfSight(player, e));
            }
        }

        Comparator<LivingEntity> comparator;
        switch (config.killTargetPriority) {
            case LOWEST_HEALTH -> comparator = Comparator.comparingDouble(LivingEntity::getHealth);
            case HIGHEST_HEALTH -> comparator = Comparator.comparingDouble(LivingEntity::getHealth).reversed();
            case FOV_CENTER -> comparator = Comparator.comparingDouble(e -> -getViewDotProduct(player, e));
            case NEAREST -> comparator = Comparator.comparingDouble(player::distanceTo);
            default -> comparator = Comparator.comparingDouble(player::distanceTo);
        }

        return stream.min(comparator).orElse(null);
    }

    public boolean isWeaponValid(ItemStack stack, TakashaMasterConfig config) {
        if (stack == null || stack.isEmpty()) return false;
        boolean isWeapon = stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(Items.MACE);

        if (!isWeapon) return false;

        if (config.weaponBreakProtection && stack.isDamageableItem()) {
            int remaining = stack.getMaxDamage() - stack.getDamageValue();
            if (remaining <= config.weaponBreakThreshold) {
                return false;
            }
        }
        return true;
    }

    private void ensureWeaponEquipped(Minecraft client, LocalPlayer player, TakashaMasterConfig config) {
        if (isWeaponValid(player.getMainHandItem(), config)) {
            return;
        }

        var inventory = player.getInventory();
        // Check hotbar
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (isWeaponValid(stack, config)) {
                inventory.setSelectedSlot(i);
                if (client.getConnection() != null) {
                    client.getConnection().send(new ServerboundSetCarriedItemPacket(i));
                }
                return;
            }
        }

        // Check main inventory (slots 9-35)
        long now = Util.getMillis();
        if (now - lastEquipAttemptTime < 800L || client.gameMode == null) return;
        lastEquipAttemptTime = now;

        for (int i = 9; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (isWeaponValid(stack, config)) {
                int targetHotbar = inventory.getSelectedSlot();
                client.gameMode.handleContainerInput(
                        0,
                        i,
                        targetHotbar,
                        ContainerInput.SWAP,
                        player
                );
                return;
            }
        }
    }

    private void handleWeaponBreakWarning(Minecraft client, LocalPlayer player, int remainingDurability) {
        long now = Util.getMillis();
        if (now - lastBreakWarningTime > 2500L) {
            lastBreakWarningTime = now;
            player.playSound(SoundEvents.ANVIL_LAND, 0.8f, 0.8f);
            player.sendOverlayMessage(Component.literal("§c[AutoKill] Senjata hampir hancur (sisa " + remainingDurability + ")! Serangan dijeda demi keamanan."));
        }
        currentTarget = null;
    }

    private void applyKaizenCameraFix(LocalPlayer player, LivingEntity target, TakashaMasterConfig config) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 targetCenter = target.position().add(0, target.getEyeHeight() * 0.5, 0);
        Vec3 targetFeet = target.position().add(0, 0.2, 0);
        Vec3 aimPos = hasLineOfSight(player, target) ? (hasLineOfSightCenter(player, target) ? targetCenter : targetFeet) : targetCenter;

        double dx = aimPos.x - eyePos.x;
        double dy = aimPos.y - eyePos.y;
        double dz = aimPos.z - eyePos.z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float targetPitch = (float) (-(Mth.atan2(dy, horizontalDist) * (180.0 / Math.PI)));

        lockedGrinderYaw = targetYaw;
        lockedGrinderPitch = targetPitch;

        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();
        float yawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        if (Math.abs(yawDiff) > 1.5f || Math.abs(pitchDiff) > 1.5f) {
            float maxStep = (float) Math.max(5.0, config.autoCameraFixSpeed);
            float stepYaw = Mth.clamp(yawDiff, -maxStep, maxStep);
            float stepPitch = Mth.clamp(pitchDiff, -maxStep, maxStep);
            player.setYRot(currentYaw + stepYaw);
            player.setXRot(Mth.clamp(currentPitch + stepPitch, -90.0f, 90.0f));
        }
    }

    private void restoreLockedGrinderAngle(LocalPlayer player, TakashaMasterConfig config) {
        if (Float.isNaN(lockedGrinderYaw) || Float.isNaN(lockedGrinderPitch)) return;
        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();
        float yawDiff = Mth.wrapDegrees(lockedGrinderYaw - currentYaw);
        float pitchDiff = lockedGrinderPitch - currentPitch;

        if (Math.abs(yawDiff) > 1.5f || Math.abs(pitchDiff) > 1.5f) {
            float maxStep = (float) Math.max(5.0, config.autoCameraFixSpeed);
            float stepYaw = Mth.clamp(yawDiff, -maxStep, maxStep);
            float stepPitch = Mth.clamp(pitchDiff, -maxStep, maxStep);
            player.setYRot(currentYaw + stepYaw);
            player.setXRot(Mth.clamp(currentPitch + stepPitch, -90.0f, 90.0f));
        }
    }

    private boolean hasLineOfSightCenter(LocalPlayer player, LivingEntity target) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 targetCenter = target.position().add(0, target.getEyeHeight() * 0.5, 0);
        HitResult hitCenter = player.level().clip(new ClipContext(
                eyePos, targetCenter, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        return hitCenter.getType() == HitResult.Type.MISS;
    }

    public void reset(Minecraft client) {
        currentTarget = null;
        lockedGrinderYaw = Float.NaN;
        lockedGrinderPitch = Float.NaN;
        antiAfk.reset(client);
    }
}
