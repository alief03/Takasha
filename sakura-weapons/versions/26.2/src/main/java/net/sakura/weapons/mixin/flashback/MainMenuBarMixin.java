package net.sakura.weapons.mixin.flashback;

import com.moulberry.flashback.editor.ui.windows.MainMenuBar;
import imgui.moulberry90.ImGui;
import net.sakura.weapons.client.replay.ReplayPoseOverrideManager;
import net.sakura.weapons.compat.flashback.TakashaStudioWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects the "🌸 Sakura" menu item into Flashback's top main menu bar.
 */
@Mixin(value = MainMenuBar.class, remap = false)
public abstract class MainMenuBarMixin {

    @Inject(method = "renderInner", at = @At("TAIL"), require = 0)
    private static void sakura$renderSakuraMenu(CallbackInfo ci) {
        try {
            if (ImGui.beginMenu("🌸 Sakura")) {
                if (ImGui.menuItem("Studio Sakura", "F8", TakashaStudioWindow.isOpen())) {
                    TakashaStudioWindow.toggle();
                }
                ImGui.separator();
                if (ImGui.menuItem("Reset Semua Pose Override")) {
                    ReplayPoseOverrideManager.clearAll();
                }
                ImGui.endMenu();
            }
        } catch (Throwable ignored) {
            // never break Flashback UI
        }
    }
}
