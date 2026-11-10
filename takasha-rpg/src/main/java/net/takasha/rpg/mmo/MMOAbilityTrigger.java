package net.takasha.rpg.mmo;

public enum MMOAbilityTrigger {
    RIGHT_CLICK("Right Click", "RIGHT_CLICK"),
    SHIFT_RIGHT_CLICK("Sneak Right Click", "SHIFT_RIGHT_CLICK"),
    LEFT_CLICK("Left Click", "LEFT_CLICK"),
    SHIFT_LEFT_CLICK("Sneak Left Click", "SHIFT_LEFT_CLICK"),
    ON_HIT("On Hit", "ON_HIT"),
    DAMAGED("When Damaged", "DAMAGED"),
    SNEAK("On Sneak", "SNEAK"),
    TIMER("Periodic Timer", "TIMER");

    private final String display;
    private final String configKey;

    MMOAbilityTrigger(String display, String configKey) {
        this.display = display;
        this.configKey = configKey;
    }

    public String getDisplay() {
        return display;
    }

    public String getConfigKey() {
        return configKey;
    }

    public static MMOAbilityTrigger fromString(String val) {
        if (val == null) return RIGHT_CLICK;
        String norm = val.toUpperCase().replace('-', '_').replace(' ', '_');
        for (MMOAbilityTrigger t : values()) {
            if (t.name().equalsIgnoreCase(norm) || t.configKey.equalsIgnoreCase(norm)) {
                return t;
            }
        }
        return RIGHT_CLICK;
    }
}
