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
public class TakashaMobKillSubScreen extends Screen {

    private final Screen parentScreen;
    private final TakashaMasterConfig config;

    private Button toggleBtn;
    private Button modeBtn;
    private Button reachBtn;
    private Button targetPriorityBtn;
    private Button attackRateBtn;
    private Button fovConeBtn;
    private Button smartCritBtn;
    private Button pvpPlayerBtn;
    private Button autoWeaponBtn;
    private Button weaponBreakBtn;
    private Button antiAfkBtn;
    private TakashaKeybindWidget keybindWidget;
    private Button backBtn;

    private static final List<MobOption> MOB_OPTIONS = List.of(
        new MobOption("minecraft:zombie", "Zombie"),
        new MobOption("minecraft:skeleton", "Skeleton"),
        new MobOption("minecraft:creeper", "Creeper"),
        new MobOption("minecraft:spider", "Spider"),
        new MobOption("minecraft:blaze", "Blaze"),
        new MobOption("minecraft:enderman", "Enderman"),
        new MobOption("minecraft:drowned", "Drowned"),
        new MobOption("minecraft:witch", "Witch"),
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
        int panelH = 300;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        // Row 1: Status & Mode (KAIZEN vs VANILLA)
        this.toggleBtn = Button.builder(
            Component.literal("Status: " + (config.autoKillEnabled ? "§aAKTIF" : "§cNONAKTIF")),
            btn -> {
                config.autoKillEnabled = !config.autoKillEnabled;
                config.save();
                btn.setMessage(Component.literal("Status: " + (config.autoKillEnabled ? "§aAKTIF" : "§cNONAKTIF")));
            }
        ).bounds(left + 14, top + 28, 190, 20).build();
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
        ).bounds(left + 214, top + 28, 190, 20)
        .tooltip(Tooltip.create(Component.literal("KAIZEN: Anti-AFK osilasi + Raycast Half-slab LOS + Cooldown\nVANILLA: Pure stationary attack + Jarak langsung + Turbo/Spam rate")))
        .build();
        this.addRenderableWidget(this.modeBtn);

        // Row 2: Reach, Prioritas Target, Attack Rate
        double[] reaches = {2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 6.0};
        this.reachBtn = Button.builder(
            Component.literal("Reach: §e" + config.killReach + "m"),
            btn -> {
                int idx = 0;
                for (int i = 0; i < reaches.length; i++) {
                    if (Math.abs(config.killReach - reaches[i]) < 0.1) {
                        idx = (i + 1) % reaches.length;
                        break;
                    }
                }
                config.killReach = reaches[idx];
                config.save();
                btn.setMessage(Component.literal("Reach: §e" + config.killReach + "m"));
            }
        ).bounds(left + 14, top + 52, 126, 20)
        .tooltip(Tooltip.create(Component.literal("Jangkauan serangan terjauh (Maks 6.0 meter)")))
        .build();
        this.addRenderableWidget(this.reachBtn);

        this.targetPriorityBtn = Button.builder(
            Component.literal("Target: §e" + getPriorityLabel(config.killTargetPriority)),
            btn -> {
                TakashaMasterConfig.KillTargetPriority[] priorities = TakashaMasterConfig.KillTargetPriority.values();
                int next = (config.killTargetPriority.ordinal() + 1) % priorities.length;
                config.killTargetPriority = priorities[next];
                config.save();
                btn.setMessage(Component.literal("Target: §e" + getPriorityLabel(config.killTargetPriority)));
            }
        ).bounds(left + 144, top + 52, 130, 20)
        .tooltip(Tooltip.create(Component.literal("Prioritas pemilihan sasaran mob")))
        .build();
        this.addRenderableWidget(this.targetPriorityBtn);

        this.attackRateBtn = Button.builder(
            Component.literal("Rate: §e" + getRateLabel(config.attackRateMode)),
            btn -> {
                TakashaMasterConfig.AttackRateMode[] rates = TakashaMasterConfig.AttackRateMode.values();
                int next = (config.attackRateMode.ordinal() + 1) % rates.length;
                config.attackRateMode = rates[next];
                config.syncAttackCooldown = (config.attackRateMode == TakashaMasterConfig.AttackRateMode.COOLDOWN_SYNC);
                config.save();
                btn.setMessage(Component.literal("Rate: §e" + getRateLabel(config.attackRateMode)));
            }
        ).bounds(left + 278, top + 52, 126, 20)
        .tooltip(Tooltip.create(Component.literal("Kecepatan ayunan senjata:\n- Cooldown Sync: ≥95% (Damage Maksimal)\n- Turbo: ≥70% (Lebih Cepat)\n- Hyper Spam: 0ms (Serang Tiap Tick)")))
        .build();
        this.addRenderableWidget(this.attackRateBtn);

        // Row 3: FOV Cone, Smart Crit, PVP Player
        this.fovConeBtn = Button.builder(
            Component.literal("Cone: §e" + (config.maxFovAngle >= 360.0 ? "360°" : ("≤ " + (int) config.maxFovAngle + "°"))),
            btn -> {
                double[] angles = {20.0, 35.0, 60.0, 90.0, 180.0, 360.0};
                int idx = 0;
                for (int i = 0; i < angles.length; i++) {
                    if (Math.abs(config.maxFovAngle - angles[i]) < 1.0) {
                        idx = (i + 1) % angles.length;
                        break;
                    }
                }
                config.maxFovAngle = angles[idx];
                config.save();
                btn.setMessage(Component.literal("Cone: §e" + (config.maxFovAngle >= 360.0 ? "360°" : ("≤ " + (int) config.maxFovAngle + "°"))));
            }
        ).bounds(left + 14, top + 76, 126, 20)
        .tooltip(Tooltip.create(Component.literal("Sudut pandang serangan (Anti-Aura ≤35°, atau 360° bebas)")))
        .build();
        this.addRenderableWidget(this.fovConeBtn);

        this.smartCritBtn = Button.builder(
            Component.literal("Smart Crit: " + (config.smartCritical ? "§aON" : "§cOFF")),
            btn -> {
                config.smartCritical = !config.smartCritical;
                config.save();
                btn.setMessage(Component.literal("Smart Crit: " + (config.smartCritical ? "§aON" : "§cOFF")));
            }
        ).bounds(left + 144, top + 76, 130, 20)
        .tooltip(Tooltip.create(Component.literal("Hanya serang ketika jatuh dari lompatan untuk memicu 150% Critical Damage")))
        .build();
        this.addRenderableWidget(this.smartCritBtn);

        this.pvpPlayerBtn = Button.builder(
            Component.literal("PVP Player: " + (config.attackPlayers ? "§aON" : "§cOFF")),
            btn -> {
                config.attackPlayers = !config.attackPlayers;
                config.save();
                btn.setMessage(Component.literal("PVP Player: " + (config.attackPlayers ? "§aON" : "§cOFF")));
            }
        ).bounds(left + 278, top + 76, 126, 20)
        .tooltip(Tooltip.create(Component.literal("Izinkan menyerang pemain lain dalam zona PVP")))
        .build();
        this.addRenderableWidget(this.pvpPlayerBtn);

        // Row 4: Auto Senjata, Proteksi Hancur, Anti-AFK
        this.autoWeaponBtn = Button.builder(
            Component.literal("Ganti Senjata: " + (config.autoSwitchWeapon ? "§aON" : "§cOFF")),
            btn -> {
                config.autoSwitchWeapon = !config.autoSwitchWeapon;
                config.save();
                btn.setMessage(Component.literal("Ganti Senjata: " + (config.autoSwitchWeapon ? "§aON" : "§cOFF")));
            }
        ).bounds(left + 14, top + 100, 126, 20)
        .tooltip(Tooltip.create(Component.literal("Otomatis ambil pedang/kapak/mace terbaik dari inventory")))
        .build();
        this.addRenderableWidget(this.autoWeaponBtn);

        this.weaponBreakBtn = Button.builder(
            Component.literal("Proteksi: " + (config.weaponBreakProtection ? "§aON" : "§cOFF")),
            btn -> {
                config.weaponBreakProtection = !config.weaponBreakProtection;
                config.save();
                btn.setMessage(Component.literal("Proteksi: " + (config.weaponBreakProtection ? "§aON" : "§cOFF")));
            }
        ).bounds(left + 144, top + 100, 130, 20)
        .tooltip(Tooltip.create(Component.literal("Otomatis jeda serangan jika durability senjata tersisa ≤ " + config.weaponBreakThreshold)))
        .build();
        this.addRenderableWidget(this.weaponBreakBtn);

        this.antiAfkBtn = Button.builder(
            Component.literal("Anti-AFK: " + (config.antiAfkEnabled ? "§aON" : "§cOFF")),
            btn -> {
                config.antiAfkEnabled = !config.antiAfkEnabled;
                config.save();
                btn.setMessage(Component.literal("Anti-AFK: " + (config.antiAfkEnabled ? "§aON" : "§cOFF")));
            }
        ).bounds(left + 278, top + 100, 126, 20)
        .tooltip(Tooltip.create(Component.literal("Micro-osilasi Anti-AFK (Aktif di mode KAIZEN)")))
        .build();
        this.addRenderableWidget(this.antiAfkBtn);

        // Row 5+: Mob Whitelist Checklist (3 cols x 4 rows)
        int startY = top + 142;
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
        int bottomY = top + panelH - 26;
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

    private String getPriorityLabel(TakashaMasterConfig.KillTargetPriority p) {
        return switch (p) {
            case NEAREST -> "Terdekat";
            case LOWEST_HEALTH -> "HP Terendah";
            case HIGHEST_HEALTH -> "HP Tertinggi";
            case FOV_CENTER -> "Crosshair";
        };
    }

    private String getRateLabel(TakashaMasterConfig.AttackRateMode r) {
        return switch (r) {
            case COOLDOWN_SYNC -> "Sync ≥95%";
            case TURBO -> "Turbo ≥70%";
            case HYPER_SPAM -> "Hyper 0ms";
        };
    }

    private Component getMobButtonLabel(MobOption mob) {
        boolean active = config.killMobWhitelist.contains(mob.id());
        return Component.literal((active ? "§a✔ " : "§7✕ ") + mob.label());
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
        guiGraphics.drawString(this.font, "⚔️ Auto Kill Mob (Kaizen vs Vanilla Engine)", left + 10, top + 7, 0xFFFFB6C1, false);

        // Section labels
        guiGraphics.drawString(this.font, "Daftar Mob Target yang Diserang:", left + 14, top + 128, 0xFFAAAAAA, false);

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
