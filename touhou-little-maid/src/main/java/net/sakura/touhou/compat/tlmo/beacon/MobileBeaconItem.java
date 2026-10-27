package net.sakura.touhou.compat.tlmo.beacon;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.sakura.touhou.compat.tlmo.beacon.client.BeaconScreen;

import java.util.List;

/**
 * MobileBeaconItem
 * Item Beacon portabel (`maidbeacon:mobile_beacon`) dari TLMO Maid Beacon.
 * Klik kanan di tangan membuka antarmuka konfigurasi aura beacon.
 * Dapat dipasang di slot maid untuk memancarkan aura efek proteksi ke sekitar.
 */
public class MobileBeaconItem extends Item {

    public MobileBeaconItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            openClientScreen(stack);
        }
        return InteractionResult.SUCCESS;
    }

    @Environment(EnvType.CLIENT)
    private void openClientScreen(ItemStack stack) {
        Minecraft.getInstance().setScreenAndShow(new BeaconScreen(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> consumer, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, consumer, flag);
        consumer.accept(Component.literal("§7Item Beacon Portabel untuk Maid.").withStyle(ChatFormatting.GRAY));
        consumer.accept(Component.literal("§6Efek Aura: §eSpeed, Resistance, Regeneration").withStyle(ChatFormatting.GOLD));
        consumer.accept(Component.literal("§bKlik kanan untuk mengatur efek & level aura.").withStyle(ChatFormatting.AQUA));
        consumer.accept(Component.literal("§dPasang di tas/bauble maid untuk mengaktifkan halo.").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
