package net.takasha.rpg.dungeon;

public enum DungeonTier {
    COMMON("Common", "common", "§a", 30, 50, 60, 90, 100, 150, 180, 220, 300),
    NIGHTMARE("Nightmare", "nightmare", "§5", 150, 200, 250, 320, 380, 450, 500, 600, 750),
    SAKURA_BOSS("Sakura Boss", "sakura_boss", "§d", 400, 500, 550, 650, 700, 800, 850, 920, 1000);

    private final String displayName;
    private final String id;
    private final String colorCode;
    private final int wave1Min, wave1Max;
    private final int wave2Min, wave2Max;
    private final int wave3Min, wave3Max;
    private final int wave4Min, wave4Max;
    private final int wave5BossLevel;

    DungeonTier(String displayName, String id, String colorCode,
                int wave1Min, int wave1Max,
                int wave2Min, int wave2Max,
                int wave3Min, int wave3Max,
                int wave4Min, int wave4Max,
                int wave5BossLevel) {
        this.displayName = displayName;
        this.id = id;
        this.colorCode = colorCode;
        this.wave1Min = wave1Min;
        this.wave1Max = wave1Max;
        this.wave2Min = wave2Min;
        this.wave2Max = wave2Max;
        this.wave3Min = wave3Min;
        this.wave3Max = wave3Max;
        this.wave4Min = wave4Min;
        this.wave4Max = wave4Max;
        this.wave5BossLevel = wave5BossLevel;
    }

    public String getDisplayName() { return displayName; }
    public String getId() { return id; }
    public String getColorCode() { return colorCode; }

    public int getMobLevelForWave(int wave, java.util.Random rng) {
        return switch (wave) {
            case 1 -> wave1Min + rng.nextInt(Math.max(1, wave1Max - wave1Min + 1));
            case 2 -> wave2Min + rng.nextInt(Math.max(1, wave2Max - wave2Min + 1));
            case 3 -> wave3Min + rng.nextInt(Math.max(1, wave3Max - wave3Min + 1));
            case 4 -> wave4Min + rng.nextInt(Math.max(1, wave4Max - wave4Min + 1));
            case 5 -> wave5BossLevel;
            default -> wave5BossLevel;
        };
    }

    public int getBossLevel() { return wave5BossLevel; }

    public static DungeonTier fromId(String id) {
        for (DungeonTier t : values()) {
            if (t.id.equalsIgnoreCase(id) || t.name().equalsIgnoreCase(id)) {
                return t;
            }
        }
        return COMMON;
    }
}
