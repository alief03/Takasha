package net.takasha.rpg.skills;

import net.minecraft.ChatFormatting;

/**
 * AuraTraitType
 * 19 Authentic Mathematical Traits dari AuraSkills 2.4.0:
 * Menghubungkan Stat primer/sekunder dan level ke modifier gameplay terukur.
 */
public enum AuraTraitType {
    ATTACK_DAMAGE("Attack Damage", "Peningkatan damage serangan tangan & busur", ChatFormatting.RED, AuraStatType.STRENGTH, "+%.1f Damage Fisik"),
    HP("Health (HP)", "Peningkatan daya tahan dan kapasitas darah maksimal", ChatFormatting.RED, AuraStatType.HEALTH, "+%.1f HP"),
    SATURATION_REGEN("Saturation Regen", "Pemulihan saturasi lapar alami secara berkala", ChatFormatting.LIGHT_PURPLE, AuraStatType.REGENERATION, "+%.2f/s Saturasi"),
    HUNGER_REGEN("Hunger Regen", "Pemulihan tingkat rasa lapar alami secara berkala", ChatFormatting.LIGHT_PURPLE, AuraStatType.REGENERATION, "+%.2f/s Lapar"),
    MANA_REGEN("Mana Regen", "Kecepatan pemulihan energi sihir per detik", ChatFormatting.DARK_AQUA, AuraStatType.REGENERATION, "+%.2f/s Mana"),

    FARMING_LUCK("Farming Luck", "Peluang mendapatkan panen tanaman ekstra", ChatFormatting.GREEN, AuraStatType.LUCK, "+%.1f%% Panen Ekstra"),
    FORAGING_LUCK("Foraging Luck", "Peluang mendapatkan kayu dan getah ekstra", ChatFormatting.DARK_GREEN, AuraStatType.LUCK, "+%.1f%% Kayu Ekstra"),
    MINING_LUCK("Mining Luck", "Peluang melipatgandakan drop bijih mineral", ChatFormatting.GRAY, AuraStatType.LUCK, "+%.1f%% Bijih Ganda"),
    FISHING_LUCK("Fishing Luck", "Peluang mendapatkan tangkapan langka & harta karun air", ChatFormatting.BLUE, AuraStatType.LUCK, "+%.1f%% Harta Karun Air"),
    EXCAVATION_LUCK("Excavation Luck", "Peluang menemukan relik dan mineral saat menggali", ChatFormatting.GOLD, AuraStatType.LUCK, "+%.1f%% Temuan Gali"),

    EXPERIENCE_BONUS("Experience Bonus", "Bonus orb pengalaman vanilla dan skill", ChatFormatting.GREEN, AuraStatType.WISDOM, "+%.1f%% Bonus XP"),
    ANVIL_DISCOUNT("Anvil Discount", "Diskon biaya level saat menggabungkan item di anvil", ChatFormatting.DARK_PURPLE, AuraStatType.WISDOM, "-%.1f%% Biaya Anvil"),
    MAX_MANA("Max Mana", "Peningkatan kapasitas energi sihir maksimal", ChatFormatting.AQUA, AuraStatType.WISDOM, "+%.1f Max Mana"),

    DAMAGE_REDUCTION("Damage Reduction", "Persentase pengurangan seluruh damage yang diterima", ChatFormatting.DARK_BLUE, AuraStatType.TOUGHNESS, "-%.1f%% Damage Masuk"),
    CRIT_CHANCE("Crit Chance", "Peluang serangan menghasilkan critical strike", ChatFormatting.YELLOW, AuraStatType.CRIT_CHANCE, "+%.1f%% Peluang Kritikal"),
    CRIT_DAMAGE("Crit Damage", "Peningkatan besaran damage saat critical strike", ChatFormatting.DARK_RED, AuraStatType.CRIT_DAMAGE, "+%.1f%% Damage Kritikal"),
    MOVEMENT_SPEED("Movement Speed", "Peningkatan kecepatan berjalan, berlari, dan kelincahan", ChatFormatting.WHITE, AuraStatType.SPEED, "+%.1f%% Kecepatan Gerak"),
    LUCK("General Luck", "Peningkatan atribut keberuntungan dasar", ChatFormatting.GREEN, AuraStatType.LUCK, "+%.1f Luck"),
    DOUBLE_DROP("Double Drop", "Peluang melipatgandakan drop item dari mob musuh", ChatFormatting.GOLD, AuraStatType.LUCK, "+%.1f%% Drop Ganda");

    private final String displayName;
    private final String description;
    private final ChatFormatting color;
    private final AuraStatType primaryStat;
    private final String format;

    AuraTraitType(String displayName, String description, ChatFormatting color, AuraStatType primaryStat, String format) {
        this.displayName = displayName;
        this.description = description;
        this.color = color;
        this.primaryStat = primaryStat;
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

    public AuraStatType getPrimaryStat() {
        return primaryStat;
    }

    public String formatValue(double value) {
        return String.format(format, value);
    }

    public String formatBonus(double value) {
        return formatValue(value);
    }
}
