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
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.sakura.weapons.client.config.TakashaMasterConfig;

import java.util.Set;

@Environment(EnvType.CLIENT)
public class TakashaFarmHandler {

    private static final TakashaFarmHandler INSTANCE = new TakashaFarmHandler();

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
            return;
        }

        LocalPlayer player = client.player;
        Level level = client.level;
        if (player == null || level == null) {
            return;
        }

        long now = System.currentTimeMillis();

        // 1. Process SellGUI if screen is open
        Screen currentScreen = client.gui.screen();
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

        // 2. Check if inventory is full of crops and needs /sellgui
        if (config.autoSellGuiOnFull && isInventoryFull(player.getInventory())) {
            if (hasSellableCrops(player.getInventory(), config.targetCrops)) {
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

        // 3. Harvest nearby mature crops (Rate-limited to every 250ms)
        if (now - lastActionTime < 250L) {
            return;
        }

        BlockPos playerPos = player.blockPosition();
        int radiusH = 3;
        int radiusV = 1;

        for (int dy = -radiusV; dy <= radiusV; dy++) {
            for (int dx = -radiusH; dx <= radiusH; dx++) {
                for (int dz = -radiusH; dz <= radiusH; dz++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    Block block = state.getBlock();
                    Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);

                    if (blockId != null && config.targetCrops.contains(blockId.toString())) {
                        if (isMatureCrop(state, block)) {
                            harvestAndReplant(client, player, pos);
                            lastActionTime = now;
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean isMatureCrop(BlockState state, Block block) {
        if (block instanceof CropBlock cropBlock) {
            return cropBlock.isMaxAge(state);
        }
        if (block == Blocks.NETHER_WART && state.hasProperty(NetherWartBlock.AGE)) {
            return state.getValue(NetherWartBlock.AGE) >= 3;
        }
        if (block == Blocks.MELON || block == Blocks.PUMPKIN) {
            return true;
        }
        if (block == Blocks.SUGAR_CANE) {
            // Check if there is sugarcane below it
            return true;
        }
        return false;
    }

    private void harvestAndReplant(Minecraft client, LocalPlayer player, BlockPos pos) {
        if (client.gameMode == null) return;

        // 1. Break / attack block
        client.gameMode.startDestroyBlock(pos, Direction.UP);
        player.swing(InteractionHand.MAIN_HAND);

        // 2. Replant attempt via useItemOn
        BlockHitResult hitResult = new BlockHitResult(
            Vec3.atCenterOf(pos),
            Direction.UP,
            pos,
            false
        );
        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
    }

    private boolean isInventoryFull(Inventory inv) {
        // Main inventory slots are 0 to 35
        for (int i = 0; i < 36; i++) {
            if (inv.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasSellableCrops(Inventory inv, Set<String> targetCrops) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (itemId != null && targetCrops.contains(itemId.toString())) {
                    return true;
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

        boolean anyMoved = false;

        // Player inventory slots in container screens are typically the last 36 slots
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            // Check if slot belongs to player inventory
            if (slot.container instanceof Inventory) {
                ItemStack stack = slot.getItem();
                if (!stack.isEmpty()) {
                    Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (itemId != null && config.targetCrops.contains(itemId.toString())) {
                        client.gameMode.handleContainerInput(
                            containerId,
                            slot.index,
                            0,
                            ContainerInput.QUICK_MOVE,
                            player
                        );
                        anyMoved = true;
                    }
                }
            }
        }

        // Close GUI after items are put into SellGUI (Strictly NO chat sales report as per user constraint)
        player.closeContainer();
        awaitingSellGui = false;
    }
}
