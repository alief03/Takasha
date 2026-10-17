package net.sakura.weapons.client.render.bedrock.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class BedrockPart {
    public final List<BedrockCube> cubes = new ArrayList<>();
    public final List<BedrockPart> children = new ArrayList<>();
    public BedrockPart parent;

    public float x = 0;
    public float y = 0;
    public float z = 0;

    public float initRotX = 0;
    public float initRotY = 0;
    public float initRotZ = 0;

    public float xRot = 0;
    public float yRot = 0;
    public float zRot = 0;

    public float offsetX = 0;
    public float offsetY = 0;
    public float offsetZ = 0;

    public float xScale = 1.0f;
    public float yScale = 1.0f;
    public float zScale = 1.0f;

    public Quaternionf additionalQuaternion = new Quaternionf(0, 0, 0, 1);
    public boolean visible = true;
    public boolean mirror = false;

    public BedrockPart() {}

    public void setPos(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setInitRotationAngle(float rx, float ry, float rz) {
        this.initRotX = rx;
        this.initRotY = ry;
        this.initRotZ = rz;
        this.xRot = rx;
        this.yRot = ry;
        this.zRot = rz;
    }

    public void resetPose() {
        this.xRot = this.initRotX;
        this.yRot = this.initRotY;
        this.zRot = this.initRotZ;
        this.offsetX = 0;
        this.offsetY = 0;
        this.offsetZ = 0;
        this.xScale = 1.0f;
        this.yScale = 1.0f;
        this.zScale = 1.0f;
        this.additionalQuaternion.identity();
        for (BedrockPart child : this.children) {
            child.resetPose();
        }
    }

    public void addChild(BedrockPart child) {
        if (child != null) {
            child.parent = this;
            this.children.add(child);
        }
    }

    public void translateAndRotate(PoseStack poseStack) {
        poseStack.translate(this.x / 16.0f + this.offsetX, this.y / 16.0f + this.offsetY, this.z / 16.0f + this.offsetZ);
        if (this.xRot != 0 || this.yRot != 0 || this.zRot != 0) {
            poseStack.last().pose().rotateZYX(this.zRot, this.yRot, this.xRot);
            poseStack.last().normal().rotateZYX(this.zRot, this.yRot, this.xRot);
        }
    }

    public void translateAndRotateAndScale(PoseStack poseStack) {
        translateAndRotate(poseStack);
        if (this.additionalQuaternion != null && (this.additionalQuaternion.x != 0 || this.additionalQuaternion.y != 0 || this.additionalQuaternion.z != 0 || this.additionalQuaternion.w != 1)) {
            poseStack.mulPose(this.additionalQuaternion);
        }
        if (this.xScale != 1.0f || this.yScale != 1.0f || this.zScale != 1.0f) {
            poseStack.scale(this.xScale, this.yScale, this.zScale);
        }
    }

    public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (!this.visible) return;
        if (this.cubes.isEmpty() && this.children.isEmpty()) return;

        poseStack.pushPose();
        this.translateAndRotateAndScale(poseStack);

        for (BedrockCube cube : this.cubes) {
            cube.compile(poseStack.last(), buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }

        for (BedrockPart child : this.children) {
            child.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }

        poseStack.popPose();
    }
}
