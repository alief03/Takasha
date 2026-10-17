package net.takasha.rpg.skills;

import net.minecraft.ChatFormatting;

public enum AuraSkillType {
    FARMING("Farming", "Bercocok tanam dan panen tanaman", ChatFormatting.GREEN, AuraStatType.HEALTH, AuraStatType.REGEN),
    FORAGING("Foraging", "Menebang pohon dan mengumpulkan kayu", ChatFormatting.DARK_GREEN, AuraStatType.STRENGTH, AuraStatType.TOUGHNESS),
    MINING("Mining", "Menambang bijih dan batu mineral", ChatFormatting.GRAY, AuraStatType.TOUGHNESS, AuraStatType.LUCK),
    FISHING("Fishing", "Memancing ikan dan harta karun air", ChatFormatting.BLUE, AuraStatType.LUCK, AuraStatType.HEALTH),
    COMBAT("Combat", "Bertarung melawan monster dengan senjata jarak dekat", ChatFormatting.RED, AuraStatType.STRENGTH, AuraStatType.SPEED),
    ARCHERY("Archery", "Menembak musuh menggunakan busur atau crossbow", ChatFormatting.GOLD, AuraStatType.SPEED, AuraStatType.STRENGTH),
    DEFENSE("Defense", "Menahan dan memblokir serangan musuh", ChatFormatting.DARK_AQUA, AuraStatType.TOUGHNESS, AuraStatType.HEALTH),
    AGILITY("Agility", "Berlari, melompat, dan menghindari marabahaya", ChatFormatting.AQUA, AuraStatType.SPEED, AuraStatType.REGEN),
    ALCHEMY("Alchemy", "Meramu ramuan dan elixir berkhasiat", ChatFormatting.LIGHT_PURPLE, AuraStatType.MANA, AuraStatType.MANA_REGEN),
    ENCHANTING("Enchanting", "Meningkatkan senjata dan armor dengan sihir", ChatFormatting.DARK_PURPLE, AuraStatType.WISDOM, AuraStatType.MANA),
    HEALING("Healing", "Memulihkan luka diri dan sekutu", ChatFormatting.YELLOW, AuraStatType.REGEN, AuraStatType.WISDOM);

    private final String displayName;
    private final String description;
    private final ChatFormatting color;
    private final AuraStatType primaryStat;
    private final AuraStatType secondaryStat;

    AuraSkillType(String displayName, String description, ChatFormatting color, AuraStatType primaryStat, AuraStatType secondaryStat) {
        this.displayName = displayName;
        this.description = description;
        this.color = color;
        this.primaryStat = primaryStat;
        this.secondaryStat = secondaryStat;
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

    public AuraStatType getPrimaryStat() {
        return primaryStat;
    }

    public AuraStatType getSecondaryStat() {
        return secondaryStat;
    }

    public double getXpForNextLevel(int currentLevel) {
        // Formula based on AuraSkills standard scaling
        return 100.0 * Math.pow(1.15, currentLevel - 1);
    }
}
