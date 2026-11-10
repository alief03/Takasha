package net.takasha.rpg.modelengine;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;

public class ActiveModelInstance {

    private final UUID id = UUID.randomUUID();
    private BlockbenchModel model;
    private final ServerLevel level;
    private Entity anchorEntity;
    private Vec3 staticPos;
    private Vec3 offset = Vec3.ZERO;
    private float yaw = 0.0f;
    private float pitch = 0.0f;
    private boolean lockPitch = false;

    private final Map<String, ModelBoneInstance> boneInstances = new LinkedHashMap<>();
    private final List<ModelBoneInstance> uniqueBones = new ArrayList<>();
    private String activeAnimationName = "";
    private double animTime = 0.0;
    private double animSpeed = 1.0;
    private boolean loop = true;
    private int lifetimeTicks = -1; // -1 = forever until anchor dies or removed
    private boolean removed = false;

    // Flipbook sequencer state
    private String sequencePrefix = null;
    private int sequenceCurrentFrame = 1;
    private int sequenceMaxFrame = 1;
    private int sequenceInterval = 1;
    private int sequenceDelay = 0;
    private int sequenceLinger = 0;
    private int sequenceTickCounter = 0;

    public ActiveModelInstance(BlockbenchModel model, ServerLevel level, Entity anchorEntity, Vec3 offset) {
        this.model = model;
        this.level = level;
        this.anchorEntity = anchorEntity;
        this.offset = offset != null ? offset : Vec3.ZERO;
        if (anchorEntity != null) {
            this.yaw = anchorEntity.getYRot();
            this.pitch = anchorEntity.getXRot();
        }
        initializeBones();
    }

    public ActiveModelInstance(BlockbenchModel model, ServerLevel level, Vec3 staticPos) {
        this.model = model;
        this.level = level;
        this.staticPos = staticPos;
        this.lifetimeTicks = 200; // Safe default 10s lifetime cap for static VFX models to prevent orphan leak
        initializeBones();
    }

    public ActiveModelInstance(BlockbenchModel model, ServerLevel level, Vec3 staticPos, float yaw, float pitch) {
        this.model = model;
        this.level = level;
        this.staticPos = staticPos;
        this.yaw = yaw;
        this.pitch = pitch;
        this.lifetimeTicks = 200; // Safe default 10s lifetime cap for static VFX models to prevent orphan leak
        initializeBones();
    }

    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void setLockPitch(boolean lockPitch) {
        this.lockPitch = lockPitch;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean isLockPitch() {
        return lockPitch;
    }

    public void setStaticPos(Vec3 pos) {
        this.staticPos = pos;
    }

    public void setOffset(Vec3 offset) {
        this.offset = offset != null ? offset : Vec3.ZERO;
    }

    private void initializeBones() {
        Vec3 spawnPos = getCurrentBasePos();
        Set<BlockbenchModel.BoneDef> distinctBones = new LinkedHashSet<>(model.getBones().values());

        for (BlockbenchModel.BoneDef def : distinctBones) {
            ModelBoneInstance instance = new ModelBoneInstance(def);

            float bScale = TakashaModelEngine.getBoneScale(model.getModelId(), def.getName());
            instance.setBoneScale(bScale);

            // Strip non-visual bones and bones without mesh cubes
            if (!TakashaModelEngine.isNonVisualBone(def.getName()) && !def.getCubes().isEmpty()) {
                ItemStack boneItem = TakashaModelEngine.createBoneItemStack(model.getModelId(), def.getName(),
                        def.getUuid());
                if (!boneItem.isEmpty()) {
                    Display.ItemDisplay display = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, level);
                    display.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                    display.getSlot(0).set(boneItem);
                    // brightness{b=15;s=15} for full luminous VFX rendering
                    ((net.takasha.rpg.mixin.DisplayAccessor) display).callSetBrightnessOverride(new net.minecraft.util.Brightness(15, 15));
                    display.addTag("takasha_temp_vfx");
                    TakashaModelEngine.registerActiveBoneDisplay(display);
                    if (level.getServer() != null) {
                        level.getServer().execute(() -> {
                            if (!display.isRemoved()) {
                                level.addFreshEntity(display);
                            }
                        });
                    } else {
                        level.addFreshEntity(display);
                    }
                    instance.setDisplayEntity(display);
                }
            }

            boneInstances.put(def.getName(), instance);
            if (def.getUuid() != null && !def.getUuid().isBlank()) {
                boneInstances.put(def.getUuid(), instance);
            }
            uniqueBones.add(instance);
        }
    }

    public Vec3 getCurrentBasePos() {
        if (anchorEntity != null && anchorEntity.isAlive()) {
            return anchorEntity.position().add(offset);
        }
        return staticPos != null ? staticPos : Vec3.ZERO;
    }

    public void playAnimation(String animName, double speed, boolean loop) {
        if (animName == null || animName.isBlank()) return;
        String resolved = resolveAnimationName(animName);
        if (resolved != null) {
            this.activeAnimationName = resolved;
            this.animSpeed = speed > 0 ? speed : 1.0;
            this.loop = loop;
            this.animTime = 0.0;
        }
    }

    private String resolveAnimationName(String name) {
        if (model == null || model.getAnimations() == null || model.getAnimations().isEmpty()) {
            return null;
        }
        if (model.getAnimations().containsKey(name)) {
            return name;
        }
        // 1. Case-insensitive exact match
        for (String k : model.getAnimations().keySet()) {
            if (k.equalsIgnoreCase(name)) return k;
        }
        // 2. Normal / alias match (e.g. slash1 -> slash_normal1, pierce -> pierce_normal)
        String lower = name.toLowerCase(Locale.ROOT);
        for (String k : model.getAnimations().keySet()) {
            String kl = k.toLowerCase(Locale.ROOT);
            if (kl.equals(lower + "_normal") || kl.equals(lower + "_normal1") || kl.replace("_normal", "").equals(lower)) {
                return k;
            }
        }
        // 3. Fallback prefix matching
        for (String k : model.getAnimations().keySet()) {
            if (k.toLowerCase(Locale.ROOT).startsWith(lower)) {
                return k;
            }
        }
        return null;
    }

    public void setupSequence(String basePrefix, int startFrame, int maxFrames, int intervalTicks, int delayTicks, int lingerTicks) {
        this.sequencePrefix = basePrefix;
        this.sequenceCurrentFrame = startFrame;
        this.sequenceMaxFrame = maxFrames;
        this.sequenceInterval = Math.max(1, intervalTicks);
        this.sequenceDelay = Math.max(0, delayTicks);
        this.sequenceLinger = Math.max(0, lingerTicks);
        this.sequenceTickCounter = 0;
    }

    public void remapModel(String newModelId) {
        BlockbenchModel newModel = TakashaModelEngine.getModel(newModelId);
        if (newModel == null) return;
        this.model = newModel;

        for (ModelBoneInstance bone : uniqueBones) {
            bone.remove();
        }
        boneInstances.clear();
        uniqueBones.clear();

        initializeBones();
        if (activeAnimationName != null && !activeAnimationName.isEmpty()) {
            playAnimation(activeAnimationName, animSpeed, loop);
        }
    }

    public void tick() {
        if (removed)
            return;

        if (anchorEntity != null && !anchorEntity.isAlive()) {
            remove();
            return;
        }

        if (lifetimeTicks > 0) {
            lifetimeTicks--;
            if (lifetimeTicks <= 0) {
                remove();
                return;
            }
        }

        // Sequence flipbook tick
        if (sequencePrefix != null) {
            if (sequenceDelay > 0) {
                sequenceDelay--;
            } else if (sequenceCurrentFrame < sequenceMaxFrame) {
                sequenceTickCounter++;
                if (sequenceTickCounter >= sequenceInterval) {
                    sequenceTickCounter = 0;
                    sequenceCurrentFrame++;
                    remapModel(sequencePrefix + "_" + sequenceCurrentFrame);
                }
            } else {
                if (sequenceLinger > 0) {
                    sequenceLinger--;
                } else {
                    remove();
                    return;
                }
            }
        }

        Vec3 basePos = getCurrentBasePos();

        // Update positions of display entities
        for (ModelBoneInstance bone : uniqueBones) {
            if (bone.getDisplayEntity() != null && bone.getDisplayEntity().isAlive()) {
                bone.getDisplayEntity().setPos(basePos.x, basePos.y, basePos.z);
            }
        }

        // Reset all bones to default transforms each tick before animation sampling
        for (ModelBoneInstance bone : uniqueBones) {
            bone.resetTransforms();
        }

        // Special handling for heat_stack_vfx when at 0 stacks: hide bright indicator bones by default
        if (model.getModelId().toLowerCase(Locale.ROOT).contains("heat_stack")) {
            if (activeAnimationName == null || activeAnimationName.isEmpty()) {
                for (ModelBoneInstance bone : uniqueBones) {
                    String bName = bone.getDefinition().getName().toLowerCase(Locale.ROOT);
                    if (bName.contains("on")) {
                        bone.setAnimatedTransform(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0, 0, 0));
                    }
                }
            }
        }

        // Animate
        BlockbenchModel.AnimationDef anim = model.getAnimations().get(activeAnimationName);
        if (anim != null) {
            animTime += 0.05 * animSpeed;
            if (animTime > anim.getLength()) {
                if (loop || anim.isLoop()) {
                    animTime = anim.getLength() > 0 ? animTime % anim.getLength() : 0.0;
                } else {
                    animTime = anim.getLength();
                }
            }

            // Interpolate keyframes for each bone
            for (Map.Entry<String, BlockbenchModel.BoneTimeline> entry : anim.getTimelines().entrySet()) {
                String boneIdentifier = entry.getKey();
                ModelBoneInstance boneInst = boneInstances.get(boneIdentifier);
                if (boneInst != null) {
                    BlockbenchModel.BoneTimeline tl = entry.getValue();
                    Vector3f pos = sampleTimeline(tl.getPositionKeys(), animTime, new Vector3f(0, 0, 0));
                    Vector3f rot = sampleTimeline(tl.getRotationKeys(), animTime, new Vector3f(0, 0, 0));
                    Vector3f scale = sampleTimeline(tl.getScaleKeys(), animTime, new Vector3f(1, 1, 1));
                    boneInst.setAnimatedTransform(pos, rot, scale);
                }
            }
        }

        if (anchorEntity != null && anchorEntity.isAlive()) {
            this.yaw = anchorEntity.getYRot();
            if (!lockPitch) {
                this.pitch = anchorEntity.getXRot();
            }
        }

        // Compute root transformation matrix with 3D Yaw and Pitch
        org.joml.Matrix4f rootMatrix = new org.joml.Matrix4f();
        rootMatrix.rotateY((float) Math.toRadians(180.0f - yaw));
        if (!lockPitch && Math.abs(pitch) > 0.01f) {
            rootMatrix.rotateX((float) Math.toRadians(-pitch));
        }

        // Compute hierarchical transformation matrices for all top-level root bones
        List<BlockbenchModel.BoneDef> roots = model.getRootBones();
        if (!roots.isEmpty()) {
            for (BlockbenchModel.BoneDef root : roots) {
                updateBoneTree(root, rootMatrix);
            }
        } else {
            for (ModelBoneInstance inst : uniqueBones) {
                inst.updateGlobalMatrix(rootMatrix);
            }
        }
    }

    private void updateBoneTree(BlockbenchModel.BoneDef def, org.joml.Matrix4f parentMatrix) {
        ModelBoneInstance inst = boneInstances.get(def.getName());
        if (inst == null && def.getUuid() != null) {
            inst = boneInstances.get(def.getUuid());
        }
        if (inst != null) {
            inst.updateGlobalMatrix(parentMatrix);
            for (BlockbenchModel.BoneDef child : def.getChildren()) {
                updateBoneTree(child, inst.getGlobalMatrix());
            }
        }
    }

    private Vector3f sampleTimeline(List<BlockbenchModel.Keyframe> keys, double time, Vector3f defaultValue) {
        if (keys.isEmpty())
            return defaultValue;
        if (keys.size() == 1)
            return keys.getFirst().getValue();

        // Find surrounding keyframes
        BlockbenchModel.Keyframe prev = keys.getFirst();
        BlockbenchModel.Keyframe next = keys.getLast();

        for (int i = 0; i < keys.size(); i++) {
            BlockbenchModel.Keyframe k = keys.get(i);
            if (k.getTime() <= time) {
                prev = k;
            }
            if (k.getTime() >= time) {
                next = k;
                break;
            }
        }

        if (prev == next || next.getTime() == prev.getTime()) {
            return prev.getValue();
        }

        float factor = (float) ((time - prev.getTime()) / (next.getTime() - prev.getTime()));
        Vector3f v1 = prev.getValue();
        Vector3f v2 = next.getValue();
        return new Vector3f(
                v1.x + (v2.x - v1.x) * factor,
                v1.y + (v2.y - v1.y) * factor,
                v1.z + (v2.z - v1.z) * factor);
    }

    public void setLifetime(int ticks) {
        this.lifetimeTicks = ticks;
    }

    public void remove() {
        if (removed)
            return;
        removed = true;
        for (ModelBoneInstance bone : uniqueBones) {
            bone.remove();
        }
        boneInstances.clear();
        uniqueBones.clear();
    }

    public boolean isRemoved() {
        return removed;
    }

    public UUID getId() {
        return id;
    }

    public BlockbenchModel getModel() {
        return model;
    }
}
