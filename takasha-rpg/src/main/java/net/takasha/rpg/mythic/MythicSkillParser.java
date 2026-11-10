package net.takasha.rpg.mythic;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses MythicMobs skill definitions from YAML files and dynamic strings.
 */
public class MythicSkillParser {

    private static final Pattern TARGETER_PATTERN = Pattern.compile("(@[a-zA-Z0-9_]+(?:\\{[^}]*\\})?)");
    private static final Pattern CONDITION_PATTERN = Pattern.compile("(\\?[a-zA-Z0-9_]+(?:\\{[^}]*\\})?(?:\\s+(?:true|false))?)");

    public static Map<String, MythicSkillDef> parseSkillsYaml(InputStream inputStream) {
        if (inputStream == null) return Collections.emptyMap();
        try {
            Yaml yaml = new Yaml();
            Object loaded = yaml.load(inputStream);
            if (loaded instanceof Map<?, ?> data) {
                return parseSkillsMap(data);
            }
        } catch (Exception e) {
            // Ignored or logged
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, MythicSkillDef> parseSkillsMap(Map<?, ?> data) {
        Map<String, MythicSkillDef> result = new HashMap<>();
        if (data == null) return result;

        try {
            for (Map.Entry<?, ?> entry : data.entrySet()) {
                String skillId = String.valueOf(entry.getKey());
                if (!(entry.getValue() instanceof Map<?, ?> map)) continue;

                MythicSkillDef def = new MythicSkillDef(skillId);

                if (map.containsKey("Cooldown")) {
                    String cdStr = String.valueOf(map.get("Cooldown")).trim();
                    try {
                        def.setCooldown(Float.parseFloat(cdStr));
                    } catch (Exception e) {
                        def.setCooldown(0.0f);
                        def.setCooldownFormula(cdStr);
                    }
                }

                // Conditions
                Object conds = map.get("Conditions");
                if (conds instanceof List<?> list) {
                    for (Object c : list) {
                        def.getConditions().add(SkillCondition.parse(String.valueOf(c)));
                    }
                }

                // TargetConditions
                Object targetConds = map.get("TargetConditions");
                if (targetConds instanceof List<?> list) {
                    for (Object c : list) {
                        def.getTargetConditions().add(SkillCondition.parse(String.valueOf(c)));
                    }
                }

                // Skills / Mechanics
                Object skillsObj = map.get("Skills");
                if (skillsObj instanceof List<?> list) {
                    int cumulativeDelay = 0;
                    for (Object skillLine : list) {
                        String line = String.valueOf(skillLine).trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;

                        if (line.toLowerCase().startsWith("delay ")) {
                            try {
                                cumulativeDelay += Integer.parseInt(line.substring(6).trim());
                            } catch (Exception ignored) {}
                            continue;
                        }

                        MythicSkillDef.MechanicEntry mechanicEntry = parseMechanicLine(line, cumulativeDelay);
                        if (mechanicEntry != null) {
                            def.addMechanic(mechanicEntry);
                        }
                    }
                }

                result.put(skillId.toUpperCase(), def);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static MythicSkillDef.MechanicEntry parseMechanicLine(String line, int baseDelay) {
        String work = line.trim();
        if (work.isEmpty() || work.startsWith("#")) return null;

        String mechName;
        Map<String, String> params = new HashMap<>();
        String remainder = "";

        int firstBrace = work.indexOf('{');
        if (firstBrace != -1) {
            mechName = work.substring(0, firstBrace).trim().toLowerCase();
            int depth = 0;
            int closingBrace = -1;
            boolean inQuotes = false;
            for (int i = firstBrace; i < work.length(); i++) {
                char c = work.charAt(i);
                if (c == '"') {
                    inQuotes = !inQuotes;
                } else if (!inQuotes) {
                    if (c == '{') {
                        depth++;
                    } else if (c == '}') {
                        depth--;
                        if (depth == 0) {
                            closingBrace = i;
                            break;
                        }
                    }
                }
            }
            if (closingBrace != -1) {
                String inner = work.substring(firstBrace + 1, closingBrace);
                for (String part : splitParams(inner)) {
                    int eq = part.indexOf('=');
                    if (eq != -1) {
                        params.put(part.substring(0, eq).trim().toLowerCase(), part.substring(eq + 1).trim());
                    }
                }
                remainder = work.substring(closingBrace + 1).trim();
            } else {
                remainder = "";
            }
        } else {
            String[] parts = work.split("\\s+", 2);
            mechName = parts[0].trim().toLowerCase();
            remainder = parts.length > 1 ? parts[1].trim() : "";
        }

        if (mechName.startsWith("effect:")) {
            mechName = mechName.substring(7);
        }

        // Targeter: Look ONLY in the remainder after mechanic parameters
        SkillTargeter targeter = null;
        int atIdx = remainder.indexOf('@');
        if (atIdx != -1) {
            int targeterEnd = remainder.length();
            int tBrace = remainder.indexOf('{', atIdx);
            if (tBrace != -1) {
                int tDepth = 0;
                boolean tQuotes = false;
                for (int i = tBrace; i < remainder.length(); i++) {
                    char c = remainder.charAt(i);
                    if (c == '"') {
                        tQuotes = !tQuotes;
                    } else if (!tQuotes) {
                        if (c == '{') tDepth++;
                        else if (c == '}') {
                            tDepth--;
                            if (tDepth == 0) {
                                targeterEnd = i + 1;
                                break;
                            }
                        }
                    }
                }
            } else {
                int spaceIdx = remainder.indexOf(' ', atIdx);
                int qIdx = remainder.indexOf('?', atIdx);
                if (spaceIdx != -1 && (qIdx == -1 || spaceIdx < qIdx)) {
                    targeterEnd = spaceIdx;
                } else if (qIdx != -1) {
                    targeterEnd = qIdx;
                }
            }
            String targeterStr = remainder.substring(atIdx, targeterEnd).trim();
            targeter = SkillTargeter.parse(targeterStr);
            remainder = (remainder.substring(0, atIdx) + " " + remainder.substring(targeterEnd)).trim();
        } else {
            targeter = SkillTargeter.INHERITED;
        }

        // Inline Condition: Look in remainder
        SkillCondition condition = null;
        int qIdx = remainder.indexOf('?');
        if (qIdx != -1) {
            String condStr = remainder.substring(qIdx).trim();
            condition = SkillCondition.parse(condStr);
        }

        // Delay & Repeat
        int delay = baseDelay;
        if (params.containsKey("delay")) {
            try {
                delay += Integer.parseInt(params.get("delay"));
            } catch (Exception ignored) {}
        }

        int repeat = 1;
        if (params.containsKey("repeat")) {
            try {
                repeat = Integer.parseInt(params.get("repeat"));
            } catch (Exception ignored) {}
        }

        int repeatInterval = 1;
        if (params.containsKey("repeati")) {
            try {
                repeatInterval = Integer.parseInt(params.get("repeati"));
            } catch (Exception ignored) {}
        }

        MythicMechanic mechanic = createMechanic(mechName, params);
        if (mechanic == null) return null;

        return new MythicSkillDef.MechanicEntry(mechanic, targeter, condition, delay, repeat, repeatInterval);
    }

    private static MythicMechanic createMechanic(String name, Map<String, String> params) {
        return switch (name) {
            case "skill" -> new SubSkillMechanic(params);
            case "damage" -> new DamageMechanic(params);
            case "aura" -> new AuraMechanic(params);
            case "auraremove" -> new AuraRemoveMechanic(params);
            case "particles", "particle" -> new ParticleMechanic(params);
            case "sound" -> new SoundMechanic(params);
            case "lunge" -> new LungeMechanic(params);
            case "state" -> new StateMechanic(params);
            case "summon" -> new SummonMechanic(params);
            case "projectile" -> new ProjectileMechanic(params);
            case "modifyprojectile" -> new ModifyProjectileMechanic(params);
            case "missile" -> new MissileMechanic(params);
            case "orbital" -> new OrbitalMechanic(params);
            case "totem" -> new TotemMechanic(params);
            case "potion" -> new PotionMechanic(params);
            case "throw" -> new ThrowMechanic(params);
            case "directionalvelocity" -> new DirectionalVelocityMechanic(params);
            default -> context -> {};
        };
    }

    private static List<String> splitParams(String inner) {
        List<String> list = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ';' && !inQuotes) {
                list.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) {
            list.add(current.toString().trim());
        }
        return list;
    }
}
