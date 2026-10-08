package net.sakura.weapons.client.replay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.Map;
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
    private static boolean loaded = false;

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

    public static boolean hasOverride(UUID uuid) {
        if (!loaded) load();
        return uuid != null && OVERRIDES.containsKey(uuid);
    }

    public static PoseOverride getOverride(UUID uuid) {
        if (!loaded) load();
        return uuid != null ? OVERRIDES.get(uuid) : null;
    }

    public static void setOverride(UUID uuid, PoseOverride override) {
        if (uuid == null || override == null) return;
        OVERRIDES.put(uuid, override);
        save();
    }

    public static void setOverride(UUID uuid, int preset, String emoteId, byte playMode) {
        if (uuid == null) return;
        OVERRIDES.put(uuid, new PoseOverride(preset, emoteId, playMode, 0.0f));
        save();
    }

    public static void removeOverride(UUID uuid) {
        if (uuid == null) return;
        OVERRIDES.remove(uuid);
        save();
    }

    public static void clearAll() {
        OVERRIDES.clear();
        save();
    }

    private static File getStorageFile() {
        File dir = new File(Minecraft.getInstance().gameDirectory, "sakura_replay_overrides");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "active_replay_overrides.json");
    }

    public static synchronized void save() {
        try {
            File file = getStorageFile();
            Map<String, PoseOverride> stringMap = new java.util.HashMap<>();
            for (Map.Entry<UUID, PoseOverride> entry : OVERRIDES.entrySet()) {
                stringMap.put(entry.getKey().toString(), entry.getValue());
            }
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(stringMap, writer);
            }
        } catch (Throwable t) {
            SakuraWeaponsMod.LOGGER.warn("Failed to persist ReplayMod pose overrides: {}", t.getMessage());
        }
    }

    public static synchronized void load() {
        loaded = true;
        try {
            File file = getStorageFile();
            if (!file.exists()) return;
            Type type = new TypeToken<Map<String, PoseOverride>>() {}.getType();
            try (FileReader reader = new FileReader(file)) {
                Map<String, PoseOverride> stringMap = GSON.fromJson(reader, type);
                if (stringMap != null) {
                    OVERRIDES.clear();
                    for (Map.Entry<String, PoseOverride> entry : stringMap.entrySet()) {
                        try {
                            OVERRIDES.put(UUID.fromString(entry.getKey()), entry.getValue());
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Throwable t) {
            SakuraWeaponsMod.LOGGER.warn("Failed to load ReplayMod pose overrides: {}", t.getMessage());
        }
    }
}
