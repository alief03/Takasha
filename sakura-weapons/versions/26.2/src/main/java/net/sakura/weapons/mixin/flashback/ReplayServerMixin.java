package net.sakura.weapons.mixin.flashback;

import com.moulberry.flashback.playback.ReplayServer;
import net.sakura.weapons.SakuraWeaponsMod;
import net.sakura.weapons.client.replay.ReplayPoseOverrideManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects into Flashback's ReplayServer to automatically load recorded player and NPC
 * emote keyframes whenever a replay session is initialized.
 */
@Mixin(value = ReplayServer.class, remap = false)
public abstract class ReplayServerMixin {

    @Inject(method = "initServer", at = @At("RETURN"), require = 0)
    private void sakura$onInitServer(CallbackInfoReturnable<Boolean> cir) {
        try {
            if (cir.getReturnValue()) {
                ReplayServer server = (ReplayServer) (Object) this;
                if (server.getMetadata() != null && server.getMetadata().replayIdentifier != null) {
                    ReplayPoseOverrideManager.loadSession(server.getMetadata().replayIdentifier);
                    SakuraWeaponsMod.LOGGER.info("Flashback ReplayServer initialized - loaded session overrides: {}", server.getMetadata().replayIdentifier);
                }
            }
        } catch (Throwable t) {
            // Guard against any soft-dependency failures
            SakuraWeaponsMod.LOGGER.warn("Failed to load replay session in ReplayServerMixin: {}", t.getMessage());
        }
    }
}
