package net.sakura.weapons.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.sakura.weapons.entity.TakashaNpcEntity;
import net.sakura.weapons.registry.ModItems;
import net.sakura.weapons.registry.ModMenuTypes;

public class TakashaNpcMenu extends AbstractContainerMenu {

    private final Container npcInventory;
    private TakashaNpcEntity npc;
    private final int entityId;

    public TakashaNpcMenu(int syncId, Inventory playerInventory, int entityId) {
        super(ModMenuTypes.TAKASHA_NPC_MENU, syncId);
        this.entityId = entityId;

        Entity entity = playerInventory.player.level().getEntity(entityId);
        if (entity instanceof TakashaNpcEntity npcEntity) {
            this.npc = npcEntity;
            this.npcInventory = npcEntity.getInventory();
            // If inventory is completely empty but entity has equipment, sync from equipment safely
            boolean allEmpty = true;
            for (int i = 0; i < 7; i++) {
                if (!this.npcInventory.getItem(i).isEmpty()) {
                    allEmpty = false;
                    break;
                }
            }
            if (allEmpty) {
                this.npc.syncInventoryFromEquipment();
            }
        } else {
            this.npc = null;
            this.npcInventory = new SimpleContainer(7);
        }

        // Slot 0..3: Armor (Head, Chest, Legs, Feet)
        this.addSlot(new Slot(this.npcInventory, 0, 8, 18) {
            @Override
            public int getMaxStackSize() { return 1; }
            @Override
            public boolean mayPlace(ItemStack stack) {
                return playerInventory.player.getEquipmentSlotForItem(stack) == EquipmentSlot.HEAD;
            }
        });
        this.addSlot(new Slot(this.npcInventory, 1, 8, 36) {
            @Override
            public int getMaxStackSize() { return 1; }
            @Override
            public boolean mayPlace(ItemStack stack) {
                return playerInventory.player.getEquipmentSlotForItem(stack) == EquipmentSlot.CHEST;
            }
        });
        this.addSlot(new Slot(this.npcInventory, 2, 8, 54) {
            @Override
            public int getMaxStackSize() { return 1; }
            @Override
            public boolean mayPlace(ItemStack stack) {
                return playerInventory.player.getEquipmentSlotForItem(stack) == EquipmentSlot.LEGS;
            }
        });
        this.addSlot(new Slot(this.npcInventory, 3, 8, 72) {
            @Override
            public int getMaxStackSize() { return 1; }
            @Override
            public boolean mayPlace(ItemStack stack) {
                return playerInventory.player.getEquipmentSlotForItem(stack) == EquipmentSlot.FEET;
            }
        });

        // Slot 4: Main Hand
        this.addSlot(new Slot(this.npcInventory, 4, 76, 44));

        // Slot 5: Off Hand
        this.addSlot(new Slot(this.npcInventory, 5, 96, 44));

        // Slot 6: Wings / Cosmetic
        this.addSlot(new Slot(this.npcInventory, 6, 116, 44) {
            @Override
            public int getMaxStackSize() { return 1; }
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.SAKURA_WING) || stack.is(ModItems.PINK_LEGACY_WINGS) || playerInventory.player.getEquipmentSlotForItem(stack) == EquipmentSlot.CHEST;
            }
        });

        // Player Inventory (3 rows x 9 columns) - Slots 7..33
        int startY = 98;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, startY + row * 18));
            }
        }

        // Player Hotbar (9 columns) - Slots 34..42
        int hotbarY = startY + 58;
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }
    }

    public TakashaNpcEntity getNpc() {
        return this.npc;
    }

    public int getEntityId() {
        return this.entityId;
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (this.npc != null) {
            this.npc.syncEquipmentFromInventory();
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.npc != null) {
            this.npc.syncEquipmentFromInventory();
            this.npc.releaseEditingLock(player.getUUID());
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.npc == null || !this.npc.isAlive()) {
            return false;
        }
        if (player.distanceToSqr(this.npc) > 64.0) {
            return false;
        }
        return this.npc.canPlayerManage(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            // From NPC slots (0..6) -> Transfer to Player Inventory (7..42)
            if (slotIndex < 7) {
                if (!this.moveItemStackTo(stackInSlot, 7, 43, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From Player Inventory (7..42) -> Transfer to NPC slots
                EquipmentSlot equipSlot = player.getEquipmentSlotForItem(stackInSlot);
                boolean moved = false;

                if (equipSlot == EquipmentSlot.HEAD) {
                    moved = this.moveItemStackTo(stackInSlot, 0, 1, false);
                } else if (equipSlot == EquipmentSlot.CHEST) {
                    moved = this.moveItemStackTo(stackInSlot, 1, 2, false);
                } else if (equipSlot == EquipmentSlot.LEGS) {
                    moved = this.moveItemStackTo(stackInSlot, 2, 3, false);
                } else if (equipSlot == EquipmentSlot.FEET) {
                    moved = this.moveItemStackTo(stackInSlot, 3, 4, false);
                } else if (stackInSlot.is(ModItems.SAKURA_WING) || stackInSlot.is(ModItems.PINK_LEGACY_WINGS)) {
                    moved = this.moveItemStackTo(stackInSlot, 6, 7, false);
                }

                if (!moved) {
                    // Try main hand, then offhand
                    if (!this.moveItemStackTo(stackInSlot, 4, 6, false)) {
                        // Quick-move between hotbar and main inventory
                        if (slotIndex >= 7 && slotIndex < 34) {
                            if (!this.moveItemStackTo(stackInSlot, 34, 43, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else if (slotIndex >= 34 && slotIndex < 43) {
                            if (!this.moveItemStackTo(stackInSlot, 7, 34, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }
        return itemstack;
    }
}
