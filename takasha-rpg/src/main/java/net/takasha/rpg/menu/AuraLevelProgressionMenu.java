package net.takasha.rpg.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
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
 * AuraLevelProgressionMenu
 * GUI Level Progression kanonikal AuraSkills (54-slot chest).
 * Menampilkan Serpentine Track 24-slot dengan indikator status:
 * - Hijau (Lime): Level yang sudah tercapai (Unlocked)
 * - Kuning (Yellow): Level berikutnya yang sedang dipelajari (In Progress)
 * - Merah (Red): Level masa depan yang belum terbuka (Locked)
 *
 * Mendukung In-Place Container Reuse agar kursor mouse tidak melompat ke tengah layar.
 */
public class AuraLevelProgressionMenu {

    // Serpentine track slots urut sesuai menus/level_progression.yml AuraSkills 2.4.0
    private static final int[] TRACK_SLOTS = new int[]{
            9, 18, 27, 36, 37, 38, 29, 20, 11, 12, 13, 22, 31, 40, 41, 42, 33, 24, 15, 16, 17, 26, 35, 44
    };

    private static final int LEVELS_PER_PAGE = 24;
    private static final int MAX_LEVEL = 100;
    private static final int MAX_PAGES = (int) Math.ceil((double) MAX_LEVEL / LEVELS_PER_PAGE);

    public static void open(ServerPlayer player, AuraSkillType skill, int page) {
        int safePage = Math.max(1, Math.min(MAX_PAGES, page));

        // ZERO CURSOR DISPLACEMENT: Reuse existing 54-slot container if already open
        if (player.containerMenu instanceof ReadOnlyChestMenu currentMenu
                && currentMenu.getContainer() instanceof SimpleContainer curCont
                && curCont.getContainerSize() == 54) {
            populateItems(curCont, player, skill, safePage);
            currentMenu.setSlotClickAction((slotId, button, clickType, pl) -> handleMenuClick(slotId, (ServerPlayer) pl, skill, safePage));
            currentMenu.broadcastChanges();
            return;
        }

        SimpleContainer container = new SimpleContainer(54);
        populateItems(container, player, skill, safePage);

        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ReadOnlyChestMenu(
                        syncId,
                        inv,
                        container,
                        6,
                        (slotId, button, clickType, pl) -> handleMenuClick(slotId, (ServerPlayer) pl, skill, safePage)
                ),
                Component.literal("§8" + skill.getDisplayName() + " - Kemajuan Level (Hlm " + safePage + "/" + MAX_PAGES + ")")
        ));
    }

    private static void populateItems(SimpleContainer container, ServerPlayer player, AuraSkillType skill, int page) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        int currentLvl = data.getSkillLevel(skill);
        double currentXp = data.getSkillXp(skill);
        double reqXp = skill.getXpForNextLevel(currentLvl);

        // Fill background with black stained glass pane
        ItemStack filler = makeItem(Items.STAINED_GLASS_PANE.black(), "§8", List.of());
        for (int i = 0; i < 54; i++) {
            container.setItem(i, filler.copy());
        }

        // Slot 0: Skill Icon Overview
        container.setItem(0, makeSkillTemplateItem(skill, currentLvl, currentXp, reqXp, data));

        // Slot 1: Your Ranking / Stats Overview
        List<Component> rankLore = new ArrayList<>();
        rankLore.add(Component.literal("§7Level Skill Ini: §eLevel " + currentLvl));
        rankLore.add(Component.literal("§7Total Seluruh Level: §a" + data.getTotalSkillLevel()));
        rankLore.add(Component.literal("§7Rata-rata Level: §b" + (data.getTotalSkillLevel() / 11)));
        rankLore.add(Component.literal(""));
        rankLore.add(Component.literal("§eJadilah yang terkuat di dunia Takasha!"));
        container.setItem(1, makeItem(Items.PAPER, "§6§l✦ Peringkat & Ringkasan ✦", rankLore));

        // Slot 2: Sources
        List<Component> sourcesLore = new ArrayList<>();
        sourcesLore.add(Component.literal("§7Aktivitas untuk mendapatkan XP §e" + skill.getDisplayName() + "§7:"));
        sourcesLore.add(Component.literal(""));
        for (String sourceLine : getSourcesDescription(skill)) {
            sourcesLore.add(Component.literal(" §7• " + sourceLine));
        }
        sourcesLore.add(Component.literal(""));
        sourcesLore.add(Component.literal("§e▶ Klik untuk melihat seluruh sumber perolehan XP"));
        container.setItem(2, makeItem(Items.EXPERIENCE_BOTTLE, "§a§l✦ Sumber Pengalaman (XP) ✦", sourcesLore));

        // Slot 3: Abilities
        List<Component> abilitiesLore = new ArrayList<>();
        abilitiesLore.add(Component.literal("§7Daftar 5 Kemampuan Pasif & 1 Kemampuan Aktif:"));
        abilitiesLore.add(Component.literal(""));
        for (int slot = 1; slot <= 5; slot++) {
            int pLvl = data.getPassiveAbilityLevel(skill, slot);
            String pName = getPassiveAbilityName(skill, slot);
            abilitiesLore.add(Component.literal(" §6" + slot + ". §e" + pName + " " + (pLvl > 0 ? "§a(Lvl " + pLvl + ")" : "§8(Terkunci)")));
        }
        int manaLvl = data.getManaAbilityLevel(skill);
        String manaName = getManaAbilityName(skill);
        abilitiesLore.add(Component.literal(""));
        abilitiesLore.add(Component.literal(" §d⚡ Mana Ability: §b" + manaName + " " + (manaLvl > 0 ? "§a(Lvl " + manaLvl + ")" : "§8(Terkunci - Lvl 6)")));
        abilitiesLore.add(Component.literal(""));
        abilitiesLore.add(Component.literal("§e▶ Klik untuk melihat seluruh kemampuan & talenta"));
        container.setItem(3, makeItem(Items.NETHER_STAR, "§c§l✦ Kemampuan Skill ✦", abilitiesLore));

        // Slot 4: Stat Rewards
        List<Component> rewardsLore = new ArrayList<>();
        rewardsLore.add(Component.literal("§7Kenaikan stat otomatis setiap level up:"));
        rewardsLore.add(Component.literal(""));
        rewardsLore.add(Component.literal(" §7• Primer: " + skill.getPrimaryStat().getColor() + skill.getPrimaryStat().getDisplayName() + " §a+1.0 / level"));
        rewardsLore.add(Component.literal(" §7• Sekunder: " + skill.getSecondaryStat().getColor() + skill.getSecondaryStat().getDisplayName() + " §a+0.5 / level"));
        rewardsLore.add(Component.literal(""));
        rewardsLore.add(Component.literal("§8Status langsung diaplikasikan ke atribut fisikmu!"));
        container.setItem(4, makeItem(Items.GOLD_INGOT, "§e§l✦ Hadiah Atribut Otomatis ✦", rewardsLore));

        // Serpentine Track 24 Slots
        int startIndex = (page - 1) * LEVELS_PER_PAGE;
        for (int i = 0; i < TRACK_SLOTS.length; i++) {
            int slotPos = TRACK_SLOTS[i];
            int targetLevel = startIndex + i + 1;

            if (targetLevel > MAX_LEVEL) {
                container.setItem(slotPos, makeItem(Items.STAINED_GLASS_PANE.gray(), "§8", List.of()));
                continue;
            }

            if (targetLevel <= currentLvl) {
                // UNLOCKED (Lime)
                container.setItem(slotPos, makeLevelItem(targetLevel, skill, "unlocked", currentXp, reqXp));
            } else if (targetLevel == currentLvl + 1) {
                // IN PROGRESS (Yellow)
                container.setItem(slotPos, makeLevelItem(targetLevel, skill, "in_progress", currentXp, reqXp));
            } else {
                // LOCKED (Red)
                container.setItem(slotPos, makeLevelItem(targetLevel, skill, "locked", currentXp, reqXp));
            }
        }

        // Slot 45: Back button
        container.setItem(45, makeItem(Items.ARROW, "§a§l← Kembali ke Menu Skills", List.of(Component.literal("§7Klik untuk kembali ke antarmuka 11 cabang skill."))));

        // Slot 46: Close button
        container.setItem(46, makeItem(Items.BARRIER, "§c§l✕ Tutup Menu", List.of(Component.literal("§7Klik untuk menutup GUI."))));

        // Slot 52: Previous Page
        if (page > 1) {
            container.setItem(52, makeItem(Items.ARROW, "§6§l← Halaman Sebelumnya (" + (page - 1) + ")", List.of(Component.literal("§7Klik untuk melihat level sebelumnya."))));
        } else {
            container.setItem(52, makeItem(Items.STAINED_GLASS_PANE.gray(), "§8Tidak Ada Halaman", List.of()));
        }

        // Slot 53: Next Page
        if (page < MAX_PAGES) {
            container.setItem(53, makeItem(Items.ARROW, "§6§lHalaman Berikutnya (" + (page + 1) + ") →", List.of(Component.literal("§7Klik untuk melihat level berikutnya."))));
        } else {
            container.setItem(53, makeItem(Items.STAINED_GLASS_PANE.gray(), "§8Tidak Ada Halaman", List.of()));
        }
    }

    private static void handleMenuClick(int slotId, ServerPlayer player, AuraSkillType skill, int page) {
        if (slotId == 2) {
            // Buka Sub-GUI AuraSourcesMenu
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
            AuraSourcesMenu.open(player, skill, 1, AuraSourcesMenu.SortMode.DEFAULT);
        } else if (slotId == 3) {
            // Buka Sub-GUI AuraAbilitiesMenu
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
            AuraAbilitiesMenu.open(player, skill);
        } else if (slotId == 45) {
            // Back to AuraSkillsMenu
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
            AuraSkillsMenu.open(player);
        } else if (slotId == 46) {
            player.closeContainer();
        } else if (slotId == 52 && page > 1) {
            // Previous Page
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
            open(player, skill, page - 1);
        } else if (slotId == 53 && page < MAX_PAGES) {
            // Next Page
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
            open(player, skill, page + 1);
        } else {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4f, 1.5f);
        }
    }

    private static ItemStack makeLevelItem(int level, AuraSkillType skill, String state, double currentXp, double reqXp) {
        Item item = switch (state) {
            case "unlocked" -> Items.STAINED_GLASS_PANE.lime();
            case "in_progress" -> Items.STAINED_GLASS_PANE.yellow();
            default -> Items.STAINED_GLASS_PANE.red();
        };

        String titleColor = switch (state) {
            case "unlocked" -> "§a";
            case "in_progress" -> "§e";
            default -> "§c";
        };

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Level: §f" + level));
        lore.add(Component.literal("§7Hadiah Stat:"));
        lore.add(Component.literal("  " + skill.getPrimaryStat().getColor() + "+1.0 " + skill.getPrimaryStat().getDisplayName()));
        lore.add(Component.literal("  " + skill.getSecondaryStat().getColor() + "+0.5 " + skill.getSecondaryStat().getDisplayName()));

        // Milestone information
        List<String> milestones = getLevelMilestones(skill, level);
        if (!milestones.isEmpty()) {
            lore.add(Component.literal(""));
            lore.add(Component.literal("§6Milestone Terbuka:"));
            for (String ms : milestones) {
                lore.add(Component.literal("  " + ms));
            }
        }

        lore.add(Component.literal(""));
        if ("unlocked".equals(state)) {
            lore.add(Component.literal("§a✔ Telah Terbuka"));
        } else if ("in_progress".equals(state)) {
            double percent = Math.min(100.0, (currentXp / reqXp) * 100.0);
            lore.add(Component.literal("§7Kemajuan: §e" + String.format("%.1f", percent) + "%"));
            lore.add(Component.literal(buildProgressBar(percent)));
            lore.add(Component.literal("§7" + String.format("%.1f", currentXp) + " / " + String.format("%.0f", reqXp) + " XP"));
            lore.add(Component.literal(""));
            lore.add(Component.literal("§e⚡ Sedang Dikejar"));
        } else {
            lore.add(Component.literal("§c🔒 Terkunci"));
        }

        return makeItem(item, titleColor + "Level " + level, lore);
    }

    private static String buildProgressBar(double percent) {
        int totalBars = 20;
        int activeBars = (int) Math.round((percent / 100.0) * totalBars);
        StringBuilder sb = new StringBuilder("§a");
        for (int i = 0; i < totalBars; i++) {
            if (i == activeBars) {
                sb.append("§7");
            }
            sb.append("▌");
        }
        return sb.toString();
    }

    private static List<String> getLevelMilestones(AuraSkillType skill, int level) {
        List<String> milestones = new ArrayList<>();
        if (level == 2) {
            milestones.add("§a✦ Kemampuan 1 Terbuka: §e" + getPassiveAbilityName(skill, 1));
        } else if (level == 3) {
            milestones.add("§a✦ Kemampuan 2 Terbuka: §e" + getPassiveAbilityName(skill, 2));
        } else if (level == 4) {
            milestones.add("§a✦ Kemampuan 3 Terbuka: §e" + getPassiveAbilityName(skill, 3));
        } else if (level == 5) {
            milestones.add("§a✦ Kemampuan 4 Terbuka: §e" + getPassiveAbilityName(skill, 4));
        } else if (level == 6) {
            milestones.add("§a✦ Kemampuan 5 Terbuka: §e" + getPassiveAbilityName(skill, 5));
            milestones.add("§d⚡ Mana Ability Terbuka: §b" + getManaAbilityName(skill));
        } else {
            // Milestone level ups
            if (level % 5 == 2) milestones.add("§eNaik Tingkat Kemampuan: §f" + getPassiveAbilityName(skill, 1));
            if (level % 5 == 3) milestones.add("§eNaik Tingkat Kemampuan: §f" + getPassiveAbilityName(skill, 2));
            if (level % 5 == 4) milestones.add("§eNaik Tingkat Kemampuan: §f" + getPassiveAbilityName(skill, 3));
            if (level % 5 == 0) milestones.add("§eNaik Tingkat Kemampuan: §f" + getPassiveAbilityName(skill, 4));
            if (level % 5 == 1 && level > 6) milestones.add("§eNaik Tingkat Kemampuan: §f" + getPassiveAbilityName(skill, 5));
            if (level % 6 == 0 && level > 6) milestones.add("§d⚡ Naik Tingkat Mana Ability: §b" + getManaAbilityName(skill));
        }
        return milestones;
    }

    private static ItemStack makeSkillTemplateItem(AuraSkillType skill, int lvl, double currentXp, double reqXp, TakashaRpgData data) {
        Item icon = switch (skill) {
            case FARMING -> Items.IRON_HOE;
            case FORAGING -> Items.IRON_AXE;
            case MINING -> Items.IRON_PICKAXE;
            case FISHING -> Items.FISHING_ROD;
            case EXCAVATION -> Items.IRON_SHOVEL;
            case ARCHERY -> Items.BOW;
            case DEFENSE -> Items.CHAINMAIL_CHESTPLATE;
            case FIGHTING -> Items.IRON_SWORD;
            case AGILITY -> Items.FEATHER;
            case ALCHEMY -> Items.BREWING_STAND;
            case ENCHANTING -> Items.ENCHANTING_TABLE;
        };

        double percent = Math.min(100.0, (currentXp / reqXp) * 100.0);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Cabang: §f" + skill.getDisplayName()));
        lore.add(Component.literal("§7Tingkat: §a§lLevel " + lvl + " §7/ 100"));
        lore.add(Component.literal("§8§m------------------------"));
        lore.add(Component.literal("§7Bonus Atribut Permanen:"));
        lore.add(Component.literal("  " + skill.getPrimaryStat().getColor() + "+ " + (lvl - 1) + ".0 " + skill.getPrimaryStat().getDisplayName()));
        lore.add(Component.literal("  " + skill.getSecondaryStat().getColor() + "+ " + String.format("%.1f", (lvl - 1) * 0.5) + " " + skill.getSecondaryStat().getDisplayName()));
        lore.add(Component.literal("§8§m------------------------"));
        lore.add(Component.literal("§7Kemajuan ke Level " + (lvl + 1) + ": §e" + String.format("%.1f", percent) + "%"));
        lore.add(Component.literal(buildProgressBar(percent)));
        lore.add(Component.literal("§7" + String.format("%.1f", currentXp) + " / " + String.format("%.0f", reqXp) + " XP"));

        return makeItem(icon, "§b§l" + skill.getDisplayName() + " §3" + lvl, lore);
    }

    private static List<String> getSourcesDescription(AuraSkillType skill) {
        return switch (skill) {
            case FARMING -> List.of("Panen tanaman matang (Gandum, Wortel, Kentang, Beetroot)", "Panen Nether Wart, Melon, Labu, dan Tebu");
            case FORAGING -> List.of("Tebang semua varian pohon kayu di alam", "Gunakan Treecapitator untuk tebang massal");
            case MINING -> List.of("Tambang mineral (Batubara, Besi, Emas, Berlian, Ancient Debris)", "Tambang bebatuan gua dan deepslate");
            case FISHING -> List.of("Pancing ikan dan harta karun di perairan", "Tarik musuh dengan Sharp Hook");
            case EXCAVATION -> List.of("Gali tanah, pasir, kerikil, clay, lumpur", "Gunakan Terraform untuk menggali cepat");
            case ARCHERY -> List.of("Tembak dan kalahkan musuh dengan panah / crossbow", "Tembak dengan Charged Shot untuk ledakan");
            case DEFENSE -> List.of("Tahan serangan fisik dan proyektil dari musuh", "Blokir dengan perisai untuk mengaktifkan Absorption");
            case FIGHTING -> List.of("Kalahkan monster dengan pedang atau senjata melee", "Picukan Lightning Blade untuk petir berantai");
            case AGILITY -> List.of("Berlari sprint jarak jauh", "Berenang di air & terbang dengan elytra", "Jatuh dari ketinggian dan mendarat selamat");
            case ALCHEMY -> List.of("Gunakan Brewing Stand untuk meracik ramuan", "Minum ramuan potion atau makan Golden Apple");
            case ENCHANTING -> List.of("Gunakan Enchanting Table, Anvil, dan Grindstone", "Pecahkan botol Experience Bottle");
        };
    }

    private static String getPassiveAbilityName(AuraSkillType skill, int slot) {
        return switch (skill) {
            case FARMING -> switch (slot) {
                case 1 -> "Bountiful Harvest";
                case 2 -> "Farmer";
                case 3 -> "Scythe Master";
                case 4 -> "Geneticist";
                default -> "Growth Aura";
            };
            case FORAGING -> switch (slot) {
                case 1 -> "Lumberjack";
                case 2 -> "Forager";
                case 3 -> "Axe Master";
                case 4 -> "Valor";
                default -> "Shredder";
            };
            case MINING -> switch (slot) {
                case 1 -> "Lucky Miner";
                case 2 -> "Miner";
                case 3 -> "Pick Master";
                case 4 -> "Stamina";
                default -> "Hardened Armor";
            };
            case FISHING -> switch (slot) {
                case 1 -> "Lucky Catch";
                case 2 -> "Fisher";
                case 3 -> "Treasure Hunter";
                case 4 -> "Grappler";
                default -> "Epic Catch";
            };
            case EXCAVATION -> switch (slot) {
                case 1 -> "Bigger Scoop";
                case 2 -> "Excavator";
                case 3 -> "Spade Master";
                case 4 -> "Metal Detector";
                default -> "Lucky Spades";
            };
            case ARCHERY -> switch (slot) {
                case 1 -> "Retrieval";
                case 2 -> "Archer";
                case 3 -> "Bow Master";
                case 4 -> "Piercing";
                default -> "Stun";
            };
            case DEFENSE -> switch (slot) {
                case 1 -> "Shielding";
                case 2 -> "Defender";
                case 3 -> "Mob Master";
                case 4 -> "Immunity";
                default -> "No Debuff";
            };
            case FIGHTING -> switch (slot) {
                case 1 -> "Parry";
                case 2 -> "Fighter";
                case 3 -> "Sword Master";
                case 4 -> "First Strike";
                default -> "Bleed";
            };
            case AGILITY -> switch (slot) {
                case 1 -> "Anti Hunger";
                case 2 -> "Runner";
                case 3 -> "Golden Heal";
                case 4 -> "Fleeting";
                default -> "Thunder Fall";
            };
            case ALCHEMY -> switch (slot) {
                case 1 -> "Alchemist";
                case 2 -> "Brewer";
                case 3 -> "Splasher";
                case 4 -> "Lingering";
                default -> "Wise Effect";
            };
            case ENCHANTING -> switch (slot) {
                case 1 -> "Xp Convert";
                case 2 -> "Enchanter";
                case 3 -> "Xp Warrior";
                case 4 -> "Enchanted Strength";
                default -> "Lucky Table";
            };
        };
    }

    private static String getManaAbilityName(AuraSkillType skill) {
        return switch (skill) {
            case FARMING -> "Replenish";
            case FORAGING -> "Treecapitator";
            case MINING -> "Speed Mine";
            case FISHING -> "Sharp Hook";
            case EXCAVATION -> "Terraform";
            case ARCHERY -> "Charged Shot";
            case DEFENSE -> "Absorption";
            case FIGHTING -> "Lightning Blade";
            case AGILITY -> "Fleet Foot";
            case ALCHEMY -> "Potion Master";
            case ENCHANTING -> "Enchantment Affinity";
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
