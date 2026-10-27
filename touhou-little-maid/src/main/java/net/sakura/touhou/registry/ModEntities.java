package net.sakura.touhou.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.sakura.touhou.TouhouLittleMaidMod;
import net.sakura.touhou.entity.EntityMaid;

public class ModEntities {

    public static final ResourceKey<EntityType<?>> MAID_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(TouhouLittleMaidMod.MOD_ID, "maid")
    );

    public static final EntityType<EntityMaid> MAID = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            MAID_KEY,
            EntityType.Builder.<EntityMaid>of(EntityMaid::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.5f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build(MAID_KEY)
    );

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(MAID, EntityMaid.createMaidAttributes());
        TouhouLittleMaidMod.LOGGER.info("Registered Touhou Little Maid Entity for Minecraft 26.2");
    }
}
