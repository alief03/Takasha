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

                // Pool 1: Utility Scrolls, Dusts & Currency (Always present in dungeons)
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
                        .add(LootItem.lootTableItem(ModRpgItems.WHITE_SCROLL).setWeight(8))
                        .add(LootItem.lootTableItem(ModRpgItems.TRANSMUTE_SCROLL).setWeight(6))
                        .add(LootItem.lootTableItem(ModRpgItems.SLOT_INCREASER_1).setWeight(6))
                        .add(LootItem.lootTableItem(ModRpgItems.STAT_TRACKER_KILLS).setWeight(8))
                        .add(LootItem.lootTableItem(ModRpgItems.STAT_TRACKER_BLOCKS).setWeight(8));

                tableBuilder.withPool(utilityPool);

                // Pool 2: Rare Tomes, Gems & Legendary Unlockers
                LootPool.Builder highTierPool = LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(ModRpgItems.TAKASHA_GEM).setWeight(12))
                        .add(LootItem.lootTableItem(ModRpgItems.BLACK_SCROLL).setWeight(6))
                        .add(LootItem.lootTableItem(ModRpgItems.SLOT_INCREASER_2).setWeight(5))
                        .add(LootItem.lootTableItem(ModRpgItems.HOME_EXPANSION_SCROLL).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.TOME_POCKET_CRAFTING).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.SCROLL_DISPOSAL).setWeight(4))
                        .add(LootItem.lootTableItem(ModRpgItems.CODEX_ENCHANTER).setWeight(3))
                        .add(LootItem.lootTableItem(ModRpgItems.TOME_TINKERER).setWeight(3))
                        .add(LootItem.lootTableItem(ModRpgItems.SCROLL_ALCHEMIST).setWeight(3));

                tableBuilder.withPool(highTierPool);

                // Pool 3: Mythic Sigils & Ancient Relics (High tier structures only)
                if (isHighTierStructure) {
                    LootPool.Builder mythicPool = LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0f))
                            .add(LootItem.lootTableItem(ModRpgItems.HOLY_SCROLL).setWeight(5))
                            .add(LootItem.lootTableItem(ModRpgItems.SLOT_INCREASER_3).setWeight(4))
                            .add(LootItem.lootTableItem(ModRpgItems.CODEX_VOID_ENDER).setWeight(3))
                            .add(LootItem.lootTableItem(ModRpgItems.SIGIL_GOURMET_FEAST).setWeight(2))
                            .add(LootItem.lootTableItem(ModRpgItems.SIGIL_VITALITY_RECOVERY).setWeight(2))
                            .add(LootItem.lootTableItem(ModRpgItems.SIGIL_SOUL_EXTRACTION).setWeight(2));

                    tableBuilder.withPool(mythicPool);
                }
            }
        });
    }
}
