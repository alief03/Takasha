package net.sakura.weapons.client.render.bedrock.pojo;

import com.google.gson.annotations.SerializedName;

public class GeometryModelLegacy {
    @SerializedName("texturewidth")
    private int textureWidth = 64;

    @SerializedName("textureheight")
    private int textureHeight = 64;

    @SerializedName("visible_bounds_width")
    private float visibleBoundsWidth = 1.0f;

    @SerializedName("visible_bounds_height")
    private float visibleBoundsHeight = 2.0f;

    @SerializedName("visible_bounds_offset")
    private float[] visibleBoundsOffset = new float[]{0, 0, 0};

    @SerializedName("bones")
    private BonesItem[] bones;

    public int getTextureWidth() {
        return textureWidth > 0 ? textureWidth : 64;
    }

    public int getTextureHeight() {
        return textureHeight > 0 ? textureHeight : 64;
    }

    public float getVisibleBoundsWidth() {
        return visibleBoundsWidth;
    }

    public float getVisibleBoundsHeight() {
        return visibleBoundsHeight;
    }

    public float[] getVisibleBoundsOffset() {
        return visibleBoundsOffset != null ? visibleBoundsOffset : new float[]{0, 0, 0};
    }

    public BonesItem[] getBones() {
        return bones != null ? bones : new BonesItem[0];
    }
}
