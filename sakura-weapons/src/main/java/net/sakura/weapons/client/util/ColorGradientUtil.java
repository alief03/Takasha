package net.sakura.weapons.client.util;

/**
 * Utility for generating Minecraft RGB gradient strings.
 * Supports both Vanilla Section symbol (§) and Plugin Ampersand (&) formats.
 */
public class ColorGradientUtil {

    // Sakura weapon set palette: Deep Sakura Rose (#E43A96) to Bright Blossom (#F251AD)
    public static final int SAKURA_START = 0xE43A96;
    public static final int SAKURA_END   = 0xF251AD;

    // Pink Legacy set palette: Hot Pink (#FF69B4) to Orchid Glow (#DA70D6)
    public static final int PINK_LEGACY_START = 0xFF69B4;
    public static final int PINK_LEGACY_END   = 0xDA70D6;

    /**
     * Converts a raw display name into a gradient formatted string.
     *
     * @param text         The plain text (e.g. "Sakura Katana")
     * @param startRgb     Starting 24-bit RGB color
     * @param endRgb       Ending 24-bit RGB color
     * @param bold         Whether to apply bold formatting
     * @param addBlossoms  Whether to prepend and append blossom emojis ("🌸 ")
     * @param useAmpersand True for server plugin format (&x&R&R...), False for vanilla format (§x§R§R...)
     * @return Formatted gradient string
     */
    public static String toGradient(String text, int startRgb, int endRgb, boolean bold, boolean addBlossoms, boolean useAmpersand) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        char prefix = useAmpersand ? '&' : '§';
        StringBuilder sb = new StringBuilder();

        if (addBlossoms) {
            appendHexCode(sb, prefix, startRgb, bold);
            sb.append("🌸 ");
        }

        int len = text.length();
        int r1 = (startRgb >> 16) & 0xFF;
        int g1 = (startRgb >> 8) & 0xFF;
        int b1 = startRgb & 0xFF;

        int r2 = (endRgb >> 16) & 0xFF;
        int g2 = (endRgb >> 8) & 0xFF;
        int b2 = endRgb & 0xFF;

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            float ratio = len > 1 ? (float) i / (float) (len - 1) : 0f;
            int r = Math.round(r1 + (r2 - r1) * ratio);
            int g = Math.round(g1 + (g2 - g1) * ratio);
            int b = Math.round(b1 + (b2 - b1) * ratio);
            int rgb = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);

            appendHexCode(sb, prefix, rgb, bold);
            sb.append(c);
        }

        if (addBlossoms) {
            appendHexCode(sb, prefix, endRgb, bold);
            sb.append(" 🌸");
        }

        return sb.toString();
    }

    private static void appendHexCode(StringBuilder sb, char prefix, int rgb, boolean bold) {
        String hex = String.format("%06X", rgb);
        sb.append(prefix).append('x');
        for (char c : hex.toCharArray()) {
            sb.append(prefix).append(Character.toLowerCase(c));
        }
        if (bold) {
            sb.append(prefix).append('l');
        }
    }

    public static String toSakuraGradient(String text, boolean bold, boolean addBlossoms, boolean useAmpersand) {
        return toGradient(text, SAKURA_START, SAKURA_END, bold, addBlossoms, useAmpersand);
    }

    public static String toPinkLegacyGradient(String text, boolean bold, boolean addBlossoms, boolean useAmpersand) {
        return toGradient(text, PINK_LEGACY_START, PINK_LEGACY_END, bold, addBlossoms, useAmpersand);
    }
}
