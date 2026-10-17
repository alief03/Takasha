package net.takasha.rpg.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.Map;

public final class RpgArmorMaterials {
    private RpgArmorMaterials() {}

    private static Map<ArmorType, Integer> makeDefense(int boots, int leggings, int chestplate, int helmet, int body) {
        return Map.of(
            ArmorType.BOOTS, boots,
            ArmorType.LEGGINGS, leggings,
            ArmorType.CHESTPLATE, chestplate,
            ArmorType.HELMET, helmet,
            ArmorType.BODY, body
        );
    }

    public static final ArmorMaterial PALADIN = new ArmorMaterial(
        37,
        makeDefense(3, 6, 8, 3, 20),
        15,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        3.0f,
        0.1f,
        ItemTags.REPAIRS_NETHERITE_ARMOR,
        RpgEquipmentAssets.PALADIN
    );

    public static final ArmorMaterial PHANTOM = new ArmorMaterial(
        33,
        makeDefense(3, 6, 8, 3, 14),
        15,
        SoundEvents.ARMOR_EQUIP_DIAMOND,
        2.0f,
        0.0f,
        ItemTags.REPAIRS_DIAMOND_ARMOR,
        RpgEquipmentAssets.PHANTOM
    );

    public static final ArmorMaterial NECROMANCER = new ArmorMaterial(
        35,
        makeDefense(3, 6, 8, 3, 16),
        18,
        SoundEvents.ARMOR_EQUIP_DIAMOND,
        2.5f,
        0.05f,
        ItemTags.REPAIRS_DIAMOND_ARMOR,
        RpgEquipmentAssets.NECROMANCER
    );

    public static final ArmorMaterial BERSERKER = new ArmorMaterial(
        37,
        makeDefense(3, 6, 8, 3, 18),
        12,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        2.5f,
        0.1f,
        ItemTags.REPAIRS_NETHERITE_ARMOR,
        RpgEquipmentAssets.BERSERKER
    );

    public static final ArmorMaterial RANGER = new ArmorMaterial(
        33,
        makeDefense(3, 6, 8, 3, 13),
        16,
        SoundEvents.ARMOR_EQUIP_CHAIN,
        2.0f,
        0.0f,
        ItemTags.REPAIRS_CHAIN_ARMOR,
        RpgEquipmentAssets.RANGER
    );

    public static final ArmorMaterial GUARDIAN = new ArmorMaterial(
        40,
        makeDefense(4, 7, 9, 4, 24),
        10,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        4.0f,
        0.2f,
        ItemTags.REPAIRS_NETHERITE_ARMOR,
        RpgEquipmentAssets.GUARDIAN
    );

    public static final ArmorMaterial VOIDWALKER = new ArmorMaterial(
        39,
        makeDefense(3, 6, 8, 3, 22),
        20,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        3.5f,
        0.15f,
        ItemTags.REPAIRS_NETHERITE_ARMOR,
        RpgEquipmentAssets.VOIDWALKER
    );
}
