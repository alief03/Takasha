package net.sakura.weapons.mixin;

import net.minecraft.world.inventory.AnvilMenu;
import net.sakura.weapons.util.MinecraftColorUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin to allow Minecraft built-in formatting codes (§d, §l, etc.) and
 * translate ampersand (&) color codes when renaming items in the Anvil.
 */
@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {

    @Redirect(
        method = "setItemName",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/AnvilMenu;validateName(Ljava/lang/String;)Ljava/lang/String;"
        )
    )
    private String redirectValidateName(String name) {
        return MinecraftColorUtil.validateAndFormatAnvilName(name);
    }
}
