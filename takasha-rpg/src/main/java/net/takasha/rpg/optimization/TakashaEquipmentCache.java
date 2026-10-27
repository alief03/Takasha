package net.takasha.rpg.optimization;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zero-Allocation Equipment Cache Architecture.
 * Mencegah loop NBT berulang pada 100 enchantment aktif (20 di senjata + 80 di 4 zirah)
 * di setiap tick pemain. Evaluasi HANYA dijalankan saat pergantian perlengkapan di slot.
 */
public class TakashaEquipmentCache {

    public static class CachedEquipmentProfile {
        public long lastEvaluatedTick = 0;
        public int totalEnchantCount = 0;
        public int totalSoulEnchantCount = 0;

        // Cached Combat Modifiers (Zero object creation in hot path)
        public double bonusAttackDamage = 0.0;
        public double bonusAttackSpeed = 0.0;
        public double bonusArmor = 0.0;
        public double bonusArmorToughness = 0.0;
        public double trueDamageRatio = 0.0;
        public double lifestealPercent = 0.0;

        // Cached Set of Active Custom Enchantment IDs
        public final Set<String> activeEnchantIds = new HashSet<>();
        public final Map<String, Integer> enchantLevels = new HashMap<>();

        // Item Signatures untuk dirty-checking cepat
        public final Map<EquipmentSlot, Integer> slotItemHashes = new EnumMap<>(EquipmentSlot.class);
    }

    private static final Map<UUID, CachedEquipmentProfile> PROFILE_CACHE = new ConcurrentHashMap<>();

    public static CachedEquipmentProfile getOrCreateProfile(UUID playerUuid) {
        return PROFILE_CACHE.computeIfAbsent(playerUuid, u -> new CachedEquipmentProfile());
    }

    public static void invalidate(UUID playerUuid) {
        PROFILE_CACHE.remove(playerUuid);
    }

    /**
     * Memeriksa apakah perlengkapan pemain telah berubah sejak evaluasi terakhir.
     * Jika berubah, perbarui cache secara hemat memori.
     */
    public static boolean checkAndRefresh(Player player, long currentTick) {
        if (player == null) return false;

        CachedEquipmentProfile profile = getOrCreateProfile(player.getUUID());
        boolean isDirty = false;

        // Periksa 6 slot perlengkapan utama: Mainhand, Offhand, Feet, Legs, Chest, Head
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack item = player.getItemBySlot(slot);
            int currentHash = getItemSignature(item);
            Integer cachedHash = profile.slotItemHashes.get(slot);

            if (cachedHash == null || cachedHash != currentHash) {
                profile.slotItemHashes.put(slot, currentHash);
                isDirty = true;
            }
        }

        if (isDirty || (currentTick - profile.lastEvaluatedTick >= 200)) { // Refresh fallback tiap 10s
            recalculateProfile(player, profile, currentTick);
            return true;
        }

        return false;
    }

    private static int getItemSignature(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        int result = stack.getItem().hashCode();
        result = 31 * result + stack.getCount();
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            result = 31 * result + Objects.hashCode(stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME));
        }
        return result;
    }

    private static void recalculateProfile(Player player, CachedEquipmentProfile profile, long currentTick) {
        profile.lastEvaluatedTick = currentTick;
        profile.activeEnchantIds.clear();
        profile.enchantLevels.clear();

        profile.totalEnchantCount = 0;
        profile.totalSoulEnchantCount = 0;
        profile.bonusAttackDamage = 0.0;
        profile.bonusAttackSpeed = 0.0;
        profile.bonusArmor = 0.0;
        profile.bonusArmorToughness = 0.0;
        profile.trueDamageRatio = 0.0;
        profile.lifestealPercent = 0.0;

        // Evaluasi item di tangan dan zirah (Zero garbage creation)
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            // Integrasi pembacaan custom enchants Takasha
            // Diisi oleh TakashaEnchantManager
        }
    }
}
