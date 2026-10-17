package net.sakura.weapons.client.fishing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.sakura.weapons.client.config.TakashaMasterConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Core fishing automation engine supporting:
 * 1. Kaizen mode: bobber motion spike packet tracking (-0.1 Y velocity) + Capcay 1.0 + Rod Slip recovery.
 * 2. Vanilla mode: sound splash packet detection (distanceSq < 25).
 * 3. Catch Filter (Keep Whitelist): automatically ejects items not selected in keepCatchWhitelist.
 */
public class TakashaFishingHandler {

    private static volatile TakashaFishingHandler INSTANCE;

    private static final double MOTION_Y_THRESHOLD = -0.1;
    private static final int START_CATCHING_AFTER_THRESHOLD_MS = 900;
    private static final double HOOK_SOUND_DIST_SQ = 25.0;

    private boolean isCapcayPaused = false;
    private boolean hasHitWater = false;
    private long bobberRiseTimestamp = 0L;
    private long scheduledRecastTime = 0L;
    private boolean isRecastQueued = false;
    private long lastReelInTime = 0L;

    // Catch Filter Watcher state
    private boolean isCatchWatcherActive = false;
    private long catchWatcherEndTime = 0L;
    private final Map<Integer, ItemStack> inventorySnapshot = new HashMap<>();

    private final Random random = new Random();

    public static TakashaFishingHandler getInstance() {
        if (INSTANCE == null) {
            synchronized (TakashaFishingHandler.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TakashaFishingHandler();
                }
            }
        }
        return INSTANCE;
    }

    public void tick(Minecraft client) {
        TakashaMasterConfig config = TakashaMasterConfig.getInstance();
        if (!config.autoFishEnabled) {
            reset();
            return;
        }

        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return;
        }

        // Tick sub-components
        CapcayManager.getInstance().tick(client);
        RodSlipHandler.getInstance().tick(client);

        if (isCapcayPaused || CapcayManager.getInstance().isBusy()) {
            return;
        }

        // Process scheduled recast
        long now = Util.getMillis();
        if (isRecastQueued && now >= scheduledRecastTime) {
            isRecastQueued = false;
            prepareAndCast(client, config);
        }

        // Monitor hook in water
        FishingHook hook = player.fishing;
        if (hook != null && isHoldingRod(player)) {
            if (isBlockInWater(hook.level(), hook.getBoundingBox())) {
                hasHitWater = true;
            }
        } else {
            hasHitWater = false;
            bobberRiseTimestamp = 0L;
        }

        // Process Catch Filter Watcher (auto-drop non-whitelisted catches)
        if (isCatchWatcherActive) {
            if (now > catchWatcherEndTime) {
                isCatchWatcherActive = false;
                inventorySnapshot.clear();
            } else {
                tickCatchFilter(client, player, config);
            }
        }
    }

    /**
     * Inspects inventory for newly caught items and throws out any item not in keepCatchWhitelist.
     */
    private void tickCatchFilter(Minecraft client, LocalPlayer player, TakashaMasterConfig config) {
        if (!config.filterCatchEnabled || client.gameMode == null) {
            return;
        }

        for (int i = 0; i < 36; i++) {
            ItemStack currentStack = player.getInventory().getItem(i);
            if (currentStack.isEmpty()) continue;

            // Never drop active fishing rod or weapons
            if (currentStack.getItem() instanceof FishingRodItem) continue;

            Identifier itemId = BuiltInRegistries.ITEM.getKey(currentStack.getItem());
            String idStr = itemId.toString();

            // Check if this item type is caught / known fishing item
            if (isKnownCatchItem(currentStack)) {
                // If item is NOT in the keep whitelist, throw it out immediately!
                if (!config.keepCatchWhitelist.contains(idStr) && !config.keepCatchWhitelist.contains(itemId.getPath())) {
                    int containerSlot = i < 9 ? (36 + i) : i;
                    client.gameMode.handleContainerInput(
                            0,
                            containerSlot,
                            1, // Throw entire stack
                            ContainerInput.THROW,
                            player
                    );
                    log("§7[Autofish Filter] Membuang hasil tangkapan non-whitelist: §f" + currentStack.getHoverName().getString());
                }
            }
        }
    }

    private boolean isKnownCatchItem(ItemStack stack) {
        var item = stack.getItem();
        return item == Items.COD
                || item == Items.SALMON
                || item == Items.TROPICAL_FISH
                || item == Items.PUFFERFISH
                || item == Items.LILY_PAD
                || item == Items.BOWL
                || item == Items.LEATHER
                || item == Items.LEATHER_BOOTS
                || item == Items.ROTTEN_FLESH
                || item == Items.STICK
                || item == Items.STRING
                || item == Items.POTION
                || item == Items.BONE
                || item == Items.INK_SAC
                || item == Items.TRIPWIRE_HOOK
                || item == Items.ENCHANTED_BOOK
                || item == Items.BOW
                || item == Items.FISHING_ROD
                || item == Items.NAME_TAG
                || item == Items.NAUTILUS_SHELL
                || item == Items.SADDLE;
    }

    public void handleMotionPacket(ClientboundSetEntityMotionPacket packet, Minecraft client) {
        TakashaMasterConfig config = TakashaMasterConfig.getInstance();
        if (!config.autoFishEnabled || config.fishingMode != TakashaMasterConfig.FishingMode.KAIZEN) {
            return;
        }
        if (isCapcayPaused || CapcayManager.getInstance().isBusy()) {
            return;
        }

        LocalPlayer player = client.player;
        if (player != null && player.fishing != null && player.fishing.getId() == packet.id()) {
            long now = Util.getMillis();
            if (hasHitWater && bobberRiseTimestamp == 0L && packet.movement().y() > 0) {
                bobberRiseTimestamp = now;
            }

            long timeInWater = now - bobberRiseTimestamp;
            if (hasHitWater && bobberRiseTimestamp != 0L && timeInWater > START_CATCHING_AFTER_THRESHOLD_MS) {
                if (packet.movement().x() == 0.0 && packet.movement().z() == 0.0 && packet.movement().y() < MOTION_Y_THRESHOLD) {
                    catchFish(client, config);
                    hasHitWater = false;
                    bobberRiseTimestamp = 0L;
                }
            }
        }
    }

    public void handleSoundPacket(ClientboundSoundPacket packet, Minecraft client) {
        TakashaMasterConfig config = TakashaMasterConfig.getInstance();
        if (!config.autoFishEnabled || config.fishingMode != TakashaMasterConfig.FishingMode.VANILLA) {
            return;
        }
        if (isCapcayPaused || CapcayManager.getInstance().isBusy()) {
            return;
        }

        SoundEvent soundEvent = packet.getSound().value();
        String soundName = soundEvent.location().toString();

        if (soundName.equalsIgnoreCase("minecraft:entity.fishing_bobber.splash") || soundName.equalsIgnoreCase("entity.fishing_bobber.splash")) {
            LocalPlayer player = client.player;
            if (player != null && player.fishing != null) {
                if (player.fishing.distanceToSqr(packet.getX(), packet.getY(), packet.getZ()) < HOOK_SOUND_DIST_SQ) {
                    catchFish(client, config);
                }
            }
        }
    }

    public void catchFish(Minecraft client, TakashaMasterConfig config) {
        if (isRecastQueued) return;
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) return;

        long now = Util.getMillis();
        if (now - lastReelInTime < 500L) return;
        lastReelInTime = now;

        // Reel in hook
        useRod(client, player);

        // Activate catch filter watcher for 2.5s to inspect and filter items received
        if (config.filterCatchEnabled) {
            isCatchWatcherActive = true;
            catchWatcherEndTime = now + 2500L;
        }

        // Schedule recast
        long recastDelay = config.recastDelayMs + (long) ((random.nextDouble() * 200.0) - 100.0);
        queueRecast(Math.max(400L, recastDelay));
    }

    public void queueRecast(long delayMs) {
        isRecastQueued = true;
        scheduledRecastTime = Util.getMillis() + delayMs;
    }

    public void prepareAndCast(Minecraft client, TakashaMasterConfig config) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) return;

        if (config.rodSlipRecovery) {
            RodSlipHandler.getInstance().ensureRodEquipped(player, config);
        }

        if (isHoldingRod(player)) {
            useRod(client, player);
        }
    }

    public void useRod(Minecraft client, LocalPlayer player) {
        if (client.gameMode == null || player == null) return;

        InteractionHand hand = InteractionHand.MAIN_HAND;
        if (!(player.getMainHandItem().getItem() instanceof FishingRodItem) && player.getOffhandItem().getItem() instanceof FishingRodItem) {
            hand = InteractionHand.OFF_HAND;
        }

        player.swing(hand);
        InteractionResult result = client.gameMode.useItem(player, hand);
        if (result != null && result.consumesAction()) {
            client.gameRenderer.itemInHandRenderer.itemUsed(hand);
        }
    }

    private boolean isHoldingRod(LocalPlayer player) {
        return player.getMainHandItem().getItem() instanceof FishingRodItem
                || player.getOffhandItem().getItem() instanceof FishingRodItem;
    }

    private boolean isBlockInWater(Level world, AABB box) {
        int minX = Mth.floor(box.minX);
        int maxX = Mth.ceil(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.ceil(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.ceil(box.maxZ);
        return BlockPos.betweenClosedStream(minX, minY, minZ, maxX - 1, maxY - 1, maxZ - 1)
                .anyMatch(pos -> world.getBlockState(pos).is(Blocks.WATER));
    }

    public void pauseForCapcay() {
        this.isCapcayPaused = true;
    }

    public void resumeAfterCapcay() {
        this.isCapcayPaused = false;
        queueRecast(600L);
    }

    public void reset() {
        isCapcayPaused = false;
        hasHitWater = false;
        bobberRiseTimestamp = 0L;
        isRecastQueued = false;
        isCatchWatcherActive = false;
    }

    private void log(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(message));
        }
    }
}
