package net.takasha.rpg;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.takasha.rpg.command.TakashaRpgCommands;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;
import net.takasha.rpg.dungeon.DungeonArenaEngine;
import net.takasha.rpg.dungeon.TakashaArenaManager;
import net.takasha.rpg.dungeon.drop.DungeonDropEngine;
import net.takasha.rpg.economy.TakashaDailyShopManager;
import net.takasha.rpg.enchant.ArmorSetBonusHandler;
import net.takasha.rpg.network.RpgClientSyncPayload;
import net.takasha.rpg.registry.ModRpgItemGroups;
import net.takasha.rpg.registry.ModRpgItems;
import net.takasha.rpg.skills.AuraSkillsEngine;
import net.takasha.rpg.world.TakashaWorldLootModifier;
import net.takasha.rpg.world.TakashaMobLevelManager;
import net.takasha.rpg.title.TakashaTitleManager;
import net.takasha.rpg.quest.TakashaQuestManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TakashaRpgMod implements ModInitializer {

    public static final String MOD_ID = "takasha_rpg";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Takasha-RPG Mod for Minecraft 26.2...");

        // 1. Register Network Payloads
        PayloadTypeRegistry.clientboundPlay().register(RpgClientSyncPayload.TYPE, RpgClientSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(net.takasha.rpg.network.ToggleScoreboardC2SPayload.TYPE, net.takasha.rpg.network.ToggleScoreboardC2SPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(net.takasha.rpg.network.ClaimQuestC2SPayload.TYPE, net.takasha.rpg.network.ClaimQuestC2SPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(net.takasha.rpg.network.EquipTitleC2SPayload.TYPE, net.takasha.rpg.network.EquipTitleC2SPayload.STREAM_CODEC);

        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(net.takasha.rpg.network.ToggleScoreboardC2SPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
                data.setShowScoreboard(!data.isShowScoreboard());
                TakashaRpgManager.savePlayerData(player.getUUID());
                TakashaRpgManager.syncToClient(player);
            });
        });

        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(net.takasha.rpg.network.ClaimQuestC2SPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                TakashaQuestManager.claimReward(player, payload.questId());
            });
        });

        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(net.takasha.rpg.network.EquipTitleC2SPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                TakashaTitleManager.handleEquipTitleRequest(player, payload.titleCode());
            });
        });

        // 2. Register Items, Entities & Creative Tabs
        ModRpgItems.initialize();
        ModRpgItemGroups.initialize();
        net.takasha.rpg.registry.ModRpgEntities.initialize();

        // 3. Register Core Engines
        net.takasha.rpg.enchant.TakashaEnchantRegistry.initialize();
        net.takasha.rpg.enchant.TakashaEnchantUtilityEngine.initialize();
        AuraSkillsEngine.initialize();
        ArmorSetBonusHandler.initialize();
        TakashaArenaManager.initialize();
        DungeonArenaEngine.initialize();
        DungeonDropEngine.initialize();
        TakashaWorldLootModifier.initialize();
        net.takasha.rpg.trade.TakashaVillagerTrades.initialize();
        TakashaMobLevelManager.initialize();
        TakashaTitleManager.initialize();
        TakashaQuestManager.initialize();
        net.takasha.rpg.economy.catalog.ShopCatalog.initialize();
        net.takasha.rpg.world.event.TakashaEventManager.initialize();

        // Custom Enchantment Combat Engine Hook (ALLOW_DAMAGE)
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof net.takasha.rpg.entity.DamageDummyEntity) {
                // Handled directly inside DamageDummyEntity.hurtServer
                return true;
            }
            if (net.takasha.rpg.enchant.TakashaEnchantCombatEngine.isProcessing()) {
                // Secondary damage triggered by enchants (AoE, slayers, thorns, reflect) - allow damage without recursing!
                return true;
            }
            net.takasha.rpg.enchant.TakashaEnchantCombatEngine.setProcessing(true);
            try {
                if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker) {
                    net.takasha.rpg.enchant.TakashaEnchantCombatEngine.applyWeaponCombatEnchants(attacker, entity, source, amount);
                    boolean allow = net.takasha.rpg.enchant.TakashaEnchantCombatEngine.applyArmorCombatEnchants(entity, attacker, source, amount);
                    if (!allow) return false;
                } else {
                    boolean allow = net.takasha.rpg.enchant.TakashaEnchantCombatEngine.applyArmorCombatEnchants(entity, null, source, amount);
                    if (!allow) return false;
                }
                return true;
            } finally {
                net.takasha.rpg.enchant.TakashaEnchantCombatEngine.setProcessing(false);
            }
        });

        // 4. Server Lifecycle & Player Data Management
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            TakashaRpgManager.init(server);
            DungeonArenaEngine.getInstance().checkAndRecoverCrash(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            try {
                net.takasha.rpg.world.event.TakashaEventManager.handleServerStopping(server);
            } catch (Exception e) {
                LOGGER.error("Error during event manager stopping: ", e);
            }
            try {
                DungeonArenaEngine.getInstance().handleServerStopping(server);
            } catch (Exception e) {
                LOGGER.error("Error during dungeon server stopping: ", e);
            }
            try {
                TakashaRpgManager.saveAll();
            } catch (Exception e) {
                LOGGER.error("Error during RPG data saveAll: ", e);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
            if (!data.getActiveTitle().isEmpty()) {
                net.takasha.rpg.title.TakashaTitle title = net.takasha.rpg.title.TakashaTitle.fromCodeOrName(data.getActiveTitle());
                if (title != null) {
                    TakashaTitleManager.setActiveTitle(player.getUUID(), title);
                }
            }

            // Penanganan Pemulihan Crash/Disconnect Dungeon (Safe Teleport & Keystone Refund)
            if (data.isPendingSafeTeleport()) {
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimKey =
                        net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.Identifier.parse(data.getSafeTeleportDim()));
                net.minecraft.server.level.ServerLevel targetLevel = server.getLevel(dimKey);
                if (targetLevel != null) {
                    player.teleportTo(targetLevel, data.getSafeTeleportX(), data.getSafeTeleportY(), data.getSafeTeleportZ(), java.util.Set.of(), player.getYRot(), player.getXRot(), true);
                }
                data.setPendingSafeTeleport(false, "minecraft:overworld", 0, 64, 0);
            }

            if (!data.getPendingKeystoneRefund().isEmpty()) {
                String tierRefund = data.getPendingKeystoneRefund();
                net.minecraft.world.item.ItemStack keystone = switch (tierRefund) {
                    case "NIGHTMARE" -> new net.minecraft.world.item.ItemStack(ModRpgItems.DUNGEON_KEYSTONE_NIGHTMARE);
                    case "SAKURA_BOSS" -> new net.minecraft.world.item.ItemStack(ModRpgItems.DUNGEON_KEYSTONE_SAKURA);
                    default -> new net.minecraft.world.item.ItemStack(ModRpgItems.DUNGEON_KEYSTONE_COMMON);
                };
                boolean added = player.getInventory().add(keystone);
                if (!added) {
                    player.drop(keystone, false);
                }
                data.setPendingKeystoneRefund("");
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a§l[Dungeon] §fRift Keystone Anda (" + tierRefund + ") telah di-refund akibat sesi arena yang terputus!"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
            }

            TakashaRpgManager.savePlayerData(player.getUUID());
            net.takasha.rpg.skills.AuraStatModifierEngine.applyStats(player);
            TakashaQuestManager.syncAutoProgress(player);
            TakashaTitleManager.checkAndUnlockTitles(player);
            TakashaRpgManager.syncToClient(player);
        });

        net.fabricmc.api.EnvType environment = net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType();

        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            net.takasha.rpg.skills.AuraStatModifierEngine.applyStats(newPlayer);
            TakashaRpgManager.syncToClient(newPlayer);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            DungeonArenaEngine.getInstance().handlePlayerDisconnect(handler.getPlayer());
            TakashaTitleManager.setActiveTitle(handler.getPlayer().getUUID(), null);
            TakashaRpgManager.onPlayerDisconnect(handler.getPlayer().getUUID());
        });

        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
                data.setLastDeathLocation(player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
                TakashaRpgManager.savePlayerData(player.getUUID());
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c§l[Takasha-RPG] §cKamu gugur di [" + (int) player.getX() + ", " + (int) player.getY() + ", " + (int) player.getZ() + "]! Ketik §e/back §csetelah respawn untuk kembali."));
            }

            // On-Kill Enchantment Triggers
            if (source != null && source.getEntity() instanceof ServerPlayer killer) {
                net.takasha.rpg.enchant.TakashaEnchantCombatEngine.applyOnKillEnchants(killer, entity);
            }
        });

        // 5. Periodic Heartbeat Sync & Daily Dawn Rotation Tick & Event Manager Ticking
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            net.takasha.rpg.world.event.TakashaEventManager.serverTick(server);

            if (server.getTickCount() % 20 == 0) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    if (player == null || player.isRemoved() || player.hasDisconnected() || player.connection == null) continue;
                    TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
                    double manaRegenBonus = data.getTraitValue(net.takasha.rpg.skills.AuraTraitType.MANA_REGEN);
                    double regenAmount = 1.5 * (1.0 + manaRegenBonus);
                    data.restoreMana(regenAmount);
                    TakashaRpgManager.syncToClient(player);
                }
            }
            if (server.getTickCount() % 80 == 0) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    if (player == null || player.isRemoved() || player.hasDisconnected() || player.connection == null) continue;
                    TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
                    double satRegen = data.getTraitValue(net.takasha.rpg.skills.AuraTraitType.SATURATION_REGEN);
                    if (satRegen > 0) {
                        player.getFoodData().eat(0, (float) (satRegen * 0.5));
                        if (player.getFoodData().getFoodLevel() < 20 && player.getRandom().nextDouble() * 100.0 < satRegen * 10.0) {
                            player.getFoodData().setFoodLevel(Math.min(20, player.getFoodData().getFoodLevel() + 1));
                        }
                    }
                }
            }
            if (server.getTickCount() % 100 == 0 && server.overworld() != null) {
                TakashaDailyShopManager.checkRotation(server.overworld().getOverworldClockTime());
            }
        });

        // 6. Register Commands
        TakashaRpgCommands.initialize();

        LOGGER.info("Takasha-RPG successfully initialized! AuraSkills, AdvancedEnchantments, Arena, ShopGUI & EssentialsX active.");
    }
}
