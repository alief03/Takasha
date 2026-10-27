package net.takasha.rpg.client.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;
import net.takasha.rpg.network.ClaimQuestC2SPayload;
import net.takasha.rpg.network.EquipTitleC2SPayload;
import net.takasha.rpg.quest.TakashaQuest;
import net.takasha.rpg.quest.TakashaQuestManager;
import net.takasha.rpg.skills.AuraSkillType;
import net.takasha.rpg.title.TakashaTitle;
import net.takasha.rpg.title.TakashaTitleManager;
import net.takasha.rpg.world.event.RpgEventInstance;
import net.takasha.rpg.world.event.RpgEventType;
import net.takasha.rpg.world.event.TakashaEventManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GrimoireScreen
 * Antarmuka Grimoire of Realms v1.8.0 dengan 5 Tab:
 * 0: [⚔ AuraSkills] - Status 11 Keahlian, XP & Visual Progress Bar
 * 1: [📜 Quests] - Papan Misi (Kemajuan Bab 1-4, Harian, Mingguan, Legendaris) dengan Tombol Klaim Hadiah
 * 2: [👑 Titles] - 16 Gelar Kedaulatan, Syarat Buka, Efek Trait Pasif & Tombol Pakai/Lepas Gelar
 * 3: [🌍 Events & Arena] - Live Status World Events (Acak & Berkala), Multiplier & Status Dungeon Arena
 * 4: [🏠 Homes] - Manajemen Titik Home Dimensial & Tombol Cepat Teleportasi
 */
public class GrimoireScreen extends Screen {

    private int activeTab = 0; // 0: Skills, 1: Quests, 2: Titles, 3: Events, 4: Homes
    private int questCategory = 0; // 0: Kemajuan Bab 1-4, 1: Harian & Mingguan, 2: 16 Legendaris
    private int questPage = 0;
    private int titlePage = 0;

    private Button openSkillsGuiBtn;
    private final List<Button> dynamicActionButtons = new ArrayList<>();

    public GrimoireScreen() {
        super(Component.literal("Grimoire of Realms — Takasha RPG"));
    }

    private TakashaRpgData getLivePlayerData() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer() && mc.player != null) {
            var server = mc.getSingleplayerServer();
            if (server != null) {
                ServerPlayer sp = server.getPlayerList().getPlayer(mc.player.getUUID());
                if (sp != null) {
                    return TakashaRpgManager.getOrCreateData(sp);
                }
            }
        }
        return null;
    }

    @Override
    protected void init() {
        super.init();
        if (this.minecraft != null && !this.minecraft.hasSingleplayerServer()) {
            this.onClose();
            return;
        }
        rebuildAllWidgets();
    }

    private void rebuildAllWidgets() {
        this.clearWidgets();
        this.dynamicActionButtons.clear();

        int startX = this.width / 2 - 190;
        int topY = 16;
        int tabW = 74;

        // 5 Tab Header Buttons
        this.addRenderableWidget(Button.builder(Component.literal(activeTab == 0 ? "§e§l⚔ Aura" : "⚔ Aura"), b -> {
            this.activeTab = 0;
            rebuildDynamicButtons();
        }).bounds(startX, topY, tabW, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(activeTab == 1 ? "§e§l📜 Quests" : "📜 Quests"), b -> {
            this.activeTab = 1;
            this.questPage = 0;
            rebuildDynamicButtons();
        }).bounds(startX + 76, topY, tabW, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(activeTab == 2 ? "§e§l👑 Titles" : "👑 Titles"), b -> {
            this.activeTab = 2;
            this.titlePage = 0;
            rebuildDynamicButtons();
        }).bounds(startX + 152, topY, tabW, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(activeTab == 3 ? "§e§l🌍 Events" : "🌍 Events"), b -> {
            this.activeTab = 3;
            rebuildDynamicButtons();
        }).bounds(startX + 228, topY, tabW, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(activeTab == 4 ? "§e§l🏠 Homes" : "🏠 Homes"), b -> {
            this.activeTab = 4;
            rebuildDynamicButtons();
        }).bounds(startX + 304, topY, tabW, 20).build());

        // Quick Command Action in AuraSkills tab
        this.openSkillsGuiBtn = Button.builder(Component.literal("⚔ Buka GUI Pohon AuraSkills [/skills]"), b -> {
            if (this.minecraft.player != null && this.minecraft.player.connection != null) {
                this.minecraft.player.connection.sendCommand("skills");
            }
            this.onClose();
        }).bounds(this.width / 2 - 130, this.height - 48, 260, 18).build();
        this.addRenderableWidget(this.openSkillsGuiBtn);

        // Close Button
        this.addRenderableWidget(Button.builder(Component.literal("Tutup [ESC]"), b -> this.onClose())
                .bounds(this.width / 2 - 40, this.height - 26, 80, 18).build());

        rebuildDynamicButtons();
    }

    private void rebuildDynamicButtons() {
        for (Button btn : dynamicActionButtons) {
            this.removeWidget(btn);
        }
        dynamicActionButtons.clear();

        if (openSkillsGuiBtn != null) {
            openSkillsGuiBtn.visible = (activeTab == 0);
        }

        int panelX = this.width / 2 - 190;
        int panelY = 40;
        int panelW = 380;
        TakashaRpgData data = getLivePlayerData();

        if (activeTab == 1) { // Quests Tab
            // Sub-category filter buttons
            Button cat0 = Button.builder(Component.literal(questCategory == 0 ? "§a[Bab 1-4]" : "Bab 1-4"), b -> {
                questCategory = 0;
                questPage = 0;
                rebuildDynamicButtons();
            }).bounds(panelX + 12, panelY + 22, 65, 16).build();

            Button cat1 = Button.builder(Component.literal(questCategory == 1 ? "§a[Harian]" : "Harian"), b -> {
                questCategory = 1;
                questPage = 0;
                rebuildDynamicButtons();
            }).bounds(panelX + 80, panelY + 22, 65, 16).build();

            Button cat2 = Button.builder(Component.literal(questCategory == 2 ? "§a[Legenda]" : "Legenda"), b -> {
                questCategory = 2;
                questPage = 0;
                rebuildDynamicButtons();
            }).bounds(panelX + 148, panelY + 22, 65, 16).build();

            dynamicActionButtons.add(cat0);
            dynamicActionButtons.add(cat1);
            dynamicActionButtons.add(cat2);
            this.addRenderableWidget(cat0);
            this.addRenderableWidget(cat1);
            this.addRenderableWidget(cat2);

            // Ambil daftar quest sesuai kategori
            List<TakashaQuest> filtered = getQuestsForCategory(questCategory);
            int pageSize = 3;
            int totalPages = Math.max(1, (filtered.size() + pageSize - 1) / pageSize);
            if (questPage >= totalPages) questPage = 0;

            // Tombol Navigasi Halaman Quest
            if (totalPages > 1) {
                Button prevQ = Button.builder(Component.literal("◄"), b -> {
                    if (questPage > 0) {
                        questPage--;
                        rebuildDynamicButtons();
                    }
                }).bounds(panelX + panelW - 60, panelY + 22, 22, 16).build();
                prevQ.active = (questPage > 0);

                Button nextQ = Button.builder(Component.literal("►"), b -> {
                    if (questPage < totalPages - 1) {
                        questPage++;
                        rebuildDynamicButtons();
                    }
                }).bounds(panelX + panelW - 34, panelY + 22, 22, 16).build();
                nextQ.active = (questPage < totalPages - 1);

                dynamicActionButtons.add(prevQ);
                dynamicActionButtons.add(nextQ);
                this.addRenderableWidget(prevQ);
                this.addRenderableWidget(nextQ);
            }

            int startIndex = questPage * pageSize;
            int endIndex = Math.min(startIndex + pageSize, filtered.size());
            int cardY = panelY + 44;

            for (int i = startIndex; i < endIndex; i++) {
                TakashaQuest q = filtered.get(i);
                int currentProg = data != null ? data.getQuestProgress(q.getId()) : 0;
                boolean isCompleted = data != null && data.isQuestCompleted(q.getId());
                boolean canClaim = !isCompleted && currentProg >= q.getTarget();

                if (canClaim) {
                    final String qId = q.getId();
                    Button claimBtn = Button.builder(Component.literal("§a§lKlaim Hadiah"), b -> {
                        claimQuest(qId);
                    }).bounds(panelX + panelW - 95, cardY + 12, 85, 18).build();
                    dynamicActionButtons.add(claimBtn);
                    this.addRenderableWidget(claimBtn);
                }
                cardY += 46;
            }
        } else if (activeTab == 2) { // Titles Tab
            TakashaTitle[] allTitles = TakashaTitle.values();
            int pageSize = 4;
            int totalPages = (allTitles.length + pageSize - 1) / pageSize;

            // Tombol Navigasi Title
            Button prevT = Button.builder(Component.literal("◄ Sebelumnya"), b -> {
                if (titlePage > 0) {
                    titlePage--;
                    rebuildDynamicButtons();
                }
            }).bounds(panelX + 12, panelY + panelH() - 28, 85, 18).build();
            prevT.active = (titlePage > 0);

            Button nextT = Button.builder(Component.literal("Berikutnya ►"), b -> {
                if (titlePage < totalPages - 1) {
                    titlePage++;
                    rebuildDynamicButtons();
                }
            }).bounds(panelX + panelW - 97, panelY + panelH() - 28, 85, 18).build();
            nextT.active = (titlePage < totalPages - 1);

            dynamicActionButtons.add(prevT);
            dynamicActionButtons.add(nextT);
            this.addRenderableWidget(prevT);
            this.addRenderableWidget(nextT);

            int startIdx = titlePage * pageSize;
            int endIdx = Math.min(startIdx + pageSize, allTitles.length);
            int itemY = panelY + 38;

            for (int i = startIdx; i < endIdx; i++) {
                TakashaTitle t = allTitles[i];
                boolean isUnlocked = data != null && (data.isTitleUnlocked(t.getCode()) || isTitleAutoUnlocked(data, t));
                boolean isEquipped = data != null && (t.getCode().equalsIgnoreCase(data.getActiveTitle()) || t.getTitleName().equalsIgnoreCase(data.getActiveTitle()));

                if (isUnlocked) {
                    final String tCode = t.getCode();
                    if (isEquipped) {
                        Button unequipBtn = Button.builder(Component.literal("§c[Lepas]"), b -> {
                            equipTitle("");
                        }).bounds(panelX + panelW - 75, itemY + 4, 65, 18).build();
                        dynamicActionButtons.add(unequipBtn);
                        this.addRenderableWidget(unequipBtn);
                    } else {
                        Button equipBtn = Button.builder(Component.literal("§a[Pakai]"), b -> {
                            equipTitle(tCode);
                        }).bounds(panelX + panelW - 75, itemY + 4, 65, 18).build();
                        dynamicActionButtons.add(equipBtn);
                        this.addRenderableWidget(equipBtn);
                    }
                }
                itemY += 34;
            }
        } else if (activeTab == 3) { // Events & Arena Tab
            // Tombol Refresh Status Event & Arena
            Button refreshBtn = Button.builder(Component.literal("🔄 Perbarui Info"), b -> {
                rebuildDynamicButtons();
            }).bounds(panelX + panelW - 110, panelY + 20, 100, 18).build();
            dynamicActionButtons.add(refreshBtn);
            this.addRenderableWidget(refreshBtn);
        } else if (activeTab == 4) { // Homes Tab
            if (data != null && !data.getHomes().isEmpty()) {
                int hY = panelY + 54;
                int idx = 0;
                for (String homeName : data.getHomes().keySet()) {
                    if (idx >= 4) break;
                    String targetHome = homeName;
                    Button tpBtn = Button.builder(Component.literal("Teleport ke '" + targetHome + "'"), b -> {
                        if (this.minecraft.player != null && this.minecraft.player.connection != null) {
                            this.minecraft.player.connection.sendCommand("home " + targetHome);
                        }
                        this.onClose();
                    }).bounds(panelX + panelW - 145, hY + (idx * 24), 135, 18).build();
                    dynamicActionButtons.add(tpBtn);
                    this.addRenderableWidget(tpBtn);
                    idx++;
                }
            }
        }
    }

    private int panelH() {
        return this.height - 80;
    }

    private void claimQuest(String questId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer() && mc.player != null) {
            var server = mc.getSingleplayerServer();
            if (server != null) {
                ServerPlayer sp = server.getPlayerList().getPlayer(mc.player.getUUID());
                if (sp != null) {
                    TakashaQuestManager.claimReward(sp, questId);
                }
            }
        } else {
            // Multiplayer send C2S payload
            ClientPlayNetworking.send(new ClaimQuestC2SPayload(questId));
        }
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.2f);
        }
        rebuildDynamicButtons();
    }

    private void equipTitle(String titleCode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer() && mc.player != null) {
            var server = mc.getSingleplayerServer();
            if (server != null) {
                ServerPlayer sp = server.getPlayerList().getPlayer(mc.player.getUUID());
                if (sp != null) {
                    TakashaTitleManager.handleEquipTitleRequest(sp, titleCode);
                }
            }
        } else {
            ClientPlayNetworking.send(new EquipTitleC2SPayload(titleCode));
        }
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f);
        }
        rebuildDynamicButtons();
    }

    private boolean isTitleAutoUnlocked(TakashaRpgData data, TakashaTitle t) {
        if (data == null || t == null) return false;
        return switch (t.getCode()) {
            case "G-01" -> data.getTotalSkillLevel() >= 100;
            case "G-02" -> data.getMaxHomes() >= 5;
            case "G-03" -> data.getBalance() >= 10000.0;
            case "G-04" -> data.getStat("mobsKilled") >= 250;
            case "G-05" -> data.getStat("netherMobsKilled") >= 150;
            case "G-06" -> data.getStat("oresMinedDeep") >= 100;
            case "G-07" -> data.getSkillLevel(AuraSkillType.DEFENSE) >= 50;
            case "G-08" -> data.getGems() >= 250;
            case "G-09" -> data.getSkillLevel(AuraSkillType.FIGHTING) >= 100;
            case "G-10" -> data.getStat("arenaWavesCleared") >= 20;
            case "G-11" -> data.getStat("sakuraWeaponKills") >= 500;
            case "G-12" -> data.getStat("bossesKilled") >= 5;
            case "G-13" -> data.getStat("magicDustUsed") >= 50;
            case "G-14" -> data.getSkillLevel(AuraSkillType.ARCHERY) >= 100;
            case "G-15" -> data.getTotalSkillLevel() >= 500;
            case "G-16" -> data.getTotalSkillLevel() >= 800 && data.getBalance() >= 100000.0;
            default -> false;
        };
    }

    private List<TakashaQuest> getQuestsForCategory(int cat) {
        List<TakashaQuest> list = new ArrayList<>();
        for (TakashaQuest q : TakashaQuestManager.getAllQuests()) {
            if (cat == 0 && q.getType() == TakashaQuest.QuestType.KEMAJUAN) {
                list.add(q);
            } else if (cat == 1 && (q.getType() == TakashaQuest.QuestType.DAILY || q.getType() == TakashaQuest.QuestType.WEEKLY)) {
                list.add(q);
            } else if (cat == 2 && q.getType() == TakashaQuest.QuestType.LEGENDARY) {
                list.add(q);
            }
        }
        return list;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null && !this.minecraft.hasSingleplayerServer()) {
            return;
        }
        // Dark Obsidian Fantasy Background
        extractor.fill(0, 0, this.width, this.height, 0xDD08080C);

        int panelX = this.width / 2 - 190;
        int panelY = 40;
        int panelW = 380;
        int panelH = panelH();

        // Main Frame Box Panel
        extractor.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xEE141620);
        extractor.outline(panelX, panelY, panelW, panelH, 0xFFEAB308); // Gold Filigree Border

        TakashaRpgData data = getLivePlayerData();
        double balance = data != null ? data.getBalance() : net.takasha.rpg.client.hud.TakashaRpgScoreboard.getBalance();
        int gems = data != null ? data.getGems() : net.takasha.rpg.client.hud.TakashaRpgScoreboard.getGems();
        double mana = data != null ? data.getCurrentMana() : net.takasha.rpg.client.hud.TakashaRpgScoreboard.getCurrentMana();
        double maxMana = data != null ? data.getMaxMana() : net.takasha.rpg.client.hud.TakashaRpgScoreboard.getMaxMana();
        int totalSkills = data != null ? data.getTotalSkillLevel() : net.takasha.rpg.client.hud.TakashaRpgScoreboard.getTotalSkillLevel();

        // Header Status Bar
        String statsBar = String.format("§6💰 %.1f Coins  §a💎 %d Gems  §b✦ %.0f/%.0f Mana  §e⚔ Total Lvl: %d", balance, gems, mana, maxMana, totalSkills);
        extractor.text(this.font, Component.literal(statsBar), panelX + 12, panelY + 6, 0xFFE0E0E0, false);
        extractor.fill(panelX + 8, panelY + 18, panelX + panelW - 8, panelY + 19, 0xFF4A3B22);

        int textY = panelY + 24;
        int textX = panelX + 14;

        switch (activeTab) {
            case 0 -> renderAuraSkillsTab(extractor, textX, textY, data, totalSkills);
            case 1 -> renderQuestsTab(extractor, textX, textY, data);
            case 2 -> renderTitlesTab(extractor, textX, textY, data);
            case 3 -> renderEventsTab(extractor, textX, textY, panelW);
            case 4 -> renderHomesTab(extractor, textX, textY, data, panelW);
        }

        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
    }

    private void renderAuraSkillsTab(GuiGraphicsExtractor extractor, int textX, int textY, TakashaRpgData data, int totalSkills) {
        extractor.text(this.font, Component.literal("§6§l[AURASKILLS MASTER] §7(Total Level: §e" + totalSkills + "§7 | Rata-rata: §e" + (totalSkills / 11) + "§7)"), textX, textY, 0xFFFFFFFF, false);
        textY += 14;

        if (data != null) {
            AuraSkillType[] skills = AuraSkillType.values();
            int lineY = textY;
            for (AuraSkillType skill : skills) {
                int lvl = data.getSkillLevel(skill);
                double currentXp = data.getSkillXp(skill);
                double reqXp = skill.getXpForNextLevel(lvl);
                String bar = renderProgressBar((int) currentXp, (int) reqXp, 10);

                String line = String.format("%s §fLvl §e%d §7XP: %s", skill.getDisplayName(), lvl, bar);
                extractor.text(this.font, Component.literal(line), textX, lineY, 0xFFE0E0E0, false);
                lineY += 11;
            }
        } else {
            extractor.text(this.font, Component.literal("§eBuka menu /skills untuk menyinkronkan status keahlian."), textX, textY, 0xFFCCCCCC, false);
        }
    }

    private void renderQuestsTab(GuiGraphicsExtractor extractor, int textX, int textY, TakashaRpgData data) {
        String catName = switch (questCategory) {
            case 0 -> "Kemajuan Bab 1-4 (PRD 13.9)";
            case 1 -> "Misi Harian & Mingguan";
            default -> "16 Misi Legendaris Kedaulatan";
        };
        extractor.text(this.font, Component.literal("§6§l[PAPAN MISI] §e" + catName), textX, textY, 0xFFFFFFFF, false);

        List<TakashaQuest> filtered = getQuestsForCategory(questCategory);
        int pageSize = 3;
        int totalPages = Math.max(1, (filtered.size() + pageSize - 1) / pageSize);
        int startIndex = questPage * pageSize;
        int endIndex = Math.min(startIndex + pageSize, filtered.size());

        int cardY = textY + 22;
        int panelX = this.width / 2 - 190;
        int panelY = 40;
        int panelW = 380;

        for (int i = startIndex; i < endIndex; i++) {
            TakashaQuest q = filtered.get(i);
            int currentProg = data != null ? data.getQuestProgress(q.getId()) : 0;
            boolean isCompleted = data != null && data.isQuestCompleted(q.getId());

            // Card background outline
            extractor.fill(panelX + 10, cardY - 2, panelX + panelW - 10, cardY + 40, 0x550F111A);
            extractor.outline(panelX + 10, cardY - 2, panelW - 20, 42, isCompleted ? 0xFF22C55E : 0xFF3B82F6);

            // Quest Title & Badge
            String header = "§e[" + q.getCode() + "] §f" + q.getTitle();
            extractor.text(this.font, Component.literal(header), textX + 2, cardY + 2, 0xFFFFFFFF, false);

            // Progress Bar Visual
            String progBar = renderProgressBar(Math.min(currentProg, q.getTarget()), q.getTarget(), 14);
            extractor.text(this.font, Component.literal("  " + progBar), textX + 2, cardY + 13, 0xFFCCCCCC, false);

            // Rewards
            String rewardText = String.format("  §6Hadiah: §e+%.0f Coins §8| §a+%d Gems §8| §b%s",
                    q.getRewardCoins(), q.getRewardGems(), q.getRewardItemSummary());
            extractor.text(this.font, Component.literal(rewardText), textX + 2, cardY + 25, 0xFFAAAAAA, false);

            if (isCompleted) {
                extractor.text(this.font, Component.literal("§a✔ DIKLAIM"), panelX + panelW - 80, cardY + 16, 0xFF22C55E, false);
            }

            cardY += 46;
        }

        String pageInfo = "Halaman " + (questPage + 1) + " / " + totalPages;
        extractor.text(this.font, Component.literal("§7" + pageInfo), panelX + panelW / 2 - 30, panelY + panelH() - 14, 0xFF888888, false);
    }

    private void renderTitlesTab(GuiGraphicsExtractor extractor, int textX, int textY, TakashaRpgData data) {
        String equippedTitle = data != null && !data.getActiveTitle().isEmpty() ? data.getActiveTitle() : "Belum Ada";
        extractor.text(this.font, Component.literal("§6§l[16 GELAR KEDAULATAN] §7(Aktif: §e" + equippedTitle + "§7)"), textX, textY, 0xFFFFFFFF, false);

        TakashaTitle[] allTitles = TakashaTitle.values();
        int pageSize = 4;
        int totalPages = (allTitles.length + pageSize - 1) / pageSize;
        int startIdx = titlePage * pageSize;
        int endIdx = Math.min(startIdx + pageSize, allTitles.length);

        int itemY = textY + 16;
        int panelX = this.width / 2 - 190;
        int panelY = 40;
        int panelW = 380;

        for (int i = startIdx; i < endIdx; i++) {
            TakashaTitle t = allTitles[i];
            boolean isUnlocked = data != null && (data.isTitleUnlocked(t.getCode()) || isTitleAutoUnlocked(data, t));
            boolean isEquipped = data != null && (t.getCode().equalsIgnoreCase(data.getActiveTitle()) || t.getTitleName().equalsIgnoreCase(data.getActiveTitle()));

            // Card outline
            extractor.fill(panelX + 10, itemY - 2, panelX + panelW - 10, itemY + 30, 0x440B0C12);
            extractor.outline(panelX + 10, itemY - 2, panelW - 20, 32, isEquipped ? 0xFF22C55E : (isUnlocked ? 0xFFEAB308 : 0xFF475569));

            String badge = isEquipped ? "§a[★ AKTIF] " : (isUnlocked ? "§e[TERBUKA] " : "§8[TERKUNCI] ");
            extractor.text(this.font, Component.literal(badge + t.getColor() + t.getCode() + " " + t.getTitleName()), textX + 2, itemY + 2, 0xFFFFFFFF, false);

            extractor.text(this.font, Component.literal(" §b✦ " + t.getTraitName() + ": §7" + t.getTraitDescription()), textX + 2, itemY + 14, 0xFFAAAAAA, false);

            if (!isUnlocked) {
                extractor.text(this.font, Component.literal("§c🔒 Kunci"), panelX + panelW - 70, itemY + 8, 0xFF94A3B8, false);
            }
            itemY += 34;
        }

        String pageInfo = "Halaman " + (titlePage + 1) + " / " + totalPages;
        extractor.text(this.font, Component.literal("§7" + pageInfo), panelX + panelW / 2 - 30, panelY + panelH() - 22, 0xFF888888, false);
    }

    private void renderEventsTab(GuiGraphicsExtractor extractor, int textX, int textY, int panelW) {
        extractor.text(this.font, Component.literal("§6§l[TAKASHA WORLD EVENTS & DUNGEON ARENA]"), textX, textY, 0xFFFFFFFF, false);
        textY += 16;

        RpgEventInstance current = TakashaEventManager.getCurrentEvent();
        int panelX = this.width / 2 - 190;

        // 1. Box World Event
        extractor.fill(panelX + 10, textY, panelX + panelW - 10, textY + 70, 0x550F111A);
        int borderCol = current != null ? 0xFFDC2626 : 0xFF3B82F6;
        extractor.outline(panelX + 10, textY, panelW - 20, 70, borderCol);

        if (current != null) {
            extractor.text(this.font, Component.literal("§fStatus: " + current.getType().getDisplayName()), textX + 4, textY + 6, 0xFFFFFFFF, false);
            extractor.text(this.font, Component.literal("§7" + current.getType().getDescription()), textX + 4, textY + 18, 0xFFAAAAAA, false);
            extractor.text(this.font, Component.literal("§eSisa Waktu: §a" + RpgEventInstance.formatTime(current.getRemainingTicks())
                    + " §8| §bEXP x" + current.getExpMultiplier() + " §8| §6Koin x" + current.getCoinMultiplier()), textX + 4, textY + 32, 0xFFFFFFFF, false);
            extractor.text(this.font, Component.literal("§dEfek Khusus Aktif di Seluruh Alam Takasha!"), textX + 4, textY + 46, 0xFFE0E0E0, false);
        } else {
            extractor.text(this.font, Component.literal("§fStatus: §aDunia Sedang Tenang (Tidak ada event aktif)"), textX + 4, textY + 6, 0xFFFFFFFF, false);
            RpgEventType nextPeriodic = TakashaEventManager.getNextPeriodicType();
            int ticksLeft = TakashaEventManager.getTicksUntilNextPeriodic();
            extractor.text(this.font, Component.literal("§7Event Berkala Berikutnya: " + nextPeriodic.getDisplayName()), textX + 4, textY + 20, 0xFFAAAAAA, false);
            extractor.text(this.font, Component.literal("§fPerkiraan Tiba Dalam: §e" + RpgEventInstance.formatTime(ticksLeft)), textX + 4, textY + 34, 0xFFFFFFFF, false);
            extractor.text(this.font, Component.literal("§8(Event acak seperti Blood Moon atau Goblin Hunt dapat pecah sewaktu-waktu)"), textX + 4, textY + 48, 0xFF888888, false);
        }

        // 2. Box Dungeon Arena
        textY += 78;
        extractor.fill(panelX + 10, textY, panelX + panelW - 10, textY + 65, 0x550F111A);
        extractor.outline(panelX + 10, textY, panelW - 20, 65, 0xFFEAB308);

        extractor.text(this.font, Component.literal("§6§l[ADVANCED DUNGEON ARENA (ADA)]"), textX + 4, textY + 6, 0xFFFFD700, false);
        extractor.text(this.font, Component.literal("§7Gunakan §eRift Keystone §7(Common, Nightmare, Sakura) di altar kubah."), textX + 4, textY + 18, 0xFFAAAAAA, false);
        extractor.text(this.font, Component.literal("§fTotal Gelombang: §e5 Gelombang §8(Wave 5: Pertempuran Bos Akhir)"), textX + 4, textY + 30, 0xFFFFFFFF, false);
        extractor.text(this.font, Component.literal("§7Ketik §b/dungeon info §7untuk melihat status pertempuran yang aktif."), textX + 4, textY + 44, 0xFF888888, false);
    }

    private void renderHomesTab(GuiGraphicsExtractor extractor, int textX, int textY, TakashaRpgData data, int panelW) {
        int usedHomes = data != null ? data.getHomes().size() : net.takasha.rpg.client.hud.TakashaRpgScoreboard.getUsedHomes();
        int maxHomes = data != null ? data.getMaxHomes() : net.takasha.rpg.client.hud.TakashaRpgScoreboard.getMaxHomes();
        extractor.text(this.font, Component.literal("§6§l[DIMENSIONAL SETHOMES] §7(Kapasitas: §a" + usedHomes + "§7/§e" + maxHomes + " §7Homes)"), textX, textY, 0xFFFFFFFF, false);
        textY += 14;

        extractor.text(this.font, Component.literal("§7Gunakan §6Ancient Keystone §7untuk menambah kuota titik home hingga 10."), textX, textY, 0xFFAAAAAA, false);
        textY += 14;

        extractor.text(this.font, Component.literal("§eDaftar Titik Home Terdaftar:"), textX, textY, 0xFFFFD700, false);
        textY += 12;

        if (data != null && !data.getHomes().isEmpty()) {
            int hCount = 0;
            for (Map.Entry<String, TakashaRpgData.HomeLocation> entry : data.getHomes().entrySet()) {
                if (hCount >= 4) break;
                String name = entry.getKey();
                TakashaRpgData.HomeLocation loc = entry.getValue();
                String dim = loc.dimension().contains("nether") ? "§cNether" : (loc.dimension().contains("end") ? "§dThe End" : "§aOverworld");
                String coordStr = String.format("X:%.0f Y:%.0f Z:%.0f", loc.x(), loc.y(), loc.z());

                extractor.text(this.font, Component.literal("§b• " + name + " §7(" + dim + "§7)"), textX + 4, textY + (hCount * 24), 0xFFFFFFFF, false);
                extractor.text(this.font, Component.literal("  §8" + coordStr), textX + 4, textY + (hCount * 24) + 10, 0xFF888888, false);
                hCount++;
            }
        } else {
            extractor.text(this.font, Component.literal("§8Belum ada titik home yang disimpan."), textX + 4, textY, 0xFF888888, false);
            extractor.text(this.font, Component.literal("§7Ketik §b/sethome <nama> §7untuk menyimpan lokasi saat ini."), textX + 4, textY + 12, 0xFFAAAAAA, false);
        }

        int panelY = 40;
        int footerY = panelY + panelH() - 16;
        extractor.text(this.font, Component.literal("§ePerintah: §f/sethome <nama> §8| §f/home <nama> §8| §f/delhome <nama>"), textX, footerY, 0xFFDDDDDD, false);
    }

    public static String renderProgressBar(int current, int max, int totalBars) {
        if (max <= 0) max = 1;
        float ratio = Math.min(1.0f, Math.max(0.0f, (float) current / (float) max));
        int filled = Math.round(ratio * totalBars);
        StringBuilder sb = new StringBuilder("§a[");
        for (int i = 0; i < totalBars; i++) {
            if (i < filled) sb.append("█");
            else sb.append("§7░");
        }
        sb.append("§a] §e").append(Math.min(current, max)).append("/").append(max)
          .append(" §7(").append((int) (ratio * 100)).append("%)");
        return sb.toString();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
