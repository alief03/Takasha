package net.sakura.weapons.client.replay;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.sakura.weapons.SakuraWeaponsMod;
import net.sakura.weapons.compat.EmotecraftCompat;
import net.sakura.weapons.compat.flashback.FlashbackCompatHelper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Background auto-capture recorder for both local user and remote players' Emotecraft emotes
 * during Flashback recordings.
 * Operates client-side autonomously on Dedicated Servers, LAN worlds, and Singleplayer.
 */
@Environment(EnvType.CLIENT)
public final class PlayerEmoteRecorder {

    private static final Map<UUID, ActiveRecording> ACTIVE_RECORDINGS = new ConcurrentHashMap<>();

    private static class ActiveRecording {
        final int startTick;
        final String emoteId;
        final byte playMode;
        int duration;
        int lastSeenTick;

        ActiveRecording(int startTick, String emoteId, byte playMode, int duration) {
            this.startTick = startTick;
            this.emoteId = emoteId;
            this.playMode = playMode;
            this.duration = Math.max(duration, 20);
            this.lastSeenTick = startTick;
        }
    }

    private PlayerEmoteRecorder() {}

    /**
     * Event hook invoked when an Emotecraft emote starts playing on any player
     * (either local client user or remote server players via network/online-emotes).
     */
    public static void onEmotePlayEvent(UUID playerUUID, Object animation, float tickOffset) {
        if (playerUUID == null || animation == null) return;
        if (!FlashbackCompatHelper.isRecording()) return;

        String emoteId = EmotecraftCompat.findEmoteIdFromAnimation(animation);
        if (emoteId == null || emoteId.isBlank()) {
            emoteId = playerUUID.toString();
        }
        EmotecraftCompat.registerNetworkAnimation(emoteId, animation);

        float len = EmotecraftCompat.getAnimationLength(animation);
        int duration = len > 0 ? Math.round(len) : 60;
        int currentTick = FlashbackCompatHelper.getCurrentRecordingTick();
        String playerName = resolvePlayerName(playerUUID);

        ActiveRecording prev = ACTIVE_RECORDINGS.get(playerUUID);
        if (prev != null) {
            int finalDuration = Math.max(1, currentTick - prev.startTick);
            ReplayPoseOverrideManager.updatePlayerKeyframeDuration(playerUUID, prev.startTick, finalDuration);
        }

        ACTIVE_RECORDINGS.put(playerUUID, new ActiveRecording(currentTick, emoteId, (byte) 0, duration));
        ReplayPoseOverrideManager.recordPlayerKeyframe(playerUUID, playerName, currentTick, duration, emoteId, (byte) 0);
        SakuraWeaponsMod.LOGGER.info("Captured emote play event for player '{}' ({}) at tick {}: {}", playerName, playerUUID, currentTick, emoteId);
    }

    /**
     * Event hook invoked when an Emotecraft emote stops on any player.
     */
    public static void onEmoteStopEvent(UUID playerUUID, UUID emoteUUID) {
        if (playerUUID == null) return;
        if (!FlashbackCompatHelper.isRecording()) return;

        int currentTick = FlashbackCompatHelper.getCurrentRecordingTick();
        String playerName = resolvePlayerName(playerUUID);

        ActiveRecording active = ACTIVE_RECORDINGS.remove(playerUUID);
        if (active != null) {
            int finalDuration = Math.max(1, currentTick - active.startTick);
            ReplayPoseOverrideManager.updatePlayerKeyframeDuration(playerUUID, active.startTick, finalDuration);
        }

        // Record clean neutral stance keyframe
        ReplayPoseOverrideManager.recordPlayerKeyframe(playerUUID, playerName, currentTick, 20, "", (byte) 2);
    }

    /**
     * Called on each client player tick (20 TPS) while in world to provide safety polling.
     */
    public static void onPlayerTick(Player player) {
        if (player == null) return;
        if (!FlashbackCompatHelper.isRecording()) {
            if (!ACTIVE_RECORDINGS.isEmpty()) {
                ACTIVE_RECORDINGS.clear();
            }
            return;
        }

        UUID uuid = player.getUUID();
        String playerName = player.getGameProfile() != null ? player.getGameProfile().name() : "";
        boolean isPlaying = EmotecraftCompat.isPlayingEmote(player);

        if (isPlaying) {
            String emoteId = EmotecraftCompat.getActiveEmoteId(player);
            if (emoteId != null && !emoteId.isBlank()) {
                byte playMode = EmotecraftCompat.getActiveEmotePlayMode(player);
                int duration = EmotecraftCompat.getActiveEmoteDuration(player);
                int currentTick = FlashbackCompatHelper.getCurrentRecordingTick();

                ActiveRecording active = ACTIVE_RECORDINGS.get(uuid);
                if (active == null || !active.emoteId.equalsIgnoreCase(emoteId)) {
                    // Previous emote ended or new emote started
                    if (active != null) {
                        int finalDuration = Math.max(1, currentTick - active.startTick);
                        ReplayPoseOverrideManager.updatePlayerKeyframeDuration(uuid, active.startTick, finalDuration);
                    }

                    ACTIVE_RECORDINGS.put(uuid, new ActiveRecording(currentTick, emoteId, playMode, duration));
                    ReplayPoseOverrideManager.recordPlayerKeyframe(uuid, playerName, currentTick, duration, emoteId, playMode);
                } else {
                    active.lastSeenTick = currentTick;
                    int elapsed = currentTick - active.startTick;
                    if (elapsed > active.duration) {
                        active.duration = elapsed + 10;
                        ReplayPoseOverrideManager.updatePlayerKeyframeDuration(uuid, active.startTick, active.duration);
                    }
                }
            }
        } else {
            // Player is no longer playing emote
            ActiveRecording active = ACTIVE_RECORDINGS.remove(uuid);
            if (active != null) {
                int currentTick = FlashbackCompatHelper.getCurrentRecordingTick();
                int finalDuration = Math.max(1, currentTick - active.startTick);
                ReplayPoseOverrideManager.updatePlayerKeyframeDuration(uuid, active.startTick, finalDuration);
                // Record clean stop keyframe
                ReplayPoseOverrideManager.recordPlayerKeyframe(uuid, playerName, currentTick, 20, "", (byte) 2);
            }
        }
    }

    /**
     * Explicit trigger if captured via direct packet or mixin hook.
     */
    public static void onPlayerEmoteStarted(Player player, String emoteId, byte playMode, int durationTicks) {
        if (player == null || emoteId == null || emoteId.isBlank()) return;
        if (!FlashbackCompatHelper.isRecording()) return;

        UUID uuid = player.getUUID();
        String playerName = player.getGameProfile() != null ? player.getGameProfile().name() : "";
        int currentTick = FlashbackCompatHelper.getCurrentRecordingTick();

        ActiveRecording prev = ACTIVE_RECORDINGS.get(uuid);
        if (prev != null) {
            int finalDuration = Math.max(1, currentTick - prev.startTick);
            ReplayPoseOverrideManager.updatePlayerKeyframeDuration(uuid, prev.startTick, finalDuration);
        }

        ACTIVE_RECORDINGS.put(uuid, new ActiveRecording(currentTick, emoteId, playMode, durationTicks));
        ReplayPoseOverrideManager.recordPlayerKeyframe(uuid, playerName, currentTick, durationTicks, emoteId, playMode);
    }

    private static String resolvePlayerName(UUID uuid) {
        if (uuid == null) return "";
        try {
            var mc = Minecraft.getInstance();
            if (mc.player != null && uuid.equals(mc.player.getUUID()) && mc.player.getGameProfile() != null) {
                return mc.player.getGameProfile().name();
            }
            if (mc.level != null) {
                Player p = mc.level.getPlayerByUUID(uuid);
                if (p != null && p.getGameProfile() != null) {
                    return p.getGameProfile().name();
                }
            }
        } catch (Throwable ignored) {}
        return "";
    }
}
