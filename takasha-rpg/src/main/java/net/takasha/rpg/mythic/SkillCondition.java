package net.takasha.rpg.mythic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Condition evaluation for MythicMobs skills and target validation.
 */
public interface SkillCondition {

    record ConditionResult(boolean passes, String castInsteadSkill) {
        public static final ConditionResult PASS = new ConditionResult(true, null);
        public static final ConditionResult FAIL = new ConditionResult(false, null);

        public static ConditionResult castInstead(String skill) {
            return new ConditionResult(false, skill);
        }
    }

    ConditionResult evaluate(SkillExecutionContext context);

    static SkillCondition parse(String conditionStr) {
        if (conditionStr == null || conditionStr.isBlank()) {
            return context -> ConditionResult.PASS;
        }

        String str = conditionStr.trim();
        if (str.startsWith("?")) {
            str = str.substring(1).trim();
        }

        boolean expectedResult = true;
        String castInsteadSkill = null;

        // Check trailing "false", "true", or "castinstead <Skill>"
        if (str.toLowerCase().contains("castinstead")) {
            int idx = str.toLowerCase().indexOf("castinstead");
            castInsteadSkill = str.substring(idx + "castinstead".length()).trim();
            str = str.substring(0, idx).trim();
        } else if (str.endsWith(" false")) {
            expectedResult = false;
            str = str.substring(0, str.length() - 6).trim();
        } else if (str.endsWith(" true")) {
            expectedResult = true;
            str = str.substring(0, str.length() - 5).trim();
        }

        String name;
        Map<String, String> params = new HashMap<>();

        int braceIndex = str.indexOf('{');
        if (braceIndex != -1 && str.endsWith("}")) {
            name = str.substring(0, braceIndex).trim().toLowerCase();
            String inner = str.substring(braceIndex + 1, str.length() - 1);
            for (String part : inner.split(";")) {
                int eq = part.indexOf('=');
                if (eq != -1) {
                    params.put(part.substring(0, eq).trim().toLowerCase(), part.substring(eq + 1).trim());
                }
            }
        } else {
            name = str.trim().toLowerCase();
        }

        final boolean finalExpected = expectedResult;
        final String finalCastInstead = castInsteadSkill;

        return switch (name) {
            case "hasaura" -> context -> {
                String auraName = params.getOrDefault("aura", params.getOrDefault("auraname", params.get("n")));
                LivingEntity ent = context.getTarget() != null ? context.getTarget() : context.getCaster();
                boolean has = ent != null && SkillAuraManager.getInstance().hasAura(ent, auraName);
                if (finalCastInstead != null) {
                    return has ? ConditionResult.castInstead(finalCastInstead) : ConditionResult.PASS;
                }
                return (has == finalExpected) ? ConditionResult.PASS : ConditionResult.FAIL;
            };
            case "hasaurastacks" -> context -> {
                String auraName = params.getOrDefault("auraname", params.getOrDefault("aura", params.get("n")));
                String stacksStr = params.getOrDefault("stacks", "1");
                LivingEntity ent = context.getTarget() != null ? context.getTarget() : context.getCaster();
                int current = ent != null ? SkillAuraManager.getInstance().getStacks(ent, auraName) : 0;
                boolean matched;
                if (stacksStr.startsWith(">=")) {
                    matched = current >= context.parseInt(stacksStr.substring(2).trim(), 1);
                } else if (stacksStr.startsWith(">")) {
                    matched = current > context.parseInt(stacksStr.substring(1).trim(), 1);
                } else if (stacksStr.startsWith("<=")) {
                    matched = current <= context.parseInt(stacksStr.substring(2).trim(), 1);
                } else if (stacksStr.startsWith("<")) {
                    matched = current < context.parseInt(stacksStr.substring(1).trim(), 1);
                } else {
                    matched = current == context.parseInt(stacksStr, 1);
                }
                if (finalCastInstead != null) {
                    return matched ? ConditionResult.castInstead(finalCastInstead) : ConditionResult.PASS;
                }
                return (matched == finalExpected) ? ConditionResult.PASS : ConditionResult.FAIL;
            };
            case "crouching", "iscrouching", "sneaking", "issneaking" -> context -> {
                LivingEntity ent = context.getCaster();
                boolean crouching = ent != null && ent.isCrouching();
                if (finalCastInstead != null) {
                    return crouching ? ConditionResult.castInstead(finalCastInstead) : ConditionResult.PASS;
                }
                return (crouching == finalExpected) ? ConditionResult.PASS : ConditionResult.FAIL;
            };
            case "entitytype" -> context -> {
                String type = params.getOrDefault("t", params.getOrDefault("types", ""));
                LivingEntity target = context.getTarget();
                if (target == null) {
                    return (!finalExpected) ? ConditionResult.PASS : ConditionResult.FAIL;
                }
                // Exact Registry ID match (never Java instanceof)
                Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
                String path = key.getPath().toUpperCase(Locale.ROOT);
                String full = key.toString().toUpperCase(Locale.ROOT);
                String targetTypeParam = type.toUpperCase(Locale.ROOT);

                boolean isMatch = path.equals(targetTypeParam) || full.equals(targetTypeParam);
                if (!isMatch && targetTypeParam.equals("ARMOR_STAND")) {
                    isMatch = key.equals(Identifier.withDefaultNamespace("armor_stand"));
                }
                return (isMatch == finalExpected) ? ConditionResult.PASS : ConditionResult.FAIL;
            };
            case "isliving" -> context -> {
                boolean living = context.getTarget() != null && context.getTarget().isAlive();
                return (living == finalExpected) ? ConditionResult.PASS : ConditionResult.FAIL;
            };
            case "mmocantarget" -> context -> {
                LivingEntity target = context.getTarget();
                if (target == null || target == context.getCaster() || target.isSpectator()) {
                    return (!finalExpected) ? ConditionResult.PASS : ConditionResult.FAIL;
                }
                if (target instanceof Player player && player.isCreative()) {
                    return (!finalExpected) ? ConditionResult.PASS : ConditionResult.FAIL;
                }
                boolean canTarget = target.isAlive();
                return (canTarget == finalExpected) ? ConditionResult.PASS : ConditionResult.FAIL;
            };
            default -> context -> ConditionResult.PASS;
        };
    }
}
