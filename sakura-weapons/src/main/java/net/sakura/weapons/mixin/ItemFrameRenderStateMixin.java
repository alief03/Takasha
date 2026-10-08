package net.sakura.weapons.mixin;

import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.world.item.ItemStack;
import net.sakura.weapons.client.ItemFrameRenderStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemFrameRenderState.class)
public class ItemFrameRenderStateMixin implements ItemFrameRenderStateAccess {
    @Unique
    private ItemStack sakura$item = ItemStack.EMPTY;

    @Override
    public ItemStack sakura$getItem() {
        return this.sakura$item;
    }

    @Override
    public void sakura$setItem(ItemStack stack) {
        this.sakura$item = stack != null ? stack : ItemStack.EMPTY;
    }
}
