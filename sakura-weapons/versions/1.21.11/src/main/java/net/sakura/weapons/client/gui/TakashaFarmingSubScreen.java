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
public class TakashaFarmingSubScreen extends Screen {

    private final Screen parentScreen;
    private final TakashaMasterConfig config;

    private Button toggleBtn;
    private Button directionBtn;
    private Button radiusHBtn;
    private Button radiusVBtn;
    private Button delayBtn;
    private Button batchBtn;
    private Button cropMemoryBtn;
    private Button pacingBtn;
    private Button emptyFarmlandBtn;
    private Button replantDelayBtn;
    private Button autoSellBtn;
    private Button reserveBtn;
    private TakashaKeybindWidget keybindWidget;
    private Button backBtn;

    private static final List<CropOption> CROP_OPTIONS = List.of(
        new CropOption("minecraft:wheat", "Gandum (Wheat)"),
        new CropOption("minecraft:carrots", "Wortel (Carrots)"),
        new CropOption("minecraft:potatoes", "Kentang (Potatoes)"),
        new CropOption("minecraft:beetroots", "Bit Merah (Beetroots)"),
        new CropOption("minecraft:nether_wart", "Nether Wart"),
        new CropOption("minecraft:cocoa", "Biji Kakao (Cocoa)"),
        new CropOption("minecraft:sweet_berry_bush", "Sweet Berries"),
        new CropOption("minecraft:sugar_cane", "Tebu (Sugar Cane)"),
        new CropOption("minecraft:cactus", "Kaktus (Cactus)"),
        new CropOption("minecraft:bamboo", "Bambu (Bamboo)"),
        new CropOption("minecraft:kelp", "Rumput Laut (Kelp)"),
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

        int panelW = 420;
        int panelH = 320;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        int col1X = left + 14;
        int col2X = left + 214;
        int btnW = 192;

        // Row 1: Auto Farm Status & Direction Mode
        this.toggleBtn = Button.builder(
            Component.literal("Auto Farm: " + (config.autoFarmEnabled ? "§aAKTIF" : "§cNONAKTIF")),
            btn -> {
                config.autoFarmEnabled = !config.autoFarmEnabled;
                config.save();
                btn.setMessage(Component.literal("Auto Farm: " + (config.autoFarmEnabled ? "§aAKTIF" : "§cNONAKTIF")));
            }
        ).bounds(col1X, top + 26, btnW, 18).build();
        this.addRenderableWidget(this.toggleBtn);

        this.directionBtn = Button.builder(
            Component.literal("Arah: " + (config.farmOnlyInFront ? "§aDi Depan Saja" : "§bSekeliling 360°")),
            btn -> {
                config.farmOnlyInFront = !config.farmOnlyInFront;
                config.save();
                btn.setMessage(Component.literal("Arah: " + (config.farmOnlyInFront ? "§aDi Depan Saja" : "§bSekeliling 360°")));
            }
        ).bounds(col2X, top + 26, btnW, 18)
        .tooltip(Tooltip.create(Component.literal("Di Depan Saja: Hanya panen crop di depan pandangan (Aman Anticheat).\nSekeliling: Panen 360 derajat di sekitar pemain.")))
        .build();
        this.addRenderableWidget(this.directionBtn);

        // Row 2: Horizontal Reach & Vertical Reach
        this.radiusHBtn = Button.builder(
            Component.literal("Jangkauan H: §e" + config.farmRadiusH + " Blok" + (config.farmRadiusH == 6 ? " (Max)" : "")),
            btn -> {
                int[] vals = {1, 2, 3, 4, 5, 6};
                int idx = 0;
                for (int i = 0; i < vals.length; i++) {
                    if (config.farmRadiusH == vals[i]) {
                        idx = (i + 1) % vals.length;
                        break;
                    }
                }
                config.farmRadiusH = vals[idx];
                config.save();
                btn.setMessage(Component.literal("Jangkauan H: §e" + config.farmRadiusH + " Blok" + (config.farmRadiusH == 6 ? " (Max)" : "")));
            }
        ).bounds(col1X, top + 46, btnW, 18)
        .tooltip(Tooltip.create(Component.literal("Jarak radius horizontal pemanenan & penanaman (1 s/d 6 blok).")))
        .build();
        this.addRenderableWidget(this.radiusHBtn);

        this.radiusVBtn = Button.builder(
            Component.literal("Jangkauan V: §e" + config.farmRadiusV + " Blok"),
            btn -> {
                int[] vals = {1, 2, 3};
                int idx = 0;
                for (int i = 0; i < vals.length; i++) {
                    if (config.farmRadiusV == vals[i]) {
                        idx = (i + 1) % vals.length;
                        break;
                    }
                }
                config.farmRadiusV = vals[idx];
                config.save();
                btn.setMessage(Component.literal("Jangkauan V: §e" + config.farmRadiusV + " Blok"));
            }
        ).bounds(col2X, top + 46, btnW, 18)
        .tooltip(Tooltip.create(Component.literal("Jarak radius vertikal (ketinggian tanah bertingkat/terasering).")))
        .build();
        this.addRenderableWidget(this.radiusVBtn);

        // Row 3: Action Delay & Actions Per Tick (Batch)
        this.delayBtn = Button.builder(
            Component.literal("Delay Aksi: §e" + (config.farmActionDelayMs == 0 ? "0ms (Instan)" : config.farmActionDelayMs + "ms")),
            btn -> {
                int[] vals = {0, 50, 100, 200};
                int idx = 0;
                for (int i = 0; i < vals.length; i++) {
                    if (config.farmActionDelayMs == vals[i]) {
                        idx = (i + 1) % vals.length;
                        break;
                    }
                }
                config.farmActionDelayMs = vals[idx];
                config.save();
                btn.setMessage(Component.literal("Delay Aksi: §e" + (config.farmActionDelayMs == 0 ? "0ms (Instan)" : config.farmActionDelayMs + "ms")));
            }
        ).bounds(col1X, top + 66, btnW, 18)
        .tooltip(Tooltip.create(Component.literal("Jeda waktu antar pemindaian panen (0ms = kecepatan tertinggi).")))
        .build();
        this.addRenderableWidget(this.delayBtn);

        this.batchBtn = Button.builder(
            Component.literal("Batch/Tick: §e" + config.farmActionsPerTick + "x" + (config.farmActionsPerTick == 12 ? " (Max)" : "")),
            btn -> {
                int[] vals = {1, 2, 4, 8, 12};
                int idx = 0;
                for (int i = 0; i < vals.length; i++) {
                    if (config.farmActionsPerTick == vals[i]) {
                        idx = (i + 1) % vals.length;
                        break;
                    }
                }
                config.farmActionsPerTick = vals[idx];
                config.save();
                btn.setMessage(Component.literal("Batch/Tick: §e" + config.farmActionsPerTick + "x" + (config.farmActionsPerTick == 12 ? " (Max)" : "")));
            }
        ).bounds(col2X, top + 66, btnW, 18)
        .tooltip(Tooltip.create(Component.literal("Jumlah maksimal crop yang dipanen/ditanam dalam 1 game tick.")))
        .build();
        this.addRenderableWidget(this.batchBtn);

        // Row 4: Crop Plot Memory & Replant Pacing
        this.cropMemoryBtn = Button.builder(
            Component.literal("Memori Lahan: " + (config.cropMemoryEnabled ? "§aON (Aktif)" : "§cOFF")),
            btn -> {
                config.cropMemoryEnabled = !config.cropMemoryEnabled;
                config.save();
                btn.setMessage(Component.literal("Memori Lahan: " + (config.cropMemoryEnabled ? "§aON (Aktif)" : "§cOFF")));
            }
        ).bounds(col1X, top + 86, btnW, 18)
        .tooltip(Tooltip.create(Component.literal("Mengingat jenis tanaman di tiap petak selama sesi aktif untuk pemulihan lahan bolong presisi.")))
        .build();
        this.addRenderableWidget(this.cropMemoryBtn);

        this.pacingBtn = Button.builder(
            Component.literal("Pacing Replant: " + (config.replantPacingEnabled ? "§aON (Seimbang)" : "§cOFF")),
            btn -> {
                config.replantPacingEnabled = !config.replantPacingEnabled;
                config.save();
                btn.setMessage(Component.literal("Pacing Replant: " + (config.replantPacingEnabled ? "§aON (Seimbang)" : "§cOFF")));
            }
        ).bounds(col2X, top + 86, btnW, 18)
        .tooltip(Tooltip.create(Component.literal("Flow Control: Mengimbangi kecepatan panen dengan replant (rem panen jika ada petak bolong di dekat pemain).")))
        .build();
        this.addRenderableWidget(this.pacingBtn);

        // Row 5: Auto Plant Empty Farmland & Replant Delay
        this.emptyFarmlandBtn = Button.builder(
            Component.literal("Tanam Farmland Kosong: " + (config.autoPlantEmptyFarmland ? "§aON" : "§cOFF")),
            btn -> {
                config.autoPlantEmptyFarmland = !config.autoPlantEmptyFarmland;
                config.save();
                btn.setMessage(Component.literal("Tanam Farmland Kosong: " + (config.autoPlantEmptyFarmland ? "§aON" : "§cOFF")));
            }
        ).bounds(col1X, top + 106, btnW, 18)
        .tooltip(Tooltip.create(Component.literal("Otomatis menanam bibit yang cocok ke tanah ladang kosong di area.")))
        .build();
        this.addRenderableWidget(this.emptyFarmlandBtn);

        this.replantDelayBtn = Button.builder(
            Component.literal("Delay Tanam: §e" + (config.farmReplantDelayMs == 0 ? "0ms (Instan)" : config.farmReplantDelayMs + "ms")),
            btn -> {
                int[] vals = {0, 50, 120, 250};
                int idx = 0;
                for (int i = 0; i < vals.length; i++) {
                    if (config.farmReplantDelayMs == vals[i]) {
                        idx = (i + 1) % vals.length;
                        break;
                    }
                }
                config.farmReplantDelayMs = vals[idx];
                config.save();
                btn.setMessage(Component.literal("Delay Tanam: §e" + (config.farmReplantDelayMs == 0 ? "0ms (Instan)" : config.farmReplantDelayMs + "ms")));
            }
        ).bounds(col2X, top + 106, btnW, 18)
        .tooltip(Tooltip.create(Component.literal("Jeda waktu sebelum menanam kembali benih setelah crop hancur.")))
        .build();
        this.addRenderableWidget(this.replantDelayBtn);

        // Row 6: Auto /sellgui & Seed Reserve
        this.autoSellBtn = Button.builder(
            Component.literal("Otomatis /sellgui: " + (config.autoSellGuiOnFull ? "§aON" : "§cOFF")),
            btn -> {
                config.autoSellGuiOnFull = !config.autoSellGuiOnFull;
                config.save();
                btn.setMessage(Component.literal("Otomatis /sellgui: " + (config.autoSellGuiOnFull ? "§aON" : "§cOFF")));
            }
        ).bounds(col1X, top + 126, btnW, 18)
        .tooltip(Tooltip.create(Component.literal("Buka /sellgui & masukkan panen otomatis saat tas penuh (Tanpa laporan penjualan)")))
        .build();
        this.addRenderableWidget(this.autoSellBtn);

        this.reserveBtn = Button.builder(
            Component.literal("Cadangan Benih: §e" + config.seedReserveCount + " biji"),
            btn -> {
                int[] vals = {8, 16, 32, 64};
                int idx = 0;
                for (int i = 0; i < vals.length; i++) {
                    if (config.seedReserveCount == vals[i]) {
                        idx = (i + 1) % vals.length;
                        break;
                    }
                }
                config.seedReserveCount = vals[idx];
                config.save();
                btn.setMessage(Component.literal("Cadangan Benih: §e" + config.seedReserveCount + " biji"));
            }
        ).bounds(col2X, top + 126, btnW, 18)
        .tooltip(Tooltip.create(Component.literal("Jumlah benih/hasil panen yang disisakan di tas agar bisa terus ditanam kembali.")))
        .build();
        this.addRenderableWidget(this.reserveBtn);

        // Crop selection buttons (2 columns x 7 rows)
        int startY = top + 160;
        for (int i = 0; i < CROP_OPTIONS.size(); i++) {
            CropOption crop = CROP_OPTIONS.get(i);
            int col = i % 2;
            int row = i / 2;
            int bx = col == 0 ? col1X : col2X;
            int by = startY + (row * 18);

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
            ).bounds(bx, by, btnW, 17).build();
            this.addRenderableWidget(cropBtn);
        }

        // Bottom Controls
        int bottomY = top + panelH - 22;
        this.keybindWidget = new TakashaKeybindWidget(
            left + 14,
            bottomY,
            150,
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x88000000);

        int panelW = 420;
        int panelH = 320;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        guiGraphics.fill(left, top, left + panelW, top + panelH, 0xEE14121E);
        guiGraphics.renderOutline(left, top, panelW, panelH, 0xFFE082A8);

        // Header
        guiGraphics.fill(left, top, left + panelW, top + 24, 0xFF241528);
        guiGraphics.drawString(this.font, "🌾 Pengaturan Auto Farm & SellGUI", left + 10, top + 7, 0xFFFFB6C1, false);

        // Section labels
        guiGraphics.drawString(this.font, "Daftar Tanaman yang Dipanen:", left + 14, top + 148, 0xFFAAAAAA, false);

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
