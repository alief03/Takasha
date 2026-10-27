package net.sakura.touhou.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.sakura.touhou.TouhouLittleMaidMod;
import net.sakura.touhou.compat.tlmo.affection.MaidsPrayerEffect;

public class ModEffects {

    public static final ResourceKey<MobEffect> MAIDS_PRAYER_KEY = ResourceKey.create(
            Registries.MOB_EFFECT,
            Identifier.fromNamespaceAndPath(TouhouLittleMaidMod.MOD_ID, "maids_prayer")
    );

    public static final Holder<MobEffect> MAIDS_PRAYER = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            MAIDS_PRAYER_KEY,
            new MaidsPrayerEffect()
    );

    public static void initialize() {
        TouhouLittleMaidMod.LOGGER.info("Registered Touhou Little Maid MobEffects (Maid's Prayer)");
    }
}
