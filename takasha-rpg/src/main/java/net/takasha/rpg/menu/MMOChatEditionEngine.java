package net.takasha.rpg.menu;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.takasha.rpg.enchant.TakashaEnchantRegistry;
import net.takasha.rpg.mmo.TakashaColorParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Takasha RPG Chat Edition & Stat Edition Engine.
 * Implements interactive chat prompt session and value processor.
 *
 * Enhanced with:
 * - Universal Color Parser support (TakashaColorParser)
 * - Clickable Chat Suggestion Chips (ClickEvent.Action.SUGGEST_COMMAND)
 * - Dynamic Command Autocompletion via /mi input <Tab>
 */
public class MMOChatEditionEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger("takasha-mmo-chat-edition");

    public enum SuggestionType {
        NONE,
        NAME,
        LORE,
        MATERIALS,
        ENCHANTS,
        ABILITIES,
        ELEMENTS,
        TIER,
        UPGRADE,
        GEM_SOCKETS,
        GEM_COLORS,
        ITEM_TYPES,
        NUMERIC,
        ID,
        GENERIC
    }

    public record EditSession(
            UUID playerUuid,
            String title,
            String statName,
            List<String> promptLines,
            SuggestionType suggestionType,
            List<String> customSuggestions,
            Runnable onCancel,
            Function<String, Boolean> processor
    ) {}

    private static final Map<UUID, EditSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;
        initialized = true;

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender == null) return true;
            UUID uuid = sender.getUUID();
            EditSession session = ACTIVE_SESSIONS.get(uuid);
            if (session == null) return true;

            // Player is in active chat edition session!
            String text = message.signedContent();
            if (text == null) {
                text = "";
            }
            text = text.trim();

            if ("cancel".equalsIgnoreCase(text)) {
                cancelSession(sender);
                return false; // Suppress chat message
            }

            // Process input
            processInput(sender, text);
            return false; // Suppress public chat broadcast
        });
    }

    public static void cancelSession(ServerPlayer player) {
        if (player == null) return;
        UUID uuid = player.getUUID();
        EditSession session = ACTIVE_SESSIONS.remove(uuid);
        if (session != null && session.onCancel() != null && player.level().getServer() != null) {
            player.level().getServer().execute(session.onCancel()::run);
        }
    }

    public static boolean processInput(ServerPlayer player, String input) {
        if (player == null) return false;
        UUID uuid = player.getUUID();
        EditSession session = ACTIVE_SESSIONS.get(uuid);
        if (session == null) return false;

        String clean = input != null ? input.trim() : "";
        if ("cancel".equalsIgnoreCase(clean)) {
            cancelSession(player);
            return true;
        }

        if (player.level().getServer() != null) {
            player.level().getServer().execute(() -> {
                boolean success = false;
                try {
                    success = session.processor().apply(clean);
                } catch (Exception e) {
                    LOGGER.warn("Input processing exception for player {}: {}", player.getScoreboardName(), e.getMessage());
                    player.sendSystemMessage(TakashaColorParser.parse("&8[&eTakasha-RPG&8] &c" + (e.getMessage() != null ? e.getMessage() : "Invalid input format.")));
                    success = false;
                }

                if (success) {
                    ACTIVE_SESSIONS.remove(uuid);
                } else {
                    // FAULT-TOLERANCE:
                    // Keep session active for immediate retry
                }
            });
            return true;
        }
        return false;
    }

    public static void beginSession(
            ServerPlayer player,
            String title,
            String statName,
            List<String> promptLines,
            Runnable onCancel,
            Function<String, Boolean> processor
    ) {
        beginSession(player, title, statName, promptLines, SuggestionType.NONE, null, onCancel, processor);
    }

    public static void beginSession(
            ServerPlayer player,
            String title,
            String statName,
            List<String> promptLines,
            SuggestionType suggestionType,
            List<String> customSuggestions,
            Runnable onCancel,
            Function<String, Boolean> processor
    ) {
        if (player == null) return;
        initialize();

        // 1. Close inventory
        player.closeContainer();

        // 2. Register session
        ACTIVE_SESSIONS.put(player.getUUID(), new EditSession(
                player.getUUID(),
                title != null ? title : "Item Edition",
                statName,
                promptLines,
                suggestionType != null ? suggestionType : SuggestionType.NONE,
                customSuggestions != null ? customSuggestions : List.of(),
                onCancel,
                processor
        ));

        // 3. Send decorative chat banner
        player.sendSystemMessage(TakashaColorParser.parse("&e&m-----------------------------------------------------"));

        // Each prompt line with official prefix §8[§eTakasha-RPG§8] §7
        if (promptLines != null) {
            for (String line : promptLines) {
                player.sendSystemMessage(TakashaColorParser.parse("&8[&eTakasha-RPG&8] &7" + line));
            }
        }

        // Suggestions Clickable Chips line
        List<String> chips = getTopChipsForType(suggestionType, customSuggestions);
        if (!chips.isEmpty()) {
            MutableComponent chipsMessage = TakashaColorParser.parse("&8[&eSuggestions&8] ").copy();
            for (int i = 0; i < chips.size(); i++) {
                String chip = chips.get(i);
                if (i > 0) {
                    chipsMessage.append(Component.literal(" §8| "));
                }
                MutableComponent chipComp = Component.literal("[" + chip + "]")
                        .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                                .withClickEvent(new ClickEvent.SuggestCommand("/input " + chip + " "))
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("§eKlik untuk mengisi '§b/input " + chip + " §e' ke chat bar"))));
                chipsMessage.append(chipComp);
            }
            player.sendSystemMessage(chipsMessage);
        }

        // Prominent 1-Click Autocomplete Button & Guidance
        MutableComponent tabButton = Component.literal("  ► [ ⚡ Klik di Sini untuk Autocomplete Tab (/input) ] ◄")
                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true)
                        .withClickEvent(new ClickEvent.SuggestCommand("/input "))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("§eKlik untuk langsung mengisi '/input ' di chat bar dan membuka pop-up autocompletion Tab!"))));
        player.sendSystemMessage(tabButton);
        player.sendSystemMessage(TakashaColorParser.parse("&8[&eTakasha-RPG&8] &eKetik &b/input <Tab> &eatau klik tombol di atas untuk melihat seluruh opsi autocomplete!"));

        // Cancel instruction
        player.sendSystemMessage(TakashaColorParser.parse("&8[&eTakasha-RPG&8] &7Type 'cancel' to abort editing."));

        // 4. Send screen title: Title = "§6§l" + title, Subtitle = "See chat."
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 40, 10));
        player.connection.send(new ClientboundSetTitleTextPacket(TakashaColorParser.parse("&6&l" + (title != null ? title : "Item Edition"))));
        player.connection.send(new ClientboundSetSubtitleTextPacket(TakashaColorParser.parse("&eSee chat.")));
    }

    public static void beginSession(
            ServerPlayer player,
            String title,
            String statName,
            String promptMessage,
            Runnable onCancel,
            Consumer<String> onInput
    ) {
        List<String> lines = new ArrayList<>();
        if (promptMessage != null && !promptMessage.isEmpty()) {
            lines.add(promptMessage);
        }
        beginSession(player, title, statName, lines, SuggestionType.NONE, null, onCancel, val -> {
            onInput.accept(val);
            return true;
        });
    }

    public static List<String> getActiveSessionSuggestions(ServerPlayer player) {
        if (player == null) return Collections.emptyList();
        EditSession session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session == null) return Collections.emptyList();

        if (session.customSuggestions() != null && !session.customSuggestions().isEmpty()) {
            return session.customSuggestions();
        }

        return getAllSuggestionsForType(session.suggestionType());
    }

    private static List<String> getTopChipsForType(SuggestionType type, List<String> custom) {
        if (custom != null && !custom.isEmpty()) {
            return custom.stream().limit(7).toList();
        }
        if (type == null) return Collections.emptyList();

        return switch (type) {
            case NAME -> List.of("&6", "&c", "&a", "&b", "&e", "&l", "&#FFD700");
            case LORE -> List.of("&7", "&f", "&e", "&6", "&c", "&a", "&7&m-------------");
            case MATERIALS -> List.of("DIAMOND_SWORD", "NETHERITE_SWORD", "IRON_SWORD", "BOW", "CROSSBOW", "TRIDENT");
            case ENCHANTS -> List.of("lifesteal", "cleave", "bleed", "telepathy", "autosmelt", "sharpness", "unbreaking");
            case ABILITIES -> List.of("circular_slash", "fireball", "shadow_step", "meteor", "lightning_strike");
            case ELEMENTS -> List.of("5.0%", "10.0%", "15.0%", "25.0%", "50.0%", "10 -> 25");
            case TIER -> List.of("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC");
            case GEM_SOCKETS -> List.of("RED", "BLUE", "YELLOW", "GREEN", "PRISMATIC", "ANY");
            case GEM_COLORS -> List.of("Red", "Blue", "Yellow", "Green", "Prismatic", "Nether", "Rainbow");
            case ITEM_TYPES -> List.of("SWORD", "ARMOR", "BOW", "CROSSBOW", "TOOL", "ACCESSORY", "CATALYST");
            case UPGRADE -> List.of("5", "10", "15", "10 2 0 0 5 30");
            case NUMERIC -> List.of("10.0", "15.0", "25.0", "50.0", "10 -> 25");
            case ID -> List.of("[id]", "NEW_ITEM", "CUSTOM_SWORD");
            case GENERIC, NONE -> Collections.emptyList();
        };
    }

    public static List<String> getAllSuggestionsForType(SuggestionType type) {
        if (type == null) return Collections.emptyList();

        return switch (type) {
            case NAME -> List.of("&6", "&c", "&a", "&b", "&e", "&d", "&l", "&o", "&n", "&m", "&#FFD700", "[display_name]");
            case LORE -> List.of("&7", "&f", "&e", "&6", "&c", "&a", "&b", "&d", "&7&m---------------------", "[lore_text]");
            case MATERIALS -> {
                try {
                    yield BuiltInRegistries.ITEM.keySet().stream()
                            .map(id -> id.getPath().toUpperCase(Locale.ROOT))
                            .filter(name -> !name.contains("AIR"))
                            .sorted()
                            .toList();
                } catch (Throwable ignored) {
                    yield List.of(
                            "DIAMOND_SWORD", "NETHERITE_SWORD", "IRON_SWORD", "GOLDEN_SWORD", "STONE_SWORD", "WOODEN_SWORD",
                            "DIAMOND_AXE", "NETHERITE_AXE", "IRON_AXE", "BOW", "CROSSBOW", "TRIDENT", "MACE",
                            "DIAMOND_PICKAXE", "NETHERITE_PICKAXE", "IRON_PICKAXE", "DIAMOND_SHOVEL", "NETHERITE_SHOVEL",
                            "DIAMOND_HELMET", "NETHERITE_HELMET", "DIAMOND_CHESTPLATE", "NETHERITE_CHESTPLATE",
                            "DIAMOND_LEGGINGS", "NETHERITE_LEGGINGS", "DIAMOND_BOOTS", "NETHERITE_BOOTS",
                            "SHIELD", "ELYTRA", "TOTEM_OF_UNDYING", "ENCHANTED_GOLDEN_APPLE", "POTION"
                    );
                }
            }
            case ENCHANTS -> {
                TakashaEnchantRegistry.initialize();
                List<String> list = new ArrayList<>(TakashaEnchantRegistry.getAllIds().stream()
                        .sorted()
                        .toList());
                list.addAll(List.of("sharpness", "smite", "bane_of_arthropods", "unbreaking", "mending", "fire_aspect", "looting", "efficiency", "fortune", "silk_touch", "protection", "feather_falling"));
                yield list;
            }
            case ABILITIES -> List.of("circular_slash", "fireball", "shadow_step", "meteor", "lightning_strike", "heal", "bleed", "ice_nova", "earthquake");
            case ELEMENTS -> List.of("5.0", "10.0", "15.0", "20.0", "25.0", "30.0", "50.0", "10 -> 25");
            case TIER -> List.of("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC");
            case GEM_SOCKETS -> List.of("RED", "BLUE", "YELLOW", "GREEN", "PRISMATIC", "ANY");
            case GEM_COLORS -> List.of("Red", "Blue", "Yellow", "Green", "Prismatic", "Nether", "Rainbow", "Purple", "White");
            case ITEM_TYPES -> List.of("SWORD", "DAGGER", "SPEAR", "HAMMER", "GAUNTLET", "WHIP", "STAFF", "BOW", "CROSSBOW", "MUSKET", "LUTE", "ARMOR", "TOOL", "ACCESSORY", "CATALYST", "SHIELD", "CONSUMABLE", "GEM_STONE");
            case UPGRADE -> List.of("5", "10", "15", "20", "10 2 0 0 5 30");
            case NUMERIC -> List.of("10.0", "15.0", "20.0", "25.0", "50.0", "10 -> 25", "10 2 0 0 5 30");
            case ID -> List.of("[id]", "[item_id]", "NEW_ITEM");
            case GENERIC, NONE -> Collections.emptyList();
        };
    }

    public static void closeSession(ServerPlayer player) {
        if (player != null) {
            ACTIVE_SESSIONS.remove(player.getUUID());
        }
    }

    public static boolean hasActiveSession(ServerPlayer player) {
        return player != null && ACTIVE_SESSIONS.containsKey(player.getUUID());
    }

    public static EditSession getSession(ServerPlayer player) {
        return player != null ? ACTIVE_SESSIONS.get(player.getUUID()) : null;
    }
}
