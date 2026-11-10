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
import net.takasha.rpg.enchant.TakashaEnchantRegistry;
import net.takasha.rpg.mmo.MMOItemManager;
import net.takasha.rpg.mmo.MMOItemTemplate;

import java.util.*;

/**
 * MMOEnchantEditionMenu
 * Takasha RPG Enchantments Edition GUI (54 slots).
 * Mengelola Vanilla & AdvancedEnchantments pada item template.
 */
public class MMOEnchantEditionMenu {

    private static final int[] GRID_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public static void open(ServerPlayer player, MMOItemTemplate template) {
        if (!player.isCreative()) {
            player.sendSystemMessage(Component.literal("§c[Takasha-RPG] Menu ini hanya dapat diakses dalam Mode Kreatif!"));
            return;
        }

        SimpleContainer container = new SimpleContainer(54);
        populateContainer(container, template);

        if (player.containerMenu instanceof ReadOnlyChestMenu currentMenu
                && currentMenu.getContainer() instanceof SimpleContainer curCont
                && curCont.getContainerSize() == 54) {
            populateContainer(curCont, template);
            currentMenu.setSlotClickAction((slotId, button, clickType, pl) -> handleClick(player, template, slotId, button));
            currentMenu.broadcastChanges();
            return;
        }

        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ReadOnlyChestMenu(id, inv, container, 6, (slotId, button, clickType, pl) ->
                        handleClick(player, template, slotId, button)),
                Component.literal("§8Item Edition > §6Enchants (" + template.getId() + ")")
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
        ItemStack noEnch = new ItemStack(Items.STAINED_GLASS_PANE.gray());
        noEnch.set(DataComponents.CUSTOM_NAME, Component.literal("§7- No Enchantment -"));
        for (int slot : GRID_SLOTS) {
            container.setItem(slot, noEnch.copy());
        }

        // Populate existing AdvancedEnchantments
        List<Map.Entry<String, Integer>> list = new ArrayList<>(template.getAdvancedEnchants().entrySet());
        for (int i = 0; i < list.size() && i < GRID_SLOTS.length; i++) {
            var entry = list.get(i);
            String enchKey = entry.getKey();
            int level = entry.getValue();

            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            book.set(DataComponents.CUSTOM_NAME, Component.literal("§d§l" + enchKey + " §eLevel " + level));
            book.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§7AdvancedEnchantments Definition"),
                    Component.empty(),
                    Component.literal("§e► Left click: +1 Level"),
                    Component.literal("§e► Right click: -1 Level"),
                    Component.literal("§c► Shift + Right click: Remove enchantment")
            )));
            container.setItem(GRID_SLOTS[i], book);
        }

        // Slot 49: Back to Item Edition
        ItemStack back = new ItemStack(Items.BARRIER);
        back.set(DataComponents.CUSTOM_NAME, Component.literal("§c◄ Back"));
        container.setItem(49, back);

        // Slot 51: Add New Enchantment (Anvil)
        ItemStack addBtn = new ItemStack(Items.ANVIL);
        addBtn.set(DataComponents.CUSTOM_NAME, Component.literal("§a§lAdd Enchantment"));
        addBtn.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Click to register an enchantment to this item."),
                Component.empty(),
                Component.literal("§e► Click to input via chat (e.g. 'EXPLOSIVE:3')")
        )));
        container.setItem(51, addBtn);
    }

    private static void handleClick(ServerPlayer player, MMOItemTemplate template, int slotId, int button) {
        // Slot 49: Back
        if (slotId == 49) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
            MMOItemEditionMenu.open(player, template.getItemType(), template.getId());
            return;
        }

        // Slot 51: Add Enchantment via Chat
        if (slotId == 51) {
            List<String> promptLines = List.of(
                    "Write in the chat the enchantment and level you want to add.",
                    "Format: <enchant> [level] (e.g. 'lifesteal 3' or 'cleave:2')",
                    "Type 'cancel' to abort."
            );
            MMOChatEditionEngine.beginSession(
                    player,
                    "Item Edition",
                    "Add Enchantment",
                    promptLines,
                    MMOChatEditionEngine.SuggestionType.ENCHANTS,
                    null,
                    () -> open(player, template),
                    input -> {
                        String clean = input.trim().toUpperCase(Locale.ROOT);
                        String enchKey = clean;
                        int level = 1;
                        if (clean.contains(":")) {
                            String[] parts = clean.split(":", 2);
                            enchKey = parts[0].trim();
                            try {
                                level = Integer.parseInt(parts[1].trim());
                            } catch (NumberFormatException ignored) {}
                        } else if (clean.contains(" ")) {
                            String[] parts = clean.split("\\s+", 2);
                            enchKey = parts[0].trim();
                            try {
                                level = Integer.parseInt(parts[1].trim());
                            } catch (NumberFormatException ignored) {}
                        }
                        TakashaEnchantRegistry.CustomEnchantDef def = TakashaEnchantRegistry.get(enchKey);
                        net.takasha.rpg.economy.catalog.EnchantmentDetailHelper.EnchantDetail det = net.takasha.rpg.economy.catalog.EnchantmentDetailHelper.getDetail(enchKey);
                        int maxLvl = def != null ? def.maxLevel : (det != null ? det.maxLevel() : 10);
                        if (level > maxLvl) {
                            player.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eTakasha-RPG&8] &eLevel " + level + " melebihi batas maksimal (" + maxLvl + "). Disesuaikan menjadi level " + maxLvl + "."));
                            level = maxLvl;
                        }
                        template.getAdvancedEnchants().put(enchKey, Math.max(1, level));
                        net.takasha.rpg.mmo.MMOItemDiskManager.saveTemplate(template);
                        player.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eTakasha-RPG&8] &7Enchantment &e" + enchKey + " " + level + " &7successfully added."));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
                        open(player, template);
                        return true;
                    }
            );
            return;
        }

        // Existing enchants in grid
        List<Map.Entry<String, Integer>> list = new ArrayList<>(template.getAdvancedEnchants().entrySet());
        for (int i = 0; i < list.size() && i < GRID_SLOTS.length; i++) {
            if (GRID_SLOTS[i] == slotId) {
                var entry = list.get(i);
                String enchKey = entry.getKey();
                int currentLvl = entry.getValue();

                TakashaEnchantRegistry.CustomEnchantDef def = TakashaEnchantRegistry.get(enchKey);
                net.takasha.rpg.economy.catalog.EnchantmentDetailHelper.EnchantDetail det = net.takasha.rpg.economy.catalog.EnchantmentDetailHelper.getDetail(enchKey);
                int maxLvl = def != null ? def.maxLevel : (det != null ? det.maxLevel() : 10);

                if (button == 0) { // Left Click: +1 level
                    if (currentLvl >= maxLvl) {
                        player.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eTakasha-RPG&8] &c" + enchKey + " sudah mencapai batas maksimal level (Level " + maxLvl + ")!"));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 0.6f);
                        return;
                    }
                    template.getAdvancedEnchants().put(enchKey, currentLvl + 1);
                    net.takasha.rpg.mmo.MMOItemDiskManager.saveTemplate(template);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.3f);
                    open(player, template);
                    return;
                } else if (button == 1) { // Right Click: -1 level or remove
                    if (currentLvl > 1) {
                        template.getAdvancedEnchants().put(enchKey, currentLvl - 1);
                    } else {
                        template.getAdvancedEnchants().remove(enchKey);
                    }
                    net.takasha.rpg.mmo.MMOItemDiskManager.saveTemplate(template);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 0.9f);
                    open(player, template);
                    return;
                }
            }
        }
    }
}

