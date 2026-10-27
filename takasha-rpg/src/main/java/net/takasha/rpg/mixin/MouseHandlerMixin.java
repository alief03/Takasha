package net.takasha.rpg.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin untuk mencegah kursor mouse ter-reset / melompat ke tengah layar
 * setiap kali berpindah atau membuka menu GUI (Server Menus, Dialog, Shop, Enchanter, Grimoire, dll.).
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow private double xpos;
    @Shadow private double ypos;
    @Shadow private boolean mouseGrabbed;
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Unique private static double takasha$lastGuiMouseX = -1;
    @Unique private static double takasha$lastGuiMouseY = -1;
    @Unique private static long takasha$lastGuiCloseTime = 0;
    @Unique private static boolean takasha$hasSavedGuiPosition = false;

    /**
     * Saat mouse bebas (pemain sedang di dalam GUI dan menggerakkan kursor),
     * simpan posisi kursor terkini secara terus-menerus.
     */
    @Inject(method = "onMove", at = @At("HEAD"))
    private void takasha$onMouseMove(long window, double x, double y, CallbackInfo ci) {
        if (this.minecraft == null || !this.minecraft.hasSingleplayerServer()) {
            return;
        }
        if (!this.mouseGrabbed) {
            takasha$lastGuiMouseX = x;
            takasha$lastGuiMouseY = y;
            takasha$lastGuiCloseTime = System.currentTimeMillis();
            takasha$hasSavedGuiPosition = true;
        }
    }

    /**
     * Saat mouse di-grab (misalnya ketika GUI lama ditutup oleh server sebelum membuka GUI baru),
     * simpan koordinat kursor terakhir jika sebelumnya mouse sedang bebas (berada di dalam GUI).
     */
    @Inject(method = "grabMouse", at = @At("HEAD"))
    private void takasha$onGrabMouse(CallbackInfo ci) {
        if (this.minecraft == null || !this.minecraft.hasSingleplayerServer()) {
            return;
        }
        if (!this.mouseGrabbed) {
            takasha$lastGuiMouseX = this.xpos;
            takasha$lastGuiMouseY = this.ypos;
            takasha$lastGuiCloseTime = System.currentTimeMillis();
            takasha$hasSavedGuiPosition = true;
        }
    }

    /**
     * Saat pemain benar-benar menggerakkan kamera dalam gameplay first-person,
     * hapus posisi kursor GUI lama hanya jika sudah berada di dunia nyata (> 2.5 detik).
     * Hal ini mencegah race-condition di mana delta mouse sisa saat klik GUI menghapus
     * koordinat sebelum layar berikutnya sempat terbuka.
     */
    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void takasha$onTurnPlayer(double time, CallbackInfo ci) {
        if (this.minecraft == null || !this.minecraft.hasSingleplayerServer()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - takasha$lastGuiCloseTime < 2500) {
            return; // Masih dalam jendela transisi GUI, lindungi koordinat kursor!
        }

        if (this.accumulatedDX != 0.0 || this.accumulatedDY != 0.0) {
            takasha$hasSavedGuiPosition = false;
            takasha$lastGuiCloseTime = 0;
            takasha$lastGuiMouseX = -1;
            takasha$lastGuiMouseY = -1;
        }
    }

    /**
     * Saat mouse di-release (GUI baru terbuka):
     * Jika transisi terjadi dalam interval singkat (< 2.5 detik), pulihkan posisi kursor
     * ke koordinat persis sebelum transisi daripada mereset ke tengah layar (screenWidth / 2, screenHeight / 2).
     */
    @Inject(method = "releaseMouse", at = @At("HEAD"), cancellable = true)
    private void takasha$onReleaseMouse(CallbackInfo ci) {
        if (this.minecraft == null || !this.minecraft.hasSingleplayerServer()) {
            return;
        }
        if (!this.mouseGrabbed) {
            return;
        }

        long now = System.currentTimeMillis();
        if ((now - takasha$lastGuiCloseTime < 2500) && takasha$lastGuiMouseX >= 0 && takasha$lastGuiMouseY >= 0) {
            this.mouseGrabbed = false;

            if (this.minecraft.getWindow() != null) {
                int screenWidth = this.minecraft.getWindow().getScreenWidth();
                int screenHeight = this.minecraft.getWindow().getScreenHeight();

                if (screenWidth > 0 && screenHeight > 0) {
                    this.xpos = Math.max(0.0, Math.min((double) screenWidth, takasha$lastGuiMouseX));
                    this.ypos = Math.max(0.0, Math.min((double) screenHeight, takasha$lastGuiMouseY));
                } else {
                    this.xpos = takasha$lastGuiMouseX;
                    this.ypos = takasha$lastGuiMouseY;
                }
            } else {
                this.xpos = takasha$lastGuiMouseX;
                this.ypos = takasha$lastGuiMouseY;
            }

            InputConstants.grabOrReleaseMouse(this.minecraft.getWindow(), 212993, this.xpos, this.ypos);
            ci.cancel();
        }
    }
}
