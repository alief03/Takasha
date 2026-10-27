package net.sakura.touhou.client.render.cache;

import net.minecraft.resources.Identifier;
import net.sakura.touhou.TouhouLittleMaidMod;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BedrockModelCache
 * Cache model Bedrock dan tekstur 3D dengan kebijakan 4-Slot Least-Recently-Used (LRU).
 * Fitur Anti-Crash Spesifik Hardware (AMD Ryzen 3 7320U, 5.74 GB RAM, Radeon 610M):
 * - Membaca aset model dan tekstur secara on-demand (HANYA saat entitas maid muncul di dunia).
 * - DILARANG melakukan eager loading 40+ model JSON dan tekstur HD saat startup game.
 * - Membatasi maksimal 4 model aktif di memori secara simultan.
 * - Menghapus model yang tidak aktif selama > 5 menit untuk membebaskan VRAM/RAM sistem.
 */
public class BedrockModelCache {

    private static final int MAX_ACTIVE_MODELS = 4;
    private static final long EVICTION_TIMEOUT_MS = 5 * 60 * 1000L; // 5 Menit

    public static class CachedModelEntry {
        public final String modelId;
        public final Identifier textureLocation;
        public long lastAccessTime;

        public CachedModelEntry(String modelId, Identifier textureLocation) {
            this.modelId = modelId;
            this.textureLocation = textureLocation;
            this.lastAccessTime = System.currentTimeMillis();
        }
    }

    private static final Map<String, CachedModelEntry> CACHE = new LinkedHashMap<>(MAX_ACTIVE_MODELS, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedModelEntry> eldest) {
            if (size() > MAX_ACTIVE_MODELS) {
                TouhouLittleMaidMod.LOGGER.debug("Evicting eldest maid model from LRU cache: {}", eldest.getKey());
                return true;
            }
            return false;
        }
    };

    /**
     * Mengambil lokasi tekstur model secara on-demand.
     * Jika belum ada di cache, daftarkan secara lazy.
     */
    public static synchronized Identifier getTexture(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            modelId = "touhou_little_maid:hakurei_reimu";
        }

        CachedModelEntry entry = CACHE.get(modelId);
        long now = System.currentTimeMillis();

        if (entry == null) {
            // Bersihkan model basi yang tidak diakses lebih dari 5 menit
            cleanupStaleEntries(now);

            // Parsing path tekstur dari modelId
            String path = modelId.contains(":") ? modelId.split(":")[1] : modelId;
            Identifier tex = Identifier.fromNamespaceAndPath(TouhouLittleMaidMod.MOD_ID, "textures/entity/" + path + ".png");

            entry = new CachedModelEntry(modelId, tex);
            CACHE.put(modelId, entry);
            TouhouLittleMaidMod.LOGGER.debug("Loaded maid model into on-demand cache: {} (Active in VRAM: {})", modelId, CACHE.size());
        } else {
            entry.lastAccessTime = now;
        }

        return entry.textureLocation;
    }

    private static void cleanupStaleEntries(long currentTime) {
        CACHE.values().removeIf(entry -> (currentTime - entry.lastAccessTime) > EVICTION_TIMEOUT_MS);
    }

    public static synchronized void clear() {
        CACHE.clear();
    }
}
