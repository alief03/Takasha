package net.takasha.rpg.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;
import net.takasha.rpg.skills.AuraSkillType;
import net.takasha.rpg.skills.AuraTraitType;

import java.util.*;

/**
 * AuraSourcesMenu
 * Sub-GUI interaktif 54-slot untuk menampilkan seluruh sumber perolehan XP asli (Sources)
 * untuk setiap cabang skill AuraSkills dengan dukungan paginasi, pengurutan hopper,
 * dan Zero Cursor Displacement container reuse.
 */
public class AuraSourcesMenu {

    public enum SortMode {
        DEFAULT("Default (Bawaan)"),
        XP_DESC("XP Tertinggi"),
        NAME_ASC("Nama (A-Z)");

        private final String displayName;

        SortMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public SortMode next() {
            return switch (this) {
                case DEFAULT -> XP_DESC;
                case XP_DESC -> NAME_ASC;
                case NAME_ASC -> DEFAULT;
            };
        }
    }

    public record SourceEntry(Item item, String name, double xp, String unit, String description) {}

    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] GRID_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public static void open(ServerPlayer player, AuraSkillType skill, int page, SortMode sortMode) {
        List<SourceEntry> entries = getSortedSources(skill, sortMode);
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / ITEMS_PER_PAGE));
        int safePage = Math.max(1, Math.min(totalPages, page));

        // ZERO CURSOR DISPLACEMENT: Gunakan kembali 54-slot container yang sedang terbuka
        if (player.containerMenu instanceof ReadOnlyChestMenu currentMenu
                && currentMenu.getContainer() instanceof SimpleContainer curCont
                && curCont.getContainerSize() == 54) {
            populateItems(curCont, player, skill, safePage, totalPages, entries, sortMode);
            currentMenu.setSlotClickAction((slotId, button, clickType, pl) -> handleMenuClick(slotId, (ServerPlayer) pl, skill, safePage, totalPages, sortMode));
            currentMenu.broadcastChanges();
            return;
        }

        SimpleContainer container = new SimpleContainer(54);
        populateItems(container, player, skill, safePage, totalPages, entries, sortMode);

        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ReadOnlyChestMenu(
                        syncId,
                        inv,
                        container,
                        6,
                        (slotId, button, clickType, pl) -> handleMenuClick(slotId, (ServerPlayer) pl, skill, safePage, totalPages, sortMode)
                ),
                Component.literal("§8" + skill.getDisplayName() + " - Sumber XP (" + safePage + "/" + totalPages + ")")
        ));
    }

    private static void populateItems(SimpleContainer container, ServerPlayer player, AuraSkillType skill,
                                      int page, int totalPages, List<SourceEntry> entries, SortMode sortMode) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        double wisdomBonus = data.getTraitValue(AuraTraitType.EXPERIENCE_BONUS);

        // Fill background with black stained glass pane
        ItemStack filler = makeItem(Items.STAINED_GLASS_PANE.black(), "§8", List.of());
        for (int i = 0; i < 54; i++) {
            container.setItem(i, filler.copy());
        }

        // Slot 4: Info Skill Header
        List<Component> headerLore = new ArrayList<>();
        headerLore.add(Component.literal("§7Cabang Skill : " + skill.getColor() + skill.getDisplayName()));
        headerLore.add(Component.literal("§7Level Pemain  : §e" + data.getSkillLevel(skill)));
        headerLore.add(Component.literal("§7Total Sumber  : §b" + entries.size() + " aktivitas"));
        if (wisdomBonus > 0) {
            headerLore.add(Component.literal("§7Bonus Wisdom  : §a+" + String.format(Locale.ROOT, "%.1f", wisdomBonus) + "% XP"));
        }
        headerLore.add(Component.literal(""));
        headerLore.add(Component.literal("§eAktivitas di bawah ini memberikan XP secara instan!"));
        container.setItem(4, makeItem(Items.EXPERIENCE_BOTTLE, "§a§l✦ Sumber Pengalaman: " + skill.getDisplayName() + " ✦", headerLore));

        // Populate 28 Grid Slots
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(entries.size(), startIndex + ITEMS_PER_PAGE);

        for (int i = 0; i < GRID_SLOTS.length; i++) {
            int slotPos = GRID_SLOTS[i];
            int entryIndex = startIndex + i;
            if (entryIndex < endIndex) {
                SourceEntry entry = entries.get(entryIndex);
                double effectiveXp = entry.xp() * (1.0 + (wisdomBonus / 100.0));

                List<Component> lore = new ArrayList<>();
                lore.add(Component.literal("§7Sumber Aktivitas : §f" + entry.name()));
                lore.add(Component.literal("§7Perolehan XP     : §a+" + String.format(Locale.ROOT, "%.1f", effectiveXp) + " XP §7/ " + entry.unit()));
                if (wisdomBonus > 0) {
                    lore.add(Component.literal("§8(Base " + String.format(Locale.ROOT, "%.0f", entry.xp()) + " XP + " + String.format(Locale.ROOT, "%.1f", wisdomBonus) + "% Wisdom)"));
                }
                lore.add(Component.literal(""));
                lore.add(Component.literal("§eInformasi:"));
                lore.add(Component.literal(" §7" + entry.description()));

                container.setItem(slotPos, makeItem(entry.item(), "§e§l" + entry.name(), lore));
            } else {
                container.setItem(slotPos, makeItem(Items.STAINED_GLASS_PANE.gray(), "§8", List.of()));
            }
        }

        // Slot 45: Back Button
        container.setItem(45, makeItem(Items.ARROW, "§a§l← Kembali ke Progres Level",
                List.of(Component.literal("§7Klik untuk kembali ke bagan level " + skill.getDisplayName() + "."))));

        // Slot 48: Previous Page
        if (page > 1) {
            container.setItem(48, makeItem(Items.ARROW, "§6§l← Halaman Sebelumnya (" + (page - 1) + ")",
                    List.of(Component.literal("§7Klik untuk melihat sumber sebelumnya."))));
        } else {
            container.setItem(48, makeItem(Items.STAINED_GLASS_PANE.gray(), "§8Tidak Ada Halaman", List.of()));
        }

        // Slot 49: Sorter Button (Hopper)
        List<Component> sorterLore = new ArrayList<>();
        sorterLore.add(Component.literal("§7Mode Urutan Saat Ini: §e" + sortMode.getDisplayName()));
        sorterLore.add(Component.literal(""));
        sorterLore.add(Component.literal("§7Pilihan Urutan:"));
        sorterLore.add(Component.literal((sortMode == SortMode.DEFAULT ? " §a▶ " : " §8• ") + "Default (Bawaan)"));
        sorterLore.add(Component.literal((sortMode == SortMode.XP_DESC ? " §a▶ " : " §8• ") + "XP Tertinggi"));
        sorterLore.add(Component.literal((sortMode == SortMode.NAME_ASC ? " §a▶ " : " §8• ") + "Nama (A-Z)"));
        sorterLore.add(Component.literal(""));
        sorterLore.add(Component.literal("§eKlik untuk mengubah mode urutan!"));
        container.setItem(49, makeItem(Items.HOPPER, "§b§l✦ Urutkan Sumber XP ✦", sorterLore));

        // Slot 50: Next Page
        if (page < totalPages) {
            container.setItem(50, makeItem(Items.ARROW, "§6§lHalaman Berikutnya (" + (page + 1) + ") →",
                    List.of(Component.literal("§7Klik untuk melihat sumber berikutnya."))));
        } else {
            container.setItem(50, makeItem(Items.STAINED_GLASS_PANE.gray(), "§8Tidak Ada Halaman", List.of()));
        }
    }

    private static void handleMenuClick(int slotId, ServerPlayer player, AuraSkillType skill,
                                        int page, int totalPages, SortMode sortMode) {
        if (slotId == 45) {
            // Kembali ke AuraLevelProgressionMenu secara in-place
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
            AuraLevelProgressionMenu.open(player, skill, 1);
        } else if (slotId == 48 && page > 1) {
            // Previous Page
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
            open(player, skill, page - 1, sortMode);
        } else if (slotId == 49) {
            // Ganti Sort Mode
            SortMode nextMode = sortMode.next();
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.4f);
            open(player, skill, 1, nextMode);
        } else if (slotId == 50 && page < totalPages) {
            // Next Page
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
            open(player, skill, page + 1, sortMode);
        } else {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4f, 1.5f);
        }
    }

    private static List<SourceEntry> getSortedSources(AuraSkillType skill, SortMode sortMode) {
        List<SourceEntry> list = new ArrayList<>(getRawSources(skill));
        if (sortMode == SortMode.XP_DESC) {
            list.sort((a, b) -> Double.compare(b.xp(), a.xp()));
        } else if (sortMode == SortMode.NAME_ASC) {
            list.sort(Comparator.comparing(SourceEntry::name));
        }
        return list;
    }

    private static List<SourceEntry> getRawSources(AuraSkillType skill) {
        return switch (skill) {
            case FARMING -> List.of(
                    new SourceEntry(Items.WHEAT, "Gandum (Wheat)", 25.0, "panen", "Memanen tanaman gandum yang telah matang sempurna."),
                    new SourceEntry(Items.POTATO, "Kentang (Potato)", 25.0, "panen", "Memanen tanaman kentang yang telah matang."),
                    new SourceEntry(Items.CARROT, "Wortel (Carrot)", 25.0, "panen", "Memanen tanaman wortel yang telah matang."),
                    new SourceEntry(Items.BEETROOT, "Bit (Beetroot)", 25.0, "panen", "Memanen tanaman umbi bit merah yang matang."),
                    new SourceEntry(Items.NETHER_WART, "Nether Wart", 25.0, "panen", "Memanen Nether Wart yang matang di Nether/kebun."),
                    new SourceEntry(Items.MELON, "Semangka (Melon Block)", 20.0, "blok", "Menghancurkan buah semangka yang tumbuh."),
                    new SourceEntry(Items.PUMPKIN, "Labu (Pumpkin)", 20.0, "blok", "Memanen buah labu kuning dari kebun."),
                    new SourceEntry(Items.SUGAR_CANE, "Tebu (Sugar Cane)", 15.0, "batang", "Memotong batang tebu di tepi perairan."),
                    new SourceEntry(Items.COCOA_BEANS, "Biji Kakao (Cocoa)", 20.0, "panen", "Memetik biji kakao matang dari pohon rimba."),
                    new SourceEntry(Items.CACTUS, "Kaktus (Cactus)", 15.0, "panen", "Memanen batang kaktus di gurun atau kebun."),
                    new SourceEntry(Items.SWEET_BERRIES, "Sweet Berries", 15.0, "semak", "Memetik buah beri manis dari semak berduri."),
                    new SourceEntry(Items.KELP, "Rumput Laut (Kelp)", 10.0, "batang", "Memotong batang rumput laut di kedalaman laut."),
                    new SourceEntry(Items.HAY_BLOCK, "Bale Jerami (Hay Bale)", 30.0, "crafting", "Merakit ikatan jerami atau mengurai gandum."),
                    new SourceEntry(Items.APPLE, "Apel Merah (Apple)", 20.0, "pungut", "Mendapatkan buah apel gugur dari pohon ek."),
                    new SourceEntry(Items.GOLDEN_CARROT, "Wortel Emas (Golden Carrot)", 35.0, "konsumsi", "Memakan wortel emas berkilau untuk nutrisi."),
                    new SourceEntry(Items.PITCHER_PLANT, "Pitcher Plant", 40.0, "panen", "Menumbuhkan dan memanen tanaman purba pitcher."),
                    new SourceEntry(Items.TORCHFLOWER, "Torchflower", 40.0, "panen", "Memanen bunga obor purba hasil carian sniffer."),
                    new SourceEntry(Items.SNIFFER_EGG, "Telur Sniffer", 50.0, "panen", "Menetaskan atau menemukan telur purba sniffer.")
            );

            case FORAGING -> List.of(
                    new SourceEntry(Items.OAK_LOG, "Kayu Ek (Oak Log)", 20.0, "blok", "Menebang pohon ek di hutan atau dataran."),
                    new SourceEntry(Items.BIRCH_LOG, "Kayu Birch (Birch Log)", 20.0, "blok", "Menebang pohon birch berbatang putih."),
                    new SourceEntry(Items.SPRUCE_LOG, "Kayu Spruce (Spruce Log)", 20.0, "blok", "Menebang pohon cemara spruce di taiga."),
                    new SourceEntry(Items.JUNGLE_LOG, "Kayu Rimba (Jungle Log)", 20.0, "blok", "Menebang pohon raksasa di hutan rimba tropis."),
                    new SourceEntry(Items.ACACIA_LOG, "Kayu Akasia (Acacia Log)", 20.0, "blok", "Menebang pohon akasia jingga di sabana."),
                    new SourceEntry(Items.DARK_OAK_LOG, "Kayu Dark Oak", 20.0, "blok", "Menebang pohon kayu gelap di kanopi lebat."),
                    new SourceEntry(Items.MANGROVE_LOG, "Kayu Bakau (Mangrove)", 25.0, "blok", "Menebang kayu bakau di rawa berlumpur."),
                    new SourceEntry(Items.CHERRY_LOG, "Kayu Sakura (Cherry Log)", 20.0, "blok", "Menebang pohon sakura bunga merah muda."),
                    new SourceEntry(Items.PALE_OAK_LOG, "Kayu Pale Oak", 25.0, "blok", "Menebang pohon pale oak di taman sunyi."),
                    new SourceEntry(Items.CRIMSON_STEM, "Crimson Stem", 25.0, "blok", "Menebang jamur crimson merah di dunia Nether."),
                    new SourceEntry(Items.WARPED_STEM, "Warped Stem", 25.0, "blok", "Menebang jamur warped toska di dunia Nether."),
                    new SourceEntry(Items.MANGROVE_ROOTS, "Akar Bakau (Roots)", 15.0, "blok", "Menggali akar bakau tebal yang merambat."),
                    new SourceEntry(Items.OAK_SAPLING, "Menanam Bibit Pohon", 10.0, "tanam", "Menanam kembali bibit pohon agar hutan lestari."),
                    new SourceEntry(Items.MANGROVE_PROPAGULE, "Propagul Bakau", 15.0, "petik", "Memetik propagul gantung dari daun bakau.")
            );

            case MINING -> List.of(
                    new SourceEntry(Items.COAL_ORE, "Bijih Batubara (Coal)", 15.0, "bijih", "Menambang batubara di pegunungan & gua."),
                    new SourceEntry(Items.COPPER_ORE, "Bijih Tembaga (Copper)", 15.0, "bijih", "Menambang urat tembaga di bawah tanah."),
                    new SourceEntry(Items.IRON_ORE, "Bijih Besi (Iron)", 25.0, "bijih", "Menambang besi mentah untuk alat & zirah."),
                    new SourceEntry(Items.GOLD_ORE, "Bijih Emas (Gold)", 35.0, "bijih", "Menambang emas berkilau di kedalaman."),
                    new SourceEntry(Items.REDSTONE_ORE, "Bijih Redstone", 30.0, "bijih", "Menambang debu redstone berenergi tinggi."),
                    new SourceEntry(Items.LAPIS_ORE, "Bijih Lapis Lazuli", 30.0, "bijih", "Menambang batu biru sihir lapis lazuli."),
                    new SourceEntry(Items.DIAMOND_ORE, "Bijih Berlian (Diamond)", 60.0, "bijih", "Menambang kristal berlian berharga."),
                    new SourceEntry(Items.DEEPSLATE_DIAMOND_ORE, "Deepslate Diamond", 80.0, "bijih", "Menambang berlian di lapisan batu dalam tebal."),
                    new SourceEntry(Items.EMERALD_ORE, "Bijih Zamrud (Emerald)", 75.0, "bijih", "Menambang zamrud langka di pegunungan."),
                    new SourceEntry(Items.NETHER_QUARTZ_ORE, "Nether Quartz Ore", 25.0, "bijih", "Menambang kristal kuarsa putih di Nether."),
                    new SourceEntry(Items.NETHER_GOLD_ORE, "Nether Gold Ore", 25.0, "bijih", "Menambang serpihan emas dari dinding Nether."),
                    new SourceEntry(Items.ANCIENT_DEBRIS, "Ancient Debris (Netherite)", 150.0, "bijih", "Menemukan puing kuno terkuat di kedalaman Nether."),
                    new SourceEntry(Items.AMETHYST_CLUSTER, "Gugusan Amethyst", 25.0, "panen", "Memanen kristal kecubung ungu di geoda bawah tanah."),
                    new SourceEntry(Items.OBSIDIAN, "Batu Obsidian", 40.0, "blok", "Menambang obsidian hitam keras tahan ledakan."),
                    new SourceEntry(Items.CRYING_OBSIDIAN, "Crying Obsidian", 45.0, "blok", "Menambang obsidian menangis bercahaya ungu."),
                    new SourceEntry(Items.STONE, "Batu Biasa (Stone)", 5.0, "blok", "Menambang batu biasa di bawah tanah."),
                    new SourceEntry(Items.DEEPSLATE, "Batu Dalam (Deepslate)", 8.0, "blok", "Menambang batu keras di lapisan bawah dunia."),
                    new SourceEntry(Items.END_STONE, "Batu End (End Stone)", 10.0, "blok", "Menambang daratan batu pulau The End.")
            );

            case FISHING -> List.of(
                    new SourceEntry(Items.COD, "Ikan Kod Mentah", 30.0, "ekor", "Memancing ikan kod di lautan atau sungai."),
                    new SourceEntry(Items.SALMON, "Ikan Salmon Mentah", 35.0, "ekor", "Menangkap salmon segar di perairan jernih."),
                    new SourceEntry(Items.TROPICAL_FISH, "Ikan Tropis Eksotis", 45.0, "ekor", "Menangkap ikan tropis berwarna cerah."),
                    new SourceEntry(Items.PUFFERFISH, "Ikan Buntal (Pufferfish)", 50.0, "ekor", "Menangkap ikan buntal beracun di laut hangat."),
                    new SourceEntry(Items.BOW, "Busur Air Terendam", 60.0, "harta", "Mendapatkan busur bertuah dari dasar air."),
                    new SourceEntry(Items.ENCHANTED_BOOK, "Kitab Sihir Pusaka", 100.0, "harta", "Mendapatkan buku sihir langka dari memancing."),
                    new SourceEntry(Items.FISHING_ROD, "Joran Pancing Kuno", 50.0, "harta", "Mendapatkan joran pancing bertuah dari air."),
                    new SourceEntry(Items.NAME_TAG, "Label Nama (Name Tag)", 80.0, "harta", "Menemukan label nama pusaka dasar danau."),
                    new SourceEntry(Items.SADDLE, "Pelana Kuda (Saddle)", 80.0, "harta", "Menemukan pelana kulit dari pusaka laut."),
                    new SourceEntry(Items.NAUTILUS_SHELL, "Cangkang Nautilus", 120.0, "harta", "Menangkap cangkang nautilus laut dalam."),
                    new SourceEntry(Items.LILY_PAD, "Daun Teratai (Lily Pad)", 25.0, "tangkapan", "Menarik daun teratai mengapung di danau."),
                    new SourceEntry(Items.LEATHER_BOOTS, "Sepatu Butut Hanyut", 20.0, "sampah", "Menarik sampah sepatu tua dari perairan.")
            );

            case EXCAVATION -> List.of(
                    new SourceEntry(Items.DIRT, "Tanah Biasa (Dirt)", 5.0, "blok", "Menggali tanah lapisan atas bumi."),
                    new SourceEntry(Items.GRASS_BLOCK, "Blok Rumput (Grass)", 8.0, "blok", "Menggali tanah berumput segar."),
                    new SourceEntry(Items.COARSE_DIRT, "Tanah Kasar (Coarse Dirt)", 6.0, "blok", "Menggali tanah berbatu tanpa rumput."),
                    new SourceEntry(Items.PODZOL, "Tanah Hutan (Podzol)", 10.0, "blok", "Menggali tanah kaya humus di hutan taiga."),
                    new SourceEntry(Items.SAND, "Pasir Kuning (Sand)", 6.0, "blok", "Menggali pasir di pantai atau gurun pasir."),
                    new SourceEntry(Items.RED_SAND, "Pasir Merah (Red Sand)", 8.0, "blok", "Menggali pasir merah di ngarai badlands."),
                    new SourceEntry(Items.GRAVEL, "Kerikil Kasar (Gravel)", 8.0, "blok", "Menggali tumpukan batu kerikil berbutir."),
                    new SourceEntry(Items.CLAY, "Lempung Halus (Clay)", 15.0, "blok", "Menggali endapan lempung di dasar sungai."),
                    new SourceEntry(Items.MUD, "Lumpur Rawa (Mud)", 10.0, "blok", "Menggali lumpur basah di rawa bakau."),
                    new SourceEntry(Items.SOUL_SAND, "Pasir Jiwa (Soul Sand)", 12.0, "blok", "Menggali pasir jiwa yang memperlambat langkah."),
                    new SourceEntry(Items.SOUL_SOIL, "Tanah Jiwa (Soul Soil)", 12.0, "blok", "Menggali tanah jiwa biru di Nether."),
                    new SourceEntry(Items.SUSPICIOUS_SAND, "Pasir Arkeologi Purba", 50.0, "sikat", "Menyikat pasir purba dan menemukan artefak kuno.")
            );

            case ARCHERY -> List.of(
                    new SourceEntry(Items.SKELETON_SKULL, "Tembak Skeleton", 35.0, "tumbang", "Menembak jatuh skeleton dari jarak jauh."),
                    new SourceEntry(Items.ARROW, "Tembak Stray Dingin", 40.0, "tumbang", "Menembak panah es stray di padang salju."),
                    new SourceEntry(Items.SPECTRAL_ARROW, "Tembak Bogged Rawa", 40.0, "tumbang", "Menembak skeleton berlumut beracun di rawa."),
                    new SourceEntry(Items.CROSSBOW, "Tembak Pillager", 45.0, "tumbang", "Menembak penyamun bersenjata crossbow."),
                    new SourceEntry(Items.POTION, "Tembak Penyihir (Witch)", 50.0, "tumbang", "Menembak penyihir sebelum meminum ramuannya."),
                    new SourceEntry(Items.GUNPOWDER, "Tembak Creeper Jarak Jauh", 45.0, "tumbang", "Melenyapkan creeper sebelum mendekat."),
                    new SourceEntry(Items.GHAST_TEAR, "Tembak Ghast Melayang", 60.0, "tumbang", "Menembak jatuh hantu ghast di langit Nether."),
                    new SourceEntry(Items.PHANTOM_MEMBRANE, "Tembak Phantom Malam", 50.0, "tumbang", "Menembak monster terbang malam di angkasa."),
                    new SourceEntry(Items.BLAZE_ROD, "Tembak Blaze Berputar", 55.0, "tumbang", "Menembak monster api di benteng Nether."),
                    new SourceEntry(Items.SHULKER_SHELL, "Tembak Shulker Peluru", 60.0, "tumbang", "Menembak shulker saat cangkangnya terbuka."),
                    new SourceEntry(Items.DRAGON_HEAD, "Tembak Ender Dragon", 500.0, "tumbang", "Menembakkan anak panah ke naga penguasa End."),
                    new SourceEntry(Items.TARGET, "Tembakan Jauh (>30 Blok)", 75.0, "snipe", "Tembakan presisi jarak jauh mengenai sasaran.")
            );

            case DEFENSE -> List.of(
                    new SourceEntry(Items.SHIELD, "Tangkis Serangan Perisai", 25.0, "tangkis", "Menahan pukulan musuh dengan perisai kayu/besi."),
                    new SourceEntry(Items.ZOMBIE_HEAD, "Tahan Pukulan Zombie", 15.0, "pukulan", "Menahan hantaman fisik dari zombie pemangsa."),
                    new SourceEntry(Items.TNT, "Tahan Ledakan Creeper", 40.0, "ledakan", "Bertahan hidup dari ledakan dahsyat creeper/tnt."),
                    new SourceEntry(Items.SPECTRAL_ARROW, "Tahan Hujan Panah", 20.0, "tangkis", "Menahan lesatan proyektil panah musuh."),
                    new SourceEntry(Items.BLAZE_POWDER, "Tahan Semburan Api", 20.0, "ketahanan", "Menahan kerusakan api atau lahar panas."),
                    new SourceEntry(Items.WITHER_SKELETON_SKULL, "Tahan Kutukan Wither", 45.0, "ketahanan", "Bertahan dari efek busuk dan layu wither."),
                    new SourceEntry(Items.SCULK_CATALYST, "Tahan Benturan Warden", 100.0, "ketahanan", "Bertahan hidup dari hantaman dahsyat sipir purba."),
                    new SourceEntry(Items.TOTEM_OF_UNDYING, "Selamat dari Kematian Fatal", 150.0, "selamat", "Memicu anugerah totem penyelamat nyawa.")
            );

            case FIGHTING -> List.of(
                    new SourceEntry(Items.IRON_SWORD, "Tebas Monster Zombie", 30.0, "tumbang", "Menumbangkan zombie dalam duel jarak dekat."),
                    new SourceEntry(Items.BONE, "Tebas Kerangka Skeleton", 30.0, "tumbang", "Memecahkan tulang skeleton dengan pedang/kapak."),
                    new SourceEntry(Items.SPIDER_EYE, "Tebas Laba-Laba Gua", 25.0, "tumbang", "Membasmi laba-laba berbisa di sarang tambang."),
                    new SourceEntry(Items.GUNPOWDER, "Tebas Cepat Creeper", 35.0, "tumbang", "Menebas creeper sebelum sempat meledak."),
                    new SourceEntry(Items.ENDER_PEARL, "Tebas Monster Enderman", 60.0, "tumbang", "Menaklukkan enderman dalam duel tatap muka."),
                    new SourceEntry(Items.EMERALD, "Tebas Kapten Penyamun", 50.0, "tumbang", "Menumbangkan kapten penyamun raid bendera."),
                    new SourceEntry(Items.TOTEM_OF_UNDYING, "Tebas Penyihir Evoker", 90.0, "tumbang", "Membunuh pemanggil monster taring evoker."),
                    new SourceEntry(Items.NETHERITE_SWORD, "Tebas Piglin Brute", 80.0, "tumbang", "Mengalahkan pejuang elit kapak benteng emas."),
                    new SourceEntry(Items.NETHER_STAR, "Tebas Bos Wither", 400.0, "tumbang", "Menumbangkan monster berkepala tiga wither."),
                    new SourceEntry(Items.ECHO_SHARD, "Tebas Sipir Purba Warden", 350.0, "tumbang", "Menumbangkan makhluk terkuat kota kuno deep dark."),
                    new SourceEntry(Items.GOLDEN_SWORD, "Serangan Pertama (First Strike)", 25.0, "serangan", "Memberikan tebasan pembuka ke musuh darah penuh."),
                    new SourceEntry(Items.DIAMOND_SWORD, "Tebasan Kritis Lompat (Crit)", 20.0, "serangan", "Mendaratkan tebasan berpartikel kritis.")
            );

            case AGILITY -> List.of(
                    new SourceEntry(Items.LEATHER_BOOTS, "Berlari Cepat (Sprint)", 15.0, "50 blok", "Berlari kencang melintasi berbagai medan dunia."),
                    new SourceEntry(Items.RABBIT_FOOT, "Melompat Rintangan Parkour", 10.0, "lompat", "Melompati jurang dan balok bertingkat."),
                    new SourceEntry(Items.TURTLE_HELMET, "Berenang Cepat di Air", 20.0, "renang", "Berenang menembus arus sungai dan samudra."),
                    new SourceEntry(Items.ELYTRA, "Meluncur Sayap (Gliding)", 35.0, "terbang", "Meluncur di angkasa menggunakan sayap elytra."),
                    new SourceEntry(Items.FEATHER, "Mendarat Selamat dari Ketinggian", 25.0, "pendaratan", "Jatuh berguling tanpa mengalami patah tulang."),
                    new SourceEntry(Items.POTION, "Berlari dengan Buff Speed", 20.0, "lari", "Melesat kencang di bawah pengaruh ramuan cepat.")
            );

            case ALCHEMY -> List.of(
                    new SourceEntry(Items.BREWING_STAND, "Meracik Ramuan Awkward", 25.0, "ramuan", "Memasak bahan dasar ramuan pada brewing stand."),
                    new SourceEntry(Items.POTION, "Meracik Ramuan Penyembuh", 35.0, "ramuan", "Membuat ramuan pemulih kesehatan instan."),
                    new SourceEntry(Items.BLAZE_POWDER, "Meracik Ramuan Kekuatan", 40.0, "ramuan", "Membuat ramuan pendongkrak tenaga tempur."),
                    new SourceEntry(Items.SUGAR, "Meracik Ramuan Kelincahan", 35.0, "ramuan", "Membuat ramuan pelari kilat berkhasiat."),
                    new SourceEntry(Items.MAGMA_CREAM, "Meracik Ramuan Tahan Api", 40.0, "ramuan", "Membuat elixir pelindung lahar membara."),
                    new SourceEntry(Items.GOLDEN_APPLE, "Memakan Apel Emas", 50.0, "konsumsi", "Menyantap buah apel bertabur emas murni."),
                    new SourceEntry(Items.ENCHANTED_GOLDEN_APPLE, "Apel Emas Bertuah Kuno", 150.0, "konsumsi", "Menyantap apel sihir para dewa pencipta.")
            );

            case ENCHANTING -> List.of(
                    new SourceEntry(Items.ENCHANTING_TABLE, "Sihir Meja Tingkat Rendah", 20.0, "enchant", "Memberikan sihir level 1 s.d. 10 pada item."),
                    new SourceEntry(Items.BOOKSHELF, "Sihir Meja Tingkat Menengah", 45.0, "enchant", "Memberikan sihir level 11 s.d. 20 pada item."),
                    new SourceEntry(Items.LAPIS_LAZULI, "Sihir Meja Tingkat Tinggi (30)", 80.0, "enchant", "Memberikan sihir sakral tingkat 30 maksimum."),
                    new SourceEntry(Items.ANVIL, "Tempa & Gabung Paron (Anvil)", 50.0, "tempa", "Menggabungkan buku sihir ke senjata di paron."),
                    new SourceEntry(Items.GRINDSTONE, "Pembersihan Sihir (Grindstone)", 30.0, "disenchant", "Menghapus sihir dan menyerap residu energinya."),
                    new SourceEntry(Items.EXPERIENCE_BOTTLE, "Teguk Botol Pengalaman", 25.0, "botol", "Memecahkan botol berkilau cairan pengalaman."),
                    new SourceEntry(Items.ENCHANTED_BOOK, "Drag & Drop Custom Enchant", 60.0, "aplikasi", "Menerapkan buku sihir langka langsung ke zirah.")
            );
        };
    }

    private static ItemStack makeItem(Item item, String displayName, List<Component> lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(displayName));
        if (!lore.isEmpty()) {
            stack.set(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(lore));
        }
        return stack;
    }
}
