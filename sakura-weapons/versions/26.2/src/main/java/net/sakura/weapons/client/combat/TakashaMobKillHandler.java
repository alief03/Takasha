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
 * Anti-Aura Auto Kill engine for mobs.
 * Strict frontal cone (FOV <= 35 degrees) prevents 360-degree KillAura bans.
 * Compatible with half-slab grinder LOS and synchronizes with weapon attack cooldown (>= 95%).
 * 100% Free Player Movement: Movement keys are never locked, allowing the player to walk/run freely.
 */
public class TakashaMobKillHandler {

    private static volatile TakashaMobKillHandler INSTANCE;

    private final TakashaMobKillAntiAfk antiAfk = new TakashaMobKillAntiAfk();
    private LivingEntity currentTarget = null;
    private long lastAttackTime = 0L;
    private long lastEquipAttemptTime = 0L;
    private long lastBreakWarningTime = 0L;

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

        // Anti-AFK tick (yields instantly if player presses movement keys)
        antiAfk.tick(client, player);

        // Pause if container screen is open
        if (client.gui.screen() != null) {
            return;
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
            return;
        }

        // Keep current target if still valid and within reach
        if (currentTarget == null || !currentTarget.isAlive() || !isValidTarget(currentTarget, config) || player.distanceTo(currentTarget) > reach) {
            currentTarget = selectBestTarget(player, candidates, config);
        }

        if (currentTarget == null) {
            return;
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

        // ANTI-AURA CHECK: Strict frontal cone (FOV <= 35 deg)
        if (!isInFrontalCone(player, currentTarget, config.maxFovAngle)) {
            // Target is not in front: do not attack (strictly no 360-degree aura)
            return;
        }

        // Distance check
        if (player.distanceTo(currentTarget) > reach) {
            return;
        }

        // LOS check (supports half-slab grinder openings)
        if (!hasLineOfSight(player, currentTarget)) {
            return;
        }

        // Weapon attack cooldown sync (wait for >= 95% full weapon strength)
        if (config.syncAttackCooldown && player.getAttackStrengthScale(0.5f) < 0.95f) {
            return;
        }

        // Execute attack!
        long now = Util.getMillis();
        if (now - lastAttackTime < 250L) {
            return;
        }
        lastAttackTime = now;

        if (client.gameMode != null) {
            client.gameMode.attack(player, currentTarget);
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

    /**
     * Checks if target is within the player's frontal view cone (dot product test).
     */
    public boolean isInFrontalCone(LocalPlayer player, Entity target, double maxFovDeg) {
        Vec3 lookVec = player.getViewVector(1.0f);
        Vec3 targetCenter = target.position().add(0, target.getEyeHeight() * 0.5, 0);
        Vec3 toTarget = targetCenter.subtract(player.getEyePosition()).normalize();

        double dot = lookVec.dot(toTarget);
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
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String idStr = id.toString();
        String path = id.getPath();
        return config.killMobWhitelist.contains(idStr) || config.killMobWhitelist.contains(path);
    }

    private LivingEntity selectBestTarget(LocalPlayer player, List<LivingEntity> list, TakashaMasterConfig config) {
        // Prioritize dangerous igniting creepers first
        for (LivingEntity entity : list) {
            if (entity instanceof Creeper creeper && creeper.isIgnited()) {
                if (isInFrontalCone(player, creeper, config.maxFovAngle)) {
                    return creeper;
                }
            }
        }

        // Closest target inside frontal cone
        return list.stream()
                .filter(e -> isInFrontalCone(player, e, config.maxFovAngle))
                .min(Comparator.comparingDouble(player::distanceTo))
                .orElse(null);
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

    public void reset(Minecraft client) {
        currentTarget = null;
        antiAfk.reset(client);
    }
}
