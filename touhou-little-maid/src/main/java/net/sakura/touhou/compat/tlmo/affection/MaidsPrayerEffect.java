package net.sakura.touhou.compat.tlmo.affection;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.sakura.touhou.TouhouLittleMaidMod;

/**
 * MaidsPrayerEffect
 * Efek buff Maid's Prayer (Doa sang Maid) dari modul Touhou Maid Affection.
 * Memberikan pemulihan bertahap, proteksi damage, dan ketahanan knockback.
 */
public class MaidsPrayerEffect extends MobEffect {

    private static final Identifier PRAYER_ARMOR =
            Identifier.fromNamespaceAndPath(TouhouLittleMaidMod.MOD_ID, "maids_prayer_armor");
    private static final Identifier PRAYER_KNOCKBACK =
            Identifier.fromNamespaceAndPath(TouhouLittleMaidMod.MOD_ID, "maids_prayer_knockback");

    public MaidsPrayerEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF69B4); // Pink lembut bercahaya

        this.addAttributeModifier(
                Attributes.ARMOR,
                PRAYER_ARMOR,
                2.0,
                AttributeModifier.Operation.ADD_VALUE
        );
        this.addAttributeModifier(
                Attributes.KNOCKBACK_RESISTANCE,
                PRAYER_KNOCKBACK,
                0.2,
                AttributeModifier.Operation.ADD_VALUE
        );
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.getHealth() < entity.getMaxHealth()) {
            float healAmount = 1.0f + (amplifier * 0.5f);
            entity.heal(healAmount);
            level.sendParticles(ParticleTypes.HEART, entity.getX(), entity.getY() + 1.2, entity.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = Math.max(10, 40 >> amplifier);
        return duration % interval == 0;
    }
}
