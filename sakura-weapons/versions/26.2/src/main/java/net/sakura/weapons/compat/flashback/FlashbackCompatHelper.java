package net.sakura.weapons.compat.flashback;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.editor.ui.windows.TimelineWindow;
import com.moulberry.flashback.playback.ReplayServer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * Isolated helper for safely interacting with Moulberry's Flashback mod in Minecraft 26.2.
 * All Flashback calls are safeguarded so classes can never trigger link/class-load crashes
 * if Flashback is absent.
 */
@Environment(EnvType.CLIENT)
public final class FlashbackCompatHelper {

    private static final boolean FLASHBACK_PRESENT;
    private static Field recorderWrittenTicksField = null;
    private static Field recorderMetadataField = null;

    public static float wingAnimationSpeedMultiplier = 1.0f;
    public static boolean wingAnimationFrozen = false;
    public static boolean sakuraParticlesEnabled = true;

    static {
        FLASHBACK_PRESENT = FabricLoader.getInstance().isModLoaded("flashback");
    }

    private FlashbackCompatHelper() {}

    public static boolean isFlashbackLoaded() {
        return FLASHBACK_PRESENT;
    }

    public static boolean isFlashbackActive() {
        if (!FLASHBACK_PRESENT) return false;
        try {
            return Flashback.isInReplay();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Checks if Flashback is currently actively recording in singleplayer or multiplayer.
     */
    public static boolean isRecording() {
        if (!FLASHBACK_PRESENT) return false;
        try {
            var recorder = Flashback.RECORDER;
            return recorder != null && !recorder.isPaused();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Gets the current written tick count of the active recording.
     */
    public static int getCurrentRecordingTick() {
        if (!FLASHBACK_PRESENT) return 0;
        try {
            var recorder = Flashback.RECORDER;
            if (recorder != null) {
                if (recorderWrittenTicksField == null) {
                    recorderWrittenTicksField = recorder.getClass().getDeclaredField("writtenTicks");
                    recorderWrittenTicksField.setAccessible(true);
                }
                return recorderWrittenTicksField.getInt(recorder);
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    /**
     * Gets the UUID uniquely identifying the current Flashback recording or replay session.
     */
    public static UUID getActiveReplayOrRecordId() {
        if (!FLASHBACK_PRESENT) return null;
        try {
            if (Flashback.isInReplay()) {
                ReplayServer server = Flashback.getReplayServer();
                if (server != null && server.getMetadata() != null) {
                    return server.getMetadata().replayIdentifier;
                }
            } else if (isRecording()) {
                var recorder = Flashback.RECORDER;
                if (recorder != null) {
                    if (recorderMetadataField == null) {
                        recorderMetadataField = recorder.getClass().getDeclaredField("metadata");
                        recorderMetadataField.setAccessible(true);
                    }
                    Object meta = recorderMetadataField.get(recorder);
                    if (meta instanceof com.moulberry.flashback.record.FlashbackMeta fMeta) {
                        return fMeta.replayIdentifier;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static boolean isExporting() {
        if (!FLASHBACK_PRESENT) return false;
        try {
            return Flashback.isExporting();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Gets the current playback tick or editor timeline cursor tick.
     */
    public static int getCurrentReplayTick() {
        if (!FLASHBACK_PRESENT) return 0;
        try {
            if (!Flashback.isInReplay()) return 0;

            ReplayServer server = Flashback.getReplayServer();
            if (server != null) {
                return server.getReplayTick();
            }

            return TimelineWindow.getCursorTick();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /**
     * Gets the sub-tick interpolated progress in Flashback for super-smooth slow-motion rendering.
     */
    public static float getPartialReplayTick() {
        if (!FLASHBACK_PRESENT) return 0f;
        try {
            if (!Flashback.isInReplay()) return 0f;

            ReplayServer server = Flashback.getReplayServer();
            if (server != null) {
                return (float) server.getPartialReplayTick();
            }
        } catch (Throwable ignored) {}
        return 0f;
    }

    /**
     * Returns the editor's cursor tick on the timeline.
     */
    public static int getTimelineCursorTick() {
        if (!FLASHBACK_PRESENT) return 0;
        try {
            return TimelineWindow.getCursorTick();
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
