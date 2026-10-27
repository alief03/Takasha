package net.takasha.rpg.world.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.takasha.rpg.item.TakashaDustItem;
import net.takasha.rpg.registry.ModRpgItems;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class RpgEventInstance {
    private final RpgEventType type;
    private final int totalDurationTicks;
    private int remainingTicks;
    private final ServerBossEvent bossBar;
    private final Random random = new Random();

    public RpgEventInstance(RpgEventType type, int durationTicks) {
        this.type = type;
        this.totalDurationTicks = durationTicks;
        this.remainingTicks = durationTicks;
        this.bossBar = new ServerBossEvent(
                UUID.randomUUID(),
                Component.literal(type.getDisplayName() + " §7(" + formatTime(durationTicks) + ")"),
                type.getBossBarColor(),
                type.getBossBarOverlay()
        );
        this.bossBar.setProgress(1.0f);
        this.bossBar.setVisible(true);
    }

    public RpgEventType getType() {
        return type;
    }

    public int getTotalDurationTicks() {
        return totalDurationTicks;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public ServerBossEvent getBossBar() {
        return bossBar;
    }

    public void onStart(MinecraftServer server) {
        // Broadcast sound dan banner ke seluruh pemain
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            bossBar.addPlayer(player);
            broadcastStart(player);
        }

        if (type == RpgEventType.MYTHIC_MERCHANT) {
            spawnMythicMerchant(server);
        }
    }

    public void tick(MinecraftServer server) {
        remainingTicks--;

        // Update progress boss bar
        float progress = Math.max(0.0f, (float) remainingTicks / (float) totalDurationTicks);
        bossBar.setProgress(progress);
        bossBar.setName(Component.literal(type.getDisplayName() + " §e[" + formatTime(remainingTicks) + "]"));

        // Pastikan seluruh player tersambung ke bossbar
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
        }

        // Jalankan periodic action tiap event
        if (remainingTicks % 200 == 0) { // Setiap 10 detik
            handleSpecialEventPulse(server);
        }
    }

    private void handleSpecialEventPulse(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        ServerPlayer target = players.get(random.nextInt(players.size()));
        if (target == null || target.level() == null || !(target.level() instanceof ServerLevel level)) return;

        switch (type) {
            case STARFALL -> {
                // Jatuhkan Star Shard / Dust di dekat pemain
                double dx = target.getX() + (random.nextDouble() - 0.5) * 16.0;
                double dz = target.getZ() + (random.nextDouble() - 0.5) * 16.0;
                double dy = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                        net.minecraft.core.BlockPos.containing(dx, target.getY(), dz)).getY() + 1.0;

                level.sendParticles(ParticleTypes.END_ROD, dx, dy + 2.0, dz, 25, 0.4, 0.5, 0.4, 0.05);
                level.sendParticles(ParticleTypes.FIREWORK, dx, dy + 0.5, dz, 15, 0.3, 0.3, 0.3, 0.05);
                level.playSound(null, dx, dy, dz, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 1.2f, 1.5f);

                ItemStack starLoot = (random.nextBoolean())
                        ? TakashaDustItem.createMagicDust(25)
                        : TakashaDustItem.createMagicDust(15);
                ItemEntity drop = new ItemEntity(level, dx, dy, dz, starLoot);
                level.addFreshEntity(drop);
            }
            case GOBLIN_HUNT -> {
                // Peluang 40% memunculkan goblin pencuri emas
                if (random.nextDouble() < 0.4) {
                    double gx = target.getX() + (random.nextDouble() - 0.5) * 20.0;
                    double gz = target.getZ() + (random.nextDouble() - 0.5) * 20.0;
                    double gy = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                            net.minecraft.core.BlockPos.containing(gx, target.getY(), gz)).getY();

                    Zombie goblin = EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
                    if (goblin != null) {
                        goblin.setBaby(true);
                        goblin.setCustomName(Component.literal("§6§l[Golden Goblin] §ePencuri Harta"));
                        goblin.setCustomNameVisible(true);
                        goblin.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                        goblin.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
                        goblin.setPos(gx, gy, gz);
                        level.addFreshEntity(goblin);

                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, gx, gy + 1, gz, 10, 0.3, 0.3, 0.3, 0.1);
                        level.playSound(null, gx, gy, gz, SoundEvents.WANDERING_TRADER_YES, SoundSource.HOSTILE, 1.0f, 1.8f);
                    }
                }
            }
            case RIFT_INCURSION -> {
                // Efek partikel rift di sekitar pemain
                level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1.0, target.getZ(), 40, 1.5, 1.0, 1.5, 0.2);
            }
            default -> {}
        }
    }

    private void spawnMythicMerchant(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) return;
        List<ServerPlayer> players = overworld.players();
        if (players.isEmpty()) return;

        ServerPlayer anchor = players.get(0);
        double sx = anchor.getX() + 3.0;
        double sz = anchor.getZ() + 3.0;
        double sy = overworld.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                net.minecraft.core.BlockPos.containing(sx, anchor.getY(), sz)).getY();

        WanderingTrader trader = EntityTypes.WANDERING_TRADER.create(overworld, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (trader != null) {
            trader.setCustomName(Component.literal("§d§l[Mythic Merchant] §fKelana Dimensi"));
            trader.setCustomNameVisible(true);
            trader.setPos(sx, sy, sz);

            MerchantOffers offers = trader.getOffers();
            offers.clear();
            // Dagangan spesial
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 32),
                    new ItemStack(ModRpgItems.WHITE_SCROLL, 2),
                    10, 5, 0.05f
            ));
            offers.add(new MerchantOffer(
                    new ItemCost(Items.DIAMOND, 16),
                    new ItemStack(ModRpgItems.DUNGEON_KEYSTONE_NIGHTMARE, 1),
                    5, 10, 0.05f
            ));
            offers.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, 48),
                    TakashaDustItem.createMagicDust(50),
                    3, 15, 0.05f
            ));
            overworld.addFreshEntity(trader);
            overworld.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, sx, sy + 1, sz, 30, 0.5, 0.5, 0.5, 0.2);
        }
    }

    public void onEnd(MinecraftServer server) {
        bossBar.removeAllPlayers();
        bossBar.setVisible(false);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal("§e§l[Takasha World Event] " + type.getDisplayName() + " §ftelah berakhir. Dunia kembali tenang."));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    public void broadcastStart(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§6=================================================="));
        player.sendSystemMessage(Component.literal("  " + type.getDisplayName()));
        player.sendSystemMessage(Component.literal("  §7" + type.getDescription()));
        player.sendSystemMessage(Component.literal("  §fDurasi: §e" + formatTime(totalDurationTicks)));
        player.sendSystemMessage(Component.literal("§6=================================================="));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.9f, 1.2f);
    }

    public double getExpMultiplier() {
        return switch (type) {
            case DOUBLE_XP -> 2.0;
            case BLOOD_MOON -> 1.5;
            default -> 1.0;
        };
    }

    public double getCoinMultiplier() {
        return switch (type) {
            case BLOOD_MOON -> 2.0;
            case GOBLIN_HUNT -> 1.5;
            default -> 1.0;
        };
    }

    public double getLootMultiplier() {
        return switch (type) {
            case DUNGEON_RUSH -> 2.0;
            case STARFALL -> 1.5;
            default -> 1.0;
        };
    }

    public double getHarvestMultiplier() {
        return (type == RpgEventType.HARVEST_FESTIVAL) ? 2.0 : 1.0;
    }

    public static String formatTime(int ticks) {
        int totalSeconds = Math.max(0, ticks / 20);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
