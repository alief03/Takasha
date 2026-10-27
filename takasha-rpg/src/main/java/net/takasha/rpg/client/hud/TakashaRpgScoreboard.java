package net.takasha.rpg.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
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
    private static double currentMana = 20.0;
    private static double maxMana = 20.0;

    public static void update(RpgClientSyncPayload payload) {
        worldName = payload.worldName();
        playerName = payload.playerName();
        balance = payload.balance();
        gems = payload.gems();
        totalSkillLevel = payload.totalSkillLevel();
        usedHomes = payload.usedHomes();
        maxHomes = payload.maxHomes();
        visible = payload.showScoreboard();
        currentMana = payload.currentMana();
        maxMana = payload.maxMana();
    }

    public static void reset() {
        worldName = "Singleplayer";
        playerName = "Player";
        balance = 100.0;
        gems = 0;
        totalSkillLevel = 11;
        usedHomes = 0;
        maxHomes = 1;
        visible = true;
        currentMana = 20.0;
        maxMana = 20.0;
    }

    public static void toggle() {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.hasSingleplayerServer()) return;
        visible = !visible;
        try {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new net.takasha.rpg.network.ToggleScoreboardC2SPayload());
        } catch (Throwable ignored) {}
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§6§l[Takasha-RPG] §fScoreboard HUD: " +
                    (visible ? "§a§lDITAMPILKAN" : "§c§lDISEMBUNYIKAN")));
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, visible ? 1.2f : 0.8f);
        }
    }

    public static double getCurrentMana() {
        return currentMana;
    }

    public static double getMaxMana() {
        return maxMana;
    }

    public static double getBalance() {
        return balance;
    }

    public static int getGems() {
        return gems;
    }

    public static int getTotalSkillLevel() {
        return totalSkillLevel;
    }

    public static int getUsedHomes() {
        return usedHomes;
    }

    public static int getMaxHomes() {
        return maxHomes;
    }

    public static String getWorldName() {
        return worldName;
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void setVisible(boolean val) {
        visible = val;
    }

    public static void register() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "takasha_rpg_scoreboard"),
            TakashaRpgScoreboard::render
        );
    }

    private static void render(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!mc.hasSingleplayerServer()) return;
        if (!visible) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Sisi kanan layar HUD scoreboard
        int boxWidth = 148;
        int boxHeight = 90;
        int startX = screenWidth - boxWidth - 6;
        int startY = Math.max(20, screenHeight / 3);

        // Semi-transparent dark background
        extractor.fill(startX, startY, startX + boxWidth, startY + boxHeight, 0x88000000);
        // Border accents (Golden Orange)
        extractor.fill(startX, startY, startX + boxWidth, startY + 1, 0xFFE5A65E);
        extractor.fill(startX, startY + boxHeight - 1, startX + boxWidth, startY + boxHeight, 0xFFE5A65E);
        extractor.fill(startX, startY, startX + 1, startY + boxHeight, 0xFFE5A65E);
        extractor.fill(startX + boxWidth - 1, startY, startX + boxWidth, startY + boxHeight, 0xFFE5A65E);

        // Header Title
        int currY = startY + 4;
        extractor.textRenderer().accept(startX + 18, currY, Component.literal("§6§l✦ TAKASHA RPG ✦"));
        currY += 10;
        extractor.fill(startX + 4, currY, startX + boxWidth - 4, currY + 1, 0x55E5A65E);
        currY += 4;

        // Player Name fallback
        String displayName = (playerName != null && !playerName.isEmpty()) ? playerName : mc.player.getName().getString();
        extractor.textRenderer().accept(startX + 6, currY, Component.literal("§7Pemain: §e" + displayName));
        currY += 9;

        // AuraSkills Total
        extractor.textRenderer().accept(startX + 6, currY, Component.literal("§7Skill Level: §a" + totalSkillLevel));
        currY += 9;

        // Economy Coins
        extractor.textRenderer().accept(startX + 6, currY, Component.literal("§7Coins: §6" + String.format("%,.1f", balance)));
        currY += 9;

        // Economy Gems
        extractor.textRenderer().accept(startX + 6, currY, Component.literal("§7Gems: §b" + gems + " ♦"));
        currY += 9;

        // Homes info
        extractor.textRenderer().accept(startX + 6, currY, Component.literal("§7Homes: §f" + usedHomes + "/" + maxHomes));
        currY += 9;

        // World / Server
        String displayWorld = (worldName != null && !worldName.isEmpty()) ? worldName : "World";
        extractor.textRenderer().accept(startX + 6, currY, Component.literal("§7Realm: §d" + displayWorld));
        currY += 8;

        // Footer hint
        extractor.textRenderer().accept(startX + 10, currY, Component.literal("§8[Tekan O untuk sembunyikan]"));
    }
}
