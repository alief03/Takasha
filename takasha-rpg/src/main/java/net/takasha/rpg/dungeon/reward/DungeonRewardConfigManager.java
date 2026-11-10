package net.takasha.rpg.dungeon.reward;

import com.google.gson.*;
import net.minecraft.world.item.ItemStack;
import net.takasha.rpg.dungeon.DungeonTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Manajer Konfigurasi Dinamis Hadiah Dungeon Takasha RPG.
 * Mengelola pemuatan, penyimpanan, modifikasi, dan simulasi roll drop pool per-Tier dan per-Wave.
 * Tersimpan secara persisten pada format JSON di config/takasha/dungeon_rewards.json.
 */
public class DungeonRewardConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("takasha-dungeon-rewards");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DungeonRewardConfigManager INSTANCE = new DungeonRewardConfigManager();

    public static DungeonRewardConfigManager getInstance() {
        return INSTANCE;
    }

    private final Map<DungeonTier, Map<Integer, List<DungeonLootEntry>>> rewardTable = new EnumMap<>(DungeonTier.class);
    private boolean loaded = false;

    private DungeonRewardConfigManager() {
        for (DungeonTier tier : DungeonTier.values()) {
            rewardTable.put(tier, new HashMap<>());
        }
    }

    public synchronized void initialize() {
        if (!loaded) {
            loadFromDisk();
            loaded = true;
        }
    }

    private Path getConfigFile() {
        try {
            net.fabricmc.loader.api.FabricLoader loader = net.fabricmc.loader.api.FabricLoader.getInstance();
            if (loader != null && loader.getConfigDir() != null) {
                return loader.getConfigDir().resolve("takasha").resolve("dungeon_rewards.json");
            }
        } catch (Throwable ignored) {}
        return Paths.get("config", "takasha", "dungeon_rewards.json");
    }

    public synchronized void loadFromDisk() {
        Path filePath = getConfigFile();
        if (!Files.exists(filePath)) {
            LOGGER.info("Dungeon rewards config not found at {}. Generating authentic defaults...", filePath);
            populateDefaults();
            saveToDisk();
            return;
        }

        try (FileReader reader = new FileReader(filePath.toFile())) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            for (DungeonTier tier : DungeonTier.values()) {
                Map<Integer, List<DungeonLootEntry>> waveMap = rewardTable.computeIfAbsent(tier, k -> new HashMap<>());
                String tierKey = tier.name();

                if (root.has(tierKey) && root.get(tierKey).isJsonObject()) {
                    JsonObject tierObj = root.getAsJsonObject(tierKey);
                    for (int wave = 1; wave <= 5; wave++) {
                        String waveKey = String.valueOf(wave);
                        List<DungeonLootEntry> entries = new ArrayList<>();
                        if (tierObj.has(waveKey) && tierObj.get(waveKey).isJsonArray()) {
                            JsonArray array = tierObj.getAsJsonArray(waveKey);
                            for (JsonElement el : array) {
                                if (el.isJsonObject()) {
                                    entries.add(DungeonLootEntry.fromJson(el.getAsJsonObject()));
                                }
                            }
                        }
                        if (entries.isEmpty()) {
                            entries = getDefaultEntriesForWave(tier, wave);
                        }
                        waveMap.put(wave, entries);
                    }
                } else {
                    for (int wave = 1; wave <= 5; wave++) {
                        waveMap.put(wave, getDefaultEntriesForWave(tier, wave));
                    }
                }
            }
            LOGGER.info("Successfully loaded dungeon rewards from {}", filePath);
        } catch (Exception e) {
            LOGGER.error("Failed to parse dungeon rewards JSON: {}", e.getMessage(), e);
            populateDefaults();
        }
    }

    public synchronized void saveToDisk() {
        Path filePath = getConfigFile();
        try {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }

            JsonObject root = new JsonObject();
            for (DungeonTier tier : DungeonTier.values()) {
                JsonObject tierObj = new JsonObject();
                Map<Integer, List<DungeonLootEntry>> waveMap = rewardTable.get(tier);
                if (waveMap != null) {
                    for (int wave = 1; wave <= 5; wave++) {
                        JsonArray waveArray = new JsonArray();
                        List<DungeonLootEntry> entries = waveMap.getOrDefault(wave, Collections.emptyList());
                        for (DungeonLootEntry entry : entries) {
                            waveArray.add(entry.toJson());
                        }
                        tierObj.add(String.valueOf(wave), waveArray);
                    }
                }
                root.add(tier.name(), tierObj);
            }

            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                GSON.toJson(root, writer);
            }
            LOGGER.info("Successfully saved dungeon rewards to {}", filePath);
        } catch (IOException e) {
            LOGGER.error("Failed to save dungeon rewards JSON: {}", e.getMessage(), e);
        }
    }

    public synchronized List<DungeonLootEntry> getLootForWave(DungeonTier tier, int wave) {
        initialize();
        Map<Integer, List<DungeonLootEntry>> waveMap = rewardTable.get(tier);
        if (waveMap == null) return Collections.emptyList();
        return waveMap.computeIfAbsent(wave, w -> getDefaultEntriesForWave(tier, w));
    }

    public synchronized void setLootForWave(DungeonTier tier, int wave, List<DungeonLootEntry> entries) {
        initialize();
        Map<Integer, List<DungeonLootEntry>> waveMap = rewardTable.computeIfAbsent(tier, k -> new HashMap<>());
        waveMap.put(wave, new ArrayList<>(entries));
    }

    public synchronized void addLootEntry(DungeonTier tier, int wave, DungeonLootEntry entry) {
        initialize();
        List<DungeonLootEntry> list = getLootForWave(tier, wave);
        list.add(entry);
    }

    public synchronized boolean removeLootEntry(DungeonTier tier, int wave, int index) {
        initialize();
        List<DungeonLootEntry> list = getLootForWave(tier, wave);
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            return true;
        }
        return false;
    }

    public synchronized void resetWaveToDefault(DungeonTier tier, int wave) {
        initialize();
        Map<Integer, List<DungeonLootEntry>> waveMap = rewardTable.computeIfAbsent(tier, k -> new HashMap<>());
        waveMap.put(wave, getDefaultEntriesForWave(tier, wave));
        saveToDisk();
    }

    public synchronized void populateDefaults() {
        for (DungeonTier tier : DungeonTier.values()) {
            Map<Integer, List<DungeonLootEntry>> waveMap = rewardTable.computeIfAbsent(tier, k -> new HashMap<>());
            for (int wave = 1; wave <= 5; wave++) {
                waveMap.put(wave, getDefaultEntriesForWave(tier, wave));
            }
        }
    }

    /**
     * Menjalankan simulasi roll 1 kali untuk wave dan tier tertentu.
     */
    public List<ItemStack> simulateRoll(DungeonTier tier, int wave, Random rng) {
        int coinMultiplier = switch (tier) {
            case COMMON -> 1;
            case NIGHTMARE -> 2;
            case SAKURA_BOSS -> 4;
        };

        List<DungeonLootEntry> entries = getLootForWave(tier, wave);
        List<ItemStack> rolled = new ArrayList<>();
        for (DungeonLootEntry entry : entries) {
            if (rng.nextFloat() <= entry.getChance()) {
                ItemStack stack = entry.createItemStack(coinMultiplier, rng);
                if (!stack.isEmpty()) {
                    addOrStackItem(rolled, stack);
                }
            }
        }
        return rolled;
    }

    private void addOrStackItem(List<ItemStack> list, ItemStack newStack) {
        if (newStack.isEmpty()) return;

        for (ItemStack existing : list) {
            if (ItemStack.isSameItemSameComponents(existing, newStack) && existing.getCount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toAdd = Math.min(space, newStack.getCount());
                existing.grow(toAdd);
                newStack.shrink(toAdd);
                if (newStack.isEmpty()) return;
            }
        }

        while (!newStack.isEmpty()) {
            int toTake = Math.min(newStack.getCount(), newStack.getMaxStackSize());
            ItemStack sub = newStack.split(toTake);
            list.add(sub);
        }
    }

    private List<DungeonLootEntry> getDefaultEntriesForWave(DungeonTier tier, int wave) {
        List<DungeonLootEntry> entries = new ArrayList<>();

        switch (wave) {
            case 1 -> {
                // Fixed base: 50 RPG Coins (scale), 2 Magic Dust
                entries.add(new DungeonLootEntry("takasha_rpg:rpg_coin", 1.0f, 50, 50, true));
                entries.add(new DungeonLootEntry("takasha_rpg:magic_dust", 1.0f, 2, 2, false));
                entries.add(new DungeonLootEntry("takasha_rpg:mystery_book_simple", 0.40f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:astrolabe_navigator", 0.08f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:tear_frostborn", 0.07f, 1, 1, false));
            }
            case 2 -> {
                // Fixed base: 120 RPG Coins (scale), 4 Magic Dust, 1 Mystery Dust
                entries.add(new DungeonLootEntry("takasha_rpg:rpg_coin", 1.0f, 120, 120, true));
                entries.add(new DungeonLootEntry("takasha_rpg:magic_dust", 1.0f, 4, 4, false));
                entries.add(new DungeonLootEntry("takasha_rpg:mystery_dust", 1.0f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:mystery_book_unique", 0.45f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:stat_tracker_kills", 0.10f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:stat_tracker_mobs", 0.10f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:stat_tracker_blocks", 0.10f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:dreamcatcher_charm", 0.20f, 1, 1, false));
            }
            case 3 -> {
                // Fixed base: 250 RPG Coins (scale), 1 Gold Coin (scale)
                entries.add(new DungeonLootEntry("takasha_rpg:rpg_coin", 1.0f, 250, 250, true));
                entries.add(new DungeonLootEntry("takasha_rpg:rpg_gold_coin", 1.0f, 1, 1, true));
                entries.add(new DungeonLootEntry("takasha_rpg:mystery_book_elite", 0.40f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:mystery_book_ultimate", 0.25f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:slot_increaser_1", 0.15f, 1, 2, false));
                entries.add(new DungeonLootEntry("takasha_rpg:secret_dust", 0.10f, 1, 2, false));
                entries.add(new DungeonLootEntry("takasha_rpg:randomization_scroll", 0.10f, 1, 2, false));
                entries.add(new DungeonLootEntry("takasha_rpg:tome_workbench", 0.07f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:scroll_ascension", 0.07f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:wanderer_compass", 0.06f, 1, 1, false));
                entries.add(new DungeonLootEntry("mmo:SWORD:CUTLASS", 0.15f, 1, 1, false));
                entries.add(new DungeonLootEntry("mmo:TOOL:SILVER_PICKAXE", 0.10f, 1, 1, false));
            }
            case 4 -> {
                // Fixed base: 500 RPG Coins (scale), 2 Gold Coins (scale), 1 White Scroll
                entries.add(new DungeonLootEntry("takasha_rpg:rpg_coin", 1.0f, 500, 500, true));
                entries.add(new DungeonLootEntry("takasha_rpg:rpg_gold_coin", 1.0f, 2, 2, true));
                entries.add(new DungeonLootEntry("takasha_rpg:white_scroll", 1.0f, 1, 1, false));

                float keystoneChance = switch (tier) {
                    case COMMON -> 0.45f;
                    case NIGHTMARE -> 0.65f;
                    case SAKURA_BOSS -> 0.85f;
                };
                entries.add(new DungeonLootEntry("takasha_rpg:ancient_keystone", keystoneChance, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:mystery_book_legendary", 0.30f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:mystery_book_fabled", 0.15f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:phantom_helmet", 0.05f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:phantom_chestplate", 0.05f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:phantom_leggings", 0.05f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:phantom_boots", 0.05f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:ranger_chestplate", 0.05f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:tome_anvil", 0.07f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:tome_smithing", 0.07f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:tome_grindstone", 0.06f, 1, 1, false));
                entries.add(new DungeonLootEntry("mmo:KATANA:MURAMASA", 0.15f, 1, 1, false));
                entries.add(new DungeonLootEntry("mmo:ARMOR:TITAN_CHESTPLATE", 0.15f, 1, 1, false));
            }
            case 5 -> {
                // Boss Wave: 1000 RPG Coins (scale), 5 Gold Coins (scale), Takasha Gems, 1 Holy Scroll
                entries.add(new DungeonLootEntry("takasha_rpg:rpg_coin", 1.0f, 1000, 1000, true));
                entries.add(new DungeonLootEntry("takasha_rpg:rpg_gold_coin", 1.0f, 5, 5, true));
                entries.add(new DungeonLootEntry("takasha_rpg:takasha_gem", 1.0f, tier == DungeonTier.SAKURA_BOSS ? 6 : 3, tier == DungeonTier.SAKURA_BOSS ? 6 : 3, false));
                entries.add(new DungeonLootEntry("takasha_rpg:holy_scroll", 1.0f, 1, 1, false));

                float keystoneChance = switch (tier) {
                    case COMMON -> 0.75f;
                    case NIGHTMARE -> 0.85f;
                    case SAKURA_BOSS -> 0.95f;
                };
                entries.add(new DungeonLootEntry("takasha_rpg:ancient_keystone", keystoneChance, 1, 1, false));

                float heroicChance = (tier == DungeonTier.COMMON ? 0.50f : 0.70f);
                entries.add(new DungeonLootEntry("takasha_rpg:mystery_book_heroic", heroicChance, 1, 1, false));

                float soulChance = switch (tier) {
                    case COMMON -> 0.25f;
                    case NIGHTMARE -> 0.50f;
                    case SAKURA_BOSS -> 0.75f;
                };
                entries.add(new DungeonLootEntry("takasha_rpg:mystery_book_soul", soulChance, 1, 1, false));

                float masteryChance = switch (tier) {
                    case COMMON -> 0.10f;
                    case NIGHTMARE -> 0.25f;
                    case SAKURA_BOSS -> 0.45f;
                };
                entries.add(new DungeonLootEntry("takasha_rpg:mystery_book_mastery", masteryChance, 1, 1, false));

                entries.add(new DungeonLootEntry("takasha_rpg:phantom_scythe", 0.10f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:koth_sword", 0.10f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:dimensional_sword", 0.10f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:hammer_master_smith", 0.08f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:soul_gem", 0.15f, 1, 1, false));
                entries.add(new DungeonLootEntry("takasha_rpg:weapon_enchantment_orb", 0.10f, 1, 1, false));
                entries.add(new DungeonLootEntry("mmo:GREATSWORD:EXCALIBUR", 0.20f, 1, 1, false));
                entries.add(new DungeonLootEntry("mmo:GREATSTAFF:STAFF_OF_APOCALYPSE", 0.20f, 1, 1, false));
            }
        }

        return entries;
    }
}
