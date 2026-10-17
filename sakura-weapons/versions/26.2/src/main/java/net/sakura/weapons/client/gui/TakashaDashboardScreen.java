package net.sakura.weapons.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.sakura.weapons.SakuraWeaponsClient;
import net.sakura.weapons.client.config.TakashaMasterConfig;
import net.sakura.weapons.client.config.TakashaNametagConfig;
import net.sakura.weapons.client.gui.widget.TakashaKeybindWidget;
import net.sakura.weapons.compat.EmotecraftCompat;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class TakashaDashboardScreen extends Screen {

    private final Screen parentScreen;
    private final TakashaMasterConfig config;

    // Card toggles & action buttons
    private Button autoFarmToggleBtn;
    private Button autoFarmConfigBtn;

    private Button autoFishToggleBtn;
    private Button autoFishConfigBtn;

    private Button autoKillToggleBtn;
    private Button autoKillConfigBtn;

    private Button autoBowToggleBtn;
    private Button autoBowConfigBtn;

    private Button fortuneToggleBtn;
    private Button fortuneConfigBtn;

    private Button studioBtn;
    private Button nametagToggleBtn;
    private Button refreshEmoteBtn;

    private TakashaKeybindWidget masterKeybindWidget;
    private Button closeBtn;

    public TakashaDashboardScreen(Screen parentScreen) {
        super(Component.literal("Takasha Master Utility Dashboard"));
        this.parentScreen = parentScreen;
        this.config = TakashaMasterConfig.getInstance();
    }

    public TakashaDashboardScreen() {
        this(null);
    }

    @Override
    protected void init() {
        super.init();

        int panelW = 420;
        int panelH = 310;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        int col1X = left + 14;
        int col2X = left + 214;
        int cardW = 192;
        int cardH = 46;

        int row1Y = top + 34;
        int row2Y = top + 84;
        int row3Y = top + 134;
        int row4Y = top + 184;
        int row5Y = top + 234;

        // Row 1, Col 1: Auto Farm
        this.autoFarmToggleBtn = Button.builder(
            getToggleComponent("Auto Farm", config.autoFarmEnabled),
            btn -> {
                config.autoFarmEnabled = !config.autoFarmEnabled;
                config.save();
                btn.setMessage(getToggleComponent("Auto Farm", config.autoFarmEnabled));
            }
        ).bounds(col1X + 4, row1Y + 22, 90, 18).build();
        this.addRenderableWidget(this.autoFarmToggleBtn);

        this.autoFarmConfigBtn = Button.builder(
            Component.literal("⚙ Opsi..."),
            btn -> this.minecraft.gui.setScreen(new TakashaFarmingSubScreen(this))
        ).bounds(col1X + 98, row1Y + 22, 90, 18).build();
        this.addRenderableWidget(this.autoFarmConfigBtn);

        // Row 1, Col 2: Auto Fish
        this.autoFishToggleBtn = Button.builder(
            getToggleComponent("Auto Fish", config.autoFishEnabled),
            btn -> {
                config.autoFishEnabled = !config.autoFishEnabled;
                config.save();
                btn.setMessage(getToggleComponent("Auto Fish", config.autoFishEnabled));
            }
        ).bounds(col2X + 4, row1Y + 22, 90, 18).build();
        this.addRenderableWidget(this.autoFishToggleBtn);

        this.autoFishConfigBtn = Button.builder(
            Component.literal("⚙ Opsi..."),
            btn -> this.minecraft.gui.setScreen(new TakashaFishingSubScreen(this))
        ).bounds(col2X + 98, row1Y + 22, 90, 18).build();
        this.addRenderableWidget(this.autoFishConfigBtn);

        // Row 2, Col 1: Auto Kill Mob
        this.autoKillToggleBtn = Button.builder(
            getToggleComponent("Auto Kill", config.autoKillEnabled),
            btn -> {
                config.autoKillEnabled = !config.autoKillEnabled;
                config.save();
                btn.setMessage(getToggleComponent("Auto Kill", config.autoKillEnabled));
            }
        ).bounds(col1X + 4, row2Y + 22, 90, 18).build();
        this.addRenderableWidget(this.autoKillToggleBtn);

        this.autoKillConfigBtn = Button.builder(
            Component.literal("⚙ Opsi..."),
            btn -> this.minecraft.gui.setScreen(new TakashaMobKillSubScreen(this))
        ).bounds(col1X + 98, row2Y + 22, 90, 18).build();
        this.addRenderableWidget(this.autoKillConfigBtn);

        // Row 2, Col 2: Auto Bow
        this.autoBowToggleBtn = Button.builder(
            getToggleComponent("Auto Bow", config.autoBowEnabled),
            btn -> {
                config.autoBowEnabled = !config.autoBowEnabled;
                config.save();
                btn.setMessage(getToggleComponent("Auto Bow", config.autoBowEnabled));
            }
        ).bounds(col2X + 4, row2Y + 22, 90, 18).build();
        this.addRenderableWidget(this.autoBowToggleBtn);

        this.autoBowConfigBtn = Button.builder(
            Component.literal("⚙ Opsi..."),
            btn -> this.minecraft.gui.setScreen(new TakashaAutoBowSubScreen(this))
        ).bounds(col2X + 98, row2Y + 22, 90, 18).build();
        this.addRenderableWidget(this.autoBowConfigBtn);

        // Row 3, Col 1: Fortune Reshaper
        this.fortuneToggleBtn = Button.builder(
            getToggleComponent("Reshaper", config.fortuneReshaperEnabled),
            btn -> {
                config.fortuneReshaperEnabled = !config.fortuneReshaperEnabled;
                config.save();
                btn.setMessage(getToggleComponent("Reshaper", config.fortuneReshaperEnabled));
            }
        ).bounds(col1X + 4, row3Y + 22, 90, 18).build();
        this.addRenderableWidget(this.fortuneToggleBtn);

        this.fortuneConfigBtn = Button.builder(
            Component.literal("⚙ Opsi..."),
            btn -> this.minecraft.gui.setScreen(new TakashaFortuneSubScreen(this))
        ).bounds(col1X + 98, row3Y + 22, 90, 18).build();
        this.addRenderableWidget(this.fortuneConfigBtn);

        // Row 3, Col 2: Sakura NPC / Replay Studio
        this.studioBtn = Button.builder(
            Component.literal("🎬 Buka Replay Studio"),
            btn -> this.minecraft.gui.setScreen(new TakashaReplayOverrideScreen(null, this))
        ).bounds(col2X + 4, row3Y + 22, 184, 18)
        .tooltip(Tooltip.create(Component.literal("Pemberi pose mannequin & emote timeline replay studio")))
        .build();
        this.addRenderableWidget(this.studioBtn);

        // Row 4, Col 1: Dynamic Nametag
        boolean nametagDisabled = TakashaNametagConfig.isPlayerNametagDisabled();
        this.nametagToggleBtn = Button.builder(
            Component.literal(nametagDisabled ? "Nametag: §cTERSEMBUNYI" : "Nametag: §aMUNCUL"),
            btn -> {
                if (this.minecraft.player != null) {
                    boolean nowDisabled = TakashaNametagConfig.toggle(this.minecraft.player);
                    btn.setMessage(Component.literal(nowDisabled ? "Nametag: §cTERSEMBUNYI" : "Nametag: §aMUNCUL"));
                }
            }
        ).bounds(col1X + 4, row4Y + 22, 184, 18).build();
        this.addRenderableWidget(this.nametagToggleBtn);

        // Row 4, Col 2: Emote 0-Lag Cache Refresh
        this.refreshEmoteBtn = Button.builder(
            Component.literal("🔄 Segarkan Emote Cache"),
            btn -> {
                EmotecraftCompat.refreshEmotes();
                btn.setMessage(Component.literal("✔ Cache Dimuat Ulang!"));
            }
        ).bounds(col2X + 4, row4Y + 22, 184, 18)
        .tooltip(Tooltip.create(Component.literal("Pindai ulang berkas emote tanpa menimbulkan lag/freeze")))
        .build();
        this.addRenderableWidget(this.refreshEmoteBtn);

        // Row 5: Touhou Maid Companion
        Button maidGalleryBtn = Button.builder(
            Component.literal("🎎 Gallery & Model Maid..."),
            btn -> this.minecraft.gui.setScreen(new TakashaMaidSubScreen(this))
        ).bounds(col1X + 4, row5Y + 22, 184, 18)
        .tooltip(Tooltip.create(Component.literal("Buka Model Gallery 3D Maid, ganti model karakter, atau atur perilaku")))
        .build();
        this.addRenderableWidget(maidGalleryBtn);

        Button maidFolderBtn = Button.builder(
            Component.literal("📂 Buka Folder Model"),
            btn -> {
                java.nio.file.Path p = java.nio.file.Path.of(this.minecraft.gameDirectory.getAbsolutePath(), "maid_models");
                net.minecraft.util.Util.getPlatform().openPath(p);
            }
        ).bounds(col2X + 4, row5Y + 22, 184, 18)
        .tooltip(Tooltip.create(Component.literal("Buka folder .minecraft/maid_models untuk memasukkan model Bedrock JSON & Texture PNG baru")))
        .build();
        this.addRenderableWidget(maidFolderBtn);

        // Bottom Controls
        int bottomY = top + panelH - 24;
        this.masterKeybindWidget = new TakashaKeybindWidget(
            left + 14,
            bottomY,
            160,
            18,
            "Master Menu Key",
            SakuraWeaponsClient.DASHBOARD_KEY
        );
        this.addRenderableWidget(this.masterKeybindWidget);

        this.closeBtn = Button.builder(
            Component.literal("Tutup [ESC]"),
            btn -> this.onClose()
        ).bounds(left + panelW - 94, bottomY, 80, 18).build();
        this.addRenderableWidget(this.closeBtn);
    }

    private Component getToggleComponent(String name, boolean enabled) {
        return Component.literal(name + ": " + (enabled ? "§aON" : "§cOFF"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // Dark translucent overlay
        extractor.fill(0, 0, this.width, this.height, 0x88000000);

        int panelW = 420;
        int panelH = 310;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        // Container Window (Glassmorphism & Sakura pink border)
        extractor.fill(left, top, left + panelW, top + panelH, 0xEE14121E);
        extractor.outline(left, top, panelW, panelH, 0xFFE082A8);

        // Header
        extractor.fill(left, top, left + panelW, top + 26, 0xFF241528);
        extractor.text(this.font, "🌸 Takasha Master Utility Dashboard", left + 10, top + 8, 0xFFFFB6C1, false);
        extractor.text(this.font, "v2.5.0 (Touhou Little Maid 26.2)", left + panelW - 195, top + 8, 0xFFDDDDDD, false);

        int col1X = left + 14;
        int col2X = left + 214;
        int cardW = 192;
        int cardH = 46;

        int row1Y = top + 34;
        int row2Y = top + 84;
        int row3Y = top + 134;
        int row4Y = top + 184;
        int row5Y = top + 234;

        // Card 1: Auto Farm
        drawCardFrame(extractor, col1X, row1Y, cardW, cardH, "🌾 Auto Farm & SellGUI", "Otomatis panen & /sellgui");

        // Card 2: Auto Fish
        String fishSub = "Mode: " + config.fishingMode.name() + (config.autoCapcayEnabled ? " | Capcay 1.0" : "");
        drawCardFrame(extractor, col2X, row1Y, cardW, cardH, "🎣 Auto Fishing", fishSub);

        // Card 3: Auto Kill Mob
        String killSub = "Frontal ≤ " + (int) config.maxFovAngle + "° | " + (config.antiAfkEnabled ? "Anti-AFK" : "Aman");
        drawCardFrame(extractor, col1X, row2Y, cardW, cardH, "⚔️ Auto Kill (Anti-Aura)", killSub);

        // Card 4: Auto Bow
        String bowSub = "Aim Balistik | " + (int) config.autoBowRange + "m | Bebas Gerak";
        drawCardFrame(extractor, col2X, row2Y, cardW, cardH, "🏹 Auto Bow & Aim", bowSub);

        // Card 5: Fortune Reshaper
        String fortSub = "Mode: " + config.fortuneDisplayMode.name() + " (" + config.fortuneDetailLevel.name() + ")";
        drawCardFrame(extractor, col1X, row3Y, cardW, cardH, "💎 Fortune Reshaper", fortSub);

        // Card 6: Replay Studio
        drawCardFrame(extractor, col2X, row3Y, cardW, cardH, "🎭 NPC Pose & Replay", "Animasi mannequin timeline");

        // Card 7: Dynamic Nametag
        drawCardFrame(extractor, col1X, row4Y, cardW, cardH, "🏷️ Dynamic Nametag", "Visibilitas identitas pemain");

        // Card 8: Emote 0-Lag Cache
        int count = EmotecraftCompat.getDetectedEmotes().size();
        drawCardFrame(extractor, col2X, row4Y, cardW, cardH, "💃 Emote Cache", count + " Emote Siap (0-Lag)");

        // Card 9: Touhou Maid Companion
        drawCardFrame(extractor, col1X, row5Y, cardW * 2 + 8, cardH, "🎎 Maid Companion (Touhou Little Maid Engine)", "189 model Bedrock bawaan + custom pack di folder maid_models");
    }

    private void drawCardFrame(GuiGraphicsExtractor extractor, int x, int y, int w, int h, String title, String sub) {
        extractor.fill(x, y, x + w, y + h, 0x88201A2C);
        extractor.outline(x, y, w, h, 0xFF443850);
        extractor.text(this.font, title, x + 5, y + 4, 0xFFFFD54F, false);
        extractor.text(this.font, sub, x + 5, y + 13, 0xFFAAAAAA, false);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.masterKeybindWidget != null && this.masterKeybindWidget.isListening()) {
            return this.masterKeybindWidget.keyPressed(event);
        }
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
