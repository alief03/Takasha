package net.sakura.touhou.compat.tlmo.task;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.sakura.touhou.entity.EntityMaid;
import net.sakura.touhou.entity.task.MaidTaskMode;

import java.util.EnumSet;

/**
 * MaidMineTask
 * Tugas penambangan otomatis bijih mineral di sekitar maid menggunakan pickaxe.
 * Berdasarkan studio.fantasyit.maid_useful_task.task.MaidMineTask.
 */
public class MaidMineTask extends Goal {

    private final EntityMaid maid;
    private BlockPos targetPos = null;
    private int breakCooldown = 0;

    public MaidMineTask(EntityMaid maid) {
        this.maid = maid;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (maid.isSitting() || maid.getTaskMode() != MaidTaskMode.FARM) {
            return false;
        }

        ItemStack mainHand = maid.getMainHandItem();
        if (!mainHand.isCorrectToolForDrops(maid.level().getBlockState(maid.blockPosition()))) {
            // Bisa menggunakan pickaxe yang ada di inventaris
            return hasPickaxe(maid);
        }
        return true;
    }

    private boolean hasPickaxe(EntityMaid maid) {
        var inv = maid.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(net.minecraft.tags.ItemTags.PICKAXES)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        if (!(maid.level() instanceof ServerLevel level)) return;

        breakCooldown++;
        if (breakCooldown % 20 != 0) return;

        BlockPos maidPos = maid.blockPosition();
        if (targetPos == null || !isOre(level.getBlockState(targetPos))) {
            targetPos = findNearestOre(level, maidPos);
        }

        if (targetPos != null) {
            double distSq = maid.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
            if (distSq > 9.0) {
                maid.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.0);
            } else {
                // Hancurkan blok bijih
                maid.swing(InteractionHand.MAIN_HAND);
                level.destroyBlock(targetPos, true, maid);
                targetPos = null;
            }
        }
    }

    private BlockPos findNearestOre(ServerLevel level, BlockPos origin) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -2, -4), origin.offset(4, 2, 4))) {
            if (isOre(level.getBlockState(pos))) {
                return pos.immutable();
            }
        }
        return null;
    }

    private boolean isOre(BlockState state) {
        if (state.isAir()) return false;
        if (state.is(BlockTags.IRON_ORES) || state.is(BlockTags.GOLD_ORES) || state.is(BlockTags.COPPER_ORES)) {
            return true;
        }
        var block = state.getBlock();
        return block == net.minecraft.world.level.block.Blocks.DIAMOND_ORE ||
                block == net.minecraft.world.level.block.Blocks.DEEPSLATE_DIAMOND_ORE ||
                block == net.minecraft.world.level.block.Blocks.COAL_ORE ||
                block == net.minecraft.world.level.block.Blocks.DEEPSLATE_COAL_ORE ||
                block == net.minecraft.world.level.block.Blocks.REDSTONE_ORE ||
                block == net.minecraft.world.level.block.Blocks.DEEPSLATE_REDSTONE_ORE ||
                block == net.minecraft.world.level.block.Blocks.LAPIS_ORE ||
                block == net.minecraft.world.level.block.Blocks.DEEPSLATE_LAPIS_ORE ||
                block == net.minecraft.world.level.block.Blocks.EMERALD_ORE ||
                block == net.minecraft.world.level.block.Blocks.DEEPSLATE_EMERALD_ORE;
    }
}
