package net.sakura.touhou.compat.tlmo.task;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sakura.touhou.entity.EntityMaid;

/**
 * MaidRevivePlayerTask
 * Tugas perlindungan darurat dari Useful Task (`studio.fantasyit.maid_useful_task`).
 * Jika pemilik berada di ambang kematian, maid otomatis mengorbankan Totem of Undying
 * atau Golden Apple dari inventarisnya untuk menyelamatkan pemilik.
 */
public class MaidRevivePlayerTask {

    public static boolean tryRevivePlayer(EntityMaid maid, Player player) {
        if (maid == null || player == null || maid.level().isClientSide() || !(maid.level() instanceof ServerLevel level)) {
            return false;
        }

        if (!maid.isOwnedBy(player)) {
            return false;
        }

        var inv = maid.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);

            // 1. Prioritas Utama: Totem of Undying
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                stack.shrink(1);

                player.setHealth(1.0f);
                player.removeAllEffects();
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));

                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.5, 0.5, 0.15);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

                if (player instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal("§6✨ [Maid] §f" + maid.getName().getString() + " §emenggunakan Totem of Undying untuk menyelamatkan nyawamu!"));
                }
                return true;
            }

            // 2. Prioritas Kedua: Golden Apple
            if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
                boolean isEnchanted = stack.is(Items.ENCHANTED_GOLDEN_APPLE);
                stack.shrink(1);

                player.heal(isEnchanted ? 8.0f : 4.0f);
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, isEnchanted ? 3 : 0));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, isEnchanted ? 400 : 100, isEnchanted ? 1 : 1));

                level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.8f, 1.2f);

                if (player instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal("§e🍎 [Maid] §f" + maid.getName().getString() + " §ememberikan Apel Emas darurat kepadamu!"));
                }
                return true;
            }
        }

        return false;
    }
}
