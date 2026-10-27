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
import net.takasha.rpg.economy.TakashaDailyShopManager;

import java.util.ArrayList;
import java.util.List;

/**
 * RotatingShopMenu
 * Antarmuka GUI Toko Berotasi 27-slot otentik EconomyShopGUI (1-sections/RotatingShop.yml).
 * Menyatukan fungsionalitas /rotatingshop dan /dailyshop dalam satu antarmuka terpadu.
 */
public class RotatingShopMenu {

    private static final int[] DISPLAY_SLOTS = {10, 11, 12, 14, 15, 16};

    public static void open(ServerPlayer player) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        List<TakashaDailyShopManager.DailyDeal> deals = TakashaDailyShopManager.getCurrentDeals();
        SimpleContainer container = new SimpleContainer(27);

        // In-place reuse jika sudah membuka ReadOnlyChestMenu 27-slot
        if (player.containerMenu instanceof ReadOnlyChestMenu currentMenu
                && currentMenu.getContainer() instanceof SimpleContainer curCont
                && curCont.getContainerSize() == 27) {

            populateRotatingContainer(curCont, player, deals, data);
            currentMenu.setSlotClickAction((slotId, button, clickType, pl) -> {
                handleRotatingClick(player, slotId);
            });
            currentMenu.broadcastChanges();
            return;
        }

        populateRotatingContainer(container, player, deals, data);

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ReadOnlyChestMenu(id, inv, container, 3, (slotId, button, clickType, pl) -> {
                handleRotatingClick(player, slotId);
            }),
            Component.literal("§d§l✦ Magic Shop — Rotating Deals ✦")
        ));
    }

    private static void populateRotatingContainer(SimpleContainer container, ServerPlayer player, List<TakashaDailyShopManager.DailyDeal> deals, TakashaRpgData data) {
        ItemStack filler = makeItem(Items.STAINED_GLASS_PANE.purple(), "§8", List.of());
        for (int i = 0; i < 27; i++) {
            container.setItem(i, filler.copy());
        }

        List<Component> clockLore = new ArrayList<>();
        clockLore.add(Component.literal("§7Katalog penawaran mistis terbatas"));
        clockLore.add(Component.literal("§7dengan potongan harga diskon 15% - 40%!"));
        clockLore.add(Component.literal("§8§m------------------------"));
        clockLore.add(Component.literal("§eRotasi Berikutnya Dalam: §b§l" + TakashaDailyShopManager.getRemainingTimeString()));
        clockLore.add(Component.literal("§7Klik langsung pada barang untuk membeli."));
        container.setItem(4, makeItem(Items.CLOCK, "§d§l✦ MAGIC SHOP (ROTATING DEALS) ✦", clockLore));

        for (int i = 0; i < DISPLAY_SLOTS.length && i < deals.size(); i++) {
            var deal = deals.get(i);
            ItemStack displayStack = deal.stack().copy();

            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("§7Kelangkaan: " + deal.tier().getColor() + deal.tier().name()));
            lore.add(Component.literal("§7Harga Standar: §m$" + String.format("%.0f", deal.originalPrice()) + "§r"));
            lore.add(Component.literal("§7Diskon Spesial: §c§l-" + (int) deal.discountPercent() + "% OFF"));
            lore.add(Component.literal("§aHarga Penawaran: §a§l" + (deal.useGems() ? ((int) deal.finalPrice() + " Gems ♦") : ("$" + String.format("%.0f", deal.finalPrice())))));
            lore.add(Component.literal("§7Sisa Stok: §e" + deal.stock() + " unit"));
            lore.add(Component.literal("§8§m------------------------"));
            if (deal.stock() > 0) {
                lore.add(Component.literal("§e[Klik Kiri] §7Beli Penawaran Spesial"));
            } else {
                lore.add(Component.literal("§c§lSTOK HABIS HARI INI"));
            }

            displayStack.set(DataComponents.LORE, new ItemLore(lore));
            container.setItem(DISPLAY_SLOTS[i], displayStack);
        }

        container.setItem(18, makeItem(Items.ARROW, "§c§l← Menu Utama Pasar", List.of(
            Component.literal("§7Kembali ke menu pasar utama (/shop).")
        )));

        container.setItem(22, makeItem(Items.EMERALD, "§6§l✦ DOMPET PENGEMBARA ✦", List.of(
            Component.literal("§7Saldo Uang: §a$" + String.format("%,.2f", data.getBalance())),
            Component.literal("§7Koin Gems: §b" + data.getGems() + " Gems ♦")
        )));

        container.setItem(26, makeItem(Items.BARRIER, "§c§l✕ Tutup Menu", List.of(
            Component.literal("§7Keluar dari toko mistis.")
        )));
    }

    private static void handleRotatingClick(ServerPlayer player, int slotId) {
        if (slotId == 18) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.0f);
            TakashaShopMenu.openShop(player);
        } else if (slotId == 26) {
            player.closeContainer();
        } else {
            for (int i = 0; i < DISPLAY_SLOTS.length; i++) {
                if (slotId == DISPLAY_SLOTS[i]) {
                    buyDeal(player, i);
                    open(player);
                    return;
                }
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.2f);
        }
    }

    private static void buyDeal(ServerPlayer player, int index) {
        List<TakashaDailyShopManager.DailyDeal> deals = TakashaDailyShopManager.getCurrentDeals();
        if (index < 0 || index >= deals.size()) return;

        TakashaDailyShopManager.DailyDeal deal = deals.get(index);
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);

        if (deal.stock() <= 0) {
            player.sendSystemMessage(Component.literal("§cStok barang ini telah habis untuk periode rotasi ini!"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6f, 1.0f);
            return;
        }

        if (deal.useGems()) {
            if (data.getGems() < (int) deal.finalPrice()) {
                player.sendSystemMessage(Component.literal("§cTakasha Gems tidak cukup! Butuh: §b" + (int) deal.finalPrice() + " Gems ♦"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6f, 1.0f);
                return;
            }
        } else {
            if (data.getBalance() < deal.finalPrice()) {
                player.sendSystemMessage(Component.literal("§cSaldo uang tidak cukup! Butuh: §a$" + String.format("%.0f", deal.finalPrice())));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6f, 1.0f);
                return;
            }
        }

        boolean success = TakashaDailyShopManager.purchaseDeal(index, data);
        if (success) {
            ItemStack reward = deal.stack().copy();
            if (!player.getInventory().add(reward)) {
                player.drop(reward, false);
            }

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.4f);

            player.sendSystemMessage(Component.literal(
                "§a§l[MagicShop] §fBerhasil membeli §e" + deal.stack().getHoverName().getString() +
                " §fx" + deal.stack().getCount() + " seharga " +
                (deal.useGems() ? ("§b" + (int) deal.finalPrice() + " Gems ♦") : ("§a$" + String.format("%.0f", deal.finalPrice()))) + "§f!"
            ));
        }
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
