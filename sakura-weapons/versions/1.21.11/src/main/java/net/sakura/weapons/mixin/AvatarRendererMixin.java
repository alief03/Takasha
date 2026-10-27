package net.sakura.weapons.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.Player;
import net.sakura.weapons.client.config.TakashaNametagConfig;
import net.sakura.weapons.client.replay.PlayerReplayPlaybackBridge;
import net.sakura.weapons.compat.flashback.FlashbackCompatHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("HEAD"), cancellable = true)
    private void takasha$suppressPlayerNametag(Avatar entity, double distanceSq, CallbackInfoReturnable<Boolean> cir) {
        if (TakashaNametagConfig.isPlayerNametagDisabled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL")
    )
    private void takasha$onExtractRenderState(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        if (entity instanceof Player player) {
            if (FlashbackCompatHelper.isFlashbackActive()) {
                PlayerReplayPlaybackBridge.applyReplayEmote(player, state, partialTick);
            }
        }
    }
}
