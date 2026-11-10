package net.sakura.weapons.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.sakura.weapons.client.config.TakashaMasterConfig;
import net.sakura.weapons.client.drop.DropFilterEntry;
import net.sakura.weapons.client.drop.TakashaAutoDropHandler;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TakashaAutoDropSubScreen extends Screen {

    public enum DropCategory {
        MINING("⛏ Mining"),
        FISHING("🎣 Fishing"),
        KILL("⚔ Kill Mob");

        private final String label;
        DropCategory(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final Screen parentScreen;
    private final TakashaMasterConfig config;

    private DropCategory currentCategory = DropCategory.MINING;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 6;

    private Button tabMiningBtn;
    private Button tabFishingBtn;
    private Button tabKillBtn;

    private Button categoryToggleBtn;
    private EditBox inputField;
    private Button addInputBtn;
    private Button quickAddFromHandBtn;

    private Button serverDropTestBtn;
    private Button protectValuablesBtn;
    private Button smartFoodReserveBtn;

    private final List<Button> itemDeleteButtons = new ArrayList<>();
    private Button prevPageBtn;
    private Button nextPageBtn;
    private Button backBtn;

    private String notificationMessage = "";
    private long notificationTime = 0L;

    public TakashaAutoDropSubScreen(Screen parentScreen) {
        super(Component.literal("Takasha Auto Drop Inventory Settings"));
        this.parentScreen = parentScreen;
        this.config = TakashaMasterConfig.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        int panelW = 420;
        int panelH = 340;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        this.itemDeleteButtons.clear();

        // Tabs
        int tabW = 126;
        this.tabMiningBtn = Button.builder(
            Component.literal(DropCategory.MINING.getLabel()),
            btn -> switchCategory(DropCategory.MINING)
        ).bounds(left + 14, top + 28, tabW, 20).build();
        this.addRenderableWidget(this.tabMiningBtn);

        this.tabFishingBtn = Button.builder(
            Component.literal(DropCategory.FISHING.getLabel()),
            btn -> switchCategory(DropCategory.FISHING)
        ).bounds(left + 146, top + 28, tabW, 20).build();
        this.addRenderableWidget(this.tabFishingBtn);

        this.tabKillBtn = Button.builder(
            Component.literal(DropCategory.KILL.getLabel()),
            btn -> switchCategory(DropCategory.KILL)
        ).bounds(left + 278, top + 28, tabW, 20).build();
        this.addRenderableWidget(this.tabKillBtn);

        // Row 2: Category status toggle & Kaizen server drop test
        this.categoryToggleBtn = Button.builder(
            getCategoryStatusLabel(),
            btn -> {
                toggleCurrentCategory();
                btn.setMessage(getCategoryStatusLabel());
            }
        ).bounds(left + 14, top + 54, 190, 20).build();
        this.addRenderableWidget(this.categoryToggleBtn);

        this.serverDropTestBtn = Button.builder(
            getServerDropGatekeeperLabel(),
            btn -> {
                config.serverDropGatekeeperEnabled = !config.serverDropGatekeeperEnabled;
                config.save();
                btn.setMessage(getServerDropGatekeeperLabel());
                if (config.serverDropGatekeeperEnabled) {
                    TakashaAutoDropHandler.getInstance().ensureServerDropActive(this.minecraft);
                    setNotice("Gatekeeper /drop diaktifkan (Mengirim command /drop)...");
                } else {
                    setNotice("Gatekeeper /drop dinonaktifkan (Tidak akan mengirim /drop ke server).");
                }
            }
        ).bounds(left + 214, top + 54, 190, 20)
        .tooltip(Tooltip.create(Component.literal("Jika AKTIF, mod memverifikasi /drop ke server Kaizen sebelum membuang item. Jika NONAKTIF, mod tidak akan pernah mengirim command /drop.")))
        .build();
        this.addRenderableWidget(this.serverDropTestBtn);

        // Row 3: EditBox Input & Quick Add from Hand
        this.inputField = new EditBox(this.font, left + 14, top + 80, 180, 20, Component.literal("Input Item"));
        this.inputField.setMaxLength(128);
        this.inputField.setHint(Component.literal("ID / nama item..."));
        this.addRenderableWidget(this.inputField);

        this.addInputBtn = Button.builder(
            Component.literal("+ Tambah"),
            btn -> addFromInputField()
        ).bounds(left + 198, top + 80, 68, 20).build();
        this.addRenderableWidget(this.addInputBtn);

        this.quickAddFromHandBtn = Button.builder(
            Component.literal("📥 Ambil dari Tangan"),
            btn -> addFromHeldItem()
        ).bounds(left + 270, top + 80, 134, 20)
        .tooltip(Tooltip.create(Component.literal("Ekstrak otomatis item yang sedang dipegang di tangan (mendukung CustomModelData & Custom Name)")))
        .build();
        this.addRenderableWidget(this.quickAddFromHandBtn);

        // Row 4: Protection toggle & Smart Food Reserve
        this.protectValuablesBtn = Button.builder(
            Component.literal("Proteksi Berharga: " + (config.protectEnchantedAndValuables ? "§aON" : "§cOFF")),
            btn -> {
                config.protectEnchantedAndValuables = !config.protectEnchantedAndValuables;
                config.save();
                btn.setMessage(Component.literal("Proteksi Berharga: " + (config.protectEnchantedAndValuables ? "§aON" : "§cOFF")));
            }
        ).bounds(left + 14, top + 104, 192, 18)
        .tooltip(Tooltip.create(Component.literal("Jangan pernah membuang item ber-enchantment, senjata, perkakas, armor, atau item ber-lore langka")))
        .build();
        this.addRenderableWidget(this.protectValuablesBtn);

        this.smartFoodReserveBtn = Button.builder(
            Component.literal("Sisakan 1 Slot Makanan: " + (config.autoDropSmartFoodReserve ? "§aON" : "§cOFF")),
            btn -> {
                config.autoDropSmartFoodReserve = !config.autoDropSmartFoodReserve;
                config.save();
                btn.setMessage(Component.literal("Sisakan 1 Slot Makanan: " + (config.autoDropSmartFoodReserve ? "§aON" : "§cOFF")));
            }
        ).bounds(left + 212, top + 104, 192, 18)
        .tooltip(Tooltip.create(Component.literal("Menyisakan minimal 1 slot makanan di tas agar pemain tidak kelaparan untuk Auto Eat, berapapun isinya")))
        .build();
        this.addRenderableWidget(this.smartFoodReserveBtn);

        // Populate item list rows (6 rows per page)
        refreshItemRows(left, top);

        // Pagination & Back Controls
        int bottomY = top + panelH - 24;
        this.prevPageBtn = Button.builder(
            Component.literal("◀ Prev"),
            btn -> {
                if (page > 0) {
                    page--;
                    this.clearWidgets();
                    this.init();
                }
            }
        ).bounds(left + 14, bottomY, 60, 18).build();
        this.addRenderableWidget(this.prevPageBtn);

        this.nextPageBtn = Button.builder(
            Component.literal("Next ▶"),
            btn -> {
                int total = getActiveList().size();
                int maxPage = Math.max(0, (total - 1) / ITEMS_PER_PAGE);
                if (page < maxPage) {
                    page++;
                    this.clearWidgets();
                    this.init();
                }
            }
        ).bounds(left + 80, bottomY, 60, 18).build();
        this.addRenderableWidget(this.nextPageBtn);

        this.backBtn = Button.builder(
            Component.literal("◀ Kembali"),
            btn -> this.onClose()
        ).bounds(left + panelW - 84, bottomY, 70, 18).build();
        this.addRenderableWidget(this.backBtn);

        updateTabColors();
    }

    private void switchCategory(DropCategory cat) {
        this.currentCategory = cat;
        this.page = 0;
        this.clearWidgets();
        this.init();
    }

    private void updateTabColors() {
        tabMiningBtn.setMessage(Component.literal((currentCategory == DropCategory.MINING ? "§a▶ " : "§7") + DropCategory.MINING.getLabel()));
        tabFishingBtn.setMessage(Component.literal((currentCategory == DropCategory.FISHING ? "§a▶ " : "§7") + DropCategory.FISHING.getLabel()));
        tabKillBtn.setMessage(Component.literal((currentCategory == DropCategory.KILL ? "§a▶ " : "§7") + DropCategory.KILL.getLabel()));
    }

    private Set<String> getActiveList() {
        return switch (currentCategory) {
            case MINING -> config.autoDropMiningList;
            case FISHING -> config.autoDropFishingList;
            case KILL -> config.autoDropKillList;
        };
    }

    private Component getCategoryStatusLabel() {
        boolean active = switch (currentCategory) {
            case MINING -> config.autoDropMiningEnabled;
            case FISHING -> config.autoDropFishingEnabled;
            case KILL -> config.autoDropKillEnabled;
        };
        return Component.literal("Status " + currentCategory.name() + ": " + (active ? "§aAKTIF" : "§cNONAKTIF"));
    }

    private Component getServerDropGatekeeperLabel() {
        return config.serverDropGatekeeperEnabled
            ? Component.literal("Gatekeeper /drop: §aAKTIF")
            : Component.literal("Gatekeeper /drop: §cNONAKTIF");
    }

    private void toggleCurrentCategory() {
        switch (currentCategory) {
            case MINING -> config.autoDropMiningEnabled = !config.autoDropMiningEnabled;
            case FISHING -> config.autoDropFishingEnabled = !config.autoDropFishingEnabled;
            case KILL -> config.autoDropKillEnabled = !config.autoDropKillEnabled;
        }
        config.save();
    }

    private void addFromInputField() {
        String text = inputField.getValue().trim();
        if (text.isEmpty()) return;
        DropFilterEntry entry = DropFilterEntry.parse(text);
        if (entry != null && !entry.getRawPattern().isEmpty()) {
            getActiveList().add(entry.getRawPattern());
            config.save();
            inputField.setValue("");
            setNotice("§aFilter berhasil ditambahkan: " + entry.getDisplayLabel());
            this.clearWidgets();
            this.init();
        }
    }

    private void addFromHeldItem() {
        if (this.minecraft.player == null) return;
        ItemStack held = this.minecraft.player.getMainHandItem();
        if (held.isEmpty()) {
            held = this.minecraft.player.getOffhandItem();
        }
        if (held.isEmpty()) {
            setNotice("§cTidak ada item yang dipegang di tangan!");
            return;
        }

        DropFilterEntry entry = DropFilterEntry.fromItemStack(held);
        if (entry != null) {
            getActiveList().add(entry.getRawPattern());
            config.save();
            setNotice("§aItem tangan berhasil ditambahkan: " + entry.getDisplayLabel());
            this.clearWidgets();
            this.init();
        }
    }

    private void refreshItemRows(int left, int top) {
        List<String> items = new ArrayList<>(getActiveList());
        int startIdx = page * ITEMS_PER_PAGE;
        int startY = top + 130;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int idx = startIdx + i;
            if (idx >= items.size()) break;

            String pattern = items.get(idx);
            int rowY = startY + (i * 24);

            Button deleteBtn = Button.builder(
                Component.literal("§c✕"),
                btn -> {
                    getActiveList().remove(pattern);
                    config.save();
                    this.clearWidgets();
                    this.init();
                }
            ).bounds(left + 380, rowY, 24, 20)
            .tooltip(Tooltip.create(Component.literal("Hapus item dari filter drop")))
            .build();

            this.itemDeleteButtons.add(deleteBtn);
            this.addRenderableWidget(deleteBtn);
        }
    }

    private void setNotice(String msg) {
        this.notificationMessage = msg;
        this.notificationTime = net.minecraft.util.Util.getMillis();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractor.fill(0, 0, this.width, this.height, 0x88000000);

        int panelW = 420;
        int panelH = 340;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        extractor.fill(left, top, left + panelW, top + panelH, 0xEE14121E);
        extractor.outline(left, top, panelW, panelH, 0xFFE082A8);

        // Header
        extractor.fill(left, top, left + panelW, top + 24, 0xFF241528);
        extractor.text(this.font, "🗑 Auto Drop Item Inventory (Mining, Fishing, Kill)", left + 10, top + 7, 0xFFFFB6C1, false);

        // Kaizen Server /drop Status text
        TakashaAutoDropHandler.ServerDropStatus dropStatus = TakashaAutoDropHandler.getInstance().getServerDropStatus();
        String statusStr = switch (dropStatus) {
            case ACTIVE -> "§aAKTIF";
            case CHECKING -> "§eMEMERIKSA...";
            case INACTIVE -> "§cNONAKTIF";
            case UNSUPPORTED -> "§cTIDAK DIDUKUNG";
            case UNKNOWN -> (config.serverDropActive ? "§aAKTIF" : "§7BELUM CEK");
        };
        extractor.text(this.font, "Status /drop: " + statusStr, left + 150, top + bottomPanelOffset(panelH), 0xFFFFFFFF, false);

        // Render Item rows
        List<String> items = new ArrayList<>(getActiveList());
        int startIdx = page * ITEMS_PER_PAGE;
        int startY = top + 130;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int idx = startIdx + i;
            if (idx >= items.size()) break;

            String pattern = items.get(idx);
            DropFilterEntry entry = DropFilterEntry.parse(pattern);
            int rowY = startY + (i * 24);

            extractor.fill(left + 14, rowY, left + 376, rowY + 20, 0x44222233);
            extractor.outline(left + 14, rowY, 362, 20, 0x44555577);
            extractor.text(this.font, "• " + entry.getDisplayLabel(), left + 20, rowY + 6, 0xFFEFEFEF, false);
        }

        if (items.isEmpty()) {
            extractor.text(this.font, "§7(Tidak ada item dalam daftar kategori ini)", left + 90, startY + 40, 0xFF888888, false);
        }

        // Notification message
        if (!notificationMessage.isEmpty() && (net.minecraft.util.Util.getMillis() - notificationTime < 3000L)) {
            extractor.text(this.font, notificationMessage, left + 14, top + panelH - 42, 0xFFFFFF55, false);
        }

        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
    }

    private int bottomPanelOffset(int panelH) {
        return panelH - 20;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.inputField != null && this.inputField.isFocused()) {
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                addFromInputField();
                return true;
            }
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (this.parentScreen != null) {
            this.minecraft.gui.setScreen(this.parentScreen);
        } else {
            super.onClose();
        }
    }
}
