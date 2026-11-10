package net.takasha.rpg.pack;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.takasha.rpg.mmo.MMOItemManager;
import net.takasha.rpg.modelengine.BlockbenchModel;
import net.takasha.rpg.modelengine.BlockbenchParser;
import net.takasha.rpg.modelengine.TakashaModelEngine;
import net.takasha.rpg.mythic.MythicMobManager;
import net.takasha.rpg.mythic.MythicSkillDef;
import net.takasha.rpg.mythic.MythicSkillExecutor;
import net.takasha.rpg.mythic.MythicSkillParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Scalable pack loader that scans both bundled jar resources and the external .minecraft/config/takasha/rpg_packs directory.
 * Automatically loads .bbmodel 3D models, MythicMobs mobs & skills, and MMOItems templates.
 */
public class TakashaPackManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("takasha-pack-manager");
    private static final TakashaPackManager INSTANCE = new TakashaPackManager();
    private final java.util.concurrent.atomic.AtomicBoolean initialized = new java.util.concurrent.atomic.AtomicBoolean(false);

    public static TakashaPackManager getInstance() {
        return INSTANCE;
    }

    private TakashaPackManager() {}

    public void initialize() {
        if (initialized.compareAndSet(false, true)) {
            loadAllPacks();
        }
    }

    public void reloadAll() {
        LOGGER.info("Reloading all Takasha RPG packs...");
        TakashaModelEngine.clearAll();
        MythicMobManager.getInstance().clear();
        MythicSkillExecutor.getInstance().clearAll();
        MMOItemManager.clear();
        loadAllPacks();
        initialized.set(true);
    }

    private void loadAllPacks() {
        int packCount = 0;

        // 1. Load bundled packs from mod container resources
        Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer(net.takasha.rpg.TakashaRpgMod.MOD_ID);
        if (modContainer.isEmpty()) {
            modContainer = FabricLoader.getInstance().getModContainer("takasha-rpg");
        }
        if (modContainer.isPresent()) {
            Optional<Path> rootPath = modContainer.get().findPath("takasha/rpg_packs");
            if (rootPath.isPresent()) {
                packCount += loadPacksFromDirectory(rootPath.get(), true);
            }
        }

        // 2. Load external packs from config directory (for drag-and-drop expansion without recompilation)
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("takasha/rpg_packs");
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            packCount += loadPacksFromDirectory(configDir, false);
        } catch (Exception e) {
            LOGGER.error("Failed to access external packs directory: {}", configDir, e);
        }

        // 3. Load custom created MMOItem templates from config/takasha/mmoitems/item/<TYPE>.yml
        int customItems = net.takasha.rpg.mmo.MMOItemDiskManager.loadCustomTemplates();
        if (customItems > 0) {
            LOGGER.info("Loaded {} custom MMOItem template(s) from user config disk.", customItems);
        }

        LOGGER.info("Successfully loaded {} RPG pack(s). Total MMOItem templates active: {}, total skills registered: {}.",
                packCount, MMOItemManager.getAllTemplates().size(), MythicSkillExecutor.getInstance().getRegisteredSkillsCount());
    }

    private int loadPacksFromDirectory(Path parentDir, boolean isResource) {
        if (!Files.exists(parentDir) || !Files.isDirectory(parentDir)) {
            return 0;
        }

        int loaded = 0;
        try (Stream<Path> stream = Files.list(parentDir)) {
            for (Path packPath : (Iterable<Path>) stream::iterator) {
                if (Files.isDirectory(packPath)) {
                    loadSinglePack(packPath);
                    loaded++;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error reading packs from {}", parentDir, e);
        }
        return loaded;
    }

    public void loadSinglePack(Path packDir) {
        String packName = packDir.getFileName().toString();
        LOGGER.info("Loading RPG pack: [{}]", packName);

        int modelsLoaded = 0;
        int mobFilesLoaded = 0;
        int skillFilesLoaded = 0;
        int itemFilesLoaded = 0;

        try (Stream<Path> walk = Files.walk(packDir)) {
            List<Path> allFiles = walk.filter(Files::isRegularFile).collect(Collectors.toList());
            allFiles.sort((p1, p2) -> {
                String s1 = p1.toString().replace('\\', '/').toLowerCase();
                String s2 = p2.toString().replace('\\', '/').toLowerCase();
                // Ensure MMOITEMS skills are loaded AFTER MMOCORE so MMOITEMS definitions override MMOCORE
                if (s1.contains("mmoitems") && s2.contains("mmocore")) return 1;
                if (s1.contains("mmocore") && s2.contains("mmoitems")) return -1;
                return s1.compareTo(s2);
            });

            for (Path file : allFiles) {
                String fileName = file.getFileName().toString();
                String pathStr = file.toString().replace('\\', '/').toLowerCase();

                // Skip conflicting alternative plugin formats to preserve 1:1 MMOItems parity & PvP/Dummy targeting (hp=true)
                if (pathStr.contains("only damage mobs") || pathStr.contains("for crucible")
                        || pathStr.contains("/mythicrpg/") || pathStr.contains("/oraxen")
                        || pathStr.contains("/nexo") || pathStr.contains("/itemsadder/")) {
                    continue;
                }

                // 1. Blockbench 3D Models (.bbmodel)
                if (fileName.toLowerCase().endsWith(".bbmodel")) {
                    String modelId = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();
                    try (InputStream is = Files.newInputStream(file)) {
                        BlockbenchModel model = BlockbenchParser.parse(modelId, is);
                        if (model != null) {
                            TakashaModelEngine.registerModel(modelId, model);
                            modelsLoaded++;
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to load model file: {}", fileName, e);
                    }
                }
                // 2. YAML Files (Heuristic Content-Signature Classification)
                else if (fileName.toLowerCase().endsWith(".yml") || fileName.toLowerCase().endsWith(".yaml")) {
                    try (InputStream is = Files.newInputStream(file)) {
                        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
                        Object loaded = yaml.load(is);
                        if (loaded instanceof Map<?, ?> rootMap && !rootMap.isEmpty()) {
                            boolean isSkill = pathStr.contains("/skills/");
                            boolean isMob = pathStr.contains("/mobs/");
                            boolean isItem = pathStr.contains("/item/") || pathStr.contains("/mmoitems/");

                            if (!isSkill && !isMob && !isItem) {
                                if (pathStr.contains("/itemsadder/") || pathStr.contains("/oraxen") || pathStr.contains("/nexo")) {
                                    // Skip alternative Spigot plugin definitions (ItemsAdder/Oraxen/Nexo) to avoid template conflicts
                                    continue;
                                }
                                // Heuristic inspection for unknown or arbitrary folder structures
                                for (Map.Entry<?, ?> entry : rootMap.entrySet()) {
                                    if (entry.getValue() instanceof Map<?, ?> valMap) {
                                        if (valMap.containsKey("Skills") || valMap.containsKey("Conditions") || valMap.containsKey("Cooldown") || valMap.containsKey("TargetConditions")) {
                                            isSkill = true;
                                            break;
                                        }
                                        if (valMap.containsKey("Type") || valMap.containsKey("DisplayOptions")) {
                                            isMob = true;
                                            break;
                                        }
                                        if (valMap.containsKey("material") || valMap.containsKey("base") || valMap.containsKey("ability") || valMap.containsKey("custom-model-data")) {
                                            isItem = true;
                                            break;
                                        }
                                    }
                                }
                            }

                            if (isSkill) {
                                Map<String, MythicSkillDef> parsedSkills = MythicSkillParser.parseSkillsMap(rootMap);
                                MythicSkillExecutor.getInstance().registerSkills(parsedSkills);
                                skillFilesLoaded++;
                            } else if (isMob) {
                                MythicMobManager.getInstance().loadFromMap(rootMap);
                                mobFilesLoaded++;
                            } else if (isItem) {
                                String defaultType = fileName.substring(0, fileName.lastIndexOf('.')).toUpperCase();
                                MMOItemManager.loadItemTemplatesMap(rootMap, defaultType);
                                itemFilesLoaded++;
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to process YAML file: {}", fileName, e);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error walking pack directory: {}", packDir, e);
        }

        LOGGER.info("Pack [{}] loaded: {} models, {} mob files, {} skill files, {} item files.",
                packName, modelsLoaded, mobFilesLoaded, skillFilesLoaded, itemFilesLoaded);
    }
}
