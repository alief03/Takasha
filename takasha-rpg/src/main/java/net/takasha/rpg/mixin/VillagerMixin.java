package net.takasha.rpg.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.takasha.rpg.trade.TakashaVillagerTrades;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerMixin {
    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void takasha$onUpdateTrades(ServerLevel serverLevel, CallbackInfo ci) {
        TakashaVillagerTrades.injectVillagerTrades((Villager) (Object) this, serverLevel);
    }
}
