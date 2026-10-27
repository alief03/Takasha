package net.takasha.rpg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.takasha.rpg.TakashaRpgMod;
import net.takasha.rpg.client.gui.GrimoireScreen;
import net.takasha.rpg.client.hud.TakashaRpgScoreboard;
import net.takasha.rpg.network.RpgClientSyncPayload;
import org.lwjgl.glfw.GLFW;

public class TakashaRpgClient implements ClientModInitializer {

    public static KeyMapping KEY_TOGGLE_SCOREBOARD;

    @Override
    public void onInitializeClient() {
        TakashaRpgMod.LOGGER.info("Initializing Takasha-RPG Client...");

        // 1. Register Client Sync Packet Receiver (hanya terima jika di singleplayer)
        ClientPlayNetworking.registerGlobalReceiver(RpgClientSyncPayload.TYPE, (payload, context) -> {
            if (!context.client().hasSingleplayerServer()) return;
            context.client().execute(() -> TakashaRpgScoreboard.update(payload));
        });

        // 2. Register Toggle Keybind 'O'
        KEY_TOGGLE_SCOREBOARD = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.takasha_rpg.toggle_hud",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.hasSingleplayerServer()) {
                while (KEY_TOGGLE_SCOREBOARD.consumeClick()) {
                    TakashaRpgScoreboard.toggle();
                }
            } else {
                while (KEY_TOGGLE_SCOREBOARD.consumeClick()) {
                    // Abaikan di server multiplayer
                }
            }
        });

        // 3. Register Scoreboard & Status HUD
        TakashaRpgScoreboard.register();
        net.takasha.rpg.client.hud.TakashaActionbarHud.register();

        // 3b. Register Entity Renderers
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                net.takasha.rpg.registry.ModRpgEntities.DAMAGE_DUMMY,
                net.minecraft.client.renderer.entity.ArmorStandRenderer::new
        );

        // 4. Inject Grimoire Button directly into Player InventoryScreen (hanya jika di singleplayer)
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!client.hasSingleplayerServer()) return;
            if (screen instanceof InventoryScreen invScreen) {
                var accessor = (net.takasha.rpg.mixin.AbstractContainerScreenAccessor) invScreen;
                int initialX = accessor.getLeftPos() + 152;
                int initialY = accessor.getTopPos() + 5;

                Button grimoireBtn = Button.builder(Component.literal("📖"),
                        btn -> client.gui.setScreen(new GrimoireScreen()))
                        .bounds(initialX, initialY, 18, 16)
                        .tooltip(Tooltip.create(Component.literal("§6§lGrimoire of Realms§r\n§eBuka status AuraSkills, Quests, Titles & Homes")))
                        .build();

                Screens.getWidgets(screen).add(grimoireBtn);
            }
        });

        // 5. Reset client state on disconnect or joining multiplayer server
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            TakashaRpgScoreboard.reset();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!client.hasSingleplayerServer()) {
                TakashaRpgScoreboard.reset();
            }
        });

        TakashaRpgMod.LOGGER.info("Takasha-RPG Client successfully initialized!");
    }
}
