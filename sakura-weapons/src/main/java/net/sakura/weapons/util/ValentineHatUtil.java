package net.sakura.weapons.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Utility for detecting whether an ItemStack represents a Valentine Hat (3D Headpiece / Glasses),
 * either natively or via Anvil rename, while safeguarding against conflicts with
 * server-side plugins like ItemsAdder or Oraxen.
 */
public final class ValentineHatUtil {
    private ValentineHatUtil() {}

    /**
     * Checks if the given ItemStack is a Valentine Hat.
     *
     * @param stack The ItemStack being worn on head or evaluated
     * @return true if the item represents Valentine Hat
     */
    public static boolean isValentineHat(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        // 1. Conflict Guard: Items with CustomModelData are managed by server plugins (ItemsAdder / Oraxen)
        if (stack.has(DataComponents.CUSTOM_MODEL_DATA)) {
            return false;
        }

        // 2. Conflict Guard: Custom NBT tags from Bukkit/Paper plugins
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            String tagString = customData.copyTag().toString().toLowerCase();
            if (tagString.contains("itemsadder") || tagString.contains("oraxen") || tagString.contains("publicbukkitvalues")) {
                return false;
            }
        }

        // 3. Check if native ModItem Valentine Hat
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if ("sakura_weapons".equals(itemId.getNamespace())) {
            String path = itemId.getPath();
            if ("valentine_hat".equals(path)) {
                return true;
            }
        }

        // 4. Check if renamed item via Anvil
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            String rawText = customName.getString().toLowerCase();
            // Match bilingual keywords (English & Indonesian)
            boolean hasValentine = rawText.contains("valentine");
            if (hasValentine) {
                // Must be Hat/Glasses, not Helmet
                if ((rawText.contains("hat") || rawText.contains("topi") || rawText.contains("glasses") || rawText.contains("kacamata"))
                        && !rawText.contains("helmet") && !rawText.contains("helm")) {
                    return true;
                }
            }
        }

        return false;
    }
}
