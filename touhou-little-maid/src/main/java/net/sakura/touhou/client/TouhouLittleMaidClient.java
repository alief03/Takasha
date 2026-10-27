package net.sakura.touhou.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.sakura.touhou.TouhouLittleMaidMod;
import net.sakura.touhou.client.render.MaidRenderer;
import net.sakura.touhou.registry.ModEntities;

@Environment(EnvType.CLIENT)
public class TouhouLittleMaidClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        TouhouLittleMaidMod.LOGGER.info("Initializing Touhou Little Maid client rendering for Minecraft 26.2...");

        // Register Entity Renderer for Maid
        EntityRendererRegistry.register(
            ModEntities.MAID,
            MaidRenderer::new
        );

        // Register client network receiver for YSM sync
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
            net.sakura.touhou.network.message.SyncYsmMaidDataPackage.TYPE,
            net.sakura.touhou.network.message.SyncYsmMaidDataPackage::handle
        );

        // Register client screen for Maid GUI
        net.minecraft.client.gui.screens.MenuScreens.register(
            net.sakura.touhou.registry.ModMenuTypes.MAID_MAIN_CONTAINER,
            net.sakura.touhou.client.gui.MaidMainScreen::new
        );

        TouhouLittleMaidMod.LOGGER.info("Touhou Little Maid client successfully initialized with on-demand 4-slot LRU caching and distance LOD.");
    }
}
