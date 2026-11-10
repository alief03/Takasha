package net.takasha.rpg.mythic;

/**
 * Functional interface representing an executable MythicMobs skill mechanic.
 */
@FunctionalInterface
public interface MythicMechanic {
    void execute(SkillExecutionContext context);
}
