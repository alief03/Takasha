package net.sakura.weapons.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
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
    private Button reelDelayBtn;
    private Button recastDelayBtn;
    private Button sensitivityBtn;
    private Button rodSwapBtn;
    private Button soundAlertBtn;
    private Button filterCatchBtn;
    private Button capcayBtn;
    private Button rodSlipBtn;
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

        int panelW = 420;
        int panelH = 300;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        // Row 1: Status & Mode (KAIZEN vs VANILLA)
        this.toggleBtn = Button.builder(
            Component.literal("Status: " + (config.autoFishEnabled ? "§aAKTIF" : "§cNONAKTIF")),
            btn -> {
                config.autoFishEnabled = !config.autoFishEnabled;
                config.save();
                btn.setMessage(Component.literal("Status: " + (config.autoFishEnabled ? "§aAKTIF" : "§cNONAKTIF")));
            }
        ).bounds(left + 14, top + 28, 190, 20).build();
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
        ).bounds(left + 214, top + 28, 190, 20)
        .tooltip(Tooltip.create(Component.literal("KAIZEN: Motion Bobber Spike (-0.1 Y) + Auto-Capcay 1.0 + Rod Slip Recovery\nVANILLA: Sound Splash Packet Detection (0% CPU, no Capcay overhead)")))
        .build();
        this.addRenderableWidget(this.modeBtn);

        // Row 2: Reel Delay, Recast Delay, Sensitivitas Gigitan
        int[] reelDelays = {0, 50, 80, 150, 300};
        this.reelDelayBtn = Button.builder(
            Component.literal("Reel: §e" + config.reelInDelayMs + "ms"),
            btn -> {
                int idx = 0;
                for (int i = 0; i < reelDelays.length; i++) {
                    if (config.reelInDelayMs == reelDelays[i]) {
                        idx = (i + 1) % reelDelays.length;
                        break;
                    }
                }
                config.reelInDelayMs = reelDelays[idx];
                config.save();
                btn.setMessage(Component.literal("Reel: §e" + config.reelInDelayMs + "ms"));
            }
        ).bounds(left + 14, top + 52, 126, 20)
        .tooltip(Tooltip.create(Component.literal("Jeda waktu sebelum menarik kail saat umpan digigit (0ms = instan)")))
        .build();
        this.addRenderableWidget(this.reelDelayBtn);

        int[] recastDelays = {250, 500, 1000, 1500};
        this.recastDelayBtn = Button.builder(
            Component.literal("Recast: §e" + config.recastDelayMs + "ms"),
            btn -> {
                int idx = 0;
                for (int i = 0; i < recastDelays.length; i++) {
                    if (config.recastDelayMs == recastDelays[i]) {
                        idx = (i + 1) % recastDelays.length;
                        break;
                    }
                }
                config.recastDelayMs = recastDelays[idx];
                config.save();
                btn.setMessage(Component.literal("Recast: §e" + config.recastDelayMs + "ms"));
            }
        ).bounds(left + 144, top + 52, 130, 20)
        .tooltip(Tooltip.create(Component.literal("Jeda waktu sebelum melempar kembali joran pancing")))
        .build();
        this.addRenderableWidget(this.recastDelayBtn);

        this.sensitivityBtn = Button.builder(
            Component.literal("Sens: §e" + getSensitivityLabel(config.biteSensitivity)),
            btn -> {
                TakashaMasterConfig.BiteSensitivity[] sensitivities = TakashaMasterConfig.BiteSensitivity.values();
                int next = (config.biteSensitivity.ordinal() + 1) % sensitivities.length;
                config.biteSensitivity = sensitivities[next];
                config.save();
                btn.setMessage(Component.literal("Sens: §e" + getSensitivityLabel(config.biteSensitivity)));
            }
        ).bounds(left + 278, top + 52, 126, 20)
        .tooltip(Tooltip.create(Component.literal("Ambang deteksi lonjakan pelampung (Ultra: -0.03, High: -0.06, Normal: -0.10)")))
        .build();
        this.addRenderableWidget(this.sensitivityBtn);

        // Row 3: Auto Rod Swap, Sound Alert, Filter Catch
        this.rodSwapBtn = Button.builder(
            Component.literal("Auto Rod Swap: " + (config.autoSwitchRodOnLowDurability ? "§aON" : "§cOFF")),
            btn -> {
                config.autoSwitchRodOnLowDurability = !config.autoSwitchRodOnLowDurability;
                config.save();
                btn.setMessage(Component.literal("Auto Rod Swap: " + (config.autoSwitchRodOnLowDurability ? "§aON" : "§cOFF")));
            }
        ).bounds(left + 14, top + 76, 126, 20)
        .tooltip(Tooltip.create(Component.literal("Otomatis ganti ke joran baru jika sisa durability ≤ 3")))
        .build();
        this.addRenderableWidget(this.rodSwapBtn);

        this.soundAlertBtn = Button.builder(
            Component.literal("Sound Alert: " + (config.soundAlertOnCatch ? "§aON" : "§cOFF")),
            btn -> {
                config.soundAlertOnCatch = !config.soundAlertOnCatch;
                config.save();
                btn.setMessage(Component.literal("Sound Alert: " + (config.soundAlertOnCatch ? "§aON" : "§cOFF")));
            }
        ).bounds(left + 144, top + 76, 130, 20)
        .tooltip(Tooltip.create(Component.literal("Memutar nada lonceng notifikasi setiap kali berhasil menarik tangkapan")))
        .build();
        this.addRenderableWidget(this.soundAlertBtn);

        this.filterCatchBtn = Button.builder(
            Component.literal("Filter Catch: " + (config.filterCatchEnabled ? "§aON" : "§cOFF")),
            btn -> {
                config.filterCatchEnabled = !config.filterCatchEnabled;
                config.save();
                btn.setMessage(Component.literal("Filter Catch: " + (config.filterCatchEnabled ? "§aON" : "§cOFF")));
            }
        ).bounds(left + 278, top + 76, 126, 20)
        .tooltip(Tooltip.create(Component.literal("Hanya simpan item yang dicentang di bawah. Item lain langsung dibuang!")))
        .build();
        this.addRenderableWidget(this.filterCatchBtn);

        // Row 4: Kaizen features: Auto-Capcay 1.0 & Rod Slip Recovery
        this.capcayBtn = Button.builder(
            Component.literal("Auto-Capcay 1.0: " + (config.autoCapcayEnabled ? "§aON" : "§cOFF")),
            btn -> {
                config.autoCapcayEnabled = !config.autoCapcayEnabled;
                config.save();
                btn.setMessage(Component.literal("Auto-Capcay 1.0: " + (config.autoCapcayEnabled ? "§aON" : "§cOFF")));
            }
        ).bounds(left + 14, top + 100, 190, 20)
        .tooltip(Tooltip.create(Component.literal("Penyelesai CAPCAY otomatis berbasis frekuensi minoritas item (Mode KAIZEN)")))
        .build();
        this.addRenderableWidget(this.capcayBtn);

        this.rodSlipBtn = Button.builder(
            Component.literal("Rod Slip Recovery: " + (config.rodSlipRecovery ? "§aON" : "§cOFF")),
            btn -> {
                config.rodSlipRecovery = !config.rodSlipRecovery;
                config.save();
                btn.setMessage(Component.literal("Rod Slip Recovery: " + (config.rodSlipRecovery ? "§aON" : "§cOFF")));
            }
        ).bounds(left + 214, top + 100, 190, 20)
        .tooltip(Tooltip.create(Component.literal("Otomatis ambil dan pasang kembali joran jika terkena disarm server (Mode KAIZEN)")))
        .build();
        this.addRenderableWidget(this.rodSlipBtn);

        // Row 5+: Catch Whitelist Checklist (2 cols x 4 rows)
        int startY = top + 142;
        for (int i = 0; i < CATCH_OPTIONS.size(); i++) {
            CatchItemOption item = CATCH_OPTIONS.get(i);
            int col = i % 2;
            int row = i / 2;
            int bx = left + 14 + (col * 196);
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
            ).bounds(bx, by, 192, 20).build();
            this.addRenderableWidget(itemBtn);
        }

        // Bottom Controls
        int bottomY = top + panelH - 26;
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

    private String getSensitivityLabel(TakashaMasterConfig.BiteSensitivity s) {
        return switch (s) {
            case ULTRA -> "Ultra (-0.03)";
            case HIGH -> "High (-0.06)";
            case NORMAL -> "Normal (-0.10)";
        };
    }

    private Component getCatchButtonLabel(CatchItemOption item) {
        boolean active = config.keepCatchWhitelist.contains(item.id());
        return Component.literal((active ? "§a✔ " : "§7✕ ") + item.label());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x88000000);

        int panelW = 420;
        int panelH = 300;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        guiGraphics.fill(left, top, left + panelW, top + panelH, 0xEE14121E);
        guiGraphics.renderOutline(left, top, panelW, panelH, 0xFFE082A8);

        // Header
        guiGraphics.fill(left, top, left + panelW, top + 24, 0xFF241528);
        guiGraphics.drawString(this.font, "🎣 Auto Fishing (Kaizen vs Vanilla Engine)", left + 10, top + 7, 0xFFFFB6C1, false);

        // Section labels
        guiGraphics.drawString(this.font, "Item yang Disimpan di Inventory (Keep Whitelist):", left + 14, top + 128, 0xFFAAAAAA, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
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
            this.minecraft.setScreen(this.parentScreen);
        } else {
            super.onClose();
        }
    }
}
