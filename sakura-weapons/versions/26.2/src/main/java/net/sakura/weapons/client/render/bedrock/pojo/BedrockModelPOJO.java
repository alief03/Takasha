package net.sakura.weapons.client.render.bedrock.pojo;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.Map;

@JsonAdapter(BedrockModelPOJO.Deserializer.class)
public class BedrockModelPOJO {
    private String formatVersion = "1.10.0";
    private GeometryModelLegacy geometryModelLegacy;
    private GeometryModelNew geometryModelNew;

    public String getFormatVersion() {
        return formatVersion;
    }

    public GeometryModelLegacy getGeometryModelLegacy() {
        return geometryModelLegacy;
    }

    public GeometryModelNew getGeometryModelNew() {
        return geometryModelNew;
    }

    public boolean isLegacy() {
        return geometryModelLegacy != null;
    }

    public static class Deserializer implements JsonDeserializer<BedrockModelPOJO> {
        @Override
        public BedrockModelPOJO deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonObject()) {
                return new BedrockModelPOJO();
            }
            JsonObject root = json.getAsJsonObject();
            BedrockModelPOJO pojo = new BedrockModelPOJO();

            if (root.has("format_version")) {
                pojo.formatVersion = root.get("format_version").getAsString();
            }

            if (root.has("minecraft:geometry")) {
                JsonElement geoElem = root.get("minecraft:geometry");
                if (geoElem.isJsonArray() && geoElem.getAsJsonArray().size() > 0) {
                    pojo.geometryModelNew = context.deserialize(geoElem.getAsJsonArray().get(0), GeometryModelNew.class);
                } else if (geoElem.isJsonObject()) {
                    pojo.geometryModelNew = context.deserialize(geoElem, GeometryModelNew.class);
                }
            } else {
                for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                    if (entry.getKey().startsWith("geometry.") && entry.getValue().isJsonObject()) {
                        pojo.geometryModelLegacy = context.deserialize(entry.getValue(), GeometryModelLegacy.class);
                        break;
                    }
                }
            }

            return pojo;
        }
    }
}
