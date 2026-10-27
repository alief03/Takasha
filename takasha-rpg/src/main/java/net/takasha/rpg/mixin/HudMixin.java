package net.takasha.rpg.mixin;

import net.minecraft.client.gui.Hud;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.takasha.rpg.client.hud.AuraHealthScaling;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Hud.class)
public abstract class HudMixin {

    @Redirect(
        method = "extractPlayerHealth",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getHealth()F")
    )
    private float redirectHealthForHud(Player player) {
        return AuraHealthScaling.getVisualHealth(player);
    }

    @Redirect(
        method = "extractPlayerHealth",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D")
    )
    private double redirectMaxHealthForHud(Player player, Holder<Attribute> attribute) {
        if (attribute == Attributes.MAX_HEALTH) {
            return AuraHealthScaling.getVisualMaxHealth(player);
        }
        return player.getAttributeValue(attribute);
    }

    @Redirect(
        method = "extractPlayerHealth",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAbsorptionAmount()F")
    )
    private float redirectAbsorptionForHud(Player player) {
        return AuraHealthScaling.getVisualAbsorption(player);
    }
}
