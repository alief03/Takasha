package net.sakura.touhou.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.sakura.touhou.entity.EntityMaid;
import net.sakura.touhou.registry.ModMenuTypes;

/**
 * MaidMainContainer
 * Screen handler container untuk inventaris, perlengkapan, bauble beacon, dan interaksi maid.
 * Mendukung sinkronisasi client-server di Minecraft 26.2.
 */
public class MaidMainContainer extends AbstractContainerMenu {

    private final EntityMaid maid;
    private final int entityId;
    private final Container maidInv;

    public MaidMainContainer(int syncId, Inventory playerInventory, int entityId) {
        super(ModMenuTypes.MAID_MAIN_CONTAINER, syncId);
        this.entityId = entityId;

        Entity entity = playerInventory.player.level().getEntity(entityId);
        if (entity instanceof EntityMaid maidEntity) {
            this.maid = maidEntity;
            this.maidInv = maidEntity.getInventory();
        } else {
            this.maid = null;
            this.maidInv = new SimpleContainer(36);
        }

        // Slot 0: Bauble / Mobile Beacon Slot (leftPos + 80, topPos + 18)
        this.addSlot(new Slot(this.maidInv, 0, 80, 18) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Slot 1..5: Maid Quick Backpack / Hotbar (x: 98..170, y: 18)
        for (int i = 1; i <= 5; i++) {
            this.addSlot(new Slot(this.maidInv, i, 80 + i * 18, 18));
        }

        // Slot 6..23: Maid Storage (2 rows of 9: x: 80..224, y: 40..58)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                int index = 6 + row * 9 + col;
                this.addSlot(new Slot(this.maidInv, index, 80 + col * 18, 40 + row * 18));
            }
        }

        // Player Inventory (3 rows of 9: y: 84..120)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, 9 + row * 9 + col, 80 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (1 row of 9: y: 142)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 80 + col * 18, 142));
        }
    }

    public EntityMaid getMaid() {
        return maid;
    }

    public int getEntityId() {
        return entityId;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack currentStack = slot.getItem();
            itemStack = currentStack.copy();

            // Maid slots: 0..23 (24 slots)
            if (slotIndex < 24) {
                // Move from Maid to Player (slots 24..59)
                if (!this.moveItemStackTo(currentStack, 24, 60, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from Player to Maid (slots 0..23)
                if (!this.moveItemStackTo(currentStack, 0, 24, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (currentStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (currentStack.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, currentStack);
        }

        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.maid != null && this.maid.isAlive() && this.maid.distanceToSqr(player) < 64.0;
    }
}
