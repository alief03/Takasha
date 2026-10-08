package net.sakura.weapons.mixin.flashback;

import com.moulberry.flashback.editor.ui.windows.SelectedEntityPopup;
import com.moulberry.flashback.state.EditorState;
import net.minecraft.world.entity.Entity;
import net.sakura.weapons.compat.flashback.FlashbackSakuraPanel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the "Sakura: Pose & Emote" section to Flashback's selected-entity popup. */
@Mixin(value = SelectedEntityPopup.class, remap = false)
public abstract class SelectedEntityPopupMixin {

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private static void sakura$renderSakuraPanel(Entity entity, EditorState editorState, CallbackInfo ci) {
        try {
            FlashbackSakuraPanel.render(entity);
        } catch (Throwable ignored) {
            // never break Flashback's UI
        }
    }
}
