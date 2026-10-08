package net.sakura.weapons.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ToolMaterial;

public class SakuraToolMaterial {
    public static final ToolMaterial SAKURA = new ToolMaterial(
        BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
        1850,
        8.5f,
        4.0f,
        18,
        ItemTags.DIAMOND_TOOL_MATERIALS
    );
}
