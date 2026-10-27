package net.sakura.touhou.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.sakura.touhou.client.gui.model.MaidModelScreen;
import net.sakura.touhou.compat.tlmo.beacon.MobileBeaconItem;
import net.sakura.touhou.compat.ysm.YsmCompat;
import net.sakura.touhou.compat.ysm.event.OpenYsmMaidScreenEvent;
import net.sakura.touhou.entity.EntityMaid;
import net.sakura.touhou.entity.task.MaidTaskMode;
import net.sakura.touhou.inventory.MaidMainContainer;

/**
 * MaidMainScreen
 * Layar antarmuka utama Maid untuk Minecraft 26.2.
 * Menyediakan manajemen inventaris, slot bauble Mobile Beacon, tombol ganti model/skin
 * (bercabang ke YSM jika terpasang, atau Bedrock screen jika tidak), serta status RPG.
 */
@Environment(EnvType.CLIENT)
public class MaidMainScreen extends AbstractContainerScreen<MaidMainContainer> {

    private Button skinButton;
    private Button taskButton;

    public MaidMainScreen(MaidMainContainer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 256, 170);
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        EntityMaid maid = this.menu.getMaid();

        // 1. Tombol Ganti Model / Skin (Sesuai posisi dan event bytecode sumber TLM)
        this.skinButton = Button.builder(
                Component.literal("👗"),
                btn -> {
                    if (maid != null) {
                        if (YsmCompat.isInstalled()) {
                            OpenYsmMaidScreenEvent.CALLBACK.invoker().post(new OpenYsmMaidScreenEvent(maid));
                        } else {
                            if (this.minecraft != null) {
                                this.minecraft.setScreenAndShow(new MaidModelScreen(maid, this));
                            }
                        }
                    }
                }
        ).bounds(this.leftPos + 62, this.topPos + 14, 12, 12)
         .tooltip(Tooltip.create(Component.literal("§dGanti Model / Skin Maid\n§7(Terintegrasi dengan Yes Steve Model & Bedrock Model)")))
         .build();
        this.addRenderableWidget(this.skinButton);

        // 2. Tombol Pengalih Mode Tugas (Follow / Idle / Farm / Attack)
        String taskName = maid != null ? maid.getTaskMode().getDisplayName() : "Tugas";
        this.taskButton = Button.builder(
                Component.literal("Mode: " + taskName),
                btn -> {
                    if (maid != null) {
                        MaidTaskMode[] modes = MaidTaskMode.values();
                        int next = (maid.getTaskMode().ordinal() + 1) % modes.length;
                        maid.setTaskMode(modes[next]);
                        if (modes[next] == MaidTaskMode.IDLE) {
                            maid.setOrderedToSit(true);
                            maid.setMaidSitting(true);
                        } else {
                            maid.setOrderedToSit(false);
                            maid.setMaidSitting(false);
                        }
                        this.taskButton.setMessage(Component.literal("Mode: " + modes[next].getDisplayName()));
                    }
                }
        ).bounds(this.leftPos + 8, this.topPos + 138, 66, 16)
         .tooltip(Tooltip.create(Component.literal("§eUbah mode tindakan dan AI Goal maid.")))
         .build();
        this.addRenderableWidget(this.taskButton);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);

        int x = this.leftPos;
        int y = this.topPos;

        // Container Background Panel
        extractor.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xEE1A1824);
        extractor.outline(x, y, this.imageWidth, this.imageHeight, 0xFFFFB6C1);

        // Divider panel antara preview kiri dan slot inventaris kanan
        extractor.fill(x + 76, y + 4, x + 77, y + this.imageHeight - 4, 0x44FFB6C1);

        // 3D Maid Preview Viewport
        int previewX1 = x + 8;
        int previewY1 = y + 14;
        int previewX2 = x + 72;
        int previewY2 = y + 132;
        extractor.fill(previewX1, previewY1, previewX2, previewY2, 0xAA0E0D14);
        extractor.outline(previewX1, previewY1, previewX2 - previewX1, previewY2 - previewY1, 0xFF443D52);

        // Render Slots Frames
        for (Slot slot : this.menu.slots) {
            int sx = x + slot.x;
            int sy = y + slot.y;
            extractor.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF353040);
            extractor.fill(sx, sy, sx + 16, sy + 16, 0xFF14131A);
        }

        // Indicator Bauble / Mobile Beacon di Slot 0
        Slot baubleSlot = this.menu.slots.get(0);
        if (!baubleSlot.hasItem()) {
            extractor.centeredText(this.font, "✦", x + baubleSlot.x + 8, y + baubleSlot.y + 4, 0x66FFB6C1);
        }

        // Tampilkan 3D Live Entity Maid
        EntityMaid maid = this.menu.getMaid();
        if (maid != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                    extractor,
                    previewX1 + 1, previewY1 + 1,
                    previewX2 - 1, previewY2 - 1,
                    30,
                    0.0625f,
                    (float) mouseX, (float) mouseY,
                    maid
            );

            // Statistik Atribut RPG
            extractor.text(this.font, "§6§l" + maid.getName().getString(), x + 80, y + 6, 0xFFFFFF, false);
            extractor.text(this.font, "§c❤ " + (int) maid.getHealth() + "/" + (int) maid.getMaxHealth(), x + 165, y + 6, 0xFFFFFF, false);
            extractor.text(this.font, "§d❤ Favor: §f" + maid.getFavorability(), x + 205, y + 6, 0xFFFFFF, false);

            // Indikator Aura Beacon
            ItemStack baubleItem = baubleSlot.getItem();
            if (baubleItem.getItem() instanceof MobileBeaconItem) {
                extractor.text(this.font, "§b✦ Aura Beacon Aktif", x + 80, y + 30, 0x80D8FF, false);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
