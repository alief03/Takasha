package net.takasha.rpg.world;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.takasha.rpg.registry.ModRpgItems;

public class TakashaWorldLootModifier {

    public static void initialize() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            String path = key.identifier().getPath();

            // Check if loot table is a chest in dungeons, temples, structures
            if (path.startsWith("chests/") && !path.contains("village")) {
                boolean isHighTierStructure = path.contains("ancient_city") || path.contains("end_city") || path.contains("nether_bridge") || path.contains("bastion");

                // Pool 1: Utility Scrolls, Dusts, Books & Currency (Always present in dungeons)
                LootPool.Builder utilityPool = LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1, 3))
                        .add(LootItem.lootTableItem(ModRpgItems.RPG_COIN).setWeight(30)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 16))))
                        .add(LootItem.lootTableItem(ModRpgItems.RPG_GOLD_COIN).setWeight(15)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(ModRpgItems.MYSTERY_DUST).setWeight(25)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(ModRpgItems.MAGIC_DUST).setWeight(15)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                        .add(LootItem.lootTableItem(ModRpgItems.SECRET_DUST).setWeight(10)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                        .add(LootItem.lootTableItem(ModRpgItems.WHITE_SCROLL).setWeight(8))
                        .add(LootItem.lootTableItem(ModRpgItems.TRANSMUTE_SCROLL).setWeight(6))
                        .add(LootItem.lootTableItem(ModRpgItems.RANDOMIZATION_SCROLL).setWeight(8))
                        .add(LootItem.lootTableItem(ModRpgItems.SLOT_INCREASER_1).setWeight(6))
                        .add(LootItem.lootTableItem(ModRpgItems.CUSTOM_BOOK_SIMPLE).setWeight(10))
                        .add(LootItem.lootTableItem(ModRpgItems.CUSTOM_BOOK_UNIQUE).setWeight(8))
                        .add(LootItem.lootTableItem(ModRpgItems.STAT_TRACKER_KILLS).setWeight(8))
                        .add(LootItem.lootTableItem(ModRpgItems.STAT_TRACKER_BLOCKS).setWeight(8));

                tableBuilder.withPool(utilityPool);

                // Pool 2: Rare Tomes, Gems, Tiered Books & Legendary Unlockers
                LootPool.Builder highTierPool = LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(ModRpgItems.TAKASHA_GEM).setWeight(12))
                        .add(LootItem.lootTableItem(ModRpgItems.BLACK_SCROLL).setWeight(6))
                        .add(LootItem.lootTableItem(ModRpgItems.SLOT_INCREASER_2).setWeight(5))
                        .add(LootItem.lootTableItem(ModRpgItems.SOUL_GEM).setWeight(6))
                        .add(LootItem.lootTableItem(ModRpgItems.CUSTOM_BOOK_ELITE).setWeight(8))
                        .add(LootItem.lootTableItem(ModRpgItems.CUSTOM_BOOK_ULTIMATE).setWeight(6))
                        .add(LootItem.lootTableItem(ModRpgItems.CUSTOM_BOOK_LEGENDARY).setWeight(5))
                        .add(LootItem.lootTableItem(ModRpgItems.CUSTOM_BOOK_FABLED).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.WEAPON_ORB).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.ARMOR_ORB).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.TOOL_ORB).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.ANCIENT_KEYSTONE).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.TOME_WORKBENCH).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.TOME_ANVIL).setWeight(3))
                        .add(LootItem.lootTableItem(ModRpgItems.TOME_DISPOSAL).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.TOME_GRINDSTONE).setWeight(3))
                        .add(LootItem.lootTableItem(ModRpgItems.TOME_STONECUTTER).setWeight(3))
                        .add(LootItem.lootTableItem(ModRpgItems.TOME_SMITHING).setWeight(3))
                        .add(LootItem.lootTableItem(ModRpgItems.TOME_CARTOGRAPHY).setWeight(3))
                        .add(LootItem.lootTableItem(ModRpgItems.TOME_LOOM).setWeight(3))
                        .add(LootItem.lootTableItem(ModRpgItems.ASTROLABE_NAVIGATOR).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.PLUMB_BOB_DEPTH).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.LODESTONE_COMPASS).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.CHRONO_CRYSTAL_LENS).setWeight(3))
                        .add(LootItem.lootTableItem(ModRpgItems.PRISM_CLEAR_SKIES).setWeight(3))
                        .add(LootItem.lootTableItem(ModRpgItems.GOLDEN_QUILL).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.JESTER_CROWN).setWeight(5))
                        .add(LootItem.lootTableItem(ModRpgItems.TALISMAN_DEPARTURE).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.WANDERER_COMPASS).setWeight(3))
                        .add(LootItem.lootTableItem(ModRpgItems.SCROLL_ASCENSION).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.SCROLL_DEEP_DESCENT).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.TEAR_FROSTBORN).setWeight(5))
                        .add(LootItem.lootTableItem(ModRpgItems.DREAMCATCHER_CHARM).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.COMPACTOR_SCROLL).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.CODEX_ENCHANTER).setWeight(3))
                        .add(LootItem.lootTableItem(ModRpgItems.TOME_TINKERER).setWeight(3))
                        .add(LootItem.lootTableItem(ModRpgItems.SCROLL_ALCHEMIST).setWeight(3));

                tableBuilder.withPool(highTierPool);

                // Pool 3: Mythic Sigils, Ancient Relics, Mastery Books & Weapons (High tier structures only)
                if (isHighTierStructure) {
                    LootPool.Builder mythicPool = LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0f))
                            .add(LootItem.lootTableItem(ModRpgItems.HOLY_SCROLL).setWeight(5))
                            .add(LootItem.lootTableItem(ModRpgItems.SLOT_INCREASER_3).setWeight(4))
                            .add(LootItem.lootTableItem(ModRpgItems.CUSTOM_BOOK_HEROIC).setWeight(4))
                            .add(LootItem.lootTableItem(ModRpgItems.CUSTOM_BOOK_SOUL).setWeight(4))
                            .add(LootItem.lootTableItem(ModRpgItems.CUSTOM_BOOK_MASTERY).setWeight(3))
                            .add(LootItem.lootTableItem(ModRpgItems.DIMENSIONAL_TRAVELLER_WEAPON).setWeight(2))
                            .add(LootItem.lootTableItem(ModRpgItems.KOTH_SWORD).setWeight(2))
                            .add(LootItem.lootTableItem(ModRpgItems.KOTH_AXE).setWeight(2))
                            .add(LootItem.lootTableItem(ModRpgItems.PHANTOM_SCYTHE).setWeight(3))
                            .add(LootItem.lootTableItem(ModRpgItems.SUPREME_WEAPON).setWeight(2))
                            .add(LootItem.lootTableItem(ModRpgItems.ANCIENT_KEYSTONE).setWeight(6))
                            .add(LootItem.lootTableItem(ModRpgItems.TOME_ENDERCHEST).setWeight(4))
                            .add(LootItem.lootTableItem(ModRpgItems.EYE_VOID_WALKER).setWeight(3))
                            .add(LootItem.lootTableItem(ModRpgItems.SIGIL_GOURMET).setWeight(3))
                            .add(LootItem.lootTableItem(ModRpgItems.SIGIL_VITALITY).setWeight(3))
                            .add(LootItem.lootTableItem(ModRpgItems.HAMMER_MASTER_SMITH).setWeight(3))
                            .add(LootItem.lootTableItem(ModRpgItems.HAMMER_DIVINE_SMITH).setWeight(2))
                            .add(LootItem.lootTableItem(ModRpgItems.MERCHANTS_LEDGER).setWeight(3))
                            .add(LootItem.lootTableItem(ModRpgItems.SIGIL_SOUL_EXTRACTION).setWeight(2));

                    tableBuilder.withPool(mythicPool);
                }
            }
        });
    }
}
