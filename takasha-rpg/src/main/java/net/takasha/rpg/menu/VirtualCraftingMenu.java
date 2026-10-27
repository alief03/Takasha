package net.takasha.rpg.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;

/**
 * VirtualCraftingMenu
 * Meja kerajinan portabel virtual tanpa mengikat ke balok crafting table di dunia,
 * memastikan stillValid selalu true sehingga window tidak tertutup otomatis.
 */
public class VirtualCraftingMenu extends CraftingMenu {

    public VirtualCraftingMenu(int containerId, Inventory playerInventory) {
        super(containerId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), playerInventory.player.blockPosition()));
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
