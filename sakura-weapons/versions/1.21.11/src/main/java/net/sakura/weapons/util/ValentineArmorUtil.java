package net.sakura.weapons.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Utility for detecting whether an ItemStack represents a piece of Valentine Armor,
 * either natively or via Anvil rename, while safeguarding against conflicts with
 * server-side plugins like ItemsAdder or Oraxen.
 */
public final class ValentineArmorUtil {
    private ValentineArmorUtil() {}

    /**
     * Checks if the given ItemStack is a Valentine armor piece.
     *
     * @param stack The ItemStack being worn or evaluated
     * @return true if the item should render with the Valentine equipment asset
     */
    public static boolean isValentineArmor(ItemStack stack) {
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

        // 3. Check if native ModItem Valentine armor
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if ("sakura_weapons".equals(itemId.getNamespace())) {
            String path = itemId.getPath();
            if (path.equals("valentine_helmet") || path.equals("valentine_chestplate")
                    || path.equals("valentine_leggings") || path.equals("valentine_boots")) {
                return true;
            }
        }

        // 4. Check if renamed item via Anvil
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            String rawText = customName.getString().toLowerCase();
            // Match bilingual keywords (English & Indonesian)
            boolean hasValentineKeyword = rawText.contains("valentine");
            if (hasValentineKeyword) {
                boolean hasArmorKeyword = rawText.contains("helmet") || rawText.contains("chestplate")
                        || rawText.contains("leggings") || rawText.contains("boots")
                        || rawText.contains("zirah") || rawText.contains("helm")
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
