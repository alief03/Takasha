package net.sakura.weapons.client.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.sakura.weapons.SakuraWeaponsMod;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dynamic Asynchronous Skin Loader for Takasha NPCs with Disk & Memory Caching.
 * Supports online player lookup (0ms), PlayerDB API, MineSkin REST API, NameMC, Imgur, and direct URLs.
 */
@Environment(EnvType.CLIENT)
public class NpcSkinManager {

    public record SkinResult(Identifier textureLocation, PlayerModelType modelType) {}

    public static class ResolvedSkinInfo {
        public final String directImageUrl;
        public final PlayerModelType modelType;

        public ResolvedSkinInfo(String directImageUrl, PlayerModelType modelType) {
            this.directImageUrl = directImageUrl;
            this.modelType = modelType;
        }
    }

    private static final ExecutorService DOWNLOAD_POOL = Executors.newFixedThreadPool(6, r -> {
        Thread thread = new Thread(r, "Takasha-Skin-Downloader");
        thread.setDaemon(true);
        return thread;
    });

    private static final Map<String, CompletableFuture<SkinResult>> IN_FLIGHT = new ConcurrentHashMap<>();
    private static final Map<String, SkinResult> CACHE = new ConcurrentHashMap<>();

    private static final int MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB max
    private static final int TIMEOUT_CONNECT_MS = 3500;
    private static final int TIMEOUT_READ_MS = 5000;

    public static Identifier getSkinTexture(String input) {
        if (input == null || input.isBlank()) {
            return DefaultPlayerSkin.getDefaultTexture();
        }

        String key = input.trim();
        SkinResult result = CACHE.get(key);
        if (result != null) {
            return result.textureLocation();
        }

        // Check if currently connected online player
        SkinResult onlineResult = getOnlinePlayerSkin(key);
        if (onlineResult != null) {
            CACHE.put(key, onlineResult);
            return onlineResult.textureLocation();
        }

        // Trigger background download
        fetchSkinAsync(key);

        return DefaultPlayerSkin.getDefaultTexture();
    }

    public static PlayerModelType getSkinModel(String input, String fallbackModelName) {
        if (input != null && !input.isBlank()) {
            String key = input.trim();
            SkinResult result = CACHE.get(key);
            if (result != null) {
                return result.modelType();
            }
            SkinResult onlineResult = getOnlinePlayerSkin(key);
            if (onlineResult != null) {
                return onlineResult.modelType();
            }
            // Fast disk cache meta check (instant offline / world load resolution)
            try {
                String keyHash = computeSha256(key.toLowerCase());
                Path cacheDir = Minecraft.getInstance().gameDirectory.toPath().resolve("takasha_cache").resolve("skins");
                Path keyMetaFile = cacheDir.resolve("key_" + keyHash + ".meta");
                if (Files.exists(keyMetaFile)) {
                    String meta = Files.readString(keyMetaFile, StandardCharsets.UTF_8).trim();
                    return "slim".equalsIgnoreCase(meta) ? PlayerModelType.SLIM : PlayerModelType.WIDE;
                }
            } catch (Throwable ignored) {}
        }

        if ("slim".equalsIgnoreCase(fallbackModelName)) {
            return PlayerModelType.SLIM;
        }
        return PlayerModelType.WIDE;
    }

    public static CompletableFuture<SkinResult> fetchSkinAsync(String input) {
        if (input == null) {
            return CompletableFuture.completedFuture(new SkinResult(
                DefaultPlayerSkin.getDefaultTexture(),
                PlayerModelType.WIDE
            ));
        }

        String key = input.trim();
        if (key.length() < 3 && !key.startsWith("http")) {
            return CompletableFuture.completedFuture(new SkinResult(
                DefaultPlayerSkin.getDefaultTexture(),
                PlayerModelType.WIDE
            ));
        }

        SkinResult existing = CACHE.get(key);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }

        SkinResult onlineResult = getOnlinePlayerSkin(key);
        if (onlineResult != null) {
            CACHE.put(key, onlineResult);
            return CompletableFuture.completedFuture(onlineResult);
        }

        return IN_FLIGHT.computeIfAbsent(key, k -> CompletableFuture.supplyAsync(() -> loadSkinInternal(k), DOWNLOAD_POOL)
            .thenApply(res -> {
                IN_FLIGHT.remove(key);
                if (res != null && !res.textureLocation().equals(DefaultPlayerSkin.getDefaultTexture())) {
                    CACHE.put(key, res);
                }
                return res != null ? res : new SkinResult(DefaultPlayerSkin.getDefaultTexture(), PlayerModelType.WIDE);
            }).exceptionally(ex -> {
                IN_FLIGHT.remove(key);
                SakuraWeaponsMod.LOGGER.warn("Failed to load skin for '{}': {}", key, ex.getMessage());
                return new SkinResult(DefaultPlayerSkin.getDefaultTexture(), PlayerModelType.WIDE);
            }));
    }

    /**
     * Checks if the username matches an online player on the server/client (instant 0ms resolution).
     */
    private static SkinResult getOnlinePlayerSkin(String username) {
        try {
            if (Minecraft.getInstance().getConnection() != null) {
                PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfoIgnoreCase(username);
                if (info != null) {
                    PlayerSkin skin = info.getSkin();
                    if (skin != null && skin.body() != null && skin.body().id() != null) {
                        return new SkinResult(skin.body().id(), skin.model());
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static SkinResult loadSkinInternal(String key) {
        try {
            String trimmedKey = key.trim();
            String keyHash = computeSha256(trimmedKey.toLowerCase());
            Path cacheDir = Minecraft.getInstance().gameDirectory.toPath().resolve("takasha_cache").resolve("skins");
            Files.createDirectories(cacheDir);

            Path keyCachedFile = cacheDir.resolve("key_" + keyHash + ".png");
            Path keyMetaFile = cacheDir.resolve("key_" + keyHash + ".meta");

            // 1. Direct local disk cache check by key (Instant 0ms, offline support across reloads)
            if (Files.exists(keyCachedFile) && Files.size(keyCachedFile) > 0) {
                try {
                    byte[] pngBytes = Files.readAllBytes(keyCachedFile);
                    NativeImage image = NativeImage.read(pngBytes);
                    PlayerModelType modelType = PlayerModelType.WIDE;
                    if (Files.exists(keyMetaFile)) {
                        String meta = Files.readString(keyMetaFile, StandardCharsets.UTF_8).trim();
                        if ("slim".equalsIgnoreCase(meta)) {
                            modelType = PlayerModelType.SLIM;
                        }
                    } else {
                        modelType = isSlimModel(image) ? PlayerModelType.SLIM : PlayerModelType.WIDE;
                    }

                    Identifier textureId = Identifier.fromNamespaceAndPath(SakuraWeaponsMod.MOD_ID, "skin_" + keyHash.substring(0, 16));
                    CompletableFuture<Void> uploadFuture = new CompletableFuture<>();
                    Minecraft.getInstance().execute(() -> {
                        try {
                            DynamicTexture dynamicTexture = new DynamicTexture(() -> "Takasha Skin " + trimmedKey, image);
                            dynamicTexture.upload();
                            Minecraft.getInstance().getTextureManager().register(textureId, dynamicTexture);
                            uploadFuture.complete(null);
                        } catch (Exception e) {
                            uploadFuture.completeExceptionally(e);
                        }
                    });
                    uploadFuture.join();
                    SakuraWeaponsMod.LOGGER.info("Loaded custom skin for '{}' directly from disk cache ({})", trimmedKey, modelType);
                    return new SkinResult(textureId, modelType);
                } catch (Exception e) {
                    Files.deleteIfExists(keyCachedFile);
                    Files.deleteIfExists(keyMetaFile);
                }
            }

            // 2. Resolve via network/API if not yet in local key disk cache
            ResolvedSkinInfo resolved = resolveDirectSkinInfo(trimmedKey);
            if (resolved == null || resolved.directImageUrl == null || resolved.directImageUrl.isBlank()) {
                return null;
            }

            String downloadUrl = resolved.directImageUrl;
            String hash = computeSha256(downloadUrl);
            Path cachedFile = cacheDir.resolve(hash + ".png");

            byte[] pngBytes;
            if (Files.exists(cachedFile) && Files.size(cachedFile) > 0) {
                pngBytes = Files.readAllBytes(cachedFile);
            } else {
                pngBytes = downloadSkinBytes(downloadUrl);
                if (pngBytes == null || pngBytes.length == 0) {
                    return null;
                }
                Files.write(cachedFile, pngBytes);
            }

            // Parse and validate PNG using NativeImage
            NativeImage image;
            try {
                image = NativeImage.read(pngBytes);
            } catch (Exception e) {
                Files.deleteIfExists(cachedFile);
                throw e;
            }

            int width = image.getWidth();
            int height = image.getHeight();

            if (width != 64 || (height != 64 && height != 32)) {
                image.close();
                Files.deleteIfExists(cachedFile);
                SakuraWeaponsMod.LOGGER.warn("Skin '{}' has invalid dimensions {}x{}. Expected 64x64 or 64x32.", trimmedKey, width, height);
                return null;
            }

            PlayerModelType modelType = resolved.modelType != null ? resolved.modelType : (isSlimModel(image) ? PlayerModelType.SLIM : PlayerModelType.WIDE);

            // Write key-based disk cache and meta file for instant subsequent loads
            try {
                Files.write(keyCachedFile, pngBytes);
                Files.writeString(keyMetaFile, modelType == PlayerModelType.SLIM ? "slim" : "default", StandardCharsets.UTF_8);
            } catch (Throwable ignored) {}

            // Register DynamicTexture on main render thread
            Identifier textureId = Identifier.fromNamespaceAndPath(SakuraWeaponsMod.MOD_ID, "skin_" + keyHash.substring(0, 16));
            CompletableFuture<Void> uploadFuture = new CompletableFuture<>();

            Minecraft.getInstance().execute(() -> {
                try {
                    DynamicTexture dynamicTexture = new DynamicTexture(() -> "Takasha Skin " + trimmedKey, image);
                    dynamicTexture.upload();
                    Minecraft.getInstance().getTextureManager().register(textureId, dynamicTexture);
                    uploadFuture.complete(null);
                } catch (Exception e) {
                    uploadFuture.completeExceptionally(e);
                }
            });

            uploadFuture.join();
            SakuraWeaponsMod.LOGGER.info("Successfully loaded custom skin for '{}' ({})", trimmedKey, modelType);
            return new SkinResult(textureId, modelType);
        } catch (Exception e) {
            SakuraWeaponsMod.LOGGER.warn("Exception while processing skin for '{}': {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Smart resolver for MineSkin links, NameMC, Imgur, PlayerDB (username), and raw URLs.
     */
    private static ResolvedSkinInfo resolveDirectSkinInfo(String key) {
        String trimmed = key.trim();

        // 1. MineSkin URL or MineSkin UUID (e.g., https://minesk.in/671ed6bb03aa48c78b4368909ad574c2)
        String mineSkinUuid = null;
        if (trimmed.contains("minesk.in/") || trimmed.contains("mineskin.org/")) {
            String[] parts = trimmed.split("/");
            mineSkinUuid = parts[parts.length - 1];
        } else if (trimmed.matches("^[0-9a-fA-F]{32}$") || trimmed.matches("^[0-9a-fA-F-]{36}$")) {
            mineSkinUuid = trimmed;
        }

        if (mineSkinUuid != null && !mineSkinUuid.isBlank()) {
            try {
                String apiUrl = "https://api.mineskin.org/get/uuid/" + mineSkinUuid;
                byte[] jsonBytes = downloadSkinBytes(apiUrl);
                if (jsonBytes != null && jsonBytes.length > 0) {
                    String jsonStr = new String(jsonBytes, StandardCharsets.UTF_8);
                    JsonObject root = JsonParser.parseString(jsonStr).getAsJsonObject();

                    String directUrl = null;
                    if (root.has("data") && root.get("data").isJsonObject()) {
                        JsonObject data = root.getAsJsonObject("data");
                        if (data.has("texture") && data.get("texture").isJsonObject()) {
                            JsonObject texture = data.getAsJsonObject("texture");
                            if (texture.has("urls") && texture.get("urls").isJsonObject()) {
                                JsonObject urls = texture.getAsJsonObject("urls");
                                if (urls.has("skin") && !urls.get("skin").isJsonNull()) {
                                    directUrl = urls.get("skin").getAsString();
                                }
                            }
                            if (directUrl == null && texture.has("url") && !texture.get("url").isJsonNull()) {
                                directUrl = texture.get("url").getAsString();
                            }
                        }
                    }
                    if (directUrl == null && root.has("hash") && !root.get("hash").isJsonNull()) {
                        directUrl = "https://textures.minecraft.net/texture/" + root.get("hash").getAsString();
                    }

                    PlayerModelType type = null;
                    if (root.has("variant") && !root.get("variant").isJsonNull()) {
                        String var = root.get("variant").getAsString();
                        type = "slim".equalsIgnoreCase(var) ? PlayerModelType.SLIM : PlayerModelType.WIDE;
                    } else if (root.has("model") && !root.get("model").isJsonNull()) {
                        String mod = root.get("model").getAsString();
                        type = "slim".equalsIgnoreCase(mod) ? PlayerModelType.SLIM : PlayerModelType.WIDE;
                    }

                    if (directUrl != null && !directUrl.isBlank()) {
                        SakuraWeaponsMod.LOGGER.info("Resolved MineSkin '{}' to texture URL: {}", mineSkinUuid, directUrl);
                        return new ResolvedSkinInfo(directUrl, type);
                    }
                }
            } catch (Exception e) {
                SakuraWeaponsMod.LOGGER.warn("Failed resolving MineSkin UUID '{}': {}", mineSkinUuid, e.getMessage());
            }
        }

        // 2. NameMC Skin URL: namemc.com/skin/<hash>
        if (trimmed.contains("namemc.com/skin/")) {
            String[] parts = trimmed.split("/");
            String hash = parts[parts.length - 1];
            if (hash.matches("^[0-9a-fA-F]+$")) {
                return new ResolvedSkinInfo("https://textures.minecraft.net/texture/" + hash, null);
            }
        }

        // 3. Imgur Web URL: imgur.com/xyz -> i.imgur.com/xyz.png
        if (trimmed.contains("imgur.com/") && !trimmed.endsWith(".png") && !trimmed.endsWith(".jpg")) {
            String[] parts = trimmed.split("/");
            String id = parts[parts.length - 1];
            return new ResolvedSkinInfo("https://i.imgur.com/" + id + ".png", null);
        }

        // 4. PlayerDB API for Minecraft username (e.g. "Notch", "Dream", "Jeb_")
        if (trimmed.matches("^[a-zA-Z0-9_]{3,16}$")) {
            try {
                String apiUrl = "https://playerdb.co/api/player/minecraft/" + trimmed;
                byte[] jsonBytes = downloadSkinBytes(apiUrl);
                if (jsonBytes != null && jsonBytes.length > 0) {
                    String jsonStr = new String(jsonBytes, StandardCharsets.UTF_8);
                    JsonObject root = JsonParser.parseString(jsonStr).getAsJsonObject();
                    if (root.has("data") && root.get("data").isJsonObject()) {
                        JsonObject data = root.getAsJsonObject("data");
                        if (data.has("player") && data.get("player").isJsonObject()) {
                            JsonObject player = data.getAsJsonObject("player");
                            String skinUrl = null;
                            if (player.has("skin_texture") && !player.get("skin_texture").isJsonNull()) {
                                skinUrl = player.get("skin_texture").getAsString();
                            }

                            PlayerModelType modelType = null;
                            if (player.has("properties") && player.get("properties").isJsonArray()) {
                                for (var elem : player.getAsJsonArray("properties")) {
                                    if (elem.isJsonObject()) {
                                        JsonObject prop = elem.getAsJsonObject();
                                        if ("textures".equals(prop.get("name").getAsString()) && prop.has("value")) {
                                            String decoded = new String(Base64.getDecoder().decode(prop.get("value").getAsString()), StandardCharsets.UTF_8);
                                            JsonObject texRoot = JsonParser.parseString(decoded).getAsJsonObject();
                                            if (texRoot.has("textures") && texRoot.getAsJsonObject("textures").has("SKIN")) {
                                                JsonObject skinObj = texRoot.getAsJsonObject("textures").getAsJsonObject("SKIN");
                                                if (skinObj.has("metadata") && skinObj.getAsJsonObject("metadata").has("model")) {
                                                    String m = skinObj.getAsJsonObject("metadata").get("model").getAsString();
                                                    modelType = "slim".equalsIgnoreCase(m) ? PlayerModelType.SLIM : PlayerModelType.WIDE;
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (skinUrl != null && !skinUrl.isBlank()) {
                                SakuraWeaponsMod.LOGGER.info("Resolved username '{}' via PlayerDB to {}", trimmed, skinUrl);
                                return new ResolvedSkinInfo(skinUrl, modelType);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                SakuraWeaponsMod.LOGGER.warn("PlayerDB resolution failed for '{}': {}", trimmed, e.getMessage());
            }

            // Fallback for username: Minotar
            return new ResolvedSkinInfo("https://minotar.net/skin/" + trimmed, null);
        }

        // 5. Direct HTTP(S) URL
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return new ResolvedSkinInfo(trimmed, null);
        }

        return new ResolvedSkinInfo("https://minotar.net/skin/" + trimmed, null);
    }

    private static byte[] downloadSkinBytes(String targetUrl) throws Exception {
        URL url = URI.create(targetUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_CONNECT_MS);
        conn.setReadTimeout(TIMEOUT_READ_MS);
        conn.setRequestProperty("User-Agent", "Takasha-Minecraft-Client/1.9.0");
        conn.setInstanceFollowRedirects(true);

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IllegalStateException("HTTP " + responseCode + " from " + targetUrl);
        }

        String contentType = conn.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("text/html") 
            && !targetUrl.contains("api.mineskin.org") && !targetUrl.contains("playerdb.co")) {
            throw new IllegalStateException("URL returned HTML webpage (" + contentType + ") instead of image/API.");
        }

        int contentLength = conn.getContentLength();
        if (contentLength > MAX_FILE_SIZE) {
            throw new IllegalStateException("Skin size " + contentLength + " exceeds limit of " + MAX_FILE_SIZE);
        }

        try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_FILE_SIZE) {
                    throw new IllegalStateException("Skin stream exceeded 5MB max payload");
                }
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } finally {
            conn.disconnect();
        }
    }

    private static boolean isSlimModel(NativeImage image) {
        if (image.getHeight() < 64) {
            return false;
        }
        int pixel = image.getPixel(54, 20);
        int alpha = (pixel >> 24) & 0xFF;
        return alpha == 0;
    }

    private static String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : encoded) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
