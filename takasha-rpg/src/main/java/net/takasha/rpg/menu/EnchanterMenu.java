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
import net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier;
import net.takasha.rpg.item.TakashaCustomBookItem;
import net.takasha.rpg.registry.ModRpgItems;

import java.util.ArrayList;
import java.util.List;

/**
 * EnchanterMenu
 * Antarmuka GUI Enchanter 18-slot autentik 100% identik dengan AdvancedEnchantments enchanter.yml.
 * - Judul: "&7Server Enchanter" (18 slot, 2 baris)
 * - Baris 1: Slot 2 (Simple), 3 (Unique), 4 (Elite), 5 (Ultimate), 6 (Legendary)
 * - Baris 2: Slot 9 (Tinkerer), 11 (Heroic), 12 (Soul), 13 (Fabled), 14 (Mastery), 17 (Alchemist)
 * - Sistem Pembayaran: EXP murni dengan menu konfirmasi 9-slot (&aConfirmation Inventory).
 */
public class EnchanterMenu {

    public static final int SIMPLE_EXP = 400;
    public static final int UNIQUE_EXP = 800;
    public static final int ELITE_EXP = 2500;
    public static final int ULTIMATE_EXP = 5000;
    public static final int LEGENDARY_EXP = 25000;
    public static final int FABLED_EXP = 40000;
    public static final int HEROIC_EXP = 60000;
    public static final int SOUL_EXP = 100000;
    public static final int MASTERY_EXP = 250000;

    public static void open(ServerPlayer player) {
        SimpleContainer container = new SimpleContainer(18);

        // Jika pemain sudah membuka ReadOnlyChestMenu 18-slot, reuse kontainer secara in-place
        if (player.containerMenu instanceof ReadOnlyChestMenu currentMenu
                && currentMenu.getContainer() instanceof SimpleContainer curCont
                && curCont.getContainerSize() == 18) {

            populateEnchanterContainer(curCont, player);
            currentMenu.setSlotClickAction((slotId, button, clickType, pl) -> {
                handleEnchanterClick(player, slotId);
            });
            currentMenu.broadcastChanges();
            return;
        }

        populateEnchanterContainer(container, player);

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ReadOnlyChestMenu(id, inv, container, 2, (slotId, button, clickType, pl) -> {
                handleEnchanterClick(player, slotId);
            }),
            Component.literal("§7Server Enchanter")
        ));
    }

    private static void populateEnchanterContainer(SimpleContainer container, ServerPlayer player) {
        ItemStack filler = makeItem(Items.STAINED_GLASS_PANE.gray(), "§7", List.of());
        for (int i = 0; i < 18; i++) {
            container.setItem(i, filler.copy());
        }

        int playerExp = getPlayerTotalExp(player);

        // Slot 2: Simple (400 EXP)
        container.setItem(2, makeTierItem(Items.STAINED_GLASS_PANE.white(), "§f§lSimple Enchantment §7(Click)",
                "simple", "§f", SIMPLE_EXP, playerExp));

        // Slot 3: Unique (800 EXP)
        container.setItem(3, makeTierItem(Items.STAINED_GLASS_PANE.lime(), "§a§lUnique Enchantment §7(Click)",
                "unique", "§a", UNIQUE_EXP, playerExp));

        // Slot 4: Elite (2,500 EXP)
        container.setItem(4, makeTierItem(Items.STAINED_GLASS_PANE.lightBlue(), "§b§lElite Enchantment §7(Click)",
                "elite", "§b", ELITE_EXP, playerExp));

        // Slot 5: Ultimate (5,000 EXP)
        container.setItem(5, makeTierItem(Items.STAINED_GLASS_PANE.yellow(), "§e§lUltimate Enchantment §7(Click)",
                "ultimate", "§e", ULTIMATE_EXP, playerExp));

        // Slot 6: Legendary (25,000 EXP)
        container.setItem(6, makeTierItem(Items.STAINED_GLASS_PANE.orange(), "§6§lLegendary Enchantment §7(Click)",
                "legendary", "§6", LEGENDARY_EXP, playerExp));

        // Slot 9: Tinkerer Shortcut (Anvil)
        container.setItem(9, makeItem(Items.ANVIL, "§fOpen §b§lTinkerer §7(Click)", List.of(
            Component.literal("§7Tinker books for Magic Dusts and EXP."),
            Component.literal(""),
            Component.literal("§b§lClick to visit")
        )));

        // Slot 11: Heroic (60,000 EXP)
        container.setItem(11, makeTierItem(Items.STAINED_GLASS_PANE.magenta(), "§c§lHeroic Enchantment §7(Click)",
                "heroic", "§c", HEROIC_EXP, playerExp));

        // Slot 12: Soul (100,000 EXP)
        container.setItem(12, makeTierItem(Items.STAINED_GLASS_PANE.cyan(), "§3§lSoul Enchantment §7(Click)",
                "soul", "§3", SOUL_EXP, playerExp));

        // Slot 13: Fabled (40,000 EXP)
        container.setItem(13, makeTierItem(Items.STAINED_GLASS_PANE.pink(), "§d§lFabled Enchantment §7(Click)",
                "fabled", "§d", FABLED_EXP, playerExp));

        // Slot 14: Mastery (250,000 EXP)
        container.setItem(14, makeTierItem(Items.STAINED_GLASS_PANE.red(), "§4§lMastery Enchantment §7(Click)",
                "mastery", "§4", MASTERY_EXP, playerExp));

        // Slot 17: Alchemist Shortcut (End Portal Frame)
        container.setItem(17, makeItem(Items.END_PORTAL_FRAME, "§fOpen §b§lAlchemist §7(Click)", List.of(
            Component.literal("§7Combine Books and Magic Dusts."),
            Component.literal(""),
            Component.literal("§b§lClick to visit")
        )));
    }

    private static ItemStack makeTierItem(Item icon, String title, String tierName, String color, int cost, int playerExp) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Examine to receive a random"));
        lore.add(Component.literal(color + tierName + " §7enchantment book."));
        lore.add(Component.literal(""));
        int left = Math.max(0, cost - playerExp);
        if (left > 0) {
            lore.add(Component.literal("§b§lCOST §f" + cost + " EXP §7(You need §c" + left + " §7more EXP)"));
        } else {
            lore.add(Component.literal("§b§lCOST §f" + cost + " EXP §7(§aReady to purchase§7)"));
        }
        return makeItem(icon, title, lore);
    }

    private static void handleEnchanterClick(ServerPlayer player, int slotId) {
        switch (slotId) {
            case 2 -> openConfirmation(player, EnchantTier.SIMPLE, SIMPLE_EXP);
            case 3 -> openConfirmation(player, EnchantTier.UNIQUE, UNIQUE_EXP);
            case 4 -> openConfirmation(player, EnchantTier.ELITE, ELITE_EXP);
            case 5 -> openConfirmation(player, EnchantTier.ULTIMATE, ULTIMATE_EXP);
            case 6 -> openConfirmation(player, EnchantTier.LEGENDARY, LEGENDARY_EXP);
            case 9 -> TinkererMenu.open(player);
            case 11 -> openConfirmation(player, EnchantTier.HEROIC, HEROIC_EXP);
            case 12 -> openConfirmation(player, EnchantTier.SOUL, SOUL_EXP);
            case 13 -> openConfirmation(player, EnchantTier.FABLED, FABLED_EXP);
            case 14 -> openConfirmation(player, EnchantTier.MASTERY, MASTERY_EXP);
            case 17 -> AlchemistMenu.open(player);
        }
    }

    /**
     * Confirmation Inventory (27 slot, 3 baris)
     * Autentik AdvancedEnchantments dengan fitur Bulk Purchase (Kuantitas Pembelian Banyak)
     * dan in-place container updates:
     * - Baris 0: Border Stained Glass Pane Gray
     * - Baris 1: Slot 10 (-10), 11 (-1), 13 (Preview Book xQty), 15 (+1), 16 (+10)
     * - Baris 2: Slot 19 (Set Min 1), 20 & 21 (Accept), 22 (Info Emerald), 23 & 24 (Cancel), 25 (Set Max Afford)
     * Menggunakan in-place container updates sehingga pemain dapat bebas mengklik penambah/pengurang
     * kuantitas tanpa jeda jaringan dan tanpa mouse cursor snapping ke tengah layar.
     */
    public static void openConfirmation(ServerPlayer player, EnchantTier tier, int cost) {
        openConfirmation(player, tier, cost, 1);
    }

    public static void openConfirmation(ServerPlayer player, EnchantTier tier, int cost, int initialQuantity) {
        int[] qtyHolder = new int[]{ Math.max(1, Math.min(64, initialQuantity)) };
        SimpleContainer container = new SimpleContainer(27);

        // Jika pemain sudah membuka ReadOnlyChestMenu 27-slot, reuse kontainer secara in-place!
        if (player.containerMenu instanceof ReadOnlyChestMenu currentMenu
                && currentMenu.getContainer() instanceof SimpleContainer curCont
                && curCont.getContainerSize() == 27) {

            populateConfirmationContainer(curCont, player, tier, cost, qtyHolder[0]);
            currentMenu.setSlotClickAction((slotId, button, clickType, pl) -> {
                handleConfirmationClick(curCont, player, tier, cost, qtyHolder, slotId);
            });
            currentMenu.broadcastChanges();
            return;
        }

        populateConfirmationContainer(container, player, tier, cost, qtyHolder[0]);

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ReadOnlyChestMenu(id, inv, container, 3, (slotId, button, clickType, pl) -> {
                handleConfirmationClick(container, player, tier, cost, qtyHolder, slotId);
            }),
            Component.literal("§aConfirmation: " + tier.getDisplayName())
        ));
    }

    private static void populateConfirmationContainer(SimpleContainer container, ServerPlayer player, EnchantTier tier, int cost, int quantity) {
        ItemStack filler = makeItem(Items.STAINED_GLASS_PANE.gray(), "§7", List.of());
        for (int i = 0; i < 27; i++) {
            container.setItem(i, filler.copy());
        }

        int playerExp = getPlayerTotalExp(player);
        int totalCost = cost * quantity;
        int maxAffordable = cost > 0 ? Math.max(1, Math.min(64, playerExp / cost)) : 64;

        // Slot 10: -10
        container.setItem(10, makeItem(Items.STAINED_GLASS_PANE.red(), "§c§l-10 Buku", List.of(
            Component.literal("§7Kurangi kuantitas pembelian sebanyak §c10§7."),
            Component.literal("§7Kuantitas saat ini: §f" + quantity)
        )));

        // Slot 11: -1
        container.setItem(11, makeItem(Items.STAINED_GLASS_PANE.red(), "§c§l-1 Buku", List.of(
            Component.literal("§7Kurangi kuantitas pembelian sebanyak §c1§7."),
            Component.literal("§7Kuantitas saat ini: §f" + quantity)
        )));

        // Slot 13: Preview Mystery Book (count = quantity)
        Item bookItem = ModRpgItems.getMysteryBookForTier(tier);
        ItemStack previewBook = new ItemStack(bookItem, Math.min(64, quantity));
        previewBook.set(DataComponents.CUSTOM_NAME, Component.literal(tier.getColorCode() + "§l" + tier.getDisplayName().toUpperCase() + " MYSTERY BOOK"));
        List<Component> bookLore = new ArrayList<>();
        bookLore.add(Component.literal("§7Tier: " + tier.getColorCode() + "§l" + tier.getDisplayName()));
        bookLore.add(Component.literal("§7Jumlah: §f" + quantity + "x Buku"));
        bookLore.add(Component.literal("§7Biaya Satuan: §e" + cost + " EXP"));
        bookLore.add(Component.literal("§7Total Biaya: §e" + totalCost + " EXP"));
        bookLore.add(Component.literal(""));
        bookLore.add(Component.literal("§eKlik kanan buku di tangan"));
        bookLore.add(Component.literal("§euntuk mengungkap sihir di dalamnya!"));
        previewBook.set(DataComponents.LORE, new ItemLore(bookLore));
        container.setItem(13, previewBook);

        // Slot 15: +1
        container.setItem(15, makeItem(Items.STAINED_GLASS_PANE.lime(), "§a§l+1 Buku", List.of(
            Component.literal("§7Tambah kuantitas pembelian sebanyak §a1§7."),
            Component.literal("§7Kuantitas saat ini: §f" + quantity)
        )));

        // Slot 16: +10
        container.setItem(16, makeItem(Items.STAINED_GLASS_PANE.lime(), "§a§l+10 Buku", List.of(
            Component.literal("§7Tambah kuantitas pembelian sebanyak §a10§7."),
            Component.literal("§7Kuantitas saat ini: §f" + quantity)
        )));

        // Slot 19: SET MIN (1)
        container.setItem(19, makeItem(Items.STAINED_GLASS_PANE.yellow(), "§e§lSetel Minimal (1x)", List.of(
            Component.literal("§7Setel kuantitas pembelian langsung ke §e1 buku§7.")
        )));

        // Slot 20 & 21: Accept Transaction
        ItemStack acceptButton = makeItem(Items.STAINED_GLASS_PANE.lime(), "§a§l✔ Accept Transaction", List.of(
            Component.literal("§7Klik untuk membayar §e" + totalCost + " EXP"),
            Component.literal("§7untuk §f" + quantity + "x " + tier.getColorCode() + tier.getDisplayName() + " Mystery Book§7.")
        ));
        container.setItem(20, acceptButton);
        container.setItem(21, acceptButton.copy());

        // Slot 22: Transaction Info Emerald
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.literal("§7Tier: " + tier.getColorCode() + "§l" + tier.getDisplayName().toUpperCase()));
        infoLore.add(Component.literal("§7Jumlah: §f" + quantity + "x Buku"));
        infoLore.add(Component.literal("§7Biaya Satuan: §e" + cost + " EXP"));
        infoLore.add(Component.literal("§7Total Biaya: §e" + totalCost + " EXP"));
        infoLore.add(Component.literal("§7EXP Pemain: §f" + playerExp + " EXP"));
        infoLore.add(Component.literal(""));
        if (playerExp >= totalCost) {
            infoLore.add(Component.literal("§a✔ Siap membeli! Klik tombol Hijau (Accept)."));
        } else {
            infoLore.add(Component.literal("§c✖ EXP kurang! Butuh §e" + (totalCost - playerExp) + " EXP §clagi."));
        }
        container.setItem(22, makeItem(Items.EMERALD, "§e§lPurchase Information", infoLore));

        // Slot 23 & 24: Cancel
        ItemStack cancelButton = makeItem(Items.STAINED_GLASS_PANE.red(), "§c§l✖ Batal (Cancel)", List.of(
            Component.literal("§7Kembali ke Server Enchanter.")
        ));
        container.setItem(23, cancelButton);
        container.setItem(24, cancelButton.copy());

        // Slot 25: SET MAX (Affordable)
        container.setItem(25, makeItem(Items.STAINED_GLASS_PANE.lightBlue(), "§b§lBeli Maksimal (Max)", List.of(
            Component.literal("§7Hitung otomatis jumlah maksimal buku"),
            Component.literal("§7yang dapat dibeli dengan EXP kamu saat ini."),
            Component.literal("§7Maksimal terjangkau: §b" + maxAffordable + "x Buku")
        )));
    }

    private static void handleConfirmationClick(SimpleContainer container, ServerPlayer player, EnchantTier tier, int cost, int[] qtyHolder, int slotId) {
        int curQty = qtyHolder[0];
        int playerExp = getPlayerTotalExp(player);

        if (slotId == 10) {
            // -10
            qtyHolder[0] = Math.max(1, curQty - 10);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 0.6f, 0.9f);
            populateConfirmationContainer(container, player, tier, cost, qtyHolder[0]);
            player.containerMenu.broadcastChanges();
        } else if (slotId == 11) {
            // -1
            qtyHolder[0] = Math.max(1, curQty - 1);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 0.6f, 0.95f);
            populateConfirmationContainer(container, player, tier, cost, qtyHolder[0]);
            player.containerMenu.broadcastChanges();
        } else if (slotId == 15) {
            // +1
            qtyHolder[0] = Math.min(64, curQty + 1);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 0.6f, 1.05f);
            populateConfirmationContainer(container, player, tier, cost, qtyHolder[0]);
            player.containerMenu.broadcastChanges();
        } else if (slotId == 16) {
            // +10
            qtyHolder[0] = Math.min(64, curQty + 10);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 0.6f, 1.1f);
            populateConfirmationContainer(container, player, tier, cost, qtyHolder[0]);
            player.containerMenu.broadcastChanges();
        } else if (slotId == 19) {
            // SET MIN (1)
            qtyHolder[0] = 1;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 0.6f, 0.8f);
            populateConfirmationContainer(container, player, tier, cost, qtyHolder[0]);
            player.containerMenu.broadcastChanges();
        } else if (slotId == 25) {
            // SET MAX (Affordable)
            int maxAfford = cost > 0 ? playerExp / cost : 64;
            qtyHolder[0] = Math.max(1, Math.min(64, maxAfford));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 0.6f, 1.2f);
            populateConfirmationContainer(container, player, tier, cost, qtyHolder[0]);
            player.containerMenu.broadcastChanges();
        } else if (slotId == 20 || slotId == 21) {
            processPurchase(player, tier, cost, qtyHolder[0]);
        } else if (slotId == 23 || slotId == 24) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 0.8f, 1.0f);
            open(player);
        }
    }

    private static void processPurchase(ServerPlayer player, EnchantTier tier, int cost, int quantity) {
        int totalCost = cost * quantity;
        int currentExp = getPlayerTotalExp(player);
        if (currentExp < totalCost) {
            player.sendSystemMessage(Component.literal("§cYou do not have enough experience! Requires §e" + totalCost + " EXP§c for §e" + quantity + "x §cbooks. (You have §e" + currentExp + " EXP§c)"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            open(player);
            return;
        }

        deductPlayerExp(player, totalCost);

        Item bookItem = ModRpgItems.getMysteryBookForTier(tier);
        int remaining = quantity;
        int maxStack = bookItem.getDefaultMaxStackSize();
        while (remaining > 0) {
            int toGive = Math.min(remaining, maxStack);
            ItemStack mysteryBook = new ItemStack(bookItem, toGive);
            if (!player.getInventory().add(mysteryBook)) {
                player.drop(mysteryBook, false);
            }
            remaining -= toGive;
        }
        player.containerMenu.broadcastChanges();

        player.sendSystemMessage(Component.literal("§a§l[Enchanter] §fTransaction successful! Received §e" + quantity + "x " + tier.getColorCode() + "§l" + tier.getDisplayName().toUpperCase() + " MYSTERY BOOK§f! (Spent §e" + totalCost + " EXP§f)"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);

        open(player);
    }

    // === EXPERIENCE CALCULATION UTILITIES ===

    public static int getPlayerTotalExp(ServerPlayer player) {
        int total = 0;
        int level = player.experienceLevel;
        for (int i = 0; i < level; i++) {
            total += getExpForLevel(i);
        }
        total += Math.round(player.experienceProgress * (float) getExpForLevel(level));
        return total;
    }

    public static int getExpForLevel(int level) {
        if (level >= 30) return 9 * level - 158;
        if (level >= 15) return 5 * level - 38;
        return 2 * level + 7;
    }

    public static void deductPlayerExp(ServerPlayer player, int amount) {
        int current = getPlayerTotalExp(player);
        int newExp = Math.max(0, current - amount);
        player.totalExperience = newExp;
        player.experienceLevel = 0;
        player.experienceProgress = 0.0f;

        while (newExp >= getExpForLevel(player.experienceLevel)) {
            newExp -= getExpForLevel(player.experienceLevel);
            player.experienceLevel++;
        }
        player.experienceProgress = (float) newExp / (float) getExpForLevel(player.experienceLevel);
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
