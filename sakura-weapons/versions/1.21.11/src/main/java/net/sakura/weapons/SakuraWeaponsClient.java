package net.sakura.weapons;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.EntityType;
import net.sakura.weapons.client.gui.AnvilSideListWidget;
import net.sakura.weapons.client.render.SakuraHatFeatureRenderer;
import net.sakura.weapons.client.render.SakuraWingsFeatureRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class SakuraWeaponsClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("sakura_weapons_client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Sakura Weapons client rendering for Minecraft 1.21.11...");

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

        LOGGER.info("Takasha client successfully initialized with Anvil scrollable GUI sidebar for Minecraft 1.21.11.");
    }
}
