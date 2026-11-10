package net.takasha.rpg.mythic;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

/**
 * Adds an aura / buff stack to target entity.
 */
public class AuraMechanic implements MythicMechanic {

    private final String auraName;
    private final String durationFormula;
    private final String maxStacksFormula;

    public AuraMechanic(Map<String, String> params) {
        this.auraName = params.getOrDefault("auraname", params.getOrDefault("aura", params.getOrDefault("n", "BUFF")));
        this.durationFormula = params.getOrDefault("duration", "100");
        this.maxStacksFormula = params.getOrDefault("maxstacks", "1");
    }

    @Override
    public void execute(SkillExecutionContext context) {
        LivingEntity target = context.getTarget() != null ? context.getTarget() : context.getCaster();
        if (target == null) return;

        int duration = context.parseInt(durationFormula, 100);
        int maxStacks = context.parseInt(maxStacksFormula, 1);

        SkillAuraManager.getInstance().addAura(target, auraName, duration, maxStacks);
    }
}
