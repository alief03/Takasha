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
    private static boolean hideSelfOnly = true;
    private static boolean serverPassthrough = true;
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
            if (json.has("hideSelfOnly")) {
                hideSelfOnly = json.get("hideSelfOnly").getAsBoolean();
            }
            if (json.has("serverPassthrough")) {
                serverPassthrough = json.get("serverPassthrough").getAsBoolean();
            }
        } catch (Throwable e) {
            SakuraWeaponsMod.LOGGER.warn("Failed to load Takasha nametag config from {}", CONFIG_PATH, e);
        }
    }

    private static final java.util.concurrent.ExecutorService CONFIG_EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Takasha-NametagConfig-Writer");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    public static synchronized void save() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("playerNametagsDisabled", playerNametagsDisabled);
            json.addProperty("hideSelfOnly", hideSelfOnly);
            json.addProperty("serverPassthrough", serverPassthrough);
            String jsonStr = GSON.toJson(json);
            CONFIG_EXECUTOR.submit(() -> {
                try {
                    File file = CONFIG_PATH.toFile();
                    if (file.getParentFile() != null) {
                        file.getParentFile().mkdirs();
                    }
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.write(jsonStr);
                    }
                } catch (Throwable e) {
                    SakuraWeaponsMod.LOGGER.warn("Failed to write Takasha nametag config: {}", e.getMessage());
                }
            });
        } catch (Throwable e) {
            SakuraWeaponsMod.LOGGER.warn("Failed to serialize Takasha nametag config: {}", e.getMessage());
        }
    }

    public static boolean isPlayerNametagDisabled() {
        if (!loaded) {
            ensureLoaded();
        }
        return playerNametagsDisabled;
    }

    public static boolean isHideSelfOnly() {
        if (!loaded) {
            ensureLoaded();
        }
        return hideSelfOnly;
    }

    public static void setHideSelfOnly(boolean selfOnly) {
        ensureLoaded();
        hideSelfOnly = selfOnly;
        save();
    }

    public static boolean isServerPassthroughEnabled() {
        if (!loaded) {
            ensureLoaded();
        }
        return serverPassthrough;
    }

    public static void setServerPassthroughEnabled(boolean enabled) {
        ensureLoaded();
        serverPassthrough = enabled;
        save();
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
