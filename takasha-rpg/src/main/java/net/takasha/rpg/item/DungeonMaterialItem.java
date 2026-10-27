package net.takasha.rpg.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.takasha.rpg.api.RarityTier;

import java.util.List;

/**
 * Item Material Dungeon untuk pembuatan Dungeon Rift Keystone.
 * Diperoleh dari drop monster hostile di Overworld dan Nether.
 */
public class DungeonMaterialItem extends Item {

    private final RarityTier tier;
    private final String description;

    public DungeonMaterialItem(Item.Properties properties, RarityTier tier, String description) {
        super(properties);
        this.tier = tier;
        this.description = description;
    }

    public RarityTier getTier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> consumer, TooltipFlag flag) {
        consumer.accept(Component.literal("§7" + description));
        consumer.accept(Component.literal(""));
        consumer.accept(Component.literal("§8Kegunaan: §fBahan Crafting Dungeon Rift Keystone"));
        super.appendHoverText(stack, context, display, consumer, flag);
    }
}
