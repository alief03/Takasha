package net.takasha.rpg.dungeon;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.takasha.rpg.TakashaRpgMod;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;
import net.takasha.rpg.dungeon.arena.ArenaBlockSnapshot;
import net.takasha.rpg.dungeon.reward.DungeonRewardAccumulator;
import net.takasha.rpg.dungeon.reward.DungeonRewardChestManager;
import net.takasha.rpg.world.TakashaMobLevelManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * DungeonArenaEngine
 * Engine arena dungeon fisik berbasis kubah Tinted Glass (R=20, H=8),
 * penskalaan LevelledMobs 5 Gelombang, perlindungan anti-daylight burn,
 * notifikasi real-time Bossbar 3-warna, Actionbar & Title,
 * sistem reward chest di tengah arena, serta mekanisme crash/force-close recovery bergaransi (Zero World Griefing).
 */
public class DungeonArenaEngine {

    private static final DungeonArenaEngine INSTANCE = new DungeonArenaEngine();

    public static DungeonArenaEngine getInstance() {
        return INSTANCE;
    }

    private boolean arenaActive = false;
    private DungeonTier tier = DungeonTier.COMMON;
    private UUID challengerUuid = null;
    private BlockPos centerPos = null;
    private ServerLevel arenaLevel = null;
    private int currentWave = 1;
    private int wavesCleared = 0;
    private int waveCooldownTicks = 0;

    // Real-Time Progress Tracking & Bossbar
    private ServerBossEvent dungeonBossBar = null;
    private int totalMobsInWave = 0;
    private int mobsKilledInWave = 0;

    private final Set<UUID> activeMobUuids = new HashSet<>();
    private final ArenaBlockSnapshot blockSnapshot = new ArenaBlockSnapshot();
    private final DungeonRewardAccumulator rewardAccumulator = new DungeonRewardAccumulator();
    private final Random rng = new Random();

    public static final int ARENA_RADIUS = 20;
    public static final int ARENA_HEIGHT = 8;
    public static final int TOTAL_WAVES = 5;

    public static void initialize() {
        // 1. Tick loop terpadu arena
        ServerTickEvents.END_SERVER_TICK.register(server -> INSTANCE.tick(server));

        // 2. Pembersihan UUID mob saat mati & update progress notifikasi real-time
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (INSTANCE.arenaActive && INSTANCE.activeMobUuids.contains(entity.getUUID())) {
                INSTANCE.activeMobUuids.remove(entity.getUUID());
                INSTANCE.onMobKilled();
            }
        });
    }

    /**
     * Dipanggil setiap kali salah satu monster arena terbunuh.
     */
    public void onMobKilled() {
        if (!arenaActive) return;
        mobsKilledInWave++;
        ServerPlayer challenger = getChallengerPlayer();
        updateProgressNotification(challenger, true);
    }

    public ServerPlayer getChallengerPlayer() {
        if (arenaLevel == null || challengerUuid == null) return null;
        MinecraftServer server = arenaLevel.getServer();
        if (server == null) return null;
        return server.getPlayerList().getPlayer(challengerUuid);
    }

    /**
     * Memperbarui tampilan BossBar 3-warna dan mengirim Actionbar progress.
     */
    public void updateProgressNotification(ServerPlayer challenger, boolean sendActionBar) {
        if (!arenaActive) return;

        int killed = Math.min(mobsKilledInWave, totalMobsInWave);
        int total = Math.max(1, totalMobsInWave);
        float progress = Math.min(1.0f, Math.max(0.0f, (float) killed / (float) total));
        int percent = Math.min(100, Math.round(progress * 100));

        // 3-Warna Dinamis: Hijau (Wave 1-2), Kuning (Wave 3-4), Merah (Wave 5)
        BossEvent.BossBarColor barColor = switch (currentWave) {
            case 1, 2 -> BossEvent.BossBarColor.GREEN;
            case 3, 4 -> BossEvent.BossBarColor.YELLOW;
            default -> BossEvent.BossBarColor.RED;
        };

        String titleText = "§6§l★ TAKASHA DUNGEON §8| §eWave " + currentWave + "/" + TOTAL_WAVES + " §8» §c" + killed + "/" + total + " Mobs §8(§a" + percent + "%§8)";
        Component barComponent = Component.literal(titleText);

        if (dungeonBossBar == null) {
            UUID barId = UUID.randomUUID();
            dungeonBossBar = new ServerBossEvent(barId, barComponent, barColor, BossEvent.BossBarOverlay.PROGRESS);
        } else {
            dungeonBossBar.setName(barComponent);
            dungeonBossBar.setColor(barColor);
        }
        dungeonBossBar.setProgress(progress);

        if (challenger != null) {
            dungeonBossBar.addPlayer(challenger);
            if (sendActionBar) {
                challenger.sendOverlayMessage(Component.literal("§6§lWave " + currentWave + "/" + TOTAL_WAVES + " §8» §c☠ Mobs: §e" + killed + "/" + total + " §8(§a" + percent + "%§8)"));
            }
        }
    }

    /**
     * Memulai sesi dungeon arena baru.
     */
    public boolean startDungeon(ServerPlayer player, DungeonTier tier, BlockPos pos) {
        if (arenaActive) {
            player.sendSystemMessage(Component.literal("§c[Dungeon] Sebuah arena sedang aktif! Tunggu hingga sesi selesai."));
            return false;
        }

        this.arenaActive = true;
        this.tier = tier;
        this.challengerUuid = player.getUUID();
        this.centerPos = pos.immutable();
        this.arenaLevel = (ServerLevel) player.level();
        this.currentWave = 1;
        this.wavesCleared = 0;
        this.waveCooldownTicks = 0;
        this.totalMobsInWave = 0;
        this.mobsKilledInWave = 0;

        this.activeMobUuids.clear();
        this.rewardAccumulator.clear();
        this.blockSnapshot.clear();

        // Bangun kubah fisik Tinted Glass (R=20, H=8)
        buildPhysicalEnclosure(arenaLevel, centerPos);

        // Inisialisasi BossBar & Titler
        updateProgressNotification(player, false);
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6§l★ TAKASHA DUNGEON ★")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§7Tingkat: " + tier.getColorCode() + tier.getDisplayName() + " §8| §cKubah Disegel!")));

        // Pesan & Sound awal
        player.sendSystemMessage(Component.literal("§6§l╔════════════════════════════════════════════╗\n" +
                "§6§l║      ★ TAKASHA DUNGEON ARENA ★             ║\n" +
                "§6§l╚════════════════════════════════════════════╝\n" +
                "§7Tingkat: " + tier.getColorCode() + tier.getDisplayName() + "\n" +
                "§fKubah Celah Dimensi (Radius 20 Blok) telah disegel!\n" +
                "§c⚠ Peringatan: Gugur di Wave 1 = Hadiah Hangus!"));

        arenaLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.5f, 1.0f);

        // Simpan sesi aktif ke file persistensi untuk proteksi crash
        saveActiveSession(arenaLevel.getServer());

        // Langsung spawn Wave 1
        spawnWave(currentWave);
        return true;
    }

    /**
     * Membangun silinder dan atap fisik menggunakan Tinted Glass.
     */
    private void buildPhysicalEnclosure(ServerLevel level, BlockPos center) {
        int r2Max = ARENA_RADIUS * ARENA_RADIUS;
        int r2Min = (ARENA_RADIUS - 1) * (ARENA_RADIUS - 1);

        for (int dx = -ARENA_RADIUS; dx <= ARENA_RADIUS; dx++) {
            for (int dz = -ARENA_RADIUS; dz <= ARENA_RADIUS; dz++) {
                int distSq = (dx * dx) + (dz * dz);
                if (distSq <= r2Max) {
                    // Dinding perimeter silinder
                    if (distSq >= r2Min) {
                        // 1. Ke atas hingga atap
                        for (int dy = 0; dy <= ARENA_HEIGHT; dy++) {
                            BlockPos pos = center.offset(dx, dy, dz);
                            blockSnapshot.recordAndSet(level, pos, Blocks.TINTED_GLASS.defaultBlockState());
                        }
                        // 2. Pemindaian adaptif ke bawah (Downward Curtain Scan)
                        for (int dy = -1; dy >= -32; dy--) {
                            BlockPos downPos = center.offset(dx, dy, dz);
                            if (downPos.getY() < level.getMinY()) break;
                            BlockState st = level.getBlockState(downPos);
                            // Jika sudah blok solid motion-blocking permanen (bukan air/daun/cairan/tanaman), hentikan tirai
                            if (st.blocksMotion() && !st.isAir() && !st.liquid()) {
                                break;
                            }
                            blockSnapshot.recordAndSet(level, downPos, Blocks.TINTED_GLASS.defaultBlockState());
                        }
                    } else {
                        // Interior arena: jika tanah di bawah altar berlubang/jurang, beri lantai pengaman sementara
                        BlockPos floorPos = center.offset(dx, -1, dz);
                        BlockState floorState = level.getBlockState(floorPos);
                        if (floorState.isAir() || floorState.liquid() || !floorState.blocksMotion()) {
                            blockSnapshot.recordAndSet(level, floorPos, Blocks.TINTED_GLASS.defaultBlockState());
                        }
                    }
                    // Atap silinder
                    BlockPos roofPos = center.offset(dx, ARENA_HEIGHT, dz);
                    blockSnapshot.recordAndSet(level, roofPos, Blocks.TINTED_GLASS.defaultBlockState());
                }
            }
        }
    }

    /**
     * Tick loop arena setiap detik (20 ticks).
     */
    public void tick(MinecraftServer server) {
        if (!arenaActive || arenaLevel == null) return;

        ServerPlayer challenger = server.getPlayerList().getPlayer(challengerUuid);

        // Cek apakah penantang gugur atau offline
        if (challenger == null || !challenger.isAlive()) {
            handleChallengerDeath(server, challenger);
            return;
        }

        // Cek batas perimeter arena (Safe boundary constraint R=21 horizontal & vertical clamping)
        double distSq = challenger.distanceToSqr(centerPos.getX() + 0.5, challenger.getY(), centerPos.getZ() + 0.5);
        boolean outOfBoundsH = distSq > ((ARENA_RADIUS + 1) * (ARENA_RADIUS + 1));
        boolean outOfBoundsV = challenger.getY() < (centerPos.getY() - 15) || challenger.getY() > (centerPos.getY() + ARENA_HEIGHT + 10);
        if (outOfBoundsH || outOfBoundsV) {
            challenger.teleportTo(centerPos.getX() + 0.5, centerPos.getY() + 1.0, centerPos.getZ() + 0.5);
            challenger.sendSystemMessage(Component.literal("§c[Dungeon] Anda tidak dapat melarikan diri dari kubah celah dimensi!"));
            arenaLevel.playSound(null, challenger.getX(), challenger.getY(), challenger.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        // Partikel batas kubah berkala
        if (server.getTickCount() % 15 == 0) {
            renderBorderParticles();
        }

        // Pembersihan entitas mati / hilang
        boolean removedAny = activeMobUuids.removeIf(uuid -> {
            Entity e = arenaLevel.getEntity(uuid);
            return e == null || !e.isAlive();
        });
        if (removedAny) {
            updateProgressNotification(challenger, false);
        }

        // Paksa targetting monster ke penantang setiap 20 tick (Infinite Tracking AI)
        if (server.getTickCount() % 20 == 0) {
            for (UUID mobUuid : activeMobUuids) {
                Entity e = arenaLevel.getEntity(mobUuid);
                if (e instanceof Mob mob) {
                    if (mob.getTarget() != challenger) {
                        mob.setTarget(challenger);
                    }
                    mob.getNavigation().moveTo(challenger, 1.25);
                }
            }
        }

        // Cek apakah gelombang saat ini telah dibersihkan
        if (activeMobUuids.isEmpty()) {
            if (waveCooldownTicks > 0) {
                waveCooldownTicks--;
                if (waveCooldownTicks == 0) {
                    currentWave++;
                    if (currentWave > TOTAL_WAVES) {
                        handleVictory(server, challenger);
                    } else {
                        spawnWave(currentWave);
                    }
                }
            } else {
                // Gelombang baru saja bersih! Akumulasikan hadiah
                wavesCleared = currentWave;
                rewardAccumulator.accumulateWave(tier, currentWave);
                if (challenger != null) {
                    net.takasha.rpg.quest.TakashaQuestManager.recordProgress(challenger, "arenaWavesCleared", 1);
                    net.takasha.rpg.quest.TakashaQuestManager.recordProgress(challenger, "arenaWaveCleared", 1);
                }
                saveActiveSession(server);

                if (currentWave < TOTAL_WAVES) {
                    waveCooldownTicks = 100; // Jeda 5 detik
                    challenger.sendSystemMessage(Component.literal("§a§l[Dungeon] §fGelombang §e" + currentWave + " §fSelesai! Hadiah terakumulasi ke dalam peti. Gelombang berikutnya dalam 5 detik..."));
                    challenger.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§a§lGELOMBANG " + currentWave + " BERSIH!")));
                    challenger.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§eHadiah terakumulasi. Bersiap gelombang berikutnya...")));
                    arenaLevel.playSound(null, challenger.getX(), challenger.getY(), challenger.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                    arenaLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            challenger.getX(), challenger.getY() + 1.0, challenger.getZ(),
                            15, 0.5, 0.5, 0.5, 0.1);
                } else {
                    // Bos Wave 5 baru saja mati -> Kemenangan langsung!
                    handleVictory(server, challenger);
                }
            }
        }
    }

    /**
     * Memunculkan gelombang monster atau bos dungeon.
     */
    private void spawnWave(int wave) {
        this.mobsKilledInWave = 0;
        ServerPlayer challenger = getChallengerPlayer();
        if (challenger != null) {
            challenger.sendSystemMessage(Component.literal("§c§l[Dungeon] >> GELOMBANG " + wave + " / " + TOTAL_WAVES + " DIMULAI! <<"));
            challenger.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§c§lGELOMBANG " + wave)));
            challenger.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(wave == TOTAL_WAVES ? "§4§l★ PERTEMPURAN BOS AKHIR! ★" : "§7Habisi seluruh monster di arena!")));
            arenaLevel.playSound(null, challenger.getX(), challenger.getY(), challenger.getZ(),
                    SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.8f, 1.2f);
        }

        if (wave < TOTAL_WAVES) {
            this.totalMobsInWave = 6 + (wave * 2);
            for (int i = 0; i < totalMobsInWave; i++) {
                spawnArenaMob(wave, false);
            }
        } else {
            // Wave 5: BOSS RUSH!
            int guardCount = switch (tier) {
                case COMMON -> 4;
                case NIGHTMARE -> 6;
                case SAKURA_BOSS -> 8;
            };
            this.totalMobsInWave = 1 + guardCount;
            spawnBoss();
            for (int i = 0; i < guardCount; i++) {
                spawnArenaMob(wave, false);
            }
        }

        updateProgressNotification(challenger, true);
        if (arenaLevel != null && arenaLevel.getServer() != null) {
            saveActiveSession(arenaLevel.getServer());
        }
    }

    private void spawnArenaMob(int wave, boolean isBoss) {
        double angle = rng.nextDouble() * 2 * Math.PI;
        double dist = 6.0 + rng.nextDouble() * 10.0;
        double x = centerPos.getX() + Math.cos(angle) * dist;
        double z = centerPos.getZ() + Math.sin(angle) * dist;

        BlockPos targetGround = arenaLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, 0, z));
        // Pastikan di dalam batas vertikal arena (toleransi lereng/jurang hingga -10 Y atau atap -2 Y)
        if (targetGround.getY() < centerPos.getY() - 10 || targetGround.getY() > centerPos.getY() + ARENA_HEIGHT - 2) {
            targetGround = centerPos.offset((int) (Math.cos(angle) * 8), 0, (int) (Math.sin(angle) * 8));
        }

        Mob mob = pickMobForTier(wave);
        if (mob != null) {
            mob.setPos(targetGround.getX() + 0.5, targetGround.getY(), targetGround.getZ() + 0.5);
            mob.setPersistenceRequired();

            int targetLevel = tier.getMobLevelForWave(wave, rng);
            TakashaMobLevelManager.applyExplicitLevel(mob, targetLevel, false);

            ServerPlayer challenger = arenaLevel.getServer().getPlayerList().getPlayer(challengerUuid);
            if (challenger != null) {
                mob.setTarget(challenger);
            }

            arenaLevel.addFreshEntity(mob);
            activeMobUuids.add(mob.getUUID());
        }
    }

    private void spawnBoss() {
        // Efek petir sambaran visual pemunculan Bos
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(arenaLevel, EntitySpawnReason.COMMAND);
        if (bolt != null) {
            bolt.setPos(centerPos.getX() + 0.5, centerPos.getY(), centerPos.getZ() + 0.5);
            bolt.setVisualOnly(true);
            arenaLevel.addFreshEntity(bolt);
        }

        Mob boss;
        switch (tier) {
            case COMMON -> boss = EntityTypes.RAVAGER.create(arenaLevel, EntitySpawnReason.COMMAND);
            case NIGHTMARE -> boss = EntityTypes.IRON_GOLEM.create(arenaLevel, EntitySpawnReason.COMMAND);
            case SAKURA_BOSS -> boss = EntityTypes.WARDEN.create(arenaLevel, EntitySpawnReason.COMMAND);
            default -> boss = EntityTypes.WITHER_SKELETON.create(arenaLevel, EntitySpawnReason.COMMAND);
        }

        if (boss != null) {
            boss.setPos(centerPos.getX() + 0.5, centerPos.getY() + 1.0, centerPos.getZ() + 0.5);
            boss.setPersistenceRequired();

            int bossLevel = tier.getBossLevel();
            TakashaMobLevelManager.applyExplicitLevel(boss, bossLevel, true);

            ServerPlayer challenger = arenaLevel.getServer().getPlayerList().getPlayer(challengerUuid);
            if (challenger != null) {
                boss.setTarget(challenger);
            }

            arenaLevel.addFreshEntity(boss);
            activeMobUuids.add(boss.getUUID());

            broadcastToAll(arenaLevel.getServer(), "§4§l[Dungeon] " + tier.getColorCode() + "★ BOS TINGKAT " + tier.getDisplayName().toUpperCase() + " (Lvl " + bossLevel + ") TELAH MUNCUL DI ARENA! ★");
        }
    }

    private Mob pickMobForTier(int wave) {
        return switch (tier) {
            case COMMON -> switch (rng.nextInt(4)) {
                case 0 -> EntityTypes.ZOMBIE.create(arenaLevel, EntitySpawnReason.COMMAND);
                case 1 -> EntityTypes.SKELETON.create(arenaLevel, EntitySpawnReason.COMMAND);
                case 2 -> EntityTypes.SPIDER.create(arenaLevel, EntitySpawnReason.COMMAND);
                default -> EntityTypes.HUSK.create(arenaLevel, EntitySpawnReason.COMMAND);
            };
            case NIGHTMARE -> switch (rng.nextInt(5)) {
                case 0 -> EntityTypes.WITHER_SKELETON.create(arenaLevel, EntitySpawnReason.COMMAND);
                case 1 -> EntityTypes.ENDERMAN.create(arenaLevel, EntitySpawnReason.COMMAND);
                case 2 -> EntityTypes.STRAY.create(arenaLevel, EntitySpawnReason.COMMAND);
                case 3 -> EntityTypes.PIGLIN_BRUTE.create(arenaLevel, EntitySpawnReason.COMMAND);
                default -> EntityTypes.WITCH.create(arenaLevel, EntitySpawnReason.COMMAND);
            };
            case SAKURA_BOSS -> switch (rng.nextInt(4)) {
                case 0 -> EntityTypes.PIGLIN_BRUTE.create(arenaLevel, EntitySpawnReason.COMMAND);
                case 1 -> EntityTypes.WITHER_SKELETON.create(arenaLevel, EntitySpawnReason.COMMAND);
                case 2 -> EntityTypes.VINDICATOR.create(arenaLevel, EntitySpawnReason.COMMAND);
                default -> EntityTypes.ENDERMAN.create(arenaLevel, EntitySpawnReason.COMMAND);
            };
        };
    }

    /**
     * Logika Kematian Penantang (Permadeath System):
     * - Wave 1 gugur: Rollback bersih 100%, TIDAK ADA PETI, Keystone hangus!
     * - Wave 2..5 gugur: Rollback blok kecuali titik tengah, peti peninggalan Wave 1..N-1 dimunculkan!
     */
    private void handleChallengerDeath(MinecraftServer server, ServerPlayer challenger) {
        String playerName = challenger != null ? challenger.getName().getString() : "Penantang";
        if (challenger != null) {
            challenger.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§c§lDEFEAT!")));
            challenger.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§7Kamu telah gugur di dalam kubah dimensi.")));
        }

        // Bersihkan seluruh monster arena yang tersisa
        despawnActiveMobs();

        if (wavesCleared == 0) {
            // GUGUR DI WAVE 1: HANGUS BERSIH
            blockSnapshot.restoreAllExcept(arenaLevel, null);
            broadcastToAll(server, "§c§l[Dungeon] §e" + playerName + " §cgugur di Gelombang 1! Rift Keystone hangus tanpa sisa peti hadiah!");
        } else {
            // GUGUR DI WAVE 2+: SPAWN PETI PENINGGALAN
            blockSnapshot.restoreAllExcept(arenaLevel, centerPos);
            String chestName = "§e§lPeti Peninggalan §8(Wave 1-" + wavesCleared + ")";
            DungeonRewardChestManager.spawnChestWithLoot(arenaLevel, centerPos, rewardAccumulator.getAccumulatedItems(), chestName);
            broadcastToAll(server, "§e§l[Dungeon] §e" + playerName + " §egugur di Gelombang " + currentWave + "! Peti peninggalan Wave 1-" + wavesCleared + " dimunculkan di tengah arena!");
        }

        deleteActiveSession(server);
        resetState();
    }

    /**
     * Logika Kemenangan Mutlak (Wave 5 Bos Dikalahkan).
     */
    private void handleVictory(MinecraftServer server, ServerPlayer challenger) {
        despawnActiveMobs();

        if (challenger != null) {
            challenger.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6§lVICTORY!")));
            challenger.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§aDungeon berhasil ditaklukkan!")));
            net.takasha.rpg.quest.TakashaQuestManager.recordProgress(challenger, "dungeonsCompleted", 1);
        }

        // Rollback kubah blok kecuali titik tengah untuk peti kemenangan
        blockSnapshot.restoreAllExcept(arenaLevel, centerPos);

        String victoryChestName = "§6§l★ Peti Kemenangan " + tier.getDisplayName() + " ★";
        DungeonRewardChestManager.spawnChestWithLoot(arenaLevel, centerPos, rewardAccumulator.getAccumulatedItems(), victoryChestName);

        // Efek audio & kembang api kemenangan
        arenaLevel.playSound(null, centerPos.getX() + 0.5, centerPos.getY() + 1.0, centerPos.getZ() + 0.5,
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.5f, 1.0f);
        arenaLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                centerPos.getX() + 0.5, centerPos.getY() + 1.0, centerPos.getZ() + 0.5,
                100, 1.0, 1.0, 1.0, 0.2);

        String name = challenger != null ? challenger.getName().getString() : "Pahlawan";
        broadcastToAll(server, "§6§l[Dungeon] §e" + name + " §aberhasil menaklukkan Dungeon §l" + tier.getDisplayName() + "§a! Peti Kemenangan Agung telah muncul di tengah arena!");

        deleteActiveSession(server);
        resetState();
    }

    private void renderBorderParticles() {
        int points = 24;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double px = centerPos.getX() + 0.5 + Math.cos(angle) * ARENA_RADIUS;
            double pz = centerPos.getZ() + 0.5 + Math.sin(angle) * ARENA_RADIUS;
            arenaLevel.sendParticles(ParticleTypes.PORTAL, px, centerPos.getY() + 1.0, pz, 2, 0.1, 0.5, 0.1, 0.02);
        }
    }

    private void despawnActiveMobs() {
        for (UUID uuid : activeMobUuids) {
            Entity e = arenaLevel.getEntity(uuid);
            if (e != null && e.isAlive()) {
                e.discard();
            }
        }
        activeMobUuids.clear();
    }

    private void resetState() {
        if (dungeonBossBar != null) {
            dungeonBossBar.removeAllPlayers();
            dungeonBossBar = null;
        }
        this.arenaActive = false;
        this.challengerUuid = null;
        this.currentWave = 1;
        this.wavesCleared = 0;
        this.waveCooldownTicks = 0;
        this.totalMobsInWave = 0;
        this.mobsKilledInWave = 0;
        this.activeMobUuids.clear();
        this.rewardAccumulator.clear();
    }

    public void forceStop(boolean asVictory) {
        if (!arenaActive || arenaLevel == null) return;
        MinecraftServer server = arenaLevel.getServer();
        ServerPlayer challenger = server.getPlayerList().getPlayer(challengerUuid);
        if (asVictory && challenger != null) {
            handleVictory(server, challenger);
        } else {
            despawnActiveMobs();
            blockSnapshot.restoreAllExcept(arenaLevel, null);
            broadcastToAll(server, "§c[Dungeon] Arena telah dihentikan oleh Administrator. Seluruh blok dipulihkan.");
            deleteActiveSession(server);
            resetState();
        }
    }

    public void forceWave(int targetWave) {
        if (!arenaActive) return;
        despawnActiveMobs();
        this.currentWave = Math.max(1, Math.min(TOTAL_WAVES, targetWave));
        this.waveCooldownTicks = 0;
        spawnWave(currentWave);
    }

    public void forceRollback() {
        if (arenaLevel != null) {
            MinecraftServer server = arenaLevel.getServer();
            despawnActiveMobs();
            blockSnapshot.restoreAllExcept(arenaLevel, null);
            deleteActiveSession(server);
        }
        resetState();
    }

    /**
     * Persistensi sesi dungeon aktif ke JSON file (world/takasha_rpg/dungeon_active_session.json).
     */
    /**
     * Persistensi sesi dungeon aktif ke JSON file (world/takasha_rpg/dungeon_active_session.json)
     * disertai backup atomik (dungeon_active_session.backup.json) untuk perlindungan anti-korupsi.
     */
    public void saveActiveSession(MinecraftServer server) {
        if (!arenaActive || arenaLevel == null || centerPos == null || challengerUuid == null || server == null) return;
        try {
            Path dir = server.getWorldPath(LevelResource.ROOT).resolve("takasha_rpg");
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path file = dir.resolve("dungeon_active_session.json");
            Path backupFile = dir.resolve("dungeon_active_session.backup.json");
            Path tempFile = dir.resolve("dungeon_active_session.json.tmp");

            JsonObject json = new JsonObject();
            json.addProperty("active", true);
            json.addProperty("challengerUuid", challengerUuid.toString());
            json.addProperty("tier", tier.name());
            json.addProperty("dimension", arenaLevel.dimension().identifier().toString());
            json.addProperty("centerX", centerPos.getX());
            json.addProperty("centerY", centerPos.getY());
            json.addProperty("centerZ", centerPos.getZ());
            json.addProperty("currentWave", currentWave);
            json.addProperty("wavesCleared", wavesCleared);
            json.addProperty("totalMobsInWave", totalMobsInWave);
            json.addProperty("mobsKilledInWave", mobsKilledInWave);
            json.add("blocks", blockSnapshot.toJsonArray());
            json.add("rewards", rewardAccumulator.toJsonArray());

            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
            }

            // Atomic replace file utama dan salin ke file backup
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            Files.copy(file, backupFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            TakashaRpgMod.LOGGER.error("Gagal menyimpan dungeon_active_session.json", e);
        }
    }

    /**
     * Menghapus file sesi dungeon aktif serta backup saat dungeon berakhir normal atau telah dipulihkan.
     */
    public void deleteActiveSession(MinecraftServer server) {
        if (server == null) return;
        try {
            Path dir = server.getWorldPath(LevelResource.ROOT).resolve("takasha_rpg");
            Path file = dir.resolve("dungeon_active_session.json");
            Path backupFile = dir.resolve("dungeon_active_session.backup.json");
            Path tempFile = dir.resolve("dungeon_active_session.json.tmp");

            if (Files.exists(file)) Files.delete(file);
            if (Files.exists(backupFile)) Files.delete(backupFile);
            if (Files.exists(tempFile)) Files.delete(tempFile);
        } catch (Exception e) {
            TakashaRpgMod.LOGGER.error("Gagal menghapus dungeon_active_session.json / backup", e);
        }
    }

    /**
     * Prosedur pemulihan crash server/force close saat server dinyalakan kembali (SERVER_STARTING).
     * Mendukung fallback otomatis ke dungeon_active_session.backup.json jika file utama korup.
     */
    public void checkAndRecoverCrash(MinecraftServer server) {
        if (server == null) return;
        Path dir = server.getWorldPath(LevelResource.ROOT).resolve("takasha_rpg");
        Path file = dir.resolve("dungeon_active_session.json");
        Path backupFile = dir.resolve("dungeon_active_session.backup.json");

        Path targetToRead = null;
        if (Files.exists(file)) {
            targetToRead = file;
        } else if (Files.exists(backupFile)) {
            targetToRead = backupFile;
            TakashaRpgMod.LOGGER.warn("[Dungeon-CrashRecovery] File sesi utama tidak ditemukan, menggunakan file backup...");
        }

        if (targetToRead == null) {
            return;
        }

        TakashaRpgMod.LOGGER.warn("[Dungeon-CrashRecovery] Ditemukan sesi dungeon aktif yang terputus tidak wajar! Memulai prosedur pemulihan...");

        try {
            JsonObject json = null;
            try (BufferedReader reader = Files.newBufferedReader(targetToRead, StandardCharsets.UTF_8)) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception ex) {
                if (targetToRead.equals(file) && Files.exists(backupFile)) {
                    TakashaRpgMod.LOGGER.warn("[Dungeon-CrashRecovery] File utama korup! Mencoba membaca dari backup...", ex);
                    try (BufferedReader reader = Files.newBufferedReader(backupFile, StandardCharsets.UTF_8)) {
                        json = JsonParser.parseReader(reader).getAsJsonObject();
                    }
                }
            }

            if (json == null || !json.has("active") || !json.get("active").getAsBoolean()) {
                deleteActiveSession(server);
                return;
            }

            UUID challengerId = UUID.fromString(json.get("challengerUuid").getAsString());
            DungeonTier crashTier = DungeonTier.valueOf(json.get("tier").getAsString());
            String dimStr = json.get("dimension").getAsString();
            int cx = json.get("centerX").getAsInt();
            int cy = json.get("centerY").getAsInt();
            int cz = json.get("centerZ").getAsInt();
            int crashWavesCleared = json.has("wavesCleared") ? json.get("wavesCleared").getAsInt() : 0;
            int crashCurrentWave = json.has("currentWave") ? json.get("currentWave").getAsInt() : 1;

            BlockPos center = new BlockPos(cx, cy, cz);
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimStr));
            ServerLevel level = server.getLevel(key);
            if (level == null) level = server.overworld();

            ArenaBlockSnapshot crashSnapshot = ArenaBlockSnapshot.fromJsonArray(json.getAsJsonArray("blocks"));
            DungeonRewardAccumulator crashRewards = DungeonRewardAccumulator.fromJsonArray(json.getAsJsonArray("rewards"));

            // 1. Purge seluruh monster di area kubah
            AABB arenaBox = new AABB(center).inflate(ARENA_RADIUS + 4);
            level.getEntitiesOfClass(Mob.class, arenaBox).forEach(Entity::discard);

            // 2. Evaluasi Wave saat crash
            if (crashWavesCleared == 0) {
                // Crash di Wave 1: Rollback 100% dan refund Keystone
                crashSnapshot.restoreAll(level);
                cleanupStrayEnclosures(level, center);

                TakashaRpgData data = TakashaRpgManager.getOrCreateData(challengerId);
                data.setPendingKeystoneRefund(crashTier.name());
                data.setPendingSafeTeleport(true, dimStr, cx + 0.5, cy + 1.0, cz + 0.5);
                TakashaRpgManager.savePlayerData(challengerId);

                TakashaRpgMod.LOGGER.info("[Dungeon-CrashRecovery] Crash pada Wave 1 berhasil dipulihkan: Seluruh blok direstorasi (0 griefing), Rift Keystone tier {} disiapkan untuk refund.", crashTier.name());
            } else {
                // Crash di Wave >= 2: Rollback blok kecuali titik tengah, spawn peti peninggalan
                crashSnapshot.restoreAllExcept(level, center);
                cleanupStrayEnclosures(level, center);

                String chestName = "§e§lPeti Peninggalan §8(Wave 1-" + crashWavesCleared + " Crash Recovery)";
                DungeonRewardChestManager.spawnChestWithLoot(level, center, crashRewards.getAccumulatedItems(), chestName);

                TakashaRpgData data = TakashaRpgManager.getOrCreateData(challengerId);
                data.setPendingSafeTeleport(true, dimStr, cx + 0.5, cy + 1.0, cz + 0.5);
                TakashaRpgManager.savePlayerData(challengerId);

                TakashaRpgMod.LOGGER.info("[Dungeon-CrashRecovery] Crash pada Wave {} berhasil dipulihkan: Peti peninggalan Wave 1-{} dimunculkan di tengah arena [{}, {}, {}].", crashCurrentWave, crashWavesCleared, cx, cy, cz);
            }

            // Hapus file sesi aktif dan backup
            deleteActiveSession(server);
        } catch (Exception e) {
            TakashaRpgMod.LOGGER.error("[Dungeon-CrashRecovery] Gagal memulihkan sesi crash!", e);
        }
    }

    /**
     * Membersihkan sisa blok tinted glass kubah dungeon yang mungkin tertinggal
     * akibat abnormal shutdown / force kill server.
     */
    public void cleanupStrayEnclosures(ServerLevel level, BlockPos center) {
        if (level == null || center == null) return;
        int rMax = ARENA_RADIUS + 1;
        int r2Max = rMax * rMax;
        int r2Min = (ARENA_RADIUS - 2) * (ARENA_RADIUS - 2);

        for (int dx = -rMax; dx <= rMax; dx++) {
            for (int dz = -rMax; dz <= rMax; dz++) {
                int distSq = (dx * dx) + (dz * dz);
                if (distSq <= r2Max) {
                    // Atap
                    BlockPos roofPos = center.offset(dx, ARENA_HEIGHT, dz);
                    if (level.getBlockState(roofPos).is(Blocks.TINTED_GLASS)) {
                        level.setBlock(roofPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                    // Perimeter silinder & tirai bawah
                    if (distSq >= r2Min) {
                        for (int dy = -32; dy <= ARENA_HEIGHT; dy++) {
                            BlockPos pos = center.offset(dx, dy, dz);
                            if (level.getBlockState(pos).is(Blocks.TINTED_GLASS)) {
                                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Menangani pemain yang tiba-tiba disconnect / force-close saat arena masih berjalan.
     */
    public void handlePlayerDisconnect(ServerPlayer player) {
        if (!arenaActive || challengerUuid == null || !challengerUuid.equals(player.getUUID())) return;

        MinecraftServer server = arenaLevel != null ? arenaLevel.getServer() : player.level().getServer();
        String playerName = player.getName().getString();

        despawnActiveMobs();

        if (wavesCleared == 0) {
            // Disconnect di Wave 1: Refund Keystone & rollback bersih
            blockSnapshot.restoreAll(arenaLevel);

            TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
            data.setPendingKeystoneRefund(tier.name());
            data.setPendingSafeTeleport(true, arenaLevel.dimension().identifier().toString(), centerPos.getX() + 0.5, centerPos.getY() + 1.0, centerPos.getZ() + 0.5);
            TakashaRpgManager.savePlayerData(player.getUUID());

            broadcastToAll(server, "§c§l[Dungeon] §e" + playerName + " §cterputus dari server pada Wave 1! Rift Keystone telah di-refund ke inventori login berikutnya.");
        } else {
            // Disconnect di Wave 2+: Peti peninggalan spawn & safe teleport
            blockSnapshot.restoreAllExcept(arenaLevel, centerPos);
            String chestName = "§e§lPeti Peninggalan §8(Wave 1-" + wavesCleared + ")";
            DungeonRewardChestManager.spawnChestWithLoot(arenaLevel, centerPos, rewardAccumulator.getAccumulatedItems(), chestName);

            TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
            data.setPendingSafeTeleport(true, arenaLevel.dimension().identifier().toString(), centerPos.getX() + 0.5, centerPos.getY() + 1.0, centerPos.getZ() + 0.5);
            TakashaRpgManager.savePlayerData(player.getUUID());

            broadcastToAll(server, "§e§l[Dungeon] §e" + playerName + " §eterputus dari server pada Wave " + currentWave + "! Peti peninggalan Wave 1-" + wavesCleared + " dimunculkan di tengah arena!");
        }

        deleteActiveSession(server);
        resetState();
    }

    /**
     * Memastikan sesi aktif tersimpan jika server di-shutdown rapi.
     */
    public void handleServerStopping(MinecraftServer server) {
        if (arenaActive) {
            saveActiveSession(server);
        }
    }

    private void broadcastToAll(MinecraftServer server, String message) {
        if (server == null) return;
        server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
    }

    public boolean isArenaActive() {
        return arenaActive;
    }

    public boolean isArenaMob(UUID uuid) {
        return activeMobUuids.contains(uuid);
    }

    public BlockPos getCenterPos() {
        return centerPos;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int getWavesCleared() {
        return wavesCleared;
    }

    public DungeonTier getTier() {
        return tier;
    }

    public UUID getChallengerUuid() {
        return challengerUuid;
    }

    public int getTotalMobsInWave() {
        return totalMobsInWave;
    }

    public int getMobsKilledInWave() {
        return mobsKilledInWave;
    }

    public int getActiveMobCount() {
        return activeMobUuids.size();
    }

    public int getBlockSnapshotCount() {
        return blockSnapshot.getRecordedBlockCount();
    }

    public boolean isPlayerInDungeon(ServerPlayer player) {
        if (!arenaActive || player == null || centerPos == null) return false;
        if (challengerUuid != null && challengerUuid.equals(player.getUUID())) return true;
        return player.distanceToSqr(centerPos.getX(), centerPos.getY(), centerPos.getZ()) <= (ARENA_RADIUS * ARENA_RADIUS * 2.0);
    }
}
