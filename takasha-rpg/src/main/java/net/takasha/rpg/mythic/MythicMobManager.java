package net.takasha.rpg.mythic;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Registry and parser for MythicMobs mob/VFX definitions.
 */
public class MythicMobManager {

    private static final MythicMobManager INSTANCE = new MythicMobManager();

    public static MythicMobManager getInstance() {
        return INSTANCE;
    }

    private final Map<String, MythicMobDef> mobs = new ConcurrentHashMap<>();

    private MythicMobManager() {}

    private static final Pattern MODEL_PATTERN = Pattern.compile("model\\{[^}]*mid=([a-zA-Z0-9_\\-]+)");
    private static final Pattern STATE_PATTERN = Pattern.compile("state\\{[^}]*s=([a-zA-Z0-9_\\-]+)");
    private static final Pattern REMOVE_PATTERN = Pattern.compile("remove\\{[^}]*delay=([0-9]+)");

    public void loadFromYaml(InputStream inputStream) {
        if (inputStream == null) return;
        try {
            Yaml yaml = new Yaml();
            Object loaded = yaml.load(inputStream);
            if (loaded instanceof Map<?, ?> data) {
                loadFromMap(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void loadFromMap(Map<?, ?> data) {
        if (data == null) return;
        try {
            for (Map.Entry<?, ?> entry : data.entrySet()) {
                String mobId = String.valueOf(entry.getKey());
                if (!(entry.getValue() instanceof Map<?, ?> map)) continue;

                MythicMobDef def = new MythicMobDef(mobId);

                if (map.containsKey("Type")) {
                    def.setMobType(String.valueOf(map.get("Type")).trim());
                }

                Object skillsObj = map.get("Skills");
                if (skillsObj instanceof java.util.List<?> list) {
                    for (Object skillLine : list) {
                        String line = String.valueOf(skillLine);

                        if ("text_display".equalsIgnoreCase(def.getMobType())) {
                            Matcher textMatcher = Pattern.compile("settextdisplay\\{[^}]*text=\"([^\"]+)\"").matcher(line);
                            if (textMatcher.find()) {
                                def.addTextFrame(textMatcher.group(1));
                            }
                        }

                        Matcher modelMatcher = MODEL_PATTERN.matcher(line);
                        if (modelMatcher.find()) {
                            def.setModelId(modelMatcher.group(1));
                        }

                        if (line.toLowerCase().contains("lockpitch=true")) {
                            def.setLockPitch(true);
                        }

                        Matcher stateMatcher = STATE_PATTERN.matcher(line);
                        if (stateMatcher.find()) {
                            def.setDefaultState(stateMatcher.group(1));
                        }

                        Matcher removeMatcher = REMOVE_PATTERN.matcher(line);
                        if (removeMatcher.find()) {
                            def.setLifetimeTicks(Integer.parseInt(removeMatcher.group(1)));
                        }
                    }
                }

                // If no explicit model id was found, check if mobId starts with VFX_ and matches a model
                if (!"text_display".equalsIgnoreCase(def.getMobType()) && def.getModelId() == null && mobId.toUpperCase().startsWith("VFX_")) {
                    def.setModelId(mobId.substring(4).toLowerCase());
                }

                mobs.put(mobId.toUpperCase(), def);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MythicMobDef get(String mobId) {
        if (mobId == null) return null;
        return mobs.get(mobId.toUpperCase());
    }

    public void register(MythicMobDef def) {
        if (def != null) {
            mobs.put(def.getId().toUpperCase(), def);
        }
    }

    public void clear() {
        mobs.clear();
    }
}
