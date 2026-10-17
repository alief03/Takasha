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
                    .icon(() -> new ItemStack(ModRpgItems.TOME_POCKET_CRAFTING))
                    .displayItems((params, output) -> {
                        // Executable Tomes (Legendary & Mythic)
                        output.accept(ModRpgItems.HOME_EXPANSION_SCROLL);
                        output.accept(ModRpgItems.TOME_POCKET_CRAFTING);
                        output.accept(ModRpgItems.SCROLL_DISPOSAL);
                        output.accept(ModRpgItems.CODEX_ENCHANTER);
                        output.accept(ModRpgItems.TOME_TINKERER);
                        output.accept(ModRpgItems.SCROLL_ALCHEMIST);
                        output.accept(ModRpgItems.CODEX_VOID_ENDER);
                        output.accept(ModRpgItems.SIGIL_GOURMET_FEAST);
                        output.accept(ModRpgItems.SIGIL_VITALITY_RECOVERY);
                        output.accept(ModRpgItems.SIGIL_SOUL_EXTRACTION);

                        // Scrolls & Dusts
                        output.accept(ModRpgItems.WHITE_SCROLL);
                        output.accept(ModRpgItems.BLACK_SCROLL);
                        output.accept(ModRpgItems.TRANSMUTE_SCROLL);
                        output.accept(ModRpgItems.HOLY_SCROLL);
                        output.accept(ModRpgItems.RANDOMIZATION_SCROLL);
                        output.accept(ModRpgItems.MAGIC_DUST);
                        output.accept(ModRpgItems.MYSTERY_DUST);
                        output.accept(ModRpgItems.SLOT_INCREASER_1);
                        output.accept(ModRpgItems.SLOT_INCREASER_2);
                        output.accept(ModRpgItems.SLOT_INCREASER_3);
                        output.accept(ModRpgItems.ITEM_RENAME_TAG);

                        // Stat Trackers
                        output.accept(ModRpgItems.STAT_TRACKER_KILLS);
                        output.accept(ModRpgItems.STAT_TRACKER_BLOCKS);
                        output.accept(ModRpgItems.STAT_TRACKER_DAMAGE);
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
                    .icon(() -> new ItemStack(ModRpgItems.PALADIN_CHESTPLATE))
                    .displayItems((params, output) -> {
                        // Paladin
                        output.accept(ModRpgItems.PALADIN_HELMET);
                        output.accept(ModRpgItems.PALADIN_CHESTPLATE);
                        output.accept(ModRpgItems.PALADIN_LEGGINGS);
                        output.accept(ModRpgItems.PALADIN_BOOTS);

                        // Phantom
                        output.accept(ModRpgItems.PHANTOM_HELMET);
                        output.accept(ModRpgItems.PHANTOM_CHESTPLATE);
                        output.accept(ModRpgItems.PHANTOM_LEGGINGS);
                        output.accept(ModRpgItems.PHANTOM_BOOTS);

                        // Necromancer
                        output.accept(ModRpgItems.NECROMANCER_HELMET);
                        output.accept(ModRpgItems.NECROMANCER_CHESTPLATE);
                        output.accept(ModRpgItems.NECROMANCER_LEGGINGS);
                        output.accept(ModRpgItems.NECROMANCER_BOOTS);

                        // Berserker
                        output.accept(ModRpgItems.BERSERKER_HELMET);
                        output.accept(ModRpgItems.BERSERKER_CHESTPLATE);
                        output.accept(ModRpgItems.BERSERKER_LEGGINGS);
                        output.accept(ModRpgItems.BERSERKER_BOOTS);

                        // Ranger
                        output.accept(ModRpgItems.RANGER_HELMET);
                        output.accept(ModRpgItems.RANGER_CHESTPLATE);
                        output.accept(ModRpgItems.RANGER_LEGGINGS);
                        output.accept(ModRpgItems.RANGER_BOOTS);

                        // Guardian
                        output.accept(ModRpgItems.GUARDIAN_HELMET);
                        output.accept(ModRpgItems.GUARDIAN_CHESTPLATE);
                        output.accept(ModRpgItems.GUARDIAN_LEGGINGS);
                        output.accept(ModRpgItems.GUARDIAN_BOOTS);

                        // Voidwalker
                        output.accept(ModRpgItems.VOIDWALKER_HELMET);
                        output.accept(ModRpgItems.VOIDWALKER_CHESTPLATE);
                        output.accept(ModRpgItems.VOIDWALKER_LEGGINGS);
                        output.accept(ModRpgItems.VOIDWALKER_BOOTS);
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
