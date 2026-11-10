package net.takasha.rpg.mythic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.takasha.rpg.combat.CombatMitigationEngine;
import net.takasha.rpg.combat.TakashaDamageMetadata;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;
import net.takasha.rpg.skills.AuraStatType;
import net.takasha.rpg.skills.AuraTraitType;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies damage via the PoE Combat Mitigation Engine with AuraSkills stat integration.
 */
public class DamageMechanic implements MythicMechanic {

    private final String amountFormula;
    private final boolean preventKnockback;

    public DamageMechanic(Map<String, String> params) {
        this.amountFormula = params.getOrDefault("a", params.getOrDefault("amount", "<skill.damage>"));
        this.preventKnockback = Boolean.parseBoolean(params.getOrDefault("pkb", "false"));
    }

    @Override
    public void execute(SkillExecutionContext context) {
        LivingEntity target = context.getTarget();
        if (target == null || !target.isAlive() || target == context.getCaster()) return;

        double baseDamage = context.parseDouble(amountFormula, 5.0);
        double burnDuration = context.parseDouble(String.valueOf(context.getVariable("burnduration")), 0.0);
        double burnDamage = context.parseDouble(String.valueOf(context.getVariable("burndamage")), 0.0);

        // AuraSkills Stat Scaling
        double strengthBonus = 1.0;
        double wisdomBonus = 1.0;
        boolean isCrit = false;
        double critMultiplier = 1.5;

        if (context.getCaster() instanceof ServerPlayer player) {
            TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
            if (data != null) {
                double str = data.getStatValue(AuraStatType.STRENGTH);
                strengthBonus = 1.0 + (str * 0.005);
                baseDamage += (str * 0.1);

                double wis = data.getStatValue(AuraStatType.WISDOM);
                wisdomBonus = 1.0 + (wis * 0.008);

                double critChance = 5.0 + data.getTraitValue(AuraTraitType.CRIT_CHANCE);
                if (ThreadLocalRandom.current().nextDouble(0, 100) < critChance) {
                    isCrit = true;
                    critMultiplier = 1.5 + (data.getTraitValue(AuraTraitType.CRIT_DAMAGE) * 0.01);
                }
            }
        }

        TakashaDamageMetadata damageMeta = new TakashaDamageMetadata(context.getCaster(), target);
        damageMeta.setPhysicalDamage(baseDamage * strengthBonus);
        damageMeta.setFireDamage((baseDamage * 0.8) * wisdomBonus);
        damageMeta.setCritical(isCrit);
        damageMeta.setCritMultiplier(critMultiplier);
        damageMeta.setBurnDurationTicks((int) ((burnDuration > 0 ? burnDuration : 3.0) * 20));
        damageMeta.setBurnDamagePerTick(burnDamage > 0 ? burnDamage : 1.0);
        damageMeta.setSourceSkill("MythicMobs");

        CombatMitigationEngine.executeDamage(damageMeta);
    }
}
