package net.sakura.weapons.client.fortune;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.sakura.weapons.client.config.TakashaMasterConfig;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intercepts and reshapes AuraSkills, AdvancedEnchantments, and Vanilla Fortune proc notifications.
 * Allows rerouting notifications between Action Bar, System Chat, and Screen Title,
 * or suppressing them completely with configurable detail levels (Detailed, Compact, Minimal).
 */
public class TakashaFortuneReshaper {

    private static volatile TakashaFortuneReshaper INSTANCE;

    private static final Pattern FORTUNE_OR_SKILL_PATTERN = Pattern.compile(
            "(?i)(auraskills|farming|double\\s*drop|triple\\s*drop|fortune\\s*proc|proc!|panen\\s*ganda|xp\\b|\\+\\d+\\s*(xp|exp))"
    );

    private static final Pattern MULTIPLIER_PATTERN = Pattern.compile("(?i)(x[2-9]|\\+\\d+)");
    private static final Pattern XP_PATTERN = Pattern.compile("(?i)(\\+\\d+\\s*(xp|exp))");

    public static TakashaFortuneReshaper getInstance() {
        if (INSTANCE == null) {
            synchronized (TakashaFortuneReshaper.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TakashaFortuneReshaper();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Checks if incoming text should be intercepted and reshaped.
     * @return true if the packet should be cancelled (suppressed / rerouted)
     */
    public boolean handleIncomingText(Component textComponent, boolean isActionBar) {
        TakashaMasterConfig config = TakashaMasterConfig.getInstance();
        if (!config.fortuneReshaperEnabled || textComponent == null) {
            return false;
        }

        String raw = textComponent.getString();
        if (raw == null || raw.isBlank()) {
            return false;
        }

        if (!FORTUNE_OR_SKILL_PATTERN.matcher(raw).find()) {
            return false; // Not a fortune / skill harvest proc
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return false;
        }

        if (config.fortuneDisplayMode == TakashaMasterConfig.FortuneDisplayMode.PASSTHROUGH) {
            return false; // Let original display
        }

        if (config.fortuneDisplayMode == TakashaMasterConfig.FortuneDisplayMode.OFF) {
            return true; // Cancel and mute notification
        }

        // Format according to detail level
        String formatted = reshapeText(raw, config);

        switch (config.fortuneDisplayMode) {
            case ACTIONBAR -> client.player.sendOverlayMessage(Component.literal(formatted));
            case CHAT -> client.player.sendSystemMessage(Component.literal(formatted));
            case TITLE -> {
                if (client.gui != null && client.gui.hud != null) {
                    client.gui.hud.setTitle(Component.literal(formatted));
                }
            }
            default -> {}
        }

        return true; // Cancel original packet to prevent duplicate display
    }

    private String reshapeText(String raw, TakashaMasterConfig config) {
        String multiplier = "";
        Matcher multMatcher = MULTIPLIER_PATTERN.matcher(raw);
        if (multMatcher.find()) {
            multiplier = multMatcher.group(1);
        }

        String xp = "";
        Matcher xpMatcher = XP_PATTERN.matcher(raw);
        if (xpMatcher.find()) {
            xp = xpMatcher.group(1);
        }

        return switch (config.fortuneDetailLevel) {
            case MINIMAL -> {
                if (!multiplier.isEmpty()) {
                    yield config.fortuneColorProc + multiplier + " Proc";
                }
                yield config.fortuneColorProc + "Harvest Proc";
            }
            case COMPACT -> {
                StringBuilder sb = new StringBuilder(config.fortunePrefix);
                if (!multiplier.isEmpty()) {
                    sb.append(config.fortuneColorProc).append("✨ Double Drop ").append(multiplier).append("! ");
                } else {
                    sb.append(config.fortuneColorProc).append("✨ Harvest Proc! ");
                }
                if (!xp.isEmpty()) {
                    sb.append(config.fortuneColorXp).append("(").append(xp).append(")");
                }
                yield sb.toString();
            }
            case DETAILED -> config.fortunePrefix + config.fortuneColorProc + raw.trim();
        };
    }
}
