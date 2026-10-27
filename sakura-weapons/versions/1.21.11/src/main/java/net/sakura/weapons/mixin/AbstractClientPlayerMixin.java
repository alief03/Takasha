package net.sakura.weapons.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import net.sakura.weapons.client.replay.PlayerEmoteRecorder;
import net.sakura.weapons.compat.flashback.FlashbackCompatHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to tick client player entities and capture their active emotes during Flashback recordings.
 */
@Environment(EnvType.CLIENT)
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void takasha$onClientPlayerTick(CallbackInfo ci) {
        if (FlashbackCompatHelper.isRecording()) {
            PlayerEmoteRecorder.onPlayerTick((AbstractClientPlayer) (Object) this);
        }
    }
}
