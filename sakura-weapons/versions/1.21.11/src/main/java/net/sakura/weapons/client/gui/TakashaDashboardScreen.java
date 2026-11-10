package net.sakura.weapons.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
    private Button serverSafeBtn;
    private Button closeBtn;
    private int cachedEmoteCount = 0;

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
        this.cachedEmoteCount = EmotecraftCompat.getDetectedEmoteCount();

        int panelW = 420;
        int panelH = 260;
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
            btn -> this.minecraft.setScreen(new TakashaFarmingSubScreen(this))
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
            btn -> this.minecraft.setScreen(new TakashaFishingSubScreen(this))
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
            btn -> this.minecraft.setScreen(new TakashaMobKillSubScreen(this))
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
            btn -> this.minecraft.setScreen(new TakashaAutoBowSubScreen(this))
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
            btn -> this.minecraft.setScreen(new TakashaFortuneSubScreen(this))
        ).bounds(col1X + 98, row3Y + 22, 90, 18).build();
        this.addRenderableWidget(this.fortuneConfigBtn);

        // Row 3, Col 2: Sakura NPC / Replay Studio
        this.studioBtn = Button.builder(
            Component.literal("🎬 Buka Replay Studio"),
            btn -> this.minecraft.setScreen(new TakashaReplayOverrideScreen(null, this))
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
                this.cachedEmoteCount = EmotecraftCompat.getDetectedEmoteCount();
                btn.setMessage(Component.literal("✔ Cache Dimuat Ulang!"));
            }
        ).bounds(col2X + 4, row4Y + 22, 184, 18)
        .tooltip(Tooltip.create(Component.literal("Pindai ulang berkas emote tanpa menimbulkan lag/freeze")))
        .build();
        this.addRenderableWidget(this.refreshEmoteBtn);

        // Bottom Controls
        int bottomY = top + panelH - 24;
        this.masterKeybindWidget = new TakashaKeybindWidget(
            left + 14,
            bottomY,
            120,
            18,
            "Master Menu Key",
            SakuraWeaponsClient.DASHBOARD_KEY
        );
        this.addRenderableWidget(this.masterKeybindWidget);

        Button autoDropBtn = Button.builder(
            Component.literal("🗑 Auto Drop"),
            btn -> this.minecraft.setScreen(new TakashaAutoDropSubScreen(this))
        ).bounds(left + 140, bottomY, 90, 18)
        .tooltip(Tooltip.create(Component.literal("Konfigurasi filter pembuangan item inventaris (Mining, Fishing, Kill)")))
        .build();
        this.addRenderableWidget(autoDropBtn);

        boolean isMultiplayer = isMultiplayerServer();
        this.serverSafeBtn = Button.builder(
            Component.literal("Server Safe: " + (config.serverSafeMode ? "§aON" : "§cOFF")),
            btn -> {
                config.serverSafeMode = !config.serverSafeMode;
                config.save();
                btn.setMessage(Component.literal("Server Safe: " + (config.serverSafeMode ? "§aON" : "§cOFF")));
                updateServerSafeElements(isMultiplayerServer());
            }
        ).bounds(left + 236, bottomY, 96, 18)
        .tooltip(Tooltip.create(Component.literal("Proteksi Server: Otomatis nonaktifkan Replay Studio (F7) & scanner emote saat terhubung ke server multiplayer")))
        .build();
        this.addRenderableWidget(this.serverSafeBtn);

        this.closeBtn = Button.builder(
            Component.literal("Tutup"),
            btn -> this.onClose()
        ).bounds(left + panelW - 80, bottomY, 66, 18).build();
        this.addRenderableWidget(this.closeBtn);

        updateServerSafeElements(isMultiplayer);
    }

    private boolean isMultiplayerServer() {
        return this.minecraft != null && this.minecraft.getCurrentServer() != null && !this.minecraft.isLocalServer();
    }

    private void updateServerSafeElements(boolean isMultiplayer) {
        if (config.serverSafeMode && isMultiplayer) {
            this.studioBtn.active = false;
            this.studioBtn.setTooltip(Tooltip.create(Component.literal("§c[Server Safe] Dinonaktifkan di server multiplayer demi keamanan!")));
            this.refreshEmoteBtn.active = false;
            this.refreshEmoteBtn.setTooltip(Tooltip.create(Component.literal("§c[Server Safe] Pemindaian emote dinonaktifkan di server multiplayer!")));
        } else {
            this.studioBtn.active = true;
            this.studioBtn.setTooltip(Tooltip.create(Component.literal("Pemberi pose mannequin & emote timeline replay studio")));
            this.refreshEmoteBtn.active = true;
            this.refreshEmoteBtn.setTooltip(Tooltip.create(Component.literal("Pindai ulang berkas emote tanpa menimbulkan lag/freeze")));
        }
    }

    private Component getToggleComponent(String name, boolean enabled) {
        return Component.literal(name + ": " + (enabled ? "§aON" : "§cOFF"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Dark translucent overlay
        guiGraphics.fill(0, 0, this.width, this.height, 0x88000000);

        int panelW = 420;
        int panelH = 260;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        // Container Window (Glassmorphism & Sakura pink border)
        guiGraphics.fill(left, top, left + panelW, top + panelH, 0xEE14121E);
        guiGraphics.renderOutline(left, top, panelW, panelH, 0xFFE082A8);

        // Header
        guiGraphics.fill(left, top, left + panelW, top + 26, 0xFF241528);
        guiGraphics.drawString(this.font, "🌸 Takasha Master Utility Dashboard", left + 10, top + 8, 0xFFFFB6C1, false);
        guiGraphics.drawString(this.font, "v2.5.0 (Master Utility)", left + panelW - 145, top + 8, 0xFFDDDDDD, false);

        int col1X = left + 14;
        int col2X = left + 214;
        int cardW = 192;
        int cardH = 46;

        int row1Y = top + 34;
        int row2Y = top + 84;
        int row3Y = top + 134;
        int row4Y = top + 184;

        boolean isMultiplayer = isMultiplayerServer();

        // Card 1: Auto Farm
        String farmSub = "Radius " + config.farmRadiusH + "H/" + config.farmRadiusV + "V | " + config.farmActionsPerTick + "x/tick";
        drawCardFrame(guiGraphics, col1X, row1Y, cardW, cardH, "🌾 Auto Farm & SellGUI", farmSub);

        // Card 2: Auto Fish
        String fishSub = "Mode: " + config.fishingMode.name() + " | Reel: " + config.reelInDelayMs + "ms";
        drawCardFrame(guiGraphics, col2X, row1Y, cardW, cardH, "🎣 Auto Fishing", fishSub);

        // Card 3: Auto Kill Mob
        String killSub = config.mobKillMode.name() + " | " + (int) config.killReach + "m | Rate: " + config.attackRateMode.name();
        drawCardFrame(guiGraphics, col1X, row2Y, cardW, cardH, "⚔️ Auto Kill (Anti-Aura)", killSub);

        // Card 4: Auto Bow
        String bowSub = "Aim Balistik | " + (int) config.autoBowRange + "m | Bebas Gerak";
        drawCardFrame(guiGraphics, col2X, row2Y, cardW, cardH, "🏹 Auto Bow & Aim", bowSub);

        // Card 5: Fortune Reshaper
        String fortSub = "Mode: " + config.fortuneDisplayMode.name() + " (" + config.fortuneDetailLevel.name() + ")";
        drawCardFrame(guiGraphics, col1X, row3Y, cardW, cardH, "💎 Fortune Reshaper", fortSub);

        // Card 6: Replay Studio
        String studioSub = (config.serverSafeMode && isMultiplayer) ? "§cDinonaktifkan (Multiplayer)" : "Animasi mannequin timeline";
        drawCardFrame(guiGraphics, col2X, row3Y, cardW, cardH, "🎭 NPC Pose & Replay", studioSub);

        // Card 7: Dynamic Nametag
        drawCardFrame(guiGraphics, col1X, row4Y, cardW, cardH, "🏷️ Dynamic Nametag", "Visibilitas identitas pemain");

        // Card 8: Emote 0-Lag Cache
        String emoteSub = (config.serverSafeMode && isMultiplayer) ? "§cProteksi Server Aktif" : (this.cachedEmoteCount + " Emote Siap (0-Lag)");
        drawCardFrame(guiGraphics, col2X, row4Y, cardW, cardH, "💃 Emote Cache", emoteSub);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawCardFrame(GuiGraphics guiGraphics, int x, int y, int w, int h, String title, String sub) {
        guiGraphics.fill(x, y, x + w, y + h, 0x88201A2C);
        guiGraphics.renderOutline(x, y, w, h, 0xFF443850);
        guiGraphics.drawString(this.font, title, x + 5, y + 4, 0xFFFFD54F, false);
        guiGraphics.drawString(this.font, sub, x + 5, y + 13, 0xFFAAAAAA, false);
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
            this.minecraft.setScreen(this.parentScreen);
        } else {
            super.onClose();
        }
    }
}
