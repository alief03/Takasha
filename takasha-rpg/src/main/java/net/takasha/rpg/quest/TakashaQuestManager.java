package net.takasha.rpg.quest;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;
import net.takasha.rpg.registry.ModRpgItems;
import net.takasha.rpg.skills.AuraSkillType;
import net.takasha.rpg.title.TakashaTitle;
import net.takasha.rpg.title.TakashaTitleManager;

import java.util.*;

/**
 * TakashaQuestManager
 * Papan Misi RPG Terpadu:
 * - 12 Quest Kemajuan Bab 1-4 (PRD 13.9)
 * - Daily Quests (Reset harian)
 * - Weekly Quests (Reset mingguan)
 * - 16 Legendary Quests Endgame (LQ-01 s/d LQ-16)
 */
public class TakashaQuestManager {

    private static final Map<String, TakashaQuest> QUEST_MAP = new LinkedHashMap<>();
    private static final List<TakashaQuest> KEMAJUAN_QUESTS = new ArrayList<>();
    private static final List<TakashaQuest> DAILY_QUESTS = new ArrayList<>();
    private static final List<TakashaQuest> WEEKLY_QUESTS = new ArrayList<>();
    private static final List<TakashaQuest> LEGENDARY_QUESTS = new ArrayList<>();

    public static void initialize() {
        QUEST_MAP.clear();
        KEMAJUAN_QUESTS.clear();
        DAILY_QUESTS.clear();
        WEEKLY_QUESTS.clear();
        LEGENDARY_QUESTS.clear();
        registerAllQuests();
    }

    private static void registerAllQuests() {
        // === 12 Quest Kemajuan Bab 1-4 (PRD 13.9) ===
        // Bab 1: Awal Kebangkitan (The Awakening)
        addKemajuan("q_1_1", "Q1-1", "Buka Kitab Petualangan", "Buka Grimoire of Realms melalui tombol inventory.", 1, 1, 100, 5, "5 Magic Dust");
        addKemajuan("q_1_2", "Q1-2", "Tempat Berteduh", "Simpan titik rumah pertamamu dengan perintah /sethome.", 1, 1, 150, 0, "10 Roti");
        addKemajuan("q_1_3", "Q1-3", "Asah Kemampuan", "Capai Level 25 pada salah satu cabang keahlian AuraSkill.", 1, 25, 250, 0, "1 White Scroll");

        // Bab 2: Saudagar & Alkemis (Merchant & Alchemist)
        addKemajuan("q_2_1", "Q2-1", "Langkah Dagang Pertama", "Lakukan transaksi jual atau beli item di /shop.", 2, 1, 300, 0, "None");
        addKemajuan("q_2_2", "Q2-2", "Alkimia Terapan", "Gunakan Magic Dust untuk memperkuat persentase buku sihir.", 2, 1, 0, 2, "1 Transmute Scroll");
        addKemajuan("q_2_3", "Q2-3", "Jangkar Dimensi Baru", "Perluas kuota /sethome menjadi 2 slot.", 2, 2, 1000, 5, "None");

        // Bab 3: Colosseum & Pertempuran (Trials of Combat)
        addKemajuan("q_3_1", "Q3-1", "Ujian Gelombang Pertama", "Kalahkan Wave 1 di Colosseum Arena (/arena start).", 3, 1, 500, 2, "None");
        addKemajuan("q_3_2", "Q3-2", "Penakluk Gladiator", "Bersihkan seluruh 5 Wave Arena hingga membuka Victory Chest.", 3, 5, 2500, 10, "1 Mystery Book Legendary");
        addKemajuan("q_3_3", "Q3-3", "Pejuang Berbakat", "Capai akumulasi Total Level AuraSkill sebesar 250 level.", 3, 250, 1500, 5, "None");

        // Bab 4: Warisan Penguasa (Vestiges of Legends)
        addKemajuan("q_4_1", "Q4-1", "Kesatuan Relik", "Kenakan 4/4 set zirah lengkap RPG (Paladin/Guardian/Ranger).", 4, 4, 3000, 0, "1 Holy Scroll");
        addKemajuan("q_4_2", "Q4-2", "Saudagar Emas", "Kumpulkan saldo kas dompet mencapai 10,000 Takasha Coins.", 4, 10000, 2500, 15, "None");
        addKemajuan("q_4_3", "Q4-3", "Grandmaster Takasha", "Capai akumulasi Total Level AuraSkill sebesar 1,000 level.", 4, 1000, 5000, 30, "Gelar G-16 (Maharaja Takasha)");

        // === Daily Quests ===
        addDaily("dq-01", "DQ-01", "Patroli Hutan Sakura", "Kalahkan 25 monster nokturnal malam hari.", 25, 250, 2, "None");
        addDaily("dq-02", "DQ-02", "Pembersihan Tambang Relik", "Tambang 48 bijih besi atau emas di Y < 0.", 48, 300, 1, "1 Magic Dust");
        addDaily("dq-03", "DQ-03", "Bidikan Jarak Jauh", "Tembak 15 Skeleton dari jarak panah >= 20 blok.", 15, 200, 1, "1 Mystery Dust");
        addDaily("dq-04", "DQ-04", "Panen Kemakmuran", "Panen 64 tanaman matang di ladang pertanian.", 64, 150, 1, "None");

        // === Weekly Quests ===
        addWeekly("wq-01", "WQ-01", "Pembersihan Lembah Nether", "Musnahkan 150 Piglin atau Wither Skeleton.", 150, 2500, 8, "1 White Scroll");
        addWeekly("wq-02", "WQ-02", "Perburuan Boss Mini", "Kalahkan 10 monster bertanda Tier Elite/Fabled/Mythic.", 10, 3000, 10, "1 Slot Increaser");
        addWeekly("wq-03", "WQ-03", "Penempa Ksatria", "Lakukan 20 kali enchant atau tempa di Anvil.", 20, 1800, 6, "3 Magic Dust");

        // === 16 Legendary Quests Endgame (LQ-01 s/d LQ-16) ===
        addLegendary("lq-01", "LQ-01", "Beban Sang Titan", "Kalahkan 500 monster ber-level >= 150 di Nether.", 500, 10000, 25, "Gelar G-05 (Penguasa Api Neraka)");
        addLegendary("lq-02", "LQ-02", "Pemburu Bintang Jatuh", "Kumpulkan 64 Netherite Scrap murni.", 64, 12000, 30, "1 Slot Increaser");
        addLegendary("lq-03", "LQ-03", "Pemberkatan Senjata Sempurna", "Pasang 15 custom enchantments pada satu senjata.", 15, 15000, 35, "2 White Scrolls");
        addLegendary("lq-04", "LQ-04", "Mata Badai Abadi", "Kalahkan 3 Wither Boss secara berturut-turut di Arena.", 3, 20000, 50, "Gelar G-03 (Ksatria Penakluk Badai)");
        addLegendary("lq-05", "LQ-05", "Penguasa Jiwa Kegelapan", "Kumpulkan 5,000 Soul Energy dari membunuh monster.", 5000, 18000, 40, "1 Soul Gem");
        addLegendary("lq-06", "LQ-06", "Ekspedisi Ujung Semesta", "Jelajahi The End dan buka 10 End Ships.", 10, 22000, 45, "Gelar G-02 (Penjelajah Lima Dimensi)");
        addLegendary("lq-07", "LQ-07", "Pembalasan Darah Relik", "Bunuh 50 monster LevelledMobs bertanda Tier Mythic Boss.", 50, 25000, 60, "Gelar G-07 (Pembalas Dendam Relik)");
        addLegendary("lq-08", "LQ-08", "Alkimia Sempurna", "Gunakan 100 Magic Dust pada buku sihir.", 100, 14000, 30, "Gelar G-08 (Alkemis Jiwa Terkutuk)");
        addLegendary("lq-09", "LQ-09", "Algojo Seratus Pertarungan", "Mendaratkan 250 serangan fatal instan.", 250, 16000, 35, "Gelar G-09 (Algojo Tak Bersuara)");
        addLegendary("lq-10", "LQ-10", "Panglima Perang Tak Terkalahkan", "Selesaikan 10 Raid Desa Level V sempurna.", 10, 28000, 65, "Gelar G-10 (Panglima Seratus Arena)");
        addLegendary("lq-11", "LQ-11", "Harmoni Bunga Sakura", "Musnahkan 100 musuh berturut-turut dengan senjata Sakura.", 100, 15000, 35, "Gelar G-11 (Pewaris Pedang Sakura)");
        addLegendary("lq-12", "LQ-12", "Kutukan Sang Dukun", "Kalahkan 20 Monster Boss bertanda Calamity.", 20, 18000, 40, "Gelar G-12 (Dukun Pembawa Petaka)");
        addLegendary("lq-13", "LQ-13", "Penempa Kedaulatan Tertinggi", "Gunakan Anvil sebanyak 50 kali dalam penempaan.", 50, 35000, 80, "Gelar G-13 (Dewa Penempa Legendaris)");
        addLegendary("lq-14", "LQ-14", "Raja Langit Kosmik", "Kalahkan Ender Dragon atau berpartisipasi di Event Meteor.", 1, 30000, 70, "Gelar G-14 (Raja Naga Bintang)");
        addLegendary("lq-15", "LQ-15", "Ujian Keabadian", "Bertahan di Cataclysm Monster Level 500+.", 1, 40000, 90, "Gelar G-15 (Penguasa Keabadian)");
        addLegendary("lq-16", "LQ-16", "Mahkota Sang Penguasa Takasha", "Capai Total AuraSkills 1000+ dan selesaikan seluruh Bab.", 1, 100000, 250, "Gelar Puncak G-16 (Maharaja Takasha)");
    }

    private static void addKemajuan(String id, String code, String title, String desc, int chapter, int goal, int coins, int gems, String extra) {
        TakashaQuest q = new TakashaQuest(id, code, title, desc, TakashaQuest.QuestType.KEMAJUAN, chapter, goal, coins, gems, extra);
        QUEST_MAP.put(id, q);
        KEMAJUAN_QUESTS.add(q);
    }

    private static void addDaily(String id, String code, String title, String desc, int goal, int coins, int gems, String extra) {
        TakashaQuest q = new TakashaQuest(id, code, title, desc, TakashaQuest.QuestType.DAILY, goal, coins, gems, extra);
        QUEST_MAP.put(id, q);
        DAILY_QUESTS.add(q);
    }

    private static void addWeekly(String id, String code, String title, String desc, int goal, int coins, int gems, String extra) {
        TakashaQuest q = new TakashaQuest(id, code, title, desc, TakashaQuest.QuestType.WEEKLY, goal, coins, gems, extra);
        QUEST_MAP.put(id, q);
        WEEKLY_QUESTS.add(q);
    }

    private static void addLegendary(String id, String code, String title, String desc, int goal, int coins, int gems, String extra) {
        TakashaQuest q = new TakashaQuest(id, code, title, desc, TakashaQuest.QuestType.LEGENDARY, goal, coins, gems, extra);
        QUEST_MAP.put(id, q);
        LEGENDARY_QUESTS.add(q);
    }

    public static TakashaQuest get(String id) {
        return QUEST_MAP.get(id);
    }

    public static List<TakashaQuest> getKemajuanQuests() {
        return Collections.unmodifiableList(KEMAJUAN_QUESTS);
    }

    public static List<TakashaQuest> getKemajuanQuests(int chapter) {
        return KEMAJUAN_QUESTS.stream().filter(q -> q.getChapter() == chapter).toList();
    }

    public static List<TakashaQuest> getLegendaryQuests() {
        return Collections.unmodifiableList(LEGENDARY_QUESTS);
    }

    public static List<TakashaQuest> getDailyQuests() {
        return Collections.unmodifiableList(DAILY_QUESTS);
    }

    public static List<TakashaQuest> getWeeklyQuests() {
        return Collections.unmodifiableList(WEEKLY_QUESTS);
    }

    public static List<TakashaQuest> getAllQuests() {
        return new ArrayList<>(QUEST_MAP.values());
    }

    /**
     * Otomatis menyinkronkan progres quest statis (balance, skill level, sethomes, equipment).
     */
    public static void syncAutoProgress(ServerPlayer player) {
        if (player == null) return;
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);

        // Quest 1.1: Buka Kitab Petualangan
        data.setQuestProgress("q_1_1", 1);

        // Quest 1.2: Tempat Berteduh (Homes >= 1)
        if (!data.getHomes().isEmpty()) {
            data.setQuestProgress("q_1_2", 1);
        }

        // Quest 1.3: Asah Kemampuan (Highest Skill Level >= 25)
        int highestSkill = 1;
        for (AuraSkillType skill : AuraSkillType.values()) {
            highestSkill = Math.max(highestSkill, data.getSkillLevel(skill));
        }
        data.setQuestProgress("q_1_3", Math.min(25, highestSkill));

        // Quest 2.3: Jangkar Dimensi Baru (Max Homes >= 2)
        if (data.getMaxHomes() >= 2) {
            data.setQuestProgress("q_2_3", data.getMaxHomes());
        }

        // Quest 3.3: Pejuang Berbakat (Total SK >= 250)
        data.setQuestProgress("q_3_3", Math.min(250, data.getTotalSkillLevel()));

        // Quest 4.2: Saudagar Emas (Balance >= 10,000)
        data.setQuestProgress("q_4_2", Math.min(10000, (int) data.getBalance()));

        // Quest 4.3: Grandmaster Takasha (Total SK >= 1,000)
        data.setQuestProgress("q_4_3", Math.min(1000, data.getTotalSkillLevel()));

        // Periksa pembukaan Title secara otomatis
        TakashaTitleManager.checkAndUnlockTitles(player);
    }

    /**
     * Mencatat progres aktivitas RPG secara otomatis dan memicu pengecekan unlock gelar.
     */
    public static void recordProgress(ServerPlayer player, String statKey, int amount) {
        if (player == null || amount <= 0) return;
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        data.addStat(statKey, amount);

        // Petakan statKey ke quest-quest terkait
        switch (statKey) {
            case "nightMobsKilled" -> {
                data.addQuestProgress("dq-01", amount);
            }
            case "deepslateOresMined" -> {
                data.addQuestProgress("dq-02", amount);
            }
            case "distanceBowKills" -> {
                data.addQuestProgress("dq-03", amount);
            }
            case "cropsHarvested" -> {
                data.addQuestProgress("dq-04", amount);
            }
            case "netherMobsKilled" -> {
                data.addQuestProgress("wq-01", amount);
                data.addQuestProgress("lq-01", amount);
            }
            case "eliteMobsKilled" -> {
                data.addQuestProgress("wq-02", amount);
                data.addQuestProgress("lq-07", amount);
            }
            case "anvilUses" -> {
                data.addQuestProgress("wq-03", amount);
                data.addQuestProgress("lq-13", amount);
            }
            case "shopPurchases" -> {
                data.addQuestProgress("q_2_1", amount);
            }
            case "magicDustUsed" -> {
                data.addQuestProgress("q_2_2", amount);
                data.addQuestProgress("lq-08", amount);
            }
            case "arenaWaveCleared" -> {
                data.addQuestProgress("q_3_1", amount);
            }
            case "arenaWavesCleared" -> {
                data.addQuestProgress("q_3_2", amount);
            }
            case "witherKills" -> {
                data.addQuestProgress("lq-04", amount);
            }
            case "soulGained" -> {
                data.addQuestProgress("lq-05", amount);
            }
            case "instantExecutions" -> {
                data.addQuestProgress("lq-09", amount);
            }
            case "sakuraWeaponKills" -> {
                data.addQuestProgress("lq-11", amount);
            }
            case "bossesKilled" -> {
                data.addQuestProgress("lq-12", amount);
            }
            case "enderDragonKills", "meteorEventsMined" -> {
                data.addQuestProgress("lq-14", amount);
            }
            case "cataclysmSurvived" -> {
                data.addQuestProgress("lq-15", amount);
            }
        }

        // Cek gelar yang terbuka
        TakashaTitleManager.checkAndUnlockTitles(player);
        TakashaRpgManager.syncToClient(player);
    }

    /**
     * Memproses klaim hadiah misi yang telah memenuhi target.
     */
    public static boolean claimReward(ServerPlayer player, String questId) {
        if (player == null || questId == null) return false;
        TakashaQuest q = get(questId);
        if (q == null) return false;

        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        if (data.isQuestCompleted(questId)) {
            player.sendSystemMessage(Component.literal("§c[Quest] Misi ini sudah pernah kamu klaim sebelumnya!"));
            return false;
        }

        syncAutoProgress(player);

        int currentProgress = data.getQuestProgress(questId);
        if (currentProgress < q.getTargetGoal()) {
            player.sendSystemMessage(Component.literal("§c[Quest] Misi belum selesai! Progres: " + currentProgress + "/" + q.getTargetGoal()));
            return false;
        }

        // Tandai selesai
        data.setQuestCompleted(questId);

        // Bagikan reward koin dan gems
        data.addBalance(q.getRewardCoins());
        data.addGems(q.getRewardGems());

        // Bagikan item reward tambahan jika ada
        giveExtraReward(player, data, q.getExtraReward());

        // Efek audio & pengumuman
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);

        player.sendSystemMessage(Component.literal("§6§l✦ HADIAH MISI DIKLAIM! ✦ §e" + q.getCode() + ": " + q.getTitle()));
        player.sendSystemMessage(Component.literal("  §fImbalan: §a+" + q.getRewardCoins() + " Coins§f, §b+" + q.getRewardGems() + " Gems§f! (" + q.getExtraReward() + ")"));

        // Periksa title unlock
        TakashaTitleManager.checkAndUnlockTitles(player);

        TakashaRpgManager.savePlayerData(player.getUUID());
        TakashaRpgManager.syncToClient(player);
        return true;
    }

    private static void giveExtraReward(ServerPlayer player, TakashaRpgData data, String extraReward) {
        if (extraReward == null || extraReward.isEmpty() || extraReward.equalsIgnoreCase("None")) return;

        if (extraReward.contains("Magic Dust")) {
            giveItem(player, new ItemStack(ModRpgItems.MAGIC_DUST, 5));
        } else if (extraReward.contains("White Scroll")) {
            giveItem(player, new ItemStack(ModRpgItems.WHITE_SCROLL, 1));
        } else if (extraReward.contains("Holy Scroll")) {
            giveItem(player, new ItemStack(ModRpgItems.HOLY_SCROLL, 1));
        } else if (extraReward.contains("Transmute Scroll")) {
            giveItem(player, new ItemStack(ModRpgItems.TRANSMUTE_SCROLL, 1));
        } else if (extraReward.contains("Roti")) {
            giveItem(player, new ItemStack(Items.BREAD, 10));
        } else if (extraReward.contains("Slot Increaser")) {
            giveItem(player, new ItemStack(ModRpgItems.SLOT_INCREASER_1, 1));
        } else if (extraReward.contains("Soul Gem")) {
            giveItem(player, new ItemStack(ModRpgItems.SOUL_GEM, 1));
        } else if (extraReward.contains("Mystery Dust")) {
            giveItem(player, new ItemStack(ModRpgItems.MYSTERY_DUST, 1));
        } else if (extraReward.contains("Mystery Book")) {
            giveItem(player, new ItemStack(ModRpgItems.MYSTERY_BOOK_LEGENDARY, 1));
        }

        // Title unlock handling from quest extraReward
        for (TakashaTitle title : TakashaTitle.values()) {
            if (extraReward.contains(title.getCode()) || extraReward.contains(title.getTitleName())) {
                data.unlockTitle(title.getCode());
                player.sendSystemMessage(Component.literal("§6§l✦ GELAR DIBERIKAN! ✦ §r" + title.getColor() + title.getCode() + " " + title.getTitleName()));
            }
        }
    }

    private static void giveItem(ServerPlayer player, ItemStack stack) {
        boolean added = player.getInventory().add(stack);
        if (!added) {
            player.drop(stack, false);
        }
    }
}
