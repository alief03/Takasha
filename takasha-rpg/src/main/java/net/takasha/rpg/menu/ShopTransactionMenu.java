package net.takasha.rpg.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;
import net.takasha.rpg.economy.DynamicPricingEngine;
import net.takasha.rpg.economy.catalog.EnchantmentDetailHelper;
import net.takasha.rpg.economy.catalog.ShopCategory;
import net.takasha.rpg.economy.catalog.ShopItemEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * ShopTransactionMenu
 * Antarmuka kalkulator transaksi 54-slot (6 baris) otentik EconomyShopGUI.
 * Menggunakan In-Place Container Updates agar kursor mouse tidak melompat ke tengah
 * saat pemain mengubah kuantitas, beralih mode, atau mengeksekusi transaksi.
 */
public class ShopTransactionMenu {

    public static void open(ServerPlayer player, ShopCategory category, ShopItemEntry entry, int returnPage, int initialQuantity, boolean initialBuyMode) {
        Item targetItem = entry.resolveItem();
        int maxStack = targetItem.getDefaultMaxStackSize();
        int maxAllowed = Math.max(64, maxStack);

        int[] qtyHolder = new int[]{ Math.max(1, Math.min(initialQuantity, maxAllowed)) };
        boolean[] modeHolder = new boolean[]{ initialBuyMode };

        ItemStack filler = makeItem(Items.STAINED_GLASS_PANE.gray(), "§8", List.of());

        // In-place reuse jika sudah membuka ReadOnlyChestMenu 54-slot
        if (player.containerMenu instanceof ReadOnlyChestMenu currentMenu
                && currentMenu.getContainer() instanceof SimpleContainer curCont
                && curCont.getContainerSize() == 54) {

            for (int i = 0; i < 54; i++) {
                curCont.setItem(i, filler.copy());
            }
            renderState(curCont, player, category, entry, qtyHolder[0], modeHolder[0]);
            currentMenu.setSlotClickAction((slotId, button, clickType, pl) -> {
                handleTransactionClick(curCont, player, category, entry, returnPage, qtyHolder, modeHolder, maxAllowed, slotId);
            });
            currentMenu.broadcastChanges();
            return;
        }

        SimpleContainer container = new SimpleContainer(54);
        for (int i = 0; i < 54; i++) {
            container.setItem(i, filler.copy());
        }

        // Render kondisi awal
        renderState(container, player, category, entry, qtyHolder[0], modeHolder[0]);

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ReadOnlyChestMenu(id, inv, container, 6, (slotId, button, clickType, pl) -> {
                handleTransactionClick(container, player, category, entry, returnPage, qtyHolder, modeHolder, maxAllowed, slotId);
            }),
            Component.literal("§8Kalkulator: §f" + targetItem.getName(new ItemStack(targetItem)).getString())
        ));
    }

    private static void handleTransactionClick(SimpleContainer container, ServerPlayer player, ShopCategory category, ShopItemEntry entry, int returnPage, int[] qtyHolder, boolean[] modeHolder, int maxAllowed, int slotId) {
        int curQty = qtyHolder[0];
        boolean curMode = modeHolder[0];

        if (slotId == 10) {
            // -32
            qtyHolder[0] = Math.max(1, curQty - 32);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 0.9f);
            renderState(container, player, category, entry, qtyHolder[0], curMode);
            player.containerMenu.broadcastChanges();
        } else if (slotId == 11) {
            // -16
            qtyHolder[0] = Math.max(1, curQty - 16);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 0.95f);
            renderState(container, player, category, entry, qtyHolder[0], curMode);
            player.containerMenu.broadcastChanges();
        } else if (slotId == 12) {
            // -1
            qtyHolder[0] = Math.max(1, curQty - 1);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.0f);
            renderState(container, player, category, entry, qtyHolder[0], curMode);
            player.containerMenu.broadcastChanges();
        } else if (slotId == 14) {
            // +1
            qtyHolder[0] = Math.min(maxAllowed, curQty + 1);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.05f);
            renderState(container, player, category, entry, qtyHolder[0], curMode);
            player.containerMenu.broadcastChanges();
        } else if (slotId == 15) {
            // +16
            qtyHolder[0] = Math.min(maxAllowed, curQty + 16);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.1f);
            renderState(container, player, category, entry, qtyHolder[0], curMode);
            player.containerMenu.broadcastChanges();
        } else if (slotId == 16) {
            // +32
            qtyHolder[0] = Math.min(maxAllowed, curQty + 32);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.15f);
            renderState(container, player, category, entry, qtyHolder[0], curMode);
            player.containerMenu.broadcastChanges();
        } else if (slotId == 20) {
            // Preset 1x
            qtyHolder[0] = 1;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.2f);
            renderState(container, player, category, entry, qtyHolder[0], curMode);
            player.containerMenu.broadcastChanges();
        } else if (slotId == 22) {
            // Preset 16x
            qtyHolder[0] = Math.min(maxAllowed, 16);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.2f);
            renderState(container, player, category, entry, qtyHolder[0], curMode);
            player.containerMenu.broadcastChanges();
        } else if (slotId == 24) {
            // Preset 64x
            qtyHolder[0] = Math.min(maxAllowed, 64);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.2f);
            renderState(container, player, category, entry, qtyHolder[0], curMode);
            player.containerMenu.broadcastChanges();
        } else if (slotId == 31) {
            // Switch Mode
            modeHolder[0] = !curMode;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.3f);
            renderState(container, player, category, entry, qtyHolder[0], modeHolder[0]);
            player.containerMenu.broadcastChanges();
        } else if (slotId == 29 && entry.canBuy()) {
            // Eksekusi Beli in-place
            executePurchaseInPlace(container, player, category, entry, qtyHolder[0], modeHolder[0]);
            player.containerMenu.broadcastChanges();
        } else if (slotId == 33 && entry.canSell()) {
            // Eksekusi Jual in-place
            executeSaleInPlace(container, player, category, entry, qtyHolder[0], modeHolder[0]);
            player.containerMenu.broadcastChanges();
        } else if (slotId == 45) {
            // Kembali ke Katalog Kategori
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.0f);
            ShopCategoryMenu.openCategory(player, category, returnPage);
        } else if (slotId == 53) {
            // Tutup
            player.closeContainer();
        }
    }

    private static void renderState(SimpleContainer container, ServerPlayer player, ShopCategory category, ShopItemEntry entry, int safeQty, boolean isBuyMode) {
        Item targetItem = entry.resolveItem();
        int maxStack = targetItem.getDefaultMaxStackSize();

        String itemKey = BuiltInRegistries.ITEM.getKey(targetItem).toString();
        double unitBuy = entry.canBuy() ? DynamicPricingEngine.getDynamicBuyPrice(itemKey, entry.getBaseBuyPrice()) : -1;
        double unitSell = entry.canSell() ? DynamicPricingEngine.getDynamicSellPrice(itemKey, entry.getBaseSellPrice()) : -1;

        double totalBuyCost = unitBuy * safeQty;
        double totalSellRevenue = unitSell * safeQty;

        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);

        // Baris 0: Header Info
        container.setItem(4, makeItem(Items.PAPER, "§6§l✦ TRANSAKSI: " + ShopItemEntry.formatColor(entry.getDisplayName() != null ? entry.getDisplayName() : targetItem.getName(new ItemStack(targetItem)).getString()) + " ✦", List.of(
            Component.literal("§7Kategori: §e" + ShopItemEntry.formatColor(category.getTitle())),
            Component.literal("§7Atur kuantitas dan lakukan transaksi secara instan.")
        )));

        // Baris 1: Tombol kuantitas
        container.setItem(10, makeItem(Items.STAINED_GLASS_PANE.red(), "§c§l-32 Kuantitas", List.of(Component.literal("§7Kurangi 32 item."))));
        container.setItem(11, makeItem(Items.STAINED_GLASS_PANE.red(), "§c§l-16 Kuantitas", List.of(Component.literal("§7Kurangi 16 item."))));
        container.setItem(12, makeItem(Items.STAINED_GLASS_PANE.red(), "§c§l-1 Kuantitas", List.of(Component.literal("§7Kurangi 1 item."))));

        // Slot 13: Pratinjau Item dengan kuantitas
        ItemStack preview = entry.createItemStack(false, player.level().registryAccess());
        preview.setCount(Math.min(safeQty, maxStack));
        List<Component> previewLore = new ArrayList<>();
        if (entry.getEnchantments() != null && !entry.getEnchantments().isEmpty()) {
            for (String ench : entry.getEnchantments()) {
                previewLore.addAll(EnchantmentDetailHelper.buildDetailedTooltipLore(ench));
            }
        }
        previewLore.add(Component.literal("§7Kuantitas Transaksi: §e§l" + safeQty + "x"));
        if (entry.canBuy()) {
            previewLore.add(Component.literal("§7Harga Satuan Beli: §a$" + String.format("%,.2f", unitBuy)));
            previewLore.add(Component.literal("§7Total Biaya Beli: §a§l$" + String.format("%,.2f", totalBuyCost)));
        }
        if (entry.canSell()) {
            previewLore.add(Component.literal("§7Harga Satuan Jual: §c$" + String.format("%,.2f", unitSell)));
            previewLore.add(Component.literal("§7Total Hasil Jual: §c§l$" + String.format("%,.2f", totalSellRevenue)));
        }
        previewLore.add(Component.literal(""));
        previewLore.add(Component.literal("§7Mode Pilihan: " + (isBuyMode ? "§a§lPEMBELIAN" : "§c§lPENJUALAN")));
        preview.set(DataComponents.LORE, new ItemLore(previewLore));
        container.setItem(13, preview);

        container.setItem(14, makeItem(Items.STAINED_GLASS_PANE.lime(), "§a§l+1 Kuantitas", List.of(Component.literal("§7Tambah 1 item."))));
        container.setItem(15, makeItem(Items.STAINED_GLASS_PANE.lime(), "§a§l+16 Kuantitas", List.of(Component.literal("§7Tambah 16 item."))));
        container.setItem(16, makeItem(Items.STAINED_GLASS_PANE.lime(), "§a§l+32 Kuantitas", List.of(Component.literal("§7Tambah 32 item."))));

        // Baris 2: Preset Kuantitas
        container.setItem(20, makeItem(Items.PAPER, "§b§lPreset: 1x", List.of(Component.literal("§7Setel kuantitas tepat 1 item."))));
        container.setItem(22, makeItem(Items.PAPER, "§b§lPreset: 16x", List.of(Component.literal("§7Setel kuantitas tepat 16 item."))));
        container.setItem(24, makeItem(Items.PAPER, "§b§lPreset: 64x", List.of(Component.literal("§7Setel kuantitas tepat 64 item (1 Stack)."))));

        // Baris 3: Konfirmasi Beli / Switch Mode / Konfirmasi Jual
        if (entry.canBuy()) {
            boolean canAfford = data.getBalance() >= totalBuyCost;
            Item buyMat = canAfford ? Items.EMERALD_BLOCK : Items.BARRIER;
            container.setItem(29, makeItem(buyMat, "§a§l✦ KONFIRMASI BELI (" + safeQty + "x) ✦", List.of(
                Component.literal("§7Total Biaya: §a$" + String.format("%,.2f", totalBuyCost)),
                Component.literal("§7Saldo Anda: §f$" + String.format("%,.2f", data.getBalance())),
                Component.literal(canAfford ? "§a✓ Saldo mencukupi!" : "§c✕ Saldo tidak mencukupi!"),
                Component.literal("§eKlik untuk membeli sekarang")
            )));
        } else {
            container.setItem(29, makeItem(Items.STAINED_GLASS_PANE.gray(), "§7Beli Tidak Tersedia", List.of()));
        }

        container.setItem(31, makeItem(Items.COMPARATOR, "§e§l⇄ Beralih Mode", List.of(
            Component.literal("§7Beralih antara mode §aBeli §7dan §cJual§7."),
            Component.literal("§7Saat ini: " + (isBuyMode ? "§aBeli" : "§cJual"))
        )));

        if (entry.canSell()) {
            int inInventory = countInInventory(player, targetItem);
            boolean hasEnough = inInventory >= safeQty;
            Item sellMat = hasEnough ? Items.REDSTONE_BLOCK : Items.BARRIER;
            container.setItem(33, makeItem(sellMat, "§c§l✦ KONFIRMASI JUAL (" + safeQty + "x) ✦", List.of(
                Component.literal("§7Total Pendapatan: §c$" + String.format("%,.2f", totalSellRevenue)),
                Component.literal("§7Jumlah di Inventaris: §f" + inInventory + "x"),
                Component.literal(hasEnough ? "§a✓ Jumlah item mencukupi!" : "§c✕ Item di inventaris kurang!"),
                Component.literal("§eKlik untuk menjual sekarang")
            )));
        } else {
            container.setItem(33, makeItem(Items.STAINED_GLASS_PANE.gray(), "§7Jual Tidak Tersedia", List.of()));
        }

        // Baris 4: Informasi Pasar Dinamis
        container.setItem(40, makeItem(Items.COMPASS, "§6§l✦ Info Pasar Dinamis ✦", List.of(
            Component.literal("§7Item: §f" + itemKey),
            Component.literal("§7Harga Beli Dasar: §a$" + String.format("%,.2f", entry.getBaseBuyPrice())),
            Component.literal("§7Harga Jual Dasar: §c$" + String.format("%,.2f", entry.getBaseSellPrice())),
            Component.literal("§8Harga bergerak dinamis berdasarkan volume transaksi.")
        )));

        // Baris 5: Navigasi
        container.setItem(45, makeItem(Items.ARROW, "§c§l← Kembali ke Katalog", List.of(
            Component.literal("§7Kembali ke daftar item " + category.getTitle())
        )));

        container.setItem(49, makeItem(Items.EMERALD, "§6§l✦ Info Dompet ✦", List.of(
            Component.literal("§7Saldo Uang: §a$" + String.format("%,.2f", data.getBalance())),
            Component.literal("§7Gems: §b" + data.getGems() + " Gems")
        )));

        container.setItem(53, makeItem(Items.BARRIER, "§c§l✕ Tutup", List.of(
            Component.literal("§7Keluar dari menu transaksi.")
        )));
    }

    private static void executePurchaseInPlace(SimpleContainer container, ServerPlayer player, ShopCategory category, ShopItemEntry entry, int qty, boolean isBuyMode) {
        Item targetItem = entry.resolveItem();
        String itemKey = BuiltInRegistries.ITEM.getKey(targetItem).toString();
        double unitBuy = entry.canBuy() ? DynamicPricingEngine.getDynamicBuyPrice(itemKey, entry.getBaseBuyPrice()) : -1;
        double totalCost = unitBuy * qty;

        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        if (data.getBalance() < totalCost) {
            player.sendSystemMessage(Component.literal("§cSaldo tidak mencukupi untuk membeli " + qty + "x item ini!"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6f, 1.0f);
            return;
        }

        data.addBalance(-totalCost);
        DynamicPricingEngine.recordPurchase(itemKey, qty);
        net.takasha.rpg.quest.TakashaQuestManager.recordProgress(player, "shopPurchases", 1);

        // Berikan item ke inventaris pemain
        int remaining = qty;
        int maxStack = targetItem.getDefaultMaxStackSize();
        while (remaining > 0) {
            int toGive = Math.min(remaining, maxStack);
            ItemStack giveStack = entry.createItemStack(false, player.level().registryAccess());
            giveStack.setCount(toGive);
            if (!player.getInventory().add(giveStack)) {
                player.drop(giveStack, false);
            }
            remaining -= toGive;
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);

        player.sendSystemMessage(Component.literal(
            "§a§l[Shop] §fBerhasil membeli §e" + qty + "x " + targetItem.getName(new ItemStack(targetItem)).getString() +
            " §fseharga §a$" + String.format("%,.2f", totalCost) + " §7(Sisa Saldo: §a$" + String.format("%,.2f", data.getBalance()) + "§7)"
        ));

        // Render ulang state di container yang sama
        renderState(container, player, category, entry, qty, isBuyMode);
    }

    private static void executeSaleInPlace(SimpleContainer container, ServerPlayer player, ShopCategory category, ShopItemEntry entry, int qty, boolean isBuyMode) {
        Item targetItem = entry.resolveItem();
        int inInv = countInInventory(player, targetItem);
        if (inInv < qty) {
            player.sendSystemMessage(Component.literal("§cItem di inventaris kurang! Anda hanya memiliki " + inInv + "x."));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6f, 1.0f);
            return;
        }

        // Hapus dari inventaris
        int toRemove = qty;
        for (int i = 0; i < player.getInventory().getContainerSize() && toRemove > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == targetItem) {
                int shrinkCount = Math.min(toRemove, stack.getCount());
                stack.shrink(shrinkCount);
                toRemove -= shrinkCount;
            }
        }

        String itemKey = BuiltInRegistries.ITEM.getKey(targetItem).toString();
        double unitSell = entry.canSell() ? DynamicPricingEngine.getDynamicSellPrice(itemKey, entry.getBaseSellPrice()) : -1;
        double totalRevenue = unitSell * qty;

        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        data.addBalance(totalRevenue);
        DynamicPricingEngine.recordSale(itemKey, qty);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7f, 1.3f);

        player.sendSystemMessage(Component.literal(
            "§a§l[Shop] §fBerhasil menjual §e" + qty + "x " + targetItem.getName(new ItemStack(targetItem)).getString() +
            " §fseharga §a$" + String.format("%,.2f", totalRevenue) + " §7(Saldo: §a$" + String.format("%,.2f", data.getBalance()) + "§7)"
        ));

        // Render ulang state di container yang sama
        renderState(container, player, category, entry, qty, isBuyMode);
    }

    private static int countInInventory(ServerPlayer player, Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static ItemStack makeItem(Item item, String name, List<Component> lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        if (lore != null && !lore.isEmpty()) {
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }
        return stack;
    }
}
