package net.sakura.weapons.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.Equippable;
import net.sakura.weapons.SakuraWeaponsMod;
import net.sakura.weapons.client.render.ModEquipmentAssets;
import net.sakura.weapons.item.SakuraToolMaterial;
import net.sakura.weapons.item.custom.SakuraBowItem;
import net.sakura.weapons.item.custom.SakuraHammerItem;
import net.sakura.weapons.item.custom.SakuraShieldItem;

import java.util.function.Function;

/**
 * Registry of 25 Dragon Mecha Overlord Animated Weapons, Tools, Armor, and Accessories.
 */
public class DragonMechaOverlordItems {

    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SakuraWeaponsMod.MOD_ID, name));
        T item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    // Weapons (9 Melee)
    public static final Item DRAGON_MECHA_OVERLORD_SWORD = register("dragon_mecha_overlord_sword", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 3.5f, -2.4f));

    public static final Item DRAGON_MECHA_OVERLORD_GREAT_SWORD = register("dragon_mecha_overlord_great_sword", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 6.0f, -3.0f));

    public static final Item DRAGON_MECHA_OVERLORD_RAPIER_SWORD = register("dragon_mecha_overlord_rapier_sword", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 3.0f, -2.0f));

    public static final Item DRAGON_MECHA_OVERLORD_DAGGER = register("dragon_mecha_overlord_dagger", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 2.0f, -1.5f));

    public static final Item DRAGON_MECHA_OVERLORD_SPEAR = register("dragon_mecha_overlord_spear", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 4.5f, -2.6f));

    public static final Item DRAGON_MECHA_OVERLORD_STAFF = register("dragon_mecha_overlord_staff", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 3.5f, -2.2f));

    public static final Item DRAGON_MECHA_OVERLORD_SCYTHE = register("dragon_mecha_overlord_scythe", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 5.0f, -2.7f));

    public static final Item DRAGON_MECHA_OVERLORD_HAMMER = register("dragon_mecha_overlord_hammer", SakuraHammerItem::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 9.0f, -3.4f));

    public static final Item DRAGON_MECHA_OVERLORD_TRIDENT = register("dragon_mecha_overlord_trident", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 4.5f, -2.5f));

    // Tools (4 Tools)
    public static final Item DRAGON_MECHA_OVERLORD_AXE = register("dragon_mecha_overlord_axe", Item::new,
        new Item.Properties().axe(SakuraToolMaterial.SAKURA, 5.0f, -3.0f));

    public static final Item DRAGON_MECHA_OVERLORD_PICKAXE = register("dragon_mecha_overlord_pickaxe", Item::new,
        new Item.Properties().pickaxe(SakuraToolMaterial.SAKURA, 1.0f, -2.8f));

    public static final Item DRAGON_MECHA_OVERLORD_SHOVEL = register("dragon_mecha_overlord_shovel", Item::new,
        new Item.Properties().shovel(SakuraToolMaterial.SAKURA, 1.5f, -3.0f));

    public static final Item DRAGON_MECHA_OVERLORD_HOE = register("dragon_mecha_overlord_hoe", Item::new,
        new Item.Properties().hoe(SakuraToolMaterial.SAKURA, -3.0f, 0.0f));

    // Ranged & Defense (4 Items)
    public static final Item DRAGON_MECHA_OVERLORD_BOW = register("dragon_mecha_overlord_bow", SakuraBowItem::new,
        new Item.Properties().durability(500));

    public static final Item DRAGON_MECHA_OVERLORD_CROSSBOW = register("dragon_mecha_overlord_crossbow", CrossbowItem::new,
        new Item.Properties().durability(500));

    public static final Item DRAGON_MECHA_OVERLORD_SHIELD = register("dragon_mecha_overlord_shield", SakuraShieldItem::new,
        SakuraShieldItem.createShieldProperties(600));

    public static final Item DRAGON_MECHA_OVERLORD_FISHING_ROD = register("dragon_mecha_overlord_fishing_rod", FishingRodItem::new,
        new Item.Properties().durability(100));

    // Cosmetics & Accessories (4 Items)
    public static final Item DRAGON_MECHA_OVERLORD_HAT = register("dragon_mecha_overlord_hat", Item::new,
        new Item.Properties().stacksTo(1).equippable(EquipmentSlot.HEAD));

    public static final Item DRAGON_MECHA_OVERLORD_WING = register("dragon_mecha_overlord_wing", Item::new,
        new Item.Properties().stacksTo(1).equippable(EquipmentSlot.CHEST));

    public static final Item DRAGON_MECHA_OVERLORD_WING_1 = register("dragon_mecha_overlord_wing_1", Item::new,
        new Item.Properties().stacksTo(1).equippable(EquipmentSlot.CHEST));

    public static final Item DRAGON_MECHA_OVERLORD_KEY = register("dragon_mecha_overlord_key", Item::new,
        new Item.Properties().stacksTo(64));

    // Armor Set (4 Items)
    public static final Item DRAGON_MECHA_OVERLORD_HELMET = register("dragon_mecha_overlord_helmet", Item::new,
        new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE,
            Equippable.builder(EquipmentSlot.HEAD).setAsset(ModEquipmentAssets.DRAGON_MECHA_OVERLORD).build()));

    public static final Item DRAGON_MECHA_OVERLORD_CHESTPLATE = register("dragon_mecha_overlord_chestplate", Item::new,
        new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE,
            Equippable.builder(EquipmentSlot.CHEST).setAsset(ModEquipmentAssets.DRAGON_MECHA_OVERLORD).build()));

    public static final Item DRAGON_MECHA_OVERLORD_LEGGINGS = register("dragon_mecha_overlord_leggings", Item::new,
        new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE,
            Equippable.builder(EquipmentSlot.LEGS).setAsset(ModEquipmentAssets.DRAGON_MECHA_OVERLORD).build()));

    public static final Item DRAGON_MECHA_OVERLORD_BOOTS = register("dragon_mecha_overlord_boots", Item::new,
        new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE,
            Equippable.builder(EquipmentSlot.FEET).setAsset(ModEquipmentAssets.DRAGON_MECHA_OVERLORD).build()));

    public static void registerModItems() {
        SakuraWeaponsMod.LOGGER.info("Registered 25 Dragon Mecha Overlord Items for Minecraft 26.2");
    }

    public static void initialize() {
        registerModItems();
    }
}
