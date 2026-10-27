package net.sakura.touhou.registry;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;
import net.sakura.touhou.TouhouLittleMaidMod;
import net.sakura.touhou.inventory.MaidMainContainer;

public class ModMenuTypes {

    public static final ResourceKey<MenuType<?>> MAID_MAIN_CONTAINER_KEY = ResourceKey.create(
            Registries.MENU,
            Identifier.fromNamespaceAndPath(TouhouLittleMaidMod.MOD_ID, "maid_main_container")
    );

    public static final ExtendedMenuType<MaidMainContainer, Integer> MAID_MAIN_CONTAINER = Registry.register(
            BuiltInRegistries.MENU,
            MAID_MAIN_CONTAINER_KEY,
            new ExtendedMenuType<>(MaidMainContainer::new, ByteBufCodecs.VAR_INT)
    );

    public static void initialize() {
        TouhouLittleMaidMod.LOGGER.info("Registered Touhou Little Maid Menu Types for Minecraft 26.2");
    }
}
