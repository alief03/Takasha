package net.sakura.weapons.registry;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;
import net.sakura.weapons.SakuraWeaponsMod;
import net.sakura.weapons.inventory.TakashaNpcMenu;

public class ModMenuTypes {

    public static final ResourceKey<MenuType<?>> TAKASHA_NPC_MENU_KEY = ResourceKey.create(
        Registries.MENU,
        Identifier.fromNamespaceAndPath(SakuraWeaponsMod.MOD_ID, "takasha_npc_menu")
    );

    public static final ExtendedMenuType<TakashaNpcMenu, Integer> TAKASHA_NPC_MENU = Registry.register(
        BuiltInRegistries.MENU,
        TAKASHA_NPC_MENU_KEY,
        new ExtendedMenuType<>(TakashaNpcMenu::new, ByteBufCodecs.VAR_INT)
    );

    public static void initialize() {
        SakuraWeaponsMod.LOGGER.info("Registered Takasha NPC Menu Type for Minecraft 26.2");
    }
}
