package net.takasha.rpg.dungeon.reward;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.takasha.rpg.dungeon.DungeonTier;
import net.takasha.rpg.registry.ModRpgItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Akumulator Hadiah Dungeon per-Wave.
 * Menghitung dan menyimpan akumulasi koin fisik, dust, scroll, serta item RPG Takasha & AE
 * yang diundi secara dinamis dengan probabilitas persentase (chance) eksplisit.
 */
public class DungeonRewardAccumulator {

    private final List<ItemStack> accumulatedItems = new ArrayList<>();
    private final Random rng = new Random();

    public void clear() {
        accumulatedItems.clear();
    }

    public void accumulateWave(DungeonTier tier, int waveNumber) {
        int coinMultiplier = switch (tier) {
            case COMMON -> 1;
            case NIGHTMARE -> 2;
            case SAKURA_BOSS -> 4;
        };

        List<DungeonLootEntry> entries = DungeonRewardConfigManager.getInstance().getLootForWave(tier, waveNumber);
        for (DungeonLootEntry entry : entries) {
            if (rng.nextFloat() <= entry.getChance()) {
                ItemStack stack = entry.createItemStack(coinMultiplier, rng);
                if (!stack.isEmpty()) {
                    addOrStackItem(stack);
                }
            }
        }
    }

    private ItemStack getRandomMMOItemByTiers(net.takasha.rpg.mmo.MMOItemTier... tiers) {
        List<net.takasha.rpg.mmo.MMOItemTemplate> matches = new ArrayList<>();
        Set<net.takasha.rpg.mmo.MMOItemTier> targetTiers = Set.of(tiers);
        for (net.takasha.rpg.mmo.MMOItemTemplate t : net.takasha.rpg.mmo.MMOItemManager.getAllTemplates()) {
            if (targetTiers.contains(t.getTier())) {
                matches.add(t);
            }
        }
        if (matches.isEmpty()) {
            return ItemStack.EMPTY;
        }
        net.takasha.rpg.mmo.MMOItemTemplate picked = matches.get(rng.nextInt(matches.size()));
        return net.takasha.rpg.mmo.MMOItemManager.generateItemStack(picked.getItemType(), picked.getId(), 1);
    }

    private Item getRandomMidTierArmor() {
        Item[] midTier = {
                ModRpgItems.PHANTOM_HELMET, ModRpgItems.PHANTOM_CHESTPLATE, ModRpgItems.PHANTOM_LEGGINGS, ModRpgItems.PHANTOM_BOOTS,
                ModRpgItems.RANGER_HELMET, ModRpgItems.RANGER_CHESTPLATE, ModRpgItems.RANGER_LEGGINGS, ModRpgItems.RANGER_BOOTS
        };
        return midTier[rng.nextInt(midTier.length)];
    }

    private Item getRandomHighTierArmor() {
        Item[] highTier = {
                ModRpgItems.DIMENSIONAL_TRAVELLER_HELMET, ModRpgItems.DIMENSIONAL_TRAVELLER_CHESTPLATE, ModRpgItems.DIMENSIONAL_TRAVELLER_LEGGINGS, ModRpgItems.DIMENSIONAL_TRAVELLER_BOOTS,
                ModRpgItems.KOTH_HELMET, ModRpgItems.KOTH_CHESTPLATE, ModRpgItems.KOTH_LEGGINGS, ModRpgItems.KOTH_BOOTS,
                ModRpgItems.SUPREME_HELMET, ModRpgItems.SUPREME_CHESTPLATE, ModRpgItems.SUPREME_LEGGINGS, ModRpgItems.SUPREME_BOOTS,
                ModRpgItems.YETI_HELMET, ModRpgItems.YETI_CHESTPLATE, ModRpgItems.YETI_LEGGINGS, ModRpgItems.YETI_BOOTS,
                ModRpgItems.YIJKI_HELMET, ModRpgItems.YIJKI_CHESTPLATE, ModRpgItems.YIJKI_LEGGINGS, ModRpgItems.YIJKI_BOOTS
        };
        return highTier[rng.nextInt(highTier.length)];
    }

    private void addOrStackItem(ItemStack newStack) {
        if (newStack.isEmpty()) return;

        for (ItemStack existing : accumulatedItems) {
            if (ItemStack.isSameItemSameComponents(existing, newStack) && existing.getCount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toAdd = Math.min(space, newStack.getCount());
                existing.grow(toAdd);
                newStack.shrink(toAdd);
                if (newStack.isEmpty()) return;
            }
        }

        while (!newStack.isEmpty()) {
            int toTake = Math.min(newStack.getCount(), newStack.getMaxStackSize());
            ItemStack sub = newStack.split(toTake);
            accumulatedItems.add(sub);
        }
    }

    public List<ItemStack> getAccumulatedItems() {
        return new ArrayList<>(accumulatedItems);
    }

    public boolean isEmpty() {
        return accumulatedItems.isEmpty();
    }

    public com.google.gson.JsonArray toJsonArray() {
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        for (ItemStack stack : accumulatedItems) {
            if (stack.isEmpty()) continue;
            com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
            obj.addProperty("item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            obj.addProperty("count", stack.getCount());
            array.add(obj);
        }
        return array;
    }

    public static DungeonRewardAccumulator fromJsonArray(com.google.gson.JsonArray array) {
        DungeonRewardAccumulator acc = new DungeonRewardAccumulator();
        if (array == null) return acc;
        for (com.google.gson.JsonElement el : array) {
            if (el.isJsonObject()) {
                com.google.gson.JsonObject obj = el.getAsJsonObject();
                if (obj.has("item") && obj.has("count")) {
                    String itemId = obj.get("item").getAsString();
                    int count = obj.get("count").getAsInt();
                    try {
                        Item it = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(itemId));
                        if (it != null && it != net.minecraft.world.item.Items.AIR) {
                            acc.addOrStackItem(new ItemStack(it, count));
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return acc;
    }
}
