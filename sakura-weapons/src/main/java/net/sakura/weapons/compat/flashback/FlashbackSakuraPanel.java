package net.sakura.weapons.compat.flashback;

import imgui.moulberry90.ImGui;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.sakura.weapons.client.gui.TakashaNpcScreen;
import net.sakura.weapons.client.gui.TakashaReplayOverrideScreen;
import net.sakura.weapons.client.replay.ReplayPoseOverrideManager;
import net.sakura.weapons.entity.TakashaNpcEntity;

import java.util.UUID;

/** ImGui section rendered inside Flashback's entity popup for Takasha NPCs. */
public final class FlashbackSakuraPanel {

    private static final String[] MODE_LABELS = {"Loop", "Tahan (Freeze)", "Sekali (Once)"};

    private FlashbackSakuraPanel() {}

    public static void render(Entity entity) {
        if (!(entity instanceof TakashaNpcEntity npc)) return;

        UUID id = npc.getUUID();
        ImGui.pushID("sakura_panel");
        ImGui.separator();
        if (ImGui.collapsingHeader("Sakura: Pose & Emote")) {
            ReplayPoseOverrideManager.PoseOverride ov = ReplayPoseOverrideManager.getOverride(id);

            int preset = ov != null ? ov.preset() : npc.getPosePreset();
            String emote = ov != null ? ov.emoteId() : npc.getEmoteId();
            byte mode = ov != null ? ov.playMode() : npc.getEmotePlayMode();
            if (emote == null) emote = "";

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
            ImGui.textUnformatted("Emote: " + (emote.isBlank() ? "(tidak ada)" : emote));
            if (!emote.isBlank() && ImGui.button("Hapus Emote##emoteClear")) {
                apply(id, preset, "", mode);
            }

            ImGui.textUnformatted("Mode: " + MODE_LABELS[Math.floorMod(mode, MODE_LABELS.length)]);
            if (ImGui.button("Ganti Mode##emoteMode")) {
                apply(id, preset, emote, (byte) ((mode + 1) % MODE_LABELS.length));
            }

            ImGui.spacing();
            if (ov != null && ImGui.button("Reset Override##reset")) {
                ReplayPoseOverrideManager.removeOverride(id);
            }
            if (ov != null) ImGui.sameLine();
            if (ImGui.button("Buka Studio Sakura (Lengkap)##studio")) {
                ImGui.closeCurrentPopup();
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> mc.gui.setScreen(new TakashaReplayOverrideScreen(id)));
            }
            if (ov != null) ImGui.textUnformatted("Override aktif (rekaman asli tidak berubah)");
        }
        ImGui.popID();
    }

    private static void apply(UUID id, int preset, String emote, byte mode) {
        ReplayPoseOverrideManager.setOverride(id, preset, emote, mode);
    }

    private static int step(int preset, int delta) {
        int total = TakashaNpcScreen.PRESET_DISPLAY_NAMES.length;
        return Math.floorMod(preset + delta, total);
    }

    /** ImGui's font lacks emoji glyphs, so strip non-ASCII symbols. */
    private static String presetName(int preset) {
        String[] names = TakashaNpcScreen.PRESET_DISPLAY_NAMES;
        if (preset < 0 || preset >= names.length) return String.valueOf(preset);
        return names[preset].replaceAll("[^\\x20-\\x7E]", "").replaceAll("\\s+", " ").trim();
    }
}
