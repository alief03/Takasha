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
import net.takasha.rpg.dungeon.TakashaArenaManager;
import net.takasha.rpg.economy.TakashaDailyShopManager;
import net.takasha.rpg.enchant.ArmorSetBonusHandler;
import net.takasha.rpg.network.RpgClientSyncPayload;
import net.takasha.rpg.registry.ModRpgItemGroups;
import net.takasha.rpg.registry.ModRpgItems;
import net.takasha.rpg.skills.AuraSkillsEngine;
import net.takasha.rpg.world.TakashaWorldLootModifier;
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

        // 2. Register Items & Creative Tabs
        ModRpgItems.initialize();
        ModRpgItemGroups.initialize();

        // 3. Register Core Engines
        AuraSkillsEngine.initialize();
        ArmorSetBonusHandler.initialize();
        TakashaArenaManager.initialize();
        TakashaWorldLootModifier.initialize();

        // 4. Server Lifecycle & Player Data Management
        ServerLifecycleEvents.SERVER_STARTING.register(TakashaRpgManager::init);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> TakashaRpgManager.saveAll());

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            TakashaRpgManager.getOrCreateData(player);
            TakashaRpgManager.syncToClient(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            TakashaRpgManager.onPlayerDisconnect(handler.getPlayer().getUUID());
        });

        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
                data.setLastDeathLocation(player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
                TakashaRpgManager.savePlayerData(player.getUUID());
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c§l[Takasha-RPG] §cKamu gugur di [" + (int) player.getX() + ", " + (int) player.getY() + ", " + (int) player.getZ() + "]! Ketik §e/back §csetelah respawn untuk kembali."));
            }
        });

        // 5. Daily Dawn Rotation Tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 100 == 0 && server.overworld() != null) {
                TakashaDailyShopManager.checkRotation(server.overworld().getOverworldClockTime());
            }
        });

        // 6. Register Commands
        TakashaRpgCommands.initialize();

        LOGGER.info("Takasha-RPG successfully initialized! AuraSkills, AdvancedEnchantments, Arena, ShopGUI & EssentialsX active.");
    }
}
