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
public class TakashaAutoBowSubScreen extends Screen {

    private final Screen parentScreen;
    private final TakashaMasterConfig config;

    private Button toggleBtn;
    private Button aimAssistBtn;
    private Button rangeBtn;
    private TakashaKeybindWidget keybindWidget;
    private Button backBtn;

    private static final List<BowTargetOption> BOW_TARGET_OPTIONS = List.of(
        new BowTargetOption("minecraft:zombie", "Zombie"),
        new BowTargetOption("minecraft:skeleton", "Skeleton"),
        new BowTargetOption("minecraft:creeper", "Creeper"),
        new BowTargetOption("minecraft:spider", "Spider (Laba-laba)"),
        new BowTargetOption("minecraft:blaze", "Blaze"),
        new BowTargetOption("minecraft:enderman", "Enderman"),
        new BowTargetOption("minecraft:drowned", "Drowned"),
        new BowTargetOption("minecraft:witch", "Witch (Penyihir)")
    );

    public record BowTargetOption(String id, String label) {}

    public TakashaAutoBowSubScreen(Screen parentScreen) {
        super(Component.literal("Takasha Auto Bow & Aim Settings"));
        this.parentScreen = parentScreen;
        this.config = TakashaMasterConfig.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        int panelW = 400;
        int panelH = 250;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        // Row 1: Status, Aim Assist, Range
        this.toggleBtn = Button.builder(
            Component.literal("Status: " + (config.autoBowEnabled ? "§aAKTIF" : "§cNONAKTIF")),
            btn -> {
                config.autoBowEnabled = !config.autoBowEnabled;
                config.save();
                btn.setMessage(Component.literal("Status: " + (config.autoBowEnabled ? "§aAKTIF" : "§cNONAKTIF")));
            }
        ).bounds(left + 14, top + 32, 116, 20).build();
        this.addRenderableWidget(this.toggleBtn);

        this.aimAssistBtn = Button.builder(
            Component.literal("Auto Aim: " + (config.autoBowAimAssist ? "§aON" : "§cOFF")),
            btn -> {
                config.autoBowAimAssist = !config.autoBowAimAssist;
                config.save();
                btn.setMessage(Component.literal("Auto Aim: " + (config.autoBowAimAssist ? "§aON" : "§cOFF")));
            }
        ).bounds(left + 134, top + 32, 126, 20)
        .tooltip(Tooltip.create(Component.literal("Membidik otomatis dengan kompensasi gravitasi panah. Pemain tetap bebas bergerak 100%!")))
        .build();
        this.addRenderableWidget(this.aimAssistBtn);

        this.rangeBtn = Button.builder(
            Component.literal("Jarak: §e" + (int) config.autoBowRange + "m"),
            btn -> {
                double[] ranges = {16.0, 24.0, 32.0};
                int idx = 0;
                for (int i = 0; i < ranges.length; i++) {
                    if (Math.abs(config.autoBowRange - ranges[i]) < 1.0) {
                        idx = (i + 1) % ranges.length;
                        break;
                    }
                }
                config.autoBowRange = ranges[idx];
                config.save();
                btn.setMessage(Component.literal("Jarak: §e" + (int) config.autoBowRange + "m"));
            }
        ).bounds(left + 264, top + 32, 122, 20).build();
        this.addRenderableWidget(this.rangeBtn);

        // Movement banner note
        // Row 2+: Bow Target Checklist (2 cols x 4 rows)
        int startY = top + 80;
        for (int i = 0; i < BOW_TARGET_OPTIONS.size(); i++) {
            BowTargetOption opt = BOW_TARGET_OPTIONS.get(i);
            int col = i % 2;
            int row = i / 2;
            int bx = left + 14 + (col * 190);
            int by = startY + (row * 24);

            Button targetBtn = Button.builder(
                getTargetButtonLabel(opt),
                btn -> {
                    if (config.bowTargetWhitelist.contains(opt.id())) {
                        config.bowTargetWhitelist.remove(opt.id());
                    } else {
                        config.bowTargetWhitelist.add(opt.id());
                    }
                    config.save();
                    btn.setMessage(getTargetButtonLabel(opt));
                }
            ).bounds(bx, by, 182, 20).build();
            this.addRenderableWidget(targetBtn);
        }

        // Bottom Controls
        int bottomY = top + panelH - 24;
        this.keybindWidget = new TakashaKeybindWidget(
            left + 14,
            bottomY,
            160,
            18,
            "Shortcut Key",
            SakuraWeaponsClient.AUTO_BOW_KEY
        );
        this.addRenderableWidget(this.keybindWidget);

        this.backBtn = Button.builder(
            Component.literal("◀ Kembali"),
            btn -> this.onClose()
        ).bounds(left + panelW - 94, bottomY, 80, 18).build();
        this.addRenderableWidget(this.backBtn);
    }

    private Component getTargetButtonLabel(BowTargetOption opt) {
        boolean active = config.bowTargetWhitelist.contains(opt.id());
        return Component.literal((active ? "§a✔ " : "§7✕ ") + opt.label());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractor.fill(0, 0, this.width, this.height, 0x88000000);

        int panelW = 400;
        int panelH = 250;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        extractor.fill(left, top, left + panelW, top + panelH, 0xEE14121E);
        extractor.outline(left, top, panelW, panelH, 0xFFE082A8);

        // Header
        extractor.fill(left, top, left + panelW, top + 26, 0xFF241528);
        extractor.text(this.font, "🏹 Auto Bow & Aim (100% Bebas Bergerak)", left + 10, top + 8, 0xFFFFB6C1, false);

        // Free movement badge
        extractor.fill(left + 14, top + 56, left + panelW - 14, top + 68, 0x44204020);
        extractor.text(this.font, "✔ Karakter tetap 100% bebas digerakkan pemain (WASD, Sprint, Jump)", left + 20, top + 58, 0xFF88FF88, false);
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
