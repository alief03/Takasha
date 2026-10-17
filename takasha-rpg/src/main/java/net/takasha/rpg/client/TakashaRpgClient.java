package net.takasha.rpg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.takasha.rpg.TakashaRpgMod;
import net.takasha.rpg.client.hud.TakashaRpgScoreboard;
import net.takasha.rpg.network.RpgClientSyncPayload;
import org.lwjgl.glfw.GLFW;

public class TakashaRpgClient implements ClientModInitializer {

    public static KeyMapping KEY_TOGGLE_SCOREBOARD;

    @Override
    public void onInitializeClient() {
        TakashaRpgMod.LOGGER.info("Initializing Takasha-RPG Client...");

        // 1. Register Client Sync Packet Receiver
        ClientPlayNetworking.registerGlobalReceiver(RpgClientSyncPayload.TYPE, (payload, context) -> {
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
            while (KEY_TOGGLE_SCOREBOARD.consumeClick()) {
                TakashaRpgScoreboard.toggle();
            }
        });

        // 3. Register Scoreboard HUD
        TakashaRpgScoreboard.register();

        TakashaRpgMod.LOGGER.info("Takasha-RPG Client successfully initialized!");
    }
}
