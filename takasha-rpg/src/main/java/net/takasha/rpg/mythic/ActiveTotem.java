package net.takasha.rpg.mythic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Stationary pulsating ground totem executing periodic skills and an on-expire skill.
 */
public class ActiveTotem {

    private final ServerLevel level;
    private final LivingEntity caster;
    private final Vec3 position;
    private final int maxLifetimeTicks;
    private final int intervalTicks;
    private final String onTickSkill;
    private final String onExpireSkill;
    private final Map<String, Object> variables = new HashMap<>();

    private int ageTicks = 0;
    private boolean removed = false;

    public ActiveTotem(ServerLevel level, LivingEntity caster, Vec3 position,
                       int maxLifetimeTicks, int intervalTicks,
                       String onTickSkill, String onExpireSkill,
                       Map<String, Object> variables) {
        this.level = level;
        this.caster = caster;
        this.position = position;
        this.maxLifetimeTicks = Math.max(1, maxLifetimeTicks);
        this.intervalTicks = Math.max(1, intervalTicks);
        this.onTickSkill = onTickSkill;
        this.onExpireSkill = onExpireSkill;
        if (variables != null) {
            this.variables.putAll(variables);
        }
    }

    public boolean tick() {
        if (removed) return false;

        ageTicks++;
        if (ageTicks % intervalTicks == 0 && onTickSkill != null && !onTickSkill.isBlank()) {
            executeSubSkill(onTickSkill);
        }

        if (ageTicks >= maxLifetimeTicks) {
            if (onExpireSkill != null && !onExpireSkill.isBlank()) {
                executeSubSkill(onExpireSkill);
            }
            removed = true;
            return false;
        }

        return true;
    }

    private void executeSubSkill(String skillName) {
        SkillExecutionContext ctx = new SkillExecutionContext(level, caster, null, position);
        ctx.getVariables().putAll(this.variables);
        MythicSkillExecutor.getInstance().executeSkill(skillName, ctx);
    }

    public void remove() {
        this.removed = true;
    }
}
