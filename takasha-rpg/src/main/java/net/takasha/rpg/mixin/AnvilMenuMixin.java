package net.takasha.rpg.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.takasha.rpg.enchant.TakashaAnvilHandler;
import net.takasha.rpg.item.TakashaCustomBookItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AnvilMenuMixin
 * Mengintegrasikan penempaan buku sihir kustom (TakashaCustomBookItem) ke dalam Anvil blok fisik vanilla.
 * Memastikan hasil penempaan kosong dan biaya 0 jika slot target penuh atau tidak kompatibel,
 * sehingga mencegah kehilangan/konsumsi buku sihir secara tidak sengaja.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    @Shadow
    @Final
    private DataSlot cost;

    public AnvilMenuMixin(MenuType<?> type, int containerId, Inventory playerInventory, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slotDefinition) {
        super(type, containerId, playerInventory, access, slotDefinition);
    }

    @Inject(method = "createResult", at = @At("RETURN"))
    private void onTakashaCreateResult(CallbackInfo ci) {
        if (this.resultSlots.getItem(0).isEmpty()) {
            ItemStack left = this.inputSlots.getItem(0);
            ItemStack right = this.inputSlots.getItem(1);
            if (!left.isEmpty() && !right.isEmpty() && right.getItem() instanceof TakashaCustomBookItem) {
                TakashaAnvilHandler.ApplyResult res = TakashaAnvilHandler.applyBookToGear(left, right, false);
                if (res.success && res.resultStack != null && !res.resultStack.isEmpty()) {
                    this.resultSlots.setItem(0, res.resultStack);
                    int calculatedCost = Math.max(1, TakashaCustomBookItem.getEnchantLevel(right) * 2);
                    this.cost.set(calculatedCost);
                    this.broadcastChanges();
                } else {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    this.cost.set(0);
                    this.broadcastChanges();
                }
            }
        }
    }
}
