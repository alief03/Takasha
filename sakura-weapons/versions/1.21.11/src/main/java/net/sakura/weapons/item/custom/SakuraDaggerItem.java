package net.sakura.weapons.item.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class SakuraDaggerItem extends Item {
    public SakuraDaggerItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        if (attacker.level() instanceof ServerLevel serverLevel) {
            Vec3 attackerLook = attacker.getLookAngle();
            Vec3 targetLook = target.getLookAngle();
            double dot = attackerLook.dot(targetLook);

            // Backstab detected if attacking from behind (angle < 50 degrees, dot > 0.6)
            if (dot > 0.6) {
                DamageSource src = (attacker instanceof Player player)
                    ? serverLevel.damageSources().playerAttack(player)
                    : serverLevel.damageSources().generic();
                target.hurtServer(serverLevel, src, 6.0f);
                serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.3, 0.3, 0.3, 0.2);
                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.5f);
            }
        }
    }
}
