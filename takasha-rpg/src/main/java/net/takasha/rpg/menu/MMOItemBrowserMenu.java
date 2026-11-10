package net.takasha.rpg.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.takasha.rpg.mmo.MMOItemDiskManager;
import net.takasha.rpg.mmo.MMOItemManager;
import net.takasha.rpg.mmo.MMOItemTemplate;

import java.util.*;

/**
 * MMOItemBrowserMenu
 * 1:1 MMOItems Item Explorer GUI (54 slots) reconstructed from Spigot ItemBrowser.class bytecode & official documentation.
 *
 * Slots & Layout:
 *   Slot 18: Previous Page (Arrow)
 *   Slot 26: Next Page (Arrow)
 *   Slot 45: Download Default Resourcepack (Hopper, only if type == BLOCK)
 *   Slot 47: Delete Item / Cancel Deletion (Cauldron)
 *   Slot 49: Back to Type Browser (Arrow)
 *   Slot 51: Create New (Writable Book, hidden in delete mode)
 *   Active Grid 21 slots: [10..16, 19..25, 28..34], placeholder: GRAY_STAINED_GLASS_PANE ("§c- No Item -")
 *   Background / Borders: Pure AIR (ItemStack.EMPTY)
 */
public class MMOItemBrowserMenu {

    private static final int PAGE_SIZE = 21;
    private static final int[] GRID_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    // Tracks which players currently have Delete Mode active
    private static final Set<UUID> DELETE_MODE_PLAYERS = Collections.synchronizedSet(new HashSet<>());

    public static void open(ServerPlayer player, String type, int page) {
        if (!player.isCreative()) {
            player.sendSystemMessage(Component.literal("§c[MMOItems] Menu ini hanya dapat diakses dalam Mode Kreatif!"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
            return;
        }

        List<MMOItemTemplate> items = MMOItemManager.getTemplatesByType(type);
        int maxPage = Math.max(0, (items.size() - 1) / PAGE_SIZE);
        int clampedPage = Math.max(0, Math.min(page, maxPage));

        SimpleContainer container = new SimpleContainer(54);
        populateContainer(container, player, type, items, clampedPage, maxPage);

        boolean isDel = DELETE_MODE_PLAYERS.contains(player.getUUID());
        String title = (isDel ? "Delete Mode: " : "Item Explorer: ") + type.toUpperCase(Locale.ROOT);

        // Always call player.openMenu to force ClientboundOpenScreenPacket with authentic title
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ReadOnlyChestMenu(id, inv, container, 6, (slotId, button, clickType, pl) ->
                        handleClick(player, type, items, clampedPage, maxPage, slotId, button, clickType)),
                Component.literal(title)
        ));
    }

    private static void populateContainer(SimpleContainer container, ServerPlayer player, String type, List<MMOItemTemplate> items, int page, int maxPage) {
        // Clear all slots to pure AIR (1:1 with Spigot Bukkit.createInventory)
        for (int i = 0; i < 54; i++) {
            container.setItem(i, ItemStack.EMPTY);
        }

        // Fill grid with placeholders (- No Item -)
        ItemStack noItem = new ItemStack(Items.STAINED_GLASS_PANE.gray());
        noItem.set(DataComponents.CUSTOM_NAME, Component.literal("§c- No Item -"));
        for (int slot : GRID_SLOTS) {
            container.setItem(slot, noItem.copy());
        }

        boolean isDeleteMode = DELETE_MODE_PLAYERS.contains(player.getUUID());
        int startIndex = page * PAGE_SIZE;

        for (int i = 0; i < PAGE_SIZE; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex >= items.size()) break;

            MMOItemTemplate template = items.get(itemIndex);
            ItemStack generated = MMOItemManager.generateItemStack(template.getItemType(), template.getId(), 1);

            ItemLore existingLore = generated.get(DataComponents.LORE);
            List<Component> enriched = new ArrayList<>();
            if (existingLore != null) {
                enriched.addAll(existingLore.lines());
            }

            enriched.add(Component.empty());
            if (isDeleteMode) {
                Component currentName = generated.get(DataComponents.CUSTOM_NAME);
                String rawName = (currentName != null) ? currentName.getString() : template.getName();
                generated.set(DataComponents.CUSTOM_NAME, Component.literal("§cDELETE: " + rawName));
                enriched.add(Component.literal("§c► CLICK TO DELETE ◄"));
            } else {
                enriched.add(net.takasha.rpg.mmo.TakashaColorParser.parseLoreLine("&e► Left click to obtain this item."));
                enriched.add(net.takasha.rpg.mmo.TakashaColorParser.parseLoreLine("&e► Right click to edit this item (Base Item & Stats)."));
            }

            generated.set(DataComponents.LORE, new ItemLore(enriched));
            container.setItem(GRID_SLOTS[i], generated);
        }

        // Slot 18: Previous Page (1:1 with ItemBrowser.class Slot 18)
        if (page > 0) {
            ItemStack prev = new ItemStack(Items.ARROW);
            prev.set(DataComponents.CUSTOM_NAME, Component.literal("§aPrevious Page"));
            container.setItem(18, prev);
        }

        // Slot 26: Next Page (1:1 with ItemBrowser.class Slot 26)
        if (page < maxPage) {
            ItemStack next = new ItemStack(Items.ARROW);
            next.set(DataComponents.CUSTOM_NAME, Component.literal("§aNext Page"));
            container.setItem(26, next);
        }

        // Slot 45: Download Default Resourcepack (1:1 with ItemBrowser.class Slot 45)
        if ("BLOCK".equalsIgnoreCase(type)) {
            ItemStack packBtn = new ItemStack(Items.HOPPER);
            packBtn.set(DataComponents.CUSTOM_NAME, Component.literal("§aDownload Default Resourcepack"));
            packBtn.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§dOnly seeing stone blocks?"),
                    Component.empty(),
                    Component.literal("§cBy downloading the default resourcepack you can"),
                    Component.literal("§cedit the blocks however you want."),
                    Component.literal("§cYou will still have to add it to your server!")
            )));
            container.setItem(45, packBtn);
        }

        // Slot 47: Delete Mode Toggle (1:1 with ItemBrowser.class: CAULDRON)
        ItemStack deleteBtn = new ItemStack(Items.CAULDRON);
        deleteBtn.set(DataComponents.CUSTOM_NAME, Component.literal(isDeleteMode ? "§cCancel Deletion" : "§cDelete Item"));
        container.setItem(47, deleteBtn);

        // Slot 49: Back to Type Browser (1:1 with ItemBrowser.class: ARROW)
        ItemStack back = new ItemStack(Items.ARROW);
        back.set(DataComponents.CUSTOM_NAME, Component.literal("§a◄ Back"));
        back.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Click to return to the type browser.")
        )));
        container.setItem(49, back);

        // Slot 51: Create a new Item (1:1 with ItemBrowser.class: WRITABLE_BOOK)
        if (!isDeleteMode) {
            ItemStack createBtn = new ItemStack(Items.WRITABLE_BOOK);
            createBtn.set(DataComponents.CUSTOM_NAME, Component.literal("§aCreate New"));
            container.setItem(51, createBtn);
        }
    }

    private static void handleClick(ServerPlayer player, String type, List<MMOItemTemplate> items,
                                     int page, int maxPage, int slotId, int button, ContainerInput clickType) {
        boolean isDeleteMode = DELETE_MODE_PLAYERS.contains(player.getUUID());

        // Slot 18: Previous Page
        if (slotId == 18 && page > 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.2f);
            open(player, type, page - 1);
            return;
        }

        // Slot 26: Next Page
        if (slotId == 26 && page < maxPage) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.2f);
            open(player, type, page + 1);
            return;
        }

        // Slot 45: Resourcepack link
        if (slotId == 45 && "BLOCK".equalsIgnoreCase(type)) {
            player.sendSystemMessage(Component.literal("§a[MMOItems] §fDownload resourcepack blok: §bhttps://www.dropbox.com/s/90w9pvdbfeyxu94/MICustomBlockPack.zip?dl=1"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.0f);
            return;
        }

        // Slot 47: Toggle Delete Mode
        if (slotId == 47) {
            if (isDeleteMode) {
                DELETE_MODE_PLAYERS.remove(player.getUUID());
                player.sendSystemMessage(Component.literal("§a[MMOItems] Delete Mode deactivated."));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.2f);
            } else {
                DELETE_MODE_PLAYERS.add(player.getUUID());
                player.sendSystemMessage(Component.literal("§c[MMOItems] Delete Mode activated! Clicking an item will delete its template."));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.7f, 0.8f);
            }
            open(player, type, page);
            return;
        }

        // Slot 49: Back to Type Browser
        if (slotId == 49) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
            MMOTypeBrowserMenu.open(player);
            return;
        }

        // Slot 51: Create New Item (Anvil / Chat Input)
        if (slotId == 51 && !isDeleteMode) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
            player.closeContainer();

            final String creationType = "ALL".equalsIgnoreCase(type) ? "SWORD" : type;
            List<String> promptLines = List.of(
                    "Write in the chat the text you want (ID).",
                    "Type 'cancel' to abort editing."
            );
            MMOChatEditionEngine.beginSession(
                    player,
                    "Create MMOItem",
                    "ID",
                    promptLines,
                    MMOChatEditionEngine.SuggestionType.ID,
                    List.of("[id]", "[item_id]", "NEW_ITEM"),
                    () -> open(player, type, page),
                    newId -> {
                        String cleanId = newId.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
                        if (cleanId.isEmpty()) {
                            player.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&c[MMOItems] ID tidak boleh kosong!"));
                            open(player, type, page);
                            return true;
                        }

                        if (MMOItemManager.getTemplate(creationType, cleanId) != null) {
                            player.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &cThere is already an item called " + cleanId + "."));
                            open(player, type, page);
                            return true;
                        }

                        // MMOItems 1:1 Parity - Pure Empty Item Creation (CreateCommandTreeNode.java L229-267)
                        // Only <id>.base.material is set. Zero default stats, zero tier, zero fake damage!
                        MMOItemTemplate newTemplate = new MMOItemTemplate(cleanId, creationType);
                        newTemplate.setMaterial(getDefaultMaterialForType(creationType));

                        MMOItemManager.registerTemplate(newTemplate);
                        MMOItemDiskManager.saveTemplate(newTemplate);

                        player.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &aYou successfully created " + cleanId + "!"));
                        MMOItemEditionMenu.open(player, creationType, cleanId);
                        return true;
                    }
            );
            return;
        }

        // Grid clicks
        int startIndex = page * PAGE_SIZE;
        for (int i = 0; i < GRID_SLOTS.length; i++) {
            if (GRID_SLOTS[i] == slotId) {
                int itemIndex = startIndex + i;
                if (itemIndex >= items.size()) return;

                MMOItemTemplate template = items.get(itemIndex);

                if (isDeleteMode) {
                    // Delete item permanently
                    MMOItemManager.deleteTemplate(template.getItemType(), template.getId());
                    MMOItemDiskManager.deleteTemplateDisk(template.getItemType(), template.getId());

                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_BREAK, SoundSource.PLAYERS, 0.8f, 1.0f);
                    player.sendSystemMessage(Component.literal("§c[MMOItems] Template §e" + template.getId() + " §cpermanently deleted."));

                    // Refresh page
                    open(player, type, page);
                    return;
                }

                // Normal Mode Clicks:
                // Left Click (button == 0) -> Obtain item
                // Right Click (button == 1) -> Edit item (1:1 with MMOItems Spigot bytecode)
                if (button == 1) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                    MMOItemEditionMenu.open(player, template.getItemType(), template.getId(), 1);
                } else {
                    ItemStack stack = MMOItemManager.generateItemStack(template.getItemType(), template.getId(), 1);
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.7f, 1.2f);
                    player.sendSystemMessage(Component.literal("§a[MMOItems] §fMenerima §6" + template.getName()));
                }
                return;
            }
        }
    }

    /**
     * Authentic 1:1 default material mapping for all 40 MMOItems types (default/item-types.yml).
     */
    public static String getDefaultMaterialForType(String type) {
        if (type == null) return "IRON_SWORD";
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "SWORD" -> "IRON_SWORD";
            case "DAGGER" -> "STONE_SWORD";
            case "SPEAR" -> "TRIDENT";
            case "HAMMER" -> "IRON_AXE";
            case "GAUNTLET" -> "IRON_SHOVEL";
            case "WHIP" -> "LEAD";
            case "STAFF" -> "DIAMOND_HOE";
            case "BOW" -> "BOW";
            case "CROSSBOW" -> "CROSSBOW";
            case "MUSKET" -> "IRON_HORSE_ARMOR";
            case "LUTE" -> "NAME_TAG";
            case "CATALYST" -> "PRISMARINE_SHARD";
            case "OFF_CATALYST", "MAIN_CATALYST" -> "PRISMARINE_CRYSTALS";
            case "ORNAMENT" -> "GOLD_NUGGET";
            case "ACCESSORY" -> "DIAMOND";
            case "ARMOR" -> "IRON_CHESTPLATE";
            case "TOOL" -> "IRON_PICKAXE";
            case "CONSUMABLE" -> "APPLE";
            case "MISCELLANEOUS" -> "WATER_BUCKET";
            case "SKIN" -> "LEATHER";
            case "GEM_STONE" -> "EMERALD";
            case "BLOCK" -> "STONE";
            case "GREATSWORD" -> "DIAMOND_SWORD";
            case "LONG_SWORD" -> "STONE_SWORD";
            case "KATANA" -> "IRON_SWORD";
            case "THRUSTING_SWORD" -> "GOLDEN_SWORD";
            case "AXE" -> "STONE_AXE";
            case "GREATAXE" -> "DIAMOND_AXE";
            case "HALBERD" -> "IRON_AXE";
            case "LANCE" -> "STICK";
            case "GREATHAMMER" -> "DIAMOND_AXE";
            case "GREATSTAFF" -> "DIAMOND_HOE";
            case "STAVE" -> "IRON_HOE";
            case "TOME" -> "BOOK";
            case "TALISMAN" -> "TOTEM_OF_UNDYING";
            case "WAND" -> "STICK";
            case "GREATBOW" -> "BOW";
            case "SHIELD" -> "SHIELD";
            case "MATERIAL" -> "PAPER";
            default -> "IRON_SWORD";
        };
    }
}
