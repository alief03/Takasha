package net.sakura.weapons.client.render.bedrock.pojo;

import com.google.gson.annotations.SerializedName;

public class FaceItem {
    @SerializedName("uv")
    private float[] uv = new float[]{0, 0};

    @SerializedName("uv_size")
    private float[] uvSize = new float[]{0, 0};

    public float[] getUv() {
        return uv;
    }

    public void setUv(float[] uv) {
        this.uv = uv;
    }

    public float[] getUvSize() {
        return uvSize;
    }

    public void setUvSize(float[] uvSize) {
        this.uvSize = uvSize;
    }
}
