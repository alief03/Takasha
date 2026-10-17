package net.sakura.weapons.client.render.bedrock.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;

public class BedrockCubeBox implements BedrockCube {
    private final BedrockPolygon[] polygons = new BedrockPolygon[6];

    public BedrockCubeBox(float u, float v,
                          float x, float y, float z,
                          float width, float height, float depth,
                          float inflate, boolean mirror,
                          float texWidth, float texHeight) {
        float x0 = (x - inflate) / 16.0f;
        float y0 = (y - inflate) / 16.0f;
        float z0 = (z - inflate) / 16.0f;
        float x1 = (x + width + inflate) / 16.0f;
        float y1 = (y + height + inflate) / 16.0f;
        float z1 = (z + depth + inflate) / 16.0f;

        if (mirror) {
            float temp = x1;
            x1 = x0;
            x0 = temp;
        }

        BedrockVertex p0 = new BedrockVertex(x0, y0, z0, 0, 0);
        BedrockVertex p1 = new BedrockVertex(x1, y0, z0, 0, 0);
        BedrockVertex p2 = new BedrockVertex(x1, y1, z0, 0, 0);
        BedrockVertex p3 = new BedrockVertex(x0, y1, z0, 0, 0);
        BedrockVertex p4 = new BedrockVertex(x0, y0, z1, 0, 0);
        BedrockVertex p5 = new BedrockVertex(x1, y0, z1, 0, 0);
        BedrockVertex p6 = new BedrockVertex(x1, y1, z1, 0, 0);
        BedrockVertex p7 = new BedrockVertex(x0, y1, z1, 0, 0);

        float w = Math.round(width);
        float h = Math.round(height);
        float d = Math.round(depth);

        // UP (Y+)
        polygons[0] = new BedrockPolygon(new BedrockVertex[]{p5, p4, p0, p1},
                u + d, v, u + d + w, v + d, texWidth, texHeight, mirror, Direction.UP);

        // DOWN (Y-)
        polygons[1] = new BedrockPolygon(new BedrockVertex[]{p2, p3, p7, p6},
                u + d + w, v, u + d + w + w, v + d, texWidth, texHeight, mirror, Direction.DOWN);

        // WEST (X-)
        polygons[2] = new BedrockPolygon(new BedrockVertex[]{p0, p4, p7, p3},
                u, v + d, u + d, v + d + h, texWidth, texHeight, mirror, Direction.WEST);

        // NORTH (Z-)
        polygons[3] = new BedrockPolygon(new BedrockVertex[]{p1, p0, p3, p2},
                u + d, v + d, u + d + w, v + d + h, texWidth, texHeight, mirror, Direction.NORTH);

        // EAST (X+)
        polygons[4] = new BedrockPolygon(new BedrockVertex[]{p5, p1, p2, p6},
                u + d + w, v + d, u + d + w + d, v + d + h, texWidth, texHeight, mirror, Direction.EAST);

        // SOUTH (Z+)
        polygons[5] = new BedrockPolygon(new BedrockVertex[]{p4, p5, p6, p7},
                u + d + w + d, v + d, u + d + w + d + w, v + d + h, texWidth, texHeight, mirror, Direction.SOUTH);
    }

    @Override
    public void compile(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        for (BedrockPolygon polygon : polygons) {
            if (polygon != null) {
                polygon.compile(pose, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            }
        }
    }
}
