package net.sakura.touhou.entity.task;

import net.minecraft.ChatFormatting;

public enum MaidTaskMode {
    IDLE("Idle / Santai", "Menjaga posisi diam di tempat", ChatFormatting.GRAY),
    FOLLOW("Mengikuti", "Mengikuti pergerakan pemilik", ChatFormatting.AQUA),
    ATTACK("Combat Vanguard", "Menyerang monster dan melindungi pemilik", ChatFormatting.RED),
    FARM("Auto-Farm", "Memanen tanaman matang dan menanam kembali benih", ChatFormatting.GREEN),
    FISH("Auto-Fish", "Memancing otomatis saat memegang joran pancing", ChatFormatting.BLUE),
    TORCH("Auto-Torch", "Memasang obor otomatis di area gelap", ChatFormatting.YELLOW);

    private final String displayName;
    private final String description;
    private final ChatFormatting color;

    MaidTaskMode(String displayName, String description, ChatFormatting color) {
        this.displayName = displayName;
        this.description = description;
        this.color = color;
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
}
