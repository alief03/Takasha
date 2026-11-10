package net.takasha.rpg.economy.catalog;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.takasha.rpg.economy.DynamicPricingEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * ShopItemEntry
 * Representasi entri barang dagangan paritas EconomyShopGUI-Premium 6.3.1.
 */
public class ShopItemEntry {

    private final String id;
    private final String category;
    private final String material;
    private final double baseBuyPrice;
    private final double baseSellPrice;
    private final String displayName;
    private final List<String> lore;
    private final String spawnerType;
    private final List<String> potionTypes;
    private final List<String> enchantments;
    private final List<String> buyCommands;
    private final List<String> sellCommands;

    public ShopItemEntry(
        String id,
        String category,
        String material,
        double baseBuyPrice,
        double baseSellPrice,
        String displayName,
        List<String> lore,
        String spawnerType,
        List<String> potionTypes,
        List<String> enchantments,
        List<String> buyCommands,
        List<String> sellCommands
    ) {
        this.id = id;
        this.category = category;
        this.material = material != null ? material : "AIR";
        this.baseBuyPrice = baseBuyPrice;
        this.baseSellPrice = baseSellPrice;
        this.displayName = displayName;
        this.lore = lore != null ? lore : new ArrayList<>();
        this.spawnerType = spawnerType;
        this.potionTypes = potionTypes != null ? potionTypes : new ArrayList<>();
        this.enchantments = enchantments != null ? enchantments : new ArrayList<>();
        this.buyCommands = buyCommands != null ? buyCommands : new ArrayList<>();
        this.sellCommands = sellCommands != null ? sellCommands : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getMaterial() {
        return material;
    }

    public double getBaseBuyPrice() {
        return baseBuyPrice;
    }

    public double getBaseSellPrice() {
        return baseSellPrice;
    }

    public double getBuyPrice() {
        return baseBuyPrice;
    }

    public double getSellPrice() {
        return baseSellPrice;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public String getSpawnerType() {
        return spawnerType;
    }

    public List<String> getPotionTypes() {
        return potionTypes;
    }

    public List<String> getEnchantments() {
        return enchantments;
    }

    public List<String> getBuyCommands() {
        return buyCommands;
    }

    public List<String> getSellCommands() {
        return sellCommands;
    }

    public boolean canBuy() {
        return baseBuyPrice >= 0;
    }

    public boolean canSell() {
        return baseSellPrice >= 0;
    }

    public boolean isCommandItem() {
        return !buyCommands.isEmpty() || !sellCommands.isEmpty();
    }

    /**
     * Mengambil item vanilla/mod yang sesuai.
     */
    public Item resolveItem() {
        String cleanMat = material.toLowerCase().trim();
        if (cleanMat.startsWith("minecraft:")) {
            cleanMat = cleanMat.substring(10);
        }

        // Cek mod takasha_rpg atau sakura_weapons
        if (cleanMat.contains(":")) {
            String[] parts = cleanMat.split(":", 2);
            Item modItem = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(parts[0], parts[1]));
            if (modItem != null && modItem != Items.AIR) return modItem;
        }

        Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", cleanMat));
        if (item != null && item != Items.AIR) return item;

        // Fallback untuk spawners
        if ("spawner".equalsIgnoreCase(cleanMat)) return Items.SPAWNER;
        return Items.BARRIER;
    }

    /**
     * Membuat ItemStack lengkap dengan NBT data, lore, dan harga pasar dinamis.
     */
    public ItemStack createItemStack(boolean withPriceLore) {
        return createItemStack(withPriceLore, null);
    }

    public ItemStack createItemStack(boolean withPriceLore, net.minecraft.core.RegistryAccess registryAccess) {
        Item item = resolveItem();
        ItemStack stack = new ItemStack(item);

        // Pasang NBT Spawner jika merupakan spawner
        if (spawnerType != null && !spawnerType.isEmpty() && item == Items.SPAWNER) {
            String entityId = "minecraft:" + spawnerType.toLowerCase();
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.putString("id", entityId);
            stack.set(DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.TypedEntityData.of(net.minecraft.world.level.block.entity.BlockEntityTypes.MOB_SPAWNER, tag));
        }

        // Pasang Enchantment Data jika memiliki enchantments
        if (enchantments != null && !enchantments.isEmpty()) {
            for (String ench : enchantments) {
                EnchantmentDetailHelper.applyEnchantmentData(stack, ench, registryAccess);
            }
        }

        // Pasang Custom Name jika ada (override dari config, atau jika spawner)
        if (displayName != null && !displayName.isEmpty()) {
            stack.set(DataComponents.CUSTOM_NAME, net.takasha.rpg.mmo.TakashaColorParser.parseItemName(displayName));
        } else if (spawnerType != null && !spawnerType.isEmpty()) {
            stack.set(DataComponents.CUSTOM_NAME, net.takasha.rpg.mmo.TakashaColorParser.parseItemName("&e" + spawnerType.replace("_", " ") + " Spawner"));
        }

        if (withPriceLore) {
            List<Component> finalLore = new ArrayList<>();

            // 1. Lore asli dari berkas konfigurasi
            if (!lore.isEmpty()) {
                for (String l : lore) {
                    finalLore.add(net.takasha.rpg.mmo.TakashaColorParser.parseLoreLine(l));
                }
                finalLore.add(Component.empty());
            }

            // 2. Info Detail Enchantment jika ada
            if (enchantments != null && !enchantments.isEmpty()) {
                for (String ench : enchantments) {
                    finalLore.addAll(EnchantmentDetailHelper.buildDetailedTooltipLore(ench));
                }
            }

            // 3. Info Spawner
            if (spawnerType != null && !spawnerType.isEmpty()) {
                finalLore.add(Component.literal("§7Jenis Monster: §6" + spawnerType));
            }

            // 4. Info Dynamic Pricing & Tren Pasar
            String itemKey = BuiltInRegistries.ITEM.getKey(item).toString();
            double curBuy = canBuy() ? DynamicPricingEngine.getDynamicBuyPrice(itemKey, baseBuyPrice) : -1;
            double curSell = canSell() ? DynamicPricingEngine.getDynamicSellPrice(itemKey, baseSellPrice) : -1;

            if (canBuy()) {
                finalLore.add(Component.literal("§7Harga Beli: §a$" + String.format("%,.2f", curBuy)));
            }
            if (canSell()) {
                finalLore.add(Component.literal("§7Harga Jual: §c$" + String.format("%,.2f", curSell)));
            }

            // Indikator tren lore
            if (canBuy() || canSell()) {
                String trendBadge = DynamicPricingEngine.getMarketTrendBadge(itemKey, baseBuyPrice, curBuy);
                finalLore.add(Component.literal("§7Tren Pasar: " + trendBadge));
                finalLore.add(Component.literal(""));
            }

            // 5. Petunjuk Interaksi Navigasi
            if (canBuy()) {
                finalLore.add(Component.literal("§e[Klik Kiri] §7Buka Kalkulator Kuantitas Beli"));
            }
            if (canSell()) {
                finalLore.add(Component.literal("§6[Klik Kanan] §7Jual 1x Langsung"));
                finalLore.add(Component.literal("§d[Shift + Klik Kanan] §7Jual Cepat Seluruhnya (Inventaris)"));
            }

            stack.set(DataComponents.LORE, new ItemLore(finalLore));
        } else {
            // Ketika withPriceLore == false (misalnya saat dibeli ke inventaris pemain)
            if (enchantments != null && !enchantments.isEmpty()) {
                List<Component> itemLore = new ArrayList<>();
                for (String ench : enchantments) {
                    itemLore.addAll(EnchantmentDetailHelper.buildDetailedTooltipLore(ench));
                }
                stack.set(DataComponents.LORE, new ItemLore(itemLore));
            }
        }

        return stack;
    }

    public static String formatColor(String text) {
        if (text == null) return "";
        return text.replace('&', '§');
    }
}
