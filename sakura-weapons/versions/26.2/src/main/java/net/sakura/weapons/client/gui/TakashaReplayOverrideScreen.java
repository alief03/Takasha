package net.sakura.weapons.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Rotations;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.sakura.weapons.client.replay.ReplayPoseOverrideManager;
import net.sakura.weapons.compat.EmotecraftCompat;
import net.sakura.weapons.entity.TakashaNpcEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class TakashaReplayOverrideScreen extends Screen {

    private final UUID initialTargetUuid;
    private final Screen parentScreen;

    private final List<TakashaNpcEntity> nearbyNpcs = new ArrayList<>();
    private int currentNpcIndex = 0;
    private TakashaNpcEntity selectedNpc = null;

    private int selectedPreset = 0;
    private String selectedEmoteId = "";
    private byte selectedPlayMode = 0; // 0: Loop, 1: Tahan, 2: Sekali

    // Widgets
    private Button prevNpcButton;
    private Button nextNpcButton;
    private Button presetPrevButton;
    private Button presetButton;
    private Button presetNextButton;
    private Button emoteSelectButton;
    private Button emoteClearButton;
    private Button emoteModeButton;
    private Button applyButton;
    private Button resetButton;
    private Button closeButton;

    // Emote modal dropdown
    private boolean isEmoteModalOpen = false;
    private EditBox emoteSearchBox;
    private final List<EmotecraftCompat.EmoteEntry> filteredEmotes = new ArrayList<>();
    private int emoteScrollOffset = 0;

    public TakashaReplayOverrideScreen(UUID targetUuid, Screen parentScreen) {
        super(Component.literal("Sakura Replay Pose & Emote Studio"));
        this.initialTargetUuid = targetUuid;
        this.parentScreen = parentScreen;
    }

    public TakashaReplayOverrideScreen(UUID targetUuid) {
        this(targetUuid, null);
    }

    public TakashaReplayOverrideScreen() {
        this(null, null);
    }

    @Override
    protected void init() {
        super.init();
        scanNearbyNpcs();

        int cx = this.width / 2;
        int cy = this.height / 2;

        int panelW = 340;
        int panelH = 210;
        int left = cx - (panelW / 2);
        int top = cy - (panelH / 2);

        // Header NPC navigation buttons
        this.prevNpcButton = Button.builder(Component.literal("◀"), btn -> switchNpc(-1))
            .bounds(left + 246, top + 6, 18, 16)
            .tooltip(Tooltip.create(Component.literal("Pilih NPC sebelumnya")))
            .build();
        this.addRenderableWidget(this.prevNpcButton);

        this.nextNpcButton = Button.builder(Component.literal("▶"), btn -> switchNpc(1))
            .bounds(left + 314, top + 6, 18, 16)
            .tooltip(Tooltip.create(Component.literal("Pilih NPC selanjutnya")))
            .build();
        this.addRenderableWidget(this.nextNpcButton);

        // Preset controls (Row 1)
        int controlsLeft = left + 116;
        int row1Y = top + 46;

        this.presetPrevButton = Button.builder(Component.literal("◀"), btn -> cyclePreset(-1))
            .bounds(controlsLeft, row1Y, 18, 18)
            .build();
        this.addRenderableWidget(this.presetPrevButton);

        this.presetButton = Button.builder(Component.literal(getPresetLabel(this.selectedPreset)), btn -> cyclePreset(1))
            .bounds(controlsLeft + 20, row1Y, 172, 18)
            .tooltip(Tooltip.create(Component.literal("Klik untuk ganti pose preset (1..32)")))
            .build();
        this.addRenderableWidget(this.presetButton);

        this.presetNextButton = Button.builder(Component.literal("▶"), btn -> cyclePreset(1))
            .bounds(controlsLeft + 194, row1Y, 18, 18)
            .build();
        this.addRenderableWidget(this.presetNextButton);

        // Emote controls (Row 2)
        int row2Y = top + 86;
        this.emoteSelectButton = Button.builder(Component.literal(getEmoteButtonLabel()), btn -> openEmoteModal())
            .bounds(controlsLeft, row2Y, 142, 18)
            .tooltip(Tooltip.create(Component.literal("Pilih emote animasi dari Emotecraft")))
            .build();
        this.addRenderableWidget(this.emoteSelectButton);

        this.emoteClearButton = Button.builder(Component.literal("✕"), btn -> clearEmote())
            .bounds(controlsLeft + 144, row2Y, 18, 18)
            .tooltip(Tooltip.create(Component.literal("Hapus emote")))
            .build();
        this.addRenderableWidget(this.emoteClearButton);

        this.emoteModeButton = Button.builder(Component.literal(getPlayModeLabel()), btn -> cyclePlayMode())
            .bounds(controlsLeft + 164, row2Y, 48, 18)
            .tooltip(Tooltip.create(Component.literal("Ubah mode putar: Loop / Tahan / Sekali")))
            .build();
        this.addRenderableWidget(this.emoteModeButton);

        // Action buttons
        int actionY = top + 130;
        this.applyButton = Button.builder(Component.literal("✔ Terapkan ke Replay"), btn -> applyOverride())
            .bounds(controlsLeft, actionY, 212, 22)
            .tooltip(Tooltip.create(Component.literal("Simpan override pose/emote untuk mannequin ini di timeline replay")))
            .build();
        this.addRenderableWidget(this.applyButton);

        int bottomY = top + 158;
        this.resetButton = Button.builder(Component.literal("🔄 Reset Rekaman"), btn -> resetOverride())
            .bounds(controlsLeft, bottomY, 128, 18)
            .tooltip(Tooltip.create(Component.literal("Hapus override dan kembalikan ke pose asli rekaman")))
            .build();
        this.addRenderableWidget(this.resetButton);

        this.closeButton = Button.builder(Component.literal("✕ Tutup"), btn -> this.onClose())
            .bounds(controlsLeft + 132, bottomY, 80, 18)
            .build();
        this.addRenderableWidget(this.closeButton);

        updateWidgetStates();
    }

    private void scanNearbyNpcs() {
        this.nearbyNpcs.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity cam = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
            AABB searchBox = cam != null
                ? cam.getBoundingBox().inflate(256.0)
                : new AABB(-500, -64, -500, 500, 320, 500);

            List<TakashaNpcEntity> list = mc.level.getEntitiesOfClass(TakashaNpcEntity.class, searchBox);
            if (cam != null) {
                list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(cam)));
            }
            this.nearbyNpcs.addAll(list);
        }

        // Match initial target UUID if specified
        this.currentNpcIndex = 0;
        if (this.initialTargetUuid != null) {
            for (int i = 0; i < this.nearbyNpcs.size(); i++) {
                if (this.nearbyNpcs.get(i).getUUID().equals(this.initialTargetUuid)) {
                    this.currentNpcIndex = i;
                    break;
                }
            }
        }

        selectNpcAtIndex(this.currentNpcIndex);
    }

    private void selectNpcAtIndex(int index) {
        if (index >= 0 && index < this.nearbyNpcs.size()) {
            this.selectedNpc = this.nearbyNpcs.get(index);
            UUID uuid = this.selectedNpc.getUUID();

            ReplayPoseOverrideManager.PoseOverride active = ReplayPoseOverrideManager.getOverride(uuid);
            if (active != null) {
                this.selectedPreset = active.preset();
                this.selectedEmoteId = active.emoteId() != null ? active.emoteId() : "";
                this.selectedPlayMode = active.playMode();
            } else {
                this.selectedPreset = this.selectedNpc.getPosePreset();
                this.selectedEmoteId = this.selectedNpc.getEmoteId() != null ? this.selectedNpc.getEmoteId() : "";
                this.selectedPlayMode = this.selectedNpc.getEmotePlayMode();
            }
            previewCurrentPose();
        } else {
            this.selectedNpc = null;
        }
    }

    private void switchNpc(int delta) {
        if (this.nearbyNpcs.isEmpty()) return;
        this.currentNpcIndex = (this.currentNpcIndex + delta + this.nearbyNpcs.size()) % this.nearbyNpcs.size();
        selectNpcAtIndex(this.currentNpcIndex);
        updateWidgetStates();
        playUiClick();
    }

    private void cyclePreset(int delta) {
        int total = TakashaNpcScreen.PRESET_DISPLAY_NAMES.length;
        this.selectedPreset = (this.selectedPreset + delta + total) % total;
        if (this.selectedPreset > 0) {
            this.selectedEmoteId = "";
        }
        previewCurrentPose();
        updateWidgetStates();
        playUiClick();
    }

    private void cyclePlayMode() {
        this.selectedPlayMode = (byte) ((this.selectedPlayMode + 1) % 3);
        updateWidgetStates();
        playUiClick();
    }

    private void clearEmote() {
        this.selectedEmoteId = "";
        previewCurrentPose();
        updateWidgetStates();
        playUiClick();
    }

    private void previewCurrentPose() {
        if (this.selectedNpc == null) return;
        if (this.selectedPreset > 0) {
            this.selectedNpc.setPosePreset(this.selectedPreset);
            this.selectedNpc.applyPosePreset(this.selectedPreset);
            this.selectedNpc.setEmoteId("");
            EmotecraftCompat.stopEmoteSafely(this.selectedNpc);
        } else if (!this.selectedEmoteId.isEmpty()) {
            this.selectedNpc.setPosePreset(0);
            this.selectedNpc.setEmoteId(this.selectedEmoteId);
            this.selectedNpc.setEmotePlayMode(this.selectedPlayMode);
            EmotecraftCompat.playEmoteSafely(this.selectedNpc, this.selectedEmoteId, this.selectedPlayMode);

            Rotations[] rots = EmotecraftCompat.sampleCurrentRotations(this.selectedNpc, this.selectedEmoteId);
            if (rots != null && rots.length >= 6) {
                this.selectedNpc.setHeadPose(rots[0]);
                this.selectedNpc.setBodyPose(rots[1]);
                this.selectedNpc.setRightArmPose(rots[2]);
                this.selectedNpc.setLeftArmPose(rots[3]);
                this.selectedNpc.setRightLegPose(rots[4]);
                this.selectedNpc.setLeftLegPose(rots[5]);
            }
        }
    }

    private void applyOverride() {
        if (this.selectedNpc == null) return;
        UUID uuid = this.selectedNpc.getUUID();
        ReplayPoseOverrideManager.setOverride(uuid, this.selectedPreset, this.selectedEmoteId, this.selectedPlayMode);
        previewCurrentPose();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            String name = this.selectedNpc.hasCustomName() ? this.selectedNpc.getCustomName().getString() : uuid.toString().substring(0, 8);
            mc.player.sendSystemMessage(Component.literal("§d[Sakura Replay] §aOverride berhasil disimpan untuk NPC: §f" + name));
        }
        playUiClick();
        updateWidgetStates();
    }

    private void resetOverride() {
        if (this.selectedNpc == null) return;
        UUID uuid = this.selectedNpc.getUUID();
        ReplayPoseOverrideManager.removeOverride(uuid);

        this.selectedPreset = this.selectedNpc.getPosePreset();
        this.selectedEmoteId = this.selectedNpc.getEmoteId() != null ? this.selectedNpc.getEmoteId() : "";
        this.selectedPlayMode = this.selectedNpc.getEmotePlayMode();
        previewCurrentPose();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§d[Sakura Replay] §eOverride dihapus, kembali ke rekaman asli."));
        }
        playUiClick();
        updateWidgetStates();
    }

    private void updateWidgetStates() {
        boolean hasNpc = this.selectedNpc != null;
        boolean multiNpc = this.nearbyNpcs.size() > 1;

        this.prevNpcButton.active = multiNpc;
        this.nextNpcButton.active = multiNpc;

        this.presetPrevButton.active = hasNpc;
        this.presetButton.active = hasNpc;
        this.presetNextButton.active = hasNpc;
        this.presetButton.setMessage(Component.literal(getPresetLabel(this.selectedPreset)));

        this.emoteSelectButton.active = hasNpc;
        this.emoteSelectButton.setMessage(Component.literal(getEmoteButtonLabel()));
        this.emoteClearButton.active = hasNpc && !this.selectedEmoteId.isEmpty();

        this.emoteModeButton.active = hasNpc && !this.selectedEmoteId.isEmpty();
        this.emoteModeButton.setMessage(Component.literal(getPlayModeLabel()));

        this.applyButton.active = hasNpc;
        boolean hasOverride = hasNpc && ReplayPoseOverrideManager.hasOverride(this.selectedNpc.getUUID());
        this.resetButton.active = hasOverride;
    }

    private String getPresetLabel(int preset) {
        if (preset >= 0 && preset < TakashaNpcScreen.PRESET_DISPLAY_NAMES.length) {
            return TakashaNpcScreen.PRESET_DISPLAY_NAMES[preset];
        }
        return "Pose: " + preset;
    }

    private String getEmoteButtonLabel() {
        if (this.selectedEmoteId.isEmpty()) {
            return "Emote: (Kosong / Default)";
        }
        for (EmotecraftCompat.EmoteEntry e : EmotecraftCompat.getDetectedEmotes()) {
            if (e.id().equalsIgnoreCase(this.selectedEmoteId)) {
                return "🎭 " + e.displayName();
            }
        }
        return "🎭 " + this.selectedEmoteId;
    }

    private String getPlayModeLabel() {
        return switch (this.selectedPlayMode) {
            case 1 -> "⏸ Tahan";
            case 2 -> "▶ Sekali";
            default -> "🔁 Loop";
        };
    }

    private void openEmoteModal() {
        this.isEmoteModalOpen = true;
        int cx = this.width / 2;
        int cy = this.height / 2;
        int dropX = cx - 100;
        int dropY = cy - 90;
        int dropW = 200;

        if (this.emoteSearchBox == null) {
            this.emoteSearchBox = new EditBox(this.font, dropX + 6, dropY + 20, dropW - 12, 14, Component.literal("Cari Emote"));
            this.emoteSearchBox.setHint(Component.literal("Cari / ketik ID emote..."));
            this.emoteSearchBox.setResponder(val -> {
                updateFilteredEmotes();
                this.emoteScrollOffset = 0;
            });
        } else {
            this.emoteSearchBox.setPosition(dropX + 6, dropY + 20);
            this.emoteSearchBox.setValue("");
        }
        this.emoteSearchBox.setFocused(true);
        this.emoteScrollOffset = 0;
        updateFilteredEmotes();
        playUiClick();
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

    private void selectEmoteFromModal(String emoteId) {
        this.selectedEmoteId = emoteId.trim();
        if (!this.selectedEmoteId.isEmpty()) {
            this.selectedPreset = 0;
            String lower = this.selectedEmoteId.toLowerCase();
            if (lower.contains("sleep") || lower.contains("lay") || lower.contains("lie") ||
                lower.contains("rest") || lower.contains("sit") || lower.contains("tpose") ||
                lower.contains("bed") || lower.contains("rebahan") || lower.contains("tidur")) {
                this.selectedPlayMode = 1; // HOLD
            }
        }
        this.isEmoteModalOpen = false;
        previewCurrentPose();
        updateWidgetStates();
        playUiClick();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // Darkened background for contrast
        extractor.fill(0, 0, this.width, this.height, 0x88000000);

        int cx = this.width / 2;
        int cy = this.height / 2;
        int panelW = 340;
        int panelH = 210;
        int left = cx - (panelW / 2);
        int top = cy - (panelH / 2);

        // Container Window (Glassmorphism & Sakura pink border)
        extractor.fill(left, top, left + panelW, top + panelH, 0xEE161420);
        extractor.outline(left, top, panelW, panelH, 0xFFE082A8);

        // Header Title
        extractor.fill(left, top, left + panelW, top + 26, 0xFF241428);
        extractor.text(this.font, "🎭 Sakura Replay Pose Studio", left + 8, top + 8, 0xFFFFB6C1, false);

        // Target NPC info in header
        if (this.selectedNpc != null) {
            String name = this.selectedNpc.hasCustomName()
                ? this.selectedNpc.getCustomName().getString()
                : "Takasha NPC (" + this.selectedNpc.getUUID().toString().substring(0, 8) + ")";
            if (this.nearbyNpcs.size() > 1) {
                extractor.centeredText(this.font, (this.currentNpcIndex + 1) + "/" + this.nearbyNpcs.size(), left + 289, top + 10, 0xFFDDDDDD);
            }
            extractor.text(this.font, name, left + 116, top + 32, 0xFFFFD54F, false);
        } else {
            extractor.text(this.font, "Tidak ada Takasha NPC ditemukan!", left + 116, top + 32, 0xFFFF6666, false);
        }

        // Section labels
        extractor.text(this.font, "Preset Pose Mannequin:", left + 116, top + 46 - 11, 0xFFAAAAAA, false);
        extractor.text(this.font, "Gerakan Emote (Emotecraft):", left + 116, top + 86 - 11, 0xFFAAAAAA, false);

        // Override status badge
        boolean hasOverride = this.selectedNpc != null && ReplayPoseOverrideManager.hasOverride(this.selectedNpc.getUUID());
        String statusText = hasOverride ? "⚡ Status: OVERRIDE AKTIF DI TIMELINE" : "✔ Status: Rekaman Asli Replay";
        int statusColor = hasOverride ? 0xFF66FF88 : 0xFFAAAAAA;
        extractor.text(this.font, statusText, left + 116, top + 115, statusColor, false);

        // 3D Live Mannequin Preview Box
        int previewX1 = left + 12;
        int previewY1 = top + 32;
        int previewX2 = left + 104;
        int previewY2 = top + 180;
        extractor.fill(previewX1, previewY1, previewX2, previewY2, 0x880B0A12);
        extractor.outline(previewX1, previewY1, previewX2 - previewX1, previewY2 - previewY1, 0xFF444455);

        if (this.selectedNpc != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                extractor,
                previewX1 + 1, previewY1 + 1,
                previewX2 - 1, previewY2 - 1,
                34,
                0.0625f,
                (float) mouseX, (float) mouseY,
                this.selectedNpc
            );
        }

        // Render base widgets (buttons, etc.)
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        // Render Emote Selection Modal on top if open
        if (this.isEmoteModalOpen) {
            renderEmoteModal(extractor, mouseX, mouseY, partialTick);
        }
    }

    private void renderEmoteModal(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int dropX = cx - 100;
        int dropY = cy - 90;
        int dropW = 200;
        int dropH = 180;

        extractor.fill(dropX - 1, dropY - 1, dropX + dropW + 1, dropY + dropH + 1, 0xFFE082A8);
        extractor.fill(dropX, dropY, dropX + dropW, dropY + dropH, 0xF8101018);

        // Modal Header
        extractor.fill(dropX, dropY, dropX + dropW, dropY + 17, 0xFF251628);
        extractor.text(this.font, "Pilih Emote untuk Timeline", dropX + 6, dropY + 4, 0xFFFFB6C1, false);

        // Close button [✕]
        int closeX = dropX + dropW - 16;
        int closeY = dropY + 3;
        boolean hoverClose = mouseX >= closeX && mouseX <= closeX + 13 && mouseY >= closeY && mouseY <= closeY + 13;
        extractor.fill(closeX, closeY, closeX + 13, closeY + 13, hoverClose ? 0xFF882030 : 0xFF352035);
        extractor.outline(closeX, closeY, 13, 13, hoverClose ? 0xFFFF4466 : 0xFF665066);
        extractor.text(this.font, "✕", closeX + 3, closeY + 2, 0xFFFFFFFF, false);

        // Search Box
        if (this.emoteSearchBox != null) {
            this.emoteSearchBox.extractRenderState(extractor, mouseX, mouseY, partialTick);
        }

        extractor.fill(dropX + 4, dropY + 37, dropX + dropW - 4, dropY + 38, 0x55E082A8);

        // List
        int listTop = dropY + 40;
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

        extractor.enableScissor(dropX + 4, listTop, dropX + dropW - 6, listBottom);

        for (int i = 0; i < visibleCount; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex >= totalItems) break;

            int iy = listTop + (itemIndex * itemH) - this.emoteScrollOffset;
            if (iy + itemH < listTop || iy > listBottom) continue;

            boolean isHovered = mouseX >= dropX + 4 && mouseX <= dropX + dropW - 8 && mouseY >= iy && mouseY < iy + itemH && mouseY >= listTop && mouseY <= listBottom;

            if (hasCustomItem && itemIndex == 0) {
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
                boolean isSelected = entry.id().equalsIgnoreCase(this.selectedEmoteId);

                int bg = isSelected ? 0xFF502540 : (isHovered ? 0xFF302030 : 0x00000000);
                if (bg != 0) {
                    extractor.fill(dropX + 4, iy, dropX + dropW - 6, iy + itemH - 1, bg);
                }
                if (isSelected) {
                    extractor.outline(dropX + 4, iy, dropW - 10, itemH - 1, 0xFFE082A8);
                }

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

        if (maxScroll > 0) {
            int barX = dropX + dropW - 5;
            int barW = 2;
            extractor.fill(barX, listTop, barX + barW, listBottom, 0x33FFFFFF);
            int thumbH = Math.max(12, (listH * listH) / (totalItems * itemH));
            int thumbY = listTop + (this.emoteScrollOffset * (listH - thumbH)) / maxScroll;
            extractor.fill(barX, thumbY, barX + barW, thumbY + thumbH, 0xFFE082A8);
        }

        if (totalItems == 0) {
            extractor.centeredText(this.font, "Tidak ada emote cocok", dropX + (dropW / 2), listTop + 30, 0xFF888899);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isFocused) {
        if (this.isEmoteModalOpen) {
            int cx = this.width / 2;
            int cy = this.height / 2;
            int dropX = cx - 100;
            int dropY = cy - 90;
            int dropW = 200;
            int dropH = 180;

            double mouseX = event.x();
            double mouseY = event.y();

            // Click outside closes modal
            if (mouseX < dropX || mouseX > dropX + dropW || mouseY < dropY || mouseY > dropY + dropH) {
                this.isEmoteModalOpen = false;
                playUiClick();
                return true;
            }

            // Close button
            int closeX = dropX + dropW - 16;
            int closeY = dropY + 3;
            if (mouseX >= closeX && mouseX <= closeX + 13 && mouseY >= closeY && mouseY <= closeY + 13) {
                this.isEmoteModalOpen = false;
                playUiClick();
                return true;
            }

            // Search box click
            if (this.emoteSearchBox != null && this.emoteSearchBox.isMouseOver(mouseX, mouseY)) {
                return this.emoteSearchBox.mouseClicked(event, isFocused);
            }

            // List item click
            int listTop = dropY + 40;
            int listBottom = dropY + dropH - 4;
            int itemH = 16;
            if (mouseY >= listTop && mouseY <= listBottom && mouseX >= dropX + 4 && mouseX <= dropX + dropW - 8) {
                int clickedIndex = (int) (mouseY - listTop + this.emoteScrollOffset) / itemH;
                String query = this.emoteSearchBox != null ? this.emoteSearchBox.getValue().trim() : "";
                boolean hasCustomItem = !query.isEmpty() && this.filteredEmotes.stream().noneMatch(e -> e.id().equalsIgnoreCase(query));

                if (hasCustomItem && clickedIndex == 0) {
                    selectEmoteFromModal(query);
                    return true;
                } else {
                    int realIdx = hasCustomItem ? clickedIndex - 1 : clickedIndex;
                    if (realIdx >= 0 && realIdx < this.filteredEmotes.size()) {
                        selectEmoteFromModal(this.filteredEmotes.get(realIdx).id());
                        return true;
                    }
                }
            }
            return true;
        }

        return super.mouseClicked(event, isFocused);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.isEmoteModalOpen) {
            int cx = this.width / 2;
            int cy = this.height / 2;
            int dropX = cx - 100;
            int dropY = cy - 90;
            int dropW = 200;
            int dropH = 180;

            if (mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= dropY + 40 && mouseY <= dropY + dropH) {
                String query = this.emoteSearchBox != null ? this.emoteSearchBox.getValue().trim() : "";
                boolean hasCustomItem = !query.isEmpty() && this.filteredEmotes.stream().noneMatch(e -> e.id().equalsIgnoreCase(query));
                int totalItems = this.filteredEmotes.size() + (hasCustomItem ? 1 : 0);
                int maxScroll = Math.max(0, totalItems * 16 - (dropH - 44));
                this.emoteScrollOffset = (int) Math.clamp(this.emoteScrollOffset - verticalAmount * 16.0, 0, maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.isEmoteModalOpen) {
            if (event.key() == 256) { // ESC closes modal
                this.isEmoteModalOpen = false;
                return true;
            }
            if (event.key() == 257 || event.key() == 335) { // Enter selects first
                String query = this.emoteSearchBox != null ? this.emoteSearchBox.getValue().trim() : "";
                if (!this.filteredEmotes.isEmpty()) {
                    selectEmoteFromModal(this.filteredEmotes.get(0).id());
                } else if (!query.isEmpty()) {
                    selectEmoteFromModal(query);
                }
                return true;
            }
            if (this.emoteSearchBox != null) {
                return this.emoteSearchBox.keyPressed(event);
            }
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.isEmoteModalOpen && this.emoteSearchBox != null) {
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
    public void onClose() {
        if (this.parentScreen != null) {
            Minecraft.getInstance().gui.setScreen(this.parentScreen);
        } else {
            super.onClose();
        }
    }

    private void playUiClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
