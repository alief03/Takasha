package net.sakura.weapons.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.sakura.weapons.client.config.TakashaMasterConfig;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class TakashaFortuneSubScreen extends Screen {

    private final Screen parentScreen;
    private final TakashaMasterConfig config;

    private Button toggleBtn;
    private Button modeBtn;
    private Button detailBtn;
    private Button testPreviewBtn;
    private Button backBtn;

    public TakashaFortuneSubScreen(Screen parentScreen) {
        super(Component.literal("Takasha Fortune & Skill Reshaper"));
        this.parentScreen = parentScreen;
        this.config = TakashaMasterConfig.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        int panelW = 380;
        int panelH = 220;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        // Row 1: Status
        this.toggleBtn = Button.builder(
            Component.literal("Status Reshaper: " + (config.fortuneReshaperEnabled ? "§aAKTIF" : "§cNONAKTIF")),
            btn -> {
                config.fortuneReshaperEnabled = !config.fortuneReshaperEnabled;
                config.save();
                btn.setMessage(Component.literal("Status Reshaper: " + (config.fortuneReshaperEnabled ? "§aAKTIF" : "§cNONAKTIF")));
            }
        ).bounds(left + 14, top + 34, 170, 20).build();
        this.addRenderableWidget(this.toggleBtn);

        // Row 1, Col 2: Display Mode Cycle
        this.modeBtn = Button.builder(
            getModeLabel(),
            btn -> {
                TakashaMasterConfig.FortuneDisplayMode[] modes = TakashaMasterConfig.FortuneDisplayMode.values();
                int next = (config.fortuneDisplayMode.ordinal() + 1) % modes.length;
                config.fortuneDisplayMode = modes[next];
                config.save();
                btn.setMessage(getModeLabel());
            }
        ).bounds(left + 196, top + 34, 170, 20)
        .tooltip(Tooltip.create(Component.literal("Lokasi penampilan laporan fortune panen:\nACTIONBAR: Di atas hotbar (bersih)\nCHAT: Pesan chat detail\nTITLE: Teks layar tengah\nOFF: Senyap\nPASSTHROUGH: Asli server")))
        .build();
        this.addRenderableWidget(this.modeBtn);

        // Row 2: Detail Level Cycle
        this.detailBtn = Button.builder(
            getDetailLabel(),
            btn -> {
                TakashaMasterConfig.FortuneDetailLevel[] levels = TakashaMasterConfig.FortuneDetailLevel.values();
                int next = (config.fortuneDetailLevel.ordinal() + 1) % levels.length;
                config.fortuneDetailLevel = levels[next];
                config.save();
                btn.setMessage(getDetailLabel());
            }
        ).bounds(left + 14, top + 64, 170, 20)
        .tooltip(Tooltip.create(Component.literal("Tingkat rincian:\nDETAILED: XP, Skill & Pengali Drop Lengkap\nCOMPACT: Ringkas 1 baris\nMINIMAL: Simpel singkat")))
        .build();
        this.addRenderableWidget(this.detailBtn);

        // Row 2, Col 2: Test Preview
        this.testPreviewBtn = Button.builder(
            Component.literal("✨ Uji Tampilan Laporan"),
            btn -> runTestPreview()
        ).bounds(left + 196, top + 64, 170, 20)
        .tooltip(Tooltip.create(Component.literal("Simulasikan notifikasi fortune/AuraSkills sesuai pengaturan saat ini")))
        .build();
        this.addRenderableWidget(this.testPreviewBtn);

        // Bottom Controls
        int bottomY = top + panelH - 24;
        this.backBtn = Button.builder(
            Component.literal("◀ Kembali ke Dashboard"),
            btn -> this.onClose()
        ).bounds(left + (panelW - 160) / 2, bottomY, 160, 18).build();
        this.addRenderableWidget(this.backBtn);
    }

    private Component getModeLabel() {
        return Component.literal("Tampilan: §b" + config.fortuneDisplayMode.name());
    }

    private Component getDetailLabel() {
        return Component.literal("Rincian: §e" + config.fortuneDetailLevel.name());
    }

    private void runTestPreview() {
        if (this.minecraft.player == null) return;
        String formatted;
        switch (config.fortuneDetailLevel) {
            case DETAILED -> formatted = config.fortunePrefix + config.fortuneColorProc + "Double Drop x3 proc! " + config.fortuneColorXp + "(+45 Farming XP, Level 52)";
            case MINIMAL -> formatted = config.fortunePrefix + config.fortuneColorProc + "x3 Drops";
            default -> formatted = config.fortunePrefix + config.fortuneColorProc + "✨ Double Drop x3 proc! " + config.fortuneColorXp + "(+45 XP)";
        }

        switch (config.fortuneDisplayMode) {
            case ACTIONBAR -> this.minecraft.player.sendOverlayMessage(Component.literal(formatted));
            case CHAT -> this.minecraft.player.sendSystemMessage(Component.literal(formatted));
            case TITLE -> this.minecraft.gui.hud.setTitle(Component.literal(config.fortuneColorProc + "x3 Harvest Drop"));
            case OFF -> this.minecraft.player.sendSystemMessage(Component.literal("§7[Fortune Reshaper: Mode OFF (Notifikasi disenyapkan)]"));
            case PASSTHROUGH -> this.minecraft.player.sendOverlayMessage(Component.literal("§6[AuraSkills] §eFarming §7+45 XP §8(§b+3 Double Drop§8)"));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractor.fill(0, 0, this.width, this.height, 0x88000000);

        int panelW = 380;
        int panelH = 220;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        extractor.fill(left, top, left + panelW, top + panelH, 0xEE14121E);
        extractor.outline(left, top, panelW, panelH, 0xFFE082A8);

        // Header
        extractor.fill(left, top, left + panelW, top + 26, 0xFF241528);
        extractor.text(this.font, "💎 Pengatur Laporan Fortune Panen & Skill", left + 10, top + 8, 0xFFFFB6C1, false);

        // Info box
        int infoY = top + 94;
        extractor.fill(left + 14, infoY, left + panelW - 14, infoY + 84, 0x88181424);
        extractor.outline(left + 14, infoY, panelW - 28, 84, 0xFF443850);

        extractor.text(this.font, "⚡ Interseptor Paket Server Terpadu:", left + 20, infoY + 6, 0xFFFFD54F, false);
        extractor.text(this.font, "• AuraSkills / Aurelium Skills proc notifikasi", left + 20, infoY + 20, 0xFFDDDDDD, false);
        extractor.text(this.font, "• AdvancedEnchantments (Harvester, Lucky, dsb.)", left + 20, infoY + 34, 0xFFDDDDDD, false);
        extractor.text(this.font, "• Vanilla Fortune & Custom Multiplier Drop", left + 20, infoY + 48, 0xFFDDDDDD, false);
        extractor.text(this.font, "✔ Mengubah format laporan agar rapi atau senyap sesuai keinginan", left + 20, infoY + 64, 0xFF88FF88, false);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (this.parentScreen != null) {
            this.minecraft.gui.setScreen(this.parentScreen);
        } else {
            super.onClose();
        }
    }
}
