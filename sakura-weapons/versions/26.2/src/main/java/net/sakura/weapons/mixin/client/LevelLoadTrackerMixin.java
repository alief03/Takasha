package net.sakura.weapons.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.sakura.weapons.SakuraWeaponsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Watchdog mixin to prevent infinite "Loading terrain..." hangs when transferring between
 * proxy subservers (such as BungeeCord/Velocity Hub -> Tycoon on KaizenMC).
 * 
 * Vanilla 26.2 relies on ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START to advance
 * LevelLoadTracker from WaitingForServer to WaitingForPlayerChunk. Because WaitingForServer
 * lacks a timeout in tick(), if a proxy or backend subserver omits this game event packet,
 * the client deadlocks on the terrain screen indefinitely.
 */
@Environment(EnvType.CLIENT)
@Mixin(LevelLoadTracker.class)
public abstract class LevelLoadTrackerMixin {

    @Shadow public abstract void loadingPacketsReceived();

    @Unique
    private int takasha$waitingTicks = 0;

    @Inject(method = "startClientLoad", at = @At("HEAD"))
    private void takasha$onStartClientLoad(LocalPlayer player, ClientLevel level, CallbackInfo ci) {
        this.takasha$waitingTicks = 0;
    }

    @Inject(method = "tickClientLoad", at = @At("HEAD"))
    private void takasha$onTickClientLoad(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        boolean isMultiplayer = mc.getCurrentServer() != null && !mc.isLocalServer();
        if (!isMultiplayer) {
            return;
        }

        this.takasha$waitingTicks++;

        // If the client has been in transition for > 3 seconds (60 ticks) on a multiplayer server
        // without the server sending LEVEL_CHUNKS_LOAD_START:
        // Automatically trigger loadingPacketsReceived() so the tracker advances and smoothly loads into the world.
        if (this.takasha$waitingTicks == 60) {
            SakuraWeaponsMod.LOGGER.info("[Takasha] Multiplayer subserver transition watchdog triggered (60 ticks). Advancing LevelLoadTracker chunk readiness.");
            this.loadingPacketsReceived();
        }
    }
}

