package net.takasha.rpg.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.takasha.rpg.enchant.TakashaEnchantManager;

import java.util.*;

/**
 * DamageDummyEntity
 * Training Combat Dummy berbasis ArmorStand untuk pengujian kalkulasi DPS, mitigasi armor,
 * floating damage indicator (Display.TextDisplay), serta visualisasi overhead status
 * yang mendukung SELURUH status efek dari AdvancedEnchantments & Vanilla.
 */
public class DamageDummyEntity extends ArmorStand {

    public record HitRecord(long gameTime, float damage) {}
    public record FloatingIndicator(Display.TextDisplay display, int maxAge, int[] ageHolder, double vx, double vy, double vz) {}

    private final List<HitRecord> recentHits = new ArrayList<>();
    private final List<FloatingIndicator> indicators = new ArrayList<>();

    private float lastDamage = 0.0f;
    private float peakDamage = 0.0f;
    private float totalDamage = 0.0f;
    private int hitCount = 0;
    private long lastHitTime = 0;
    private int indicatorCounter = 0;
    private boolean isProcessingHit = false;

    // AdvancedEnchantments Custom Status Effects
    private int bleedTicks = 0;
    private int bleedAmp = 0;
    private int freezeTicks = 0;
    private int burnTicks = 0;
    private int shockTicks = 0;
    private int silenceTicks = 0;
    private int disarmTicks = 0;
    private float guardShield = 0.0f;
    private int fractureTicks = 0;
    private int snowblindTicks = 0;

    // Overhead multi-line hologram display
    private Display.TextDisplay overheadDisplay;
    private UUID overheadDisplayUuid;

    public DamageDummyEntity(EntityType<? extends ArmorStand> entityType, Level level) {
        super(entityType, level);
        this.setShowArms(true);
        this.setNoBasePlate(false);
        this.setCustomName(Component.literal("§6§l[ TARGET DUMMY ]"));
        this.setCustomNameVisible(false); // Matikan nametag native agar tidak bertabrakan dengan TextDisplay
    }

    public static AttributeSupplier.Builder createAttributes() {
        return ArmorStand.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return true; // Mendukung SEMUA efek potion vanilla
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isRemoved() || this.isProcessingHit) return false;
        this.isProcessingHit = true;

        try {
            // Shift + Pukul dengan tangan kosong oleh admin/creative -> Hapus Dummy
            if (source.getEntity() instanceof ServerPlayer player && player.isCreative() && player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
                player.sendSystemMessage(Component.literal("§e§l[Target Dummy] §cDummy berhasil dihapus!"));
                this.discard();
                return true;
            }

            // Mitigasi dengan Guard Shield jika ada
            float effectiveDamage = amount;
            if (this.guardShield > 0.0f) {
                if (effectiveDamage <= this.guardShield) {
                    this.guardShield -= effectiveDamage;
                    effectiveDamage = 0.0f;
                } else {
                    effectiveDamage -= this.guardShield;
                    this.guardShield = 0.0f;
                }
            }

            if (effectiveDamage > 0.0f) {
                long now = level.getGameTime();

                // Deteksi Critical Hit (Vanilla Jump-Crit atau AuraSkills Archery Crit Chance) & Strength
                boolean isCrit = false;
                if (source.getEntity() instanceof ServerPlayer serverPlayer) {
                    net.takasha.rpg.data.TakashaRpgData data = net.takasha.rpg.data.TakashaRpgManager.getOrCreateData(serverPlayer);
                    double critChance = data.getTraitValue(net.takasha.rpg.skills.AuraTraitType.CRIT_CHANCE);
                    double critDmg = data.getTraitValue(net.takasha.rpg.skills.AuraTraitType.CRIT_DAMAGE);
                    boolean isArrow = source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile;
                    boolean isJumpCrit = !isArrow && serverPlayer.fallDistance > 0.0F && !serverPlayer.onGround() && !serverPlayer.onClimbable() && !serverPlayer.isInWater() && !serverPlayer.hasEffect(MobEffects.BLINDNESS);
                    boolean isChanceCrit = serverPlayer.getRandom().nextDouble() * 100.0 < critChance;
                    isCrit = isJumpCrit || isChanceCrit;

                    if (isCrit) {
                        if (isJumpCrit) {
                            effectiveDamage = (float) (effectiveDamage * (1.0 + (critDmg / 150.0)));
                        } else {
                            effectiveDamage = (float) (effectiveDamage * (1.5 + (critDmg / 100.0)));
                        }
                    }
                    if (isArrow) {
                        double strengthDmg = data.getTraitValue(net.takasha.rpg.skills.AuraTraitType.ATTACK_DAMAGE);
                        effectiveDamage = (float) (effectiveDamage * (1.0 + (strengthDmg / 100.0)));
                    }
                } else if (source.getEntity() instanceof Player player) {
                    isCrit = player.fallDistance > 0.0F && !player.onGround() && !player.onClimbable() && !player.isInWater() && !player.hasEffect(MobEffects.BLINDNESS);
                }

                this.lastDamage = effectiveDamage;
                this.totalDamage += effectiveDamage;
                this.hitCount++;
                this.lastHitTime = now;
                if (effectiveDamage > this.peakDamage) {
                    this.peakDamage = effectiveDamage;
                }
                this.recentHits.add(new HitRecord(now, effectiveDamage));

                if (isCrit) {
                    level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.2, getZ(), 12, 0.3, 0.3, 0.3, 0.15);
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.1f);
                }

                // Pemicu pendarahan atau efek sihir dari senjata penyerang (termasuk panah/proyektil)
                LivingEntity attacker = (source.getEntity() instanceof LivingEntity le) ? le : null;
                triggerWeaponEnchants(attacker, source, effectiveDamage);

                // Deteksi tipe DoT
                String dotType = null;
                if (this.hasEffect(MobEffects.POISON) && source.is(DamageTypes.MAGIC)) dotType = "poison";
                else if (source.is(DamageTypes.WITHER)) dotType = "wither";
                else if (source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.IN_FIRE)) dotType = "burn";
                else if (source.is(DamageTypes.MAGIC)) dotType = "magic";

                // Spawn floating damage text indicator
                spawnFloatingIndicator(level, effectiveDamage, isCrit, dotType);

                // Suara pukulan
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.ARMOR_STAND_HIT, SoundSource.NEUTRAL, 1.0f, 1.0f);
            }

            // Selalu reset health ke max agar dummy tidak pernah hancur
            this.setHealth(this.getMaxHealth());
            updateOverheadDisplay(level);
            return true;
        } finally {
            this.isProcessingHit = false;
        }
    }

    private void triggerWeaponEnchants(LivingEntity attacker, DamageSource source, float damage) {
        if (attacker == null) return;
        net.takasha.rpg.enchant.TakashaEnchantCombatEngine.setProcessing(true);
        try {
            net.takasha.rpg.enchant.TakashaEnchantCombatEngine.applyWeaponCombatEnchants(attacker, this, source, damage);
        } finally {
            net.takasha.rpg.enchant.TakashaEnchantCombatEngine.setProcessing(false);
        }
    }

    public void spawnFloatingIndicator(ServerLevel level, float damage, boolean isCrit, String dotType) {
        if (this.isRemoved()) return;

        // Cap maksimal 5 indikator aktif agar layar tidak kotor dan performa terjaga
        while (this.indicators.size() >= 5) {
            FloatingIndicator oldest = this.indicators.remove(0);
            if (!oldest.display().isRemoved()) {
                oldest.display().discard();
            }
        }

        Display.TextDisplay textDisplay = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level);

        // Dispersi horizontal menyamping radial (alternating kiri & kanan)
        double sideSign = (this.indicatorCounter++ % 2 == 0) ? 1.0 : -1.0;
        double radius = 0.70 + (this.random.nextDouble() * 0.20);
        double angle = (sideSign * (Math.PI / 2.0)) + ((this.random.nextDouble() - 0.5) * 0.5);

        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;
        double offsetY = (dotType != null) ? (1.10 + (this.random.nextDouble() * 0.20)) : (1.35 + (this.random.nextDouble() * 0.25));

        textDisplay.setPos(this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ);

        String textStr;
        if (dotType != null) {
            textStr = switch (dotType.toLowerCase()) {
                case "poison" -> String.format("§2-%.1f ☠", damage);
                case "wither" -> String.format("§8-%.1f ☣", damage);
                case "bleed" -> String.format("§4-%.1f 🩸", damage);
                case "burn" -> String.format("§6-%.1f 🔥", damage);
                default -> String.format("§5-%.1f ✦", damage);
            };
        } else if (isCrit) {
            textStr = String.format("§6§l✦ CRIT! §e-%.1f", damage);
        } else {
            textStr = String.format("§c-%.1f", damage);
        }

        textDisplay.setText(Component.literal(textStr));
        textDisplay.setBillboardConstraints(Display.BillboardConstraints.CENTER);
        textDisplay.setNoGravity(true);
        textDisplay.addTag("takasha_damage_indicator");

        level.addFreshEntity(textDisplay);

        // Trayektori melayang keluar (outward arc) dan kenaikan vertikal pelan
        double vx = Math.cos(angle) * 0.018;
        double vz = Math.sin(angle) * 0.018;
        double vy = 0.014;

        this.indicators.add(new FloatingIndicator(textDisplay, 20, new int[]{0}, vx, vy, vz));
    }

    /**
     * Menghitung interval tick DOT adaptif (20 s.d 100 tick / 1.0s s.d 5.0s)
     * berbasis 5-Tier adaptif (MSPT & nearby living entities) agar performa
     * klien/server tetap mulus tanpa lag saat pertempuran ramai.
     */
    public int resolveAdaptiveDotInterval() {
        if (!(this.level() instanceof ServerLevel sl)) return 20;
        int nearby = sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(16.0)).size() - 1;
        double mspt = 20.0;
        if (sl.getServer() != null) {
            mspt = sl.getServer().getAverageTickTimeNanos() / 1_000_000.0;
        }

        if (mspt > 65.0 || nearby > 50) return 100; // Tier 5: Extreme (5.0s, 5.0x scale)
        if (mspt > 55.0 || nearby > 35) return 80;  // Tier 4: Intense (4.0s, 4.0x scale)
        if (mspt > 45.0 || nearby > 20) return 60;  // Tier 3: Heavy (3.0s, 3.0x scale)
        if (mspt > 35.0 || nearby > 10) return 40;  // Tier 2: Moderate (2.0s, 2.0x scale)
        return 20; // Tier 1: Normal (1.0s, 1.0x scale)
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // 1. Ticking vanilla mob effects
            this.tickEffects();

            // 2. Ticking custom status AE dengan interval adaptif
            tickAeStatuses(serverLevel);

            // 3. Ticking floating indicators
            tickFloatingIndicators();

            // 4. Update overhead display adaptif (tiap 8 s.d 20 tick)
            int updateRate = Math.min(20, Math.max(8, resolveAdaptiveDotInterval() / 4));
            if (this.tickCount % updateRate == 0) {
                updateOverheadDisplay(serverLevel);
            }
        }
    }

    private void tickAeStatuses(ServerLevel level) {
        int interval = resolveAdaptiveDotInterval();
        float scale = interval / 20.0f; // Konversi damage proporsional agar DPS identik 100%

        if (this.bleedTicks > 0) {
            this.bleedTicks--;
            if (this.bleedTicks % interval == 0) {
                float bleedDmg = (1.5f * (this.bleedAmp + 1)) * scale;
                this.lastDamage = bleedDmg;
                this.totalDamage += bleedDmg;
                this.recentHits.add(new HitRecord(level.getGameTime(), bleedDmg));
                spawnFloatingIndicator(level, bleedDmg, false, "bleed");
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 1.2, getZ(), Math.max(2, (int)(6 / scale)), 0.2, 0.3, 0.2, 0.05);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_HURT, SoundSource.NEUTRAL, 0.6f, 1.2f);
            }
        }
        int activeFrozen = Math.max(this.freezeTicks, this.getTicksFrozen());
        if (activeFrozen > 0) {
            this.freezeTicks = activeFrozen;
        }
        if (this.freezeTicks > 0) {
            this.freezeTicks--;
            this.setTicksFrozen(this.freezeTicks);
            int pRate = Math.min(20, Math.max(6, interval / 5));
            if (this.tickCount % pRate == 0) {
                level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1.0, getZ(), 2, 0.25, 0.5, 0.25, 0.02);
            }
        }
        int activeFireTicks = Math.max(this.burnTicks, this.getRemainingFireTicks());
        if (activeFireTicks > 0) {
            this.burnTicks = activeFireTicks;
        }
        if (this.burnTicks > 0) {
            this.burnTicks--;
            this.setRemainingFireTicks(this.burnTicks);
            if (this.burnTicks % interval == 0) {
                float burnDmg = 1.0f * scale;
                this.totalDamage += burnDmg;
                this.recentHits.add(new HitRecord(level.getGameTime(), burnDmg));
                spawnFloatingIndicator(level, burnDmg, false, "burn");
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_HURT_ON_FIRE, SoundSource.NEUTRAL, 0.5f, 1.2f);
            }
            int flameRate = Math.min(20, Math.max(6, interval / 4));
            if (this.tickCount % flameRate == 0) {
                level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.0, getZ(), 1, 0.2, 0.4, 0.2, 0.02);
            }
        }
        int particleInterval = Math.min(25, Math.max(8, interval / 4));
        if (this.shockTicks > 0) {
            this.shockTicks--;
            if (this.tickCount % particleInterval == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.2, getZ(), 2, 0.25, 0.4, 0.25, 0.05);
            }
        }
        if (this.silenceTicks > 0) {
            this.silenceTicks--;
            if (this.tickCount % particleInterval == 0) {
                level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1.6, getZ(), 1, 0.2, 0.2, 0.2, 0.02);
            }
        }
        if (this.disarmTicks > 0) {
            this.disarmTicks--;
            if (this.tickCount % particleInterval == 0) {
                level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.0, getZ(), 1, 0.2, 0.3, 0.2, 0.02);
            }
        }
        if (this.fractureTicks > 0) {
            this.fractureTicks--;
            if (this.tickCount % particleInterval == 0) {
                level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.0, getZ(), 2, 0.2, 0.3, 0.2, 0.05);
            }
        }
        if (this.snowblindTicks > 0) {
            this.snowblindTicks--;
            if (this.tickCount % particleInterval == 0) {
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL, getX(), getY() + 1.6, getZ(), 1, 0.2, 0.2, 0.2, 0.02);
            }
        }
    }

    private void tickFloatingIndicators() {
        for (Iterator<FloatingIndicator> it = indicators.iterator(); it.hasNext(); ) {
            FloatingIndicator ind = it.next();
            ind.ageHolder()[0]++;
            if (ind.ageHolder()[0] >= ind.maxAge() || ind.display().isRemoved()) {
                ind.display().discard();
                it.remove();
            } else {
                ind.display().setPos(
                        ind.display().getX() + ind.vx(),
                        ind.display().getY() + ind.vy(),
                        ind.display().getZ() + ind.vz()
                );
            }
        }
    }

    private void purgeOrphanDisplays(ServerLevel level) {
        List<Display.TextDisplay> nearby = level.getEntitiesOfClass(
                Display.TextDisplay.class,
                this.getBoundingBox().inflate(2.5, 3.0, 2.5)
        );

        String ownTag = "takasha_dummy_overhead_" + this.getUUID();

        for (Display.TextDisplay td : nearby) {
            if (td == this.overheadDisplay) continue;

            // Jangan hapus indikator aktif milik dummy ini
            boolean isOwnIndicator = false;
            for (FloatingIndicator ind : indicators) {
                if (ind.display() == td) {
                    isOwnIndicator = true;
                    break;
                }
            }
            if (isOwnIndicator) continue;

            // Jika entitas memiliki tag overhead dummy ini, sambungkan kembali jika belum punya overheadDisplay aktif
            if (td.entityTags().contains(ownTag)) {
                if (this.overheadDisplay == null || this.overheadDisplay.isRemoved()) {
                    this.overheadDisplay = td;
                    this.overheadDisplayUuid = td.getUUID();
                    continue;
                } else if (td != this.overheadDisplay) {
                    td.discard();
                    continue;
                }
            }

            // Jika posisinya di zona overhead kepala dummy (Y >= getY() + 1.8), periksa apakah milik dummy lain
            if (td.getY() >= this.getY() + 1.8 || td.distanceToSqr(this.getX(), this.getY() + 2.50, this.getZ()) < 0.35) {
                boolean belongsToOtherAliveDummy = false;
                for (String tag : td.entityTags()) {
                    if (tag.startsWith("takasha_dummy_overhead_") && !tag.equals(ownTag)) {
                        try {
                            UUID otherId = UUID.fromString(tag.substring("takasha_dummy_overhead_".length()));
                            if (level.getEntity(otherId) instanceof DamageDummyEntity otherDummy && !otherDummy.isRemoved()) {
                                belongsToOtherAliveDummy = true;
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (!belongsToOtherAliveDummy) {
                    td.discard();
                }
            }
        }
    }

    private void updateOverheadDisplay(ServerLevel level) {
        if (this.isRemoved()) return;

        // 1. Bersihkan entitas DisplayText yatim piatu / duplikat
        purgeOrphanDisplays(level);

        // 2. Hubungkan kembali overhead display jika referensi Java terputus
        if (this.overheadDisplay == null || this.overheadDisplay.isRemoved()) {
            if (this.overheadDisplayUuid != null) {
                net.minecraft.world.entity.Entity existing = level.getEntity(this.overheadDisplayUuid);
                if (existing instanceof Display.TextDisplay td && !td.isRemoved()) {
                    this.overheadDisplay = td;
                }
            }
        }

        // 3. Buat display baru hanya jika belum ada sama sekali
        if (this.overheadDisplay == null || this.overheadDisplay.isRemoved()) {
            this.overheadDisplay = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level);
            this.overheadDisplay.setPos(this.getX(), this.getY() + 2.50, this.getZ());
            this.overheadDisplay.setBillboardConstraints(Display.BillboardConstraints.CENTER);
            this.overheadDisplay.setNoGravity(true);
            this.overheadDisplay.addTag("takasha_dummy_overhead");
            this.overheadDisplay.addTag("takasha_dummy_overhead_" + this.getUUID());
            level.addFreshEntity(this.overheadDisplay);
            this.overheadDisplayUuid = this.overheadDisplay.getUUID();
        } else {
            this.overheadDisplay.setPos(this.getX(), this.getY() + 2.50, this.getZ());
            this.overheadDisplayUuid = this.overheadDisplay.getUUID();
        }

        // Hitung DPS (jendela 5 detik)
        long now = level.getGameTime();
        this.recentHits.removeIf(h -> (now - h.gameTime()) > 100);
        float windowDamage = 0.0f;
        for (HitRecord h : this.recentHits) {
            windowDamage += h.damage();
        }
        float dps = windowDamage / 5.0f;

        int armorValue = this.getArmorValue();
        double approxMitigation = Math.min(80.0, armorValue * 4.0);

        // Pastikan native ArmorStand nametag tetap NONAKTIF agar tidak muncul double nametag
        this.setCustomName(Component.literal(String.format("§6§l[ TARGET DUMMY ] §7| §9🛡 %d Armor §7| §eDPS: §b%.1f", armorValue, dps)));
        this.setCustomNameVisible(false);

        // Update Layer 2 (Overhead Billboard TextDisplay)
        String line1 = String.format("§6§l[ TARGET DUMMY ] §a❤ HP: ∞ §7| §9🛡 Armor: %d §7(Mitigasi ~%.0f%%)", armorValue, approxMitigation);
        String line2 = String.format("§eDPS: §b%.1f §7| §cLast: §f%.1f §7| §6Peak: §f%.1f §7| §aTotal: §f%.1f §7(%d hits)",
                dps, this.lastDamage, this.peakDamage, this.totalDamage, this.hitCount);

        StringBuilder statusSb = new StringBuilder();

        // 1. Efek Vanilla
        for (MobEffectInstance inst : this.getActiveEffects()) {
            String effectName = inst.getEffect().value().getDisplayName().getString();
            int durationSeconds = inst.getDuration() / 20;
            String timerStr = String.format("%d:%02d", durationSeconds / 60, durationSeconds % 60);
            int amp = inst.getAmplifier();
            String ampStr = amp == 0 ? "I" : (amp == 1 ? "II" : String.valueOf(amp + 1));
            String prefix = getEffectPrefix(inst.getEffect().value());
            statusSb.append(prefix).append(effectName).append(" ").append(ampStr).append(" (").append(timerStr).append(") ");
        }

        // 2. Status Custom AE & Vanilla Fire/Freeze Synchronized
        int currentFireTicks = Math.max(this.burnTicks, this.getRemainingFireTicks());
        if (currentFireTicks > 0) {
            int sec = currentFireTicks / 20;
            statusSb.append(String.format("§6🔥 Burn (%d:%02d) ", sec / 60, sec % 60));
        }
        int currentFreezeTicks = Math.max(this.freezeTicks, this.getTicksFrozen());
        if (currentFreezeTicks > 0) {
            int sec = currentFreezeTicks / 20;
            statusSb.append(String.format("§b❄ Frozen (%d:%02d) ", sec / 60, sec % 60));
        }
        if (this.shockTicks > 0) {
            statusSb.append(String.format("§e⚡ Shock (%d:%02d) ",
                    (this.shockTicks / 20) / 60, (this.shockTicks / 20) % 60));
        }
        if (this.silenceTicks > 0) {
            statusSb.append(String.format("§5🤐 Silence (%d:%02d) ",
                    (this.silenceTicks / 20) / 60, (this.silenceTicks / 20) % 60));
        }
        if (this.disarmTicks > 0) {
            statusSb.append(String.format("§7⚔ Disarm (%d:%02d) ",
                    (this.disarmTicks / 20) / 60, (this.disarmTicks / 20) % 60));
        }
        if (this.guardShield > 0.0f) {
            statusSb.append(String.format("§9🛡 Guard (%.0f) ", this.guardShield));
        }
        if (this.fractureTicks > 0) {
            statusSb.append(String.format("§c💥 Fracture (%d:%02d) ",
                    (this.fractureTicks / 20) / 60, (this.fractureTicks / 20) % 60));
        }
        if (this.snowblindTicks > 0) {
            statusSb.append(String.format("§f❄ Snowblind (%d:%02d) ",
                    (this.snowblindTicks / 20) / 60, (this.snowblindTicks / 20) % 60));
        }

        String line3 = statusSb.length() > 0 ? statusSb.toString().trim() : "§7[Status: Netral / Tidak Ada Efek]";

        this.overheadDisplay.setText(Component.literal(line1 + "\n" + line2 + "\n" + line3));
    }

    private static String getEffectPrefix(MobEffect effect) {
        if (effect == MobEffects.POISON.value()) return "§2☠ ";
        if (effect == MobEffects.WITHER.value()) return "§8☣ ";
        if (effect == MobEffects.SLOWNESS.value()) return "§9❄ ";
        if (effect == MobEffects.WEAKNESS.value()) return "§8⚔ ";
        if (effect == MobEffects.BLINDNESS.value() || effect == MobEffects.DARKNESS.value()) return "§0👁 ";
        if (effect == MobEffects.GLOWING.value()) return "§e✨ ";
        if (effect == MobEffects.LEVITATION.value()) return "§d☁ ";
        if (effect == MobEffects.REGENERATION.value()) return "§d💖 ";
        if (effect == MobEffects.RESISTANCE.value()) return "§9🛡 ";
        if (effect == MobEffects.FIRE_RESISTANCE.value()) return "§6🔥 ";
        if (effect == MobEffects.SPEED.value()) return "§b⚡ ";
        if (effect == MobEffects.STRENGTH.value()) return "§c💪 ";
        if (effect == MobEffects.ABSORPTION.value()) return "§e💛 ";
        return "§f";
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // 1. Susu -> Bersihkan semua efek
        if (held.is(Items.MILK_BUCKET)) {
            this.clearAllStatuses();
            this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0f, 1.0f);
            if (!player.isCreative()) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
            player.sendSystemMessage(Component.literal("§e§l[Target Dummy] §aSeluruh status efek (Buff & Debuff) berhasil dinetralisir!"));
            return InteractionResult.SUCCESS;
        }

        // 2. Shift + Tangan kosong -> Reset statistik
        if (player.isShiftKeyDown() && held.isEmpty()) {
            this.resetCombatStats();
            this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
            player.sendSystemMessage(Component.literal("§e§l[Target Dummy] §aStatistik tempur (DPS, Last, Peak, Total) berhasil direset ke 0!"));
            return InteractionResult.SUCCESS;
        }

        // 3. Jika pemain memegang zirah (Armor), serahkan ke vanilla ArmorStand interact untuk memasang/melepas zirah
        EquipmentSlot eqSlot = this.getEquipmentSlotForItem(held);
        boolean isArmorPiece = eqSlot == EquipmentSlot.HEAD || eqSlot == EquipmentSlot.CHEST || eqSlot == EquipmentSlot.LEGS || eqSlot == EquipmentSlot.FEET;
        if (isArmorPiece) {
            return super.interact(player, hand, location);
        }

        // 4. Klik kanan dengan item pengujian apapun (senjata, panah, tongkat, atau tangan kosong) -> Tampilkan laporan statistik lengkap
        printCombatReport(player);
        return InteractionResult.SUCCESS;
    }

    public void printCombatReport(Player player) {
        long now = this.level().getGameTime();
        this.recentHits.removeIf(h -> (now - h.gameTime()) > 100);
        float windowDamage = 0.0f;
        for (HitRecord h : this.recentHits) {
            windowDamage += h.damage();
        }
        float dps = windowDamage / 5.0f;

        int armorValue = this.getArmorValue();
        double approxMitigation = Math.min(80.0, armorValue * 4.0);

        player.sendSystemMessage(Component.literal("§6§l========================================"));
        player.sendSystemMessage(Component.literal("§e§l         TARGET DUMMY REPORT            "));
        player.sendSystemMessage(Component.literal("§6§l========================================"));
        player.sendSystemMessage(Component.literal(" §e• DPS (Jendela 5s)  : §b" + String.format("%.1f", dps) + " DPS"));
        player.sendSystemMessage(Component.literal(" §e• Last Hit Damage   : §f" + String.format("%.1f", this.lastDamage)));
        player.sendSystemMessage(Component.literal(" §e• Peak Hit (Rekor)  : §6§l" + String.format("%.1f", this.peakDamage)));
        player.sendSystemMessage(Component.literal(" §e• Total Serangan    : §a" + String.format("%.1f", this.totalDamage) + " §7(" + this.hitCount + " hit)"));
        player.sendSystemMessage(Component.literal(" §e• Armor Pertahanan  : §b" + armorValue + " poin §7(~" + String.format("%.1f", approxMitigation) + "% mitigasi)"));

        List<String> statuses = new ArrayList<>();
        for (MobEffectInstance inst : this.getActiveEffects()) {
            statuses.add(inst.getEffect().value().getDisplayName().getString() + " " + (inst.getAmplifier() + 1));
        }
        int activeReportFreeze = Math.max(this.freezeTicks, this.getTicksFrozen());
        int activeReportBurn = Math.max(this.burnTicks, this.getRemainingFireTicks());
        if (this.bleedTicks > 0) statuses.add("Bleed (sisa " + (this.bleedTicks / 20) + "s)");
        if (activeReportFreeze > 0) statuses.add("Frozen (sisa " + (activeReportFreeze / 20) + "s)");
        if (activeReportBurn > 0) statuses.add("Burn (sisa " + (activeReportBurn / 20) + "s)");
        if (this.shockTicks > 0) statuses.add("Shock (sisa " + (this.shockTicks / 20) + "s)");
        if (this.silenceTicks > 0) statuses.add("Silence (sisa " + (this.silenceTicks / 20) + "s)");
        if (this.disarmTicks > 0) statuses.add("Disarm (sisa " + (this.disarmTicks / 20) + "s)");
        if (this.guardShield > 0.0f) statuses.add("Guard Shield (" + (int) this.guardShield + ")");
        if (this.fractureTicks > 0) statuses.add("Fracture (sisa " + (this.fractureTicks / 20) + "s)");
        if (this.snowblindTicks > 0) statuses.add("Snowblind (sisa " + (this.snowblindTicks / 20) + "s)");

        String statusStr = statuses.isEmpty() ? "§7Tidak ada efek aktif" : "§f" + String.join(", ", statuses);
        player.sendSystemMessage(Component.literal(" §e• Status Efek Aktif : " + statusStr));
        player.sendSystemMessage(Component.literal("§6§l========================================"));
        player.sendSystemMessage(Component.literal("§7(Tips: Shift+Klik Kanan dengan Susu untuk netralkan efek, Shift+Klik Kanan tangan kosong untuk reset stats)"));
    }

    public void applyAeStatus(String name, int durationTicks, int amplifier) {
        String lower = name.toLowerCase();
        switch (lower) {
            case "bleed" -> {
                this.bleedTicks = Math.max(this.bleedTicks, durationTicks);
                this.bleedAmp = Math.max(this.bleedAmp, amplifier);
            }
            case "freeze", "frostbite" -> this.freezeTicks = Math.max(this.freezeTicks, durationTicks);
            case "burn", "ignite" -> {
                this.burnTicks = Math.max(this.burnTicks, durationTicks);
                this.setRemainingFireTicks(this.burnTicks);
            }
            case "snowblind" -> this.snowblindTicks = Math.max(this.snowblindTicks, durationTicks);
            case "shock", "electrocute" -> this.shockTicks = Math.max(this.shockTicks, durationTicks);
            case "silence", "silenced" -> this.silenceTicks = Math.max(this.silenceTicks, durationTicks);
            case "disarm", "disarmed" -> this.disarmTicks = Math.max(this.disarmTicks, durationTicks);
            case "guard", "shield" -> this.guardShield = Math.max(this.guardShield, 20.0f * (amplifier + 1));
            case "fracture", "acid", "corrosion" -> this.fractureTicks = Math.max(this.fractureTicks, durationTicks);
        }
        if (this.level() instanceof ServerLevel sl) {
            updateOverheadDisplay(sl);
        }
    }

    public boolean hasAeStatus(String name) {
        return switch (name.toLowerCase()) {
            case "bleed" -> this.bleedTicks > 0;
            case "freeze", "frostbite" -> (this.freezeTicks > 0 || this.getTicksFrozen() > 0);
            case "burn", "ignite" -> (this.burnTicks > 0 || this.getRemainingFireTicks() > 0);
            case "snowblind" -> this.snowblindTicks > 0;
            case "shock", "electrocute" -> this.shockTicks > 0;
            case "silence", "silenced" -> this.silenceTicks > 0;
            case "disarm", "disarmed" -> this.disarmTicks > 0;
            case "guard", "shield" -> this.guardShield > 0.0f;
            case "fracture", "acid", "corrosion" -> this.fractureTicks > 0;
            default -> false;
        };
    }

    public void clearAllStatuses() {
        this.removeAllEffects();
        this.bleedTicks = 0;
        this.bleedAmp = 0;
        this.freezeTicks = 0;
        this.setTicksFrozen(0);
        this.burnTicks = 0;
        this.setRemainingFireTicks(0);
        this.clearFire();
        this.shockTicks = 0;
        this.silenceTicks = 0;
        this.disarmTicks = 0;
        this.guardShield = 0.0f;
        this.fractureTicks = 0;
        this.snowblindTicks = 0;

        if (this.level() instanceof ServerLevel sl) {
            updateOverheadDisplay(sl);
        }
    }

    public void resetCombatStats() {
        this.recentHits.clear();
        this.lastDamage = 0.0f;
        this.peakDamage = 0.0f;
        this.totalDamage = 0.0f;
        this.hitCount = 0;
        this.lastHitTime = 0;

        if (this.level() instanceof ServerLevel sl) {
            updateOverheadDisplay(sl);
        }
    }

    public void equipArmorSet(String type) {
        switch (type.toLowerCase()) {
            case "none", "clear" -> {
                this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                this.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                this.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
                this.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
            }
            case "leather" -> {
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
                this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
            }
            case "iron" -> {
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            }
            case "diamond" -> {
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            }
            case "netherite" -> {
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
                this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
            }
        }
        if (this.level() instanceof ServerLevel sl) {
            updateOverheadDisplay(sl);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.overheadDisplay != null && !this.overheadDisplay.isRemoved()) {
            this.overheadDisplay.discard();
        }
        if (this.overheadDisplayUuid != null && this.level() instanceof ServerLevel sl) {
            net.minecraft.world.entity.Entity ent = sl.getEntity(this.overheadDisplayUuid);
            if (ent != null && !ent.isRemoved()) {
                ent.discard();
            }
        }
        for (FloatingIndicator ind : indicators) {
            if (!ind.display().isRemoved()) {
                ind.display().discard();
            }
        }
        indicators.clear();
        super.remove(reason);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("DummyLastDamage", this.lastDamage);
        output.putFloat("DummyPeakDamage", this.peakDamage);
        output.putFloat("DummyTotalDamage", this.totalDamage);
        output.putInt("DummyHitCount", this.hitCount);
        output.putInt("DummyBleedTicks", this.bleedTicks);
        output.putInt("DummyBleedAmp", this.bleedAmp);
        output.putInt("DummyFreezeTicks", this.freezeTicks);
        output.putInt("DummyBurnTicks", this.burnTicks);
        output.putInt("DummyShockTicks", this.shockTicks);
        output.putInt("DummySilenceTicks", this.silenceTicks);
        output.putInt("DummyDisarmTicks", this.disarmTicks);
        output.putFloat("DummyGuardShield", this.guardShield);
        output.putInt("DummyFractureTicks", this.fractureTicks);
        output.putInt("DummySnowblindTicks", this.snowblindTicks);

        if (this.overheadDisplay != null && !this.overheadDisplay.isRemoved()) {
            output.putString("OverheadDisplayUUID", this.overheadDisplay.getUUID().toString());
        } else if (this.overheadDisplayUuid != null) {
            output.putString("OverheadDisplayUUID", this.overheadDisplayUuid.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.lastDamage = input.getFloatOr("DummyLastDamage", 0.0f);
        this.peakDamage = input.getFloatOr("DummyPeakDamage", 0.0f);
        this.totalDamage = input.getFloatOr("DummyTotalDamage", 0.0f);
        this.hitCount = input.getIntOr("DummyHitCount", 0);
        this.bleedTicks = input.getIntOr("DummyBleedTicks", 0);
        this.bleedAmp = input.getIntOr("DummyBleedAmp", 0);
        this.freezeTicks = input.getIntOr("DummyFreezeTicks", 0);
        this.burnTicks = input.getIntOr("DummyBurnTicks", 0);
        this.shockTicks = input.getIntOr("DummyShockTicks", 0);
        this.silenceTicks = input.getIntOr("DummySilenceTicks", 0);
        this.disarmTicks = input.getIntOr("DummyDisarmTicks", 0);
        this.guardShield = input.getFloatOr("DummyGuardShield", 0.0f);
        this.fractureTicks = input.getIntOr("DummyFractureTicks", 0);
        this.snowblindTicks = input.getIntOr("DummySnowblindTicks", 0);

        String savedDisplayUuid = input.getStringOr("OverheadDisplayUUID", "");
        if (!savedDisplayUuid.isEmpty()) {
            try {
                this.overheadDisplayUuid = UUID.fromString(savedDisplayUuid);
            } catch (Exception ignored) {}
        }
    }
}
