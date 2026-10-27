package net.takasha.rpg.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.takasha.rpg.trade.TakashaVillagerTrades;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WanderingTrader.class)
public abstract class WanderingTraderMixin {
    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void takasha$onUpdateTrades(ServerLevel serverLevel, CallbackInfo ci) {
        TakashaVillagerTrades.injectWanderingTraderTrades((WanderingTrader) (Object) this, serverLevel);
    }
}
