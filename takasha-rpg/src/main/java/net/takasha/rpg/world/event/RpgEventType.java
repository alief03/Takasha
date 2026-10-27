package net.takasha.rpg.world.event;

import net.minecraft.world.BossEvent;

public enum RpgEventType {
    // 4 Acak (Random Events)
    BLOOD_MOON("blood_moon", "§4§l[BLOOD MOON] §cKebangkitan Bulan Darah", "Monster spawn lebih ganas, level +5, koin drop x2, EXP x1.5!", false, 12000, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10),
    GOBLIN_HUNT("goblin_hunt", "§6§l[GOBLIN HUNT] §eSerbuan Goblin Emas", "Goblin pembawa harta berkeliaran di alam liar! Kalahkan untuk jarahan melimpah!", false, 9600, BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_6),
    STARFALL("starfall", "§b§l[STARFALL] §3Hujan Bintang Surgawi", "Meteor kristal jatuh dari angkasa menjatuhkan Gems dan Enchantment Dust!", false, 8400, BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_12),
    RIFT_INCURSION("rift_incursion", "§5§l[RIFT INCURSION] §dInvasi Retakan Void", "Retakan dimensi void terbuka! Monster kegelapan menyerbu dunia!", false, 12000, BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_20),

    // 4 Berkala (Periodic Events)
    DOUBLE_XP("double_xp", "§a§l[DOUBLE EXP] §2Lonjakan Pengalaman", "Semua perolehan EXP bertambah 2x lipat!", true, 18000, BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS),
    MYTHIC_MERCHANT("mythic_merchant", "§d§l[MYTHIC MERCHANT] §fPedagang Misterius Tiba", "Pedagang mistis mengembara membawa relik dan scroll langka!", true, 14400, BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS),
    DUNGEON_RUSH("dungeon_rush", "§c§l[DUNGEON RUSH] §6Jam Puncak Rift Dungeon", "Hadiah dungeon berlipat ganda, arena drop rate naik tajam!", true, 18000, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10),
    HARVEST_FESTIVAL("harvest_festival", "§e§l[HARVEST FESTIVAL] §6Pesta Panen Raya", "Hasil panen berlipat 2x, EXP bertani x2, dan bonus nugget emas!", true, 18000, BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);

    private final String id;
    private final String displayName;
    private final String description;
    private final boolean periodic;
    private final int defaultDurationTicks;
    private final BossEvent.BossBarColor bossBarColor;
    private final BossEvent.BossBarOverlay bossBarOverlay;

    RpgEventType(String id, String displayName, String description, boolean periodic, int defaultDurationTicks,
                 BossEvent.BossBarColor bossBarColor, BossEvent.BossBarOverlay bossBarOverlay) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.periodic = periodic;
        this.defaultDurationTicks = defaultDurationTicks;
        this.bossBarColor = bossBarColor;
        this.bossBarOverlay = bossBarOverlay;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPeriodic() {
        return periodic;
    }

    public int getDefaultDurationTicks() {
        return defaultDurationTicks;
    }

    public BossEvent.BossBarColor getBossBarColor() {
        return bossBarColor;
    }

    public BossEvent.BossBarOverlay getBossBarOverlay() {
        return bossBarOverlay;
    }

    public static RpgEventType fromId(String id) {
        for (RpgEventType type : values()) {
            if (type.id.equalsIgnoreCase(id) || type.name().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
