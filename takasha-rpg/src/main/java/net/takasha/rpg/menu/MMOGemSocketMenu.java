package net.takasha.rpg.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.takasha.rpg.mmo.MMOItemDiskManager;
import net.takasha.rpg.mmo.MMOItemManager;
import net.takasha.rpg.mmo.MMOItemTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * MMOGemSocketMenu
 * 1:1 MMOItems Gem Sockets Edition GUI (54 slots).
 * Allows configuring gem socket types (Red, Blue, Yellow, Diamond, etc.) on an item template.
 */
public class MMOGemSocketMenu {

    private static final int[] GRID_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public static void open(ServerPlayer player, MMOItemTemplate template, int returnPage) {
        if (!player.isCreative()) {
            player.sendSystemMessage(Component.literal("§c[MMOItems] Menu ini hanya dapat diakses dalam Mode Kreatif!"));
            return;
        }

        SimpleContainer container = new SimpleContainer(54);
        populateContainer(container, template);

        if (player.containerMenu instanceof ReadOnlyChestMenu currentMenu
                && currentMenu.getContainer() instanceof SimpleContainer curCont
                && curCont.getContainerSize() == 54) {
            populateContainer(curCont, template);
            currentMenu.setSlotClickAction((slotId, button, clickType, pl) -> handleClick(player, template, returnPage, slotId, button));
            currentMenu.broadcastChanges();
            return;
        }

        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ReadOnlyChestMenu(id, inv, container, 6, (slotId, button, clickType, pl) ->
                        handleClick(player, template, returnPage, slotId, button)),
                Component.literal("§8Item Edition > §6Gem Sockets (" + template.getId() + ")")
        ));
    }

    private static void populateContainer(SimpleContainer container, MMOItemTemplate template) {
        ItemStack filler = new ItemStack(Items.STAINED_GLASS_PANE.gray());
        filler.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        for (int i = 0; i < 54; i++) {
            container.setItem(i, filler.copy());
        }

        // Slot 4: Live Item Preview
        ItemStack preview = MMOItemManager.generateItemStack(template.getItemType(), template.getId(), 1);
        container.setItem(4, preview);

        // Placeholders in grid
        ItemStack noSocket = new ItemStack(Items.STAINED_GLASS_PANE.gray());
        noSocket.set(DataComponents.CUSTOM_NAME, Component.literal("§7- No Gem Socket -"));
        for (int slot : GRID_SLOTS) {
            container.setItem(slot, noSocket.copy());
        }

        // Populate existing gem sockets
        List<String> list = template.getGemSockets();
        for (int i = 0; i < list.size() && i < GRID_SLOTS.length; i++) {
            String socketColor = list.get(i);
            ItemStack gem = new ItemStack(Items.EMERALD);
            gem.set(DataComponents.CUSTOM_NAME, Component.literal("§a§l" + socketColor + " Socket"));
            gem.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§7Socket Type: §e" + socketColor),
                    Component.empty(),
                    Component.literal("§c► Right click to remove this socket")
            )));
            container.setItem(GRID_SLOTS[i], gem);
        }

        // Slot 49: Back to Item Edition
        ItemStack back = new ItemStack(Items.BARRIER);
        back.set(DataComponents.CUSTOM_NAME, Component.literal("§c◄ Back"));
        container.setItem(49, back);

        // Slot 51: Add New Socket (Anvil)
        ItemStack addBtn = new ItemStack(Items.ANVIL);
        addBtn.set(DataComponents.CUSTOM_NAME, Component.literal("§a§lAdd Gem Socket"));
        addBtn.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Click to add a gem socket color or category."),
                Component.empty(),
                Component.literal("§e► Click to input socket color via chat (e.g. 'Red', 'Blue', 'Diamond')")
        )));
        container.setItem(51, addBtn);
    }

    private static void handleClick(ServerPlayer player, MMOItemTemplate template, int returnPage, int slotId, int button) {
        // Slot 49: Back
        if (slotId == 49) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
            MMOItemEditionMenu.open(player, template.getItemType(), template.getId(), returnPage);
            return;
        }

        // Slot 51: Add Socket via Chat with dynamic GEM_STONE suggestions
        if (slotId == 51) {
            player.closeContainer();

            // Dynamic Scan of all existing GEM_STONE templates
            List<MMOItemTemplate> gems = MMOItemManager.getTemplatesByType("GEM_STONE");
            Set<String> discoveredColors = new LinkedHashSet<>();
            Map<String, List<String>> colorToGems = new LinkedHashMap<>();

            for (MMOItemTemplate gem : gems) {
                if (gem.hasGemColor()) {
                    String col = gem.getGemColor().trim();
                    discoveredColors.add(col);
                    colorToGems.computeIfAbsent(col.toUpperCase(Locale.ROOT), k -> new ArrayList<>())
                            .add(gem.getName() != null && !gem.getName().isEmpty() ? gem.getName() : gem.getId());
                }
            }

            // Standard MMOItems socket colors
            List<String> standardColors = List.of("Red", "Blue", "Yellow", "Green", "Prismatic", "Any");
            for (String sc : standardColors) {
                discoveredColors.add(sc);
            }

            List<String> suggestions = new ArrayList<>(discoveredColors);
            List<String> promptLines = new ArrayList<>();
            promptLines.add("Editing: &eAdd Gem Socket &7(Template: &f" + template.getId() + "&7)");
            promptLines.add("Write in the chat the socket color or type (e.g. 'Red', 'Blue', 'Yellow', 'Prismatic').");
            if (!colorToGems.isEmpty()) {
                promptLines.add("&6Gemstone yang terdaftar di server: &7" + String.join(", ", colorToGems.keySet()));
            }

            MMOChatEditionEngine.beginSession(
                    player,
                    "Item Edition",
                    "Add Gem Socket",
                    promptLines,
                    MMOChatEditionEngine.SuggestionType.GEM_SOCKETS,
                    suggestions,
                    () -> open(player, template, returnPage),
                    input -> {
                        String clean = input.trim();
                        if (clean.isEmpty()) {
                            open(player, template, returnPage);
                            return true;
                        }
                        template.getGemSockets().add(clean);
                        MMOItemDiskManager.saveTemplate(template);
                        player.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &aGem socket &6" + clean + " &aberhasil ditambahkan!"));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
                        open(player, template, returnPage);
                        return true;
                    }
            );
            return;
        }

        // Existing sockets in grid
        List<String> list = template.getGemSockets();
        for (int i = 0; i < list.size() && i < GRID_SLOTS.length; i++) {
            if (GRID_SLOTS[i] == slotId) {
                if (button == 1) { // Right Click: Remove
                    String removed = list.remove(i);
                    MMOItemDiskManager.saveTemplate(template);
                    player.sendSystemMessage(Component.literal("§c[MMOItems] Gem socket §e" + removed + " §ctelah dihapus dari template."));
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 0.7f, 1.2f);
                    open(player, template, returnPage);
                    return;
                }
            }
        }
    }
}

