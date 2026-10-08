package net.sakura.weapons.util;

import net.minecraft.util.StringUtil;

/**
 * Utility for Minecraft built-in color codes (§d for Light Purple/Pink, §l for Bold)
 * and Anvil text validation without bloated hex gradients.
 */
public final class MinecraftColorUtil {
    private MinecraftColorUtil() {}

    public static final String SAKURA_COLOR = "§d";
    public static final String PINK_LEGACY_COLOR = "§d";
    public static final String VALENTINE_COLOR = "§c";
    public static final String DRAGON_MECHA_COLOR = "§6";
    public static final String DRAGON_MECHA_SECONDARY = "§a";
    public static final String BOLD = "§l";
    public static final String RESET = "§r";

    /**
     * Formats a Sakura set item name with Minecraft built-in pink (§d), bold (§l), and sakura flowers (🌸).
     */
    public static String formatSakuraName(String baseName, boolean bold, boolean flowerDecor) {
        if (baseName == null) return "";
        if (flowerDecor) {
            return SAKURA_COLOR + (bold ? BOLD : "") + "🌸 " + baseName + " 🌸";
        }
        return SAKURA_COLOR + (bold ? BOLD : "") + baseName;
    }

    /**
     * Formats a Pink Legacy set item name with Minecraft built-in pink (§d), bold (§l), and sparkles (✨).
     */
    public static String formatPinkLegacyName(String baseName, boolean bold, boolean sparkleDecor) {
        if (baseName == null) return "";
        if (sparkleDecor) {
            return PINK_LEGACY_COLOR + (bold ? BOLD : "") + "✨ " + baseName + " ✨";
        }
        return PINK_LEGACY_COLOR + (bold ? BOLD : "") + baseName;
    }

    public static String formatPinkLegacyName(String baseName, boolean bold) {
        return formatPinkLegacyName(baseName, bold, true);
    }

    /**
     * Formats a Valentine set item name with Minecraft built-in crimson red (§c), bold (§l), and hearts (❤).
     */
    public static String formatValentineName(String baseName, boolean bold, boolean heartDecor) {
        if (baseName == null) return "";
        if (heartDecor) {
            return VALENTINE_COLOR + (bold ? BOLD : "") + "❤ " + baseName + " ❤";
        }
        return VALENTINE_COLOR + (bold ? BOLD : "") + baseName;
    }

    public static String formatValentineName(String baseName, boolean bold) {
        return formatValentineName(baseName, bold, true);
    }

    /**
     * Formats a Dragon Mecha Overlord set item name with Minecraft built-in gold (§6), bold (§l), and dragons (🐲).
     */
    public static String formatDragonMechaName(String baseName, boolean bold, boolean dragonDecor) {
        if (baseName == null) return "";
        if (dragonDecor) {
            return DRAGON_MECHA_COLOR + (bold ? BOLD : "") + "🐲 " + baseName + " 🐲";
        }
        return DRAGON_MECHA_COLOR + (bold ? BOLD : "") + baseName;
    }

    public static String formatDragonMechaName(String baseName, boolean bold) {
        return formatDragonMechaName(baseName, bold, true);
    }

    /**
     * Translates standard ampersand color codes (&0-9, &a-f, &k-o, &r) to section sign (§).
     */
    public static String translateAmpersand(String text) {
        if (text == null) return "";
        return text.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }

    /**
     * Filters text for Anvil names, allowing the Section sign (§) for built-in colors
     * while retaining standard chat character filtering.
     */
    public static String filterAnvilText(String text) {
        if (text == null) return "";
        String translated = translateAmpersand(text);
        StringBuilder sb = new StringBuilder();
        for (char c : translated.toCharArray()) {
            if (c == '§' || StringUtil.isAllowedChatCharacter(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Validates and formats an item name for the Anvil, keeping length under the 50-character limit.
     */
    public static String validateAndFormatAnvilName(String text) {
        if (text == null) return null;
        String filtered = filterAnvilText(text);
        if (filtered.length() > 50) {
            return null;
        }
        return filtered;
    }
}
