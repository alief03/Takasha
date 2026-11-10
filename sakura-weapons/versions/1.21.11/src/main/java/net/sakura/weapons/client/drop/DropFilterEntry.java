package net.sakura.weapons.client.drop;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Scalable Drop Filter Rule supporting:
 * 1. Standard Item ID: "minecraft:cobblestone" or "cobblestone"
 * 2. CustomModelData (CMD): "minecraft:paper#1024" or "cmd:10052"
 * 3. Custom Display Name: "name:Batu Sampah" or "name:\"Pecahan Besi\""
 * 4. Automatic extraction from currently held item (Quick-Add).
 */
public class DropFilterEntry {

    private final String rawPattern;
    private final String itemId;
    private final Integer customModelData;
    private final String customNameContains;

    public DropFilterEntry(String rawPattern, String itemId, Integer customModelData, String customNameContains) {
        this.rawPattern = rawPattern != null ? rawPattern.trim() : "";
        this.itemId = itemId != null && !itemId.isEmpty() ? itemId.trim().toLowerCase() : null;
        this.customModelData = customModelData;
        this.customNameContains = customNameContains != null && !customNameContains.isEmpty() ? customNameContains.trim().toLowerCase() : null;
    }

    public static DropFilterEntry parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new DropFilterEntry("", null, null, null);
        }
        String s = input.trim();

        // 1. Name pattern: name:"Custom Name" or name:Custom Name
        if (s.toLowerCase().startsWith("name:")) {
            String name = s.substring(5).trim();
            if (name.startsWith("\"") && name.endsWith("\"") && name.length() >= 2) {
                name = name.substring(1, name.length() - 1);
            }
            return new DropFilterEntry(s, null, null, name);
        }

        // 2. CMD pattern: cmd:10052
        if (s.toLowerCase().startsWith("cmd:")) {
            try {
                int cmd = Integer.parseInt(s.substring(4).trim());
                return new DropFilterEntry(s, null, cmd, null);
            } catch (NumberFormatException ignored) {}
        }

        // 3. ID + CMD pattern: minecraft:iron_nugget#10052
        if (s.contains("#")) {
            String[] parts = s.split("#", 2);
            String id = parts[0].trim();
            try {
                int cmd = Integer.parseInt(parts[1].trim());
                return new DropFilterEntry(s, id, cmd, null);
            } catch (NumberFormatException ignored) {
                return new DropFilterEntry(s, id, null, null);
            }
        }

        // 4. Default: Raw Item ID
        return new DropFilterEntry(s, s, null, null);
    }

    public static DropFilterEntry fromItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        String id = getItemId(stack);
        Integer cmd = getCustomModelData(stack);
        String cleanName = getCleanItemName(stack);

        if (cmd != null) {
            String pattern = id + "#" + cmd;
            return new DropFilterEntry(pattern, id, cmd, null);
        }

        if (stack.has(DataComponents.CUSTOM_NAME) && !cleanName.isEmpty()) {
            String pattern = "name:\"" + cleanName + "\"";
            return new DropFilterEntry(pattern, null, null, cleanName);
        }

        return new DropFilterEntry(id, id, null, null);
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        // Check CustomModelData if specified
        if (this.customModelData != null) {
            Integer itemCmd = getCustomModelData(stack);
            if (itemCmd == null || !this.customModelData.equals(itemCmd)) {
                return false;
            }
        }

        // Check Item ID if specified
        if (this.itemId != null) {
            String stackId = getItemId(stack);
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String path = id != null ? id.getPath() : "";
            if (!stackId.equalsIgnoreCase(this.itemId) && !path.equalsIgnoreCase(this.itemId)) {
                return false;
            }
        }

        // Check Custom Name if specified
        if (this.customNameContains != null) {
            String cleanName = getCleanItemName(stack).toLowerCase();
            if (!cleanName.contains(this.customNameContains.toLowerCase())) {
                return false;
            }
        }

        // Fallback if none of the specific fields were defined
        if (this.itemId == null && this.customModelData == null && this.customNameContains == null) {
            String stackId = getItemId(stack);
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String path = id != null ? id.getPath() : "";
            String cleanName = getCleanItemName(stack);
            String raw = this.rawPattern.toLowerCase();
            return stackId.equalsIgnoreCase(raw) || path.equalsIgnoreCase(raw) || cleanName.equalsIgnoreCase(raw);
        }

        return true;
    }

    public String getRawPattern() {
        return rawPattern;
    }

    public String getDisplayLabel() {
        if (customModelData != null && itemId != null) {
            return itemId + " [CMD #" + customModelData + "]";
        }
        if (customModelData != null) {
            return "[CMD #" + customModelData + "]";
        }
        if (customNameContains != null) {
            return "Nama: \"" + customNameContains + "\"";
        }
        return itemId != null ? itemId : rawPattern;
    }

    public static String getItemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null ? id.toString() : "";
    }

    public static String getCleanItemName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        String name = stack.getHoverName().getString();
        return name.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
    }

    public static Integer getCustomModelData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        try {
            Object cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
            if (cmd != null) {
                // Try 1.20.5 - 1.21.3 `value()`
                try {
                    Method valueMethod = cmd.getClass().getMethod("value");
                    Object val = valueMethod.invoke(cmd);
                    if (val instanceof Number num) return num.intValue();
                } catch (NoSuchMethodException e) {
                    // Try 1.21.4 / 26.2 `floats()`
                    try {
                        Method floatsMethod = cmd.getClass().getMethod("floats");
                        Object list = floatsMethod.invoke(cmd);
                        if (list instanceof List<?> l && !l.isEmpty()) {
                            Object first = l.get(0);
                            if (first instanceof Number num) return num.intValue();
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
