package net.takasha.rpg.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;
import net.takasha.rpg.economy.DynamicPricingEngine;
import net.takasha.rpg.economy.catalog.ShopCategory;
import net.takasha.rpg.economy.catalog.ShopItemEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * ShopCategoryMenu
 * Antarmuka 54-slot paginasi untuk melihat dan bertransaksi item di dalam kategori toko.
 * Mendukung In-Place Container Updates agar kursor mouse pemain tidak ter-reset ke tengah
 * saat berpindah halaman atau melakukan quick-sell/sell 1x.
 */
public class ShopCategoryMenu {

    public static void openCategory(ServerPlayer player, ShopCategory category, int page) {
        if (category == null) {
            TakashaShopMenu.openShop(player);
            return;
        }

        int totalPages = category.getTotalPages();
        int safePage = Math.max(1, Math.min(page, totalPages));
        List<ShopItemEntry> pageItems = category.getPageItems(safePage);

        // Jika player sudah membuka ReadOnlyChestMenu 54-slot, lakukan In-Place Container Update!
        if (player.containerMenu instanceof ReadOnlyChestMenu currentMenu
                && currentMenu.getContainer() instanceof SimpleContainer existingContainer
                && existingContainer.getContainerSize() == 54) {

            populateContainer(existingContainer, player, category, safePage, totalPages, pageItems);
            currentMenu.setSlotClickAction((slotId, button, clickType, pl) -> {
                handleMenuClick(player, category, safePage, totalPages, pageItems, slotId, button, clickType);
            });
            currentMenu.broadcastChanges();
            return;
        }

        SimpleContainer container = new SimpleContainer(54);
        populateContainer(container, player, category, safePage, totalPages, pageItems);

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ReadOnlyChestMenu(id, inv, container, 6, (slotId, button, clickType, pl) -> {
                handleMenuClick(player, category, safePage, totalPages, pageItems, slotId, button, clickType);
            }),
            Component.literal(ShopItemEntry.formatColor(category.getTitle()) + " §8(Hal " + safePage + "/" + totalPages + ")")
        ));
    }

    private static void populateContainer(SimpleContainer container, ServerPlayer player, ShopCategory category, int safePage, int totalPages, List<ShopItemEntry> pageItems) {
        // Isi baris 1-5 dengan item dagangan
        for (int i = 0; i < 45; i++) {
            if (i < pageItems.size()) {
                ShopItemEntry entry = pageItems.get(i);
                container.setItem(i, entry.createItemStack(true, player.level().registryAccess()));
            } else {
                container.setItem(i, ItemStack.EMPTY);
            }
        }

        // Isi baris 6 (slot 45-53) dengan panel kontrol navigasi
        ItemStack grayGlass = makeGlass(Items.STAINED_GLASS_PANE.gray(), "§8");
        for (int i = 45; i < 54; i++) {
            container.setItem(i, grayGlass.copy());
        }

        // Slot 45: Kembali ke menu utama pasar
        container.setItem(45, makeControlItem(Items.ARROW, "§c§l← Menu Utama Pasar", List.of(
            Component.literal("§7Kembali ke daftar seluruh kategori pasar."),
            Component.literal("§eKlik untuk kembali")
        )));

        // Slot 48: Halaman sebelumnya
        if (safePage > 1) {
            container.setItem(48, makeControlItem(Items.SPECTRAL_ARROW, "§e§l← Halaman Sebelumnya (" + (safePage - 1) + ")", List.of(
                Component.literal("§7Klik untuk ke halaman sebelumnya.")
            )));
        } else {
            container.setItem(48, grayGlass.copy());
        }

        // Slot 49: Info Status & Petunjuk Transaksi Cepat
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        container.setItem(49, makeControlItem(Items.BOOK, "§6§l✦ " + ShopItemEntry.formatColor(category.getTitle()) + " ✦", List.of(
            Component.literal("§7Halaman: §e" + safePage + " §7dari §b" + totalPages),
            Component.literal("§7Saldo Anda: §a$" + String.format("%,.2f", data.getBalance())),
            Component.literal(""),
            Component.literal("§e[Klik Kiri Item] §7Buka kalkulator kuantitas"),
            Component.literal("§6[Klik Kanan Item] §7Jual langsung 1x"),
            Component.literal("§d[Shift + Klik Kanan Item] §7Quick Sell seluruh inventaris")
        )));

        // Slot 50: Halaman berikutnya
        if (safePage < totalPages) {
            container.setItem(50, makeControlItem(Items.SPECTRAL_ARROW, "§e§lHalaman Berikutnya (" + (safePage + 1) + ") →", List.of(
                Component.literal("§7Klik untuk ke halaman berikutnya.")
            )));
        } else {
            container.setItem(50, grayGlass.copy());
        }

        // Slot 53: Tutup antarmuka
        container.setItem(53, makeControlItem(Items.BARRIER, "§c§l✕ Tutup Pasar", List.of(
            Component.literal("§7Keluar dari menu pasar.")
        )));
    }

    private static void handleMenuClick(ServerPlayer player, ShopCategory category, int safePage, int totalPages, List<ShopItemEntry> pageItems, int slotId, int button, ContainerInput clickType) {
        if (slotId == 45) {
            // Kembali ke Menu Utama
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.0f);
            TakashaShopMenu.openShop(player);
        } else if (slotId == 48 && safePage > 1) {
            // Hal Sebelumnya
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.1f);
            openCategory(player, category, safePage - 1);
        } else if (slotId == 50 && safePage < totalPages) {
            // Hal Berikutnya
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.1f);
            openCategory(player, category, safePage + 1);
        } else if (slotId == 53) {
            // Tutup
            player.closeContainer();
        } else if (slotId >= 0 && slotId < 45 && slotId < pageItems.size()) {
            ShopItemEntry entry = pageItems.get(slotId);
            handleItemClick(player, category, entry, safePage, button, clickType);
        }
    }

    private static void handleItemClick(ServerPlayer player, ShopCategory category, ShopItemEntry entry, int page, int button, ContainerInput clickType) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        Item targetItem = entry.resolveItem();
        String itemKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(targetItem).toString();

        // 1. Shift + Klik Kanan: Quick Sell seluruh item sejenis dari inventaris
        if (button == 1 && clickType == ContainerInput.QUICK_MOVE) {
            if (!entry.canSell()) {
                player.sendSystemMessage(Component.literal("§cItem ini tidak dapat dijual ke pasar!"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6f, 1.0f);
                return;
            }

            int countFound = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() == targetItem) {
                    countFound += stack.getCount();
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }

            if (countFound <= 0) {
                player.sendSystemMessage(Component.literal("§cTidak ditemukan §e" + targetItem.getName(new ItemStack(targetItem)).getString() + " §cdi inventaris Anda!"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6f, 1.0f);
                return;
            }

            double unitPrice = DynamicPricingEngine.getDynamicSellPrice(itemKey, entry.getBaseSellPrice());
            double totalRevenue = unitPrice * countFound;

            data.addBalance(totalRevenue);
            DynamicPricingEngine.recordSale(itemKey, countFound);

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.4f);

            player.sendSystemMessage(Component.literal(
                "§a§l[QuickSell] §fBerhasil menjual §e" + countFound + "x " + targetItem.getName(new ItemStack(targetItem)).getString() +
                " §fseharga §a$" + String.format("%,.2f", totalRevenue) + " §7(Saldo: §a$" + String.format("%,.2f", data.getBalance()) + "§7)"
            ));

            openCategory(player, category, page);
            return;
        }

        // 2. Klik Kanan Biasa: Jual 1x langsung
        if (button == 1) {
            if (!entry.canSell()) {
                player.sendSystemMessage(Component.literal("§cItem ini tidak dapat dijual ke pasar!"));
                return;
            }

            boolean removed = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() == targetItem && stack.getCount() >= 1) {
                    stack.shrink(1);
                    removed = true;
                    break;
                }
            }

            if (!removed) {
                player.sendSystemMessage(Component.literal("§cAnda tidak memiliki item ini di inventaris untuk dijual!"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6f, 1.0f);
                return;
            }

            double unitPrice = DynamicPricingEngine.getDynamicSellPrice(itemKey, entry.getBaseSellPrice());
            data.addBalance(unitPrice);
            DynamicPricingEngine.recordSale(itemKey, 1);

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);

            player.sendSystemMessage(Component.literal(
                "§a§l[Shop] §fBerhasil menjual 1x §e" + targetItem.getName(new ItemStack(targetItem)).getString() +
                " §fseharga §a$" + String.format("%,.2f", unitPrice) + " §7(Saldo: §a$" + String.format("%,.2f", data.getBalance()) + "§7)"
            ));

            openCategory(player, category, page);
            return;
        }

        // 3. Klik Kiri Biasa: Buka Kalkulator Kuantitas (atau eksekusi perintah jika item Commands)
        if (button == 0) {
            if (entry.isCommandItem()) {
                handleCommandPurchase(player, entry);
                return;
            }

            // Buka ShopTransactionMenu
            ShopTransactionMenu.open(player, category, entry, page, 1, true);
        }
    }

    private static void handleCommandPurchase(ServerPlayer player, ShopItemEntry entry) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        double price = entry.getBaseBuyPrice();

        if (price < 0) {
            player.sendSystemMessage(Component.literal("§cItem perintah ini tidak dapat dibeli!"));
            return;
        }

        if (data.getBalance() < price) {
            player.sendSystemMessage(Component.literal("§cSaldo tidak cukup! Membutuhkan: §a$" + String.format("%,.2f", price)));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6f, 1.0f);
            return;
        }

        data.addBalance(-price);

        // Eksekusi seluruh buy-commands
        for (String cmd : entry.getBuyCommands()) {
            String executable = cmd.trim();
            if (executable.startsWith("/")) executable = executable.substring(1);
            executable = executable.replace("%player%", player.getScoreboardName());
            try {
                var server = player.level().getServer();
                if (server != null) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), executable);
                }
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("§cGagal menjalankan perintah: " + executable));
            }
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.5f);
        player.sendSystemMessage(Component.literal("§a§l[CommandShop] §fBerhasil membeli perintah seharga §a$" + String.format("%,.2f", price) + "§f!"));
    }

    private static ItemStack makeGlass(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static ItemStack makeControlItem(Item item, String name, List<Component> lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        if (lore != null && !lore.isEmpty()) {
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }
        return stack;
    }
}
