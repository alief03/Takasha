package net.takasha.rpg.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ToolMaterial;

/**
 * RpgToolMaterials
 * Tool material tier legendaris & kosmik untuk senjata kustom Takasha RPG (CosmicPvP).
 * Memiliki durabilitas 2500, nilai efisiensi 9.0f, bonus serangan 5.0f, dan enchantability 22.
 */
public final class RpgToolMaterials {
    private RpgToolMaterials() {}

    public static final ToolMaterial COSMIC = new ToolMaterial(
        BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
        2500,
        9.0f,
        5.0f,
        22,
        ItemTags.NETHERITE_TOOL_MATERIALS
    );
}
