package net.takasha.rpg.economy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.takasha.rpg.api.RarityTier;
import net.takasha.rpg.registry.ModRpgItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    private static long lastRotationDay = -1;

    public static List<DailyDeal> getCurrentDeals() {
        if (CURRENT_DEALS.isEmpty()) {
            rotateDailyDeals(0);
        }
        return CURRENT_DEALS;
    }

    public static void checkRotation(long dayTime) {
        long currentDay = dayTime / 24000L;
        if (currentDay != lastRotationDay) {
            rotateDailyDeals(currentDay);
            DynamicPricingEngine.onNewDay();
            lastRotationDay = currentDay;
        }
    }

    public static void rotateDailyDeals(long daySeed) {
        CURRENT_DEALS.clear();
        Random rng = new Random(daySeed * 314159L + 42L);

        // Pool of candidate items: Mix of vanilla & modded items
        List<DealCandidate> pool = new ArrayList<>();

        // 1. Vanilla treasures
        pool.add(new DealCandidate(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), RarityTier.LEGENDARY, 1500.0, false));
        pool.add(new DealCandidate(new ItemStack(Items.NETHERITE_INGOT, 2), RarityTier.EPIC, 800.0, false));
        pool.add(new DealCandidate(new ItemStack(Items.TOTEM_OF_UNDYING), RarityTier.RARE, 400.0, false));
        pool.add(new DealCandidate(new ItemStack(Items.ELYTRA), RarityTier.LEGENDARY, 2500.0, true));
        pool.add(new DealCandidate(new ItemStack(Items.GOLDEN_CARROT, 32), RarityTier.UNCOMMON, 120.0, false));
        pool.add(new DealCandidate(new ItemStack(Items.DIAMOND_BLOCK, 4), RarityTier.RARE, 350.0, false));

        // 2. Takasha-RPG Scrolls, Dusts & Trackers
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.WHITE_SCROLL), RarityTier.RARE, 300.0, false));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.BLACK_SCROLL), RarityTier.EPIC, 650.0, false));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.TRANSMUTE_SCROLL), RarityTier.RARE, 280.0, false));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.HOLY_SCROLL), RarityTier.LEGENDARY, 1800.0, true));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.MAGIC_DUST, 4), RarityTier.UNCOMMON, 150.0, false));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.SLOT_INCREASER_1), RarityTier.RARE, 400.0, false));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.SLOT_INCREASER_2), RarityTier.EPIC, 900.0, false));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.STAT_TRACKER_KILLS), RarityTier.UNCOMMON, 180.0, false));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.STAT_TRACKER_DAMAGE), RarityTier.RARE, 320.0, false));

        // 3. Rare Tomes (Low chance in daily deals)
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.HOME_EXPANSION_SCROLL), RarityTier.LEGENDARY, 2000.0, true));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.TOME_POCKET_CRAFTING), RarityTier.LEGENDARY, 1200.0, false));
        pool.add(new DealCandidate(new ItemStack(ModRpgItems.CODEX_ENCHANTER), RarityTier.LEGENDARY, 1600.0, false));

        // 4. Takasha Mod Weapons (if present in runtime registry)
        Item sakuraKatana = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("sakura_weapons", "sakura_katana"));
        if (sakuraKatana != null && sakuraKatana != Items.AIR) {
            pool.add(new DealCandidate(new ItemStack(sakuraKatana), RarityTier.RARE, 750.0, false));
        }
        Item dragonSword = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("sakura_weapons", "dragon_mecha_overlord_sword"));
        if (dragonSword != null && dragonSword != Items.AIR) {
            pool.add(new DealCandidate(new ItemStack(dragonSword), RarityTier.LEGENDARY, 2200.0, true));
        }

        // Pick 6 unique random deals
        List<DealCandidate> shuffled = new ArrayList<>(pool);
        java.util.Collections.shuffle(shuffled, rng);

        int count = Math.min(6, shuffled.size());
        for (int i = 0; i < count; i++) {
            DealCandidate cand = shuffled.get(i);
            double discount = 15 + rng.nextInt(26); // 15% to 40% discount
            double original = cand.basePrice * cand.tier.getPriceMultiplier() * 0.5; // Scaled by tier
            double finalPrice = Math.round(original * (1.0 - (discount / 100.0)));
            int stock = cand.tier == RarityTier.LEGENDARY || cand.tier == RarityTier.MYTHIC ? 1 : (1 + rng.nextInt(3));

            CURRENT_DEALS.add(new DailyDeal(
                cand.stack.copy(),
                cand.tier,
                original,
                discount,
                finalPrice,
                cand.useGems,
                stock
            ));
        }
    }

    private record DealCandidate(ItemStack stack, RarityTier tier, double basePrice, boolean useGems) {}
}
