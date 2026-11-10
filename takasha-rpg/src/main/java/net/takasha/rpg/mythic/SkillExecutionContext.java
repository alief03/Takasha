package net.takasha.rpg.mythic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Execution context passed down through MythicMobs skill mechanic chains.
 */
public class SkillExecutionContext {

    private final ServerLevel level;
    private final LivingEntity caster;
    private LivingEntity target;
    private Vec3 origin;
    private Vec3 targetLocation;
    private float targetYaw = 0.0f;
    private float targetPitch = 0.0f;
    private boolean hasTargetRotation = false;
    private ActiveProjectile activeProjectile;
    private final Map<String, Object> variables = new HashMap<>();

    public SkillExecutionContext(ServerLevel level, LivingEntity caster, LivingEntity target, Vec3 origin) {
        this.level = level;
        this.caster = caster;
        this.target = target;
        this.origin = origin != null ? origin : (caster != null ? caster.position() : Vec3.ZERO);
        this.targetLocation = target != null ? target.position() : this.origin;
        if (target != null) {
            this.targetYaw = target.getYRot();
            this.targetPitch = target.getXRot();
            this.hasTargetRotation = true;
        } else if (caster != null) {
            this.targetYaw = caster.getYRot();
            this.targetPitch = caster.getXRot();
            this.hasTargetRotation = true;
        }
    }

    public SkillExecutionContext(ServerLevel level, LivingEntity caster, LivingEntity target) {
        this(level, caster, target, caster != null ? caster.position() : Vec3.ZERO);
    }

    public SkillExecutionContext(ServerLevel level, LivingEntity caster) {
        this(level, caster, null, caster != null ? caster.position() : Vec3.ZERO);
    }

    public SkillExecutionContext copy() {
        SkillExecutionContext copy = new SkillExecutionContext(level, caster, target, origin);
        copy.targetLocation = this.targetLocation;
        copy.targetYaw = this.targetYaw;
        copy.targetPitch = this.targetPitch;
        copy.hasTargetRotation = this.hasTargetRotation;
        copy.activeProjectile = this.activeProjectile;
        copy.variables.putAll(this.variables);
        return copy;
    }

    public float getTargetYaw() {
        return hasTargetRotation ? targetYaw : (caster != null ? caster.getYRot() : 0.0f);
    }

    public float getTargetPitch() {
        return hasTargetRotation ? targetPitch : (caster != null ? caster.getXRot() : 0.0f);
    }

    public void setTargetRotation(float yaw, float pitch) {
        this.targetYaw = yaw;
        this.targetPitch = pitch;
        this.hasTargetRotation = true;
    }

    public boolean hasTargetRotation() {
        return hasTargetRotation;
    }

    public ActiveProjectile getActiveProjectile() {
        return activeProjectile;
    }

    public void setActiveProjectile(ActiveProjectile activeProjectile) {
        this.activeProjectile = activeProjectile;
    }

    public void setParameter(String name, Object value) {
        setVariable(name, value);
    }

    public Map<String, Object> getParameters() {
        return variables;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public LivingEntity getCaster() {
        return caster;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public void setTarget(LivingEntity target) {
        this.target = target;
        if (target != null) {
            this.targetLocation = target.position();
        }
    }

    public Vec3 getOrigin() {
        return origin;
    }

    public void setOrigin(Vec3 origin) {
        this.origin = origin;
    }

    public Vec3 getTargetLocation() {
        return targetLocation;
    }

    public void setTargetLocation(Vec3 targetLocation) {
        this.targetLocation = targetLocation;
    }

    public void setVariable(String name, Object value) {
        if (name != null && value != null) {
            variables.put(name.toLowerCase(), value);
        }
    }

    public Object getVariable(String name) {
        return name != null ? variables.get(name.toLowerCase()) : null;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    private static final Pattern RANDOM_FLOAT_PATTERN = Pattern.compile("<random\\.float\\.(-?[0-9.]+)to(-?[0-9.]+)>");
    private static final Pattern VAR_PATTERN = Pattern.compile("<(?:skill|modifier)\\.([a-zA-Z0-9_]+)>");

    /**
     * Resolves variables and random expressions in a numeric/string attribute.
     */
    public String parseString(String input) {
        if (input == null || input.isBlank()) return "";
        String result = input;

        // Replace <random.float.XtoY>
        Matcher randMatcher = RANDOM_FLOAT_PATTERN.matcher(result);
        if (randMatcher.find()) {
            StringBuilder sb = new StringBuilder();
            do {
                try {
                    float min = Float.parseFloat(randMatcher.group(1));
                    float max = Float.parseFloat(randMatcher.group(2));
                    float val = (float) ThreadLocalRandom.current().nextDouble(min, max);
                    randMatcher.appendReplacement(sb, String.format(java.util.Locale.ROOT, "%.3f", val));
                } catch (Exception e) {
                    randMatcher.appendReplacement(sb, "0");
                }
            } while (randMatcher.find());
            randMatcher.appendTail(sb);
            result = sb.toString();
        }

        // Replace <skill.var> or <modifier.var>
        Matcher varMatcher = VAR_PATTERN.matcher(result);
        if (varMatcher.find()) {
            StringBuilder sb = new StringBuilder();
            do {
                String varName = varMatcher.group(1).toLowerCase();
                Object val = variables.get(varName);
                if (val != null) {
                    varMatcher.appendReplacement(sb, String.valueOf(val));
                } else {
                    varMatcher.appendReplacement(sb, "1.0");
                }
            } while (varMatcher.find());
            varMatcher.appendTail(sb);
            result = sb.toString();
        }

        return result;
    }

    /**
     * Evaluates a mathematical string like "10.0 * 1.5 + 2" or parsed variable.
     */
    public double parseDouble(String input, double fallback) {
        if (input == null || input.isBlank()) return fallback;
        String resolved = parseString(input).trim();
        try {
            // Handle simple arithmetic operations: * and +
            if (resolved.contains("*")) {
                String[] parts = resolved.split("\\*");
                double res = 1.0;
                for (String part : parts) {
                    res *= Double.parseDouble(part.trim());
                }
                return res;
            } else if (resolved.contains("+")) {
                String[] parts = resolved.split("\\+");
                double res = 0.0;
                for (String part : parts) {
                    res += Double.parseDouble(part.trim());
                }
                return res;
            }
            return Double.parseDouble(resolved);
        } catch (Exception e) {
            return fallback;
        }
    }

    public float parseFloat(String input, float fallback) {
        return (float) parseDouble(input, fallback);
    }

    public int parseInt(String input, int fallback) {
        return (int) Math.round(parseDouble(input, fallback));
    }
}
