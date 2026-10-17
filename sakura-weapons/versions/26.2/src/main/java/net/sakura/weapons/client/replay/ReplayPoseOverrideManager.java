package net.sakura.weapons.client.replay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Rotations;
import net.sakura.weapons.SakuraWeaponsMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side management layer for dynamic pose and emote overrides in ReplayMod / Flashback.
 * Allows video editors to re-assign or tweak NPC emotes and poses directly on the replay timeline
 * without requiring re-recording.
 */
@Environment(EnvType.CLIENT)
public class ReplayPoseOverrideManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, PoseOverride> OVERRIDES = new ConcurrentHashMap<>();
    private static final Map<UUID, TimelineTrack> TIMELINE_TRACKS = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerTimelineTrack> PLAYER_TRACKS = new ConcurrentHashMap<>();
    private static volatile UUID loadedSessionId = null;
    private static boolean loaded = false;

    private static void ensureLoaded() {
        UUID currentSession = net.sakura.weapons.compat.flashback.FlashbackCompatHelper.getActiveReplayOrRecordId();
        if (!loaded || (currentSession != null && !currentSession.equals(loadedSessionId))) {
            loadSession(currentSession);
        }
    }

    public static class PlayerEmoteKeyframe {
        public int startTick = 0;
        public int durationTicks = 60;
        public String emoteId = "";
        public byte playMode = 0; // 0: Loop, 1: Hold (Tahan), 2: Stand (Sekali)

        public PlayerEmoteKeyframe() {}

        public PlayerEmoteKeyframe(int startTick, int durationTicks, String emoteId, byte playMode) {
            this.startTick = startTick;
            this.durationTicks = durationTicks > 0 ? durationTicks : 60;
            this.emoteId = emoteId != null ? emoteId : "";
            this.playMode = playMode;
        }

        public boolean isActiveAt(int tick) {
            if (emoteId == null || emoteId.isBlank()) return false;
            if (tick < startTick) return false;
            if (playMode == 2) {
                return tick <= (startTick + durationTicks);
            }
            return tick <= (startTick + Math.max(durationTicks, 60));
        }

        public float getProgress(int tick) {
            if (durationTicks <= 0) return 0f;
            float elapsed = (float) (tick - startTick);
            if (playMode == 1) {
                return Math.min(elapsed / (float) durationTicks, 1.0f);
            }
            if (playMode == 0) {
                return (elapsed % (float) durationTicks) / (float) durationTicks;
            }
            return Math.min(Math.max(elapsed / (float) durationTicks, 0f), 1f);
        }
    }

    public static class PlayerTimelineTrack {
        public String playerName = "";
        public final TreeMap<Integer, PlayerEmoteKeyframe> keyframes = new TreeMap<>();

        public PlayerTimelineTrack() {}

        public PlayerTimelineTrack(String playerName) {
            this.playerName = playerName != null ? playerName : "";
        }

        public PlayerEmoteKeyframe getActiveKeyframeAt(int tick) {
            if (keyframes.isEmpty()) return null;
            Map.Entry<Integer, PlayerEmoteKeyframe> entry = keyframes.floorEntry(tick);
            if (entry == null) return null;
            PlayerEmoteKeyframe kf = entry.getValue();
            if (kf != null && kf.isActiveAt(tick)) {
                return kf;
            }
            return null;
        }
    }

    public static class PoseOverride {
        public int posePreset = 0;
        public String emoteId = "";
        public byte emotePlayMode = 0; // 0: Loop, 1: Hold (Tahan), 2: Stand (Sekali)
        public float bedHeightOffset = 0.0f;
        public float yawRotation = 0.0f;

        public float headX = 0f, headY = 0f, headZ = 0f;
        public float bodyX = 0f, bodyY = 0f, bodyZ = 0f;
        public float leftArmX = 0f, leftArmY = 0f, leftArmZ = 0f;
        public float rightArmX = 0f, rightArmY = 0f, rightArmZ = 0f;
        public float leftLegX = 0f, leftLegY = 0f, leftLegZ = 0f;
        public float rightLegX = 0f, rightLegY = 0f, rightLegZ = 0f;

        public PoseOverride() {}

        public PoseOverride(int posePreset, String emoteId, byte emotePlayMode, float bedHeightOffset) {
            this.posePreset = posePreset;
            this.emoteId = emoteId != null ? emoteId : "";
            this.emotePlayMode = emotePlayMode;
            this.bedHeightOffset = bedHeightOffset;
        }

        public Rotations getHeadRotations() { return new Rotations(headX, headY, headZ); }
        public Rotations getBodyRotations() { return new Rotations(bodyX, bodyY, bodyZ); }
        public Rotations getLeftArmRotations() { return new Rotations(leftArmX, leftArmY, leftArmZ); }
        public Rotations getRightArmRotations() { return new Rotations(rightArmX, rightArmY, rightArmZ); }
        public Rotations getLeftLegRotations() { return new Rotations(leftLegX, leftLegY, leftLegZ); }
        public Rotations getRightLegRotations() { return new Rotations(rightLegX, rightLegY, rightLegZ); }

        public boolean hasPreset() { return posePreset > 0; }
        public boolean hasEmote() { return emoteId != null && !emoteId.isBlank(); }
        public boolean hasCustomRotations() {
            return headX != 0f || headY != 0f || headZ != 0f
                || bodyX != 0f || bodyY != 0f || bodyZ != 0f
                || leftArmX != 0f || leftArmY != 0f || leftArmZ != 0f
                || rightArmX != 0f || rightArmY != 0f || rightArmZ != 0f
                || leftLegX != 0f || leftLegY != 0f || leftLegZ != 0f
                || rightLegX != 0f || rightLegY != 0f || rightLegZ != 0f;
        }
        public int preset() { return posePreset; }
        public String emoteId() { return emoteId; }
        public byte playMode() { return emotePlayMode; }

        public void setRotations(Rotations head, Rotations body, Rotations leftArm, Rotations rightArm, Rotations leftLeg, Rotations rightLeg) {
            if (head != null) { this.headX = head.x(); this.headY = head.y(); this.headZ = head.z(); }
            if (body != null) { this.bodyX = body.x(); this.bodyY = body.y(); this.bodyZ = body.z(); }
            if (leftArm != null) { this.leftArmX = leftArm.x(); this.leftArmY = leftArm.y(); this.leftArmZ = leftArm.z(); }
            if (rightArm != null) { this.rightArmX = rightArm.x(); this.rightArmY = rightArm.y(); this.rightArmZ = rightArm.z(); }
            if (leftLeg != null) { this.leftLegX = leftLeg.x(); this.leftLegY = leftLeg.y(); this.leftLegZ = leftLeg.z(); }
            if (rightLeg != null) { this.rightLegX = rightLeg.x(); this.rightLegY = rightLeg.y(); this.rightLegZ = rightLeg.z(); }
        }
    }

    public static class TimelineTrack {
        public final TreeMap<Integer, PoseOverride> keyframes = new TreeMap<>();

        public PoseOverride getAtTick(int tick) {
            if (keyframes.isEmpty()) return null;
            Map.Entry<Integer, PoseOverride> entry = keyframes.floorEntry(tick);
            return entry != null ? entry.getValue() : keyframes.firstEntry().getValue();
        }
    }

    public static boolean hasOverride(UUID uuid) {
        ensureLoaded();
        return uuid != null && (OVERRIDES.containsKey(uuid) || TIMELINE_TRACKS.containsKey(uuid));
    }

    public static PoseOverride getOverride(UUID uuid) {
        ensureLoaded();
        return uuid != null ? OVERRIDES.get(uuid) : null;
    }

    /**
     * Timeline-aware pose lookup: returns active keyframe at the given tick if present,
     * otherwise falls back to static override.
     */
    public static PoseOverride getOverride(UUID uuid, int tick) {
        ensureLoaded();
        if (uuid == null) return null;

        TimelineTrack track = TIMELINE_TRACKS.get(uuid);
        if (track != null && !track.keyframes.isEmpty()) {
            PoseOverride keyframed = track.getAtTick(tick);
            if (keyframed != null) {
                return keyframed;
            }
        }
        return OVERRIDES.get(uuid);
    }

    public static void setOverride(UUID uuid, PoseOverride override) {
        if (uuid == null || override == null) return;
        ensureLoaded();
        OVERRIDES.put(uuid, override);
        save();
    }

    public static void setOverride(UUID uuid, int preset, String emoteId, byte playMode) {
        if (uuid == null) return;
        ensureLoaded();
        OVERRIDES.put(uuid, new PoseOverride(preset, emoteId, playMode, 0.0f));
        save();
    }

    public static void removeOverride(UUID uuid) {
        if (uuid == null) return;
        ensureLoaded();
        OVERRIDES.remove(uuid);
        TIMELINE_TRACKS.remove(uuid);
        save();
    }

    public static void setKeyframe(UUID uuid, int tick, PoseOverride override) {
        if (uuid == null || override == null) return;
        ensureLoaded();
        TIMELINE_TRACKS.computeIfAbsent(uuid, k -> new TimelineTrack()).keyframes.put(tick, override);
        save();
    }

    public static void removeKeyframe(UUID uuid, int tick) {
        if (uuid == null) return;
        ensureLoaded();
        TimelineTrack track = TIMELINE_TRACKS.get(uuid);
        if (track != null) {
            track.keyframes.remove(tick);
            if (track.keyframes.isEmpty()) {
                TIMELINE_TRACKS.remove(uuid);
            }
            save();
        }
    }

    public static TimelineTrack getTimelineTrack(UUID uuid) {
        ensureLoaded();
        return uuid != null ? TIMELINE_TRACKS.get(uuid) : null;
    }

    public static void recordPlayerKeyframe(UUID uuid, String name, int tick, int duration, String emoteId, byte playMode) {
        if (uuid == null) return;
        ensureLoaded();
        PlayerTimelineTrack track = PLAYER_TRACKS.computeIfAbsent(uuid, k -> new PlayerTimelineTrack(name));
        if (name != null && !name.isBlank()) {
            track.playerName = name;
        }
        track.keyframes.put(tick, new PlayerEmoteKeyframe(tick, duration, emoteId, playMode));
        save();
    }

    public static void updatePlayerKeyframeDuration(UUID uuid, int tick, int newDuration) {
        if (uuid == null) return;
        ensureLoaded();
        PlayerTimelineTrack track = PLAYER_TRACKS.get(uuid);
        if (track != null) {
            PlayerEmoteKeyframe kf = track.keyframes.get(tick);
            if (kf != null) {
                kf.durationTicks = newDuration;
                save();
            }
        }
    }

    public static void setPlayerKeyframe(UUID uuid, String name, int tick, int duration, String emoteId, byte playMode) {
        recordPlayerKeyframe(uuid, name, tick, duration, emoteId, playMode);
    }

    public static void removePlayerKeyframe(UUID uuid, int tick) {
        if (uuid == null) return;
        ensureLoaded();
        PlayerTimelineTrack track = PLAYER_TRACKS.get(uuid);
        if (track != null) {
            track.keyframes.remove(tick);
            if (track.keyframes.isEmpty()) {
                PLAYER_TRACKS.remove(uuid);
            }
            save();
        }
    }

    public static PlayerEmoteKeyframe getPlayerKeyframe(UUID uuid, int tick) {
        ensureLoaded();
        if (uuid == null) return null;
        PlayerTimelineTrack track = PLAYER_TRACKS.get(uuid);
        if (track != null) {
            return track.getActiveKeyframeAt(tick);
        }
        return null;
    }

    public static PlayerTimelineTrack getPlayerTrack(UUID uuid) {
        ensureLoaded();
        return uuid != null ? PLAYER_TRACKS.get(uuid) : null;
    }

    public static Map<UUID, PlayerTimelineTrack> getAllPlayerTracks() {
        ensureLoaded();
        return PLAYER_TRACKS;
    }

    public static boolean hasPlayerTrack(UUID uuid) {
        ensureLoaded();
        return uuid != null && PLAYER_TRACKS.containsKey(uuid);
    }

    public static void clearAll() {
        OVERRIDES.clear();
        TIMELINE_TRACKS.clear();
        PLAYER_TRACKS.clear();
        save();
    }

    private static File getStorageFile() {
        File dir = new File(Minecraft.getInstance().gameDirectory, "sakura_replay_overrides");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "active_replay_overrides.json");
    }

    private static File getSessionStorageFile(UUID sessionId) {
        File dir = new File(Minecraft.getInstance().gameDirectory, "takasha_cache/replay_players");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, sessionId + ".json");
    }

    public static synchronized void save() {
        try {
            Map<String, Object> root = new HashMap<>();

            Map<String, PoseOverride> staticMap = new HashMap<>();
            for (Map.Entry<UUID, PoseOverride> entry : OVERRIDES.entrySet()) {
                staticMap.put(entry.getKey().toString(), entry.getValue());
            }
            root.put("static_overrides", staticMap);

            Map<String, Map<String, PoseOverride>> trackMap = new HashMap<>();
            for (Map.Entry<UUID, TimelineTrack> entry : TIMELINE_TRACKS.entrySet()) {
                Map<String, PoseOverride> tickMap = new HashMap<>();
                for (Map.Entry<Integer, PoseOverride> kf : entry.getValue().keyframes.entrySet()) {
                    tickMap.put(kf.getKey().toString(), kf.getValue());
                }
                trackMap.put(entry.getKey().toString(), tickMap);
            }
            root.put("timeline_tracks", trackMap);

            Map<String, Map<String, Object>> playerTrackMap = new HashMap<>();
            for (Map.Entry<UUID, PlayerTimelineTrack> entry : PLAYER_TRACKS.entrySet()) {
                Map<String, Object> trackData = new HashMap<>();
                trackData.put("name", entry.getValue().playerName);
                Map<String, PlayerEmoteKeyframe> tickMap = new HashMap<>();
                for (Map.Entry<Integer, PlayerEmoteKeyframe> kf : entry.getValue().keyframes.entrySet()) {
                    tickMap.put(kf.getKey().toString(), kf.getValue());
                }
                trackData.put("keyframes", tickMap);
                playerTrackMap.put(entry.getKey().toString(), trackData);
            }
            root.put("player_timeline_tracks", playerTrackMap);

            File file = getStorageFile();
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(root, writer);
            }

            UUID sessionId = loadedSessionId != null ? loadedSessionId : net.sakura.weapons.compat.flashback.FlashbackCompatHelper.getActiveReplayOrRecordId();
            if (sessionId != null) {
                loadedSessionId = sessionId;
                File sessionFile = getSessionStorageFile(sessionId);
                try (FileWriter writer = new FileWriter(sessionFile)) {
                    GSON.toJson(root, writer);
                }
            }
        } catch (Throwable t) {
            SakuraWeaponsMod.LOGGER.warn("Failed to persist ReplayMod pose/player overrides: {}", t.getMessage());
        }
    }

    public static synchronized void loadSession(UUID sessionId) {
        if (sessionId != null && sessionId.equals(loadedSessionId) && loaded) {
            return;
        }
        loadedSessionId = sessionId;
        loaded = true;
        loadForSession(sessionId);
    }

    public static synchronized void load() {
        UUID currentSessionId = net.sakura.weapons.compat.flashback.FlashbackCompatHelper.getActiveReplayOrRecordId();
        loadSession(currentSessionId);
    }

    private static void loadForSession(UUID sessionId) {
        OVERRIDES.clear();
        TIMELINE_TRACKS.clear();
        PLAYER_TRACKS.clear();

        try {
            File sessionFile = sessionId != null ? getSessionStorageFile(sessionId) : null;
            File file = (sessionFile != null && sessionFile.exists()) ? sessionFile : getStorageFile();
            if (!file.exists()) return;

            try (FileReader reader = new FileReader(file)) {
                JsonElement rootElement = JsonParser.parseReader(reader);
                if (rootElement.isJsonObject()) {
                    JsonObject root = rootElement.getAsJsonObject();

                    if (root.has("static_overrides") || root.has("timeline_tracks") || root.has("player_timeline_tracks")) {
                        if (root.has("static_overrides")) {
                            Type mapType = new TypeToken<Map<String, PoseOverride>>() {}.getType();
                            Map<String, PoseOverride> staticMap = GSON.fromJson(root.get("static_overrides"), mapType);
                            if (staticMap != null) {
                                for (Map.Entry<String, PoseOverride> entry : staticMap.entrySet()) {
                                    try {
                                        OVERRIDES.put(UUID.fromString(entry.getKey()), entry.getValue());
                                    } catch (Exception ignored) {}
                                }
                            }
                        }

                        if (root.has("timeline_tracks")) {
                            Type trackType = new TypeToken<Map<String, Map<String, PoseOverride>>>() {}.getType();
                            Map<String, Map<String, PoseOverride>> tracks = GSON.fromJson(root.get("timeline_tracks"), trackType);
                            if (tracks != null) {
                                for (Map.Entry<String, Map<String, PoseOverride>> entry : tracks.entrySet()) {
                                    try {
                                        UUID uuid = UUID.fromString(entry.getKey());
                                        TimelineTrack track = new TimelineTrack();
                                        for (Map.Entry<String, PoseOverride> tickEntry : entry.getValue().entrySet()) {
                                            try {
                                                track.keyframes.put(Integer.parseInt(tickEntry.getKey()), tickEntry.getValue());
                                            } catch (Exception ignored) {}
                                        }
                                        if (!track.keyframes.isEmpty()) {
                                            TIMELINE_TRACKS.put(uuid, track);
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }
                        }

                        if (root.has("player_timeline_tracks")) {
                            JsonObject pTracks = root.getAsJsonObject("player_timeline_tracks");
                            for (String uuidStr : pTracks.keySet()) {
                                try {
                                    UUID uuid = UUID.fromString(uuidStr);
                                    JsonObject pData = pTracks.getAsJsonObject(uuidStr);
                                    String pName = pData.has("name") ? pData.get("name").getAsString() : "";
                                    PlayerTimelineTrack track = new PlayerTimelineTrack(pName);
                                    if (pData.has("keyframes")) {
                                        JsonObject kfs = pData.getAsJsonObject("keyframes");
                                        for (String tickStr : kfs.keySet()) {
                                            try {
                                                int tick = Integer.parseInt(tickStr);
                                                PlayerEmoteKeyframe kf = GSON.fromJson(kfs.get(tickStr), PlayerEmoteKeyframe.class);
                                                if (kf != null) {
                                                    track.keyframes.put(tick, kf);
                                                }
                                            } catch (Exception ignored) {}
                                        }
                                    }
                                    if (!track.keyframes.isEmpty()) {
                                        PLAYER_TRACKS.put(uuid, track);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    } else {
                        // Legacy single-map format fallback
                        Type type = new TypeToken<Map<String, PoseOverride>>() {}.getType();
                        Map<String, PoseOverride> stringMap = GSON.fromJson(root, type);
                        if (stringMap != null) {
                            for (Map.Entry<String, PoseOverride> entry : stringMap.entrySet()) {
                                try {
                                    OVERRIDES.put(UUID.fromString(entry.getKey()), entry.getValue());
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            SakuraWeaponsMod.LOGGER.warn("Failed to load ReplayMod pose/player overrides: {}", t.getMessage());
        }
    }
}
