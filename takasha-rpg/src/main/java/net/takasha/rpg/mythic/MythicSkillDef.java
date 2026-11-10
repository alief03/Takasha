package net.takasha.rpg.mythic;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed definition of a MythicMobs skill containing conditions, targeters, delays, and mechanics.
 */
public class MythicSkillDef {

    public record MechanicEntry(
            MythicMechanic mechanic,
            SkillTargeter targeter,
            SkillCondition inlineCondition,
            int delayTicks,
            int repeatCount,
            int repeatInterval
    ) {}

    private final String id;
    private float cooldown = 0.0f;
    private String cooldownFormula = "";
    private final List<SkillCondition> conditions = new ArrayList<>();
    private final List<SkillCondition> targetConditions = new ArrayList<>();
    private final List<MechanicEntry> mechanics = new ArrayList<>();

    public MythicSkillDef(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public float getCooldown() {
        return cooldown;
    }

    public void setCooldown(float cooldown) {
        this.cooldown = cooldown;
    }

    public String getCooldownFormula() {
        return cooldownFormula;
    }

    public void setCooldownFormula(String cooldownFormula) {
        this.cooldownFormula = cooldownFormula != null ? cooldownFormula : "";
    }

    public List<SkillCondition> getConditions() {
        return conditions;
    }

    public List<SkillCondition> getTargetConditions() {
        return targetConditions;
    }

    public List<MechanicEntry> getMechanics() {
        return mechanics;
    }

    public void addMechanic(MechanicEntry entry) {
        if (entry != null) {
            mechanics.add(entry);
        }
    }
}
