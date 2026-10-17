package net.takasha.rpg.economy;

import java.util.HashMap;
import java.util.Map;

public class DynamicPricingEngine {

    private static final Map<String, Integer> BUY_VOLUME = new HashMap<>();
    private static final Map<String, Integer> SELL_VOLUME = new HashMap<>();

    public static double getDynamicBuyPrice(String itemId, double basePrice) {
        int bought = BUY_VOLUME.getOrDefault(itemId, 0);
        int sold = SELL_VOLUME.getOrDefault(itemId, 0);
        int netDemand = bought - sold;

        // Dynamic multiplier: +2% per 10 net buys, max +100%
        double factor = 1.0 + Math.max(-0.4, Math.min(1.0, netDemand * 0.002));
        return Math.max(1.0, basePrice * factor);
    }

    public static double getDynamicSellPrice(String itemId, double basePrice) {
        int bought = BUY_VOLUME.getOrDefault(itemId, 0);
        int sold = SELL_VOLUME.getOrDefault(itemId, 0);
        int netDemand = bought - sold;

        // Sell price is typically 50% of buy price, dropping if flooded
        double factor = 0.5 + Math.max(-0.25, Math.min(0.3, netDemand * 0.001));
        return Math.max(0.5, basePrice * factor);
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
}
