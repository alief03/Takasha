package net.sakura.weapons.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Utility for detecting whether an ItemStack represents a piece of Pink Legacy Armor,
 * either natively or via Anvil rename, while safeguarding against conflicts with
 * server-side plugins like ItemsAdder or Oraxen.
 */
public final class PinkLegacyArmorUtil {
    private PinkLegacyArmorUtil() {}

    /**
     * Checks if the given ItemStack is a Pink Legacy armor piece.
     *
     * @param stack The ItemStack being worn or evaluated
     * @return true if the item should render with the Pink Legacy equipment asset
     */
    public static boolean isPinkLegacyArmor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        // 1. Check if native ModItem Pink Legacy armor (Always takes top priority)
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if ("sakura_weapons".equals(itemId.getNamespace())) {
            String path = itemId.getPath();
            if (path.equals("pink_legacy_helmet") || path.equals("pink_legacy_chestplate")
                    || path.equals("pink_legacy_leggings") || path.equals("pink_legacy_boots")) {
                return true;
            }
        }

        // 2. Custom NBT tags (supports ItemsAdder / Oraxen set integration or guards against foreign items)
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            String tagString = customData.copyTag().toString().toLowerCase();
            if (tagString.contains("pink_legacy") || tagString.contains("pinklegacy")) {
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
            boolean hasPinkKeyword = rawText.contains("pink") || rawText.contains("legacy");
            if (hasPinkKeyword) {
                boolean hasArmorKeyword = rawText.contains("helmet") || rawText.contains("chestplate")
                        || rawText.contains("leggings") || rawText.contains("boots")
                        || rawText.contains("zirah") || rawText.contains("topi")
                        || rawText.contains("celana") || rawText.contains("sepatu")
                        || rawText.contains("kepala") || rawText.contains("dada")
                        || rawText.contains("kaki") || rawText.contains("armor");
                if (hasArmorKeyword) {
                    return true;
                }
            }
        }

        return false;
    }
}
