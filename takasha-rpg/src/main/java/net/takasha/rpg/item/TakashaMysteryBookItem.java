package net.takasha.rpg.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.takasha.rpg.enchant.TakashaEnchantRegistry;
import net.takasha.rpg.enchant.TakashaEnchantRegistry.CustomEnchantDef;
import net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier;

import java.util.function.Consumer;

/**
 * TakashaMysteryBookItem
 * Buku Misteri 9 Tier Kelangkaan otentik AdvancedEnchantments.
 * Berupa Grimoire Kuno Tertutup / Tersegel.
 * Klik kanan di tangan mengonsumsi 1 buku dan memberikan buku sihir terbuka (TakashaCustomBookItem).
 */
public class TakashaMysteryBookItem extends Item {

    private final EnchantTier tier;

    public TakashaMysteryBookItem(Properties properties, EnchantTier tier) {
        super(properties);
        this.tier = tier;
    }

    public EnchantTier getTier() {
        return tier;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(tier.getColorCode() + "§l" + tier.getDisplayName().toUpperCase() + " MYSTERY BOOK");
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            ItemStack revealed = TakashaCustomBookItem.createRandomRevealedBook(tier);
            revealed.setCount(1);
            held.shrink(1);

            String name = TakashaCustomBookItem.getEnchantId(revealed);
            CustomEnchantDef def = TakashaEnchantRegistry.get(name);
            String displayName = def != null ? def.name : (name != null ? name : "Sihir Kuno");
            int lvl = TakashaCustomBookItem.getEnchantLevel(revealed);
            int success = TakashaCustomBookItem.getSuccessRate(revealed);
            int destroy = TakashaCustomBookItem.getDestroyRate(revealed);

            if (!player.getInventory().add(revealed)) {
                player.drop(revealed, false);
            }
            player.containerMenu.broadcastChanges();

            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 35, 0.4, 0.4, 0.4, 0.1);
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.3, 0.3, 0.3, 0.15);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);

            player.sendSystemMessage(Component.literal("§a§l✦ BUKU TERBUKA! ✦\n"
                    + tier.getColorCode() + "§l" + displayName + " " + TakashaCustomBookItem.toRoman(lvl)
                    + " §f(Sukses: §a" + success + "%§f, Hancur: §c" + destroy + "%§f)"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, consumer, flag);

        consumer.accept(Component.literal("§7Buku misteri berisi mantra rahasia kuno."));
        consumer.accept(Component.literal("§8Tingkat: " + tier.getColorCode() + tier.getDisplayName() + " §7(" + tier.getEnchantCount() + " Variasi)"));
        consumer.accept(Component.literal(""));
        consumer.accept(Component.literal("§e✦ Klik Kanan di Tangan untuk Membuka ✦"));
    }
}
