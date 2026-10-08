package net.sakura.weapons.client.render;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * Registry keys for equipment assets in Minecraft 26.2 (1.21.2+).
 */
public final class ModEquipmentAssets {
    private ModEquipmentAssets() {}

    /**
     * Equipment asset for the Pink Legacy Armor set (Humanoid, Leggings, Baby).
     */
    public static final ResourceKey<EquipmentAsset> PINK_LEGACY =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("pink_legacy", "pink_legacy"));

    /**
     * Equipment asset for the Valentine Armor set (Humanoid, Leggings, Baby).
     */
    public static final ResourceKey<EquipmentAsset> VALENTINE =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("valentine", "valentine"));

    /**
     * Equipment asset for the Dragon Mecha Overlord Armor set (Humanoid, Leggings).
     */
    public static final ResourceKey<EquipmentAsset> DRAGON_MECHA_OVERLORD =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("dragon_mecha_overlord", "dragon_mecha_overlord"));
}
