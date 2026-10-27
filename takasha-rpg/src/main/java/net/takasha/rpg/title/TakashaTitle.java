package net.takasha.rpg.title;

import net.minecraft.ChatFormatting;

/**
 * 16 Gelar Kehormatan (Titles) dengan Title Sovereignty Trait Engine.
 * Setiap gelar memiliki efek unik aktif/pasif yang TIDAK ADA di enchant maupun potion.
 */
public enum TakashaTitle {
    G01("G-01", "Sang Pemula Abadi", "Novice's Grace", "Adrenaline Surge",
            "Saat HP <= 15%, memicu Speed II + Resistance I selama 6s (CD 90s).",
            ChatFormatting.GREEN, 90),

    G02("G-02", "Penjelajah Lima Dimensi", "Dimension Stride", "Void Anchor & Chorus Agility",
            "Kebal void pull, reduksi levitasi 50%, dan teleportasi Chorus Fruit instan tanpa delay makan.",
            ChatFormatting.AQUA, 0),

    G03("G-03", "Ksatria Penakluk Badai", "Tempest Ward", "Lightning Galvanization",
            "Sambaran petir menyembuhkan 20 HP seketika + bonus Attack Speed +25% selama 8s.",
            ChatFormatting.YELLOW, 0),

    G04("G-04", "Penebas Bayangan Malam", "Phantom Riposte", "Shadow Knife Counter",
            "Peluang 25% hindari proyektil dan seketika balas meluncurkan pisau bayangan berdaya 12 True Damage.",
            ChatFormatting.BLUE, 0),

    G05("G-05", "Penguasa Api Neraka", "Infernal Monarch", "Hellfire Burst",
            "Monster terbakar berjarak <= 6 blok meledak dalam Hellfire Burst tiap 3s (10 area fire damage).",
            ChatFormatting.GOLD, 3),

    G06("G-06", "Penjaga Gerbang Jurang", "Abyssal Vortex", "Gravity Singularity",
            "Setiap 5 kill streak memanggil pusaran jurang mini yang menyerap mob dan melambatkan musuh 40%.",
            ChatFormatting.DARK_AQUA, 10),

    G07("G-07", "Pembalas Dendam Relik", "Vengeful Rebound", "True Damage Reflection",
            "Saat menerima damage fatal (> 40% Max HP/hit), pantulkan 40% damage tersebut sebagai True Damage.",
            ChatFormatting.RED, 12),

    G08("G-08", "Alkemis Jiwa Terkutuk", "Soul Transmutation", "Gem Shard Synthesis",
            "Reduksi konsumsi Soul Enchantment 30% dan ubah tiap 50 Soul terpakai jadi 1 Arcane Gem Shard.",
            ChatFormatting.LIGHT_PURPLE, 0),

    G09("G-09", "Algojo Tak Bersuara", "Executioner's Guillotine", "Instant Decapitation",
            "Monster dengan sisa HP < 12% Max HP otomatis terpenggal mati seketika mengabaikan invulnerability.",
            ChatFormatting.DARK_RED, 0),

    G10("G-10", "Panglima Seratus Arena", "Warlord's Domain", "Phalanx Ward Aura",
            "Meningkatkan +15% Armor Toughness seluruh kawan dalam radius 12 blok.",
            ChatFormatting.YELLOW, 0),

    G11("G-11", "Pewaris Pedang Sakura", "Petal Dance Blade", "Cherry Blossom Tempest",
            "Combo 5 pukulan pedang memanggil badai kelopak sakura yang membutakan lawan 3s + Bleed III.",
            ChatFormatting.LIGHT_PURPLE, 0),

    G12("G-12", "Dukun Pembawa Petaka", "Calamity Hex", "Hex of Despair",
            "Serangan kritikal menorehkan kutukan Calamity Hex yang memotong regenerasi HP musuh 75% selama 8s.",
            ChatFormatting.DARK_PURPLE, 0),

    G13("G-13", "Dewa Penempa Legendaris", "Master Forge Affinity", "Anvil Mastery & Dust Catalyst",
            "Biaya EXP Anvil berkurang 35% dan peluang kegagalan Magic Dust berkurang 50% permanen.",
            ChatFormatting.GREEN, 0),

    G14("G-14", "Raja Naga Bintang", "Cosmic Singularity", "Singularity Arrow Pull",
            "Tembakan panah menghasilkan tarikan gravitasi lubang hitam mini di titik benturan selama 2s (CD 15s).",
            ChatFormatting.BLUE, 15),

    G15("G-15", "Penguasa Keabadian", "Aegis of the Immortal Sovereign", "Chrono-Stasis Ward",
            "Saat terima damage mematikan, bekukan waktu pemain 2.5s (kebal total) sembari pulihkan 50% HP (CD 5m).",
            ChatFormatting.GOLD, 300),

    G16("G-16", "Maharaja Takasha", "True Sovereignty Aura", "Emperor's Domain & Terror Feat",
            "Aura kedaulatan kaisar: seluruh monster di bawah Level 300 lari ketakutan (Terror Feat) + Koin & EXP +35%.",
            ChatFormatting.DARK_RED, 0);

    private final String code;
    private final String titleName;
    private final String romanTitle;
    private final String traitName;
    private final String traitDescription;
    private final ChatFormatting color;
    private final int cooldownSeconds;

    TakashaTitle(String code, String titleName, String romanTitle, String traitName, String traitDescription, ChatFormatting color, int cooldownSeconds) {
        this.code = code;
        this.titleName = titleName;
        this.romanTitle = romanTitle;
        this.traitName = traitName;
        this.traitDescription = traitDescription;
        this.color = color;
        this.cooldownSeconds = cooldownSeconds;
    }

    public String getCode() {
        return code;
    }

    public String getTitleName() {
        return titleName;
    }

    public String getRomanTitle() {
        return romanTitle;
    }

    public String getTraitName() {
        return traitName;
    }

    public String getTraitDescription() {
        return traitDescription;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public static TakashaTitle fromCodeOrName(String query) {
        if (query == null) return null;
        String clean = query.trim().toLowerCase().replace("-", "").replace("_", "");
        for (TakashaTitle t : values()) {
            if (t.getCode().toLowerCase().replace("-", "").equals(clean)
                    || t.name().toLowerCase().equals(clean)
                    || t.getTitleName().toLowerCase().contains(clean)) {
                return t;
            }
        }
        return null;
    }
}
