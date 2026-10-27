package net.takasha.rpg.dungeon.drop;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.takasha.rpg.dungeon.DungeonArenaEngine;
import net.takasha.rpg.registry.ModRpgItems;

import java.util.Random;

/**
 * DungeonDropEngine
 * Mengatur peluang drop material Dungeon Keystone dari monster hostile di dunia:
 * - Dungeon Core Fragment: 5.0% (Nether/End: 10.0%)
 * - Abyssal Binding Thread: 3.0% (Nether/End: 6.0%)
 * - Runic Essence: 1.5% (Nether/End: 3.0%)
 */
public class DungeonDropEngine {

    private static final Random RANDOM = new Random();

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.level().isClientSide()) return;
            if (!(entity instanceof Enemy)) return;
            if (entity instanceof Player) return;

            // Monster di dalam arena dikendalikan oleh DungeonArenaEngine (tidak menduplikasi drop material)
            if (DungeonArenaEngine.getInstance().isArenaActive() &&
                DungeonArenaEngine.getInstance().isArenaMob(entity.getUUID())) {
                return;
            }

            if (!(entity.level() instanceof ServerLevel level)) return;

            // Pengali dimensi untuk Nether / End
            boolean isNetherOrEnd = level.dimension() == Level.NETHER || level.dimension() == Level.END;
            double mult = isNetherOrEnd ? 2.0 : 1.0;

            double roll = RANDOM.nextDouble() * 100.0;

            // 1. Dungeon Core Fragment (Base 5%, Nether/End 10%)
            if (roll < (5.0 * mult)) {
                dropItem(level, entity.getX(), entity.getY() + 0.5, entity.getZ(),
                        new ItemStack(ModRpgItems.DUNGEON_CORE_FRAGMENT, 1));
            }

            // 2. Abyssal Binding Thread (Base 3%, Nether/End 6%)
            double threadRoll = RANDOM.nextDouble() * 100.0;
            if (threadRoll < (3.0 * mult)) {
                dropItem(level, entity.getX(), entity.getY() + 0.5, entity.getZ(),
                        new ItemStack(ModRpgItems.ABYSSAL_BINDING_THREAD, 1));
            }

            // 3. Runic Essence (Base 1.5%, Nether/End 3%)
            double essenceRoll = RANDOM.nextDouble() * 100.0;
            if (essenceRoll < (1.5 * mult)) {
                dropItem(level, entity.getX(), entity.getY() + 0.5, entity.getZ(),
                        new ItemStack(ModRpgItems.RUNIC_ESSENCE, 1));
            }
        });
    }

    private static void dropItem(ServerLevel level, double x, double y, double z, ItemStack stack) {
        ItemEntity itemEntity = new ItemEntity(level, x, y, z, stack);
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }
}
