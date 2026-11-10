package net.takasha.rpg.mythic;

import java.util.ArrayList;
import java.util.List;

/**
 * Definition of a MythicMob VFX or custom mob entity loaded dynamically from Mobs/*.yml.
 */
public class MythicMobDef {

    private final String id;
    private String modelId;
    private String mobType = "item_display";
    private String defaultState = "animation";
    private float animationSpeed = 1.0f;
    private int lifetimeTicks = 40;
    private boolean lockPitch = false;
    private final List<String> spawnSkills = new ArrayList<>();
    private final List<String> textFrames = new ArrayList<>();

    public MythicMobDef(String id) {
        this.id = id;
    }

    public String getMobType() {
        return mobType;
    }

    public void setMobType(String mobType) {
        this.mobType = mobType != null ? mobType : "item_display";
    }

    public List<String> getTextFrames() {
        return textFrames;
    }

    public void addTextFrame(String frame) {
        this.textFrames.add(frame);
    }

    public boolean isLockPitch() {
        return lockPitch;
    }

    public void setLockPitch(boolean lockPitch) {
        this.lockPitch = lockPitch;
    }

    public String getId() {
        return id;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getDefaultState() {
        return defaultState;
    }

    public void setDefaultState(String defaultState) {
        this.defaultState = defaultState;
    }

    public float getAnimationSpeed() {
        return animationSpeed;
    }

    public void setAnimationSpeed(float animationSpeed) {
        this.animationSpeed = animationSpeed;
    }

    public int getLifetimeTicks() {
        return lifetimeTicks;
    }

    public void setLifetimeTicks(int lifetimeTicks) {
        this.lifetimeTicks = lifetimeTicks;
    }

    public List<String> getSpawnSkills() {
        return spawnSkills;
    }
}
