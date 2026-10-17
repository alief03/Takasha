package net.sakura.weapons.client.render.bedrock.animation;

import net.sakura.weapons.client.render.bedrock.model.BedrockModel;
import net.sakura.weapons.client.render.bedrock.model.BedrockPart;
import net.sakura.weapons.client.render.entity.state.TakashaMaidRenderState;

public class BedrockMaidAnimation {

    public static void apply(BedrockModel model, TakashaMaidRenderState state) {
        model.resetPose();

        float degToRad = (float) (Math.PI / 180.0);
        float time = state.animTime;
        float limbSwing = state.walkPos;
        float limbSwingAmount = Math.min(1.0f, state.walkSpeed);

        // 1. Head rotation (Look At)
        BedrockPart head = model.getPart("head");
        if (head != null) {
            head.xRot += state.headPitch * degToRad;
            head.yRot += state.netHeadYaw * degToRad;

            if (state.isBegging) {
                head.zRot += (float) Math.sin(time * 0.1f) * 0.12f;
                head.xRot += 0.15f;
            }
        }

        // 2. Eyes blink
        BedrockPart blink = model.getPart("blink");
        if (blink != null) {
            int blinkTick = ((int) time) % 80;
            blink.visible = (blinkTick >= 76 && blinkTick <= 79);
        }

        // 3. Legs and Sitting
        BedrockPart legRight = model.getPart("legRight");
        if (legRight == null) legRight = model.getPart("rightLeg");
        BedrockPart legLeft = model.getPart("legLeft");
        if (legLeft == null) legLeft = model.getPart("leftLeg");

        BedrockPart armRight = model.getPart("armRight");
        if (armRight == null) armRight = model.getPart("rightArm");
        BedrockPart armLeft = model.getPart("armLeft");
        if (armLeft == null) armLeft = model.getPart("leftArm");

        BedrockPart body = model.getPart("body");
        BedrockPart skirt = model.getPart("sittingRotationSkirt");

        if (state.isSitting) {
            if (legRight != null) {
                legRight.xRot = -1.4137f;
                legRight.yRot = 0.314f;
                legRight.zRot = 0.078f;
            }
            if (legLeft != null) {
                legLeft.xRot = -1.4137f;
                legLeft.yRot = -0.314f;
                legLeft.zRot = -0.078f;
            }
            if (armRight != null) {
                armRight.xRot = -0.628f;
                armRight.zRot = 0.1f;
            }
            if (armLeft != null) {
                armLeft.xRot = -0.628f;
                armLeft.zRot = -0.1f;
            }
            if (skirt != null) {
                skirt.xRot = -0.65f;
            }
        } else {
            // Walking animation
            if (legRight != null) {
                legRight.xRot += (float) Math.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
            }
            if (legLeft != null) {
                legLeft.xRot += (float) Math.cos(limbSwing * 0.6662f + Math.PI) * 1.4f * limbSwingAmount;
            }

            if (state.isBegging) {
                if (armRight != null) {
                    armRight.xRot = -0.75f;
                    armRight.yRot = -0.3f;
                    armRight.zRot = 0.1f + (float) Math.sin(time * 0.2f) * 0.05f;
                }
                if (armLeft != null) {
                    armLeft.xRot = -0.75f;
                    armLeft.yRot = 0.3f;
                    armLeft.zRot = -0.1f - (float) Math.sin(time * 0.2f) * 0.05f;
                }
            } else {
                if (armRight != null) {
                    armRight.xRot += (float) Math.cos(limbSwing * 0.6662f + Math.PI) * 1.4f * limbSwingAmount * 0.5f;
                    armRight.zRot += (float) Math.cos(time * 0.09f) * 0.05f + 0.05f;
                }
                if (armLeft != null) {
                    armLeft.xRot += (float) Math.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount * 0.5f;
                    armLeft.zRot -= (float) Math.cos(time * 0.09f) * 0.05f + 0.05f;
                }
            }
        }

        // 4. Attack / Swing Animation
        if (state.attackTime > 0.0f && armRight != null) {
            float swing = state.attackTime;
            float swingCurve = (float) Math.sin(swing * Math.PI);
            armRight.xRot -= swingCurve * 1.2f;
            armRight.yRot += swingCurve * 0.5f;
        }

        // 5. Hair, ribbons & tails idle physics
        for (String hairBone : new String[]{"hair", "hairBack", "tail", "ahoge", "ribbon"}) {
            BedrockPart part = model.getPart(hairBone);
            if (part != null) {
                part.zRot += (float) Math.sin(time * 0.08f) * 0.04f;
                part.xRot += (float) Math.cos(time * 0.08f) * 0.04f;
            }
        }
    }
}
