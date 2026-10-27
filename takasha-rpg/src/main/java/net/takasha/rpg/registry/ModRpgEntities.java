package net.takasha.rpg.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.takasha.rpg.TakashaRpgMod;
import net.takasha.rpg.entity.DamageDummyEntity;

public class ModRpgEntities {

    public static final ResourceKey<EntityType<?>> DAMAGE_DUMMY_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "damage_dummy")
    );

    public static final EntityType<DamageDummyEntity> DAMAGE_DUMMY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DAMAGE_DUMMY_KEY,
            EntityType.Builder.<DamageDummyEntity>of(DamageDummyEntity::new, MobCategory.MISC)
                    .sized(0.5f, 1.975f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build(DAMAGE_DUMMY_KEY)
    );

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(DAMAGE_DUMMY, DamageDummyEntity.createAttributes());
        TakashaRpgMod.LOGGER.info("Registered Damage Dummy Entity for Takasha-RPG");
    }
}
