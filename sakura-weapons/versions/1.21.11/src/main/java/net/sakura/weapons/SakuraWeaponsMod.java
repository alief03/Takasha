package net.sakura.weapons;

import net.fabricmc.api.ModInitializer;
import net.sakura.weapons.registry.DragonMechaOverlordItems;
import net.sakura.weapons.registry.ModItemGroups;
import net.sakura.weapons.registry.ModItems;
import net.sakura.weapons.registry.ValentineItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SakuraWeaponsMod implements ModInitializer {
    public static final String MOD_ID = "sakura_weapons";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Sakura Weapons Mod for Minecraft 1.21.11...");
        ModItems.initialize();
        ValentineItems.initialize();
        DragonMechaOverlordItems.initialize();
        ModItemGroups.initialize();

        LOGGER.info("Takasha multi-set mod successfully initialized (Sakura, Pink Legacy, Valentine, Dragon Mecha Overlord) for Minecraft 1.21.11!");
    }
}
