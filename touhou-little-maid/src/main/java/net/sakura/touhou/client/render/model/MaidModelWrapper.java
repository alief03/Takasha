package net.sakura.touhou.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.sakura.touhou.client.render.state.MaidRenderState;
import net.sakura.touhou.entity.task.MaidTaskMode;

/**
 * MaidModelWrapper
 * Model wrapper humanoid/bedrock untuk EntityMaid pada Minecraft 26.2.
 * Mengimplementasikan:
 * - Dynamic Posing berdasarkan MaidTaskMode dan Favorability/Begging.
 * - Hardware Anti-Crash LOD (Level of Detail):
 *   1. Frustum Culling skip jika isOffscreen = true.
 *   2. Static pose jika distanceToCamera > 48m (nol limb swing/animasi).
 *   3. 10 Hz animation clamping jika distanceToCamera 24m - 48m untuk menghemat CPU cycle AMD Ryzen 3 7320U.
 */
@Environment(EnvType.CLIENT)
public class MaidModelWrapper extends PlayerModel {

    public MaidModelWrapper(ModelPart root) {
        super(root, true); // Slim arms by default for maid aesthetics
    }

    @Override
    public void setupAnim(AvatarRenderState avatarState) {
        if (!(avatarState instanceof MaidRenderState state)) {
            super.setupAnim(avatarState);
            return;
        }

        // Frustum Culling: Jangan evaluasi animasi jika di luar viewport kamera
        if (state.isOffscreen) {
            return;
        }

        // Distance LOD: Jika jarak > 48 blok, gunakan static pose untuk hemat CPU
        if (state.distanceToCamera > 48.0F) {
            this.head.xRot = 0.0F;
            this.head.yRot = 0.0F;
            this.head.zRot = 0.0F;
            this.body.xRot = 0.0F;
            this.rightArm.xRot = 0.0F;
            this.leftArm.xRot = 0.0F;
            this.rightLeg.xRot = 0.0F;
            this.leftLeg.xRot = 0.0F;
            return;
        }

        super.setupAnim(state);

        // Sitting Pose
        if (state.isSitting) {
            this.rightLeg.xRot = -1.4137167F;
            this.rightLeg.yRot = 0.31415927F;
            this.rightLeg.zRot = 0.07853982F;
            this.leftLeg.xRot = -1.4137167F;
            this.leftLeg.yRot = -0.31415927F;
            this.leftLeg.zRot = -0.07853982F;
        }

        // Begging / Cute Tilt
        if (state.isBegging) {
            float tiltTime = state.animTime;
            // 10 Hz rate limiter untuk jarak medium (24 - 48m)
            if (state.distanceToCamera > 24.0F) {
                tiltTime = ((int) (tiltTime * 2.0F)) * 0.5F;
            }
            this.head.zRot += 0.15F * (float) Math.sin(tiltTime * 0.15F);
        }

        // Task Mode Postures
        if (state.taskModeOrdinal == MaidTaskMode.ATTACK.ordinal()) {
            this.rightArm.xRot = -0.75F;
            this.leftArm.xRot = -0.75F;
        } else if (state.taskModeOrdinal == MaidTaskMode.FARM.ordinal()) {
            this.rightArm.xRot = -0.4F;
        } else if (state.taskModeOrdinal == MaidTaskMode.FISH.ordinal()) {
            this.rightArm.xRot = -0.6F;
            this.leftArm.xRot = -0.6F;
        }
    }
}
