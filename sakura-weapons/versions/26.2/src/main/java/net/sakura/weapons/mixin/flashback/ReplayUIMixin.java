package net.sakura.weapons.mixin.flashback;

import com.moulberry.flashback.editor.ui.ReplayUI;
import net.sakura.weapons.compat.flashback.TakashaStudioWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects rendering of custom Dear ImGui floating windows (such as TakashaStudioWindow)
 * into Flashback's overlay draw cycle.
 */
@Mixin(value = ReplayUI.class, remap = false)
public abstract class ReplayUIMixin {

    @Inject(
        method = "drawOverlayInternal",
        at = @At(
            value = "INVOKE",
            target = "Lcom/moulberry/flashback/editor/ui/windows/WindowType;renderAll()V",
            shift = At.Shift.AFTER
        ),
        require = 0
    )
    private static void sakura$renderCustomWindows(CallbackInfo ci) {
        try {
            TakashaStudioWindow.render();
        } catch (Throwable ignored) {
            // never break Flashback UI
        }
    }
}
