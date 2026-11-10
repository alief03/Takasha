package net.takasha.rpg.mmo;

public enum MMOStatType {
    // Vanilla / Physical
    ATTACK_DAMAGE("attack-damage", "Attack Damage", "⚔", "§c"),
    ATTACK_SPEED("attack-speed", "Attack Speed", "⚡", "§e"),
    MAX_HEALTH("max-health", "Max Health", "❤", "§a"),
    ARMOR("armor", "Armor", "🛡", "§9"),
    ARMOR_TOUGHNESS("armor-toughness", "Armor Toughness", "🔰", "§b"),
    KNOCKBACK_RESISTANCE("knockback-resistance", "Knockback Resistance", "🧱", "§7"),
    MOVEMENT_SPEED("movement-speed", "Movement Speed", "👟", "§f"),

    // RPG Offensive Stats
    PHYSICAL_DAMAGE("physical-damage", "Physical Damage", "🗡", "§c"),
    MAGIC_DAMAGE("magic-damage", "Magic Damage", "✨", "§d"),
    FIRE_DAMAGE("fire-damage", "Fire Damage", "🔥", "§6"),
    ICE_DAMAGE("ice-damage", "Ice Damage", "❄", "§b"),
    LIGHTNING_DAMAGE("lightning-damage", "Lightning Damage", "⚡", "§e"),
    EARTH_DAMAGE("earth-damage", "Earth Damage", "🌍", "§2"),
    LIGHT_DAMAGE("light-damage", "Light Damage", "☀️", "§e"),
    DARKNESS_DAMAGE("darkness-damage", "Darkness Damage", "🌑", "§8"),

    // Critical Strike Stats
    CRITICAL_STRIKE_CHANCE("critical-strike-chance", "Critical Strike Chance", "🎯", "§c"),
    CRITICAL_STRIKE_POWER("critical-strike-power", "Critical Strike Power", "💥", "§4"),
    SPELL_CRITICAL_STRIKE_CHANCE("spell-critical-strike-chance", "Spell Crit Chance", "🔮", "§d"),
    SPELL_CRITICAL_STRIKE_POWER("spell-critical-strike-power", "Spell Crit Power", "🌌", "§5"),

    // RPG Defensive Stats (PoE Mitigation)
    DEFENSE("defense", "Defense", "🛡", "§9"),
    FIRE_DEFENSE("fire-defense", "Fire Defense", "🔥🛡", "§6"),
    ICE_DEFENSE("ice-defense", "Ice Defense", "❄🛡", "§b"),
    LIGHTNING_DEFENSE("lightning-defense", "Lightning Defense", "⚡🛡", "§e"),
    EARTH_DEFENSE("earth-defense", "Earth Defense", "🌍🛡", "§2"),
    LIGHT_DEFENSE("light-defense", "Light Defense", "☀️🛡", "§e"),
    DARKNESS_DEFENSE("darkness-defense", "Darkness Defense", "🌑🛡", "§8"),
    DAMAGE_REDUCTION("damage-reduction", "Damage Reduction", "🛡%", "§b"),

    // Additional Combat & Damage Multipliers
    RANGE("range", "Combat Range", "🏹", "§e"),
    STAMINA_COST("stamina-cost", "Stamina Cost", "⚡", "§e"),
    ARROW_VELOCITY("arrow-velocity", "Arrow Velocity", "🏹", "§f"),
    BLUNT_POWER("blunt-power", "Blunt Power", "🔨", "§7"),
    BLUNT_RATING("blunt-rating", "Blunt Rating", "🔨", "§7"),
    LIFESTEAL("lifesteal", "Lifesteal", "🩸", "§c"),
    SPELL_VAMPIRISM("spell-vampirism", "Spell Vampirism", "🔮", "§d"),
    PVE_DAMAGE("pve-damage", "PvE Damage", "🥩", "§a"),
    PVP_DAMAGE("pvp-damage", "PvP Damage", "💀", "§c"),
    WEAPON_DAMAGE("weapon-damage", "Weapon Damage", "⚔", "§c"),
    SKILL_DAMAGE("skill-damage", "Skill Damage", "📖", "§d"),
    PROJECTILE_DAMAGE("projectile-damage", "Projectile Damage", "🏹", "§f"),
    UNDEAD_DAMAGE("undead-damage", "Undead Damage", "💀", "§e"),

    // Additional Defense & Mitigation
    BLOCK_POWER("block-power", "Block Power", "🛡", "§b"),
    BLOCK_RATING("block-rating", "Block Rating", "🛡", "§b"),
    BLOCK_COOLDOWN_REDUCTION("block-cooldown-reduction", "Block CDR", "⏳", "§a"),
    DODGE_RATING("dodge-rating", "Dodge Rating", "🪶", "§f"),
    DODGE_COOLDOWN_REDUCTION("dodge-cooldown-reduction", "Dodge CDR", "⏳", "§a"),
    PARRY_RATING("parry-rating", "Parry Rating", "🪣", "§e"),
    PARRY_COOLDOWN_REDUCTION("parry-cooldown-reduction", "Parry CDR", "⏳", "§a"),
    FALL_DAMAGE_REDUCTION("fall-damage-reduction", "Fall Damage Reduction", "🪶", "§f"),
    FIRE_DAMAGE_REDUCTION("fire-damage-reduction", "Fire Damage Reduction", "🔥🛡", "§6"),
    MAGIC_DAMAGE_REDUCTION("magic-damage-reduction", "Magic Damage Reduction", "✨🛡", "§d"),
    PROJECTILE_DAMAGE_REDUCTION("projectile-damage-reduction", "Projectile Damage Reduction", "🏹🛡", "§7"),
    PHYSICAL_DAMAGE_REDUCTION("physical-damage-reduction", "Physical Damage Reduction", "🗡🛡", "§c"),
    PVE_DAMAGE_REDUCTION("pve-damage-reduction", "PvE Damage Reduction", "🥩🛡", "§a"),
    PVP_DAMAGE_REDUCTION("pvp-damage-reduction", "PvP Damage Reduction", "💀🛡", "§c"),

    // Resource & Consumables
    MAX_MANA("max-mana", "Max Mana", "💧", "§9"),
    RESTORE_HEALTH("restore-health", "Restore Health", "❤", "§c"),
    RESTORE_FOOD("restore-food", "Restore Food", "🍖", "§6"),
    RESTORE_SATURATION("restore-saturation", "Restore Saturation", "🥕", "§e"),
    RESTORE_MANA("restore-mana", "Restore Mana", "💧", "§3"),
    RESTORE_STAMINA("restore-stamina", "Restore Stamina", "⚡", "§e"),
    ITEM_COOLDOWN("item-cooldown", "Item Cooldown", "🍗", "§7"),

    // Utility & Resource Stats
    MANA_COST("mana-cost", "Mana Cost", "💧", "§3"),
    COOLDOWN_REDUCTION("cooldown-reduction", "Cooldown Reduction", "⏳", "§a"),

    // Tool & Mining Stats
    PICKAXE_POWER("pickaxe-power", "Pickaxe Power", "⛏", "§6"),
    MINING_EFFICIENCY("mining-efficiency", "Mining Speed", "⚡", "§e"),
    LUCK("luck", "Mining Luck", "🍀", "§a"),
    MAX_DURABILITY("max-durability", "Max Durability", "🔨", "§7"),
    WILL_BREAK("will-break", "Will Break", "💔", "§c"),
    AUTOSMELT("autosmelt", "Autosmelt", "🔥", "§6"),
    BOUNCING_CRACK("bouncing-crack", "Bouncing Crack", "💥", "§e"),

    // Requirements (AuraSkills & Attributes)
    REQUIRED_LEVEL("required-level", "Required Level", "⭐", "§e"),
    REQUIRED_FIGHTING("required-fighting", "Required Fighting", "⚔", "§c"),
    REQUIRED_MINING("required-mining", "Required Mining", "⛏", "§6"),
    REQUIRED_FARMING("required-farming", "Required Farming", "🌾", "§a"),
    REQUIRED_WOODCUTTING("required-woodcutting", "Required Woodcutting", "🪓", "§2"),
    REQUIRED_EXCAVATION("required-excavation", "Required Excavation", "🍂", "§e"),
    REQUIRED_WISDOM("required-wisdom", "Required Wisdom", "📖", "§b"),

    // AuraSkills Stat Modifiers
    AURA_SKILLS_WISDOM("additional-wisdom", "Bonus Wisdom", "🧠", "§b"),
    AURA_SKILLS_LUCK("additional-luck", "Bonus Luck", "🍀", "§a"),
    AURA_SKILLS_EXPERIENCE("additional-experience", "Bonus Skill XP", "✨", "§d");

    private final String key;
    private final String displayName;
    private final String symbol;
    private final String color;

    MMOStatType(String key, String displayName, String symbol, String color) {
        this.key = key;
        this.displayName = displayName;
        this.symbol = symbol;
        this.color = color;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getColor() {
        return color;
    }

    public static MMOStatType fromKey(String key) {
        if (key == null) return null;
        String normalized = key.toLowerCase().replace('_', '-');
        for (MMOStatType stat : values()) {
            if (stat.key.equalsIgnoreCase(normalized)) {
                return stat;
            }
        }
        return null;
    }
}
