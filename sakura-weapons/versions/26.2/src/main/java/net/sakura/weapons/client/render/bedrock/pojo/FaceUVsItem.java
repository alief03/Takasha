package net.sakura.weapons.client.render.bedrock.pojo;

import com.google.gson.annotations.SerializedName;

public class FaceUVsItem {
    @SerializedName("north")
    private FaceItem north;

    @SerializedName("south")
    private FaceItem south;

    @SerializedName("east")
    private FaceItem east;

    @SerializedName("west")
    private FaceItem west;

    @SerializedName("up")
    private FaceItem up;

    @SerializedName("down")
    private FaceItem down;

    public FaceItem getNorth() {
        return north;
    }

    public FaceItem getSouth() {
        return south;
    }

    public FaceItem getEast() {
        return east;
    }

    public FaceItem getWest() {
        return west;
    }

    public FaceItem getUp() {
        return up;
    }

    public FaceItem getDown() {
        return down;
    }

    public FaceItem getFace(int faceIndex) {
        return switch (faceIndex) {
            case 0 -> up;
            case 1 -> down;
            case 2 -> north;
            case 3 -> south;
            case 4 -> west;
            case 5 -> east;
            default -> null;
        };
    }
}
