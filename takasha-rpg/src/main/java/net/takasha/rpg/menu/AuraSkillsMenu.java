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
import net.takasha.rpg.skills.AuraSkillType;

import java.util.ArrayList;
import java.util.List;

/**
 * AuraSkillsMenu
 * GUI Utama 11 Cabang Kemampuan AuraSkills 2.4.0 (54-slot chest).
 * Menampilkan profil pemain, 11 skill interaktif yang membuka GUI Level Progression saat diklik,
 * ringkasan 8 kemampuan aktif, dan tombol akses menu 9 atribut stat.
 *
 * Mengimplementasikan In-Place Container Reuse agar kursor mouse tidak melompat ke tengah layar.
 */
public class AuraSkillsMenu {

    public static void open(ServerPlayer player) {
        // ZERO CURSOR DISPLACEMENT: Reuse existing 54-slot container if already open
        if (player.containerMenu instanceof ReadOnlyChestMenu currentMenu
                && currentMenu.getContainer() instanceof SimpleContainer curCont
                && curCont.getContainerSize() == 54) {
            populateItems(curCont, player);
            currentMenu.setSlotClickAction((slotId, button, clickType, pl) -> handleMenuClick(slotId, (ServerPlayer) pl));
            currentMenu.broadcastChanges();
            return;
        }

        SimpleContainer container = new SimpleContainer(54);
        populateItems(container, player);

        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ReadOnlyChestMenu(
                        syncId,
                        inv,
                        container,
                        6,
                        (slotId, button, clickType, pl) -> handleMenuClick(slotId, (ServerPlayer) pl)
                ),
                Component.literal("§6§l✦ AuraSkills — 11 Cabang Kemampuan ✦")
        ));
    }

    private static void populateItems(SimpleContainer container, ServerPlayer player) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);

        // Fill background with Dark / Gray glass
        ItemStack filler = makeItem(Items.STAINED_GLASS_PANE.gray(), "§8", List.of());
        for (int i = 0; i < 54; i++) {
            container.setItem(i, filler.copy());
        }

        // Decorative Cyan frames
        ItemStack frame = makeItem(Items.STAINED_GLASS_PANE.cyan(), "§b✦", List.of());
        int[] frameSlots = new int[]{0, 1, 7, 8, 9, 17, 36, 44, 45, 46, 52, 53};
        for (int s : frameSlots) {
            container.setItem(s, frame.copy());
        }

        // Slot 4: Player Head Profile
        List<Component> profileLore = new ArrayList<>();
        profileLore.add(Component.literal("§7Nama: §e" + player.getScoreboardName()));
        profileLore.add(Component.literal("§7Gelar: §d" + (data.getActiveTitle().isEmpty() ? "§7(Belum Ada)" : data.getActiveTitle())));
        profileLore.add(Component.literal("§8§m------------------------"));
        profileLore.add(Component.literal("§7Total Level: §a§l" + data.getTotalSkillLevel() + " §7(Rata-rata: §e" + (data.getTotalSkillLevel() / 11) + "§7)"));
        profileLore.add(Component.literal("§7Saldo Uang: §a§l$" + String.format("%,.2f", data.getBalance())));
        profileLore.add(Component.literal("§7Koin Gems: §b§l" + data.getGems() + " Gems ♦"));
        profileLore.add(Component.literal("§7Darah (HP): §c§l" + String.format("%.0f/%.0f HP", player.getHealth(), player.getMaxHealth())));
        profileLore.add(Component.literal("§7Mana: §b§l" + String.format("%.0f/%.0f", data.getCurrentMana(), data.getMaxMana()) + " §7(+1.5/s)"));
        profileLore.add(Component.literal("§8§m------------------------"));
        profileLore.add(Component.literal("§aJelajahi dunia, lakukan aktivitas,"));
        profileLore.add(Component.literal("§adan perkuat kemampuan pengembaramu!"));

        ItemStack profileItem = makeItem(Items.PLAYER_HEAD, "§6§l✦ PROFIL PENGEMBARA ✦", profileLore);
        container.setItem(4, profileItem);

        // Row 2: Gathering & Combat Skills (Slots 19-25)
        container.setItem(19, makeSkillItem(data, AuraSkillType.FARMING, Items.WHEAT,
                "Panen gandum, wortel, kentang, melon, tebu",
                "§6🌱 Replenish §7(Shift+RClick Cangkul -> Panen Tanam Ulang Otomatis)"));
        container.setItem(20, makeSkillItem(data, AuraSkillType.FORAGING, Items.OAK_LOG,
                "Tebang semua varian kayu pohon",
                "§6🪓 Treecapitator §7(Shift+RClick Kapak -> Tumbangkan Seluruh Pohon)"));
        container.setItem(21, makeSkillItem(data, AuraSkillType.MINING, Items.DIAMOND_PICKAXE,
                "Tambang bijih mineral & bebatuan bawah tanah",
                "§6⛏ Speed Mine §7(Shift+RClick Beliung -> Haste X selama 10s)"));
        container.setItem(22, makeSkillItem(data, AuraSkillType.FISHING, Items.FISHING_ROD,
                "Pancing ikan & harta karun di perairan",
                "§6🎣 Sharp Hook §7(Shift+RClick Pancingan -> Tarik Musuh & Berikan Damage)"));
        container.setItem(23, makeSkillItem(data, AuraSkillType.EXCAVATION, Items.DIAMOND_SHOVEL,
                "Gali tanah, pasir, kerikil, dan lempung",
                "§6⛏ Terraform §7(Shift+RClick Sekop -> Gali Cepat Area 3x3)"));
        container.setItem(24, makeSkillItem(data, AuraSkillType.ARCHERY, Items.BOW,
                "Tumbangkan musuh dari kejauhan dengan panah",
                "§a🏹 Charged Shot §7(Shift+RClick Busur -> Tembakan Piercing Shockwave)"));
        container.setItem(25, makeSkillItem(data, AuraSkillType.DEFENSE, Items.SHIELD,
                "Tahan serangan damage dari musuh",
                "§e🛡 Absorption §7(Shift+RClick Perisai / Blokir -> +4 Golden Hearts)"));

        // Row 3: Fighting, Agility, Alchemy & Enchanting (Slots 29, 30, 32, 33)
        container.setItem(29, makeSkillItem(data, AuraSkillType.FIGHTING, Items.DIAMOND_SWORD,
                "Kalahkan monster & entitas musuh melee",
                "§c⚔ Lightning Blade §7(Shift+RClick Pedang -> Sambaran Petir Berantai)"));
        container.setItem(30, makeSkillItem(data, AuraSkillType.AGILITY, Items.FEATHER,
                "Sprint berlari, berenang, terbang elytra & jatuh",
                "§b⚡ Fleet Foot §7(Peningkatan kecepatan gerak pasif)"));
        container.setItem(32, makeSkillItem(data, AuraSkillType.ALCHEMY, Items.BREWING_STAND,
                "Racik ramuan & konsumsi potion / apel emas",
                "§d🧪 Potion Master §7(Durasi potion meningkat & +Max Mana)"));
        container.setItem(33, makeSkillItem(data, AuraSkillType.ENCHANTING, Items.ENCHANTING_TABLE,
                "Enchant gear, anvil, grindstone & exp bottles",
                "§5✨ Enchantment Affinity §7(Efisiensi slot custom enchant)"));

        // Slot 40: Mana Abilities Guide (Nether Star)
        List<Component> manaLore = new ArrayList<>();
        manaLore.add(Component.literal("§7Gunakan tool dengan §eShift + Klik Kanan §7atau"));
        manaLore.add(Component.literal("§7klik kanan udara untuk menyiagakan alat (§aReadied§7):"));
        manaLore.add(Component.literal(""));
        manaLore.add(Component.literal("§61. Treecapitator §8[Kapak]:"));
        manaLore.add(Component.literal("   §7Tebang 1 batang kayu -> Tumbangkan seluruh pohon"));
        manaLore.add(Component.literal("   §8Konsumsi: §b20 Mana §8| Cooldown: §e30s"));
        manaLore.add(Component.literal("§62. Speed Mine §8[Beliung]:"));
        manaLore.add(Component.literal("   §7Tambang blok -> Haste X selama 10 detik"));
        manaLore.add(Component.literal("   §8Konsumsi: §b20 Mana §8| Cooldown: §e30s"));
        manaLore.add(Component.literal("§63. Replenish §8[Cangkul]:"));
        manaLore.add(Component.literal("   §7Panen tanaman matang -> Otomatis ditanam ulang"));
        manaLore.add(Component.literal("   §8Konsumsi: §b20 Mana §8| Cooldown: §e20s"));
        manaLore.add(Component.literal("§64. Terraform §8[Sekop]:"));
        manaLore.add(Component.literal("   §7Gali tanah/pasir -> Gali instan area 3x3"));
        manaLore.add(Component.literal("   §8Konsumsi: §b20 Mana §8| Cooldown: §e25s"));
        manaLore.add(Component.literal("§65. Lightning Blade §8[Pedang]:"));
        manaLore.add(Component.literal("   §7Serang musuh -> Sambaran petir berantai ke musuh"));
        manaLore.add(Component.literal("   §8Konsumsi: §b20 Mana §8| Cooldown: §e20s"));
        manaLore.add(Component.literal("§66. Charged Shot §8[Busur/Crossbow]:"));
        manaLore.add(Component.literal("   §7Tembak panah -> +15 Damage ledakan piercing"));
        manaLore.add(Component.literal("   §8Konsumsi: §b20 Mana §8| Cooldown: §e20s"));
        manaLore.add(Component.literal("§67. Absorption §8[Perisai]:"));
        manaLore.add(Component.literal("   §7Tahan serangan saat blokir -> +4 Golden Hearts"));
        manaLore.add(Component.literal("   §8Konsumsi: §b20 Mana §8| Cooldown: §e30s"));
        manaLore.add(Component.literal("§68. Sharp Hook §8[Pancingan]:"));
        manaLore.add(Component.literal("   §7Tarik musuh yang tersangkut kail -> 4 Damage"));
        manaLore.add(Component.literal("   §8Konsumsi: §b5 Mana §8| Cooldown: §e3s"));

        ItemStack manaItem = makeItem(Items.NETHER_STAR, "§b§l✦ 8 KEMAMPUAN AKTIF MANA ABILITIES ✦", manaLore);
        container.setItem(40, manaItem);

        // Slot 48: Open Stats Menu Button
        List<Component> statsLore = new ArrayList<>();
        statsLore.add(Component.literal("§7Lihat 9 atribut fisik & sihir pengembara:"));
        statsLore.add(Component.literal("§7Health, Strength, Toughness, Luck, Wisdom, dll."));
        statsLore.add(Component.literal("§7Stat bertambah otomatis saat level skill naik!"));
        statsLore.add(Component.literal(""));
        statsLore.add(Component.literal("§eKlik untuk membuka ringkasan stat."));
        ItemStack statsItem = makeItem(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, "§6§l✦ 9 ATRIBUT RPG (/stats) ✦", statsLore);
        container.setItem(48, statsItem);

        // Slot 50: Close Button
        ItemStack closeItem = makeItem(Items.BARRIER, "§c§l✕ TUTUP MENU", List.of(Component.literal("§7Klik untuk menutup antarmuka skills.")));
        container.setItem(50, closeItem);
    }

    private static void handleMenuClick(int slot, ServerPlayer player) {
        if (slot == 50) {
            player.closeContainer();
        } else if (slot == 48) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
            AuraStatsMenu.open(player);
        } else {
            AuraSkillType clickedSkill = getSkillFromSlot(slot);
            if (clickedSkill != null) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.3f);
                AuraLevelProgressionMenu.open(player, clickedSkill, 1);
            } else {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4f, 1.5f);
            }
        }
    }

    private static AuraSkillType getSkillFromSlot(int slot) {
        return switch (slot) {
            case 19 -> AuraSkillType.FARMING;
            case 20 -> AuraSkillType.FORAGING;
            case 21 -> AuraSkillType.MINING;
            case 22 -> AuraSkillType.FISHING;
            case 23 -> AuraSkillType.EXCAVATION;
            case 24 -> AuraSkillType.ARCHERY;
            case 25 -> AuraSkillType.DEFENSE;
            case 29 -> AuraSkillType.FIGHTING;
            case 30 -> AuraSkillType.AGILITY;
            case 32 -> AuraSkillType.ALCHEMY;
            case 33 -> AuraSkillType.ENCHANTING;
            default -> null;
        };
    }

    private static ItemStack makeSkillItem(TakashaRpgData data, AuraSkillType skill, Item icon, String sourceHint, String abilityHint) {
        int lvl = data.getSkillLevel(skill);
        double xp = data.getSkillXp(skill);
        double req = skill.getXpForNextLevel(lvl);
        double percent = Math.min(100.0, (xp / req) * 100.0);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Tingkat: §a§lLevel " + lvl + " §7(Maks Lvl 100)"));
        lore.add(Component.literal(String.format("§7Kemajuan: §e%.1f §7/ §6%.0f XP §8(§b%.1f%%§8)", xp, req, percent)));
        lore.add(Component.literal("§8§m------------------------"));
        lore.add(Component.literal("§fSumber XP: §7" + sourceHint));
        lore.add(Component.literal("§fKemampuan Khusus: " + abilityHint));
        lore.add(Component.literal("§8§m------------------------"));
        lore.add(Component.literal("§eHadiah Stat (AuraSkills):"));
        lore.add(Component.literal(" §7• §fPrimer: " + skill.getPrimaryStat().getColor() + skill.getPrimaryStat().getDisplayName() + " §a+" + (lvl - 1) + " §7(+1.0/lvl)"));
        lore.add(Component.literal(" §7• §fSekunder: " + skill.getSecondaryStat().getColor() + skill.getSecondaryStat().getDisplayName() + " §a+" + ((lvl - 1) / 2) + " §7(+0.5/lvl)"));
        lore.add(Component.literal(""));
        lore.add(Component.literal("§eKlik untuk melihat rincian Level Progression!"));

        return makeItem(icon, "§e§l" + skill.getDisplayName() + " §7(Lvl " + lvl + ")", lore);
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
