package net.takasha.rpg.skills;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.takasha.rpg.enchant.TakashaEnchantCombatEngine;
import net.takasha.rpg.entity.DamageDummyEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AuraSkillsEngine
 * Sistem perolehan pengalaman (XP) komprehensif untuk 11 cabang skill AuraSkills:
 * 1. FARMING: Memanen tanaman padi, kentang, wortel, melon, tebu, labu.
 * 2. FORAGING: Menebang semua varian kayu & Skill Aktif Treecapitator (Shift + RClick Axe).
 * 3. MINING: Menambang bijih mineral (coal, iron, gold, diamond, ancient debris, dll).
 * 4. FISHING: Menarik pancingan (reel-in) dan mendapatkan tangkapan air.
 * 5. EXCAVATION: Menggali tanah, pasir, kerikil, clay, mud, dan menemukan relic.
 * 6. ARCHERY: Menumbangkan monster musuh dengan panah / crossbow.
 * 7. DEFENSE: Menahan damage dari serangan musuh.
 * 8. FIGHTING: Menumbangkan monster musuh dengan senjata jarak dekat (melee).
 * 9. AGILITY: Berlari (sprinting), berenang (swimming), terbang elytra (gliding), jatuh selamat.
 * 10. ALCHEMY: Menggunakan brewing stand, meminum potion, dan memakan golden apple.
 * 11. ENCHANTING: Menggunakan enchanting table, anvil, grindstone, dan experience bottle.
 */
public class AuraSkillsEngine {

    private static final Map<UUID, Long> LAST_INTERACTION_XP = new ConcurrentHashMap<>();

    public static void initialize() {
        // 1. Block Break XP & Mana Ability Listener (Farming, Foraging, Mining)
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || level.isClientSide()) {
                return;
            }

            Block block = state.getBlock();
            TakashaRpgData data = TakashaRpgManager.getOrCreateData(serverPlayer);

            // FARMING & REPLENISH
            if (block instanceof CropBlock crop) {
                if (crop.isMaxAge(state)) {
                    TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.FARMING, 25.0 * net.takasha.rpg.world.event.TakashaEventManager.getExpMultiplier());
                    net.takasha.rpg.quest.TakashaQuestManager.recordProgress(serverPlayer, "cropsHarvested", 1);

                    // Event Harvest Festival Bonus Drop
                    if (net.takasha.rpg.world.event.TakashaEventManager.getHarvestMultiplier() > 1.0) {
                        Block.dropResources(state, (ServerLevel) level, pos);
                        if (serverPlayer.getRandom().nextDouble() < 0.25) {
                            Block.popResource((ServerLevel) level, pos, new ItemStack(Items.GOLD_NUGGET, 1));
                        }
                    }

                    // Passive: Bountiful Harvest (Slot 1)
                    int harvestLvl = data.getPassiveAbilityLevel(AuraSkillType.FARMING, 1);
                    if (harvestLvl > 0 && data.isAbilityEnabled("bountiful_harvest")) {
                        double chance = 10.0 + (harvestLvl - 1) * 10.0;
                        if (serverPlayer.getRandom().nextDouble() * 100.0 < chance) {
                            Block.dropResources(state, (ServerLevel) level, pos);
                            serverPlayer.sendSystemMessage(Component.literal("§6✦ [AuraSkills] §eBountiful Harvest! §a(Hasil panen berganda)"));
                        }
                    }

                    // Active Ability: Replenish (Hoe)
                    if (data.isReplenishReady() || data.isReplenishActive()) {
                        long now = System.currentTimeMillis();
                        double replenishCost = data.getManaAbilityCost(AuraSkillType.FARMING);
                        long replenishCd = (long) (data.getManaAbilityCooldownSeconds(AuraSkillType.FARMING) * 1000L);
                        if (now >= data.getReplenishCooldownUntil() && data.consumeMana(replenishCost)) {
                            data.setReplenishReadyUntil(0);
                            data.setReplenishActive(false);
                            data.setReplenishCooldownUntil(now + replenishCd);
                            boolean replanted = tryReplenishCrop(serverPlayer, pos, block);
                            if (replanted) {
                                serverPlayer.sendSystemMessage(Component.literal("§6§l✦ [AuraSkills] §eReplenish AKTIF! §a(Tanaman otomatis ditanam ulang) §7[-" + (int) replenishCost + " Mana]"));
                                serverPlayer.level().playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
                            }
                            TakashaRpgManager.syncToClient(serverPlayer);
                        } else if (data.isReplenishReady() && data.getCurrentMana() < replenishCost) {
                            serverPlayer.sendSystemMessage(Component.literal("§c§l[Replenish] §cMana tidak cukup! Membutuhkan §b" + (int) replenishCost + " Mana§c."));
                        }
                    }
                }
            } else if (block instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= 3) {
                TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.FARMING, 25.0 * net.takasha.rpg.world.event.TakashaEventManager.getExpMultiplier());
                net.takasha.rpg.quest.TakashaQuestManager.recordProgress(serverPlayer, "cropsHarvested", 1);
                if (data.isReplenishReady() || data.isReplenishActive()) {
                    long now = System.currentTimeMillis();
                    double replenishCost = data.getManaAbilityCost(AuraSkillType.FARMING);
                    long replenishCd = (long) (data.getManaAbilityCooldownSeconds(AuraSkillType.FARMING) * 1000L);
                    if (now >= data.getReplenishCooldownUntil() && data.consumeMana(replenishCost)) {
                        data.setReplenishReadyUntil(0);
                        data.setReplenishActive(false);
                        data.setReplenishCooldownUntil(now + replenishCd);
                        tryReplenishCrop(serverPlayer, pos, block);
                        serverPlayer.sendSystemMessage(Component.literal("§6§l✦ [AuraSkills] §eReplenish AKTIF! §a(Nether Wart otomatis ditanam ulang) §7[-" + (int) replenishCost + " Mana]"));
                        TakashaRpgManager.syncToClient(serverPlayer);
                    }
                }
            } else if (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN) || state.is(Blocks.SUGAR_CANE)) {
                TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.FARMING, 15.0 * net.takasha.rpg.world.event.TakashaEventManager.getExpMultiplier());
                net.takasha.rpg.quest.TakashaQuestManager.recordProgress(serverPlayer, "cropsHarvested", 1);
            }
            // FORAGING & TREECAPITATOR
            else if (state.is(BlockTags.LOGS)) {
                // Passive: Lumberjack (Slot 1)
                int lumberjackLvl = data.getPassiveAbilityLevel(AuraSkillType.FORAGING, 1);
                if (lumberjackLvl > 0 && data.isAbilityEnabled("lumberjack")) {
                    double chance = 10.0 + (lumberjackLvl - 1) * 10.0;
                    if (serverPlayer.getRandom().nextDouble() * 100.0 < chance) {
                        Block.dropResources(state, (ServerLevel) level, pos);
                    }
                }

                boolean axeHeld = serverPlayer.getMainHandItem().getItem() instanceof AxeItem || serverPlayer.getMainHandItem().is(ItemTags.AXES);
                if (axeHeld && (data.isTreecapitatorReady() || data.isTreecapitatorActive())) {
                    long now = System.currentTimeMillis();
                    double treeCost = data.getManaAbilityCost(AuraSkillType.FORAGING);
                    long treeCd = (long) (data.getManaAbilityCooldownSeconds(AuraSkillType.FORAGING) * 1000L);
                    if (now >= data.getTreecapitatorCooldownUntil() && data.consumeMana(treeCost)) {
                        data.setTreecapitatorReadyUntil(0);
                        data.setTreecapitatorActive(false);
                        data.setTreecapitatorCooldownUntil(now + treeCd);
                        int chopped = chopTree((ServerLevel) serverPlayer.level(), serverPlayer, pos);
                        TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.FORAGING, (20.0 + (chopped * 15.0)) * net.takasha.rpg.world.event.TakashaEventManager.getExpMultiplier());
                        serverPlayer.sendSystemMessage(Component.literal("§6§l✦ [AuraSkills] §eTreecapitator AKTIF! §a(Total: §e" + (chopped + 1) + " §akayu ditumbangkan) §7[-" + (int) treeCost + " Mana]"));
                        serverPlayer.level().playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.2f, 0.8f);
                        TakashaRpgManager.syncToClient(serverPlayer);
                    } else if (data.isTreecapitatorReady() && data.getCurrentMana() < treeCost) {
                        serverPlayer.sendSystemMessage(Component.literal("§c§l[Treecapitator] §cMana tidak cukup! Membutuhkan §b" + (int) treeCost + " Mana§c."));
                    }
                } else {
                    TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.FORAGING, 20.0 * net.takasha.rpg.world.event.TakashaEventManager.getExpMultiplier());
                }
            }
            // MINING & SPEED MINE
            else if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
                double xp = 2.0;
                boolean isOre = false;
                if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE) || state.is(Blocks.ANCIENT_DEBRIS)) {
                    xp = 75.0;
                    isOre = true;
                } else if (state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)) {
                    xp = 60.0;
                    isOre = true;
                } else if (state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE) || state.is(Blocks.NETHER_GOLD_ORE)) {
                    xp = 25.0;
                    isOre = true;
                } else if (state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE) || state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE)) {
                    xp = 12.0;
                    isOre = true;
                } else if (state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE) || state.is(Blocks.NETHER_QUARTZ_ORE)) {
                    xp = 8.0;
                    isOre = true;
                }
                if (isOre && pos.getY() < 0) {
                    net.takasha.rpg.quest.TakashaQuestManager.recordProgress(serverPlayer, "oresMinedDeep", 1);
                }
                TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.MINING, xp * net.takasha.rpg.world.event.TakashaEventManager.getExpMultiplier());

                // Passive: Lucky Miner (Slot 1)
                int luckyMinerLvl = data.getPassiveAbilityLevel(AuraSkillType.MINING, 1);
                if (luckyMinerLvl > 0 && data.isAbilityEnabled("lucky_miner") && (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE) || state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE) || state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE) || state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE) || state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE) || state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE))) {
                    double chance = 5.0 + (luckyMinerLvl - 1) * 5.0;
                    if (serverPlayer.getRandom().nextDouble() * 100.0 < chance) {
                        Block.dropResources(state, (ServerLevel) level, pos);
                        serverPlayer.sendSystemMessage(Component.literal("§6✦ [AuraSkills] §eLucky Miner! §a(Bijih tambang berganda)"));
                    }
                }

                // Active Ability: Speed Mine (Pickaxe)
                if ((data.isSpeedMineReady() || data.isSpeedMineActive()) && data.isAbilityEnabled("speed_mine")) {
                    long now = System.currentTimeMillis();
                    double speedCost = data.getManaAbilityCost(AuraSkillType.MINING);
                    long speedCd = (long) (data.getManaAbilityCooldownSeconds(AuraSkillType.MINING) * 1000L);
                    int speedMineLvl = data.getManaAbilityLevel(AuraSkillType.MINING);
                    int hasteDurationTicks = (10 + (speedMineLvl - 1) * 5) * 20;
                    if (now >= data.getSpeedMineCooldownUntil() && data.consumeMana(speedCost)) {
                        data.setSpeedMineReadyUntil(0);
                        data.setSpeedMineActive(false);
                        data.setSpeedMineCooldownUntil(now + speedCd);
                        serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.HASTE, hasteDurationTicks, 9, false, true, true));
                        serverPlayer.sendSystemMessage(Component.literal("§6§l✦ [AuraSkills] §eSpeed Mine AKTIF! §a(+Haste X selama " + (hasteDurationTicks / 20) + " detik) §7[-" + (int) speedCost + " Mana]"));
                        serverPlayer.level().playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.5f);
                        TakashaRpgManager.syncToClient(serverPlayer);
                    } else if (data.isSpeedMineReady() && data.getCurrentMana() < speedCost) {
                        serverPlayer.sendSystemMessage(Component.literal("§c§l[Speed Mine] §cMana tidak cukup! Membutuhkan §b" + (int) speedCost + " Mana§c."));
                    }
                }
            }
            // EXCAVATION & TERRAFORM
            else if (state.is(BlockTags.MINEABLE_WITH_SHOVEL) || state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)
                    || state.is(Blocks.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY)
                    || state.is(Blocks.MUD) || state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL)) {
                double xp = 4.0;
                if (state.is(Blocks.CLAY) || state.is(Blocks.GRAVEL)) {
                    xp = 12.0;
                }
                TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.EXCAVATION, xp);

                // Passive: Bigger Scoop (Slot 1)
                int scoopLvl = data.getPassiveAbilityLevel(AuraSkillType.EXCAVATION, 1);
                if (scoopLvl > 0 && data.isAbilityEnabled("bigger_scoop")) {
                    double chance = 10.0 + (scoopLvl - 1) * 10.0;
                    if (serverPlayer.getRandom().nextDouble() * 100.0 < chance) {
                        Block.dropResources(state, (ServerLevel) level, pos);
                    }
                }
                // Passive: Metal Detector (Slot 4)
                int metalDetectorLvl = data.getPassiveAbilityLevel(AuraSkillType.EXCAVATION, 4);
                if (metalDetectorLvl > 0 && data.isAbilityEnabled("metal_detector")) {
                    double chance = 0.3 + (metalDetectorLvl - 1) * 0.3;
                    if (serverPlayer.getRandom().nextDouble() * 100.0 < chance) {
                        ItemStack nugget = serverPlayer.getRandom().nextBoolean() ? new ItemStack(Items.IRON_NUGGET) : new ItemStack(Items.GOLD_NUGGET);
                        Block.popResource((ServerLevel) level, pos, nugget);
                        serverPlayer.sendSystemMessage(Component.literal("§6✦ [AuraSkills] §eMetal Detector! §aMenemukan " + nugget.getHoverName().getString() + " saat menggali!"));
                    }
                }

                // Active Ability: Terraform (Shovel)
                boolean shovelHeld = serverPlayer.getMainHandItem().is(ItemTags.SHOVELS);
                if (shovelHeld && (data.isTerraformReady() || data.isTerraformActive()) && data.isAbilityEnabled("terraform")) {
                    long now = System.currentTimeMillis();
                    double terraCost = data.getManaAbilityCost(AuraSkillType.EXCAVATION);
                    long terraCd = (long) (data.getManaAbilityCooldownSeconds(AuraSkillType.EXCAVATION) * 1000L);
                    if (now >= data.getTerraformCooldownUntil() && data.consumeMana(terraCost)) {
                        data.setTerraformReadyUntil(0);
                        data.setTerraformActive(false);
                        data.setTerraformCooldownUntil(now + terraCd);
                        int dug = terraformBlocks((ServerLevel) serverPlayer.level(), serverPlayer, pos);
                        TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.EXCAVATION, 15.0 + (dug * 4.0));
                        serverPlayer.sendSystemMessage(Component.literal("§6§l✦ [AuraSkills] §eTerraform AKTIF! §a(Total: §e" + (dug + 1) + " §ablok digali cepat) §7[-" + (int) terraCost + " Mana]"));
                        serverPlayer.level().playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1.2f, 0.8f);
                        TakashaRpgManager.syncToClient(serverPlayer);
                    } else if (data.isTerraformReady() && data.getCurrentMana() < terraCost) {
                        serverPlayer.sendSystemMessage(Component.literal("§c§l[Terraform] §cMana tidak cukup! Membutuhkan §b" + (int) terraCost + " Mana§c."));
                    }
                }
            }
        });

        // 2. Entity Death XP Listener (Fighting/Combat & Archery with LevelledMobs scaling)
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof DamageDummyEntity || entity.getType().toShortString().toLowerCase().contains("dummy")
                    || (entity.getCustomName() != null && entity.getCustomName().getString().toLowerCase().contains("dummy"))) {
                return;
            }
            if (entity instanceof Enemy && damageSource.getEntity() instanceof ServerPlayer player) {
                boolean isArrow = damageSource.getDirectEntity() instanceof Projectile;
                AuraSkillType targetSkill = isArrow ? AuraSkillType.ARCHERY : AuraSkillType.FIGHTING;

                // Integrasi LevelledMobs: Penskalaan XP = 1.0 + (mobLevel - 1) * 0.04
                net.takasha.rpg.world.TakashaMobLevelManager.MobLevelData mobData =
                        net.takasha.rpg.world.TakashaMobLevelManager.getMobData(entity.getUUID());
                int mobLvl = (mobData != null) ? mobData.level : 1;
                double levelMultiplier = 1.0 + (mobLvl - 1) * 0.04;

                float maxHp = entity.getMaxHealth();
                double baseExp = Math.max(15.0, maxHp * 1.5);
                double finalExp = baseExp * levelMultiplier * net.takasha.rpg.world.event.TakashaEventManager.getExpMultiplier();

                // Wisdom trait bonus
                TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
                double wisdomBonus = data.getTraitValue(AuraTraitType.EXPERIENCE_BONUS);
                if (wisdomBonus > 0) {
                    finalExp *= (1.0 + wisdomBonus / 100.0);
                }

                TakashaRpgManager.giveSkillXp(player, targetSkill, finalExp);

                // Auto-tracking RPG Quests & Stats
                net.takasha.rpg.quest.TakashaQuestManager.recordProgress(player, "mobsKilled", 1);
                boolean isNightOrDark = player.level().isDarkOutside() || (player.level().getOverworldClockTime() % 24000L >= 13000L);
                if (isNightOrDark) {
                    net.takasha.rpg.quest.TakashaQuestManager.recordProgress(player, "nocturnalMobsKilled", 1);
                }
                if (player.level().dimension() == net.minecraft.world.level.Level.NETHER) {
                    net.takasha.rpg.quest.TakashaQuestManager.recordProgress(player, "netherMobsKilled", 1);
                }
                if (entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss || entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || maxHp >= 100.0f) {
                    net.takasha.rpg.quest.TakashaQuestManager.recordProgress(player, "bossesKilled", 1);
                }
                String heldItemKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
                if (heldItemKey.contains("sakura") || heldItemKey.contains("takasha")) {
                    net.takasha.rpg.quest.TakashaQuestManager.recordProgress(player, "sakuraWeaponKills", 1);
                }

                // World Event Coin Multiplier Bonus Drop
                double coinMult = net.takasha.rpg.world.event.TakashaEventManager.getCoinMultiplier();
                if (coinMult > 1.0) {
                    double bonusCoin = (5.0 + player.getRandom().nextDouble() * 10.0) * (coinMult - 1.0);
                    data.addBalance(bonusCoin);
                    player.sendSystemMessage(Component.literal("§6✦ [World Event] §eBonus Koin Event: +$" + String.format("%.1f", bonusCoin)));
                }

                // Double Drop Chance Trait
                double doubleDropChance = data.getTraitValue(AuraTraitType.DOUBLE_DROP);
                if (doubleDropChance > 0 && player.getRandom().nextDouble() * 100.0 < doubleDropChance) {
                    player.sendSystemMessage(Component.literal("§6✦ [AuraSkills Perk] §eDouble Drop terpicu! (Loot digandakan)"));
                }
            }
        });

        // 3. Defense & Absorption & Agility Fall Damage & Combat Abilities Listener
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            // Anti-recursion & secondary damage guard (enchant procs, magic ticks, thorns)
            if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.THORNS) || TakashaEnchantCombatEngine.isProcessing()) {
                return true;
            }

            // Player menyerang target (Crit Chance, Crit Damage, Strength bow bonus, Lightning Blade, Charged Shot, Sword Master, Axe Master, First Strike, Bleed, Stun, Retrieval)
            if (source.getEntity() instanceof ServerPlayer attacker && entity instanceof net.minecraft.world.entity.LivingEntity target && !(entity instanceof ServerPlayer)) {
                TakashaRpgData atkData = TakashaRpgManager.getOrCreateData(attacker);
                long now = System.currentTimeMillis();
                boolean isArrow = source.getDirectEntity() instanceof Projectile;

                // 3.1 AURA SKILLS CRITICAL STRIKE (Archery Crit Chance & Fighting Crit Damage)
                double critChance = atkData.getTraitValue(AuraTraitType.CRIT_CHANCE);
                double critDamage = atkData.getTraitValue(AuraTraitType.CRIT_DAMAGE);
                boolean isJumpCrit = !isArrow && attacker.fallDistance > 0.0F && !attacker.onGround() && !attacker.onClimbable() && !attacker.isInWater() && !attacker.hasEffect(MobEffects.BLINDNESS);
                boolean isChanceCrit = attacker.getRandom().nextDouble() * 100.0 < critChance;
                boolean isCrit = isJumpCrit || isChanceCrit;

                float extraCritDmg = 0.0f;
                if (isCrit) {
                    if (isJumpCrit) {
                        extraCritDmg = (float) (amount * (critDamage / 150.0));
                    } else {
                        extraCritDmg = (float) (amount * (0.5 + (critDamage / 100.0)));
                    }
                    ServerLevel level = (ServerLevel) attacker.level();
                    level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 12, 0.3, 0.3, 0.3, 0.15);
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.1f);
                }

                // 3.2 AURA SKILLS STRENGTH (Ranged Bow/Crossbow Damage Scaling; Melee is scaled via Attributes.ATTACK_DAMAGE modifier)
                float arrowStrengthBonus = 0.0f;
                if (isArrow) {
                    double strengthTrait = atkData.getTraitValue(AuraTraitType.ATTACK_DAMAGE);
                    arrowStrengthBonus = (float) (amount * (strengthTrait / 100.0));
                }

                float totalExtraDmg = extraCritDmg + arrowStrengthBonus;
                if (totalExtraDmg > 0.2f) {
                    TakashaEnchantCombatEngine.setProcessing(true);
                    try {
                        target.hurtServer((ServerLevel) attacker.level(), attacker.damageSources().magic(), totalExtraDmg);
                    } finally {
                        TakashaEnchantCombatEngine.setProcessing(false);
                    }
                }

                // FIGHTING: Sword Master (Slot 3)
                if (!isArrow && attacker.getMainHandItem().is(ItemTags.SWORDS)) {
                    int swordMasterLvl = atkData.getPassiveAbilityLevel(AuraSkillType.FIGHTING, 3);
                    if (swordMasterLvl > 0 && atkData.isAbilityEnabled("sword_master")) {
                        float bonusDmg = (float) (amount * (0.03 + (swordMasterLvl - 1) * 0.02));
                        if (bonusDmg > 0.5f) {
                            TakashaEnchantCombatEngine.setProcessing(true);
                            try {
                                target.hurtServer((ServerLevel) attacker.level(), attacker.damageSources().magic(), bonusDmg);
                            } finally {
                                TakashaEnchantCombatEngine.setProcessing(false);
                            }
                        }
                    }
                }

                // FORAGING: Axe Master (Slot 3)
                if (!isArrow && (attacker.getMainHandItem().getItem() instanceof AxeItem || attacker.getMainHandItem().is(ItemTags.AXES))) {
                    int axeMasterLvl = atkData.getPassiveAbilityLevel(AuraSkillType.FORAGING, 3);
                    if (axeMasterLvl > 0 && atkData.isAbilityEnabled("axe_master")) {
                        float bonusDmg = (float) (amount * (0.04 + (axeMasterLvl - 1) * 0.03));
                        if (bonusDmg > 0.5f) {
                            TakashaEnchantCombatEngine.setProcessing(true);
                            try {
                                target.hurtServer((ServerLevel) attacker.level(), attacker.damageSources().magic(), bonusDmg);
                            } finally {
                                TakashaEnchantCombatEngine.setProcessing(false);
                            }
                        }
                    }
                }

                // FIGHTING: First Strike (Slot 4 - target full HP)
                if (!isArrow && target.getHealth() >= target.getMaxHealth() - 0.5f) {
                    int firstStrikeLvl = atkData.getPassiveAbilityLevel(AuraSkillType.FIGHTING, 4);
                    if (firstStrikeLvl > 0 && atkData.isAbilityEnabled("first_strike")) {
                        float bonusDmg = (float) (amount * (0.15 + (firstStrikeLvl - 1) * 0.05));
                        TakashaEnchantCombatEngine.setProcessing(true);
                        try {
                            target.hurtServer((ServerLevel) attacker.level(), attacker.damageSources().magic(), bonusDmg);
                        } finally {
                            TakashaEnchantCombatEngine.setProcessing(false);
                        }
                        attacker.sendSystemMessage(Component.literal("§6✦ [AuraSkills] §eFirst Strike! §a(+" + String.format(Locale.ROOT, "%.1f", bonusDmg) + " bonus damage ke musuh full HP)"));
                    }
                }

                // FIGHTING: Bleed (Slot 5)
                if (!isArrow && attacker.getMainHandItem().is(ItemTags.SWORDS)) {
                    int bleedLvl = atkData.getPassiveAbilityLevel(AuraSkillType.FIGHTING, 5);
                    if (bleedLvl > 0 && atkData.isAbilityEnabled("bleed")) {
                        double chance = 3.0 + (bleedLvl - 1) * 3.0;
                        if (attacker.getRandom().nextDouble() * 100.0 < chance) {
                            int durationTicks = Math.min(220, (3 + (bleedLvl - 1) * 2) * 20);
                            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WITHER, durationTicks, 1, false, true, true));
                            if (target instanceof DamageDummyEntity dummy) {
                                dummy.applyAeStatus("bleed", durationTicks, 1);
                            }
                            attacker.sendSystemMessage(Component.literal("§6✦ [AuraSkills] §eBleed! §c(Musuh mengalami pendarahan hebat)"));
                        }
                    }
                }

                // ARCHERY: Stun (Slot 5)
                if (isArrow) {
                    int stunLvl = atkData.getPassiveAbilityLevel(AuraSkillType.ARCHERY, 5);
                    if (stunLvl > 0 && atkData.isAbilityEnabled("stun")) {
                        double chance = 2.0 + (stunLvl - 1) * 1.0;
                        if (attacker.getRandom().nextDouble() * 100.0 < chance) {
                            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.SLOWNESS, 80, 2, false, true, true));
                            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WEAKNESS, 80, 1, false, true, true));
                            if (target instanceof DamageDummyEntity dummy) {
                                dummy.applyAeStatus("freeze", 80, 1);
                            }
                            attacker.sendSystemMessage(Component.literal("§6✦ [AuraSkills] §eStun! §a(Musuh terkena efek lambat dan lemas)"));
                        }
                    }
                }

                // ARCHERY: Retrieval (Slot 1)
                if (isArrow) {
                    int retrievalLvl = atkData.getPassiveAbilityLevel(AuraSkillType.ARCHERY, 1);
                    if (retrievalLvl > 0 && atkData.isAbilityEnabled("retrieval")) {
                        double chance = 5.0 + (retrievalLvl - 1) * 5.0;
                        if (attacker.getRandom().nextDouble() * 100.0 < chance) {
                            attacker.getInventory().add(new ItemStack(Items.ARROW));
                            attacker.sendSystemMessage(Component.literal("§6✦ [AuraSkills] §eRetrieval! §a(Anak panah kembali ke tas)"));
                        }
                    }
                }

                // Lightning Blade (Swords) - Dynamic mana & cooldown
                if (attacker.getMainHandItem().is(ItemTags.SWORDS) && (atkData.isLightningBladeReady() || atkData.isLightningBladeActive()) && atkData.isAbilityEnabled("lightning_blade")) {
                    double lbCost = atkData.getManaAbilityCost(AuraSkillType.FIGHTING);
                    long lbCd = (long) (atkData.getManaAbilityCooldownSeconds(AuraSkillType.FIGHTING) * 1000L);
                    if (now >= atkData.getLightningBladeCooldownUntil() && atkData.consumeMana(lbCost)) {
                        atkData.setLightningBladeReadyUntil(0);
                        atkData.setLightningBladeActive(false);
                        atkData.setLightningBladeCooldownUntil(now + lbCd);

                        ServerLevel serverLevel = (ServerLevel) attacker.level();
                        TakashaEnchantCombatEngine.setProcessing(true);
                        try {
                            target.hurtServer(serverLevel, attacker.damageSources().magic(), 12.0f);

                            // Rantai serangan kilat ke musuh sekitar (5 blok)
                            var nearby = serverLevel.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, target.getBoundingBox().inflate(5.0),
                                    e -> e instanceof Enemy && e != target && e.isAlive());
                            for (var nearbyEnemy : nearby) {
                                nearbyEnemy.hurtServer(serverLevel, attacker.damageSources().magic(), 8.0f);
                            }
                        } finally {
                            TakashaEnchantCombatEngine.setProcessing(false);
                        }

                        attacker.sendSystemMessage(Component.literal("§6§l✦ [AuraSkills] §eLightning Blade AKTIF! §a(Petir menyambar target & musuh sekitar!) §7[-" + (int) lbCost + " Mana]"));
                        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.5f);
                        TakashaRpgManager.syncToClient(attacker);
                    } else if (atkData.isLightningBladeReady() && atkData.getCurrentMana() < lbCost) {
                        attacker.sendSystemMessage(Component.literal("§c§l[Lightning Blade] §cMana tidak cukup! Membutuhkan §b" + (int) lbCost + " Mana§c."));
                    }
                }

                // Charged Shot (Bows) - Dynamic mana & cooldown
                if (isArrow && (atkData.isChargedShotReady() || atkData.isChargedShotActive()) && atkData.isAbilityEnabled("charged_shot")) {
                    double csCost = atkData.getManaAbilityCost(AuraSkillType.ARCHERY);
                    long csCd = (long) (atkData.getManaAbilityCooldownSeconds(AuraSkillType.ARCHERY) * 1000L);
                    if (now >= atkData.getChargedShotCooldownUntil() && atkData.consumeMana(csCost)) {
                        atkData.setChargedShotReadyUntil(0);
                        atkData.setChargedShotActive(false);
                        atkData.setChargedShotCooldownUntil(now + csCd);

                        ServerLevel serverLevel = (ServerLevel) attacker.level();
                        TakashaEnchantCombatEngine.setProcessing(true);
                        try {
                            target.hurtServer(serverLevel, attacker.damageSources().magic(), 15.0f);
                        } finally {
                            TakashaEnchantCombatEngine.setProcessing(false);
                        }
                        attacker.sendSystemMessage(Component.literal("§6§l✦ [AuraSkills] §eCharged Shot AKTIF! §a(+15 Damage Piercing Shockwave) §7[-" + (int) csCost + " Mana]"));
                        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.6f);
                        TakashaRpgManager.syncToClient(attacker);
                    } else if (atkData.isChargedShotReady() && atkData.getCurrentMana() < csCost) {
                        attacker.sendSystemMessage(Component.literal("§c§l[Charged Shot] §cMana tidak cukup! Membutuhkan §b" + (int) csCost + " Mana§c."));
                    }
                }
            }

            // Player menerima damage (Defense, Parry, Absorption, Fleeting, Fall, Toughness Reduction)
            if (entity instanceof ServerPlayer player && amount > 0.5f) {
                TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
                long now = System.currentTimeMillis();

                // TOUGHNESS: Damage Reduction asymptotic mitigation
                double dmgReduction = data.getTraitValue(AuraTraitType.DAMAGE_REDUCTION);
                if (dmgReduction > 0.0 && !source.is(DamageTypes.FELL_OUT_OF_WORLD) && !source.is(DamageTypes.GENERIC_KILL) && player.level().getServer() != null) {
                    float mitigated = (float) (amount * (dmgReduction / 100.0));
                    if (mitigated > 0.2f) {
                        player.level().getServer().execute(() -> {
                            if (player.isAlive()) {
                                player.heal(mitigated);
                            }
                        });
                    }
                }

                if (source.is(DamageTypes.FALL)) {
                    double fallXp = Math.min(60.0, amount * 3.0);
                    TakashaRpgManager.giveSkillXp(player, AuraSkillType.AGILITY, fallXp);
                } else {
                    // DEFENSE: Parry (Slot 1)
                    if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity && !(source.getDirectEntity() instanceof Projectile)) {
                        int parryLvl = data.getPassiveAbilityLevel(AuraSkillType.FIGHTING, 1);
                        if (parryLvl > 0 && data.isAbilityEnabled("parry")) {
                            double parryChance = 5.0 + (parryLvl - 1) * 2.0;
                            if (player.getRandom().nextDouble() * 100.0 < parryChance) {
                                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.5f);
                                player.sendSystemMessage(Component.literal("§6✦ [AuraSkills] §eParry! §a(Serangan melee musuh berhasil ditangkis!)"));
                                return false; // Cancel incoming melee damage!
                            }
                        }
                    }

                    // AGILITY: Fleeting (Slot 4 - Speed boost when taking damage)
                    int fleetingLvl = data.getPassiveAbilityLevel(AuraSkillType.AGILITY, 4);
                    if (fleetingLvl > 0 && data.isAbilityEnabled("fleeting")) {
                        double fleetingChance = 5.0 + (fleetingLvl - 1) * 4.0;
                        if (player.getHealth() <= player.getMaxHealth() * 0.35f || player.getRandom().nextDouble() * 100.0 < fleetingChance) {
                            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.SPEED, 100, 1, false, true, true));
                            player.sendSystemMessage(Component.literal("§6✦ [AuraSkills] §eFleeting! §a(+Speed II darurat!)"));
                        }
                    }

                    int atkLvl = 1;
                    if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attackerMob) {
                        var mobData = net.takasha.rpg.world.TakashaMobLevelManager.getMobData(attackerMob.getUUID());
                        if (mobData != null) atkLvl = mobData.level;
                    }
                    double defXp = Math.min(150.0, amount * 2.0 * (1.0 + (atkLvl - 1) * 0.02));
                    TakashaRpgManager.giveSkillXp(player, AuraSkillType.DEFENSE, defXp);

                    // Active Ability: Absorption (Shield block) - Dynamic mana & cooldown
                    if (player.isBlocking() && data.isAbilityEnabled("absorption")) {
                        double absCost = data.getManaAbilityCost(AuraSkillType.DEFENSE);
                        long absCd = (long) (data.getManaAbilityCooldownSeconds(AuraSkillType.DEFENSE) * 1000L);
                        if ((data.isAbsorptionReady() || player.isCrouching()) && now >= data.getAbsorptionCooldownUntil()) {
                            if (data.consumeMana(absCost)) {
                                data.setAbsorptionReadyUntil(0);
                                data.setAbsorptionCooldownUntil(now + absCd);
                                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.ABSORPTION, 300, 1, false, true, true));
                                player.sendSystemMessage(Component.literal("§6§l✦ [AuraSkills] §eAbsorption AKTIF! §a(+4 Golden Hearts selama 15s) §7[-" + (int) absCost + " Mana]"));
                                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.2f);
                                TakashaRpgManager.syncToClient(player);
                            } else if (data.isAbsorptionReady() && data.getCurrentMana() < absCost) {
                                player.sendSystemMessage(Component.literal("§c§l[Absorption] §cMana tidak cukup! Membutuhkan §b" + (int) absCost + " Mana§c."));
                            }
                        }
                    }
                }
            }
            return true;
        });

        // 4. Use Item Listener (Tool readiness, Sharp Hook, Fishing, Alchemy, Healing, Enchanting)
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || level.isClientSide()) {
                return InteractionResult.PASS;
            }

            ItemStack stack = player.getItemInHand(hand);
            // REA Parity: Custom MMO RPG weapons handle their own abilities (e.g. Volcanic Spear, Magma Dash)
            if (net.takasha.rpg.mmo.MMOItemData.hasMMOData(stack)) {
                return InteractionResult.PASS;
            }
            long now = System.currentTimeMillis();
            TakashaRpgData data = TakashaRpgManager.getOrCreateData(serverPlayer);

            // Active Ability Tool Readiness: Shift + Right Click or Air Right Click
            if (player.isCrouching()) {
                if (stack.is(ItemTags.AXES)) {
                    tryReadyTool(serverPlayer, AuraSkillType.FORAGING, "Treecapitator", data.getTreecapitatorCooldownUntil(), () -> data.setTreecapitatorReadyUntil(now + 5000L));
                    return InteractionResult.SUCCESS;
                } else if (stack.is(ItemTags.PICKAXES)) {
                    tryReadyTool(serverPlayer, AuraSkillType.MINING, "Speed Mine", data.getSpeedMineCooldownUntil(), () -> data.setSpeedMineReadyUntil(now + 5000L));
                    return InteractionResult.SUCCESS;
                } else if (stack.is(ItemTags.SHOVELS)) {
                    tryReadyTool(serverPlayer, AuraSkillType.EXCAVATION, "Terraform", data.getTerraformCooldownUntil(), () -> data.setTerraformReadyUntil(now + 5000L));
                    return InteractionResult.SUCCESS;
                } else if (stack.is(ItemTags.SWORDS)) {
                    tryReadyTool(serverPlayer, AuraSkillType.FIGHTING, "Lightning Blade", data.getLightningBladeCooldownUntil(), () -> data.setLightningBladeReadyUntil(now + 5000L));
                    return InteractionResult.SUCCESS;
                } else if (stack.is(ItemTags.HOES)) {
                    tryReadyTool(serverPlayer, AuraSkillType.FARMING, "Replenish", data.getReplenishCooldownUntil(), () -> data.setReplenishReadyUntil(now + 5000L));
                    return InteractionResult.SUCCESS;
                } else if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW)) {
                    tryReadyTool(serverPlayer, AuraSkillType.ARCHERY, "Charged Shot", data.getChargedShotCooldownUntil(), () -> data.setChargedShotReadyUntil(now + 5000L));
                    return InteractionResult.SUCCESS;
                } else if (stack.is(Items.SHIELD)) {
                    tryReadyTool(serverPlayer, AuraSkillType.DEFENSE, "Absorption", data.getAbsorptionCooldownUntil(), () -> data.setAbsorptionReadyUntil(now + 5000L));
                    return InteractionResult.SUCCESS;
                }
            }

            // Sharp Hook (Fishing Rod reel-in on hooked entity)
            if (stack.getItem() instanceof FishingRodItem && serverPlayer.fishing != null) {
                var hooked = serverPlayer.fishing.getHookedIn();
                if (hooked != null) {
                    if (data.isSharpHookReady() || player.isCrouching()) {
                        double hookCost = data.getManaAbilityCost(AuraSkillType.FISHING);
                        long hookCd = (long) (data.getManaAbilityCooldownSeconds(AuraSkillType.FISHING) * 1000L);
                        if (now >= data.getSharpHookCooldownUntil()) {
                            if (data.consumeMana(hookCost)) {
                                data.setSharpHookReadyUntil(0);
                                data.setSharpHookCooldownUntil(now + hookCd);
                                net.minecraft.world.phys.Vec3 vec = serverPlayer.position().subtract(hooked.position()).normalize().scale(1.4).add(0, 0.4, 0);
                                hooked.setDeltaMovement(vec);
                                hooked.hurtServer((ServerLevel) serverPlayer.level(), serverPlayer.damageSources().playerAttack(serverPlayer), 4.0f);
                                serverPlayer.sendSystemMessage(Component.literal("§6§l✦ [AuraSkills] §eSharp Hook AKTIF! §a(Target ditarik & menerima 4 damage) §7[-" + (int) hookCost + " Mana]"));
                                serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                        SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0f, 1.2f);
                                TakashaRpgManager.syncToClient(serverPlayer);
                            }
                        }
                    }
                }
            }

            // Air right-click readiness for Fishing Rod
            if (stack.getItem() instanceof FishingRodItem && player.isCrouching()) {
                tryReadyTool(serverPlayer, AuraSkillType.FISHING, "Sharp Hook", data.getSharpHookCooldownUntil(), () -> data.setSharpHookReadyUntil(now + 5000L));
            }

            long lastXp = LAST_INTERACTION_XP.getOrDefault(player.getUUID(), 0L);

            // Rate-limit pemicu use item minimal 500ms agar tidak spam tick
            if (now - lastXp > 500L) {
                // FISHING
                if (stack.getItem() instanceof FishingRodItem && serverPlayer.fishing != null) {
                    TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.FISHING, 35.0);
                    LAST_INTERACTION_XP.put(player.getUUID(), now);
                }
                // ALCHEMY (Potions & Golden Apples)
                else if (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
                    TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.ALCHEMY, 30.0);
                    LAST_INTERACTION_XP.put(player.getUUID(), now);
                }
                else if (stack.is(Items.GOLDEN_APPLE)) {
                    TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.ALCHEMY, 30.0);
                    LAST_INTERACTION_XP.put(player.getUUID(), now);
                } else if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
                    TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.ALCHEMY, 500.0);
                    LAST_INTERACTION_XP.put(player.getUUID(), now);
                }
                // ENCHANTING (Exp Bottles)
                else if (stack.is(Items.EXPERIENCE_BOTTLE)) {
                    TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.ENCHANTING, 20.0);
                    LAST_INTERACTION_XP.put(player.getUUID(), now);
                }
            }

            return InteractionResult.PASS;
        });

        // 5. Use Block Listener (Alchemy - Brewing Stand, Enchanting - Table, Anvil, Grindstone)
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || level.isClientSide()) {
                return InteractionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            Block block = level.getBlockState(pos).getBlock();
            long now = System.currentTimeMillis();
            long lastXp = LAST_INTERACTION_XP.getOrDefault(player.getUUID(), 0L);

            if (now - lastXp > 1000L) {
                if (block == Blocks.BREWING_STAND) {
                    TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.ALCHEMY, 15.0);
                    LAST_INTERACTION_XP.put(player.getUUID(), now);
                } else if (block == Blocks.ENCHANTING_TABLE) {
                    TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.ENCHANTING, 25.0);
                    LAST_INTERACTION_XP.put(player.getUUID(), now);
                } else if (block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL) {
                    TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.ENCHANTING, 15.0);
                    LAST_INTERACTION_XP.put(player.getUUID(), now);
                } else if (block == Blocks.GRINDSTONE) {
                    TakashaRpgManager.giveSkillXp(serverPlayer, AuraSkillType.ENCHANTING, 15.0);
                    LAST_INTERACTION_XP.put(player.getUUID(), now);
                }
            }

            return InteractionResult.PASS;
        });

        // 6. Server Tick Periodic Check (Agility)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 40 != 0) return; // Setiap 2 detik

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.isSpectator() || !player.isAlive()) continue;

                // AGILITY: Sprinting, Swimming, Gliding
                if (player.isSprinting()) {
                    TakashaRpgManager.giveSkillXp(player, AuraSkillType.AGILITY, 2.0);
                } else if (player.isSwimming()) {
                    TakashaRpgManager.giveSkillXp(player, AuraSkillType.AGILITY, 3.0);
                } else if (player.isFallFlying()) {
                    TakashaRpgManager.giveSkillXp(player, AuraSkillType.AGILITY, 4.0);
                }
            }
        });
    }

    private static void tryReadyTool(ServerPlayer player, AuraSkillType skill, String abilityName, long cooldownUntil, Runnable onReady) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        if (data.getSkillLevel(skill) < 6) {
            player.sendSystemMessage(Component.literal("§c§l[AuraSkills] §cKemampuan aktif " + abilityName + " terkunci! Membutuhkan " + skill.getDisplayName() + " Level 6."));
            return;
        }

        long now = System.currentTimeMillis();
        if (now < cooldownUntil) {
            long sec = Math.max(1, (cooldownUntil - now) / 1000L);
            player.sendSystemMessage(Component.literal("§c§l[" + abilityName + "] §cCooldown! Tunggu §e" + sec + " §cdetik lagi."));
            return;
        }
        onReady.run();
        player.sendSystemMessage(Component.literal("§e✦ §6[AuraSkills] §aYou readied your " + abilityName + "! §7(Lakukan aksi dalam 5 detik)"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.8f, 1.5f);
    }

    private static boolean tryReplenishCrop(ServerPlayer player, BlockPos pos, Block brokenBlock) {
        Item neededSeed = null;
        BlockState replantState = null;

        if (brokenBlock instanceof CropBlock) {
            if (brokenBlock == Blocks.WHEAT) {
                neededSeed = Items.WHEAT_SEEDS;
                replantState = Blocks.WHEAT.defaultBlockState();
            } else if (brokenBlock == Blocks.CARROTS) {
                neededSeed = Items.CARROT;
                replantState = Blocks.CARROTS.defaultBlockState();
            } else if (brokenBlock == Blocks.POTATOES) {
                neededSeed = Items.POTATO;
                replantState = Blocks.POTATOES.defaultBlockState();
            } else if (brokenBlock == Blocks.BEETROOTS) {
                neededSeed = Items.BEETROOT_SEEDS;
                replantState = Blocks.BEETROOTS.defaultBlockState();
            }
        } else if (brokenBlock == Blocks.NETHER_WART) {
            neededSeed = Items.NETHER_WART;
            replantState = Blocks.NETHER_WART.defaultBlockState();
        }

        if (neededSeed != null && replantState != null) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack invStack = player.getInventory().getItem(i);
                if (!invStack.isEmpty() && invStack.is(neededSeed)) {
                    invStack.shrink(1);
                    ((ServerLevel) player.level()).setBlockAndUpdate(pos, replantState);
                    return true;
                }
            }
        }
        return false;
    }

    private static int chopTree(ServerLevel level, ServerPlayer player, BlockPos startPos) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startPos);
        visited.add(startPos);

        int count = 0;
        int maxLogs = 48;

        while (!queue.isEmpty() && count < maxLogs) {
            BlockPos current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            BlockState nState = level.getBlockState(neighbor);
                            if (nState.is(BlockTags.LOGS)) {
                                queue.add(neighbor);
                                level.destroyBlock(neighbor, true, player);
                                count++;
                                if (count >= maxLogs) break;
                            }
                        }
                    }
                    if (count >= maxLogs) break;
                }
                if (count >= maxLogs) break;
            }
        }
        return count;
    }

    private static int terraformBlocks(ServerLevel level, ServerPlayer player, BlockPos startPos) {
        int count = 0;
        int maxBlocks = 26; // Area 3x3x2
        for (int dy = 0; dy >= -1; dy--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos target = startPos.offset(dx, dy, dz);
                    BlockState tState = level.getBlockState(target);
                    if (tState.is(BlockTags.MINEABLE_WITH_SHOVEL) || tState.is(Blocks.DIRT) || tState.is(Blocks.GRASS_BLOCK)
                            || tState.is(Blocks.SAND) || tState.is(Blocks.GRAVEL) || tState.is(Blocks.CLAY)
                            || tState.is(Blocks.MUD) || tState.is(Blocks.SOUL_SAND) || tState.is(Blocks.SOUL_SOIL)) {
                        level.destroyBlock(target, true, player);
                        count++;
                        if (count >= maxBlocks) break;
                    }
                }
                if (count >= maxBlocks) break;
            }
            if (count >= maxBlocks) break;
        }
        return count;
    }
}
