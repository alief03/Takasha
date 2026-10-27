package net.takasha.rpg.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.takasha.rpg.enchant.TakashaEnchantRegistry;
import net.takasha.rpg.enchant.TakashaEnchantRegistry.CustomEnchantDef;
import net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier;
import net.takasha.rpg.item.TakashaCustomBookItem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TakashaEnchantCatalogMenu
 * Antarmuka GUI Ensiklopedia 54-slot interaktif untuk perintah Creative/Admin (/ae premade & /ae catalog).
 * Menampilkan seluruh 336 Custom Enchantments secara terpadu dari 3 preset (Default, CosmicPvP, Vanilla)
 * lengkap dengan navigasi multi-halaman dan filter dinamis per tier atau tipe gear.
 * Mengklik entri enchant langsung memberikan buku sihir 100% success rate ke admin.
 */
public class TakashaEnchantCatalogMenu {

    private static final int PAGE_SIZE = 45; // 5 baris pertama (slot 0..44)
    private static final Map<UUID, UserFilterState> USER_STATES = new ConcurrentHashMap<>();

    public static class UserFilterState {
        public int page = 0;
        public EnchantTier filterTier = null; // null = semua tier
        public String filterCategory = "ALL"; // ALL, WEAPON, ARMOR, TOOL, BOW
    }

    public static void open(ServerPlayer player) {
        UserFilterState state = USER_STATES.computeIfAbsent(player.getUUID(), u -> new UserFilterState());
        openWithState(player, state);
    }

    public static void openWithState(ServerPlayer player, UserFilterState state) {
        SimpleContainer container = new SimpleContainer(54);
        List<CustomEnchantDef> filtered = getFilteredList(state);
        int maxPage = Math.max(0, (filtered.size() - 1) / PAGE_SIZE);
        if (state.page > maxPage) state.page = maxPage;
        if (state.page < 0) state.page = 0;

        populateCatalogContainer(container, filtered, state, maxPage);

        // In-place reuse jika sudah membuka ReadOnlyChestMenu 54-slot
        if (player.containerMenu instanceof ReadOnlyChestMenu currentMenu
                && currentMenu.getContainer() instanceof SimpleContainer curCont
                && curCont.getContainerSize() == 54) {

            populateCatalogContainer(curCont, filtered, state, maxPage);
            currentMenu.setSlotClickAction((slotId, button, clickType, pl) -> {
                handleCatalogClick(player, slotId, button, clickType, state);
            });
            currentMenu.broadcastChanges();
            return;
        }

        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new ReadOnlyChestMenu(id, inv, container, 6, (slotId, button, clickType, pl) -> {
                handleCatalogClick(player, slotId, button, clickType, state);
            }),
            Component.literal("§6§l✦ ADVANCED ENCHANTMENTS CATALOG (336) ✦")
        ));
    }

    private static List<CustomEnchantDef> getFilteredList(UserFilterState state) {
        List<CustomEnchantDef> all = new ArrayList<>(TakashaEnchantRegistry.getAll());
        // Urutkan berdasarkan Tier lalu nama
        all.sort(Comparator.comparing((CustomEnchantDef d) -> d.tier.ordinal())
                .thenComparing(d -> d.name));

        List<CustomEnchantDef> result = new ArrayList<>();
        for (CustomEnchantDef def : all) {
            if (state.filterTier != null && def.tier != state.filterTier) {
                continue;
            }
            if (!"ALL".equalsIgnoreCase(state.filterCategory)) {
                String target = def.appliesTo != null ? def.appliesTo.toUpperCase() : "";
                if ("WEAPON".equalsIgnoreCase(state.filterCategory) && !target.contains("SWORD") && !target.contains("AXE") && !target.contains("WEAPON")) {
                    continue;
                }
                if ("ARMOR".equalsIgnoreCase(state.filterCategory) && !target.contains("ARMOR") && !target.contains("HELMET") && !target.contains("BOOTS") && !target.contains("CHESTPLATE") && !target.contains("LEGGINGS")) {
                    continue;
                }
                if ("TOOL".equalsIgnoreCase(state.filterCategory) && !target.contains("PICKAXE") && !target.contains("TOOL") && !target.contains("SHOVEL")) {
                    continue;
                }
                if ("BOW".equalsIgnoreCase(state.filterCategory) && !target.contains("BOW") && !target.contains("CROSSBOW")) {
                    continue;
                }
            }
            result.add(def);
        }
        return result;
    }

    private static void populateCatalogContainer(SimpleContainer container, List<CustomEnchantDef> list, UserFilterState state, int maxPage) {
        ItemStack filler = makeItem(Items.STAINED_GLASS_PANE.black(), "§8", List.of());
        for (int i = 0; i < 54; i++) {
            container.setItem(i, filler.copy());
        }

        int startIndex = state.page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, list.size());

        for (int i = startIndex; i < endIndex; i++) {
            CustomEnchantDef def = list.get(i);
            int slot = i - startIndex;

            Item bookItem = TakashaCustomBookItem.getBookItemForTier(def.tier);
            ItemStack bookStack = new ItemStack(bookItem);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("§8ID: §7" + def.id));
            lore.add(Component.literal("§7Tingkat: " + def.tier.getColorCode() + def.tier.getDisplayName() + " §7(Tier " + (def.tier.ordinal() + 1) + ")"));
            lore.add(Component.literal("§7Berlaku untuk: §f" + (def.appliesTo != null && !def.appliesTo.isEmpty() ? def.appliesTo : "Semua Peralatan")));
            lore.add(Component.literal("§7Level Maksimal: §e" + TakashaCustomBookItem.toRoman(def.maxLevel) + " §7(" + def.maxLevel + ")"));
            lore.add(Component.literal("§8§m------------------------"));
            if (def.description != null && !def.description.isEmpty()) {
                lore.add(Component.literal("§f" + def.description));
            }
            lore.add(Component.literal("§8§m------------------------"));
            lore.add(Component.literal("§a100% Tingkat Keberhasilan §7| §c0% Tingkat Hancur"));
            lore.add(Component.literal(""));
            lore.add(Component.literal("§e§l[KLIK KIRI] §fAmbil Buku Level Max (" + def.maxLevel + ")"));
            lore.add(Component.literal("§b§l[KLIK KANAN] §fAmbil Buku Level I"));
            lore.add(Component.literal("§6§l[SHIFT+KLIK] §fAmbil 1 Stack (64x)"));

            bookStack.set(DataComponents.CUSTOM_NAME, Component.literal(def.tier.getColorCode() + "§l" + def.name + " " + TakashaCustomBookItem.toRoman(def.maxLevel)));
            bookStack.set(DataComponents.LORE, new ItemLore(lore));

            container.setItem(slot, bookStack);
        }

        // === BARIS 6: NAVIGASI & KONTROL (Slots 45..53) ===
        // 45: Halaman Sebelumnya
        if (state.page > 0) {
            container.setItem(45, makeItem(Items.ARROW, "§a§l◀ Halaman Sebelumnya", List.of(
                Component.literal("§7Ke halaman " + state.page)
            )));
        } else {
            container.setItem(45, makeItem(Items.STAINED_GLASS_PANE.gray(), "§8Tidak Ada Halaman Sebelumnya", List.of()));
        }

        // 46: Filter Tier (Cycle)
        String tierName = state.filterTier != null ? state.filterTier.getColorCode() + state.filterTier.getDisplayName() : "§fSEMUA TIER";
        container.setItem(46, makeItem(Items.COMPASS, "§e§lFilter Tier: " + tierName, List.of(
            Component.literal("§7Klik untuk mengganti filter kelangkaan:"),
            Component.literal("§7Semua -> Simple -> Unique -> Elite ->"),
            Component.literal("§7Ultimate -> Legendary -> Fabled -> Heroic ->"),
            Component.literal("§7Soul -> Mastery"),
            Component.literal("§8§m------------------------"),
            Component.literal("§e§lKLIK UNTUK MENGGANTI TIER")
        )));

        // 47: Filter Target Peralatan (ALL, WEAPON, ARMOR, TOOL, BOW)
        container.setItem(47, makeItem(Items.IRON_SWORD, "§b§lFilter Kategori: §f" + state.filterCategory, List.of(
            Component.literal("§7Klik untuk memfilter jenis peralatan:"),
            Component.literal("§7ALL -> WEAPON -> ARMOR -> TOOL -> BOW"),
            Component.literal("§8§m------------------------"),
            Component.literal("§e§lKLIK UNTUK MENGGANTI KATEGORI")
        )));

        // 48: Info Registri
        container.setItem(48, makeItem(Items.BOOKSHELF, "§6§l✦ TOTAL ENCHANTMENTS ✦", List.of(
            Component.literal("§7Total Enchant Terdaftar: §e" + TakashaEnchantRegistry.getAll().size() + " Sihir"),
            Component.literal("§7Hasil Filter Saat Ini: §a" + list.size() + " Sihir"),
            Component.literal("§8Preset: Default + CosmicPvP + Vanilla")
        )));

        // 49: Indikator Halaman
        container.setItem(49, makeItem(Items.PAPER, "§f§lHalaman " + (state.page + 1) + " / " + (maxPage + 1), List.of(
            Component.literal("§7Menampilkan enchant §e" + (list.isEmpty() ? 0 : startIndex + 1) + "§7 - §e" + endIndex + "§7 dari §e" + list.size())
        )));

        // 50: Halaman Pertama
        container.setItem(50, makeItem(Items.SPECTRAL_ARROW, "§b§l|◀ Halaman Pertama", List.of(
            Component.literal("§7Lompat langsung ke halaman 1")
        )));

        // 51: Halaman Terakhir
        container.setItem(51, makeItem(Items.SPECTRAL_ARROW, "§b§lHalaman Terakhir ▶|", List.of(
            Component.literal("§7Lompat langsung ke halaman " + (maxPage + 1))
        )));

        // 52: Halaman Selanjutnya
        if (state.page < maxPage) {
            container.setItem(52, makeItem(Items.ARROW, "§a§lHalaman Selanjutnya ▶", List.of(
                Component.literal("§7Ke halaman " + (state.page + 2))
            )));
        } else {
            container.setItem(52, makeItem(Items.STAINED_GLASS_PANE.gray(), "§8Tidak Ada Halaman Selanjutnya", List.of()));
        }

        // 53: Tutup Menu
        container.setItem(53, makeItem(Items.BARRIER, "§c§lTutup Katalog", List.of(
            Component.literal("§7Klik untuk menutup antarmuka katalog.")
        )));
    }

    private static void handleCatalogClick(ServerPlayer player, int slotId, int button, ContainerInput clickType, UserFilterState state) {
        List<CustomEnchantDef> list = getFilteredList(state);
        int maxPage = Math.max(0, (filteredSize(state) - 1) / PAGE_SIZE);

        if (slotId >= 0 && slotId < PAGE_SIZE) {
            int index = state.page * PAGE_SIZE + slotId;
            if (index < list.size()) {
                CustomEnchantDef def = list.get(index);
                int level = (button == 1) ? 1 : def.maxLevel;
                int count = (clickType == ContainerInput.QUICK_MOVE) ? 64 : 1;

                ItemStack book = TakashaCustomBookItem.createRevealedBook(def, level, 100, 0);
                book.setCount(count);

                if (!player.getInventory().add(book)) {
                    player.drop(book, false);
                }

                player.sendSystemMessage(Component.literal("§a[Takasha-AE] Diberikan: " + def.tier.getColorCode() + "§l" + def.name + " " + TakashaCustomBookItem.toRoman(level) + " §e(" + count + "x, 100% Rate)"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8f, 1.2f);
            }
            return;
        }

        // Kontrol navigasi di baris 6
        switch (slotId) {
            case 45 -> { // Prev
                if (state.page > 0) {
                    state.page--;
                    openWithState(player, state);
                    playClick(player);
                }
            }
            case 46 -> { // Filter Tier Cycle
                state.filterTier = nextTier(state.filterTier);
                state.page = 0;
                openWithState(player, state);
                playClick(player);
            }
            case 47 -> { // Filter Category Cycle
                state.filterCategory = nextCategory(state.filterCategory);
                state.page = 0;
                openWithState(player, state);
                playClick(player);
            }
            case 50 -> { // First Page
                state.page = 0;
                openWithState(player, state);
                playClick(player);
            }
            case 51 -> { // Last Page
                state.page = maxPage;
                openWithState(player, state);
                playClick(player);
            }
            case 52 -> { // Next
                if (state.page < maxPage) {
                    state.page++;
                    openWithState(player, state);
                    playClick(player);
                }
            }
            case 53 -> player.closeContainer();
        }
    }

    private static int filteredSize(UserFilterState state) {
        return getFilteredList(state).size();
    }

    private static EnchantTier nextTier(EnchantTier current) {
        if (current == null) return EnchantTier.SIMPLE;
        int next = current.ordinal() + 1;
        EnchantTier[] all = EnchantTier.values();
        if (next >= all.length) return null; // Kembali ke ALL
        return all[next];
    }

    private static String nextCategory(String current) {
        return switch (current.toUpperCase()) {
            case "ALL" -> "WEAPON";
            case "WEAPON" -> "ARMOR";
            case "ARMOR" -> "TOOL";
            case "TOOL" -> "BOW";
            default -> "ALL";
        };
    }

    private static void playClick(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
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
