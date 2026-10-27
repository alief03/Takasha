package net.takasha.rpg.optimization;

/**
 * Konfigurasi performa khusus arsitektur hardware laptop berdaya rendah
 * Dioptimalkan untuk:
 * - CPU: AMD Ryzen 3 7320U (4 Cores / 8 Threads, 15W TDP)
 * - GPU: AMD Radeon 610M (2 Compute Units, RDNA 2)
 * - RAM: 8 GB LPDDR5 Shared System Memory
 */
public class TakashaPerformanceConfig {

    // Jarak culling nametag 3D mob (dalam blok)
    public static double NAMETAG_CULL_DISTANCE_SQ = 20.0 * 20.0; // 400.0 (Radius 20 blok)

    // Batas partikel per efek tebasan/ledakan (mencegah overdraw GPU Radeon 610M)
    public static int MAX_PARTICLES_PER_HIT = 15;

    // Radius culling efek partikel dari kamera pemain (dalam blok)
    public static double PARTICLE_CULL_DISTANCE_SQ = 16.0 * 16.0; // 256.0 (Radius 16 blok)

    // Interval batch eksekusi aura/perk pasif perlengkapan (tick)
    // 15 tick = ~0.75 detik (menghemat 90% waktu CPU perulangan tick pemain)
    public static int EQUIPMENT_PASSIVE_BATCH_TICKS = 15;

    // Jumlah thread background worker asynchronous
    public static int ASYNC_WORKER_THREADS = 4;

    // Toggle Occlusion Culling (tidak render nametag jika mob terhalang dinding batu)
    public static boolean ENABLE_OCCLUSION_CULLING = true;

    // Toggle Fast Font Rendering (menonaktifkan drop-shadow ganda pada nametag mob)
    public static boolean FAST_NAMETAG_FONT_ENABLED = true;

    public static boolean isWithinNametagDistance(double distanceSq) {
        return distanceSq <= NAMETAG_CULL_DISTANCE_SQ;
    }

    public static boolean isWithinParticleDistance(double distanceSq) {
        return distanceSq <= PARTICLE_CULL_DISTANCE_SQ;
    }
}
