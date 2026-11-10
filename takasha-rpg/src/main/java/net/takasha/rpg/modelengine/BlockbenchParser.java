package net.takasha.rpg.modelengine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class BlockbenchParser {

    private static final Logger LOGGER = LoggerFactory.getLogger("takasha-model");

    public static BlockbenchModel parse(String modelId, java.io.InputStream stream) {
        return parse(modelId, new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8));
    }

    public static BlockbenchModel parse(String modelId, Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        BlockbenchModel model = new BlockbenchModel(modelId);

        // 1. Elements (Cubes)
        Map<String, BlockbenchModel.CubeDef> cubeMap = new HashMap<>();
        if (root.has("elements")) {
            for (JsonElement el : root.getAsJsonArray("elements")) {
                JsonObject cubeObj = el.getAsJsonObject();
                String uuid = cubeObj.has("uuid") ? cubeObj.get("uuid").getAsString() : "";
                Vector3f from = parseVector3f(cubeObj.getAsJsonArray("from"));
                Vector3f to = parseVector3f(cubeObj.getAsJsonArray("to"));
                Vector3f origin = cubeObj.has("origin") ? parseVector3f(cubeObj.getAsJsonArray("origin")) : new Vector3f(0, 0, 0);
                cubeMap.put(uuid, new BlockbenchModel.CubeDef(from, to, origin));
            }
        }

        // 2. Groups (Bones)
        Map<String, BlockbenchModel.BoneDef> boneUuidMap = new HashMap<>();
        if (root.has("groups")) {
            for (JsonElement el : root.getAsJsonArray("groups")) {
                if (!el.isJsonObject()) continue;
                JsonObject groupObj = el.getAsJsonObject();
                String uuid = groupObj.has("uuid") ? groupObj.get("uuid").getAsString() : "";
                String rawName = groupObj.has("name") ? groupObj.get("name").getAsString() : null;
                String name = (rawName != null && !rawName.isBlank()) ? rawName : uuid;
                Vector3f origin = groupObj.has("origin") ? parseVector3f(groupObj.getAsJsonArray("origin")) : new Vector3f(0, 0, 0);
                Vector3f rotation = groupObj.has("rotation") ? parseVector3f(groupObj.getAsJsonArray("rotation")) : new Vector3f(0, 0, 0);

                BlockbenchModel.BoneDef bone = new BlockbenchModel.BoneDef(uuid, name, origin, rotation);
                model.getBones().put(name, bone);
                if (uuid != null && !uuid.isBlank()) {
                    model.getBones().put(uuid, bone);
                    boneUuidMap.put(uuid, bone);
                }
            }
        }

        // 3. Outliner Hierarchy
        if (root.has("outliner")) {
            parseOutliner(root.getAsJsonArray("outliner"), null, boneUuidMap, cubeMap, model);
        }

        // 4. Animations
        if (root.has("animations")) {
            for (JsonElement el : root.getAsJsonArray("animations")) {
                JsonObject animObj = el.getAsJsonObject();
                String name = animObj.has("name") ? animObj.get("name").getAsString() : "anim";
                String loopStr = animObj.has("loop") ? animObj.get("loop").getAsString() : "once";
                boolean loop = "loop".equalsIgnoreCase(loopStr);
                double length = animObj.has("length") ? animObj.get("length").getAsDouble() : 0.05;

                BlockbenchModel.AnimationDef anim = new BlockbenchModel.AnimationDef(name, loop, length);

                if (animObj.has("animators")) {
                    JsonObject animators = animObj.getAsJsonObject("animators");
                    for (Map.Entry<String, JsonElement> entry : animators.entrySet()) {
                        String boneUuidOrId = entry.getKey();
                        JsonObject boneAnim = entry.getValue().getAsJsonObject();
                        String boneName = boneAnim.has("name") ? boneAnim.get("name").getAsString() : boneUuidOrId;

                        BlockbenchModel.BoneTimeline timeline = new BlockbenchModel.BoneTimeline();

                        if (boneAnim.has("keyframes")) {
                            for (JsonElement kfEl : boneAnim.getAsJsonArray("keyframes")) {
                                JsonObject kfObj = kfEl.getAsJsonObject();
                                String channel = kfObj.has("channel") ? kfObj.get("channel").getAsString().toLowerCase() : "rotation";
                                double time = kfObj.has("time") ? kfObj.get("time").getAsDouble() : 0.0;
                                String interp = kfObj.has("interpolation") ? kfObj.get("interpolation").getAsString() : "linear";

                                Vector3f val = new Vector3f(0, 0, 0);
                                if (kfObj.has("data_points")) {
                                    JsonArray dp = kfObj.getAsJsonArray("data_points");
                                    if (!dp.isEmpty() && dp.get(0).isJsonObject()) {
                                        JsonObject p = dp.get(0).getAsJsonObject();
                                        float x = parseFlexibleFloat(p.get("x"));
                                        float y = parseFlexibleFloat(p.get("y"));
                                        float z = parseFlexibleFloat(p.get("z"));
                                        val.set(x, y, z);
                                    }
                                }

                                BlockbenchModel.Keyframe kf = new BlockbenchModel.Keyframe(time, channel, val, interp);
                                switch (channel) {
                                    case "position" -> timeline.getPositionKeys().add(kf);
                                    case "rotation" -> timeline.getRotationKeys().add(kf);
                                    case "scale" -> timeline.getScaleKeys().add(kf);
                                }
                            }
                        }

                        anim.getTimelines().put(boneName, timeline);
                        anim.getTimelines().put(boneUuidOrId, timeline);
                    }
                }

                model.getAnimations().put(name, anim);
            }
        }

        LOGGER.info("Parsed Blockbench model '{}': {} bones, {} animations", modelId, model.getBones().size(), model.getAnimations().size());
        return model;
    }

    private static void parseOutliner(JsonArray array, BlockbenchModel.BoneDef parent,
                                      Map<String, BlockbenchModel.BoneDef> boneUuidMap,
                                      Map<String, BlockbenchModel.CubeDef> cubeMap,
                                      BlockbenchModel model) {
        for (JsonElement el : array) {
            if (el.isJsonPrimitive()) {
                // String uuid of a cube element
                String cubeUuid = el.getAsString();
                BlockbenchModel.CubeDef cube = cubeMap.get(cubeUuid);
                if (cube != null && parent != null) {
                    parent.getCubes().add(cube);
                }
            } else if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                String uuid = obj.has("uuid") ? obj.get("uuid").getAsString() : "";
                BlockbenchModel.BoneDef bone = boneUuidMap.get(uuid);
                if (bone == null && !uuid.isBlank()) {
                    String rawName = obj.has("name") ? obj.get("name").getAsString() : null;
                    String name = (rawName != null && !rawName.isBlank()) ? rawName : uuid;
                    Vector3f origin = obj.has("origin") ? parseVector3f(obj.getAsJsonArray("origin")) : new Vector3f(0, 0, 0);
                    Vector3f rotation = obj.has("rotation") ? parseVector3f(obj.getAsJsonArray("rotation")) : new Vector3f(0, 0, 0);
                    bone = new BlockbenchModel.BoneDef(uuid, name, origin, rotation);
                    model.getBones().put(name, bone);
                    model.getBones().put(uuid, bone);
                    boneUuidMap.put(uuid, bone);
                }
                if (bone != null) {
                    if (parent != null) {
                        bone.setParent(parent);
                        parent.getChildren().add(bone);
                    } else {
                        model.addRootBone(bone);
                    }

                    if (obj.has("children")) {
                        parseOutliner(obj.getAsJsonArray("children"), bone, boneUuidMap, cubeMap, model);
                    }
                }
            }
        }
    }

    private static Vector3f parseVector3f(JsonArray array) {
        if (array == null || array.size() < 3) return new Vector3f(0, 0, 0);
        return new Vector3f(
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat()
        );
    }

    private static float parseFlexibleFloat(JsonElement el) {
        if (el == null || el.isJsonNull()) return 0f;
        try {
            return el.getAsFloat();
        } catch (Exception e) {
            try {
                return Float.parseFloat(el.getAsString());
            } catch (Exception ignored) {
                return 0f;
            }
        }
    }
}
