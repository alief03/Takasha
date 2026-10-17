package net.sakura.weapons.client.render.bedrock.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public interface BedrockCube {
    void compile(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha);
}
