package net.sakura.weapons.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.sakura.weapons.SakuraWeaponsClient;
import net.sakura.weapons.client.config.TakashaMasterConfig;
import net.sakura.weapons.client.gui.widget.TakashaKeybindWidget;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Environment(EnvType.CLIENT)
public class TakashaFishingSubScreen extends Screen {

    private final Screen parentScreen;
    private final TakashaMasterConfig config;

    private Button toggleBtn;
    private Button modeBtn;
    private Button capcayBtn;
    private Button rodSlipBtn;
    private Button filterCatchBtn;
    private TakashaKeybindWidget keybindWidget;
    private Button backBtn;

    private static final List<CatchItemOption> CATCH_OPTIONS = List.of(
        new CatchItemOption("minecraft:enchanted_book", "Enchanted Book (Buku Sihir)"),
        new CatchItemOption("minecraft:nautilus_shell", "Nautilus Shell"),
        new CatchItemOption("minecraft:saddle", "Saddle (Pelana)"),
        new CatchItemOption("minecraft:name_tag", "Name Tag"),
        new CatchItemOption("minecraft:bow", "Bow (Busur)"),
        new CatchItemOption("minecraft:fishing_rod", "Fishing Rod (Joran)"),
        new CatchItemOption("minecraft:cod", "Ikan Mentah (Raw Cod)"),
        new CatchItemOption("minecraft:salmon", "Ikan Salmon (Raw Salmon)")
    );

    public record CatchItemOption(String id, String label) {}

    public TakashaFishingSubScreen(Screen parentScreen) {
        super(Component.literal("Takasha Auto Fishing Settings"));
        this.parentScreen = parentScreen;
        this.config = TakashaMasterConfig.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        int panelW = 400;
        int panelH = 260;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        // Row 1: Status & Mode
        this.toggleBtn = Button.builder(
            Component.literal("Status: " + (config.autoFishEnabled ? "§aAKTIF" : "§cNONAKTIF")),
            btn -> {
                config.autoFishEnabled = !config.autoFishEnabled;
                config.save();
                btn.setMessage(Component.literal("Status: " + (config.autoFishEnabled ? "§aAKTIF" : "§cNONAKTIF")));
            }
        ).bounds(left + 14, top + 32, 180, 20).build();
        this.addRenderableWidget(this.toggleBtn);

        this.modeBtn = Button.builder(
            Component.literal("Mode: §b" + config.fishingMode.name()),
            btn -> {
                config.fishingMode = (config.fishingMode == TakashaMasterConfig.FishingMode.KAIZEN)
                    ? TakashaMasterConfig.FishingMode.VANILLA
                    : TakashaMasterConfig.FishingMode.KAIZEN;
                config.save();
                btn.setMessage(Component.literal("Mode: §b" + config.fishingMode.name()));
            }
        ).bounds(left + 206, top + 32, 180, 20)
        .tooltip(Tooltip.create(Component.literal("KAIZEN: Motion Bobber + Auto-Capcay 1.0\nVANILLA: Sound Splash Detection")))
        .build();
        this.addRenderableWidget(this.modeBtn);

        // Row 2: Capcay & Rod Slip & Filter Catch
        this.capcayBtn = Button.builder(
            Component.literal("Auto-Capcay 1.0: " + (config.autoCapcayEnabled ? "§aON" : "§cOFF")),
            btn -> {
                config.autoCapcayEnabled = !config.autoCapcayEnabled;
                config.save();
                btn.setMessage(Component.literal("Auto-Capcay 1.0: " + (config.autoCapcayEnabled ? "§aON" : "§cOFF")));
            }
        ).bounds(left + 14, top + 56, 116, 20)
        .tooltip(Tooltip.create(Component.literal("Penyelesai tantangan CAPCAY minoritas Kaizen otomatis")))
        .build();
        this.addRenderableWidget(this.capcayBtn);

        this.rodSlipBtn = Button.builder(
            Component.literal("Rod Recovery: " + (config.rodSlipRecovery ? "§aON" : "§cOFF")),
            btn -> {
                config.rodSlipRecovery = !config.rodSlipRecovery;
                config.save();
                btn.setMessage(Component.literal("Rod Recovery: " + (config.rodSlipRecovery ? "§aON" : "§cOFF")));
            }
        ).bounds(left + 134, top + 56, 126, 20)
        .tooltip(Tooltip.create(Component.literal("Pulihkan joran saat slot terlempar / server slot disarm")))
        .build();
        this.addRenderableWidget(this.rodSlipBtn);

        this.filterCatchBtn = Button.builder(
            Component.literal("Filter Catch: " + (config.filterCatchEnabled ? "§aON" : "§cOFF")),
            btn -> {
                config.filterCatchEnabled = !config.filterCatchEnabled;
                config.save();
                btn.setMessage(Component.literal("Filter Catch: " + (config.filterCatchEnabled ? "§aON" : "§cOFF")));
            }
        ).bounds(left + 264, top + 56, 122, 20)
        .tooltip(Tooltip.create(Component.literal("Hanya simpan item yang dicentang di bawah. Item lain langsung dibuang!")))
        .build();
        this.addRenderableWidget(this.filterCatchBtn);

        // Row 3+: Catch Whitelist Checklist (2 cols x 4 rows)
        int startY = top + 98;
        for (int i = 0; i < CATCH_OPTIONS.size(); i++) {
            CatchItemOption item = CATCH_OPTIONS.get(i);
            int col = i % 2;
            int row = i / 2;
            int bx = left + 14 + (col * 190);
            int by = startY + (row * 24);

            Button itemBtn = Button.builder(
                getCatchButtonLabel(item),
                btn -> {
                    if (config.keepCatchWhitelist.contains(item.id())) {
                        config.keepCatchWhitelist.remove(item.id());
                    } else {
                        config.keepCatchWhitelist.add(item.id());
                    }
                    config.save();
                    btn.setMessage(getCatchButtonLabel(item));
                }
            ).bounds(bx, by, 182, 20).build();
            this.addRenderableWidget(itemBtn);
        }

        // Bottom Controls
        int bottomY = top + panelH - 24;
        this.keybindWidget = new TakashaKeybindWidget(
            left + 14,
            bottomY,
            160,
            18,
            "Shortcut Key",
            SakuraWeaponsClient.AUTO_FISH_KEY
        );
        this.addRenderableWidget(this.keybindWidget);

        this.backBtn = Button.builder(
            Component.literal("◀ Kembali"),
            btn -> this.onClose()
        ).bounds(left + panelW - 94, bottomY, 80, 18).build();
        this.addRenderableWidget(this.backBtn);
    }

    private Component getCatchButtonLabel(CatchItemOption item) {
        boolean keep = config.keepCatchWhitelist.contains(item.id());
        return Component.literal((keep ? "§a✔ [SIMPAN] " : "§c✕ [BUANG] ") + item.label());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractor.fill(0, 0, this.width, this.height, 0x88000000);

        int panelW = 400;
        int panelH = 260;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        extractor.fill(left, top, left + panelW, top + panelH, 0xEE14121E);
        extractor.outline(left, top, panelW, panelH, 0xFFE082A8);

        // Header
        extractor.fill(left, top, left + panelW, top + 26, 0xFF241528);
        extractor.text(this.font, "🎣 Pengaturan Auto Fishing & Filter Tangkapan", left + 10, top + 8, 0xFFFFB6C1, false);

        // Section labels
        extractor.text(this.font, "Item yang DISIMPAN di Inventory (Sisanya dibuang otomatis):", left + 14, top + 86, 0xFFAAAAAA, false);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.keybindWidget != null && this.keybindWidget.isListening()) {
            return this.keybindWidget.keyPressed(event);
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
