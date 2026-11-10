package net.takasha.rpg.mythic;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

/**
 * Removes an aura / buff stack from target entity and cleans up associated orbital models.
 */
public class AuraRemoveMechanic implements MythicMechanic {

    private final String auraName;

    public AuraRemoveMechanic(Map<String, String> params) {
        this.auraName = params.getOrDefault("auraname", params.getOrDefault("aura", params.getOrDefault("n", "BUFF")));
    }

    @Override
    public void execute(SkillExecutionContext context) {
        LivingEntity target = context.getTarget() != null ? context.getTarget() : context.getCaster();
        if (target != null) {
            SkillAuraManager.getInstance().removeAura(target, auraName);
            ActiveOrbital orbital = MythicSkillExecutor.getInstance().getOrbital(target, auraName);
            if (orbital != null) {
                orbital.remove();
            }
        }
    }
}
