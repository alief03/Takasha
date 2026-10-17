package net.takasha.rpg.skills;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.takasha.rpg.data.TakashaRpgManager;

public class AuraSkillsEngine {

    public static void initialize() {
        // 1. Block Break XP Listener (Farming, Foraging, Mining)
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || level.isClientSide()) {
                return;
            }

            Block block = state.getBlock();

            // Farming
            if (block instanceof CropBlock crop) {
                if (crop.isMaxAge(state)) {
                    TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.FARMING, 25.0);
                }
            } else if (block instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= 3) {
                TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.FARMING, 25.0);
            } else if (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN) || state.is(Blocks.SUGAR_CANE)) {
                TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.FARMING, 15.0);
            }
            // Foraging
            else if (state.is(BlockTags.LOGS)) {
                TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.FORAGING, 20.0);
            }
            // Mining
            else if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
                double xp = 2.0;
                if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE) || state.is(Blocks.ANCIENT_DEBRIS)) {
                    xp = 75.0;
                } else if (state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)) {
                    xp = 60.0;
                } else if (state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE) || state.is(Blocks.NETHER_GOLD_ORE)) {
                    xp = 25.0;
                } else if (state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE) || state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE)) {
                    xp = 12.0;
                } else if (state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE) || state.is(Blocks.NETHER_QUARTZ_ORE)) {
                    xp = 8.0;
                }
                TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.MINING, xp);
            }
        });

        // 2. Entity Death XP Listener (Combat & Archery)
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof Enemy && damageSource.getEntity() instanceof ServerPlayer player) {
                boolean isArrow = damageSource.getDirectEntity() instanceof Projectile;
                AuraSkillType targetSkill = isArrow ? AuraSkillType.ARCHERY : AuraSkillType.COMBAT;

                float maxHp = entity.getMaxHealth();
                double xp = Math.max(15.0, maxHp * 1.5);

                TakashaRpgManager.giveSkillXp(player, targetSkill, xp);
            }
        });

        // 3. Defense XP Listener (When player takes damage)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player && amount > 0.5f) {
                double xp = Math.min(50.0, amount * 2.0);
                TakashaRpgManager.giveSkillXp(player, AuraSkillType.DEFENSE, xp);
            }
            return true;
        });
    }
}
