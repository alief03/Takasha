package net.sakura.touhou.client.gui.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.sakura.touhou.client.render.cache.BedrockModelCache;
import net.sakura.touhou.entity.EntityMaid;

import java.util.List;

/**
 * MaidModelScreen
 * Antarmuka pemilihan model Bedrock 3D bawaan Touhou Little Maid saat Yes Steve Model (YSM) tidak terpasang.
 * Terhubung dengan sistem 4-slot LRU Cache dan matrix pooling.
 */
@Environment(EnvType.CLIENT)
public class MaidModelScreen extends Screen {

    private final EntityMaid maid;
    private final Screen parentScreen;
    private static final List<String> BUILTIN_MODELS = List.of(
            "touhou_little_maid:reimu",
            "touhou_little_maid:marisa",
            "touhou_little_maid:sakuya",
            "touhou_little_maid:flandre",
            "touhou_little_maid:remilia",
            "touhou_little_maid:youmu",
            "touhou_little_maid:yuyuko",
            "touhou_little_maid:sanae"
    );

    private int selectedIndex = 0;

    public MaidModelScreen(EntityMaid maid, Screen parentScreen) {
        super(Component.literal("Pemilihan Model Bedrock Maid"));
        this.maid = maid;
        this.parentScreen = parentScreen;

        if (maid != null) {
            String current = maid.getModelId();
            for (int i = 0; i < BUILTIN_MODELS.size(); i++) {
                if (BUILTIN_MODELS.get(i).equalsIgnoreCase(current)) {
                    this.selectedIndex = i;
                    break;
                }
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Tombol Sebelumnya [<]
        this.addRenderableWidget(Button.builder(Component.literal("◀ Model Sebelumnya"), btn -> {
            if (selectedIndex > 0) {
                selectedIndex--;
                updateModelPreview();
            } else {
                selectedIndex = BUILTIN_MODELS.size() - 1;
                updateModelPreview();
            }
        }).bounds(centerX - 120, centerY + 65, 115, 20).build());

        // Tombol Selanjutnya [>]
        this.addRenderableWidget(Button.builder(Component.literal("Model Berikutnya ▶"), btn -> {
            if (selectedIndex < BUILTIN_MODELS.size() - 1) {
                selectedIndex++;
                updateModelPreview();
            } else {
                selectedIndex = 0;
                updateModelPreview();
            }
        }).bounds(centerX + 5, centerY + 65, 115, 20).build());

        // Tombol Terapkan Model
        this.addRenderableWidget(Button.builder(Component.literal("✔ Terapkan Model"), btn -> {
            if (maid != null) {
                maid.setModelId(BUILTIN_MODELS.get(selectedIndex));
                maid.setIsYsmModel(false);
            }
            this.onClose();
        }).bounds(centerX - 120, centerY + 90, 115, 20).build());

        // Tombol Batal / Kembali
        this.addRenderableWidget(Button.builder(Component.literal("Kembali"), btn -> this.onClose())
                .bounds(centerX + 5, centerY + 90, 115, 20).build());
    }

    private void updateModelPreview() {
        if (maid != null) {
            maid.setModelId(BUILTIN_MODELS.get(selectedIndex));
            maid.setIsYsmModel(false);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Panel Latar Belakang
        extractor.fill(centerX - 130, centerY - 110, centerX + 130, centerY + 120, 0xEE1A1A2A);
        extractor.outline(centerX - 130, centerY - 110, 260, 230, 0xFFFFB6C1);

        // Header Judul
        extractor.centeredText(this.font, "§d§lKatalog Model Touhou Little Maid", centerX, centerY - 100, 0xFFFFFF);
        String currentId = BUILTIN_MODELS.get(selectedIndex);
        extractor.centeredText(this.font, "§eModel Terpilih: §f" + currentId, centerX, centerY - 85, 0xFFFFFF);
        extractor.centeredText(this.font, "§7(Rendering Bedrock 3D dengan 4-Slot LRU Cache)", centerX, centerY - 72, 0xAAAAAA);

        // Preview Box Model 3D
        int boxX1 = centerX - 45;
        int boxY1 = centerY - 60;
        int boxX2 = centerX + 45;
        int boxY2 = centerY + 55;
        extractor.fill(boxX1, boxY1, boxX2, boxY2, 0xFF101018);
        extractor.outline(boxX1, boxY1, 90, 115, 0xFF555566);

        if (maid != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                    extractor,
                    boxX1 + 2, boxY1 + 2,
                    boxX2 - 2, boxY2 - 2,
                    36,
                    0.0625f,
                    (float) mouseX, (float) mouseY,
                    maid
            );
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null && this.parentScreen != null) {
            this.minecraft.setScreenAndShow(this.parentScreen);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
