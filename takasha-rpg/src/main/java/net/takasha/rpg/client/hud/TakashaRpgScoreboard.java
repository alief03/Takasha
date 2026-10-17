package net.takasha.rpg.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.takasha.rpg.TakashaRpgMod;
import net.takasha.rpg.network.RpgClientSyncPayload;

public class TakashaRpgScoreboard {

    private static String worldName = "Singleplayer";
    private static String playerName = "Player";
    private static double balance = 100.0;
    private static int gems = 0;
    private static int totalSkillLevel = 11;
    private static int usedHomes = 0;
    private static int maxHomes = 1;
    private static boolean visible = true;

    public static void update(RpgClientSyncPayload payload) {
        worldName = payload.worldName();
        playerName = payload.playerName();
        balance = payload.balance();
        gems = payload.gems();
        totalSkillLevel = payload.totalSkillLevel();
        usedHomes = payload.usedHomes();
        maxHomes = payload.maxHomes();
        visible = payload.showScoreboard();
    }

    public static void toggle() {
        visible = !visible;
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void register() {
        Identifier hudId = Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "scoreboard");

        HudElementRegistry.addLast(hudId, (extractor, deltaTracker) -> {
            if (!visible) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();

            // Right sidebar scoreboard positioning
            int boxWidth = 144;
            int boxHeight = 88;
            int startX = screenWidth - boxWidth - 6;
            int startY = screenHeight / 3;

            // Semi-transparent dark background
            extractor.fill(startX, startY, startX + boxWidth, startY + boxHeight, 0x88000000);
            // Border accents
            extractor.fill(startX, startY, startX + boxWidth, startY + 1, 0xFFE5A65E);
            extractor.fill(startX, startY + boxHeight - 1, startX + boxWidth, startY + boxHeight, 0xFFE5A65E);

            // Title
            extractor.text(mc.font, Component.literal("§6§l★ TAKASHA RPG ★"), startX + 16, startY + 5, 0xFFFFFF, true);

            // 6 Explicitly Approved Lines
            int lineY = startY + 18;
            int lineSpacing = 11;

            extractor.text(mc.font, Component.literal("§7Dunia: §f" + worldName), startX + 6, lineY, 0xFFFFFF, true);
            lineY += lineSpacing;

            extractor.text(mc.font, Component.literal("§7Nama: §e" + playerName), startX + 6, lineY, 0xFFFFFF, true);
            lineY += lineSpacing;

            extractor.text(mc.font, Component.literal("§7Balance: §a$" + String.format("%.2f", balance)), startX + 6, lineY, 0xFFFFFF, true);
            lineY += lineSpacing;

            extractor.text(mc.font, Component.literal("§7Koin: §b" + gems + " Gems"), startX + 6, lineY, 0xFFFFFF, true);
            lineY += lineSpacing;

            extractor.text(mc.font, Component.literal("§7Level AuraSkill: §d" + totalSkillLevel), startX + 6, lineY, 0xFFFFFF, true);
            lineY += lineSpacing;

            extractor.text(mc.font, Component.literal("§7Sethome: §b" + usedHomes + "§7/§b" + maxHomes), startX + 6, lineY, 0xFFFFFF, true);
        });
    }
}
