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
 * AuraAbilitiesMenu
 * Sub-GUI interaktif 54-slot untuk menampilkan seluruh kemampuan (5 Pasif + 1 Mana Ability)
 * per cabang skill AuraSkills dengan indikator visual dye (terbuka vs terkunci), rincian mekanik,
 * biaya mana/cooldown, dan Zero Cursor Displacement container reuse.
 */
public class AuraAbilitiesMenu {

    public static void open(ServerPlayer player, AuraSkillType skill) {
        // ZERO CURSOR DISPLACEMENT: Gunakan kembali 54-slot container yang sedang terbuka
        if (player.containerMenu instanceof ReadOnlyChestMenu currentMenu
                && currentMenu.getContainer() instanceof SimpleContainer curCont
                && curCont.getContainerSize() == 54) {
            populateItems(curCont, player, skill);
            currentMenu.setSlotClickAction((slotId, button, clickType, pl) -> handleMenuClick(slotId, (ServerPlayer) pl, skill));
            currentMenu.broadcastChanges();
            return;
        }

        SimpleContainer container = new SimpleContainer(54);
        populateItems(container, player, skill);

        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ReadOnlyChestMenu(
                        syncId,
                        inv,
                        container,
                        6,
                        (slotId, button, clickType, pl) -> handleMenuClick(slotId, (ServerPlayer) pl, skill)
                ),
                Component.literal("§8" + skill.getDisplayName() + " - Kemampuan & Talenta")
        ));
    }

    private static void populateItems(SimpleContainer container, ServerPlayer player, AuraSkillType skill) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        int currentLvl = data.getSkillLevel(skill);

        // Fill background with black stained glass pane
        ItemStack filler = makeItem(Items.STAINED_GLASS_PANE.black(), "§8", List.of());
        for (int i = 0; i < 54; i++) {
            container.setItem(i, filler.copy());
        }

        // Slot 4: Info Header Skill Icon
        Item skillIcon = getSkillIcon(skill);
        List<Component> headerLore = new ArrayList<>();
        headerLore.add(Component.literal("§7Cabang Skill : " + skill.getColor() + skill.getDisplayName()));
        headerLore.add(Component.literal("§7Level Pemain  : §eLevel " + currentLvl));
        headerLore.add(Component.literal("§7Kemampuan     : §b5 Pasif §7+ §d1 Mana Aktif"));
        headerLore.add(Component.literal(""));
        headerLore.add(Component.literal("§eKemampuan meningkat otomatis saat level skill bertambah!"));
        container.setItem(4, makeItem(skillIcon, "§b§l✦ Kemampuan Skill: " + skill.getDisplayName() + " ✦", headerLore));

        // 5 Passive Abilities (Slot 20, 21, 22, 23, 24)
        int[] passiveSlots = {20, 21, 22, 23, 24};
        int[] unlockLevels = {2, 3, 4, 5, 6};

        for (int i = 0; i < 5; i++) {
            int slotNumber = i + 1;
            int targetSlot = passiveSlots[i];
            int unlockLvl = unlockLevels[i];
            int pLvl = data.getPassiveAbilityLevel(skill, slotNumber);
            String name = getPassiveAbilityName(skill, slotNumber);
            String desc = getPassiveDescription(skill, slotNumber);

            boolean isUnlocked = pLvl > 0;
            Item dyeItem = isUnlocked ? Items.DYE.lightBlue() : Items.DYE.gray();
            String title = (isUnlocked ? "§b§l" : "§7§l") + slotNumber + ". " + name + (isUnlocked ? " §a(Lvl " + pLvl + ")" : " §8(Terkunci)");

            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("§7Tipe Kemampuan : §eKemampuan Pasif"));
            if (isUnlocked) {
                lore.add(Component.literal("§7Tingkat Saat Ini: §aLevel " + pLvl + " §7/ 5"));
            } else {
                lore.add(Component.literal("§7Syarat Terbuka : §cMencapai Level " + unlockLvl + " " + skill.getDisplayName()));
            }
            lore.add(Component.literal(""));
            lore.add(Component.literal("§eEfek Mekanik:"));
            lore.add(Component.literal(" §7" + desc));
            lore.add(Component.literal(""));
            if (isUnlocked) {
                lore.add(Component.literal("§a✔ Telah Terbuka & Aktif"));
            } else {
                lore.add(Component.literal("§c🔒 Terkunci"));
            }

            container.setItem(targetSlot, makeItem(dyeItem, title, lore));
        }

        // 1 Active Mana Ability (Slot 31)
        int manaLvl = data.getManaAbilityLevel(skill);
        String manaName = getManaAbilityName(skill);
        String manaDesc = getManaAbilityDescription(skill);
        double manaCost = data.getManaAbilityCost(skill);
        double cdSec = data.getManaAbilityCooldownSeconds(skill);

        boolean isManaUnlocked = manaLvl > 0;
        Item manaDye = isManaUnlocked ? Items.DYE.magenta() : Items.DYE.gray();
        String manaTitle = (isManaUnlocked ? "§d§l⚡ " : "§7§l⚡ ") + manaName + (isManaUnlocked ? " §a(Lvl " + manaLvl + ")" : " §8(Terkunci)");

        List<Component> manaLore = new ArrayList<>();
        manaLore.add(Component.literal("§7Tipe Kemampuan : §dKemampuan Mana Aktif"));
        if (isManaUnlocked) {
            manaLore.add(Component.literal("§7Tingkat Saat Ini: §aLevel " + manaLvl + " §7/ 5"));
            manaLore.add(Component.literal("§7Biaya Konsumsi : §b" + (int) manaCost + " Mana"));
            manaLore.add(Component.literal("§7Waktu Cooldown : §e" + (int) cdSec + " Detik"));
        } else {
            manaLore.add(Component.literal("§7Syarat Terbuka : §cMencapai Level 6 " + skill.getDisplayName()));
        }
        manaLore.add(Component.literal(""));
        manaLore.add(Component.literal("§eCara Aktivasi:"));
        manaLore.add(Component.literal(" §7" + getManaActivationHint(skill)));
        manaLore.add(Component.literal(""));
        manaLore.add(Component.literal("§eEfek Mekanik:"));
        manaLore.add(Component.literal(" §7" + manaDesc));
        manaLore.add(Component.literal(""));
        if (isManaUnlocked) {
            manaLore.add(Component.literal("§a✔ Telah Terbuka & Siap Digunakan"));
        } else {
            manaLore.add(Component.literal("§c🔒 Terkunci"));
        }

        container.setItem(31, makeItem(manaDye, manaTitle, manaLore));

        // Slot 45: Back Button
        container.setItem(45, makeItem(Items.ARROW, "§a§l← Kembali ke Progres Level",
                List.of(Component.literal("§7Klik untuk kembali ke bagan level " + skill.getDisplayName() + "."))));

        // Slot 49: Close Button
        container.setItem(49, makeItem(Items.BARRIER, "§c§l✕ Tutup Menu",
                List.of(Component.literal("§7Klik untuk menutup antarmuka."))));
    }

    private static void handleMenuClick(int slotId, ServerPlayer player, AuraSkillType skill) {
        if (slotId == 45) {
            // Kembali ke AuraLevelProgressionMenu secara in-place
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
            AuraLevelProgressionMenu.open(player, skill, 1);
        } else if (slotId == 49) {
            player.closeContainer();
        } else {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4f, 1.5f);
        }
    }

    private static Item getSkillIcon(AuraSkillType skill) {
        return switch (skill) {
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

    private static String getPassiveDescription(AuraSkillType skill, int slot) {
        return switch (skill) {
            case FARMING -> switch (slot) {
                case 1 -> "Peluang mendapatkan hasil panen ganda saat memanen tanaman matang.";
                case 2 -> "Meningkatkan perolehan EXP bertani dan kecepatan pertumbuhan benih.";
                case 3 -> "Meningkatkan damage serangan saat menggunakan arit atau cangkul.";
                case 4 -> "Peluang menemukan bibit varietas unggul atau tanaman purba.";
                default -> "Mempercepat pertumbuhan tanaman di sekeliling pemain secara pasif.";
            };
            case FORAGING -> switch (slot) {
                case 1 -> "Peluang menjatuhkan gelondongan kayu ganda saat menebang pohon.";
                case 2 -> "Meningkatkan perolehan EXP kehutanan dan peluang menjatuhkan apel.";
                case 3 -> "Meningkatkan damage serangan kapak ke monster dan memotong perisai.";
                case 4 -> "Memberikan ketahanan armor ekstra saat berada di dalam hutan lebat.";
                default -> "Menghancurkan dedaunan sekitar secara instan saat batang pohon tumbang.";
            };
            case MINING -> switch (slot) {
                case 1 -> "Peluang mendapatkan bijih mineral ganda tanpa merusak ketahanan alat.";
                case 2 -> "Meningkatkan kecepatan menambang (Haste pasif) di kedalaman gua.";
                case 3 -> "Meningkatkan damage beliung dan efisiensi penambangan obsidian.";
                case 4 -> "Mengurangi keausan stamina dan lapar saat menambang berjam-jam.";
                default -> "Menambah ketahanan tubuh (Toughness) secara permanen dari intisari mineral.";
            };
            case FISHING -> switch (slot) {
                case 1 -> "Peluang menangkap 2 ikan sekaligus dalam sekali tarikan joran.";
                case 2 -> "Mengurangi waktu tunggu ikan menyambar umpan di segala cuaca.";
                case 3 -> "Peluang lebih tinggi menemukan pusaka laut dan buku sihir langka.";
                case 4 -> "Menarik musuh atau item jatuh mendekat saat mengenai kail pancing.";
                default -> "Peluang menangkap ikan legendaris berhadiah EXP dan koin melimpah.";
            };
            case EXCAVATION -> switch (slot) {
                case 1 -> "Menggali blok tanah dan pasir dalam area sapuan yang lebih lebar.";
                case 2 -> "Meningkatkan perolehan EXP penggalian dan drop batu api/clay.";
                case 3 -> "Meningkatkan kecepatan sekop dan daya tahan alat penggali.";
                case 4 -> "Peluang mendeteksi harta karun purba, permata, atau relic saat menggali.";
                default -> "Menemukan koin keberuntungan dan material langka dari timbunan pasir.";
            };
            case ARCHERY -> switch (slot) {
                case 1 -> "Peluang anak panah yang mengenai sasaran kembali ke kantong pemain.";
                case 2 -> "Meningkatkan kecepatan tarikan busur dan akurasi bidikan panah.";
                case 3 -> "Meningkatkan damage proyektil panah sebanding dengan jarak tembak.";
                case 4 -> "Anak panah menembus pertahanan zirah dan perisai lawan.";
                default -> "Peluang memberikan efek kaku (Stun) dan Slowness pada target tembakan.";
            };
            case DEFENSE -> switch (slot) {
                case 1 -> "Meningkatkan ketahanan perisai dan mengurangi knockback diterima.";
                case 2 -> "Mengurangi seluruh kerusakan fisik yang masuk sebesar persentase tetap.";
                case 3 -> "Mengurangi kerusakan khusus dari monster agresif dan bos dungeon.";
                case 4 -> "Peluang menepis efek racun, layu, dan api sebelum meresap ke tubuh.";
                default -> "Menghilangkan durasi debuff negatif lebih cepat saat menerima serangan.";
            };
            case FIGHTING -> switch (slot) {
                case 1 -> "Peluang menangkis serangan melee musuh dan memantulkan pukulannya.";
                case 2 -> "Meningkatkan base damage seluruh senjata pedang dan belati.";
                case 3 -> "Meningkatkan peluang dan multiplier tebasan kritikal (Crit Damage).";
                case 4 -> "Memberikan bonus damage masif pada serangan pembuka ke musuh darah penuh.";
                default -> "Serangan menyebabkan pendarahan berkelanjutan (Bleed DoT) pada lawan.";
            };
            case AGILITY -> switch (slot) {
                case 1 -> "Mengurangi konsumsi bar lapar secara drastis saat berlari sprint.";
                case 2 -> "Meningkatkan kecepatan gerak kaki (Movement Speed) secara permanen.";
                case 3 -> "Memulihkan sebagian kecil HP saat melompati rintangan dengan lincah.";
                case 4 -> "Peluang menghindari serangan musuh sepenuhnya dengan partikel lincah.";
                default -> "Mengubah benturan jatuh menjadi gelombang kejut radial yang aman.";
            };
            case ALCHEMY -> switch (slot) {
                case 1 -> "Mempercepat waktu pemasakan ramuan pada brewing stand.";
                case 2 -> "Peluang menghasilkan botol ramuan ekstra saat proses fermentasi selesai.";
                case 3 -> "Memperluas radius ledakan dan durasi ramuan lempar (Splash Potion).";
                case 4 -> "Memperpanjang kabut awan ramuan lingering dan kekuatannya.";
                default -> "Meningkatkan durasi dan potensi seluruh efek buff positif yang diminum.";
            };
            case ENCHANTING -> switch (slot) {
                case 1 -> "Mengurangi biaya level EXP saat menaruh sihir pada meja sihir.";
                case 2 -> "Meningkatkan level sihir yang ditawarkan pada meja sihir paron.";
                case 3 -> "Memberikan bonus EXP pertempuran setiap kali mengalahkan musuh.";
                case 4 -> "Meningkatkan efektivitas sihir penyerangan pada senjata yang ditempa.";
                default -> "Peluang mendapatkan opsi sihir tingkat tertinggi pada penawaran meja sihir.";
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
            case DEFENSE -> "Absorption Shield";
            case FIGHTING -> "Lightning Blade";
            case AGILITY -> "Fleet Foot";
            case ALCHEMY -> "Potion Master";
            case ENCHANTING -> "Enchantment Affinity";
        };
    }

    private static String getManaAbilityDescription(AuraSkillType skill) {
        return switch (skill) {
            case FARMING -> "Secara otomatis menanam kembali benih tanaman matang yang baru saja dipanen.";
            case FORAGING -> "Menebang seluruh batang pohon sekaligus dari pucuk hingga akar dalam 1 tebasan.";
            case MINING -> "Memperoleh status Haste tingkat tinggi untuk menambang urat bijih dalam sekejap.";
            case FISHING -> "Menembakkan kail berkekuatan magis yang menyambar tangkapan secara instan.";
            case EXCAVATION -> "Menghancurkan kubus tanah/pasir dalam radius 3x3 secara instan.";
            case ARCHERY -> "Menembakkan anak panah petir yang meledak saat menghantam tanah sasaran.";
            case DEFENSE -> "Membangkitkan perisai sihir emas yang menyerap seluruh damage selama beberapa detik.";
            case FIGHTING -> "Memancarkan kilatan petir berantai ke musuh di sekitar pada setiap tebasan pedang.";
            case AGILITY -> "Lonjakan kecepatan lari kilat yang meninggalkan jejak angin dan partikel kencang.";
            case ALCHEMY -> "Mengaktifkan intisari ramuan acak terkuat ke tubuh tanpa meminum botol.";
            case ENCHANTING -> "Meningkatkan peluang keberhasilan penempaan dan menghilangkan biaya material sementara.";
        };
    }

    private static String getManaActivationHint(AuraSkillType skill) {
        return switch (skill) {
            case FARMING -> "Klik Kanan tanah dengan Cangkul (Hoe) siap siaga";
            case FORAGING -> "Shift + Klik Kanan batang kayu dengan Kapak (Axe)";
            case MINING -> "Shift + Klik Kanan batu dengan Beliung (Pickaxe)";
            case FISHING -> "Klik Kanan memegang Joran Pancing di perairan luas";
            case EXCAVATION -> "Shift + Klik Kanan tanah dengan Sekop (Shovel)";
            case ARCHERY -> "Tahan dan bidik Busur (Bow) hingga bergetar maksimal";
            case DEFENSE -> "Tahan Perisai (Shield) tepat sebelum pukulan musuh tiba";
            case FIGHTING -> "Shift + Klik Kanan udara dengan Pedang (Sword)";
            case AGILITY -> "Tekan ganda tombol lari (Sprint) secara beruntun";
            case ALCHEMY -> "Shift + Klik Kanan memegang Botol Ramuan (Potion)";
            case ENCHANTING -> "Klik Kanan Meja Sihir sambil memegang Kitab Kuno";
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
