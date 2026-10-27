package net.takasha.rpg.enchant;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.takasha.rpg.TakashaRpgMod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Unified Registry of 336 Custom Enchantments in Takasha RPG
 * Terbagi ke dalam 9 Tier Resmi:
 * 1. Simple (71)
 * 2. Unique (26)
 * 3. Elite (54)
 * 4. Ultimate (71)
 * 5. Legendary (57)
 * 6. Fabled (13)
 * 7. Heroic (28)
 * 8. Soul (9)
 * 9. Mastery (7)
 */
public class TakashaEnchantRegistry {

    public enum EnchantTier {
        SIMPLE("Simple", "§7", 71, 150),
        UNIQUE("Unique", "§a", 26, 450),
        ELITE("Elite", "§b", 54, 1200),
        ULTIMATE("Ultimate", "§e", 71, 3500),
        LEGENDARY("Legendary", "§6", 57, 8500),
        FABLED("Fabled", "§d", 13, 18000),
        HEROIC("Heroic", "§c", 28, 35000),
        SOUL("Soul", "§3", 9, 50000),
        MASTERY("Mastery", "§e", 7, 100000);

        private final String displayName;
        private final String colorCode;
        private final int enchantCount;
        private final int gachaCostCoins;

        EnchantTier(String displayName, String colorCode, int enchantCount, int gachaCostCoins) {
            this.displayName = displayName;
            this.colorCode = colorCode;
            this.enchantCount = enchantCount;
            this.gachaCostCoins = gachaCostCoins;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColorCode() {
            return colorCode;
        }

        public int getEnchantCount() {
            return enchantCount;
        }

        public int getGachaCostCoins() {
            return gachaCostCoins;
        }
    }

    public static class CustomEnchantDef {
        public final String id;
        public final String name;
        public final String description;
        public final EnchantTier tier;
        public final String appliesTo;
        public final int maxLevel;

        public CustomEnchantDef(String id, String name, String description, EnchantTier tier, String appliesTo, int maxLevel) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.tier = tier;
            this.appliesTo = appliesTo;
            this.maxLevel = maxLevel;
        }
    }

    private static final Map<String, CustomEnchantDef> ENCHANTMENTS = new LinkedHashMap<>();
    private static final Map<EnchantTier, List<CustomEnchantDef>> BY_TIER = new EnumMap<>(EnchantTier.class);

    static {
        for (EnchantTier tier : EnchantTier.values()) {
            BY_TIER.put(tier, new ArrayList<>());
        }
        initialize();
    }

    public static void initialize() {
        if (!ENCHANTMENTS.isEmpty()) return;

        // Method 1: Fabric ModContainer findPath
        try {
            java.util.Optional<java.nio.file.Path> pathOpt = net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getModContainer("takasha_rpg")
                    .flatMap(c -> c.findPath("data/takasha_rpg/enchantments_unified.json"));
            if (pathOpt.isPresent()) {
                try (InputStream in = java.nio.file.Files.newInputStream(pathOpt.get())) {
                    loadFromStream(in);
                }
            }
        } catch (Throwable t) {
            TakashaRpgMod.LOGGER.debug("ModContainer resource lookup skipped: {}", t.getMessage());
        }

        // Method 2: ClassLoader resource stream
        if (ENCHANTMENTS.isEmpty()) {
            try (InputStream in = TakashaEnchantRegistry.class.getClassLoader().getResourceAsStream("data/takasha_rpg/enchantments_unified.json")) {
                if (in != null) {
                    loadFromStream(in);
                }
            } catch (Exception e) {
                TakashaRpgMod.LOGGER.debug("ClassLoader resource stream failed: {}", e.getMessage());
            }
        }

        // Method 3: Class resource stream
        if (ENCHANTMENTS.isEmpty()) {
            try (InputStream in = TakashaEnchantRegistry.class.getResourceAsStream("/data/takasha_rpg/enchantments_unified.json")) {
                if (in != null) {
                    loadFromStream(in);
                }
            } catch (Exception e) {
                TakashaRpgMod.LOGGER.debug("Class getResourceAsStream failed: {}", e.getMessage());
            }
        }

        // Method 4: Fallback to all 9 core tiers
        if (ENCHANTMENTS.isEmpty()) {
            TakashaRpgMod.LOGGER.warn("All resource streams failed. Loading emergency core definitions for 9 tiers...");
            registerCoreDefinitions();
        } else {
            TakashaRpgMod.LOGGER.info("TakashaEnchantRegistry successfully loaded {} custom enchantments across 9 tiers!", ENCHANTMENTS.size());
        }
    }

    private static void loadFromStream(InputStream in) throws Exception {
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonArray arr = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String id = obj.get("id").getAsString();
                String name = obj.get("name").getAsString();
                String desc = obj.has("description") ? obj.get("description").getAsString() : "";
                String tierStr = obj.has("tier") ? obj.get("tier").getAsString() : "SIMPLE";
                String appliesTo = obj.has("appliesTo") ? obj.get("appliesTo").getAsString() : "Senjata Tempur";
                int maxLvl = obj.has("maxLevel") ? obj.get("maxLevel").getAsInt() : 1;

                EnchantTier tier;
                try {
                    tier = EnchantTier.valueOf(tierStr.toUpperCase());
                } catch (Exception ex) {
                    tier = EnchantTier.SIMPLE;
                }
                register(id, name, desc, tier, appliesTo, maxLvl);
            }
        }
    }

    public static void register(String id, String name, String description, EnchantTier tier, String appliesTo, int maxLevel) {
        CustomEnchantDef def = new CustomEnchantDef(id, name, description, tier, appliesTo, maxLevel);
        ENCHANTMENTS.put(id.toLowerCase(), def);
        BY_TIER.get(tier).add(def);
    }

    public static CustomEnchantDef get(String id) {
        if (id == null) return null;
        return ENCHANTMENTS.get(id.toLowerCase());
    }

    public static List<CustomEnchantDef> getByTier(EnchantTier tier) {
        return Collections.unmodifiableList(BY_TIER.getOrDefault(tier, Collections.emptyList()));
    }

    public static Collection<CustomEnchantDef> getAll() {
        return Collections.unmodifiableCollection(ENCHANTMENTS.values());
    }

    public static Set<String> getAllIds() {
        return Collections.unmodifiableSet(ENCHANTMENTS.keySet());
    }

    public static int getTotalCount() {
        return ENCHANTMENTS.size();
    }

    public static List<CustomEnchantDef> search(String query) {
        if (query == null || query.isBlank()) {
            return new ArrayList<>(ENCHANTMENTS.values());
        }
        String q = query.toLowerCase().trim();
        List<CustomEnchantDef> results = new ArrayList<>();
        for (CustomEnchantDef def : ENCHANTMENTS.values()) {
            if (def.id.contains(q) || def.name.toLowerCase().contains(q) || def.tier.name().toLowerCase().contains(q) || def.appliesTo.toLowerCase().contains(q)) {
                results.add(def);
            }
        }
        return results;
    }

    private static void registerCoreDefinitions() {
        // Simple Tier
        register("abrasion", "Abrasion", "Merusak dan mengikis ketahanan armor lawan saat diserang.", EnchantTier.SIMPLE, "Senjata Tempur", 3);
        register("adrenaline", "Adrenaline", "Memberikan dorongan Strength saat bertahan dari monster.", EnchantTier.SIMPLE, "Semua Armor", 3);
        register("alacrity", "Alacrity", "Meningkatkan kecepatan ayunan saat menggunakan alat perkakas.", EnchantTier.SIMPLE, "Alat Perkakas", 3);
        register("autosmelt", "Auto Smelt", "Melebur bijih logam secara otomatis saat ditambang.", EnchantTier.SIMPLE, "Beliung (Pickaxe)", 3);

        // Unique Tier
        register("berserk", "Berserk", "Meningkatkan kekuatan serangan fisik saat kesehatan menipis.", EnchantTier.UNIQUE, "Pedang & Kapak", 3);
        register("commander", "Commander", "Memberikan buff Haste kepada seluruh rekan tim di sekitar.", EnchantTier.UNIQUE, "Semua Armor", 3);
        register("devour", "Devour", "Peluang memulihkan barisan rasa lapar saat mengalahkan musuh.", EnchantTier.UNIQUE, "Senjata Tempur", 3);
        register("replanter", "Replanter", "Menanam kembali benih tanaman secara otomatis saat panen.", EnchantTier.UNIQUE, "Cangkul", 1);

        // Elite Tier
        register("allure", "Allure", "Menarik mob yang terkena serangan mendekat ke arah pengguna.", EnchantTier.ELITE, "Pedang", 3);
        register("antigravity", "Anti Gravity", "Memberikan lompatan super tinggi layaknya tanpa gravitasi.", EnchantTier.ELITE, "Sepatu Zirah", 3);
        register("critical", "Critical", "Meningkatkan kerusakan pukulan kritikal secara signifikan.", EnchantTier.ELITE, "Pedang", 4);
        register("veinminer", "Vein Miner", "Menambang seluruh urat bijih tambang yang tersambung sekaligus.", EnchantTier.ELITE, "Beliung (Pickaxe)", 3);

        // Ultimate Tier
        register("aegis", "Aegis", "Peluang mendapatkan lonjakan kecepatan lari saat terkena fall damage.", EnchantTier.ULTIMATE, "Sepatu Zirah", 3);
        register("angelic", "Angelic", "Memulihkan kesehatan pengguna saat menerima serangan musuh.", EnchantTier.ULTIMATE, "Semua Armor", 5);
        register("cleave", "Cleave", "Tebasan kapak menyapu yang meremukkan musuh di hadapan.", EnchantTier.ULTIMATE, "Kapak Tempur", 7);
        register("tank", "Tank", "Meningkatkan ketahanan zirah dan kapasitas darah maksimal.", EnchantTier.ULTIMATE, "Semua Armor", 4);

        // Legendary Tier
        register("abiding", "Abiding", "Menjadikan senjata tidak dapat hancur (Unbreakable).", EnchantTier.LEGENDARY, "Senjata Tempur", 1);
        register("blacksmith", "Blacksmith", "Memperbaiki ketahanan durability perlengkapan saat berhasil menyerang.", EnchantTier.LEGENDARY, "Kapak Tempur", 5);
        register("lifesteal", "Lifesteal", "Menghisap darah musuh dalam jumlah besar untuk menyembuhkan diri.", EnchantTier.LEGENDARY, "Pedang", 5);
        register("overload", "Overload", "Membuka barisan hati kesehatan tambahan secara permanen.", EnchantTier.LEGENDARY, "Semua Armor", 3);

        // Fabled Tier
        register("axeofspirits", "Axe of Spirits", "Memanggil arwah perang kuno untuk melipatgandakan daya hancur kapak.", EnchantTier.FABLED, "Kapak Tempur", 4);
        register("disarm", "Disarm", "Peluang melucuti dan melepaskan senjata utama dari lawan.", EnchantTier.FABLED, "Pedang", 3);
        register("gemify", "Gemify", "Peluang mengubah bijih tambang langsung menjadi blok mineral padat.", EnchantTier.FABLED, "Beliung (Pickaxe)", 3);
        register("unbreakable", "Unbreakable", "Menjadikan alat perkakas tambang abadi dan tidak dapat hancur.", EnchantTier.FABLED, "Alat Perkakas", 1);

        // Heroic Tier
        register("alienimplants", "Alien Implants", "Regenerasi kesehatan super cepat berteknologi biokimia alien.", EnchantTier.HEROIC, "Helm Zirah", 3);
        register("atomicdetonate", "Atomic Detonate", "Memicu ledakan atom berkekuatan raksasa saat musuh tereliminasi.", EnchantTier.HEROIC, "Alat Perkakas", 4);
        register("godlyoverload", "Godly Overload", "Melipatgandakan jumlah maksimal HP dan perisai energi permanen.", EnchantTier.HEROIC, "Semua Armor", 3);
        register("paladinarmored", "Paladin Armored", "Armor suci Paladin yang memitigasi sebagian besar damage fisik & sihir.", EnchantTier.HEROIC, "Semua Armor", 4);

        // Soul Tier
        register("rogue", "Rogue", "Memberikan hingga 2.0x lipat damage mematikan dari belakang lawan.", EnchantTier.SOUL, "Kapak Tempur", 3);
        register("sabotage", "Sabotage", "Melumpuhkan mekanisme roket pelarian dan teleportasi mutiara lawan.", EnchantTier.SOUL, "Pedang", 5);
        register("natureswrath", "Natures Wrath", "Melepaskan kemurkaan alam yang membekukan dan mementalkan lawan.", EnchantTier.SOUL, "Semua Armor", 4);
        register("teleblock", "Teleblock", "Mencegah musuh menggunakan Ender Pearl atau teleportasi untuk kabur.", EnchantTier.SOUL, "Busur Panah", 5);
        register("soultrap", "Soul Trap", "Mengunci dan menonaktifkan seluruh sihir jiwa lawan saat diserang.", EnchantTier.SOUL, "Senjata Tempur", 3);
        register("divineimmolation", "Divine Immolation", "Membakar musuh dalam api suci ilahi mengabaikan Fire Resistance.", EnchantTier.SOUL, "Pedang", 4);
        register("paradox", "Paradox", "Mengubah rasa sakit menjadi kesembuhan bagi rekan tim.", EnchantTier.SOUL, "Pedang", 5);
        register("soulharden", "Soul Harden", "Mengeraskan jiwa untuk menahan true damage dan serangan penetrasi.", EnchantTier.SOUL, "Semua Armor", 3);
        register("soulleech", "Soul Leech", "Menghisap esensi jiwa dan kesehatan musuh saat menyerang.", EnchantTier.SOUL, "Pedang", 4);

        // Mastery Tier
        register("halloweenify", "Halloweenify", "Mengubah helm musuh menjadi labu berukir yang menghalangi pandangan.", EnchantTier.MASTERY, "Senjata Tempur", 5);
        register("markofthebeast", "Mark of the Beast", "Menandai musuh dengan segel iblis; musuh menerima damage 2x lipat.", EnchantTier.MASTERY, "Semua Armor", 6);
        register("horrify", "Horrify", "Menyebarkan teror kengerian radius 32x32 blok membingungkan lawan.", EnchantTier.MASTERY, "Semua Armor", 4);
        register("chainlifesteal", "Chain Lifesteal", "Menghisap darah berantai dari target utama ke musuh sekitarnya.", EnchantTier.MASTERY, "Zirah Dada", 5);
        register("poltergeist", "Poltergeist", "Memberikan kekebalan dari jeratan Nature's Wrath dan efek gravitasi.", EnchantTier.MASTERY, "Sepatu Zirah", 3);
        register("feigndeath", "Feign Death", "Berpura-pura mati dan menghilang sesaat saat menerima damage fatal.", EnchantTier.MASTERY, "Semua Armor", 4);
        register("soulsiphone", "Soul Siphone", "Menyerap jiwa dan durability perlengkapan musuh secara langsung.", EnchantTier.MASTERY, "Semua Armor", 4);
    }
}
