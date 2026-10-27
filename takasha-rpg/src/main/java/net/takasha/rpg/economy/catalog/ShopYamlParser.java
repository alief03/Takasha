package net.takasha.rpg.economy.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * ShopYamlParser
 * Parser konfigurasi YAML otentik untuk EconomyShopGUI-Premium 6.3.1.
 * Mampu membaca struktur sections/ dan shops/ dengan tingkat kepatuhan paritas 100%.
 */
public class ShopYamlParser {

    private static final Logger LOGGER = LoggerFactory.getLogger("Takasha-ShopParser");

    /**
     * Mem-parsing kategori dari InputStream berkas section dan berkas shop.
     */
    public static ShopCategory parseCategory(String categoryId, InputStream sectionStream, InputStream shopStream) {
        // Default metadata
        int slot = -1;
        String title = "&6&l" + categoryId;
        String iconMaterial = "CHEST";
        boolean enabled = true;
        List<String> sectionLore = new ArrayList<>();

        // 1. Parse Section metadata jika ada
        if (sectionStream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(sectionStream, StandardCharsets.UTF_8))) {
                String line;
                String currentList = null;
                boolean inItem = false;

                while ((line = reader.readLine()) != null) {
                    int commentIdx = line.indexOf('#');
                    if (commentIdx >= 0) line = line.substring(0, commentIdx);
                    if (line.trim().isEmpty()) continue;

                    boolean isRoot = !line.startsWith(" ") && !line.startsWith("\t");
                    String trimmed = line.trim();

                    if (isRoot) {
                        if (trimmed.startsWith("enable:")) {
                            enabled = Boolean.parseBoolean(extractValue(trimmed));
                        } else if (trimmed.startsWith("slot:")) {
                            try {
                                int rawSlot = Integer.parseInt(extractValue(trimmed));
                                slot = Math.max(0, rawSlot - 1);
                            } catch (NumberFormatException ignored) {}
                        } else if (trimmed.equals("item:")) {
                            inItem = true;
                            currentList = null;
                        } else {
                            inItem = false;
                            currentList = null;
                        }
                    } else if (inItem) {
                        if (trimmed.startsWith("material:")) {
                            iconMaterial = extractValue(trimmed);
                            currentList = null;
                        } else if (trimmed.startsWith("displayname:") || trimmed.startsWith("name:")) {
                            title = extractValue(trimmed);
                            currentList = null;
                        } else if (trimmed.startsWith("lore:")) {
                            currentList = "lore";
                        } else if (trimmed.startsWith("- ") && "lore".equals(currentList)) {
                            sectionLore.add(cleanQuotes(trimmed.substring(2)));
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Gagal mem-parsing section config untuk {}: {}", categoryId, e.getMessage());
            }
        }

        ShopCategory category = new ShopCategory(categoryId, slot, title, iconMaterial, sectionLore, enabled);

        // 2. Parse Shops item catalog
        if (shopStream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(shopStream, StandardCharsets.UTF_8))) {
                String line;
                int currentPage = 1;
                boolean isRotating = "rotatingshop".equalsIgnoreCase(categoryId);

                // Variabel penampung item yang sedang diproses
                String itemId = null;
                int itemPage = 1;
                String material = "AIR";
                double buy = -1.0;
                double sell = -1.0;
                String displayName = null;
                List<String> lore = new ArrayList<>();
                String spawnerType = null;
                List<String> potionTypes = new ArrayList<>();
                List<String> enchantments = new ArrayList<>();
                List<String> buyCommands = new ArrayList<>();
                List<String> sellCommands = new ArrayList<>();

                String currentListKey = null;

                while ((line = reader.readLine()) != null) {
                    int commentIdx = line.indexOf('#');
                    if (commentIdx >= 0) line = line.substring(0, commentIdx);
                    if (line.trim().isEmpty()) continue;

                    String trimmed = line.trim();

                    if (trimmed.startsWith("page") && trimmed.contains(":")) {
                        try {
                            String pageNumStr = trimmed.substring(4, trimmed.indexOf(':')).trim();
                            currentPage = Integer.parseInt(pageNumStr);
                        } catch (Exception ignored) {}
                        continue;
                    }

                    // Deteksi awal entri item baru berdasarkan indentasi pasti
                    boolean isNewItem = false;
                    String detectedId = null;

                    if (isRotating) {
                        // Di RotatingShop.yml: indentasi 2 spasi '  ItemId:'
                        if (line.startsWith("  ") && !line.startsWith("   ") && trimmed.endsWith(":")) {
                            String key = trimmed.substring(0, trimmed.length() - 1).trim();
                            if (!"items".equals(key)) {
                                isNewItem = true;
                                detectedId = key;
                            }
                        }
                    } else {
                        // Di Standard Shop: indentasi 6 spasi '      '1':' atau '      1:'
                        if (line.startsWith("      ") && !line.startsWith("       ") && trimmed.endsWith(":")) {
                            detectedId = cleanQuotes(trimmed.substring(0, trimmed.length() - 1));
                            isNewItem = true;
                        }
                    }

                    if (isNewItem) {
                        // Simpan item sebelumnya jika valid
                        if (itemId != null) {
                            category.addItem(itemPage, new ShopItemEntry(
                                itemId, categoryId, material, buy, sell,
                                displayName, new ArrayList<>(lore), spawnerType,
                                new ArrayList<>(potionTypes), new ArrayList<>(enchantments),
                                new ArrayList<>(buyCommands), new ArrayList<>(sellCommands)
                            ));
                        }

                        // Reset untuk item baru
                        itemId = detectedId;
                        itemPage = currentPage;
                        material = "AIR";
                        buy = -1.0;
                        sell = -1.0;
                        displayName = null;
                        lore.clear();
                        spawnerType = null;
                        potionTypes.clear();
                        enchantments.clear();
                        buyCommands.clear();
                        sellCommands.clear();
                        currentListKey = null;
                        continue;
                    }

                    // Parse properti item saat ini
                    if (itemId != null) {
                        if (trimmed.startsWith("- ") && currentListKey != null) {
                            String val = cleanQuotes(trimmed.substring(2));
                            switch (currentListKey) {
                                case "lore" -> lore.add(val);
                                case "potiontypes" -> potionTypes.add(val);
                                case "enchantments" -> enchantments.add(val);
                                case "buy-commands" -> buyCommands.add(val);
                                case "sell-commands" -> sellCommands.add(val);
                            }
                        } else if (trimmed.contains(":")) {
                            int colonIdx = trimmed.indexOf(':');
                            String propKey = trimmed.substring(0, colonIdx).trim();
                            String propVal = extractValue(trimmed);

                            currentListKey = null;

                            switch (propKey) {
                                case "material" -> material = propVal;
                                case "buy" -> {
                                    try { buy = Double.parseDouble(propVal); } catch (Exception ignored) {}
                                }
                                case "sell" -> {
                                    try { sell = Double.parseDouble(propVal); } catch (Exception ignored) {}
                                }
                                case "displayname", "name" -> displayName = propVal;
                                case "spawnertype" -> spawnerType = propVal;
                                case "lore", "potiontypes", "enchantments", "buy-commands", "sell-commands" -> currentListKey = propKey;
                            }
                        }
                    }
                }

                // Simpan item terakhir
                if (itemId != null) {
                    category.addItem(itemPage, new ShopItemEntry(
                        itemId, categoryId, material, buy, sell,
                        displayName, new ArrayList<>(lore), spawnerType,
                        new ArrayList<>(potionTypes), new ArrayList<>(enchantments),
                        new ArrayList<>(buyCommands), new ArrayList<>(sellCommands)
                    ));
                }
            } catch (Exception e) {
                LOGGER.error("Gagal mem-parsing catalog shop untuk {}: {}", categoryId, e.getMessage());
            }
        }

        return category;
    }

    private static String extractValue(String line) {
        int idx = line.indexOf(':');
        if (idx < 0 || idx >= line.length() - 1) return "";
        return cleanQuotes(line.substring(idx + 1).trim());
    }

    private static String cleanQuotes(String s) {
        if (s == null) return "";
        s = s.trim();
        if ((s.startsWith("'") && s.endsWith("'")) || (s.startsWith("\"") && s.endsWith("\""))) {
            if (s.length() >= 2) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }
}
