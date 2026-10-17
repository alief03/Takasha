package net.sakura.weapons.client.render.bedrock.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class BedrockPolygon {
    public final BedrockVertex[] vertices;
    public final Vector3f normal;

    public BedrockPolygon(BedrockVertex[] vertices, float u1, float v1, float u2, float v2, float texWidth, float texHeight, boolean mirror, Direction direction) {
        this.vertices = vertices;
        float uMin = u1 / texWidth;
        float uMax = u2 / texWidth;
        float vMin = v1 / texHeight;
        float vMax = v2 / texHeight;

        if (mirror) {
            float temp = uMin;
            uMin = uMax;
            uMax = temp;
        }

        this.vertices[0] = vertices[0].remap(uMax, vMin);
        this.vertices[1] = vertices[1].remap(uMin, vMin);
        this.vertices[2] = vertices[2].remap(uMin, vMax);
        this.vertices[3] = vertices[3].remap(uMax, vMax);

        this.normal = direction.step();
        if (mirror) {
            this.normal.mul(-1.0f, 1.0f, 1.0f);
        }
    }

    public void compile(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        Vector3f transformedNormal = normalMatrix.transform(new Vector3f(this.normal));

        int r = (int) (red * 255.0f);
        int g = (int) (green * 255.0f);
        int b = (int) (blue * 255.0f);
        int a = (int) (alpha * 255.0f);
        int color = ((a & 0xFF) << 24) | ((b & 0xFF) << 16) | ((g & 0xFF) << 8) | (r & 0xFF);

        for (BedrockVertex vertex : this.vertices) {
            Vector3f transformedPos = poseMatrix.transformPosition(vertex.pos.x(), vertex.pos.y(), vertex.pos.z(), new Vector3f());
            buffer.addVertex(
                transformedPos.x(), transformedPos.y(), transformedPos.z(),
                color,
                vertex.u, vertex.v,
                packedOverlay, packedLight,
                transformedNormal.x(), transformedNormal.y(), transformedNormal.z()
            );
        }
    }
}
