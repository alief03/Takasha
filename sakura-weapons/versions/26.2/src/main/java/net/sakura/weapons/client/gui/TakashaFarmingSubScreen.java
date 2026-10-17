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
public class TakashaFarmingSubScreen extends Screen {

    private final Screen parentScreen;
    private final TakashaMasterConfig config;

    private Button toggleBtn;
    private Button autoSellBtn;
    private TakashaKeybindWidget keybindWidget;
    private Button backBtn;

    private static final List<CropOption> CROP_OPTIONS = List.of(
        new CropOption("minecraft:wheat", "Gandum (Wheat)"),
        new CropOption("minecraft:carrots", "Wortel (Carrots)"),
        new CropOption("minecraft:potatoes", "Kentang (Potatoes)"),
        new CropOption("minecraft:beetroots", "Bit Merah (Beetroots)"),
        new CropOption("minecraft:nether_wart", "Nether Wart"),
        new CropOption("minecraft:sugar_cane", "Tebu (Sugar Cane)"),
        new CropOption("minecraft:melon", "Melon"),
        new CropOption("minecraft:pumpkin", "Labu (Pumpkin)")
    );

    public record CropOption(String id, String label) {}

    public TakashaFarmingSubScreen(Screen parentScreen) {
        super(Component.literal("Takasha Auto Farm & SellGUI Settings"));
        this.parentScreen = parentScreen;
        this.config = TakashaMasterConfig.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        int panelW = 380;
        int panelH = 240;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        // Toggle button
        this.toggleBtn = Button.builder(
            Component.literal("Status Auto Farm: " + (config.autoFarmEnabled ? "§aAKTIF" : "§cNONAKTIF")),
            btn -> {
                config.autoFarmEnabled = !config.autoFarmEnabled;
                config.save();
                btn.setMessage(Component.literal("Status Auto Farm: " + (config.autoFarmEnabled ? "§aAKTIF" : "§cNONAKTIF")));
            }
        ).bounds(left + 14, top + 34, 170, 20).build();
        this.addRenderableWidget(this.toggleBtn);

        // Auto SellGUI toggle
        this.autoSellBtn = Button.builder(
            Component.literal("Otomatis /sellgui: " + (config.autoSellGuiOnFull ? "§aON" : "§cOFF")),
            btn -> {
                config.autoSellGuiOnFull = !config.autoSellGuiOnFull;
                config.save();
                btn.setMessage(Component.literal("Otomatis /sellgui: " + (config.autoSellGuiOnFull ? "§aON" : "§cOFF")));
            }
        ).bounds(left + 196, top + 34, 170, 20)
        .tooltip(Tooltip.create(Component.literal("Buka /sellgui & masukkan panen otomatis saat tas penuh (Tanpa laporan penjualan)")))
        .build();
        this.addRenderableWidget(this.autoSellBtn);

        // Crop selection buttons (2 columns x 4 rows)
        int startY = top + 76;
        for (int i = 0; i < CROP_OPTIONS.size(); i++) {
            CropOption crop = CROP_OPTIONS.get(i);
            int col = i % 2;
            int row = i / 2;
            int bx = left + 14 + (col * 182);
            int by = startY + (row * 24);

            Button cropBtn = Button.builder(
                getCropButtonLabel(crop),
                btn -> {
                    if (config.targetCrops.contains(crop.id())) {
                        config.targetCrops.remove(crop.id());
                    } else {
                        config.targetCrops.add(crop.id());
                    }
                    config.save();
                    btn.setMessage(getCropButtonLabel(crop));
                }
            ).bounds(bx, by, 170, 20).build();
            this.addRenderableWidget(cropBtn);
        }

        // Bottom Controls
        int bottomY = top + panelH - 24;
        this.keybindWidget = new TakashaKeybindWidget(
            left + 14,
            bottomY,
            160,
            18,
            "Shortcut Key",
            SakuraWeaponsClient.AUTO_FARM_KEY
        );
        this.addRenderableWidget(this.keybindWidget);

        this.backBtn = Button.builder(
            Component.literal("◀ Kembali"),
            btn -> this.onClose()
        ).bounds(left + panelW - 94, bottomY, 80, 18).build();
        this.addRenderableWidget(this.backBtn);
    }

    private Component getCropButtonLabel(CropOption crop) {
        boolean active = config.targetCrops.contains(crop.id());
        return Component.literal((active ? "§a✔ " : "§7✕ ") + crop.label());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractor.fill(0, 0, this.width, this.height, 0x88000000);

        int panelW = 380;
        int panelH = 240;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        extractor.fill(left, top, left + panelW, top + panelH, 0xEE14121E);
        extractor.outline(left, top, panelW, panelH, 0xFFE082A8);

        // Header
        extractor.fill(left, top, left + panelW, top + 26, 0xFF241528);
        extractor.text(this.font, "🌾 Pengaturan Auto Farm & SellGUI", left + 10, top + 8, 0xFFFFB6C1, false);

        // Section labels
        extractor.text(this.font, "Daftar Tanaman yang Dipanen:", left + 14, top + 64, 0xFFAAAAAA, false);
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
