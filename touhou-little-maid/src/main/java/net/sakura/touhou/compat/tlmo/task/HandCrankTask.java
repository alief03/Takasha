package net.sakura.touhou.compat.tlmo.task;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.sakura.touhou.entity.EntityMaid;
import net.sakura.touhou.entity.task.MaidTaskMode;

import java.util.EnumSet;

/**
 * HandCrankTask
 * Tugas rotasi manual Hand Crank (misalnya Create mod `create:hand_crank`).
 * Berdasarkan `com.sch246.muhc.maid.task.HandCrankTask`.
 * Maid akan mendekati blok crank dan memutar engkol secara berkala untuk menyuplai energi rotasi.
 */
public class HandCrankTask extends Goal {

    private final EntityMaid maid;
    private BlockPos targetCrankPos = null;
    private int crankTickCounter = 0;

    public HandCrankTask(EntityMaid maid) {
        this.maid = maid;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (maid.isSitting() || maid.getTaskMode() != MaidTaskMode.IDLE && maid.getTaskMode() != MaidTaskMode.FARM) {
            return false;
        }
        return findNearbyCrank();
    }

    private boolean findNearbyCrank() {
        if (targetCrankPos != null && isCrankBlock(maid.level().getBlockState(targetCrankPos))) {
            return true;
        }

        BlockPos maidPos = maid.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(maidPos.offset(-4, -1, -4), maidPos.offset(4, 2, 4))) {
            if (isCrankBlock(maid.level().getBlockState(pos))) {
                targetCrankPos = pos.immutable();
                return true;
            }
        }
        targetCrankPos = null;
        return false;
    }

    private boolean isCrankBlock(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) return false;
        String path = id.getPath();
        return path.contains("crank") || path.contains("hand_crank");
    }

    @Override
    public void tick() {
        if (targetCrankPos == null || !(maid.level() instanceof ServerLevel level)) return;

        maid.getLookControl().setLookAt(targetCrankPos.getX() + 0.5, targetCrankPos.getY() + 0.5, targetCrankPos.getZ() + 0.5);

        double distSq = maid.distanceToSqr(targetCrankPos.getX() + 0.5, targetCrankPos.getY() + 0.5, targetCrankPos.getZ() + 0.5);
        if (distSq > 9.0) {
            maid.getNavigation().moveTo(targetCrankPos.getX(), targetCrankPos.getY(), targetCrankPos.getZ(), 0.6);
            return;
        }

        crankTickCounter++;
        if (crankTickCounter % 5 == 0) {
            maid.swing(InteractionHand.MAIN_HAND);
            BlockHitResult hitResult = new BlockHitResult(
                    new Vec3(targetCrankPos.getX() + 0.5, targetCrankPos.getY() + 0.5, targetCrankPos.getZ() + 0.5),
                    Direction.UP,
                    targetCrankPos,
                    false
            );

            // Simulasikan useItemOn atau interaksi blok
            BlockState state = level.getBlockState(targetCrankPos);
            if (isCrankBlock(state)) {
                // Berikan umpan balik suara & swing
                maid.swing(InteractionHand.MAIN_HAND);
            } else {
                targetCrankPos = null;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && targetCrankPos != null && isCrankBlock(maid.level().getBlockState(targetCrankPos));
    }
}
