package net.takasha.rpg.mmo;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * MMOItemDiskManager
 * Handles real-time persistence of MMOItems templates to physical YAML files
 * in the configuration directory: config/takasha/mmoitems/item/<TYPE>.yml
 *
 * Implements 1:1 format compatibility with MMOItems Spigot YAML specifications.
 */
public class MMOItemDiskManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("takasha-mmo-disk");
    private static final Object FILE_LOCK = new Object();

    public static Path getItemsDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve("takasha/mmoitems/item");
    }

    public static Path getTypeFilePath(String type) {
        String cleanType = (type == null || type.isBlank()) ? "TOOL" : type.toUpperCase(Locale.ROOT);
        return getItemsDirectory().resolve(cleanType + ".yml");
    }

    /**
     * Loads all custom MMOItems templates from config/takasha/mmoitems/item/<TYPE>.yml
     */
    public static int loadCustomTemplates() {
        Path dir = getItemsDirectory();
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                LOGGER.error("Failed to create MMOItems config directory: {}", dir, e);
                return 0;
            }
        }

        int loadedCount = 0;
        try (Stream<Path> stream = Files.list(dir)) {
            for (Path file : (Iterable<Path>) stream::iterator) {
                String fileName = file.getFileName().toString();
                if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
                    String type = fileName.substring(0, fileName.lastIndexOf('.')).toUpperCase(Locale.ROOT);
                    try (InputStream is = Files.newInputStream(file)) {
                        Yaml yaml = new Yaml();
                        Object loaded = yaml.load(is);
                        if (loaded instanceof Map<?, ?> rootMap) {
                            MMOItemManager.loadItemTemplatesMap(rootMap, type);
                            loadedCount += rootMap.size();
                            LOGGER.info("Loaded {} custom MMOItem template(s) from {}", rootMap.size(), fileName);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to load custom MMOItem YAML: {}", fileName, e);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error reading MMOItems items directory: {}", dir, e);
        }
        return loadedCount;
    }

    /**
     * Persists or updates a template directly to config/takasha/mmoitems/item/<TYPE>.yml
     */
    @SuppressWarnings("unchecked")
    public static boolean saveTemplate(MMOItemTemplate template) {
        if (template == null || template.getId() == null || template.getItemType() == null) {
            return false;
        }

        synchronized (FILE_LOCK) {
            Path filePath = getTypeFilePath(template.getItemType());
            try {
                if (!Files.exists(filePath.getParent())) {
                    Files.createDirectories(filePath.getParent());
                }

                Map<String, Object> rootMap = new LinkedHashMap<>();
                if (Files.exists(filePath)) {
                    try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                        Yaml yaml = new Yaml();
                        Object existing = yaml.load(reader);
                        if (existing instanceof Map<?, ?> map) {
                            for (Map.Entry<?, ?> entry : map.entrySet()) {
                                rootMap.put(String.valueOf(entry.getKey()), entry.getValue());
                            }
                        }
                    }
                }

                Map<String, Object> itemMap = exportTemplateToMap(template);
                rootMap.put(template.getId().toUpperCase(Locale.ROOT), itemMap);

                writeYamlFile(filePath, rootMap);
                LOGGER.info("Successfully persisted MMOItem template {}:{} to {}", template.getItemType(), template.getId(), filePath.getFileName());
                return true;
            } catch (Exception e) {
                LOGGER.error("Failed to save MMOItem template {}:{} to disk", template.getItemType(), template.getId(), e);
                return false;
            }
        }
    }

    /**
     * Deletes a template section from config/takasha/mmoitems/item/<TYPE>.yml
     */
    @SuppressWarnings("unchecked")
    public static boolean deleteTemplateDisk(String type, String id) {
        if (type == null || id == null) return false;

        synchronized (FILE_LOCK) {
            Path filePath = getTypeFilePath(type);
            if (!Files.exists(filePath)) {
                return false;
            }

            try {
                Map<String, Object> rootMap = new LinkedHashMap<>();
                try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                    Yaml yaml = new Yaml();
                    Object existing = yaml.load(reader);
                    if (existing instanceof Map<?, ?> map) {
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            rootMap.put(String.valueOf(entry.getKey()), entry.getValue());
                        }
                    }
                }

                String upperId = id.toUpperCase(Locale.ROOT);
                if (rootMap.remove(upperId) != null) {
                    writeYamlFile(filePath, rootMap);
                    LOGGER.info("Deleted MMOItem template {}:{} from disk file {}", type, id, filePath.getFileName());
                    return true;
                }
                return false;
            } catch (Exception e) {
                LOGGER.error("Failed to delete MMOItem template {}:{} from disk", type, id, e);
                return false;
            }
        }
    }

    public static Map<String, Object> exportTemplateToMap(MMOItemTemplate template) {
        Map<String, Object> itemMap = new LinkedHashMap<>();
        Map<String, Object> baseMap = new LinkedHashMap<>();

        baseMap.put("material", template.getMaterial());
        if (template.getName() != null && !template.getName().isEmpty()) {
            baseMap.put("name", template.getName());
        }
        if (template.getTier() != null) {
            baseMap.put("tier", template.getTier().name());
        }
        if (template.getCustomModelData() > 0) {
            baseMap.put("custom-model-data", template.getCustomModelData());
        }
        if (!template.getDisplayedType().isEmpty()) {
            baseMap.put("displayed-type", template.getDisplayedType());
        }
        if (template.getAttackDamage() > 0) {
            baseMap.put("attack-damage", template.getAttackDamage());
        }
        if (template.getAttackSpeed() > 0 && template.getAttackSpeed() != 4.0) {
            baseMap.put("attack-speed", template.getAttackSpeed());
        }
        if (template.isUnbreakable()) {
            baseMap.put("unbreakable", true);
        }
        if (template.isDisableAttackPassive()) {
            baseMap.put("disable-attack-passive", true);
        }
        if (template.getPickaxePower() > 0) {
            baseMap.put("pickaxe-power", template.getPickaxePower());
        }
        if (template.getMiningEfficiency() > 0) {
            baseMap.put("mining-efficiency", template.getMiningEfficiency());
        }
        if (template.isAutosmelt()) {
            baseMap.put("autosmelt", true);
        }
        if (template.isBouncingCrack()) {
            baseMap.put("bouncing-crack", true);
        }
        if (template.getMaxDurability() > 0) {
            baseMap.put("max-durability", template.getMaxDurability());
        }
        if (!template.isWillBreak()) {
            baseMap.put("will-break", false);
        }
        if (!template.getLore().isEmpty()) {
            baseMap.put("lore", new ArrayList<>(template.getLore()));
        }

        // Numerical / RPG Stats
        for (Map.Entry<MMOStatType, Double> entry : template.getStats().entrySet()) {
            baseMap.put(entry.getKey().getKey(), entry.getValue());
        }

        // Vanilla Enchants
        if (!template.getVanillaEnchants().isEmpty()) {
            baseMap.put("enchants", new LinkedHashMap<>(template.getVanillaEnchants()));
        }

        // AdvancedEnchantments
        if (!template.getAdvancedEnchants().isEmpty()) {
            baseMap.put("advancedenchantments", new LinkedHashMap<>(template.getAdvancedEnchants()));
        }

        // Gem Sockets
        if (!template.getGemSockets().isEmpty()) {
            baseMap.put("gem-sockets", new ArrayList<>(template.getGemSockets()));
        }

        // Gem Stone Attributes (MMOItems 1:1)
        if (template.hasGemColor()) {
            baseMap.put("gem-color", template.getGemColor());
        }
        if (template.getGemUpgradeScaling() != null && !template.getGemUpgradeScaling().equalsIgnoreCase("SUBSEQUENT")) {
            baseMap.put("gem-upgrade-scaling", template.getGemUpgradeScaling());
        }
        if (!template.getItemTypeRestrictions().isEmpty()) {
            baseMap.put("item-type-restriction", new ArrayList<>(template.getItemTypeRestrictions()));
        }
        if (template.hasSuccessRate() && template.getSuccessRate() < 100.0) {
            baseMap.put("success-rate", template.getSuccessRate());
        }

        // Advanced & Cosmetic Attributes (MMOItems 1:1)
        if (template.hasItemSet()) {
            baseMap.put("item-set", template.getItemSet());
        }
        if (template.hasLoreFormat()) {
            baseMap.put("lore-format", template.getLoreFormat());
        }
        if (template.hasShieldPattern()) {
            baseMap.put("shield-pattern", template.getShieldPattern());
        }
        if (template.hasDyeColor()) {
            baseMap.put("dye-color", template.getDyeColor());
        }
        if (template.hasTrimMaterial()) {
            baseMap.put("trim-material", template.getTrimMaterial());
        }
        if (template.hasTrimPattern()) {
            baseMap.put("trim-pattern", template.getTrimPattern());
        }
        if (template.hasRequiredClass()) {
            baseMap.put("required-class", template.getRequiredClass());
        }
        if (template.hasPermission()) {
            baseMap.put("permission", template.getPermission());
        }
        if (template.hasTwoHanded()) {
            baseMap.put("two-handed", template.isTwoHanded());
        }
        if (template.hasHandworn()) {
            baseMap.put("handworn", template.isHandworn());
        }
        if (template.hasHideEnchants()) {
            baseMap.put("hide-enchants", template.isHideEnchants());
        }
        if (template.hasCanIdentify()) {
            baseMap.put("can-identify", template.isCanIdentify());
        }
        if (template.hasCanDeconstruct()) {
            baseMap.put("can-deconstruct", template.isCanDeconstruct());
        }
        if (template.hasCanDeskin()) {
            baseMap.put("can-deskin", template.isCanDeskin());
        }
        if (template.hasKnockback() && template.getKnockback() > 0) {
            baseMap.put("knockback", template.getKnockback());
        }
        if (template.hasRecoil() && template.getRecoil() > 0) {
            baseMap.put("recoil", template.getRecoil());
        }
        if (!template.getCommands().isEmpty()) {
            baseMap.put("commands", new ArrayList<>(template.getCommands()));
        }
        if (!template.getPermEffects().isEmpty()) {
            baseMap.put("perm-effects", new ArrayList<>(template.getPermEffects()));
        }
        if (!template.getPotionEffects().isEmpty()) {
            baseMap.put("potion-effects", new ArrayList<>(template.getPotionEffects()));
        }
        if (!template.getCustomSounds().isEmpty()) {
            baseMap.put("custom-sounds", new ArrayList<>(template.getCustomSounds()));
        }
        if (template.hasItemParticles()) {
            baseMap.put("item-particles", template.getItemParticles());
        }
        if (template.hasArrowParticles()) {
            baseMap.put("arrow-particles", template.getArrowParticles());
        }

        // Abilities
        if (!template.getAbilities().isEmpty()) {
            Map<String, Object> abRoot = new LinkedHashMap<>();
            for (MMOItemTemplate.AbilityConfig ab : template.getAbilities()) {
                Map<String, Object> abDetails = new LinkedHashMap<>();
                abDetails.put("type", ab.getType());
                abDetails.put("mode", ab.getMode().name());
                if (ab.getCooldown() > 0) {
                    abDetails.put("cooldown", ab.getCooldown());
                }
                if (ab.getManaCost() > 0) {
                    abDetails.put("mana", ab.getManaCost());
                }
                if (ab.getDamage() > 0) {
                    abDetails.put("damage", ab.getDamage());
                }
                if (ab.getBurnDuration() > 0) {
                    abDetails.put("burnduration", ab.getBurnDuration());
                }
                if (ab.getBurnDamage() > 0) {
                    abDetails.put("burndamage", ab.getBurnDamage());
                }
                if (ab.getExtraParams() != null && !ab.getExtraParams().isEmpty()) {
                    abDetails.putAll(ab.getExtraParams());
                }
                abRoot.put(ab.getAbilityKey(), abDetails);
            }
            baseMap.put("ability", abRoot);
        }

        itemMap.put("base", baseMap);
        return itemMap;
    }

    private static void writeYamlFile(Path filePath, Map<String, Object> data) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);

        Yaml yaml = new Yaml(options);
        try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            yaml.dump(data, writer);
        }
    }
}
