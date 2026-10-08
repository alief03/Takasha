package net.sakura.weapons.item.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class SakuraHammerItem extends Item {
    public SakuraHammerItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        if (attacker.level() instanceof ServerLevel serverLevel) {
            AABB area = target.getBoundingBox().inflate(3.5);
            List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> e != attacker && e != target && e.isAlive()
            );

            DamageSource src = (attacker instanceof Player player)
                ? serverLevel.damageSources().playerAttack(player)
                : serverLevel.damageSources().generic();

            for (LivingEntity entity : nearby) {
                entity.hurtServer(serverLevel, src, 4.5f);
                entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, false));
                entity.push(
                    (entity.getX() - target.getX()) * 0.4,
                    0.25,
                    (entity.getZ() - target.getZ()) * 0.4
                );
            }

            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, target.getX(), target.getY() + 0.5, target.getZ(), 30, 1.5, 0.3, 1.5, 0.1);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.2, target.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }
}
