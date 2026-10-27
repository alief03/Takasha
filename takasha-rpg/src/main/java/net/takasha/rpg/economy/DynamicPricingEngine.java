package net.takasha.rpg.economy;

import java.util.HashMap;
import java.util.Map;

/**
 * DynamicPricingEngine
 * Mesin kalkulasi harga jual & beli pasar dinamis berbasis supply & demand EconomyShopGUI.
 */
public class DynamicPricingEngine {

    private static final Map<String, Integer> BUY_VOLUME = new HashMap<>();
    private static final Map<String, Integer> SELL_VOLUME = new HashMap<>();
    private static final Map<String, Double> BASE_PRICES = new HashMap<>();

    static {
        // Minerals & Ores
        BASE_PRICES.put("minecraft:diamond", 100.0);
        BASE_PRICES.put("minecraft:diamond_block", 900.0);
        BASE_PRICES.put("minecraft:iron_ingot", 15.0);
        BASE_PRICES.put("minecraft:iron_block", 135.0);
        BASE_PRICES.put("minecraft:raw_iron", 12.0);
        BASE_PRICES.put("minecraft:gold_ingot", 25.0);
        BASE_PRICES.put("minecraft:gold_block", 225.0);
        BASE_PRICES.put("minecraft:raw_gold", 20.0);
        BASE_PRICES.put("minecraft:copper_ingot", 5.0);
        BASE_PRICES.put("minecraft:raw_copper", 4.0);
        BASE_PRICES.put("minecraft:emerald", 50.0);
        BASE_PRICES.put("minecraft:emerald_block", 450.0);
        BASE_PRICES.put("minecraft:netherite_ingot", 1200.0);
        BASE_PRICES.put("minecraft:netherite_scrap", 280.0);
        BASE_PRICES.put("minecraft:ancient_debris", 350.0);
        BASE_PRICES.put("minecraft:coal", 4.0);
        BASE_PRICES.put("minecraft:charcoal", 3.0);
        BASE_PRICES.put("minecraft:coal_block", 36.0);
        BASE_PRICES.put("minecraft:lapis_lazuli", 4.0);
        BASE_PRICES.put("minecraft:redstone", 3.0);
        BASE_PRICES.put("minecraft:quartz", 5.0);
        BASE_PRICES.put("minecraft:amethyst_shard", 8.0);

        // Farming & Crops
        BASE_PRICES.put("minecraft:wheat", 2.0);
        BASE_PRICES.put("minecraft:wheat_seeds", 0.5);
        BASE_PRICES.put("minecraft:carrot", 2.0);
        BASE_PRICES.put("minecraft:potato", 2.0);
        BASE_PRICES.put("minecraft:baked_potato", 3.5);
        BASE_PRICES.put("minecraft:beetroot", 2.0);
        BASE_PRICES.put("minecraft:melon_slice", 1.0);
        BASE_PRICES.put("minecraft:melon", 8.0);
        BASE_PRICES.put("minecraft:pumpkin", 6.0);
        BASE_PRICES.put("minecraft:sugar_cane", 2.0);
        BASE_PRICES.put("minecraft:bamboo", 1.0);
        BASE_PRICES.put("minecraft:cactus", 1.5);
        BASE_PRICES.put("minecraft:cocoa_beans", 3.0);
        BASE_PRICES.put("minecraft:nether_wart", 5.0);
        BASE_PRICES.put("minecraft:apple", 4.0);
        BASE_PRICES.put("minecraft:golden_apple", 150.0);
        BASE_PRICES.put("minecraft:enchanted_golden_apple", 2500.0);

        // Wood & Logs
        for (String wood : new String[]{"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak"}) {
            BASE_PRICES.put("minecraft:" + wood + "_log", 3.0);
            BASE_PRICES.put("minecraft:" + wood + "_planks", 0.75);
        }
        BASE_PRICES.put("minecraft:crimson_stem", 4.0);
        BASE_PRICES.put("minecraft:warped_stem", 4.0);

        // Building Blocks
        BASE_PRICES.put("minecraft:cobblestone", 0.5);
        BASE_PRICES.put("minecraft:stone", 0.75);
        BASE_PRICES.put("minecraft:deepslate", 0.75);
        BASE_PRICES.put("minecraft:cobbled_deepslate", 0.5);
        BASE_PRICES.put("minecraft:dirt", 0.25);
        BASE_PRICES.put("minecraft:sand", 0.5);
        BASE_PRICES.put("minecraft:gravel", 0.5);
        BASE_PRICES.put("minecraft:obsidian", 15.0);
        BASE_PRICES.put("minecraft:crying_obsidian", 35.0);
        BASE_PRICES.put("minecraft:glass", 1.0);

        // Mob Drops
        BASE_PRICES.put("minecraft:rotten_flesh", 1.5);
        BASE_PRICES.put("minecraft:bone", 2.5);
        BASE_PRICES.put("minecraft:bone_meal", 1.0);
        BASE_PRICES.put("minecraft:string", 2.0);
        BASE_PRICES.put("minecraft:spider_eye", 3.0);
        BASE_PRICES.put("minecraft:gunpowder", 5.0);
        BASE_PRICES.put("minecraft:ender_pearl", 20.0);
        BASE_PRICES.put("minecraft:blaze_rod", 25.0);
        BASE_PRICES.put("minecraft:ghast_tear", 80.0);
        BASE_PRICES.put("minecraft:slime_ball", 10.0);
        BASE_PRICES.put("minecraft:magma_cream", 12.0);
        BASE_PRICES.put("minecraft:leather", 4.0);
        BASE_PRICES.put("minecraft:feather", 1.5);
        BASE_PRICES.put("minecraft:ink_sac", 3.0);
        BASE_PRICES.put("minecraft:glow_ink_sac", 6.0);

        // Foods
        BASE_PRICES.put("minecraft:beef", 3.0);
        BASE_PRICES.put("minecraft:cooked_beef", 6.0);
        BASE_PRICES.put("minecraft:porkchop", 3.0);
        BASE_PRICES.put("minecraft:cooked_porkchop", 6.0);
        BASE_PRICES.put("minecraft:mutton", 2.5);
        BASE_PRICES.put("minecraft:cooked_mutton", 5.0);
        BASE_PRICES.put("minecraft:chicken", 2.0);
        BASE_PRICES.put("minecraft:cooked_chicken", 4.5);
        BASE_PRICES.put("minecraft:bread", 4.0);
    }

    public static double getBasePrice(String itemId) {
        return BASE_PRICES.getOrDefault(itemId.toLowerCase(), -1.0);
    }

    public static double getDynamicBuyPrice(String itemId, double fallbackBasePrice) {
        double base = BASE_PRICES.getOrDefault(itemId.toLowerCase(), fallbackBasePrice);
        if (base <= 0) base = fallbackBasePrice;
        int bought = BUY_VOLUME.getOrDefault(itemId, 0);
        int sold = SELL_VOLUME.getOrDefault(itemId, 0);
        int netDemand = bought - sold;

        // Dynamic multiplier: +2% per 10 net buys, max +100%
        double factor = 1.0 + Math.max(-0.4, Math.min(1.0, netDemand * 0.002));
        return Math.max(1.0, base * factor);
    }

    public static double getDynamicSellPrice(String itemId, double fallbackBasePrice) {
        double base = BASE_PRICES.getOrDefault(itemId.toLowerCase(), fallbackBasePrice);
        if (base <= 0) return -1.0;

        int bought = BUY_VOLUME.getOrDefault(itemId, 0);
        int sold = SELL_VOLUME.getOrDefault(itemId, 0);
        int netDemand = bought - sold;

        // Sell price is typically 50% of buy price, dropping if flooded
        double factor = 0.5 + Math.max(-0.25, Math.min(0.3, netDemand * 0.001));
        return Math.max(0.1, base * factor);
    }

    public static void recordPurchase(String itemId, int count) {
        BUY_VOLUME.put(itemId, BUY_VOLUME.getOrDefault(itemId, 0) + count);
    }

    public static void recordSale(String itemId, int count) {
        SELL_VOLUME.put(itemId, SELL_VOLUME.getOrDefault(itemId, 0) + count);
    }

    public static void onNewDay() {
        // Natural market decay towards equilibrium each dawn
        for (String id : BUY_VOLUME.keySet()) {
            BUY_VOLUME.put(id, (int) (BUY_VOLUME.get(id) * 0.8));
        }
        for (String id : SELL_VOLUME.keySet()) {
            SELL_VOLUME.put(id, (int) (SELL_VOLUME.get(id) * 0.8));
        }
    }

    public static String getMarketTrendBadge(String itemId, double baseBuy, double currentBuy) {
        if (baseBuy <= 0 || currentBuy <= 0) {
            return "§7▬ Harga Stabil";
        }
        double diff = currentBuy - baseBuy;
        double pct = (diff / baseBuy) * 100.0;
        if (pct >= 5.0) {
            return String.format("§a▲ Permintaan Tinggi (+%.1f%%)", pct);
        } else if (pct <= -5.0) {
            return String.format("§c▼ Penawaran Melimpah (%.1f%%)", pct);
        } else {
            return "§7▬ Harga Stabil";
        }
    }

    public static Map<String, Integer> getBuyVolumes() {
        return new HashMap<>(BUY_VOLUME);
    }

    public static Map<String, Integer> getSellVolumes() {
        return new HashMap<>(SELL_VOLUME);
    }
}
