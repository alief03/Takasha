package net.takasha.rpg.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.takasha.rpg.dungeon.DungeonArenaEngine;
import net.takasha.rpg.dungeon.DungeonTier;

import java.util.List;

/**
 * Item Kunci Celah Dungeon Sekali Pakai (Consumable Rift Keystone).
 * Dikonsumsi saat aktivasi untuk memunculkan kubah arena fisik dan memulai gelombang LevelledMobs.
 */
public class DungeonRiftKeystoneItem extends Item {

    private final DungeonTier tier;

    public DungeonRiftKeystoneItem(Item.Properties properties, DungeonTier tier) {
        super(properties);
        this.tier = tier;
    }

    public DungeonTier getTier() {
        return tier;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        if (DungeonArenaEngine.getInstance().isArenaActive()) {
            player.sendSystemMessage(Component.literal("§c[Dungeon] Sebuah arena sedang aktif! Tunggu hingga sesi selesai."));
            return InteractionResult.FAIL;
        }

        boolean started = DungeonArenaEngine.getInstance().startDungeon(serverPlayer, tier, player.blockPosition());
        if (started) {
            stack.shrink(1);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        50, 0.5, 0.5, 0.5, 0.5);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0f, 1.2f);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> consumer, TooltipFlag flag) {
        consumer.accept(Component.literal(tier.getColorCode() + "§lDUNGEON RIFT KEYSTONE"));
        consumer.accept(Component.literal("§7Tingkat: " + tier.getColorCode() + tier.getDisplayName()));
        consumer.accept(Component.literal("§8Gelombang: §f5 Wave (Wave 5: Boss Rush)"));
        consumer.accept(Component.literal("§8Skala Mob: §eLvl " + tier.getMobLevelForWave(1, new java.util.Random()) + " - " + tier.getBossLevel()));
        consumer.accept(Component.literal(""));
        consumer.accept(Component.literal("§e► Klik Kanan di tanah terbuka untuk membuka Celah Arena!"));
        consumer.accept(Component.literal("§c⚠ Peringatan: Item dikonsumsi sekali pakai (-1)!"));
        super.appendHoverText(stack, context, display, consumer, flag);
    }
}
