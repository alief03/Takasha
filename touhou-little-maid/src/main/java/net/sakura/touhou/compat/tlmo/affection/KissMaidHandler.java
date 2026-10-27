package net.sakura.touhou.compat.tlmo.affection;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.sakura.touhou.entity.EntityMaid;
import net.sakura.touhou.registry.ModEffects;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KissMaidHandler
 * Menangani interaksi kasih sayang / petting / cium maid dari Touhou Maid Affection.
 * Dipicu saat Sneak (Shift) + Klik Kanan dengan tangan kosong.
 */
public class KissMaidHandler {

    private record KissRecord(int count, long lastTimestamp) {}
    private static final Map<UUID, KissRecord> KISS_TRACKER = new ConcurrentHashMap<>();

    public static InteractionResult handleKiss(EntityMaid maid, Player player) {
        if (maid == null || player == null) {
            return InteractionResult.PASS;
        }

        if (maid.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!maid.isOwnedBy(player)) {
            player.sendSystemMessage(Component.literal("§7[Maid] Hanya pemilik maid yang dapat menunjukkan kasih sayang ini."));
            return InteractionResult.CONSUME;
        }

        if (!(maid.level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        long now = System.currentTimeMillis();
        UUID ownerId = player.getUUID();
        KissRecord record = KISS_TRACKER.getOrDefault(ownerId, new KissRecord(0, 0));

        // Jika interaksi terakhir > 10 detik lalu, reset hitungan
        int nextCount = (now - record.lastTimestamp < 10000) ? record.count + 1 : 1;
        KISS_TRACKER.put(ownerId, new KissRecord(nextCount, now));

        // Tambah favorability
        int fav = maid.getFavorability();
        if (fav < 200) {
            maid.setFavorability(fav + 1);
        }

        // Efek Visual & Audio
        level.sendParticles(ParticleTypes.HEART, maid.getX(), maid.getY() + 1.2, maid.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
        level.playSound(null, maid.getX(), maid.getY(), maid.getZ(), SoundEvents.CAT_PURREOW_BABY.value(), SoundSource.NEUTRAL, 0.8f, 1.3f);

        // Ambang batas (5 kali berturut-turut): Memberikan buff MAIDS_PRAYER
        if (nextCount >= 5) {
            int amplifier = Math.min(2, fav / 50);
            int durationTicks = 600 + (amplifier * 200); // 30 - 50 detik

            player.addEffect(new MobEffectInstance(ModEffects.MAIDS_PRAYER, durationTicks, amplifier));
            maid.addEffect(new MobEffectInstance(ModEffects.MAIDS_PRAYER, durationTicks, amplifier));

            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, maid.getX(), maid.getY() + 1.0, maid.getZ(), 15, 0.3, 0.3, 0.3, 0.1);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.8f);

            player.sendSystemMessage(Component.literal("§d❤ [Touhou Maid] §f" + maid.getName().getString() + " memberikan §eDoa sang Maid (Maid's Prayer)§f kepadamu!"));
            KISS_TRACKER.put(ownerId, new KissRecord(0, now));
        } else {
            player.sendSystemMessage(Component.literal("§d❤ §7Kamu membelai kepala §f" + maid.getName().getString() + " §7dengan penuh kasih sayang (" + nextCount + "/5)."));
        }

        return InteractionResult.SUCCESS;
    }
}
