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

    // --- AUTO FARM & SELLGUI ---
    public boolean autoFarmEnabled = false;
    public boolean autoSellGuiOnFull = true;
    public int autoSellDelayMs = 300;
    public Set<String> targetCrops = new HashSet<>(Set.of(
        "minecraft:wheat",
        "minecraft:carrots",
        "minecraft:potatoes",
        "minecraft:beetroots",
        "minecraft:nether_wart",
        "minecraft:sugar_cane",
        "minecraft:melon",
        "minecraft:pumpkin"
    ));

    // --- AUTO FISHING ---
    public enum FishingMode {
        KAIZEN,
        VANILLA
    }
    public boolean autoFishEnabled = false;
    public FishingMode fishingMode = FishingMode.KAIZEN;
    public boolean autoCapcayEnabled = true;
    public int capcayDelayMs = 1200;
    public int capcayCommandDelayMs = 800;
    public boolean capcayHumanJitter = true;
    public boolean capcaySoundAlert = true;
    public boolean capcayAutoResumeAutofish = true;
    public String capcayTriggerRegex = "(?i)(/capcay|verifikasi|klik\\s+item)";
    public String capcayTitleRegex = "(?i)(capcay|captcha|verifikasi)";
    public boolean rodSlipRecovery = true;
    public int reelInDelayMs = 150;
    public int recastDelayMs = 1200;
    public boolean tripleClick = false;
    public boolean rodBreakProtection = true;
    public int rodBreakThreshold = 3;
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
    public boolean autoKillEnabled = false;
    public MobKillMode mobKillMode = MobKillMode.KAIZEN;
    public double killReach = 3.5; // Max 3.5 blocks reach
    public double maxFovAngle = 35.0; // Strict frontal cone (FOV <= 35 deg, Anti-Aura)
    public boolean syncAttackCooldown = true; // Wait for >= 95% weapon charge
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

    public synchronized void save() {
        try {
            File file = CONFIG_PATH.toFile();
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(this, writer);
            }
        } catch (Throwable e) {
            SakuraWeaponsMod.LOGGER.warn("Failed to save Takasha master config: {}", e.getMessage());
        }
    }

    private void ensureCollectionsNotNull() {
        if (targetCrops == null) targetCrops = new HashSet<>();
        if (keepCatchWhitelist == null) keepCatchWhitelist = new HashSet<>();
        if (killMobWhitelist == null) killMobWhitelist = new HashSet<>();
        if (bowTargetWhitelist == null) bowTargetWhitelist = new HashSet<>();
    }
}
