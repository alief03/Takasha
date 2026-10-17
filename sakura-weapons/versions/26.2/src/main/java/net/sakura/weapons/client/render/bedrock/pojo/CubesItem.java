package net.sakura.weapons.client.render.bedrock.pojo;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;

@JsonAdapter(CubesItem.Deserializer.class)
public class CubesItem {
    @SerializedName("origin")
    private float[] origin = new float[]{0, 0, 0};

    @SerializedName("size")
    private float[] size = new float[]{0, 0, 0};

    @SerializedName("pivot")
    private float[] pivot;

    @SerializedName("rotation")
    private float[] rotation;

    @SerializedName("uv")
    private float[] uv = new float[]{0, 0};

    @SerializedName("face_uv")
    private FaceUVsItem faceUv;

    @SerializedName("inflate")
    private float inflate = 0.0f;

    @SerializedName("mirror")
    private boolean mirror = false;

    public float[] getOrigin() {
        return origin;
    }

    public void setOrigin(float[] origin) {
        this.origin = origin;
    }

    public float[] getSize() {
        return size;
    }

    public void setSize(float[] size) {
        this.size = size;
    }

    public float[] getPivot() {
        return pivot;
    }

    public void setPivot(float[] pivot) {
        this.pivot = pivot;
    }

    public float[] getRotation() {
        return rotation;
    }

    public void setRotation(float[] rotation) {
        this.rotation = rotation;
    }

    public float[] getUv() {
        return uv;
    }

    public void setUv(float[] uv) {
        this.uv = uv;
    }

    public FaceUVsItem getFaceUv() {
        return faceUv;
    }

    public void setFaceUv(FaceUVsItem faceUv) {
        this.faceUv = faceUv;
    }

    public float getInflate() {
        return inflate;
    }

    public void setInflate(float inflate) {
        this.inflate = inflate;
    }

    public boolean isMirror() {
        return mirror;
    }

    public void setMirror(boolean mirror) {
        this.mirror = mirror;
    }

    public static class Deserializer implements JsonDeserializer<CubesItem> {
        @Override
        public CubesItem deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonObject()) {
                return new CubesItem();
            }
            JsonObject obj = json.getAsJsonObject();
            CubesItem item = new CubesItem();

            if (obj.has("origin") && obj.get("origin").isJsonArray()) {
                JsonArray arr = obj.getAsJsonArray("origin");
                item.origin = new float[]{
                    arr.size() > 0 ? arr.get(0).getAsFloat() : 0,
                    arr.size() > 1 ? arr.get(1).getAsFloat() : 0,
                    arr.size() > 2 ? arr.get(2).getAsFloat() : 0
                };
            }

            if (obj.has("size") && obj.get("size").isJsonArray()) {
                JsonArray arr = obj.getAsJsonArray("size");
                item.size = new float[]{
                    arr.size() > 0 ? arr.get(0).getAsFloat() : 0,
                    arr.size() > 1 ? arr.get(1).getAsFloat() : 0,
                    arr.size() > 2 ? arr.get(2).getAsFloat() : 0
                };
            }

            if (obj.has("pivot") && obj.get("pivot").isJsonArray()) {
                JsonArray arr = obj.getAsJsonArray("pivot");
                item.pivot = new float[]{
                    arr.size() > 0 ? arr.get(0).getAsFloat() : 0,
                    arr.size() > 1 ? arr.get(1).getAsFloat() : 0,
                    arr.size() > 2 ? arr.get(2).getAsFloat() : 0
                };
            }

            if (obj.has("rotation") && obj.get("rotation").isJsonArray()) {
                JsonArray arr = obj.getAsJsonArray("rotation");
                item.rotation = new float[]{
                    arr.size() > 0 ? arr.get(0).getAsFloat() : 0,
                    arr.size() > 1 ? arr.get(1).getAsFloat() : 0,
                    arr.size() > 2 ? arr.get(2).getAsFloat() : 0
                };
            }

            if (obj.has("uv")) {
                JsonElement uvElem = obj.get("uv");
                if (uvElem.isJsonArray()) {
                    JsonArray arr = uvElem.getAsJsonArray();
                    item.uv = new float[]{
                        arr.size() > 0 ? arr.get(0).getAsFloat() : 0,
                        arr.size() > 1 ? arr.get(1).getAsFloat() : 0
                    };
                } else if (uvElem.isJsonObject()) {
                    item.faceUv = context.deserialize(uvElem, FaceUVsItem.class);
                }
            }

            if (obj.has("inflate")) {
                item.inflate = obj.get("inflate").getAsFloat();
            }

            if (obj.has("mirror")) {
                item.mirror = obj.get("mirror").getAsBoolean();
            }

            return item;
        }
    }
}
