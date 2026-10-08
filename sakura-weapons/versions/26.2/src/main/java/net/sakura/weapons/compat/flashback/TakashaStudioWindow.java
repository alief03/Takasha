package net.sakura.weapons.compat.flashback;

import imgui.moulberry90.ImGui;
import imgui.moulberry90.type.ImBoolean;
import imgui.moulberry90.type.ImString;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Rotations;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sakura.weapons.client.gui.TakashaNpcScreen;
import net.sakura.weapons.client.replay.ReplayPoseOverrideManager;
import net.sakura.weapons.compat.EmotecraftCompat;
import net.sakura.weapons.entity.TakashaNpcEntity;

import java.util.*;

/**
 * Dedicated floating Dear ImGui studio window for Flashback editor in Minecraft 26.2.
 * Integrates NPC scene radar, 32-preset pose browser, limb rotation sliders,
 * searchable Emotecraft emote picker, wing animation timing controls, and timeline keyframing.
 */
public final class TakashaStudioWindow {

    public static final ImBoolean isOpen = new ImBoolean(false);
    private static UUID selectedNpcUuid = null;

    private static final ImString npcFilterQuery = new ImString(64);
    private static final ImString emoteSearchQuery = new ImString(64);
    private static int activePresetCategory = 0; // 0: All, 1: Combat, 2: Casual, 3: Valentine, 4: Mecha

    private static final String[] MODE_LABELS = {"Loop", "Tahan (Freeze)", "Sekali (Once)"};

    private TakashaStudioWindow() {}

    public static void toggle() {
        isOpen.set(!isOpen.get());
    }

    public static boolean isOpen() {
        return isOpen.get();
    }

    public static void setOpen(boolean open) {
        isOpen.set(open);
    }

    public static void selectNpc(UUID uuid) {
        selectedNpcUuid = uuid;
    }

    public static void render() {
        if (!isOpen.get()) return;

        try {
            ImGui.setNextWindowSize(560f, 440f);
            if (ImGui.begin("Sakura Replay Studio (Flashback 26.2)###TakashaStudio", isOpen)) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) {
                    ImGui.textUnformatted("Dunia replay belum dimuat.");
                    ImGui.end();
                    return;
                }

                // Scan nearby NPCs
                Entity cam = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
                AABB box = cam != null
                    ? cam.getBoundingBox().inflate(256.0)
                    : new AABB(-500, -64, -500, 500, 320, 500);

                List<TakashaNpcEntity> npcs = mc.level.getEntitiesOfClass(TakashaNpcEntity.class, box);
                if (cam != null) {
                    npcs.sort(Comparator.comparingDouble(e -> e.distanceToSqr(cam)));
                }

                // Ensure a valid selected NPC if available
                TakashaNpcEntity currentNpc = null;
                if (!npcs.isEmpty()) {
                    if (selectedNpcUuid != null) {
                        for (TakashaNpcEntity npc : npcs) {
                            if (npc.getUUID().equals(selectedNpcUuid)) {
                                currentNpc = npc;
                                break;
                            }
                        }
                    }
                    if (currentNpc == null) {
                        currentNpc = npcs.get(0);
                        selectedNpcUuid = currentNpc.getUUID();
                    }
                }

                // Top Header Summary
                if (currentNpc != null) {
                    String name = currentNpc.hasCustomName() ? currentNpc.getCustomName().getString() : "Takasha NPC";
                    ImGui.textUnformatted("Target Aktif: " + cleanAscii(name) + " (" + currentNpc.getUUID().toString().substring(0, 8) + ")");
                } else {
                    ImGui.textUnformatted("Target Aktif: (Tidak ada Takasha NPC ditemukan di adegan)");
                }

                ImGui.separator();

                if (ImGui.beginTabBar("TakashaStudioTabs")) {

                    // ==========================================
                    // TAB 1: SCENE RADAR
                    // ==========================================
                    if (ImGui.beginTabItem("Scene Radar##tabRadar")) {
                        renderRadarTab(npcs, cam);
                        ImGui.endTabItem();
                    }

                    // ==========================================
                    // TAB 2: KATALOG POSE & EMOTE
                    // ==========================================
                    if (ImGui.beginTabItem("Pose & Emote##tabPose")) {
                        renderPoseTab(currentNpc);
                        ImGui.endTabItem();
                    }

                    // ==========================================
                    // TAB 3: KOSMETIK & SAYAP
                    // ==========================================
                    if (ImGui.beginTabItem("Kosmetik & Sayap##tabCosmetics")) {
                        renderCosmeticsTab();
                        ImGui.endTabItem();
                    }

                    // ==========================================
                    // TAB 4: TIMELINE KEYFRAMING
                    // ==========================================
                    if (ImGui.beginTabItem("Timeline Keyframes##tabTimeline")) {
                        renderTimelineTab(currentNpc);
                        ImGui.endTabItem();
                    }

                    ImGui.endTabBar();
                }
            }
            ImGui.end();
        } catch (Throwable ignored) {
            // Guard against any UI interruption
        }
    }

    private static void renderRadarTab(List<TakashaNpcEntity> npcs, Entity cam) {
        ImGui.textUnformatted("Total NPC terdeteksi: " + npcs.size());
        ImGui.inputTextWithHint("##npcFilter", "Cari nama NPC...", npcFilterQuery);
        String filter = npcFilterQuery.get().trim().toLowerCase();

        if (ImGui.beginChild("radar_list_child", 0f, 280f, true)) {
            if (npcs.isEmpty()) {
                ImGui.textUnformatted("Tidak ada Takasha NPC di dekat kamera replay.");
            } else {
                for (TakashaNpcEntity npc : npcs) {
                    String name = npc.hasCustomName() ? npc.getCustomName().getString() : "Takasha NPC";
                    if (!filter.isEmpty() && !name.toLowerCase().contains(filter)) {
                        continue;
                    }

                    UUID id = npc.getUUID();
                    boolean isSelected = id.equals(selectedNpcUuid);
                    double dist = cam != null ? Math.sqrt(npc.distanceToSqr(cam)) : 0.0;

                    int currentTick = FlashbackCompatHelper.getCurrentReplayTick();
                    ReplayPoseOverrideManager.PoseOverride ov = ReplayPoseOverrideManager.getOverride(id, currentTick);
                    int preset = ov != null ? ov.preset() : npc.getPosePreset();
                    String emote = ov != null ? ov.emoteId() : npc.getEmoteId();

                    ImGui.pushID(id.toString());
                    if (ImGui.selectable(cleanAscii(name) + " [" + String.format(Locale.ROOT, "%.1fm", dist) + "]##sel", isSelected)) {
                        selectedNpcUuid = id;
                    }
                    ImGui.sameLine();
                    if (ImGui.button("Kamera##cam")) {
                        selectedNpcUuid = id;
                        if (cam != null) {
                            cam.lookAt(EntityAnchorArgument.Anchor.EYES, npc.getEyePosition());
                        }
                    }
                    ImGui.sameLine();
                    String poseDesc = preset > 0 ? "Pose " + preset : (!emote.isBlank() ? "Emote: " + emote : "Bebas");
                    ImGui.textUnformatted("(" + poseDesc + ")");
                    ImGui.popID();
                }
            }
            ImGui.endChild();
        }

        if (ImGui.button("Reset Semua Override NPC##resetAll")) {
            ReplayPoseOverrideManager.clearAll();
        }
    }

    private static void renderPoseTab(TakashaNpcEntity npc) {
        if (npc == null) {
            ImGui.textUnformatted("Pilih NPC terlebih dahulu di tab 'Scene Radar'.");
            return;
        }

        UUID id = npc.getUUID();
        int currentTick = FlashbackCompatHelper.getCurrentReplayTick();
        ReplayPoseOverrideManager.PoseOverride ov = ReplayPoseOverrideManager.getOverride(id, currentTick);

        int activePreset = ov != null ? ov.preset() : npc.getPosePreset();
        String activeEmote = ov != null ? ov.emoteId() : npc.getEmoteId();
        byte activeMode = ov != null ? ov.playMode() : npc.getEmotePlayMode();
        if (activeEmote == null) activeEmote = "";

        ImGui.textUnformatted("Pose Aktif: " + FlashbackSakuraPanel.presetName(activePreset));
        if (!activeEmote.isBlank()) {
            ImGui.sameLine();
            ImGui.textUnformatted("| Emote: " + activeEmote);
        }

        // Category Filter
        if (ImGui.button("Semua##catAll")) activePresetCategory = 0;
        ImGui.sameLine();
        if (ImGui.button("Combat##catCombat")) activePresetCategory = 1;
        ImGui.sameLine();
        if (ImGui.button("Casual/Idle##catCasual")) activePresetCategory = 2;
        ImGui.sameLine();
        if (ImGui.button("Valentine##catVal")) activePresetCategory = 3;
        ImGui.sameLine();
        if (ImGui.button("Dragon Mecha##catMecha")) activePresetCategory = 4;

        // Preset Grid
        int start = 1, end = 32;
        if (activePresetCategory == 1) { start = 1; end = 8; }
        else if (activePresetCategory == 2) { start = 9; end = 16; }
        else if (activePresetCategory == 3) { start = 17; end = 24; }
        else if (activePresetCategory == 4) { start = 25; end = 32; }

        if (ImGui.beginChild("preset_grid_child", 0f, 110f, true)) {
            if (ImGui.button("0: Bebas (Default)##p0")) {
                apply(id, 0, activeEmote, activeMode);
            }
            ImGui.sameLine();

            int col = 1;
            for (int p = start; p <= end; p++) {
                String label = p + ": " + FlashbackSakuraPanel.presetName(p);
                boolean isCurrent = (activePreset == p);
                if (ImGui.button((isCurrent ? "> " : "") + label + "##p" + p)) {
                    apply(id, p, "", activeMode);
                }
                col++;
                if (col % 2 != 0 && p < end) {
                    ImGui.sameLine();
                }
            }
            ImGui.endChild();
        }

        // Emotecraft Section
        ImGui.separator();
        ImGui.textUnformatted("Emotecraft Emote Picker:");
        ImGui.inputTextWithHint("##studioEmoteSearch", "Cari nama emote...", emoteSearchQuery);
        String q = emoteSearchQuery.get().trim().toLowerCase();

        if (ImGui.beginChild("studio_emotes_scroll", 0f, 95f, true)) {
            List<EmotecraftCompat.EmoteEntry> emotes = EmotecraftCompat.getDetectedEmotes();
            boolean found = false;
            for (EmotecraftCompat.EmoteEntry entry : emotes) {
                if (q.isEmpty() || entry.displayName().toLowerCase().contains(q) || entry.id().toLowerCase().contains(q)) {
                    found = true;
                    boolean isCur = activeEmote.equalsIgnoreCase(entry.id());
                    if (ImGui.selectable(cleanAscii(entry.toString()) + "##" + entry.id(), isCur)) {
                        apply(id, 0, entry.id(), activeMode);
                    }
                }
            }
            if (!found) {
                ImGui.textUnformatted("Tidak ada emote yang cocok.");
            }
            ImGui.endChild();
        }

        // Mode and Limb Rotations
        ImGui.textUnformatted("Mode Putar: " + MODE_LABELS[Math.floorMod(activeMode, MODE_LABELS.length)]);
        ImGui.sameLine();
        if (ImGui.button("Ganti Mode Putar##studioMode")) {
            apply(id, activePreset, activeEmote, (byte) ((activeMode + 1) % MODE_LABELS.length));
        }
        if (!activeEmote.isBlank()) {
            ImGui.sameLine();
            if (ImGui.button("Hapus Emote##studioClearEmote")) {
                apply(id, activePreset, "", activeMode);
            }
        }

        if (ImGui.treeNode("Rotasi Halus Anggota Tubuh (Limb Sliders)##studioLimbs")) {
            Rotations head = ov != null ? ov.getHeadRotations() : npc.getHeadPose();
            float[] headArr = new float[]{head.x(), head.y(), head.z()};
            if (ImGui.sliderFloat3("Kepala##sHead", headArr, -180f, 180f)) {
                applyRotations(id, ov, new Rotations(headArr[0], headArr[1], headArr[2]), null, null, null, null, null);
            }

            Rotations body = ov != null ? ov.getBodyRotations() : npc.getBodyPose();
            float[] bodyArr = new float[]{body.x(), body.y(), body.z()};
            if (ImGui.sliderFloat3("Badan##sBody", bodyArr, -180f, 180f)) {
                applyRotations(id, ov, null, new Rotations(bodyArr[0], bodyArr[1], bodyArr[2]), null, null, null, null);
            }

            Rotations leftArm = ov != null ? ov.getLeftArmRotations() : npc.getLeftArmPose();
            float[] laArr = new float[]{leftArm.x(), leftArm.y(), leftArm.z()};
            if (ImGui.sliderFloat3("Lengan Kiri##sLA", laArr, -180f, 180f)) {
                applyRotations(id, ov, null, null, new Rotations(laArr[0], laArr[1], laArr[2]), null, null, null);
            }

            Rotations rightArm = ov != null ? ov.getRightArmRotations() : npc.getRightArmPose();
            float[] raArr = new float[]{rightArm.x(), rightArm.y(), rightArm.z()};
            if (ImGui.sliderFloat3("Lengan Kanan##sRA", raArr, -180f, 180f)) {
                applyRotations(id, ov, null, null, null, new Rotations(raArr[0], raArr[1], raArr[2]), null, null);
            }

            ImGui.treePop();
        }
    }

    private static void renderCosmeticsTab() {
        ImGui.textUnformatted("Pengaturan Render Sinematik Sayap & Kosmetik:");
        ImGui.spacing();

        // Wing Animation Speed Multiplier
        float[] speed = new float[]{FlashbackCompatHelper.wingAnimationSpeedMultiplier};
        if (ImGui.sliderFloat("Kecepatan Sayap (x)##wingSpeed", speed, 0.0f, 3.0f)) {
            FlashbackCompatHelper.wingAnimationSpeedMultiplier = speed[0];
        }

        ImGui.sameLine();
        if (ImGui.button("Reset (1.0x)##resetSpeed")) {
            FlashbackCompatHelper.wingAnimationSpeedMultiplier = 1.0f;
        }

        // Quick Presets
        if (ImGui.button("Normal (1.0x)##w1")) FlashbackCompatHelper.wingAnimationSpeedMultiplier = 1.0f;
        ImGui.sameLine();
        if (ImGui.button("Slow Motion (0.5x)##w05")) FlashbackCompatHelper.wingAnimationSpeedMultiplier = 0.5f;
        ImGui.sameLine();
        if (ImGui.button("Super Slow (0.2x)##w02")) FlashbackCompatHelper.wingAnimationSpeedMultiplier = 0.2f;
        ImGui.sameLine();
        if (ImGui.button("Cepat (2.0x)##w2")) FlashbackCompatHelper.wingAnimationSpeedMultiplier = 2.0f;

        ImGui.spacing();
        if (ImGui.checkbox("Kunci/Freeze Posisi Sayap (Still Frame)##freezeWing", FlashbackCompatHelper.wingAnimationFrozen)) {
            FlashbackCompatHelper.wingAnimationFrozen = !FlashbackCompatHelper.wingAnimationFrozen;
        }

        ImGui.spacing();
        if (ImGui.checkbox("Tampilkan Partikel Sakura di Replay##sakuraPart", FlashbackCompatHelper.sakuraParticlesEnabled)) {
            FlashbackCompatHelper.sakuraParticlesEnabled = !FlashbackCompatHelper.sakuraParticlesEnabled;
        }

        ImGui.separator();
        ImGui.textUnformatted("Keterangan: Pengaturan ini disinkronkan dengan sub-tick Flashback");
        ImGui.textUnformatted("sehingga render video ekspor tidak patah pada kecepatan rendah.");
    }

    private static void renderTimelineTab(TakashaNpcEntity npc) {
        if (npc == null) {
            ImGui.textUnformatted("Pilih NPC terlebih dahulu di tab 'Scene Radar'.");
            return;
        }

        UUID id = npc.getUUID();
        int currentTick = FlashbackCompatHelper.getCurrentReplayTick();

        ImGui.textUnformatted("Posisi Tick Replay Saat Ini: " + currentTick);
        ImGui.spacing();

        ReplayPoseOverrideManager.PoseOverride activeOv = ReplayPoseOverrideManager.getOverride(id, currentTick);
        int preset = activeOv != null ? activeOv.preset() : npc.getPosePreset();
        String emote = activeOv != null ? activeOv.emoteId() : npc.getEmoteId();
        byte mode = activeOv != null ? activeOv.playMode() : npc.getEmotePlayMode();

        if (ImGui.button("[+] Simpan Keyframe di Tick Ini (" + currentTick + ")##addKf")) {
            ReplayPoseOverrideManager.PoseOverride kf = new ReplayPoseOverrideManager.PoseOverride(preset, emote, mode, activeOv != null ? activeOv.bedHeightOffset : 0f);
            if (activeOv != null) {
                kf.setRotations(activeOv.getHeadRotations(), activeOv.getBodyRotations(), activeOv.getLeftArmRotations(), activeOv.getRightArmRotations(), activeOv.getLeftLegRotations(), activeOv.getRightLegRotations());
            }
            ReplayPoseOverrideManager.setKeyframe(id, currentTick, kf);
        }

        ImGui.separator();
        ImGui.textUnformatted("Daftar Keyframe Aktif untuk NPC Ini:");

        ReplayPoseOverrideManager.TimelineTrack track = ReplayPoseOverrideManager.getTimelineTrack(id);
        if (track == null || track.keyframes.isEmpty()) {
            ImGui.textUnformatted("(Belum ada keyframe tersimpan pada timeline untuk NPC ini)");
        } else {
            if (ImGui.beginChild("kf_list_child", 0f, 180f, true)) {
                List<Integer> ticksToRemove = new ArrayList<>();
                for (Map.Entry<Integer, ReplayPoseOverrideManager.PoseOverride> entry : track.keyframes.entrySet()) {
                    int kfTick = entry.getKey();
                    ReplayPoseOverrideManager.PoseOverride kf = entry.getValue();

                    String desc = kf.preset() > 0 ? "Preset " + kf.preset() : (!kf.emoteId().isBlank() ? "Emote: " + kf.emoteId() : "Pose Bebas");
                    ImGui.textUnformatted("Tick " + kfTick + ": " + desc);
                    ImGui.sameLine();
                    if (ImGui.button("Hapus##delKf" + kfTick)) {
                        ticksToRemove.add(kfTick);
                    }
                }
                for (int t : ticksToRemove) {
                    ReplayPoseOverrideManager.removeKeyframe(id, t);
                }
                ImGui.endChild();
            }

            if (ImGui.button("Hapus Semua Keyframe NPC Ini##clearAllNpcKf")) {
                for (int t : new ArrayList<>(track.keyframes.keySet())) {
                    ReplayPoseOverrideManager.removeKeyframe(id, t);
                }
            }
        }
    }

    private static void apply(UUID id, int preset, String emote, byte mode) {
        ReplayPoseOverrideManager.PoseOverride current = ReplayPoseOverrideManager.getOverride(id);
        ReplayPoseOverrideManager.PoseOverride updated = new ReplayPoseOverrideManager.PoseOverride(preset, emote, mode, current != null ? current.bedHeightOffset : 0f);
        if (current != null) {
            updated.setRotations(current.getHeadRotations(), current.getBodyRotations(), current.getLeftArmRotations(), current.getRightArmRotations(), current.getLeftLegRotations(), current.getRightLegRotations());
        }
        ReplayPoseOverrideManager.setOverride(id, updated);
    }

    private static void applyRotations(UUID id, ReplayPoseOverrideManager.PoseOverride ov, Rotations head, Rotations body, Rotations leftArm, Rotations rightArm, Rotations leftLeg, Rotations rightLeg) {
        ReplayPoseOverrideManager.PoseOverride updated = ov != null ? ov : new ReplayPoseOverrideManager.PoseOverride(0, "", (byte) 0, 0f);
        updated.setRotations(
            head != null ? head : updated.getHeadRotations(),
            body != null ? body : updated.getBodyRotations(),
            leftArm != null ? leftArm : updated.getLeftArmRotations(),
            rightArm != null ? rightArm : updated.getRightArmRotations(),
            leftLeg != null ? leftLeg : updated.getLeftLegRotations(),
            rightLeg != null ? rightLeg : updated.getRightLegRotations()
        );
        ReplayPoseOverrideManager.setOverride(id, updated);
    }

    public static String cleanAscii(String text) {
        if (text == null) return "";
        return text.replaceAll("[^\\x20-\\x7E]", "").replaceAll("\\s+", " ").trim();
    }
}
