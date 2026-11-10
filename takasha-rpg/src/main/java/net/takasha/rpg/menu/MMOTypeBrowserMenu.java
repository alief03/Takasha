package net.takasha.rpg.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.takasha.rpg.mmo.MMOItemManager;

import java.util.*;

/**
 * MMOTypeBrowserMenu
 * 1:1 MMOItems Type Browser GUI (54 slots) reconstructed from MMOItems Spigot bytecode & official documentation.
 *
 * Contains authentic MMOItems item types (Root Types + Sub-Types + ALL).
 * Multi-page navigation:
 *   Slot 18: Previous Page (Arrow, visible if page > 1)
 *   Slot 26: Next Page (Arrow, visible if page < maxPage)
 *   Active Grid (21 slots): [10..16, 19..25, 28..34]
 *   Background / Border: Pure AIR (ItemStack.EMPTY)
 */
public class MMOTypeBrowserMenu {

    public record TypeDef(String type, String displayName, Item icon) {}

    private static final int PAGE_SIZE = 21;
    private static final int[] GRID_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public static final List<TypeDef> TYPE_DEFS = List.of(
            // Page 1
            new TypeDef("SWORD", "Sword", Items.IRON_SWORD),
            new TypeDef("AXE", "Axe", Items.STONE_AXE),
            new TypeDef("PIERCE", "Piercing", Items.TRIDENT),
            new TypeDef("BLUNT", "Blunt", Items.IRON_AXE),
            new TypeDef("GREATSWORD", "Greatsword", Items.NETHERITE_SWORD),
            new TypeDef("KATANA", "Katana", Items.IRON_SWORD),
            new TypeDef("HALBERD", "Halberd", Items.GOLDEN_AXE),
            new TypeDef("STAFF", "Staff", Items.BLAZE_ROD),
            new TypeDef("TOME", "Tome", Items.BOOK),
            new TypeDef("DAGGER", "Dagger", Items.STONE_SWORD),
            new TypeDef("HAMMER", "Hammer", Items.NETHERITE_AXE),
            new TypeDef("GAUNTLET", "Gauntlet", Items.IRON_INGOT),
            new TypeDef("WHIP", "Whip", Items.LEAD),
            new TypeDef("BOW", "Bow", Items.BOW),
            new TypeDef("CROSSBOW", "Crossbow", Items.CROSSBOW),
            new TypeDef("MUSKET", "Musket", Items.GOLDEN_HOE),
            new TypeDef("LUTE", "Lute", Items.NOTE_BLOCK),
            new TypeDef("SHIELD", "Shield", Items.SHIELD),
            new TypeDef("ARMOR", "Armor", Items.IRON_CHESTPLATE),
            new TypeDef("HELMET", "Helmet", Items.IRON_HELMET),
            new TypeDef("CHESTPLATE", "Chestplate", Items.IRON_CHESTPLATE),

            // Page 2
            new TypeDef("LEGGINGS", "Leggings", Items.IRON_LEGGINGS),
            new TypeDef("BOOTS", "Boots", Items.IRON_BOOTS),
            new TypeDef("ACCESSORY", "Accessory", Items.TOTEM_OF_UNDYING),
            new TypeDef("RING", "Ring", Items.IRON_NUGGET),
            new TypeDef("AMULET", "Amulet", Items.HEART_OF_THE_SEA),
            new TypeDef("BELT", "Belt", Items.LEATHER),
            new TypeDef("CONSUMABLE", "Consumable", Items.POTION),
            new TypeDef("GEM_STONE", "Gem Stone", Items.EMERALD),
            new TypeDef("ORNAMENT", "Ornament", Items.GOLD_INGOT),
            new TypeDef("TOOL", "Tool", Items.IRON_PICKAXE),
            new TypeDef("PICKAXE", "Pickaxe", Items.IRON_PICKAXE),
            new TypeDef("SHOVEL", "Shovel", Items.IRON_SHOVEL),
            new TypeDef("HOE", "Hoe", Items.IRON_HOE),
            new TypeDef("SHEARS", "Shears", Items.SHEARS),
            new TypeDef("FISHING_ROD", "Fishing Rod", Items.FISHING_ROD),
            new TypeDef("BLOCK", "Block", Items.STONE),
            new TypeDef("MISCELLANEOUS", "Miscellaneous", Items.STICK),
            new TypeDef("CATALYST", "Catalyst", Items.NETHER_STAR),
            new TypeDef("ALL", "All Items", Items.COMPASS)
    );

    public static void open(ServerPlayer player) {
        open(player, 1);
    }

    public static void open(ServerPlayer player, int page) {
        if (!player.isCreative()) {
            player.sendSystemMessage(Component.literal("§c[MMOItems] Menu ini hanya dapat diakses dalam Mode Kreatif!"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
            return;
        }

        int maxPage = Math.max(1, (int) Math.ceil((double) TYPE_DEFS.size() / PAGE_SIZE));
        int clampedPage = Math.max(1, Math.min(page, maxPage));

        SimpleContainer container = new SimpleContainer(54);
        populateContainer(container, clampedPage, maxPage);

        final int curPage = clampedPage;
        // Always openMenu to guarantee ClientboundOpenScreenPacket is sent with the authentic title
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ReadOnlyChestMenu(id, inv, container, 6, (slotId, button, clickType, pl) -> handleClick(player, curPage, maxPage, slotId)),
                Component.literal("Type Browser")
        ));
    }

    private static void populateContainer(SimpleContainer container, int page, int maxPage) {
        // Clear all slots to pure AIR (1:1 with Spigot Bukkit.createInventory)
        for (int i = 0; i < 54; i++) {
            container.setItem(i, ItemStack.EMPTY);
        }

        // Fill active grid with "- No type -" placeholders (1:1 with TypeBrowser.class bytecode)
        ItemStack noType = new ItemStack(Items.STAINED_GLASS_PANE.gray());
        noType.set(DataComponents.CUSTOM_NAME, Component.literal("§c- No type -"));
        for (int slot : GRID_SLOTS) {
            container.setItem(slot, noType.copy());
        }

        // Place types into 21-slot grid
        int startIndex = (page - 1) * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int typeIdx = startIndex + i;
            if (typeIdx >= TYPE_DEFS.size()) break;

            TypeDef def = TYPE_DEFS.get(typeIdx);
            int slot = GRID_SLOTS[i];

            int itemCount = "ALL".equalsIgnoreCase(def.type())
                    ? MMOItemManager.getAllTemplates().size()
                    : MMOItemManager.getTemplatesByType(def.type()).size();

            ItemStack iconStack = new ItemStack(def.icon());
            iconStack.set(DataComponents.CUSTOM_NAME, Component.literal("§a" + def.displayName() + "§8 (click to browse)"));

            String countStr = (itemCount < 1) ? "§c§ono" : ("§6§o" + itemCount);
            String isAre = (itemCount == 1) ? "is" : "are";
            String s = (itemCount == 1) ? "" : "s";
            String loreLine = String.format("§7§oThere %s %s §7§oitem%s in this type.", isAre, countStr, s);

            iconStack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(loreLine))));
            container.setItem(slot, iconStack);
        }

        // Slot 18: Previous Page (Arrow)
        if (page > 1) {
            ItemStack prev = new ItemStack(Items.ARROW);
            prev.set(DataComponents.CUSTOM_NAME, Component.literal("§aPrevious Page"));
            container.setItem(18, prev);
        }

        // Slot 26: Next Page (Arrow)
        if (page < maxPage) {
            ItemStack next = new ItemStack(Items.ARROW);
            next.set(DataComponents.CUSTOM_NAME, Component.literal("§aNext Page"));
            container.setItem(26, next);
        }
    }

    private static void handleClick(ServerPlayer player, int page, int maxPage, int slotId) {
        // Slot 18: Previous Page
        if (slotId == 18 && page > 1) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.2f);
            open(player, page - 1);
            return;
        }

        // Slot 26: Next Page
        if (slotId == 26 && page < maxPage) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.2f);
            open(player, page + 1);
            return;
        }

        // Grid clicks
        int startIndex = (page - 1) * PAGE_SIZE;
        for (int i = 0; i < GRID_SLOTS.length; i++) {
            if (GRID_SLOTS[i] == slotId) {
                int typeIdx = startIndex + i;
                if (typeIdx >= TYPE_DEFS.size()) return;

                TypeDef def = TYPE_DEFS.get(typeIdx);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                MMOItemBrowserMenu.open(player, def.type(), 0);
                return;
            }
        }
    }
}
