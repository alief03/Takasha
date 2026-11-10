package net.takasha.rpg.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;
import net.takasha.rpg.dungeon.TakashaArenaManager;
import net.takasha.rpg.optimization.TakashaPerformanceConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LevelledMobs Engine (Lvl 1 - 1000)
 * Penskalaan dinamis berdasarkan AuraSkills pemain, jarak koordinat, dan dimensi.
 * Dilengkapi Anti-Melt Damage Cap (15% Max HP/hit) untuk Boss Lvl 750-1000 dan True Damage Lvl 600+.
 */
public class TakashaMobLevelManager {

    public static class MobLevelData {
        public final int level;
        public final String tierName;
        public final String tierColor;
        public final float maxHp;
        public final float attackDamage;
        public final float trueDamagePercent;
        public final boolean hasAntiMeltCap;

        // Stunlock prevention
        public long lastDamageTime = 0;
        public int recentHitCount = 0;
        public long stunlockImmunityUntil = 0;

        public MobLevelData(int level, String tierName, String tierColor, float maxHp, float attackDamage, float trueDamagePercent, boolean hasAntiMeltCap) {
            this.level = level;
            this.tierName = tierName;
            this.tierColor = tierColor;
            this.maxHp = maxHp;
            this.attackDamage = attackDamage;
            this.trueDamagePercent = trueDamagePercent;
            this.hasAntiMeltCap = hasAntiMeltCap;
        }
    }

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("takasha_mob_nametag.json");
    private static boolean mobNametagEnabled = true;

    static {
        loadConfig();
    }

    public static boolean isMobNametagEnabled() {
        return mobNametagEnabled;
    }

    public static void setMobNametagEnabled(boolean enabled) {
        mobNametagEnabled = enabled;
        saveConfig();
    }

    private static void loadConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String str = Files.readString(CONFIG_PATH);
                JsonObject json = JsonParser.parseString(str).getAsJsonObject();
                if (json.has("enabled")) {
                    mobNametagEnabled = json.get("enabled").getAsBoolean();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void saveConfig() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("enabled", mobNametagEnabled);
            Files.writeString(CONFIG_PATH, json.toString());
        } catch (Exception ignored) {
        }
    }

    private static final Map<UUID, MobLevelData> MOB_DATA = new ConcurrentHashMap<>();

    public static void initialize() {
        // 1. Damage Interceptor (Anti-Melt Cap 15% Max HP per Hit & True Damage)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity.level().isClientSide()) return true;

            // Proteksi Anti-Melt Cap pada Boss LevelledMobs (Lvl 750-1000)
            MobLevelData data = MOB_DATA.get(entity.getUUID());
            if (data != null && data.hasAntiMeltCap) {
                long now = entity.level().getGameTime();

                // Stunlock immunity check (4s kekebalan jika diserang >3 hit dalam 2s)
                if (now < data.stunlockImmunityUntil) {
                    return false; // Immune during stunlock grace period
                }

                if (now - data.lastDamageTime <= 40) { // 2 detik
                    data.recentHitCount++;
                    if (data.recentHitCount >= 3) {
                        data.stunlockImmunityUntil = now + 80; // 4 detik
                        data.recentHitCount = 0;
                    }
                } else {
                    data.recentHitCount = 1;
                }
                data.lastDamageTime = now;

                // Anti-One-Shot: Capped at 15% of Max HP per single hit
                float cap = data.maxHp * 0.15f;
                if (amount > cap) {
                    // Apply capped damage directly
                    entity.hurtServer((ServerLevel) entity.level(), source, cap);
                    return false;
                }
            }

            // True Damage dari Monster Lvl 600+ ke Pemain
            if (source.getEntity() instanceof LivingEntity attacker) {
                MobLevelData attackerData = MOB_DATA.get(attacker.getUUID());
                if (attackerData != null && attackerData.trueDamagePercent > 0 && entity instanceof ServerPlayer player) {
                    float trueDmg = amount * (attackerData.trueDamagePercent / 100.0f);
                    // Langsung kurangi health pemain (bypass armor & resistance)
                    player.setHealth(Math.max(0.5f, player.getHealth() - trueDmg));
                }
            }

            return true;
        });

        // 2. Pembersihan memori saat entitas mati (Zero Memory Leak)
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            MOB_DATA.remove(entity.getUUID());
        });

        // 3. Update Health Bar Nametag after damage
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (entity instanceof Enemy && entity instanceof LivingEntity living) {
                MobLevelData data = MOB_DATA.get(entity.getUUID());
                if (data != null) {
                    updateMobNametag(living, data);
                }
            }
        });

        // 4. Otomatis pasang level dan scaling saat monster dimuat/spawn di server
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (level.isClientSide()) return;
            if (entity instanceof Enemy && entity instanceof LivingEntity living && !(entity instanceof ServerPlayer)) {
                ServerPlayer nearest = (ServerPlayer) level.getNearestPlayer(entity, 128.0);
                applyMobLevel(living, nearest);
            }
        });
    }

    /**
     * Daftarkan level dan kalkulasikan atribut monster saat pertama kali bertemu pemain
     */
    public static MobLevelData applyMobLevel(LivingEntity entity, ServerPlayer player) {
        if (!(entity instanceof Enemy) || entity instanceof ServerPlayer) {
            return null;
        }

        if (MOB_DATA.containsKey(entity.getUUID())) {
            MobLevelData existing = MOB_DATA.get(entity.getUUID());
            updateMobNametag(entity, existing);
            return existing;
        }

        ServerLevel level = (ServerLevel) entity.level();
        int playerAvgSkill = 1;
        if (player != null) {
            TakashaRpgData rpgData = TakashaRpgManager.getOrCreateData(player);
            playerAvgSkill = Math.max(1, rpgData.getTotalSkillLevel() / 11);
        }

        // Hitung jarak dari titik spawn dunia
        double spawnX = level.getRespawnData().pos().getX();
        double spawnZ = level.getRespawnData().pos().getZ();
        double dist = Math.sqrt(entity.distanceToSqr(spawnX, entity.getY(), spawnZ));

        // Buffer: 250 blok
        double activeDist = Math.max(0.0, dist - 250.0);
        // Ringed: 750 blok per ring
        double ringLevel = (activeDist / 750.0) * 15.0;

        // Y-Depth: di bawah Y=60 semakin dalam semakin kuat
        double depthBonus = 0.0;
        if (entity.getY() < 60.0) {
            depthBonus = Math.max(0.0, (60.0 - entity.getY()) * 0.25);
        }

        // Pengali dimensi
        double dimMult = 1.0;
        if (level.dimension() == net.minecraft.world.level.Level.NETHER) {
            dimMult = 1.35;
        } else if (level.dimension() == net.minecraft.world.level.Level.END) {
            dimMult = 1.75;
        }

        // Kalkulasi Level (1 - 1000)
        int calculatedLevel = (int) Math.round(((playerAvgSkill * 0.6) + (activeDist / 30.0) + ringLevel + depthBonus) * dimMult);
        calculatedLevel = Math.max(1, Math.min(1000, calculatedLevel));

        // Penskalaan Non-Linear HP Curvature
        float baseHp = (float) entity.getAttributeBaseValue(Attributes.MAX_HEALTH);
        float scaledHp;
        if (calculatedLevel <= 250) {
            scaledHp = baseHp * (1.0f + calculatedLevel * 0.015f);
        } else if (calculatedLevel <= 500) {
            float hp250 = baseHp * (1.0f + 250 * 0.015f);
            scaledHp = hp250 * (1.0f + (calculatedLevel - 250) * 0.022f);
        } else if (calculatedLevel <= 750) {
            float hp250 = baseHp * (1.0f + 250 * 0.015f);
            float hp500 = hp250 * (1.0f + 250 * 0.022f);
            scaledHp = hp500 * (1.0f + (calculatedLevel - 500) * 0.032f);
        } else {
            float hp250 = baseHp * (1.0f + 250 * 0.015f);
            float hp500 = hp250 * (1.0f + 250 * 0.022f);
            float hp750 = hp500 * (1.0f + 250 * 0.032f);
            scaledHp = hp750 * (1.0f + (calculatedLevel - 750) * 0.045f);
        }

        // Attack Damage & True Damage
        float attackDmg = (float) (3.0 + (calculatedLevel * 0.35));
        float trueDmg = 0.0f;
        if (calculatedLevel >= 901) trueDmg = 30.0f;
        else if (calculatedLevel >= 751) trueDmg = 20.0f;
        else if (calculatedLevel >= 600) trueDmg = 15.0f;

        // Anti-Melt Cap per hit
        boolean hasAntiMelt = calculatedLevel >= 750;

        // Tier classification
        String tierName;
        String tierColor;
        if (calculatedLevel <= 150) {
            tierName = "Common";
            tierColor = "§7";
        } else if (calculatedLevel <= 350) {
            tierName = "Uncommon";
            tierColor = "§a";
        } else if (calculatedLevel <= 550) {
            tierName = "Elite";
            tierColor = "§b";
        } else if (calculatedLevel <= 750) {
            tierName = "Epic";
            tierColor = "§d";
        } else if (calculatedLevel <= 900) {
            tierName = "Legendary Titan";
            tierColor = "§6";
        } else {
            tierName = "Mythic Cataclysm Boss";
            tierColor = "§4";
        }

        // Terapkan atribut ke entitas Minecraft
        if (entity.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
            entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(scaledHp);
            entity.setHealth(scaledHp);
        }
        if (entity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
            entity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(attackDmg);
        }

        MobLevelData data = new MobLevelData(calculatedLevel, tierName, tierColor, scaledHp, attackDmg, trueDmg, hasAntiMelt);
        MOB_DATA.put(entity.getUUID(), data);

        updateMobNametag(entity, data);
        return data;
    }

    /**
     * Terapkan level dungeon/arena secara eksplisit ke entitas monster dengan proteksi daylight burn.
     */
    public static MobLevelData applyExplicitLevel(LivingEntity entity, int targetLevel, boolean isBoss) {
        if (!(entity instanceof Enemy) || entity instanceof ServerPlayer) {
            return null;
        }

        int calculatedLevel = Math.max(1, Math.min(1000, targetLevel));

        // Penskalaan Non-Linear HP Curvature
        float baseHp = (float) entity.getAttributeBaseValue(Attributes.MAX_HEALTH);
        float scaledHp;
        if (calculatedLevel <= 250) {
            scaledHp = baseHp * (1.0f + calculatedLevel * 0.015f);
        } else if (calculatedLevel <= 500) {
            float hp250 = baseHp * (1.0f + 250 * 0.015f);
            scaledHp = hp250 * (1.0f + (calculatedLevel - 250) * 0.022f);
        } else if (calculatedLevel <= 750) {
            float hp250 = baseHp * (1.0f + 250 * 0.015f);
            float hp500 = hp250 * (1.0f + 250 * 0.022f);
            scaledHp = hp500 * (1.0f + (calculatedLevel - 500) * 0.032f);
        } else {
            float hp250 = baseHp * (1.0f + 250 * 0.015f);
            float hp500 = hp250 * (1.0f + 250 * 0.022f);
            float hp750 = hp500 * (1.0f + 250 * 0.032f);
            scaledHp = hp750 * (1.0f + (calculatedLevel - 750) * 0.045f);
        }

        if (isBoss) {
            scaledHp *= 2.5f;
        }

        // Attack Damage & True Damage
        float attackDmg = (float) (3.0 + (calculatedLevel * 0.35));
        if (isBoss) {
            attackDmg *= 1.5f;
        }

        float trueDmg = 0.0f;
        if (calculatedLevel >= 901) trueDmg = 30.0f;
        else if (calculatedLevel >= 751) trueDmg = 20.0f;
        else if (calculatedLevel >= 600) trueDmg = 15.0f;
        if (isBoss && trueDmg < 20.0f) {
            trueDmg = 20.0f;
        }

        // Anti-Melt Cap per hit
        boolean hasAntiMelt = isBoss || calculatedLevel >= 750;

        // Tier classification
        String tierName;
        String tierColor;
        if (isBoss) {
            tierName = "Dungeon Boss";
            tierColor = "§4§l";
        } else if (calculatedLevel <= 150) {
            tierName = "Common";
            tierColor = "§7";
        } else if (calculatedLevel <= 350) {
            tierName = "Uncommon";
            tierColor = "§a";
        } else if (calculatedLevel <= 550) {
            tierName = "Elite";
            tierColor = "§b";
        } else if (calculatedLevel <= 750) {
            tierName = "Epic";
            tierColor = "§d";
        } else if (calculatedLevel <= 900) {
            tierName = "Legendary Titan";
            tierColor = "§6";
        } else {
            tierName = "Mythic Cataclysm Boss";
            tierColor = "§4";
        }

        // Terapkan atribut ke entitas Minecraft
        if (entity.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
            entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(scaledHp);
            entity.setHealth(scaledHp);
        }
        if (entity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
            entity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(attackDmg);
        }

        // Anti-Daylight Burn protection for dungeon arena mobs
        entity.clearFire();
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, false, false, false));

        MobLevelData data = new MobLevelData(calculatedLevel, tierName, tierColor, scaledHp, attackDmg, trueDmg, hasAntiMelt);
        MOB_DATA.put(entity.getUUID(), data);

        updateMobNametag(entity, data);
        return data;
    }

    public static void updateMobNametag(LivingEntity entity, MobLevelData data) {
        if (entity == null || data == null) return;
        if (!mobNametagEnabled) return;
        float currentHp = Math.max(0.0f, entity.getHealth());
        float maxHp = data.maxHp;
        float ratio = Math.max(0.0f, Math.min(1.0f, currentHp / maxHp));

        int totalBars = 8;
        int filledBars = Math.round(ratio * totalBars);
        int emptyBars = totalBars - filledBars;

        String barColor = ratio > 0.5f ? "§a" : (ratio > 0.2f ? "§e" : "§c");
        StringBuilder healthBar = new StringBuilder(barColor);
        for (int i = 0; i < filledBars; i++) {
            healthBar.append("█");
        }
        healthBar.append("§7");
        for (int i = 0; i < emptyBars; i++) {
            healthBar.append("░");
        }

        boolean isArena = TakashaArenaManager.isArenaMob(entity.getUUID());
        String currentCustomName = (entity.hasCustomName() && entity.getCustomName() != null) ? entity.getCustomName().getString() : "";
        String prefix = (isArena || currentCustomName.contains("[Wave")) ?
                (currentCustomName.contains("[Wave") ? currentCustomName.substring(0, currentCustomName.indexOf("]") + 1) + " " : "§c§l[Wave] ") : "";

        String mobName = entity.getType().getDescription().getString();
        // LevelledMobs indicator format: §8[§cLvl %lvl%§8] §f%name% §8[§a%health_bar%§8] §e%hp%/%maxhp%
        String displayName = prefix + "§8[" + data.tierColor + "Lvl " + data.level + "§8] §f" + mobName
                + " §8[" + healthBar + "§8] §e" + (int) currentHp + "§7/§e" + (int) maxHp + " HP";

        entity.setCustomName(Component.literal(displayName));
        entity.setCustomNameVisible(true);
    }

    public static MobLevelData getMobData(UUID uuid) {
        return MOB_DATA.get(uuid);
    }
}
