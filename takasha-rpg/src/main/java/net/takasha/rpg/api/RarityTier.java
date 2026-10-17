package net.takasha.rpg.api;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum RarityTier {
    COMMON("Common", ChatFormatting.WHITE, 1.0, 1),
    UNCOMMON("Uncommon", ChatFormatting.GREEN, 1.5, 2),
    RARE("Rare", ChatFormatting.AQUA, 2.5, 3),
    EPIC("Epic", ChatFormatting.LIGHT_PURPLE, 5.0, 4),
    LEGENDARY("Legendary", ChatFormatting.GOLD, 12.0, 5),
    MYTHIC("Mythic", ChatFormatting.RED, 30.0, 6);

    private final String displayName;
    private final ChatFormatting formatting;
    private final double priceMultiplier;
    private final int level;

    RarityTier(String displayName, ChatFormatting formatting, double priceMultiplier, int level) {
        this.displayName = displayName;
        this.formatting = formatting;
        this.priceMultiplier = priceMultiplier;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatFormatting getFormatting() {
        return formatting;
    }

    public ChatFormatting getColor() {
        return formatting;
    }

    public double getPriceMultiplier() {
        return priceMultiplier;
    }

    public int getLevel() {
        return level;
    }

    public MutableComponent getBadge() {
        return Component.literal("[" + displayName.toUpperCase() + "]")
                .withStyle(formatting, ChatFormatting.BOLD);
    }

    public MutableComponent formatName(String name) {
        return Component.literal(name).withStyle(formatting);
    }
}
