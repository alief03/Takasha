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

    public static TakashaRpgData getOrCreateData(UUID uuid) {
        return DATA_MAP.computeIfAbsent(uuid, id -> {
            TakashaRpgData data = new TakashaRpgData();
            data.setPlayerUuid(id);
            loadPlayerData(id, data);
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
        if (player == null || player.isRemoved() || player.hasDisconnected() || player.connection == null) return;
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
            data.isShowScoreboard(),
            data.getCurrentMana(),
            data.getMaxMana()
        );

        ServerPlayNetworking.send(player, payload);
    }

    public static void giveSkillXp(ServerPlayer player, AuraSkillType skill, double xp) {
        TakashaRpgData data = getOrCreateData(player);
        int oldLvl = data.getSkillLevel(skill);
        boolean levelUp = data.addSkillXp(skill, xp);

        if (levelUp) {
            int newLvl = data.getSkillLevel(skill);
            double currentXp = data.getSkillXp(skill);
            double reqXp = skill.getXpForNextLevel(newLvl);
            net.takasha.rpg.hud.TakashaXpBossbarManager.showXpProgress(player, skill, xp, newLvl, currentXp, reqXp, true);

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);

            // Send Title & Subtitle animation packets
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                Component.literal("§6§l✦ SKILL LEVEL UP! ✦")
            ));
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                Component.literal("§e" + skill.getDisplayName() + " §7Level §b" + oldLvl + " §f➜ §a§l" + newLvl)
            ));

            // Level up divisualisasikan melalui Title, Chat breakdown, suara, dan Bossbar atas tanpa menimpa Actionbar HUD.

            // Aplikasikan atribut fisik baru ke pemain
            net.takasha.rpg.skills.AuraStatModifierEngine.applyStats(player);

            // Chat breakdown seperti di plugin AuraSkills asli
            player.sendSystemMessage(Component.literal("§8§m----------------------------------------"));
            player.sendSystemMessage(Component.literal("§6§l✦ AURASKILLS LEVEL UP! ✦"));
            player.sendSystemMessage(Component.literal("§fSkill: §b" + skill.getDisplayName() + " §7(Level §e" + oldLvl + " §7➜ §a§l" + newLvl + "§7)"));
            player.sendSystemMessage(Component.literal("§eHadiah Peningkatan Atribut & Stat:"));
            switch (skill) {
                case FARMING -> {
                    player.sendSystemMessage(Component.literal(" §7• §c❤ Max Health: §a+0.2 HP §7(Sekarang: §c" + String.format("%.1f", player.getMaxHealth()) + " HP§7)"));
                    player.sendSystemMessage(Component.literal(" §7• §a🌾 Perk: §fBountiful Harvest (Peluang panen ekstra)"));
                }
                case FORAGING -> {
                    player.sendSystemMessage(Component.literal(" §7• §c⚔ Attack Damage: §a+0.05 §7(Pukulan lebih bertenaga)"));
                    player.sendSystemMessage(Component.literal(" §7• §6🪓 Perk Aktif: §eTreecapitator §7(Shift + Klik Kanan kapak, 20 Mana)"));
                }
                case MINING -> {
                    player.sendSystemMessage(Component.literal(" §7• §b🛡 Armor Toughness: §a+0.02 §7| §e✦ Luck: §a+0.01"));
                    player.sendSystemMessage(Component.literal(" §7• §b⛏ Perk: §fOre Doubler (Peluang bijih ganda)"));
                }
                case FISHING -> {
                    player.sendSystemMessage(Component.literal(" §7• §e✦ Luck: §a+0.02 §7(Hoki memancing harta karun)"));
                    player.sendSystemMessage(Component.literal(" §7• §3🎣 Perk: §fTreasure Hunter"));
                }
                case EXCAVATION -> {
                    player.sendSystemMessage(Component.literal(" §7• §c⚔ Strength & Luck: §a+0.02 §7| §eRelic Finder"));
                    player.sendSystemMessage(Component.literal(" §7• §6⛏ Perk Aktif: §eTerraform §7(Shift + Klik Kanan sekop, 20 Mana)"));
                }
                case FIGHTING -> {
                    player.sendSystemMessage(Component.literal(" §7• §c⚔ Critical Damage: §a+1.0% §7| §d✦ Regeneration: §a+0.5 (setiap 2 level)"));
                    player.sendSystemMessage(Component.literal(" §7• §c⚔ Perk Aktif: §eLightning Blade §7(Shift + Klik Kanan pedang, 20 Mana)"));
                }
                case ARCHERY -> {
                    player.sendSystemMessage(Component.literal(" §7• §a⚡ Movement Speed: §a+0.00015 §7| §e🏹 Velocity: §a+1%"));
                    player.sendSystemMessage(Component.literal(" §7• §a🏹 Perk Aktif: §eCharged Shot §7(Shift + Klik Kanan busur, 20 Mana)"));
                }
                case DEFENSE -> {
                    player.sendSystemMessage(Component.literal(" §7• §c❤ Max Health: §a+0.2 HP §7| §b🛡 Armor Toughness: §a+0.02"));
                    player.sendSystemMessage(Component.literal(" §7• §e🛡 Perk Aktif: §eAbsorption §7(Shift + Klik Kanan perisai, 20 Mana)"));
                }
                case AGILITY -> {
                    player.sendSystemMessage(Component.literal(" §7• §a⚡ Movement Speed: §a+0.00015 §7| §f🕊 Fall Damage: §a-1%"));
                    player.sendSystemMessage(Component.literal(" §7• §f💨 Perk: §fLight Footed & Dodge"));
                }
                case ALCHEMY -> {
                    player.sendSystemMessage(Component.literal(" §7• §d✦ Potion Duration: §a+1.5% §7| §b✦ Max Mana: §a+2.0"));
                    player.sendSystemMessage(Component.literal(" §7• §5🧪 Perk: §fBrew Master"));
                }
                case ENCHANTING -> {
                    player.sendSystemMessage(Component.literal(" §7• §b✦ Max Mana: §a+5.0 §7(Sekarang: §b" + (int)(data.getMaxMana()) + " Mana§7)"));
                    player.sendSystemMessage(Component.literal(" §7• §9✨ Perk: §fExperience Syphon & Sage"));
                }
            }
            player.sendSystemMessage(Component.literal("§8§m----------------------------------------"));
        } else {
            int currentLvl = data.getSkillLevel(skill);
            double currentXp = data.getSkillXp(skill);
            double reqXp = skill.getXpForNextLevel(currentLvl);

            net.takasha.rpg.hud.TakashaXpBossbarManager.showXpProgress(player, skill, xp, currentLvl, currentXp, reqXp, false);
            // Non-level-up XP message: Progres divisualisasikan secara terpusat melalui TakashaXpBossbarManager di layar atas.
            // Overlay actionbar dinonaktifkan agar tidak menumpuk dengan HUD HP & Mana (TakashaActionbarHud).
        }

        syncToClient(player);
    }
}
