package net.takasha.rpg.dungeon.reward;

import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import net.takasha.rpg.mmo.MMOItemManager;
import net.takasha.rpg.mmo.MMOItemTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Entri Hadiah Loot Dungeon.
 * Mewakili item dalam drop pool dengan probabilitas chance (0.01 s.d. 1.0),
 * rentang kuantitas min-max, multiplier scaling, identifikasi item (Vanilla / Takasha / MMOItems),
 * serta preservasi Custom Model Data dan Item Model untuk mencegah artefak barrier block.
 */
public class DungeonLootEntry {

    private String id; // Format: "takasha_rpg:rpg_coin", "minecraft:diamond", atau "mmo:SWORD:CUTLASS"
    private float chance; // 0.0f - 1.0f (misal 0.45f = 45%)
    private int minAmount;
    private int maxAmount;
    private boolean scaleWithMultiplier; // Jika true, dikalikan tier multiplier (misal koin)
    private String customDisplayName;
    private Integer customModelData;
    private String itemModel;

    public DungeonLootEntry(String id, float chance, int minAmount, int maxAmount, boolean scaleWithMultiplier,
                            String customDisplayName, Integer customModelData, String itemModel) {
        this.id = id != null ? id.trim() : "minecraft:air";
        this.chance = Math.max(0.001f, Math.min(1.0f, chance));
        this.minAmount = Math.max(1, minAmount);
        this.maxAmount = Math.max(this.minAmount, maxAmount);
        this.scaleWithMultiplier = scaleWithMultiplier;
        this.customDisplayName = customDisplayName;
        this.customModelData = customModelData;
        this.itemModel = itemModel;
    }

    public DungeonLootEntry(String id, float chance, int minAmount, int maxAmount, boolean scaleWithMultiplier, String customDisplayName) {
        this(id, chance, minAmount, maxAmount, scaleWithMultiplier, customDisplayName, null, null);
    }

    public DungeonLootEntry(String id, float chance, int minAmount, int maxAmount, boolean scaleWithMultiplier) {
        this(id, chance, minAmount, maxAmount, scaleWithMultiplier, null, null, null);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id != null && !id.isBlank()) {
            this.id = id.trim();
        }
    }

    public float getChance() {
        return chance;
    }

    public void setChance(float chance) {
        this.chance = Math.max(0.001f, Math.min(1.0f, chance));
    }

    public int getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(int minAmount) {
        this.minAmount = Math.max(1, minAmount);
        if (this.maxAmount < this.minAmount) {
            this.maxAmount = this.minAmount;
        }
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(int maxAmount) {
        this.maxAmount = Math.max(1, maxAmount);
        if (this.minAmount > this.maxAmount) {
            this.minAmount = this.maxAmount;
        }
    }

    public boolean isScaleWithMultiplier() {
        return scaleWithMultiplier;
    }

    public void setScaleWithMultiplier(boolean scaleWithMultiplier) {
        this.scaleWithMultiplier = scaleWithMultiplier;
    }

    public String getCustomDisplayName() {
        return customDisplayName;
    }

    public void setCustomDisplayName(String customDisplayName) {
        this.customDisplayName = customDisplayName;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(Integer customModelData) {
        this.customModelData = customModelData;
    }

    public String getItemModel() {
        return itemModel;
    }

    public void setItemModel(String itemModel) {
        this.itemModel = itemModel;
    }

    /**
     * Menghasilkan ItemStack aktual untuk roll dungeon.
     */
    public ItemStack createItemStack(int coinMultiplier, Random rng) {
        int count = minAmount;
        if (maxAmount > minAmount) {
            count = minAmount + rng.nextInt(maxAmount - minAmount + 1);
        }
        if (scaleWithMultiplier && coinMultiplier > 1) {
            count *= coinMultiplier;
        }
        if (count <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = resolveBaseStack(count);
        if (!stack.isEmpty() && customDisplayName != null && !customDisplayName.isBlank()) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(customDisplayName));
        }
        return stack;
    }

    /**
     * Menghasilkan preview ItemStack untuk ditampilkan di GUI manajer hadiah.
     */
    public ItemStack getDisplayStack() {
        ItemStack stack = resolveBaseStack(1);
        if (stack.isEmpty()) {
            stack = new ItemStack(Items.BARRIER);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("§cItem Tidak Ditemukan: " + id));
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Peluang Drop: §a" + String.format(Locale.ROOT, "%.1f%%", chance * 100.0f) + " §8(" + chance + ")"));
        String amountStr = (minAmount == maxAmount) ? String.valueOf(minAmount) : (minAmount + " - " + maxAmount);
        lore.add(Component.literal("§7Jumlah Drop: §e" + amountStr + (scaleWithMultiplier ? " §8(x Multiplier Tier)" : "")));
        lore.add(Component.literal("§7Skala Multiplier: " + (scaleWithMultiplier ? "§aAktif" : "§7Nonaktif")));
        lore.add(Component.literal("§7Resource ID: §8" + id));
        lore.add(Component.literal("§8§m-----------------------------"));
        lore.add(Component.literal("§e► Klik Kiri: §7Ubah Peluang Drop (Chance)"));
        lore.add(Component.literal("§e► Klik Kanan: §7Ubah Rentang Jumlah (min-max)"));
        lore.add(Component.literal("§b► Klik Tengah: §7Toggle Multiplier Scaling"));
        lore.add(Component.literal("§c► Shift + Klik Kanan: §7Hapus dari Wave ini"));

        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private ItemStack resolveBaseStack(int count) {
        if (id.toLowerCase(Locale.ROOT).startsWith("mmo:")) {
            // Format: mmo:<TYPE>:<ID> atau mmo:<ID>
            String[] parts = id.split(":");
            if (parts.length >= 3) {
                String type = parts[1].toUpperCase(Locale.ROOT);
                String mmoId = parts[2].toUpperCase(Locale.ROOT);
                ItemStack mmoStack = MMOItemManager.generateItemStack(type, mmoId, count);
                if (!mmoStack.isEmpty()) {
                    return applyOverrides(mmoStack);
                }
            } else if (parts.length == 2) {
                String mmoId = parts[1].toUpperCase(Locale.ROOT);
                MMOItemTemplate tmpl = MMOItemManager.getTemplateById(mmoId);
                if (tmpl != null) {
                    return applyOverrides(MMOItemManager.generateItemStack(tmpl.getItemType(), tmpl.getId(), count));
                }
            }
            return applyOverrides(new ItemStack(Items.IRON_SWORD, count));
        }

        // Resolusi ID dengan aliasing namespace cerdas: takasha: -> takasha_rpg:
        String resolvedId = id;
        if (resolvedId.startsWith("takasha:")) {
            resolvedId = "takasha_rpg:" + resolvedId.substring("takasha:".length());
        }

        try {
            Identifier identifier = Identifier.tryParse(resolvedId.contains(":") ? resolvedId : "minecraft:" + resolvedId);
            if (identifier != null) {
                Item item = BuiltInRegistries.ITEM.getValue(identifier);
                if (item != null && item != Items.AIR) {
                    return applyOverrides(new ItemStack(item, count));
                }
            }
            // Fallback: coba namespace takasha_rpg jika tidak diawali namespace
            if (!resolvedId.contains(":")) {
                Identifier rpgId = Identifier.tryParse("takasha_rpg:" + resolvedId);
                if (rpgId != null) {
                    Item item = BuiltInRegistries.ITEM.getValue(rpgId);
                    if (item != null && item != Items.AIR) {
                        return applyOverrides(new ItemStack(item, count));
                    }
                }
            }
        } catch (Exception ignored) {}

        return ItemStack.EMPTY;
    }

    private ItemStack applyOverrides(ItemStack stack) {
        if (stack.isEmpty()) return stack;
        if (customModelData != null && customModelData > 0) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                    List.of((float) customModelData),
                    List.of(),
                    List.of(),
                    List.of()
            ));
        }
        if (itemModel != null && !itemModel.isBlank()) {
            Identifier mid = Identifier.tryParse(itemModel);
            if (mid != null) {
                stack.set(DataComponents.ITEM_MODEL, mid);
            }
        }
        if (customDisplayName != null && !customDisplayName.isBlank()) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(customDisplayName));
        }
        return stack;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("chance", chance);
        json.addProperty("minAmount", minAmount);
        json.addProperty("maxAmount", maxAmount);
        json.addProperty("scaleWithMultiplier", scaleWithMultiplier);
        if (customDisplayName != null && !customDisplayName.isBlank()) {
            json.addProperty("displayName", customDisplayName);
        }
        if (customModelData != null) {
            json.addProperty("customModelData", customModelData);
        }
        if (itemModel != null && !itemModel.isBlank()) {
            json.addProperty("itemModel", itemModel);
        }
        return json;
    }

    public static DungeonLootEntry fromJson(JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : "minecraft:air";
        float chance = json.has("chance") ? json.get("chance").getAsFloat() : 0.5f;
        int min = json.has("minAmount") ? json.get("minAmount").getAsInt() : 1;
        int max = json.has("maxAmount") ? json.get("maxAmount").getAsInt() : min;
        boolean scale = json.has("scaleWithMultiplier") && json.get("scaleWithMultiplier").getAsBoolean();
        String name = json.has("displayName") ? json.get("displayName").getAsString() : null;
        Integer cmd = json.has("customModelData") ? json.get("customModelData").getAsInt() : null;
        String model = json.has("itemModel") ? json.get("itemModel").getAsString() : null;
        return new DungeonLootEntry(id, chance, min, max, scale, name, cmd, model);
    }

    public DungeonLootEntry copy() {
        return new DungeonLootEntry(id, chance, minAmount, maxAmount, scaleWithMultiplier, customDisplayName, customModelData, itemModel);
    }
}
