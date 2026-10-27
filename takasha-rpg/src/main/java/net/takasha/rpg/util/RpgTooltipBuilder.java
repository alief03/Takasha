package net.takasha.rpg.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.takasha.rpg.api.RarityTier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * UI/UX Pro Max Tooltip Architecture for Minecraft RPG Items & Interfaces.
 * Ensures consistent 6-zone visual hierarchy, contrast >= 4.5:1, clean formatting,
 * and eliminates text-wrapping overflows.
 */
public class RpgTooltipBuilder {

    public static final String DIVIDER = "§8§m────────────────────────────";
    public static final int MAX_LINE_LENGTH = 36;

    private final String title;
    private final RarityTier rarity;
    private String categoryBadge = "";
    private final List<Component> stats = new ArrayList<>();
    private final List<Component> descriptions = new ArrayList<>();
    private final List<String> affordances = new ArrayList<>();

    public enum StatGlyph {
        DEFENSE("§b🛡"),
        HEALTH("§c❤"),
        MANA("§b✦"),
        STRENGTH("§c⚔"),
        SPEED("§a⚡"),
        LUCK("§e★"),
        COIN("§6$"),
        SOUL("§d●"),
        MINING("§b⛏"),
        WARNING("§c⚠");

        public final String icon;

        StatGlyph(String icon) {
            this.icon = icon;
        }
    }

    public static RpgTooltipBuilder create(String title, RarityTier rarity) {
        return new RpgTooltipBuilder(title, rarity);
    }

    private RpgTooltipBuilder(String title, RarityTier rarity) {
        this.title = title;
        this.rarity = rarity;
    }

    public RpgTooltipBuilder category(String category) {
        this.categoryBadge = category;
        return this;
    }

    public RpgTooltipBuilder stat(String label, String value, StatGlyph glyph) {
        MutableComponent comp = Component.literal(" §7" + label + ": ")
                .append(Component.literal(value + " " + glyph.icon).withStyle(ChatFormatting.WHITE));
        stats.add(comp);
        return this;
    }

    public RpgTooltipBuilder stat(String rawLine) {
        stats.add(Component.literal(" " + rawLine));
        return this;
    }

    public RpgTooltipBuilder ability(String abilityName, String description) {
        descriptions.add(Component.literal("§5§lKEMAMPUAN: §d" + abilityName));
        for (String wrapped : wrapText(description, MAX_LINE_LENGTH)) {
            descriptions.add(Component.literal("  §7" + wrapped));
        }
        return this;
    }

    public RpgTooltipBuilder setBonus(String setName, int piecesRequired, List<String> bonusLines) {
        descriptions.add(Component.literal("§5§lBONUS SET: §e" + setName + " §8(" + piecesRequired + "/" + piecesRequired + " Piece)"));
        for (String line : bonusLines) {
            for (String wrapped : wrapText(line, MAX_LINE_LENGTH)) {
                descriptions.add(Component.literal("  §d✦ §f" + wrapped));
            }
        }
        return this;
    }

    public RpgTooltipBuilder description(String desc) {
        for (String wrapped : wrapText(desc, MAX_LINE_LENGTH)) {
            descriptions.add(Component.literal(" §7" + wrapped));
        }
        return this;
    }

    public RpgTooltipBuilder affordance(String actionKey, String actionDescription) {
        affordances.add("§e[" + actionKey + "] §7" + actionDescription);
        return this;
    }

    public void build(Consumer<Component> consumer) {
        // Zona 1: Category & Rarity Badge
        if (!categoryBadge.isEmpty()) {
            consumer.accept(Component.literal("§8[ " + rarity.getColorCode() + categoryBadge.toUpperCase() + " §8]"));
        } else {
            consumer.accept(rarity.getBadge());
        }

        // Zona 2 & 3: Stats
        if (!stats.isEmpty()) {
            consumer.accept(Component.literal(DIVIDER));
            for (Component stat : stats) {
                consumer.accept(stat);
            }
        }

        // Zona 4 & 5: Ability & Descriptions
        if (!descriptions.isEmpty()) {
            consumer.accept(Component.literal(DIVIDER));
            for (Component desc : descriptions) {
                consumer.accept(desc);
            }
        }

        // Zona 6: Interaction Affordances / Guide
        if (!affordances.isEmpty()) {
            consumer.accept(Component.literal(DIVIDER));
            for (String affordance : affordances) {
                consumer.accept(Component.literal(affordance));
            }
        }
    }

    public static List<String> wrapText(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;

        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.length() + word.length() + 1 > maxChars && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder();
            }
            if (current.length() > 0) {
                current.append(" ");
            }
            current.append(word);
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }
}
