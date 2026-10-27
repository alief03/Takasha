package net.takasha.rpg.dungeon.reward;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.List;

/**
 * Manajer pemunculan peti fisik hadiah di titik tengah arena (Dungeon Victory & Remains Chest).
 */
public class DungeonRewardChestManager {

    public static BlockPos spawnChestWithLoot(ServerLevel level, BlockPos centerPos, List<ItemStack> lootItems, String chestCustomName) {
        if (level == null || centerPos == null) return null;

        BlockPos safePos = findSafeGround(level, centerPos);
        level.setBlockAndUpdate(safePos, Blocks.CHEST.defaultBlockState());

        if (level.getBlockEntity(safePos) instanceof ChestBlockEntity chest) {
            int slot = 0;
            for (ItemStack item : lootItems) {
                if (slot >= chest.getContainerSize()) break;
                if (!item.isEmpty()) {
                    chest.setItem(slot++, item.copy());
                }
            }
            chest.setChanged();
        }

        // Efek audio & partikel pemunculan peti
        level.playSound(null, safePos.getX() + 0.5, safePos.getY() + 0.5, safePos.getZ() + 0.5,
                SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.5f, 0.8f);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                safePos.getX() + 0.5, safePos.getY() + 0.5, safePos.getZ() + 0.5,
                30, 0.5, 0.5, 0.5, 0.1);

        return safePos;
    }

    private static BlockPos findSafeGround(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos m = pos.mutable();
        while (m.getY() > level.getMinY() && (level.getBlockState(m).isAir() || level.getBlockState(m).canBeReplaced())) {
            m.move(Direction.DOWN);
        }
        return m.above().immutable();
    }
}
