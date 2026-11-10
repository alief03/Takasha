package net.takasha.rpg.mmo;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Random;

/**
 * MMOCombatEngine
 * Menghubungkan seluruh statistik MMOItems & MythicLib ke sistem pertempuran Minecraft 26.2:
 * - Offense: Critical Strike, Elements (Fire, Ice, Lightning, Earth, Light, Darkness), Lifesteal, Spell Vampirism,
 *            PvE Damage, PvP Damage, Weapon/Physical/Magic Damage multipliers.
 * - Defense: Dodge Rating (whoosh SFX, cancel), Parry Rating (shield block SFX, cancel),
 *            PoE Mitigation Formula, Physical/Magic/Projectile/PvE/PvP/Elemental Damage Reductions.
 */
public class MMOCombatEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger("takasha-mmo-combat");
    private static final Random RNG = new Random();
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;
        initialized = true;

        // 1. ALLOW_DAMAGE: Dodge, Parry, Attacker Damage Bonus
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity.level().isClientSide()) return true;

            // 1a. Korban ServerPlayer: Dodge & Parry Check
            if (entity instanceof ServerPlayer victim) {
                if (checkDodgeOrParry(victim, source, amount)) {
                    return false; // Serangan sepenuhnya dinegasikan
                }
            }

            // 1b. Pemain Penyerang memegang MMOItem
            if (source.getEntity() instanceof ServerPlayer attacker) {
                ItemStack mainhand = attacker.getMainHandItem();
                Optional<MMOItemData> optData = MMOItemData.readFromStack(mainhand);
                if (optData.isPresent()) {
                    MMOItemData data = optData.get();
                    handleAttackerStats(attacker, entity, source, amount, data);
                }
            }

            return true;
        });

        // 2. AFTER_DAMAGE: Multi-Type Damage Reductions & Mitigation
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (entity.level().isClientSide() || damageTaken <= 0) return;
            if (entity instanceof ServerPlayer victim) {
                applyDamageReductions(victim, source, damageTaken);
            }
        });

        LOGGER.info("Initialized MMOCombatEngine with ALLOW_DAMAGE and AFTER_DAMAGE listeners.");
    }

    private static boolean checkDodgeOrParry(ServerPlayer victim, DamageSource source, float amount) {
        if (!(victim.level() instanceof ServerLevel slevel)) return false;

        double totalDodge = 0.0;
        double totalParry = 0.0;

        EquipmentSlot[] activeSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND, EquipmentSlot.MAINHAND};
        for (EquipmentSlot slot : activeSlots) {
            ItemStack gear = victim.getItemBySlot(slot);
            Optional<MMOItemData> opt = MMOItemData.readFromStack(gear);
            if (opt.isPresent()) {
                MMOItemData d = opt.get();
                totalDodge += d.getStats().getOrDefault(MMOStatType.DODGE_RATING, 0.0);
                totalParry += d.getStats().getOrDefault(MMOStatType.PARRY_RATING, 0.0);
            }
        }

        // 1. Dodge Check (Cap 80%)
        if (totalDodge > 0) {
            double cappedDodge = Math.min(80.0, totalDodge);
            if (RNG.nextDouble() * 100.0 < cappedDodge) {
                slevel.sendParticles(ParticleTypes.POOF, victim.getX(), victim.getY() + 1.0, victim.getZ(), 10, 0.3, 0.4, 0.3, 0.05);
                victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.8f);
                victim.sendSystemMessage(Component.literal("§b§l[DODGE] §7Kamu berhasil menghindari serangan!"));
                if (source.getEntity() instanceof ServerPlayer attackerPlayer) {
                    attackerPlayer.sendSystemMessage(Component.literal("§c§l[DODGED] §7Seranganmu dihindari oleh musuh!"));
                }
                return true;
            }
        }

        // 2. Parry Check (Cap 80%)
        if (totalParry > 0) {
            double cappedParry = Math.min(80.0, totalParry);
            if (RNG.nextDouble() * 100.0 < cappedParry) {
                slevel.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1.2, victim.getZ(), 12, 0.2, 0.3, 0.2, 0.1);
                victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.2f);
                victim.sendSystemMessage(Component.literal("§e§l[PARRY] §7Kamu menangkis serangan!"));
                if (source.getEntity() instanceof ServerPlayer attackerPlayer) {
                    attackerPlayer.sendSystemMessage(Component.literal("§c§l[PARRIED] §7Seranganmu ditangkis oleh musuh!"));
                }
                return true;
            }
        }

        return false;
    }

    private static void handleAttackerStats(ServerPlayer attacker, LivingEntity target, DamageSource source, float baseDamage, MMOItemData data) {
        if (!(attacker.level() instanceof ServerLevel slevel)) return;

        // 1. Critical Strike Chance & Power
        double critChance = data.getStats().getOrDefault(MMOStatType.CRITICAL_STRIKE_CHANCE, 0.0);
        if (critChance > 0 && RNG.nextDouble() * 100.0 < critChance) {
            double critPower = data.getStats().getOrDefault(MMOStatType.CRITICAL_STRIKE_POWER, 150.0);
            float bonusDamage = (float) (baseDamage * (critPower / 100.0 - 1.0));
            if (bonusDamage > 0) {
                target.hurtServer(slevel, source, bonusDamage);
            }
            slevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.3, 0.5, 0.3, 0.2);
            attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        // 2. Elemental: Fire Damage
        double fireDamage = data.getStats().getOrDefault(MMOStatType.FIRE_DAMAGE, 0.0);
        if (fireDamage > 0) {
            target.igniteForSeconds((int) Math.max(2, Math.round(fireDamage / 3.0)));
            target.hurtServer(slevel, attacker.damageSources().inFire(), (float) fireDamage);
            slevel.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.8, target.getZ(), 10, 0.2, 0.4, 0.2, 0.05);
        }

        // 3. Elemental: Ice Damage
        double iceDamage = data.getStats().getOrDefault(MMOStatType.ICE_DAMAGE, 0.0);
        if (iceDamage > 0) {
            target.setTicksFrozen(target.getTicksFrozen() + (int) (iceDamage * 20));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, false));
            slevel.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 0.8, target.getZ(), 10, 0.2, 0.4, 0.2, 0.05);
        }

        // 4. Elemental: Lightning Damage
        double lightningDamage = data.getStats().getOrDefault(MMOStatType.LIGHTNING_DAMAGE, 0.0);
        if (lightningDamage > 0) {
            target.hurtServer(slevel, attacker.damageSources().lightningBolt(), (float) lightningDamage);
            slevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.3, 0.3, 0.3, 0.1);
        }

        // 4b. Elemental: Earth, Light, Darkness Damage
        double earthDmg = data.getStats().getOrDefault(MMOStatType.EARTH_DAMAGE, 0.0);
        if (earthDmg > 0) {
            target.hurtServer(slevel, source, (float) earthDmg);
            slevel.sendParticles(ParticleTypes.ITEM_SLIME, target.getX(), target.getY() + 0.5, target.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
        }
        double lightDmg = data.getStats().getOrDefault(MMOStatType.LIGHT_DAMAGE, 0.0);
        if (lightDmg > 0) {
            target.hurtServer(slevel, attacker.damageSources().magic(), (float) lightDmg);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
            slevel.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.2, 0.3, 0.2, 0.05);
        }
        double darkDmg = data.getStats().getOrDefault(MMOStatType.DARKNESS_DAMAGE, 0.0);
        if (darkDmg > 0) {
            target.hurtServer(slevel, attacker.damageSources().wither(), (float) darkDmg);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, false));
            slevel.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 0.8, target.getZ(), 8, 0.2, 0.3, 0.2, 0.05);
        }

        // 5. Weapon, Physical, and Magic Damage Multipliers
        double weaponDmgBonus = data.getStats().getOrDefault(MMOStatType.WEAPON_DAMAGE, 0.0);
        if (weaponDmgBonus > 0 && baseDamage > 0) {
            target.hurtServer(slevel, source, (float) (baseDamage * (weaponDmgBonus / 100.0)));
        }
        double physDmgBonus = data.getStats().getOrDefault(MMOStatType.PHYSICAL_DAMAGE, 0.0);
        if (physDmgBonus > 0 && baseDamage > 0) {
            target.hurtServer(slevel, source, (float) (baseDamage * (physDmgBonus / 100.0)));
        }
        double magicDmgBonus = data.getStats().getOrDefault(MMOStatType.MAGIC_DAMAGE, 0.0);
        if (magicDmgBonus > 0 && baseDamage > 0) {
            target.hurtServer(slevel, attacker.damageSources().magic(), (float) (baseDamage * (magicDmgBonus / 100.0)));
        }

        // 6. Lifesteal & Spell Vampirism
        double lifesteal = data.getStats().getOrDefault(MMOStatType.LIFESTEAL, 0.0);
        if (lifesteal > 0 && baseDamage > 0) {
            float healAmount = (float) (baseDamage * (lifesteal / 100.0));
            attacker.heal(healAmount);
        }
        double spellVamp = data.getStats().getOrDefault(MMOStatType.SPELL_VAMPIRISM, 0.0);
        if (spellVamp > 0 && magicDmgBonus > 0) {
            attacker.heal((float) ((baseDamage * (magicDmgBonus / 100.0)) * (spellVamp / 100.0)));
        }

        // 7. PvP Damage (vs ServerPlayer)
        if (target instanceof ServerPlayer) {
            double pvpDamage = data.getStats().getOrDefault(MMOStatType.PVP_DAMAGE, 0.0);
            if (pvpDamage > 0) {
                float extraPvp = (float) (baseDamage * (pvpDamage / 100.0));
                target.hurtServer(slevel, source, extraPvp);
            }
        }

        // 8. PvE Damage (vs Monster / Enemy)
        if (target instanceof Enemy) {
            double pveDamage = data.getStats().getOrDefault(MMOStatType.PVE_DAMAGE, 0.0);
            if (pveDamage > 0) {
                float extraPve = (float) (baseDamage * (pveDamage / 100.0));
                target.hurtServer(slevel, source, extraPve);
            }
        }

        // 9. Undead Damage (vs Undead)
        if (target.isInvertedHealAndHarm()) {
            double undeadDamage = data.getStats().getOrDefault(MMOStatType.UNDEAD_DAMAGE, 0.0);
            if (undeadDamage > 0) {
                float extraUndead = (float) (baseDamage * (undeadDamage / 100.0));
                target.hurtServer(slevel, source, extraUndead);
            }
        }
    }

    private static void applyDamageReductions(ServerPlayer victim, DamageSource source, float damageTaken) {
        double totalDefense = 0.0;
        double dmgReduction = 0.0;
        double pvpReduction = 0.0;
        double pveReduction = 0.0;
        double projReduction = 0.0;
        double magicReduction = 0.0;
        double physReduction = 0.0;
        double fireDefense = 0.0;
        double iceDefense = 0.0;
        double lightningDefense = 0.0;

        EquipmentSlot[] activeSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND, EquipmentSlot.MAINHAND};
        for (EquipmentSlot slot : activeSlots) {
            ItemStack gear = victim.getItemBySlot(slot);
            Optional<MMOItemData> opt = MMOItemData.readFromStack(gear);
            if (opt.isPresent()) {
                MMOItemData d = opt.get();
                totalDefense += d.getStats().getOrDefault(MMOStatType.DEFENSE, 0.0);
                dmgReduction += d.getStats().getOrDefault(MMOStatType.DAMAGE_REDUCTION, 0.0);
                pvpReduction += d.getStats().getOrDefault(MMOStatType.PVP_DAMAGE_REDUCTION, 0.0);
                pveReduction += d.getStats().getOrDefault(MMOStatType.PVE_DAMAGE_REDUCTION, 0.0);
                projReduction += d.getStats().getOrDefault(MMOStatType.PROJECTILE_DAMAGE_REDUCTION, 0.0);
                magicReduction += d.getStats().getOrDefault(MMOStatType.MAGIC_DAMAGE_REDUCTION, 0.0);
                physReduction += d.getStats().getOrDefault(MMOStatType.PHYSICAL_DAMAGE_REDUCTION, 0.0);
                fireDefense += d.getStats().getOrDefault(MMOStatType.FIRE_DEFENSE, 0.0) + d.getStats().getOrDefault(MMOStatType.FIRE_DAMAGE_REDUCTION, 0.0);
                iceDefense += d.getStats().getOrDefault(MMOStatType.ICE_DEFENSE, 0.0);
                lightningDefense += d.getStats().getOrDefault(MMOStatType.LIGHTNING_DEFENSE, 0.0);
            }
        }

        // PoE Mitigation: defense / (defense + 100)
        double defenseMitigation = totalDefense > 0 ? (totalDefense / (totalDefense + 100.0)) : 0.0;
        double bonusReduction = dmgReduction / 100.0;

        if (source.getEntity() instanceof ServerPlayer) {
            bonusReduction += (pvpReduction / 100.0);
        } else if (source.getEntity() instanceof Enemy) {
            bonusReduction += (pveReduction / 100.0);
        }

        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            bonusReduction += (projReduction / 100.0);
        } else if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            bonusReduction += (magicReduction / 100.0);
        } else {
            bonusReduction += (physReduction / 100.0);
        }

        if (source.is(DamageTypeTags.IS_FIRE)) {
            bonusReduction += (fireDefense / 100.0);
        } else if (source.is(DamageTypeTags.IS_FREEZING)) {
            bonusReduction += (iceDefense / 100.0);
        } else if (source.is(DamageTypeTags.IS_LIGHTNING)) {
            bonusReduction += (lightningDefense / 100.0);
        }

        double totalReductionRatio = Math.min(0.90, defenseMitigation + bonusReduction);
        if (totalReductionRatio > 0) {
            float absorbed = damageTaken * (float) totalReductionRatio;
            victim.heal(absorbed);
        }
    }

    /**
     * Metode murni simulasi kalkulasi efek combat untuk verifikasi unit test.
     */
    public static CombatSimulationResult simulateCombat(float baseDamage, MMOItemData weaponData, boolean targetIsMonster, boolean targetIsUndead) {
        return simulateCombat(baseDamage, weaponData, false, targetIsMonster, targetIsUndead);
    }

    public static CombatSimulationResult simulateCombat(float baseDamage, MMOItemData weaponData, boolean targetIsPlayer, boolean targetIsMonster, boolean targetIsUndead) {
        float finalDamage = baseDamage;
        boolean critTriggered = false;

        // Crit
        double critChance = weaponData.getStats().getOrDefault(MMOStatType.CRITICAL_STRIKE_CHANCE, 0.0);
        double critPower = weaponData.getStats().getOrDefault(MMOStatType.CRITICAL_STRIKE_POWER, 150.0);
        if (critChance >= 100.0) {
            critTriggered = true;
            finalDamage *= (float) (critPower / 100.0);
        }

        // Elements
        double fire = weaponData.getStats().getOrDefault(MMOStatType.FIRE_DAMAGE, 0.0);
        finalDamage += (float) fire;
        double ice = weaponData.getStats().getOrDefault(MMOStatType.ICE_DAMAGE, 0.0);
        double lightning = weaponData.getStats().getOrDefault(MMOStatType.LIGHTNING_DAMAGE, 0.0);
        finalDamage += (float) lightning;
        double earth = weaponData.getStats().getOrDefault(MMOStatType.EARTH_DAMAGE, 0.0);
        finalDamage += (float) earth;

        // Weapon & Physical & Magic Multipliers
        double weaponDmg = weaponData.getStats().getOrDefault(MMOStatType.WEAPON_DAMAGE, 0.0);
        finalDamage += (float) (baseDamage * (weaponDmg / 100.0));
        double physDmg = weaponData.getStats().getOrDefault(MMOStatType.PHYSICAL_DAMAGE, 0.0);
        finalDamage += (float) (baseDamage * (physDmg / 100.0));

        // PvP & PvE & Undead
        if (targetIsPlayer) {
            double pvp = weaponData.getStats().getOrDefault(MMOStatType.PVP_DAMAGE, 0.0);
            finalDamage += (float) (baseDamage * (pvp / 100.0));
        }
        if (targetIsMonster) {
            double pve = weaponData.getStats().getOrDefault(MMOStatType.PVE_DAMAGE, 0.0);
            finalDamage += (float) (baseDamage * (pve / 100.0));
        }
        if (targetIsUndead) {
            double undead = weaponData.getStats().getOrDefault(MMOStatType.UNDEAD_DAMAGE, 0.0);
            finalDamage += (float) (baseDamage * (undead / 100.0));
        }

        // Lifesteal
        double lifesteal = weaponData.getStats().getOrDefault(MMOStatType.LIFESTEAL, 0.0);
        float healed = (float) (baseDamage * (lifesteal / 100.0));

        return new CombatSimulationResult(finalDamage, critTriggered, (float) fire, (float) ice, healed);
    }

    public record CombatSimulationResult(float totalDamage, boolean crit, float fireDmg, float iceDmg, float healedHp) {}
}
