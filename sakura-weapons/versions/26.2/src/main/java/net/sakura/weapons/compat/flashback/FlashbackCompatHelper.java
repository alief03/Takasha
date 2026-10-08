package net.sakura.weapons.compat.flashback;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.editor.ui.windows.TimelineWindow;
import com.moulberry.flashback.playback.ReplayServer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Isolated helper for safely interacting with Moulberry's Flashback mod in Minecraft 26.2.
 * All Flashback calls are safeguarded so classes can never trigger link/class-load crashes
 * if Flashback is absent.
 */
@Environment(EnvType.CLIENT)
public final class FlashbackCompatHelper {

    private static final boolean FLASHBACK_PRESENT;

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
