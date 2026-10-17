package net.sakura.weapons.client.fishing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.sakura.weapons.client.config.TakashaMasterConfig;

import java.util.List;

/**
 * Handles fishing rod disarm / slip recovery on Kaizen server or vanilla gameplay.
 * Automatically equips replacement rod from inventory, protects rod slot,
 * and rescues dropped rods from ground.
 */
public class RodSlipHandler {

    private static volatile RodSlipHandler INSTANCE;

    private int lastKnownRodSlot = -1;
    private long rodMissingSince = 0L;
    private long lastRecoveryTriggeredTime = 0L;
    private long lastSlotProtectionActionTime = 0L;
    private long lastGroundRescueCheckTime = 0L;
    private long lastJunkEjectTime = 0L;
    private boolean isWaitingForRodReappear = false;

    public static RodSlipHandler getInstance() {
        if (INSTANCE == null) {
            synchronized (RodSlipHandler.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RodSlipHandler();
                }
            }
        }
        return INSTANCE;
    }

    public void tick(Minecraft client) {
        TakashaMasterConfig config = TakashaMasterConfig.getInstance();
        if (!config.rodSlipRecovery || !config.autoFishEnabled) {
            return;
        }

        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return;
        }

        if (CapcayManager.getInstance().isBusy()) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        int selectedSlot = player.getInventory().getSelectedSlot();

        if (isRodValid(mainHand, config)) {
            lastKnownRodSlot = selectedSlot;
            isWaitingForRodReappear = false;
            rodMissingSince = 0L;
        } else {
            // Main hand has no rod!
            long now = Util.getMillis();
            if (rodMissingSince == 0L) {
                rodMissingSince = now;
            }

            // Layer 1: Slot protection
            tickSlotProtection(client, player);

            // Layer 2: Emergency ground rescue
            tickEmergencyGroundRescue(client, player, config);

            // Layer 3: Inventory rod swap
            if (now - rodMissingSince >= 300L) {
                boolean equipped = ensureRodEquipped(player, config);
                if (equipped) {
                    triggerRecovery("Joran otomatis dipasang kembali");
                }
            }
        }
    }

    public boolean isRodValid(ItemStack stack, TakashaMasterConfig config) {
        if (stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof FishingRodItem)) return false;

        if (config.rodBreakProtection && stack.isDamageableItem()) {
            int remaining = stack.getMaxDamage() - stack.getDamageValue();
            if (remaining <= config.rodBreakThreshold) {
                return false;
            }
        }
        return true;
    }

    public boolean ensureRodEquipped(LocalPlayer player, TakashaMasterConfig config) {
        if (player == null) return false;
        if (isRodValid(player.getMainHandItem(), config)) return true;

        Inventory inventory = player.getInventory();

        // 1. Search hotbar (0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (isRodValid(stack, config)) {
                inventory.setSelectedSlot(i);
                Minecraft client = Minecraft.getInstance();
                if (client.getConnection() != null) {
                    client.getConnection().send(new ServerboundSetCarriedItemPacket(i));
                }
                lastKnownRodSlot = i;
                return true;
            }
        }

        // 2. Search main inventory (9-35)
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode != null && client.player != null) {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = inventory.getItem(i);
                if (isRodValid(stack, config)) {
                    int targetHotbarSlot = (lastKnownRodSlot >= 0 && lastKnownRodSlot < 9) ? lastKnownRodSlot : inventory.getSelectedSlot();
                    client.gameMode.handleContainerInput(
                            0,
                            i,
                            targetHotbarSlot,
                            ContainerInput.SWAP,
                            client.player
                    );
                    inventory.setSelectedSlot(targetHotbarSlot);
                    if (client.getConnection() != null) {
                        client.getConnection().send(new ServerboundSetCarriedItemPacket(targetHotbarSlot));
                    }
                    lastKnownRodSlot = targetHotbarSlot;
                    log("§a[Autofish] Joran cadangan diambil dari tas ke hotbar slot " + (targetHotbarSlot + 1));
                    return true;
                }
            }
        }

        return false;
    }

    private void tickSlotProtection(Minecraft client, LocalPlayer player) {
        if (lastKnownRodSlot < 0 || lastKnownRodSlot >= 9) return;
        ItemStack itemInSlot = player.getInventory().getItem(lastKnownRodSlot);
        if (itemInSlot.isEmpty()) return;

        TakashaMasterConfig config = TakashaMasterConfig.getInstance();
        if (isRodValid(itemInSlot, config)) return;

        long now = Util.getMillis();
        if (now - lastSlotProtectionActionTime < 500L) return;
        lastSlotProtectionActionTime = now;

        int emptyInvSlot = -1;
        for (int i = 9; i < 36; i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                emptyInvSlot = i;
                break;
            }
        }

        if (emptyInvSlot != -1) {
            client.gameMode.handleContainerInput(
                    0,
                    emptyInvSlot,
                    lastKnownRodSlot,
                    ContainerInput.SWAP,
                    player
            );
        }
    }

    private void tickEmergencyGroundRescue(Minecraft client, LocalPlayer player, TakashaMasterConfig config) {
        long now = Util.getMillis();
        if (now - lastGroundRescueCheckTime < 400L) return;
        lastGroundRescueCheckTime = now;

        AABB searchBox = player.getBoundingBox().inflate(3.5D);
        List<ItemEntity> nearbyRods = client.level.getEntitiesOfClass(ItemEntity.class, searchBox, entity ->
                entity != null && entity.isAlive() && isRodValid(entity.getItem(), config)
        );

        if (nearbyRods.isEmpty()) return;

        boolean hasEmptySlot = false;
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                hasEmptySlot = true;
                break;
            }
        }

        if (!hasEmptySlot) {
            if (now - lastJunkEjectTime < 1000L) return;
            lastJunkEjectTime = now;

            int junkSlot = findJunkSlotToDrop(player.getInventory(), config);
            if (junkSlot != -1) {
                int containerSlot = junkSlot < 9 ? (36 + junkSlot) : junkSlot;
                client.gameMode.handleContainerInput(
                        0,
                        containerSlot,
                        1,
                        ContainerInput.THROW,
                        player
                );
                log("§c[Autofish] Joran di tanah terdeteksi! Membuang 1 item sampah untuk memungut joran.");
            }
        } else {
            if (ensureRodEquipped(player, config)) {
                log("§a[Autofish] Joran pancing di tanah berhasil diselamatkan dan dipasang!");
                triggerRecovery("Joran berhasil diselamatkan dari tanah");
            }
        }
    }

    private int findJunkSlotToDrop(Inventory inventory, TakashaMasterConfig config) {
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && isJunkItem(stack, config)) {
                return i;
            }
        }
        for (int i = 0; i < 9; i++) {
            if (i == inventory.getSelectedSlot()) continue;
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && isJunkItem(stack, config)) {
                return i;
            }
        }
        return -1;
    }

    public boolean isJunkItem(ItemStack stack, TakashaMasterConfig config) {
        if (stack == null || stack.isEmpty()) return false;
        if (isRodValid(stack, config)) return false;
        if (stack.isEnchanted()) return false;

        Item item = stack.getItem();
        return item == Items.COD
                || item == Items.SALMON
                || item == Items.TROPICAL_FISH
                || item == Items.PUFFERFISH
                || item == Items.LILY_PAD
                || item == Items.BOWL
                || item == Items.LEATHER
                || item == Items.ROTTEN_FLESH
                || item == Items.STICK
                || item == Items.STRING
                || item == Items.BONE
                || item == Items.GLASS_BOTTLE
                || item == Items.POTION
                || item == Items.INK_SAC
                || item == Items.TRIPWIRE_HOOK
                || item == Items.KELP
                || item == Items.SEAGRASS;
    }

    public void onHookCast() {
        // called when hook is cast
    }

    public void triggerRecovery(String reason) {
        long now = Util.getMillis();
        if (now - lastRecoveryTriggeredTime < 1500L) return;
        lastRecoveryTriggeredTime = now;
        TakashaFishingHandler.getInstance().queueRecast(600L);
    }

    private void log(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(message));
        }
    }
}
