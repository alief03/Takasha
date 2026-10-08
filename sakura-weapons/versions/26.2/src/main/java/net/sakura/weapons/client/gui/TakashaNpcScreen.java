package net.sakura.weapons.client.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Rotations;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.sakura.weapons.compat.EmotecraftCompat;
import net.sakura.weapons.entity.TakashaNpcEntity;
import net.sakura.weapons.entity.npc.NpcPushPermission;
import net.sakura.weapons.inventory.TakashaNpcMenu;
import net.sakura.weapons.network.DismantleNpcPayload;
import net.sakura.weapons.network.UpdateNpcPayload;
import net.sakura.weapons.network.packet.NpcPositionActionPayload;
import net.sakura.weapons.network.packet.NpcUpdatePhysicsPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TakashaNpcScreen extends AbstractContainerScreen<TakashaNpcMenu> {

    public static final String[] PRESET_DISPLAY_NAMES = {
        "0: Bebas / Custom",
        "1: 🛌 Tidur Kasur",
        "2: 🛌 Tidur Tengkurap",
        "3: 🛌 Tidur Miring R",
        "4: 🛌 Tidur Miring L",
        "5: 🛌 Rebahan Santai",
        "6: 🛌 Peluk Senjata",
        "7: 🛌 Terkapar KO",
        "8: 🧘 Duduk Kursi",
        "9: 🧘 Duduk Sandar",
        "10: 🧘 Duduk Bersila",
        "11: 🧘 Meditasi Zen",
        "12: 🧘 Jongkok Intai",
        "13: 🧘 Berlutut Kaki",
        "14: 🧘 Bersimpuh Seiza",
        "15: ⚔ Siaga Berdiri",
        "16: ⚔ Siap Bertarung",
        "17: ⚔ Cabut Katana",
        "18: ⚔ Senjata Akimbo",
        "19: ⚔ Bidik Busur",
        "20: ⚔ Kuda-kuda Tombak",
        "21: ⚔ Palu Godam",
        "22: ⚔ Pasang Perisai",
        "23: ⚔ Tusukan Cepat",
        "24: ⚔ Tebas Melompat",
        "25: 🎭 Sedekap Dada",
        "26: 🎭 Hormat Ksatria",
        "27: 🎭 Hormat Bungkuk",
        "28: 🎭 Menunjuk Arah",
        "29: 🎭 Melambai Sapa",
        "30: 🎭 Tangis Terisak",
        "31: 🎭 Malu Facepalm",
        "32: 🎭 Berpikir Keras"
    };

    private static final String[] PERM_DISPLAY_NAMES = {
        "Akses: Privat (Pemilik)",
        "Akses: Tonton Saja",
        "Akses: Tim Co-op",
        "Akses: Publik Bebas"
    };

    private EditBox nameBox;
    private EditBox skinBox;
    private Button skinApplyButton;

    private Button posePrevButton;
    private Button poseButton;
    private Button poseNextButton;
    private Button modelButton;
    private Button bedOffsetButton;
    private Button yawLeftButton;
    private Button yawResetButton;
    private Button yawRightButton;
    private Button smallButton;
    private Button lockButton;
    private Button showNameButton;
    private Button permButton;
    private Button emoteSelectButton;
    private Button emoteModeButton;
    private Button bakePoseButton;
    private Button emoteClearButton;
    private Button saveButton;
    private Button cancelButton;
    private Button deleteButton;

    // Tab switching
    private int activeTab = 0; // 0 = Pose & Tampilan, 1 = Fisika & Interaksi
    private Button poseTabButton;
    private Button physicsTabButton;

    // Physics state
    private boolean pushable = true;
    private float pushStrength = 0.35f;
    private NpcPushPermission pushPermission = NpcPushPermission.EVERYONE;

    // Physics tab widgets
    private Button pushStatusButton;
    private Button pushPermissionButton;
    private PushStrengthSlider pushStrengthSlider;
    private Button resetOriginButton;
    private Button lockOriginButton;

    private byte currentEmotePlayMode = 0;

    // Emotecraft dropdown popup state
    private boolean isEmoteDropdownOpen = false;
    private EditBox emoteSearchBox;
    private final List<EmotecraftCompat.EmoteEntry> filteredEmotes = new ArrayList<>();
    private int emoteScrollOffset = 0;
    private String currentEmoteId = "";

    private int currentPreset = 0;
    private String currentModel = "default";
    private float currentBedOffset = 0.0f;
    private float currentYaw = 0.0f;
    private boolean isSmall = false;
    private boolean isLocked = false;
    private boolean showName = true;
    private byte permissionMode = TakashaNpcEntity.PERM_PROTECTED_VIEW;
    private boolean isSaving = false;
    private boolean isDeleted = false;

    public static class PushStrengthSlider extends AbstractSliderButton {
        private final Consumer<Float> onApply;

        public PushStrengthSlider(int x, int y, int width, int height, float initialStrength, Consumer<Float> onApply) {
            super(x, y, width, height, Component.empty(), (double) Math.clamp((initialStrength - 0.05f) / 0.95f, 0.0f, 1.0f));
            this.onApply = onApply;
            updateMessage();
        }

        public float getStrength() {
            return (float) (0.05 + this.value * 0.95);
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(String.format("Gaya Dorong: %.2fx", getStrength())));
        }

        @Override
        protected void applyValue() {
            if (this.onApply != null) {
                this.onApply.accept(getStrength());
            }
        }
    }

    public TakashaNpcScreen(TakashaNpcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 360, 220);
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        TakashaNpcEntity npc = this.menu.getNpc();
        String initialName = "";
        String initialSkin = "";
        String initialEmote = "";

        if (npc != null) {
            this.currentPreset = npc.getPosePreset();
            this.currentModel = npc.getSkinModel();
            this.currentBedOffset = npc.getBedHeightOffset();
            this.currentYaw = npc.getYawRotation();
            this.isSmall = npc.isSmall();
            this.isLocked = npc.isLocked();
            this.showName = npc.shouldShowName();
            this.permissionMode = npc.getPermissionMode();
            this.currentEmotePlayMode = npc.getEmotePlayMode();
            initialName = npc.hasCustomName() ? npc.getCustomName().getString() : "";
            initialSkin = npc.getSkinUrl();
            initialEmote = npc.getEmoteId();
            this.pushable = npc.isPushableMode();
            this.pushStrength = npc.getPushStrength();
            this.pushPermission = npc.getPushPermission();
        }
        this.currentEmoteId = initialEmote != null ? initialEmote : "";

        // Tab Switchers: 🎭 Pose (Tab 0) vs ⚡ Fisika (Tab 1)
        this.poseTabButton = Button.builder(
            Component.literal("▶ 🎭 Pose"),
            btn -> switchTab(0)
        ).bounds(this.leftPos + 182, this.topPos - 18, 80, 16)
         .tooltip(Tooltip.create(Component.literal("Kustomisasi Pose, Model, Skin & Gerakan Emote")))
         .build();
        this.addRenderableWidget(this.poseTabButton);

        this.physicsTabButton = Button.builder(
            Component.literal("⚡ Fisika"),
            btn -> switchTab(1)
        ).bounds(this.leftPos + 266, this.topPos - 18, 84, 16)
         .tooltip(Tooltip.create(Component.literal("Pengaturan Tabrakan Fisika & Titik Patokan Origin")))
         .build();
        this.addRenderableWidget(this.physicsTabButton);

        // Row 1: Name Box (y: 22)
        this.nameBox = new EditBox(this.font, this.leftPos + 182, this.topPos + 22, 168, 14, Component.literal("Nama"));
        this.nameBox.setMaxLength(64);
        this.nameBox.setValue(initialName);
        this.addRenderableWidget(this.nameBox);

        // Row 2: Skin Box & Apply Button (y: 48)
        this.skinBox = new EditBox(this.font, this.leftPos + 182, this.topPos + 48, 144, 14, Component.literal("Skin"));
        this.skinBox.setMaxLength(512);
        this.skinBox.setValue(initialSkin);
        this.addRenderableWidget(this.skinBox);

        this.skinApplyButton = Button.builder(
            Component.literal("↻"),
            btn -> applySkinInput()
        ).bounds(this.leftPos + 328, this.topPos + 47, 22, 16)
         .tooltip(Tooltip.create(Component.literal("Terapkan & Muat Skin (Enter)")))
         .build();
        this.addRenderableWidget(this.skinApplyButton);

        // Row 3: Pose Preset with Left & Right cycle buttons (y: 66)
        this.posePrevButton = Button.builder(
            Component.literal("◀"),
            btn -> cyclePose(-1)
        ).bounds(this.leftPos + 182, this.topPos + 66, 18, 16).build();
        this.addRenderableWidget(this.posePrevButton);

        this.poseButton = Button.builder(
            Component.literal(getPresetLabel(this.currentPreset)),
            btn -> cyclePose(1)
        ).bounds(this.leftPos + 202, this.topPos + 66, 128, 16).build();
        this.addRenderableWidget(this.poseButton);

        this.poseNextButton = Button.builder(
            Component.literal("▶"),
            btn -> cyclePose(1)
        ).bounds(this.leftPos + 332, this.topPos + 66, 18, 16).build();
        this.addRenderableWidget(this.poseNextButton);

        // Row 4: Model (Steve/Alex) & Bed Snap Offset (y: 86)
        this.modelButton = Button.builder(
            Component.literal("Model: " + ("slim".equalsIgnoreCase(this.currentModel) ? "Alex" : "Steve")),
            btn -> toggleModel()
        ).bounds(this.leftPos + 182, this.topPos + 86, 82, 16).build();
        this.addRenderableWidget(this.modelButton);

        this.bedOffsetButton = Button.builder(
            Component.literal(getBedOffsetLabel()),
            btn -> toggleBedOffset()
        ).bounds(this.leftPos + 268, this.topPos + 86, 82, 16).build();
        this.addRenderableWidget(this.bedOffsetButton);

        // Row 5: Yaw Rotation (⟲, Reset 0°, ⟳) (y: 106)
        this.yawLeftButton = Button.builder(
            Component.literal("⟲ -15°"),
            btn -> rotateYaw(-15f)
        ).bounds(this.leftPos + 182, this.topPos + 106, 50, 16).build();
        this.addRenderableWidget(this.yawLeftButton);

        this.yawResetButton = Button.builder(
            Component.literal("0° Hadap"),
            btn -> resetYaw()
        ).bounds(this.leftPos + 236, this.topPos + 106, 58, 16).build();
        this.addRenderableWidget(this.yawResetButton);

        this.yawRightButton = Button.builder(
            Component.literal("+15° ⟳"),
            btn -> rotateYaw(15f)
        ).bounds(this.leftPos + 298, this.topPos + 106, 52, 16).build();
        this.addRenderableWidget(this.yawRightButton);

        // Row 6: Mini, Kunci, Nama Toggles (y: 126)
        this.smallButton = Button.builder(
            Component.literal("Mini: " + (this.isSmall ? "ON" : "OFF")),
            btn -> toggleSmall()
        ).bounds(this.leftPos + 182, this.topPos + 126, 52, 16).build();
        this.addRenderableWidget(this.smallButton);

        this.lockButton = Button.builder(
            Component.literal("Kunci: " + (this.isLocked ? "ON" : "OFF")),
            btn -> toggleLock()
        ).bounds(this.leftPos + 238, this.topPos + 126, 52, 16).build();
        this.addRenderableWidget(this.lockButton);

        this.showNameButton = Button.builder(
            Component.literal("Nama: " + (this.showName ? "ON" : "OFF")),
            btn -> toggleShowName()
        ).bounds(this.leftPos + 294, this.topPos + 126, 56, 16).build();
        this.addRenderableWidget(this.showNameButton);

        // Row 7: Permission Mode (y: 146)
        this.permButton = Button.builder(
            Component.literal(getPermLabel(this.permissionMode)),
            btn -> cyclePermission()
        ).bounds(this.leftPos + 182, this.topPos + 146, 168, 16).build();
        this.addRenderableWidget(this.permButton);

        // Row 8: Emote Selector, Mode Toggle, Bake Pose & Clear Button (y: 172)
        this.emoteSelectButton = Button.builder(
            Component.literal(getEmoteButtonLabel()),
            btn -> toggleEmoteDropdown()
        ).bounds(this.leftPos + 182, this.topPos + 172, 82, 16).build();
        this.addRenderableWidget(this.emoteSelectButton);

        this.emoteModeButton = Button.builder(
            Component.literal(getEmoteModeLabel()),
            btn -> cycleEmoteMode()
        ).bounds(this.leftPos + 266, this.topPos + 172, 38, 16)
         .tooltip(Tooltip.create(Component.literal(getEmoteModeTooltip())))
         .build();
        this.addRenderableWidget(this.emoteModeButton);

        this.bakePoseButton = Button.builder(
            Component.literal("📌"),
            btn -> bakeEmoteToPose()
        ).bounds(this.leftPos + 306, this.topPos + 172, 22, 16)
         .tooltip(Tooltip.create(Component.translatable("gui.sakura_weapons.takasha_npc.bake_pose")))
         .build();
        this.addRenderableWidget(this.bakePoseButton);

        this.emoteClearButton = Button.builder(
            Component.literal("✕"),
            btn -> clearEmote()
        ).bounds(this.leftPos + 330, this.topPos + 172, 20, 16)
         .tooltip(Tooltip.create(Component.literal("Kosongkan / Berhenti Gerakan")))
         .build();
        this.addRenderableWidget(this.emoteClearButton);

        // Row 9: Action Buttons: Simpan, Tutup, and Hapus (y: 194)
        this.saveButton = Button.builder(
            Component.literal("✔ Simpan"),
            btn -> saveAndApply()
        ).bounds(this.leftPos + 182, this.topPos + 194, 68, 18).build();
        this.addRenderableWidget(this.saveButton);

        this.cancelButton = Button.builder(
            Component.literal("Tutup"),
            btn -> this.onClose()
        ).bounds(this.leftPos + 254, this.topPos + 194, 42, 18).build();
        this.addRenderableWidget(this.cancelButton);

        this.deleteButton = Button.builder(
            Component.literal("🗑 Hapus"),
            btn -> deleteNpc()
        ).bounds(this.leftPos + 300, this.topPos + 194, 50, 18)
         .tooltip(Tooltip.create(Component.literal("Bongkar & Ambil Kembali NPC (Item kembali ke tas)")))
         .build();
        this.addRenderableWidget(this.deleteButton);

        // Tab 1: Physics Widgets (Fisika & Interaksi)
        this.pushStatusButton = Button.builder(
            Component.literal("Status: " + (this.pushable ? "BISA DIDORONG" : "TERKUNCI / DIAM")),
            btn -> {
                this.pushable = !this.pushable;
                this.pushStatusButton.setMessage(Component.literal("Status: " + (this.pushable ? "BISA DIDORONG" : "TERKUNCI / DIAM")));
                TakashaNpcEntity n = this.menu.getNpc();
                if (n != null) n.setPushableMode(this.pushable);
                playUiClick();
            }
        ).bounds(this.leftPos + 182, this.topPos + 26, 168, 18)
         .tooltip(Tooltip.create(Component.literal("Aktifkan agar NPC dapat didorong oleh pemain saat bertabrakan")))
         .build();
        this.addRenderableWidget(this.pushStatusButton);

        this.pushPermissionButton = Button.builder(
            Component.literal(getPushPermLabel(this.pushPermission)),
            btn -> {
                int next = (this.pushPermission.ordinal() + 1) % NpcPushPermission.values().length;
                this.pushPermission = NpcPushPermission.values()[next];
                this.pushPermissionButton.setMessage(Component.literal(getPushPermLabel(this.pushPermission)));
                TakashaNpcEntity n = this.menu.getNpc();
                if (n != null) n.setPushPermission(this.pushPermission);
                playUiClick();
            }
        ).bounds(this.leftPos + 182, this.topPos + 50, 168, 18)
         .tooltip(Tooltip.create(Component.literal("Hak akses pemain yang diizinkan untuk mendorong NPC ini")))
         .build();
        this.addRenderableWidget(this.pushPermissionButton);

        this.pushStrengthSlider = new PushStrengthSlider(
            this.leftPos + 182, this.topPos + 84, 168, 18,
            this.pushStrength,
            val -> {
                this.pushStrength = val;
                TakashaNpcEntity n = this.menu.getNpc();
                if (n != null) n.setPushStrength(this.pushStrength);
            }
        );
        this.addRenderableWidget(this.pushStrengthSlider);

        this.resetOriginButton = Button.builder(
            Component.literal("↺ Reset ke Posisi Awal"),
            btn -> {
                ClientPlayNetworking.send(new NpcPositionActionPayload(this.menu.getEntityId(), NpcPositionActionPayload.ACTION_RESET_ORIGIN));
                TakashaNpcEntity n = this.menu.getNpc();
                if (n != null) n.resetToOriginPosition();
                playUiClick();
            }
        ).bounds(this.leftPos + 182, this.topPos + 120, 168, 18)
         .tooltip(Tooltip.create(Component.literal("Kembalikan koordinat dan hadap NPC ke titik awal (origin) spawn")))
         .build();
        this.addRenderableWidget(this.resetOriginButton);

        this.lockOriginButton = Button.builder(
            Component.literal("📌 Kunci Posisi Saat Ini"),
            btn -> {
                ClientPlayNetworking.send(new NpcPositionActionPayload(this.menu.getEntityId(), NpcPositionActionPayload.ACTION_LOCK_NEW_ORIGIN));
                TakashaNpcEntity n = this.menu.getNpc();
                if (n != null) n.lockCurrentAsNewOrigin();
                playUiClick();
            }
        ).bounds(this.leftPos + 182, this.topPos + 144, 168, 18)
         .tooltip(Tooltip.create(Component.literal("Jadikan posisi NPC saat ini sebagai titik origin baru")))
         .build();
        this.addRenderableWidget(this.lockOriginButton);

        // Initialize default tab visibility
        switchTab(0);
    }

    private void switchTab(int tab) {
        this.activeTab = tab;
        if (this.poseTabButton != null) {
            this.poseTabButton.setMessage(Component.literal(tab == 0 ? "▶ 🎭 Pose" : "🎭 Pose"));
        }
        if (this.physicsTabButton != null) {
            this.physicsTabButton.setMessage(Component.literal(tab == 1 ? "▶ ⚡ Fisika" : "⚡ Fisika"));
        }

        boolean showTab0 = (tab == 0);
        boolean showTab1 = (tab == 1);

        // Tab 0 widgets
        if (this.nameBox != null) this.nameBox.visible = showTab0;
        if (this.skinBox != null) this.skinBox.visible = showTab0;
        if (this.skinApplyButton != null) this.skinApplyButton.visible = showTab0;
        if (this.posePrevButton != null) this.posePrevButton.visible = showTab0;
        if (this.poseButton != null) this.poseButton.visible = showTab0;
        if (this.poseNextButton != null) this.poseNextButton.visible = showTab0;
        if (this.modelButton != null) this.modelButton.visible = showTab0;
        if (this.bedOffsetButton != null) this.bedOffsetButton.visible = showTab0;
        if (this.yawLeftButton != null) this.yawLeftButton.visible = showTab0;
        if (this.yawResetButton != null) this.yawResetButton.visible = showTab0;
        if (this.yawRightButton != null) this.yawRightButton.visible = showTab0;
        if (this.smallButton != null) this.smallButton.visible = showTab0;
        if (this.lockButton != null) this.lockButton.visible = showTab0;
        if (this.showNameButton != null) this.showNameButton.visible = showTab0;
        if (this.permButton != null) this.permButton.visible = showTab0;
        if (this.emoteSelectButton != null) this.emoteSelectButton.visible = showTab0;
        if (this.emoteModeButton != null) this.emoteModeButton.visible = showTab0;
        if (this.bakePoseButton != null) this.bakePoseButton.visible = showTab0;
        if (this.emoteClearButton != null) this.emoteClearButton.visible = showTab0;

        if (!showTab0 && this.isEmoteDropdownOpen) {
            this.isEmoteDropdownOpen = false;
        }

        // Tab 1 widgets
        if (this.pushStatusButton != null) this.pushStatusButton.visible = showTab1;
        if (this.pushPermissionButton != null) this.pushPermissionButton.visible = showTab1;
        if (this.pushStrengthSlider != null) this.pushStrengthSlider.visible = showTab1;
        if (this.resetOriginButton != null) this.resetOriginButton.visible = showTab1;
        if (this.lockOriginButton != null) this.lockOriginButton.visible = showTab1;
    }

    private String getPushPermLabel(NpcPushPermission perm) {
        if (perm == null) return "Izin Dorong: SEMUA";
        return switch (perm) {
            case EVERYONE -> "Izin Dorong: SEMUA PEMAIN";
            case OWNER_ONLY -> "Izin Dorong: HANYA PEMILIK";
            case TEAM_WHITELIST -> "Izin Dorong: ANGGOTA TIM";
            case OP_ONLY -> "Izin Dorong: HANYA OPERATOR";
        };
    }

    private void applySkinInput() {
        String skin = this.skinBox.getValue().trim();
        TakashaNpcEntity npc = this.menu.getNpc();
        if (npc != null) {
            npc.setSkinUrl(skin);
        }
    }

    private void deleteNpc() {
        this.isDeleted = true;
        ClientPlayNetworking.send(new DismantleNpcPayload(this.menu.getEntityId()));
        this.onClose();
    }

    private void toggleModel() {
        this.currentModel = "slim".equalsIgnoreCase(this.currentModel) ? "default" : "slim";
        this.modelButton.setMessage(Component.literal("Model: " + ("slim".equalsIgnoreCase(this.currentModel) ? "Alex" : "Steve")));
        TakashaNpcEntity npc = this.menu.getNpc();
        if (npc != null) {
            npc.setSkinModel(this.currentModel);
        }
    }

    private void cyclePose(int delta) {
        this.currentPreset = (this.currentPreset + delta + PRESET_DISPLAY_NAMES.length) % PRESET_DISPLAY_NAMES.length;
        this.poseButton.setMessage(Component.literal(getPresetLabel(this.currentPreset)));
        TakashaNpcEntity npc = this.menu.getNpc();
        if (npc != null) {
            npc.setPosePreset(this.currentPreset);
            if (this.currentPreset > 0 && this.currentPreset <= 32) {
                npc.applyPosePreset(this.currentPreset);
            }
            // Automatically snap bed height offset when selecting any Lying Down preset (1..7)
            if (npc.isLyingDownPose()) {
                if (this.currentBedOffset < 0.2f) {
                    this.currentBedOffset = 0.5625f;
                    this.bedOffsetButton.setMessage(Component.literal(getBedOffsetLabel()));
                    npc.setBedHeightOffset(this.currentBedOffset);
                }
            }
            // Mutual exclusion: when selecting a static pose preset, clear active emote
            if (this.currentPreset != 0 && !this.currentEmoteId.isEmpty()) {
                this.currentEmoteId = "";
                this.emoteSelectButton.setMessage(Component.literal(getEmoteButtonLabel()));
                npc.setEmoteId("");
                EmotecraftCompat.stopEmoteSafely(npc);
            }
        }
    }

    private String getPresetLabel(int preset) {
        if (preset >= 0 && preset < PRESET_DISPLAY_NAMES.length) {
            return PRESET_DISPLAY_NAMES[preset];
        }
        return "Pose: " + preset;
    }

    private void toggleBedOffset() {
        if (this.currentBedOffset < 0.2f) {
            this.currentBedOffset = 0.5625f; // Kasur surface snap
        } else {
            this.currentBedOffset = 0.0f;    // Lantai
        }
        this.bedOffsetButton.setMessage(Component.literal(getBedOffsetLabel()));
        TakashaNpcEntity npc = this.menu.getNpc();
        if (npc != null) {
            npc.setBedHeightOffset(this.currentBedOffset);
        }
    }

    private String getBedOffsetLabel() {
        return this.currentBedOffset > 0.2f ? "Kasur (+0.56)" : "Lantai (0.0)";
    }

    private void resetYaw() {
        this.currentYaw = 0.0f;
        TakashaNpcEntity npc = this.menu.getNpc();
        if (npc != null) {
            npc.setYawRotation(this.currentYaw);
        }
    }

    private void rotateYaw(float delta) {
        this.currentYaw = (this.currentYaw + delta) % 360f;
        if (this.currentYaw < 0) this.currentYaw += 360f;
        TakashaNpcEntity npc = this.menu.getNpc();
        if (npc != null) {
            npc.setYawRotation(this.currentYaw);
        }
    }

    private void toggleSmall() {
        this.isSmall = !this.isSmall;
        this.smallButton.setMessage(Component.literal("Mini: " + (this.isSmall ? "ON" : "OFF")));
        TakashaNpcEntity npc = this.menu.getNpc();
        if (npc != null) {
            npc.setSmall(this.isSmall);
        }
    }

    private void toggleLock() {
        this.isLocked = !this.isLocked;
        this.lockButton.setMessage(Component.literal("Kunci: " + (this.isLocked ? "ON" : "OFF")));
        TakashaNpcEntity npc = this.menu.getNpc();
        if (npc != null) {
            npc.setLocked(this.isLocked);
        }
    }

    private void toggleShowName() {
        this.showName = !this.showName;
        this.showNameButton.setMessage(Component.literal("Nama: " + (this.showName ? "ON" : "OFF")));
        TakashaNpcEntity npc = this.menu.getNpc();
        if (npc != null) {
            npc.setShowName(this.showName);
        }
    }

    private void cyclePermission() {
        this.permissionMode = (byte) ((this.permissionMode + 1) % 4);
        this.permButton.setMessage(Component.literal(getPermLabel(this.permissionMode)));
        TakashaNpcEntity npc = this.menu.getNpc();
        if (npc != null) {
            npc.setPermissionMode(this.permissionMode);
        }
    }

    private String getPermLabel(byte mode) {
        if (mode >= 0 && mode < PERM_DISPLAY_NAMES.length) {
            return PERM_DISPLAY_NAMES[mode];
        }
        return "Akses: " + mode;
    }

    // -----------------------------------------------------------------------------------------
    // Emotecraft Dropdown Handlers
    // -----------------------------------------------------------------------------------------

    private void toggleEmoteDropdown() {
        this.isEmoteDropdownOpen = !this.isEmoteDropdownOpen;
        if (this.isEmoteDropdownOpen) {
            int dropX = this.leftPos + 180;
            int dropY = this.topPos + 20;
            int dropW = 172;
            if (this.emoteSearchBox == null) {
                this.emoteSearchBox = new EditBox(this.font, dropX + 4, dropY + 18, dropW - 8, 14, Component.literal("Cari Emote"));
                this.emoteSearchBox.setHint(Component.literal("Cari / ketik ID..."));
                this.emoteSearchBox.setResponder(val -> {
                    updateFilteredEmotes();
                    this.emoteScrollOffset = 0;
                });
            } else {
                this.emoteSearchBox.setPosition(dropX + 4, dropY + 18);
                this.emoteSearchBox.setValue("");
            }
            this.emoteSearchBox.setFocused(true);
            this.emoteScrollOffset = 0;
            updateFilteredEmotes();
        }
    }

    private void updateFilteredEmotes() {
        String query = this.emoteSearchBox != null ? this.emoteSearchBox.getValue().trim().toLowerCase() : "";
        List<EmotecraftCompat.EmoteEntry> all = EmotecraftCompat.getDetectedEmotes();
        this.filteredEmotes.clear();
        for (EmotecraftCompat.EmoteEntry entry : all) {
            if (query.isEmpty()
                    || entry.displayName().toLowerCase().contains(query)
                    || entry.id().toLowerCase().contains(query)
                    || entry.author().toLowerCase().contains(query)) {
                this.filteredEmotes.add(entry);
            }
        }
    }

    private void selectEmote(String emoteId) {
        this.currentEmoteId = emoteId.trim();
        this.emoteSelectButton.setMessage(Component.literal(getEmoteButtonLabel()));

        // Smart auto-detection: If selecting sleep / lie / sit / relax emote, default mode to HOLD (1)
        if (!this.currentEmoteId.isEmpty()) {
            String lower = this.currentEmoteId.toLowerCase();
            if (lower.contains("sleep") || lower.contains("lay") || lower.contains("lie") ||
                lower.contains("rest") || lower.contains("sit") || lower.contains("tpose") ||
                lower.contains("bed") || lower.contains("rebahan") || lower.contains("tidur")) {
                this.currentEmotePlayMode = 1; // HOLD
                if (this.emoteModeButton != null) {
                    this.emoteModeButton.setMessage(Component.literal(getEmoteModeLabel()));
                    this.emoteModeButton.setTooltip(Tooltip.create(Component.literal(getEmoteModeTooltip())));
                }
            }
        }

        TakashaNpcEntity npc = this.menu.getNpc();
        if (npc != null) {
            npc.setEmotePlayMode(this.currentEmotePlayMode);
            npc.setEmoteId(this.currentEmoteId);
            if (!this.currentEmoteId.isEmpty()) {
                // Mutual exclusion: when selecting an emote, reset static pose preset to neutral
                this.currentPreset = 0;
                this.poseButton.setMessage(Component.literal(getPresetLabel(0)));
                npc.setPosePreset(0);
                EmotecraftCompat.playEmoteSafely(npc, this.currentEmoteId, this.currentEmotePlayMode);

                // ReplayMod zero-desync: sample base limb rotations immediately into SynchedEntityData
                Rotations[] rots = EmotecraftCompat.sampleCurrentRotations(npc, this.currentEmoteId);
                if (rots != null && rots.length >= 6) {
                    npc.setHeadPose(rots[0]);
                    npc.setBodyPose(rots[1]);
                    npc.setRightArmPose(rots[2]);
                    npc.setLeftArmPose(rots[3]);
                    npc.setRightLegPose(rots[4]);
                    npc.setLeftLegPose(rots[5]);
                }
            } else {
                EmotecraftCompat.stopEmoteSafely(npc);
            }
        }
        playUiClick();
    }

    private void clearEmote() {
        selectEmote("");
        if (this.isEmoteDropdownOpen) {
            this.isEmoteDropdownOpen = false;
        }
    }

    private void bakeEmoteToPose() {
        TakashaNpcEntity npc = this.menu.getNpc();
        if (npc == null) return;

        // Sample current limb rotations from the active emote or procedural engine
        Rotations[] rots = EmotecraftCompat.sampleRotationsAtTick(this.currentEmoteId, npc.tickCount);
        if (rots == null) {
            rots = EmotecraftCompat.sampleCurrentRotations(npc, this.currentEmoteId);
        }
        if (rots != null && rots.length >= 6) {
            npc.setHeadPose(rots[0]);
            npc.setBodyPose(rots[1]);
            npc.setRightArmPose(rots[2]);
            npc.setLeftArmPose(rots[3]);
            npc.setRightLegPose(rots[4]);
            npc.setLeftLegPose(rots[5]);
        }

        // Freeze into custom static pose (preset 0)
        this.currentPreset = 0;
        this.poseButton.setMessage(Component.literal(getPresetLabel(0)));
        npc.setPosePreset(0);

        // Clear active emote
        this.currentEmoteId = "";
        this.emoteSelectButton.setMessage(Component.literal(getEmoteButtonLabel()));
        npc.setEmoteId("");
        EmotecraftCompat.stopEmoteSafely(npc);

        // Play feedback sound
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(net.minecraft.sounds.SoundEvents.ANVIL_USE, 0.7F, 1.2F);
        }
    }

    private String getEmoteModeLabel() {
        return switch (this.currentEmotePlayMode) {
            case 1 -> "🛑 Tahan";
            case 2 -> "🚶 1x";
            default -> "🔁 Loop";
        };
    }

    private String getEmoteModeTooltip() {
        return switch (this.currentEmotePlayMode) {
            case 1 -> "Mode: Tahan di Gerakan Terakhir (Membeku di akhir pose, misal tiduran/duduk)";
            case 2 -> "Mode: Mainkan Sekali lalu Berdiri (Kembali tegak ke pose normal setelah selesai)";
            default -> "Mode: Loop Terus Menerus (Berputar berulang-ulang tanpa henti)";
        };
    }

    private void cycleEmoteMode() {
        this.currentEmotePlayMode = (byte) ((this.currentEmotePlayMode + 1) % 3);
        if (this.emoteModeButton != null) {
            this.emoteModeButton.setMessage(Component.literal(getEmoteModeLabel()));
            this.emoteModeButton.setTooltip(Tooltip.create(Component.literal(getEmoteModeTooltip())));
        }
        TakashaNpcEntity npc = this.menu.getNpc();
        if (npc != null) {
            npc.setEmotePlayMode(this.currentEmotePlayMode);
        }
        playUiClick();
    }

    private String getEmoteButtonLabel() {
        if (this.currentEmoteId == null || this.currentEmoteId.isEmpty()) {
            return "Pilih ▼";
        }
        for (EmotecraftCompat.EmoteEntry e : EmotecraftCompat.getDetectedEmotes()) {
            if (e.id().equalsIgnoreCase(this.currentEmoteId)) {
                String name = e.displayName();
                if (name.length() > 9) name = name.substring(0, 8) + "..";
                return name + " ▼";
            }
        }
        String name = this.currentEmoteId;
        if (name.length() > 9) name = name.substring(0, 8) + "..";
        return name + " ▼";
    }

    private void playUiClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void saveAndApply() {
        if (this.isDeleted) return;
        this.isSaving = true;
        applySkinInput();
        TakashaNpcEntity npc = this.menu.getNpc();
        if (npc != null) {
            if (this.currentPreset > 0 && this.currentPreset <= 32) {
                npc.setPosePreset(this.currentPreset);
                npc.applyPosePreset(this.currentPreset);
            } else if (!this.currentEmoteId.isEmpty()) {
                Rotations[] rots = EmotecraftCompat.sampleRotationsAtTick(this.currentEmoteId, npc.tickCount);
                if (rots == null) {
                    rots = EmotecraftCompat.sampleCurrentRotations(npc, this.currentEmoteId);
                }
                if (rots != null && rots.length >= 6) {
                    npc.setHeadPose(rots[0]);
                    npc.setBodyPose(rots[1]);
                    npc.setRightArmPose(rots[2]);
                    npc.setLeftArmPose(rots[3]);
                    npc.setRightLegPose(rots[4]);
                    npc.setLeftLegPose(rots[5]);
                }
            }
        }
        Rotations head = npc != null ? npc.getHeadPose() : new Rotations(0, 0, 0);
        Rotations body = npc != null ? npc.getBodyPose() : new Rotations(0, 0, 0);
        Rotations leftArm = npc != null ? npc.getLeftArmPose() : new Rotations(0, 0, 0);
        Rotations rightArm = npc != null ? npc.getRightArmPose() : new Rotations(0, 0, 0);
        Rotations leftLeg = npc != null ? npc.getLeftLegPose() : new Rotations(0, 0, 0);
        Rotations rightLeg = npc != null ? npc.getRightLegPose() : new Rotations(0, 0, 0);

        UpdateNpcPayload payload = new UpdateNpcPayload(
            this.menu.getEntityId(),
            this.skinBox != null ? this.skinBox.getValue().trim() : "",
            this.currentModel,
            this.currentPreset,
            head, body, leftArm, rightArm, leftLeg, rightLeg,
            this.currentBedOffset,
            this.currentEmoteId,
            this.currentEmotePlayMode,
            this.currentYaw,
            this.isSmall,
            this.isLocked,
            this.showName,
            this.nameBox != null ? this.nameBox.getValue().trim() : "",
            this.permissionMode
        );

        ClientPlayNetworking.send(payload);

        // Synchronize push physics state
        ClientPlayNetworking.send(new NpcUpdatePhysicsPayload(
            this.menu.getEntityId(),
            this.pushable,
            this.pushStrength,
            this.pushPermission.getId()
        ));

        this.onClose();
    }

    @Override
    public void onClose() {
        if (!this.isSaving && !this.isDeleted) {
            saveAndApply();
            return;
        }
        super.onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isFocused) {
        if (this.isEmoteDropdownOpen) {
            double mx = event.x();
            double my = event.y();
            int dropX = this.leftPos + 180;
            int dropY = this.topPos + 20;
            int dropW = 172;
            int dropH = 172;

            // 1. Click outside modal -> close dropdown
            if (mx < dropX || mx > dropX + dropW || my < dropY || my > dropY + dropH) {
                this.isEmoteDropdownOpen = false;
                return true; // Consumed click so background widgets aren't triggered
            }

            // 2. Header close button [✕]
            int closeX = dropX + dropW - 16;
            int closeY = dropY + 3;
            if (mx >= closeX && mx <= closeX + 13 && my >= closeY && my <= closeY + 13) {
                this.isEmoteDropdownOpen = false;
                playUiClick();
                return true;
            }

            // 3. Header reload button [🔄]
            int reloadX = dropX + dropW - 32;
            int reloadY = dropY + 3;
            if (mx >= reloadX && mx <= reloadX + 13 && my >= reloadY && my <= reloadY + 13) {
                EmotecraftCompat.refreshEmotes();
                updateFilteredEmotes();
                playUiClick();
                return true;
            }

            // 4. Click inside search box
            if (this.emoteSearchBox != null && mx >= this.emoteSearchBox.getX() && mx <= this.emoteSearchBox.getX() + this.emoteSearchBox.getWidth()
                    && my >= this.emoteSearchBox.getY() && my <= this.emoteSearchBox.getY() + this.emoteSearchBox.getHeight()) {
                this.emoteSearchBox.onClick(event, isFocused);
                return true;
            }

            // 5. Click on list items
            int listTop = dropY + 38;
            int listBottom = dropY + dropH - 4;
            int itemH = 16;
            if (my >= listTop && my <= listBottom && mx >= dropX + 4 && mx <= dropX + dropW - 4) {
                String query = this.emoteSearchBox != null ? this.emoteSearchBox.getValue().trim() : "";
                boolean hasCustomItem = !query.isEmpty() && this.filteredEmotes.stream().noneMatch(e -> e.id().equalsIgnoreCase(query));
                int totalItems = this.filteredEmotes.size() + (hasCustomItem ? 1 : 0);

                int clickedIndex = (int) ((my - listTop + this.emoteScrollOffset) / itemH);
                if (clickedIndex >= 0 && clickedIndex < totalItems) {
                    if (hasCustomItem && clickedIndex == 0) {
                        selectEmote(query);
                    } else {
                        int itemIndex = hasCustomItem ? clickedIndex - 1 : clickedIndex;
                        if (itemIndex >= 0 && itemIndex < this.filteredEmotes.size()) {
                            selectEmote(this.filteredEmotes.get(itemIndex).id());
                        }
                    }
                    this.isEmoteDropdownOpen = false;
                    return true;
                }
            }

            return true; // Consume all clicks inside popup
        }

        return super.mouseClicked(event, isFocused);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.isEmoteDropdownOpen) {
            int dropX = this.leftPos + 180;
            int dropY = this.topPos + 20;
            int dropW = 172;
            int dropH = 172;
            if (mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= dropY + 38 && mouseY <= dropY + dropH) {
                String query = this.emoteSearchBox != null ? this.emoteSearchBox.getValue().trim() : "";
                boolean hasCustomItem = !query.isEmpty() && this.filteredEmotes.stream().noneMatch(e -> e.id().equalsIgnoreCase(query));
                int totalItems = this.filteredEmotes.size() + (hasCustomItem ? 1 : 0);
                int maxScroll = Math.max(0, totalItems * 16 - (dropH - 42));
                this.emoteScrollOffset = (int) Math.clamp(this.emoteScrollOffset - verticalAmount * 16.0, 0, maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.isEmoteDropdownOpen) {
            if (event.key() == 256) { // ESC closes dropdown
                this.isEmoteDropdownOpen = false;
                return true;
            }
            if (event.key() == 257 || event.key() == 335) { // Enter selects first item or typed custom
                String query = this.emoteSearchBox != null ? this.emoteSearchBox.getValue().trim() : "";
                if (!this.filteredEmotes.isEmpty()) {
                    selectEmote(this.filteredEmotes.get(0).id());
                } else if (!query.isEmpty()) {
                    selectEmote(query);
                }
                this.isEmoteDropdownOpen = false;
                return true;
            }
            if (this.emoteSearchBox != null) {
                return this.emoteSearchBox.keyPressed(event);
            }
            return true;
        }

        if (this.activeTab == 0 && this.nameBox.isFocused()) {
            if (event.key() == 256) {
                this.onClose();
                return true;
            }
            return this.nameBox.keyPressed(event);
        }
        if (this.activeTab == 0 && this.skinBox.isFocused()) {
            if (event.key() == 256) {
                this.onClose();
                return true;
            }
            if (event.key() == 257 || event.key() == 335) {
                applySkinInput();
                return true;
            }
            return this.skinBox.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.isEmoteDropdownOpen && this.emoteSearchBox != null) {
            boolean handled = this.emoteSearchBox.charTyped(event);
            if (handled) {
                updateFilteredEmotes();
                this.emoteScrollOffset = 0;
            }
            return handled;
        }
        return super.charTyped(event);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        // Left column titles
        extractor.text(this.font, this.title, 8, 6, 0xFFE082A8, false);
        extractor.text(this.font, this.playerInventoryTitle, 8, 86, 0xFF666677, false);

        if (this.activeTab == 0) {
            // Right column labels for Tab 0 (Pose & Tampilan)
            extractor.text(this.font, Component.literal("Nama Mannequin:"), 182, 11, 0xFFDDDDDD, false);
            extractor.text(this.font, Component.literal("Skin (URL / Mineskin / Nama):"), 182, 37, 0xFFDDDDDD, false);
            extractor.text(this.font, Component.literal("Gerakan & Mode Putar:"), 182, 161, 0xFFDDDDDD, false);
        } else {
            // Right column labels for Tab 1 (Fisika & Interaksi)
            extractor.text(this.font, Component.literal("Pengaturan Fisika & Tabrakan:"), 182, 12, 0xFFDDDDDD, false);
            extractor.text(this.font, Component.literal("Sensitivitas Gaya Dorong:"), 182, 72, 0xFFDDDDDD, false);
            extractor.text(this.font, Component.literal("Patokan Koordinat (Origin):"), 182, 108, 0xFFDDDDDD, false);
        }

        TakashaNpcEntity npc = this.menu.getNpc();
        if (npc != null) {
            String owner = npc.getOwnerName();
            if (owner != null && !owner.isBlank()) {
                extractor.text(this.font, Component.literal("Owner: " + owner), 8, 178, 0xFF888899, false);
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;

        // Draw tab headers highlight
        if (this.activeTab == 0) {
            extractor.fill(x + 182, y - 18, x + 262, y, 0x44E082A8);
            extractor.outline(x + 182, y - 18, 80, 18, 0xFFE082A8);
        } else {
            extractor.fill(x + 266, y - 18, x + 350, y, 0x44E082A8);
            extractor.outline(x + 266, y - 18, 84, 18, 0xFFE082A8);
        }

        // Main dark container background with Sakura accent outline
        extractor.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xEE161622);
        extractor.outline(x, y, this.imageWidth, this.imageHeight, 0xFFE082A8);

        // Divider line between inventory and customization panel
        extractor.fill(x + 176, y + 4, x + 177, y + this.imageHeight - 4, 0x55E082A8);

        // 3D Live Mannequin Preview background box
        int previewX1 = x + 30;
        int previewY1 = y + 16;
        int previewX2 = x + 72;
        int previewY2 = y + 84;
        extractor.fill(previewX1, previewY1, previewX2, previewY2, 0x880A0A12);
        extractor.outline(previewX1, previewY1, previewX2 - previewX1, previewY2 - previewY1, 0xFF444455);

        // Draw slots frames & backgrounds
        for (Slot slot : this.menu.slots) {
            int sx = x + slot.x;
            int sy = y + slot.y;
            extractor.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF353545);
            extractor.fill(sx, sy, sx + 16, sy + 16, 0xFF14141A);
        }

        // Category ghost indicator for empty slots 0..6
        if (!this.menu.slots.get(0).hasItem()) extractor.centeredText(this.font, "⛑", x + 8 + 8, y + 18 + 4, 0x55E082A8);
        if (!this.menu.slots.get(1).hasItem()) extractor.centeredText(this.font, "🥋", x + 8 + 8, y + 36 + 4, 0x55E082A8);
        if (!this.menu.slots.get(2).hasItem()) extractor.centeredText(this.font, "👖", x + 8 + 8, y + 54 + 4, 0x55E082A8);
        if (!this.menu.slots.get(3).hasItem()) extractor.centeredText(this.font, "👢", x + 8 + 8, y + 72 + 4, 0x55E082A8);
        if (!this.menu.slots.get(4).hasItem()) extractor.centeredText(this.font, "⚔", x + 76 + 8, y + 44 + 4, 0x55E082A8);
        if (!this.menu.slots.get(5).hasItem()) extractor.centeredText(this.font, "🛡", x + 96 + 8, y + 44 + 4, 0x55E082A8);
        if (!this.menu.slots.get(6).hasItem()) extractor.centeredText(this.font, "🪽", x + 116 + 8, y + 44 + 4, 0x55E082A8);

        // Red danger outline around delete button
        extractor.outline(x + 300, y + 194, 50, 18, 0xFFFF4444);

        // Render live 3D entity preview
        TakashaNpcEntity npc = this.menu.getNpc();
        if (npc != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                extractor,
                previewX1 + 1, previewY1 + 1,
                previewX2 - 1, previewY2 - 1,
                28,
                0.0625f,
                (float) mouseX, (float) mouseY,
                npc
            );
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        if (this.isEmoteDropdownOpen) {
            renderEmoteDropdownModal(extractor, mouseX, mouseY, partialTick);
        } else {
            renderSlotTooltips(extractor, mouseX, mouseY);
        }
    }

    private void renderSlotTooltips(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        for (int i = 0; i <= 6; i++) {
            Slot slot = this.menu.slots.get(i);
            int sx = x + slot.x;
            int sy = y + slot.y;

            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                if (!slot.hasItem()) {
                    List<Component> lines = new ArrayList<>();
                    switch (i) {
                        case 0 -> {
                            lines.add(Component.literal("Slot Kepala / Helm").withColor(0xFFFFD54F));
                            lines.add(Component.literal("Kategori: Helm, Topi, Kepala Mob").withColor(0xFFAAAAAA));
                            lines.add(Component.literal("Zirah pelindung kepala NPC").withColor(0xFF888899));
                        }
                        case 1 -> {
                            lines.add(Component.literal("Slot Zirah Dada").withColor(0xFFFFD54F));
                            lines.add(Component.literal("Kategori: Zirah Dada (Chestplate)").withColor(0xFFAAAAAA));
                            lines.add(Component.literal("Zirah pelindung badan NPC").withColor(0xFF888899));
                        }
                        case 2 -> {
                            lines.add(Component.literal("Slot Celana Zirah").withColor(0xFFFFD54F));
                            lines.add(Component.literal("Kategori: Celana Zirah (Leggings)").withColor(0xFFAAAAAA));
                            lines.add(Component.literal("Zirah pelindung kaki NPC").withColor(0xFF888899));
                        }
                        case 3 -> {
                            lines.add(Component.literal("Slot Sepatu Zirah").withColor(0xFFFFD54F));
                            lines.add(Component.literal("Kategori: Sepatu Zirah (Boots)").withColor(0xFFAAAAAA));
                            lines.add(Component.literal("Zirah pelindung telapak NPC").withColor(0xFF888899));
                        }
                        case 4 -> {
                            lines.add(Component.literal("Slot Tangan Utama (Main Hand)").withColor(0xFFFFD54F));
                            lines.add(Component.literal("Kategori: Senjata (Pedang/Kapak/Busur/Alat)").withColor(0xFFAAAAAA));
                            lines.add(Component.literal("Item yang dipegang di tangan kanan NPC").withColor(0xFF888899));
                        }
                        case 5 -> {
                            lines.add(Component.literal("Slot Tangan Kiri (Off Hand)").withColor(0xFFFFD54F));
                            lines.add(Component.literal("Kategori: Perisai (Shield), Totem, Panah, Obor").withColor(0xFFAAAAAA));
                            lines.add(Component.literal("Item yang dipegang di tangan kiri NPC").withColor(0xFF888899));
                        }
                        case 6 -> {
                            lines.add(Component.literal("Slot Sayap Kosmetik").withColor(0xFFFFD54F));
                            lines.add(Component.literal("Kategori: Sayap Sakura, Elytra").withColor(0xFFAAAAAA));
                            lines.add(Component.literal("Sayap kosmetik hiasan punggung NPC").withColor(0xFF888899));
                        }
                    }
                    if (!lines.isEmpty()) {
                        var visualLines = lines.stream().map(Component::getVisualOrderText).toList();
                        extractor.setTooltipForNextFrame(this.font, visualLines, mouseX, mouseY);
                    }
                }
                break;
            }
        }
    }

    private void renderEmoteDropdownModal(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int dropX = this.leftPos + 180;
        int dropY = this.topPos + 20;
        int dropW = 172;
        int dropH = 172;

        // Outer border and modal background
        extractor.fill(dropX - 1, dropY - 1, dropX + dropW + 1, dropY + dropH + 1, 0xFFE082A8); // Sakura outline
        extractor.fill(dropX, dropY, dropX + dropW, dropY + dropH, 0xF8101018); // Dark tinted modal glass

        // Header bar
        extractor.fill(dropX, dropY, dropX + dropW, dropY + 17, 0xFF251628);
        extractor.text(this.font, "Pilih Gerakan Emote", dropX + 6, dropY + 4, 0xFFFFB6C1, false);

        // Refresh button [🔄]
        int reloadX = dropX + dropW - 32;
        int reloadY = dropY + 3;
        boolean hoverReload = mouseX >= reloadX && mouseX <= reloadX + 13 && mouseY >= reloadY && mouseY <= reloadY + 13;
        extractor.fill(reloadX, reloadY, reloadX + 13, reloadY + 13, hoverReload ? 0xFF502540 : 0xFF352035);
        extractor.outline(reloadX, reloadY, 13, 13, hoverReload ? 0xFFFFB6C1 : 0xFF665066);
        extractor.text(this.font, "🔄", reloadX + 1, reloadY + 2, 0xFFFFFFFF, false);

        // Close button [✕]
        int closeX = dropX + dropW - 16;
        int closeY = dropY + 3;
        boolean hoverClose = mouseX >= closeX && mouseX <= closeX + 13 && mouseY >= closeY && mouseY <= closeY + 13;
        extractor.fill(closeX, closeY, closeX + 13, closeY + 13, hoverClose ? 0xFF882030 : 0xFF352035);
        extractor.outline(closeX, closeY, 13, 13, hoverClose ? 0xFFFF4466 : 0xFF665066);
        extractor.text(this.font, "✕", closeX + 3, closeY + 2, 0xFFFFFFFF, false);

        // Search Box rendering
        if (this.emoteSearchBox != null) {
            this.emoteSearchBox.extractRenderState(extractor, mouseX, mouseY, partialTick);
        }

        // Separator line below search box
        extractor.fill(dropX + 4, dropY + 35, dropX + dropW - 4, dropY + 36, 0x55E082A8);

        // Emotes List
        int listTop = dropY + 38;
        int listBottom = dropY + dropH - 4;
        int listH = listBottom - listTop;
        int itemH = 16;

        String query = this.emoteSearchBox != null ? this.emoteSearchBox.getValue().trim() : "";
        boolean hasCustomItem = !query.isEmpty() && this.filteredEmotes.stream().noneMatch(e -> e.id().equalsIgnoreCase(query));
        int totalItems = this.filteredEmotes.size() + (hasCustomItem ? 1 : 0);
        int maxScroll = Math.max(0, totalItems * itemH - listH);
        this.emoteScrollOffset = Math.clamp(this.emoteScrollOffset, 0, maxScroll);

        int startIndex = this.emoteScrollOffset / itemH;
        int visibleCount = (listH / itemH) + 2;

        // Enable scissor clipping so items never bleed outside modal bounds
        extractor.enableScissor(dropX + 4, listTop, dropX + dropW - 6, listBottom);

        for (int i = 0; i < visibleCount; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex >= totalItems) break;

            int iy = listTop + (itemIndex * itemH) - this.emoteScrollOffset;
            if (iy + itemH < listTop || iy > listBottom) continue;

            boolean isHovered = mouseX >= dropX + 4 && mouseX <= dropX + dropW - 8 && mouseY >= iy && mouseY < iy + itemH && mouseY >= listTop && mouseY <= listBottom;

            if (hasCustomItem && itemIndex == 0) {
                // Custom ID item
                int bg = isHovered ? 0xFF3A2535 : 0xFF201625;
                extractor.fill(dropX + 4, iy, dropX + dropW - 6, iy + itemH - 1, bg);
                extractor.outline(dropX + 4, iy, dropW - 10, itemH - 1, 0xFFE082A8);
                extractor.text(this.font, "✨", dropX + 7, iy + 4, 0xFFFFE082, false);
                String customLabel = "Gunakan: '" + query + "'";
                if (this.font.width(customLabel) > dropW - 32) {
                    customLabel = this.font.plainSubstrByWidth(customLabel, dropW - 38) + "...'";
                }
                extractor.text(this.font, customLabel, dropX + 22, iy + 4, 0xFFFFE082, false);
            } else {
                int realIdx = hasCustomItem ? itemIndex - 1 : itemIndex;
                EmotecraftCompat.EmoteEntry entry = this.filteredEmotes.get(realIdx);
                boolean isSelected = entry.id().equalsIgnoreCase(this.currentEmoteId);

                int bg = isSelected ? 0xFF502540 : (isHovered ? 0xFF302030 : 0x00000000);
                if (bg != 0) {
                    extractor.fill(dropX + 4, iy, dropX + dropW - 6, iy + itemH - 1, bg);
                }
                if (isSelected) {
                    extractor.outline(dropX + 4, iy, dropW - 10, itemH - 1, 0xFFE082A8);
                }

                // 12x12 Emote Icon or Emotecraft Official Mod Logo fallback (NO text emojis)
                Identifier icon = entry.iconId() != null ? entry.iconId() : EmotecraftCompat.EMOTECRAFT_LOGO;
                extractor.blit(icon, dropX + 7, iy + 2, 12, 12, 0.0f, 0.0f, 1.0f, 1.0f);

                String displayName = (isSelected ? "✔ " : "") + entry.displayName();
                int textColor = isSelected ? 0xFFFFB6C1 : (isHovered ? 0xFFFFFFFF : 0xFFDDDDDD);
                if (this.font.width(displayName) > dropW - 32) {
                    displayName = this.font.plainSubstrByWidth(displayName, dropW - 38) + "..";
                }
                extractor.text(this.font, displayName, dropX + 22, iy + 4, textColor, false);
            }
        }

        extractor.disableScissor();

        // Scrollbar
        if (maxScroll > 0) {
            int barX = dropX + dropW - 5;
            int barW = 2;
            extractor.fill(barX, listTop, barX + barW, listBottom, 0x33FFFFFF);
            int thumbH = Math.max(12, (listH * listH) / (totalItems * itemH));
            int thumbY = listTop + (this.emoteScrollOffset * (listH - thumbH)) / maxScroll;
            extractor.fill(barX, thumbY, barX + barW, thumbY + thumbH, 0xFFE082A8);
        }

        // If empty list
        if (totalItems == 0) {
            extractor.centeredText(this.font, "Tidak ada emote cocok", dropX + (dropW / 2), listTop + 30, 0xFF888899);
        }
    }
}
