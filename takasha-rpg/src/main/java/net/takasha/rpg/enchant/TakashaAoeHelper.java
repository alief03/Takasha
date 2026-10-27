package net.takasha.rpg.enchant;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.takasha.rpg.entity.DamageDummyEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * TakashaAoeHelper
 * Utilitas komputasi spasial lingkaran dan visualisasi partikel untuk custom enchantments (AOE, Cleave, Defense, Blast).
 * Mendukung filter entitas musuh, DamageDummyEntity, dan proteksi entitas ramah (Pet, Player).
 */
public class TakashaAoeHelper {

    /**
     * Memindai seluruh entitas yang valid dalam radius lingkaran/bola dari titik pusat.
     *
     * @param level ServerLevel aktif
     * @param center Titik pusat lingkaran serangan
     * @param radius Radius jangkauan (blok)
     * @param exclude Entitas penyerang / pemicu yang harus dikecualikan
     * @param monstersAndDummiesOnly Jika true, hanya mencakup Monster/Enemy agresif dan DamageDummyEntity
     * @return List entitas LivingEntity yang berada di dalam radius
     */
    public static List<LivingEntity> getEntitiesInRadius(
            ServerLevel level, Vec3 center, double radius, LivingEntity exclude, boolean monstersAndDummiesOnly) {
        List<LivingEntity> result = new ArrayList<>();
        if (level == null || center == null || radius <= 0) return result;

        double radiusSqr = radius * radius;
        AABB box = AABB.ofSize(center, radius * 2.0, radius * 2.0, radius * 2.0);

        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity e : entities) {
            if (e == null || e == exclude || e.isRemoved() || !e.isAlive()) continue;

            // Hitung jarak horizontal/3D dari pusat
            if (center.distanceToSqr(e.position()) > radiusSqr) continue;

            if (monstersAndDummiesOnly) {
                // Selalu sertakan boneka latihan tempur
                if (e instanceof DamageDummyEntity) {
                    result.add(e);
                    continue;
                }

                // Jangan lukai hewan peliharaan / pet jinak milik penyerang
                if (e instanceof TamableAnimal tamed && exclude != null && tamed.isOwnedBy(exclude)) {
                    continue;
                }

                // Jika penyerang adalah Player, jangan lukai player lain dalam AOE PvE santai
                if (exclude instanceof Player && e instanceof Player) {
                    continue;
                }

                // Monster dan Enemy agresif
                if (e instanceof Monster || e instanceof Enemy) {
                    result.add(e);
                    continue;
                }

                // Mob berbahaya lain yang mungkin tidak mengimplementasi Enemy langsung (misal slime atau boss)
                String typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
                if (typeId.contains("slime") || typeId.contains("magma_cube") || typeId.contains("ghast")
                        || typeId.contains("dragon") || typeId.contains("shulker") || typeId.contains("hoglin")) {
                    result.add(e);
                }
            } else {
                // Semua living entity kecuali exclude
                result.add(e);
            }
        }

        return result;
    }

    /**
     * Memunculkan cincin partikel horizontal melingkar di permukaan tanah di sekitar titik pusat.
     *
     * @param level ServerLevel aktif
     * @param particle Tipe partikel (misal FALLING_LAVA, SWEEP_ATTACK, ELECTRIC_SPARK)
     * @param center Titik pusat cincin
     * @param radius Radius cincin partikel
     * @param count Jumlah partikel dalam lingkaran
     */
    public static void spawnParticleRing(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
        if (level == null || particle == null || count <= 0) return;

        double step = (2.0 * Math.PI) / count;
        for (int i = 0; i < count; i++) {
            double angle = i * step;
            double px = center.x + (radius * Math.cos(angle));
            double pz = center.z + (radius * Math.sin(angle));
            level.sendParticles(particle, px, center.y + 0.15, pz, 1, 0, 0.02, 0, 0.01);
        }
    }

    /**
     * Memunculkan cincin partikel ganda (partikel utama dan partikel aksen) untuk visual spektakuler (misal Immolation).
     */
    public static void spawnParticleRingDouble(ServerLevel level, ParticleOptions primary, ParticleOptions accent, Vec3 center, double radius, int count) {
        spawnParticleRing(level, primary, center, radius, count);
        if (accent != null) {
            spawnParticleRing(level, accent, center, radius * 0.75, Math.max(4, count / 2));
        }
    }
}
