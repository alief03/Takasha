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
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.takasha.rpg.dungeon.DungeonTier;
import net.takasha.rpg.dungeon.reward.DungeonLootEntry;
import net.takasha.rpg.dungeon.reward.DungeonRewardConfigManager;
import net.takasha.rpg.mmo.MMOItemData;
import net.takasha.rpg.mmo.MMOItemManager;
import net.takasha.rpg.mmo.MMOItemTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * DungeonRewardManagerMenu
 * Antarmuka GUI interaktif (54 slot) untuk manajemen drop hadiah dungeon Takasha RPG & ADA.
 * Memungkinkan Gamemaster/OP mengonfigurasi peluang persentase, rentang jumlah, multiplier,
 * menambah item dari inventory/chat, menghapus, simulasi tes roll 1x, dan ekspor konfigurasi JSON.
 */
public class DungeonRewardManagerMenu {

    private static final int PAGE_SIZE = 27; // Grid slots: 18 s.d. 44
    private static final Random RNG = new Random();

    public static void open(ServerPlayer player, DungeonTier tier, int wave) {
        open(player, tier, wave, 0);
    }

    public static void open(ServerPlayer player, DungeonTier tier, int wave, int page) {
        if (!player.isCreative() && !player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) {
            player.sendSystemMessage(Component.literal("§c[Dungeon Rewards] Menu ini hanya dapat diakses oleh Administrator/Gamemaster!"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
            return;
        }

        DungeonTier activeTier = (tier != null) ? tier : DungeonTier.COMMON;
        int activeWave = Math.max(1, Math.min(5, wave));

        List<DungeonLootEntry> entries = DungeonRewardConfigManager.getInstance().getLootForWave(activeTier, activeWave);
        int maxPage = Math.max(0, (entries.size() - 1) / PAGE_SIZE);
        int clampedPage = Math.max(0, Math.min(page, maxPage));

        SimpleContainer container = new SimpleContainer(54);
        populateContainer(container, activeTier, activeWave, entries, clampedPage, maxPage);

        String title = "Dungeon Rewards: " + activeTier.getDisplayName() + " (W" + activeWave + ")";
        ReadOnlyChestMenu.openOrUpdate(player, Component.literal(title), container, 6, (slotId, button, clickType, pl) ->
                handleClick(player, activeTier, activeWave, entries, clampedPage, maxPage, slotId, button, clickType));
    }

    private static void populateContainer(SimpleContainer container, DungeonTier tier, int wave, List<DungeonLootEntry> entries, int page, int maxPage) {
        for (int i = 0; i < 54; i++) {
            container.setItem(i, ItemStack.EMPTY);
        }

        // ================= ROW 0: TAB TIER & GLOBAL ACTIONS =================
        // Slot 0: Common Tier Tab
        ItemStack commonTab = new ItemStack(Items.STAINED_GLASS.lime());
        boolean isCommon = (tier == DungeonTier.COMMON);
        commonTab.set(DataComponents.CUSTOM_NAME, Component.literal((isCommon ? "§a✔ " : "§7") + "§lTIER: COMMON"));
        commonTab.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, isCommon);
        commonTab.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Multiplier Koin: §e1x"),
                Component.literal("§7Klik untuk beralih ke pool Common.")
        )));
        container.setItem(0, commonTab);

        // Slot 1: Nightmare Tier Tab
        ItemStack nightmareTab = new ItemStack(Items.STAINED_GLASS.purple());
        boolean isNightmare = (tier == DungeonTier.NIGHTMARE);
        nightmareTab.set(DataComponents.CUSTOM_NAME, Component.literal((isNightmare ? "§a✔ " : "§5") + "§lTIER: NIGHTMARE"));
        nightmareTab.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, isNightmare);
        nightmareTab.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Multiplier Koin: §e2x"),
                Component.literal("§7Klik untuk beralih ke pool Nightmare.")
        )));
        container.setItem(1, nightmareTab);

        // Slot 2: Sakura Boss Tier Tab
        ItemStack sakuraTab = new ItemStack(Items.STAINED_GLASS.magenta());
        boolean isSakura = (tier == DungeonTier.SAKURA_BOSS);
        sakuraTab.set(DataComponents.CUSTOM_NAME, Component.literal((isSakura ? "§a✔ " : "§d") + "§lTIER: SAKURA BOSS"));
        sakuraTab.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, isSakura);
        sakuraTab.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Multiplier Koin: §e4x"),
                Component.literal("§7Klik untuk beralih ke pool Sakura Boss.")
        )));
        container.setItem(2, sakuraTab);

        // Slot 4: Info Beacon
        ItemStack infoBeacon = new ItemStack(Items.BEACON);
        infoBeacon.set(DataComponents.CUSTOM_NAME, Component.literal("§6§lDungeon Reward Engine"));
        float totalChance = 0.0f;
        for (DungeonLootEntry e : entries) {
            totalChance += e.getChance();
        }
        infoBeacon.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Tier Terpilih: " + tier.getColorCode() + tier.getDisplayName()),
                Component.literal("§7Gelombang (Wave): §e" + wave + " / 5"),
                Component.literal("§7Jumlah Entri Hadiah: §b" + entries.size() + " Item"),
                Component.literal("§7Akumulasi Peluang: §f" + String.format(Locale.ROOT, "%.1f%%", totalChance * 100.0f)),
                Component.literal("§7Status Penyimpanan: §aTersinkronisasi")
        )));
        container.setItem(4, infoBeacon);

        // Slot 6: Simulasi Roll Tes
        ItemStack rollTest = new ItemStack(Items.DISPENSER);
        rollTest.set(DataComponents.CUSTOM_NAME, Component.literal("§e§l[🎲 Simulasi Roll 1x]"));
        rollTest.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Uji coba acak drop rate hadiah"),
                Component.literal("§7untuk Wave " + wave + " (" + tier.getDisplayName() + ")."),
                Component.literal("§7Hasil undian dikirim langsung ke chat."),
                Component.literal("§e► Klik untuk Menjalankan Tes Roll")
        )));
        container.setItem(6, rollTest);

        // Slot 7: Simpan Konfigurasi ke File
        ItemStack saveItem = new ItemStack(Items.EMERALD);
        saveItem.set(DataComponents.CUSTOM_NAME, Component.literal("§a§l[💾 Simpan ke JSON]"));
        saveItem.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Tulis seluruh perubahan konfigurasi"),
                Component.literal("§7ke file §fdungeon_rewards.json§7."),
                Component.literal("§a► Klik untuk Menyimpan")
        )));
        container.setItem(7, saveItem);

        // Slot 8: Tutup Menu
        ItemStack closeItem = new ItemStack(Items.BARRIER);
        closeItem.set(DataComponents.CUSTOM_NAME, Component.literal("§c§l[❌ Tutup Menu]"));
        container.setItem(8, closeItem);

        // ================= ROW 1: WAVE SELECTOR (Slots 11..15) =================
        ItemStack border = new ItemStack(Items.STAINED_GLASS_PANE.black());
        border.set(DataComponents.CUSTOM_NAME, Component.literal("§8---"));
        container.setItem(9, border);
        container.setItem(10, border);
        container.setItem(16, border);
        container.setItem(17, border);

        Item[] waveMaterials = { Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.NETHERITE_SCRAP, Items.NETHERITE_INGOT };
        for (int w = 1; w <= 5; w++) {
            ItemStack waveIcon = new ItemStack(waveMaterials[w - 1]);
            boolean isCurrentWave = (wave == w);
            String label = (w == 5) ? "Wave 5 (Boss Final)" : ("Wave " + w);
            waveIcon.set(DataComponents.CUSTOM_NAME, Component.literal((isCurrentWave ? "§a✔ " : "§e") + "§l" + label));
            waveIcon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, isCurrentWave);
            waveIcon.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§7Klik untuk mengelola drop hadiah " + label + ".")
            )));
            container.setItem(10 + w, waveIcon);
        }

        // ================= ROWS 2..4: LOOT ITEMS GRID (Slots 18..44) =================
        int startIndex = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int entryIndex = startIndex + i;
            int targetSlot = 18 + i;
            if (entryIndex < entries.size()) {
                DungeonLootEntry entry = entries.get(entryIndex);
                container.setItem(targetSlot, entry.getDisplayStack());
            } else {
                container.setItem(targetSlot, ItemStack.EMPTY);
            }
        }

        // ================= ROW 5: FOOTER & CONTROLS =================
        // Slot 45: Kembali ke Pemilih Dungeon
        ItemStack backBtn = new ItemStack(Items.BARRIER);
        backBtn.set(DataComponents.CUSTOM_NAME, Component.literal("§c§l[◀ Kembali ke Pemilih Dungeon]"));
        backBtn.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Kembali ke menu 3 pilihan tingkat kesulitan dungeon."),
                Component.literal("§c► Klik untuk Kembali")
        )));
        container.setItem(45, backBtn);

        // Slot 46: Tambah Item Baru
        ItemStack addItem = new ItemStack(Items.WRITABLE_BOOK);
        addItem.set(DataComponents.CUSTOM_NAME, Component.literal("§a§l[➕ Tambah Item Hadiah]"));
        addItem.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§e► Klik Kiri: §7Gunakan item di Tangan Utama"),
                Component.literal("§e► Klik Kanan: §7Ketik Resource ID di Chat"),
                Component.literal("§8Contoh ID: takasha_rpg:rpg_coin, mmo:SWORD:CUTLASS")
        )));
        container.setItem(46, addItem);

        // Slot 47: Halaman Sebelumnya
        if (page > 0) {
            ItemStack prev = new ItemStack(Items.ARROW);
            prev.set(DataComponents.CUSTOM_NAME, Component.literal("§e◀ Halaman Sebelumnya §8(" + page + "/" + (maxPage + 1) + ")"));
            container.setItem(47, prev);
        }

        // Slot 49: Reset Wave ke Default
        ItemStack resetWave = new ItemStack(Items.CAULDRON);
        resetWave.set(DataComponents.CUSTOM_NAME, Component.literal("§c§l[⚠ Reset Wave ke Default]"));
        resetWave.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Mengembalikan seluruh item wave " + wave),
                Component.literal("§7ke konfigurasi autentik bawaan."),
                Component.literal("§c► Klik untuk Melakukan Reset")
        )));
        container.setItem(49, resetWave);

        // Slot 51: Halaman Berikutnya
        if (page < maxPage) {
            ItemStack next = new ItemStack(Items.ARROW);
            next.set(DataComponents.CUSTOM_NAME, Component.literal("§eHalaman Berikutnya ▶ §8(" + (page + 2) + "/" + (maxPage + 1) + ")"));
            container.setItem(51, next);
        }

        // Slot 53: Ringkasan Drop
        ItemStack summary = new ItemStack(Items.PAPER);
        summary.set(DataComponents.CUSTOM_NAME, Component.literal("§b§l[📜 Ringkasan Drop Wave]"));
        summary.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Cetak seluruh ringkasan peluang drop"),
                Component.literal("§7Wave " + wave + " ke jendela chat pemain."),
                Component.literal("§b► Klik untuk Mencetak Ringkasan")
        )));
        container.setItem(53, summary);
    }

    private static void handleClick(ServerPlayer player, DungeonTier tier, int wave, List<DungeonLootEntry> entries, int page, int maxPage, int slotId, int button, ContainerInput clickType) {
        // --- Tab Tier Selection ---
        if (slotId == 0) {
            open(player, DungeonTier.COMMON, wave, 0);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
            return;
        }
        if (slotId == 1) {
            open(player, DungeonTier.NIGHTMARE, wave, 0);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
            return;
        }
        if (slotId == 2) {
            open(player, DungeonTier.SAKURA_BOSS, wave, 0);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
            return;
        }

        // --- Global Actions ---
        if (slotId == 6) {
            // Simulasi Roll
            List<ItemStack> rolled = DungeonRewardConfigManager.getInstance().simulateRoll(tier, wave, RNG);
            player.sendSystemMessage(Component.literal("§6§l=== HASIL SIMULASI ROLL (Wave " + wave + " - " + tier.getDisplayName() + ") ==="));
            if (rolled.isEmpty()) {
                player.sendSystemMessage(Component.literal("§7(Tidak ada item yang berhasil di-roll / ZONK)"));
            } else {
                for (ItemStack st : rolled) {
                    player.sendSystemMessage(Component.literal("§a► §e" + st.getCount() + "x §f" + st.getHoverName().getString()));
                }
            }
            player.sendSystemMessage(Component.literal("§6§l================================================="));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.2f);
            return;
        }
        if (slotId == 7) {
            // Simpan JSON
            DungeonRewardConfigManager.getInstance().saveToDisk();
            player.sendSystemMessage(Component.literal("§8[§eDungeon Rewards§8] §aKonfigurasi berhasil disimpan ke file config/takasha/dungeon_rewards.json!"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0f, 1.5f);
            return;
        }
        if (slotId == 8) {
            player.closeContainer();
            return;
        }

        // --- Wave Selection (Slots 11..15) ---
        if (slotId >= 11 && slotId <= 15) {
            int selectedWave = slotId - 10;
            open(player, tier, selectedWave, 0);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
            return;
        }

        // --- Pagination ---
        if (slotId == 47 && page > 0) {
            open(player, tier, wave, page - 1);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
            return;
        }
        if (slotId == 51 && page < maxPage) {
            open(player, tier, wave, page + 1);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
            return;
        }

        // --- Reset Wave ---
        if (slotId == 49) {
            DungeonRewardConfigManager.getInstance().resetWaveToDefault(tier, wave);
            player.sendSystemMessage(Component.literal("§8[§eDungeon Rewards§8] §eWave " + wave + " (" + tier.getDisplayName() + ") berhasil direset ke konfigurasi default bawaan."));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
            open(player, tier, wave, 0);
            return;
        }

        // --- Print Summary ---
        if (slotId == 53) {
            player.sendSystemMessage(Component.literal("§b§l=== RINGKASAN DROP HADIAH (Wave " + wave + " - " + tier.getDisplayName() + ") ==="));
            int idx = 1;
            for (DungeonLootEntry e : entries) {
                String amt = (e.getMinAmount() == e.getMaxAmount()) ? String.valueOf(e.getMinAmount()) : (e.getMinAmount() + "-" + e.getMaxAmount());
                player.sendSystemMessage(Component.literal("§7#" + (idx++) + " §f" + e.getId() + " §7| Peluang: §a" + String.format(Locale.ROOT, "%.1f%%", e.getChance() * 100.0f) + " §7| Jumlah: §e" + amt + (e.isScaleWithMultiplier() ? " §8(x Mult)" : "")));
            }
            player.sendSystemMessage(Component.literal("§b§l======================================================="));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 0.9f, 1.0f);
            return;
        }

        // --- Tombol Kembali ke Pemilih Dungeon ---
        if (slotId == 45) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
            DungeonTierSelectorMenu.open(player);
            return;
        }

        // --- Tambah Item Baru ---
        if (slotId == 46) {
            if (button == 0) { // Left Click: Use Mainhand Item
                ItemStack handItem = player.getMainHandItem();
                if (handItem.isEmpty()) {
                    player.sendSystemMessage(Component.literal("§8[§eDungeon Rewards§8] §cTangan utama Anda kosong! Pegang item yang ingin ditambahkan atau Klik Kanan untuk memasukkan ID secara manual."));
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
                    return;
                }

                String itemId;
                Integer cmd = null;
                String model = null;
                String customName = handItem.has(DataComponents.CUSTOM_NAME) ? handItem.get(DataComponents.CUSTOM_NAME).getString() : null;

                net.minecraft.world.item.component.CustomModelData cmdComp = handItem.get(DataComponents.CUSTOM_MODEL_DATA);
                if (cmdComp != null && !cmdComp.floats().isEmpty()) {
                    cmd = Math.round(cmdComp.floats().get(0));
                }
                Identifier itemModelId = handItem.get(DataComponents.ITEM_MODEL);
                if (itemModelId != null) {
                    model = itemModelId.toString();
                }

                java.util.Optional<MMOItemData> optMmo = MMOItemData.readFromStack(handItem);
                if (optMmo.isPresent()) {
                    MMOItemData d = optMmo.get();
                    itemId = "mmo:" + d.getType() + ":" + d.getId();
                } else {
                    itemId = BuiltInRegistries.ITEM.getKey(handItem.getItem()).toString();
                }

                DungeonLootEntry newEntry = new DungeonLootEntry(itemId, 0.50f, handItem.getCount(), handItem.getCount(), false, customName, cmd, model);
                DungeonRewardConfigManager.getInstance().addLootEntry(tier, wave, newEntry);
                player.sendSystemMessage(Component.literal("§8[§eDungeon Rewards§8] §aBerhasil menambahkan §e" + handItem.getHoverName().getString() + " §ake pool Wave " + wave + "!"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                open(player, tier, wave, page);
                return;
            } else { // Right Click: Prompt Chat for Item ID
                List<String> prompt = List.of(
                        "Masukkan Resource ID item baru yang ingin ditambahkan ke Wave " + wave + ":",
                        "Format standar: §fminecraft:diamond§7, §ftakasha_rpg:rpg_coin§7, atau §fmmo:SWORD:CUTLASS"
                );
                MMOChatEditionEngine.beginSession(player, "Add Dungeon Loot", "New Loot Item", prompt,
                        () -> open(player, tier, wave, page),
                        input -> {
                            String trimmed = input.trim();
                            if (trimmed.isEmpty()) {
                                player.sendSystemMessage(Component.literal("§8[§eDungeon Rewards§8] §cResource ID tidak boleh kosong!"));
                                return false;
                            }
                            // Validate identifier
                            if (trimmed.toLowerCase(Locale.ROOT).startsWith("mmo:")) {
                                String[] parts = trimmed.split(":");
                                if (parts.length < 2) {
                                    player.sendSystemMessage(Component.literal("§8[§eDungeon Rewards§8] §cFormat MMOItems salah! Gunakan format mmo:<TYPE>:<ID> atau mmo:<ID>."));
                                    return false;
                                }
                            } else {
                                Identifier id = Identifier.tryParse(trimmed.contains(":") ? trimmed : "minecraft:" + trimmed);
                                if (id == null || BuiltInRegistries.ITEM.getValue(id) == Items.AIR) {
                                    player.sendSystemMessage(Component.literal("§8[§eDungeon Rewards§8] §cItem dengan ID '" + trimmed + "' tidak ditemukan!"));
                                    return false;
                                }
                            }

                            DungeonLootEntry newEntry = new DungeonLootEntry(trimmed, 0.50f, 1, 1, false);
                            DungeonRewardConfigManager.getInstance().addLootEntry(tier, wave, newEntry);
                            player.sendSystemMessage(Component.literal("§8[§eDungeon Rewards§8] §aItem §e" + trimmed + " §aberhasil ditambahkan ke Wave " + wave + "!"));
                            open(player, tier, wave, page);
                            return true;
                        });
                return;
            }
        }

        // --- Grid Loot Item Clicks (Slots 18..44) ---
        if (slotId >= 18 && slotId <= 44) {
            int entryIndex = (page * PAGE_SIZE) + (slotId - 18);
            if (entryIndex >= entries.size()) return;

            DungeonLootEntry entry = entries.get(entryIndex);

            // Shift-Click (Quick Move) -> Delete Item
            if (clickType == ContainerInput.QUICK_MOVE || (button == 1 && player.isShiftKeyDown())) {
                DungeonRewardConfigManager.getInstance().removeLootEntry(tier, wave, entryIndex);
                player.sendSystemMessage(Component.literal("§8[§eDungeon Rewards§8] §cItem §e" + entry.getId() + " §cdihapus dari pool Wave " + wave + "."));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                open(player, tier, wave, page);
                return;
            }

            // Middle Click (Clone) -> Toggle Multiplier
            if (clickType == ContainerInput.CLONE || button == 2) {
                entry.setScaleWithMultiplier(!entry.isScaleWithMultiplier());
                player.sendSystemMessage(Component.literal("§8[§eDungeon Rewards§8] §7Skala multiplier untuk §e" + entry.getId() + " §7diubah menjadi: " + (entry.isScaleWithMultiplier() ? "§aAktif" : "§7Nonaktif")));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
                open(player, tier, wave, page);
                return;
            }

            // Left Click -> Edit Chance
            if (button == 0) {
                List<String> prompt = List.of(
                        "Mengubah peluang drop untuk item: §e" + entry.getId(),
                        "Masukkan nilai peluang baru dalam persentase (1..100) atau desimal (0.01..1.0):",
                        "Contoh: §f50%§7, §f25.5§7, §f0.40"
                );
                MMOChatEditionEngine.beginSession(player, "Edit Loot Chance", entry.getId(), prompt,
                        () -> open(player, tier, wave, page),
                        input -> {
                            String clean = input.replace("%", "").trim();
                            try {
                                float val = Float.parseFloat(clean);
                                if (val > 1.0f) {
                                    val /= 100.0f;
                                }
                                if (val <= 0.0f || val > 1.0f) {
                                    player.sendSystemMessage(Component.literal("§8[§eDungeon Rewards§8] §cPeluang harus berada dalam rentang 0.1% s.d. 100%!"));
                                    return false;
                                }
                                entry.setChance(val);
                                player.sendSystemMessage(Component.literal("§8[§eDungeon Rewards§8] §aPeluang drop §e" + entry.getId() + " §aberhasil diubah menjadi §e" + String.format(Locale.ROOT, "%.1f%%", val * 100.0f) + "!"));
                                open(player, tier, wave, page);
                                return true;
                            } catch (NumberFormatException e) {
                                player.sendSystemMessage(Component.literal("§8[§eDungeon Rewards§8] §cInput bukan angka yang valid!"));
                                return false;
                            }
                        });
                return;
            }

            // Right Click -> Edit Amount Range
            if (button == 1) {
                List<String> prompt = List.of(
                        "Mengubah rentang jumlah drop untuk item: §e" + entry.getId(),
                        "Masukkan angka tunggal atau rentang minimum-maksimum:",
                        "Contoh: §f1-5§7, §f10§7, §f64"
                );
                MMOChatEditionEngine.beginSession(player, "Edit Loot Amount", entry.getId(), prompt,
                        () -> open(player, tier, wave, page),
                        input -> {
                            String clean = input.trim();
                            try {
                                int min, max;
                                if (clean.contains("-")) {
                                    String[] parts = clean.split("-");
                                    min = Integer.parseInt(parts[0].trim());
                                    max = Integer.parseInt(parts[1].trim());
                                } else {
                                    min = Integer.parseInt(clean);
                                    max = min;
                                }
                                if (min < 1 || max < min) {
                                    player.sendSystemMessage(Component.literal("§8[§eDungeon Rewards§8] §cRentang tidak valid! Pastikan min >= 1 dan max >= min."));
                                    return false;
                                }
                                entry.setMinAmount(min);
                                entry.setMaxAmount(max);
                                String amountStr = (min == max) ? String.valueOf(min) : (min + " - " + max);
                                player.sendSystemMessage(Component.literal("§8[§eDungeon Rewards§8] §aRentang jumlah drop §e" + entry.getId() + " §aberhasil diubah menjadi §e" + amountStr + "!"));
                                open(player, tier, wave, page);
                                return true;
                            } catch (Exception e) {
                                player.sendSystemMessage(Component.literal("§8[§eDungeon Rewards§8] §cFormat tidak valid! Gunakan format seperti '1-5' atau '10'."));
                                return false;
                            }
                        });
            }
        }
    }
}
