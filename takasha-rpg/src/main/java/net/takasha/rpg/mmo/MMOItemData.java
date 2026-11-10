package net.takasha.rpg.mmo;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.*;

public class MMOItemData {

    public static final String NBT_ROOT = "TakashaMMOItem";

    private String type = "TOOL";
    private String id = "";
    private MMOItemTier tier = null;
    private int customModelData = 0;
    private String displayName = "";
    private final Map<MMOStatType, Double> stats = new EnumMap<>(MMOStatType.class);
    private final List<MMOAbilityReference> abilities = new ArrayList<>();
    private final List<String> rawLore = new ArrayList<>();
    private final List<String> permEffects = new ArrayList<>();
    private final List<String> potionEffects = new ArrayList<>();
    private final List<String> commands = new ArrayList<>();

    // Tool Attributes & Special Properties (MMOItems 1:1)
    private boolean autosmelt = false;
    private boolean bouncingCrack = false;
    private int pickaxePower = 0;
    private double miningEfficiency = 0.0;
    private int maxDurability = 0;
    private int currentDurability = 0;
    private boolean willBreak = true;
    private int revisionId = 1;

    public MMOItemData() {
    }

    public MMOItemData(String type, String id, MMOItemTier tier, int customModelData, String displayName) {
        this.type = type != null ? type.toUpperCase(Locale.ROOT) : "TOOL";
        this.id = id;
        this.tier = tier;
        this.customModelData = customModelData;
        this.displayName = displayName;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public MMOItemTier getTier() { return tier; }
    public void setTier(MMOItemTier tier) { this.tier = tier; }

    public int getCustomModelData() { return customModelData; }
    public void setCustomModelData(int customModelData) { this.customModelData = customModelData; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Map<MMOStatType, Double> getStats() { return stats; }
    public double getStat(MMOStatType type) { return stats.getOrDefault(type, 0.0); }
    public void setStat(MMOStatType type, double value) { this.stats.put(type, value); }

    public List<MMOAbilityReference> getAbilities() { return abilities; }
    public void addAbility(MMOAbilityReference ability) { this.abilities.add(ability); }

    public List<String> getRawLore() { return rawLore; }
    public void setRawLore(List<String> lore) {
        this.rawLore.clear();
        if (lore != null) {
            this.rawLore.addAll(lore);
        }
    }

    public boolean isAutosmelt() { return autosmelt; }
    public void setAutosmelt(boolean autosmelt) { this.autosmelt = autosmelt; }

    public boolean isBouncingCrack() { return bouncingCrack; }
    public void setBouncingCrack(boolean bouncingCrack) { this.bouncingCrack = bouncingCrack; }

    public int getPickaxePower() { return pickaxePower; }
    public void setPickaxePower(int pickaxePower) { this.pickaxePower = pickaxePower; }

    public double getMiningEfficiency() { return miningEfficiency; }
    public void setMiningEfficiency(double miningEfficiency) { this.miningEfficiency = miningEfficiency; }

    public int getMaxDurability() { return maxDurability; }
    public void setMaxDurability(int maxDurability) { this.maxDurability = maxDurability; }

    public int getCurrentDurability() { return currentDurability; }
    public void setCurrentDurability(int currentDurability) { this.currentDurability = currentDurability; }

    public boolean isWillBreak() { return willBreak; }
    public void setWillBreak(boolean willBreak) { this.willBreak = willBreak; }

    public int getRevisionId() { return revisionId; }
    public void setRevisionId(int revisionId) { this.revisionId = revisionId; }

    public List<String> getPermEffects() { return permEffects; }
    public List<String> getPotionEffects() { return potionEffects; }
    public List<String> getCommands() { return commands; }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        if (type != null) tag.putString("Type", type);
        if (id != null) tag.putString("Id", id);
        if (tier != null) tag.putString("Tier", tier.name());
        tag.putInt("CustomModelData", customModelData);
        if (displayName != null) tag.putString("DisplayName", displayName);
        tag.putInt("RevisionId", revisionId);

        tag.putBoolean("Autosmelt", autosmelt);
        tag.putBoolean("BouncingCrack", bouncingCrack);
        tag.putInt("PickaxePower", pickaxePower);
        tag.putDouble("MiningEfficiency", miningEfficiency);
        tag.putInt("MaxDurability", maxDurability);
        tag.putInt("CurrentDurability", currentDurability > 0 ? currentDurability : maxDurability);
        tag.putBoolean("WillBreak", willBreak);

        CompoundTag statsTag = new CompoundTag();
        for (Map.Entry<MMOStatType, Double> entry : stats.entrySet()) {
            statsTag.putDouble(entry.getKey().name(), entry.getValue());
        }
        tag.put("Stats", statsTag);

        ListTag abilitiesTag = new ListTag();
        for (MMOAbilityReference ability : abilities) {
            abilitiesTag.add(ability.toTag());
        }
        tag.put("Abilities", abilitiesTag);

        ListTag loreTag = new ListTag();
        for (String line : rawLore) {
            loreTag.add(StringTag.valueOf(line));
        }
        tag.put("Lore", loreTag);

        ListTag permTag = new ListTag();
        for (String pe : permEffects) {
            permTag.add(StringTag.valueOf(pe));
        }
        tag.put("PermEffects", permTag);

        ListTag potionTag = new ListTag();
        for (String pe : potionEffects) {
            potionTag.add(StringTag.valueOf(pe));
        }
        tag.put("PotionEffects", potionTag);

        ListTag cmdTag = new ListTag();
        for (String cmd : commands) {
            cmdTag.add(StringTag.valueOf(cmd));
        }
        tag.put("Commands", cmdTag);

        return tag;
    }

    public static MMOItemData fromTag(CompoundTag tag) {
        MMOItemData data = new MMOItemData();
        data.type = tag.getStringOr("Type", "TOOL");
        data.id = tag.getStringOr("Id", "");
        data.tier = tag.contains("Tier") ? MMOItemTier.fromString(tag.getStringOr("Tier", "COMMON")) : null;
        data.customModelData = tag.getIntOr("CustomModelData", 0);
        data.displayName = tag.getStringOr("DisplayName", "");
        data.revisionId = tag.getIntOr("RevisionId", 1);

        data.autosmelt = tag.getBooleanOr("Autosmelt", false);
        data.bouncingCrack = tag.getBooleanOr("BouncingCrack", false);
        data.pickaxePower = tag.getIntOr("PickaxePower", 0);
        data.miningEfficiency = tag.getDoubleOr("MiningEfficiency", 0.0);
        data.maxDurability = tag.getIntOr("MaxDurability", 0);
        data.currentDurability = tag.getIntOr("CurrentDurability", data.maxDurability);
        data.willBreak = tag.getBooleanOr("WillBreak", true);

        if (tag.contains("Stats")) {
            CompoundTag statsTag = tag.getCompoundOrEmpty("Stats");
            for (String key : statsTag.keySet()) {
                try {
                    MMOStatType statType = MMOStatType.valueOf(key);
                    data.stats.put(statType, statsTag.getDoubleOr(key, 0.0));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        if (tag.contains("Abilities")) {
            ListTag abilitiesTag = tag.getListOrEmpty("Abilities");
            for (int i = 0; i < abilitiesTag.size(); i++) {
                Tag t = abilitiesTag.get(i);
                if (t instanceof CompoundTag c) {
                    data.abilities.add(MMOAbilityReference.fromTag(c));
                }
            }
        }

        if (tag.contains("Lore")) {
            ListTag loreTag = tag.getListOrEmpty("Lore");
            for (int i = 0; i < loreTag.size(); i++) {
                data.rawLore.add(loreTag.get(i).asString().orElse(""));
            }
        }

        if (tag.contains("PermEffects")) {
            ListTag permTag = tag.getListOrEmpty("PermEffects");
            for (int i = 0; i < permTag.size(); i++) {
                data.permEffects.add(permTag.get(i).asString().orElse(""));
            }
        }

        if (tag.contains("PotionEffects")) {
            ListTag potionTag = tag.getListOrEmpty("PotionEffects");
            for (int i = 0; i < potionTag.size(); i++) {
                data.potionEffects.add(potionTag.get(i).asString().orElse(""));
            }
        }

        if (tag.contains("Commands")) {
            ListTag cmdTag = tag.getListOrEmpty("Commands");
            for (int i = 0; i < cmdTag.size(); i++) {
                data.commands.add(cmdTag.get(i).asString().orElse(""));
            }
        }

        return data;
    }

    public static boolean hasMMOData(ItemStack stack) {
        if (stack.isEmpty()) return false;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag root = customData.copyTag();
            return root.contains(NBT_ROOT) || root.contains("MMOITEMS_ITEM_TYPE");
        }
        return false;
    }

    public static Optional<MMOItemData> readFromStack(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag root = customData.copyTag();
            if (root.contains(NBT_ROOT)) {
                return Optional.of(fromTag(root.getCompoundOrEmpty(NBT_ROOT)));
            } else if (root.contains("MMOITEMS_ITEM_TYPE")) {
                // Reconstruct from Spigot 1:1 tags
                MMOItemData data = new MMOItemData();
                data.type = root.getStringOr("MMOITEMS_ITEM_TYPE", "TOOL");
                data.id = root.getStringOr("MMOITEMS_ITEM_ID", "");
                data.tier = MMOItemTier.fromString(root.getStringOr("MMOITEMS_TIER", "COMMON"));
                data.revisionId = root.getIntOr("MMOITEMS_REVISION_ID", 1);
                data.autosmelt = root.getBooleanOr("MMOITEMS_AUTOSMELT", false);
                data.bouncingCrack = root.getBooleanOr("MMOITEMS_BOUNCING_CRACK", false);
                data.pickaxePower = root.getIntOr("MMOITEMS_PICKAXE_POWER", 0);
                data.maxDurability = root.getIntOr("MMOITEMS_MAX_DURABILITY", 0);
                data.currentDurability = root.getIntOr("MMOITEMS_DURABILITY", data.maxDurability);
                data.willBreak = root.getBooleanOr("MMOITEMS_WILL_BREAK", true);
                return Optional.of(data);
            }
        }
        return Optional.empty();
    }

    public void writeToStack(ItemStack stack) {
        if (stack.isEmpty()) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            // 1. Structured internal Fabric component
            root.put(NBT_ROOT, toTag());

            // 2. 1:1 Spigot MMOItems Standard Root NBT tags
            if (type != null) root.putString("MMOITEMS_ITEM_TYPE", type);
            if (id != null) root.putString("MMOITEMS_ITEM_ID", id);
            if (tier != null) root.putString("MMOITEMS_TIER", tier.name());
            root.putInt("MMOITEMS_REVISION_ID", revisionId);
            if (autosmelt) root.putBoolean("MMOITEMS_AUTOSMELT", true);
            if (bouncingCrack) root.putBoolean("MMOITEMS_BOUNCING_CRACK", true);
            if (pickaxePower > 0) root.putInt("MMOITEMS_PICKAXE_POWER", pickaxePower);
            if (miningEfficiency > 0) root.putDouble("MMOITEMS_MINING_EFFICIENCY", miningEfficiency);
            if (maxDurability > 0) {
                root.putInt("MMOITEMS_MAX_DURABILITY", maxDurability);
                root.putInt("MMOITEMS_DURABILITY", currentDurability > 0 ? currentDurability : maxDurability);
            }
            root.putBoolean("MMOITEMS_WILL_BREAK", willBreak);

            for (Map.Entry<MMOStatType, Double> entry : stats.entrySet()) {
                root.putDouble("MMOITEMS_" + entry.getKey().name(), entry.getValue());
            }
        });
    }
}
