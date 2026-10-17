package net.sakura.weapons.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.sakura.weapons.entity.TakashaMaidEntity;
import net.sakura.weapons.maid.model.MaidModelInfo;
import net.sakura.weapons.maid.model.MaidModelRegistry;
import net.sakura.weapons.registry.ModEntities;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public class TakashaMaidSubScreen extends Screen {

    private static final int ITEMS_PER_PAGE = 6;

    private final TakashaMaidEntity entity;
    private final Screen parentScreen;

    private TakashaMaidEntity previewDummy;
    private String selectedModelId;

    private EditBox searchBox;
    private final List<MaidModelInfo> filteredModels = new ArrayList<>();
    private int currentPage = 0;

    private final List<Button> modelButtons = new ArrayList<>();
    private Button prevPageBtn;
    private Button nextPageBtn;
    private Button openFolderBtn;
    private Button reloadBtn;
    private Button applyModelBtn;

    // Entity task buttons (only shown when entity != null)
    private Button sitToggleBtn;
    private Button followBtn;
    private Button farmBtn;
    private Button fishBtn;

    public TakashaMaidSubScreen(TakashaMaidEntity entity, Screen parentScreen) {
        super(Component.literal("Takasha Maid Companion (Touhou Little Maid Engine)"));
        this.entity = entity;
        this.parentScreen = parentScreen;
        this.selectedModelId = entity != null ? entity.getModelId() : MaidModelRegistry.DEFAULT_MODEL_ID;
    }

    public TakashaMaidSubScreen(TakashaMaidEntity entity) {
        this(entity, null);
    }

    public TakashaMaidSubScreen(Screen parentScreen) {
        this(null, parentScreen);
    }

    public TakashaMaidSubScreen() {
        this(null, null);
    }

    @Override
    protected void init() {
        super.init();

        MaidModelRegistry.initialize();

        if (this.entity == null && this.previewDummy == null && this.minecraft != null && this.minecraft.level != null) {
            this.previewDummy = new TakashaMaidEntity(ModEntities.TAKASHA_MAID, this.minecraft.level);
            this.previewDummy.setModelId(this.selectedModelId);
        }

        int panelW = 440;
        int panelH = 264;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        int rightX = left + 172;
        int rightW = 254;

        // Search Box
        int searchY = top + 34;
        this.searchBox = new EditBox(this.font, rightX, searchY, rightW, 16, Component.literal("Cari"));
        this.searchBox.setHint(Component.literal("🔍 Cari nama, karakter, atau ID model..."));
        this.searchBox.setResponder(val -> {
            updateFilteredList(val);
            this.currentPage = 0;
            rebuildModelButtons();
        });
        this.addRenderableWidget(this.searchBox);

        // Model List Item Buttons
        this.modelButtons.clear();
        int btnStartY = searchY + 20;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int slotY = btnStartY + (i * 22);
            final int index = i;
            Button btn = Button.builder(Component.empty(), b -> onModelButtonClicked(index))
                    .bounds(rightX, slotY, rightW, 20)
                    .build();
            this.modelButtons.add(btn);
            this.addRenderableWidget(btn);
        }

        // Pagination buttons
        int navY = btnStartY + (ITEMS_PER_PAGE * 22) + 2;
        this.prevPageBtn = Button.builder(Component.literal("◀ Prev"), b -> {
            if (this.currentPage > 0) {
                this.currentPage--;
                playUiClick();
                rebuildModelButtons();
            }
        }).bounds(rightX, navY, 60, 18).build();
        this.addRenderableWidget(this.prevPageBtn);

        this.nextPageBtn = Button.builder(Component.literal("Next ▶"), b -> {
            int maxPage = getMaxPage();
            if (this.currentPage < maxPage) {
                this.currentPage++;
                playUiClick();
                rebuildModelButtons();
            }
        }).bounds(rightX + rightW - 60, navY, 60, 18).build();
        this.addRenderableWidget(this.nextPageBtn);

        // Apply Model Button (under 3D preview)
        int previewLeft = left + 14;
        int previewW = 146;
        int applyY = top + 168;

        this.applyModelBtn = Button.builder(Component.literal("✨ Terapkan Model"), b -> {
            applySelectedModel();
            playUiClick();
        }).bounds(previewLeft, applyY, previewW, 18)
        .tooltip(Tooltip.create(Component.literal("Terapkan model terpilih ke Maid")))
        .build();
        this.addRenderableWidget(this.applyModelBtn);

        // Bottom Action Bar
        int bottomY = top + panelH - 24;

        this.openFolderBtn = Button.builder(Component.literal("📂 Buka Folder Model"), b -> {
            openModelFolder();
            playUiClick();
        }).bounds(left + 14, bottomY, 120, 18)
        .tooltip(Tooltip.create(Component.literal("Buka folder .minecraft/maid_models untuk menambahkan model JSON Bedrock & Texture PNG")))
        .build();
        this.addRenderableWidget(this.openFolderBtn);

        this.reloadBtn = Button.builder(Component.literal("🔄 Reload Model"), b -> {
            MaidModelRegistry.reloadModels();
            updateFilteredList(this.searchBox != null ? this.searchBox.getValue() : "");
            this.currentPage = 0;
            rebuildModelButtons();
            playUiClick();
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendOverlayMessage(Component.literal("§d[Takasha Maid] §fModel berhasil dimuat ulang!"));
            }
        }).bounds(left + 138, bottomY, 95, 18)
        .tooltip(Tooltip.create(Component.literal("Scan ulang folder maid_models tanpa perlu restart game")))
        .build();
        this.addRenderableWidget(this.reloadBtn);

        // Entity Task Selector Buttons
        if (this.entity != null) {
            int taskBtnW = 60;
            int taskX = left + 238;

            this.followBtn = Button.builder(Component.literal(entity.getMaidTask() == TakashaMaidEntity.TASK_FOLLOW ? "§a🚶 Ikuti" : "🚶 Ikuti"), b -> {
                entity.setMaidTask(TakashaMaidEntity.TASK_FOLLOW);
                entity.setOrderedToSit(false);
                entity.setInSittingPose(false);
                updateTaskButtons();
                playUiClick();
            }).bounds(taskX, bottomY, taskBtnW, 18).build();
            this.addRenderableWidget(this.followBtn);

            this.farmBtn = Button.builder(Component.literal(entity.getMaidTask() == TakashaMaidEntity.TASK_FARM ? "§a🌾 Farm" : "🌾 Farm"), b -> {
                entity.setMaidTask(TakashaMaidEntity.TASK_FARM);
                entity.setOrderedToSit(false);
                entity.setInSittingPose(false);
                updateTaskButtons();
                playUiClick();
            }).bounds(taskX + 64, bottomY, taskBtnW, 18).build();
            this.addRenderableWidget(this.farmBtn);

            this.fishBtn = Button.builder(Component.literal(entity.getMaidTask() == TakashaMaidEntity.TASK_FISH ? "§a🎣 Fish" : "🎣 Fish"), b -> {
                entity.setMaidTask(TakashaMaidEntity.TASK_FISH);
                entity.setOrderedToSit(false);
                entity.setInSittingPose(false);
                updateTaskButtons();
                playUiClick();
            }).bounds(taskX + 128, bottomY, taskBtnW, 18).build();
            this.addRenderableWidget(this.fishBtn);
        }

        // Close Button
        Button closeBtn = Button.builder(Component.literal("Tutup [ESC]"), b -> this.onClose())
                .bounds(left + panelW - 74, top + 6, 60, 16)
                .build();
        this.addRenderableWidget(closeBtn);

        // Initialize model list
        updateFilteredList("");
        rebuildModelButtons();
    }

    private void updateTaskButtons() {
        if (this.entity == null) return;
        int task = this.entity.getMaidTask();
        if (this.followBtn != null) {
            this.followBtn.setMessage(Component.literal(task == TakashaMaidEntity.TASK_FOLLOW ? "§a🚶 Ikuti" : "🚶 Ikuti"));
        }
        if (this.farmBtn != null) {
            this.farmBtn.setMessage(Component.literal(task == TakashaMaidEntity.TASK_FARM ? "§a🌾 Farm" : "🌾 Farm"));
        }
        if (this.fishBtn != null) {
            this.fishBtn.setMessage(Component.literal(task == TakashaMaidEntity.TASK_FISH ? "§a🎣 Fish" : "🎣 Fish"));
        }
    }

    private void updateFilteredList(String query) {
        this.filteredModels.clear();
        List<MaidModelInfo> all = MaidModelRegistry.getAllModels();
        if (query == null || query.isBlank()) {
            this.filteredModels.addAll(all);
            return;
        }

        String lower = query.toLowerCase(Locale.ROOT).trim();
        for (MaidModelInfo info : all) {
            String name = info.getName().toLowerCase(Locale.ROOT);
            String id = info.getModelId().toLowerCase(Locale.ROOT);
            String author = info.getAuthor().toLowerCase(Locale.ROOT);
            if (name.contains(lower) || id.contains(lower) || author.contains(lower)) {
                this.filteredModels.add(info);
            }
        }
    }

    private int getMaxPage() {
        if (this.filteredModels.isEmpty()) return 0;
        return (this.filteredModels.size() - 1) / ITEMS_PER_PAGE;
    }

    private void rebuildModelButtons() {
        int maxPage = getMaxPage();
        if (this.currentPage > maxPage) this.currentPage = maxPage;
        if (this.currentPage < 0) this.currentPage = 0;

        int startIndex = this.currentPage * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            Button btn = this.modelButtons.get(i);
            int dataIndex = startIndex + i;
            if (dataIndex < this.filteredModels.size()) {
                MaidModelInfo info = this.filteredModels.get(dataIndex);
                boolean isSelected = info.getModelId().equals(this.selectedModelId);
                String prefix = isSelected ? "§e▶ " : "  ";
                String name = info.getName();
                String author = info.getAuthor();
                String label = prefix + name + (author.isEmpty() ? "" : " §8(" + author + ")");
                btn.setMessage(Component.literal(label));
                btn.visible = true;
                btn.active = true;
            } else {
                btn.visible = false;
                btn.active = false;
            }
        }

        if (this.prevPageBtn != null) {
            this.prevPageBtn.active = this.currentPage > 0;
        }
        if (this.nextPageBtn != null) {
            this.nextPageBtn.active = this.currentPage < maxPage;
        }
    }

    private void onModelButtonClicked(int buttonIndex) {
        int dataIndex = (this.currentPage * ITEMS_PER_PAGE) + buttonIndex;
        if (dataIndex >= 0 && dataIndex < this.filteredModels.size()) {
            MaidModelInfo info = this.filteredModels.get(dataIndex);
            this.selectedModelId = info.getModelId();
            if (this.previewDummy != null) {
                this.previewDummy.setModelId(this.selectedModelId);
            }
            if (this.entity != null) {
                this.entity.setModelId(this.selectedModelId);
            }
            playUiClick();
            rebuildModelButtons();
        }
    }

    private void applySelectedModel() {
        if (this.entity != null && this.selectedModelId != null) {
            this.entity.setModelId(this.selectedModelId);
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendOverlayMessage(
                        Component.literal("§d[Takasha Maid] §fModel berhasil diubah menjadi: §e" + MaidModelRegistry.formatName(this.selectedModelId))
                );
            }
        }
    }

    private void openModelFolder() {
        if (this.minecraft == null) return;
        Path folder = Path.of(this.minecraft.gameDirectory.getAbsolutePath(), "maid_models");
        Util.getPlatform().openPath(folder);
    }

    private void playUiClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // Dark background overlay
        extractor.fill(0, 0, this.width, this.height, 0x88000000);

        int panelW = 440;
        int panelH = 264;
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        // Container Window (Glassmorphism dark purple + sakura pink border)
        extractor.fill(left, top, left + panelW, top + panelH, 0xEE161320);
        extractor.outline(left, top, panelW, panelH, 0xFFE082A8);

        // Header
        extractor.fill(left, top, left + panelW, top + 26, 0xFF241528);
        extractor.text(this.font, "🌸 Takasha Maid Companion (Touhou Little Maid Engine)", left + 12, top + 8, 0xFFFFB6C1, false);

        // Left: 3D Preview Frame
        int previewLeft = left + 14;
        int previewW = 146;
        int previewTop = top + 34;
        int previewH = 130;

        extractor.fill(previewLeft, previewTop, previewLeft + previewW, previewTop + previewH, 0xAA0E0C16);
        extractor.outline(previewLeft, previewTop, previewW, previewH, 0xFF4A3854);

        // Render 3D LivingEntity in Preview Window
        TakashaMaidEntity previewTarget = (this.entity != null) ? this.entity : this.previewDummy;
        if (previewTarget != null) {
            previewTarget.setModelId(this.selectedModelId);
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                    extractor,
                    previewLeft,
                    previewTop,
                    previewLeft + previewW,
                    previewTop + previewH,
                    48,
                    0.0625F,
                    mouseX,
                    mouseY,
                    previewTarget
            );
        }

        // Selected Model Info labels
        MaidModelInfo selectedInfo = MaidModelRegistry.getModelInfo(this.selectedModelId);
        String nameStr = selectedInfo != null ? selectedInfo.getName() : MaidModelRegistry.formatName(this.selectedModelId);
        String authorStr = selectedInfo != null && !selectedInfo.getAuthor().isEmpty() ? "Author: " + selectedInfo.getAuthor() : "Karya: Touhou Mod";
        String isCustom = selectedInfo != null && selectedInfo.isCustom() ? "§a[Kustom Folder]" : "§d[Internal Touhou]";

        int infoY = previewTop + previewH + 26;
        extractor.text(this.font, nameStr, previewLeft, infoY, 0xFFFFD54F, false);
        extractor.text(this.font, authorStr, previewLeft, infoY + 10, 0xFFAAAAAA, false);
        extractor.text(this.font, isCustom, previewLeft, infoY + 20, 0xFFCCCCCC, false);

        // Page info text on Right side
        int rightX = left + 172;
        int rightW = 254;
        int navY = top + 34 + 20 + (ITEMS_PER_PAGE * 22) + 6;
        int maxPage = getMaxPage();
        String pageStr = "Halaman " + (this.currentPage + 1) + " / " + Math.max(1, maxPage + 1) + " (" + this.filteredModels.size() + " model)";
        int textW = this.font.width(pageStr);
        int textX = rightX + (rightW - textW) / 2;
        extractor.text(this.font, pageStr, textX, navY, 0xFFDDDDDD, false);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (this.searchBox.keyPressed(event)) {
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
        if (this.parentScreen != null && this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parentScreen);
        } else {
            super.onClose();
        }
    }
}
