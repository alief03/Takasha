package net.takasha.rpg.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.takasha.rpg.api.RarityTier;

import java.util.function.Consumer;

public class TakashaArmorSetItem extends Item {

    private final RarityTier tier;
    private final String setName;
    private final String setBonusDescription;

    public TakashaArmorSetItem(Properties properties, RarityTier tier, String setName, String setBonusDescription) {
        super(properties);
        this.tier = tier;
        this.setName = setName;
        this.setBonusDescription = setBonusDescription;
    }

    public RarityTier getTier() {
        return tier;
    }

    public String getSetName() {
        return setName;
    }

    public String getSetBonusDescription() {
        return setBonusDescription;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, consumer, flag);
        consumer.accept(tier.getBadge());
        consumer.accept(Component.literal("§6Set: §e" + setName + " Armor").withStyle(ChatFormatting.GOLD));
        consumer.accept(Component.literal("§7Bonus Set Penuh (4/4):").withStyle(ChatFormatting.DARK_AQUA));
        consumer.accept(Component.literal("  §f" + setBonusDescription).withStyle(ChatFormatting.WHITE));
    }
}
