package net.sakura.weapons.client.replay;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.Player;
import net.sakura.weapons.compat.EmotecraftCompat;
import net.sakura.weapons.compat.flashback.FlashbackCompatHelper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zero-desync playback and scrubbing bridge for other players' emotes in Flashback Replay Viewer.
 * Ensures smooth timeline seeking, rewinding, slow-motion interpolation, and clean stance reset.
 */
@Environment(EnvType.CLIENT)
public final class PlayerReplayPlaybackBridge {

    private static final Map<UUID, ActivePlaybackState> ACTIVE_PLAYBACK = new ConcurrentHashMap<>();

    private static class ActivePlaybackState {
        String lastEmoteId = "";
        int lastStartTick = -1;
        float lastSeekOffset = -1f;
    }

    private PlayerReplayPlaybackBridge() {}

    /**
     * Bridges timeline keyframes to live avatar state during render extraction in Flashback replays.
     */
    public static void applyReplayEmote(Player player, AvatarRenderState state, float partialTick) {
        if (player == null) return;
        if (!FlashbackCompatHelper.isFlashbackActive()) {
            if (!ACTIVE_PLAYBACK.isEmpty()) {
                ACTIVE_PLAYBACK.clear();
            }
            return;
        }

        UUID uuid = player.getUUID();
        int currentReplayTick = FlashbackCompatHelper.getCurrentReplayTick();
        var keyframe = ReplayPoseOverrideManager.getPlayerKeyframe(uuid, currentReplayTick);

        ActivePlaybackState playback = ACTIVE_PLAYBACK.computeIfAbsent(uuid, k -> new ActivePlaybackState());

        if (keyframe != null && keyframe.isActiveAt(currentReplayTick)) {
            float elapsedOffset = (float) (currentReplayTick - keyframe.startTick) + partialTick;

            boolean needsPlay = false;
            if (!keyframe.emoteId.equalsIgnoreCase(playback.lastEmoteId) || keyframe.startTick != playback.lastStartTick) {
                needsPlay = true;
            } else {
                // If user scrubbed / jumped significantly (> 2.5 ticks), re-seek emote position
                float expectedOffset = playback.lastSeekOffset + 1.0f;
                if (Math.abs(elapsedOffset - expectedOffset) > 2.5f) {
                    needsPlay = true;
                }
            }

            if (needsPlay || !EmotecraftCompat.isPlayingEmote(player)) {
                EmotecraftCompat.playEmoteSafely(player, keyframe.emoteId, keyframe.playMode, elapsedOffset);
                playback.lastEmoteId = keyframe.emoteId;
                playback.lastStartTick = keyframe.startTick;
            }
            playback.lastSeekOffset = elapsedOffset;

            if (state != null) {
                EmotecraftCompat.extractAnimationState(player, state, partialTick);
            }
        } else {
            // Outside of active keyframe range: reset player avatar pose cleanly
            if (!playback.lastEmoteId.isEmpty() || EmotecraftCompat.isPlayingEmote(player)) {
                EmotecraftCompat.resetAnimStateSafely(player);
                playback.lastEmoteId = "";
                playback.lastStartTick = -1;
                playback.lastSeekOffset = -1f;
            }
        }
    }
}
