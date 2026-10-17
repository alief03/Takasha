package net.sakura.weapons.client.render.bedrock.pojo;

import com.google.gson.annotations.SerializedName;

public class Description {
    @SerializedName("identifier")
    private String identifier;

    @SerializedName("texture_width")
    private int textureWidth = 64;

    @SerializedName("texture_height")
    private int textureHeight = 64;

    @SerializedName("visible_bounds_width")
    private float visibleBoundsWidth = 1.0f;

    @SerializedName("visible_bounds_height")
    private float visibleBoundsHeight = 2.0f;

    @SerializedName("visible_bounds_offset")
    private float[] visibleBoundsOffset = new float[]{0, 0, 0};

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public int getTextureWidth() {
        return textureWidth > 0 ? textureWidth : 64;
    }

    public void setTextureWidth(int textureWidth) {
        this.textureWidth = textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight > 0 ? textureHeight : 64;
    }

    public void setTextureHeight(int textureHeight) {
        this.textureHeight = textureHeight;
    }

    public float getVisibleBoundsWidth() {
        return visibleBoundsWidth;
    }

    public void setVisibleBoundsWidth(float visibleBoundsWidth) {
        this.visibleBoundsWidth = visibleBoundsWidth;
    }

    public float getVisibleBoundsHeight() {
        return visibleBoundsHeight;
    }

    public void setVisibleBoundsHeight(float visibleBoundsHeight) {
        this.visibleBoundsHeight = visibleBoundsHeight;
    }

    public float[] getVisibleBoundsOffset() {
        return visibleBoundsOffset != null ? visibleBoundsOffset : new float[]{0, 0, 0};
    }

    public void setVisibleBoundsOffset(float[] visibleBoundsOffset) {
        this.visibleBoundsOffset = visibleBoundsOffset;
    }
}
