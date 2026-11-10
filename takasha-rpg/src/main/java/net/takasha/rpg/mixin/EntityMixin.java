package net.takasha.rpg.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void takasha$preventVfxSave(ValueOutput valueOutput, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.entityTags().contains("takasha_temp_vfx")
                || self.entityTags().contains("takasha_damage_indicator")
                || net.takasha.rpg.modelengine.TakashaModelEngine.isManagedDisplay(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldBeSaved", at = @At("HEAD"), cancellable = true)
    private void takasha$preventVfxShouldBeSaved(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.entityTags().contains("takasha_temp_vfx")
                || self.entityTags().contains("takasha_damage_indicator")
                || net.takasha.rpg.modelengine.TakashaModelEngine.isManagedDisplay(self)) {
            cir.setReturnValue(false);
        }
    }
}
