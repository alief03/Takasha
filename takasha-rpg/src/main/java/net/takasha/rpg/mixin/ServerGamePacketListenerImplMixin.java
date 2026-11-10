package net.takasha.rpg.mixin;

import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.takasha.rpg.mmo.MMOInteractionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleAnimate", at = @At("TAIL"))
    private void takasha$onHandleAnimate(ServerboundSwingPacket packet, CallbackInfo ci) {
        if (packet.getHand() == InteractionHand.MAIN_HAND && this.player != null) {
            net.minecraft.server.MinecraftServer s = this.player.level().getServer();
            if (s == null) return;
            if (s.isSameThread()) {
                if (!this.player.isRemoved() && !this.player.hasDisconnected()) {
                    MMOInteractionHandler.onPlayerSwing(this.player);
                }
            } else {
                s.execute(() -> {
                    if (this.player != null && !this.player.isRemoved() && !this.player.hasDisconnected()) {
                        MMOInteractionHandler.onPlayerSwing(this.player);
                    }
                });
            }
        }
    }
}

