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

    // 3. Stat Trackers
    public static final Item STAT_TRACKER_KILLS = register("stat_tracker_kills",
            p -> new TakashaStatTrackerItem(p, RarityTier.UNCOMMON, TakashaStatTrackerItem.TrackerType.KILLS),
            new Item.Properties().stacksTo(16));

    public static final Item STAT_TRACKER_BLOCKS = register("stat_tracker_blocks",
            p -> new TakashaStatTrackerItem(p, RarityTier.UNCOMMON, TakashaStatTrackerItem.TrackerType.BLOCKS_MINED),
            new Item.Properties().stacksTo(16));

    public static final Item STAT_TRACKER_DAMAGE = register("stat_tracker_damage",
            p -> new TakashaStatTrackerItem(p, RarityTier.RARE, TakashaStatTrackerItem.TrackerType.DAMAGE_DEALT),
            new Item.Properties().stacksTo(16));

    // 4. Executable Command & Home Unlockers (STRICTLY LEGENDARY & MYTHIC)
    public static final Item HOME_EXPANSION_SCROLL = register("home_expansion_scroll",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.HOME_EXPANSION,
                    "sethome", "Gulungan kuno yang memperluas batas memori koordinat rumah player"),
            new Item.Properties().stacksTo(1));

    public static final Item TOME_POCKET_CRAFTING = register("tome_pocket_crafting",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "workbench", "Kitab saku pandai besi yang membuka meja kerajinan portabel kapan saja"),
            new Item.Properties().stacksTo(1));

    public static final Item SCROLL_DISPOSAL = register("scroll_disposal",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "trash", "Gulungan pembuangan sampah yang melenyapkan item tak terpakai secara instan"),
            new Item.Properties().stacksTo(1));

    public static final Item CODEX_ENCHANTER = register("codex_enchanter",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "ce", "Kitab sihir kuno yang membuka akses pasar enchantment rahasia Enchanter"),
            new Item.Properties().stacksTo(1));

    public static final Item TOME_TINKERER = register("tome_tinkerer",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "tinker", "Buku panduan pengrajin untuk membongkar perlengkapan sihir menjadi Secret Dust"),
            new Item.Properties().stacksTo(1));

    public static final Item SCROLL_ALCHEMIST = register("scroll_alchemist",
            p -> new TakashaExecutableItem(p, RarityTier.LEGENDARY, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "alchem", "Resep alkimia master untuk menggabungkan serbuk dan gulungan menjadi kualitas tinggi"),
            new Item.Properties().stacksTo(1));

    public static final Item CODEX_VOID_ENDER = register("codex_void_ender",
            p -> new TakashaExecutableItem(p, RarityTier.MYTHIC, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "enderchest", "Artifak dimensi kehampaan yang menghubungkan player ke peti ender dari mana saja"),
            new Item.Properties().stacksTo(1));

    public static final Item SIGIL_GOURMET_FEAST = register("sigil_gourmet_feast",
            p -> new TakashaExecutableItem(p, RarityTier.MYTHIC, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "feed", "Segel kemakmuran abadi yang memulihkan rasa lapar player secara ajaib"),
            new Item.Properties().stacksTo(1));

    public static final Item SIGIL_VITALITY_RECOVERY = register("sigil_vitality_recovery",
            p -> new TakashaExecutableItem(p, RarityTier.MYTHIC, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "heal", "Segel suci pemulihan mutlak yang meregenerasi seluruh darah dan kondisi tubuh"),
            new Item.Properties().stacksTo(1));

    public static final Item SIGIL_SOUL_EXTRACTION = register("sigil_soul_extraction",
            p -> new TakashaExecutableItem(p, RarityTier.MYTHIC, TakashaExecutableItem.ExecutableType.COMMAND_UNLOCK,
                    "withdrawsouls", "Segel terlarang untuk menarik dan mengkristalkan energi jiwa ke bentuk fisik"),
            new Item.Properties().stacksTo(1));

    // 5. 7 AdvancedEnchantments Armor Sets (28 Armor Pieces)
    // Paladin Set (Holy defense, +20% damage resistance, passive regen)
    public static final Item PALADIN_HELMET = register("paladin_helmet",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Paladin", "Holy Defense: +20% Resistensi Kerusakan & Regenerasi Darah Pasif"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.PALADIN, ArmorType.HELMET));
    public static final Item PALADIN_CHESTPLATE = register("paladin_chestplate",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Paladin", "Holy Defense: +20% Resistensi Kerusakan & Regenerasi Darah Pasif"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.PALADIN, ArmorType.CHESTPLATE));
    public static final Item PALADIN_LEGGINGS = register("paladin_leggings",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Paladin", "Holy Defense: +20% Resistensi Kerusakan & Regenerasi Darah Pasif"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.PALADIN, ArmorType.LEGGINGS));
    public static final Item PALADIN_BOOTS = register("paladin_boots",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Paladin", "Holy Defense: +20% Resistensi Kerusakan & Regenerasi Darah Pasif"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.PALADIN, ArmorType.BOOTS));

    // Phantom Set (+25% speed, +15% dodge attack chance, feather falling)
    public static final Item PHANTOM_HELMET = register("phantom_helmet",
            p -> new TakashaArmorSetItem(p, RarityTier.EPIC, "Phantom", "Shadow Step: +25% Kecepatan Gerak, +15% Peluang Menghindar & Bebas Jatuh"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.PHANTOM, ArmorType.HELMET));
    public static final Item PHANTOM_CHESTPLATE = register("phantom_chestplate",
            p -> new TakashaArmorSetItem(p, RarityTier.EPIC, "Phantom", "Shadow Step: +25% Kecepatan Gerak, +15% Peluang Menghindar & Bebas Jatuh"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.PHANTOM, ArmorType.CHESTPLATE));
    public static final Item PHANTOM_LEGGINGS = register("phantom_leggings",
            p -> new TakashaArmorSetItem(p, RarityTier.EPIC, "Phantom", "Shadow Step: +25% Kecepatan Gerak, +15% Peluang Menghindar & Bebas Jatuh"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.PHANTOM, ArmorType.LEGGINGS));
    public static final Item PHANTOM_BOOTS = register("phantom_boots",
            p -> new TakashaArmorSetItem(p, RarityTier.EPIC, "Phantom", "Shadow Step: +25% Kecepatan Gerak, +15% Peluang Menghindar & Bebas Jatuh"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.PHANTOM, ArmorType.BOOTS));

    // Necromancer Set (+15% lifesteal, wither aura pada musuh)
    public static final Item NECROMANCER_HELMET = register("necromancer_helmet",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Necromancer", "Soul Siphon: +15% Life Steal & Efek Kutukan Pembusukan pada Penyerang"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.NECROMANCER, ArmorType.HELMET));
    public static final Item NECROMANCER_CHESTPLATE = register("necromancer_chestplate",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Necromancer", "Soul Siphon: +15% Life Steal & Efek Kutukan Pembusukan pada Penyerang"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.NECROMANCER, ArmorType.CHESTPLATE));
    public static final Item NECROMANCER_LEGGINGS = register("necromancer_leggings",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Necromancer", "Soul Siphon: +15% Life Steal & Efek Kutukan Pembusukan pada Penyerang"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.NECROMANCER, ArmorType.LEGGINGS));
    public static final Item NECROMANCER_BOOTS = register("necromancer_boots",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Necromancer", "Soul Siphon: +15% Life Steal & Efek Kutukan Pembusukan pada Penyerang"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.NECROMANCER, ArmorType.BOOTS));

    // Berserker Set (Damage meningkat saat darah menipis, +3 Strength)
    public static final Item BERSERKER_HELMET = register("berserker_helmet",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Berserker", "Blood Frenzy: Kekuatan Serangan Naik hingga +50% saat HP Menipis"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.BERSERKER, ArmorType.HELMET));
    public static final Item BERSERKER_CHESTPLATE = register("berserker_chestplate",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Berserker", "Blood Frenzy: Kekuatan Serangan Naik hingga +50% saat HP Menipis"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.BERSERKER, ArmorType.CHESTPLATE));
    public static final Item BERSERKER_LEGGINGS = register("berserker_leggings",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Berserker", "Blood Frenzy: Kekuatan Serangan Naik hingga +50% saat HP Menipis"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.BERSERKER, ArmorType.LEGGINGS));
    public static final Item BERSERKER_BOOTS = register("berserker_boots",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Berserker", "Blood Frenzy: Kekuatan Serangan Naik hingga +50% saat HP Menipis"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.BERSERKER, ArmorType.BOOTS));

    // Ranger Set (Triple shot panah, draw speed +30%)
    public static final Item RANGER_HELMET = register("ranger_helmet",
            p -> new TakashaArmorSetItem(p, RarityTier.EPIC, "Ranger", "Eagle Eye: +30% Kecepatan Tarik Busur & Peluang Tembakan Panah Ganda"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.RANGER, ArmorType.HELMET));
    public static final Item RANGER_CHESTPLATE = register("ranger_chestplate",
            p -> new TakashaArmorSetItem(p, RarityTier.EPIC, "Ranger", "Eagle Eye: +30% Kecepatan Tarik Busur & Peluang Tembakan Panah Ganda"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.RANGER, ArmorType.CHESTPLATE));
    public static final Item RANGER_LEGGINGS = register("ranger_leggings",
            p -> new TakashaArmorSetItem(p, RarityTier.EPIC, "Ranger", "Eagle Eye: +30% Kecepatan Tarik Busur & Peluang Tembakan Panah Ganda"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.RANGER, ArmorType.LEGGINGS));
    public static final Item RANGER_BOOTS = register("ranger_boots",
            p -> new TakashaArmorSetItem(p, RarityTier.EPIC, "Ranger", "Eagle Eye: +30% Kecepatan Tarik Busur & Peluang Tembakan Panah Ganda"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.RANGER, ArmorType.BOOTS));

    // Guardian Set (Menahan serangan fatal 1x per 5 menit, Resistance III saat kritis)
    public static final Item GUARDIAN_HELMET = register("guardian_helmet",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Guardian", "Fortress Aegis: Menahan 1x Serangan Mematikan & Kebal saat Darah Kritis"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.GUARDIAN, ArmorType.HELMET));
    public static final Item GUARDIAN_CHESTPLATE = register("guardian_chestplate",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Guardian", "Fortress Aegis: Menahan 1x Serangan Mematikan & Kebal saat Darah Kritis"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.GUARDIAN, ArmorType.CHESTPLATE));
    public static final Item GUARDIAN_LEGGINGS = register("guardian_leggings",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Guardian", "Fortress Aegis: Menahan 1x Serangan Mematikan & Kebal saat Darah Kritis"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.GUARDIAN, ArmorType.LEGGINGS));
    public static final Item GUARDIAN_BOOTS = register("guardian_boots",
            p -> new TakashaArmorSetItem(p, RarityTier.LEGENDARY, "Guardian", "Fortress Aegis: Menahan 1x Serangan Mematikan & Kebal saat Darah Kritis"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.GUARDIAN, ArmorType.BOOTS));

    // Voidwalker Set (Teleportasi saat diserang, kebal void)
    public static final Item VOIDWALKER_HELMET = register("voidwalker_helmet",
            p -> new TakashaArmorSetItem(p, RarityTier.MYTHIC, "Voidwalker", "Ender Mastery: Kebal Kerusakan Void & Peluang Evasion Teleportasi Cepat"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.VOIDWALKER, ArmorType.HELMET));
    public static final Item VOIDWALKER_CHESTPLATE = register("voidwalker_chestplate",
            p -> new TakashaArmorSetItem(p, RarityTier.MYTHIC, "Voidwalker", "Ender Mastery: Kebal Kerusakan Void & Peluang Evasion Teleportasi Cepat"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.VOIDWALKER, ArmorType.CHESTPLATE));
    public static final Item VOIDWALKER_LEGGINGS = register("voidwalker_leggings",
            p -> new TakashaArmorSetItem(p, RarityTier.MYTHIC, "Voidwalker", "Ender Mastery: Kebal Kerusakan Void & Peluang Evasion Teleportasi Cepat"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.VOIDWALKER, ArmorType.LEGGINGS));
    public static final Item VOIDWALKER_BOOTS = register("voidwalker_boots",
            p -> new TakashaArmorSetItem(p, RarityTier.MYTHIC, "Voidwalker", "Ender Mastery: Kebal Kerusakan Void & Peluang Evasion Teleportasi Cepat"),
            new Item.Properties().humanoidArmor(RpgArmorMaterials.VOIDWALKER, ArmorType.BOOTS));

    public static void initialize() {
        TakashaRpgMod.LOGGER.info("Registered {} Takasha-RPG items successfully!", ALL_RPG_ITEMS.size());
    }
}
