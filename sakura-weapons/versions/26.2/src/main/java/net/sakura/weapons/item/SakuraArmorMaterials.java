package net.sakura.weapons.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.sakura.weapons.client.render.ModEquipmentAssets;

import java.util.Map;

/**
 * Standardized Armor Materials for Sakura Weapons (Minecraft 26.2).
 * Provides authentic durability, defense values, toughness, knockback resistance,
 * equip sounds, repair ingredients, and equipment asset links matching source specifications.
 */
public final class SakuraArmorMaterials {
    private SakuraArmorMaterials() {}

    private static Map<ArmorType, Integer> makeDefense(int boots, int leggings, int chestplate, int helmet, int body) {
        return Map.of(
            ArmorType.BOOTS, boots,
            ArmorType.LEGGINGS, leggings,
            ArmorType.CHESTPLATE, chestplate,
            ArmorType.HELMET, helmet,
            ArmorType.BODY, body
        );
    }

    /**
     * Dragon Mecha Overlord Armor: Netherite-tier with enhanced stats.
     * Durability multiplier 37 (Helm: 407, Chest: 592, Legs: 555, Boots: 481).
     * Defense: Boots 3, Leggings 6, Chestplate 8, Helmet 3, Body 19.
     * Toughness: 3.0, Knockback Resistance: 0.1.
     */
    public static final ArmorMaterial DRAGON_MECHA_OVERLORD = new ArmorMaterial(
        37,
        makeDefense(3, 6, 8, 3, 19),
        15,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        3.0f,
        0.1f,
        ItemTags.REPAIRS_NETHERITE_ARMOR,
        ModEquipmentAssets.DRAGON_MECHA_OVERLORD
    );

    /**
     * Valentine Armor: Diamond-tier stats.
     * Durability multiplier 33 (Helm: 363, Chest: 528, Legs: 495, Boots: 429).
     * Defense: Boots 3, Leggings 6, Chestplate 8, Helmet 3, Body 11.
     * Toughness: 2.0, Knockback Resistance: 0.0.
     */
    public static final ArmorMaterial VALENTINE = new ArmorMaterial(
        33,
        makeDefense(3, 6, 8, 3, 11),
        10,
        SoundEvents.ARMOR_EQUIP_DIAMOND,
        2.0f,
        0.0f,
        ItemTags.REPAIRS_DIAMOND_ARMOR,
        ModEquipmentAssets.VALENTINE
    );

    /**
     * Pink Legacy Armor: Diamond-tier stats.
     * Durability multiplier 33 (Helm: 363, Chest: 528, Legs: 495, Boots: 429).
     * Defense: Boots 3, Leggings 6, Chestplate 8, Helmet 3, Body 11.
     * Toughness: 2.0, Knockback Resistance: 0.0.
     */
    public static final ArmorMaterial PINK_LEGACY = new ArmorMaterial(
        33,
        makeDefense(3, 6, 8, 3, 11),
        10,
        SoundEvents.ARMOR_EQUIP_DIAMOND,
        2.0f,
        0.0f,
        ItemTags.REPAIRS_DIAMOND_ARMOR,
        ModEquipmentAssets.PINK_LEGACY
    );
}
