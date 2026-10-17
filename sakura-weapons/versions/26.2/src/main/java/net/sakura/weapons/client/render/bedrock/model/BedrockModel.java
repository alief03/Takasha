package net.sakura.weapons.client.render.bedrock.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.sakura.weapons.SakuraWeaponsMod;
import net.sakura.weapons.client.render.bedrock.pojo.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BedrockModel {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(CubesItem.class, new CubesItem.Deserializer())
            .registerTypeAdapter(BedrockModelPOJO.class, new BedrockModelPOJO.Deserializer())
            .create();

    private final Map<String, BedrockPart> modelMap = new HashMap<>();
    private final List<BedrockPart> rootParts = new ArrayList<>();
    private int textureWidth = 64;
    private int textureHeight = 64;

    public BedrockModel() {}

    public static BedrockModel fromInputStream(InputStream stream) {
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            BedrockModelPOJO pojo = GSON.fromJson(reader, BedrockModelPOJO.class);
            BedrockModel model = new BedrockModel();
            model.loadModel(pojo);
            return model;
        } catch (Exception e) {
            SakuraWeaponsMod.LOGGER.error("Failed to parse Bedrock model from input stream", e);
            return new BedrockModel();
        }
    }

    public void loadModel(BedrockModelPOJO pojo) {
        if (pojo == null) return;
        modelMap.clear();
        rootParts.clear();

        BonesItem[] bones;
        if (pojo.isLegacy()) {
            GeometryModelLegacy legacy = pojo.getGeometryModelLegacy();
            this.textureWidth = legacy.getTextureWidth();
            this.textureHeight = legacy.getTextureHeight();
            bones = legacy.getBones();
        } else if (pojo.getGeometryModelNew() != null) {
            GeometryModelNew modern = pojo.getGeometryModelNew();
            this.textureWidth = modern.getDescription().getTextureWidth();
            this.textureHeight = modern.getDescription().getTextureHeight();
            bones = modern.getBones();
        } else {
            return;
        }

        Map<String, BonesItem> indexBones = new HashMap<>();
        for (BonesItem bone : bones) {
            if (bone.getName() != null) {
                indexBones.put(bone.getName(), bone);
                BedrockPart part = new BedrockPart();
                part.mirror = bone.isMirror();
                part.visible = !bone.isNeverRender();
                modelMap.put(bone.getName(), part);
            }
        }

        for (BonesItem bone : bones) {
            String name = bone.getName();
            if (name == null) continue;
            BedrockPart part = modelMap.get(name);
            if (part == null) continue;

            float posX = convertPivot(indexBones, bone, 0);
            float posY = convertPivot(indexBones, bone, 1);
            float posZ = convertPivot(indexBones, bone, 2);
            part.setPos(posX, posY, posZ);

            float[] rot = bone.getRotation();
            if (rot != null && rot.length >= 3) {
                part.setInitRotationAngle(
                        (float) Math.toRadians(rot[0]),
                        (float) Math.toRadians(rot[1]),
                        (float) Math.toRadians(rot[2])
                );
            }

            String parentName = bone.getParent();
            if (parentName != null && modelMap.containsKey(parentName)) {
                modelMap.get(parentName).addChild(part);
            } else {
                rootParts.add(part);
            }

            CubesItem[] cubes = bone.getCubes();
            if (cubes != null) {
                for (CubesItem cube : cubes) {
                    float[] origin = cube.getOrigin();
                    float[] size = cube.getSize();
                    float[] pivot = bone.getPivot();
                    float inflate = cube.getInflate() != 0 ? cube.getInflate() : bone.getInflate();

                    float ox = origin[0] - pivot[0];
                    float oy = pivot[1] - origin[1] - size[1];
                    float oz = origin[2] - pivot[2];

                    if (cube.getFaceUv() != null) {
                        part.cubes.add(new BedrockCubePerFace(
                                ox, oy, oz,
                                size[0], size[1], size[2],
                                inflate,
                                this.textureWidth, this.textureHeight,
                                cube.getFaceUv()
                        ));
                    } else {
                        float[] uv = cube.getUv();
                        boolean mirror = cube.isMirror() || bone.isMirror();
                        part.cubes.add(new BedrockCubeBox(
                                uv != null && uv.length > 0 ? uv[0] : 0,
                                uv != null && uv.length > 1 ? uv[1] : 0,
                                ox, oy, oz,
                                size[0], size[1], size[2],
                                inflate, mirror,
                                this.textureWidth, this.textureHeight
                        ));
                    }
                }
            }
        }
    }

    private float convertPivot(Map<String, BonesItem> indexBones, BonesItem bone, int axis) {
        String parentName = bone.getParent();
        if (parentName != null && indexBones.containsKey(parentName)) {
            BonesItem parentBone = indexBones.get(parentName);
            float parentP = parentBone.getPivot()[axis];
            float childP = bone.getPivot()[axis];
            if (axis == 2) {
                return childP - parentP;
            } else {
                return parentP - childP;
            }
        } else {
            if (axis == 1) {
                return 24.0f - bone.getPivot()[1];
            } else if (axis == 0) {
                return -bone.getPivot()[0];
            } else {
                return bone.getPivot()[2];
            }
        }
    }

    public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        for (BedrockPart rootPart : rootParts) {
            rootPart.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    public void resetPose() {
        for (BedrockPart rootPart : rootParts) {
            rootPart.resetPose();
        }
    }

    public BedrockPart getPart(String name) {
        return modelMap.get(name);
    }

    public Map<String, BedrockPart> getModelMap() {
        return modelMap;
    }

    public boolean hasPart(String name) {
        return modelMap.containsKey(name);
    }
}
