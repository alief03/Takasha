package net.takasha.rpg.economy.catalog;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.*;

/**
 * EnchantmentDetailHelper
 * Kamus metadata dan utilitas visual untuk seluruh 42 Enchantment resmi EconomyShopGUI / Bukkit.
 * Memberikan informasi detail nama, tingkat Romawi, target perlengkapan, dan penjelasan efek mekanika.
 */
public class EnchantmentDetailHelper {

    public record EnchantDetail(
        String bukkitKey,
        ResourceKey<Enchantment> vanillaKey,
        String englishName,
        String indonesianName,
        String appliesTo,
        String description,
        int maxLevel
    ) {}

    private static final Map<String, EnchantDetail> ENCHANTS = new HashMap<>();

    static {
        // 1. Aqua Affinity
        register("WATER_WORKER", Enchantments.AQUA_AFFINITY, "Aqua Affinity", "Afinitas Air",
            "Helm (Helmet)", "Meningkatkan kecepatan menambang blok di dalam air hingga setara kecepatan di daratan.", 1);

        // 2. Bane of Arthropods
        register("DAMAGE_ARTHROPODS", Enchantments.BANE_OF_ARTHROPODS, "Bane of Arthropods", "Pembasmi Artropoda",
            "Pedang, Kapak, Mace", "Memberikan tambahan damage besar (+2.5 per level) dan efek Slowness IV pada laba-laba, silverfish, dan endermite.", 5);

        // 3. Curse of Binding
        register("BINDING_CURSE", Enchantments.BINDING_CURSE, "Curse of Binding", "Kutukan Pengikat",
            "Armor Lengkap, Elytra, Kepala", "Kutukan: Item yang telah dipakai di badan tidak dapat dilepaskan hingga pemain tewas.", 1);

        // 4. Breach
        register("BREACH", Enchantments.BREACH, "Breach", "Penembus Zirah",
            "Mace", "Mengabaikan dan mengurangi efektivitas proteksi armor target sebesar 15% per level (hingga 60% pada level IV).", 4);

        // 5. Blast Protection
        register("PROTECTION_EXPLOSIONS", Enchantments.BLAST_PROTECTION, "Blast Protection", "Proteksi Ledakan",
            "Armor Lengkap (Helm, Chest, Legs, Boots)", "Mengurangi kerusakan akibat ledakan (Creeper, TNT, Fireball) sebesar 8% per level serta knockback ledakan.", 4);

        // 6. Channeling
        register("CHANNELING", Enchantments.CHANNELING, "Channeling", "Penyalur Petir",
            "Trident (Trisula)", "Menyambar petir dahsyat ke arah musuh yang terkena lemparan trisula di tengah cuaca badai petir.", 1);

        // 7. Density
        register("DENSITY", Enchantments.DENSITY, "Density", "Kepadatan Hantaman",
            "Mace", "Menambahkan +0.5 damage ekstra per blok ketinggian jatuh saat mendaratkan smash attack (+2.5 per level pada level V).", 5);

        // 8. Depth Strider
        register("DEPTH_STRIDER", Enchantments.DEPTH_STRIDER, "Depth Strider", "Penjelajah Kedalaman",
            "Boots (Sepatu Zirah)", "Mengurangi hambatan arus air hingga kecepatan bergerak dan berenang setara berjalan di daratan.", 3);

        // 9. Efficiency
        register("DIG_SPEED", Enchantments.EFFICIENCY, "Efficiency", "Efisiensi Tambang",
            "Alat Tambang (Pickaxe, Axe, Shovel, Hoe, Gunting)", "Meningkatkan kecepatan menghancurkan dan menambang blok secara signifikan (+45%).", 5);

        // 10. Feather Falling
        register("PROTECTION_FALL", Enchantments.FEATHER_FALLING, "Feather Falling", "Bulu Angsa",
            "Boots (Sepatu Zirah)", "Mengurangi kerusakan akibat jatuh dari ketinggian dan teleportasi ender pearl sebesar 12% per level (maks 48%).", 4);

        // 11. Fire Aspect
        register("FIRE_ASPECT", Enchantments.FIRE_ASPECT, "Fire Aspect", "Unsur Api",
            "Pedang, Mace", "Membakar target musuh selama 4 detik per level (8 detik pada level II) saat tebasan mendarat.", 2);

        // 12. Fire Protection
        register("PROTECTION_FIRE", Enchantments.FIRE_PROTECTION, "Fire Protection", "Proteksi Kobaran Api",
            "Armor Lengkap", "Mengurangi kerusakan akibat api, lahar/lava, dan magma blocks sebesar 8% per level serta memangkas durasi terbakar.", 4);

        // 13. Flame
        register("ARROW_FIRE", Enchantments.FLAME, "Flame", "Panah Berapi",
            "Busur (Bow)", "Mengubah anak panah biasa menjadi panah api membara yang membakar musuh selama 5 detik.", 1);

        // 14. Fortune
        register("LOOT_BONUS_BLOCKS", Enchantments.FORTUNE, "Fortune", "Keberuntungan Tambang",
            "Pickaxe, Shovel, Axe, Hoe", "Memberikan peluang melipatgandakan drop mineral tambang (Diamond, Coal, Redstone, Lapis) hingga 4x lipat.", 3);

        // 15. Frost Walker
        register("FROST_WALKER", Enchantments.FROST_WALKER, "Frost Walker", "Langkah Embun Beku",
            "Boots (Sepatu Zirah)", "Membekukan permukaan air di sekitar langkah kaki menjadi balok es retak dan memberikan kekebalan pada blok magma.", 2);

        // 16. Impaling
        register("IMPALING", Enchantments.IMPALING, "Impaling", "Penusuk Ombak",
            "Trident (Trisula)", "Memberikan ekstra +2.5 damage per level (+12.5 pada level V) terhadap makhluk akuatik atau target di dalam air/hujan.", 5);

        // 17. Infinity
        register("ARROW_INFINITE", Enchantments.INFINITY, "Infinity", "Panah Tak Terbatas",
            "Busur (Bow)", "Menembakkan anak panah biasa tanpa batas selama memiliki minimal 1 anak panah di dalam tas inventaris.", 1);

        // 18. Knockback
        register("KNOCKBACK", Enchantments.KNOCKBACK, "Knockback", "Hantaman Mundur",
            "Pedang", "Mementalkan musuh mundur ke belakang sejauh 3 blok per level (6 blok pada level II) saat terkena tebasan.", 2);

        // 19. Looting
        register("LOOT_BONUS_MOBS", Enchantments.LOOTING, "Looting", "Penjarah Monster",
            "Pedang", "Meningkatkan kuantitas hasil drop mob dan menaikkan drastis peluang mendapatkan item langka (Wither Skull, Blaze Rod, dll.).", 3);

        // 20. Loyalty
        register("LOYALTY", Enchantments.LOYALTY, "Loyalty", "Kesetiaan Trisula",
            "Trident (Trisula)", "Membuat trisula otomatis terbang kembali ke tangan pelempar setelah menghantam target atau permukaan tanah.", 3);

        // 21. Luck of the Sea
        register("LUCK", Enchantments.LUCK_OF_THE_SEA, "Luck of the Sea", "Keberuntungan Laut",
            "Alat Pancing (Fishing Rod)", "Meningkatkan peluang memancing peti harta karun laut sebesar +2% per level dan mengurangi peluang sampah.", 3);

        // 22. Lure
        register("LURE", Enchantments.LURE, "Lure", "Umpan Pemikat",
            "Alat Pancing (Fishing Rod)", "Mempercepat waktu tunggu umpan disambar ikan sebesar 5 detik per level (hingga 15 detik lebih cepat).", 3);

        // 23. Mending
        register("MENDING", Enchantments.MENDING, "Mending", "Pemulihan Abadi",
            "Seluruh Perlengkapan Berdurabilitas", "Menyerap orbs pengalaman (EXP) yang dipungut untuk memperbaiki durabilitas perlengkapan (2 durabilitas per EXP).", 1);

        // 24. Multishot
        register("MULTISHOT", Enchantments.MULTISHOT, "Multishot", "Tembakan Menyebar",
            "Crossbow (Busur Silang)", "Menembakkan 3 proyektil anak panah atau kembang api sekaligus dalam pola menyebar hanya dengan 1 amunisi.", 1);

        // 25. Piercing
        register("PIERCING", Enchantments.PIERCING, "Piercing", "Tembakan Penembus",
            "Crossbow (Busur Silang)", "Anak panah menembus hingga beberapa musuh sekaligus (1 musuh per level) dan panah dapat dipungut kembali.", 4);

        // 26. Power
        register("ARROW_DAMAGE", Enchantments.POWER, "Power", "Kekuatan Busur",
            "Busur (Bow)", "Meningkatkan kerusakan tembakan panah sebesar +25% x (level + 1), mencapai +150% damage ekstra pada level V.", 5);

        // 27. Projectile Protection
        register("PROTECTION_PROJECTILE", Enchantments.PROJECTILE_PROTECTION, "Projectile Protection", "Proteksi Proyektil",
            "Armor Lengkap", "Mengurangi kerusakan akibat proyektil (panah, trisula, shulker bullets, peluru ghast) sebesar 8% per level.", 4);

        // 28. Protection
        register("PROTECTION_ENVIRONMENTAL", Enchantments.PROTECTION, "Protection", "Perlindungan Mutlak",
            "Armor Lengkap", "Mengurangi kerusakan dari hampir seluruh sumber luka sebesar 4% per level per armor (hingga batas maksimal 80%).", 4);

        // 29. Punch
        register("ARROW_KNOCKBACK", Enchantments.PUNCH, "Punch", "Dorongan Panah",
            "Busur (Bow)", "Mementalkan target mundur ke belakang sejauh 3 blok per level saat terkena lesatan anak panah.", 2);

        // 30. Quick Charge
        register("QUICK_CHARGE", Enchantments.QUICK_CHARGE, "Quick Charge", "Kokang Cepat",
            "Crossbow (Busur Silang)", "Mempercepat waktu pengisian amunisi busur silang sebesar 0.25 detik per level (0.75 detik pada level III).", 3);

        // 31. Respiration
        register("OXYGEN", Enchantments.RESPIRATION, "Respiration", "Pernapasan Air",
            "Helm (Helmet)", "Memperpanjang kapasitas napas di dalam air (+15 detik per level) dan mengurangi peluang terkena luka lemas.", 3);

        // 32. Riptide
        register("RIPTIDE", Enchantments.RIPTIDE, "Riptide", "Pusaran Arus",
            "Trident (Trisula)", "Melontarkan pemain meluncur deras ke udara atau air saat melempar trisula ketika berada di dalam air atau hujan lebat.", 3);

        // 33. Sharpness
        register("DAMAGE_ALL", Enchantments.SHARPNESS, "Sharpness", "Ketajaman Bilah",
            "Pedang, Kapak, Mace", "Meningkatkan kerusakan serangan jarak dekat sebesar +0.5 x level + 0.5 (+3.0 ekstra damage pada level V).", 5);

        // 34. Silk Touch
        register("SILK_TOUCH", Enchantments.SILK_TOUCH, "Silk Touch", "Sentuhan Sutra",
            "Alat Tambang (Pickaxe, Shovel, Axe, Hoe)", "Menjatuhkan blok asli yang rapuh saat ditambang (Es, Kaca, Bijih Mentah, Sarang Lebah) tanpa pecah.", 1);

        // 35. Smite
        register("DAMAGE_UNDEAD", Enchantments.SMITE, "Smite", "Penghancur Undead",
            "Pedang, Kapak, Mace", "Memberikan tambahan +2.5 damage per level (+12.5 pada level V) terhadap monster Undead (Zombie, Skeleton, Wither).", 5);

        // 36. Soul Speed
        register("SOUL_SPEED", Enchantments.SOUL_SPEED, "Soul Speed", "Kecepatan Jiwa",
            "Boots (Sepatu Zirah)", "Meningkatkan kecepatan berlari di atas Pasir Jiwa (Soul Sand) dan Tanah Jiwa (Soul Soil) tanpa diperlambat.", 3);

        // 37. Sweeping Edge
        register("SWEEPING_EDGE", Enchantments.SWEEPING_EDGE, "Sweeping Edge", "Tebasan Menyapu",
            "Pedang", "Meningkatkan kerusakan tebasan area menyapu (Sweeping Attack) ke seluruh musuh di sekitar target sebesar +75% pada level III.", 3);

        // 38. Swift Sneak
        register("SWIFT_SNEAK", Enchantments.SWIFT_SNEAK, "Swift Sneak", "Langkah Senyap",
            "Leggings (Celana Zirah)", "Meningkatkan kecepatan berjalan saat dalam posisi mengendap (sneaking) hingga 75% dari kecepatan biasa.", 3);

        // 39. Thorns
        register("THORNS", Enchantments.THORNS, "Thorns", "Duri Pembalas",
            "Armor Lengkap", "Memberikan peluang 15% per level untuk memantulkan 1-4 poin kerusakan kembali ke penyerang saat pemain diserang.", 3);

        // 40. Unbreaking
        register("DURABILITY", Enchantments.UNBREAKING, "Unbreaking", "Ketahanan Abadi",
            "Seluruh Perlengkapan Berdurabilitas", "Memberikan peluang 75% durabilitas item tidak berkurang saat digunakan, melipatgandakan masa pakai 4x lipat.", 3);

        // 41. Curse of Vanishing
        register("VANISHING_CURSE", Enchantments.VANISHING_CURSE, "Curse of Vanishing", "Kutukan Lenyap",
            "Seluruh Perlengkapan", "Kutukan: Item akan lenyap seketika dan tidak akan pernah jatuh ke tanah saat pemain tewas.", 1);

        // 42. Wind Burst
        register("WIND_BURST", Enchantments.WIND_BURST, "Wind Burst", "Ledakan Badai Angin",
            "Mace", "Melepaskan ledakan angin kuat yang melontarkan pemain tinggi ke udara setelah smash attack berhasil, memungkinkan serangan berantai.", 3);
    }

    private static void register(String bukkitKey, ResourceKey<Enchantment> vanillaKey, String englishName,
                                 String indonesianName, String appliesTo, String description, int maxLevel) {
        EnchantDetail detail = new EnchantDetail(bukkitKey, vanillaKey, englishName, indonesianName, appliesTo, description, maxLevel);
        ENCHANTS.put(bukkitKey.toUpperCase(), detail);

        // Alias via vanilla name
        if (vanillaKey != null) {
            String path = vanillaKey.identifier().getPath().toUpperCase();
            ENCHANTS.putIfAbsent(path, detail);
        }
    }

    public static EnchantDetail getDetail(String rawString) {
        if (rawString == null || rawString.isEmpty()) return null;
        String clean = rawString.trim().toUpperCase();
        if (clean.contains(":")) {
            clean = clean.split(":", 2)[0].trim();
        }
        if (clean.startsWith("MINECRAFT:")) {
            clean = clean.substring(10);
        }
        return ENCHANTS.get(clean);
    }

    public static int parseLevel(String rawString, int fallback) {
        if (rawString == null || !rawString.contains(":")) return fallback;
        try {
            String[] parts = rawString.split(":", 2);
            return Integer.parseInt(parts[1].trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    public static String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(number);
        };
    }

    /**
     * Memasang nama, rincian lore, dan komponen sihir pada ItemStack.
     */
    public static void applyEnchantmentData(ItemStack stack, String rawEnchant, RegistryAccess registryAccess) {
        EnchantDetail detail = getDetail(rawEnchant);
        if (detail == null) return;

        int level = parseLevel(rawEnchant, 1);
        String roman = toRoman(level);

        // Pasang Custom Name
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§d§lBuku Sihir: " + detail.englishName() + " " + roman));

        // Pasang Stored Enchantments jika registry access tersedia
        if (registryAccess != null && detail.vanillaKey() != null) {
            try {
                var registry = registryAccess.lookup(Registries.ENCHANTMENT);
                if (registry.isPresent()) {
                    var holderOpt = registry.get().get(detail.vanillaKey());
                    if (holderOpt.isPresent()) {
                        Holder<Enchantment> holder = holderOpt.get();
                        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                        mutable.set(holder, level);
                        stack.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
                    }
                }
            } catch (Throwable ignored) {}
        }

        // Glint override agar selalu berkilau sihir
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    /**
     * Menghasilkan daftar baris lore rincian sihir untuk ditampilkan pada GUI Pasar.
     */
    public static List<Component> buildDetailedTooltipLore(String rawEnchant) {
        EnchantDetail detail = getDetail(rawEnchant);
        List<Component> list = new ArrayList<>();
        if (detail == null) return list;

        int level = parseLevel(rawEnchant, 1);
        String roman = toRoman(level);

        list.add(Component.literal("§8§m------------------------"));
        list.add(Component.literal("§6§l✦ RINCIAN KEKUATAN SIHIR ✦"));
        list.add(Component.literal("§7Nama: §d§l" + detail.englishName() + " " + roman + " §8(" + detail.indonesianName() + ")"));
        list.add(Component.literal("§7Tingkat: §eLevel " + level + " §7(Maksimal: §6Tier " + toRoman(detail.maxLevel()) + "§7)"));
        list.add(Component.literal("§7Diterapkan Pada: §b" + detail.appliesTo()));
        list.add(Component.literal("§7Efek Sihir:"));

        // Bungkus deskripsi jika panjang
        String desc = detail.description();
        String[] words = desc.split(" ");
        StringBuilder line = new StringBuilder("§f");
        for (String w : words) {
            if (line.length() + w.length() > 42) {
                list.add(Component.literal(line.toString()));
                line = new StringBuilder("§f").append(w).append(" ");
            } else {
                line.append(w).append(" ");
            }
        }
        if (line.length() > 2) {
            list.add(Component.literal(line.toString().trim()));
        }

        list.add(Component.literal("§8§m------------------------"));
        return list;
    }
}
