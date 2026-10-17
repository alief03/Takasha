package net.sakura.weapons.client.render.bedrock.pojo;

import com.google.gson.annotations.SerializedName;

public class GeometryModelNew {
    @SerializedName("description")
    private Description description;

    @SerializedName("bones")
    private BonesItem[] bones;

    public Description getDescription() {
        return description != null ? description : new Description();
    }

    public void setDescription(Description description) {
        this.description = description;
    }

    public BonesItem[] getBones() {
        return bones != null ? bones : new BonesItem[0];
    }

    public void setBones(BonesItem[] bones) {
        this.bones = bones;
    }
}
