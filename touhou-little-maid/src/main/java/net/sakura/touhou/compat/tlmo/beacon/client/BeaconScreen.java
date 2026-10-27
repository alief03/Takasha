package net.sakura.touhou.compat.tlmo.beacon.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * BeaconScreen
 * Antarmuka GUI konfigurasi Mobile Beacon untuk pemain.
 * Menampilkan status aura beacon dan efek yang aktif.
 */
@Environment(EnvType.CLIENT)
public class BeaconScreen extends Screen {

    private final ItemStack beaconStack;

    public BeaconScreen(ItemStack beaconStack) {
        super(Component.literal("Konfigurasi Maid Mobile Beacon"));
        this.beaconStack = beaconStack;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(Button.builder(Component.literal("Tutup & Simpan"), btn -> this.onClose())
                .bounds(centerX - 60, centerY + 50, 120, 20)
                .build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        extractor.fill(centerX - 130, centerY - 80, centerX + 130, centerY + 80, 0xEE1A1A2A);
        extractor.outline(centerX - 130, centerY - 80, 260, 160, 0xFFFFB6C1);

        extractor.centeredText(this.font, "§6§lMaid Mobile Beacon", centerX, centerY - 60, 0xFFFFFF);
        extractor.centeredText(this.font, "§eStatus Aura: §aAktif", centerX, centerY - 40, 0xFFFFFF);
        extractor.centeredText(this.font, "§7Efek Radius: §bRegeneration I, Resistance I, Speed I", centerX, centerY - 25, 0xFFFFFF);
        extractor.centeredText(this.font, "§7Radius Cakupan: §f16 Blok di sekitar Maid", centerX, centerY - 10, 0xFFFFFF);
        extractor.centeredText(this.font, "§dCincin Halo bercahaya aktif di atas kepala maid.", centerX, centerY + 10, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
