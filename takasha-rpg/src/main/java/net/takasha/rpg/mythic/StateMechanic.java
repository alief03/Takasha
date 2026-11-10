package net.takasha.rpg.mythic;

import net.minecraft.world.entity.LivingEntity;
import net.takasha.rpg.modelengine.TakashaModelEngine;

import java.util.Map;

/**
 * Changes active animation state on ModelEngine model attached to entity.
 */
public class StateMechanic implements MythicMechanic {

    private final String stateName;
    private final String speedFormula;

    public StateMechanic(Map<String, String> params) {
        this.stateName = params.getOrDefault("s", params.getOrDefault("state", "animation"));
        this.speedFormula = params.getOrDefault("speed", params.getOrDefault("p", "1.0"));
    }

    @Override
    public void execute(SkillExecutionContext context) {
        LivingEntity target = context.getTarget() != null ? context.getTarget() : context.getCaster();
        if (target == null) return;

        float speed = context.parseFloat(speedFormula, 1.0f);
        TakashaModelEngine.setEntityModelState(target, stateName, speed);
    }
}
