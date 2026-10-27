package net.takasha.rpg.enchant;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.takasha.rpg.entity.DamageDummyEntity;

import java.util.List;
import java.util.Map;

/**
 * TakashaEnchantCombatEngine
 * Engine eksekusi tempur terpusat untuk seluruh custom enchantment RPG & AdvancedEnchantments.
 * Mendukung kalkulasi AOE lingkaran otentik (Immolation, Cleave, Ambit), Mob Slayers, DoT, Armor Defense, dan On-Kill triggers.
 */
public class TakashaEnchantCombatEngine {

    private static final ThreadLocal<Boolean> IS_PROCESSING_COMBAT_ENCHANTS = ThreadLocal.withInitial(() -> false);

    public static boolean isProcessing() {
        return IS_PROCESSING_COMBAT_ENCHANTS.get();
    }

    public static void setProcessing(boolean val) {
        IS_PROCESSING_COMBAT_ENCHANTS.set(val);
    }

    /**
     * Menyelesaikan senjata penyerang secara cerdas:
     * 1. Jika sumber serangan adalah proyektil (panah dari Bow / Crossbow): periksa arrow.getWeaponItem().
     * 2. Fallback ke bow / crossbow yang dipegang penyerang (main/offhand).
     * 3. Fallback ke main hand attacker jika bukan item kosong.
     * 4. Fallback ke offhand attacker.
     */
    public static ItemStack resolveWeapon(LivingEntity attacker, DamageSource source) {
        if (source != null && source.getDirectEntity() != null) {
            net.minecraft.world.entity.Entity direct = source.getDirectEntity();
            if (direct instanceof AbstractArrow arrow) {
                ItemStack weapon = arrow.getWeaponItem();
                if (weapon != null && !weapon.isEmpty()) {
                    return weapon;
                }
            }
            if (direct instanceof Projectile proj && proj.getOwner() instanceof LivingEntity owner) {
                ItemStack main = owner.getMainHandItem();
                if (main.getItem() instanceof BowItem || main.getItem() instanceof CrossbowItem) return main;
                ItemStack off = owner.getOffhandItem();
                if (off.getItem() instanceof BowItem || off.getItem() instanceof CrossbowItem) return off;
                if (!main.isEmpty()) return main;
            }
        }

        if (attacker != null) {
            ItemStack main = attacker.getMainHandItem();
            if (!main.isEmpty()) {
                return main;
            }
            ItemStack off = attacker.getOffhandItem();
            if (!off.isEmpty()) {
                return off;
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * Memproses seluruh custom enchantment dari senjata penyerang terhadap target (Player, Mob, Dummy).
     */
    public static void applyWeaponCombatEnchants(LivingEntity attacker, LivingEntity target, DamageSource source, float damage) {
        if (target == null || target.level().isClientSide()) return;
        if (IS_PROCESSING_COMBAT_ENCHANTS.get()) return;
        if (!(target.level() instanceof ServerLevel level)) return;

        ItemStack weapon = resolveWeapon(attacker, source);
        if (weapon.isEmpty()) return;

        // 1. Eksekusi Efek Khusus Senjata RPG Kustom
        applyRpgWeaponSpecialEffects(attacker, target, source, damage);

        // 2. Eksekusi Set Bonus Armor RPG (Attacker Damage Multipliers)
        applyArmorSetBonusCombatAttacker(attacker, target, source, damage);

        Map<String, Integer> enchants = TakashaEnchantManager.getCustomEnchants(weapon);
        if (enchants.isEmpty()) return;

        // Poin C.2: RPG Balance Soft Cap untuk slot custom enchant > 10 guna mencegah eksponensial power creep
        int activeEnchantsCount = enchants.size();
        float softCap = 1.0f;
        if (activeEnchantsCount > 10) {
            softCap = (float) ((10.0 + Math.sqrt(activeEnchantsCount - 10)) / activeEnchantsCount);
        }

        boolean isDummy = (target instanceof DamageDummyEntity);
        DamageDummyEntity dummy = isDummy ? (DamageDummyEntity) target : null;

        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            String id = entry.getKey().toLowerCase();
            int lvl = entry.getValue();

            // ==========================================
            // 1. MELEE AOE & RADIAL BURST
            // ==========================================

            // IMMOLATION / IMMOLATE -> Radial AOE Burn dengan Cincin FALLING_LAVA
            if (id.equals("immolation") || id.equals("immolate")) {
                double radius = Math.max(2.0, lvl * 2.0); // Lv 1: 2b, Lv 2: 4b, Lv 3: 6b
                int duration = 40 + (lvl * 20); // 60, 80, 100 ticks

                // Target utama terbakar
                target.setRemainingFireTicks(duration);
                if (dummy != null) {
                    dummy.applyAeStatus("burn", duration, lvl);
                }

                // Cincin partikel horizontal FALLING_LAVA dan LAVA di sekeliling pusat benturan
                TakashaAoeHelper.spawnParticleRingDouble(level, ParticleTypes.FALLING_LAVA, ParticleTypes.LAVA, target.position(), radius, (int) (radius * 8));
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 1.1f);

                // Seluruh monster dan dummy dalam radius ikut terbakar
                List<LivingEntity> aoeTargets = TakashaAoeHelper.getEntitiesInRadius(level, target.position(), radius, attacker, true);
                for (LivingEntity near : aoeTargets) {
                    if (near != target) {
                        near.setRemainingFireTicks(duration);
                        if (near instanceof DamageDummyEntity nearDummy) {
                            nearDummy.applyAeStatus("burn", duration, lvl);
                        }
                    }
                }
            }

            // INFLAME -> Radial Flame Burst
            else if (id.equals("inflame")) {
                double radius = Math.max(2.0, lvl * 2.0);
                int duration = 50 + (lvl * 20);

                target.setRemainingFireTicks(duration);
                if (dummy != null) dummy.applyAeStatus("burn", duration, lvl);

                TakashaAoeHelper.spawnParticleRing(level, ParticleTypes.FLAME, target.position(), radius, (int) (radius * 8));
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7f, 1.2f);

                List<LivingEntity> aoeTargets = TakashaAoeHelper.getEntitiesInRadius(level, target.position(), radius, attacker, true);
                for (LivingEntity near : aoeTargets) {
                    if (near != target) {
                        near.setRemainingFireTicks(duration);
                        if (near instanceof DamageDummyEntity nearDummy) {
                            nearDummy.applyAeStatus("burn", duration, lvl);
                        }
                    }
                }
            }

            // CLEAVE / CARVE / MIGHTYCLEAVE -> Radial Sweep Damage & Fracture
            else if (id.equals("cleave") || id.equals("carve") || id.equals("mightycleave")) {
                double radius = 1.5 + (lvl * 0.8);
                float sweepDamage = (1.5f * lvl) * softCap;

                if (dummy != null) {
                    dummy.applyAeStatus("fracture", 80 + lvl * 20, lvl);
                }

                TakashaAoeHelper.spawnParticleRing(level, ParticleTypes.SWEEP_ATTACK, target.position(), radius, Math.max(6, (int) (radius * 3)));
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.0f);

                List<LivingEntity> aoeTargets = TakashaAoeHelper.getEntitiesInRadius(level, target.position(), radius, attacker, true);
                for (LivingEntity near : aoeTargets) {
                    if (near != target) {
                        DamageSource sweepSource = (attacker instanceof Player p) ? target.damageSources().playerAttack(p) : target.damageSources().mobAttack(attacker);
                        near.hurtServer(level, sweepSource, sweepDamage);
                    }
                }
            }

            // DERANGED -> Radial Cosmic Thunder Shock
            else if (id.equals("deranged")) {
                double radius = 4.0 + (lvl * 1.0);
                float zapDamage = (1.0f * lvl) * softCap;

                TakashaAoeHelper.spawnParticleRing(level, ParticleTypes.ELECTRIC_SPARK, target.position(), radius, (int) (radius * 6));
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4f, 1.8f);

                List<LivingEntity> aoeTargets = TakashaAoeHelper.getEntitiesInRadius(level, target.position(), radius, attacker, true);
                for (LivingEntity near : aoeTargets) {
                    if (near != target) {
                        if (near instanceof DamageDummyEntity nearDummy) {
                            nearDummy.applyAeStatus("shock", 60 + lvl * 20, lvl);
                        }
                        DamageSource zapSource = (attacker instanceof Player p) ? target.damageSources().playerAttack(p) : target.damageSources().mobAttack(attacker);
                        near.hurtServer(level, zapSource, zapDamage);
                    }
                }
            }

            // KILLAURA -> Death Aura AoE
            else if (id.equals("killaura")) {
                double radius = 2.0 + (lvl * 0.7);
                float auraDmg = (1.0f * lvl) * softCap;

                TakashaAoeHelper.spawnParticleRing(level, ParticleTypes.SOUL, target.position(), radius, (int) (radius * 5));

                List<LivingEntity> aoeTargets = TakashaAoeHelper.getEntitiesInRadius(level, target.position(), radius, attacker, true);
                for (LivingEntity near : aoeTargets) {
                    if (near != target) {
                        DamageSource auraSource = (attacker instanceof Player p) ? target.damageSources().playerAttack(p) : target.damageSources().mobAttack(attacker);
                        near.hurtServer(level, auraSource, auraDmg);
                    }
                }
            }

            // AMBIT -> Burst wave damage
            else if (id.equals("ambit")) {
                double radius = 2.0 + (lvl * 0.8);
                TakashaAoeHelper.spawnParticleRing(level, ParticleTypes.CRIT, target.position(), radius, (int) (radius * 4));
                List<LivingEntity> aoeTargets = TakashaAoeHelper.getEntitiesInRadius(level, target.position(), radius, attacker, true);
                for (LivingEntity near : aoeTargets) {
                    if (near != target) {
                        DamageSource src = (attacker instanceof Player p) ? target.damageSources().playerAttack(p) : target.damageSources().mobAttack(attacker);
                        near.hurtServer(level, src, (1.0f * lvl) * softCap);
                    }
                }
            }

            // CONTAGION -> Poison Spread AoE
            else if (id.equals("contagion") || id.contains("contagion")) {
                int duration = 100 + lvl * 20;
                target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, Math.min(2, lvl - 1)));
                level.sendParticles(ParticleTypes.SNEEZE, target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.4, 0.4, 0.4, 0.05);

                List<LivingEntity> aoeTargets = TakashaAoeHelper.getEntitiesInRadius(level, target.position(), 4.0, attacker, true);
                for (LivingEntity near : aoeTargets) {
                    if (near != target) {
                        near.addEffect(new MobEffectInstance(MobEffects.POISON, duration / 2, 0));
                    }
                }
            }

            // ==========================================
            // 2. MOB-SPECIFIC SLAYERS (BONUS DAMAGE)
            // ==========================================
            else if (isSlayerEnchant(id)) {
                float bonusMultiplier = getSlayerMultiplier(id, lvl, target);
                if (bonusMultiplier > 0.0f) {
                    float extraDamage = (damage * bonusMultiplier) * softCap;
                    DamageSource slayerSource = (attacker instanceof Player p) ? target.damageSources().playerAttack(p) : target.damageSources().mobAttack(attacker);
                    target.hurtServer(level, slayerSource, extraDamage);
                    level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.2, target.getZ(), 8, 0.25, 0.3, 0.25, 0.08);
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.7f, 1.2f);
                }
            }

            // ==========================================
            // 3. MELEE STATUS DEBUFFS & DOTS
            // ==========================================

            // VIRUS (Ranged / Melee) -> Poison + Wither
            else if (id.equals("virus") || id.contains("virus")) {
                int duration = 100 + lvl * 20;
                int amp = Math.max(0, Math.min(2, lvl - 1));
                target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, amp));
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, amp));
                level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.3, 0.4, 0.3, 0.05);
            }

            // PERISH -> Wither
            else if (id.equals("perish") || id.contains("perish")) {
                int duration = 120 + lvl * 30;
                int amp = Math.min(2, lvl);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, amp));
                level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.25, 0.4, 0.25, 0.02);
            }

            // FROST / PERMAFROST / ICEASPECT / GLACIER -> Freezing + Slowness
            else if (id.contains("freez") || id.contains("frost") || id.contains("ice") || id.contains("chill") || id.contains("glacier")) {
                int duration = 60 + lvl * 20;
                if (dummy != null) dummy.applyAeStatus("freeze", duration, lvl);
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, Math.min(3, lvl)));
                target.setTicksFrozen(target.getTicksFrozen() + duration);
                level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SNOW_GOLEM_HURT, SoundSource.PLAYERS, 0.6f, 1.2f);
            }

            // BLEED / DEEPBLEED / LACERATE / GOUGE / WOUND -> Bleed Status DoT
            else if (id.contains("bleed") || id.contains("lacerat") || id.contains("wound") || id.contains("gouge")) {
                int duration = 100 + lvl * 20;
                if (dummy != null) {
                    dummy.applyAeStatus("bleed", duration, lvl);
                } else {
                    target.hurtServer(level, target.damageSources().generic(), (1.5f * lvl) * softCap);
                }
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.2, target.getZ(), 8, 0.25, 0.4, 0.25, 0.05);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.6f, 0.8f);
            }

            // GENERIC BURN / INFERNO / COMBUST / HELLFIRE / FLAME / ARSONIST / DIVINEIMMOLATION
            else if (id.contains("burn") || id.contains("ignit") || id.contains("inferno") || id.contains("combust") || id.contains("hellfire") || id.contains("flame") || id.contains("arsonist")) {
                int duration = 80 + lvl * 20;
                if (dummy != null) dummy.applyAeStatus("burn", duration, lvl);
                target.setRemainingFireTicks(duration);
                level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.25, 0.4, 0.25, 0.03);
            }

            // SHOCK / THUNDERLORD / LIGHTNING / STRIKE / THOR / SPARK / STORM
            else if (id.contains("shock") || id.contains("electr") || id.contains("lightn") || id.contains("thunder") || id.contains("spark") || id.contains("storm") || id.contains("thor") || id.contains("strike")) {
                int duration = 60 + lvl * 20;
                if (dummy != null) dummy.applyAeStatus("shock", duration, lvl);
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 1));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.2, target.getZ(), 10, 0.3, 0.4, 0.3, 0.08);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4f, 1.8f);
            }

            // SILENCE / MUTE / HUSH
            else if (id.contains("silenc") || id.contains("mute") || id.contains("hush")) {
                int duration = 80 + lvl * 20;
                if (dummy != null) dummy.applyAeStatus("silence", duration, lvl);
                level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1.5, target.getZ(), 6, 0.2, 0.3, 0.2, 0.02);
            }

            // DISARM / DISARMOR
            else if (id.contains("disarm")) {
                int duration = 60 + lvl * 20;
                if (dummy != null) dummy.applyAeStatus("disarm", duration, lvl);
                target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, duration, 2));
                level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 1.0, target.getZ(), 6, 0.2, 0.3, 0.2, 0.02);
            }

            // FRACTURE / ACID / CORROSION / SHRED / SHATTER
            else if (id.contains("fractur") || id.contains("acid") || id.contains("corros") || id.contains("shred") || id.contains("shatter")) {
                int duration = 80 + lvl * 20;
                if (dummy != null) dummy.applyAeStatus("fracture", duration, lvl);
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.2, 0.3, 0.2, 0.05);
            }

            // FLASHBANG -> Blindness + Slowness + Fireworks
            else if (id.contains("flashbang")) {
                int duration = 60 + lvl * 20;
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 1));
                level.sendParticles(ParticleTypes.FIREWORK, target.getX(), target.getY() + 1.5, target.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.8f, 1.2f);
            }

            // CONFUSE / CONFUSION -> Nausea
            else if (id.contains("confus")) {
                target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80 + lvl * 20, 0));
            }

            // DIMINISH / DOMINATE / VOODOO -> Weakness
            else if (id.contains("diminish") || id.contains("dominate")) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80 + lvl * 20, Math.min(2, lvl - 1)));
            }

            // STUN / CRIPPLE / TRAP -> Extreme Slowness
            else if (id.contains("stun") || id.contains("cripple") || id.equals("trap") || id.equals("titantrap")) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60 + lvl * 20, 3));
            }

            // FAMINE / STARVATION -> Hunger
            else if (id.contains("famine") || id.contains("starvation")) {
                target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 80 + lvl * 20, 1));
            }

            // ==========================================
            // 4. MELEE COMBAT PROCS & CRITICALS
            // ==========================================

            // LIFESTEAL / DEMONICLIFESTEAL
            else if (id.contains("lifesteal")) {
                if (attacker != null) {
                    float healAmount = (damage * (0.05f * lvl)) * softCap;
                    attacker.heal(healAmount);
                    level.sendParticles(ParticleTypes.HEART, attacker.getX(), attacker.getY() + 1.5, attacker.getZ(), 3, 0.2, 0.2, 0.2, 0.02);
                }
            }

            // BERSERK / ENRAGE / SHURA -> Strength saat HP rendah
            else if (id.contains("berserk") || id.contains("enrage") || id.equals("shura")) {
                if (attacker != null && attacker.getHealth() < (attacker.getMaxHealth() * 0.5f)) {
                    attacker.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60 + lvl * 20, Math.min(2, lvl - 1)));
                    level.sendParticles(ParticleTypes.ANGRY_VILLAGER, attacker.getX(), attacker.getY() + 1.5, attacker.getZ(), 4, 0.2, 0.2, 0.2, 0.02);
                }
            }

            // CRITICAL / EXECUTE / FINISHING / FIRST_STRIKE
            else if (id.equals("critical") || id.equals("execute") || id.equals("finishing") || id.equals("first_strike")) {
                boolean executeProc = id.equals("execute") && target.getHealth() < (target.getMaxHealth() * 0.35f);
                boolean firstProc = id.equals("first_strike") && target.getHealth() >= (target.getMaxHealth() * 0.95f);
                if (executeProc || firstProc || id.equals("critical")) {
                    float extraCrit = (damage * (0.15f * lvl)) * softCap;
                    DamageSource critSource = (attacker instanceof Player p) ? target.damageSources().playerAttack(p) : target.damageSources().mobAttack(attacker);
                    target.hurtServer(level, critSource, extraCrit);
                    level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.2, target.getZ(), 8, 0.2, 0.2, 0.2, 0.08);
                }
            }

            // PULL_CLOSER: ALLURE / MAGNET / SHACKLE
            else if (id.equals("allure") || id.equals("magnet") || id.equals("shackle")) {
                if (attacker != null) {
                    Vec3 pullVec = attacker.position().subtract(target.position()).normalize().scale(0.5);
                    target.push(pullVec.x, 0.2, pullVec.z);
                }
            }

            // PULL_AWAY / FORCEFIELD / OBLITERATE / REPEL
            else if (id.equals("forcefield") || id.equals("obliterate") || id.equals("repel")) {
                if (attacker != null) {
                    Vec3 pushVec = target.position().subtract(attacker.position()).normalize().scale(0.6 * lvl);
                    target.push(pushVec.x, 0.3, pushVec.z);
                }
            }

            // ==========================================
            // 5. RANGED & PROJECTILE COMBAT
            // ==========================================

            // EXPLOSIVE / FRENZY
            else if (id.equals("explosive") || id.contains("explosive") || id.equals("frenzy")) {
                level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7f, 1.2f);
                List<LivingEntity> splash = TakashaAoeHelper.getEntitiesInRadius(level, target.position(), 2.5, attacker, true);
                for (LivingEntity near : splash) {
                    if (near != target) {
                        near.hurtServer(level, target.damageSources().explosion(null, attacker), (2.0f * lvl) * softCap);
                    }
                }
            }
        }
    }

    /**
     * Memproses custom enchantment pertahanan (Armor) saat defender menerima serangan.
     * @return boolean: true jika damage diizinkan masuk, false jika damage dibatalkan/ditepis (Dodge, Deflect, CreeperArmor).
     */
    public static boolean applyArmorCombatEnchants(LivingEntity defender, LivingEntity attacker, DamageSource source, float damage) {
        if (defender == null || defender.level().isClientSide()) return true;
        if (IS_PROCESSING_COMBAT_ENCHANTS.get()) return true;
        if (!(defender.level() instanceof ServerLevel level)) return true;

        boolean allowDamage = true;

        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = defender.getItemBySlot(slot);
            if (armor.isEmpty()) continue;

            Map<String, Integer> enchants = TakashaEnchantManager.getCustomEnchants(armor);
            for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
                String id = entry.getKey().toLowerCase();
                int lvl = entry.getValue();

                // 1. CREEPERARMOR -> Batalkan damage dari ledakan Creeper / Explosion
                if (id.equals("creeperarmor") && (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION) || source.is(DamageTypeTags.IS_EXPLOSION))) {
                    level.sendParticles(ParticleTypes.SMOKE, defender.getX(), defender.getY() + 1.0, defender.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
                    allowDamage = false;
                }

                // 2. ARROWDEFLECT / ARROWBREAK -> Batalkan proyektil panah
                if ((id.equals("arrowdeflect") || id.equals("arrowbreak")) && source.is(DamageTypeTags.IS_PROJECTILE)) {
                    if (level.getRandom().nextFloat() < (0.20f * lvl)) {
                        level.playSound(null, defender.getX(), defender.getY(), defender.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 1.2f);
                        level.sendParticles(ParticleTypes.ITEM_SNOWBALL, defender.getX(), defender.getY() + 1.2, defender.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
                        allowDamage = false;
                    }
                }

                // 3. DODGE -> Peluang menepis serangan sepenuhnya
                if (id.equals("dodge")) {
                    if (level.getRandom().nextFloat() < (0.08f * lvl)) {
                        level.playSound(null, defender.getX(), defender.getY(), defender.getZ(), SoundEvents.BAT_TAKEOFF, SoundSource.PLAYERS, 0.7f, 1.3f);
                        level.sendParticles(ParticleTypes.POOF, defender.getX(), defender.getY() + 1.0, defender.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
                        allowDamage = false;
                    }
                }

                // 4. JELLYLEGS / FEATHER_STEP -> Batalkan fall damage
                if ((id.equals("jellylegs") || id.equals("feather_step")) && source.is(DamageTypeTags.IS_FALL)) {
                    allowDamage = false;
                }

                // 5. PLUMMET -> Batalkan fall damage dan picu hentakan gempa AoE
                if (id.equals("plummet") && source.is(DamageTypeTags.IS_FALL)) {
                    allowDamage = false;
                    double radius = 3.0 + lvl;
                    float quakeDmg = 2.0f * lvl;
                    TakashaAoeHelper.spawnParticleRing(level, ParticleTypes.EXPLOSION, defender.position(), radius, (int) (radius * 4));
                    level.playSound(null, defender.getX(), defender.getY(), defender.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 0.8f);

                    List<LivingEntity> enemies = TakashaAoeHelper.getEntitiesInRadius(level, defender.position(), radius, defender, true);
                    for (LivingEntity e : enemies) {
                        e.hurtServer(level, defender.damageSources().mobAttack(defender), quakeDmg);
                    }
                }

                // 6. MOLTEN / SCORCHING -> Bakar attacker
                if ((id.contains("molten") || id.contains("scorch")) && attacker != null) {
                    attacker.setRemainingFireTicks(60 + lvl * 20);
                }

                // 7. FROZEN -> Slow attacker
                if (id.contains("frozen") && attacker != null) {
                    attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60 + lvl * 20, Math.min(2, lvl - 1)));
                }

                // 8. UNHOLY -> Wither + Weakness attacker
                if (id.contains("unholy") && attacker != null) {
                    attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, 60 + lvl * 20, 0));
                    attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60 + lvl * 20, 0));
                }

                // 9. SHOCKWAVE -> Pental musuh sekitar
                if (id.contains("shockwave")) {
                    if (level.getRandom().nextFloat() < (0.25f * lvl)) {
                        TakashaAoeHelper.spawnParticleRing(level, ParticleTypes.EXPLOSION, defender.position(), 4.0, 16);
                        level.playSound(null, defender.getX(), defender.getY(), defender.getZ(), SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 0.8f, 1.2f);
                        List<LivingEntity> enemies = TakashaAoeHelper.getEntitiesInRadius(level, defender.position(), 4.0, defender, true);
                        for (LivingEntity e : enemies) {
                            Vec3 diff = e.position().subtract(defender.position()).normalize();
                            e.push(diff.x * 0.8, 0.35, diff.z * 0.8);
                        }
                    }
                }

                // 10. SMOKEBOMB -> Asap tebal dan blindness
                if (id.contains("smokebomb") && defender.getHealth() < (defender.getMaxHealth() * 0.5f)) {
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, defender.getX(), defender.getY() + 1.0, defender.getZ(), 20, 0.6, 0.6, 0.6, 0.02);
                    if (attacker != null) {
                        attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80 + lvl * 20, 0));
                    }
                }

                // 11. REFLECT / REBOUNDING / CACTUS -> Pantulkan sebagian damage
                if ((id.equals("reflect") || id.equals("rebounding") || id.contains("cactus")) && attacker != null) {
                    float reflected = damage * (0.08f * lvl);
                    attacker.hurtServer(level, defender.damageSources().thorns(defender), reflected);
                    level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 3, 0.2, 0.2, 0.2, 0.02);
                }

                // 12. GUARD / SAFEGUARD / WARD -> Absorption
                if (id.contains("guard") || id.contains("ward")) {
                    defender.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, Math.min(2, lvl - 1)));
                }

                // 13. ESCAPE / GETAWAY -> Lonjakan speed saat darah sekarat
                if ((id.equals("escape") || id.equals("getaway")) && defender.getHealth() < (defender.getMaxHealth() * 0.25f)) {
                    defender.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 2));
                    defender.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1));
                    level.sendParticles(ParticleTypes.POOF, defender.getX(), defender.getY() + 1.0, defender.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                }

                // 14. CLEANSING (Clarity, Lucid, Metaphysical, Blessed)
                if (id.contains("clarity")) {
                    defender.removeEffect(MobEffects.BLINDNESS);
                }
                if (id.contains("lucid")) {
                    defender.removeEffect(MobEffects.POISON);
                    defender.removeEffect(MobEffects.DARKNESS);
                }
                if (id.contains("metaphysical") || id.contains("blessed")) {
                    defender.removeEffect(MobEffects.SLOWNESS);
                    defender.removeEffect(MobEffects.NAUSEA);
                    defender.removeEffect(MobEffects.WEAKNESS);
                }
            }
        }

        // 15. Set Bonus Mitigasi Pertahanan Nyata untuk 7 Armor RPG Sets
        if (defender instanceof ServerPlayer player) {
            String fullSet = ArmorSetBonusHandler.getEquippedSet(player);
            if (fullSet != null) {
                switch (fullSet) {
                    case "Ranger" -> {
                        // Resistensi Panah Musuh 25%
                        if (source.is(DamageTypes.ARROW) || source.is(DamageTypeTags.IS_PROJECTILE)) {
                            player.heal(damage * 0.25f);
                            level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
                        }
                    }
                    case "Yeti" -> {
                        // -10% Kerusakan Diterima
                        player.heal(damage * 0.10f);
                        level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
                    }
                    case "Yijki" -> {
                        // -30% Kerusakan Diterima
                        player.heal(damage * 0.30f);
                        level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
                    }
                }
            }
        }

        return allowDamage;
    }

    /**
     * Memproses custom enchantment saat membunuh entitas (On-Kill triggers).
     */
    public static void applyOnKillEnchants(ServerPlayer killer, LivingEntity victim) {
        if (killer == null || victim == null || killer.level().isClientSide()) return;
        if (!(killer.level() instanceof ServerLevel level)) return;

        ItemStack weapon = killer.getMainHandItem();
        if (weapon.isEmpty()) return;

        Map<String, Integer> enchants = TakashaEnchantManager.getCustomEnchants(weapon);
        if (enchants.isEmpty()) return;

        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            String id = entry.getKey().toLowerCase();
            int lvl = entry.getValue();

            // Decapitation -> Peluang menjatuhkan kepala mob
            if (id.equals("decapitation") || id.contains("decapitat")) {
                if (level.getRandom().nextFloat() < (0.15f * lvl)) {
                    ItemStack head = getMobHead(victim);
                    if (!head.isEmpty()) {
                        level.addFreshEntity(new ItemEntity(level, victim.getX(), victim.getY() + 0.5, victim.getZ(), head));
                        level.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + 0.5, victim.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                    }
                }
            }

            // Devour -> Restore Food & Saturation
            if (id.equals("devour")) {
                killer.getFoodData().eat(lvl, 0.3f * lvl);
            }

            // Rebound -> Heal HP
            if (id.equals("rebound")) {
                killer.heal(2.0f * lvl);
                level.sendParticles(ParticleTypes.HEART, killer.getX(), killer.getY() + 1.5, killer.getZ(), 4, 0.2, 0.2, 0.2, 0.02);
            }

            // Inquisitive / Master Inquisitive -> Extra Exp
            if (id.contains("inquisitive")) {
                int expAmount = 6 * lvl;
                level.addFreshEntity(new ExperienceOrb(level, victim.getX(), victim.getY(), victim.getZ(), expAmount));
            }
        }
    }

    // ==========================================
    // HELPER METHODS: MOB SLAYERS & MOB HEADS
    // ==========================================

    public static boolean isSlayerEnchant(String id) {
        return id.equals("bonecrusher") || id.equals("deathpunch") || id.equals("enderslayer")
                || id.equals("enderbane") || id.equals("endmaster") || id.equals("blazereaper")
                || id.equals("netherslayer") || id.equals("netherling") || id.equals("beastslayer")
                || id.equals("hunter") || id.equals("cubism") || id.equals("brightness")
                || id.equals("soulless");
    }

    public static float getSlayerMultiplier(String id, int lvl, LivingEntity target) {
        if (target == null) return 0.0f;
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getPath();

        // 1. Bonecrusher -> Skeleton
        if (id.equals("bonecrusher") && (path.contains("skeleton") || path.contains("stray") || path.contains("bogged"))) {
            return 0.20f * lvl;
        }

        // 2. Deathpunch -> Zombie
        if (id.equals("deathpunch") && (path.contains("zombie") || path.contains("husk") || path.contains("drowned"))) {
            return 0.20f * lvl;
        }

        // 3. Enderslayer / Enderbane / Endmaster -> Enderman, Shulker, Dragon
        if ((id.equals("enderslayer") || id.equals("enderbane") || id.equals("endmaster"))
                && (path.contains("ender") || path.contains("shulker"))) {
            return 0.25f * lvl;
        }

        // 4. Blazereaper / Netherslayer / Netherling -> Nether mobs
        if ((id.equals("blazereaper") || id.equals("netherslayer") || id.equals("netherling"))
                && (target.level().dimension() == Level.NETHER || path.contains("blaze") || path.contains("magma") || path.contains("ghast") || path.contains("piglin") || path.contains("wither") || path.contains("hoglin"))) {
            return 0.25f * lvl;
        }

        // 5. Beastslayer / Hunter -> Arthropods & Beasts
        if ((id.equals("beastslayer") || id.equals("hunter"))
                && (path.contains("spider") || path.contains("silverfish") || path.contains("wolf") || path.contains("ravager") || path.contains("bee"))) {
            return 0.20f * lvl;
        }

        // 6. Cubism -> Slimes & Magma Cubes
        if (id.equals("cubism") && (path.contains("slime") || path.contains("magma_cube"))) {
            return 0.30f * lvl;
        }

        // 7. Brightness -> Low light / Warden
        if (id.equals("brightness") && (path.contains("warden") || target.level().getMaxLocalRawBrightness(target.blockPosition()) <= 4)) {
            return 0.25f * lvl;
        }

        // 8. Soulless -> Hostile Monsters
        if (id.equals("soulless") && (target instanceof Monster || target instanceof Enemy)) {
            return 0.15f * lvl;
        }

        return 0.0f;
    }

    private static ItemStack getMobHead(LivingEntity victim) {
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).getPath();
        if (path.contains("zombie")) return new ItemStack(Items.ZOMBIE_HEAD);
        if (path.contains("wither_skeleton")) return new ItemStack(Items.WITHER_SKELETON_SKULL);
        if (path.contains("skeleton")) return new ItemStack(Items.SKELETON_SKULL);
        if (path.contains("creeper")) return new ItemStack(Items.CREEPER_HEAD);
        if (path.contains("piglin")) return new ItemStack(Items.PIGLIN_HEAD);
        if (path.contains("dragon")) return new ItemStack(Items.DRAGON_HEAD);
        if (victim instanceof Player) return new ItemStack(Items.PLAYER_HEAD);
        return ItemStack.EMPTY;
    }

    /**
     * Memproses efek pasif & kalkulasi combat nyata dari 5 Senjata RPG Kustom.
     */
    public static void applyRpgWeaponSpecialEffects(LivingEntity attacker, LivingEntity target, DamageSource source, float damage) {
        if (attacker == null || target == null || !(target.level() instanceof ServerLevel level)) return;
        ItemStack weapon = resolveWeapon(attacker, source);
        if (weapon.isEmpty() || !(weapon.getItem() instanceof net.takasha.rpg.item.TakashaCustomWeaponItem customWeapon)) return;

        net.takasha.rpg.item.TakashaCustomWeaponItem.WeaponType type = customWeapon.getWeaponType();
        switch (type) {
            case DIMENSIONAL_TRAVELLER -> {
                // +30% Attack Damage & Dimensional Shift
                float extraDamage = damage * 0.30f;
                target.hurtServer(level, source, extraDamage);
                attacker.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, false, false, true));
                level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.4, 0.4, 0.4, 0.1);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.6f);
            }
            case KOTH_SWORD -> {
                // +20% PvP Damage, +50% PvE Damage, Auto Bless
                boolean isPvp = (target instanceof Player);
                float extraDamage = isPvp ? (damage * 0.20f) : (damage * 0.50f);
                target.hurtServer(level, source, extraDamage);
                attacker.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false, true));
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, attacker.getX(), attacker.getY() + 1.2, attacker.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
                level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6f, 1.4f);
            }
            case KOTH_AXE -> {
                // +20% PvP Damage & Cleave AoE
                if (target instanceof Player) {
                    target.hurtServer(level, source, damage * 0.20f);
                }
                double radius = 4.0;
                float cleaveDamage = Math.max(1.5f, damage * 0.40f);
                TakashaAoeHelper.spawnParticleRing(level, ParticleTypes.SWEEP_ATTACK, target.position(), radius, 12);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 0.9f);
                List<LivingEntity> aoeTargets = TakashaAoeHelper.getEntitiesInRadius(level, target.position(), radius, attacker, true);
                for (LivingEntity near : aoeTargets) {
                    if (near != target) {
                        near.hurtServer(level, source, cleaveDamage);
                    }
                }
            }
            case PHANTOM_SCYTHE -> {
                // +25% Damage ke seluruh musuh & Phased Shadow
                float extraDamage = damage * 0.25f;
                target.hurtServer(level, source, extraDamage);
                attacker.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 1, false, false, true));
                attacker.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false, true));
                level.sendParticles(ParticleTypes.SQUID_INK, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.8f, 1.2f);
            }
            case SUPREME_WEAPON -> {
                // +15% Damage, +200% Aura Kemuliaan & bebas lapar/jatuh
                float extraDamage = damage * 0.15f;
                target.hurtServer(level, source, extraDamage);
                level.sendParticles(ParticleTypes.GLOW, attacker.getX(), attacker.getY() + 1.2, attacker.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, attacker.getX(), attacker.getY() + 1.2, attacker.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
                level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.5f);
                if (attacker instanceof Player p) {
                    p.getFoodData().setSaturation(20.0f);
                }
            }
        }
    }

    /**
     * Memproses kalkulasi bonus damage tempur nyata dari 7 Set Bonus Armor RPG.
     */
    public static void applyArmorSetBonusCombatAttacker(LivingEntity attacker, LivingEntity target, DamageSource source, float damage) {
        if (attacker == null || target == null || !(target.level() instanceof ServerLevel level)) return;
        if (!(attacker instanceof ServerPlayer player)) return;

        String fullSet = ArmorSetBonusHandler.getEquippedSet(player);
        if (fullSet == null) return;

        float extraDamage = 0.0f;
        switch (fullSet) {
            case "DimensionalTraveller" -> {
                // +30% Damage
                extraDamage = damage * 0.30f;
            }
            case "Koth" -> {
                // +20% PvP Damage, +50% PvE Damage
                boolean isPvp = (target instanceof Player);
                extraDamage = isPvp ? (damage * 0.20f) : (damage * 0.50f);
            }
            case "Phantom" -> {
                // +25% Damage ke Seluruh Musuh
                extraDamage = damage * 0.25f;
            }
            case "Ranger" -> {
                // +30% Damage Panah
                if (source.is(DamageTypes.ARROW) || source.is(DamageTypeTags.IS_PROJECTILE)) {
                    extraDamage = damage * 0.30f;
                }
            }
            case "Supreme" -> {
                // +15% Damage
                extraDamage = damage * 0.15f;
            }
            case "Yeti" -> {
                // +10% Damage Serangan
                extraDamage = damage * 0.10f;
            }
        }

        if (extraDamage > 0.0f) {
            target.hurtServer(level, source, extraDamage);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
        }
    }
}
