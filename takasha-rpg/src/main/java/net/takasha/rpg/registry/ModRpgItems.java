package net.takasha.rpg.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.takasha.rpg.TakashaRpgMod;
import net.takasha.rpg.api.RarityTier;
import net.takasha.rpg.dungeon.DungeonTier;
import net.takasha.rpg.item.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ModRpgItems {

    public static final List<Item> ALL_RPG_ITEMS = new ArrayList<>();

    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, name));
        T item = factory.apply(properties.setId(key));
        T registered = Registry.register(BuiltInRegistries.ITEM, key, item);
        ALL_RPG_ITEMS.add(registered);
        return registered;
    }

    // 1. Multi-Currency Items
    public static final Item RPG_COIN = register("rpg_coin",
            p -> new TakashaCurrencyItem(p, RarityTier.COMMON, TakashaCurrencyItem.CurrencyType.MONEY, 1.0),
            new Item.Properties().stacksTo(64));

    public static final Item RPG_GOLD_COIN = register("rpg_gold_coin",
            p -> new TakashaCurrencyItem(p, RarityTier.UNCOMMON, TakashaCurrencyItem.CurrencyType.MONEY, 100.0),
            new Item.Properties().stacksTo(64));

    public static final Item RPG_PLATINUM_BAR = register("rpg_platinum_bar",
            p -> new TakashaCurrencyItem(p, RarityTier.RARE, TakashaCurrencyItem.CurrencyType.MONEY, 10000.0),
            new Item.Properties().stacksTo(64));

    public static final Item TAKASHA_GEM = register("takasha_gem",
            p -> new TakashaCurrencyItem(p, RarityTier.EPIC, TakashaCurrencyItem.CurrencyType.GEM, 1.0),
            new Item.Properties().stacksTo(64));

    // 2. AdvancedEnchantments Utility Scrolls & Dusts
    public static final Item WHITE_SCROLL = register("white_scroll",
            p -> new TakashaScrollItem(p, RarityTier.RARE, TakashaScrollItem.ScrollType.WHITE),
            new Item.Properties().stacksTo(16));

    public static final Item BLACK_SCROLL = register("black_scroll",
            p -> new TakashaScrollItem(p, RarityTier.EPIC, TakashaScrollItem.ScrollType.BLACK),
            new Item.Properties().stacksTo(16));

    public static final Item TRANSMUTE_SCROLL = register("transmute_scroll",
            p -> new TakashaScrollItem(p, RarityTier.RARE, TakashaScrollItem.ScrollType.TRANSMUTATION),
            new Item.Properties().stacksTo(16));

    public static final Item HOLY_SCROLL = register("holy_scroll",
            p -> new TakashaScrollItem(p, RarityTier.LEGENDARY, TakashaScrollItem.ScrollType.HOLY),
            new Item.Properties().stacksTo(8));

    public static final Item RANDOMIZATION_SCROLL = register("randomization_scroll",
            p -> new TakashaScrollItem(p, RarityTier.UNCOMMON, TakashaScrollItem.ScrollType.RANDOMIZATION),
            new Item.Properties().stacksTo(16));

    public static final Item MAGIC_DUST = register("magic_dust",
            p -> new TakashaDustItem(p, RarityTier.UNCOMMON, TakashaDustItem.DustType.MAGIC_DUST),
            new Item.Properties().stacksTo(64));

    public static final Item MYSTERY_DUST = register("mystery_dust",
            p -> new TakashaDustItem(p, RarityTier.COMMON, TakashaDustItem.DustType.MYSTERY_DUST),
            new Item.Properties().stacksTo(64));

    public static final Item SLOT_INCREASER_1 = register("slot_increaser_1",
            p -> new TakashaDustItem(p, RarityTier.RARE, TakashaDustItem.DustType.SLOT_INCREASER_1),
            new Item.Properties().stacksTo(16));

    public static final Item SLOT_INCREASER_2 = register("slot_increaser_2",
            p -> new TakashaDustItem(p, RarityTier.EPIC, TakashaDustItem.DustType.SLOT_INCREASER_2),
            new Item.Properties().stacksTo(8));

    public static final Item SLOT_INCREASER_3 = register("slot_increaser_3",
            p -> new TakashaDustItem(p, RarityTier.LEGENDARY, TakashaDustItem.DustType.SLOT_INCREASER_3),
            new Item.Properties().stacksTo(4));

    public static final Item ITEM_RENAME_TAG = register("item_rename_tag",
            p -> new TakashaDustItem(p, RarityTier.UNCOMMON, TakashaDustItem.DustType.ITEM_RENAME_TAG),
            new Item.Properties().stacksTo(16));

    public static final Item SECRET_DUST = register("secret_dust",
            p -> new TakashaDustItem(p, RarityTier.EPIC, TakashaDustItem.DustType.SECRET_DUST),
            new Item.Properties().stacksTo(64));

    public static final Item WEAPON_ENCHANTMENT_ORB = register("weapon_orb",
            p -> new TakashaOrbItem(p, RarityTier.RARE, TakashaOrbItem.OrbType.WEAPON_ORB),
            new Item.Properties().stacksTo(16));

    public static final Item ARMOR_ENCHANTMENT_ORB = register("armor_orb",
            p -> new TakashaOrbItem(p, RarityTier.RARE, TakashaOrbItem.OrbType.ARMOR_ORB),
            new Item.Properties().stacksTo(16));

    public static final Item TOOL_ENCHANTMENT_ORB = register("tool_orb",
            p -> new TakashaOrbItem(p, RarityTier.RARE, TakashaOrbItem.OrbType.TOOL_ORB),
            new Item.Properties().stacksTo(16));

    public static final Item SOUL_GEM = register("soulgem",
            p -> new TakashaOrbItem(p, RarityTier.EPIC, TakashaOrbItem.OrbType.SOUL_GEM),
            new Item.Properties().stacksTo(16));

    // Aliases for orb items
    public static final Item WEAPON_ORB = WEAPON_ENCHANTMENT_ORB;
    public static final Item ARMOR_ORB = ARMOR_ENCHANTMENT_ORB;
    public static final Item TOOL_ORB = TOOL_ENCHANTMENT_ORB;

    // 3. Stat Trackers
    public static final Item STAT_TRACKER_KILLS = register("stat_tracker_kills",
            p -> new TakashaStatTrackerItem(p, RarityTier.UNCOMMON, TakashaStatTrackerItem.TrackerType.KILLS),
            new Item.Properties().stacksTo(16));

    public static final Item STAT_TRACKER_MOBS = register("stat_tracker_mobs",
            p -> new TakashaStatTrackerItem(p, RarityTier.UNCOMMON, TakashaStatTrackerItem.TrackerType.MOBS_KILLED),
            new Item.Properties().stacksTo(16));

    public static final Item STAT_TRACKER_BLOCKS = register("stat_tracker_blocks",
            p -> new TakashaStatTrackerItem(p, RarityTier.UNCOMMON, TakashaStatTrackerItem.TrackerType.BLOCKS_MINED),
            new Item.Properties().stacksTo(16));

    public static final Item STAT_TRACKER_DAMAGE = register("stat_tracker_damage",
            p -> new TakashaStatTrackerItem(p, RarityTier.RARE, TakashaStatTrackerItem.TrackerType.DAMAGE_DEALT),
            new Item.Properties().stacksTo(16));

    public static final Item STAT_TRACKER_FISH = register("stat_tracker_fish",
            p -> new TakashaStatTrackerItem(p, RarityTier.UNCOMMON, TakashaStatTrackerItem.TrackerType.FISH_CAUGHT),
            new Item.Properties().stacksTo(16));

    // 3.4. 9 Mystery Books (Unrevealed Tomes)
    public static final Item MYSTERY_BOOK_SIMPLE = register("mystery_book_simple",
            p -> new TakashaMysteryBookItem(p, net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier.SIMPLE),
            new Item.Properties().stacksTo(16));

    public static final Item MYSTERY_BOOK_UNIQUE = register("mystery_book_unique",
            p -> new TakashaMysteryBookItem(p, net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier.UNIQUE),
            new Item.Properties().stacksTo(16));

    public static final Item MYSTERY_BOOK_ELITE = register("mystery_book_elite",
            p -> new TakashaMysteryBookItem(p, net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier.ELITE),
            new Item.Properties().stacksTo(16));

    public static final Item MYSTERY_BOOK_ULTIMATE = register("mystery_book_ultimate",
            p -> new TakashaMysteryBookItem(p, net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier.ULTIMATE),
            new Item.Properties().stacksTo(16));

    public static final Item MYSTERY_BOOK_LEGENDARY = register("mystery_book_legendary",
            p -> new TakashaMysteryBookItem(p, net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier.LEGENDARY),
            new Item.Properties().stacksTo(16));

    public static final Item MYSTERY_BOOK_FABLED = register("mystery_book_fabled",
            p -> new TakashaMysteryBookItem(p, net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier.FABLED),
            new Item.Properties().stacksTo(16));

    public static final Item MYSTERY_BOOK_HEROIC = register("mystery_book_heroic",
            p -> new TakashaMysteryBookItem(p, net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier.HEROIC),
            new Item.Properties().stacksTo(16));

    public static final Item MYSTERY_BOOK_SOUL = register("mystery_book_soul",
            p -> new TakashaMysteryBookItem(p, net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier.SOUL),
            new Item.Properties().stacksTo(16));

    public static final Item MYSTERY_BOOK_MASTERY = register("mystery_book_mastery",
            p -> new TakashaMysteryBookItem(p, net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier.MASTERY),
            new Item.Properties().stacksTo(16));

    public static Item getMysteryBookForTier(net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier tier) {
        return switch (tier) {
            case SIMPLE -> MYSTERY_BOOK_SIMPLE;
            case UNIQUE -> MYSTERY_BOOK_UNIQUE;
            case ELITE -> MYSTERY_BOOK_ELITE;
            case ULTIMATE -> MYSTERY_BOOK_ULTIMATE;
            case LEGENDARY -> MYSTERY_BOOK_LEGENDARY;
            case FABLED -> MYSTERY_BOOK_FABLED;
            case HEROIC -> MYSTERY_BOOK_HEROIC;
            case SOUL -> MYSTERY_BOOK_SOUL;
            case MASTERY -> MYSTERY_BOOK_MASTERY;
        };
    }

    // 3.5. 9 Custom Enchanted Books
    public static final Item CUSTOM_BOOK_SIMPLE = register("custom_book_simple",
            p -> new TakashaCustomBookItem(p, net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier.SIMPLE),
            new Item.Properties().stacksTo(16));

    public static final Item CUSTOM_BOOK_UNIQUE = register("custom_book_unique",
            p -> new TakashaCustomBookItem(p, net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier.UNIQUE),
            new Item.Properties().stacksTo(16));

    public static final Item CUSTOM_BOOK_ELITE = register("custom_book_elite",
            p -> new TakashaCustomBookItem(p, net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier.ELITE),
            new Item.Properties().stacksTo(16));

    public static final Item CUSTOM_BOOK_ULTIMATE = register("custom_book_ultimate",
            p -> new TakashaCustomBookItem(p, net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier.ULTIMATE),
            new Item.Properties().stacksTo(16));

    public static final Item CUSTOM_BOOK_LEGENDARY = register("custom_book_legendary",
            p -> new TakashaCustomBookItem(p, net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier.LEGENDARY),
            new Item.Properties().stacksTo(16));

    public static final Item CUSTOM_BOOK_FABLED = register("custom_book_fabled",
            p -> new TakashaCustomBookItem(p, net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier.FABLED),
            new Item.Properties().stacksTo(16));

    public static final Item CUSTOM_BOOK_HEROIC = register("custom_book_heroic",
            p -> new TakashaCustomBookItem(p, net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier.HEROIC),
            new Item.Properties().stacksTo(16));

    public static final Item CUSTOM_BOOK_SOUL = register("custom_book_soul",
            p -> new TakashaCustomBookItem(p, net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier.SOUL),
            new Item.Properties().stacksTo(16));

    public static final Item CUSTOM_BOOK_MASTERY = register("custom_book_mastery",
            p -> new TakashaCustomBookItem(p, net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier.MASTERY),
            new Item.Properties().stacksTo(16));

    // 3.6. 5 Custom Weapons (CosmicPvP)
    public static final Item DIMENSIONAL_TRAVELLER_WEAPON = register("dimensional_traveller_weapon",
            p -> new TakashaCustomWeaponItem(p, RarityTier.LEGENDARY, TakashaCustomWeaponItem.WeaponType.DIMENSIONAL_TRAVELLER),
            new Item.Properties().sword(RpgToolMaterials.COSMIC, 6.0f, -2.4f));

    public static final Item KOTH_SWORD = register("koth_sword",
            p -> new TakashaCustomWeaponItem(p, RarityTier.LEGENDARY, TakashaCustomWeaponItem.WeaponType.KOTH_SWORD),
            new Item.Properties().sword(RpgToolMaterials.COSMIC, 7.0f, -2.4f));

    public static final Item KOTH_AXE = register("koth_axe",
            p -> new TakashaCustomWeaponItem(p, RarityTier.LEGENDARY, TakashaCustomWeaponItem.WeaponType.KOTH_AXE),
            new Item.Properties().axe(RpgToolMaterials.COSMIC, 9.0f, -3.0f));

    public static final Item PHANTOM_SCYTHE = register("phantom_scythe",
            p -> new TakashaCustomWeaponItem(p, RarityTier.EPIC, TakashaCustomWeaponItem.WeaponType.PHANTOM_SCYTHE),
            new Item.Properties().sword(RpgToolMaterials.COSMIC, 6.5f, -2.2f));

    public static final Item SUPREME_WEAPON = register("supreme_weapon",
            p -> new TakashaCustomWeaponItem(p, RarityTier.LEGENDARY, TakashaCustomWeaponItem.WeaponType.SUPREME_WEAPON),
            new Item.Properties().sword(RpgToolMaterials.COSMIC, 5.5f, -2.0f));

    // Alias for DIMENSIONAL_SWORD
    public static final Item DIMENSIONAL_SWORD = DIMENSIONAL_TRAVELLER_WEAPON;

    // 4. Executable Command & Home Unlockers (Authentic RPG Custom Loot)
    // 1) Workstations
    public static final Item TOME_WORKBENCH = register("tome_workbench",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "workbench", "ANCIENT TOME", "Kitab saku pandai besi yang membuka meja kerajinan portabel kapan saja (/workbench)."),
            new Item.Properties().stacksTo(1));

    public static final Item TOME_ANVIL = register("tome_anvil",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "anvil", "ANCIENT TOME", "Kitab tempa master yang membuka paron portabel untuk memperbaiki dan menamai barang (/anvil)."),
            new Item.Properties().stacksTo(1));

    public static final Item TOME_ENDERCHEST = register("tome_enderchest",
            p -> new TakashaExecutableItem(p, RarityTier.MYTHIC, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "enderchest", "ANCIENT TOME", "Artifak dimensi kehampaan yang menghubungkan player ke peti ender dari mana saja (/enderchest)."),
            new Item.Properties().stacksTo(1));

    public static final Item TOME_DISPOSAL = register("tome_disposal",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "trash", "ANCIENT TOME", "Kitab pembuangan kehampaan yang melenyapkan item tak terpakai secara instan (/trash)."),
            new Item.Properties().stacksTo(1));

    public static final Item TOME_GRINDSTONE = register("tome_grindstone",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "grindstone", "ANCIENT TOME", "Kitab pengasah kuno untuk menghapus sihir dan memulihkan pengalaman (/grindstone)."),
            new Item.Properties().stacksTo(1));

    public static final Item TOME_STONECUTTER = register("tome_stonecutter",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "stonecutter", "ANCIENT TOME", "Kitab pemotong batu arsitek yang membuka meja pahat portabel (/stonecutter)."),
            new Item.Properties().stacksTo(1));

    public static final Item TOME_SMITHING = register("tome_smithing",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "smithingtable", "ANCIENT TOME", "Kitab tempa netherite kuno untuk meningkatkan perlengkapan secara portabel (/smithingtable)."),
            new Item.Properties().stacksTo(1));

    public static final Item TOME_CARTOGRAPHY = register("tome_cartography",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "cartographytable", "ANCIENT TOME", "Kitab kartografi penjelajah yang membuka meja pemetaan portabel (/cartographytable)."),
            new Item.Properties().stacksTo(1));

    public static final Item TOME_LOOM = register("tome_loom",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "loom", "ANCIENT TOME", "Kitab tenun penjahit yang membuka alat tenun spanduk portabel (/loom)."),
            new Item.Properties().stacksTo(1));

    // 2) Orientasi, Visual & Personalisasi
    public static final Item ASTROLABE_NAVIGATOR = register("astrolabe_navigator",
            p -> new TakashaExecutableItem(p, RarityTier.RARE, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "getpos", "NAVIGATOR RELIC", "Astrolabe navigasi pelaut yang menampilkan koordinat presisi dan orientasi kompas (/getpos)."),
            new Item.Properties().stacksTo(1));

    public static final Item PLUMB_BOB_DEPTH = register("plumb_bob_depth",
            p -> new TakashaExecutableItem(p, RarityTier.RARE, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "depth", "SURVEYOR RELIC", "Bandul kedalaman bawah tanah yang mengukur jarak vertikal terhadap permukaan laut (/depth)."),
            new Item.Properties().stacksTo(1));

    public static final Item LODESTONE_COMPASS = register("lodestone_compass",
            p -> new TakashaExecutableItem(p, RarityTier.RARE, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "compass", "NAVIGATOR RELIC", "Kompas batu magnet kuno yang mendeteksi arah mata angin cardinal jelajah (/compass)."),
            new Item.Properties().stacksTo(1));

    public static final Item CHRONO_CRYSTAL_LENS = register("chrono_crystal_lens",
            p -> new TakashaExecutableItem(p, RarityTier.EPIC, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "ptime", "CHRONOMANCY", "Lensa kristal waktu yang mengontrol persepsi visual waktu siang/malam personal (/ptime)."),
            new Item.Properties().stacksTo(1));

    public static final Item PRISM_CLEAR_SKIES = register("prism_clear_skies",
            p -> new TakashaExecutableItem(p, RarityTier.EPIC, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "pweather", "ELEMENTAL PRISM", "Prisma langit cerah yang mengatur cuaca visual personal tanpa mengganggu dunia (/pweather)."),
            new Item.Properties().stacksTo(1));

    public static final Item GOLDEN_QUILL = register("golden_quill",
            p -> new TakashaExecutableItem(p, RarityTier.RARE, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "itemname", "CALLIGRAPHY TOOL", "Pena emas kaligrafi bangsawan yang memberi nama dan warna pada item di tangan (/itemname)."),
            new Item.Properties().stacksTo(1));

    public static final Item JESTER_CROWN = register("jester_crown",
            p -> new TakashaExecutableItem(p, RarityTier.UNCOMMON, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "hat", "ROYAL ACCESSORY", "Mahkota pelawak istana yang memungkinkan memakai item/blok apapun sebagai topi (/hat)."),
            new Item.Properties().stacksTo(1));

    public static final Item TALISMAN_DEPARTURE = register("talisman_departure",
            p -> new TakashaExecutableItem(p, RarityTier.RARE, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "suicide", "SACRED TALISMAN", "Jimat kerelaan jiwa untuk melepaskan raga dan respawn darurat ke titik awal (/suicide)."),
            new Item.Properties().stacksTo(1));

    // 3) Hearth Expansion & Survival Teleportasi
    public static final Item ANCIENT_KEYSTONE = register("ancient_keystone",
            p -> new TakashaExecutableItem(p, RarityTier.MYTHIC, TakashaExecutableItem.ExecutableType.HOME_EXPANSION,
                    "sethome", "MYSTIC HEARTH", "Batu Kunci Kuno yang memperluas batas maksimal penyimpanan koordinat rumah player (+1 Slot Home, Maksimal 10)."),
            new Item.Properties().stacksTo(1));

    public static final Item WANDERER_COMPASS = register("wanderer_compass",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "tpr", "WILD COMPASS", "Kompas pengelana rimba yang menteleportasikan player ke koordinat acak alam liar yang aman (/tpr /rtp)."),
            new Item.Properties().stacksTo(1));

    public static final Item SCROLL_ASCENSION = register("scroll_ascension",
            p -> new TakashaExecutableItem(p, RarityTier.RARE, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "top", "FORBIDDEN SCROLL", "Gulungan kenaikan puncak yang menteleportasikan player langsung ke permukaan tanah tertinggi (/top)."),
            new Item.Properties().stacksTo(1));

    public static final Item SCROLL_DEEP_DESCENT = register("scroll_deep_descent",
            p -> new TakashaExecutableItem(p, RarityTier.RARE, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "bottom", "FORBIDDEN SCROLL", "Gulungan penyelaman palung yang menteleportasikan player ke lantai gua terdasar di bawah (/bottom)."),
            new Item.Properties().stacksTo(1));

    public static final Item EYE_VOID_WALKER = register("eye_void_walker",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "jump", "VOID EYE", "Mata penjelajah kehampaan yang menteleportasikan tubuh ke titik blok yang sedang ditatap (/jump)."),
            new Item.Properties().stacksTo(1));

    // 4) Pemulihan, Utilitas & Ekonomi Survival
    public static final Item SIGIL_GOURMET = register("sigil_gourmet",
            p -> new TakashaExecutableItem(p, RarityTier.MYTHIC, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "feed", "SACRED SIGIL", "Segel kemakmuran abadi yang memulihkan rasa lapar dan saturasi player seketika (/feed)."),
            new Item.Properties().stacksTo(1));

    public static final Item SIGIL_VITALITY = register("sigil_vitality",
            p -> new TakashaExecutableItem(p, RarityTier.MYTHIC, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "heal", "SACRED SIGIL", "Segel suci pemulihan mutlak yang meregenerasi darah dan membersihkan status buruk (/heal)."),
            new Item.Properties().stacksTo(1));

    public static final Item TEAR_FROSTBORN = register("tear_frostborn",
            p -> new TakashaExecutableItem(p, RarityTier.UNCOMMON, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "ext", "FROST RELIC", "Air mata es beku abadi yang memadamkan api yang membakar tubuh seketika (/ext)."),
            new Item.Properties().stacksTo(1));

    public static final Item DREAMCATCHER_CHARM = register("dreamcatcher_charm",
            p -> new TakashaExecutableItem(p, RarityTier.RARE, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "rest", "MYSTIC CHARM", "Jimat penangkap mimpi yang mereset statistik insomnia tanpa harus tidur (/rest)."),
            new Item.Properties().stacksTo(1));

    public static final Item HAMMER_MASTER_SMITH = register("hammer_master_smith",
            p -> new TakashaExecutableItem(p, RarityTier.MYTHIC, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "repair", "SMITHING RELIC", "Palu tempa sang master yang memulihkan daya tahan item di tangan seperti baru (/repair /fix)."),
            new Item.Properties().stacksTo(1));

    public static final Item HAMMER_DIVINE_SMITH = register("hammer_divine_smith",
            p -> new TakashaExecutableItem(p, RarityTier.MYTHIC, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "repair_all", "repair", "DIVINE SMITHING RELIC", "Palu tempa suci dewata yang memulihkan seluruh item dan perlengkapan di inventaris sekaligus (/repair all /fix all). Butuh akses /repair."),
            new Item.Properties().stacksTo(1));

    public static final Item COMPACTOR_SCROLL = register("compactor_scroll",
            p -> new TakashaExecutableItem(p, RarityTier.RARE, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "condense", "FORBIDDEN SCROLL", "Gulungan kompresi yang memadatkan batangan mineral di tas menjadi blok secara otomatis (/condense)."),
            new Item.Properties().stacksTo(1));

    public static final Item MERCHANTS_LEDGER = register("merchants_ledger",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "sell", "MERCANTILE RELIC", "Buku besar saudagar emas yang menjual item di tangan langsung ke pasar ekonomi (/sell)."),
            new Item.Properties().stacksTo(1));

    // 5) AdvancedEnchantments Additional Unlocks
    public static final Item CODEX_ENCHANTER = register("codex_enchanter",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "ce", "ENCHANTING CODEX", "Kitab sihir kuno yang membuka akses pasar enchantment rahasia Enchanter (/ce)."),
            new Item.Properties().stacksTo(1));

    public static final Item TOME_TINKERER = register("tome_tinkerer",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "tinker", "ANCIENT TOME", "Buku panduan pengrajin untuk membongkar perlengkapan sihir menjadi Secret Dust (/tinker)."),
            new Item.Properties().stacksTo(1));

    public static final Item SCROLL_ALCHEMIST = register("scroll_alchemist",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "alchem", "FORBIDDEN SCROLL", "Resep alkimia master untuk menggabungkan serbuk dan gulungan menjadi kualitas tinggi (/alchem)."),
            new Item.Properties().stacksTo(1));

    public static final Item SIGIL_SOUL_EXTRACTION = register("sigil_soul_extraction",
            p -> new TakashaExecutableItem(p, RarityTier.MYTHIC, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "withdrawsouls", "SACRED SIGIL", "Segel terlarang untuk menarik dan mengkristalkan energi jiwa ke bentuk fisik (/withdrawsouls)."),
            new Item.Properties().stacksTo(1));

    // Backward compatibility aliases
    public static final Item HOME_EXPANSION_SCROLL = ANCIENT_KEYSTONE;
    public static final Item TOME_POCKET_CRAFTING = TOME_WORKBENCH;
    public static final Item SCROLL_DISPOSAL = TOME_DISPOSAL;
    public static final Item CODEX_VOID_ENDER = TOME_ENDERCHEST;
    public static final Item SIGIL_GOURMET_FEAST = SIGIL_GOURMET;
    public static final Item SIGIL_VITALITY_RECOVERY = SIGIL_VITALITY;

    // 5. 7 AdvancedEnchantments Authentic Armor Sets (28 Armor Pieces)
    // 1. Dimensional Traveller Set (+30% damage, Dimensional Shift Passive Ability)
    public static final Item DIMENSIONAL_TRAVELLER_HELMET = register("dimensional_traveller_helmet",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "DimensionalTraveller", "+30% Damage & Kemampuan Pasif Dimensional Shift"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.DIMENSIONAL_TRAVELLER, ArmorType.HELMET));
    public static final Item DIMENSIONAL_TRAVELLER_CHESTPLATE = register("dimensional_traveller_chestplate",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "DimensionalTraveller", "+30% Damage & Kemampuan Pasif Dimensional Shift"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.DIMENSIONAL_TRAVELLER, ArmorType.CHESTPLATE));
    public static final Item DIMENSIONAL_TRAVELLER_LEGGINGS = register("dimensional_traveller_leggings",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "DimensionalTraveller", "+30% Damage & Kemampuan Pasif Dimensional Shift"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.DIMENSIONAL_TRAVELLER, ArmorType.LEGGINGS));
    public static final Item DIMENSIONAL_TRAVELLER_BOOTS = register("dimensional_traveller_boots",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "DimensionalTraveller", "+30% Damage & Kemampuan Pasif Dimensional Shift"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.DIMENSIONAL_TRAVELLER, ArmorType.BOOTS));

    // 2. Koth Set (+20% PvP Damage, +50% PvE Damage, Auto Bless, No Fall Damage)
    public static final Item KOTH_HELMET = register("koth_helmet",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Koth", "+20% PvP Damage, +50% PvE Damage, Auto Bless & No Fall Damage"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.KOTH, ArmorType.HELMET));
    public static final Item KOTH_CHESTPLATE = register("koth_chestplate",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Koth", "+20% PvP Damage, +50% PvE Damage, Auto Bless & No Fall Damage"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.KOTH, ArmorType.CHESTPLATE));
    public static final Item KOTH_LEGGINGS = register("koth_leggings",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Koth", "+20% PvP Damage, +50% PvE Damage, Auto Bless & No Fall Damage"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.KOTH, ArmorType.LEGGINGS));
    public static final Item KOTH_BOOTS = register("koth_boots",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Koth", "+20% PvP Damage, +50% PvE Damage, Auto Bless & No Fall Damage"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.KOTH, ArmorType.BOOTS));

    // 3. Phantom Set (+25% damage to all enemies, shadow movement)
    public static final Item PHANTOM_HELMET = register("phantom_helmet",
            p -> new TakashaArmorSetItem(p, RarityTier.EPIC, "Phantom", "+25% Damage ke Seluruh Musuh & Kecepatan Phased Shadow"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.PHANTOM, ArmorType.HELMET));
    public static final Item PHANTOM_CHESTPLATE = register("phantom_chestplate",
            p -> new TakashaArmorSetItem(p, RarityTier.EPIC, "Phantom", "+25% Damage ke Seluruh Musuh & Kecepatan Phased Shadow"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.PHANTOM, ArmorType.CHESTPLATE));
    public static final Item PHANTOM_LEGGINGS = register("phantom_leggings",
            p -> new TakashaArmorSetItem(p, RarityTier.EPIC, "Phantom", "+25% Damage ke Seluruh Musuh & Kecepatan Phased Shadow"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.PHANTOM, ArmorType.LEGGINGS));
    public static final Item PHANTOM_BOOTS = register("phantom_boots",
            p -> new TakashaArmorSetItem(p, RarityTier.EPIC, "Phantom", "+25% Damage ke Seluruh Musuh & Kecepatan Phased Shadow"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.PHANTOM, ArmorType.BOOTS));

    // 4. Ranger Set (Enemy bows -25% damage, Ranger Bow +30% damage, Immune Teleblock, Gears IV)
    public static final Item RANGER_HELMET = register("ranger_helmet",
            p -> new TakashaArmorSetItem(p, RarityTier.EPIC, "Ranger", "Kebal Teleblock, +30% Damage Panah, Gears IV & Resistensi Panah Musuh 25%"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.RANGER, ArmorType.HELMET));
    public static final Item RANGER_CHESTPLATE = register("ranger_chestplate",
            p -> new TakashaArmorSetItem(p, RarityTier.EPIC, "Ranger", "Kebal Teleblock, +30% Damage Panah, Gears IV & Resistensi Panah Musuh 25%"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.RANGER, ArmorType.CHESTPLATE));
    public static final Item RANGER_LEGGINGS = register("ranger_leggings",
            p -> new TakashaArmorSetItem(p, RarityTier.EPIC, "Ranger", "Kebal Teleblock, +30% Damage Panah, Gears IV & Resistensi Panah Musuh 25%"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.RANGER, ArmorType.LEGGINGS));
    public static final Item RANGER_BOOTS = register("ranger_boots",
            p -> new TakashaArmorSetItem(p, RarityTier.EPIC, "Ranger", "Kebal Teleblock, +30% Damage Panah, Gears IV & Resistensi Panah Musuh 25%"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.RANGER, ArmorType.BOOTS));

    // 5. Supreme Set (Gears IV, No Fall/Food Loss, +15% damage, +200% Clout)
    public static final Item SUPREME_HELMET = register("supreme_helmet",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Supreme", "Gears IV, Bebas Jatuh & Lapar, +15% Damage & +200% Aura Kemuliaan"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.SUPREME, ArmorType.HELMET));
    public static final Item SUPREME_CHESTPLATE = register("supreme_chestplate",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Supreme", "Gears IV, Bebas Jatuh & Lapar, +15% Damage & +200% Aura Kemuliaan"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.SUPREME, ArmorType.CHESTPLATE));
    public static final Item SUPREME_LEGGINGS = register("supreme_leggings",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Supreme", "Gears IV, Bebas Jatuh & Lapar, +15% Damage & +200% Aura Kemuliaan"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.SUPREME, ArmorType.LEGGINGS));
    public static final Item SUPREME_BOOTS = register("supreme_boots",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Supreme", "Gears IV, Bebas Jatuh & Lapar, +15% Damage & +200% Aura Kemuliaan"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.SUPREME, ArmorType.BOOTS));

    // 6. Yeti Set (+10% damage, enemies deal 10% less damage, freezing aura)
    public static final Item YETI_HELMET = register("yeti_helmet",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Yeti", "+10% Damage Serangan, -10% Kerusakan Diterima & Aura Beku Mutlak"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.YETI, ArmorType.HELMET));
    public static final Item YETI_CHESTPLATE = register("yeti_chestplate",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Yeti", "+10% Damage Serangan, -10% Kerusakan Diterima & Aura Beku Mutlak"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.YETI, ArmorType.CHESTPLATE));
    public static final Item YETI_LEGGINGS = register("yeti_leggings",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Yeti", "+10% Damage Serangan, -10% Kerusakan Diterima & Aura Beku Mutlak"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.YETI, ArmorType.LEGGINGS));
    public static final Item YETI_BOOTS = register("yeti_boots",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Yeti", "+10% Damage Serangan, -10% Kerusakan Diterima & Aura Beku Mutlak"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.YETI, ArmorType.BOOTS));

    // 7. Yijki Set (Enemies deal 30% less damage, Revenge of Yijki Passive Ability)
    public static final Item YIJKI_HELMET = register("yijki_helmet",
            p -> new TakashaArmorSetItem(p, RarityTier.MYTHIC, "Yijki", "-30% Kerusakan Diterima & Kemampuan Pasif Revenge of Yijki"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.YIJKI, ArmorType.HELMET));
    public static final Item YIJKI_CHESTPLATE = register("yijki_chestplate",
            p -> new TakashaArmorSetItem(p, RarityTier.MYTHIC, "Yijki", "-30% Kerusakan Diterima & Kemampuan Pasif Revenge of Yijki"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.YIJKI, ArmorType.CHESTPLATE));
    public static final Item YIJKI_LEGGINGS = register("yijki_leggings",
            p -> new TakashaArmorSetItem(p, RarityTier.MYTHIC, "Yijki", "-30% Kerusakan Diterima & Kemampuan Pasif Revenge of Yijki"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.YIJKI, ArmorType.LEGGINGS));
    public static final Item YIJKI_BOOTS = register("yijki_boots",
            p -> new TakashaArmorSetItem(p, RarityTier.MYTHIC, "Yijki", "-30% Kerusakan Diterima & Kemampuan Pasif Revenge of Yijki"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.YIJKI, ArmorType.BOOTS));

    // 8. Advanced Dungeon Arena (ADA) - Materials & Rift Keystones
    public static final Item DUNGEON_CORE_FRAGMENT = register("dungeon_core_fragment",
            p -> new DungeonMaterialItem(p, RarityTier.UNCOMMON, "Pecahan inti dungeon dari monster buas."),
            new Item.Properties().stacksTo(64));

    public static final Item RUNIC_ESSENCE = register("runic_essence",
            p -> new DungeonMaterialItem(p, RarityTier.RARE, "Esensi energi kuno yang memancar dari celah dimensi."),
            new Item.Properties().stacksTo(64));

    public static final Item ABYSSAL_BINDING_THREAD = register("abyssal_binding_thread",
            p -> new DungeonMaterialItem(p, RarityTier.UNCOMMON, "Benang pengikat dimensi jurang pemisah."),
            new Item.Properties().stacksTo(64));

    public static final Item DUNGEON_KEYSTONE_COMMON = register("dungeon_keystone_common",
            p -> new DungeonRiftKeystoneItem(p, DungeonTier.COMMON),
            new Item.Properties().stacksTo(16));

    public static final Item DUNGEON_KEYSTONE_NIGHTMARE = register("dungeon_keystone_nightmare",
            p -> new DungeonRiftKeystoneItem(p, DungeonTier.NIGHTMARE),
            new Item.Properties().stacksTo(16));

    public static final Item DUNGEON_KEYSTONE_SAKURA = register("dungeon_keystone_sakura",
            p -> new DungeonRiftKeystoneItem(p, DungeonTier.SAKURA_BOSS),
            new Item.Properties().stacksTo(16));

    public static void initialize() {
        TakashaRpgMod.LOGGER.info("Registered {} Takasha-RPG items successfully!", ALL_RPG_ITEMS.size());
    }
}
