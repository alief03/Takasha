package net.sakura.weapons.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.sakura.weapons.SakuraWeaponsMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class TakashaMasterConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("takasha_master_features.json");
    private static volatile TakashaMasterConfig INSTANCE;

    // --- SERVER SAFE MODE ---
    public boolean serverSafeMode = true;

    // --- AUTO FARM & SELLGUI ---
    public boolean autoFarmEnabled = false;
    public boolean autoSellGuiOnFull = true;
    public int autoSellDelayMs = 300;
    public boolean farmOnlyInFront = false;
    public int seedReserveCount = 32;
    public int farmRadiusH = 6;
    public int farmRadiusV = 2;
    public int farmActionDelayMs = 0;
    public int farmActionsPerTick = 12;
    public int farmReplantDelayMs = 0;
    public boolean autoPlantEmptyFarmland = true;
    public boolean cropMemoryEnabled = true;
    public boolean replantPacingEnabled = true;
    public int maxReplantRetries = 25;
    public Set<String> targetCrops = new HashSet<>(Set.of(
        "minecraft:wheat",
        "minecraft:carrots",
        "minecraft:potatoes",
        "minecraft:beetroots",
        "minecraft:nether_wart",
        "minecraft:sugar_cane",
        "minecraft:melon",
        "minecraft:pumpkin",
        "minecraft:cocoa",
        "minecraft:sweet_berry_bush",
        "minecraft:cactus",
        "minecraft:bamboo",
        "minecraft:kelp"
    ));

    // --- AUTO FISHING ---
    public enum FishingMode {
        KAIZEN,
        VANILLA
    }
    public enum BiteSensitivity {
        ULTRA(-0.03),
        HIGH(-0.06),
        NORMAL(-0.10);

        private final double threshold;

        BiteSensitivity(double threshold) {
            this.threshold = threshold;
        }

        public double getThreshold() {
            return threshold;
        }
    }
    public boolean autoFishEnabled = false;
    public FishingMode fishingMode = FishingMode.KAIZEN;
    public BiteSensitivity biteSensitivity = BiteSensitivity.ULTRA;
    public boolean autoCapcayEnabled = true;
    public int capcayDelayMs = 1200;
    public int capcayCommandDelayMs = 800;
    public boolean capcayHumanJitter = true;
    public boolean capcaySoundAlert = true;
    public boolean capcayAutoResumeAutofish = true;
    public String capcayTriggerRegex = "(?i)(/capcay|verifikasi|klik\\s+item)";
    public String capcayTitleRegex = "(?i)(capcay|captcha|verifikasi)";
    public boolean rodSlipRecovery = true;
    public int reelInDelayMs = 0;
    public int recastDelayMs = 250;
    public boolean tripleClick = false;
    public boolean rodBreakProtection = true;
    public int rodBreakThreshold = 3;
    public boolean autoSwitchRodOnLowDurability = true;
    public boolean soundAlertOnCatch = true;
    public boolean filterCatchEnabled = true;
    // Whitelist of items to KEEP in inventory. Items caught not in this whitelist will be dropped!
    public Set<String> keepCatchWhitelist = new HashSet<>(Set.of(
        "minecraft:enchanted_book",
        "minecraft:bow",
        "minecraft:fishing_rod",
        "minecraft:nautilus_shell",
        "minecraft:saddle",
        "minecraft:name_tag"
    ));

    // --- AUTO KILL MOB ---
    public enum MobKillMode {
        KAIZEN,
        VANILLA
    }
    public enum KillTargetPriority {
        NEAREST,
        LOWEST_HEALTH,
        HIGHEST_HEALTH,
        FOV_CENTER
    }
    public enum AttackRateMode {
        COOLDOWN_SYNC,
        TURBO,
        HYPER_SPAM
    }
    public boolean autoKillEnabled = false;
    public MobKillMode mobKillMode = MobKillMode.KAIZEN;
    public KillTargetPriority killTargetPriority = KillTargetPriority.NEAREST;
    public AttackRateMode attackRateMode = AttackRateMode.COOLDOWN_SYNC;
    public double killReach = 4.5; // Default 4.5m reach
    public double maxFovAngle = 60.0; // Frontal view cone
    public boolean syncAttackCooldown = true; // Wait for >= 95% weapon charge
    public boolean smartCritical = false;
    public boolean attackPlayers = false;
    public boolean attackHostileOnly = true;
    public boolean autoSwitchWeapon = true;
    public boolean weaponBreakProtection = true;
    public int weaponBreakThreshold = 5;
    public boolean antiAfkEnabled = true;
    public boolean antiAfkContinuous = true;
    public int antiAfkDurationMs = 180;
    public boolean antiAfkAlwaysSneak = true;
    public Set<String> killMobWhitelist = new HashSet<>(Set.of(
        "minecraft:cow",
        "minecraft:sheep",
        "minecraft:pig",
        "minecraft:chicken",
        "minecraft:zombie",
        "minecraft:skeleton",
        "minecraft:creeper",
        "minecraft:spider",
        "minecraft:blaze",
        "minecraft:enderman",
        "minecraft:drowned",
        "minecraft:husk",
        "minecraft:stray"
    ));

    // --- AUTO BOW ---
    public boolean autoBowEnabled = false;
    public double autoBowRange = 24.0;
    public boolean autoBowAimAssist = true;
    public int autoBowPullTicks = 20; // 1.0 full draw
    public Set<String> bowTargetWhitelist = new HashSet<>(Set.of(
        "minecraft:zombie",
        "minecraft:skeleton",
        "minecraft:creeper",
        "minecraft:spider",
        "minecraft:blaze",
        "minecraft:enderman",
        "minecraft:drowned",
        "minecraft:witch"
    ));

    // --- FORTUNE RESHAPER ---
    public enum FortuneDisplayMode {
        ACTIONBAR,
        CHAT,
        TITLE,
        OFF,
        PASSTHROUGH
    }
    public enum FortuneDetailLevel {
        DETAILED,
        COMPACT,
        MINIMAL
    }
    public boolean fortuneReshaperEnabled = true;
    public FortuneDisplayMode fortuneDisplayMode = FortuneDisplayMode.ACTIONBAR;
    public FortuneDetailLevel fortuneDetailLevel = FortuneDetailLevel.COMPACT;
    public String fortunePrefix = "§d[Takasha Fortune] ";
    public String fortuneColorProc = "§e";
    public String fortuneColorXp = "§b";

    public static TakashaMasterConfig getInstance() {
        if (INSTANCE == null) {
            synchronized (TakashaMasterConfig.class) {
                if (INSTANCE == null) {
                    INSTANCE = load();
                }
            }
        }
        return INSTANCE;
    }

    public static TakashaMasterConfig load() {
        File file = CONFIG_PATH.toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                TakashaMasterConfig cfg = GSON.fromJson(reader, TakashaMasterConfig.class);
                if (cfg != null) {
                    cfg.ensureCollectionsNotNull();
                    return cfg;
                }
            } catch (Throwable e) {
                SakuraWeaponsMod.LOGGER.warn("Failed to load Takasha master config, using defaults: {}", e.getMessage());
            }
        }
        TakashaMasterConfig fallback = new TakashaMasterConfig();
        fallback.save();
        return fallback;
    }

    private static final java.util.concurrent.ExecutorService CONFIG_EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Takasha-MasterConfig-Writer");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    public synchronized void save() {
        try {
            String json = GSON.toJson(this);
            CONFIG_EXECUTOR.submit(() -> {
                try {
                    File file = CONFIG_PATH.toFile();
                    if (file.getParentFile() != null) {
                        file.getParentFile().mkdirs();
                    }
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.write(json);
                    }
                } catch (Throwable e) {
                    SakuraWeaponsMod.LOGGER.warn("Failed to write Takasha master config: {}", e.getMessage());
                }
            });
        } catch (Throwable e) {
            SakuraWeaponsMod.LOGGER.warn("Failed to serialize Takasha master config: {}", e.getMessage());
        }
    }

    private void ensureCollectionsNotNull() {
        if (targetCrops == null) targetCrops = new HashSet<>();
        if (keepCatchWhitelist == null) keepCatchWhitelist = new HashSet<>();
        if (killMobWhitelist == null) killMobWhitelist = new HashSet<>();
        if (bowTargetWhitelist == null) bowTargetWhitelist = new HashSet<>();
        if (fishingMode == null) fishingMode = FishingMode.KAIZEN;
        if (biteSensitivity == null) biteSensitivity = BiteSensitivity.ULTRA;
        if (mobKillMode == null) mobKillMode = MobKillMode.KAIZEN;
        if (killTargetPriority == null) killTargetPriority = KillTargetPriority.NEAREST;
        if (attackRateMode == null) attackRateMode = AttackRateMode.COOLDOWN_SYNC;
    }
}
