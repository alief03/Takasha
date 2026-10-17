package net.sakura.weapons.client.render.bedrock.pojo;

import com.google.gson.annotations.SerializedName;

public class BonesItem {
    @SerializedName("name")
    private String name = "";

    @SerializedName("parent")
    private String parent;

    @SerializedName("pivot")
    private float[] pivot = new float[]{0, 0, 0};

    @SerializedName("rotation")
    private float[] rotation;

    @SerializedName("cubes")
    private CubesItem[] cubes;

    @SerializedName("mirror")
    private boolean mirror = false;

    @SerializedName("neverRender")
    private boolean neverRender = false;

    @SerializedName("inflate")
    private float inflate = 0.0f;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public float[] getPivot() {
        return pivot != null ? pivot : new float[]{0, 0, 0};
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

    public CubesItem[] getCubes() {
        return cubes;
    }

    public void setCubes(CubesItem[] cubes) {
        this.cubes = cubes;
    }

    public boolean isMirror() {
        return mirror;
    }

    public void setMirror(boolean mirror) {
        this.mirror = mirror;
    }

    public boolean isNeverRender() {
        return neverRender;
    }

    public void setNeverRender(boolean neverRender) {
        this.neverRender = neverRender;
    }

    public float getInflate() {
        return inflate;
    }

    public void setInflate(float inflate) {
        this.inflate = inflate;
    }
}
