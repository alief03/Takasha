package net.takasha.rpg.mmo;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Universal high-performance color and style parser for Takasha RPG items.
 * Supports:
 * - Legacy formatting: &0-&f, &l, &m, &n, &o, &k, &r (and section signs §0-§f)
 * - Hex colors: &#RRGGBB, <#RRGGBB>, <color:#RRGGBB>, &x&r&r&g&g&b&b
 * - Gradients: <gradient:#HEX1:#HEX2>text</gradient>
 * - Default non-italic toggle (cleans vanilla forced italics on items)
 */
public class TakashaColorParser {

    private static final int[] LEGACY_COLORS = new int[]{
            0x000000, // 0 Black
            0x0000AA, // 1 Dark Blue
            0x00AA00, // 2 Dark Green
            0x00AAAA, // 3 Dark Aqua
            0xAA0000, // 4 Dark Red
            0xAA00AA, // 5 Dark Purple
            0xFFAA00, // 6 Gold
            0xAAAAAA, // 7 Gray
            0x555555, // 8 Dark Gray
            0x5555FF, // 9 Blue
            0x55FF55, // a Green
            0x55FFFF, // b Aqua
            0xFF5555, // c Red
            0xFF55FF, // d Light Purple
            0xFFFF55, // e Yellow
            0xFFFFFF  // f White
    };

    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:([^>]+)>(.*?)(?:</gradient>|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPIGOT_HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern TAG_HEX_PATTERN = Pattern.compile("<(?:color:)?#([0-9a-fA-F]{6})>");
    private static final Pattern BUNGEE_HEX_PATTERN = Pattern.compile("&x&([0-9a-fA-F])&([0-9a-fA-F])&([0-9a-fA-F])&([0-9a-fA-F])&([0-9a-fA-F])&([0-9a-fA-F])");

    public static Component parse(String input) {
        return parse(input, false);
    }

    public static Component parseItemName(String input) {
        return parse(input, true);
    }

    public static Component parseLoreLine(String input) {
        return parse(input, true);
    }

    /**
     * Parse raw string with color & format codes into a rich Component.
     *
     * @param input             The raw input string
     * @param defaultNonItalic  If true, disables vanilla item italic by default unless &o is used
     */
    public static Component parse(String input, boolean defaultNonItalic) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        // 1. Process gradient tags first if present
        if (input.contains("<gradient:") || input.contains("<GRADIENT:")) {
            return parseWithGradients(input, defaultNonItalic);
        }

        // 2. Normalize hex formats to internal §x§r§r§g§g§b§b
        String normalized = normalizeHexCodes(input);

        // 3. Tokenize and parse legacy & normalized hex codes
        return parseTokens(normalized, defaultNonItalic);
    }

    private static String normalizeHexCodes(String text) {
        // Convert &#RRGGBB to \u00A7x\u00A7r\u00A7r\u00A7g\u00A7g\u00A7b\u00A7b
        Matcher spigotMatcher = SPIGOT_HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (spigotMatcher.find()) {
            String hex = spigotMatcher.group(1);
            spigotMatcher.appendReplacement(sb, toBungeeHex(hex));
        }
        spigotMatcher.appendTail(sb);
        text = sb.toString();

        // Convert <#RRGGBB> or <color:#RRGGBB>
        Matcher tagMatcher = TAG_HEX_PATTERN.matcher(text);
        sb = new StringBuilder();
        while (tagMatcher.find()) {
            String hex = tagMatcher.group(1);
            tagMatcher.appendReplacement(sb, toBungeeHex(hex));
        }
        tagMatcher.appendTail(sb);
        text = sb.toString();
        text = text.replace("</color>", "").replace("</#>", "");

        // Convert &x&r&r&g&g&b&b
        Matcher bungeeMatcher = BUNGEE_HEX_PATTERN.matcher(text);
        sb = new StringBuilder();
        while (bungeeMatcher.find()) {
            String hex = "" + bungeeMatcher.group(1) + bungeeMatcher.group(2) + bungeeMatcher.group(3)
                    + bungeeMatcher.group(4) + bungeeMatcher.group(5) + bungeeMatcher.group(6);
            bungeeMatcher.appendReplacement(sb, toBungeeHex(hex));
        }
        bungeeMatcher.appendTail(sb);

        return sb.toString();
    }

    private static String toBungeeHex(String hex6) {
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex6.toCharArray()) {
            sb.append('§').append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    private static Component parseTokens(String text, boolean defaultNonItalic) {
        MutableComponent root = Component.empty();

        TextColor currentColor = null;
        boolean bold = false;
        boolean italic = false;
        boolean underlined = false;
        boolean strikethrough = false;
        boolean obfuscated = false;

        StringBuilder buffer = new StringBuilder();
        int len = text.length();

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);

            if ((c == '&' || c == '§') && i + 1 < len) {
                // Check for 14-char bungee hex: §x§r§r§g§g§b§b
                if (i + 13 < len && text.charAt(i + 1) == 'x' && text.charAt(i + 2) == '§' && text.charAt(i + 4) == '§'
                        && text.charAt(i + 6) == '§' && text.charAt(i + 8) == '§' && text.charAt(i + 10) == '§' && text.charAt(i + 12) == '§') {
                    if (buffer.length() > 0) {
                        appendSegment(root, buffer.toString(), currentColor, bold, italic, underlined, strikethrough, obfuscated, defaultNonItalic);
                        buffer.setLength(0);
                    }
                    String hexStr = "" + text.charAt(i + 3) + text.charAt(i + 5) + text.charAt(i + 7)
                            + text.charAt(i + 9) + text.charAt(i + 11) + text.charAt(i + 13);
                    try {
                        int rgb = Integer.parseInt(hexStr, 16);
                        currentColor = TextColor.fromRgb(rgb);
                    } catch (NumberFormatException ignored) {}
                    // Color code resets formatting in Minecraft
                    bold = false;
                    italic = false;
                    underlined = false;
                    strikethrough = false;
                    obfuscated = false;
                    i += 13;
                    continue;
                }

                char code = Character.toLowerCase(text.charAt(i + 1));
                int colorIndex = getColorIndex(code);

                if (colorIndex != -1) {
                    // Standard color code
                    if (buffer.length() > 0) {
                        appendSegment(root, buffer.toString(), currentColor, bold, italic, underlined, strikethrough, obfuscated, defaultNonItalic);
                        buffer.setLength(0);
                    }
                    currentColor = TextColor.fromRgb(LEGACY_COLORS[colorIndex]);
                    // Legacy color codes reset formatting
                    bold = false;
                    italic = false;
                    underlined = false;
                    strikethrough = false;
                    obfuscated = false;
                    i++;
                    continue;
                }

                // Formatting codes
                boolean isFormat = true;
                switch (code) {
                    case 'l' -> {
                        if (buffer.length() > 0) {
                            appendSegment(root, buffer.toString(), currentColor, bold, italic, underlined, strikethrough, obfuscated, defaultNonItalic);
                            buffer.setLength(0);
                        }
                        bold = true;
                    }
                    case 'o' -> {
                        if (buffer.length() > 0) {
                            appendSegment(root, buffer.toString(), currentColor, bold, italic, underlined, strikethrough, obfuscated, defaultNonItalic);
                            buffer.setLength(0);
                        }
                        italic = true;
                    }
                    case 'n' -> {
                        if (buffer.length() > 0) {
                            appendSegment(root, buffer.toString(), currentColor, bold, italic, underlined, strikethrough, obfuscated, defaultNonItalic);
                            buffer.setLength(0);
                        }
                        underlined = true;
                    }
                    case 'm' -> {
                        if (buffer.length() > 0) {
                            appendSegment(root, buffer.toString(), currentColor, bold, italic, underlined, strikethrough, obfuscated, defaultNonItalic);
                            buffer.setLength(0);
                        }
                        strikethrough = true;
                    }
                    case 'k' -> {
                        if (buffer.length() > 0) {
                            appendSegment(root, buffer.toString(), currentColor, bold, italic, underlined, strikethrough, obfuscated, defaultNonItalic);
                            buffer.setLength(0);
                        }
                        obfuscated = true;
                    }
                    case 'r' -> {
                        if (buffer.length() > 0) {
                            appendSegment(root, buffer.toString(), currentColor, bold, italic, underlined, strikethrough, obfuscated, defaultNonItalic);
                            buffer.setLength(0);
                        }
                        currentColor = null;
                        bold = false;
                        italic = false;
                        underlined = false;
                        strikethrough = false;
                        obfuscated = false;
                    }
                    default -> isFormat = false;
                }

                if (isFormat) {
                    i++;
                    continue;
                }
            }

            buffer.append(c);
        }

        if (buffer.length() > 0) {
            appendSegment(root, buffer.toString(), currentColor, bold, italic, underlined, strikethrough, obfuscated, defaultNonItalic);
        }

        return root;
    }

    private static void appendSegment(
            MutableComponent root,
            String text,
            TextColor color,
            boolean bold,
            boolean italic,
            boolean underlined,
            boolean strikethrough,
            boolean obfuscated,
            boolean defaultNonItalic
    ) {
        if (text.isEmpty()) return;

        Style style = Style.EMPTY;
        if (color != null) {
            style = style.withColor(color);
        }
        if (bold) {
            style = style.withBold(true);
        }
        if (italic) {
            style = style.withItalic(true);
        } else if (defaultNonItalic) {
            style = style.withItalic(false);
        }
        if (underlined) {
            style = style.withUnderlined(true);
        }
        if (strikethrough) {
            style = style.withStrikethrough(true);
        }
        if (obfuscated) {
            style = style.withObfuscated(true);
        }

        root.append(Component.literal(text).withStyle(style));
    }

    private static Component parseWithGradients(String input, boolean defaultNonItalic) {
        MutableComponent root = Component.empty();
        Matcher matcher = GRADIENT_PATTERN.matcher(input);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String before = input.substring(lastEnd, matcher.start());
                root.append(parseTokens(normalizeHexCodes(before), defaultNonItalic));
            }

            String hexColorsPart = matcher.group(1);
            String text = matcher.group(2);
            String[] hexCodes = hexColorsPart.split(":");
            List<Integer> colors = new ArrayList<>();
            for (String h : hexCodes) {
                String clean = h.replace("#", "").trim();
                try {
                    colors.add(Integer.parseInt(clean, 16));
                } catch (NumberFormatException ignored) {}
            }

            if (!colors.isEmpty() && !text.isEmpty()) {
                int len = text.length();
                for (int i = 0; i < len; i++) {
                    char c = text.charAt(i);
                    float ratio = len > 1 ? (float) i / (len - 1) : 0f;
                    int rgb = interpolateMulti(colors, ratio);
                    Style style = Style.EMPTY.withColor(TextColor.fromRgb(rgb));
                    if (defaultNonItalic) {
                        style = style.withItalic(false);
                    }
                    root.append(Component.literal(String.valueOf(c)).withStyle(style));
                }
            } else if (!text.isEmpty()) {
                root.append(parseTokens(normalizeHexCodes(text), defaultNonItalic));
            }

            lastEnd = matcher.end();
        }

        if (lastEnd < input.length()) {
            String after = input.substring(lastEnd);
            root.append(parseTokens(normalizeHexCodes(after), defaultNonItalic));
        }

        return root;
    }

    private static int getColorIndex(char code) {
        if (code >= '0' && code <= '9') {
            return code - '0';
        } else if (code >= 'a' && code <= 'f') {
            return 10 + (code - 'a');
        }
        return -1;
    }

    private static int interpolateMulti(List<Integer> colors, float ratio) {
        if (colors.isEmpty()) return 0xFFFFFF;
        if (colors.size() == 1) return colors.getFirst();

        float segment = ratio * (colors.size() - 1);
        int index = (int) segment;
        float frac = segment - index;

        if (index >= colors.size() - 1) {
            return colors.getLast();
        }

        return interpolateColor(colors.get(index), colors.get(index + 1), frac);
    }

    private static int interpolateColor(int c1, int c2, float frac) {
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int r = (int) (r1 + frac * (r2 - r1));
        int g = (int) (g1 + frac * (g2 - g1));
        int b = (int) (b1 + frac * (b2 - b1));

        return (r << 16) | (g << 8) | b;
    }
}
