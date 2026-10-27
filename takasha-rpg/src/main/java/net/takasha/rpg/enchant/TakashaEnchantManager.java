package net.takasha.rpg.enchant;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.takasha.rpg.item.TakashaStatTrackerItem;

import java.util.*;

/**
 * TakashaEnchantManager
 * Enchantment Slot Rule Enforcement & Application Engine:
 * - Default Bawaan: 15 Slot Custom Enchantments aktif langsung pada item polos tanpa biaya awal.
 * - Slot Increaser Reguler (1): Menambah batas slot dari 15 hingga 20 slot (+1 per use).
 * - Slot Increaser Master (2): Menambah batas slot dari 20 hingga Grand Hard Cap 25 slot (+1 per use).
 * - Slot Increaser Soul (3): Menambah batas dedicated Soul slot dari 2 hingga 5 slot (+1 per use).
 * - White Scroll: Memberikan perlindungan dari kehancuran item saat aplikasi enchant gagal.
 * - Holy Scroll: Memberikan proteksi item agar tidak hilang saat mati (Keep on Death).
 * - Black Scroll: Mengekstrak salah satu enchant dari gear menjadi buku sihir 100% sukses.
 * - Randomization Scroll: Mengacak ulang persentase Success & Destroy rate pada buku.
 * - isForceAdmin: Mode bypass batas slot & roll untuk perintah /ae apply admin.
 */
public class TakashaEnchantManager {

    public static final int DEFAULT_SLOTS = 15;
    public static final int REGULAR_CAP_SLOTS = 20;
    public static final int GRAND_HARD_CAP_SLOTS = 25;

    public static final int DEFAULT_SOUL_SLOTS = 2;
    public static final int MAX_SOUL_SLOTS = 5;

    public static final String NBT_SLOT_CAP = "TakashaMaxSlots";
    public static final String NBT_SOUL_SLOT_CAP = "TakashaMaxSoulSlots";
    public static final String NBT_ENCHANTS = "TakashaCustomEnchants";
    public static final String NBT_WHITE_SCROLL = "WhiteScrollProtected";
    public static final String NBT_HOLY_SCROLL = "HolyScrollProtected";
    public static final String NBT_TRACKERS = "TakashaStatTrackers";

    private static final Random RANDOM = new Random();

    public static int getMaxSlots(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(NBT_SLOT_CAP)) {
                return Math.min(GRAND_HARD_CAP_SLOTS, Math.max(DEFAULT_SLOTS, tag.getIntOr(NBT_SLOT_CAP, DEFAULT_SLOTS)));
            }
        }
        return DEFAULT_SLOTS;
    }

    public static int getMaxSoulSlots(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(NBT_SOUL_SLOT_CAP)) {
                return Math.min(MAX_SOUL_SLOTS, Math.max(DEFAULT_SOUL_SLOTS, tag.getIntOr(NBT_SOUL_SLOT_CAP, DEFAULT_SOUL_SLOTS)));
            }
        }
        return DEFAULT_SOUL_SLOTS;
    }

    public static int getEnchantCount(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(NBT_ENCHANTS)) {
                return tag.getCompoundOrEmpty(NBT_ENCHANTS).size();
            }
        }
        return 0;
    }

    public static int getSoulEnchantCount(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0;
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(NBT_ENCHANTS)) return 0;

        CompoundTag enchTag = tag.getCompoundOrEmpty(NBT_ENCHANTS);
        int soulCount = 0;
        for (String key : enchTag.keySet()) {
            TakashaEnchantRegistry.CustomEnchantDef def = TakashaEnchantRegistry.get(key);
            if (def != null && def.tier == TakashaEnchantRegistry.EnchantTier.SOUL) {
                soulCount++;
            }
        }
        return soulCount;
    }

    public static Map<String, Integer> getCustomEnchants(ItemStack stack) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (stack.isEmpty()) return map;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return map;
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(NBT_ENCHANTS)) return map;

        CompoundTag enchTag = tag.getCompoundOrEmpty(NBT_ENCHANTS);
        for (String key : enchTag.keySet()) {
            map.put(key, enchTag.getIntOr(key, 1));
        }
        return map;
    }

    public static boolean hasCustomEnchant(ItemStack stack, String enchantId) {
        if (stack.isEmpty() || enchantId == null) return false;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;
        CompoundTag tag = customData.copyTag();
        return tag.contains(NBT_ENCHANTS) && tag.getCompoundOrEmpty(NBT_ENCHANTS).contains(enchantId.toLowerCase());
    }

    public static int getEnchantLevel(ItemStack stack, String enchantId) {
        if (stack.isEmpty() || enchantId == null) return 0;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0;
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(NBT_ENCHANTS)) return 0;
        return tag.getCompoundOrEmpty(NBT_ENCHANTS).getIntOr(enchantId.toLowerCase(), 0);
    }

    public static boolean canApplyEnchant(ItemStack stack, TakashaEnchantRegistry.CustomEnchantDef def, boolean isForceAdmin) {
        if (stack.isEmpty() || def == null) return false;
        if (isForceAdmin) return true;

        int currentSlots = getEnchantCount(stack);
        int maxSlots = getMaxSlots(stack);

        if (currentSlots >= maxSlots && !hasCustomEnchant(stack, def.id)) {
            return false;
        }

        if (def.tier == TakashaEnchantRegistry.EnchantTier.SOUL) {
            int currentSoul = getSoulEnchantCount(stack);
            int maxSoul = getMaxSoulSlots(stack);
            if (currentSoul >= maxSoul && !hasCustomEnchant(stack, def.id)) {
                return false;
            }
        }

        return true;
    }

    public static boolean applyCustomEnchant(ItemStack stack, TakashaEnchantRegistry.CustomEnchantDef def, int level, boolean isForceAdmin) {
        if (!canApplyEnchant(stack, def, isForceAdmin)) return false;

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag enchTag;
            if (tag.contains(NBT_ENCHANTS)) {
                enchTag = tag.getCompoundOrEmpty(NBT_ENCHANTS);
            } else {
                enchTag = new CompoundTag();
            }
            int cappedLvl = Math.max(1, Math.min(def.maxLevel, level));
            enchTag.putInt(def.id.toLowerCase(), cappedLvl);
            tag.put(NBT_ENCHANTS, enchTag);
        });
        return true;
    }

    public static boolean removeCustomEnchant(ItemStack stack, String enchantId) {
        if (!hasCustomEnchant(stack, enchantId)) return false;

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (tag.contains(NBT_ENCHANTS)) {
                CompoundTag enchTag = tag.getCompoundOrEmpty(NBT_ENCHANTS);
                enchTag.remove(enchantId.toLowerCase());
                if (enchTag.isEmpty()) {
                    tag.remove(NBT_ENCHANTS);
                } else {
                    tag.put(NBT_ENCHANTS, enchTag);
                }
            }
        });
        return true;
    }

    public static boolean applySlotIncreaser(ItemStack stack, int tier) {
        if (stack.isEmpty()) return false;
        if (tier == 1) {
            // Reguler: 15 s/d 20
            int currentMax = getMaxSlots(stack);
            if (currentMax >= REGULAR_CAP_SLOTS) return false;
            setMaxSlots(stack, currentMax + 1);
            return true;
        } else if (tier == 2) {
            // Master: 20 s/d 25
            int currentMax = getMaxSlots(stack);
            if (currentMax < REGULAR_CAP_SLOTS || currentMax >= GRAND_HARD_CAP_SLOTS) return false;
            setMaxSlots(stack, currentMax + 1);
            return true;
        } else if (tier == 3) {
            // Soul: 2 s/d 5
            int currentMax = getMaxSoulSlots(stack);
            if (currentMax >= MAX_SOUL_SLOTS) return false;
            setMaxSoulSlots(stack, currentMax + 1);
            return true;
        }
        return false;
    }

    public static boolean isWhiteScrollProtected(ItemStack stack) {
        if (stack.isEmpty()) return false;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return false;
        return cd.copyTag().getBooleanOr(NBT_WHITE_SCROLL, false);
    }

    public static void setWhiteScrollProtected(ItemStack stack, boolean protectedVal) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (protectedVal) {
                tag.putBoolean(NBT_WHITE_SCROLL, true);
            } else {
                tag.remove(NBT_WHITE_SCROLL);
            }
        });
    }

    public static boolean isHolyScrollProtected(ItemStack stack) {
        if (stack.isEmpty()) return false;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return false;
        return cd.copyTag().getBooleanOr(NBT_HOLY_SCROLL, false);
    }

    public static void setHolyScrollProtected(ItemStack stack, boolean protectedVal) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (protectedVal) {
                tag.putBoolean(NBT_HOLY_SCROLL, true);
            } else {
                tag.remove(NBT_HOLY_SCROLL);
            }
        });
    }

    private static void setMaxSlots(ItemStack stack, int newMax) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(NBT_SLOT_CAP, newMax);
        });
    }

    private static void setMaxSoulSlots(ItemStack stack, int newMax) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(NBT_SOUL_SLOT_CAP, newMax);
        });
    }

    public static boolean hasTracker(ItemStack stack, TakashaStatTrackerItem.TrackerType type) {
        if (stack.isEmpty() || type == null) return false;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return false;
        CompoundTag tag = cd.copyTag();
        return tag.contains(NBT_TRACKERS) && tag.getCompoundOrEmpty(NBT_TRACKERS).contains(type.name());
    }

    public static int getTrackerValue(ItemStack stack, TakashaStatTrackerItem.TrackerType type) {
        if (stack.isEmpty() || type == null) return 0;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return 0;
        CompoundTag tag = cd.copyTag();
        if (!tag.contains(NBT_TRACKERS)) return 0;
        return tag.getCompoundOrEmpty(NBT_TRACKERS).getIntOr(type.name(), 0);
    }

    public static boolean applyTracker(ItemStack stack, TakashaStatTrackerItem.TrackerType type) {
        if (stack.isEmpty() || type == null) return false;
        if (hasTracker(stack, type)) return false;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag trackerTag;
            if (tag.contains(NBT_TRACKERS)) {
                trackerTag = tag.getCompoundOrEmpty(NBT_TRACKERS);
            } else {
                trackerTag = new CompoundTag();
            }
            trackerTag.putInt(type.name(), 0);
            tag.put(NBT_TRACKERS, trackerTag);
        });
        return true;
    }

    public static void incrementTracker(ItemStack stack, TakashaStatTrackerItem.TrackerType type, int amount) {
        if (stack.isEmpty() || type == null || !hasTracker(stack, type)) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag trackerTag = tag.getCompoundOrEmpty(NBT_TRACKERS);
            int current = trackerTag.getIntOr(type.name(), 0);
            trackerTag.putInt(type.name(), current + amount);
            tag.put(NBT_TRACKERS, trackerTag);
        });
    }
}
