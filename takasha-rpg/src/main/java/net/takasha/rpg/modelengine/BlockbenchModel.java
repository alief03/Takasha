package net.takasha.rpg.modelengine;

import org.joml.Vector3f;

import java.util.*;

public class BlockbenchModel {

    public static class CubeDef {
        private final Vector3f from;
        private final Vector3f to;
        private final Vector3f origin;

        public CubeDef(Vector3f from, Vector3f to, Vector3f origin) {
            this.from = from;
            this.to = to;
            this.origin = origin;
        }

        public Vector3f getFrom() { return from; }
        public Vector3f getTo() { return to; }
        public Vector3f getOrigin() { return origin; }
    }

    public static class BoneDef {
        private final String uuid;
        private final String name;
        private final Vector3f origin;
        private final Vector3f baseRotation;
        private BoneDef parent;
        private final List<BoneDef> children = new ArrayList<>();
        private final List<CubeDef> cubes = new ArrayList<>();

        public BoneDef(String uuid, String name, Vector3f origin, Vector3f baseRotation) {
            this.uuid = uuid;
            this.name = name;
            this.origin = origin;
            this.baseRotation = baseRotation;
        }

        public String getUuid() { return uuid; }
        public String getName() { return name; }
        public Vector3f getOrigin() { return origin; }
        public Vector3f getBaseRotation() { return baseRotation; }
        public BoneDef getParent() { return parent; }
        public void setParent(BoneDef parent) { this.parent = parent; }
        public List<BoneDef> getChildren() { return children; }
        public List<CubeDef> getCubes() { return cubes; }
    }

    public static class Keyframe {
        private final double time;
        private final String channel; // "position", "rotation", "scale"
        private final Vector3f value;
        private final String interpolation;

        public Keyframe(double time, String channel, Vector3f value, String interpolation) {
            this.time = time;
            this.channel = channel;
            this.value = value;
            this.interpolation = interpolation;
        }

        public double getTime() { return time; }
        public String getChannel() { return channel; }
        public Vector3f getValue() { return value; }
        public String getInterpolation() { return interpolation; }
    }

    public static class BoneTimeline {
        private final List<Keyframe> positionKeys = new ArrayList<>();
        private final List<Keyframe> rotationKeys = new ArrayList<>();
        private final List<Keyframe> scaleKeys = new ArrayList<>();

        public List<Keyframe> getPositionKeys() { return positionKeys; }
        public List<Keyframe> getRotationKeys() { return rotationKeys; }
        public List<Keyframe> getScaleKeys() { return scaleKeys; }
    }

    public static class AnimationDef {
        private final String name;
        private final boolean loop;
        private final double length;
        private final Map<String, BoneTimeline> timelines = new HashMap<>();

        public AnimationDef(String name, boolean loop, double length) {
            this.name = name;
            this.loop = loop;
            this.length = length;
        }

        public String getName() { return name; }
        public boolean isLoop() { return loop; }
        public double getLength() { return length; }
        public Map<String, BoneTimeline> getTimelines() { return timelines; }
    }

    private final String modelId;
    private final Map<String, BoneDef> bones = new LinkedHashMap<>();
    private final Map<String, AnimationDef> animations = new HashMap<>();
    private final List<BoneDef> rootBones = new ArrayList<>();

    public BlockbenchModel(String modelId) {
        this.modelId = modelId;
    }

    public String getModelId() { return modelId; }
    public Map<String, BoneDef> getBones() { return bones; }
    public Map<String, AnimationDef> getAnimations() { return animations; }
    public BoneDef getRootBone() { return rootBones.isEmpty() ? null : rootBones.get(0); }
    public void setRootBone(BoneDef rootBone) {
        this.rootBones.clear();
        if (rootBone != null) {
            this.rootBones.add(rootBone);
        }
    }
    public List<BoneDef> getRootBones() { return rootBones; }
    public void addRootBone(BoneDef rootBone) {
        if (rootBone != null && !rootBones.contains(rootBone)) {
            rootBones.add(rootBone);
        }
    }
}
