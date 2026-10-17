package net.sakura.weapons.client.render.bedrock.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import net.sakura.weapons.client.render.bedrock.pojo.FaceItem;
import net.sakura.weapons.client.render.bedrock.pojo.FaceUVsItem;

import java.util.ArrayList;
import java.util.List;

public class BedrockCubePerFace implements BedrockCube {
    private final List<BedrockPolygon> polygons = new ArrayList<>();

    public BedrockCubePerFace(float x, float y, float z,
                              float width, float height, float depth,
                              float inflate,
                              float texWidth, float texHeight,
                              FaceUVsItem faceUVs) {
        float x0 = (x - inflate) / 16.0f;
        float y0 = (y - inflate) / 16.0f;
        float z0 = (z - inflate) / 16.0f;
        float x1 = (x + width + inflate) / 16.0f;
        float y1 = (y + height + inflate) / 16.0f;
        float z1 = (z + depth + inflate) / 16.0f;

        BedrockVertex p0 = new BedrockVertex(x0, y0, z0, 0, 0);
        BedrockVertex p1 = new BedrockVertex(x1, y0, z0, 0, 0);
        BedrockVertex p2 = new BedrockVertex(x1, y1, z0, 0, 0);
        BedrockVertex p3 = new BedrockVertex(x0, y1, z0, 0, 0);
        BedrockVertex p4 = new BedrockVertex(x0, y0, z1, 0, 0);
        BedrockVertex p5 = new BedrockVertex(x1, y0, z1, 0, 0);
        BedrockVertex p6 = new BedrockVertex(x1, y1, z1, 0, 0);
        BedrockVertex p7 = new BedrockVertex(x0, y1, z1, 0, 0);

        if (faceUVs != null) {
            addFace(faceUVs.getUp(), new BedrockVertex[]{p5, p4, p0, p1}, texWidth, texHeight, Direction.UP);
            addFace(faceUVs.getDown(), new BedrockVertex[]{p2, p3, p7, p6}, texWidth, texHeight, Direction.DOWN);
            addFace(faceUVs.getWest(), new BedrockVertex[]{p0, p4, p7, p3}, texWidth, texHeight, Direction.WEST);
            addFace(faceUVs.getNorth(), new BedrockVertex[]{p1, p0, p3, p2}, texWidth, texHeight, Direction.NORTH);
            addFace(faceUVs.getEast(), new BedrockVertex[]{p5, p1, p2, p6}, texWidth, texHeight, Direction.EAST);
            addFace(faceUVs.getSouth(), new BedrockVertex[]{p4, p5, p6, p7}, texWidth, texHeight, Direction.SOUTH);
        }
    }

    private void addFace(FaceItem item, BedrockVertex[] verts, float tw, float th, Direction dir) {
        if (item == null) return;
        float[] uv = item.getUv();
        float[] uvSize = item.getUvSize();
        if (uv == null || uv.length < 2 || uvSize == null || uvSize.length < 2) return;
        float u1 = uv[0];
        float v1 = uv[1];
        float u2 = u1 + uvSize[0];
        float v2 = v1 + uvSize[1];
        polygons.add(new BedrockPolygon(verts, u1, v1, u2, v2, tw, th, false, dir));
    }

    @Override
    public void compile(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        for (BedrockPolygon polygon : polygons) {
            polygon.compile(pose, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }
}
