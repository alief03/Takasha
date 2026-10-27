package net.sakura.weapons;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.sakura.weapons.client.command.TakashaClientCommands;
import net.sakura.weapons.client.config.TakashaMasterConfig;
import net.sakura.weapons.client.config.TakashaNametagConfig;
import net.sakura.weapons.client.gui.AnvilSideListWidget;
import net.sakura.weapons.client.gui.TakashaDashboardScreen;
import net.sakura.weapons.client.gui.TakashaNpcScreen;
import net.sakura.weapons.compat.EmotecraftCompat;
import net.sakura.weapons.client.gui.TakashaReplayOverrideScreen;
import net.sakura.weapons.client.render.SakuraHatFeatureRenderer;
import net.sakura.weapons.client.render.SakuraWingsFeatureRenderer;
import net.sakura.weapons.client.render.entity.TakashaNpcRenderer;
import net.sakura.weapons.registry.ModEntities;
import net.sakura.weapons.registry.ModMenuTypes;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class SakuraWeaponsClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("sakura_weapons_client");

    public static final KeyMapping.Category TAKASHA_CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("sakura_weapons", "takasha"));

    public static KeyMapping DASHBOARD_KEY;
    public static KeyMapping AUTO_FARM_KEY;
    public static KeyMapping AUTO_FISH_KEY;
    public static KeyMapping MOB_KILL_KEY;
    public static KeyMapping AUTO_BOW_KEY;
    public static KeyMapping TOGGLE_NAMETAG_KEY;
    public static KeyMapping REPLAY_STUDIO_KEY;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Sakura Weapons client rendering for Minecraft 1.21.11...");

        // Asynchronously pre-warm configs and emote scanner at startup to eliminate delay on first keybind GUI open
        TakashaMasterConfig.getInstance();
        TakashaNametagConfig.ensureLoaded();
        EmotecraftCompat.prewarm();

        // Register custom decorative Sakura Wings and Sakura Hat feature renderers for players and armor stands
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityType == EntityType.PLAYER && entityRenderer instanceof AvatarRenderer avatarRenderer) {
                registrationHelper.register(new SakuraWingsFeatureRenderer<>(avatarRenderer, context.getItemModelResolver()));
                registrationHelper.register(new SakuraHatFeatureRenderer<>(avatarRenderer, context.getItemModelResolver()));
            } else if (entityType == EntityType.ARMOR_STAND && entityRenderer instanceof ArmorStandRenderer armorStandRenderer) {
                registrationHelper.register(new SakuraWingsFeatureRenderer<>(armorStandRenderer, context.getItemModelResolver()));
                registrationHelper.register(new SakuraHatFeatureRenderer<>(armorStandRenderer, context.getItemModelResolver()));
            }
        });

        // Register Interactive Scrollable Anvil GUI Side-List for Takasha sets
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AnvilScreen) {
                ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, hAmount, vAmount) -> {
                    for (var widget : Screens.getButtons(s)) {
                        if (widget instanceof AnvilSideListWidget sideList && sideList.isMouseOver(mouseX, mouseY)) {
                            sideList.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
                            return false; // consumed
                        }
                    }
                    return true;
                });
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AnvilScreen anvilScreen) {
                int panelWidth = 140;
                int panelHeight = 166;
                int panelX = (scaledWidth - 176) / 2 + 176 + 4;
                if (panelX + panelWidth > scaledWidth) {
                    panelX = Math.max(2, (scaledWidth - 176) / 2 - panelWidth - 4);
                }
                int panelY = (scaledHeight - 166) / 2;

                AnvilSideListWidget sideList =
                    new AnvilSideListWidget(anvilScreen, panelX, panelY, panelWidth, panelHeight);
                Screens.getButtons(screen).add(sideList);
            }
        });

        // Register Entity Renderer for Takasha NPC
        EntityRendererRegistry.register(
            ModEntities.TAKASHA_NPC,
            TakashaNpcRenderer::new
        );

        // Register Extended Container Menu Screen for Takasha NPC
        MenuScreens.register(ModMenuTypes.TAKASHA_NPC_MENU, TakashaNpcScreen::new);

        // Register Client Commands (/takasha nametag ...)
        TakashaClientCommands.initialize();

        // Register Keybindings with custom Takasha Category
        DASHBOARD_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.sakura_weapons.master_dashboard",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            TAKASHA_CATEGORY
        ));

        AUTO_FARM_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.sakura_weapons.auto_farm",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F6,
            TAKASHA_CATEGORY
        ));

        AUTO_FISH_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.sakura_weapons.auto_fish",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            TAKASHA_CATEGORY
        ));

        MOB_KILL_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.sakura_weapons.mob_kill",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F10,
            TAKASHA_CATEGORY
        ));

        AUTO_BOW_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.sakura_weapons.auto_bow",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            TAKASHA_CATEGORY
        ));

        TOGGLE_NAMETAG_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.sakura_weapons.toggle_nametag",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            TAKASHA_CATEGORY
        ));

        REPLAY_STUDIO_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.sakura_weapons.replay_studio",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F7,
            TAKASHA_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (DASHBOARD_KEY.consumeClick()) {
                if (client.player != null) {
                    client.setScreen(new TakashaDashboardScreen(null));
                }
            }
            while (AUTO_FARM_KEY.consumeClick()) {
                if (client.player != null) {
                    var cfg = net.sakura.weapons.client.config.TakashaMasterConfig.getInstance();
                    cfg.autoFarmEnabled = !cfg.autoFarmEnabled;
                    cfg.save();
                    client.player.displayClientMessage(Component.literal(
                        cfg.autoFarmEnabled ? "§a[Auto Farm] Aktif" : "§c[Auto Farm] Nonaktif"
                    ), true);
                }
            }
            while (AUTO_FISH_KEY.consumeClick()) {
                if (client.player != null) {
                    var cfg = net.sakura.weapons.client.config.TakashaMasterConfig.getInstance();
                    cfg.autoFishEnabled = !cfg.autoFishEnabled;
                    cfg.save();
                    client.player.displayClientMessage(Component.literal(
                        cfg.autoFishEnabled ? "§a[Auto Fishing] Aktif (" + cfg.fishingMode + ")" : "§c[Auto Fishing] Nonaktif"
                    ), true);
                }
            }
            while (MOB_KILL_KEY.consumeClick()) {
                if (client.player != null) {
                    var cfg = net.sakura.weapons.client.config.TakashaMasterConfig.getInstance();
                    cfg.autoKillEnabled = !cfg.autoKillEnabled;
                    cfg.save();
                    client.player.displayClientMessage(Component.literal(
                        cfg.autoKillEnabled ? "§a[Auto Kill Mob] Aktif (Anti-Aura 35°)" : "§c[Auto Kill Mob] Nonaktif"
                    ), true);
                }
            }
            while (AUTO_BOW_KEY.consumeClick()) {
                if (client.player != null) {
                    var cfg = net.sakura.weapons.client.config.TakashaMasterConfig.getInstance();
                    cfg.autoBowEnabled = !cfg.autoBowEnabled;
                    cfg.save();
                    client.player.displayClientMessage(Component.literal(
                        cfg.autoBowEnabled ? "§a[Auto Bow] Aktif (Ballistic Aim)" : "§c[Auto Bow] Nonaktif"
                    ), true);
                }
            }
            while (TOGGLE_NAMETAG_KEY.consumeClick()) {
                if (client.player != null) {
                    TakashaNametagConfig.toggle(client.player);
                }
            }
            while (REPLAY_STUDIO_KEY.consumeClick()) {
                if (client.player != null) {
                    var cfg = net.sakura.weapons.client.config.TakashaMasterConfig.getInstance();
                    boolean isMultiplayer = client.getCurrentServer() != null && !client.isLocalServer();
                    if (cfg.serverSafeMode && isMultiplayer) {
                        client.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "§c[Server Safe] Replay Studio dinonaktifkan di server multiplayer demi keamanan!"
                        ), true);
                    } else {
                        client.setScreen(new TakashaReplayOverrideScreen());
                    }
                }
            }

            // Run automation handlers
            net.sakura.weapons.client.farming.TakashaFarmHandler.getInstance().tick(client);
            net.sakura.weapons.client.fishing.TakashaFishingHandler.getInstance().tick(client);
            net.sakura.weapons.client.combat.TakashaMobKillHandler.getInstance().tick(client);
            net.sakura.weapons.client.combat.TakashaAutoBowHandler.getInstance().tick(client);
        });

        LOGGER.info("Takasha client successfully initialized with Anvil scrollable GUI sidebar, NPC rendering, menu screens, and studio shortcuts for Minecraft 1.21.11.");
    }
}
