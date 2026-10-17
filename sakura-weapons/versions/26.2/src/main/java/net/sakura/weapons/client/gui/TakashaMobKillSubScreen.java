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
public class TakashaMobKillSubScreen extends Screen {

    private final Screen parentScreen;
    private final TakashaMasterConfig config;

    private Button toggleBtn;
    private Button modeBtn;
    private Button fovConeBtn;
    private Button syncCdBtn;
    private Button antiAfkBtn;
    private TakashaKeybindWidget keybindWidget;
    private Button backBtn;

    private static final List<MobOption> MOB_OPTIONS = List.of(
        new MobOption("minecraft:zombie", "Zombie"),
        new MobOption("minecraft:skeleton", "Skeleton"),
        new MobOption("minecraft:creeper", "Creeper"),
        new MobOption("minecraft:spider", "Spider (Laba-laba)"),
        new MobOption("minecraft:blaze", "Blaze"),
        new MobOption("minecraft:enderman", "Enderman"),
        new MobOption("minecraft:drowned", "Drowned"),
        new MobOption("minecraft:witch", "Witch (Penyihir)"),
        new MobOption("minecraft:cow", "Sapi (Cow)"),
        new MobOption("minecraft:sheep", "Domba (Sheep)"),
        new MobOption("minecraft:pig", "Babi (Pig)"),
        new MobOption("minecraft:chicken", "Ayam (Chicken)")
    );

    public record MobOption(String id, String label) {}

    public TakashaMobKillSubScreen(Screen parentScreen) {
        super(Component.literal("Takasha Auto Kill Mob Settings"));
        this.parentScreen = parentScreen;
        this.config = TakashaMasterConfig.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        int panelW = 420;
        int panelH = 260;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        // Row 1: Status & Mode
        this.toggleBtn = Button.builder(
            Component.literal("Status: " + (config.autoKillEnabled ? "§aAKTIF" : "§cNONAKTIF")),
            btn -> {
                config.autoKillEnabled = !config.autoKillEnabled;
                config.save();
                btn.setMessage(Component.literal("Status: " + (config.autoKillEnabled ? "§aAKTIF" : "§cNONAKTIF")));
            }
        ).bounds(left + 14, top + 32, 190, 20).build();
        this.addRenderableWidget(this.toggleBtn);

        this.modeBtn = Button.builder(
            Component.literal("Mode: §b" + config.mobKillMode.name()),
            btn -> {
                config.mobKillMode = (config.mobKillMode == TakashaMasterConfig.MobKillMode.KAIZEN)
                    ? TakashaMasterConfig.MobKillMode.VANILLA
                    : TakashaMasterConfig.MobKillMode.KAIZEN;
                config.save();
                btn.setMessage(Component.literal("Mode: §b" + config.mobKillMode.name()));
            }
        ).bounds(left + 214, top + 32, 190, 20)
        .tooltip(Tooltip.create(Component.literal("KAIZEN: Anti-AFK osilasi + recovery\nVANILLA: Standard attack cooldown")))
        .build();
        this.addRenderableWidget(this.modeBtn);

        // Row 2: FOV Cone, Sync CD, Anti-AFK
        this.fovConeBtn = Button.builder(
            Component.literal("Cone: §e≤ " + (int) config.maxFovAngle + "°"),
            btn -> {
                double[] angles = {20.0, 25.0, 35.0, 45.0};
                int idx = 0;
                for (int i = 0; i < angles.length; i++) {
                    if (Math.abs(config.maxFovAngle - angles[i]) < 1.0) {
                        idx = (i + 1) % angles.length;
                        break;
                    }
                }
                config.maxFovAngle = angles[idx];
                config.save();
                btn.setMessage(Component.literal("Cone: §e≤ " + (int) config.maxFovAngle + "°"));
            }
        ).bounds(left + 14, top + 56, 126, 20)
        .tooltip(Tooltip.create(Component.literal("Anti-Aura: Hanya serang mob di depan sudut pandang (Maks 35°). Tidak ada 360° KillAura!")))
        .build();
        this.addRenderableWidget(this.fovConeBtn);

        this.syncCdBtn = Button.builder(
            Component.literal("Cooldown: " + (config.syncAttackCooldown ? "§a≥95%" : "§cSpam")),
            btn -> {
                config.syncAttackCooldown = !config.syncAttackCooldown;
                config.save();
                btn.setMessage(Component.literal("Cooldown: " + (config.syncAttackCooldown ? "§a≥95%" : "§cSpam")));
            }
        ).bounds(left + 144, top + 56, 130, 20)
        .tooltip(Tooltip.create(Component.literal("Hanya serang ketika bilah kekuatan senjata terisi penuh (Damage maksimal)")))
        .build();
        this.addRenderableWidget(this.syncCdBtn);

        this.antiAfkBtn = Button.builder(
            Component.literal("Anti-AFK: " + (config.antiAfkEnabled ? "§aON" : "§cOFF")),
            btn -> {
                config.antiAfkEnabled = !config.antiAfkEnabled;
                config.save();
                btn.setMessage(Component.literal("Anti-AFK: " + (config.antiAfkEnabled ? "§aON" : "§cOFF")));
            }
        ).bounds(left + 278, top + 56, 126, 20)
        .tooltip(Tooltip.create(Component.literal("Gerak otomatis hanya saat diam. Jika pemain menekan tombol gerakan (WASD/Lompat), kendali 100% bebas!")))
        .build();
        this.addRenderableWidget(this.antiAfkBtn);

        // Row 3+: Mob Whitelist Checklist (3 cols x 4 rows)
        int startY = top + 98;
        for (int i = 0; i < MOB_OPTIONS.size(); i++) {
            MobOption mob = MOB_OPTIONS.get(i);
            int col = i % 3;
            int row = i / 3;
            int bx = left + 14 + (col * 132);
            int by = startY + (row * 24);

            Button mobBtn = Button.builder(
                getMobButtonLabel(mob),
                btn -> {
                    if (config.killMobWhitelist.contains(mob.id())) {
                        config.killMobWhitelist.remove(mob.id());
                    } else {
                        config.killMobWhitelist.add(mob.id());
                    }
                    config.save();
                    btn.setMessage(getMobButtonLabel(mob));
                }
            ).bounds(bx, by, 128, 20).build();
            this.addRenderableWidget(mobBtn);
        }

        // Bottom Controls
        int bottomY = top + panelH - 24;
        this.keybindWidget = new TakashaKeybindWidget(
            left + 14,
            bottomY,
            160,
            18,
            "Shortcut Key",
            SakuraWeaponsClient.MOB_KILL_KEY
        );
        this.addRenderableWidget(this.keybindWidget);

        this.backBtn = Button.builder(
            Component.literal("◀ Kembali"),
            btn -> this.onClose()
        ).bounds(left + panelW - 94, bottomY, 80, 18).build();
        this.addRenderableWidget(this.backBtn);
    }

    private Component getMobButtonLabel(MobOption mob) {
        boolean active = config.killMobWhitelist.contains(mob.id());
        return Component.literal((active ? "§a✔ " : "§7✕ ") + mob.label());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractor.fill(0, 0, this.width, this.height, 0x88000000);

        int panelW = 420;
        int panelH = 260;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        extractor.fill(left, top, left + panelW, top + panelH, 0xEE14121E);
        extractor.outline(left, top, panelW, panelH, 0xFFE082A8);

        // Header
        extractor.fill(left, top, left + panelW, top + 26, 0xFF241528);
        extractor.text(this.font, "⚔️ Auto Kill Mob (Strict Frontal Cone / Anti-Aura)", left + 10, top + 8, 0xFFFFB6C1, false);

        // Section labels
        extractor.text(this.font, "Pilih Mob Target (Anti-Aura: Hanya yang di depan pandangan):", left + 14, top + 86, 0xFFAAAAAA, false);
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
