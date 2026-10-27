package net.takasha.rpg.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;

/**
 * VirtualWorkstationMenus
 * Kelas pembungkus untuk workstation portabel virtual (Anvil, Grindstone, Stonecutter,
 * Smithing Table, Cartography Table, Loom) yang tidak terikat ke koordinat balok fisik di dunia.
 */
public class VirtualWorkstationMenus {

    public static class VirtualAnvilMenu extends AnvilMenu {
        public VirtualAnvilMenu(int containerId, Inventory playerInventory) {
            super(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), playerInventory.player.blockPosition()));
        }

        @Override
        public void createResult() {
            super.createResult();
            if (this.resultSlots.getItem(0).isEmpty()) {
                net.minecraft.world.item.ItemStack left = this.inputSlots.getItem(0);
                net.minecraft.world.item.ItemStack right = this.inputSlots.getItem(1);
                if (!left.isEmpty() && !right.isEmpty() && right.getItem() instanceof net.takasha.rpg.item.TakashaCustomBookItem) {
                    net.takasha.rpg.enchant.TakashaAnvilHandler.ApplyResult res = net.takasha.rpg.enchant.TakashaAnvilHandler.applyBookToGear(left, right, false);
                    if (res.success && res.resultStack != null && !res.resultStack.isEmpty()) {
                        this.resultSlots.setItem(0, res.resultStack);
                        int calculatedCost = Math.max(1, net.takasha.rpg.item.TakashaCustomBookItem.getEnchantLevel(right) * 2);
                        try {
                            java.lang.reflect.Field costField = AnvilMenu.class.getDeclaredField("cost");
                            costField.setAccessible(true);
                            ((DataSlot) costField.get(this)).set(calculatedCost);
                        } catch (Exception ignored) {
                            this.setData(0, calculatedCost);
                        }
                    } else {
                        this.resultSlots.setItem(0, net.minecraft.world.item.ItemStack.EMPTY);
                        try {
                            java.lang.reflect.Field costField = AnvilMenu.class.getDeclaredField("cost");
                            costField.setAccessible(true);
                            ((DataSlot) costField.get(this)).set(0);
                        } catch (Exception ignored) {
                            this.setData(0, 0);
                        }
                    }
                    this.broadcastChanges();
                }
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    public static class VirtualGrindstoneMenu extends GrindstoneMenu {
        public VirtualGrindstoneMenu(int containerId, Inventory playerInventory) {
            super(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), playerInventory.player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    public static class VirtualStonecutterMenu extends StonecutterMenu {
        public VirtualStonecutterMenu(int containerId, Inventory playerInventory) {
            super(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), playerInventory.player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    public static class VirtualSmithingMenu extends SmithingMenu {
        public VirtualSmithingMenu(int containerId, Inventory playerInventory) {
            super(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), playerInventory.player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    public static class VirtualCartographyTableMenu extends CartographyTableMenu {
        public VirtualCartographyTableMenu(int containerId, Inventory playerInventory) {
            super(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), playerInventory.player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    public static class VirtualLoomMenu extends LoomMenu {
        public VirtualLoomMenu(int containerId, Inventory playerInventory) {
            super(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), playerInventory.player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }
}
