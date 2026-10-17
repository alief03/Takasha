package net.sakura.weapons.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.sakura.weapons.client.fishing.CapcayManager;
import net.sakura.weapons.client.fishing.TakashaFishingHandler;
import net.sakura.weapons.client.fortune.TakashaFortuneReshaper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class TakashaClientPacketListenerMixin extends ClientCommonPacketListenerImpl {

    protected TakashaClientPacketListenerMixin(Minecraft client, Connection connection, CommonListenerCookie connectionState) {
        super(client, connection, connectionState);
    }

    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void onSoundEvent(ClientboundSoundPacket packet, CallbackInfo ci) {
        if (this.minecraft.isSameThread()) {
            TakashaFishingHandler.getInstance().handleSoundPacket(packet, this.minecraft);
        }
    }

    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"))
    private void onEntityMotion(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        if (this.minecraft.isSameThread()) {
            TakashaFishingHandler.getInstance().handleMotionPacket(packet, this.minecraft);
        }
    }

    @Inject(method = "handleSystemChat", at = @At("HEAD"), cancellable = true)
    private void onSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        if (this.minecraft.isSameThread() && packet != null && packet.content() != null) {
            String text = packet.content().getString();
            CapcayManager.getInstance().onChatReceived(text);

            boolean cancel = TakashaFortuneReshaper.getInstance().handleIncomingText(packet.content(), packet.overlay());
            if (cancel) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "handleDisguisedChat", at = @At("HEAD"), cancellable = true)
    private void onDisguisedChat(ClientboundDisguisedChatPacket packet, CallbackInfo ci) {
        if (this.minecraft.isSameThread() && packet != null && packet.message() != null) {
            String text = packet.message().getString();
            CapcayManager.getInstance().onChatReceived(text);

            boolean cancel = TakashaFortuneReshaper.getInstance().handleIncomingText(packet.message(), false);
            if (cancel) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "setActionBarText", at = @At("HEAD"), cancellable = true)
    private void onSetActionBarText(ClientboundSetActionBarTextPacket packet, CallbackInfo ci) {
        if (this.minecraft.isSameThread() && packet != null && packet.text() != null) {
            String text = packet.text().getString();
            CapcayManager.getInstance().onChatReceived(text);

            boolean cancel = TakashaFortuneReshaper.getInstance().handleIncomingText(packet.text(), true);
            if (cancel) {
                ci.cancel();
            }
        }
    }
}
