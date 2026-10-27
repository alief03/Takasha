package net.sakura.weapons.client.fishing.capcay;

public enum ItemCategory {
    SWORD("Pedang"),
    PICKAXE("Beliung"),
    AXE("Kapak"),
    SHOVEL("Sekop"),
    HOE("Cangkul"),
    BOW("Busur"),
    HELMET("Helm"),
    CHESTPLATE("Baju Zirah"),
    LEGGINGS("Celana Zirah"),
    BOOTS("Sepatu Zirah"),
    MUSIC_DISC("Piringan Musik"),
    POTION("Ramuan"),
    FOOD("Makanan"),
    FLOWER("Bunga"),
    DYE("Pewarna"),
    INGOT_OR_GEM("Batangan Logam / Permata"),
    ORE("Bijih Tambang"),
    WOOD("Kayu"),
    STONE("Batu"),
    TOOL("Alat"),
    OTHER("Lainnya");

    private final String displayName;

    ItemCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
