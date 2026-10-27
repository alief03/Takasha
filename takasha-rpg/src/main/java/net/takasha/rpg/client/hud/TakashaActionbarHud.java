package net.takasha.rpg.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.takasha.rpg.TakashaRpgMod;

public class TakashaActionbarHud {

    public static void register() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "takasha_rpg_status_hud"),
            TakashaActionbarHud::render
        );
    }

    private static void render(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!mc.hasSingleplayerServer()) return;
        if (mc.player.isSpectator()) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float currentHp = mc.player.getHealth();
        float maxHp = mc.player.getMaxHealth();
        double mana = TakashaRpgScoreboard.getCurrentMana();
        double maxMana = TakashaRpgScoreboard.getMaxMana();

        Component text = Component.literal(
            String.format("§c❤ %.0f/%.0f   §b✦ %.0f/%.0f Mana", currentHp, maxHp, mana, maxMana)
        );

        int textWidth = mc.font.width(text);
        int x = (screenWidth - textWidth) / 2;

        // Anti-Collision Dynamic Y:
        // Menempatkan HUD bar tepat di atas baris hati (maks 2 baris via AuraHealthScaling),
        // bar armor (jika ada armor), serta bar oksigen/mount (jika berenang atau berkendara).
        int rows = Math.max(1, Math.min(2, (int) Math.ceil((AuraHealthScaling.getVisualMaxHealth(mc.player) + AuraHealthScaling.getVisualAbsorption(mc.player)) / 20.0f)));
        int heartHeight = 10 + (rows - 1) * 10;
        int armorOffset = (mc.player.getArmorValue() > 0) ? 10 : 0;
        int vehicleAirOffset = (mc.player.getVehicle() != null || mc.player.getAirSupply() < mc.player.getMaxAirSupply()) ? 10 : 0;
        int y = screenHeight - 39 - heartHeight - armorOffset - vehicleAirOffset - 6;

        // Subtle dark pill background with clean contrast
        extractor.fill(x - 4, y - 2, x + textWidth + 4, y + 10, 0x66000000);
        extractor.textRenderer().accept(x, y, text);
    }
}
