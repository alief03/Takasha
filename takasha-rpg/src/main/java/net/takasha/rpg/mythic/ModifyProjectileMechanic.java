package net.takasha.rpg.mythic;

import java.util.Map;

/**
 * Modifies an in-flight projectile's properties such as velocity.
 */
public class ModifyProjectileMechanic implements MythicMechanic {

    private final String trait;
    private final String action;
    private final String valueFormula;

    public ModifyProjectileMechanic(Map<String, String> params) {
        this.trait = params.getOrDefault("trait", "VELOCITY").toUpperCase();
        this.action = params.getOrDefault("action", "SET").toUpperCase();
        this.valueFormula = params.getOrDefault("value", params.getOrDefault("v", "20"));
    }

    @Override
    public void execute(SkillExecutionContext context) {
        ActiveProjectile proj = context.getActiveProjectile();
        if (proj != null) {
            double val = context.parseDouble(valueFormula, 20.0);
            if ("VELOCITY".equals(trait)) {
                if ("MULTIPLY".equals(action)) {
                    proj.setSpeed(proj.getSpeed() * val);
                } else if ("ADD".equals(action)) {
                    proj.setSpeed(proj.getSpeed() + val);
                } else {
                    proj.setSpeed(val);
                }
            }
        }
    }
}

