package net.takasha.rpg.item;

import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.takasha.rpg.api.RarityTier;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;

import java.util.function.Consumer;

public class TakashaExecutableItem extends Item {

    public enum ExecutableType {
        HOME_EXPANSION,
        COMMAND_UNLOCK
    }

    private final RarityTier tier;
    private final ExecutableType executableType;
    private final String commandId;
    private final String description;

    public TakashaExecutableItem(Properties properties, RarityTier tier, ExecutableType executableType, String commandId, String description) {
        super(properties);
        this.tier = tier;
        this.executableType = executableType;
        this.commandId = commandId;
        this.description = description;
    }

    public RarityTier getTier() {
        return tier;
    }

    public ExecutableType getExecutableType() {
        return executableType;
    }

    public String getCommandId() {
        return commandId;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            TakashaRpgData data = TakashaRpgManager.getOrCreateData(serverPlayer);

            if (executableType == ExecutableType.HOME_EXPANSION) {
                if (data.getMaxHomes() >= TakashaRpgData.ABSOLUTE_MAX_HOMES) {
                    serverPlayer.sendSystemMessage(Component.literal("§cKapasitas /sethome kamu sudah mencapai batas maksimal ("
                            + TakashaRpgData.ABSOLUTE_MAX_HOMES + ")!"));
                    return InteractionResult.FAIL;
                }

                data.increaseMaxHomes(1);
                serverPlayer.sendSystemMessage(Component.literal("§6§l[Takasha-RPG] §aSlot /sethome berhasil bertambah! Sekarang kamu bisa menyimpan hingga §e§l"
                        + data.getMaxHomes() + "§a home!"));

                playSuccessEffects(serverPlayer);
                stack.consume(1, serverPlayer);
                TakashaRpgManager.savePlayerData(serverPlayer.getUUID());
                TakashaRpgManager.syncToClient(serverPlayer);
                return InteractionResult.CONSUME;
            }

            if (executableType == ExecutableType.COMMAND_UNLOCK) {
                if (data.isCommandUnlocked(commandId)) {
                    serverPlayer.sendSystemMessage(Component.literal("§cKamu sudah membuka akses perintah /" + commandId + "!"));
                    return InteractionResult.FAIL;
                }

                data.unlockCommand(commandId);
                serverPlayer.sendSystemMessage(Component.literal("§6§l[Takasha-RPG] §d★ Pengetahuan Kuno Terbuka! ★\n§fKamu sekarang memiliki akses permanen ke perintah: §b§l/"
                        + commandId + "§f!"));

                playSuccessEffects(serverPlayer);
                stack.consume(1, serverPlayer);
                TakashaRpgManager.savePlayerData(serverPlayer.getUUID());
                TakashaRpgManager.syncToClient(serverPlayer);
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    private void playSuccessEffects(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.2f);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    25, 0.5, 0.5, 0.5, 0.1);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, consumer, flag);
        consumer.accept(tier.getBadge());
        consumer.accept(Component.literal(description).withStyle(ChatFormatting.GRAY));

        if (executableType == ExecutableType.HOME_EXPANSION) {
            consumer.accept(Component.literal("§bManfaat: §a+1 Kapasitas /sethome (Maks 10)"));
        } else {
            consumer.accept(Component.literal("§bMembuka Akses Perintah: §e/" + commandId));
        }

        consumer.accept(Component.literal("§8Klik Kanan untuk mempelajari tomes ini"));
    }
}
