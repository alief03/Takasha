package net.takasha.rpg.title;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;
import net.takasha.rpg.optimization.TakashaPerformanceConfig;
import net.takasha.rpg.registry.ModRpgItems;
import net.takasha.rpg.skills.AuraSkillType;
import net.takasha.rpg.world.TakashaMobLevelManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Title Sovereignty Trait Engine
 * Menangani implementasi lengkap 16 Gelar Kedaulatan Resmi (G-01 s/d G-16),
 * efek unik aktif/pasif, pengecekan progres unlock otomatis, dan request equip/unequip.
 */
public class TakashaTitleManager {

    private static final Map<UUID, TakashaTitle> ACTIVE_TITLES = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<TakashaTitle, Long>> COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> KILL_STREAKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SWORD_COMBOS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_HIT_TIMES = new ConcurrentHashMap<>();

    public static void initialize() {
        // 1. Damage & Combat Hook untuk Title Traits
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity.level().isClientSide()) return true;

            // === EFEK SAAT PEMAIN MENERIMA DAMAGE ===
            if (entity instanceof ServerPlayer player) {
                TakashaTitle active = getActiveTitle(player.getUUID());
                if (active != null) {
                    long now = player.level().getGameTime();

                    // G-15: Penguasa Keabadian (Chrono-Stasis Ward)
                    if (active == TakashaTitle.G15 && (player.getHealth() - amount <= 0.0f)) {
                        if (isOffCooldown(player.getUUID(), TakashaTitle.G15, now, 6000)) { // 5 menit = 6000 tick
                            triggerChronoStasis(player);
                            setCooldown(player.getUUID(), TakashaTitle.G15, now);
                            return false; // Batalkan kematian
                        }
                    }

                    // G-01: Sang Pemula Abadi (Adrenaline Surge)
                    if (active == TakashaTitle.G01 && ((player.getHealth() - amount) <= player.getMaxHealth() * 0.15f)) {
                        if (isOffCooldown(player.getUUID(), TakashaTitle.G01, now, 1800)) { // 90s = 1800 tick
                            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 1, false, true));
                            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, 0, false, true));
                            player.sendSystemMessage(Component.literal("§a§l[G-01] §fAdrenaline Surge aktif! Speed II & Resistance I selama 6 detik!"));
                            setCooldown(player.getUUID(), TakashaTitle.G01, now);
                        }
                    }

                    // G-02: Penjelajah Lima Dimensi (Void Anchor)
                    if (active == TakashaTitle.G02 && (source.is(DamageTypes.FELL_OUT_OF_WORLD) || player.getY() < -60.0)) {
                        double safeY = 85.0;
                        player.teleportTo(player.getX(), safeY, player.getZ());
                        player.setDeltaMovement(Vec3.ZERO);
                        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 1, false, true));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.2f);
                        player.sendSystemMessage(Component.literal("§b§l[G-02] §fVoid Anchor! Kamu ditarik kembali ke posisi aman dengan Slow Falling!"));
                        return false; // Batalkan damage void
                    }

                    // G-03: Ksatria Penakluk Badai (Lightning Galvanization)
                    if (active == TakashaTitle.G03 && source.is(DamageTypes.LIGHTNING_BOLT)) {
                        player.heal(20.0f);
                        player.addEffect(new MobEffectInstance(MobEffects.HASTE, 160, 1, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 160, 0, false, true));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.5f);
                        player.sendSystemMessage(Component.literal("§e§l[G-03] §fLightning Galvanization! Sambaran petir memulihkan 20 HP & Attack Speed +25%!"));
                        return false; // Kebal petir
                    }

                    // G-04: Penebas Bayangan Malam (Phantom Riposte)
                    if (active == TakashaTitle.G04 && source.getDirectEntity() instanceof Projectile) {
                        if (player.getRandom().nextFloat() < 0.25f) {
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.5f);
                            if (source.getEntity() instanceof LivingEntity shooter) {
                                shooter.hurtServer((ServerLevel) player.level(), player.damageSources().magic(), 12.0f);
                                player.sendSystemMessage(Component.literal("§9§l[G-04] §fPhantom Riposte! Panah dihindari dan membalas 12 True Damage!"));
                            }
                            return false;
                        }
                    }

                    // G-07: Pembalas Dendam Relik (Vengeful Rebound)
                    if (active == TakashaTitle.G07 && amount >= player.getMaxHealth() * 0.40f) {
                        if (isOffCooldown(player.getUUID(), TakashaTitle.G07, now, 240)) { // 12s
                            if (source.getEntity() instanceof LivingEntity attacker) {
                                float reboundDmg = amount * 0.40f;
                                attacker.hurtServer((ServerLevel) player.level(), player.damageSources().magic(), reboundDmg);
                                player.sendSystemMessage(Component.literal("§c§l[G-07] §fVengeful Rebound memantulkan " + (int) reboundDmg + " True Damage!"));
                            }
                            setCooldown(player.getUUID(), TakashaTitle.G07, now);
                        }
                    }
                }
            }

            // === EFEK SAAT PEMAIN MENYERANG MONSTER ===
            if (source.getEntity() instanceof ServerPlayer attacker && entity instanceof Enemy target) {
                TakashaTitle active = getActiveTitle(attacker.getUUID());
                if (active != null) {
                    long now = attacker.level().getGameTime();

                    // G-09: Algojo Tak Bersuara (Executioner's Guillotine)
                    if (active == TakashaTitle.G09) {
                        float remainingHpPercent = (entity.getHealth() - amount) / entity.getMaxHealth();
                        if (remainingHpPercent > 0.0f && remainingHpPercent <= 0.12f) {
                            entity.kill((ServerLevel) entity.level());
                            attacker.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8f, 1.8f);
                            attacker.sendSystemMessage(Component.literal("§4§l[G-09] §c⚔️ Algojo Tak Bersuara! Musuh terpenggal mati seketika!"));
                            return false;
                        }
                    }

                    // G-11: Pewaris Pedang Sakura (Petal Dance Blade - Combo 5 Hit)
                    if (active == TakashaTitle.G11) {
                        Long lastHit = LAST_HIT_TIMES.get(attacker.getUUID());
                        int combo = SWORD_COMBOS.getOrDefault(attacker.getUUID(), 0);
                        if (lastHit != null && (now - lastHit <= 60)) { // Combo window 3 detik
                            combo++;
                        } else {
                            combo = 1;
                        }
                        LAST_HIT_TIMES.put(attacker.getUUID(), now);
                        SWORD_COMBOS.put(attacker.getUUID(), combo);

                        if (combo >= 5) {
                            SWORD_COMBOS.put(attacker.getUUID(), 0);
                            triggerSakuraPetalTempest(attacker, (LivingEntity) entity);
                        }
                    }

                    // G-12: Dukun Pembawa Petaka (Calamity Hex)
                    if (active == TakashaTitle.G12 && (attacker.fallDistance > 0.0f || attacker.getRandom().nextFloat() < 0.35f)) {
                        LivingEntity livingTarget = (LivingEntity) entity;
                        livingTarget.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0, false, true));
                        livingTarget.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1, false, true));
                        livingTarget.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 160, 1, false, true));
                        attacker.sendSystemMessage(Component.literal("§5§l[G-12] §dCalamity Hex terpasang! Musuh dibutakan dan dilemahkan selama 8s!"));
                    }

                    // G-14: Raja Naga Bintang (Singularity Arrow Pull)
                    if (active == TakashaTitle.G14 && source.getDirectEntity() instanceof Projectile) {
                        if (isOffCooldown(attacker.getUUID(), TakashaTitle.G14, now, 300)) { // 15s = 300 ticks
                            triggerSingularityPull(attacker, entity.position());
                            setCooldown(attacker.getUUID(), TakashaTitle.G14, now);
                        }
                    }
                }
            }

            return true;
        });

        // 2. Kill Event Hooks untuk G-06 (Abyssal Vortex) & G-16 (Bonus Drop)
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.level().isClientSide()) return;
            if (source != null && source.getEntity() instanceof ServerPlayer killer && entity instanceof Enemy) {
                TakashaTitle active = getActiveTitle(killer.getUUID());

                // G-06: Abyssal Vortex (Tiap 5 Kill Streak)
                int streak = KILL_STREAKS.getOrDefault(killer.getUUID(), 0) + 1;
                KILL_STREAKS.put(killer.getUUID(), streak);
                if (active == TakashaTitle.G06 && streak % 5 == 0) {
                    triggerAbyssalVortex(killer, entity.position());
                }

                // G-16: Maharaja Takasha (Bonus Koin & EXP +35%)
                if (active == TakashaTitle.G16) {
                    TakashaRpgData data = TakashaRpgManager.getOrCreateData(killer);
                    data.addBalance(15.0); // Bonus kas kaisar
                    killer.giveExperiencePoints(10);
                }
            }
        });

        // 3. Periodic Ticker untuk Aura Kedaulatan (G-16, G-10, G-05)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % TakashaPerformanceConfig.EQUIPMENT_PASSIVE_BATCH_TICKS != 0) return;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                TakashaTitle active = getActiveTitle(player.getUUID());
                if (active == null) continue;

                ServerLevel level = player.level();

                // G-16: True Sovereignty Aura (Monster Lvl < 300 kabur ketakutan)
                if (active == TakashaTitle.G16) {
                    List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10.0), e -> e instanceof Enemy);
                    for (LivingEntity mob : nearby) {
                        TakashaMobLevelManager.MobLevelData data = TakashaMobLevelManager.getMobData(mob.getUUID());
                        if (data != null && data.level < 300) {
                            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 3, false, false));
                            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 2, false, false));
                        }
                    }
                }

                // G-10: Warlord's Domain (+15% Armor Toughness ke kawan sekitar 12 blok)
                if (active == TakashaTitle.G10) {
                    List<ServerPlayer> allies = level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(12.0));
                    for (ServerPlayer ally : allies) {
                        ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0, false, false));
                    }
                }

                // G-05: Penguasa Api Neraka (Hellfire Burst tiap 3s)
                if (active == TakashaTitle.G05 && (server.getTickCount() % 60 == 0)) {
                    List<LivingEntity> burningMobs = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0), e -> e instanceof Enemy && e.isOnFire());
                    for (LivingEntity mob : burningMobs) {
                        level.sendParticles(ParticleTypes.FLAME, mob.getX(), mob.getY() + 0.5, mob.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
                        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
                        List<LivingEntity> nearbyMobs = level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(4.0), e -> e instanceof Enemy && e != mob);
                        for (LivingEntity splashTarget : nearbyMobs) {
                            splashTarget.hurtServer(level, player.damageSources().onFire(), 10.0f);
                            splashTarget.igniteForSeconds(4.0f);
                        }
                    }
                }
            }
        });
    }

    private static void triggerChronoStasis(ServerPlayer player) {
        player.setHealth(player.getMaxHealth() * 0.5f);
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 50, 4, false, true)); // 2.5s invulnerable
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2, false, true));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.2f, 0.9f);
        player.sendSystemMessage(Component.literal("§6§l[G-15] §eCHRONO-STASIS WARD! §fWaktu membeku 2.5 detik, memulihkan 50% HP!"));
    }

    private static void triggerSakuraPetalTempest(ServerPlayer player, LivingEntity target) {
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.CHERRY_LEAVES, target.getX(), target.getY() + 1.0, target.getZ(), 35, 0.6, 0.6, 0.6, 0.1);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.4f);
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 2, false, true)); // Bleed III
        player.sendSystemMessage(Component.literal("§d§l[G-11] §fCherry Blossom Tempest! Musuh terkena Blindness 3s + Bleed III!"));
    }

    private static void triggerSingularityPull(ServerPlayer player, Vec3 center) {
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.PORTAL, center.x, center.y + 0.5, center.z, 50, 0.5, 0.5, 0.5, 0.2);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.8f, 1.8f);

        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, new AABB(center.x - 8, center.y - 4, center.z - 8, center.x + 8, center.y + 4, center.z + 8), e -> e instanceof Enemy);
        for (LivingEntity mob : nearby) {
            Vec3 dir = center.subtract(mob.position()).normalize().scale(0.8);
            mob.setDeltaMovement(dir.x, 0.3, dir.z);
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2, false, true));
        }
        player.sendSystemMessage(Component.literal("§9§l[G-14] §fSingularity Arrow Pull! Tarikan gravitasi kosmik menarik monster dalam radius 8 blok!"));
    }

    private static void triggerAbyssalVortex(ServerPlayer player, Vec3 pos) {
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y + 0.5, pos.z, 40, 0.6, 0.3, 0.6, 0.05);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 0.8f);

        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos.x - 7, pos.y - 3, pos.z - 7, pos.x + 7, pos.y + 3, pos.z + 7), e -> e instanceof Enemy);
        for (LivingEntity mob : nearby) {
            Vec3 dir = pos.subtract(mob.position()).normalize().scale(0.5);
            mob.setDeltaMovement(dir.x, 0.1, dir.z);
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2, false, true)); // 40% slow
        }
        player.sendSystemMessage(Component.literal("§3§l[G-06] §fAbyssal Vortex aktif! Pusaran jurang menyerap monster di sekitar!"));
    }

    /**
     * Memeriksa dan membuka gelar-gelar baru yang syarat progresnya telah dipenuhi pemain.
     */
    public static void checkAndUnlockTitles(ServerPlayer player) {
        if (player == null) return;
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        boolean anyNew = false;

        for (TakashaTitle title : TakashaTitle.values()) {
            if (data.isTitleUnlocked(title.getCode())) continue;
            if (canUnlockTitle(player, data, title)) {
                data.unlockTitle(title.getCode());
                anyNew = true;
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.2f);
                player.sendSystemMessage(Component.literal("§6§l✦ GELAR BARU TERBUKA! ✦ §r" + title.getColor() + title.getCode() + " " + title.getTitleName()));
                player.sendSystemMessage(Component.literal("  §7Trait Sovereignty: §b" + title.getTraitName() + " §8- " + title.getTraitDescription()));
            }
        }

        if (anyNew) {
            TakashaRpgManager.savePlayerData(player.getUUID());
            TakashaRpgManager.syncToClient(player);
        }
    }

    private static boolean canUnlockTitle(ServerPlayer player, TakashaRpgData data, TakashaTitle title) {
        return switch (title) {
            case G01 -> true; // Sang Pemula Abadi: Selalu terbuka untuk semua petualang
            case G02 -> data.isQuestCompleted("q_2_3") || data.getStat("endShipsOpened") >= 1 || player.level().dimension() == Level.END;
            case G03 -> data.isQuestCompleted("q_3_1") || data.getStat("witherKills") >= 1 || data.getSkillLevel(AuraSkillType.DEFENSE) >= 30;
            case G04 -> data.getSkillLevel(AuraSkillType.ARCHERY) >= 40 || data.getSkillLevel(AuraSkillType.AGILITY) >= 40 || data.getStat("nightMobsKilled") >= 100;
            case G05 -> data.getStat("netherMobsKilled") >= 250 || data.getStat("bloodMoonSurvived") >= 1 || data.getSkillLevel(AuraSkillType.FIGHTING) >= 50;
            case G06 -> data.getStat("highestKillStreak") >= 50 || data.getStat("deepslateOresMined") >= 300;
            case G07 -> data.getStat("mythicBossKills") >= 10 || data.getStat("eliteMobsKilled") >= 30;
            case G08 -> data.getSkillLevel(AuraSkillType.ALCHEMY) >= 40 || data.getStat("magicDustUsed") >= 50;
            case G09 -> data.getStat("instantExecutions") >= 50 || data.getStat("mobsKilled") >= 500;
            case G10 -> data.isQuestCompleted("q_3_2") || data.getStat("arenaWins") >= 1 || data.getStat("raidsWon") >= 5;
            case G11 -> data.getStat("sakuraWeaponKills") >= 100;
            case G12 -> data.getSkillLevel(AuraSkillType.ENCHANTING) >= 40 || data.getStat("bossesKilled") >= 20;
            case G13 -> data.getStat("anvilUses") >= 50 || data.getSkillLevel(AuraSkillType.ENCHANTING) >= 60;
            case G14 -> data.getStat("enderDragonKills") >= 1 || data.getStat("meteorEventsMined") >= 3;
            case G15 -> data.getTotalSkillLevel() >= 500 || data.getStat("cataclysmSurvived") >= 1;
            case G16 -> data.getTotalSkillLevel() >= 1000 || data.isQuestCompleted("q_4_3");
        };
    }

    /**
     * Memproses permintaan pemain untuk memakai (equip) atau melepas gelar.
     */
    public static void handleEquipTitleRequest(ServerPlayer player, String titleCode) {
        if (player == null) return;
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);

        if (titleCode == null || titleCode.isEmpty() || titleCode.equalsIgnoreCase("CLEAR") || titleCode.equalsIgnoreCase("NONE")) {
            data.setActiveTitle("");
            setActiveTitle(player.getUUID(), null);
            player.sendSystemMessage(Component.literal("§e[Gelar] §7Gelar kedaulatan telah dilepas."));
            TakashaRpgManager.savePlayerData(player.getUUID());
            TakashaRpgManager.syncToClient(player);
            return;
        }

        TakashaTitle title = TakashaTitle.fromCodeOrName(titleCode);
        if (title == null) {
            player.sendSystemMessage(Component.literal("§c[Gelar] Kode gelar '" + titleCode + "' tidak ditemukan."));
            return;
        }

        if (!data.isTitleUnlocked(title.getCode())) {
            player.sendSystemMessage(Component.literal("§c[Gelar] Gelar " + title.getTitleName() + " masih terkunci! Selesaikan syarat progresnya terlebih dahulu."));
            return;
        }

        data.setActiveTitle(title.getCode());
        setActiveTitle(player.getUUID(), title);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.5f);
        player.sendSystemMessage(Component.literal("§a[Gelar] §fKamu sekarang menyandang gelar " + title.getColor() + "★ " + title.getCode() + " " + title.getTitleName() + "§f!"));
        player.sendSystemMessage(Component.literal("§7Trait aktif: §b" + title.getTraitName() + " §8- " + title.getTraitDescription()));

        TakashaRpgManager.savePlayerData(player.getUUID());
        TakashaRpgManager.syncToClient(player);
    }

    // Helper untuk G-08 Soul Transmutation
    public static void onSoulUsed(ServerPlayer player, int soulAmount) {
        if (player == null || soulAmount <= 0) return;
        TakashaTitle active = getActiveTitle(player.getUUID());
        if (active == TakashaTitle.G08) {
            TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
            long totalUsed = data.addStat("soulsTransmuted", soulAmount);
            if (totalUsed % 50 == 0) {
                ItemStack gem = new ItemStack(ModRpgItems.TAKASHA_GEM, 1);
                if (!player.getInventory().add(gem)) {
                    player.drop(gem, false);
                }
                player.sendSystemMessage(Component.literal("§d§l[G-08] §fSoul Transmutation! 50 Soul terpakai disintesis menjadi 1 Arcane Gem!"));
            }
        }
    }

    // Helper untuk G-13 Anvil & Dust Affinity
    public static float getAnvilExpMultiplier(ServerPlayer player) {
        if (player == null) return 1.0f;
        TakashaTitle active = getActiveTitle(player.getUUID());
        return (active == TakashaTitle.G13) ? 0.65f : 1.0f;
    }

    public static float getMagicDustFailureMultiplier(ServerPlayer player) {
        if (player == null) return 1.0f;
        TakashaTitle active = getActiveTitle(player.getUUID());
        return (active == TakashaTitle.G13) ? 0.50f : 1.0f;
    }

    private static boolean isOffCooldown(UUID uuid, TakashaTitle title, long now, long cdTicks) {
        Map<TakashaTitle, Long> map = COOLDOWNS.get(uuid);
        if (map == null) return true;
        Long last = map.get(title);
        return last == null || (now - last >= cdTicks);
    }

    private static void setCooldown(UUID uuid, TakashaTitle title, long now) {
        COOLDOWNS.computeIfAbsent(uuid, u -> new HashMap<>()).put(title, now);
    }

    public static TakashaTitle getActiveTitle(UUID uuid) {
        return ACTIVE_TITLES.get(uuid);
    }

    public static void setActiveTitle(UUID uuid, TakashaTitle title) {
        if (title == null) {
            ACTIVE_TITLES.remove(uuid);
        } else {
            ACTIVE_TITLES.put(uuid, title);
        }
    }
}
