package net.sakura.weapons.maid.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.sakura.weapons.SakuraWeaponsMod;
import net.sakura.weapons.client.render.bedrock.model.BedrockModel;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class MaidModelRegistry {
    public static final String DEFAULT_MODEL_ID = "touhou_little_maid:hakurei_reimu";
    private static final Map<String, MaidModelInfo> REGISTERED_MODELS = new LinkedHashMap<>();
    private static final Map<String, BedrockModel> MODEL_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Identifier> TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    public static synchronized void initialize() {
        if (initialized) return;
        reloadModels();
        initialized = true;
    }

    public static synchronized void reloadModels() {
        REGISTERED_MODELS.clear();
        MODEL_CACHE.clear();
        TEXTURE_CACHE.clear();

        SakuraWeaponsMod.LOGGER.info("Starting Maid Model Registry scan...");

        // 1. Load internal Touhou Little Maid models from manifest
        loadInternalManifest();

        // 2. Setup and scan external directories (.minecraft/maid_models and .minecraft/tlm_custom_pack)
        setupExternalDirectories();
        scanExternalFolder("maid_models");
        scanExternalFolder("tlm_custom_pack");

        SakuraWeaponsMod.LOGGER.info("Maid Model Registry loaded {} models successfully.", REGISTERED_MODELS.size());
    }

    private static void loadInternalManifest() {
        String manifestPath = "/assets/touhou_little_maid/tlm_custom_pack/touhou_little_maid-1.0.0/assets/touhou_little_maid/maid_model.json";
        try (InputStream stream = MaidModelRegistry.class.getResourceAsStream(manifestPath)) {
            if (stream == null) {
                SakuraWeaponsMod.LOGGER.warn("Internal maid manifest not found at {}", manifestPath);
                registerFallbackReimu();
                return;
            }
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.has("model_list") && json.get("model_list").isJsonArray()) {
                JsonArray list = json.getAsJsonArray("model_list");
                for (JsonElement element : list) {
                    if (!element.isJsonObject()) continue;
                    JsonObject obj = element.getAsJsonObject();
                    if (!obj.has("model_id")) continue;

                    String modelId = obj.get("model_id").getAsString();
                    String pathPart = modelId.contains(":") ? modelId.split(":", 2)[1] : modelId;
                    String formattedName = formatName(pathPart);

                    if (obj.has("name")) {
                        String rawName = obj.get("name").getAsString();
                        if (!rawName.startsWith("{")) {
                            formattedName = rawName;
                        }
                    }

                    MaidModelInfo info = new MaidModelInfo(modelId, formattedName);

                    String modelLocStr = obj.has("model") ? obj.get("model").getAsString() : "touhou_little_maid:models/entity/" + pathPart + ".json";
                    info.setModelLocation(parseIdentifier(modelLocStr));

                    String textureLocStr = "touhou_little_maid:textures/entity/" + pathPart + ".png";
                    info.setTextureLocation(parseIdentifier(textureLocStr));

                    if (obj.has("description") && obj.get("description").isJsonArray()) {
                        for (JsonElement descEl : obj.getAsJsonArray("description")) {
                            info.getDescription().add(descEl.getAsString());
                        }
                    }

                    REGISTERED_MODELS.put(modelId, info);
                }
            }
        } catch (Exception e) {
            SakuraWeaponsMod.LOGGER.error("Error reading internal maid manifest", e);
            registerFallbackReimu();
        }
    }

    private static void registerFallbackReimu() {
        MaidModelInfo reimu = new MaidModelInfo(DEFAULT_MODEL_ID, "Hakurei Reimu");
        reimu.setModelLocation(Identifier.fromNamespaceAndPath("touhou_little_maid", "models/entity/hakurei_reimu.json"));
        reimu.setTextureLocation(Identifier.fromNamespaceAndPath("touhou_little_maid", "textures/entity/hakurei_reimu.png"));
        REGISTERED_MODELS.put(DEFAULT_MODEL_ID, reimu);
    }

    private static void setupExternalDirectories() {
        try {
            Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
            Path maidModelsDir = gameDir.resolve("maid_models");
            Path tlmPacksDir = gameDir.resolve("tlm_custom_pack");

            if (!Files.exists(maidModelsDir)) {
                Files.createDirectories(maidModelsDir);
                Path readme = maidModelsDir.resolve("README_CARA_TAMBAH_MODEL.txt");
                if (!Files.exists(readme)) {
                    String guide = """
================================================================================
  PANDUAN MENAMBAHKAN MODEL MAID CUSTOM (TAKASHA COMPANION 26.2)
================================================================================

Folder ini digunakan untuk menambahkan model karakter Maid Bedrock secara langsung
tanpa perlu install mod tambahan atau restart game!

CARA 1: MENAMBAHKAN FILE LANGSUNG (.JSON + .PNG)
1. Siapkan file model format Bedrock Geometry (.json) dan tekstur (.png).
2. Pastikan kedua file memiliki NAMA YANG SAMA, contoh:
   - remilia.json
   - remilia.png
3. Masukkan kedua file tersebut ke dalam folder ini (maid_models).
4. Di dalam game Minecraft, tekan [ K ] untuk membuka Master Dashboard,
   pilih menu Maid Companion, lalu klik [ 🔄 Reload Model ].
5. Model langsung muncul di galeri dan siap digunakan!

CARA 2: MEMASUKKAN FOLDER PACK / TLM CUSTOM PACK
Jika kamu memiliki pack Touhou Little Maid utuh (.zip atau folder):
- Ekstrak pack tersebut atau masukkan foldernya ke dalam:
  .minecraft/maid_models/ atau .minecraft/tlm_custom_pack/

TOMBOL CEPAT DI IN-GAME:
- Tekan tombol [ 📂 Buka Folder Model ] di menu Maid untuk membuka folder ini.
- Tekan tombol [ 🔄 Reload Model ] untuk menerapkan perubahan seketika.
================================================================================
""";
                    Files.writeString(readme, guide, StandardCharsets.UTF_8);
                }
            }

            if (!Files.exists(tlmPacksDir)) {
                Files.createDirectories(tlmPacksDir);
            }
        } catch (Exception e) {
            SakuraWeaponsMod.LOGGER.error("Failed to setup external maid directories", e);
        }
    }

    private static void scanExternalFolder(String folderName) {
        try {
            Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve(folderName);
            if (!Files.exists(dir) || !Files.isDirectory(dir)) return;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry)) {
                        scanExternalPackSubdir(entry);
                    } else if (Files.isRegularFile(entry) && entry.toString().toLowerCase().endsWith(".json") && !entry.getFileName().toString().equalsIgnoreCase("maid_model.json")) {
                        // Direct json + png pair in root folder
                        String baseName = getBaseName(entry.getFileName().toString());
                        Path pngFile = dir.resolve(baseName + ".png");
                        if (Files.exists(pngFile)) {
                            registerDirectExternalModel(baseName, entry, pngFile);
                        }
                    }
                }
            }
        } catch (Exception e) {
            SakuraWeaponsMod.LOGGER.error("Error scanning external folder: " + folderName, e);
        }
    }

    private static void scanExternalPackSubdir(Path subDir) {
        try {
            // Check if directory contains a maid_model.json manifest
            Path manifest = subDir.resolve("assets").resolve("touhou_little_maid").resolve("maid_model.json");
            if (!Files.exists(manifest)) {
                manifest = subDir.resolve("maid_model.json");
            }

            if (Files.exists(manifest)) {
                loadExternalManifest(manifest, subDir);
                return;
            }

            // Otherwise, scan for any pair of .json + .png inside this folder
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(subDir)) {
                for (Path file : stream) {
                    if (Files.isRegularFile(file) && file.toString().toLowerCase().endsWith(".json")) {
                        String baseName = getBaseName(file.getFileName().toString());
                        Path pngFile = subDir.resolve(baseName + ".png");
                        if (Files.exists(pngFile)) {
                            registerDirectExternalModel(subDir.getFileName().toString() + "_" + baseName, file, pngFile);
                        }
                    }
                }
            }
        } catch (Exception e) {
            SakuraWeaponsMod.LOGGER.error("Error scanning pack subdirectory: " + subDir, e);
        }
    }

    private static void loadExternalManifest(Path manifest, Path packRoot) {
        try (InputStream stream = Files.newInputStream(manifest)) {
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.has("model_list") && json.get("model_list").isJsonArray()) {
                for (JsonElement el : json.getAsJsonArray("model_list")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();
                    if (!obj.has("model_id")) continue;

                    String modelId = obj.get("model_id").getAsString();
                    String pathPart = modelId.contains(":") ? modelId.split(":", 2)[1] : modelId;
                    String name = obj.has("name") ? obj.get("name").getAsString() : formatName(pathPart);

                    Path modelFile = packRoot.resolve("assets").resolve("touhou_little_maid").resolve("models").resolve("entity").resolve(pathPart + ".json");
                    Path textureFile = packRoot.resolve("assets").resolve("touhou_little_maid").resolve("textures").resolve("entity").resolve(pathPart + ".png");

                    if (!Files.exists(modelFile)) {
                        modelFile = packRoot.resolve(pathPart + ".json");
                    }
                    if (!Files.exists(textureFile)) {
                        textureFile = packRoot.resolve(pathPart + ".png");
                    }

                    if (Files.exists(modelFile) && Files.exists(textureFile)) {
                        registerDirectExternalModel("custom:" + pathPart, modelFile, textureFile);
                    }
                }
            }
        } catch (Exception e) {
            SakuraWeaponsMod.LOGGER.error("Failed to load external manifest at " + manifest, e);
        }
    }

    private static void registerDirectExternalModel(String id, Path jsonPath, Path pngPath) {
        String sanitizedId = "custom:" + id.toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
        MaidModelInfo info = new MaidModelInfo(sanitizedId, formatName(id));
        info.setExternal(true);
        info.setExternalModelPath(jsonPath);
        info.setExternalTexturePath(pngPath);

        // Register dynamic texture
        Identifier textureId = Identifier.fromNamespaceAndPath("sakura_custom", sanitizedId.replace(":", "_"));
        try {
            byte[] bytes = Files.readAllBytes(pngPath);
            NativeImage image = NativeImage.read(bytes);
            Minecraft.getInstance().execute(() -> {
                DynamicTexture texture = new DynamicTexture(() -> "Maid Texture " + sanitizedId, image);
                texture.upload();
                Minecraft.getInstance().getTextureManager().register(textureId, texture);
            });
            info.setTextureLocation(textureId);
            TEXTURE_CACHE.put(sanitizedId, textureId);
        } catch (Exception e) {
            SakuraWeaponsMod.LOGGER.error("Failed to load external texture for " + id, e);
            info.setTextureLocation(Identifier.fromNamespaceAndPath("touhou_little_maid", "textures/entity/hakurei_reimu.png"));
        }

        REGISTERED_MODELS.put(sanitizedId, info);
        SakuraWeaponsMod.LOGGER.info("Registered external maid model: {}", sanitizedId);
    }

    public static BedrockModel getModel(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            modelId = DEFAULT_MODEL_ID;
        }

        return MODEL_CACHE.computeIfAbsent(modelId, id -> {
            MaidModelInfo info = REGISTERED_MODELS.get(id);
            if (info == null) {
                info = REGISTERED_MODELS.get(DEFAULT_MODEL_ID);
            }
            if (info == null) {
                return new BedrockModel();
            }

            if (info.isExternal() && info.getExternalModelPath() != null) {
                try (InputStream in = Files.newInputStream(info.getExternalModelPath())) {
                    return BedrockModel.fromInputStream(in);
                } catch (Exception e) {
                    SakuraWeaponsMod.LOGGER.error("Failed to load external model file: " + info.getExternalModelPath(), e);
                }
            } else if (info.getModelLocation() != null) {
                String resourcePath = "/assets/" + info.getModelLocation().getNamespace() + "/" + info.getModelLocation().getPath();
                // In shared-resources, models might be inside tlm_custom_pack folder
                InputStream in = MaidModelRegistry.class.getResourceAsStream(resourcePath);
                if (in == null) {
                    String altPath = "/assets/touhou_little_maid/tlm_custom_pack/touhou_little_maid-1.0.0/assets/"
                            + info.getModelLocation().getNamespace() + "/" + info.getModelLocation().getPath();
                    in = MaidModelRegistry.class.getResourceAsStream(altPath);
                }
                if (in != null) {
                    try (InputStream is = in) {
                        return BedrockModel.fromInputStream(is);
                    } catch (Exception e) {
                        SakuraWeaponsMod.LOGGER.error("Error loading model resource: " + resourcePath, e);
                    }
                }
            }

            // Fallback
            return new BedrockModel();
        });
    }

    public static Identifier getTexture(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            modelId = DEFAULT_MODEL_ID;
        }

        Identifier cached = TEXTURE_CACHE.get(modelId);
        if (cached != null) return cached;

        MaidModelInfo info = REGISTERED_MODELS.get(modelId);
        if (info != null && info.getTextureLocation() != null) {
            TEXTURE_CACHE.put(modelId, info.getTextureLocation());
            return info.getTextureLocation();
        }

        return Identifier.fromNamespaceAndPath("touhou_little_maid", "textures/entity/hakurei_reimu.png");
    }

    public static List<MaidModelInfo> getAllModels() {
        return new ArrayList<>(REGISTERED_MODELS.values());
    }

    public static MaidModelInfo getModelInfo(String modelId) {
        return REGISTERED_MODELS.get(modelId);
    }

    private static Identifier parseIdentifier(String str) {
        if (str.contains(":")) {
            String[] parts = str.split(":", 2);
            return Identifier.fromNamespaceAndPath(parts[0], parts[1]);
        }
        return Identifier.fromNamespaceAndPath("touhou_little_maid", str);
    }

    private static String getBaseName(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    public static String formatName(String id) {
        String clean = id.replace("touhou_little_maid:", "")
                .replace("custom:", "")
                .replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : clean.split("\\s+")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}
