package net.takasha.rpg.mmo;

public enum MMOItemTier {
    COMMON("Common", "§f", "§7"),
    UNCOMMON("Uncommon", "§a", "§2"),
    RARE("Rare", "§9", "§1"),
    EPIC("Epic", "§5", "§d"),
    LEGENDARY("Legendary", "§6", "§e"),
    MYTHIC("Mythic", "§c", "§4"),
    AWAKENED("Awakened", "§d§l", "§5§l");

    private final String displayName;
    private final String prefixColor;
    private final String loreColor;

    MMOItemTier(String displayName, String prefixColor, String loreColor) {
        this.displayName = displayName;
        this.prefixColor = prefixColor;
        this.loreColor = loreColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPrefixColor() {
        return prefixColor;
    }

    public String getLoreColor() {
        return loreColor;
    }

    public static MMOItemTier fromString(String name) {
        if (name == null) return COMMON;
        for (MMOItemTier tier : values()) {
            if (tier.name().equalsIgnoreCase(name)) {
                return tier;
            }
        }
        return COMMON;
    }
}
