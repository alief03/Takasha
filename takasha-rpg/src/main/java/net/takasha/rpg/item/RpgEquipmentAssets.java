package net.takasha.rpg.item;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.takasha.rpg.TakashaRpgMod;

public final class RpgEquipmentAssets {
    private RpgEquipmentAssets() {}

    public static final ResourceKey<EquipmentAsset> DIMENSIONAL_TRAVELLER =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "dimensional_traveller"));

    public static final ResourceKey<EquipmentAsset> KOTH =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "koth"));

    public static final ResourceKey<EquipmentAsset> PHANTOM =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "phantom"));

    public static final ResourceKey<EquipmentAsset> RANGER =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "ranger"));

    public static final ResourceKey<EquipmentAsset> SUPREME =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "supreme"));

    public static final ResourceKey<EquipmentAsset> YETI =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "yeti"));

    public static final ResourceKey<EquipmentAsset> YIJKI =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "yijki"));
}
