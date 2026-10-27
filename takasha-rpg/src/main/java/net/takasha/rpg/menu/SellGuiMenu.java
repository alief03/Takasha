package net.takasha.rpg.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;
import net.takasha.rpg.economy.DynamicPricingEngine;
import net.takasha.rpg.economy.catalog.ShopCatalog;
import net.takasha.rpg.economy.catalog.ShopItemEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * SellGuiMenu
 * Wadah peti 54 slot berbasis EconomyShopGUI 6.3.1.
 * - Slot 0-44: Area meletakkan barang dagangan.
 * - Slot 45: Ringkasan total pendapatan pasar.
 * - Slot 49: Batal & Kembalikan seluruh barang.
 * - Slot 53: Konfirmasi Penjualan instan.
 */
public class SellGuiMenu extends ChestMenu {

    public static void open(ServerPlayer player) {
        SimpleContainer container = new SimpleContainer(54);
        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new SellGuiMenu(id, inv, container),
            Component.literal("§a§lPeti Jual Otomatis (EconomyShopGUI)")
        ));
    }

    private final SimpleContainer sellContainer;
    private boolean processed = false;

    public SellGuiMenu(int containerId, Inventory playerInventory, SimpleContainer container) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6);
        this.sellContainer = container;
        setupControlBar();
    }

    private void setupControlBar() {
        ItemStack filler = makeItem(Items.STAINED_GLASS_PANE.gray(), "§8", List.of());
        for (int i = 46; i <= 52; i++) {
            if (i == 49) continue;
            sellContainer.setItem(i, filler.copy());
        }

        updateSummary();

        // Slot 49: Cancel & Return
        sellContainer.setItem(49, makeItem(Items.STAINED_GLASS_PANE.red(), "§c§l✕ BATALKAN & KEMBALIKAN", List.of(
            Component.literal("§7Klik untuk membatalkan transaksi"),
            Component.literal("§7dan mengembalikan semua barang ke inventarismu.")
        )));

        // Slot 53: Confirm Sell
        sellContainer.setItem(53, makeItem(Items.STAINED_GLASS_PANE.lime(), "§a§l✔ KONFIRMASI PENJUALAN", List.of(
            Component.literal("§7Klik untuk menjual seluruh barang yang"),
            Component.literal("§7memiliki nilai jual pasar.")
        )));
    }

    public void updateSummary() {
        double totalEarnings = 0.0;
        int totalItems = 0;

        for (int i = 0; i < 45; i++) {
            ItemStack stack = sellContainer.getItem(i);
            if (stack.isEmpty()) continue;
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            ShopItemEntry entry = ShopCatalog.findItemEntry(stack);
            double fallbackBase = (entry != null && entry.canSell()) ? entry.getBaseSellPrice() : -1.0;
            double unitPrice = DynamicPricingEngine.getDynamicSellPrice(itemId, fallbackBase);
            if (unitPrice > 0) {
                totalEarnings += unitPrice * stack.getCount();
                totalItems += stack.getCount();
            }
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Total Item Layak Jual: §e" + totalItems + " buah"));
        lore.add(Component.literal("§7Estimasi Saldo Diterima: §a§l$" + String.format("%.2f", totalEarnings)));
        lore.add(Component.literal("§8§m------------------------"));
        lore.add(Component.literal("§7Taruh barang di slot 1-45, lalu"));
        lore.add(Component.literal("§7tekan tombol hijau untuk konfirmasi."));

        sellContainer.setItem(45, makeItem(Items.EMERALD, "§6§l✦ ESTIMASI HASIL JUAL ✦", lore));
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId >= 45 && slotId <= 53) {
            if (player instanceof ServerPlayer sp) {
                if (slotId == 49) { // Cancel
                    sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
                    cancelAndReturn(sp);
                    sp.closeContainer();
                    return;
                } else if (slotId == 53) { // Confirm Sell
                    confirmSell(sp);
                    sp.closeContainer();
                    return;
                }
            }
            return; // Block taking UI control items
        }

        super.clicked(slotId, button, clickType, player);
        updateSummary();
    }

    private void confirmSell(ServerPlayer sp) {
        if (processed) return;
        processed = true;

        double totalEarnings = 0.0;
        int totalItems = 0;
        List<ItemStack> unsold = new ArrayList<>();

        for (int i = 0; i < 45; i++) {
            ItemStack stack = sellContainer.getItem(i);
            if (stack.isEmpty()) continue;

            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            ShopItemEntry entry = ShopCatalog.findItemEntry(stack);
            double fallbackBase = (entry != null && entry.canSell()) ? entry.getBaseSellPrice() : -1.0;
            double unitPrice = DynamicPricingEngine.getDynamicSellPrice(itemId, fallbackBase);
            if (unitPrice > 0) {
                totalEarnings += unitPrice * stack.getCount();
                totalItems += stack.getCount();
                DynamicPricingEngine.recordSale(itemId, stack.getCount());
                sellContainer.setItem(i, ItemStack.EMPTY);
            } else {
                unsold.add(stack);
                sellContainer.setItem(i, ItemStack.EMPTY);
            }
        }

        // Return unsold items
        for (ItemStack rem : unsold) {
            if (!sp.getInventory().add(rem)) {
                sp.drop(rem, false);
            }
        }

        if (totalEarnings > 0) {
            TakashaRpgData data = TakashaRpgManager.getOrCreateData(sp);
            data.addBalance(totalEarnings);
            TakashaRpgManager.savePlayerData(sp.getUUID());
            TakashaRpgManager.syncToClient(sp);

            sp.sendSystemMessage(Component.literal("§a§l[EconomyShopGUI] §fBerhasil menjual §e" + totalItems + " §fitem dengan total pendapatan: §a§l$" + String.format("%.2f", totalEarnings) + "§f!"));
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.2f);
        } else if (!unsold.isEmpty()) {
            sp.sendSystemMessage(Component.literal("§e§l[EconomyShopGUI] §cBarang tidak memiliki nilai jual pasar dan dikembalikan ke inventaris."));
        }
    }

    private void cancelAndReturn(ServerPlayer sp) {
        if (processed) return;
        processed = true;

        for (int i = 0; i < 45; i++) {
            ItemStack stack = sellContainer.getItem(i);
            if (!stack.isEmpty()) {
                if (!sp.getInventory().add(stack)) {
                    sp.drop(stack, false);
                }
                sellContainer.setItem(i, ItemStack.EMPTY);
            }
        }
        sp.sendSystemMessage(Component.literal("§c§l[EconomyShopGUI] §7Penjualan dibatalkan. Seluruh barang dikembalikan."));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer sp && !processed) {
            // Auto-confirm sell on close for convenience
            confirmSell(sp);
        }
    }

    private static ItemStack makeItem(Item item, String name, List<Component> lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        if (!lore.isEmpty()) {
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }
        return stack;
    }
}
