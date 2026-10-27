package net.sakura.touhou.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.sakura.touhou.TouhouLittleMaidMod;

import java.util.function.Function;

public class ModItems {

    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(TouhouLittleMaidMod.MOD_ID, name));
        T item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static final Item MAID_SPAWN_EGG = register(
            "maid_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.MAID)),
            new Item.Properties()
    );

    public static final Item CHERRY_BLOSSOM_CAKE = register(
            "cherry_blossom_cake",
            Item::new,
            new Item.Properties().stacksTo(16)
    );

    public static final Item MOBILE_BEACON = register(
            "mobile_beacon",
            net.sakura.touhou.compat.tlmo.beacon.MobileBeaconItem::new,
            new Item.Properties()
    );

    public static final ResourceKey<CreativeModeTab> TOUHOU_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(TouhouLittleMaidMod.MOD_ID, "touhou_tab")
    );

    public static final CreativeModeTab TOUHOU_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            TOUHOU_TAB_KEY,
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.touhou_little_maid"))
                    .icon(() -> new ItemStack(MAID_SPAWN_EGG))
                    .displayItems((params, output) -> {
                        output.accept(MAID_SPAWN_EGG);
                        output.accept(CHERRY_BLOSSOM_CAKE);
                        output.accept(MOBILE_BEACON);
                    })
                    .build()
    );

    public static void initialize() {
        TouhouLittleMaidMod.LOGGER.info("Registered Touhou Little Maid Items & Creative Tab for Minecraft 26.2");
    }
}
