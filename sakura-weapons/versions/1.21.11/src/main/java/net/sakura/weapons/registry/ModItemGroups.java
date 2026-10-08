package net.sakura.weapons.registry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.sakura.weapons.SakuraWeaponsMod;

public class ModItemGroups {

    public static final ResourceKey<CreativeModeTab> SAKURA_TAB_KEY = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        Identifier.fromNamespaceAndPath(SakuraWeaponsMod.MOD_ID, "sakura_weapons_tab")
    );

    public static final CreativeModeTab SAKURA_TAB = Registry.register(
        BuiltInRegistries.CREATIVE_MODE_TAB,
        SAKURA_TAB_KEY,
        FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.sakura_weapons.sakura_tab"))
            .icon(() -> new ItemStack(ModItems.SAKURA_KATANA))
            .displayItems((params, output) -> {
                output.accept(ModItems.SAKURA_KATANA);
                output.accept(ModItems.SAKURA_SWORD);
                output.accept(ModItems.SAKURA_BIGSWORD);
                output.accept(ModItems.SAKURA_DAGGER);
                output.accept(ModItems.SAKURA_HAMMER);
                output.accept(ModItems.SAKURA_SPEAR);
                output.accept(ModItems.SAKURA_HALBERD);
                output.accept(ModItems.SAKURA_CLUB);
                output.accept(ModItems.SAKURA_MACE);
                output.accept(ModItems.SAKURA_GAUNTLET);
                output.accept(ModItems.SAKURA_PICKAXE);
                output.accept(ModItems.SAKURA_AXE);
                output.accept(ModItems.SAKURA_SHOVEL);
                output.accept(ModItems.SAKURA_HOE);
                output.accept(ModItems.SAKURA_BOW);
                output.accept(ModItems.SAKURA_SHIELD);
                output.accept(ModItems.SAKURA_FISHING_ROD);
                output.accept(ModItems.SAKURA_HAT);
                output.accept(ModItems.SAKURA_KEY);
                output.accept(ModItems.SAKURA_WING);

                // Pink Legacy Set
                output.accept(ModItems.PINK_LEGACY_SWORD);
                output.accept(ModItems.PINK_LEGACY_BATTLE_AXE);
                output.accept(ModItems.PINK_LEGACY_SPEAR);
                output.accept(ModItems.PINK_LEGACY_HALBERD);
                output.accept(ModItems.PINK_LEGACY_HAMMER);
                output.accept(ModItems.PINK_LEGACY_STAFF);
                output.accept(ModItems.PINK_LEGACY_BOW);
                output.accept(ModItems.PINK_LEGACY_SHIELD);
                output.accept(ModItems.PINK_LEGACY_FISHING_ROD);
                output.accept(ModItems.PINK_LEGACY_PICKAXE);
                output.accept(ModItems.PINK_LEGACY_AXE);
                output.accept(ModItems.PINK_LEGACY_SHOVEL);
                output.accept(ModItems.PINK_LEGACY_HOE);
                output.accept(ModItems.PINK_LEGACY_HELMET);
                output.accept(ModItems.PINK_LEGACY_CHESTPLATE);
                output.accept(ModItems.PINK_LEGACY_LEGGINGS);
                output.accept(ModItems.PINK_LEGACY_BOOTS);
                output.accept(ModItems.PINK_LEGACY_WINGS);
                output.accept(ModItems.PINK_LEGACY_KEY);

                // Valentine Set (21 Items)
                output.accept(ValentineItems.VALENTINE_SWORD);
                output.accept(ValentineItems.VALENTINE_AXE);
                output.accept(ValentineItems.VALENTINE_HAMMER);
                output.accept(ValentineItems.VALENTINE_SPEAR);
                output.accept(ValentineItems.VALENTINE_STAFF);
                output.accept(ValentineItems.VALENTINE_PICKAXE);
                output.accept(ValentineItems.VALENTINE_SHOVEL);
                output.accept(ValentineItems.VALENTINE_HOE);
                output.accept(ValentineItems.VALENTINE_BOW);
                output.accept(ValentineItems.VALENTINE_CROSSBOW);
                output.accept(ValentineItems.VALENTINE_SHIELD);
                output.accept(ValentineItems.VALENTINE_FISHING_ROD);
                output.accept(ValentineItems.VALENTINE_HAT);
                output.accept(ValentineItems.VALENTINE_WING);
                output.accept(ValentineItems.VALENTINE_KEY);
                output.accept(ValentineItems.VALENTINE_GRENADE);
                output.accept(ValentineItems.VALENTINE_HELMET);
                output.accept(ValentineItems.VALENTINE_CHESTPLATE);
                output.accept(ValentineItems.VALENTINE_LEGGINGS);
                output.accept(ValentineItems.VALENTINE_BOOTS);

                // Dragon Mecha Overlord Set (25 Items)
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_SWORD);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_GREAT_SWORD);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_RAPIER_SWORD);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_DAGGER);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_SPEAR);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_STAFF);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_SCYTHE);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_HAMMER);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_TRIDENT);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_AXE);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_PICKAXE);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_SHOVEL);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_HOE);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_BOW);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_CROSSBOW);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_SHIELD);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_FISHING_ROD);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_HAT);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_WING);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_WING_1);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_KEY);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_HELMET);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_CHESTPLATE);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_LEGGINGS);
                output.accept(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_BOOTS);
            })
            .build()
    );

    public static void registerItemGroups() {
        SakuraWeaponsMod.LOGGER.info("Registered Sakura Arsenal Creative Tab for Minecraft 1.21.11");
    }

    public static void initialize() {
        registerItemGroups();
    }
}
