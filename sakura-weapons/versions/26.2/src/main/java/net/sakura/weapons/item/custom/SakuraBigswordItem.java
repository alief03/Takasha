package net.sakura.weapons.item.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SakuraBigswordItem extends Item {
    public SakuraBigswordItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        if (attacker.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.5, 0.5, 0.5, 0.15);
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, target.getX(), target.getY() + 0.5, target.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
        }
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, true));
    }
}
