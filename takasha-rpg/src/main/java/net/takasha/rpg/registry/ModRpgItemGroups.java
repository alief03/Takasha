package net.takasha.rpg.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.takasha.rpg.TakashaRpgMod;

public class ModRpgItemGroups {

    public static final ResourceKey<CreativeModeTab> RPG_UTILITY_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "rpg_utility_tab")
    );

    public static final CreativeModeTab RPG_UTILITY_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            RPG_UTILITY_TAB_KEY,
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.takasha_rpg.utility_tab"))
                    .icon(() -> new ItemStack(ModRpgItems.TOME_WORKBENCH))
                    .displayItems((params, output) -> {
                        // 1) Workstations
                        output.accept(ModRpgItems.TOME_WORKBENCH);
                        output.accept(ModRpgItems.TOME_ANVIL);
                        output.accept(ModRpgItems.TOME_ENDERCHEST);
                        output.accept(ModRpgItems.TOME_DISPOSAL);
                        output.accept(ModRpgItems.TOME_GRINDSTONE);
                        output.accept(ModRpgItems.TOME_STONECUTTER);
                        output.accept(ModRpgItems.TOME_SMITHING);
                        output.accept(ModRpgItems.TOME_CARTOGRAPHY);
                        output.accept(ModRpgItems.TOME_LOOM);

                        // 2) Orientasi, Visual & Personalisasi
                        output.accept(ModRpgItems.ASTROLABE_NAVIGATOR);
                        output.accept(ModRpgItems.PLUMB_BOB_DEPTH);
                        output.accept(ModRpgItems.LODESTONE_COMPASS);
                        output.accept(ModRpgItems.CHRONO_CRYSTAL_LENS);
                        output.accept(ModRpgItems.PRISM_CLEAR_SKIES);
                        output.accept(ModRpgItems.GOLDEN_QUILL);
                        output.accept(ModRpgItems.JESTER_CROWN);
                        output.accept(ModRpgItems.TALISMAN_DEPARTURE);

                        // 3) Hearth & Survival Teleportasi
                        output.accept(ModRpgItems.ANCIENT_KEYSTONE);
                        output.accept(ModRpgItems.WANDERER_COMPASS);
                        output.accept(ModRpgItems.SCROLL_ASCENSION);
                        output.accept(ModRpgItems.SCROLL_DEEP_DESCENT);
                        output.accept(ModRpgItems.EYE_VOID_WALKER);

                        // 4) Pemulihan, Utilitas & Ekonomi
                        output.accept(ModRpgItems.SIGIL_GOURMET);
                        output.accept(ModRpgItems.SIGIL_VITALITY);
                        output.accept(ModRpgItems.TEAR_FROSTBORN);
                        output.accept(ModRpgItems.DREAMCATCHER_CHARM);
                        output.accept(ModRpgItems.HAMMER_MASTER_SMITH);
                        output.accept(ModRpgItems.HAMMER_DIVINE_SMITH);
                        output.accept(ModRpgItems.COMPACTOR_SCROLL);
                        output.accept(ModRpgItems.MERCHANTS_LEDGER);

                        // 5) AdvancedEnchantments Additional Unlocks
                        output.accept(ModRpgItems.CODEX_ENCHANTER);
                        output.accept(ModRpgItems.TOME_TINKERER);
                        output.accept(ModRpgItems.SCROLL_ALCHEMIST);
                        output.accept(ModRpgItems.SIGIL_SOUL_EXTRACTION);

                        // Scrolls & Dusts
                        output.accept(ModRpgItems.WHITE_SCROLL);
                        output.accept(ModRpgItems.BLACK_SCROLL);
                        output.accept(ModRpgItems.TRANSMUTE_SCROLL);
                        output.accept(ModRpgItems.HOLY_SCROLL);
                        output.accept(ModRpgItems.RANDOMIZATION_SCROLL);
                        output.accept(ModRpgItems.MAGIC_DUST);
                        output.accept(ModRpgItems.MYSTERY_DUST);
                        output.accept(ModRpgItems.SECRET_DUST);
                        output.accept(ModRpgItems.SLOT_INCREASER_1);
                        output.accept(ModRpgItems.SLOT_INCREASER_2);
                        output.accept(ModRpgItems.SLOT_INCREASER_3);
                        output.accept(ModRpgItems.WEAPON_ENCHANTMENT_ORB);
                        output.accept(ModRpgItems.ARMOR_ENCHANTMENT_ORB);
                        output.accept(ModRpgItems.TOOL_ENCHANTMENT_ORB);
                        output.accept(ModRpgItems.SOUL_GEM);
                        output.accept(ModRpgItems.ITEM_RENAME_TAG);

                        // Stat Trackers
                        output.accept(ModRpgItems.STAT_TRACKER_KILLS);
                        output.accept(ModRpgItems.STAT_TRACKER_MOBS);
                        output.accept(ModRpgItems.STAT_TRACKER_BLOCKS);
                        output.accept(ModRpgItems.STAT_TRACKER_DAMAGE);
                        output.accept(ModRpgItems.STAT_TRACKER_FISH);

                        // 9 Mystery Books (Unidentified Tomes)
                        output.accept(ModRpgItems.MYSTERY_BOOK_SIMPLE);
                        output.accept(ModRpgItems.MYSTERY_BOOK_UNIQUE);
                        output.accept(ModRpgItems.MYSTERY_BOOK_ELITE);
                        output.accept(ModRpgItems.MYSTERY_BOOK_ULTIMATE);
                        output.accept(ModRpgItems.MYSTERY_BOOK_LEGENDARY);
                        output.accept(ModRpgItems.MYSTERY_BOOK_FABLED);
                        output.accept(ModRpgItems.MYSTERY_BOOK_HEROIC);
                        output.accept(ModRpgItems.MYSTERY_BOOK_SOUL);
                        output.accept(ModRpgItems.MYSTERY_BOOK_MASTERY);

                        // 9 Custom Enchanted Books
                        output.accept(ModRpgItems.CUSTOM_BOOK_SIMPLE);
                        output.accept(ModRpgItems.CUSTOM_BOOK_UNIQUE);
                        output.accept(ModRpgItems.CUSTOM_BOOK_ELITE);
                        output.accept(ModRpgItems.CUSTOM_BOOK_ULTIMATE);
                        output.accept(ModRpgItems.CUSTOM_BOOK_LEGENDARY);
                        output.accept(ModRpgItems.CUSTOM_BOOK_FABLED);
                        output.accept(ModRpgItems.CUSTOM_BOOK_HEROIC);
                        output.accept(ModRpgItems.CUSTOM_BOOK_SOUL);
                        output.accept(ModRpgItems.CUSTOM_BOOK_MASTERY);

                        // 5 Custom Weapons (CosmicPvP)
                        output.accept(ModRpgItems.DIMENSIONAL_TRAVELLER_WEAPON);
                        output.accept(ModRpgItems.KOTH_SWORD);
                        output.accept(ModRpgItems.KOTH_AXE);
                        output.accept(ModRpgItems.PHANTOM_SCYTHE);
                        output.accept(ModRpgItems.SUPREME_WEAPON);

                        // Takasha RPG Dynamic Templates (Semua Tool, Senjata & Perkakas RPG Terdaftar)
                        for (net.takasha.rpg.mmo.MMOItemTemplate template : net.takasha.rpg.mmo.MMOItemManager.getAllTemplates()) {
                            ItemStack mmoItem = net.takasha.rpg.mmo.MMOItemManager.generateItemStack(template.getItemType(), template.getId(), 1);
                            if (!mmoItem.isEmpty()) {
                                output.accept(mmoItem);
                            }
                        }

                        // Advanced Dungeon Arena (ADA) - Materials & Rift Keystones
                        output.accept(ModRpgItems.DUNGEON_CORE_FRAGMENT);
                        output.accept(ModRpgItems.RUNIC_ESSENCE);
                        output.accept(ModRpgItems.ABYSSAL_BINDING_THREAD);
                        output.accept(ModRpgItems.DUNGEON_KEYSTONE_COMMON);
                        output.accept(ModRpgItems.DUNGEON_KEYSTONE_NIGHTMARE);
                        output.accept(ModRpgItems.DUNGEON_KEYSTONE_SAKURA);
                    })
                    .build()
    );

    public static final ResourceKey<CreativeModeTab> RPG_ARMOR_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "rpg_armor_tab")
    );

    public static final CreativeModeTab RPG_ARMOR_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            RPG_ARMOR_TAB_KEY,
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.takasha_rpg.armor_tab"))
                    .icon(() -> new ItemStack(ModRpgItems.DIMENSIONAL_TRAVELLER_CHESTPLATE))
                    .displayItems((params, output) -> {
                        // 1. Dimensional Traveller
                        output.accept(ModRpgItems.DIMENSIONAL_TRAVELLER_HELMET);
                        output.accept(ModRpgItems.DIMENSIONAL_TRAVELLER_CHESTPLATE);
                        output.accept(ModRpgItems.DIMENSIONAL_TRAVELLER_LEGGINGS);
                        output.accept(ModRpgItems.DIMENSIONAL_TRAVELLER_BOOTS);

                        // 2. Koth
                        output.accept(ModRpgItems.KOTH_HELMET);
                        output.accept(ModRpgItems.KOTH_CHESTPLATE);
                        output.accept(ModRpgItems.KOTH_LEGGINGS);
                        output.accept(ModRpgItems.KOTH_BOOTS);

                        // 3. Phantom
                        output.accept(ModRpgItems.PHANTOM_HELMET);
                        output.accept(ModRpgItems.PHANTOM_CHESTPLATE);
                        output.accept(ModRpgItems.PHANTOM_LEGGINGS);
                        output.accept(ModRpgItems.PHANTOM_BOOTS);

                        // 4. Ranger
                        output.accept(ModRpgItems.RANGER_HELMET);
                        output.accept(ModRpgItems.RANGER_CHESTPLATE);
                        output.accept(ModRpgItems.RANGER_LEGGINGS);
                        output.accept(ModRpgItems.RANGER_BOOTS);

                        // 5. Supreme
                        output.accept(ModRpgItems.SUPREME_HELMET);
                        output.accept(ModRpgItems.SUPREME_CHESTPLATE);
                        output.accept(ModRpgItems.SUPREME_LEGGINGS);
                        output.accept(ModRpgItems.SUPREME_BOOTS);

                        // 6. Yeti
                        output.accept(ModRpgItems.YETI_HELMET);
                        output.accept(ModRpgItems.YETI_CHESTPLATE);
                        output.accept(ModRpgItems.YETI_LEGGINGS);
                        output.accept(ModRpgItems.YETI_BOOTS);

                        // 7. Yijki
                        output.accept(ModRpgItems.YIJKI_HELMET);
                        output.accept(ModRpgItems.YIJKI_CHESTPLATE);
                        output.accept(ModRpgItems.YIJKI_LEGGINGS);
                        output.accept(ModRpgItems.YIJKI_BOOTS);
                    })
                    .build()
    );

    public static final ResourceKey<CreativeModeTab> RPG_ECONOMY_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "rpg_economy_tab")
    );

    public static final CreativeModeTab RPG_ECONOMY_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            RPG_ECONOMY_TAB_KEY,
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.takasha_rpg.economy_tab"))
                    .icon(() -> new ItemStack(ModRpgItems.TAKASHA_GEM))
                    .displayItems((params, output) -> {
                        output.accept(ModRpgItems.RPG_COIN);
                        output.accept(ModRpgItems.RPG_GOLD_COIN);
                        output.accept(ModRpgItems.RPG_PLATINUM_BAR);
                        output.accept(ModRpgItems.TAKASHA_GEM);
                    })
                    .build()
    );

    public static void initialize() {
        TakashaRpgMod.LOGGER.info("Registered Takasha-RPG creative tabs!");
    }
}
