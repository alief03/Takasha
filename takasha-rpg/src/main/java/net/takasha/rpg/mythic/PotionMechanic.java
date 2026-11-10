package net.takasha.rpg.mythic;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;
import java.util.Map;

/**
 * Applies status effects (e.g. SLOW, SPEED) to target or caster.
 */
public class PotionMechanic implements MythicMechanic {

    private final String effectType;
    private final String durationFormula;
    private final String levelFormula;
    private final boolean hasParticles;

    public PotionMechanic(Map<String, String> params) {
        this.effectType = params.getOrDefault("type", params.getOrDefault("t", "SLOW")).toUpperCase(Locale.ROOT);
        this.durationFormula = params.getOrDefault("duration", params.getOrDefault("d", "20"));
        this.levelFormula = params.getOrDefault("level", params.getOrDefault("l", "1"));
        this.hasParticles = Boolean.parseBoolean(params.getOrDefault("hasparticles", params.getOrDefault("p", "false")));
    }

    @Override
    public void execute(SkillExecutionContext context) {
        LivingEntity target = context.getTarget() != null ? context.getTarget() : context.getCaster();
        if (target == null || target.isDeadOrDying()) return;

        int durationTicks = Math.max(1, context.parseInt(durationFormula, 20));
        int level = Math.max(1, context.parseInt(levelFormula, 1));
        int amplifier = Math.max(0, level - 1);

        Holder<MobEffect> effectHolder = resolveEffect(effectType);
        if (effectHolder != null) {
            target.addEffect(new MobEffectInstance(effectHolder, durationTicks, amplifier, false, hasParticles, true));
        }
    }

    private Holder<MobEffect> resolveEffect(String type) {
        return switch (type) {
            case "SLOW", "SLOWNESS" -> MobEffects.SLOWNESS;
            case "SPEED" -> MobEffects.SPEED;
            case "FIRE_RESISTANCE" -> MobEffects.FIRE_RESISTANCE;
            case "REGENERATION" -> MobEffects.REGENERATION;
            case "RESISTANCE" -> MobEffects.RESISTANCE;
            case "STRENGTH", "INCREASE_DAMAGE" -> MobEffects.STRENGTH;
            case "WEAKNESS" -> MobEffects.WEAKNESS;
            case "POISON" -> MobEffects.POISON;
            case "WITHER" -> MobEffects.WITHER;
            case "GLOWING" -> MobEffects.GLOWING;
            case "LEVITATION" -> MobEffects.LEVITATION;
            case "BLINDNESS" -> MobEffects.BLINDNESS;
            default -> {
                Identifier id = Identifier.tryParse(type.toLowerCase(Locale.ROOT));
                if (id != null && BuiltInRegistries.MOB_EFFECT.containsKey(id)) {
                    yield BuiltInRegistries.MOB_EFFECT.get(id).orElse(null);
                }
                yield null;
            }
        };
    }
}
