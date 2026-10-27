package net.takasha.rpg.economy.catalog;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

/**
 * ShopCatalog
 * Registri pusat seluruh 19 berkas katalog pasar EconomyShopGUI-Premium 6.3.1.
 */
public class ShopCatalog {

    private static final Logger LOGGER = LoggerFactory.getLogger("Takasha-ShopCatalog");

    public static final String[] ALL_CATEGORY_FILES = {
        "RotatingShop",
        "Workstations",
        "Ores",
        "Music",
        "Blocks",
        "Farming",
        "Mobs",
        "Enchanting",
        "Dyes",
        "Miscellaneous",
        "Food",
        "SpawnEggs",
        "Decoration",
        "Spawners",
        "Redstone",
        "Potions",
        "Z_EverythingElse"
    };

    private static final Map<String, ShopCategory> CATEGORIES = new LinkedHashMap<>();
    private static final Map<Integer, ShopCategory> SLOT_MAP = new HashMap<>();
    private static final Map<String, ShopItemEntry> ITEM_LOOKUP = new HashMap<>();
    private static boolean initialized = false;

    public static synchronized void initialize() {
        if (initialized) return;

        CATEGORIES.clear();
        SLOT_MAP.clear();
        ITEM_LOOKUP.clear();

        int totalItemsLoaded = 0;

        for (String catFile : ALL_CATEGORY_FILES) {
            String catId = catFile.toLowerCase();
            String sectionPath = "/takasha/sections/" + catFile + ".yml";
            String shopPath = "/takasha/shops/" + catFile + ".yml";

            InputStream sectionStream = ShopCatalog.class.getResourceAsStream(sectionPath);
            InputStream shopStream = ShopCatalog.class.getResourceAsStream(shopPath);

            if (shopStream == null) {
                LOGGER.warn("Berkas shop catalog tidak ditemukan: {}", shopPath);
                continue;
            }

            ShopCategory category = ShopYamlParser.parseCategory(catId, sectionStream, shopStream);

            // Koreksi khusus untuk kategori tertentu jika belum ada di section
            if ("spawneggs".equals(catId) && category.getSlot() < 0) {
                category.setSlot(29);
                category.setIconMaterial("BAT_SPAWN_EGG");
                category.setTitle("&8&lSpawnEggs");
            } else if ("rotatingshop".equals(catId) && category.getSlot() < 0) {
                category.setSlot(4);
                category.setIconMaterial("CLOCK");
                category.setTitle("&d&lMagic shop");
            } else if ("z_everythingelse".equals(catId)) {
                category.setEnabled(false); // Default disabled di menu utama
            }

            CATEGORIES.put(catId, category);

            if (category.isEnabled() && category.getSlot() >= 0) {
                SLOT_MAP.put(category.getSlot(), category);
            }

            for (ShopItemEntry entry : category.getAllItems()) {
                totalItemsLoaded++;
                // Index berdasarkan material
                String cleanMat = entry.getMaterial().toLowerCase();
                if (!cleanMat.startsWith("minecraft:") && !cleanMat.contains(":")) {
                    cleanMat = "minecraft:" + cleanMat;
                }

                if ("minecraft:spawner".equals(cleanMat) && entry.getSpawnerType() != null) {
                    ITEM_LOOKUP.put("spawner:" + entry.getSpawnerType().toLowerCase(), entry);
                } else {
                    ITEM_LOOKUP.putIfAbsent(cleanMat, entry);
                }
            }
        }

        initialized = true;
        LOGGER.info("ShopCatalog berhasil memuat {} kategori ({} item) dengan Zero-Omission Parity!", CATEGORIES.size(), totalItemsLoaded);
    }

    public static ShopCategory getCategory(String id) {
        if (!initialized) initialize();
        if (id == null) return null;
        return CATEGORIES.get(id.toLowerCase());
    }

    public static ShopCategory getCategoryBySlot(int slot) {
        if (!initialized) initialize();
        return SLOT_MAP.get(slot);
    }

    public static Collection<ShopCategory> getAllCategories() {
        if (!initialized) initialize();
        return Collections.unmodifiableCollection(CATEGORIES.values());
    }

    public static List<String> getCategoryIds() {
        if (!initialized) initialize();
        List<String> ids = new ArrayList<>(CATEGORIES.keySet());
        Collections.sort(ids);
        return ids;
    }

    public static ShopItemEntry findItemEntry(ItemStack stack) {
        if (!initialized) initialize();
        if (stack == null || stack.isEmpty()) return null;

        String itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase();

        // Cek spawner khusus
        if (stack.getItem() == Items.SPAWNER) {
            var blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (blockEntityData != null) {
                var tag = blockEntityData.getUnsafe();
                if (tag != null) {
                    String entityId = tag.getStringOr("id", "");
                    if (entityId.startsWith("minecraft:")) entityId = entityId.substring(10);
                    ShopItemEntry spawnerEntry = ITEM_LOOKUP.get("spawner:" + entityId.toLowerCase());
                    if (spawnerEntry != null) return spawnerEntry;
                }
            }
        }

        return ITEM_LOOKUP.get(itemKey);
    }

    public static void reload() {
        initialized = false;
        initialize();
    }
}
