package net.sakura.touhou.compat.tlmo.task;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.sakura.touhou.entity.EntityMaid;
import net.sakura.touhou.entity.task.MaidTaskMode;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * MaidTreeTask
 * Tugas penebangan pohon otomatis (felling logs & leaves) dan penanaman kembali bibit pohon (saplings).
 * Berdasarkan `studio.fantasyit.maid_useful_task.task.MaidTreeTask`.
 */
public class MaidTreeTask extends Goal {

    private final EntityMaid maid;
    private BlockPos targetLogPos = null;
    private BlockPos plantPos = null;
    private final Queue<BlockPos> treeBlocksQueue = new ArrayDeque<>();
    private int breakCooldown = 0;

    public MaidTreeTask(EntityMaid maid) {
        this.maid = maid;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (maid.isSitting() || maid.getTaskMode() != MaidTaskMode.FARM) {
            return false;
        }
        return hasAxe(maid);
    }

    private boolean hasAxe(EntityMaid maid) {
        if (maid.getMainHandItem().is(ItemTags.AXES)) {
            return true;
        }
        var inv = maid.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(ItemTags.AXES)) {
                return true;
            }
        }
        return false;
    }

    private void equipAxe() {
        if (maid.getMainHandItem().is(ItemTags.AXES)) return;
        var inv = maid.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(ItemTags.AXES)) {
                ItemStack mainHand = maid.getMainHandItem();
                maid.setItemInHand(InteractionHand.MAIN_HAND, stack);
                inv.setItem(i, mainHand);
                return;
            }
        }
    }

    @Override
    public void tick() {
        if (!(maid.level() instanceof ServerLevel level)) return;

        breakCooldown++;

        // 1. Replanting stage if a tree was chopped
        if (plantPos != null && treeBlocksQueue.isEmpty()) {
            tryReplantSapling(level, plantPos);
            plantPos = null;
            return;
        }

        // 2. Active tree blocks felling
        if (!treeBlocksQueue.isEmpty()) {
            BlockPos nextPos = treeBlocksQueue.peek();
            if (nextPos == null || level.getBlockState(nextPos).isAir()) {
                treeBlocksQueue.poll();
                return;
            }

            equipAxe();
            maid.getLookControl().setLookAt(nextPos.getX() + 0.5, nextPos.getY() + 0.5, nextPos.getZ() + 0.5);

            double distSq = maid.distanceToSqr(nextPos.getX() + 0.5, nextPos.getY() + 0.5, nextPos.getZ() + 0.5);
            if (distSq > 16.0) {
                maid.getNavigation().moveTo(nextPos.getX(), nextPos.getY(), nextPos.getZ(), 0.7);
                return;
            }

            if (breakCooldown >= 15) {
                breakCooldown = 0;
                maid.swing(InteractionHand.MAIN_HAND);
                level.destroyBlock(nextPos, true, maid);
                treeBlocksQueue.poll();
            }
            return;
        }

        // 3. Scan for tree base within 8 blocks
        if (breakCooldown % 40 == 0) {
            BlockPos maidPos = maid.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(maidPos.offset(-8, -1, -8), maidPos.offset(8, 2, 8))) {
                BlockState state = level.getBlockState(pos);
                if (state.is(BlockTags.LOGS)) {
                    // Check if it's the base of a tree on dirt
                    BlockState below = level.getBlockState(pos.below());
                    if (below.is(BlockTags.DIRT)) {
                        targetLogPos = pos.immutable();
                        plantPos = targetLogPos;
                        collectTreeBlocks(level, targetLogPos);
                        break;
                    }
                }
            }
        }
    }

    private void collectTreeBlocks(ServerLevel level, BlockPos startLog) {
        treeBlocksQueue.clear();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> searchQueue = new ArrayDeque<>();

        searchQueue.add(startLog);
        visited.add(startLog);

        while (!searchQueue.isEmpty() && treeBlocksQueue.size() < 64) {
            BlockPos current = searchQueue.poll();
            treeBlocksQueue.add(current);

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    BlockState state = level.getBlockState(neighbor);
                    if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                        searchQueue.add(neighbor);
                    }
                }
            }
        }
    }

    private void tryReplantSapling(ServerLevel level, BlockPos basePos) {
        if (!level.getBlockState(basePos).isAir()) return;
        if (!level.getBlockState(basePos.below()).is(BlockTags.DIRT)) return;

        var inv = maid.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(ItemTags.SAPLINGS)) {
                Block saplingBlock = Block.byItem(stack.getItem());
                if (saplingBlock != Blocks.AIR) {
                    level.setBlock(basePos, saplingBlock.defaultBlockState(), 3);
                    stack.shrink(1);
                    maid.swing(InteractionHand.MAIN_HAND);
                    return;
                }
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && (!treeBlocksQueue.isEmpty() || plantPos != null);
    }
}
