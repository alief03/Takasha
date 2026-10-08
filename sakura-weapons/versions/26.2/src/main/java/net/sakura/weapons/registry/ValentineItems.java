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
 * Registry of 21 Valentine Animated Weapons, Tools, Armor, and Accessories.
 */
public class ValentineItems {

    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SakuraWeaponsMod.MOD_ID, name));
        T item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    // Weapons
    public static final Item VALENTINE_SWORD = register("valentine_sword", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 3.5f, -2.4f));

    public static final Item VALENTINE_AXE = register("valentine_axe", Item::new,
        new Item.Properties().axe(SakuraToolMaterial.SAKURA, 5.0f, -3.0f));

    public static final Item VALENTINE_HAMMER = register("valentine_hammer", SakuraHammerItem::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 9.0f, -3.4f));

    public static final Item VALENTINE_SPEAR = register("valentine_spear", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 4.5f, -2.6f));

    public static final Item VALENTINE_STAFF = register("valentine_staff", Item::new,
        new Item.Properties().sword(SakuraToolMaterial.SAKURA, 3.5f, -2.2f));

    // Tools
    public static final Item VALENTINE_PICKAXE = register("valentine_pickaxe", Item::new,
        new Item.Properties().pickaxe(SakuraToolMaterial.SAKURA, 1.0f, -2.8f));

    public static final Item VALENTINE_SHOVEL = register("valentine_shovel", Item::new,
        new Item.Properties().shovel(SakuraToolMaterial.SAKURA, 1.5f, -3.0f));

    public static final Item VALENTINE_HOE = register("valentine_hoe", Item::new,
        new Item.Properties().hoe(SakuraToolMaterial.SAKURA, -3.0f, 0.0f));

    // Ranged & Defense
    public static final Item VALENTINE_BOW = register("valentine_bow", SakuraBowItem::new,
        new Item.Properties().durability(500));

    public static final Item VALENTINE_CROSSBOW = register("valentine_crossbow", CrossbowItem::new,
        new Item.Properties().durability(500));

    public static final Item VALENTINE_SHIELD = register("valentine_shield", SakuraShieldItem::new,
        SakuraShieldItem.createShieldProperties(600));

    public static final Item VALENTINE_FISHING_ROD = register("valentine_fishing_rod", FishingRodItem::new,
        new Item.Properties().durability(100));

    // Cosmetics & Accessories
    public static final Item VALENTINE_HAT = register("valentine_hat", Item::new,
        new Item.Properties().stacksTo(1).equippable(EquipmentSlot.HEAD));

    public static final Item VALENTINE_WING = register("valentine_wing", Item::new,
        new Item.Properties().stacksTo(1).equippable(EquipmentSlot.CHEST));

    public static final Item VALENTINE_KEY = register("valentine_key", Item::new,
        new Item.Properties().stacksTo(64));

    public static final Item VALENTINE_GRENADE = register("valentine_grenade", Item::new,
        new Item.Properties().stacksTo(16));

    // Armor Set
    public static final Item VALENTINE_HELMET = register("valentine_helmet", Item::new,
        new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE,
            Equippable.builder(EquipmentSlot.HEAD).setAsset(ModEquipmentAssets.VALENTINE).build()));

    public static final Item VALENTINE_CHESTPLATE = register("valentine_chestplate", Item::new,
        new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE,
            Equippable.builder(EquipmentSlot.CHEST).setAsset(ModEquipmentAssets.VALENTINE).build()));

    public static final Item VALENTINE_LEGGINGS = register("valentine_leggings", Item::new,
        new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE,
            Equippable.builder(EquipmentSlot.LEGS).setAsset(ModEquipmentAssets.VALENTINE).build()));

    public static final Item VALENTINE_BOOTS = register("valentine_boots", Item::new,
        new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE,
            Equippable.builder(EquipmentSlot.FEET).setAsset(ModEquipmentAssets.VALENTINE).build()));

    public static void registerModItems() {
        SakuraWeaponsMod.LOGGER.info("Registered 20 Valentine Items for Minecraft 26.2");
    }

    public static void initialize() {
        registerModItems();
    }
}
