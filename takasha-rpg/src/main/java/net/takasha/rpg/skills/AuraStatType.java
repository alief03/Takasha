package net.takasha.rpg.skills;

import net.minecraft.ChatFormatting;

public enum AuraStatType {
    STRENGTH("Strength", "Meningkatkan serangan jarak dekat dan jarak jauh", ChatFormatting.DARK_RED, "+%d Serangan Fisik"),
    HEALTH("Health", "Meningkatkan daya tahan tubuh maksimal", ChatFormatting.RED, "+%d Daya Tahan"),
    REGEN("Regeneration", "Mempercepat regenerasi tubuh secara alami", ChatFormatting.LIGHT_PURPLE, "+%d Kecepatan Pemulihan"),
    LUCK("Luck", "Meningkatkan peluang drop langka dan panen ganda", ChatFormatting.GREEN, "+%d%% Peluang Keberuntungan"),
    WISDOM("Wisdom", "Meningkatkan perolehan pengalaman (XP)", ChatFormatting.AQUA, "+%d%% Pengali Pengalaman"),
    TOUGHNESS("Toughness", "Mengurangi kerusakan serangan musuh", ChatFormatting.BLUE, "+%d Ketahanan Armor"),
    SPEED("Speed", "Meningkatkan kecepatan bergerak dan kelincahan", ChatFormatting.WHITE, "+%d%% Kelincahan"),
    MANA("Mana", "Meningkatkan kapasitas energi sihir maksimal", ChatFormatting.DARK_AQUA, "+%d Kapasitas Sihir"),
    MANA_REGEN("Mana Regen", "Mempercepat pemulihan energi sihir", ChatFormatting.DARK_BLUE, "+%d/s Pemulihan Sihir");

    private final String displayName;
    private final String description;
    private final ChatFormatting color;
    private final String format;

    AuraStatType(String displayName, String description, ChatFormatting color, String format) {
        this.displayName = displayName;
        this.description = description;
        this.color = color;
        this.format = format;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public String formatBonus(int bonus) {
        return String.format(format, bonus);
    }
}
