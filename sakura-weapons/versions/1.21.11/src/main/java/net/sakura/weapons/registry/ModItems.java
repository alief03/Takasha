package net.sakura.weapons.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.Equippable;
import net.sakura.weapons.SakuraWeaponsMod;
import net.sakura.weapons.item.SakuraToolMaterial;
import net.sakura.weapons.item.custom.SakuraBigswordItem;
import net.sakura.weapons.item.custom.SakuraBowItem;
import net.sakura.weapons.item.custom.SakuraDaggerItem;
import net.sakura.weapons.item.custom.SakuraHammerItem;
import net.sakura.weapons.item.custom.SakuraKatanaItem;
import net.sakura.weapons.item.custom.SakuraShieldItem;


import java.util.function.Function;

public class ModItems {

    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SakuraWeaponsMod.MOD_ID, name));
        T item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    // Weapons
    public static final Item SAKURA_SWORD = register("sakura_sword", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 3.0f, -2.4f));

    public static final Item SAKURA_KATANA = register("sakura_katana", SakuraKatanaItem::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 4.0f, -2.0f));

    public static final Item SAKURA_BIGSWORD = register("sakura_bigsword", SakuraBigswordItem::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 8.0f, -3.2f));

    public static final Item SAKURA_DAGGER = register("sakura_dagger", SakuraDaggerItem::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 1.5f, -1.2f));

    public static final Item SAKURA_HAMMER = register("sakura_hammer", SakuraHammerItem::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 9.0f, -3.4f));

    public static final Item SAKURA_SPEAR = register("sakura_spear", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 4.5f, -2.6f));

    public static final Item SAKURA_HALBERD = register("sakura_halberd", Item::new,
        new Item.Properties().axe(SakuraToolMaterial.SAKURA, 6.0f, -3.1f));

    public static final Item SAKURA_CLUB = register("sakura_club", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 5.0f, -2.8f));

    public static final Item SAKURA_MACE = register("sakura_mace", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 6.0f, -3.0f));

    public static final Item SAKURA_GAUNTLET = register("sakura_gauntlet", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 2.0f, -1.5f));

    // Standard Tools
    public static final Item SAKURA_PICKAXE = register("sakura_pickaxe", Item::new,
        new Item.Properties().pickaxe(SakuraToolMaterial.SAKURA, 1.0f, -2.8f));

    public static final Item SAKURA_AXE = register("sakura_axe", Item::new,
        new Item.Properties().axe(SakuraToolMaterial.SAKURA, 5.0f, -3.0f));

    public static final Item SAKURA_SHOVEL = register("sakura_shovel", Item::new,
        new Item.Properties().shovel(SakuraToolMaterial.SAKURA, 1.5f, -3.0f));

    public static final Item SAKURA_HOE = register("sakura_hoe", Item::new,
        new Item.Properties().hoe(SakuraToolMaterial.SAKURA, -3.0f, 0.0f));

    // Utility & Defense
    public static final Item SAKURA_BOW = register("sakura_bow", SakuraBowItem::new,
        new Item.Properties().durability(500));

    public static final Item SAKURA_SHIELD = register("sakura_shield", SakuraShieldItem::new,
        SakuraShieldItem.createShieldProperties(600));

    public static final Item SAKURA_FISHING_ROD = register("sakura_fishing_rod", FishingRodItem::new,
        new Item.Properties().durability(100));

    // Cosmetics & Accessories
    public static final Item SAKURA_HAT = register("sakura_hat", Item::new,
        new Item.Properties().stacksTo(1).equippable(EquipmentSlot.HEAD));

    public static final Item SAKURA_KEY = register("sakura_key", Item::new,
        new Item.Properties().stacksTo(64));

    public static final Item SAKURA_WING = register("sakura_wing", Item::new,
        new Item.Properties().stacksTo(1).equippable(EquipmentSlot.CHEST));

    // --- PINK LEGACY SET (19 Items) ---
    public static final Item PINK_LEGACY_SWORD = register("pink_legacy_sword", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 3.0f, -2.4f));

    public static final Item PINK_LEGACY_BATTLE_AXE = register("pink_legacy_battle_axe", Item::new,
        new Item.Properties().axe(SakuraToolMaterial.SAKURA, 6.5f, -3.1f));

    public static final Item PINK_LEGACY_SPEAR = register("pink_legacy_spear", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 4.5f, -2.6f));

    public static final Item PINK_LEGACY_HALBERD = register("pink_legacy_halberd", Item::new,
        new Item.Properties().axe(SakuraToolMaterial.SAKURA, 6.0f, -3.1f));

    public static final Item PINK_LEGACY_HAMMER = register("pink_legacy_hammer", SakuraHammerItem::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 9.0f, -3.4f));

    public static final Item PINK_LEGACY_STAFF = register("pink_legacy_staff", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 3.5f, -2.2f));

    public static final Item PINK_LEGACY_BOW = register("pink_legacy_bow", SakuraBowItem::new,
        new Item.Properties().durability(500));

    public static final Item PINK_LEGACY_SHIELD = register("pink_legacy_shield", SakuraShieldItem::new,
        SakuraShieldItem.createShieldProperties(600));

    public static final Item PINK_LEGACY_FISHING_ROD = register("pink_legacy_fishing_rod", FishingRodItem::new,
        new Item.Properties().durability(100));

    public static final Item PINK_LEGACY_PICKAXE = register("pink_legacy_pickaxe", Item::new,
        new Item.Properties().pickaxe(SakuraToolMaterial.SAKURA, 1.0f, -2.8f));

    public static final Item PINK_LEGACY_AXE = register("pink_legacy_axe", Item::new,
        new Item.Properties().axe(SakuraToolMaterial.SAKURA, 5.0f, -3.0f));

    public static final Item PINK_LEGACY_SHOVEL = register("pink_legacy_shovel", Item::new,
        new Item.Properties().shovel(SakuraToolMaterial.SAKURA, 1.5f, -3.0f));

    public static final Item PINK_LEGACY_HOE = register("pink_legacy_hoe", Item::new,
        new Item.Properties().hoe(SakuraToolMaterial.SAKURA, -3.0f, 0.0f));

    public static final Item PINK_LEGACY_HELMET = register("pink_legacy_helmet", Item::new,
        new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE,
            Equippable.builder(EquipmentSlot.HEAD).setAsset(net.sakura.weapons.client.render.ModEquipmentAssets.PINK_LEGACY).build()));

    public static final Item PINK_LEGACY_CHESTPLATE = register("pink_legacy_chestplate", Item::new,
        new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE,
            Equippable.builder(EquipmentSlot.CHEST).setAsset(net.sakura.weapons.client.render.ModEquipmentAssets.PINK_LEGACY).build()));

    public static final Item PINK_LEGACY_LEGGINGS = register("pink_legacy_leggings", Item::new,
        new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE,
            Equippable.builder(EquipmentSlot.LEGS).setAsset(net.sakura.weapons.client.render.ModEquipmentAssets.PINK_LEGACY).build()));

    public static final Item PINK_LEGACY_BOOTS = register("pink_legacy_boots", Item::new,
        new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE,
            Equippable.builder(EquipmentSlot.FEET).setAsset(net.sakura.weapons.client.render.ModEquipmentAssets.PINK_LEGACY).build()));

    public static final Item PINK_LEGACY_WINGS = register("pink_legacy_wings", Item::new,
        new Item.Properties().stacksTo(1).equippable(EquipmentSlot.CHEST));

    public static final Item PINK_LEGACY_KEY = register("pink_legacy_key", Item::new,
        new Item.Properties().stacksTo(64));

    public static void registerModItems() {
        SakuraWeaponsMod.LOGGER.info("Registered 20 Sakura Weapons and 19 Pink Legacy Items for Minecraft 1.21.11");
    }

    public static void initialize() {
        registerModItems();
    }
}
