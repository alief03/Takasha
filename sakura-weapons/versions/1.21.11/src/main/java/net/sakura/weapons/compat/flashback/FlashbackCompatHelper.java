package net.sakura.weapons.compat.flashback;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.util.UUID;

/**
 * Isolated helper for safely interacting with Flashback in Minecraft 1.21.11.
 * Pure reflection / safe stubbing ensures no compile or classloading issues when Flashback is absent.
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
            Class<?> clazz = Class.forName("com.moulberry.flashback.Flashback");
            Object res = clazz.getMethod("isInReplay").invoke(null);
            return res instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isRecording() {
        if (!FLASHBACK_PRESENT) return false;
        try {
            Class<?> clazz = Class.forName("com.moulberry.flashback.Flashback");
            Object recorder = clazz.getField("RECORDER").get(null);
            if (recorder != null) {
                Object paused = recorder.getClass().getMethod("isPaused").invoke(recorder);
                return paused instanceof Boolean b && !b;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static int getCurrentRecordingTick() {
        return 0;
    }

    public static UUID getActiveReplayOrRecordId() {
        return null;
    }

    public static boolean isExporting() {
        if (!FLASHBACK_PRESENT) return false;
        try {
            Class<?> clazz = Class.forName("com.moulberry.flashback.Flashback");
            Object res = clazz.getMethod("isExporting").invoke(null);
            return res instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static int getCurrentReplayTick() {
        return 0;
    }

    public static float getPartialReplayTick() {
        return 0f;
    }

    public static int getTimelineCursorTick() {
        return 0;
    }
}
