package net.takasha.rpg.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Random;

public class CombatMitigationEngine {

    private static final Random RANDOM = new Random();

    /**
     * PoE Defense Mitigation Formula:
     * DR = Defense / (Defense + 5 * RawDamage)
     * EffectiveDamage = RawDamage * (1.0 - DR)
     */
    public static double calculatePoEMitigation(double rawDamage, double defense) {
        if (rawDamage <= 0) return 0;
        if (defense <= 0) return rawDamage;

        double dr = defense / (defense + 5.0 * rawDamage);
        dr = Math.min(0.85, dr); // Cap maximum defense reduction at 85%
        return rawDamage * (1.0 - dr);
    }

    public static void executeDamage(TakashaDamageMetadata meta) {
        LivingEntity target = meta.getTarget();
        LivingEntity attacker = meta.getAttacker();
        if (target == null || target.isDeadOrDying() || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Get target defense (vanilla armor value + bonus)
        double targetDefense = target.getArmorValue();

        // Calculate mitigation
        double physical = calculatePoEMitigation(meta.getPhysicalDamage(), targetDefense);
        double fire = meta.getFireDamage(); // Elemental bypasses physical armor, checked against fire resistance
        if (target.fireImmune()) {
            fire = 0.0;
        }
        double magic = meta.getMagicDamage();

        double total = physical + fire + magic;
        if (meta.isCritical()) {
            total *= meta.getCritMultiplier();
        }

        if (total <= 0) return;

        // Create damage source
        DamageSource source;
        if (attacker instanceof Player player) {
            source = serverLevel.damageSources().playerAttack(player);
        } else if (attacker != null) {
            source = serverLevel.damageSources().mobAttack(attacker);
        } else {
            source = serverLevel.damageSources().magic();
        }

        // Reset vanilla invulnerability ticks so rapid RPG skill combos & missiles register
        target.invulnerableTime = 0;

        // Apply server damage
        target.hurtServer(serverLevel, source, (float) total);
        target.invulnerableTime = 0;

        // Burn duration & ticks
        if (meta.getBurnDurationTicks() > 0 && !target.fireImmune()) {
            int currentFire = target.getRemainingFireTicks();
            target.setRemainingFireTicks(Math.max(currentFire, meta.getBurnDurationTicks()));
        }

        // Particles & SFX (Only emit critical hit burst; no forced flame/lava particle clutter)
        if (meta.isCritical()) {
            serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 15, 0.3, 0.3, 0.3, 0.15);
            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        // Feedback to attacker (Send via Action Bar to prevent chat spam & FPS/packet lag)
        if (attacker instanceof ServerPlayer sp) {
            String critTag = meta.isCritical() ? "§c§lCRIT! " : "";
            String msg = String.format("§6⚔ %s§f%.1f dmg §7(§c%s§7)", critTag, total, meta.getSourceSkill().isEmpty() ? "Attack" : meta.getSourceSkill());
            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg), true);
        }
    }
}
