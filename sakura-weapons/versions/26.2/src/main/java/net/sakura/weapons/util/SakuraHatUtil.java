package net.sakura.weapons.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Utility for detecting whether an ItemStack represents a Sakura Hat,
 * either natively or via Anvil rename, while safeguarding against conflicts with
 * server-side plugins like ItemsAdder or Oraxen.
 */
public final class SakuraHatUtil {
    private SakuraHatUtil() {}

    /**
     * Checks if the given ItemStack is a Sakura Hat.
     *
     * @param stack The ItemStack being worn on head or evaluated
     * @return true if the item represents Sakura Hat
     */
    public static boolean isSakuraHat(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        // 1. Check if native ModItem Sakura Hat (Always takes top priority)
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if ("sakura_weapons".equals(itemId.getNamespace())) {
            String path = itemId.getPath();
            if ("sakura_hat".equals(path)) {
                return true;
            }
        }

        // 2. Custom NBT tags (supports ItemsAdder / Oraxen set integration or guards against foreign items)
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            String tagString = customData.copyTag().toString().toLowerCase();
            if (tagString.contains("sakura_hat") || tagString.contains("sakura")) {
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
            boolean hasSakura = rawText.contains("sakura");
            if (hasSakura) {
                if (rawText.contains("hat") || rawText.contains("topi") || rawText.contains("caping") || rawText.contains("straw")) {
                    return true;
                }
            }
        }

        return false;
    }
}
