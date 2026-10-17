package net.takasha.rpg.dungeon;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.takasha.rpg.data.TakashaRpgManager;
import net.takasha.rpg.registry.ModRpgItems;

import java.util.*;

public class TakashaArenaManager {

    private static boolean arenaActive = false;
    private static BlockPos centerPos = null;
    private static ServerLevel arenaLevel = null;
    private static UUID challengerUuid = null;
    private static int currentWave = 0;
    private static int totalWaves = 5;
    private static final Set<UUID> activeMobUuids = new HashSet<>();
    private static int waveCooldownTicks = 0;

    public static final double SAFE_BUBBLE_RADIUS = 25.0;

    public static void initialize() {
        // Arena tick loop
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!arenaActive || arenaLevel == null) return;

            // Check if challenger is still alive & nearby
            ServerPlayer challenger = server.getPlayerList().getPlayer(challengerUuid);
            if (challenger == null || !challenger.isAlive() || challenger.distanceToSqr(centerPos.getX(), centerPos.getY(), centerPos.getZ()) > (SAFE_BUBBLE_RADIUS * SAFE_BUBBLE_RADIUS * 2.0)) {
                endArena(false, "Penantang keluar dari arena atau gugur!");
                return;
            }

            // Render Safe Bubble border particles
            if (server.getTickCount() % 10 == 0) {
                renderSafeBubbleParticles();
            }

            // Prune dead mobs
            activeMobUuids.removeIf(uuid -> {
                var entity = arenaLevel.getEntity(uuid);
                return entity == null || !entity.isAlive();
            });

            // Check if wave cleared
            if (activeMobUuids.isEmpty()) {
                if (waveCooldownTicks > 0) {
                    waveCooldownTicks--;
                    if (waveCooldownTicks == 0) {
                        currentWave++;
                        if (currentWave > totalWaves) {
                            victory(challenger);
                        } else {
                            spawnWave(challenger, currentWave);
                        }
                    }
                } else {
                    // Start 5-second countdown to next wave
                    waveCooldownTicks = 100;
                    challenger.sendSystemMessage(Component.literal("§a§l[Arena] §fWave §e" + currentWave + " §fSelesai! Gelombang berikutnya dalam 5 detik..."));
                    challenger.level().playSound(null, challenger.getX(), challenger.getY(), challenger.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }
        });

        // Safe bubble damage protection: Prevent explosion block damage in arena radius
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (arenaActive && activeMobUuids.contains(entity.getUUID())) {
                activeMobUuids.remove(entity.getUUID());
            }
        });
    }

    public static boolean startArena(ServerPlayer player) {
        if (arenaActive) {
            player.sendSystemMessage(Component.literal("§cArena sedang aktif! Tunggu hingga selesai."));
            return false;
        }

        arenaActive = true;
        challengerUuid = player.getUUID();
        centerPos = player.blockPosition();
        arenaLevel = player.level();
        currentWave = 1;
        waveCooldownTicks = 0;
        activeMobUuids.clear();

        player.sendSystemMessage(Component.literal("§6§l╔══════════════════════════════════╗\n" +
                "§6§l║      ★ TAKASHA DUNGEON ARENA ★       ║\n" +
                "§6§l╚══════════════════════════════════╝\n" +
                "§eSafe Bubble (Radius 25 Blok) telah diaktifkan!\n" +
                "§fBersiaplah menghadapi 5 gelombang monster dan bos!"));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.5f, 1.0f);

        spawnWave(player, currentWave);
        return true;
    }

    private static void spawnWave(ServerPlayer player, int wave) {
        player.sendSystemMessage(Component.literal("§c§l[Arena] >> GELOMBANG " + wave + " / " + totalWaves + " DIMULAI! <<"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.7f, 1.2f);

        Random rng = new Random();
        int mobCount = 4 + wave * 2;

        for (int i = 0; i < mobCount; i++) {
            double angle = rng.nextDouble() * 2 * Math.PI;
            double dist = 8.0 + rng.nextDouble() * 10.0;
            double x = centerPos.getX() + Math.cos(angle) * dist;
            double z = centerPos.getZ() + Math.sin(angle) * dist;
            BlockPos spawnPos = arenaLevel.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, 0, z));

            Mob mob = null;
            if (wave == 1) {
                mob = (rng.nextBoolean()) ? EntityTypes.ZOMBIE.create(arenaLevel, net.minecraft.world.entity.EntitySpawnReason.COMMAND)
                        : EntityTypes.SKELETON.create(arenaLevel, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            } else if (wave == 2) {
                mob = (rng.nextBoolean()) ? EntityTypes.SPIDER.create(arenaLevel, net.minecraft.world.entity.EntitySpawnReason.COMMAND)
                        : EntityTypes.ZOMBIE.create(arenaLevel, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            } else if (wave == 3) {
                mob = (rng.nextBoolean()) ? EntityTypes.ENDERMAN.create(arenaLevel, net.minecraft.world.entity.EntitySpawnReason.COMMAND)
                        : EntityTypes.WITCH.create(arenaLevel, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            } else if (wave == 4) {
                mob = (rng.nextBoolean()) ? EntityTypes.BLAZE.create(arenaLevel, net.minecraft.world.entity.EntitySpawnReason.COMMAND)
                        : EntityTypes.PIGLIN_BRUTE.create(arenaLevel, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            } else {
                // Wave 5: Boss Rush
                mob = EntityTypes.WITHER_SKELETON.create(arenaLevel, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            }

            if (mob != null) {
                mob.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                mob.setTarget(player);
                arenaLevel.addFreshEntity(mob);
                activeMobUuids.add(mob.getUUID());

                arenaLevel.sendParticles(ParticleTypes.LARGE_SMOKE, spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5, 10, 0.3, 0.5, 0.3, 0.05);
            }
        }
    }

    private static void renderSafeBubbleParticles() {
        if (arenaLevel == null || centerPos == null) return;
        int points = 32;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double px = centerPos.getX() + Math.cos(angle) * SAFE_BUBBLE_RADIUS;
            double pz = centerPos.getZ() + Math.sin(angle) * SAFE_BUBBLE_RADIUS;
            double py = centerPos.getY() + 1.0;
            arenaLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, px, py, pz, 1, 0, 0.1, 0, 0.02);
        }
    }

    private static void victory(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§6§l★ SELAMAT! Kamu berhasil menaklukkan Takasha Dungeon Arena! ★"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);

        // Spawn victory loot chest at center
        BlockPos chestPos = centerPos.above();
        arenaLevel.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        if (arenaLevel.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            // Fill with rewards
            chest.setItem(0, new ItemStack(ModRpgItems.RPG_GOLD_COIN, 5));
            chest.setItem(1, new ItemStack(ModRpgItems.TAKASHA_GEM, 3));
            chest.setItem(2, new ItemStack(ModRpgItems.WHITE_SCROLL, 1));
            chest.setItem(3, new ItemStack(ModRpgItems.MAGIC_DUST, 8));
            chest.setItem(4, new ItemStack(ModRpgItems.SLOT_INCREASER_2, 1));
            chest.setItem(5, new ItemStack(ModRpgItems.STAT_TRACKER_KILLS, 1));

            // 35% chance of Legendary Home Expansion Scroll / Tome
            Random rng = new Random();
            if (rng.nextFloat() < 0.35f) {
                chest.setItem(6, new ItemStack(ModRpgItems.HOME_EXPANSION_SCROLL, 1));
            } else if (rng.nextFloat() < 0.25f) {
                chest.setItem(6, new ItemStack(ModRpgItems.TOME_POCKET_CRAFTING, 1));
            }
        }

        player.sendSystemMessage(Component.literal("§ePeti Harta Karun Arena telah muncul di koordinat tengah!"));
        endArena(true, "Arena selesai dengan kemenangan!");
    }

    public static void endArena(boolean won, String reason) {
        arenaActive = false;
        activeMobUuids.clear();
        challengerUuid = null;
        arenaLevel = null;
        centerPos = null;
    }

    public static boolean isArenaActive() {
        return arenaActive;
    }
}
