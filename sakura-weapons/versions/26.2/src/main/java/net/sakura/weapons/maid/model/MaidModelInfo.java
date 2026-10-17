package net.sakura.weapons.maid.model;

import net.minecraft.resources.Identifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MaidModelInfo {
    private final String modelId;
    private String name;
    private final List<String> description = new ArrayList<>();
    private Identifier modelLocation;
    private Identifier textureLocation;
    private boolean isExternal = false;
    private Path externalModelPath;
    private Path externalTexturePath;
    private String author = "";
    private boolean isCustom = false;

    public MaidModelInfo(String modelId, String name) {
        this.modelId = modelId;
        this.name = name;
    }

    public String getAuthor() {
        return author != null ? author : "";
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public boolean isCustom() {
        return isCustom || isExternal;
    }

    public void setCustom(boolean custom) {
        this.isCustom = custom;
    }

    public String getModelId() {
        return modelId;
    }

    public String getName() {
        return name != null && !name.isBlank() ? name : modelId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getDescription() {
        return description;
    }

    public Identifier getModelLocation() {
        return modelLocation;
    }

    public void setModelLocation(Identifier modelLocation) {
        this.modelLocation = modelLocation;
    }

    public Identifier getTextureLocation() {
        return textureLocation;
    }

    public void setTextureLocation(Identifier textureLocation) {
        this.textureLocation = textureLocation;
    }

    public boolean isExternal() {
        return isExternal;
    }

    public void setExternal(boolean external) {
        isExternal = external;
    }

    public Path getExternalModelPath() {
        return externalModelPath;
    }

    public void setExternalModelPath(Path externalModelPath) {
        this.externalModelPath = externalModelPath;
    }

    public Path getExternalTexturePath() {
        return externalTexturePath;
    }

    public void setExternalTexturePath(Path externalTexturePath) {
        this.externalTexturePath = externalTexturePath;
    }
}
