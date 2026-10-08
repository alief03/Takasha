package net.sakura.weapons.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.sakura.weapons.SakuraWeaponsMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

@Environment(EnvType.CLIENT)
public class TakashaNametagConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("takasha_nametag_client.json");

    private static boolean playerNametagsDisabled = false;
    private static boolean loaded = false;

    public static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        File file = CONFIG_PATH.toFile();
        if (!file.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("playerNametagsDisabled")) {
                playerNametagsDisabled = json.get("playerNametagsDisabled").getAsBoolean();
            }
        } catch (Throwable e) {
            SakuraWeaponsMod.LOGGER.warn("Failed to load Takasha nametag config from {}", CONFIG_PATH, e);
        }
    }

    public static synchronized void save() {
        try {
            File file = CONFIG_PATH.toFile();
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            JsonObject json = new JsonObject();
            json.addProperty("playerNametagsDisabled", playerNametagsDisabled);
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(json, writer);
            }
        } catch (Throwable e) {
            SakuraWeaponsMod.LOGGER.warn("Failed to save Takasha nametag config to {}", CONFIG_PATH, e);
        }
    }

    public static boolean isPlayerNametagDisabled() {
        if (!loaded) {
            ensureLoaded();
        }
        return playerNametagsDisabled;
    }

    public static void setPlayerNametagDisabled(boolean disabled, Player player) {
        ensureLoaded();
        playerNametagsDisabled = disabled;
        save();
        if (player != null) {
            Component message = disabled
                ? Component.translatable("message.sakura_weapons.nametag.disabled")
                : Component.translatable("message.sakura_weapons.nametag.enabled");
            player.sendOverlayMessage(message);
        }
    }

    public static boolean toggle(Player player) {
        boolean newState = !isPlayerNametagDisabled();
        setPlayerNametagDisabled(newState, player);
        return newState;
    }
}
