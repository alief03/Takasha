package net.sakura.weapons.item.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SakuraKatanaItem extends Item {
    public SakuraKatanaItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        if (attacker.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, target.getX(), target.getY() + 1.0, target.getZ(), 16, 0.35, 0.5, 0.35, 0.08);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 0.8, target.getZ(), 2, 0.1, 0.1, 0.1, 0.0);
        }
        attacker.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0, false, false));
    }
}
