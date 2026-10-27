package net.sakura.touhou.compat.enchant;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.sakura.touhou.entity.EntityMaid;
import net.takasha.rpg.enchant.TakashaEnchantCombatEngine;
import net.takasha.rpg.enchant.TakashaEnchantManager;

/**
 * MaidEnchantBridge
 * Jembatan eksekusi Custom Enchantments & AdvancedEnchantments untuk EntityMaid.
 * Memastikan maid memicu custom enchant senjata, proteksi armor, dan resonansi set legendaris.
 */
public class MaidEnchantBridge {

    public static boolean isRpgActive() {
        return FabricLoader.getInstance().isModLoaded("takasha_rpg");
    }

    /**
     * Dipanggil saat maid menyerang target musuh secara fisik atau menggunakan panah.
     */
    public static void onMeleeAttack(EntityMaid maid, LivingEntity target, DamageSource source, float damage) {
        if (!isRpgActive() || maid == null || target == null || maid.level().isClientSide()) return;
        if (!(maid.level() instanceof ServerLevel level)) return;

        // 1. Eksekusi Seluruh Custom Enchantment Senjata
        TakashaEnchantCombatEngine.applyWeaponCombatEnchants(maid, target, source, damage);

        // 2. Eksekusi Set Resonance Aura dari Pemilik
        String resonance = maid.getActiveSetResonance();
        if (resonance != null) {
            switch (resonance) {
                case "Supreme" -> {
                    // +15% Damage & Efek Kobaran Api Halus
                    target.hurtServer(level, source, damage * 0.15f);
                    level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1.0, target.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
                }
                case "Yeti" -> {
                    // 20% Peluang Membekukan Target (Slowness III)
                    if (maid.getRandom().nextFloat() < 0.20f) {
                        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
                        level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
                    }
                }
                case "Phantom" -> {
                    // +25% Damage Ekstra
                    target.hurtServer(level, source, damage * 0.25f);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 1.0, target.getZ(), 5, 0.2, 0.2, 0.2, 0.02);
                }
                case "Koth" -> {
                    // +50% PvE Damage Bonus
                    if (!(target instanceof Player)) {
                        target.hurtServer(level, source, damage * 0.50f);
                    }
                }
            }
        }
    }

    /**
     * Dipanggil saat maid menerima damage (Armor Custom Enchantment triggers).
     * @return true jika damage diizinkan masuk, false jika berhasil dihindari (Dodge).
     */
    public static boolean onHurt(EntityMaid maid, LivingEntity attacker, DamageSource source, float damage) {
        if (!isRpgActive() || maid == null || maid.level().isClientSide()) return true;

        // 1. Efek Phantom Dodge (10% peluang menghindar penuh jika pemilik memakai Phantom Set)
        if ("Phantom".equals(maid.getActiveSetResonance())) {
            if (maid.getRandom().nextFloat() < 0.10f) {
                if (maid.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.POOF, maid.getX(), maid.getY() + 0.8, maid.getZ(), 8, 0.2, 0.3, 0.2, 0.02);
                }
                return false; // Serangan dihindari
            }
        }

        // 2. Eksekusi Armor Defense Enchantments
        return TakashaEnchantCombatEngine.applyArmorCombatEnchants(maid, attacker, source, damage);
    }

    /**
     * Dipanggil saat maid membunuh mob musuh.
     */
    public static void onKill(EntityMaid maid, LivingEntity victim) {
        if (!isRpgActive() || maid == null || victim == null || maid.level().isClientSide()) return;
        if (maid.getOwner() instanceof ServerPlayer player) {
            TakashaEnchantCombatEngine.applyOnKillEnchants(player, victim);
        } else {
            var weapon = maid.getMainHandItem();
            if (!weapon.isEmpty()) {
                var enchants = TakashaEnchantManager.getCustomEnchants(weapon);
                if (enchants.containsKey("rebound")) {
                    maid.heal(2.0f * enchants.get("rebound"));
                }
            }
        }
    }

    /**
     * Pengecekan keamanan ramah lingkungan (Friendly Fire Guard).
     * Menjamin serangan maid atau sihir area tidak pernah melukai pemilik, sesama maid, atau pet jinak.
     */
    public static boolean isFriendly(EntityMaid maid, Entity target) {
        if (maid == null || target == null) return false;
        if (target == maid) return true;

        // Pemilik maid
        LivingEntity owner = maid.getOwner();
        if (owner != null && target == owner) return true;

        // Pet lain milik pemilik yang sama
        if (target instanceof TamableAnimal otherPet && owner != null) {
            if (otherPet.getOwner() == owner) return true;
        }

        // Player sekutu dalam PvE santai
        if (target instanceof Player) {
            return true;
        }

        return false;
    }
}
