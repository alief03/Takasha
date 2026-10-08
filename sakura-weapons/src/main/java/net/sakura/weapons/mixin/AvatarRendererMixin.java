package net.sakura.weapons.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;
import net.sakura.weapons.client.config.TakashaNametagConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
}
