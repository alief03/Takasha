package net.sakura.touhou.compat.ysm;

public record YsmMaidInfo(boolean isYsmModel, String modelId, String textureId, String name) {
    public static final YsmMaidInfo EMPTY = new YsmMaidInfo(false, "", "", "");
}
