package net.takasha.rpg.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.takasha.rpg.menu.MMOItemEditionMenu;
import net.takasha.rpg.menu.MMOTypeBrowserMenu;
import net.takasha.rpg.mmo.MMOItemData;
import net.takasha.rpg.mmo.MMOItemManager;
import net.takasha.rpg.mmo.MMOItemTemplate;
import net.takasha.rpg.pack.TakashaPackManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MMOCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger("takasha-mmo-command");

    private static final SuggestionProvider<CommandSourceStack> MMO_TYPE_SUGGESTIONS = (context, builder) -> {
        List<String> types = new ArrayList<>(MMOItemManager.getAllItemTypes());
        return SharedSuggestionProvider.suggest(types, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> MMO_ITEM_SUGGESTIONS = (context, builder) -> {
        List<String> suggestions = new ArrayList<>();
        for (MMOItemTemplate template : MMOItemManager.getAllTemplates()) {
            String id = template.getId().toLowerCase(Locale.ROOT);
            if (!suggestions.contains(id)) {
                suggestions.add(id);
            }
        }
        return SharedSuggestionProvider.suggest(suggestions, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> FILTERED_ITEM_SUGGESTIONS = (context, builder) -> {
        try {
            String type = StringArgumentType.getString(context, "type").toUpperCase(Locale.ROOT);
            List<String> list = MMOItemManager.getTemplatesByType(type).stream()
                    .map(t -> t.getId().toLowerCase(Locale.ROOT))
                    .toList();
            if (list.isEmpty()) {
                list = List.of("[id]");
            }
            return SharedSuggestionProvider.suggest(list, builder);
        } catch (Exception e) {
            return MMO_ITEM_SUGGESTIONS.getSuggestions(context, builder);
        }
    };

    private static final SuggestionProvider<CommandSourceStack> MMO_INPUT_SUGGESTIONS = (context, builder) -> {
        if (context.getSource().isPlayer()) {
            List<String> dynamic = net.takasha.rpg.menu.MMOChatEditionEngine.getActiveSessionSuggestions(context.getSource().getPlayer());
            if (!dynamic.isEmpty()) {
                return SharedSuggestionProvider.suggest(dynamic, builder);
            }
        }
        return SharedSuggestionProvider.suggest(List.of("[value]", "cancel"), builder);
    };

    public static void initialize() {
        LOGGER.info("Registering Takasha RPG Item Commands (/mi, /mmo)...");
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher);
        });
    }

    public static boolean checkCanAdmin(CommandSourceStack src) {
        if (!src.isPlayer()) return true;
        ServerPlayer player = src.getPlayer();
        if (player == null) return false;
        return player.isCreative();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildMmoNode(String rootLiteral) {
        return Commands.literal(rootLiteral)
                .requires(MMOCommand::checkCanAdmin)
                .executes(ctx -> {
                    if (ctx.getSource().isPlayer()) {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        MMOTypeBrowserMenu.open(player);
                        return 1;
                    }
                    return executeHelp(ctx.getSource());
                })
                .then(Commands.literal("input")
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                .suggests(MMO_INPUT_SUGGESTIONS)
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String val = StringArgumentType.getString(ctx, "value");
                                    boolean ok = net.takasha.rpg.menu.MMOChatEditionEngine.processInput(player, val);
                                    if (!ok) {
                                        ctx.getSource().sendFailure(Component.literal("§c[Takasha-RPG] Kamu tidak sedang dalam sesi chat edition!"));
                                        return 0;
                                    }
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("help")
                        .executes(ctx -> executeHelp(ctx.getSource()))
                )
                .then(Commands.literal("browse")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            MMOTypeBrowserMenu.open(player);
                            return 1;
                        })
                )
                .then(Commands.literal("gui")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            MMOTypeBrowserMenu.open(player);
                            return 1;
                        })
                )
                .then(Commands.literal("list")
                        .executes(ctx -> executeList(ctx.getSource(), "ALL"))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests(MMO_TYPE_SUGGESTIONS)
                                .executes(ctx -> executeList(ctx.getSource(), StringArgumentType.getString(ctx, "type")))
                        )
                )
                .then(Commands.literal("edit")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests(MMO_TYPE_SUGGESTIONS)
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(FILTERED_ITEM_SUGGESTIONS)
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String type = StringArgumentType.getString(ctx, "type");
                                            String id = StringArgumentType.getString(ctx, "id");
                                            MMOItemEditionMenu.open(player, type, id);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("create")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests(MMO_TYPE_SUGGESTIONS)
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(List.of("[id]", "[item_id]", "NEW_ITEM"), b))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String type = StringArgumentType.getString(ctx, "type").toUpperCase(Locale.ROOT);
                                            String id = StringArgumentType.getString(ctx, "id").toUpperCase(Locale.ROOT);

                                            MMOItemTemplate existing = MMOItemManager.getTemplate(type, id);
                                            if (existing == null) {
                                                existing = new MMOItemTemplate(id, type);
                                                existing.setName("&f" + id.replace('_', ' '));
                                                MMOItemManager.saveAndRegisterTemplate(existing);
                                                player.sendSystemMessage(net.takasha.rpg.mmo.TakashaColorParser.parse("&a[Takasha-RPG] Berhasil membuat template item baru: &6" + type + ":" + id));
                                            }
                                            MMOItemEditionMenu.open(player, type, id);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("get")
                        .then(Commands.argument("item_id", StringArgumentType.greedyString())
                                .suggests(MMO_ITEM_SUGGESTIONS)
                                .executes(ctx -> {
                                    ServerPlayer self = ctx.getSource().getPlayerOrException();
                                    return executeGive(ctx.getSource(), self, StringArgumentType.getString(ctx, "item_id"), 1);
                                })
                        )
                )
                .then(Commands.literal("give")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests((ctx, b) -> {
                                    List<String> combined = new ArrayList<>();
                                    for (MMOItemTemplate t : MMOItemManager.getAllTemplates()) {
                                        String id = t.getId().toLowerCase(Locale.ROOT);
                                        if (!combined.contains(id)) combined.add(id);
                                    }
                                    if (ctx.getSource().getServer() != null) {
                                        for (ServerPlayer sp : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                                            combined.add(sp.getScoreboardName());
                                        }
                                    }
                                    return SharedSuggestionProvider.suggest(combined, b);
                                })
                                .executes(ctx -> {
                                    String arg = StringArgumentType.getString(ctx, "target");
                                    ServerPlayer self = ctx.getSource().getPlayerOrException();
                                    return executeGive(ctx.getSource(), self, arg, 1);
                                })
                                .then(Commands.argument("item_id", StringArgumentType.word())
                                        .suggests(MMO_ITEM_SUGGESTIONS)
                                        .executes(ctx -> {
                                            String targetName = StringArgumentType.getString(ctx, "target");
                                            String itemId = StringArgumentType.getString(ctx, "item_id");
                                            ServerPlayer target = ctx.getSource().getServer().getPlayerList().getPlayerByName(targetName);
                                            if (target == null) {
                                                ServerPlayer self = ctx.getSource().getPlayerOrException();
                                                return executeGive(ctx.getSource(), self, targetName + ":" + itemId, 1);
                                            }
                                            return executeGive(ctx.getSource(), target, itemId, 1);
                                        })
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> {
                                                    String targetName = StringArgumentType.getString(ctx, "target");
                                                    String itemId = StringArgumentType.getString(ctx, "item_id");
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                    ServerPlayer target = ctx.getSource().getServer().getPlayerList().getPlayerByName(targetName);
                                                    if (target == null) {
                                                        ServerPlayer self = ctx.getSource().getPlayerOrException();
                                                        return executeGive(ctx.getSource(), self, targetName + ":" + itemId, amount);
                                                    }
                                                    return executeGive(ctx.getSource(), target, itemId, amount);
                                                })
                                        )
                                )
                        )
                )
                .then(Commands.literal("copy")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests(MMO_TYPE_SUGGESTIONS)
                                .then(Commands.argument("old_id", StringArgumentType.word())
                                        .suggests(FILTERED_ITEM_SUGGESTIONS)
                                        .then(Commands.argument("new_id", StringArgumentType.word())
                                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(List.of("[new_id]", "[id_duplikat]"), b))
                                                .executes(ctx -> {
                                                    String type = StringArgumentType.getString(ctx, "type").toUpperCase(Locale.ROOT);
                                                    String oldId = StringArgumentType.getString(ctx, "old_id").toUpperCase(Locale.ROOT);
                                                    String newId = StringArgumentType.getString(ctx, "new_id").toUpperCase(Locale.ROOT);
                                                    MMOItemTemplate copied = MMOItemManager.copyTemplate(type, oldId, newId);
                                                    if (copied == null) {
                                                        ctx.getSource().sendFailure(Component.literal("§c[Takasha-RPG] Template asal '" + oldId + "' tidak ditemukan!"));
                                                        return 0;
                                                    }
                                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[Takasha-RPG] Berhasil menduplikasi " + oldId + " menjadi " + newId + "!"), true);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
                .then(Commands.literal("delete")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests(MMO_TYPE_SUGGESTIONS)
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(FILTERED_ITEM_SUGGESTIONS)
                                        .executes(ctx -> {
                                            String type = StringArgumentType.getString(ctx, "type").toUpperCase(Locale.ROOT);
                                            String id = StringArgumentType.getString(ctx, "id").toUpperCase(Locale.ROOT);
                                            boolean deleted = MMOItemManager.deleteTemplate(type, id);
                                            if (!deleted) {
                                                ctx.getSource().sendFailure(Component.literal("§c[Takasha-RPG] Template '" + id + "' tidak ditemukan!"));
                                                return 0;
                                            }
                                            ctx.getSource().sendSuccess(() -> Component.literal("§a[Takasha-RPG] Berhasil menghapus template " + id + " dari memori & konfigurasi!"), true);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("giveall")
                        .then(Commands.argument("item_id", StringArgumentType.word())
                                .suggests(MMO_ITEM_SUGGESTIONS)
                                .executes(ctx -> executeGiveAll(ctx.getSource(), StringArgumentType.getString(ctx, "item_id"), 1))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> executeGiveAll(ctx.getSource(), StringArgumentType.getString(ctx, "item_id"), IntegerArgumentType.getInteger(ctx, "amount")))
                                )
                        )
                )
                .then(Commands.literal("take")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("item_id", StringArgumentType.word())
                                        .suggests(MMO_ITEM_SUGGESTIONS)
                                        .executes(ctx -> executeTake(ctx.getSource(), StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "item_id"), 1))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> executeTake(ctx.getSource(), StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "item_id"), IntegerArgumentType.getInteger(ctx, "amount")))
                                        )
                                )
                        )
                )
                .then(Commands.literal("drop")
                        .then(Commands.argument("item_id", StringArgumentType.word())
                                .suggests(MMO_ITEM_SUGGESTIONS)
                                .executes(ctx -> executeDrop(ctx.getSource(), StringArgumentType.getString(ctx, "item_id"), 1))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> executeDrop(ctx.getSource(), StringArgumentType.getString(ctx, "item_id"), IntegerArgumentType.getInteger(ctx, "amount")))
                                )
                        )
                )
                .then(Commands.literal("revid")
                        .executes(ctx -> executeRevId(ctx.getSource(), "ALL"))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests(MMO_TYPE_SUGGESTIONS)
                                .executes(ctx -> executeRevId(ctx.getSource(), StringArgumentType.getString(ctx, "type")))
                        )
                )
                .then(Commands.literal("update")
                        .executes(ctx -> executeUpdateHeldItem(ctx.getSource()))
                )
                .then(Commands.literal("stations")
                        .then(Commands.literal("open")
                                .then(Commands.argument("station_id", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(List.of("arcane-forge", "default"), b))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String stationId = StringArgumentType.getString(ctx, "station_id");
                                            net.takasha.rpg.menu.CraftingStationMenu.open(player, stationId);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("reload")
                        .executes(ctx -> executeReload(ctx.getSource()))
                );
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Primary command: /mi ...
        dispatcher.register(buildMmoNode("mi"));

        // Alias command: /mmo ...
        dispatcher.register(buildMmoNode("mmo"));

        // Shortcut commands: /input <value> and /mmoinput <value>
        dispatcher.register(Commands.literal("input")
                .then(Commands.argument("value", StringArgumentType.greedyString())
                        .suggests(MMO_INPUT_SUGGESTIONS)
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String val = StringArgumentType.getString(ctx, "value");
                            boolean ok = net.takasha.rpg.menu.MMOChatEditionEngine.processInput(player, val);
                            if (!ok) {
                                ctx.getSource().sendFailure(Component.literal("§c[Takasha-RPG] Kamu tidak sedang dalam sesi chat edition!"));
                                return 0;
                            }
                            return 1;
                        })
                )
        );
        dispatcher.register(Commands.literal("mmoinput")
                .then(Commands.argument("value", StringArgumentType.greedyString())
                        .suggests(MMO_INPUT_SUGGESTIONS)
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String val = StringArgumentType.getString(ctx, "value");
                            boolean ok = net.takasha.rpg.menu.MMOChatEditionEngine.processInput(player, val);
                            if (!ok) {
                                ctx.getSource().sendFailure(Component.literal("§c[Takasha-RPG] Kamu tidak sedang dalam sesi chat edition!"));
                                return 0;
                            }
                            return 1;
                        })
                )
        );

        // Subcommand: /rpg mi ... & /rpg mmo ...
        dispatcher.register(Commands.literal("rpg")
                .then(buildMmoNode("mi"))
                .then(buildMmoNode("mmo"))
        );
    }

    private static int executeHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6§l✦ TAKASHA RPG ITEM SUITE ✦"), false);
        source.sendSuccess(() -> Component.literal(" §e/mi browse [type] §7- Membuka Type/Item Browser GUI 54-slot"), false);
        source.sendSuccess(() -> Component.literal(" §e/mi edit <type> <id> §7- Membuka Item Edition GUI dengan live preview slot 4"), false);
        source.sendSuccess(() -> Component.literal(" §e/mi create <type> <id> §7- Membuat item MMO baru & membuka editor"), false);
        source.sendSuccess(() -> Component.literal(" §e/mi copy <type> <old> <new> §7- Menduplikasi template item ke disk"), false);
        source.sendSuccess(() -> Component.literal(" §e/mi delete <type> <id> §7- Menghapus template item dari memori & disk"), false);
        source.sendSuccess(() -> Component.literal(" §e/mi get <item_id> §7- Mengambil item MMO langsung ke tangan"), false);
        source.sendSuccess(() -> Component.literal(" §e/mi give <player> <item_id> [amount] §7- Memberikan item ke pemain lain"), false);
        source.sendSuccess(() -> Component.literal(" §e/mi list [type] §7- Menampilkan daftar item terdaftar"), false);
        source.sendSuccess(() -> Component.literal(" §e/mi stations open <station_id> §7- Membuka Crafting Station GUI 2-tier"), false);
        source.sendSuccess(() -> Component.literal(" §e/mi revid [type] §7- Menaikkan revision ID template item (+1)"), false);
        source.sendSuccess(() -> Component.literal(" §e/mi update §7- Memperbarui item di tangan utama ke template terbaru"), false);
        source.sendSuccess(() -> Component.literal(" §e/mi reload §7- Memuat ulang seluruh pack RPG dan konfigurasi disk"), false);
        return 1;
    }

    private static int executeGive(CommandSourceStack source, ServerPlayer target, String itemId, int amount) {
        if (source.getEntity() instanceof ServerPlayer callerPlayer && !callerPlayer.isCreative()) {
            source.sendFailure(Component.literal("§c[Takasha-RPG] Perintah pembuatan & pengambilan item MMO hanya dapat digunakan dalam Mode Kreatif!"));
            return 0;
        }

        ItemStack stack = MMOItemManager.generateItemStack(itemId);
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("§c[Takasha-RPG] Item '" + itemId + "' tidak ditemukan! Ketik /mi browse atau /mi list."));
            return 0;
        }

        stack.setCount(Math.max(1, amount));
        boolean added = target.getInventory().add(stack);
        if (!added) {
            target.drop(stack, false);
        }

        source.sendSuccess(() -> Component.literal("§a[Takasha-RPG] §fBerhasil memberikan §e" + amount + "x §6" + stack.getHoverName().getString() + " §fke §e" + target.getScoreboardName()), true);
        target.sendSystemMessage(Component.literal("§a[Takasha-RPG] §fKamu telah menerima §6" + amount + "x " + stack.getHoverName().getString()));
        return 1;
    }

    private static int executeList(CommandSourceStack source, String type) {
        var templates = "ALL".equalsIgnoreCase(type)
                ? MMOItemManager.getAllTemplates()
                : MMOItemManager.getTemplatesByType(type);

        if (templates.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e[Takasha-RPG] §7Tidak ada item MMO terdaftar untuk tipe: " + type), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("§6§l✦ DAFTAR ITEM RPG (" + templates.size() + ") ✦"), false);
        for (MMOItemTemplate t : templates) {
            String giveCmd = "/mi get " + t.getId().toLowerCase(Locale.ROOT);
            Component line = Component.literal(" §7• §f" + t.getItemType().toUpperCase(Locale.ROOT) + " §8| §e" + t.getId() + " §7- " + t.getTier().getPrefixColor() + t.getTier().getDisplayName() + " ")
                    .append(Component.literal("§a§l[AMBIL]")
                            .withStyle(style -> style
                                    .withClickEvent(new ClickEvent.RunCommand(giveCmd))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("§eKlik untuk mengambil " + t.getId() + " (Mode Kreatif)")))));
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int executeReload(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§e[Takasha-RPG] §7Memuat ulang seluruh pack RPG, item tool, senjata, dan stat..."), true);
        try {
            TakashaPackManager.getInstance().reloadAll();
            source.sendSuccess(() -> Component.literal("§a[Takasha-RPG] §fReload selesai! Total item terdaftar: §e" + MMOItemManager.getAllTemplates().size()), true);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to reload RPG packs: ", e);
            source.sendFailure(Component.literal("§c[Takasha-RPG] Terjadi kegagalan saat reload: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeGiveAll(CommandSourceStack source, String itemId, int amount) {
        if (source.getEntity() instanceof ServerPlayer callerPlayer && !callerPlayer.isCreative()) {
            source.sendFailure(Component.literal("§c[Takasha-RPG] Perintah giveall hanya dapat digunakan dalam Mode Kreatif!"));
            return 0;
        }
        if (source.getServer() == null) return 0;
        int count = 0;
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            ItemStack stack = MMOItemManager.generateItemStack(itemId);
            if (!stack.isEmpty()) {
                stack.setCount(Math.max(1, amount));
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
                player.sendSystemMessage(Component.literal("§a[Takasha-RPG] §fKamu telah menerima §6" + amount + "x " + stack.getHoverName().getString()));
                count++;
            }
        }
        int finalCount = count;
        source.sendSuccess(() -> Component.literal("§a[Takasha-RPG] §fBerhasil membagikan §e" + amount + "x " + itemId + " §fke §6" + finalCount + " §fpemain online!"), true);
        return 1;
    }

    private static int executeTake(CommandSourceStack source, String playerName, String itemId, int amount) {
        if (source.getServer() == null) return 0;
        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (target == null) {
            source.sendFailure(Component.literal("§c[Takasha-RPG] Pemain '" + playerName + "' tidak ditemukan!"));
            return 0;
        }
        int remaining = Math.max(1, amount);
        var inv = target.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            var mmoOpt = MMOItemData.readFromStack(stack);
            if (mmoOpt.isPresent() && mmoOpt.get().getId().equalsIgnoreCase(itemId)) {
                int take = Math.min(stack.getCount(), remaining);
                stack.shrink(take);
                remaining -= take;
                if (remaining <= 0) break;
            }
        }
        int taken = amount - remaining;
        if (taken == 0) {
            source.sendFailure(Component.literal("§c[Takasha-RPG] Pemain " + playerName + " tidak memiliki item MMO '" + itemId + "'!"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("§a[Takasha-RPG] §fBerhasil mengambil §e" + taken + "x " + itemId + " §fdari §6" + playerName), true);
        target.sendSystemMessage(Component.literal("§c[Takasha-RPG] §e" + taken + "x " + itemId + " §ftelah diambil dari inventori Anda."));
        return 1;
    }

    private static int executeDrop(CommandSourceStack source, String itemId, int amount) {
        if (source.getEntity() instanceof ServerPlayer callerPlayer && !callerPlayer.isCreative()) {
            source.sendFailure(Component.literal("§c[Takasha-RPG] Perintah drop hanya dapat digunakan dalam Mode Kreatif!"));
            return 0;
        }
        ItemStack stack = MMOItemManager.generateItemStack(itemId);
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("§c[Takasha-RPG] Item '" + itemId + "' tidak ditemukan!"));
            return 0;
        }
        stack.setCount(Math.max(1, amount));
        var level = source.getLevel();
        var pos = source.getPosition();
        var itemEntity = new net.minecraft.world.entity.item.ItemEntity(level, pos.x, pos.y, pos.z, stack);
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
        source.sendSuccess(() -> Component.literal("§a[Takasha-RPG] §fBerhasil menjatuhkan §e" + amount + "x " + stack.getHoverName().getString() + " §fpada koordinat (" + (int)pos.x + ", " + (int)pos.y + ", " + (int)pos.z + ")"), true);
        return 1;
    }

    private static int executeRevId(CommandSourceStack source, String typeOrAll) {
        var templates = "ALL".equalsIgnoreCase(typeOrAll)
                ? MMOItemManager.getAllTemplates()
                : MMOItemManager.getTemplatesByType(typeOrAll);
        if (templates.isEmpty()) {
            source.sendFailure(Component.literal("§c[Takasha-RPG] Tidak ada item terdaftar untuk kategori: " + typeOrAll));
            return 0;
        }
        for (MMOItemTemplate t : templates) {
            t.setRevisionId(t.getRevisionId() + 1);
            net.takasha.rpg.mmo.MMOItemDiskManager.saveTemplate(t);
        }
        source.sendSuccess(() -> Component.literal("§a[Takasha-RPG] §fRevision ID dinaikkan (+1) untuk §e" + templates.size() + " §fitem MMO (" + typeOrAll + ")! Item pemain akan ter-update otomatis saat /mi update."), true);
        return 1;
    }

    private static int executeUpdateHeldItem(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[Takasha-RPG] Perintah update hanya dapat digunakan oleh pemain in-game!"));
            return 0;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            source.sendFailure(Component.literal("§c[Takasha-RPG] Pegang item MMO di tangan utama untuk mengupdate!"));
            return 0;
        }
        if (!MMOItemData.hasMMOData(held)) {
            source.sendFailure(Component.literal("§c[Takasha-RPG] Item yang Anda pegang bukan item RPG!"));
            return 0;
        }
        boolean updated = MMOItemManager.updateItem(held);
        if (updated) {
            source.sendSuccess(() -> Component.literal("§a[Takasha-RPG] §fItem di tangan utama berhasil diupdate ke revisi template terbaru!"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§c[Takasha-RPG] Gagal mengupdate item: Template asal tidak ditemukan!"));
            return 0;
        }
    }
}
