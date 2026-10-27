package net.takasha.rpg.skills;

import net.minecraft.ChatFormatting;

public enum AuraSkillType {
    FARMING("Farming", "Bercocok tanam dan panen tanaman", ChatFormatting.GREEN, AuraStatType.HEALTH, AuraStatType.STRENGTH),
    FORAGING("Foraging", "Menebang pohon dan mengumpulkan kayu", ChatFormatting.DARK_GREEN, AuraStatType.STRENGTH, AuraStatType.TOUGHNESS),
    MINING("Mining", "Menambang bijih dan batu mineral", ChatFormatting.GRAY, AuraStatType.TOUGHNESS, AuraStatType.LUCK),
    FISHING("Fishing", "Memancing ikan dan harta karun air", ChatFormatting.BLUE, AuraStatType.LUCK, AuraStatType.HEALTH),
    EXCAVATION("Excavation", "Menggali tanah, pasir, kerikil, dan lempung", ChatFormatting.GOLD, AuraStatType.REGENERATION, AuraStatType.STRENGTH),
    ARCHERY("Archery", "Menembak musuh menggunakan busur atau crossbow", ChatFormatting.DARK_AQUA, AuraStatType.CRIT_CHANCE, AuraStatType.STRENGTH),
    DEFENSE("Defense", "Menahan dan memblokir serangan musuh", ChatFormatting.BLUE, AuraStatType.TOUGHNESS, AuraStatType.HEALTH),
    FIGHTING("Fighting", "Bertarung melawan monster dengan senjata jarak dekat", ChatFormatting.RED, AuraStatType.CRIT_DAMAGE, AuraStatType.REGENERATION),
    AGILITY("Agility", "Berlari, melompat, dan menghindari marabahaya", ChatFormatting.AQUA, AuraStatType.REGENERATION, AuraStatType.WISDOM),
    ALCHEMY("Alchemy", "Meramu ramuan dan elixir berkhasiat", ChatFormatting.LIGHT_PURPLE, AuraStatType.WISDOM, AuraStatType.HEALTH),
    ENCHANTING("Enchanting", "Meningkatkan senjata dan armor dengan sihir", ChatFormatting.DARK_PURPLE, AuraStatType.WISDOM, AuraStatType.LUCK);

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
        // Skala polinomial seimbang untuk Level 1 s/d 1,000 (menghindari double overflow)
        return 100.0 + (currentLevel * 60.0) + (Math.pow(currentLevel, 1.85) * 8.0);
    }

    public static AuraSkillType fromString(String name) {
        if (name == null || name.isBlank()) return null;
        String clean = name.trim().toLowerCase();
        if (clean.equals("combat") || clean.equals("fighting")) {
            return FIGHTING;
        }
        if (clean.equals("healing") || clean.equals("alchemy")) {
            return ALCHEMY;
        }
        for (AuraSkillType s : values()) {
            if (s.name().equalsIgnoreCase(clean) || s.getDisplayName().equalsIgnoreCase(clean)) {
                return s;
            }
        }
        return null;
    }
}
