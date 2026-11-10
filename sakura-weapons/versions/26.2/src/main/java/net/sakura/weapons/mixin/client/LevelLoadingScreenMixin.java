package net.sakura.weapons.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.network.chat.Component;
import net.sakura.weapons.SakuraWeaponsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Secondary safety-net mixin for LevelLoadingScreen to guarantee that players on multiplayer
 * proxy networks never remain stuck indefinitely on "Loading terrain...".
 */
@Environment(EnvType.CLIENT)
@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {

    @Shadow private LevelLoadTracker loadTracker;

    @Unique
    private int takasha$screenTicks = 0;

    protected LevelLoadingScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void takasha$watchdogLoadingScreen(CallbackInfo ci) {
        Minecraft mc = this.minecraft;
        if (mc == null) return;

        boolean isMultiplayer = mc.getCurrentServer() != null && !mc.isLocalServer();
        if (!isMultiplayer) return;

        this.takasha$screenTicks++;

        // If the loading screen stays open for > 5 seconds (100 ticks) while the player and level are already valid:
        // Force dismiss the loading screen so the player enters the world smoothly.
        if (this.takasha$screenTicks > 100 && mc.player != null && mc.level != null) {
            SakuraWeaponsMod.LOGGER.warn("[Takasha] LevelLoadingScreen hang watchdog triggered after {} ticks. Dismissing screen.", this.takasha$screenTicks);
            if (this.loadTracker != null) {
                try {
                    this.loadTracker.loadingPacketsReceived();
                } catch (Throwable ignored) {
                }
            }
            this.onClose();
        }
    }
}

