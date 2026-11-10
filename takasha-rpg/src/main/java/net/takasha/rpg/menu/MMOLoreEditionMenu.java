package net.takasha.rpg.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.takasha.rpg.mmo.MMOItemDiskManager;
import net.takasha.rpg.mmo.MMOItemTemplate;

import java.util.List;

/**
 * MMOLoreEditionMenu
 * Sub-menu GUI 54-slot visual untuk melihat, menambah, mengedit,
 * dan menghapus baris lore item secara interaktif.
 */
public class MMOLoreEditionMenu {

    public static void open(ServerPlayer player, MMOItemTemplate template, int parentPage) {
        SimpleContainer container = new SimpleContainer(54);
        List<String> loreLines = template.getLore();

        // Slots 0..44: Lore lines
        for (int i = 0; i < Math.min(45, loreLines.size()); i++) {
            ItemStack lineItem = new ItemStack(Items.PAPER);
            lineItem.set(DataComponents.CUSTOM_NAME, Component.literal("§eBaris #" + (i + 1)));
            lineItem.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§7Isi: §f" + loreLines.get(i)),
                    Component.empty(),
                    Component.literal("§e► Klik Kiri untuk Mengedit baris ini"),
                    Component.literal("§c► Klik Kanan untuk Menghapus baris ini")
            )));
            container.setItem(i, lineItem);
        }

        // Slot 47: Tambah Baris Baru
        ItemStack addLine = new ItemStack(Items.WRITABLE_BOOK);
        addLine.set(DataComponents.CUSTOM_NAME, Component.literal("§a§l[➕ Tambah Baris Lore]"));
        addLine.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Klik untuk mengetik baris baru di chat.")
        )));
        container.setItem(47, addLine);

        // Slot 49: Back Button
        ItemStack back = new ItemStack(Items.BARRIER);
        back.set(DataComponents.CUSTOM_NAME, Component.literal("§cBack"));
        container.setItem(49, back);

        // Slot 51: Hapus Semua Lore
        ItemStack clear = new ItemStack(Items.CAULDRON);
        clear.set(DataComponents.CUSTOM_NAME, Component.literal("§c§l[🗑 Bersihkan Semua Lore]"));
        container.setItem(51, clear);

        Component title = Component.literal("Lore Edition: " + template.getId());
        ReadOnlyChestMenu.openOrUpdate(player, title, container, 6, (slotId, button, clickType, pl) -> {
            if (slotId == 49) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
                MMOItemEditionMenu.open(player, template.getItemType(), template.getId(), parentPage);
                return;
            }

            if (slotId == 51) {
                template.getLore().clear();
                MMOItemDiskManager.saveTemplate(template);
                player.sendSystemMessage(Component.literal("§8[§eMMOItems§8] §7Successfully removed Lore§7."));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
                open(player, template, parentPage);
                return;
            }

            if (slotId == 47) {
                List<String> promptLines = List.of(
                        "Write in the chat the line you want to add.",
                        "Example: &e&7Forged in the heart of Mount Doom.",
                        "Type 'cancel' to abort editing."
                );
                MMOChatEditionEngine.beginSession(player, "Lore Edition", "Add Line", promptLines,
                        MMOChatEditionEngine.SuggestionType.LORE, null,
                        () -> open(player, template, parentPage),
                        input -> {
                            template.getLore().add(input);
                            MMOItemDiskManager.saveTemplate(template);
                            player.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Lore line added: ").copy()
                                    .append(net.takasha.rpg.mmo.TakashaColorParser.parseLoreLine(input)));
                            open(player, template, parentPage);
                            return true;
                        });
                return;
            }

            if (slotId >= 0 && slotId < loreLines.size()) {
                final int lineIndex = slotId;
                if (button == 1) { // Right Click: Delete line
                    template.getLore().remove(lineIndex);
                    MMOItemDiskManager.saveTemplate(template);
                    player.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Removed line #" + (lineIndex + 1)));
                    open(player, template, parentPage);
                } else { // Left Click: Edit line
                    List<String> promptLines = List.of(
                            "Editing line #" + (lineIndex + 1) + " (Current: &f" + loreLines.get(lineIndex) + "&7)",
                            "Write in the chat the new text.",
                            "Type 'cancel' to abort editing."
                    );
                    MMOChatEditionEngine.beginSession(player, "Lore Edition", "Edit Line #" + (lineIndex + 1), promptLines,
                            MMOChatEditionEngine.SuggestionType.LORE, null,
                            () -> open(player, template, parentPage),
                            input -> {
                                template.getLore().set(lineIndex, input);
                                MMOItemDiskManager.saveTemplate(template);
                                player.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&8[&eMMOItems&8] &7Line #" + (lineIndex + 1) + " updated!"));
                                open(player, template, parentPage);
                                return true;
                            });
                }
            }
        });
    }
}
