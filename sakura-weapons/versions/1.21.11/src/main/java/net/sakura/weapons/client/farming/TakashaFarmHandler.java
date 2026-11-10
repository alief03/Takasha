package net.sakura.weapons.client.farming;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.sakura.weapons.client.config.TakashaMasterConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class TakashaFarmHandler {

    private static final TakashaFarmHandler INSTANCE = new TakashaFarmHandler();

    public record CropPlotInfo(
        BlockPos cropPos,
        BlockPos groundPos,
        Direction plantFace,
        Item seedItem,
        Block cropBlock,
        long lastSeenTimeMs
    ) {}

    private record PendingReplant(
        BlockPos cropPos,
        BlockPos groundPos,
        Direction face,
        Item seedItem,
        long targetTimeMs,
        int retriesLeft
    ) {}

    private record PendingVerification(
        BlockPos cropPos,
        BlockPos groundPos,
        Direction face,
        Item seedItem,
        long verifyTimeMs,
        int retriesLeft
    ) {}

    // In-memory collections bound to active autofarm session
    private final Map<BlockPos, CropPlotInfo> cropMemory = new HashMap<>();
    private final List<PendingReplant> pendingReplants = new ArrayList<>();
    private final List<PendingVerification> pendingVerifications = new ArrayList<>();

    private long lastActionTime = 0L;
    private long lastSellGuiCommandTime = 0L;
    private boolean awaitingSellGui = false;
    private long awaitingSellGuiStartTime = 0L;

    public static TakashaFarmHandler getInstance() {
        return INSTANCE;
    }

    public void tick(Minecraft client) {
        TakashaMasterConfig config = TakashaMasterConfig.getInstance();
        if (!config.autoFarmEnabled) {
            awaitingSellGui = false;
            pendingReplants.clear();
            pendingVerifications.clear();
            cropMemory.clear();
            return;
        }

        LocalPlayer player = client.player;
        Level level = client.level;
        if (player == null || level == null) {
            return;
        }

        if (net.sakura.weapons.client.fishing.CapcayManager.getInstance().isBusy() || net.sakura.weapons.client.food.TakashaAutoEatHandler.getInstance().isEating()) {
            return;
        }

        long now = System.currentTimeMillis();

        // 1. Process SellGUI if screen is open
        Screen currentScreen = client.screen;
        if (awaitingSellGui) {
            if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
                processSellGuiScreen(client, player, containerScreen);
                return;
            } else if (now - awaitingSellGuiStartTime > 4000L) {
                // Timeout after 4 seconds
                awaitingSellGui = false;
            }
        }

        // Pause harvesting while a container screen is open
        if (currentScreen != null) {
            return;
        }

        // 2. Process post-planting verifications (250ms check to detect rejected/desynced replants)
        processPendingVerifications(client, player, level, now);

        // 3. Process pending replants first (ensure broken crops get replanted from offhand)
        processPendingReplants(client, player, level, now, config);

        // 4. Check if inventory is full of crops and needs /sellgui
        if (config.autoSellGuiOnFull && isInventoryFull(player.getInventory())) {
            if (hasSellableCrops(player.getInventory(), config.targetCrops, config.seedReserveCount)) {
                if (now - lastSellGuiCommandTime > 5000L) {
                    lastSellGuiCommandTime = now;
                    awaitingSellGui = true;
                    awaitingSellGuiStartTime = now;
                    if (player.connection != null) {
                        player.connection.sendCommand("sellgui");
                    }
                    return;
                }
            }
        }

        // 5. Flow Control Backpressure: Rem pemanenan baru jika replant di sekitar pemain (<= 4.2m)
        // masih menumpuk (misal menunggu benih tersedot magnet). Mencegah pemain meninggalkan lahan bolong saat berlari.
        if (config.replantPacingEnabled && countPendingReplantsInReach(player, 4.2) >= 2) {
            lastActionTime = now;
            return;
        }

        // 6. Harvest nearby mature crops and plant empty farmland
        long actionDelay = Math.max(0L, (long) config.farmActionDelayMs);
        if (now - lastActionTime < actionDelay) {
            return;
        }

        BlockPos playerPos = player.blockPosition();
        int radiusH = Math.max(1, config.farmRadiusH);
        int radiusV = Math.max(1, config.farmRadiusV);
        int maxActions = Math.max(1, config.farmActionsPerTick);
        int actionsDone = 0;

        Vec3 lookVec = player.getViewVector(1.0f);
        Vec3 eyePos = player.getEyePosition();

        for (int dy = -radiusV; dy <= radiusV; dy++) {
            for (int dx = -radiusH; dx <= radiusH; dx++) {
                for (int dz = -radiusH; dz <= radiusH; dz++) {
                    if (actionsDone >= maxActions) {
                        lastActionTime = now;
                        return;
                    }

                    BlockPos pos = playerPos.offset(dx, dy, dz);

                    // Frontal view check if configured
                    if (config.farmOnlyInFront) {
                        Vec3 toTarget = Vec3.atCenterOf(pos).subtract(eyePos).normalize();
                        if (lookVec.dot(toTarget) < 0.35) {
                            continue;
                        }
                    }

                    BlockState state = level.getBlockState(pos);
                    Block block = state.getBlock();
                    Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);

                    // Passive Observation: Catat ke crop memory jika tanaman terdaftar
                    if (config.cropMemoryEnabled && isTrackableCrop(block)) {
                        recordCropPlotToMemory(pos, state, block, now);
                    }

                    // A. Check mature crop harvest
                    if (blockId != null && config.targetCrops.contains(blockId.toString())) {
                        if (isMatureCrop(state, block, level, pos)) {
                            harvestAndScheduleReplant(client, player, level, pos, state, block, now, config);
                            actionsDone++;
                            continue;
                        }
                    }

                    // B. Auto plant empty farmland or heal plots
                    if (config.autoPlantEmptyFarmland) {
                        if (block == Blocks.FARMLAND || block == Blocks.SOUL_SAND) {
                            BlockPos abovePos = pos.above();
                            BlockState aboveState = level.getBlockState(abovePos);
                            if (aboveState.isAir()) {
                                if (tryPlantOrHealPlot(client, player, level, pos, abovePos, config, now)) {
                                    actionsDone++;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (actionsDone > 0) {
            lastActionTime = now;
        }

        // Memory cleanup jika ukuran memori melebihi 1000 entri untuk plot yang jauh (>64 blok)
        if (config.cropMemoryEnabled && cropMemory.size() > 1000) {
            cropMemory.entrySet().removeIf(entry ->
                entry.getValue().cropPos().distSqr(playerPos) > 4096
            );
        }
    }

    private boolean isTrackableCrop(Block block) {
        return block instanceof CropBlock
            || block == Blocks.NETHER_WART
            || block == Blocks.COCOA
            || block == Blocks.SWEET_BERRY_BUSH;
    }

    private void recordCropPlotToMemory(BlockPos pos, BlockState state, Block block, long now) {
        Item seedItem = getSeedItemForCrop(block);
        if (seedItem == null) return;

        BlockPos groundPos = (block == Blocks.COCOA && state.hasProperty(CocoaBlock.FACING))
            ? pos.relative(state.getValue(CocoaBlock.FACING))
            : pos.below();
        Direction face = (block == Blocks.COCOA && state.hasProperty(CocoaBlock.FACING))
            ? state.getValue(CocoaBlock.FACING).getOpposite()
            : Direction.UP;

        cropMemory.put(pos, new CropPlotInfo(pos, groundPos, face, seedItem, block, now));
    }

    private int countPendingReplantsInReach(LocalPlayer player, double maxDist) {
        if (pendingReplants.isEmpty()) return 0;
        Vec3 eyePos = player.getEyePosition();
        double maxDistSqr = maxDist * maxDist;
        int count = 0;
        for (PendingReplant replant : pendingReplants) {
            if (eyePos.distanceToSqr(Vec3.atCenterOf(replant.groundPos())) <= maxDistSqr) {
                count++;
            }
        }
        return count;
    }

    private boolean isMatureCrop(BlockState state, Block block, Level level, BlockPos pos) {
        if (block instanceof CropBlock cropBlock) {
            return cropBlock.isMaxAge(state);
        }
        if (block == Blocks.NETHER_WART && state.hasProperty(NetherWartBlock.AGE)) {
            return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        }
        if (block == Blocks.COCOA && state.hasProperty(CocoaBlock.AGE)) {
            return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
        }
        if (block == Blocks.SWEET_BERRY_BUSH && state.hasProperty(SweetBerryBushBlock.AGE)) {
            return state.getValue(SweetBerryBushBlock.AGE) >= 2;
        }
        if (block == Blocks.MELON || block == Blocks.PUMPKIN) {
            return true;
        }
        if (block == Blocks.SUGAR_CANE) {
            return level.getBlockState(pos.below()).is(Blocks.SUGAR_CANE);
        }
        if (block == Blocks.CACTUS) {
            return level.getBlockState(pos.below()).is(Blocks.CACTUS);
        }
        if (block == Blocks.BAMBOO) {
            return level.getBlockState(pos.below()).is(Blocks.BAMBOO);
        }
        if (block == Blocks.KELP || block == Blocks.KELP_PLANT) {
            BlockState below = level.getBlockState(pos.below());
            return below.is(Blocks.KELP) || below.is(Blocks.KELP_PLANT);
        }
        return false;
    }

    private Item getSeedItemForCrop(Block block) {
        if (block == Blocks.WHEAT) return Items.WHEAT_SEEDS;
        if (block == Blocks.CARROTS) return Items.CARROT;
        if (block == Blocks.POTATOES) return Items.POTATO;
        if (block == Blocks.BEETROOTS) return Items.BEETROOT_SEEDS;
        if (block == Blocks.NETHER_WART) return Items.NETHER_WART;
        if (block == Blocks.COCOA) return Items.COCOA_BEANS;
        return null;
    }

    private void harvestAndScheduleReplant(Minecraft client, LocalPlayer player, Level level, BlockPos pos, BlockState state, Block block, long now, TakashaMasterConfig config) {
        if (client.gameMode == null) return;

        // Sweet Berry Bush is non-destructive (right-click harvest)
        if (block == Blocks.SWEET_BERRY_BUSH) {
            BlockHitResult hitResult = new BlockHitResult(
                Vec3.atCenterOf(pos),
                Direction.UP,
                pos,
                false
            );
            client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
            player.swing(InteractionHand.MAIN_HAND);
            return;
        }

        // Ensure main hand holds hoe
        ensureHoeInMainHand(client, player);

        // Calculate plot coordinates and seed item
        BlockPos groundPos = (block == Blocks.COCOA && state.hasProperty(CocoaBlock.FACING))
            ? pos.relative(state.getValue(CocoaBlock.FACING))
            : pos.below();
        Direction face = (block == Blocks.COCOA && state.hasProperty(CocoaBlock.FACING))
            ? state.getValue(CocoaBlock.FACING).getOpposite()
            : Direction.UP;
        Item seedItem = getSeedItemForCrop(block);

        // Record to crop memory for current session
        if (seedItem != null && config.cropMemoryEnabled) {
            cropMemory.put(pos, new CropPlotInfo(pos, groundPos, face, seedItem, block, now));
        }

        // Break block
        client.gameMode.startDestroyBlock(pos, Direction.UP);
        client.gameMode.destroyBlock(pos);
        player.swing(InteractionHand.MAIN_HAND);

        // IN-TICK INSTANT REPLANT PAIRING:
        // Jika benih sudah siap di tas/offhand dan jarak interaksi survival valid (<= 4.2m)
        if (seedItem != null) {
            Vec3 eyePos = player.getEyePosition();
            double distSqr = eyePos.distanceToSqr(Vec3.atCenterOf(groundPos));
            boolean inReach = distSqr <= 17.64; // 4.2 * 4.2

            if (inReach && ensureOffhandHasSeed(client, player, seedItem)) {
                BlockHitResult hitResult = new BlockHitResult(
                    Vec3.atCenterOf(pos),
                    face,
                    groundPos,
                    false
                );
                client.gameMode.useItemOn(player, InteractionHand.OFF_HAND, hitResult);
                player.swing(InteractionHand.OFF_HAND);
                // Jadwalkan verifikasi pasca-tanam 250ms
                pendingVerifications.add(new PendingVerification(pos, groundPos, face, seedItem, now + 250L, 3));
            } else {
                // Benih belum di tas (menunggu drop ~500ms) atau di luar reach saat itu -> antrekan dengan backoff
                long replantDelay = Math.max(0L, (long) config.farmReplantDelayMs);
                pendingReplants.add(new PendingReplant(pos, groundPos, face, seedItem, now + replantDelay, config.maxReplantRetries));
            }
        }
    }

    private boolean tryPlantOrHealPlot(Minecraft client, LocalPlayer player, Level level, BlockPos groundPos, BlockPos cropPos, TakashaMasterConfig config, long now) {
        if (client.gameMode == null) return false;

        // Reach distance <= 4.2m
        if (player.getEyePosition().distanceToSqr(Vec3.atCenterOf(groundPos)) > 17.64) {
            return false;
        }

        CropPlotInfo memory = cropMemory.get(cropPos);
        Item seed = null;
        Direction face = Direction.UP;

        if (config.cropMemoryEnabled && memory != null && memory.seedItem() != null && hasItemInInventoryOrOffhand(player, memory.seedItem())) {
            seed = memory.seedItem();
            face = memory.plantFace();
        } else if (level.getBlockState(groundPos).is(Blocks.FARMLAND)) {
            seed = findAvailableSeedForFarmland(player, config);
        } else if (level.getBlockState(groundPos).is(Blocks.SOUL_SAND) && config.targetCrops.contains("minecraft:nether_wart") && hasItemInInventoryOrOffhand(player, Items.NETHER_WART)) {
            seed = Items.NETHER_WART;
        }

        if (seed == null) return false;

        if (!ensureOffhandHasSeed(client, player, seed)) {
            return false;
        }

        BlockHitResult hitResult = new BlockHitResult(
            Vec3.atCenterOf(cropPos),
            face,
            groundPos,
            false
        );
        client.gameMode.useItemOn(player, InteractionHand.OFF_HAND, hitResult);
        player.swing(InteractionHand.OFF_HAND);
        pendingVerifications.add(new PendingVerification(cropPos, groundPos, face, seed, now + 250L, 3));
        return true;
    }

    private Item findAvailableSeedForFarmland(LocalPlayer player, TakashaMasterConfig config) {
        if (config.targetCrops.contains("minecraft:wheat") && hasItemInInventoryOrOffhand(player, Items.WHEAT_SEEDS)) {
            return Items.WHEAT_SEEDS;
        }
        if (config.targetCrops.contains("minecraft:carrots") && hasItemInInventoryOrOffhand(player, Items.CARROT)) {
            return Items.CARROT;
        }
        if (config.targetCrops.contains("minecraft:potatoes") && hasItemInInventoryOrOffhand(player, Items.POTATO)) {
            return Items.POTATO;
        }
        if (config.targetCrops.contains("minecraft:beetroots") && hasItemInInventoryOrOffhand(player, Items.BEETROOT_SEEDS)) {
            return Items.BEETROOT_SEEDS;
        }
        return null;
    }

    private boolean hasItemInInventoryOrOffhand(LocalPlayer player, Item item) {
        if (player.getOffhandItem().is(item)) return true;
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private void processPendingVerifications(Minecraft client, LocalPlayer player, Level level, long now) {
        if (client.gameMode == null || pendingVerifications.isEmpty()) return;

        List<PendingVerification> toVerify = new ArrayList<>(pendingVerifications);
        pendingVerifications.clear();

        for (PendingVerification verify : toVerify) {
            if (now < verify.verifyTimeMs()) {
                pendingVerifications.add(verify);
                continue;
            }

            BlockPos cropPos = verify.cropPos();
            BlockState state = level.getBlockState(cropPos);

            // Jika blok masih udara atau canBeReplaced, penanaman ditolak server / desync!
            if (state.isAir() || state.canBeReplaced()) {
                BlockState ground = level.getBlockState(verify.groundPos());
                if (!ground.isAir() && verify.retriesLeft() > 0) {
                    // Masukkan kembali ke pendingReplants dengan prioritas segera!
                    pendingReplants.add(new PendingReplant(
                        cropPos,
                        verify.groundPos(),
                        verify.face(),
                        verify.seedItem(),
                        now,
                        verify.retriesLeft() - 1
                    ));
                }
            }
        }
    }

    private void processPendingReplants(Minecraft client, LocalPlayer player, Level level, long now, TakashaMasterConfig config) {
        if (client.gameMode == null || pendingReplants.isEmpty()) return;

        List<PendingReplant> toProcess = new ArrayList<>(pendingReplants);
        pendingReplants.clear();

        int maxActions = Math.max(1, config.farmActionsPerTick);
        int actionsDone = 0;
        Vec3 eyePos = player.getEyePosition();

        for (PendingReplant replant : toProcess) {
            if (now < replant.targetTimeMs()) {
                pendingReplants.add(replant);
                continue;
            }

            BlockPos cropPos = replant.cropPos();
            BlockPos groundPos = replant.groundPos();

            // Pengecekan jarak reach <= 4.2m
            double distSqr = eyePos.distanceToSqr(Vec3.atCenterOf(groundPos));
            if (distSqr > 17.64) {
                // Pemain berada di luar jangkauan saat ini -> simpan kembali tanpa penalti retry!
                pendingReplants.add(replant);
                continue;
            }

            BlockState currentState = level.getBlockState(cropPos);

            // Jika blok sudah terisi tanaman / bukan udara
            if (!currentState.isAir() && !currentState.canBeReplaced()) {
                if (currentState.getBlock() instanceof CropBlock
                    || currentState.getBlock() == Blocks.NETHER_WART
                    || currentState.getBlock() == Blocks.COCOA) {
                    continue; // Sudah tertanam dengan sukses
                }
                // Masih proses hancur di sisi server
                if (replant.retriesLeft() > 0) {
                    pendingReplants.add(new PendingReplant(
                        cropPos, groundPos, replant.face(), replant.seedItem(),
                        now + 100L, replant.retriesLeft() - 1
                    ));
                }
                continue;
            }

            // Pastikan tanah dasarnya masih ada
            BlockState groundState = level.getBlockState(groundPos);
            if (groundState.isAir()) {
                continue;
            }

            // Pastikan offhand memiliki benih yang diperlukan
            if (!ensureOffhandHasSeed(client, player, replant.seedItem())) {
                // Benih belum di tas (sedang jatuh / delay magnet ~500ms) -> JANGAN DIHAPUS!
                if (replant.retriesLeft() > 0) {
                    pendingReplants.add(new PendingReplant(
                        cropPos, groundPos, replant.face(), replant.seedItem(),
                        now + 150L, replant.retriesLeft() - 1
                    ));
                }
                continue;
            }

            // Replant using offhand
            BlockHitResult hitResult = new BlockHitResult(
                Vec3.atCenterOf(cropPos),
                replant.face(),
                groundPos,
                false
            );
            client.gameMode.useItemOn(player, InteractionHand.OFF_HAND, hitResult);
            player.swing(InteractionHand.OFF_HAND);

            // Masukkan ke verifikasi pasca-tanam 250ms
            pendingVerifications.add(new PendingVerification(cropPos, groundPos, replant.face(), replant.seedItem(), now + 250L, 3));

            actionsDone++;
            if (actionsDone >= maxActions) {
                break;
            }
        }
    }

    private boolean ensureOffhandHasSeed(Minecraft client, LocalPlayer player, Item seedItem) {
        if (client.gameMode == null) return false;

        // Check if offhand already has the seed (STICKY OFFHAND CACHING - 0 SWAP PACKET!)
        ItemStack offhandStack = player.getOffhandItem();
        if (!offhandStack.isEmpty() && offhandStack.is(seedItem)) {
            return true;
        }

        // Search player inventory (slots 0-35)
        Inventory inv = player.getInventory();
        int foundSlot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(seedItem)) {
                foundSlot = i;
                break;
            }
        }

        if (foundSlot == -1) {
            return false;
        }

        // In InventoryMenu (containerId = 0):
        // Hotbar 0..8 -> container slots 36..44
        // Main inventory 9..35 -> container slots 9..35
        int containerSlot = (foundSlot < 9 ? 36 + foundSlot : foundSlot);
        performOffhandSwap(client, player, containerSlot);
        return true;
    }

    private void performOffhandSwap(Minecraft client, LocalPlayer player, int containerSlot) {
        client.gameMode.handleInventoryMouseClick(0, containerSlot, 40, ClickType.SWAP, player);
    }

    private void performHotbarSwap(Minecraft client, LocalPlayer player, int containerSlot, int hotbarIndex) {
        client.gameMode.handleInventoryMouseClick(0, containerSlot, hotbarIndex, ClickType.SWAP, player);
    }

    private void performQuickMove(Minecraft client, LocalPlayer player, int containerId, int slotIndex) {
        client.gameMode.handleInventoryMouseClick(containerId, slotIndex, 0, ClickType.QUICK_MOVE, player);
    }

    private void ensureHoeInMainHand(Minecraft client, LocalPlayer player) {
        if (player.getMainHandItem().getItem() instanceof HoeItem) {
            return;
        }

        Inventory inv = player.getInventory();
        // 1. Hotbar check
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).getItem() instanceof HoeItem) {
                inv.setSelectedSlot(i);
                if (player.connection != null) {
                    player.connection.send(new ServerboundSetCarriedItemPacket(i));
                }
                return;
            }
        }

        // 2. Main inventory check
        for (int i = 9; i < 36; i++) {
            if (inv.getItem(i).getItem() instanceof HoeItem) {
                int currentHotbar = inv.getSelectedSlot();
                performHotbarSwap(client, player, i, currentHotbar);
                return;
            }
        }
    }

    private boolean isInventoryFull(Inventory inv) {
        for (int i = 0; i < 36; i++) {
            if (inv.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean isItemMatchingCrop(Item item, String cropId) {
        if ("minecraft:wheat".equals(cropId)) return item == Items.WHEAT;
        if ("minecraft:carrots".equals(cropId)) return item == Items.CARROT;
        if ("minecraft:potatoes".equals(cropId)) return item == Items.POTATO;
        if ("minecraft:beetroots".equals(cropId)) return item == Items.BEETROOT;
        if ("minecraft:nether_wart".equals(cropId)) return item == Items.NETHER_WART;
        if ("minecraft:sugar_cane".equals(cropId)) return item == Items.SUGAR_CANE;
        if ("minecraft:melon".equals(cropId)) return item == Items.MELON_SLICE || item == Items.MELON;
        if ("minecraft:pumpkin".equals(cropId)) return item == Items.PUMPKIN;
        if ("minecraft:cocoa".equals(cropId)) return item == Items.COCOA_BEANS;
        if ("minecraft:sweet_berry_bush".equals(cropId)) return item == Items.SWEET_BERRIES;
        if ("minecraft:cactus".equals(cropId)) return item == Items.CACTUS;
        if ("minecraft:bamboo".equals(cropId)) return item == Items.BAMBOO;
        if ("minecraft:kelp".equals(cropId)) return item == Items.KELP;
        return false;
    }

    private boolean isReserveCropItem(Item item) {
        return item == Items.CARROT || item == Items.POTATO || item == Items.NETHER_WART
            || item == Items.COCOA_BEANS || item == Items.SWEET_BERRIES;
    }

    private boolean hasSellableCrops(Inventory inv, Set<String> targetCrops, int seedReserveCount) {
        Map<Item, Integer> counts = new HashMap<>();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                counts.put(stack.getItem(), counts.getOrDefault(stack.getItem(), 0) + stack.getCount());
            }
        }

        for (Map.Entry<Item, Integer> entry : counts.entrySet()) {
            Item item = entry.getKey();
            int count = entry.getValue();

            // Never sell seeds
            if (item == Items.WHEAT_SEEDS || item == Items.BEETROOT_SEEDS) {
                continue;
            }

            for (String targetCrop : targetCrops) {
                if (isItemMatchingCrop(item, targetCrop)) {
                    if (isReserveCropItem(item)) {
                        if (count > seedReserveCount) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void processSellGuiScreen(Minecraft client, LocalPlayer player, AbstractContainerScreen<?> containerScreen) {
        if (client.gameMode == null) return;
        AbstractContainerMenu menu = containerScreen.getMenu();
        int containerId = menu.containerId;
        TakashaMasterConfig config = TakashaMasterConfig.getInstance();

        // Calculate item counts in player inventory
        Map<Item, Integer> remainingCounts = new HashMap<>();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                remainingCounts.put(stack.getItem(), remainingCounts.getOrDefault(stack.getItem(), 0) + stack.getCount());
            }
        }

        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot.container instanceof Inventory) {
                ItemStack stack = slot.getItem();
                if (!stack.isEmpty()) {
                    Item item = stack.getItem();

                    // Never sell seeds
                    if (item == Items.WHEAT_SEEDS || item == Items.BEETROOT_SEEDS) {
                        continue;
                    }

                    boolean matched = false;
                    for (String targetCrop : config.targetCrops) {
                        if (isItemMatchingCrop(item, targetCrop)) {
                            matched = true;
                            break;
                        }
                    }

                    if (matched) {
                        int currentCount = remainingCounts.getOrDefault(item, 0);
                        if (isReserveCropItem(item)) {
                            if (currentCount - stack.getCount() >= config.seedReserveCount) {
                                remainingCounts.put(item, currentCount - stack.getCount());
                                performQuickMove(client, player, containerId, slot.index);
                            }
                        } else {
                            performQuickMove(client, player, containerId, slot.index);
                        }
                    }
                }
            }
        }

        player.closeContainer();
        awaitingSellGui = false;
    }
}
