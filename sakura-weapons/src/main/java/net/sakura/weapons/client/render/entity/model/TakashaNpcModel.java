package net.sakura.weapons.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.sakura.weapons.client.render.entity.state.TakashaNpcRenderState;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Environment(EnvType.CLIENT)
public class TakashaNpcModel extends PlayerModel {

    public TakashaNpcModel(ModelPart root, boolean slim) {
        super(root, slim);
        disableSkinLayersCompat();
    }

    public void disableSkinLayersCompat() {
        // 1. Invoke setIgnored(true) on this model instance across its class hierarchy and interfaces
        try {
            for (Method m : this.getClass().getMethods()) {
                if (m.getName().equalsIgnoreCase("setIgnored") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                    m.setAccessible(true);
                    m.invoke(this, true);
                }
            }
            for (Class<?> clazz = this.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().equalsIgnoreCase("setIgnored") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                        m.setAccessible(true);
                        m.invoke(this, true);
                    }
                }
                for (Class<?> iface : clazz.getInterfaces()) {
                    for (Method m : iface.getDeclaredMethods()) {
                        if (m.getName().equalsIgnoreCase("setIgnored") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                            m.setAccessible(true);
                            m.invoke(this, true);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 2. Set any boolean field named "ignored" to true across class hierarchy
        try {
            for (Class<?> clazz = this.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
                for (Field f : clazz.getDeclaredFields()) {
                    if (f.getType() == boolean.class && f.getName().toLowerCase().contains("ignored")) {
                        f.setAccessible(true);
                        f.setBoolean(this, true);
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 3. Clear any third-party injected 3D voxel meshes
        clearInjectedMeshes();
    }

    public void clearInjectedMeshes() {
        clearPartMesh(this.hat);
        clearPartMesh(this.jacket);
        clearPartMesh(this.leftSleeve);
        clearPartMesh(this.rightSleeve);
        clearPartMesh(this.leftPants);
        clearPartMesh(this.rightPants);
        clearPartMesh(this.head);
        clearPartMesh(this.body);
        clearPartMesh(this.leftArm);
        clearPartMesh(this.rightArm);
        clearPartMesh(this.leftLeg);
        clearPartMesh(this.rightLeg);
    }

    public static void clearPartMesh(ModelPart part) {
        if (part == null) return;
        try {
            for (Method m : part.getClass().getMethods()) {
                if (m.getName().equals("setInjectedMesh") && m.getParameterCount() == 2) {
                    m.invoke(part, (Object) null, (Object) null);
                }
            }
        } catch (Throwable ignored) {}
        try {
            for (Class<?> c = part.getClass(); c != null; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (f.getName().toLowerCase().contains("injectedmesh")) {
                        f.setAccessible(true);
                        f.set(part, null);
                    }
                }
            }
        } catch (Throwable ignored) {}
        try {
            for (Field f : part.getClass().getDeclaredFields()) {
                if (java.util.Map.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object val = f.get(part);
                    if (val instanceof java.util.Map<?, ?> map) {
                        for (Object child : map.values()) {
                            if (child instanceof ModelPart childPart) {
                                clearPartMesh(childPart);
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void copyModelPartPose(ModelPart source, ModelPart target) {
        if (source == null || target == null) return;
        target.x = source.x;
        target.y = source.y;
        target.z = source.z;
        target.xRot = source.xRot;
        target.yRot = source.yRot;
        target.zRot = source.zRot;
        target.xScale = source.xScale;
        target.yScale = source.yScale;
        target.zScale = source.zScale;
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        disableSkinLayersCompat();
        try {
            super.setupAnim(state);
        } catch (Throwable ignored) {
            // Silently absorb third-party player model mixin errors (e.g., skinlayers3d casting to ClientAvatarEntity)
        }

        // Wipe any injected meshes that third-party mixins might have attached during super.setupAnim
        clearInjectedMeshes();

        if (state instanceof TakashaNpcRenderState npcState) {
            // Ensure vanilla outer skin layer visibility is respected
            this.hat.visible = state.showHat;
            this.jacket.visible = state.showJacket;
            this.leftSleeve.visible = state.showLeftSleeve;
            this.rightSleeve.visible = state.showRightSleeve;
            this.leftPants.visible = state.showLeftPants;
            this.rightPants.visible = state.showRightPants;

            float degToRad = (float) (Math.PI / 180.0);
            int preset = npcState.posePreset;

            boolean isSitting = (preset == 8 || (npcState.emoteId != null && !npcState.emoteId.isBlank() && (npcState.emoteId.toLowerCase(java.util.Locale.ROOT).contains("sit") || npcState.emoteId.toLowerCase(java.util.Locale.ROOT).contains("duduk"))));
            boolean isFloorSitting = (preset == 9 || preset == 10 || preset == 11 || preset == 14 || (npcState.emoteId != null && !npcState.emoteId.isBlank() && (npcState.emoteId.toLowerCase(java.util.Locale.ROOT).contains("zen") || npcState.emoteId.toLowerCase(java.util.Locale.ROOT).contains("seiza") || npcState.emoteId.toLowerCase(java.util.Locale.ROOT).contains("bersila"))));
            boolean isKneeling = (preset == 13 || (npcState.emoteId != null && !npcState.emoteId.isBlank() && (npcState.emoteId.toLowerCase(java.util.Locale.ROOT).contains("kneel") || npcState.emoteId.toLowerCase(java.util.Locale.ROOT).contains("lutut"))));
            boolean isCrouching = (preset == 12 || npcState.currentPose == net.minecraft.world.entity.Pose.CROUCHING);

            // Pivot adjustments for Chair Sitting (8), Floor Sitting (9, 10, 11, 14), Kneeling (13), and Crouching (12)
            if (isSitting) {
                // Chair / Edge sitting: lower torso by 8.5 pixels, translate thighs forward
                this.head.y = 8.5F;
                this.head.z = 0.0F;
                this.body.y = 8.5F;
                this.body.z = 0.0F;
                this.leftArm.y = 10.5F;
                this.leftArm.z = 0.0F;
                this.rightArm.y = 10.5F;
                this.rightArm.z = 0.0F;
                this.leftLeg.y = 17.5F;
                this.leftLeg.z = -3.5F;
                this.rightLeg.y = 17.5F;
                this.rightLeg.z = -3.5F;
            } else if (isFloorSitting) {
                // Floor sitting / Meditation / Seiza / Duduk Bersandar: lower torso down to floor
                this.head.y = 11.5F;
                this.head.z = 0.0F;
                this.body.y = 11.5F;
                this.body.z = 0.0F;
                this.leftArm.y = 13.5F;
                this.leftArm.z = 0.0F;
                this.rightArm.y = 13.5F;
                this.rightArm.z = 0.0F;
                this.leftLeg.y = 20.5F;
                this.leftLeg.z = -2.5F;
                this.rightLeg.y = 20.5F;
                this.rightLeg.z = -2.5F;
            } else if (isKneeling) {
                // Knight Kneeling: lower torso by 7.0 pixels so the knee rests on the floor
                this.head.y = 7.0F;
                this.head.z = 0.0F;
                this.body.y = 7.0F;
                this.body.z = 0.0F;
                this.leftArm.y = 9.0F;
                this.leftArm.z = 0.0F;
                this.rightArm.y = 9.0F;
                this.rightArm.z = 0.0F;
                this.leftLeg.y = 16.0F;
                this.leftLeg.z = -2.0F;
                this.rightLeg.y = 16.0F;
                this.rightLeg.z = -2.0F;
            } else if (isCrouching) {
                // Tactical Crouch / Sneaking
                this.head.y = 4.0F;
                this.head.z = 0.0F;
                this.body.y = 4.0F;
                this.body.z = 0.0F;
                this.leftArm.y = 6.0F;
                this.leftArm.z = 0.0F;
                this.rightArm.y = 6.0F;
                this.rightArm.z = 0.0F;
                this.leftLeg.y = 14.0F;
                this.leftLeg.z = 0.0F;
                this.rightLeg.y = 14.0F;
                this.rightLeg.z = 0.0F;
            } else {
                // Normal standing: reset default pivots
                this.head.y = 0.0F;
                this.head.z = 0.0F;
                this.body.y = 0.0F;
                this.body.z = 0.0F;
                this.leftArm.y = 2.0F;
                this.leftArm.z = 0.0F;
                this.rightArm.y = 2.0F;
                this.rightArm.z = 0.0F;
                this.leftLeg.y = 12.0F;
                this.leftLeg.z = 0.0F;
                this.rightLeg.y = 12.0F;
                this.rightLeg.z = 0.0F;
            }

            // UNCONDITIONAL BASELINE POSING FOR ALL 32 PRESETS (1..32) AND DEFAULT POSES:
            // SynchedEntityData Euler rotations from npcState are ALWAYS applied as the foundational anatomy!
            // Head
            this.head.xRot = npcState.headPose.x() * degToRad;
            this.head.yRot = npcState.headPose.y() * degToRad;
            this.head.zRot = npcState.headPose.z() * degToRad;

            // Body
            this.body.xRot = npcState.bodyPose.x() * degToRad;
            this.body.yRot = npcState.bodyPose.y() * degToRad;
            this.body.zRot = npcState.bodyPose.z() * degToRad;

            // Left Arm
            this.leftArm.xRot = npcState.leftArmPose.x() * degToRad;
            this.leftArm.yRot = npcState.leftArmPose.y() * degToRad;
            this.leftArm.zRot = npcState.leftArmPose.z() * degToRad;
            if (state.leftArmPose == net.minecraft.client.model.HumanoidModel.ArmPose.ITEM && preset == 0 && (npcState.emoteId == null || npcState.emoteId.isBlank())) {
                this.leftArm.xRot = this.leftArm.xRot * 0.5F - 0.31415927F;
            }

            // Right Arm
            this.rightArm.xRot = npcState.rightArmPose.x() * degToRad;
            this.rightArm.yRot = npcState.rightArmPose.y() * degToRad;
            this.rightArm.zRot = npcState.rightArmPose.z() * degToRad;
            if (state.rightArmPose == net.minecraft.client.model.HumanoidModel.ArmPose.ITEM && preset == 0 && (npcState.emoteId == null || npcState.emoteId.isBlank())) {
                this.rightArm.xRot = this.rightArm.xRot * 0.5F - 0.31415927F;
            }

            // Left Leg
            this.leftLeg.xRot = npcState.leftLegPose.x() * degToRad;
            this.leftLeg.yRot = npcState.leftLegPose.y() * degToRad;
            this.leftLeg.zRot = npcState.leftLegPose.z() * degToRad;

            // Right Leg
            this.rightLeg.xRot = npcState.rightLegPose.x() * degToRad;
            this.rightLeg.yRot = npcState.rightLegPose.y() * degToRad;
            this.rightLeg.zRot = npcState.rightLegPose.z() * degToRad;

            // Synchronize vanilla outer skin layers to follow inner limbs and torso
            copyModelPartPose(this.head, this.hat);
            copyModelPartPose(this.body, this.jacket);
            copyModelPartPose(this.leftArm, this.leftSleeve);
            copyModelPartPose(this.rightArm, this.rightSleeve);
            copyModelPartPose(this.leftLeg, this.leftPants);
            copyModelPartPose(this.rightLeg, this.rightPants);

            // Emote Animation overlay: applies when preset == 0 (no static pose preset)
            if (preset == 0 && npcState.emoteId != null && !npcState.emoteId.isBlank()) {
                net.minecraft.core.Rotations[] animRots = net.sakura.weapons.compat.EmotecraftCompat.sampleRotationsAtTick(npcState.emoteId, npcState.animTime);
                if (animRots != null && animRots.length >= 6) {
                    this.head.xRot = animRots[0].x() * degToRad;
                    this.head.yRot = animRots[0].y() * degToRad;
                    this.head.zRot = animRots[0].z() * degToRad;

                    this.body.xRot = animRots[1].x() * degToRad;
                    this.body.yRot = animRots[1].y() * degToRad;
                    this.body.zRot = animRots[1].z() * degToRad;

                    this.rightArm.xRot = animRots[2].x() * degToRad;
                    this.rightArm.yRot = animRots[2].y() * degToRad;
                    this.rightArm.zRot = animRots[2].z() * degToRad;

                    this.leftArm.xRot = animRots[3].x() * degToRad;
                    this.leftArm.yRot = animRots[3].y() * degToRad;
                    this.leftArm.zRot = animRots[3].z() * degToRad;

                    this.rightLeg.xRot = animRots[4].x() * degToRad;
                    this.rightLeg.yRot = animRots[4].y() * degToRad;
                    this.rightLeg.zRot = animRots[4].z() * degToRad;

                    this.leftLeg.xRot = animRots[5].x() * degToRad;
                    this.leftLeg.yRot = animRots[5].y() * degToRad;
                    this.leftLeg.zRot = animRots[5].z() * degToRad;
                } else {
                    applyProceduralEmote(npcState.emoteId, npcState.animTime);
                }
                copyModelPartPose(this.head, this.hat);
                copyModelPartPose(this.body, this.jacket);
                copyModelPartPose(this.leftArm, this.leftSleeve);
                copyModelPartPose(this.rightArm, this.rightSleeve);
                copyModelPartPose(this.leftLeg, this.leftPants);
                copyModelPartPose(this.rightLeg, this.rightPants);
            }

            // Apply Bendable Cuboids joint articulation (elbows, knees, and torso)
            applyBendableCuboidsPoses(preset, npcState.emoteId);
        }

        // Final cleanse of injected meshes to guarantee vanilla 2D skin layer rendering
        clearInjectedMeshes();
    }

    private void applyBendableCuboidsPoses(int preset, String emoteId) {
        // Disable Sodium's static quad caching so dynamically bent vertices render cleanly
        net.sakura.weapons.compat.BendableCuboidsCompat.disableSodiumCache(this.root());

        // Reset all limb bends to neutral first
        net.sakura.weapons.compat.BendableCuboidsCompat.resetBend(this.head);
        net.sakura.weapons.compat.BendableCuboidsCompat.resetBend(this.hat);
        net.sakura.weapons.compat.BendableCuboidsCompat.resetBend(this.body);
        net.sakura.weapons.compat.BendableCuboidsCompat.resetBend(this.jacket);
        net.sakura.weapons.compat.BendableCuboidsCompat.resetBend(this.leftArm);
        net.sakura.weapons.compat.BendableCuboidsCompat.resetBend(this.leftSleeve);
        net.sakura.weapons.compat.BendableCuboidsCompat.resetBend(this.rightArm);
        net.sakura.weapons.compat.BendableCuboidsCompat.resetBend(this.rightSleeve);
        net.sakura.weapons.compat.BendableCuboidsCompat.resetBend(this.leftLeg);
        net.sakura.weapons.compat.BendableCuboidsCompat.resetBend(this.leftPants);
        net.sakura.weapons.compat.BendableCuboidsCompat.resetBend(this.rightLeg);
        net.sakura.weapons.compat.BendableCuboidsCompat.resetBend(this.rightPants);

        if (!net.sakura.weapons.compat.BendableCuboidsCompat.isAvailable()) {
            return;
        }

        if (preset == 0 && emoteId != null && !emoteId.isBlank()) {
            String key = emoteId.toLowerCase(java.util.Locale.ROOT);
            if (key.contains("miring_kanan") || key.contains("side_right")) {
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.2f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.3f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 0.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 0.35f);
                return;
            } else if (key.contains("miring_kiri") || key.contains("side_left") || key.contains("miring")) {
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -1.2f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.3f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 0.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 0.35f);
                return;
            } else if (key.contains("sleep") || key.contains("tidur") || key.contains("rebahan") || key.contains("lay") || key.contains("rest") || key.contains("tengkurap") || key.contains("prone")) {
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 0.25f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 0.25f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.35f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.35f);
                return;
            } else if (key.contains("sit") || key.contains("duduk")) {
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 1.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 1.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.6f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.6f);
                return;
            } else if (key.contains("dab")) {
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -1.4f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.2f);
                return;
            } else if (key.contains("think") || key.contains("pikir") || key.contains("bingung")) {
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.35f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.5f);
                return;
            }
        }

        switch (preset) {
            case 1, 2, 5, 6, 7 -> { // 🛌 Sleeping poses (Relaxed joints)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 0.25f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 0.25f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.35f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.35f);
            }
            case 3 -> { // 🛌 Tidur Miring Kanan (Right Side Sleeping)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.2f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.3f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 0.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 0.35f);
            }
            case 4 -> { // 🛌 Tidur Miring Kiri (Left Side Sleeping)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -1.2f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.3f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 0.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 0.35f);
            }
            case 8 -> { // 🧘 Duduk Kursi (Chair Sitting)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 1.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 1.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.6f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.6f);
            }
            case 9 -> { // 🧘 Duduk Sandar (Reclined Floor Sitting)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 0.05f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 0.05f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.4f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.4f);
            }
            case 10 -> { // 🧘 Duduk Bersila (Cross-Legged Sitting)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 1.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 1.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.8f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.8f);
            }
            case 11 -> { // 🧘 Meditasi Zen (Zen Meditation)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 1.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 1.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -1.1f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.1f);
            }
            case 12 -> { // 🧘 Jongkok Siaga (Tactical Crouch)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 1.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 1.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendBody(this.body, this.jacket, 0.35f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.9f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.9f);
            }
            case 13 -> { // 🧘 Berlutut Ksatria (One-knee Kneeling)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 1.35f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 1.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.7f);
            }
            case 14 -> { // 🧘 Bersimpuh Seiza (Traditional Seiza Sitting)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 1.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 1.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.4f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.4f);
            }
            case 16 -> { // ⚔ Siap Bertarung (Combat Guard)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -1.1f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.9f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 0.35f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 0.35f);
            }
            case 17 -> { // ⚔ Cabut Katana Iaido
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -1.2f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.4f);
            }
            case 18 -> { // ⚔ Senjata Akimbo (Dual Wielding)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.75f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.75f);
            }
            case 19 -> { // ⚔ Bidik Busur (Bow Aiming)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.15f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.45f);
            }
            case 20 -> { // ⚔ Kuda-kuda Tombak
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.3f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.8f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 0.3f);
            }
            case 21 -> { // ⚔ Palu Godam
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.2f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -1.0f);
            }
            case 22 -> { // ⚔ Pasang Perisai
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -1.2f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.4f);
            }
            case 24 -> { // ⚔ Tebasan Melompat
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.5f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 0.4f);
            }
            case 25 -> { // 🎭 Bersedekap Dada (Cross Arms)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -1.35f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.35f);
            }
            case 26 -> { // 🎭 Hormat Ksatria (Knight Salute)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.45f);
            }
            case 27 -> { // 🎭 Hormat Bungkuk (Bowing)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendBody(this.body, this.jacket, 0.35f);
            }
            case 28 -> { // 🎭 Menunjuk Arah (Pointing)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.1f);
            }
            case 29 -> { // 🎭 Melambai Ramah (Waving)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.25f);
            }
            case 30 -> { // 🎭 Menangis Terisak
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -1.4f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.4f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendBody(this.body, this.jacket, 0.2f);
            }
            case 31 -> { // 🎭 Facepalm
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.45f);
            }
            case 32 -> { // 🎭 Berpikir Keras
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.35f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.5f);
            }
            default -> { // 0: Default Stand & unhandled (Natural joint relaxation, subtle life-like curve)
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.12f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.10f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 0.08f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendBody(this.body, this.jacket, 0.02f);
            }
        }
    }

    private boolean applyProceduralEmote(String emoteId, float animTime) {
        if (emoteId == null || emoteId.isBlank()) return false;
        String key = emoteId.toLowerCase(java.util.Locale.ROOT);
        float degToRad = (float) (Math.PI / 180.0);

        if (key.contains("wave")) {
            float wave = (float) Math.sin(animTime * 0.35f);
            this.rightArm.xRot = -140.0f * degToRad + wave * 0.1f;
            this.rightArm.yRot = 0.0f;
            this.rightArm.zRot = 25.0f * degToRad + wave * 0.35f;
            this.head.zRot += wave * 0.05f;
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.25f);
            return true;
        } else if (key.contains("clap")) {
            float clap = (float) Math.abs(Math.sin(animTime * 0.45f));
            this.rightArm.xRot = -55.0f * degToRad;
            this.rightArm.yRot = -20.0f * degToRad - clap * 0.35f;
            this.rightArm.zRot = 10.0f * degToRad;
            this.leftArm.xRot = -55.0f * degToRad;
            this.leftArm.yRot = 20.0f * degToRad + clap * 0.35f;
            this.leftArm.zRot = -10.0f * degToRad;
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.0f);
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -1.0f);
            return true;
        } else if (key.contains("cheer")) {
            float cheer = (float) Math.sin(animTime * 0.3f);
            this.rightArm.xRot = -150.0f * degToRad + cheer * 0.15f;
            this.rightArm.zRot = 25.0f * degToRad;
            this.leftArm.xRot = -150.0f * degToRad - cheer * 0.15f;
            this.leftArm.zRot = -25.0f * degToRad;
            this.head.xRot = -20.0f * degToRad + cheer * 0.05f;
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.4f);
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.4f);
            return true;
        } else if (key.contains("dance")) {
            float sway = (float) Math.sin(animTime * 0.25f);
            float bounce = (float) Math.abs(Math.cos(animTime * 0.5f)) * 1.5f;
            this.body.y += bounce;
            this.body.zRot = sway * 0.1f;
            this.head.zRot = -sway * 0.08f;
            this.rightArm.xRot = -30.0f * degToRad + sway * 0.4f;
            this.leftArm.xRot = -30.0f * degToRad - sway * 0.4f;
            this.rightLeg.xRot = sway * 0.2f;
            this.leftLeg.xRot = -sway * 0.2f;
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.6f);
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.6f);
            return true;
        } else if (key.contains("salute")) {
            this.rightArm.xRot = -70.0f * degToRad;
            this.rightArm.yRot = -35.0f * degToRad;
            this.rightArm.zRot = 40.0f * degToRad;
            this.head.xRot = 5.0f * degToRad;
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.45f);
            return true;
        } else if (key.contains("point")) {
            this.rightArm.xRot = -90.0f * degToRad;
            this.rightArm.yRot = 0.0f;
            this.rightArm.zRot = 0.0f;
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.1f);
            return true;
        } else if (key.contains("bow")) {
            this.body.xRot = 30.0f * degToRad;
            this.head.xRot = 20.0f * degToRad;
            this.rightArm.xRot = -10.0f * degToRad;
            this.leftArm.xRot = -10.0f * degToRad;
            net.sakura.weapons.compat.BendableCuboidsCompat.bendBody(this.body, this.jacket, 0.35f);
            return true;
        } else if (key.contains("cry")) {
            float shudder = (float) Math.sin(animTime * 0.6f) * 0.05f;
            this.head.xRot = 25.0f * degToRad + shudder;
            this.rightArm.xRot = -75.0f * degToRad;
            this.rightArm.yRot = 45.0f * degToRad;
            this.leftArm.xRot = -75.0f * degToRad;
            this.leftArm.yRot = -45.0f * degToRad;
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.4f);
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -1.4f);
            return true;
        } else if (key.contains("facepalm")) {
            this.head.xRot = 10.0f * degToRad;
            this.rightArm.xRot = -120.0f * degToRad;
            this.rightArm.yRot = -30.0f * degToRad;
            this.rightArm.zRot = 25.0f * degToRad;
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.45f);
            return true;
        } else if (key.contains("shrug")) {
            float shrug = (float) Math.sin(animTime * 0.2f) * 0.1f;
            this.rightArm.xRot = -35.0f * degToRad;
            this.rightArm.zRot = 45.0f * degToRad + shrug;
            this.leftArm.xRot = -35.0f * degToRad;
            this.leftArm.zRot = -45.0f * degToRad - shrug;
            this.head.zRot = 10.0f * degToRad;
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.8f);
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.8f);
            return true;
        } else if (key.contains("tpose")) {
            this.rightArm.xRot = 0.0f;
            this.rightArm.zRot = 90.0f * degToRad;
            this.leftArm.xRot = 0.0f;
            this.leftArm.zRot = -90.0f * degToRad;
            return true;
        } else if (key.contains("zombie")) {
            this.rightArm.xRot = -90.0f * degToRad;
            this.leftArm.xRot = -90.0f * degToRad;
            return true;
        } else if (key.contains("sit") || key.contains("duduk")) {
            float breathe = (float) Math.sin(animTime * 0.15f) * 0.02f;
            this.head.xRot = breathe;
            this.body.xRot = 0.0f;
            this.rightLeg.xRot = -90.0f * degToRad;
            this.rightLeg.yRot = 8.0f * degToRad;
            this.leftLeg.xRot = -90.0f * degToRad;
            this.leftLeg.yRot = -8.0f * degToRad;
            this.rightArm.xRot = -20.0f * degToRad;
            this.rightArm.yRot = -15.0f * degToRad;
            this.leftArm.xRot = -20.0f * degToRad;
            this.leftArm.yRot = 15.0f * degToRad;
            net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 1.45f);
            net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 1.45f);
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.6f);
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.6f);
            return true;
        } else if (key.contains("sleep") || key.contains("tidur") || key.contains("rebahan") || key.contains("lay") || key.contains("rest")) {
            if (key.contains("miring_kanan") || key.contains("side_right")) {
                float breathe = (float) Math.sin(animTime * 0.15f) * 0.02f;
                this.head.xRot = 5.0f * degToRad;
                this.head.yRot = -10.0f * degToRad + breathe;
                this.head.zRot = 5.0f * degToRad;
                this.rightArm.xRot = -60.0f * degToRad;
                this.rightArm.yRot = 20.0f * degToRad;
                this.leftArm.xRot = -25.0f * degToRad;
                this.leftArm.yRot = -15.0f * degToRad;
                this.rightLeg.xRot = -15.0f * degToRad;
                this.leftLeg.xRot = -25.0f * degToRad;
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.2f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.3f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 0.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 0.35f);
                return true;
            } else if (key.contains("miring_kiri") || key.contains("side_left") || key.contains("miring")) {
                float breathe = (float) Math.sin(animTime * 0.15f) * 0.02f;
                this.head.xRot = 5.0f * degToRad;
                this.head.yRot = 10.0f * degToRad + breathe;
                this.head.zRot = -5.0f * degToRad;
                this.leftArm.xRot = -60.0f * degToRad;
                this.leftArm.yRot = -20.0f * degToRad;
                this.rightArm.xRot = -25.0f * degToRad;
                this.rightArm.yRot = 15.0f * degToRad;
                this.leftLeg.xRot = -15.0f * degToRad;
                this.rightLeg.xRot = -25.0f * degToRad;
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -1.2f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.3f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 0.45f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 0.35f);
                return true;
            } else if (key.contains("tengkurap") || key.contains("prone")) {
                float breathe = (float) Math.sin(animTime * 0.15f) * 0.02f;
                this.head.xRot = -10.0f * degToRad;
                this.head.yRot = 55.0f * degToRad + breathe;
                this.rightArm.xRot = -140.0f * degToRad;
                this.rightArm.yRot = 30.0f * degToRad;
                this.leftArm.xRot = -140.0f * degToRad;
                this.leftArm.yRot = -30.0f * degToRad;
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.2f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -1.2f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 0.15f);
                net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 0.15f);
                return true;
            }
            float breathe = (float) Math.sin(animTime * 0.15f) * 0.02f;
            this.head.xRot = -5.0f * degToRad;
            this.head.yRot = 10.0f * degToRad;
            this.body.yRot = breathe;
            this.rightArm.xRot = -20.0f * degToRad;
            this.rightArm.yRot = -15.0f * degToRad;
            this.leftArm.xRot = -20.0f * degToRad;
            this.leftArm.yRot = 15.0f * degToRad;
            this.rightLeg.xRot = 0.0f;
            this.leftLeg.xRot = 0.0f;
            net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.leftLeg, this.leftPants, 0.25f);
            net.sakura.weapons.compat.BendableCuboidsCompat.bendLeg(this.rightLeg, this.rightPants, 0.25f);
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.35f);
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.35f);
            return true;
        } else if (key.contains("dab")) {
            this.head.xRot = 35.0f * degToRad;
            this.head.yRot = 45.0f * degToRad;
            this.rightArm.xRot = -125.0f * degToRad;
            this.rightArm.zRot = 45.0f * degToRad;
            this.leftArm.xRot = -45.0f * degToRad;
            this.leftArm.yRot = 45.0f * degToRad;
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -1.4f);
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.2f);
            return true;
        } else if (key.contains("think") || key.contains("pikir") || key.contains("bingung")) {
            this.head.xRot = -5.0f * degToRad;
            this.head.zRot = 15.0f * degToRad;
            this.rightArm.xRot = -110.0f * degToRad;
            this.rightArm.yRot = -30.0f * degToRad;
            this.rightArm.zRot = 10.0f * degToRad;
            this.leftArm.xRot = -30.0f * degToRad;
            this.leftArm.yRot = 25.0f * degToRad;
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.35f);
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.5f);
            return true;
        } else if (key.contains("hug") || key.contains("peluk")) {
            float sway = (float) Math.sin(animTime * 0.2f) * 0.04f;
            this.head.zRot = 12.0f * degToRad;
            this.head.xRot = 5.0f * degToRad;
            this.body.zRot = sway;
            this.rightArm.xRot = -75.0f * degToRad;
            this.rightArm.yRot = -35.0f * degToRad;
            this.rightArm.zRot = 15.0f * degToRad;
            this.leftArm.xRot = -75.0f * degToRad;
            this.leftArm.yRot = 35.0f * degToRad;
            this.leftArm.zRot = -15.0f * degToRad;
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -0.9f);
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -0.9f);
            return true;
        } else if (key.contains("laugh") || key.contains("tawa") || key.contains("ketawa") || key.contains("giggle")) {
            float laugh = (float) Math.abs(Math.sin(animTime * 0.6f)) * 0.15f;
            this.head.xRot = -20.0f * degToRad - laugh;
            this.body.xRot = -5.0f * degToRad - laugh * 0.5f;
            this.rightArm.xRot = -45.0f * degToRad;
            this.rightArm.yRot = -25.0f * degToRad;
            this.leftArm.xRot = -45.0f * degToRad;
            this.leftArm.yRot = 25.0f * degToRad;
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.rightArm, this.rightSleeve, -1.1f);
            net.sakura.weapons.compat.BendableCuboidsCompat.bendArm(this.leftArm, this.leftSleeve, -1.1f);
            return true;
        }
        return false;
    }

    public net.minecraft.core.Rotations[] getCurrentRotations() {
        float radToDeg = (float) (180.0 / Math.PI);
        return new net.minecraft.core.Rotations[] {
            new net.minecraft.core.Rotations(this.head.xRot * radToDeg, this.head.yRot * radToDeg, this.head.zRot * radToDeg),
            new net.minecraft.core.Rotations(this.body.xRot * radToDeg, this.body.yRot * radToDeg, this.body.zRot * radToDeg),
            new net.minecraft.core.Rotations(this.rightArm.xRot * radToDeg, this.rightArm.yRot * radToDeg, this.rightArm.zRot * radToDeg),
            new net.minecraft.core.Rotations(this.leftArm.xRot * radToDeg, this.leftArm.yRot * radToDeg, this.leftArm.zRot * radToDeg),
            new net.minecraft.core.Rotations(this.rightLeg.xRot * radToDeg, this.rightLeg.yRot * radToDeg, this.rightLeg.zRot * radToDeg),
            new net.minecraft.core.Rotations(this.leftLeg.xRot * radToDeg, this.leftLeg.yRot * radToDeg, this.leftLeg.zRot * radToDeg)
        };
    }

    public static net.minecraft.core.Rotations[] calculateProceduralRotations(String emoteId, float animTime) {
        String key = emoteId.toLowerCase(java.util.Locale.ROOT);
        net.minecraft.core.Rotations head = new net.minecraft.core.Rotations(0f, 0f, 0f);
        net.minecraft.core.Rotations body = new net.minecraft.core.Rotations(0f, 0f, 0f);
        net.minecraft.core.Rotations rightArm = new net.minecraft.core.Rotations(0f, 0f, 0f);
        net.minecraft.core.Rotations leftArm = new net.minecraft.core.Rotations(0f, 0f, 0f);
        net.minecraft.core.Rotations rightLeg = new net.minecraft.core.Rotations(0f, 0f, 0f);
        net.minecraft.core.Rotations leftLeg = new net.minecraft.core.Rotations(0f, 0f, 0f);

        float radToDeg = (float) (180.0 / Math.PI);

        if (key.contains("wave")) {
            float wave = (float) Math.sin(animTime * 0.35f);
            rightArm = new net.minecraft.core.Rotations(-140.0f + wave * 0.1f * radToDeg, 0f, 25.0f + wave * 0.35f * radToDeg);
            head = new net.minecraft.core.Rotations(0f, 0f, wave * 0.05f * radToDeg);
        } else if (key.contains("clap")) {
            float clap = (float) Math.abs(Math.sin(animTime * 0.45f));
            rightArm = new net.minecraft.core.Rotations(-55.0f, -20.0f - clap * 0.35f * radToDeg, 10.0f);
            leftArm = new net.minecraft.core.Rotations(-55.0f, 20.0f + clap * 0.35f * radToDeg, -10.0f);
        } else if (key.contains("cheer")) {
            float cheer = (float) Math.sin(animTime * 0.3f);
            rightArm = new net.minecraft.core.Rotations(-150.0f + cheer * 0.15f * radToDeg, 0f, 25.0f);
            leftArm = new net.minecraft.core.Rotations(-150.0f - cheer * 0.15f * radToDeg, 0f, -25.0f);
            head = new net.minecraft.core.Rotations(-20.0f + cheer * 0.05f * radToDeg, 0f, 0f);
        } else if (key.contains("dance")) {
            float sway = (float) Math.sin(animTime * 0.25f);
            body = new net.minecraft.core.Rotations(0f, 0f, sway * 0.1f * radToDeg);
            head = new net.minecraft.core.Rotations(0f, 0f, -sway * 0.08f * radToDeg);
            rightArm = new net.minecraft.core.Rotations(-30.0f + sway * 0.4f * radToDeg, 0f, 0f);
            leftArm = new net.minecraft.core.Rotations(-30.0f - sway * 0.4f * radToDeg, 0f, 0f);
            rightLeg = new net.minecraft.core.Rotations(sway * 0.2f * radToDeg, 0f, 0f);
            leftLeg = new net.minecraft.core.Rotations(-sway * 0.2f * radToDeg, 0f, 0f);
        } else if (key.contains("salute")) {
            rightArm = new net.minecraft.core.Rotations(-70.0f, -35.0f, 40.0f);
            head = new net.minecraft.core.Rotations(5.0f, 0f, 0f);
        } else if (key.contains("point")) {
            rightArm = new net.minecraft.core.Rotations(-90.0f, 0f, 0f);
        } else if (key.contains("bow")) {
            body = new net.minecraft.core.Rotations(30.0f, 0f, 0f);
            head = new net.minecraft.core.Rotations(20.0f, 0f, 0f);
            rightArm = new net.minecraft.core.Rotations(-10.0f, 0f, 0f);
            leftArm = new net.minecraft.core.Rotations(-10.0f, 0f, 0f);
        } else if (key.contains("cry")) {
            float shudder = (float) Math.sin(animTime * 0.6f) * 0.05f;
            head = new net.minecraft.core.Rotations(25.0f + shudder * radToDeg, 0f, 0f);
            rightArm = new net.minecraft.core.Rotations(-75.0f, 45.0f, 0f);
            leftArm = new net.minecraft.core.Rotations(-75.0f, -45.0f, 0f);
        } else if (key.contains("facepalm")) {
            head = new net.minecraft.core.Rotations(10.0f, 0f, 0f);
            rightArm = new net.minecraft.core.Rotations(-120.0f, -30.0f, 25.0f);
        } else if (key.contains("shrug")) {
            float shrug = (float) Math.sin(animTime * 0.2f) * 0.1f;
            rightArm = new net.minecraft.core.Rotations(-35.0f, 0f, 45.0f + shrug * radToDeg);
            leftArm = new net.minecraft.core.Rotations(-35.0f, 0f, -45.0f - shrug * radToDeg);
            head = new net.minecraft.core.Rotations(0f, 0f, 10.0f);
        } else if (key.contains("tpose")) {
            rightArm = new net.minecraft.core.Rotations(0f, 0f, 90.0f);
            leftArm = new net.minecraft.core.Rotations(0f, 0f, -90.0f);
        } else if (key.contains("zombie")) {
            rightArm = new net.minecraft.core.Rotations(-90.0f, 0f, 0f);
            leftArm = new net.minecraft.core.Rotations(-90.0f, 0f, 0f);
        } else if (key.contains("sit") || key.contains("duduk")) {
            float breathe = (float) Math.sin(animTime * 0.15f) * 0.02f;
            head = new net.minecraft.core.Rotations(breathe * radToDeg, 0f, 0f);
            rightArm = new net.minecraft.core.Rotations(-20.0f, -15.0f, 0f);
            leftArm = new net.minecraft.core.Rotations(-20.0f, 15.0f, 0f);
            rightLeg = new net.minecraft.core.Rotations(-90.0f, 8.0f, 0f);
            leftLeg = new net.minecraft.core.Rotations(-90.0f, -8.0f, 0f);
        } else if (key.contains("sleep") || key.contains("tidur") || key.contains("rebahan") || key.contains("lay") || key.contains("rest")) {
            if (key.contains("miring_kanan") || key.contains("side_right")) {
                head = new net.minecraft.core.Rotations(5.0f, -10.0f, 5.0f);
                rightArm = new net.minecraft.core.Rotations(-60.0f, 20.0f, 0.0f);
                leftArm = new net.minecraft.core.Rotations(-25.0f, -15.0f, 0.0f);
                rightLeg = new net.minecraft.core.Rotations(-15.0f, 0.0f, 0.0f);
                leftLeg = new net.minecraft.core.Rotations(-25.0f, 0.0f, 0.0f);
            } else if (key.contains("miring_kiri") || key.contains("side_left") || key.contains("miring")) {
                head = new net.minecraft.core.Rotations(5.0f, 10.0f, -5.0f);
                leftArm = new net.minecraft.core.Rotations(-60.0f, -20.0f, 0.0f);
                rightArm = new net.minecraft.core.Rotations(-25.0f, 15.0f, 0.0f);
                leftLeg = new net.minecraft.core.Rotations(-15.0f, 0.0f, 0.0f);
                rightLeg = new net.minecraft.core.Rotations(-25.0f, 0.0f, 0.0f);
            } else if (key.contains("tengkurap") || key.contains("prone")) {
                head = new net.minecraft.core.Rotations(-10.0f, 55.0f, 0.0f);
                rightArm = new net.minecraft.core.Rotations(-140.0f, 30.0f, 0.0f);
                leftArm = new net.minecraft.core.Rotations(-140.0f, -30.0f, 0.0f);
                rightLeg = new net.minecraft.core.Rotations(0.0f, 0.0f, 0.0f);
                leftLeg = new net.minecraft.core.Rotations(0.0f, 0.0f, 0.0f);
            } else {
                head = new net.minecraft.core.Rotations(-5.0f, 10.0f, 0f);
                rightArm = new net.minecraft.core.Rotations(-20.0f, -15.0f, 0f);
                leftArm = new net.minecraft.core.Rotations(-20.0f, 15.0f, 0f);
            }
        } else if (key.contains("dab")) {
            head = new net.minecraft.core.Rotations(35.0f, 45.0f, 0.0f);
            rightArm = new net.minecraft.core.Rotations(-125.0f, 0.0f, 45.0f);
            leftArm = new net.minecraft.core.Rotations(-45.0f, 45.0f, 0.0f);
        } else if (key.contains("think") || key.contains("pikir") || key.contains("bingung")) {
            head = new net.minecraft.core.Rotations(-5.0f, 0.0f, 15.0f);
            rightArm = new net.minecraft.core.Rotations(-110.0f, -30.0f, 10.0f);
            leftArm = new net.minecraft.core.Rotations(-30.0f, 25.0f, 0.0f);
        } else if (key.contains("hug") || key.contains("peluk")) {
            head = new net.minecraft.core.Rotations(5.0f, 0f, 12.0f);
            rightArm = new net.minecraft.core.Rotations(-75.0f, -35.0f, 15.0f);
            leftArm = new net.minecraft.core.Rotations(-75.0f, 35.0f, -15.0f);
        } else if (key.contains("laugh") || key.contains("tawa") || key.contains("ketawa") || key.contains("giggle")) {
            float laugh = (float) Math.abs(Math.sin(animTime * 0.6f)) * 0.15f;
            head = new net.minecraft.core.Rotations(-20.0f - laugh * radToDeg, 0f, 0f);
            body = new net.minecraft.core.Rotations(-5.0f - laugh * 0.5f * radToDeg, 0f, 0f);
            rightArm = new net.minecraft.core.Rotations(-45.0f, -25.0f, 0f);
            leftArm = new net.minecraft.core.Rotations(-45.0f, 25.0f, 0f);
        }

        return new net.minecraft.core.Rotations[] { head, body, rightArm, leftArm, rightLeg, leftLeg };
    }
}
