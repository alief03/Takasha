package net.sakura.weapons.client.fishing;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.sakura.weapons.client.fishing.capcay.ItemCategory;
import net.sakura.weapons.client.fishing.capcay.ItemClassifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Solves the Kaizen CAPCAY challenge: "Klik items yang berbeda jenis".
 * Analyzes chest slots, clusters items by semantic category, and picks the minority category slot.
 */
public final class CapcaySolver {

    private CapcaySolver() {}

    public record SolveResult(
            boolean success,
            int targetSlotIndex,
            List<Integer> candidateSlots,
            ItemCategory minorityCategory,
            String itemSummary,
            String explanation
    ) {
        public static SolveResult failed(String reason) {
            return new SolveResult(false, -1, Collections.emptyList(), ItemCategory.OTHER, "", reason);
        }
    }

    /**
     * Analyzes chest container slots and finds the outlier / minority item slot.
     */
    public static SolveResult solve(AbstractContainerMenu menu, Object playerInventory) {
        if (menu == null || menu.slots == null || menu.slots.isEmpty()) {
            return SolveResult.failed("Menu container kosong atau tidak valid.");
        }

        Map<Integer, ItemStack> chestSlots = new HashMap<>();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot.container != playerInventory) {
                ItemStack stack = slot.getItem();
                if (!ItemClassifier.isFiller(stack)) {
                    chestSlots.put(i, stack);
                }
            }
        }

        if (chestSlots.isEmpty()) {
            return SolveResult.failed("Tidak ditemukan item valid di container (hanya filler/kosong).");
        }

        // Semantic classification
        Map<ItemCategory, List<Integer>> categoryGroups = new EnumMap<>(ItemCategory.class);
        for (Map.Entry<Integer, ItemStack> entry : chestSlots.entrySet()) {
            int slotIdx = entry.getKey();
            ItemStack stack = entry.getValue();
            ItemCategory cat = ItemClassifier.classify(stack);
            categoryGroups.computeIfAbsent(cat, k -> new ArrayList<>()).add(slotIdx);
        }

        if (categoryGroups.size() >= 2) {
            ItemCategory minorityCat = null;
            int minCount = Integer.MAX_VALUE;
            int maxCount = Integer.MIN_VALUE;

            for (Map.Entry<ItemCategory, List<Integer>> entry : categoryGroups.entrySet()) {
                int count = entry.getValue().size();
                if (count < minCount) {
                    minCount = count;
                    minorityCat = entry.getKey();
                }
                if (count > maxCount) {
                    maxCount = count;
                }
            }

            if (minorityCat != null && minCount < maxCount) {
                List<Integer> candidateSlots = categoryGroups.get(minorityCat);
                int targetSlot = candidateSlots.get(0);
                ItemStack targetStack = chestSlots.get(targetSlot);
                String itemName = targetStack != null ? targetStack.getHoverName().getString() : minorityCat.getDisplayName();

                return new SolveResult(
                        true,
                        targetSlot,
                        candidateSlots,
                        minorityCat,
                        itemName,
                        String.format("Ditemukan minoritas kategori %s (%d item vs mayoritas %d). Slot: %d (%s)",
                                minorityCat.getDisplayName(), minCount, maxCount, targetSlot, itemName)
                );
            }
        }

        // Fallback: group by exact item
        Map<Item, List<Integer>> itemGroups = new HashMap<>();
        for (Map.Entry<Integer, ItemStack> entry : chestSlots.entrySet()) {
            int slotIdx = entry.getKey();
            ItemStack stack = entry.getValue();
            itemGroups.computeIfAbsent(stack.getItem(), k -> new ArrayList<>()).add(slotIdx);
        }

        if (itemGroups.size() >= 2) {
            Item minorityItem = null;
            int minCount = Integer.MAX_VALUE;
            int maxCount = Integer.MIN_VALUE;

            for (Map.Entry<Item, List<Integer>> entry : itemGroups.entrySet()) {
                int count = entry.getValue().size();
                if (count < minCount) {
                    minCount = count;
                    minorityItem = entry.getKey();
                }
                if (count > maxCount) {
                    maxCount = count;
                }
            }

            if (minorityItem != null && minCount < maxCount) {
                List<Integer> candidateSlots = itemGroups.get(minorityItem);
                int targetSlot = candidateSlots.get(0);
                ItemStack targetStack = chestSlots.get(targetSlot);
                Identifier id = BuiltInRegistries.ITEM.getKey(minorityItem);
                String itemName = targetStack != null ? targetStack.getHoverName().getString() : id.getPath();

                return new SolveResult(
                        true,
                        targetSlot,
                        candidateSlots,
                        ItemCategory.OTHER,
                        itemName,
                        String.format("Ditemukan minoritas item ID %s (%d vs %d). Slot target: %d",
                                itemName, minCount, maxCount, targetSlot)
                );
            }
        }

        return SolveResult.failed("Gagal membedakan minoritas: item tampak seragam.");
    }
}
