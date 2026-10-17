package net.sakura.weapons.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Utility for detecting whether an ItemStack represents a Dragon Mecha Overlord Hat (3D Headpiece / Hat),
 * either natively or via Anvil rename, while safeguarding against conflicts with
 * server-side plugins like ItemsAdder or Oraxen.
 */
public final class DragonMechaOverlordHatUtil {
    private DragonMechaOverlordHatUtil() {}

    /**
     * Checks if the given ItemStack is a Dragon Mecha Overlord Hat.
     *
     * @param stack The ItemStack being worn on head or evaluated
     * @return true if the item represents Dragon Mecha Overlord Hat
     */
    public static boolean isDragonMechaOverlordHat(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        // 1. Check if native ModItem Dragon Mecha Overlord Hat (Always takes top priority)
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if ("sakura_weapons".equals(itemId.getNamespace())) {
            String path = itemId.getPath();
            if ("dragon_mecha_overlord_hat".equals(path)) {
                return true;
            }
        }

        // 2. Custom NBT tags (supports ItemsAdder / Oraxen set integration or guards against foreign items)
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            String tagString = customData.copyTag().toString().toLowerCase();
            if (tagString.contains("dragon_mecha_overlord_hat") || tagString.contains("dragon_mecha_overlord")) {
                return true;
            }
            if (tagString.contains("itemsadder") || tagString.contains("oraxen") || tagString.contains("publicbukkitvalues")) {
                return false;
            }
        }

        // 4. Check if renamed item via Anvil
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            String rawText = customName.getString().toLowerCase();
            // Match bilingual keywords (English & Indonesian)
            boolean hasDragon = rawText.contains("dragon") || rawText.contains("mecha") || rawText.contains("overlord");
            if (hasDragon) {
                // Must be Hat/Headpiece, not Helmet
                if ((rawText.contains("hat") || rawText.contains("topi") || rawText.contains("headpiece"))
                        && !rawText.contains("helmet") && !rawText.contains("helm")) {
                    return true;
                }
            }
        }

        return false;
    }
}
