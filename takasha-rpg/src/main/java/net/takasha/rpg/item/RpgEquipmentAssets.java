package net.takasha.rpg.item;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.takasha.rpg.TakashaRpgMod;

public final class RpgEquipmentAssets {
    private RpgEquipmentAssets() {}

    public static final ResourceKey<EquipmentAsset> PALADIN =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "paladin"));

    public static final ResourceKey<EquipmentAsset> PHANTOM =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "phantom"));

    public static final ResourceKey<EquipmentAsset> NECROMANCER =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "necromancer"));

    public static final ResourceKey<EquipmentAsset> BERSERKER =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "berserker"));

    public static final ResourceKey<EquipmentAsset> RANGER =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "ranger"));

    public static final ResourceKey<EquipmentAsset> GUARDIAN =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "guardian"));

    public static final ResourceKey<EquipmentAsset> VOIDWALKER =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "voidwalker"));
}
