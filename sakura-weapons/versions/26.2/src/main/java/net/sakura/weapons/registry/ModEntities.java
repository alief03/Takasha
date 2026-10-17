package net.sakura.weapons.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.sakura.weapons.SakuraWeaponsMod;
import net.sakura.weapons.entity.TakashaNpcEntity;

public class ModEntities {

    public static final ResourceKey<EntityType<?>> TAKASHA_NPC_KEY = ResourceKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath(SakuraWeaponsMod.MOD_ID, "takasha_npc")
    );

    public static final EntityType<TakashaNpcEntity> TAKASHA_NPC = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        TAKASHA_NPC_KEY,
        EntityType.Builder.<TakashaNpcEntity>of(TakashaNpcEntity::new, MobCategory.MISC)
            .sized(0.6f, 1.8f)
            .clientTrackingRange(10)
            .updateInterval(3)
            .build(TAKASHA_NPC_KEY)
    );

    public static final ResourceKey<EntityType<?>> TAKASHA_MAID_KEY = ResourceKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath(SakuraWeaponsMod.MOD_ID, "takasha_maid")
    );

    public static final EntityType<net.sakura.weapons.entity.TakashaMaidEntity> TAKASHA_MAID = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        TAKASHA_MAID_KEY,
        EntityType.Builder.<net.sakura.weapons.entity.TakashaMaidEntity>of(net.sakura.weapons.entity.TakashaMaidEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.5f)
            .clientTrackingRange(10)
            .updateInterval(3)
            .build(TAKASHA_MAID_KEY)
    );

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(TAKASHA_NPC, TakashaNpcEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(TAKASHA_MAID, net.sakura.weapons.entity.TakashaMaidEntity.createAttributes());
        SakuraWeaponsMod.LOGGER.info("Registered Takasha NPC and Maid Entities for Minecraft 26.2");
    }
}
