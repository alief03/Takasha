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
import net.takasha.rpg.skills.AuraStatType;
import net.takasha.rpg.skills.AuraTraitType;

import java.util.ArrayList;
import java.util.List;

/**
 * AuraStatsMenu
 * GUI 54-slot untuk melihat 9 atribut AuraSkills:
 * Strength, Health, Regeneration, Luck, Wisdom, Toughness, Speed, Crit Chance, Crit Damage.
 * Juga menampilkan rincian matematis 19 Traits otentik AuraSkills 2.4.0.
 *
 * Menerapkan In-Place Container Reuse agar kursor mouse tidak melompat ke tengah layar saat navigasi.
 */
public class AuraStatsMenu {

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
                Component.literal("§6§l✦ AuraSkills — 9 Atribut Karakter ✦")
        ));
    }

    private static void populateItems(SimpleContainer container, ServerPlayer player) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);

        // Fill background with Dark Gray glass
        ItemStack filler = makeItem(Items.STAINED_GLASS_PANE.gray(), "§8", List.of());
        for (int i = 0; i < 54; i++) {
            container.setItem(i, filler.copy());
        }

        // Decorative Yellow frames
        ItemStack frame = makeItem(Items.STAINED_GLASS_PANE.yellow(), "§e✦", List.of());
        int[] frameSlots = new int[]{0, 1, 7, 8, 9, 17, 36, 44, 45, 46, 52, 53};
        for (int s : frameSlots) {
            container.setItem(s, frame.copy());
        }

        // Slot 4: Player Profile Overview
        List<Component> profileLore = new ArrayList<>();
        profileLore.add(Component.literal("§7Pengembara: §e" + player.getScoreboardName()));
        profileLore.add(Component.literal("§8§m------------------------"));
        profileLore.add(Component.literal("§7Total Level Skill: §a§l" + data.getTotalSkillLevel()));
        profileLore.add(Component.literal("§7Sistem Progresi: §bKanonikal AuraSkills"));
        profileLore.add(Component.literal("§8§m------------------------"));
        profileLore.add(Component.literal("§aStat fisik dan sihir meningkat secara"));
        profileLore.add(Component.literal("§aotomatis seiring kenaikan level 11 cabang skill!"));

        ItemStack profileItem = makeItem(Items.PLAYER_HEAD, "§6§l✦ 9 ATRIBUT KARAKTER ✦", profileLore);
        container.setItem(4, profileItem);

        // 9 Stats Icons & Slots
        // Row 2: Strength (19), Health (20), Regeneration (21), Luck (22), Wisdom (23), Toughness (24), Speed (25)
        // Row 3: Crit Chance (29), Crit Damage (30)
        container.setItem(19, makeStatItem(data, AuraStatType.STRENGTH, Items.NETHERITE_SWORD, "Tingkatkan serangan fisik melee dan ranged."));
        container.setItem(20, makeStatItem(data, AuraStatType.HEALTH, Items.GLISTERING_MELON_SLICE, "Tingkatkan daya tahan dan darah (HP) maksimal."));
        container.setItem(21, makeStatItem(data, AuraStatType.REGENERATION, Items.GHAST_TEAR, "Percepat pemulihan darah alami secara berkala."));
        container.setItem(22, makeStatItem(data, AuraStatType.LUCK, Items.RABBIT_FOOT, "Tingkatkan peluang panen ganda dan drop langka."));
        container.setItem(23, makeStatItem(data, AuraStatType.WISDOM, Items.BOOK, "Tingkatkan pengali perolehan XP dan kapasitas Mana."));
        container.setItem(24, makeStatItem(data, AuraStatType.TOUGHNESS, Items.DIAMOND_CHESTPLATE, "Kurangi besaran kerusakan yang masuk dari musuh."));
        container.setItem(25, makeStatItem(data, AuraStatType.SPEED, Items.SUGAR, "Tingkatkan kecepatan gerak, lari, dan kelincahan."));

        container.setItem(29, makeStatItem(data, AuraStatType.CRIT_CHANCE, Items.BLAZE_POWDER, "Tingkatkan persentase peluang serangan kritikal."));
        container.setItem(30, makeStatItem(data, AuraStatType.CRIT_DAMAGE, Items.FIRE_CHARGE, "Tingkatkan persentase besaran damage kritikal."));

        // Slot 33: 19 Traits Breakdown Book
        List<Component> traitsLore = new ArrayList<>();
        traitsLore.add(Component.literal("§7Status pasif turunan dari level & stat kamu:"));
        traitsLore.add(Component.literal("§8§m------------------------"));
        for (AuraTraitType trait : AuraTraitType.values()) {
            double val = data.getTraitValue(trait);
            traitsLore.add(Component.literal("§7• " + trait.getColor() + trait.getDisplayName() + ": §f" + trait.formatBonus(val)));
        }
        traitsLore.add(Component.literal("§8§m------------------------"));
        traitsLore.add(Component.literal("§eTingkatkan level skill kamu"));
        traitsLore.add(Component.literal("§euntuk memaksimalkan nilai trait!"));

        ItemStack traitsItem = makeItem(Items.ENCHANTED_BOOK, "§b§l✦ 19 TRAITS AURA SKILLS ✦", traitsLore);
        container.setItem(33, traitsItem);

        // Slot 47: Info Progression Item
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.literal("§7Dalam sistem kanonikal AuraSkills, seluruh"));
        infoLore.add(Component.literal("§7stat bertambah secara otomatis dari reward skill."));
        infoLore.add(Component.literal(""));
        infoLore.add(Component.literal("§eTingkatkan skill yang sesuai dengan gaya"));
        infoLore.add(Component.literal("§ebermainmu untuk memperkuat stat incaran!"));
        ItemStack infoItem = makeItem(Items.NETHER_STAR, "§6§l✦ PROGRESI OTOMATIS ✦", infoLore);
        container.setItem(47, infoItem);

        // Slot 49: Back to Skills Menu
        ItemStack backItem = makeItem(Items.ARROW, "§a§l← KEMBALI KE MENU SKILLS", List.of(Component.literal("§7Buka kembali antarmuka 11 cabang AuraSkills.")));
        container.setItem(49, backItem);

        // Slot 51: Close Button
        ItemStack closeItem = makeItem(Items.BARRIER, "§c§l✕ TUTUP", List.of(Component.literal("§7Tutup jendela antarmuka ini.")));
        container.setItem(51, closeItem);
    }

    private static void handleMenuClick(int slot, ServerPlayer player) {
        if (slot == 51) {
            player.closeContainer();
        } else if (slot == 49) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
            AuraSkillsMenu.open(player);
        } else {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4f, 1.5f);
        }
    }

    private static ItemStack makeStatItem(TakashaRpgData data, AuraStatType stat, Item icon, String effectHint) {
        double totalVal = data.getStatValue(stat);
        String valStr = (totalVal == (long) totalVal) ? String.format("%d", (long) totalVal) : String.format("%.1f", totalVal);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Total Nilai Stat: " + stat.getColor() + "§l" + valStr));
        lore.add(Component.literal("§8§m------------------------"));
        lore.add(Component.literal("§fEfek: §7" + effectHint));
        lore.add(Component.literal("§8§m------------------------"));
        lore.add(Component.literal("§7Sumber Peningkatan:"));
        lore.add(Component.literal(" §7• " + getContributingSkillsDescription(stat)));
        lore.add(Component.literal(""));
        lore.add(Component.literal("§aStat ini bertambah otomatis setiap kenaikan level skill!"));

        return makeItem(icon, stat.getColor() + "§l" + stat.getDisplayName() + " §7(Nilai: " + totalVal + ")", lore);
    }

    private static String getContributingSkillsDescription(AuraStatType stat) {
        return switch (stat) {
            case STRENGTH -> "Foraging (+1.0/lvl), Farming (+0.5/lvl), Archery (+0.5/lvl), Excavation (+0.5/lvl)";
            case HEALTH -> "Farming (+0.4/lvl), Defense (+0.2/lvl), Fishing (+0.2/lvl), Alchemy (+0.2/lvl)";
            case REGENERATION -> "Excavation (+1.0/lvl), Agility (+1.0/lvl), Fighting (+0.5/lvl)";
            case LUCK -> "Fishing (+1.0/lvl), Mining (+0.5/lvl), Enchanting (+0.5/lvl)";
            case WISDOM -> "Alchemy (+1.0/lvl), Enchanting (+1.0/lvl), Agility (+0.5/lvl)";
            case TOUGHNESS -> "Defense (+1.0/lvl), Mining (+1.0/lvl), Foraging (+0.5/lvl)";
            case SPEED -> "Agility (+1.0/lvl)";
            case CRIT_CHANCE -> "Archery (+1.0/lvl)";
            case CRIT_DAMAGE -> "Fighting (+1.0/lvl)";
        };
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
