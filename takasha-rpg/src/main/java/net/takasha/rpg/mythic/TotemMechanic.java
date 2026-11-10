package net.takasha.rpg.mythic;

import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Spawns an ActiveTotem at the target or origin position.
 */
public class TotemMechanic implements MythicMechanic {

    private final String maxDurationFormula;
    private final String intervalFormula;
    private final String onTickSkill;
    private final String onExpireSkill;

    public TotemMechanic(Map<String, String> params) {
        this.maxDurationFormula = params.getOrDefault("md", params.getOrDefault("maxduration", "80"));
        this.intervalFormula = params.getOrDefault("i", params.getOrDefault("interval", "10"));
        this.onTickSkill = params.getOrDefault("ot", params.getOrDefault("ontick", ""));
        this.onExpireSkill = params.getOrDefault("oe", params.getOrDefault("onexpire", ""));
    }

    @Override
    public void execute(SkillExecutionContext context) {
        Vec3 pos = context.getTargetLocation();
        if (pos == null) pos = context.getOrigin();

        int md = context.parseInt(maxDurationFormula, 80);
        int interval = context.parseInt(intervalFormula, 10);

        ActiveTotem totem = new ActiveTotem(
                context.getLevel(),
                context.getCaster(),
                pos,
                md,
                interval,
                onTickSkill,
                onExpireSkill,
                context.getVariables()
        );

        MythicSkillExecutor.getInstance().addTotem(totem);
    }
}
