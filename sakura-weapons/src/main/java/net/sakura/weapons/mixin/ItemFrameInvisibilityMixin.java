package net.sakura.weapons.mixin;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.sakura.weapons.SakuraWeaponsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrame.class)
public abstract class ItemFrameInvisibilityMixin extends HangingEntity {

    protected ItemFrameInvisibilityMixin(EntityType<? extends HangingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow public abstract ItemStack getItem();
    @Shadow public abstract void setRotation(int rotation);

    @Unique
    private static boolean sakura$isTakashaItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getNamespace().equals(SakuraWeaponsMod.MOD_ID);
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void sakura$handleFrameInteraction(Player player, InteractionHand hand, Vec3 hitPos, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack held = player.getItemInHand(hand);

        // 1. Feature: Toggle Invisibility (Sneak + Empty Hand on frame with item)
        if (player.isShiftKeyDown() && held.isEmpty() && !this.getItem().isEmpty()) {
            if (!this.level().isClientSide()) {
                boolean newInvis = !this.isInvisible();
                this.setInvisible(newInvis);
                this.playSound(SoundEvents.ITEM_FRAME_ROTATE_ITEM, 1.0F, newInvis ? 1.4F : 0.8F);
                player.sendOverlayMessage(
                    Component.translatable(newInvis ? "message.sakura_weapons.item_frame.hidden" : "message.sakura_weapons.item_frame.shown")
                );
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        // 2. Feature: Auto-Orient Takasha item when placed into empty frame
        if (this.getItem().isEmpty() && !held.isEmpty() && sakura$isTakashaItem(held)) {
            if (!this.level().isClientSide()) {
                Direction dir = this.getDirection();
                if (dir == Direction.UP) {
                    // Placed on floor: orient so hilt/top is away from player and blade/bottom is towards player
                    int rot = player.getDirection().get2DDataValue() * 2;
                    this.setRotation(rot);
                } else if (dir == Direction.DOWN) {
                    // Placed on ceiling: orient aligned with player view looking up
                    int rot = (player.getDirection().get2DDataValue() * 2 + 4) % 8;
                    this.setRotation(rot);
                } else {
                    // Placed on wall: rot = 0 guarantees vertical alignment (hilt top, blade bottom)
                    this.setRotation(0);
                }
            }
        }
    }
}
