package net.sakura.weapons.compat.flashback;

import imgui.moulberry90.ImGui;
import imgui.moulberry90.type.ImInt;
import imgui.moulberry90.type.ImString;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Rotations;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.sakura.weapons.client.gui.TakashaNpcScreen;
import net.sakura.weapons.client.gui.TakashaReplayOverrideScreen;
import net.sakura.weapons.client.replay.ReplayPoseOverrideManager;
import net.sakura.weapons.compat.EmotecraftCompat;
import net.sakura.weapons.entity.TakashaNpcEntity;

import java.util.List;
import java.util.UUID;

/**
 * ImGui section rendered inside Flashback's entity popup for Takasha NPCs and Players.
 * Provides live pose presets, searchable Emotecraft emote selection, limb rotation sliders,
 * and multi-player replay timeline keyframe controls.
 */
public final class FlashbackSakuraPanel {

    private static final String[] MODE_LABELS = {"Loop", "Tahan (Freeze)", "Sekali (Once)"};
    private static final ImString emoteSearchQuery = new ImString(64);
    private static boolean showEmoteList = false;

    // Player Emote Editor State
    private static final ImString playerEmoteSearchQuery = new ImString(64);
    private static boolean showPlayerEmoteList = false;
    private static String selectedPlayerEmote = "";
    private static byte selectedPlayerMode = 0;
    private static int selectedPlayerDuration = 60;

    private FlashbackSakuraPanel() {}

    public static void render(Entity entity) {
        if (entity instanceof TakashaNpcEntity npc) {
            renderNpcSection(npc);
        } else if (entity instanceof Player player) {
            renderPlayerSection(player);
        }
    }

    private static void renderNpcSection(TakashaNpcEntity npc) {
        UUID id = npc.getUUID();
        int currentTick = FlashbackCompatHelper.getCurrentReplayTick();

        ImGui.pushID("sakura_npc_panel");
        ImGui.separator();
        if (ImGui.collapsingHeader("Sakura: Pose & Emote (NPC)")) {
            ReplayPoseOverrideManager.PoseOverride ov = ReplayPoseOverrideManager.getOverride(id, currentTick);

            int preset = ov != null ? ov.preset() : npc.getPosePreset();
            String emote = ov != null ? ov.emoteId() : npc.getEmoteId();
            byte mode = ov != null ? ov.playMode() : npc.getEmotePlayMode();
            if (emote == null) emote = "";

            // 1. Pose Presets Section
            ImGui.textUnformatted("Pose: " + presetName(preset));
            if (ImGui.button("<##presetPrev")) {
                apply(id, step(preset, -1), emote, mode);
            }
            ImGui.sameLine();
            if (ImGui.button(">##presetNext")) {
                apply(id, step(preset, 1), emote, mode);
            }
            ImGui.sameLine();
            if (ImGui.button("Bebas (0)##presetZero")) {
                apply(id, 0, emote, mode);
            }

            ImGui.spacing();

            // 2. Emotecraft Emote Picker Section
            ImGui.textUnformatted("Emote: " + (emote.isBlank() ? "(tidak ada)" : emote));
            if (ImGui.button((showEmoteList ? "Tutup Daftar Emote" : "Pilih Emote Emotecraft...") + "##toggleEmotes")) {
                showEmoteList = !showEmoteList;
            }
            if (!emote.isBlank()) {
                ImGui.sameLine();
                if (ImGui.button("Hapus Emote##emoteClear")) {
                    apply(id, preset, "", mode);
                }
            }

            if (showEmoteList) {
                ImGui.inputTextWithHint("##emoteSearch", "Cari emote...", emoteSearchQuery);
                String query = emoteSearchQuery.get().trim().toLowerCase();

                if (ImGui.beginChild("sakura_emotes_child", 0, 110, true)) {
                    List<EmotecraftCompat.EmoteEntry> emotes = EmotecraftCompat.getDetectedEmotes();
                    boolean anyMatched = false;
                    for (EmotecraftCompat.EmoteEntry entry : emotes) {
                        String name = entry.displayName();
                        String eid = entry.id();
                        if (query.isEmpty() || name.toLowerCase().contains(query) || eid.toLowerCase().contains(query)) {
                            anyMatched = true;
                            boolean isSelected = emote.equalsIgnoreCase(eid);
                            String label = entry.toString();
                            if (ImGui.selectable(cleanAscii(label) + "##" + eid, isSelected)) {
                                apply(id, 0, eid, mode);
                                showEmoteList = false;
                            }
                        }
                    }
                    if (!anyMatched) {
                        ImGui.textUnformatted("Tidak ada emote yang cocok.");
                    }
                    ImGui.endChild();
                }
            }

            // 3. Play Mode Section
            ImGui.spacing();
            ImGui.textUnformatted("Mode: " + MODE_LABELS[Math.floorMod(mode, MODE_LABELS.length)]);
            ImGui.sameLine();
            if (ImGui.button("Ganti Mode##emoteMode")) {
                apply(id, preset, emote, (byte) ((mode + 1) % MODE_LABELS.length));
            }

            // 4. Quick Limb Rotations (Head & Body fine tuning)
            if (ImGui.treeNode("Rotasi Halus (Limb)##limbs")) {
                Rotations head = ov != null ? ov.getHeadRotations() : npc.getHeadPose();
                float[] headArr = new float[]{head.x(), head.y(), head.z()};
                if (ImGui.sliderFloat3("Kepala##head", headArr, -180f, 180f)) {
                    applyRotations(id, ov, new Rotations(headArr[0], headArr[1], headArr[2]), null, null, null, null, null);
                }

                Rotations body = ov != null ? ov.getBodyRotations() : npc.getBodyPose();
                float[] bodyArr = new float[]{body.x(), body.y(), body.z()};
                if (ImGui.sliderFloat3("Badan##body", bodyArr, -180f, 180f)) {
                    applyRotations(id, ov, null, new Rotations(bodyArr[0], bodyArr[1], bodyArr[2]), null, null, null, null);
                }

                ImGui.treePop();
            }

            // 5. Actions & Studio Shortcuts
            ImGui.spacing();
            if (ov != null && ImGui.button("Reset Override##reset")) {
                ReplayPoseOverrideManager.removeOverride(id);
            }
            if (ov != null) ImGui.sameLine();
            if (ImGui.button("Buka Studio Sakura (Floating)##studioFloating")) {
                TakashaStudioWindow.selectNpc(id);
                TakashaStudioWindow.isOpen.set(true);
            }
            ImGui.sameLine();
            if (ImGui.button("GUI Layar Penuh##studioVanilla")) {
                ImGui.closeCurrentPopup();
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> mc.gui.setScreen(new TakashaReplayOverrideScreen(id)));
            }

            if (ov != null) {
                ImGui.textUnformatted("Override aktif (rekaman asli aman)");
            }
        }
        ImGui.popID();
    }

    private static void renderPlayerSection(Player player) {
        UUID id = player.getUUID();
        String name = player.getGameProfile() != null ? player.getGameProfile().name() : player.getName().getString();
        int currentTick = FlashbackCompatHelper.getCurrentReplayTick();

        ImGui.pushID("sakura_player_panel");
        ImGui.separator();
        if (ImGui.collapsingHeader("Sakura: Emote Pemain (" + cleanAscii(name) + ")")) {
            ReplayPoseOverrideManager.PlayerEmoteKeyframe currentKf = ReplayPoseOverrideManager.getPlayerKeyframe(id, currentTick);
            ReplayPoseOverrideManager.PlayerTimelineTrack track = ReplayPoseOverrideManager.getPlayerTrack(id);

            ImGui.textUnformatted("Pemain: " + cleanAscii(name) + " (Tick Replay: " + currentTick + ")");

            if (currentKf != null && currentKf.isActiveAt(currentTick)) {
                ImGui.textUnformatted("Status: Memutar [Tick " + currentKf.startTick + " - " + (currentKf.startTick + currentKf.durationTicks) + "]");
                ImGui.textUnformatted("Emote: " + cleanAscii(currentKf.emoteId) + " (" + MODE_LABELS[Math.floorMod(currentKf.playMode, MODE_LABELS.length)] + ")");
            } else {
                ImGui.textUnformatted("Status: Berdiri Netral / Idle");
            }

            ImGui.spacing();

            // Emote Selection for Player
            String displayEmote = !selectedPlayerEmote.isBlank() ? selectedPlayerEmote : (currentKf != null ? currentKf.emoteId : "");
            ImGui.textUnformatted("Pilihan Emote: " + (displayEmote.isBlank() ? "(belum dipilih)" : cleanAscii(displayEmote)));
            if (ImGui.button((showPlayerEmoteList ? "Tutup Daftar Emote" : "Pilih Emote Emotecraft...") + "##togglePlayerEmotes")) {
                showPlayerEmoteList = !showPlayerEmoteList;
            }
            if (!displayEmote.isBlank()) {
                ImGui.sameLine();
                if (ImGui.button("Reset Pilihan##clearPlayerEmoteChoice")) {
                    selectedPlayerEmote = "";
                }
            }

            if (showPlayerEmoteList) {
                ImGui.inputTextWithHint("##playerEmoteSearch", "Cari emote pemain...", playerEmoteSearchQuery);
                String query = playerEmoteSearchQuery.get().trim().toLowerCase();

                if (ImGui.beginChild("sakura_player_emotes_child", 0, 110, true)) {
                    List<EmotecraftCompat.EmoteEntry> emotes = EmotecraftCompat.getDetectedEmotes();
                    boolean anyMatched = false;
                    for (EmotecraftCompat.EmoteEntry entry : emotes) {
                        String eName = entry.displayName();
                        String eid = entry.id();
                        if (query.isEmpty() || eName.toLowerCase().contains(query) || eid.toLowerCase().contains(query)) {
                            anyMatched = true;
                            boolean isSelected = displayEmote.equalsIgnoreCase(eid);
                            String label = entry.toString();
                            if (ImGui.selectable(cleanAscii(label) + "##p_" + eid, isSelected)) {
                                selectedPlayerEmote = eid;
                                showPlayerEmoteList = false;
                            }
                        }
                    }
                    if (!anyMatched) {
                        ImGui.textUnformatted("Tidak ada emote yang cocok.");
                    }
                    ImGui.endChild();
                }
            }

            // Mode Selection
            ImGui.spacing();
            ImGui.textUnformatted("Mode: " + MODE_LABELS[Math.floorMod(selectedPlayerMode, MODE_LABELS.length)]);
            ImGui.sameLine();
            if (ImGui.button("Ganti Mode##playerModeBtn")) {
                selectedPlayerMode = (byte) ((selectedPlayerMode + 1) % MODE_LABELS.length);
            }

            // Duration Slider
            int[] durationArr = new int[]{selectedPlayerDuration};
            if (ImGui.sliderInt("Durasi (Tick)##pDuration", durationArr, 10, 1200)) {
                selectedPlayerDuration = Math.max(10, durationArr[0]);
            }

            // Keyframe Operations
            ImGui.spacing();
            String emoteToApply = !selectedPlayerEmote.isBlank() ? selectedPlayerEmote : (currentKf != null ? currentKf.emoteId : "");
            if (ImGui.button("Pasang Keyframe (Tick " + currentTick + ")##addKfBtn")) {
                if (!emoteToApply.isBlank()) {
                    ReplayPoseOverrideManager.setPlayerKeyframe(id, name, currentTick, selectedPlayerDuration, emoteToApply, selectedPlayerMode);
                }
            }

            if (currentKf != null) {
                ImGui.sameLine();
                if (ImGui.button("Hapus Keyframe (Tick " + currentKf.startTick + ")##delCurrentKfBtn")) {
                    ReplayPoseOverrideManager.removePlayerKeyframe(id, currentKf.startTick);
                }
            }

            ImGui.sameLine();
            if (ImGui.button("Hentikan Emote di Sini (Netral)##stopKfBtn")) {
                ReplayPoseOverrideManager.setPlayerKeyframe(id, name, currentTick, 20, "", (byte) 2);
            }

            // Existing Keyframes List
            ImGui.spacing();
            int kfCount = track != null ? track.keyframes.size() : 0;
            if (ImGui.treeNode("Timeline Keyframe (" + kfCount + ")##playerTimelineList")) {
                if (track == null || track.keyframes.isEmpty()) {
                    ImGui.textUnformatted("Belum ada keyframe tersimpan.");
                } else {
                    Integer toRemoveTick = null;
                    for (var entry : track.keyframes.entrySet()) {
                        var kf = entry.getValue();
                        String kfDesc = "[Tick " + kf.startTick + " - " + (kf.startTick + kf.durationTicks) + "] "
                            + (kf.emoteId.isBlank() ? "(STOP / Netral)" : cleanAscii(kf.emoteId))
                            + " [" + MODE_LABELS[Math.floorMod(kf.playMode, MODE_LABELS.length)] + "]";
                        ImGui.textUnformatted(kfDesc);
                        ImGui.sameLine();
                        if (ImGui.button("Hapus##delKf_" + kf.startTick)) {
                            toRemoveTick = kf.startTick;
                        }
                    }
                    if (toRemoveTick != null) {
                        ReplayPoseOverrideManager.removePlayerKeyframe(id, toRemoveTick);
                    }

                    ImGui.spacing();
                    if (ImGui.button("Hapus Semua Keyframe##delAllKfs")) {
                        track.keyframes.clear();
                        ReplayPoseOverrideManager.save();
                    }
                }
                ImGui.treePop();
            }
        }
        ImGui.popID();
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

    private static int step(int preset, int delta) {
        int total = TakashaNpcScreen.PRESET_DISPLAY_NAMES.length;
        return Math.floorMod(preset + delta, total);
    }

    /** ImGui's font lacks emoji glyphs, so strip non-ASCII symbols. */
    public static String cleanAscii(String text) {
        if (text == null) return "";
        return text.replaceAll("[^\\x20-\\x7E]", "").replaceAll("\\s+", " ").trim();
    }

    public static String presetName(int preset) {
        String[] names = TakashaNpcScreen.PRESET_DISPLAY_NAMES;
        if (preset < 0 || preset >= names.length) return String.valueOf(preset);
        return cleanAscii(names[preset]);
    }
}
