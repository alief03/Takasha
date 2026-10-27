package net.takasha.rpg.economy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.takasha.rpg.api.RarityTier;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.economy.catalog.ShopCatalog;
import net.takasha.rpg.economy.catalog.ShopCategory;
import net.takasha.rpg.economy.catalog.ShopItemEntry;
import net.takasha.rpg.registry.ModRpgItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * TakashaDailyShopManager
 * Pengelola toko berotasi terpadu (RotatingShop & DailyShop).
 * Berotasi secara periodik (setiap 1 jam / fajar in-game) dengan memilih 6 penawaran eksklusif
 * dari gabungan 24 item otentik RotatingShop.yml dan 15 item kustom mod takasha-rpg.
 */
public class TakashaDailyShopManager {

    public record DailyDeal(
        ItemStack stack,
        RarityTier tier,
        double originalPrice,
        double discountPercent,
        double finalPrice,
        boolean useGems,
        int stock
    ) {}

    private static final List<DailyDeal> CURRENT_DEALS = new ArrayList<>();
    private static long lastRotationTime = 0;
    private static long lastRotationDay = -1;
    private static final long ROTATION_INTERVAL_MS = 3600_000L; // 1 Jam

    public static List<DailyDeal> getCurrentDeals() {
        checkRotation(System.currentTimeMillis());
        if (CURRENT_DEALS.isEmpty()) {
            rotateDailyDeals(System.currentTimeMillis());
        }
        return CURRENT_DEALS;
    }

    public static long getRemainingSeconds() {
        if (lastRotationTime <= 0) {
            rotateDailyDeals(System.currentTimeMillis());
            lastRotationTime = System.currentTimeMillis();
        }
        long elapsed = System.currentTimeMillis() - lastRotationTime;
        long remaining = (ROTATION_INTERVAL_MS - elapsed) / 1000L;
        return Math.max(0, remaining);
    }

    public static String getRemainingTimeString() {
        long secs = getRemainingSeconds();
        long hours = secs / 3600;
        long mins = (secs % 3600) / 60;
        long remSecs = secs % 60;
        if (hours > 0) {
            return String.format("%02dh %02dm %02ds", hours, mins, remSecs);
        } else {
            return String.format("%02dm %02ds", mins, remSecs);
        }
    }

    public static void checkRotation(long currentTimeMs) {
        if (currentTimeMs - lastRotationTime >= ROTATION_INTERVAL_MS) {
            rotateDailyDeals(currentTimeMs);
            DynamicPricingEngine.onNewDay();
            lastRotationTime = currentTimeMs;
        }
    }

    public static void checkGameDayRotation(long dayTime) {
        long currentDay = dayTime / 24000L;
        if (currentDay != lastRotationDay) {
            rotateDailyDeals(System.currentTimeMillis());
            DynamicPricingEngine.onNewDay();
            lastRotationDay = currentDay;
            lastRotationTime = System.currentTimeMillis();
        }
    }

    public static void rotateDailyDeals(long seed) {
        CURRENT_DEALS.clear();
        Random rng = new Random(seed);

        List<DealCandidate> pool = new ArrayList<>();

        // 1. Ingesti seluruh 24 item otentik dari RotatingShop.yml via ShopCatalog
        ShopCategory rotCat = ShopCatalog.getCategory("rotatingshop");
        if (rotCat != null && !rotCat.getAllItems().isEmpty()) {
            for (ShopItemEntry entry : rotCat.getAllItems()) {
                ItemStack stack = entry.createItemStack(false);
                double basePrice = entry.getBaseBuyPrice();
                if (basePrice <= 0) basePrice = 250.0;
                RarityTier tier = basePrice >= 2000 ? RarityTier.LEGENDARY : (basePrice >= 500 ? RarityTier.EPIC : RarityTier.RARE);
                pool.add(new DealCandidate(stack, tier, basePrice, false));
            }
        } else {
            // Fallback cadangan item otentik
            pool.add(new DealCandidate(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), RarityTier.MYTHIC, 5000.0, false));
            pool.add(new DealCandidate(new ItemStack(Items.GOLDEN_APPLE, 2), RarityTier.RARE, 275.0, false));
            pool.add(new DealCandidate(new ItemStack(Items.SUSPICIOUS_STEW), RarityTier.UNCOMMON, 50.0, false));
            pool.add(new DealCandidate(new ItemStack(Items.ENCHANTED_BOOK), RarityTier.EPIC, 970.0, false));
        }

        // 2. Gabungkan 15 Item Kustom Mod Takasha-RPG
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.WHITE_SCROLL), RarityTier.RARE, 300.0, false));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.BLACK_SCROLL), RarityTier.EPIC, 650.0, false));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.TRANSMUTE_SCROLL), RarityTier.RARE, 280.0, false));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.HOLY_SCROLL), RarityTier.LEGENDARY, 1800.0, true));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.RANDOMIZATION_SCROLL), RarityTier.EPIC, 500.0, false));

        pool.add(new DealCandidate(new ItemStack(ModRpgItems.MAGIC_DUST, 4), RarityTier.UNCOMMON, 150.0, false));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.MYSTERY_DUST, 8), RarityTier.COMMON, 80.0, false));

        pool.add(new DealCandidate(new ItemStack(ModRpgItems.SLOT_INCREASER_1), RarityTier.RARE, 400.0, false));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.SLOT_INCREASER_2), RarityTier.EPIC, 900.0, false));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.SLOT_INCREASER_3), RarityTier.LEGENDARY, 1600.0, true));

        pool.add(new DealCandidate(new ItemStack(ModRpgItems.TAKASHA_GEM, 2), RarityTier.LEGENDARY, 1000.0, false));

        pool.add(new DealCandidate(new ItemStack(ModRpgItems.HOME_EXPANSION_SCROLL), RarityTier.LEGENDARY, 2000.0, true));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.TOME_POCKET_CRAFTING), RarityTier.LEGENDARY, 1200.0, false));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.CODEX_ENCHANTER), RarityTier.LEGENDARY, 1600.0, false));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.TOME_TINKERER), RarityTier.LEGENDARY, 1400.0, false));

        // 3. Mod Sakura Weapons (jika terpasang)
        Item sakuraKatana = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("sakura_weapons", "sakura_katana"));
        if (sakuraKatana != null && sakuraKatana != Items.AIR) {
            pool.add(new DealCandidate(new ItemStack(sakuraKatana), RarityTier.RARE, 750.0, false));
        }
        Item dragonSword = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("sakura_weapons", "dragon_mecha_overlord_sword"));
        if (dragonSword != null && dragonSword != Items.AIR) {
            pool.add(new DealCandidate(new ItemStack(dragonSword), RarityTier.LEGENDARY, 2200.0, true));
        }

        // Acak dan pilih 6 penawaran unik
        List<DealCandidate> shuffled = new ArrayList<>(pool);
        java.util.Collections.shuffle(shuffled, rng);

        int count = Math.min(6, shuffled.size());
        for (int i = 0; i < count; i++) {
            DealCandidate cand = shuffled.get(i);
            double discount = 15 + rng.nextInt(26); // Diskon 15% - 40%
            double base = cand.basePrice * (cand.tier != null ? cand.tier.getPriceMultiplier() * 0.5 : 1.0);
            double finalPrice = Math.max(1.0, Math.round(base * (1.0 - (discount / 100.0))));
            int stock = (cand.tier == RarityTier.LEGENDARY || cand.tier == RarityTier.MYTHIC) ? 1 : (1 + rng.nextInt(4));

            CURRENT_DEALS.add(new DailyDeal(
                cand.stack.copy(),
                cand.tier != null ? cand.tier : RarityTier.RARE,
                base,
                discount,
                finalPrice,
                cand.useGems,
                stock
            ));
        }
    }

    public static boolean purchaseDeal(int index, TakashaRpgData data) {
        if (index < 0 || index >= CURRENT_DEALS.size()) return false;
        DailyDeal deal = CURRENT_DEALS.get(index);
        if (deal.stock() <= 0) return false;

        if (deal.useGems()) {
            if (data.getGems() < (int) deal.finalPrice()) return false;
            data.addGems(-((int) deal.finalPrice()));
        } else {
            if (data.getBalance() < deal.finalPrice()) return false;
            data.addBalance(-deal.finalPrice());
        }

        String itemKey = BuiltInRegistries.ITEM.getKey(deal.stack().getItem()).toString();
        DynamicPricingEngine.recordPurchase(itemKey, deal.stack().getCount());

        CURRENT_DEALS.set(index, new DailyDeal(
            deal.stack(),
            deal.tier(),
            deal.originalPrice(),
            deal.discountPercent(),
            deal.finalPrice(),
            deal.useGems(),
            deal.stock() - 1
        ));
        return true;
    }

    private static class DealCandidate {
        final ItemStack stack;
        final RarityTier tier;
        final double basePrice;
        final boolean useGems;

        DealCandidate(ItemStack stack, RarityTier tier, double basePrice, boolean useGems) {
            this.stack = stack;
            this.tier = tier;
            this.basePrice = basePrice;
            this.useGems = useGems;
        }
    }
}
