package net.sakura.weapons.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import net.sakura.weapons.client.util.ColorGradientUtil;

import java.util.List;

public class AnvilSideListWidget extends AbstractWidget {

    private final Screen parentScreen;
    private int activeTab = 0; // 0 = ALL, 1 = SAKURA, 2 = PINK LEGACY, 3 = VALENTINE, 4 = DRAGON MECHA
    private double scrollOffset = 0.0;
    private boolean isDraggingScrollbar = false;
    private double dragStartY = 0.0;
    private double dragStartOffset = 0.0;
    private ItemStack lastInputStack = ItemStack.EMPTY;

    private static final int ROW_HEIGHT = 20;
    private static final int HEADER_HEIGHT = 38;

    public AnvilSideListWidget(Screen parentScreen, int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Takasha Anvil Guide"));
        this.parentScreen = parentScreen;
    }

    public ItemStack getInputSlotItem() {
        if (this.parentScreen instanceof AbstractContainerScreen<?> containerScreen) {
            try {
                return containerScreen.getMenu().getSlot(0).getItem();
            } catch (Throwable ignored) {}
        }
        return ItemStack.EMPTY;
    }

    private List<AnvilItemCatalog.Entry> getActiveEntries() {
        ItemStack inputStack = getInputSlotItem();
        String setId = switch (activeTab) {
            case 1 -> "sakura";
            case 2 -> "pink_legacy";
            case 3 -> "valentine";
            case 4 -> "dragon_mecha_overlord";
            default -> "all";
        };
        return AnvilItemCatalog.getEntries(setId, inputStack);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        boolean isId = client.getLanguageManager().getSelected().toLowerCase().startsWith("id");

        int px = getX();
        int py = getY();
        int pw = getWidth();
        int ph = getHeight();

        // 0. Detect slot 0 input changes and auto-reset scroll
        ItemStack inputStack = getInputSlotItem();
        boolean inputChanged = !ItemStack.matches(inputStack, this.lastInputStack);
        if (inputChanged) {
            this.lastInputStack = inputStack.copy();
            this.scrollOffset = 0.0;
        }

        // 1. Render Outer Frame & Glassmorphism Background
        guiGraphics.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFFE082A8); // Sakura pink outer border
        guiGraphics.fill(px, py, px + pw, py + ph, 0xEE121018); // Dark tinted container

        // 2. Render Header
        guiGraphics.fill(px, py, px + pw, py + HEADER_HEIGHT, 0xFF231828);
        guiGraphics.text(font, "Takasha Rename Guide", px + 6, py + 3, 0xFFFFB6C1, false);

        // 3. Render Set Filter Tabs (5 tabs: All, Sakura, Pink, Val, Dragon)
        int tabY = py + 14;
        int tabH = 10;
        drawTab(guiGraphics, font, "All", 0, px + 3, tabY, 18, tabH);
        drawTab(guiGraphics, font, "Sakura", 1, px + 23, tabY, 32, tabH);
        drawTab(guiGraphics, font, "Pink", 2, px + 57, tabY, 24, tabH);
        drawTab(guiGraphics, font, "Val", 3, px + 83, tabY, 22, tabH);
        drawTab(guiGraphics, font, "Dragon", 4, px + 107, tabY, 30, tabH);

        // 4. Calculate List Dimensions & Scrolling
        List<AnvilItemCatalog.Entry> entries = getActiveEntries();

        // 5. Render Dynamic Smart Filter Status Badge in Header
        int filterY = py + 26;
        if (!inputStack.isEmpty()) {
            String inputName = inputStack.getHoverName().getString();
            int maxLabelWidth = pw - 24;
            String badgePrefix = "⚡ ";
            String countSuffix = " (" + entries.size() + ")";
            int availableTextWidth = maxLabelWidth - font.width(badgePrefix) - font.width(countSuffix);
            if (font.width(inputName) > availableTextWidth) {
                inputName = font.plainSubstrByWidth(inputName, Math.max(10, availableTextWidth - font.width(".."))) + "..";
            }
            int badgeColor = entries.isEmpty() ? 0xFFFF7777 : 0xFF88FF88;
            guiGraphics.text(font, badgePrefix + inputName + countSuffix, px + 6, filterY, badgeColor, false);
        } else {
            String allText = isId ? "Semua Item (" + entries.size() + ")" : "All Items (" + entries.size() + ")";
            guiGraphics.text(font, allText, px + 6, filterY, 0xFFB8A6C0, false);
        }
        int listTop = py + HEADER_HEIGHT + 2;
        int listBottom = py + ph - 2;
        int listHeight = listBottom - listTop;
        int totalContentHeight = entries.size() * ROW_HEIGHT;
        int maxScroll = Math.max(0, totalContentHeight - listHeight);
        scrollOffset = Math.clamp(scrollOffset, 0.0, maxScroll);

        // 5. Render Scrollable Items with scissor clipping
        AnvilItemCatalog.Entry hoveredEntry = null;
        ItemStack hoveredStack = null;

        guiGraphics.enableScissor(px, listTop, px + pw, listBottom);

        for (int i = 0; i < entries.size(); i++) {
            int rowY = listTop + (i * ROW_HEIGHT) - (int) scrollOffset;

            // Viewport culling
            if (rowY + ROW_HEIGHT < listTop || rowY > listBottom) {
                continue;
            }

            AnvilItemCatalog.Entry entry = entries.get(i);
            int rowX = px + 3;
            int rowW = pw - (maxScroll > 0 ? 11 : 6);

            boolean isHovered = mouseX >= rowX && mouseX <= rowX + rowW &&
                                mouseY >= Math.max(listTop, rowY) && mouseY <= Math.min(listBottom, rowY + ROW_HEIGHT);

            if (isHovered) {
                guiGraphics.fill(rowX, rowY, rowX + rowW, rowY + ROW_HEIGHT, 0x40FFB6C1);
                hoveredEntry = entry;
                hoveredStack = entry.createStack();
            }

            // Draw small item icon (16x16)
            ItemStack stack = entry.createStack();
            guiGraphics.fakeItem(stack, rowX + 2, rowY + 2);

            // Draw item display name
            String rawName = isId ? entry.displayNameId() : entry.displayNameEn();
            int maxTextWidth = rowW - 24;
            String truncatedName = rawName;
            if (font.width(truncatedName) > maxTextWidth) {
                truncatedName = font.plainSubstrByWidth(truncatedName, maxTextWidth - font.width("..")) + "..";
            }

            int textColor = isHovered ? 0xFFFFFFFF : 0xFFE0D8E8;
            guiGraphics.text(font, truncatedName, rowX + 22, rowY + 6, textColor, false);
        }

        guiGraphics.disableScissor();

        // 6. Render Scrollbar (if content overflows)
        if (maxScroll > 0) {
            int trackX = px + pw - 6;
            guiGraphics.fill(trackX, listTop, trackX + 3, listBottom, 0x55000000);

            int thumbHeight = Math.max(14, (int) ((float) listHeight * listHeight / totalContentHeight));
            int thumbY = listTop + (int) ((listHeight - thumbHeight) * (scrollOffset / maxScroll));
            int thumbColor = isDraggingScrollbar ? 0xFFFFB6C1 : 0xFFCC7090;
            guiGraphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, thumbColor);
        }

        // 7. Render Hovered Item Tooltip
        if (hoveredEntry != null) {
            java.util.List<Component> tooltipLines = new java.util.ArrayList<>();
            tooltipLines.add(Component.literal(hoveredEntry.displayNameEn()).withColor(0xFFFFB6C1));
            tooltipLines.add(Component.literal(hoveredEntry.displayNameId()).withColor(0xFFE0D8E8));
            tooltipLines.add(Component.literal("Item Dasar: " + hoveredEntry.baseItemHint()).withColor(0xFFAAAAAA));
            tooltipLines.add(Component.literal("Klik: Terapkan nama CIT (Normal)").withColor(0xFF88FF88));
            String colorHint;
            int colorHintHex;
            if ("dragon_mecha_overlord".equals(hoveredEntry.setId())) {
                colorHint = " (Vanilla §6 🐲)";
                colorHintHex = 0xFFFFAA00;
            } else if ("valentine".equals(hoveredEntry.setId())) {
                colorHint = " (Vanilla §c ❤)";
                colorHintHex = 0xFFFF5555;
            } else {
                colorHint = " (Vanilla §d)";
                colorHintHex = 0xFFFF77BB;
            }
            tooltipLines.add(Component.literal("Shift + Klik: Terapkan nama Berwarna" + colorHint).withColor(colorHintHex));
            guiGraphics.setComponentTooltipForNextFrame(font, tooltipLines, mouseX, mouseY);
        }
    }

    private void drawTab(GuiGraphicsExtractor guiGraphics, Font font, String label, int tabIndex, int tx, int ty, int tw, int th) {
        boolean active = this.activeTab == tabIndex;
        int bgColor = active ? 0xFFE082A8 : 0xFF35243C;
        int txtColor = active ? 0xFFFFFFFF : 0xFFB8A6C0;

        guiGraphics.fill(tx, ty, tx + tw, ty + th, bgColor);
        int textX = tx + (tw - font.width(label)) / 2;
        int textY = ty + (th - 8) / 2;
        guiGraphics.text(font, label, textX, textY, txtColor, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isFocused) {
        if (!visible || !active) return false;
        double mx = event.x();
        double my = event.y();
        if (!isMouseOver(mx, my)) return false;

        int px = getX();
        int py = getY();
        int pw = getWidth();
        int ph = getHeight();

        // 1. Check Tabs Click (All, Sakura, Pink, Val, Dragon)
        int tabY = py + 14;
        int tabH = 10;
        if (my >= tabY && my <= tabY + tabH) {
            if (mx >= px + 3 && mx < px + 22) {
                switchTab(0);
                return true;
            } else if (mx >= px + 23 && mx < px + 56) {
                switchTab(1);
                return true;
            } else if (mx >= px + 57 && mx < px + 82) {
                switchTab(2);
                return true;
            } else if (mx >= px + 83 && mx < px + 106) {
                switchTab(3);
                return true;
            } else if (mx >= px + 107 && mx <= px + 138) {
                switchTab(4);
                return true;
            }
        }

        int listTop = py + HEADER_HEIGHT + 2;
        int listBottom = py + ph - 2;
        int listHeight = listBottom - listTop;
        List<AnvilItemCatalog.Entry> entries = getActiveEntries();
        int totalContentHeight = entries.size() * ROW_HEIGHT;
        int maxScroll = Math.max(0, totalContentHeight - listHeight);

        // 2. Check Scrollbar Click / Drag start
        if (maxScroll > 0 && mx >= px + pw - 8 && mx <= px + pw && my >= listTop && my <= listBottom) {
            isDraggingScrollbar = true;
            dragStartY = my;
            dragStartOffset = scrollOffset;
            return true;
        }

        // 3. Check Item Click (Auto-fill Anvil rename box & clipboard)
        int rowW = pw - (maxScroll > 0 ? 11 : 6);
        if (mx >= px + 3 && mx <= px + 3 + rowW && my >= listTop && my <= listBottom) {
            int clickedIndex = (int) ((my - listTop + scrollOffset) / ROW_HEIGHT);
            if (clickedIndex >= 0 && clickedIndex < entries.size()) {
                AnvilItemCatalog.Entry entry = entries.get(clickedIndex);
                boolean isShift = event.hasShiftDown();
                applyItemNameToAnvil(entry, isShift);
                playButtonClickSound(Minecraft.getInstance().getSoundManager());
                return true;
            }
        }

        return true;
    }

    private void switchTab(int tabIndex) {
        this.activeTab = tabIndex;
        this.scrollOffset = 0.0;
        playButtonClickSound(Minecraft.getInstance().getSoundManager());
    }

    private void applyItemNameToAnvil(AnvilItemCatalog.Entry entry, boolean useVanillaColor) {
        Minecraft client = Minecraft.getInstance();
        String nameToApply;

        if (useVanillaColor) {
            if ("dragon_mecha_overlord".equals(entry.setId())) {
                nameToApply = net.sakura.weapons.util.MinecraftColorUtil.formatDragonMechaName(entry.displayNameEn(), true);
            } else if ("valentine".equals(entry.setId())) {
                nameToApply = net.sakura.weapons.util.MinecraftColorUtil.formatValentineName(entry.displayNameEn(), true);
            } else if ("pink_legacy".equals(entry.setId())) {
                nameToApply = net.sakura.weapons.util.MinecraftColorUtil.formatPinkLegacyName(entry.displayNameEn(), true);
            } else {
                nameToApply = net.sakura.weapons.util.MinecraftColorUtil.formatSakuraName(entry.displayNameEn(), true, true);
            }
        } else {
            nameToApply = entry.displayNameEn();
        }

        // Populate EditBox in AnvilScreen safely
        for (var child : parentScreen.children()) {
            if (child instanceof EditBox editBox) {
                editBox.setMaxLength(Math.max(256, nameToApply.length() + 10));
                editBox.setValue(nameToApply);
                break;
            }
        }

        // Copy to system clipboard for convenience
        client.keyboardHandler.setClipboard(nameToApply);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isMouseOver(mouseX, mouseY)) {
            List<AnvilItemCatalog.Entry> entries = getActiveEntries();
            int listHeight = getHeight() - HEADER_HEIGHT - 4;
            int maxScroll = Math.max(0, entries.size() * ROW_HEIGHT - listHeight);
            scrollOffset = Math.clamp(scrollOffset - verticalAmount * 16.0, 0.0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (isDraggingScrollbar) {
            int listHeight = getHeight() - HEADER_HEIGHT - 4;
            List<AnvilItemCatalog.Entry> entries = getActiveEntries();
            int totalContentHeight = entries.size() * ROW_HEIGHT;
            int maxScroll = Math.max(0, totalContentHeight - listHeight);
            if (maxScroll > 0) {
                double delta = event.y() - dragStartY;
                double factor = (double) totalContentHeight / listHeight;
                scrollOffset = Math.clamp(dragStartOffset + delta * factor, 0.0, maxScroll);
                return true;
            }
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        isDraggingScrollbar = false;
        return super.mouseReleased(event);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
