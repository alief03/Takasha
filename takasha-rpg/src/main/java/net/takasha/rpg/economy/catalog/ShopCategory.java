package net.takasha.rpg.economy.catalog;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.*;

/**
 * ShopCategory
 * Kategori toko dalam sistem pasar EconomyShopGUI-Premium 6.3.1.
 */
public class ShopCategory {

    private final String id;
    private int slot;
    private String title;
    private String iconMaterial;
    private List<String> lore;
    private boolean enabled;
    private final Map<Integer, List<ShopItemEntry>> pages = new TreeMap<>();
    private final List<ShopItemEntry> allItems = new ArrayList<>();

    public ShopCategory(String id, int slot, String title, String iconMaterial, List<String> lore, boolean enabled) {
        this.id = id;
        this.slot = slot;
        this.title = title != null ? title : id;
        this.iconMaterial = iconMaterial != null ? iconMaterial : "CHEST";
        this.lore = lore != null ? lore : new ArrayList<>();
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIconMaterial() {
        return iconMaterial;
    }

    public void setIconMaterial(String iconMaterial) {
        this.iconMaterial = iconMaterial;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void addItem(int page, ShopItemEntry item) {
        pages.computeIfAbsent(page, k -> new ArrayList<>()).add(item);
        allItems.add(item);
    }

    public List<ShopItemEntry> getPageItems(int page) {
        return pages.getOrDefault(page, Collections.emptyList());
    }

    public int getTotalPages() {
        return Math.max(1, pages.size());
    }

    public List<ShopItemEntry> getAllItems() {
        return Collections.unmodifiableList(allItems);
    }

    public Item resolveIconItem() {
        String clean = iconMaterial.toLowerCase().trim();
        if (clean.startsWith("minecraft:")) clean = clean.substring(10);
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", clean));
        return (item != null && item != Items.AIR) ? item : Items.CHEST;
    }

    public ItemStack createIconStack() {
        Item item = resolveIconItem();
        ItemStack stack = new ItemStack(item);

        stack.set(DataComponents.CUSTOM_NAME, Component.literal(ShopItemEntry.formatColor(title)));

        List<Component> finalLore = new ArrayList<>();
        if (!lore.isEmpty()) {
            for (String l : lore) {
                String processed = l;
                if (processed.contains("%nextRotation%")) {
                    processed = processed.replace("%nextRotation%", net.takasha.rpg.economy.TakashaDailyShopManager.getRemainingTimeString());
                }
                finalLore.add(Component.literal(ShopItemEntry.formatColor(processed)));
            }
        }
        finalLore.add(Component.literal(""));
        finalLore.add(Component.literal("§7Total Komoditas: §e" + allItems.size() + " Item"));
        finalLore.add(Component.literal("§7Kapasitas: §b" + getTotalPages() + " Halaman"));
        finalLore.add(Component.literal("§e[Klik Kiri] §7Buka Katalog Toko"));

        stack.set(DataComponents.LORE, new ItemLore(finalLore));
        return stack;
    }
}
