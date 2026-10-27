package net.takasha.rpg.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * ReadOnlyChestMenu
 * Container menu untuk antarmuka GUI RPG (Skills, Shop, DailyShop, Enchanter, Transaction)
 * yang mencegah pemain mengambil item display dari dalam menu, dengan dukungan deteksi
 * jenis klik (Left-click, Right-click, Shift-click).
 */
public class ReadOnlyChestMenu extends ChestMenu {

    @FunctionalInterface
    public interface SlotClickAction {
        void onClick(int slotId, int button, ContainerInput clickType, Player player);
    }

    private final Consumer<Integer> clickCallback;
    private SlotClickAction slotClickAction;

    public ReadOnlyChestMenu(int containerId, Inventory playerInventory, Container container, int rows, SlotClickAction slotClickAction) {
        super(getMenuType(rows), containerId, playerInventory, container, rows);
        this.slotClickAction = slotClickAction;
        this.clickCallback = null;
    }

    public void setSlotClickAction(SlotClickAction slotClickAction) {
        this.slotClickAction = slotClickAction;
    }

    public ReadOnlyChestMenu(int containerId, Inventory playerInventory, Container container, int rows, Consumer<Integer> clickCallback) {
        super(getMenuType(rows), containerId, playerInventory, container, rows);
        this.clickCallback = clickCallback;
        this.slotClickAction = null;
    }

    public ReadOnlyChestMenu(int containerId, Inventory playerInventory, Container container, int rows) {
        this(containerId, playerInventory, container, rows, (Consumer<Integer>) null);
    }

    private static MenuType<ChestMenu> getMenuType(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId >= 0 && slotId < this.getContainer().getContainerSize()) {
            if (slotClickAction != null) {
                slotClickAction.onClick(slotId, button, clickType, player);
            } else if (clickCallback != null) {
                clickCallback.accept(slotId);
            }
            return; // Mencegah item display terambil
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY; // Mencegah shift-click memindahkan item
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
