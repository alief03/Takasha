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
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;
import net.takasha.rpg.economy.catalog.ShopCatalog;
import net.takasha.rpg.economy.catalog.ShopCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * TakashaShopMenu
 * Antarmuka GUI Pasar 54-slot otentik 100% paritas EconomyShopGUI-Premium 6.3.1 (1-sections/*.yml).
 * Memuat 18 kategori aktif langsung dari ShopCatalog dengan posisi slot otentik sumber.
 */
public class TakashaShopMenu {

    public static void openShop(ServerPlayer player) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        SimpleContainer container = new SimpleContainer(54);

        // Latar belakang kaca hitam
        ItemStack filler = makeItem(Items.STAINED_GLASS_PANE.black(), "§8", List.of());
        for (int i = 0; i < 54; i++) {
            container.setItem(i, filler.copy());
        }

        // Pasang seluruh kategori aktif dari ShopCatalog ke slot masing-masing
        for (ShopCategory cat : ShopCatalog.getAllCategories()) {
            if (cat.isEnabled() && cat.getSlot() >= 0 && cat.getSlot() < 54) {
                container.setItem(cat.getSlot(), cat.createIconStack());
            }
        }

        // Slot 45: Dompet Pengembara
        List<Component> walletLore = new ArrayList<>();
        walletLore.add(Component.literal("§7Saldo Uang: §a§l$" + String.format("%,.2f", data.getBalance())));
        walletLore.add(Component.literal("§7Koin Gems: §b§l" + data.getGems() + " Gems ♦"));
        walletLore.add(Component.literal(""));
        walletLore.add(Component.literal("§8Nilai pasar dinamis terbarui secara realtime."));
        container.setItem(45, makeItem(Items.EMERALD, "§6§l✦ DOMPET PENGEMBARA ✦", walletLore));

        // Slot 49: Buka Peti Jual Massal (/sellgui)
        List<Component> sellLore = new ArrayList<>();
        sellLore.add(Component.literal("§7Buka peti jual otomatis 54 slot."));
        sellLore.add(Component.literal("§7Taruh puluhan stack item sekaligus, lalu"));
        sellLore.add(Component.literal("§7tutup peti untuk menguangkan seluruhnya!"));
        sellLore.add(Component.literal(""));
        sellLore.add(Component.literal("§e[Klik Kiri] §7Buka /sellgui"));
        container.setItem(49, makeItem(Items.CHEST, "§a§l💰 PETI JUAL CEPAT (/SELLGUI)", sellLore));

        // Slot 53: Tutup
        container.setItem(53, makeItem(Items.BARRIER, "§c§l✕ Tutup Pasar", List.of(
            Component.literal("§7Keluar dari menu pasar.")
        )));

        // In-place reuse jika sudah membuka ReadOnlyChestMenu 54-slot
        if (player.containerMenu instanceof ReadOnlyChestMenu currentMenu
                && currentMenu.getContainer() instanceof SimpleContainer curCont
                && curCont.getContainerSize() == 54) {

            // Latar belakang kaca hitam
            for (int i = 0; i < 54; i++) {
                curCont.setItem(i, filler.copy());
            }

            for (ShopCategory cat : ShopCatalog.getAllCategories()) {
                if (cat.isEnabled() && cat.getSlot() >= 0 && cat.getSlot() < 54) {
                    curCont.setItem(cat.getSlot(), cat.createIconStack());
                }
            }

            curCont.setItem(45, makeItem(Items.EMERALD, "§6§l✦ DOMPET PENGEMBARA ✦", walletLore));
            curCont.setItem(49, makeItem(Items.CHEST, "§a§l💰 PETI JUAL CEPAT (/SELLGUI)", sellLore));
            curCont.setItem(53, makeItem(Items.BARRIER, "§c§l✕ Tutup Pasar", List.of(
                Component.literal("§7Keluar dari menu pasar.")
            )));

            currentMenu.setSlotClickAction((slotId, button, clickType, pl) -> {
                handleShopClick(player, slotId);
            });
            currentMenu.broadcastChanges();
            return;
        }

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ReadOnlyChestMenu(id, inv, container, 6, (slotId, button, clickType, pl) -> {
                handleShopClick(player, slotId);
            }),
            Component.literal("§6§l✦ Pasar Takasha — EconomyShopGUI ✦")
        ));
    }

    private static void handleShopClick(ServerPlayer player, int slotId) {
        if (slotId == 49) {
            player.closeContainer();
            openSellGui(player);
        } else if (slotId == 53) {
            player.closeContainer();
        } else if (slotId == 4) {
            // Toko Berotasi / RotatingShop
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.2f);
            RotatingShopMenu.open(player);
        } else {
            ShopCategory clickedCat = ShopCatalog.getCategoryBySlot(slotId);
            if (clickedCat != null) {
                if ("rotatingshop".equalsIgnoreCase(clickedCat.getId())) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.2f);
                    RotatingShopMenu.open(player);
                } else {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.1f);
                    ShopCategoryMenu.openCategory(player, clickedCat, 1);
                }
            } else {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 0.8f);
            }
        }
    }

    public static void openSellGui(ServerPlayer player) {
        SellGuiMenu.open(player);
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
