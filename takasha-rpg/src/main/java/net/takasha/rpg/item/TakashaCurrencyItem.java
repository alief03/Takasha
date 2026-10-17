package net.takasha.rpg.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.takasha.rpg.api.RarityTier;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;

import java.util.function.Consumer;

public class TakashaCurrencyItem extends Item {

    public enum CurrencyType {
        MONEY,
        GEM
    }

    private final RarityTier tier;
    private final CurrencyType currencyType;
    private final double valuePerUnit;

    public TakashaCurrencyItem(Properties properties, RarityTier tier, CurrencyType currencyType, double valuePerUnit) {
        super(properties);
        this.tier = tier;
        this.currencyType = currencyType;
        this.valuePerUnit = valuePerUnit;
    }

    public RarityTier getTier() {
        return tier;
    }

    public CurrencyType getCurrencyType() {
        return currencyType;
    }

    public double getValuePerUnit() {
        return valuePerUnit;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            TakashaRpgData data = TakashaRpgManager.getOrCreateData(serverPlayer);
            int countToDeposit = player.isShiftKeyDown() ? stack.getCount() : 1;

            if (currencyType == CurrencyType.MONEY) {
                double total = valuePerUnit * countToDeposit;
                data.addBalance(total);
                serverPlayer.sendSystemMessage(Component.literal("§6§l[Dompet] §aMenyetor §e$" + String.format("%.2f", total)
                        + " §ake saldo dompet! Total Saldo: §e$" + String.format("%.2f", data.getBalance())));
            } else {
                int totalGems = (int) (valuePerUnit * countToDeposit);
                data.addGems(totalGems);
                serverPlayer.sendSystemMessage(Component.literal("§d§l[Permata] §aMenyetor §b" + totalGems
                        + " Takasha Gems§a! Total Permata: §b" + data.getGems()));
            }

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.4f);

            stack.consume(countToDeposit, serverPlayer);
            TakashaRpgManager.savePlayerData(serverPlayer.getUUID());
            TakashaRpgManager.syncToClient(serverPlayer);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, consumer, flag);
        consumer.accept(tier.getBadge());

        if (currencyType == CurrencyType.MONEY) {
            consumer.accept(Component.literal("§7Nilai: §a$" + String.format("%.2f", valuePerUnit)));
        } else {
            consumer.accept(Component.literal("§7Nilai: §b" + (int) valuePerUnit + " Takasha Gem"));
        }

        consumer.accept(Component.literal("§8Klik Kanan: Setor 1 unit ke dompet"));
        consumer.accept(Component.literal("§8Shift + Klik Kanan: Setor seluruh tumpukan"));
    }
}
