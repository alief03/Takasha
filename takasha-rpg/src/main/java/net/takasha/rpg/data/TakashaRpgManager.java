package net.takasha.rpg.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.storage.LevelResource;
import net.takasha.rpg.TakashaRpgMod;
import net.takasha.rpg.network.RpgClientSyncPayload;
import net.takasha.rpg.skills.AuraSkillType;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TakashaRpgManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, TakashaRpgData> DATA_MAP = new ConcurrentHashMap<>();
    private static File dataFolder = null;

    public static void init(MinecraftServer server) {
        dataFolder = server.getWorldPath(LevelResource.ROOT).resolve("takasha_rpg").toFile();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public static TakashaRpgData getOrCreateData(ServerPlayer player) {
        return DATA_MAP.computeIfAbsent(player.getUUID(), uuid -> {
            TakashaRpgData data = new TakashaRpgData();
            data.setPlayerUuid(uuid);
            data.setPlayerName(player.getScoreboardName());
            loadPlayerData(uuid, data);
            return data;
        });
    }

    public static TakashaRpgData getData(UUID uuid) {
        return DATA_MAP.get(uuid);
    }

    public static void loadPlayerData(UUID uuid, TakashaRpgData data) {
        if (dataFolder == null) return;
        File file = new File(dataFolder, uuid.toString() + ".json");
        if (file.exists()) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                data.fromJson(json);
            } catch (Exception e) {
                TakashaRpgMod.LOGGER.error("Failed to load RPG data for UUID {}", uuid, e);
            }
        }
    }

    public static void savePlayerData(UUID uuid) {
        TakashaRpgData data = DATA_MAP.get(uuid);
        if (data == null || dataFolder == null) return;
        File file = new File(dataFolder, uuid.toString() + ".json");
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(data.toJson(), writer);
        } catch (IOException e) {
            TakashaRpgMod.LOGGER.error("Failed to save RPG data for UUID {}", uuid, e);
        }
    }

    public static void saveAll() {
        for (UUID uuid : DATA_MAP.keySet()) {
            savePlayerData(uuid);
        }
    }

    public static void onPlayerDisconnect(UUID uuid) {
        savePlayerData(uuid);
        DATA_MAP.remove(uuid);
    }

    public static void syncToClient(ServerPlayer player) {
        TakashaRpgData data = getOrCreateData(player);
        String worldName = (player.level().getServer() != null) ? player.level().getServer().getWorldData().getLevelName() : "Singleplayer";
        String playerName = player.getScoreboardName();

        RpgClientSyncPayload payload = new RpgClientSyncPayload(
            worldName,
            playerName,
            data.getBalance(),
            data.getGems(),
            data.getTotalSkillLevel(),
            data.getHomes().size(),
            data.getMaxHomes(),
            data.isShowScoreboard()
        );

        ServerPlayNetworking.send(player, payload);
    }

    public static void giveSkillXp(ServerPlayer player, AuraSkillType skill, double xp) {
        TakashaRpgData data = getOrCreateData(player);
        int oldLvl = data.getSkillLevel(skill);
        boolean levelUp = data.addSkillXp(skill, xp);

        if (levelUp) {
            int newLvl = data.getSkillLevel(skill);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);

            player.sendSystemMessage(Component.literal("§6§l[AuraSkills] §eSelamat! §fSkill §b" + skill.getDisplayName()
                + " §fnaik ke Level §a§l" + newLvl + "§f!"));

            player.sendOverlayMessage(Component.literal("§e▲ §b" + skill.getDisplayName() + " §fLevel §a" + newLvl)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        } else {
            player.sendOverlayMessage(Component.literal("§a+" + String.format("%.1f", xp) + " " + skill.getDisplayName() + " XP")
                .withStyle(skill.getColor()));
        }

        syncToClient(player);
    }
}
