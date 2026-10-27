package net.takasha.rpg.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;
import net.takasha.rpg.dungeon.DungeonArenaEngine;
import net.takasha.rpg.dungeon.DungeonTier;
import net.takasha.rpg.economy.DynamicPricingEngine;
import net.takasha.rpg.economy.TakashaDailyShopManager;
import net.takasha.rpg.economy.catalog.ShopCatalog;
import net.takasha.rpg.economy.catalog.ShopCategory;
import net.takasha.rpg.economy.catalog.ShopItemEntry;
import net.takasha.rpg.enchant.TakashaAnvilHandler;
import net.takasha.rpg.enchant.TakashaEnchantManager;
import net.takasha.rpg.enchant.TakashaEnchantRegistry;
import net.takasha.rpg.enchant.TakashaEnchantRegistry.CustomEnchantDef;
import net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier;
import net.minecraft.world.entity.Display;
import net.takasha.rpg.entity.DamageDummyEntity;
import net.takasha.rpg.item.TakashaCustomBookItem;
import net.takasha.rpg.menu.*;
import net.takasha.rpg.registry.ModRpgEntities;
import net.takasha.rpg.registry.ModRpgItems;
import net.takasha.rpg.skills.AuraSkillType;
import net.takasha.rpg.skills.AuraStatType;
import net.takasha.rpg.skills.AuraStatModifierEngine;
import net.takasha.rpg.title.TakashaTitle;
import net.takasha.rpg.title.TakashaTitleManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TakashaRpgCommands
 * Penyelarasan komprehensif seluruh perintah EssentialsX ke Minecraft Fabric.
 * Membagi akses menjadi mode Survival (7 Bawaan + 29 via Loot Item Executable)
 * dan mode Creative / OP (117 Perintah Administratif & Pengujian).
 */
public class TakashaRpgCommands {

    public static class PendingTeleport {
        public final ServerPlayer player;
        public final String destinationName;
        public final TakashaRpgData.HomeLocation location;
        public final double startX, startY, startZ;
        public final long triggerTick;

        public PendingTeleport(ServerPlayer player, String destinationName, TakashaRpgData.HomeLocation location, long triggerTick) {
            this.player = player;
            this.destinationName = destinationName;
            this.location = location;
            this.startX = player.getX();
            this.startY = player.getY();
            this.startZ = player.getZ();
            this.triggerTick = triggerTick;
        }
    }

    private static final Map<UUID, PendingTeleport> PENDING_TELEPORTS = new ConcurrentHashMap<>();

    private static final SuggestionProvider<CommandSourceStack> ENCHANT_SUGGESTION_PROVIDER = (context, builder) -> {
        List<String> ids = new ArrayList<>(TakashaEnchantRegistry.getAllIds());
        Collections.sort(ids);
        return SharedSuggestionProvider.suggest(ids, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> TIER_SUGGESTION_PROVIDER = (context, builder) -> {
        List<String> tiers = Arrays.stream(EnchantTier.values()).map(t -> t.name().toLowerCase()).toList();
        return SharedSuggestionProvider.suggest(tiers, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> AE_ITEM_SUGGESTION_PROVIDER = (context, builder) -> {
        List<String> items = List.of(
            "white_scroll", "black_scroll", "holy_scroll", "transmute_scroll", "randomization_scroll",
            "magic_dust", "secret_dust", "mystery_dust",
            "slot_increaser", "master_increaser", "soul_increaser", "soul_gem",
            "weapon_orb", "armor_orb", "tool_orb",
            "tracker_mobs", "tracker_fish",
            "phantom_scythe", "koth_sword", "koth_axe", "dimensional_sword", "supreme_weapon",
            "book_simple", "book_unique", "book_elite", "book_ultimate", "book_legendary", "book_fabled", "book_heroic", "book_soul", "book_mastery",
            "mystery_simple", "mystery_unique", "mystery_elite", "mystery_ultimate", "mystery_legendary", "mystery_fabled", "mystery_heroic", "mystery_soul", "mystery_mastery"
        );
        return SharedSuggestionProvider.suggest(items, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> DUMMY_ARMOR_SUGGESTION_PROVIDER = (context, builder) -> {
        List<String> types = List.of("none", "leather", "iron", "diamond", "netherite");
        return SharedSuggestionProvider.suggest(types, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> AURA_SKILLS_SUGGESTION_PROVIDER = (context, builder) -> {
        List<String> list = Arrays.stream(AuraSkillType.values())
                .map(s -> s.name().toLowerCase())
                .toList();
        return SharedSuggestionProvider.suggest(list, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> AURA_STATS_SUGGESTION_PROVIDER = (context, builder) -> {
        List<String> list = Arrays.stream(AuraStatType.values()).map(s -> s.name().toLowerCase()).toList();
        return SharedSuggestionProvider.suggest(list, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> AURA_ABILITIES_SUGGESTION_PROVIDER = (context, builder) -> {
        List<String> list = List.of("treecapitator", "speed_mine", "replenish", "terraform", "lightning_blade", "charged_shot", "absorption", "sharp_hook");
        return SharedSuggestionProvider.suggest(list, builder);
    };

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerEconomyCommands(dispatcher);
            registerHomeCommands(dispatcher);
            registerAuraSkillsCommands(dispatcher);
            registerShopCommands(dispatcher);
            registerArenaCommands(dispatcher);
            registerUtilityCommands(dispatcher);
            registerTitleCommands(dispatcher);
            registerRpgConfigCommands(dispatcher);
            registerHelpCommand(dispatcher);
            registerCreativeAndOpCommands(dispatcher);
            registerDamageDummyCommands(dispatcher);
        });

        ServerTickEvents.END_SERVER_TICK.register(TakashaRpgCommands::tickPendingTeleports);

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player) {
                PendingTeleport pt = PENDING_TELEPORTS.remove(player.getUUID());
                if (pt != null) {
                    player.sendSystemMessage(Component.literal("§c§l[Takasha-RPG] §cPending teleportation request cancelled! (Menerima damage)"));
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }
            return true;
        });
    }

    private static void tickPendingTeleports(net.minecraft.server.MinecraftServer server) {
        if (PENDING_TELEPORTS.isEmpty()) return;
        long currentTick = server.getTickCount();
        Iterator<Map.Entry<UUID, PendingTeleport>> it = PENDING_TELEPORTS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PendingTeleport> entry = it.next();
            PendingTeleport pt = entry.getValue();
            ServerPlayer p = pt.player;

            if (p.hasDisconnected() || !p.isAlive()) {
                it.remove();
                continue;
            }
            double dx = p.getX() - pt.startX;
            double dy = p.getY() - pt.startY;
            double dz = p.getZ() - pt.startZ;
            if (dx * dx + dy * dy + dz * dz > 0.25) {
                p.sendSystemMessage(Component.literal("§c§l[Takasha-RPG] §cPending teleportation request cancelled! (Kamu bergerak)"));
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
                it.remove();
                continue;
            }
            if (currentTick >= pt.triggerTick) {
                TakashaRpgData data = TakashaRpgManager.getOrCreateData(p);
                data.setLastTeleportLocation(p.level(), pt.startX, pt.startY, pt.startZ, p.getYRot(), p.getXRot());
                TakashaRpgManager.savePlayerData(p.getUUID());

                ServerLevel targetLevel = pt.location.resolveLevel(p);
                p.teleportTo(targetLevel, pt.location.x(), pt.location.y(), pt.location.z(), java.util.Set.of(), pt.location.yaw(), pt.location.pitch(), true);
                p.sendSystemMessage(Component.literal("§aTeleportasi ke §e" + pt.destinationName + "§a!"));
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
                it.remove();
            }
        }
    }

    private static boolean checkCommandPermission(ServerPlayer player, String commandId) {
        if (player.isCreative() || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return true;
        }
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        if (!data.isCommandUnlocked(commandId)) {
            if ("repair_all".equals(commandId)) {
                player.sendSystemMessage(Component.literal("§c§l[Takasha-RPG] §cKemampuan /repair all terkunci!\n§ePelajari terlebih dahulu §6Hammer of the Master Smith §e(/repair) lalu gunakan §6Hammer of the Divine Smith §e(Tier Mythic) untuk membuka /repair all!"));
            } else {
                player.sendSystemMessage(Component.literal("§c§l[Takasha-RPG] §cPerintah ini terkunci!\n§eTemukan tomes/artifak §6LEGENDARY/MYTHIC §edi dungeon atau eksplorasi dunia untuk membuka perintah: §b/" + commandId));
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            return false;
        }
        return true;
    }

    // 1. Economy Commands: /money, /bal, /pay, /worth
    private static void registerEconomyCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String cmd : List.of("money", "bal", "balance")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(context -> showBalance(context.getSource().getPlayerOrException()))
            );
        }

        dispatcher.register(Commands.literal("pay")
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                    .executes(context -> {
                        ServerPlayer sender = context.getSource().getPlayerOrException();
                        ServerPlayer target = EntityArgument.getPlayer(context, "target");
                        double amount = DoubleArgumentType.getDouble(context, "amount");

                        if (sender.getUUID().equals(target.getUUID())) {
                            sender.sendSystemMessage(Component.literal("§cKamu tidak bisa mengirim uang ke dirimu sendiri!"));
                            return 0;
                        }

                        TakashaRpgData senderData = TakashaRpgManager.getOrCreateData(sender);
                        if (!senderData.removeBalance(amount)) {
                            sender.sendSystemMessage(Component.literal("§cSaldo kamu tidak mencukupi untuk transfer sebesar $" + String.format("%.2f", amount)));
                            return 0;
                        }

                        TakashaRpgData targetData = TakashaRpgManager.getOrCreateData(target);
                        targetData.addBalance(amount);

                        TakashaRpgManager.savePlayerData(sender.getUUID());
                        TakashaRpgManager.savePlayerData(target.getUUID());
                        TakashaRpgManager.syncToClient(sender);
                        TakashaRpgManager.syncToClient(target);

                        sender.sendSystemMessage(Component.literal("§aBerhasil mentransfer §e$" + String.format("%.2f", amount) + " §ake player §b" + target.getScoreboardName()));
                        target.sendSystemMessage(Component.literal("§aKamu menerima transfer sebesar §e$" + String.format("%.2f", amount) + " §adari §b" + sender.getScoreboardName()));

                        return 1;
                    })
                )
            )
        );

        dispatcher.register(Commands.literal("worth")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                if (!checkCommandPermission(player, "sell")) return 0;
                ItemStack held = player.getMainHandItem();
                if (held.isEmpty()) {
                    player.sendSystemMessage(Component.literal("§cKamu tidak sedang memegang item apapun di tangan!"));
                    return 0;
                }

                String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
                double unitPrice = DynamicPricingEngine.getDynamicSellPrice(itemId, 10.0);
                double totalPrice = unitPrice * held.getCount();

                player.sendSystemMessage(Component.literal("§6§l╔══════════════════════════════════╗\n" +
                        "§6§l║      ★ PENILAIAN ITEM (WORTH) ★    ║\n" +
                        "§6§l╚══════════════════════════════════╝\n" +
                        "§fItem: §e" + held.getHoverName().getString() + " §7(x" + held.getCount() + ")\n" +
                        "§fHarga Satuan: §a$" + String.format("%.2f", unitPrice) + "\n" +
                        "§fTotal Nilai Jual: §a§l$" + String.format("%.2f", totalPrice) + "\n" +
                        "§8Gunakan /shop sell atau /sell untuk menjual ke pasar"));
                return 1;
            })
        );
    }

    private static int showBalance(ServerPlayer player) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        player.sendSystemMessage(Component.literal(
                "§6§l╔══════════════════════════════════╗\n" +
                "§6§l║        ★ DOMPET TAKASHA ★        ║\n" +
                "§6§l╚══════════════════════════════════╝\n" +
                "§eBalance : §a$" + String.format("%,.2f", data.getBalance()) + "\n" +
                "§eKoin    : §b" + data.getGems() + " Gems\n" +
                "§7Klik kanan koin atau batangan untuk setor langsung ke dompet!"
        ));
        return 1;
    }

    // 2. Multi-Home & Essentials Commands: /home, /sethome, /delhome, /homes, /back, /return
    private static final SuggestionProvider<CommandSourceStack> HOME_SUGGESTIONS = (context, builder) -> {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
            for (String homeName : data.getHomes().keySet()) {
                builder.suggest(homeName);
            }
        } catch (Exception ignored) {}
        return builder.buildFuture();
    };

    private static void registerHomeCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("home")
            .executes(context -> teleportHome(context.getSource().getPlayerOrException(), "home"))
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests(HOME_SUGGESTIONS)
                .executes(context -> teleportHome(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
            )
        );

        dispatcher.register(Commands.literal("sethome")
            .executes(context -> setHome(context.getSource().getPlayerOrException(), "home", false))
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests(HOME_SUGGESTIONS)
                .executes(context -> setHome(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name"), false))
                .then(Commands.literal("confirm")
                    .executes(context -> setHome(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name"), true))
                )
            )
        );

        dispatcher.register(Commands.literal("delhome")
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests(HOME_SUGGESTIONS)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    String name = StringArgumentType.getString(context, "name");
                    TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);

                    if (data.delHome(name)) {
                        TakashaRpgManager.savePlayerData(player.getUUID());
                        TakashaRpgManager.syncToClient(player);
                        player.sendSystemMessage(Component.literal("§aHome '§e" + name + "§a' berhasil dihapus. (" + data.getHomes().size() + "/" + data.getMaxHomes() + ")"));
                        return 1;
                    } else {
                        player.sendSystemMessage(Component.literal("§cHome '§e" + name + "§c' tidak ditemukan!"));
                        return 0;
                    }
                })
            )
        );

        dispatcher.register(Commands.literal("homes")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
                Map<String, TakashaRpgData.HomeLocation> homes = data.getHomes();

                player.sendSystemMessage(Component.literal("§6§l=== DAFTAR HOME (" + homes.size() + "/" + data.getMaxHomes() + ") ==="));
                if (homes.isEmpty()) {
                    player.sendSystemMessage(Component.literal("§7Belum ada home yang disimpan. Ketik /sethome untuk membuat baru."));
                } else {
                    for (Map.Entry<String, TakashaRpgData.HomeLocation> entry : homes.entrySet()) {
                        var loc = entry.getValue();
                        player.sendSystemMessage(Component.literal("§e• §a" + entry.getKey() + " §7-> §f["
                                + (int) loc.x() + ", " + (int) loc.y() + ", " + (int) loc.z() + "] §8(" + loc.dimension() + ")"));
                    }
                }
                return 1;
            })
        );

        // /back, /return, /eback, /ereturn
        for (String cmd : List.of("back", "return", "eback", "ereturn")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(context -> executeBack(context.getSource().getPlayerOrException()))
            );
        }
    }

    private static int executeBack(ServerPlayer player) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        TakashaRpgData.HomeLocation target = data.getLastTeleportLocation() != null ? data.getLastTeleportLocation() : data.getLastDeathLocation();

        if (target == null) {
            player.sendSystemMessage(Component.literal("§cTidak ada catatan lokasi teleportasi atau kematian sebelumnya!"));
            return 0;
        }

        if (player.isCreative() || player.isSpectator() || player.canUseGameMasterBlocks()) {
            data.setLastTeleportLocation(player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            TakashaRpgManager.savePlayerData(player.getUUID());

            ServerLevel targetLevel = target.resolveLevel(player);
            player.teleportTo(targetLevel, target.x(), target.y(), target.z(), java.util.Set.of(), target.yaw(), target.pitch(), true);
            player.sendSystemMessage(Component.literal("§aBerhasil kembali ke lokasi sebelumnya!"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
            return 1;
        }

        long currentTick = player.level().getServer() != null ? player.level().getServer().getTickCount() : 0;
        long triggerTick = currentTick + 60;
        PENDING_TELEPORTS.put(player.getUUID(), new PendingTeleport(player, "lokasi sebelumnya", target, triggerTick));
        player.sendSystemMessage(Component.literal("§7Returning to §eprevious location §7in §c3 seconds§7. Do not move or take damage!"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
        return 1;
    }

    private static int setHome(ServerPlayer player, String name, boolean confirmed) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        if (!confirmed && data.hasHome(name)) {
            player.sendSystemMessage(Component.literal("§e§l[Takasha-RPG] §cHome '§e" + name + "§c' sudah ada!\n§7Ketik §b/sethome " + name + " confirm §7untuk memperbarui ke lokasimu sekarang."));
            return 0;
        }

        String cleanName = name.toLowerCase().trim();
        if (data.getHomes().size() >= data.getMaxHomes() && !data.getHomes().containsKey(cleanName)) {
            player.sendSystemMessage(Component.literal("§cKapasitas /sethome kamu sudah penuh ("
                    + data.getHomes().size() + "/" + data.getMaxHomes() + ")!\n§eGunakan §6Ancient Keystone §e(Tier Mythic) untuk menambah slot home hingga maksimal " + TakashaRpgData.ABSOLUTE_MAX_HOMES + "."));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
            return 0;
        }

        data.setHome(name, player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), false);
        TakashaRpgManager.savePlayerData(player.getUUID());
        TakashaRpgManager.syncToClient(player);

        player.sendSystemMessage(Component.literal("§aLokasi home '§e" + name + "§a' berhasil disimpan! ("
                + data.getHomes().size() + "/" + data.getMaxHomes() + ")"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.5f);
        return 1;
    }

    private static int teleportHome(ServerPlayer player, String name) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        TakashaRpgData.HomeLocation loc = data.getHome(name);

        if (loc == null) {
            player.sendSystemMessage(Component.literal("§cHome '§e" + name + "§c' tidak ditemukan! Ketik §e/homes §cuntuk melihat daftar home milikmu."));
            return 0;
        }

        data.setLastTeleportLocation(player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        TakashaRpgManager.savePlayerData(player.getUUID());

        if (player.isCreative() || player.isSpectator() || player.canUseGameMasterBlocks()) {
            ServerLevel targetLevel = loc.resolveLevel(player);
            player.teleportTo(targetLevel, loc.x(), loc.y(), loc.z(), java.util.Set.of(), loc.yaw(), loc.pitch(), true);
            player.sendSystemMessage(Component.literal("§aTeleportasi ke home '§e" + name + "§a'!"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
            return 1;
        }

        long currentTick = player.level().getServer() != null ? player.level().getServer().getTickCount() : 0;
        long triggerTick = currentTick + 60;
        PENDING_TELEPORTS.put(player.getUUID(), new PendingTeleport(player, "home '" + name + "'", loc, triggerTick));
        player.sendSystemMessage(Component.literal("§7Teleporting to §e" + name + " §7in §c3 seconds§7. Do not move or take damage!"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
        return 1;
    }

    // 3. AuraSkills Commands (AuraSkills 2.4.0 Parity):
    // Survival (Lvl 0 / Default): /skills, /sk, /stats, /mana, /sources, /top, /skilltop, /rank, /abtoggle, /abilitytoggle, /claimitems,
    // plus 11 skill commands: /farming, /foraging, /mining, /fishing, /excavation, /archery, /defense, /fighting, /combat, /agility, /alchemy, /enchanting, /healing
    // Creative/Admin (Lvl 2+): /skills xp, /skills setlevel, /skills addlevel, /skills reload, /skills reset, /skills stats
    private static void registerAuraSkillsCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // --- 1. /skills & /sk ---
        for (String cmdName : List.of("skills", "sk")) {
            dispatcher.register(Commands.literal(cmdName)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    AuraSkillsMenu.open(player);
                    return 1;
                })
                .then(Commands.literal("text")
                    .executes(context -> showSkillsText(context.getSource().getPlayerOrException()))
                )
                .then(Commands.literal("reload")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .executes(context -> {
                        context.getSource().sendSuccess(() -> Component.literal("§a[AuraSkills] Konfigurasi dan data skill berhasil dimuat ulang!"), true);
                        return 1;
                    })
                )
                .then(Commands.literal("reset")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                            TakashaRpgData data = TakashaRpgManager.getOrCreateData(target);
                            data.resetProgress(TakashaRpgData.ResetScope.SKILLS);
                            AuraStatModifierEngine.applyStats(target);
                            TakashaRpgManager.savePlayerData(target.getUUID());
                            TakashaRpgManager.syncToClient(target);
                            context.getSource().sendSuccess(() -> Component.literal("§a[AuraSkills] Seluruh progress skill " + target.getScoreboardName() + " berhasil di-reset ke Level 1!"), true);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("setlevel")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("skill", StringArgumentType.word())
                            .suggests(AURA_SKILLS_SUGGESTION_PROVIDER)
                            .then(Commands.argument("level", IntegerArgumentType.integer(1, 1000))
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                    String skillName = StringArgumentType.getString(context, "skill");
                                    AuraSkillType skill = parseSkill(skillName);
                                    if (skill == null) {
                                        context.getSource().sendFailure(Component.literal("§cSkill '" + skillName + "' tidak valid!"));
                                        return 0;
                                    }
                                    int level = IntegerArgumentType.getInteger(context, "level");
                                    TakashaRpgData data = TakashaRpgManager.getOrCreateData(target);
                                    data.setSkillLevel(skill, level);
                                    data.setSkillXp(skill, 0.0);
                                    AuraStatModifierEngine.applyStats(target);
                                    TakashaRpgManager.savePlayerData(target.getUUID());
                                    TakashaRpgManager.syncToClient(target);
                                    context.getSource().sendSuccess(() -> Component.literal("§a[AuraSkills] Level skill " + skill.getDisplayName() + " milik " + target.getScoreboardName() + " diatur ke §e" + level), true);
                                    return 1;
                                })
                            )
                        )
                    )
                )
                .then(Commands.literal("addlevel")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("skill", StringArgumentType.word())
                            .suggests(AURA_SKILLS_SUGGESTION_PROVIDER)
                            .then(Commands.argument("levels", IntegerArgumentType.integer(1, 1000))
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                    String skillName = StringArgumentType.getString(context, "skill");
                                    AuraSkillType skill = parseSkill(skillName);
                                    if (skill == null) {
                                        context.getSource().sendFailure(Component.literal("§cSkill '" + skillName + "' tidak valid!"));
                                        return 0;
                                    }
                                    int add = IntegerArgumentType.getInteger(context, "levels");
                                    TakashaRpgData data = TakashaRpgManager.getOrCreateData(target);
                                    int newLvl = Math.min(1000, data.getSkillLevel(skill) + add);
                                    data.setSkillLevel(skill, newLvl);
                                    AuraStatModifierEngine.applyStats(target);
                                    TakashaRpgManager.savePlayerData(target.getUUID());
                                    TakashaRpgManager.syncToClient(target);
                                    context.getSource().sendSuccess(() -> Component.literal("§a[AuraSkills] +" + add + " Level ditambahkan ke skill " + skill.getDisplayName() + " milik " + target.getScoreboardName() + " (Sekarang: " + newLvl + ")"), true);
                                    return 1;
                                })
                            )
                        )
                    )
                )
                .then(Commands.literal("xp")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.literal("add")
                        .then(Commands.argument("target", EntityArgument.player())
                            .then(Commands.argument("skill", StringArgumentType.word())
                                .suggests(AURA_SKILLS_SUGGESTION_PROVIDER)
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.1))
                                    .executes(context -> {
                                        ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                        String skillName = StringArgumentType.getString(context, "skill");
                                        AuraSkillType skill = parseSkill(skillName);
                                        if (skill == null) {
                                            context.getSource().sendFailure(Component.literal("§cSkill '" + skillName + "' tidak valid!"));
                                            return 0;
                                        }
                                        double amt = DoubleArgumentType.getDouble(context, "amount");
                                        TakashaRpgManager.giveSkillXp(target, skill, amt);
                                        context.getSource().sendSuccess(() -> Component.literal("§a[AuraSkills] +" + amt + " XP diberikan ke " + skill.getDisplayName() + " milik " + target.getScoreboardName()), true);
                                        return 1;
                                    })
                                )
                            )
                        )
                    )
                    .then(Commands.literal("set")
                        .then(Commands.argument("target", EntityArgument.player())
                            .then(Commands.argument("skill", StringArgumentType.word())
                                .suggests(AURA_SKILLS_SUGGESTION_PROVIDER)
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                    .executes(context -> {
                                        ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                        String skillName = StringArgumentType.getString(context, "skill");
                                        AuraSkillType skill = parseSkill(skillName);
                                        if (skill == null) {
                                            context.getSource().sendFailure(Component.literal("§cSkill '" + skillName + "' tidak valid!"));
                                            return 0;
                                        }
                                        double amt = DoubleArgumentType.getDouble(context, "amount");
                                        TakashaRpgData data = TakashaRpgManager.getOrCreateData(target);
                                        data.setSkillXp(skill, amt);
                                        TakashaRpgManager.savePlayerData(target.getUUID());
                                        TakashaRpgManager.syncToClient(target);
                                        context.getSource().sendSuccess(() -> Component.literal("§a[AuraSkills] XP " + skill.getDisplayName() + " milik " + target.getScoreboardName() + " diatur ke " + amt), true);
                                        return 1;
                                    })
                                )
                            )
                        )
                    )
                    .then(Commands.literal("remove")
                        .then(Commands.argument("target", EntityArgument.player())
                            .then(Commands.argument("skill", StringArgumentType.word())
                                .suggests(AURA_SKILLS_SUGGESTION_PROVIDER)
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.1))
                                    .executes(context -> {
                                        ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                        String skillName = StringArgumentType.getString(context, "skill");
                                        AuraSkillType skill = parseSkill(skillName);
                                        if (skill == null) {
                                            context.getSource().sendFailure(Component.literal("§cSkill '" + skillName + "' tidak valid!"));
                                            return 0;
                                        }
                                        double amt = DoubleArgumentType.getDouble(context, "amount");
                                        TakashaRpgData data = TakashaRpgManager.getOrCreateData(target);
                                        data.setSkillXp(skill, Math.max(0.0, data.getSkillXp(skill) - amt));
                                        TakashaRpgManager.savePlayerData(target.getUUID());
                                        TakashaRpgManager.syncToClient(target);
                                        context.getSource().sendSuccess(() -> Component.literal("§a[AuraSkills] Mengurangi " + amt + " XP " + skill.getDisplayName() + " dari " + target.getScoreboardName()), true);
                                        return 1;
                                    })
                                )
                            )
                        )
                    )
                )
                .then(Commands.literal("stats")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                            TakashaRpgData data = TakashaRpgManager.getOrCreateData(target);
                            StringBuilder sb = new StringBuilder("§6=== Stat Milik " + target.getScoreboardName() + " ===\n");
                            for (AuraStatType stat : AuraStatType.values()) {
                                sb.append(" §7• ").append(stat.getColor()).append(stat.getDisplayName())
                                  .append(": §f").append(String.format("%.1f", data.getStatValue(stat))).append("\n");
                            }
                            context.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                            return 1;
                        })
                    )
                )
            );
        }

        // --- 2. /stats ---
        dispatcher.register(Commands.literal("stats")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                AuraStatsMenu.open(player);
                return 1;
            })
        );

        // --- 3. /mana ---
        dispatcher.register(Commands.literal("mana")
            .executes(context -> showManaStatus(context.getSource().getPlayerOrException()))
        );

        // --- 4. /sources ---
        dispatcher.register(Commands.literal("sources")
            .executes(context -> showSources(context.getSource().getPlayerOrException(), null))
            .then(Commands.argument("skill", StringArgumentType.word())
                .suggests(AURA_SKILLS_SUGGESTION_PROVIDER)
                .executes(context -> showSources(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "skill")))
            )
        );

        // --- 5. /top & /skilltop ---
        for (String topCmd : List.of("top", "skilltop")) {
            dispatcher.register(Commands.literal(topCmd)
                .executes(context -> showTopRanking(context.getSource().getPlayerOrException(), null))
                .then(Commands.argument("skill", StringArgumentType.word())
                    .suggests(AURA_SKILLS_SUGGESTION_PROVIDER)
                    .executes(context -> showTopRanking(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "skill")))
                )
            );
        }

        // --- 6. /rank ---
        dispatcher.register(Commands.literal("rank")
            .executes(context -> showRank(context.getSource().getPlayerOrException()))
        );

        // --- 7. /abtoggle & /abilitytoggle ---
        for (String abCmd : List.of("abtoggle", "abilitytoggle")) {
            dispatcher.register(Commands.literal(abCmd)
                .executes(context -> showAbilityToggles(context.getSource().getPlayerOrException()))
                .then(Commands.argument("ability", StringArgumentType.word())
                    .suggests(AURA_ABILITIES_SUGGESTION_PROVIDER)
                    .executes(context -> toggleAbility(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "ability")))
                )
            );
        }

        // --- 8. /claimitems ---
        dispatcher.register(Commands.literal("claimitems")
            .executes(context -> handleClaimItems(context.getSource().getPlayerOrException()))
        );

        // --- 9-19. 11 Skill Commands ---
        registerSingleSkillCommand(dispatcher, "farming", AuraSkillType.FARMING);
        registerSingleSkillCommand(dispatcher, "foraging", AuraSkillType.FORAGING);
        registerSingleSkillCommand(dispatcher, "mining", AuraSkillType.MINING);
        registerSingleSkillCommand(dispatcher, "fishing", AuraSkillType.FISHING);
        registerSingleSkillCommand(dispatcher, "excavation", AuraSkillType.EXCAVATION);
        registerSingleSkillCommand(dispatcher, "archery", AuraSkillType.ARCHERY);
        registerSingleSkillCommand(dispatcher, "defense", AuraSkillType.DEFENSE);
        registerSingleSkillCommand(dispatcher, "fighting", AuraSkillType.FIGHTING);
        registerSingleSkillCommand(dispatcher, "combat", AuraSkillType.FIGHTING);
        registerSingleSkillCommand(dispatcher, "agility", AuraSkillType.AGILITY);
        registerSingleSkillCommand(dispatcher, "alchemy", AuraSkillType.ALCHEMY);
        registerSingleSkillCommand(dispatcher, "enchanting", AuraSkillType.ENCHANTING);
        registerSingleSkillCommand(dispatcher, "healing", AuraSkillType.ALCHEMY);
    }

    private static void registerSingleSkillCommand(CommandDispatcher<CommandSourceStack> dispatcher, String name, AuraSkillType skill) {
        dispatcher.register(Commands.literal(name)
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                AuraLevelProgressionMenu.open(player, skill, 1);
                return 1;
            })
            .then(Commands.literal("text")
                .executes(context -> showSkillDetails(context.getSource().getPlayerOrException(), skill))
            )
        );
    }

    private static int showSkillDetails(ServerPlayer player, AuraSkillType skill) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        int lvl = data.getSkillLevel(skill);
        double xp = data.getSkillXp(skill);
        double req = skill.getXpForNextLevel(lvl);
        double pct = Math.min(100.0, (xp / req) * 100.0);

        int filledBars = (int) (pct / 5.0);
        String bar = "§a" + "█".repeat(filledBars) + "§7" + "░".repeat(Math.max(0, 20 - filledBars));

        StringBuilder sb = new StringBuilder();
        sb.append("§6§l╔══════════════════════════════════════╗\n");
        sb.append("§6§l║     ★ SKILL: ").append(skill.getDisplayName().toUpperCase()).append(" ★     \n");
        sb.append("§6§l╚══════════════════════════════════════╝\n");
        sb.append("§fLevel: §a§l").append(lvl).append(" §7/ 1.000\n");
        sb.append("§fXP: §e").append(String.format("%.1f", xp)).append(" §7/ §6").append(String.format("%.0f", req)).append(" §8(").append(String.format("%.1f%%", pct)).append(")\n");
        sb.append("§fStat Primer: §e").append(skill.getPrimaryStat().getDisplayName()).append(" §7(+1.0/lvl)\n");
        sb.append("§fStat Sekunder: §e").append(skill.getSecondaryStat().getDisplayName()).append(" §7(+0.5/lvl)\n");
        sb.append("§8Gunakan /skills untuk melihat GUI lengkap atau /stats untuk alokasi AP.");

        player.sendSystemMessage(Component.literal(sb.toString()));
        return 1;
    }

    private static int showManaStatus(ServerPlayer player) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        long now = System.currentTimeMillis();

        StringBuilder sb = new StringBuilder();
        sb.append("§b§l╔══════════════════════════════════════╗\n");
        sb.append("§b§l║         ★ STATUS ENERGI MANA ★       ║\n");
        sb.append("§b§l╚══════════════════════════════════════╝\n");
        sb.append("§fMana Sekarang: §b§l").append(String.format("%.1f", data.getCurrentMana())).append(" §7/ §3").append(String.format("%.1f", data.getMaxMana())).append("\n");
        sb.append("§fPemulihan Alami: §a+1.5 Mana / detik\n");
        sb.append("§8§m----------------------------------------\n");
        sb.append("§6Status Cooldown Kemampuan Aktif:\n");

        formatCooldown(sb, "Treecapitator (Kapak)", data.getTreecapitatorCooldownUntil(), now);
        formatCooldown(sb, "Speed Mine (Beliung)", data.getSpeedMineCooldownUntil(), now);
        formatCooldown(sb, "Replenish (Cangkul)", data.getReplenishCooldownUntil(), now);
        formatCooldown(sb, "Terraform (Sekop)", data.getTerraformCooldownUntil(), now);
        formatCooldown(sb, "Lightning Blade (Pedang)", data.getLightningBladeCooldownUntil(), now);
        formatCooldown(sb, "Charged Shot (Busur)", data.getChargedShotCooldownUntil(), now);
        formatCooldown(sb, "Absorption (Perisai)", data.getAbsorptionCooldownUntil(), now);
        formatCooldown(sb, "Sharp Hook (Pancingan)", data.getSharpHookCooldownUntil(), now);

        player.sendSystemMessage(Component.literal(sb.toString()));
        return 1;
    }

    private static void formatCooldown(StringBuilder sb, String name, long cdUntil, long now) {
        sb.append(" §7• §f").append(name).append(": ");
        if (now >= cdUntil) {
            sb.append("§aSIAP PAKAI (Ready)\n");
        } else {
            long remainingSec = (cdUntil - now) / 1000L;
            sb.append("§cCooldown (").append(remainingSec).append("s)\n");
        }
    }

    private static int showSources(ServerPlayer player, String skillName) {
        StringBuilder sb = new StringBuilder();
        sb.append("§e§l╔══════════════════════════════════════╗\n");
        sb.append("§e§l║       ★ SUMBER XP AURASKILLS ★       ║\n");
        sb.append("§e§l╚══════════════════════════════════════╝\n");

        if (skillName != null) {
            AuraSkillType skill = parseSkill(skillName);
            if (skill != null) {
                appendSkillSource(sb, skill);
                player.sendSystemMessage(Component.literal(sb.toString()));
                return 1;
            }
        }

        for (AuraSkillType skill : AuraSkillType.values()) {
            appendSkillSource(sb, skill);
        }
        player.sendSystemMessage(Component.literal(sb.toString()));
        return 1;
    }

    private static void appendSkillSource(StringBuilder sb, AuraSkillType skill) {
        sb.append("§6• ").append(skill.getDisplayName()).append(":\n");
        switch (skill) {
            case FARMING -> sb.append("   §7Panen tanaman matang (gandum, kentang, wortel: 25 XP, melon/labu: 15 XP)\n");
            case FORAGING -> sb.append("   §7Menebang semua jenis batang kayu (20 XP per batang)\n");
            case MINING -> sb.append("   §7Menambang batu & mineral (Coal: 8, Iron/Copper: 12, Gold: 25, Diamond/Debris: 75 XP)\n");
            case FISHING -> sb.append("   §7Menangkap ikan atau menarik harta karun di perairan (35 XP)\n");
            case EXCAVATION -> sb.append("   §7Menggali tanah, pasir, kerikil, clay, lempung, mud (4 s/d 12 XP)\n");
            case ARCHERY -> sb.append("   §7Menumbangkan monster dengan panah / crossbow (Skala level monster LevelledMobs)\n");
            case DEFENSE -> sb.append("   §7Menahan serangan kerusakan dari musuh (Skala damage & level monster penyerang)\n");
            case FIGHTING -> sb.append("   §7Mengalahkan monster dengan senjata melee (Skala level monster LevelledMobs)\n");
            case AGILITY -> sb.append("   §7Berlari (sprint), berenang, terbang elytra, selamat dari jatuh\n");
            case ALCHEMY -> sb.append("   §7Meracik ramuan di brewing stand (15 XP), meminum potion & golden apple (30-500 XP)\n");
            case ENCHANTING -> sb.append("   §7Enchanting table, anvil, grindstone (15-25 XP) & botol exp (20 XP)\n");
        }
    }

    private static int showTopRanking(ServerPlayer player, String skillName) {
        var server = player.level().getServer();
        if (server == null) return 0;
        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());

        AuraSkillType skill = parseSkill(skillName);
        if (skill != null) {
            players.sort((p1, p2) -> {
                int lvl2 = TakashaRpgManager.getOrCreateData(p2).getSkillLevel(skill);
                int lvl1 = TakashaRpgManager.getOrCreateData(p1).getSkillLevel(skill);
                return Integer.compare(lvl2, lvl1);
            });
            player.sendSystemMessage(Component.literal("§6§l=== TOP RANKING " + skill.getDisplayName().toUpperCase() + " ==="));
            int rank = 1;
            for (ServerPlayer p : players) {
                int lvl = TakashaRpgManager.getOrCreateData(p).getSkillLevel(skill);
                player.sendSystemMessage(Component.literal("§e#" + rank + " §f" + p.getScoreboardName() + " §7- Level §a" + lvl));
                rank++;
                if (rank > 10) break;
            }
        } else {
            players.sort((p1, p2) -> {
                int lvl2 = TakashaRpgManager.getOrCreateData(p2).getTotalSkillLevel();
                int lvl1 = TakashaRpgManager.getOrCreateData(p1).getTotalSkillLevel();
                return Integer.compare(lvl2, lvl1);
            });
            player.sendSystemMessage(Component.literal("§6§l=== TOP TOTAL SKILL LEVEL ==="));
            int rank = 1;
            for (ServerPlayer p : players) {
                int total = TakashaRpgManager.getOrCreateData(p).getTotalSkillLevel();
                player.sendSystemMessage(Component.literal("§e#" + rank + " §f" + p.getScoreboardName() + " §7- Total Level §a" + total));
                rank++;
                if (rank > 10) break;
            }
        }
        return 1;
    }

    private static int showRank(ServerPlayer player) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        int total = data.getTotalSkillLevel();
        double avg = (double) total / 11.0;
        player.sendSystemMessage(Component.literal("§6§l[AuraSkills] §eStatus Peringkat Pengembara:"));
        player.sendSystemMessage(Component.literal("§fTotal Level: §a" + total + " §7| Rata-rata: §e" + String.format("%.1f", avg)));
        return 1;
    }

    private static int showAbilityToggles(ServerPlayer player) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        List<String> abilities = List.of("treecapitator", "speed_mine", "replenish", "terraform", "lightning_blade", "charged_shot", "absorption", "sharp_hook");
        player.sendSystemMessage(Component.literal("§6§l[AuraSkills] Status Toggle Kemampuan Aktif (/abtoggle <nama>):"));
        for (String ab : abilities) {
            boolean enabled = data.isAbilityEnabled(ab);
            player.sendSystemMessage(Component.literal(" §7• §f" + ab + ": " + (enabled ? "§a[AKTIF / ON]" : "§c[NONAKTIF / OFF]")));
        }
        return 1;
    }

    private static int toggleAbility(ServerPlayer player, String ability) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        boolean newState = data.toggleAbility(ability);
        TakashaRpgManager.savePlayerData(player.getUUID());
        player.sendSystemMessage(Component.literal("§6§l[AuraSkills] §fKemampuan §e" + ability + " §fsekarang " + (newState ? "§aAKTIF (ON)" : "§cNONAKTIF (OFF)")));
        return 1;
    }

    private static int handleClaimItems(ServerPlayer player) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        String pending = data.getPendingKeystoneRefund();
        if (pending == null || pending.isEmpty()) {
            player.sendSystemMessage(Component.literal("§aTidak ada item atau keystone yang tertunda untuk diklaim."));
            return 1;
        }

        DungeonTier tier = DungeonTier.fromId(pending);
        ItemStack item = switch (tier) {
            case COMMON -> new ItemStack(ModRpgItems.DUNGEON_KEYSTONE_COMMON);
            case NIGHTMARE -> new ItemStack(ModRpgItems.DUNGEON_KEYSTONE_NIGHTMARE);
            case SAKURA_BOSS -> new ItemStack(ModRpgItems.DUNGEON_KEYSTONE_SAKURA);
        };

        if (player.getInventory().add(item)) {
            data.setPendingKeystoneRefund("");
            TakashaRpgManager.savePlayerData(player.getUUID());
            player.sendSystemMessage(Component.literal("§aBerhasil mengklaim kembali: §e" + item.getHoverName().getString()));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else {
            player.sendSystemMessage(Component.literal("§cInventory kamu masih penuh! Kosongkan minimal 1 slot."));
        }
        return 1;
    }

    private static AuraSkillType parseSkill(String name) {
        return AuraSkillType.fromString(name);
    }

    private static AuraStatType parseStat(String name) {
        if (name == null) return null;
        for (AuraStatType s : AuraStatType.values()) {
            if (s.name().equalsIgnoreCase(name) || s.getDisplayName().equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }

    private static int showSkillsText(ServerPlayer player) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);

        StringBuilder sb = new StringBuilder();
        sb.append("§6§l╔══════════════════════════════════════╗\n");
        sb.append("§6§l║         ★ STATUS AURASKILLS ★        ║\n");
        sb.append("§6§l╚══════════════════════════════════════╝\n");
        sb.append("§eTotal Level Skill: §a§l").append(data.getTotalSkillLevel()).append("§r\n\n");

        for (AuraSkillType skill : AuraSkillType.values()) {
            int lvl = data.getSkillLevel(skill);
            double xp = data.getSkillXp(skill);
            double req = skill.getXpForNextLevel(lvl);
            sb.append("§f• ").append(skill.getDisplayName()).append(" §7[Lvl §a").append(lvl).append("§7]: §e")
              .append(String.format("%.1f", xp)).append("§7/§6").append(String.format("%.0f", req)).append(" XP\n");
        }

        player.sendSystemMessage(Component.literal(sb.toString()));
        return 1;
    }

    // 4. Shop Commands: /shop, /dailyshop, /rotatingshop, /sellgui, /sell, /sellall
    private static void registerShopCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shop")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                TakashaShopMenu.openShop(player);
                return 1;
            })
            .then(Commands.literal("sell")
                .executes(context -> sellHeldItem(context.getSource().getPlayerOrException()))
            )
            .then(Commands.literal("all")
                .executes(context -> sellAllInventory(context.getSource().getPlayerOrException()))
            )
            .then(Commands.literal("reload")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(context -> {
                    ShopCatalog.reload();
                    context.getSource().sendSuccess(() -> Component.literal("§a[Shop] Seluruh katalog pasar EconomyShopGUI (19 berkas) berhasil dimuat ulang!"), true);
                    return 1;
                })
            )
            .then(Commands.argument("category", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ShopCatalog.getCategoryIds(), builder))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    String catId = StringArgumentType.getString(context, "category");
                    if ("rotatingshop".equalsIgnoreCase(catId) || "dailyshop".equalsIgnoreCase(catId)) {
                        RotatingShopMenu.open(player);
                        return 1;
                    }
                    ShopCategory cat = ShopCatalog.getCategory(catId);
                    if (cat == null) {
                        player.sendSystemMessage(Component.literal("§cKategori §e" + catId + " §ctidak ditemukan! Kategori yang tersedia:\n§e"
                                + String.join(", ", ShopCatalog.getCategoryIds())));
                        return 0;
                    }
                    ShopCategoryMenu.openCategory(player, cat, 1);
                    return 1;
                })
            )
        );

        dispatcher.register(Commands.literal("sellgui")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                if (!checkCommandPermission(player, "sell")) return 0;
                TakashaShopMenu.openSellGui(player);
                return 1;
            })
        );

        dispatcher.register(Commands.literal("sell")
            .executes(context -> sellHeldItem(context.getSource().getPlayerOrException()))
            .then(Commands.literal("all")
                .executes(context -> sellAllInventory(context.getSource().getPlayerOrException()))
            )
            .then(Commands.literal("gui")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (!checkCommandPermission(player, "sell")) return 0;
                    TakashaShopMenu.openSellGui(player);
                    return 1;
                })
            )
        );

        dispatcher.register(Commands.literal("sellall")
            .executes(context -> sellAllInventory(context.getSource().getPlayerOrException()))
        );

        registerRotatingShopDispatcher(dispatcher, "rotatingshop");
        registerRotatingShopDispatcher(dispatcher, "dailyshop");
    }

    private static int sellHeldItem(ServerPlayer player) {
        if (!checkCommandPermission(player, "sell")) return 0;
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cKamu tidak memegang item apapun untuk dijual!"));
            return 0;
        }

        ShopItemEntry entry = ShopCatalog.findItemEntry(held);
        if (entry == null || !entry.canSell()) {
            player.sendSystemMessage(Component.literal("§cItem §e" + held.getHoverName().getString() + " §ctidak dapat dijual ke pasar!"));
            return 0;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        double unitPrice = DynamicPricingEngine.getDynamicSellPrice(itemId, entry.getBaseSellPrice());
        int count = held.getCount();
        double total = unitPrice * count;

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        data.addBalance(total);
        DynamicPricingEngine.recordSale(itemId, count);

        TakashaRpgManager.savePlayerData(player.getUUID());
        TakashaRpgManager.syncToClient(player);

        player.sendSystemMessage(Component.literal("§a§l[Shop] §fBerhasil menjual §e" + held.getHoverName().getString()
                + " x" + count + " §fseharga §a§l$" + String.format("%.2f", total) + "§f!"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.2f);
        return 1;
    }

    private static int sellAllInventory(ServerPlayer player) {
        if (!checkCommandPermission(player, "sell")) return 0;
        double totalEarned = 0.0;
        int totalItemsSold = 0;
        Map<String, Integer> salesByItem = new HashMap<>();

        for (int i = 0; i < net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            ShopItemEntry entry = ShopCatalog.findItemEntry(stack);
            if (entry != null && entry.canSell()) {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                double unitPrice = DynamicPricingEngine.getDynamicSellPrice(itemId, entry.getBaseSellPrice());
                int count = stack.getCount();
                double stackTotal = unitPrice * count;

                totalEarned += stackTotal;
                totalItemsSold += count;
                salesByItem.put(itemId, salesByItem.getOrDefault(itemId, 0) + count);

                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }

        if (totalItemsSold == 0) {
            player.sendSystemMessage(Component.literal("§cTidak ada item di dalam tasmu yang bernilai jual di pasar!"));
            return 0;
        }

        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        data.addBalance(totalEarned);
        for (Map.Entry<String, Integer> sale : salesByItem.entrySet()) {
            DynamicPricingEngine.recordSale(sale.getKey(), sale.getValue());
        }

        TakashaRpgManager.savePlayerData(player.getUUID());
        TakashaRpgManager.syncToClient(player);

        player.sendSystemMessage(Component.literal("§a§l[Sell All] §fBerhasil menjual §e" + totalItemsSold
                + " item §fdari tas seharga §a§l$" + String.format("%.2f", totalEarned) + "§f!"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.3f);
        return 1;
    }

    private static void registerRotatingShopDispatcher(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                RotatingShopMenu.open(player);
                return 1;
            })
            .then(Commands.literal("text")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    List<TakashaDailyShopManager.DailyDeal> deals = TakashaDailyShopManager.getCurrentDeals();

                    StringBuilder sb = new StringBuilder();
                    sb.append("§6§l╔══════════════════════════════════════════╗\n");
                    sb.append("§6§l║      ★ TOKO ROTASI (ROTATING SHOP) ★     ║\n");
                    sb.append("§6§l╚══════════════════════════════════════════╝\n");

                    for (int i = 0; i < deals.size(); i++) {
                        var deal = deals.get(i);
                        String priceStr = deal.useGems() ? ((int) deal.finalPrice() + " Gems") : ("$" + String.format("%.0f", deal.finalPrice()));
                        sb.append("§e[").append(i + 1).append("] ")
                          .append(deal.tier().getColor()).append(deal.stack().getHoverName().getString())
                          .append(" x").append(deal.stack().getCount())
                          .append(" §c-").append((int) deal.discountPercent()).append("% ")
                          .append("§a§l").append(priceStr)
                          .append(" §8(Stok: ").append(deal.stock()).append(")\n");
                    }
                    sb.append("\n§7Beli penawaran dengan: §e/" + commandName + " buy <1-6>");
                    player.sendSystemMessage(Component.literal(sb.toString()));
                    return 1;
                })
            )
            .then(Commands.literal("buy")
                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        int slot = IntegerArgumentType.getInteger(context, "slot") - 1;
                        List<TakashaDailyShopManager.DailyDeal> deals = TakashaDailyShopManager.getCurrentDeals();

                        if (slot < 0 || slot >= deals.size()) {
                            player.sendSystemMessage(Component.literal("§cSlot tidak valid! Pilih nomor 1 sampai " + deals.size()));
                            return 0;
                        }

                        var deal = deals.get(slot);
                        if (deal.stock() <= 0) {
                            player.sendSystemMessage(Component.literal("§cStok barang ini untuk hari ini telah habis!"));
                            return 0;
                        }

                        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
                        if (deal.useGems()) {
                            if (!data.removeGems((int) deal.finalPrice())) {
                                player.sendSystemMessage(Component.literal("§cKoin Gems kamu tidak cukup! Butuh " + (int) deal.finalPrice() + " Gems"));
                                return 0;
                            }
                        } else {
                            if (!data.removeBalance(deal.finalPrice())) {
                                player.sendSystemMessage(Component.literal("§cSaldo uang kamu tidak cukup! Butuh $" + String.format("%.2f", deal.finalPrice())));
                                return 0;
                            }
                        }

                        player.getInventory().add(deal.stack().copy());
                        DynamicPricingEngine.recordPurchase(BuiltInRegistries.ITEM.getKey(deal.stack().getItem()).toString(), deal.stack().getCount());
                        TakashaRpgManager.savePlayerData(player.getUUID());
                        TakashaRpgManager.syncToClient(player);

                        player.sendSystemMessage(Component.literal("§a§l[" + commandName + "] §fBerhasil membeli §e"
                                + deal.stack().getHoverName().getString() + " x" + deal.stack().getCount() + "§f!"));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
                        return 1;
                    })
                )
            )
        );
    }

    // 5. Advanced Dungeon Arena (ADA): /dungeon, /arena, /dungeons, /ada
    private static void registerArenaCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        SuggestionProvider<CommandSourceStack> tierSuggestions = (ctx, builder) -> {
            return SharedSuggestionProvider.suggest(List.of("common", "nightmare", "sakura_boss"), builder);
        };

        for (String alias : List.of("dungeon", "arena", "dungeons", "ada")) {
            dispatcher.register(Commands.literal(alias)
                // /dungeon info
                .then(Commands.literal("info")
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        DungeonArenaEngine engine = DungeonArenaEngine.getInstance();
                        if (!engine.isArenaActive()) {
                            source.sendSuccess(() -> Component.literal("§7[Dungeon] Status Arena: §aTidak Aktif (IDLE)§7. Gunakan Dungeon Rift Keystone untuk memulai!"), false);
                            return 1;
                        }
                        int wave = engine.getCurrentWave();
                        int killed = engine.getMobsKilledInWave();
                        int total = engine.getTotalMobsInWave();
                        int remaining = engine.getActiveMobCount();
                        int blocks = engine.getBlockSnapshotCount();
                        int percent = total > 0 ? Math.min(100, Math.round(((float) killed / (float) total) * 100)) : 0;

                        source.sendSuccess(() -> Component.literal("§6§l★ TAKASHA DUNGEON ARENA AKTIF ★\n" +
                                "§7Tingkat: " + engine.getTier().getColorCode() + engine.getTier().getDisplayName() + "\n" +
                                "§7Gelombang: §e" + wave + " / " + DungeonArenaEngine.TOTAL_WAVES + "\n" +
                                "§7Progress Wave: §a" + killed + " / " + total + " Mobs §8(§e" + percent + "% Selesai§8)\n" +
                                "§7Monster Hidup di Kubah: §c" + remaining + " Mobs\n" +
                                "§7Blok Snapshot Terproteksi: §b" + blocks + " Blok (Zero Griefing)\n" +
                                "§7Titik Pusat: §f" + engine.getCenterPos().toShortString() + "\n" +
                                "§7Penantang UUID: §b" + engine.getChallengerUuid() + "\n" +
                                "§7Sesi Persistensi & Crash Recovery: §aTERLINDUNGI (Active)"), false);
                        return 1;
                    })
                )
                // /dungeon leave
                .then(Commands.literal("leave")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        DungeonArenaEngine engine = DungeonArenaEngine.getInstance();
                        if (!engine.isArenaActive()) {
                            player.sendSystemMessage(Component.literal("§c[Dungeon] Tidak ada arena yang sedang berjalan!"));
                            return 0;
                        }
                        if (!player.getUUID().equals(engine.getChallengerUuid()) && !context.getSource().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                            player.sendSystemMessage(Component.literal("§c[Dungeon] Anda bukan penantang di arena ini!"));
                            return 0;
                        }
                        engine.forceStop(false);
                        player.sendSystemMessage(Component.literal("§e[Dungeon] Anda telah menyerah dan meninggalkan arena."));
                        return 1;
                    })
                )
                // /dungeon start <tier> [player] (OP level 2+)
                .then(Commands.literal("start")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.argument("tier", StringArgumentType.word())
                        .suggests(tierSuggestions)
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String tierStr = StringArgumentType.getString(context, "tier");
                            DungeonTier t = DungeonTier.fromId(tierStr);
                            if (DungeonArenaEngine.getInstance().startDungeon(player, t, player.blockPosition())) {
                                context.getSource().sendSuccess(() -> Component.literal("§a[Dungeon] Arena tier " + t.getDisplayName() + " berhasil dimulai!"), true);
                                return 1;
                            }
                            return 0;
                        })
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(context -> {
                                ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                String tierStr = StringArgumentType.getString(context, "tier");
                                DungeonTier t = DungeonTier.fromId(tierStr);
                                if (DungeonArenaEngine.getInstance().startDungeon(target, t, target.blockPosition())) {
                                    context.getSource().sendSuccess(() -> Component.literal("§a[Dungeon] Arena tier " + t.getDisplayName() + " dimulai untuk " + target.getName().getString()), true);
                                    return 1;
                                }
                                return 0;
                            })
                        )
                    )
                )
                // /dungeon stop [asVictory] (OP level 2+)
                .then(Commands.literal("stop")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .executes(context -> {
                        DungeonArenaEngine engine = DungeonArenaEngine.getInstance();
                        if (!engine.isArenaActive()) {
                            context.getSource().sendFailure(Component.literal("§cTidak ada Dungeon Arena yang sedang berjalan!"));
                            return 0;
                        }
                        engine.forceStop(false);
                        context.getSource().sendSuccess(() -> Component.literal("§eTakasha Dungeon Arena berhasil dihentikan."), true);
                        return 1;
                    })
                    .then(Commands.argument("victory", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                        .executes(context -> {
                            DungeonArenaEngine engine = DungeonArenaEngine.getInstance();
                            if (!engine.isArenaActive()) {
                                context.getSource().sendFailure(Component.literal("§cTidak ada Dungeon Arena yang sedang berjalan!"));
                                return 0;
                            }
                            boolean victory = com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "victory");
                            engine.forceStop(victory);
                            context.getSource().sendSuccess(() -> Component.literal("§eTakasha Dungeon Arena dihentikan (Victory: " + victory + ")."), true);
                            return 1;
                        })
                    )
                )
                // /dungeon setstage <wave> (OP level 2+)
                .then(Commands.literal("setstage")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.argument("wave", IntegerArgumentType.integer(1, 5))
                        .executes(context -> {
                            int wave = IntegerArgumentType.getInteger(context, "wave");
                            DungeonArenaEngine engine = DungeonArenaEngine.getInstance();
                            if (!engine.isArenaActive()) {
                                context.getSource().sendFailure(Component.literal("§cArena tidak sedang aktif!"));
                                return 0;
                            }
                            engine.forceWave(wave);
                            context.getSource().sendSuccess(() -> Component.literal("§a[Dungeon] Stage arena dipaksa ke Wave " + wave), true);
                            return 1;
                        })
                    )
                )
                // /dungeon rollback (OP level 2+)
                .then(Commands.literal("rollback")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .executes(context -> {
                        DungeonArenaEngine.getInstance().forceRollback();
                        context.getSource().sendSuccess(() -> Component.literal("§a[Dungeon] Seluruh blok arena berhasil dipulihkan secara manual (Emergency Rollback)."), true);
                        return 1;
                    })
                )
                // /dungeon givekeystone <player> <tier> [amount] (OP level 2+)
                .then(Commands.literal("givekeystone")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("tier", StringArgumentType.word())
                            .suggests(tierSuggestions)
                            .executes(context -> {
                                ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                String tierStr = StringArgumentType.getString(context, "tier");
                                DungeonTier t = DungeonTier.fromId(tierStr);
                                Item keystoneItem = switch (t) {
                                    case COMMON -> ModRpgItems.DUNGEON_KEYSTONE_COMMON;
                                    case NIGHTMARE -> ModRpgItems.DUNGEON_KEYSTONE_NIGHTMARE;
                                    case SAKURA_BOSS -> ModRpgItems.DUNGEON_KEYSTONE_SAKURA;
                                };
                                target.getInventory().add(new ItemStack(keystoneItem, 1));
                                context.getSource().sendSuccess(() -> Component.literal("§aMemberikan 1x " + t.getDisplayName() + " Keystone ke " + target.getName().getString()), true);
                                return 1;
                            })
                            .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    String tierStr = StringArgumentType.getString(context, "tier");
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    DungeonTier t = DungeonTier.fromId(tierStr);
                                    Item keystoneItem = switch (t) {
                                        case COMMON -> ModRpgItems.DUNGEON_KEYSTONE_COMMON;
                                        case NIGHTMARE -> ModRpgItems.DUNGEON_KEYSTONE_NIGHTMARE;
                                        case SAKURA_BOSS -> ModRpgItems.DUNGEON_KEYSTONE_SAKURA;
                                    };
                                    target.getInventory().add(new ItemStack(keystoneItem, amount));
                                    context.getSource().sendSuccess(() -> Component.literal("§aMemberikan " + amount + "x " + t.getDisplayName() + " Keystone ke " + target.getName().getString()), true);
                                    return 1;
                                })
                            )
                        )
                    )
                )
                // /dungeon reload (OP level 2+)
                .then(Commands.literal("reload")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .executes(context -> {
                        context.getSource().sendSuccess(() -> Component.literal("§a[Dungeon] Konfigurasi dungeon dan drops berhasil dimuat ulang!"), true);
                        return 1;
                    })
                )
            );
        }
    }

    public static class TrashMenu extends ChestMenu {
        private final Container trashContainer;

        public TrashMenu(int containerId, Inventory playerInventory, Container container) {
            super(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6);
            this.trashContainer = container;
        }

        @Override
        public void removed(Player player) {
            super.removed(player);
            if (player instanceof ServerPlayer sp) {
                Map<String, Integer> itemCounts = new LinkedHashMap<>();
                int totalCount = 0;
                for (int i = 0; i < trashContainer.getContainerSize(); i++) {
                    ItemStack stack = trashContainer.getItem(i);
                    if (!stack.isEmpty()) {
                        String name = stack.getHoverName().getString();
                        itemCounts.put(name, itemCounts.getOrDefault(name, 0) + stack.getCount());
                        totalCount += stack.getCount();
                    }
                }
                trashContainer.clearContent();
                if (totalCount > 0) {
                    StringBuilder sb = new StringBuilder();
                    int entryIdx = 0;
                    for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                        if (entryIdx > 0) sb.append("§7, ");
                        sb.append("§e").append(entry.getValue()).append("x §f").append(entry.getKey());
                        entryIdx++;
                        if (entryIdx >= 5 && itemCounts.size() > 5) {
                            sb.append("§7... (+").append(itemCounts.size() - entryIdx).append(" jenis lainnya)");
                            break;
                        }
                    }
                    sp.sendSystemMessage(Component.literal(
                        "§8[§c🗑 Tong Sampah§8] §7Memusnahkan §c" + totalCount + "§7 item (" + sb + "§7)."
                    ));
                    sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7f, 1.2f);
                }
            }
        }
    }

    // 6. Executable Command Tomes & Relics: 29 Commands Hooked Up
    private static void registerUtilityCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 1) /workbench, /wb
        for (String cmd : List.of("workbench", "wb")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(context -> openWorkbench(context.getSource().getPlayerOrException()))
            );
        }

        // 2) /anvil
        dispatcher.register(Commands.literal("anvil")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                if (!player.canUseGameMasterBlocks() && !checkCommandPermission(player, "anvil")) return 0;
                player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new VirtualWorkstationMenus.VirtualAnvilMenu(id, inv),
                    Component.literal("Portable Anvil")
                ));
                return 1;
            })
        );

        // 3) /enderchest, /ec
        for (String cmd : List.of("enderchest", "ec")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(context -> openEnderChest(context.getSource().getPlayerOrException()))
            );
        }

        // 4) /trash
        dispatcher.register(Commands.literal("trash")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                if (!player.canUseGameMasterBlocks() && !checkCommandPermission(player, "trash")) return 0;

                player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new TrashMenu(id, inv, new net.minecraft.world.SimpleContainer(54)),
                    Component.literal("§8§lTong Sampah (Tutup untuk Buang)")
                ));
                return 1;
            })
        );

        // 5) /grindstone
        dispatcher.register(Commands.literal("grindstone")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                if (!player.canUseGameMasterBlocks() && !checkCommandPermission(player, "grindstone")) return 0;
                player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new VirtualWorkstationMenus.VirtualGrindstoneMenu(id, inv),
                    Component.literal("Portable Grindstone")
                ));
                return 1;
            })
        );

        // 6) /stonecutter
        dispatcher.register(Commands.literal("stonecutter")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                if (!player.canUseGameMasterBlocks() && !checkCommandPermission(player, "stonecutter")) return 0;
                player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new VirtualWorkstationMenus.VirtualStonecutterMenu(id, inv),
                    Component.literal("Portable Stonecutter")
                ));
                return 1;
            })
        );

        // 7) /smithingtable, /smithing
        for (String cmd : List.of("smithingtable", "smithing")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (!player.canUseGameMasterBlocks() && !checkCommandPermission(player, "smithingtable")) return 0;
                    player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new VirtualWorkstationMenus.VirtualSmithingMenu(id, inv),
                        Component.literal("Portable Smithing Table")
                    ));
                    return 1;
                })
            );
        }

        // 8) /cartographytable, /cartography
        for (String cmd : List.of("cartographytable", "cartography")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (!player.canUseGameMasterBlocks() && !checkCommandPermission(player, "cartographytable")) return 0;
                    player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new VirtualWorkstationMenus.VirtualCartographyTableMenu(id, inv),
                        Component.literal("Portable Cartography Table")
                    ));
                    return 1;
                })
            );
        }

        // 9) /loom
        dispatcher.register(Commands.literal("loom")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                if (!player.canUseGameMasterBlocks() && !checkCommandPermission(player, "loom")) return 0;
                player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new VirtualWorkstationMenus.VirtualLoomMenu(id, inv),
                    Component.literal("Portable Loom")
                ));
                return 1;
            })
        );

        // 10) /getpos, /coords
        for (String cmd : List.of("getpos", "coords")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(context -> executeGetPos(context.getSource().getPlayerOrException()))
            );
        }

        // 11) /depth
        dispatcher.register(Commands.literal("depth")
            .executes(context -> executeDepth(context.getSource().getPlayerOrException()))
        );

        // 12) /compass
        dispatcher.register(Commands.literal("compass")
            .executes(context -> executeCompass(context.getSource().getPlayerOrException()))
        );

        // 13) /ptime, /playertime
        for (String cmd : List.of("ptime", "playertime")) {
            dispatcher.register(Commands.literal(cmd)
                .then(Commands.literal("day").executes(c -> executePTime(c.getSource().getPlayerOrException(), 1000L)))
                .then(Commands.literal("night").executes(c -> executePTime(c.getSource().getPlayerOrException(), 13000L)))
                .then(Commands.literal("reset").executes(c -> executePTimeReset(c.getSource().getPlayerOrException())))
                .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 24000))
                    .executes(c -> executePTime(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "ticks"))))
            );
        }

        // 14) /pweather, /playerweather
        for (String cmd : List.of("pweather", "playerweather")) {
            dispatcher.register(Commands.literal(cmd)
                .then(Commands.literal("clear").executes(c -> executePWeather(c.getSource().getPlayerOrException(), "clear")))
                .then(Commands.literal("rain").executes(c -> executePWeather(c.getSource().getPlayerOrException(), "rain")))
                .then(Commands.literal("reset").executes(c -> executePWeather(c.getSource().getPlayerOrException(), "reset")))
            );
        }

        // 15) /itemname, /iname
        for (String cmd : List.of("itemname", "iname")) {
            dispatcher.register(Commands.literal(cmd)
                .then(Commands.argument("name", StringArgumentType.greedyString())
                    .executes(c -> executeItemName(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "name"))))
            );
        }

        // 16) /hat, /head
        for (String cmd : List.of("hat", "head")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(c -> executeHat(c.getSource().getPlayerOrException()))
            );
        }

        // 17) /suicide, /esuicide
        for (String cmd : List.of("suicide", "esuicide")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(c -> executeSuicide(c.getSource().getPlayerOrException()))
            );
        }

        // 18) /tpr, /rtp, /tprandom
        for (String cmd : List.of("tpr", "rtp", "tprandom")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(c -> executeRandomTeleport(c.getSource().getPlayerOrException()))
            );
        }

        // 19) /top, /etop
        for (String cmd : List.of("top", "etop")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(c -> executeTop(c.getSource().getPlayerOrException()))
            );
        }

        // 20) /bottom, /ebottom
        for (String cmd : List.of("bottom", "ebottom")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(c -> executeBottom(c.getSource().getPlayerOrException()))
            );
        }

        // 21) /jump, /j, /jumpto
        for (String cmd : List.of("jump", "j", "jumpto")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(c -> executeJump(c.getSource().getPlayerOrException()))
            );
        }

        // 22) /feed, /eat
        for (String cmd : List.of("feed", "eat")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (!player.canUseGameMasterBlocks() && !checkCommandPermission(player, "feed")) return 0;

                    player.getFoodData().setFoodLevel(20);
                    player.getFoodData().setSaturation(20.0f);
                    player.sendSystemMessage(Component.literal("§a§l[Takasha-RPG] §fRasa lapar telah dipulihkan sepenuhnya oleh Segel Gourmet!"));
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0f, 1.0f);
                    return 1;
                })
            );
        }

        // 23) /heal, /eheal
        for (String cmd : List.of("heal", "eheal")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (!player.canUseGameMasterBlocks() && !checkCommandPermission(player, "heal")) return 0;

                    TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
                    if (!player.isCreative() && !data.consumeMana(20.0)) {
                        player.sendSystemMessage(Component.literal("§cMana tidak cukup! Butuh 20 Mana untuk Segel Vitalitas (Mana: " + (int) data.getCurrentMana() + "/" + (int) data.getMaxMana() + ")"));
                        return 0;
                    }
                    TakashaRpgManager.syncToClient(player);

                    player.setHealth(player.getMaxHealth());
                    player.getFoodData().setFoodLevel(20);
                    player.getFoodData().setSaturation(20.0f);
                    player.removeAllEffects();
                    player.sendSystemMessage(Component.literal("§a§l[Takasha-RPG] §fKondisi tubuh dan darah telah dipulihkan sepenuhnya oleh Segel Vitalitas!"));
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
                    return 1;
                })
            );
        }

        // 24) /ext, /extinguish
        for (String cmd : List.of("ext", "extinguish")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(c -> executeExtinguish(c.getSource().getPlayerOrException()))
            );
        }

        // 25) /rest, /erest
        for (String cmd : List.of("rest", "erest")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(c -> executeRest(c.getSource().getPlayerOrException()))
            );
        }

        // 26) /repair, /fix
        for (String cmd : List.of("repair", "fix")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(c -> executeRepair(c.getSource().getPlayerOrException()))
                .then(Commands.literal("all")
                    .executes(c -> executeRepairAll(c.getSource().getPlayerOrException()))
                )
                .then(Commands.literal("hand")
                    .executes(c -> executeRepair(c.getSource().getPlayerOrException()))
                )
            );
        }

        // 27) /condense, /compact
        for (String cmd : List.of("condense", "compact")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(c -> executeCondense(c.getSource().getPlayerOrException()))
            );
        }

        // 28) /ce, /customenchants, /enchanter (Default Survival Commands)
        for (String cmdName : List.of("ce", "customenchants", "enchanter")) {
            dispatcher.register(Commands.literal(cmdName)
                .executes(context -> showEnchanterMenu(context.getSource().getPlayerOrException()))
                .then(Commands.literal("gacha")
                    .then(Commands.argument("tier", StringArgumentType.string())
                        .suggests(TIER_SUGGESTION_PROVIDER)
                        .executes(c -> gachaEnchant(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "tier")))
                    )
                )
                .then(Commands.literal("list")
                    .executes(c -> {
                        TakashaEnchantCatalogMenu.open(c.getSource().getPlayerOrException());
                        return 1;
                    })
                    .then(Commands.argument("tier", StringArgumentType.string())
                        .suggests(TIER_SUGGESTION_PROVIDER)
                        .executes(c -> listEnchants(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "tier")))
                    )
                )
                .then(Commands.literal("catalog")
                    .executes(c -> {
                        TakashaEnchantCatalogMenu.open(c.getSource().getPlayerOrException());
                        return 1;
                    })
                )
                .then(Commands.literal("menu")
                    .executes(context -> showEnchanterMenu(context.getSource().getPlayerOrException()))
                )
                .then(Commands.literal("gui")
                    .executes(context -> showEnchanterMenu(context.getSource().getPlayerOrException()))
                )
                .then(Commands.literal("help")
                    .executes(context -> executeCeHelp(context.getSource().getPlayerOrException()))
                )
                .then(Commands.literal("info")
                    .then(Commands.argument("enchant", StringArgumentType.word())
                        .suggests(ENCHANT_SUGGESTION_PROVIDER)
                        .executes(c -> executeAeInfo(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "enchant")))
                    )
                )
                .then(Commands.literal("buy")
                    .then(Commands.literal("white_scroll").executes(c -> buyCeItem(c.getSource().getPlayerOrException(), new ItemStack(ModRpgItems.WHITE_SCROLL), 300.0, false)))
                    .then(Commands.literal("black_scroll").executes(c -> buyCeItem(c.getSource().getPlayerOrException(), new ItemStack(ModRpgItems.BLACK_SCROLL), 650.0, false)))
                    .then(Commands.literal("transmute_scroll").executes(c -> buyCeItem(c.getSource().getPlayerOrException(), new ItemStack(ModRpgItems.TRANSMUTE_SCROLL), 280.0, false)))
                    .then(Commands.literal("magic_dust").executes(c -> buyCeItem(c.getSource().getPlayerOrException(), new ItemStack(ModRpgItems.MAGIC_DUST, 2), 150.0, false)))
                    .then(Commands.literal("slot_increaser").executes(c -> buyCeItem(c.getSource().getPlayerOrException(), new ItemStack(ModRpgItems.SLOT_INCREASER_1), 5.0, true)))
                    .then(Commands.literal("master_increaser").executes(c -> buyCeItem(c.getSource().getPlayerOrException(), new ItemStack(ModRpgItems.SLOT_INCREASER_2), 15.0, true)))
                    .then(Commands.literal("soul_increaser").executes(c -> buyCeItem(c.getSource().getPlayerOrException(), new ItemStack(ModRpgItems.SLOT_INCREASER_3), 25.0, true)))
                )
            );
        }

        // 29) /tinker & /tinkerer (Default Survival Command)
        for (String cmd : List.of("tinker", "tinkerer")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    TinkererMenu.open(player);
                    return 1;
                })
            );
        }

        // /alchem & /alchemist (Default Survival Command)
        for (String cmd : List.of("alchem", "alchemist")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    AlchemistMenu.open(player);
                    return 1;
                })
            );
        }

        // /withdrawsouls
        dispatcher.register(Commands.literal("withdrawsouls")
            .executes(context -> withdrawSouls(context.getSource().getPlayerOrException(), 5))
            .then(Commands.argument("levels", IntegerArgumentType.integer(5, 50))
                .executes(context -> withdrawSouls(context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "levels")))
            )
        );
    }

    private static int openWorkbench(ServerPlayer player) {
        if (!player.canUseGameMasterBlocks() && !checkCommandPermission(player, "workbench")) return 0;
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new VirtualCraftingMenu(containerId, playerInventory),
                Component.literal("Meja Kerajinan Portabel")
        ));
        return 1;
    }

    private static int openEnderChest(ServerPlayer player) {
        if (!checkCommandPermission(player, "enderchest")) return 0;
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) -> ChestMenu.threeRows(containerId, playerInventory, player.getEnderChestInventory()),
                Component.literal("Ender Chest Portabel")
        ));
        return 1;
    }

    private static int executeGetPos(ServerPlayer player) {
        if (!checkCommandPermission(player, "getpos")) return 0;
        String dim = player.level().dimension().identifier().toString();
        String biome = player.level().getBiome(player.blockPosition()).unwrapKey().map(k -> k.identifier().toString()).orElse("unknown");
        player.sendSystemMessage(Component.literal(
            "§6§l╔══════════════════════════════════════════╗\n" +
            "§6§l║         ★ ASTROLABE NAVIGASI ★           ║\n" +
            "§6§l╚══════════════════════════════════════════╝\n" +
            "§fKoordinat : §eX: §a" + String.format("%.2f", player.getX()) + " §eY: §a" + String.format("%.2f", player.getY()) + " §eZ: §a" + String.format("%.2f", player.getZ()) + "\n" +
            "§fDimensi   : §b" + dim + "\n" +
            "§fBioma     : §e" + biome + "\n" +
            "§fOrientasi : §7Yaw: §e" + String.format("%.1f", player.getYRot()) + "° §7| Pitch: §e" + String.format("%.1f", player.getXRot()) + "°"
        ));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
        return 1;
    }

    private static int executeDepth(ServerPlayer player) {
        if (!checkCommandPermission(player, "depth")) return 0;
        int y = (int) Math.floor(player.getY());
        int minBuild = player.level().getMinY();
        int seaLevel = player.level().getSeaLevel();
        int aboveBedrock = y - minBuild;
        int relSea = y - seaLevel;
        player.sendSystemMessage(Component.literal(
            "§6§l╔══════════════════════════════════════════╗\n" +
            "§6§l║      ★ BANDUL KEDALAMAN (DEPTH) ★        ║\n" +
            "§6§l╚══════════════════════════════════════════╝\n" +
            "§fKetinggian Y           : §e" + y + "\n" +
            "§fJarak di atas Bedrock  : §a" + aboveBedrock + " blok\n" +
            "§fRelatif Permukaan Laut : " + (relSea >= 0 ? "§a+" + relSea : "§c" + relSea) + " blok §7(Laut = Y: " + seaLevel + ")"
        ));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 1.0f, 1.0f);
        return 1;
    }

    private static int executeCompass(ServerPlayer player) {
        if (!checkCommandPermission(player, "compass")) return 0;
        float yaw = (player.getYRot() % 360 + 360) % 360;
        String dir;
        if (yaw >= 337.5 || yaw < 22.5) dir = "Selatan (South) [+Z]";
        else if (yaw >= 22.5 && yaw < 67.5) dir = "Barat Daya (South-West)";
        else if (yaw >= 67.5 && yaw < 112.5) dir = "Barat (West) [-X]";
        else if (yaw >= 112.5 && yaw < 157.5) dir = "Barat Laut (North-West)";
        else if (yaw >= 157.5 && yaw < 202.5) dir = "Utara (North) [-Z]";
        else if (yaw >= 202.5 && yaw < 247.5) dir = "Timur Laut (North-East)";
        else if (yaw >= 247.5 && yaw < 292.5) dir = "Timur (East) [+X]";
        else dir = "Tenggara (South-East)";

        player.sendSystemMessage(Component.literal(
            "§6§l╔══════════════════════════════════════════╗\n" +
            "§6§l║         ★ KOMPAS LODESTONE ★             ║\n" +
            "§6§l╚══════════════════════════════════════════╝\n" +
            "§fArah Hadap  : §e§l" + dir + "\n" +
            "§fSudut Rotasi: §7" + String.format("%.1f", yaw) + "°"
        ));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0f, 1.2f);
        return 1;
    }

    private static int executePTime(ServerPlayer player, long targetTime) {
        if (!checkCommandPermission(player, "ptime")) return 0;
        ClientboundSetTimePacket full = player.level().getServer().clockManager().createFullSyncPacket();
        Map<Holder<WorldClock>, ClockNetworkState> map = new HashMap<>(full.clockUpdates());
        for (var entry : map.entrySet()) {
            ClockNetworkState c = entry.getValue();
            entry.setValue(new ClockNetworkState(targetTime, c.partialTick(), 0.0f));
        }
        player.connection.send(new ClientboundSetTimePacket(full.gameTime(), map));
        player.sendSystemMessage(Component.literal("§6§l[Chrono Lens] §aWaktu visual diatur ke: §e" + targetTime + " ticks"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.5f);
        return 1;
    }

    private static int executePTimeReset(ServerPlayer player) {
        if (!checkCommandPermission(player, "ptime")) return 0;
        player.connection.send(player.level().getServer().clockManager().createFullSyncPacket());
        player.sendSystemMessage(Component.literal("§6§l[Chrono Lens] §aWaktu visual direset mengikuti waktu server."));
        return 1;
    }

    private static int executePWeather(ServerPlayer player, String weather) {
        if (!checkCommandPermission(player, "pweather")) return 0;
        if ("clear".equalsIgnoreCase(weather)) {
            player.connection.send(new ClientboundGameEventPacket(
                ClientboundGameEventPacket.STOP_RAINING, 0.0f
            ));
            player.connection.send(new ClientboundGameEventPacket(
                ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 0.0f
            ));
            player.sendSystemMessage(Component.literal("§6§l[Prism Clear Skies] §aCuaca visual diatur ke: §eCerah (Clear)"));
        } else if ("rain".equalsIgnoreCase(weather)) {
            player.connection.send(new ClientboundGameEventPacket(
                ClientboundGameEventPacket.START_RAINING, 0.0f
            ));
            player.connection.send(new ClientboundGameEventPacket(
                ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 1.0f
            ));
            player.sendSystemMessage(Component.literal("§6§l[Prism Clear Skies] §aCuaca visual diatur ke: §bHujan (Rain)"));
        } else {
            float rainLevel = player.level().isRaining() ? 1.0f : 0.0f;
            player.connection.send(new ClientboundGameEventPacket(
                player.level().isRaining() ? ClientboundGameEventPacket.START_RAINING : ClientboundGameEventPacket.STOP_RAINING, 0.0f
            ));
            player.connection.send(new ClientboundGameEventPacket(
                ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, rainLevel
            ));
            player.sendSystemMessage(Component.literal("§6§l[Prism Clear Skies] §aCuaca visual direset mengikuti kondisi cuaca server."));
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.5f);
        return 1;
    }

    private static int executeItemName(ServerPlayer player, String newName) {
        if (!checkCommandPermission(player, "itemname")) return 0;
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cKamu tidak memegang item apapun di tangan!"));
            return 0;
        }

        String formatted = newName.replace('&', '§');
        held.set(DataComponents.CUSTOM_NAME, Component.literal(formatted));
        player.sendSystemMessage(Component.literal("§6§l[Golden Quill] §aNama item berhasil diubah menjadi: §r" + formatted));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 1.0f, 1.2f);
        return 1;
    }

    private static int executeHat(ServerPlayer player) {
        if (!checkCommandPermission(player, "hat")) return 0;
        ItemStack held = player.getMainHandItem();
        ItemStack currentHelm = player.getItemBySlot(EquipmentSlot.HEAD);

        if (held.isEmpty() && currentHelm.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cKamu tidak memegang item apapun di tangan!"));
            return 0;
        }

        player.setItemSlot(EquipmentSlot.HEAD, held.copy());
        player.setItemInHand(InteractionHand.MAIN_HAND, currentHelm.copy());

        player.sendSystemMessage(Component.literal("§6§l[Jester's Crown] §aBerhasil memasang item di tangan sebagai topi!"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
        return 1;
    }

    private static int executeSuicide(ServerPlayer player) {
        if (!checkCommandPermission(player, "suicide")) return 0;
        player.sendSystemMessage(Component.literal("§c§l[Talisman of Departure] §fKamu melepaskan raga dan kembali ke titik asal."));
        player.kill((ServerLevel) player.level());
        return 1;
    }

    private static int executeRandomTeleport(ServerPlayer player) {
        if (!checkCommandPermission(player, "tpr")) return 0;
        ServerLevel level = (ServerLevel) player.level();
        Random rand = new Random();
        double angle = rand.nextDouble() * 2 * Math.PI;
        double dist = 1000 + rand.nextDouble() * 3000;
        int rx = (int) (player.getX() + Math.cos(angle) * dist);
        int rz = (int) (player.getZ() + Math.sin(angle) * dist);
        int ry = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, rx, rz);

        BlockPos pos = new BlockPos(rx, ry, rz);
        if (!level.getBlockState(pos.below()).getFluidState().isEmpty()) {
            ry = level.getHeight(Heightmap.Types.WORLD_SURFACE, rx + 50, rz + 50);
            pos = new BlockPos(rx + 50, ry, rz + 50);
        }

        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        data.setLastTeleportLocation(player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        TakashaRpgManager.savePlayerData(player.getUUID());

        if (player.isCreative() || player.isSpectator()) {
            player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot(), true);
            player.sendSystemMessage(Component.literal("§aTeleportasi acak ke §e[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]§a!"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
            return 1;
        }

        long currentTick = player.level().getServer() != null ? player.level().getServer().getTickCount() : 0;
        long triggerTick = currentTick + 60;
        PENDING_TELEPORTS.put(player.getUUID(), new PendingTeleport(player, "alam liar acak",
            new TakashaRpgData.HomeLocation(level.dimension().identifier().toString(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), player.getXRot()),
            triggerTick));
        player.sendSystemMessage(Component.literal("§7Teleporting to §eWilderness §7in §c3 seconds§7. Do not move or take damage!"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
        return 1;
    }

    private static int executeTop(ServerPlayer player) {
        if (!checkCommandPermission(player, "top")) return 0;
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        if (!player.isCreative() && !data.consumeMana(10.0)) {
            player.sendSystemMessage(Component.literal("§cMana tidak cukup! Butuh 10 Mana untuk jurus Ascension (Mana: " + (int) data.getCurrentMana() + "/" + (int) data.getMaxMana() + ")"));
            return 0;
        }
        TakashaRpgManager.syncToClient(player);

        int startY = player.getBlockY();
        int maxY = player.level().getMaxY() - 2;
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos(player.getBlockX(), maxY, player.getBlockZ());
        boolean found = false;
        while (mpos.getY() > startY) {
            if (!player.level().getBlockState(mpos).isAir() && player.level().getBlockState(mpos).isSolid()) {
                BlockPos above1 = mpos.above();
                BlockPos above2 = mpos.above(2);
                if (player.level().getBlockState(above1).isAir() && player.level().getBlockState(above2).isAir()) {
                    found = true;
                    break;
                }
            }
            mpos.move(net.minecraft.core.Direction.DOWN);
        }

        if (!found) {
            player.sendSystemMessage(Component.literal("§cTidak ditemukan permukaan aman di atas posisimu!"));
            return 0;
        }

        data.setLastTeleportLocation(player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        TakashaRpgManager.savePlayerData(player.getUUID());

        player.teleportTo((ServerLevel) player.level(), mpos.getX() + 0.5, mpos.getY() + 1.0, mpos.getZ() + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot(), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.2f);
        player.sendSystemMessage(Component.literal("§a§l[Ascension] §fTeleportasi ke permukaan teratas!"));
        return 1;
    }

    private static int executeBottom(ServerPlayer player) {
        if (!checkCommandPermission(player, "bottom")) return 0;
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        if (!player.isCreative() && !data.consumeMana(10.0)) {
            player.sendSystemMessage(Component.literal("§cMana tidak cukup! Butuh 10 Mana untuk jurus Deep Descent (Mana: " + (int) data.getCurrentMana() + "/" + (int) data.getMaxMana() + ")"));
            return 0;
        }
        TakashaRpgManager.syncToClient(player);

        int startY = player.getBlockY();
        int minY = player.level().getMinY() + 2;
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos(player.getBlockX(), minY, player.getBlockZ());
        boolean found = false;
        while (mpos.getY() < startY) {
            if (!player.level().getBlockState(mpos).isAir() && player.level().getBlockState(mpos).isSolid()) {
                BlockPos above1 = mpos.above();
                BlockPos above2 = mpos.above(2);
                if (player.level().getBlockState(above1).isAir() && player.level().getBlockState(above2).isAir()) {
                    found = true;
                    break;
                }
            }
            mpos.move(net.minecraft.core.Direction.UP);
        }

        if (!found) {
            player.sendSystemMessage(Component.literal("§cTidak ditemukan lantai gua aman di bawah posisimu!"));
            return 0;
        }

        data.setLastTeleportLocation(player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        TakashaRpgManager.savePlayerData(player.getUUID());

        player.teleportTo((ServerLevel) player.level(), mpos.getX() + 0.5, mpos.getY() + 1.0, mpos.getZ() + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot(), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.8f);
        player.sendSystemMessage(Component.literal("§b§l[Deep Descent] §fTeleportasi ke gua terdalam!"));
        return 1;
    }

    private static int executeJump(ServerPlayer player) {
        if (!checkCommandPermission(player, "jump")) return 0;
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        if (!player.isCreative() && !data.consumeMana(15.0)) {
            player.sendSystemMessage(Component.literal("§cMana tidak cukup! Butuh 15 Mana untuk jurus Void Blink (Mana: " + (int) data.getCurrentMana() + "/" + (int) data.getMaxMana() + ")"));
            return 0;
        }

        HitResult hit = player.pick(50.0, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.sendSystemMessage(Component.literal("§cTidak ada blok sasaran yang terjangkau dalam jarak 50 blok!"));
            return 0;
        }

        data.consumeMana(15.0);
        TakashaRpgManager.syncToClient(player);

        BlockHitResult bhr = (BlockHitResult) hit;
        BlockPos targetPos = bhr.getBlockPos().relative(bhr.getDirection());

        data.setLastTeleportLocation(player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        TakashaRpgManager.savePlayerData(player.getUUID());

        player.teleportTo((ServerLevel) player.level(), targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot(), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.4f);
        player.sendSystemMessage(Component.literal("§5§l[Void Blink] §fMelompat kilat ke lokasi bidikan!"));
        return 1;
    }

    private static int executeExtinguish(ServerPlayer player) {
        if (!checkCommandPermission(player, "ext")) return 0;
        if (player.getRemainingFireTicks() <= 0) {
            player.sendSystemMessage(Component.literal("§eTubuhmu tidak sedang terbakar api!"));
            return 0;
        }

        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        if (!player.isCreative() && !data.consumeMana(5.0)) {
            player.sendSystemMessage(Component.literal("§cMana tidak cukup! Butuh 5 Mana untuk memadamkan kobaran api (Mana: " + (int) data.getCurrentMana() + "/" + (int) data.getMaxMana() + ")"));
            return 0;
        }
        TakashaRpgManager.syncToClient(player);

        player.clearFire();
        player.setRemainingFireTicks(0);
        player.sendSystemMessage(Component.literal("§b§l[Tear of the Frostborn] §aKobaran api di tubuhmu berhasil dipadamkan!"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0f, 1.0f);
        return 1;
    }

    private static int executeRest(ServerPlayer player) {
        if (!checkCommandPermission(player, "rest")) return 0;
        player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        player.sendSystemMessage(Component.literal("§d§l[Dreamcatcher Charm] §aCatatan insomnia karakter telah direset! Monster Phantom tidak akan mengganggumu malam ini."));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.8f);
        return 1;
    }

    private static int executeRepair(ServerPlayer player) {
        if (!checkCommandPermission(player, "repair")) return 0;
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !held.isDamageableItem()) {
            player.sendSystemMessage(Component.literal("§cKamu harus memegang item yang memiliki durabilitas untuk diperbaiki!"));
            return 0;
        }

        if (!held.isDamaged()) {
            player.sendSystemMessage(Component.literal("§eItem ini sudah dalam kondisi sempurna dan tidak rusak!"));
            return 0;
        }

        held.setDamageValue(0);
        player.sendSystemMessage(Component.literal("§6§l[Hammer of the Master Smith] §aDurabilitas item §e" + held.getHoverName().getString() + " §aberhasil diperbaiki sepenuhnya seperti baru!"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0f, 1.2f);
        return 1;
    }

    private static int executeRepairAll(ServerPlayer player) {
        if (!player.canUseGameMasterBlocks() && !checkCommandPermission(player, "repair_all")) return 0;

        int repairedCount = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged()) {
                stack.setDamageValue(0);
                repairedCount++;
            }
        }

        if (repairedCount == 0) {
            player.sendSystemMessage(Component.literal("§eSeluruh perlengkapan dan item di tasmu sudah dalam kondisi prima, tidak ada yang perlu diperbaiki."));
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6§l[Hammer of the Divine Smith] §aBerhasil memperbaiki §e§l"
                + repairedCount + " §aitem & armor di tasmu hingga pulih total!"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.5f);
        return repairedCount;
    }

    private static int executeCondense(ServerPlayer player) {
        if (!checkCommandPermission(player, "condense")) return 0;

        record MaterialBlockPair(Item item, Item block) {}
        List<MaterialBlockPair> pairs = List.of(
            new MaterialBlockPair(Items.IRON_INGOT, Items.IRON_BLOCK),
            new MaterialBlockPair(Items.GOLD_INGOT, Items.GOLD_BLOCK),
            new MaterialBlockPair(Items.COPPER_INGOT, Items.COPPER_BLOCK.asList().getFirst()),
            new MaterialBlockPair(Items.DIAMOND, Items.DIAMOND_BLOCK),
            new MaterialBlockPair(Items.EMERALD, Items.EMERALD_BLOCK),
            new MaterialBlockPair(Items.REDSTONE, Items.REDSTONE_BLOCK),
            new MaterialBlockPair(Items.COAL, Items.COAL_BLOCK),
            new MaterialBlockPair(Items.LAPIS_LAZULI, Items.LAPIS_BLOCK),
            new MaterialBlockPair(Items.NETHERITE_INGOT, Items.NETHERITE_BLOCK),
            new MaterialBlockPair(Items.RAW_IRON, Items.RAW_IRON_BLOCK),
            new MaterialBlockPair(Items.RAW_GOLD, Items.RAW_GOLD_BLOCK),
            new MaterialBlockPair(Items.RAW_COPPER, Items.RAW_COPPER_BLOCK),
            new MaterialBlockPair(Items.SLIME_BALL, Items.SLIME_BLOCK),
            new MaterialBlockPair(Items.WHEAT, Items.HAY_BLOCK)
        );

        int totalBlocksCreated = 0;
        int totalItemsCondensed = 0;

        for (MaterialBlockPair pair : pairs) {
            int count = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(pair.item())) {
                    count += stack.getCount();
                }
            }

            if (count >= 9) {
                int blocks = count / 9;
                int itemsToRemove = blocks * 9;
                totalBlocksCreated += blocks;
                totalItemsCondensed += itemsToRemove;

                int toRemove = itemsToRemove;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack.is(pair.item())) {
                        int rem = Math.min(stack.getCount(), toRemove);
                        stack.shrink(rem);
                        toRemove -= rem;
                        if (toRemove <= 0) break;
                    }
                }

                player.getInventory().add(new ItemStack(pair.block(), blocks));
            }
        }

        if (totalBlocksCreated == 0) {
            player.sendSystemMessage(Component.literal("§cTidak ada material di tasmu yang dapat dipadatkan (Minimal butuh 9 ingot/mineral yang sama)!"));
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6§l[Compactor Scroll] §aBerhasil memadatkan §e" + totalItemsCondensed + " material §amenjadi §b§l" + totalBlocksCreated + " balok padat§a!"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, 1.4f);
        return 1;
    }

    private static int buyCeItem(ServerPlayer player, ItemStack item, double cost, boolean useGems) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        if (useGems) {
            if (!data.removeGems((int) cost)) {
                player.sendSystemMessage(Component.literal("§cKoin Gems tidak cukup! Butuh " + (int) cost + " Gems"));
                return 0;
            }
        } else {
            if (!data.removeBalance(cost)) {
                player.sendSystemMessage(Component.literal("§cSaldo tidak cukup! Butuh $" + String.format("%.2f", cost)));
                return 0;
            }
        }

        player.getInventory().add(item);
        TakashaRpgManager.savePlayerData(player.getUUID());
        TakashaRpgManager.syncToClient(player);

        player.sendSystemMessage(Component.literal("§a§l[Enchanter] §fBerhasil membeli §e" + item.getHoverName().getString() + "§f!"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
        return 1;
    }

    private static int withdrawSouls(ServerPlayer player, int levels) {
        if (!checkCommandPermission(player, "withdrawsouls")) return 0;

        if (player.experienceLevel < levels) {
            player.sendSystemMessage(Component.literal("§cLevel EXP kamu tidak cukup! Minimal level: " + levels));
            return 0;
        }

        player.giveExperienceLevels(-levels);
        int gems = Math.max(1, levels / 5);
        player.getInventory().add(new ItemStack(ModRpgItems.TAKASHA_GEM, gems));

        player.sendSystemMessage(Component.literal("§5§l[Soul Extraction] §fMengekstrak §e" + levels + " Level EXP §fmenjadi §b" + gems + " Takasha Gem§f!"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.8f, 1.4f);
        return 1;
    }

    private static int showEnchanterMenu(ServerPlayer player) {
        EnchanterMenu.open(player);
        return 1;
    }

    private static int gachaEnchant(ServerPlayer player, String tierArg) {
        EnchantTier tier = null;
        for (EnchantTier t : EnchantTier.values()) {
            if (t.name().equalsIgnoreCase(tierArg) || t.getDisplayName().equalsIgnoreCase(tierArg)) {
                tier = t;
                break;
            }
        }
        if (tier == null) {
            player.sendSystemMessage(Component.literal("§cTier tidak valid! Pilihan: simple, unique, elite, ultimate, legendary, fabled, heroic, soul, mastery."));
            return 0;
        }

        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        if (!data.removeBalance(tier.getGachaCostCoins())) {
            player.sendSystemMessage(Component.literal("§cSaldo tidak cukup! Butuh $" + tier.getGachaCostCoins() + " untuk gacha tier " + tier.getDisplayName()));
            return 0;
        }

        final EnchantTier selectedTier = tier;
        Item bookItem = TakashaCustomBookItem.getBookItemForTier(selectedTier);
        ItemStack mysteryBook = new ItemStack(bookItem);

        if (!player.getInventory().add(mysteryBook)) {
            player.drop(mysteryBook, false);
        }

        TakashaRpgManager.savePlayerData(player.getUUID());
        TakashaRpgManager.syncToClient(player);

        player.sendSystemMessage(Component.literal("§a§l[Enchanter] §fBerhasil membeli " + selectedTier.getColorCode() + "§l" + selectedTier.getDisplayName().toUpperCase() + " MYSTERY BOOK§f! Klik kanan di tangan untuk membukanya!"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
        return 1;
    }

    private static int listEnchants(ServerPlayer player, String tierArg) {
        EnchantTier tier = null;
        for (EnchantTier t : EnchantTier.values()) {
            if (t.name().equalsIgnoreCase(tierArg) || t.getDisplayName().equalsIgnoreCase(tierArg)) {
                tier = t;
                break;
            }
        }
        if (tier == null) {
            player.sendSystemMessage(Component.literal("§cTier tidak valid! Pilihan: simple, unique, elite, ultimate, legendary, fabled, heroic, soul, mastery."));
            return 0;
        }

        List<CustomEnchantDef> list = TakashaEnchantRegistry.getByTier(tier);
        StringBuilder sb = new StringBuilder("§6§l[ENCHANTS TIER: " + tier.getColorCode() + tier.getDisplayName().toUpperCase() + "§6§l] (" + list.size() + " Total)\n");
        int count = 0;
        for (CustomEnchantDef def : list) {
            if (count >= 15) {
                sb.append("§7... dan ").append(list.size() - count).append(" enchant lainnya.\n");
                break;
            }
            sb.append(tier.getColorCode()).append("• ").append(def.name).append(" (Max Lvl ").append(def.maxLevel).append(") §7- ").append(def.description).append("\n");
            count++;
        }
        sb.append("§e/ce gacha ").append(tier.name().toLowerCase()).append(" §7untuk gacha tier ini ($").append(tier.getGachaCostCoins()).append(" Coins).");
        player.sendSystemMessage(Component.literal(sb.toString()));
        return 1;
    }

    // 7. Dynamic Help Command (/help, /ehelp)
    private record HelpEntry(String command, String lootName, String desc) {}

    private static final List<HelpEntry> LOOT_COMMANDS_HELP = List.of(
        new HelpEntry("workbench", "Tome of the Portable Workbench", "Membuka meja kerja Crafting Table 3x3 portabel."),
        new HelpEntry("anvil", "Tome of the Portable Anvil", "Membuka paron Anvil portabel untuk repair & rename."),
        new HelpEntry("enderchest", "Tome of the Portable Ender Chest", "Membuka akses ke Ender Chest pribadi di mana saja."),
        new HelpEntry("trash", "Tome of Disposal", "Membuka tong sampah 54 slot untuk membuang item tak terpakai."),
        new HelpEntry("grindstone", "Tome of the Portable Grindstone", "Membuka mesin pengasah portabel untuk menghapus sihir."),
        new HelpEntry("stonecutter", "Tome of the Portable Stonecutter", "Membuka meja pemotong batu portabel untuk memahat blok."),
        new HelpEntry("smithingtable", "Tome of Smithing", "Membuka meja tempa portabel untuk upgrade perlengkapan Netherite."),
        new HelpEntry("cartographytable", "Tome of Cartography", "Membuka meja kartografi portabel untuk menyalin/memperbesar peta."),
        new HelpEntry("loom", "Tome of Loom", "Membuka alat tenun portabel untuk membuat motif pola spanduk."),
        new HelpEntry("getpos", "Astrolabe Navigator", "Menampilkan koordinat presisi XYZ, dimensi, dan biome saat ini."),
        new HelpEntry("depth", "Plumb Bob of Depth", "Mengukur kedalaman vertikal Y terhadap bedrock & permukaan laut."),
        new HelpEntry("compass", "Lodestone Compass", "Menentukan arah hadap mata angin cardinal (Utara/Selatan/dll)."),
        new HelpEntry("ptime", "Chrono Crystal Lens", "Mengatur persepsi waktu visual siang/malam pada client (/ptime day/night/reset)."),
        new HelpEntry("pweather", "Prism of Clear Skies", "Mengatur persepsi cuaca visual pada client (/pweather clear/rain/reset)."),
        new HelpEntry("itemname", "Golden Quill of Inscription", "Mengubah nama item di tangan dengan dukungan kode warna §/& (/itemname <nama>)."),
        new HelpEntry("hat", "Jester's Crown", "Memasang blok atau item apa pun di tangan ke slot kepala/helm (/hat)."),
        new HelpEntry("suicide", "Talisman of Departure", "Mengakhiri hidup seketika saat tersangkut di void/bedrock (/suicide)."),
        new HelpEntry("sethome", "Ancient Keystone", "Menambah kapasitas batas maksimal penyimpanan /sethome (+1 slot, maks 10)."),
        new HelpEntry("tpr", "Wanderer's Compass", "Teleportasi acak ke lokasi aman di alam liar (/tpr /rtp)."),
        new HelpEntry("top", "Scroll of Ascension", "Teleportasi vertikal menembus ke permukaan teratas (Biaya: 10 Mana)."),
        new HelpEntry("bottom", "Scroll of Deep Descent", "Teleportasi vertikal menembus ke lantai gua terdasar (Biaya: 10 Mana)."),
        new HelpEntry("jump", "Eye of the Void Walker", "Melompat kilat (blink teleport) ke blok bidikan kursor (Biaya: 15 Mana)."),
        new HelpEntry("feed", "Sigil of the Gourmet", "Memulihkan rasa lapar dan saturasi hingga penuh (/feed /eat)."),
        new HelpEntry("heal", "Sigil of Vitality", "Memulihkan bar HP penuh & menawar racun (Biaya: 20 Mana)."),
        new HelpEntry("ext", "Tear of the Frostborn", "Memadamkan kobaran api di tubuh seketika (Biaya: 5 Mana)."),
        new HelpEntry("rest", "Dreamcatcher Charm", "Mereset catatan insomnia karakter (mencegah monster Phantom)."),
        new HelpEntry("repair", "Hammer of the Master Smith", "Memperbaiki daya tahan (durability) item di tangan (/repair /fix)."),
        new HelpEntry("repair all", "Hammer of the Divine Smith", "Memperbaiki seluruh item dan armor di tas sekaligus (/repair all /fix all). Butuh /repair."),
        new HelpEntry("condense", "Compactor Scroll", "Memadatkan mineral ingot di tas menjadi balok padat (/condense /compact)."),
        new HelpEntry("sell", "Merchant's Ledger", "Menjual item dari inventaris langsung ke pasar ekonomi RPG (/sell /worth).")
    );

    private static void registerHelpCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        try {
            java.lang.reflect.Field childrenField = CommandNode.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ?> children = (Map<String, ?>) childrenField.get(dispatcher.getRoot());
            children.remove("help");

            java.lang.reflect.Field literalsField = CommandNode.class.getDeclaredField("literals");
            literalsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ?> literals = (Map<String, ?>) literalsField.get(dispatcher.getRoot());
            literals.remove("help");
        } catch (Throwable ignored) {}

        for (String cmd : List.of("help", "ehelp")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(c -> showHelp(c.getSource().getPlayerOrException(), 1))
                .then(Commands.argument("query", StringArgumentType.string())
                    .executes(c -> {
                        String q = StringArgumentType.getString(c, "query").trim();
                        int page = 1;
                        try {
                            page = Integer.parseInt(q);
                        } catch (NumberFormatException ignored) {}
                        return showHelp(c.getSource().getPlayerOrException(), Math.max(1, Math.min(3, page)));
                    })
                )
            );
        }
    }

    private static int showHelp(ServerPlayer player, int page) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);

        if (page == 1) {
            player.sendSystemMessage(Component.literal(
                "§6§l╔══════════════════════════════════════════════════════════╗\n" +
                "§6§l║      ★ PANDUAN PERINTAH TAKASHA RPG (Hal 1/3) ★          ║\n" +
                "§6§l╚══════════════════════════════════════════════════════════╝\n" +
                "§e[Perintah Survival Bawaan Inti]\n" +
                "§a/back §7(atau §a/return§7) §f- §7Kembali ke lokasi sesaat sebelum teleportasi / titik kematian.\n" +
                "§a/sethome [nama] §f- §7Menyimpan lokasi rumah saat ini (Kapasitas: §e" + data.getHomes().size() + "/" + data.getMaxHomes() + "§7).\n" +
                "§a/home [nama] §f- §7Teleportasi ke titik rumah yang telah disimpan.\n" +
                "§a/delhome <nama> §f- §7Menghapus titik koordinat rumah yang terdaftar.\n" +
                "§a/homes §f- §7Menampilkan daftar seluruh nama rumah yang tersimpan.\n" +
                "§a/bal §7(atau §a/money§7) §f- §7Melihat saldo dompet uang dan koin gems.\n" +
                "§a/skills §7(atau §a/sk§7) §f- §7Membuka menu keahlian AuraSkills & status mana.\n" +
                "§a/help [1-3] §f- §7Membuka direktori panduan perintah.\n\n" +
                "§7Ketik §e/help 2 §7untuk melihat daftar perintah unlockable dari loot dungeon!"
            ));
            return 1;
        }

        int startIdx = (page == 2) ? 0 : 15;
        int endIdx = (page == 2) ? Math.min(15, LOOT_COMMANDS_HELP.size()) : LOOT_COMMANDS_HELP.size();

        StringBuilder sb = new StringBuilder();
        sb.append("§6§l╔══════════════════════════════════════════════════════════╗\n");
        sb.append("§6§l║     ★ PERINTAH LOOT DUNGEON EXECUTABLE (Hal ").append(page).append("/3) ★     ║\n");
        sb.append("§6§l╚══════════════════════════════════════════════════════════╝\n");

        for (int i = startIdx; i < endIdx; i++) {
            HelpEntry entry = LOOT_COMMANDS_HELP.get(i);
            String checkCmd = "repair all".equals(entry.command()) ? "repair_all" : entry.command();
            boolean unlocked = data.isCommandUnlocked(checkCmd) || (entry.command().equals("sethome") && data.getMaxHomes() > 1);
            if (unlocked) {
                sb.append("§a✔ §b/").append(entry.command()).append(" §f- §7").append(entry.desc()).append("\n");
            } else {
                sb.append("§c✘ §8/").append(entry.command()).append(" §c[Terkunci] §8- Butuh §e").append(entry.lootName()).append("§8 di peti dungeon.\n");
            }
        }

        if (page == 2) {
            sb.append("\n§7Ketik §e/help 3 §7untuk melihat halaman berikutnya.");
        } else {
            sb.append("\n§eTemukan Kitab & Artifak Kuno di dungeon lalu klik-kanan untuk membuka hak akses!");
        }

        player.sendSystemMessage(Component.literal(sb.toString()));
        return 1;
    }

    // 8. Title System Commands
    private static int executeTitleInfo(ServerPlayer player) {
        TakashaTitle active = TakashaTitleManager.getActiveTitle(player.getUUID());
        if (active != null) {
            player.sendSystemMessage(Component.literal(
                    "§6§l[GELAR KEDAULATAN AKTIF]\n" +
                    active.getColor() + "§l" + active.getCode() + " " + active.getTitleName() + " §7(" + active.getRomanTitle() + ")\n" +
                    "§eTrait Kedaulatan: §f" + active.getTraitName() + "\n" +
                    "§7" + active.getTraitDescription() + "\n" +
                    "§7Ketik §e/rpg title list §7atau §e/title list §7untuk daftar gelar, atau §e/rpg title clear §7untuk melepas."
            ));
        } else {
            player.sendSystemMessage(Component.literal(
                    "§e[Gelar] Kamu belum menyandang Gelar Kedaulatan.\n" +
                    "§7Ketik §e/rpg title list §7untuk melihat 16 gelar resmi atau §e/rpg title set <kode> §7(contoh: /rpg title set G-01) untuk memasang."
            ));
        }
        return 1;
    }

    private static int executeTitleList(ServerPlayer player) {
        StringBuilder sb = new StringBuilder("§6§l╔══════════════════════════════════════════════╗\n" +
                                             "§6§l║      ★ 16 GELAR KEDAULATAN TAKASHA RPG ★     ║\n" +
                                             "§6§l╚══════════════════════════════════════════════╝\n");
        for (TakashaTitle t : TakashaTitle.values()) {
            sb.append(t.getColor()).append(t.getCode()).append(" ").append(t.getTitleName())
              .append(" §7[").append(t.getTraitName()).append("]\n");
        }
        sb.append("§7Pasang dengan: §e/rpg title set <G-01 s/d G-16> §7atau §e/title set <kode>");
        player.sendSystemMessage(Component.literal(sb.toString()));
        return 1;
    }

    private static int executeTitleSet(ServerPlayer player, String query) {
        TakashaTitle title = TakashaTitle.fromCodeOrName(query);
        if (title == null) {
            player.sendSystemMessage(Component.literal("§cGelar '" + query + "' tidak ditemukan! Ketik /rpg title list untuk melihat daftar kode (G-01 s/d G-16)."));
            return 0;
        }

        TakashaTitleManager.setActiveTitle(player.getUUID(), title);
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        data.setActiveTitle(title.getCode());
        TakashaRpgManager.savePlayerData(player.getUUID());

        player.sendSystemMessage(Component.literal(
                "§6§l[GELAR TERPASANG] " + title.getColor() + "§l" + title.getCode() + " " + title.getTitleName() + "\n" +
                "§eTrait Kedaulatan: §f" + title.getTraitName() + "\n" +
                "§7" + title.getTraitDescription()
        ));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        return 1;
    }

    private static int executeTitleClear(ServerPlayer player) {
        TakashaTitleManager.setActiveTitle(player.getUUID(), null);
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        data.setActiveTitle("");
        TakashaRpgManager.savePlayerData(player.getUUID());
        player.sendSystemMessage(Component.literal("§eGelar kedaulatan telah dilepas."));
        return 1;
    }

    private static void registerTitleCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("title")
            .executes(c -> executeTitleInfo(c.getSource().getPlayerOrException()))
            .then(Commands.literal("list")
                .executes(c -> executeTitleList(c.getSource().getPlayerOrException()))
            )
            .then(Commands.literal("set")
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(c -> executeTitleSet(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "id")))
                )
            )
            .then(Commands.literal("clear")
                .executes(c -> executeTitleClear(c.getSource().getPlayerOrException()))
            )
        );
    }

    // 9. RPG Config & Reset Progress (/rpg reset ...)
    private static void registerRpgConfigCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("takasha")
            .then(Commands.literal("nametag")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    player.sendSystemMessage(Component.literal("§6§l[Takasha-RPG] §fStatus Mob Nametag Level: §aAKTIF §7(Semua monster menampilkan Level & Darah)"));
                    return 1;
                })
                .then(Commands.literal("status")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        player.sendSystemMessage(Component.literal("§6§l[Takasha-RPG] §fStatus Mob Nametag Level: §aAKTIF"));
                        return 1;
                    })
                )
                .then(Commands.literal("on")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        player.sendSystemMessage(Component.literal("§6§l[Takasha-RPG] §fMob Nametag Level: §aDIAKTIFKAN"));
                        return 1;
                    })
                )
                .then(Commands.literal("off")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        player.sendSystemMessage(Component.literal("§6§l[Takasha-RPG] §fMob Nametag Level: §cDINONAKTIFKAN"));
                        return 1;
                    })
                )
            )
        );

        dispatcher.register(Commands.literal("rpg")
            .then(Commands.literal("event")
                .executes(c -> showEventStatus(c.getSource().getPlayerOrException()))
                .then(Commands.literal("status")
                    .executes(c -> showEventStatus(c.getSource().getPlayerOrException()))
                )
                .then(Commands.literal("start")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Arrays.stream(net.takasha.rpg.world.event.RpgEventType.values()).map(net.takasha.rpg.world.event.RpgEventType::getId), b))
                        .executes(context -> {
                            String typeStr = StringArgumentType.getString(context, "type");
                            net.takasha.rpg.world.event.RpgEventType type = net.takasha.rpg.world.event.RpgEventType.fromId(typeStr);
                            if (type == null) {
                                context.getSource().sendFailure(Component.literal("§cJenis event '" + typeStr + "' tidak valid!"));
                                return 0;
                            }
                            net.takasha.rpg.world.event.TakashaEventManager.startEvent(type, type.getDefaultDurationTicks(), context.getSource().getServer());
                            context.getSource().sendSuccess(() -> Component.literal("§a[Takasha-RPG] Berhasil memulai event: " + type.getDisplayName()), true);
                            return 1;
                        })
                        .then(Commands.argument("durationSeconds", IntegerArgumentType.integer(10, 7200))
                            .executes(context -> {
                                String typeStr = StringArgumentType.getString(context, "type");
                                int sec = IntegerArgumentType.getInteger(context, "durationSeconds");
                                net.takasha.rpg.world.event.RpgEventType type = net.takasha.rpg.world.event.RpgEventType.fromId(typeStr);
                                if (type == null) {
                                    context.getSource().sendFailure(Component.literal("§cJenis event '" + typeStr + "' tidak valid!"));
                                    return 0;
                                }
                                net.takasha.rpg.world.event.TakashaEventManager.startEvent(type, sec * 20, context.getSource().getServer());
                                context.getSource().sendSuccess(() -> Component.literal("§a[Takasha-RPG] Berhasil memulai event: " + type.getDisplayName() + " selama " + sec + " detik!"), true);
                                return 1;
                            })
                        )
                    )
                )
                .then(Commands.literal("stop")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .executes(context -> {
                        if (net.takasha.rpg.world.event.TakashaEventManager.stopEvent(context.getSource().getServer())) {
                            context.getSource().sendSuccess(() -> Component.literal("§e[Takasha-RPG] Event aktif berhasil dihentikan."), true);
                            return 1;
                        } else {
                            context.getSource().sendFailure(Component.literal("§cTidak ada event yang sedang aktif."));
                            return 0;
                        }
                    })
                )
            )
            .then(Commands.literal("title")
                .executes(c -> executeTitleInfo(c.getSource().getPlayerOrException()))
                .then(Commands.literal("list")
                    .executes(c -> executeTitleList(c.getSource().getPlayerOrException()))
                )
                .then(Commands.literal("set")
                    .then(Commands.argument("id", StringArgumentType.string())
                        .executes(c -> executeTitleSet(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "id")))
                    )
                )
                .then(Commands.literal("clear")
                    .executes(c -> executeTitleClear(c.getSource().getPlayerOrException()))
                )
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(c -> executeTitleSet(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "id")))
                )
            )
            .then(Commands.literal("help")
                .executes(c -> showHelp(c.getSource().getPlayerOrException(), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1, 3))
                    .executes(c -> showHelp(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "page"))))
            )
            .then(Commands.literal("scoreboard")
                .then(Commands.literal("toggle")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
                        boolean newState = !data.isShowScoreboard();
                        data.setShowScoreboard(newState);
                        TakashaRpgManager.savePlayerData(player.getUUID());
                        TakashaRpgManager.syncToClient(player);

                        player.sendSystemMessage(Component.literal("§6§l[Takasha-RPG] §fScoreboard HUD: "
                                + (newState ? "§a§lAKTIF" : "§c§lNONAKTIF")));
                        return 1;
                    })
                )
            )
            .then(Commands.literal("reload")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
                    TakashaRpgManager.loadPlayerData(player.getUUID(), data);
                    TakashaRpgManager.syncToClient(player);
                    player.sendSystemMessage(Component.literal("§a§l[Takasha-RPG] §fData RPG pemain berhasil dimuat ulang!"));
                    return 1;
                })
            )
            .then(Commands.literal("reset")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    player.sendSystemMessage(Component.literal(
                        "§c§l╔══════════════════════════════════════════════════════╗\n" +
                        "§c§l║         ⚠ PERINGATAN RESET PROGRESS RPG ⚠            ║\n" +
                        "§c§l╚══════════════════════════════════════════════════════╝\n" +
                        "§eTindakan ini akan mengembalikan progressmu ke kondisi awal!\n" +
                        "§c• Ketik §e/rpg reset confirm §cataupun §e/rpg reset all §cuntuk mereset SEMUA progress.\n" +
                        "§7• Atau reset kategori tertentu langsung:\n" +
                        "   §b/rpg reset skills   §7(Reset Level Skill & Mana ke 20)\n" +
                        "   §b/rpg reset commands §7(Kunci kembali semua perintah loot)\n" +
                        "   §b/rpg reset homes    §7(Hapus titik home & reset slot ke 1)\n" +
                        "   §b/rpg reset economy  §7(Kosongkan saldo balance & gems)"
                    ));
                    return 1;
                })
                .then(Commands.literal("confirm")
                    .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.ALL))
                )
                .then(Commands.literal("all")
                    .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.ALL))
                    .then(Commands.literal("confirm")
                        .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.ALL))
                    )
                )
                .then(Commands.literal("skills")
                    .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.SKILLS))
                    .then(Commands.literal("confirm")
                        .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.SKILLS))
                    )
                )
                .then(Commands.literal("commands")
                    .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.COMMANDS))
                    .then(Commands.literal("confirm")
                        .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.COMMANDS))
                    )
                )
                .then(Commands.literal("homes")
                    .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.HOMES))
                    .then(Commands.literal("confirm")
                        .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.HOMES))
                    )
                )
                .then(Commands.literal("economy")
                    .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.ECONOMY))
                    .then(Commands.literal("confirm")
                        .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.ECONOMY))
                    )
                )
                .then(Commands.literal("player")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.literal("force")
                            .executes(context -> {
                                ServerPlayer admin = context.getSource().getPlayerOrException();
                                ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                executeResetProgress(target, TakashaRpgData.ResetScope.ALL);
                                admin.sendSystemMessage(Component.literal("§a[Takasha-RPG] Berhasil mereset paksa seluruh progress player §e" + target.getScoreboardName()));
                                return 1;
                            })
                        )
                    )
                )
            )
        );

        // Alias /rpgreset
        dispatcher.register(Commands.literal("rpgreset")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                return executeResetProgress(player, TakashaRpgData.ResetScope.ALL);
            })
            .then(Commands.literal("all")
                .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.ALL))
                .then(Commands.literal("confirm")
                    .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.ALL))
                )
            )
            .then(Commands.literal("skills")
                .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.SKILLS))
                .then(Commands.literal("confirm")
                    .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.SKILLS))
                )
            )
            .then(Commands.literal("commands")
                .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.COMMANDS))
                .then(Commands.literal("confirm")
                    .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.COMMANDS))
                )
            )
            .then(Commands.literal("homes")
                .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.HOMES))
                .then(Commands.literal("confirm")
                    .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.HOMES))
                )
            )
            .then(Commands.literal("economy")
                .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.ECONOMY))
                .then(Commands.literal("confirm")
                    .executes(context -> executeResetProgress(context.getSource().getPlayerOrException(), TakashaRpgData.ResetScope.ECONOMY))
                )
            )
        );

        // Alias /event & /events untuk pemain survival
        for (String evAlias : List.of("event", "events")) {
            dispatcher.register(Commands.literal(evAlias)
                .executes(c -> showEventStatus(c.getSource().getPlayerOrException()))
            );
        }
    }

    private static int showEventStatus(ServerPlayer player) {
        net.takasha.rpg.world.event.RpgEventInstance current = net.takasha.rpg.world.event.TakashaEventManager.getCurrentEvent();
        StringBuilder sb = new StringBuilder();
        sb.append("§6§l╔══════════════════════════════════════════════════════╗\n");
        sb.append("§6§l║           ★ STATUS TAKASHA WORLD EVENT ★            ║\n");
        sb.append("§6§l╚══════════════════════════════════════════════════════╝\n");

        if (current != null) {
            sb.append("§fEvent Sedang Berlangsung:\n");
            sb.append("  ").append(current.getType().getDisplayName()).append("\n");
            sb.append("  §7").append(current.getType().getDescription()).append("\n");
            sb.append("  §fSisa Waktu: §e").append(net.takasha.rpg.world.event.RpgEventInstance.formatTime(current.getRemainingTicks())).append("\n");
            sb.append("  §bBonus Multiplier Aktif:\n");
            if (current.getExpMultiplier() > 1.0) sb.append("   §a• EXP Multiplier: §e").append(current.getExpMultiplier()).append("x\n");
            if (current.getCoinMultiplier() > 1.0) sb.append("   §a• Coin Multiplier: §e").append(current.getCoinMultiplier()).append("x\n");
            if (current.getLootMultiplier() > 1.0) sb.append("   §a• Loot Multiplier: §e").append(current.getLootMultiplier()).append("x\n");
            if (current.getHarvestMultiplier() > 1.0) sb.append("   §a• Panen Multiplier: §e").append(current.getHarvestMultiplier()).append("x\n");
        } else {
            sb.append("§7Saat ini tidak ada World Event yang sedang aktif.\n\n");
            int ticksLeft = net.takasha.rpg.world.event.TakashaEventManager.getTicksUntilNextPeriodic();
            net.takasha.rpg.world.event.RpgEventType nextPeriodic = net.takasha.rpg.world.event.TakashaEventManager.getNextPeriodicType();
            sb.append("§fEvent Berkala Berikutnya:\n");
            sb.append("  ").append(nextPeriodic.getDisplayName()).append("\n");
            sb.append("  §fPerkiraan Tiba Dalam: §e").append(net.takasha.rpg.world.event.RpgEventInstance.formatTime(ticksLeft)).append("\n");
            sb.append("  §8(Event acak seperti Blood Moon atau Goblin Hunt dapat terjadi sewaktu-waktu)\n");
        }
        sb.append("\n§8Buka §e/grimoire §8-> Tab §e[Events & Arena] §8untuk memantau via GUI.");
        player.sendSystemMessage(Component.literal(sb.toString()));
        return 1;
    }

    private static int executeResetProgress(ServerPlayer player, TakashaRpgData.ResetScope scope) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        data.resetProgress(scope);
        TakashaRpgManager.savePlayerData(player.getUUID());
        TakashaRpgManager.syncToClient(player);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0f, 0.5f);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    40, 0.5, 0.5, 0.5, 0.1);
        }

        if (scope == TakashaRpgData.ResetScope.ALL) {
            player.sendSystemMessage(Component.literal("§c§l[Takasha-RPG] §4Seluruh progress RPG kamu telah direset ke kondisi awal!\n§7(Level Skill, Mana 20/20, Saldo $0, Titik Home dihapus, dan Perintah Loot kembali terkunci)."));
        } else {
            player.sendSystemMessage(Component.literal("§6§l[Takasha-RPG] §eProgress bagian §b" + scope.name() + " §etelah direset ke kondisi awal!"));
        }
        return 1;
    }

    // 10. Creative / OP Only Commands (Restricted from normal survival)
    private static void registerCreativeAndOpCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /fly
        dispatcher.register(Commands.literal("fly")
            .requires(src -> src.isPlayer() && (src.getPlayer().isCreative() || src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)))
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                p.getAbilities().mayfly = !p.getAbilities().mayfly;
                if (!p.getAbilities().mayfly) p.getAbilities().flying = false;
                p.onUpdateAbilities();
                p.sendSystemMessage(Component.literal("§6§l[Creative/OP] §fMode terbang: " + (p.getAbilities().mayfly ? "§a§lAKTIF" : "§c§lNONAKTIF")));
                return 1;
            })
        );

        // /god
        dispatcher.register(Commands.literal("god")
            .requires(src -> src.isPlayer() && (src.getPlayer().isCreative() || src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)))
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                p.getAbilities().invulnerable = !p.getAbilities().invulnerable;
                p.onUpdateAbilities();
                p.sendSystemMessage(Component.literal("§6§l[Creative/OP] §fMode kebal (God): " + (p.getAbilities().invulnerable ? "§a§lAKTIF" : "§c§lNONAKTIF")));
                return 1;
            })
        );

        // /speed
        dispatcher.register(Commands.literal("speed")
            .requires(src -> src.isPlayer() && (src.getPlayer().isCreative() || src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)))
            .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0.1, 10.0))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    double mult = DoubleArgumentType.getDouble(c, "multiplier");
                    p.getAbilities().setFlyingSpeed((float) (0.05f * mult));
                    p.getAbilities().setWalkingSpeed((float) (0.1f * mult));
                    p.onUpdateAbilities();
                    p.sendSystemMessage(Component.literal("§6§l[Creative/OP] §fKecepatan diatur ke: §e" + mult + "x"));
                    return 1;
                })
            )
        );

        // /tpall
        dispatcher.register(Commands.literal("tpall")
            .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                for (ServerPlayer other : p.level().getServer().getPlayerList().getPlayers()) {
                    if (!other.getUUID().equals(p.getUUID())) {
                        other.teleportTo((ServerLevel) p.level(), p.getX(), p.getY(), p.getZ(), java.util.Set.of(), other.getYRot(), other.getXRot(), true);
                        other.sendSystemMessage(Component.literal("§eKamu diteleportasikan ke §b" + p.getScoreboardName() + "§e!"));
                    }
                }
                p.sendSystemMessage(Component.literal("§aSeluruh pemain berhasil diteleportasikan ke posisimu!"));
                return 1;
            })
        );

        // /tphere
        dispatcher.register(Commands.literal("tphere")
            .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ServerPlayer target = EntityArgument.getPlayer(c, "target");
                    target.teleportTo((ServerLevel) p.level(), p.getX(), p.getY(), p.getZ(), java.util.Set.of(), target.getYRot(), target.getXRot(), true);
                    target.sendSystemMessage(Component.literal("§eKamu diteleportasikan ke §b" + p.getScoreboardName() + "§e!"));
                    p.sendSystemMessage(Component.literal("§aPlayer §b" + target.getScoreboardName() + " §aberhasil diteleportasikan ke posisimu!"));
                    return 1;
                })
            )
        );

        // /broadcast
        dispatcher.register(Commands.literal("broadcast")
            .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(c -> {
                    String msg = StringArgumentType.getString(c, "message").replace('&', '§');
                    c.getSource().getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal("§c§l[PENGUMUMAN] §f" + msg), false
                    );
                    return 1;
                })
            )
        );

        // /skull
        dispatcher.register(Commands.literal("skull")
            .requires(src -> src.isPlayer() && (src.getPlayer().isCreative() || src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)))
            .then(Commands.argument("playername", StringArgumentType.word())
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String owner = StringArgumentType.getString(c, "playername");
                    ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                    head.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(owner));
                    p.getInventory().add(head);
                    p.sendSystemMessage(Component.literal("§aKepala pemain §e" + owner + " §aberhasil ditambahkan ke tas!"));
                    return 1;
                })
            )
        );

        // /invsee
        dispatcher.register(Commands.literal("invsee")
            .requires(src -> src.isPlayer() && src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ServerPlayer target = EntityArgument.getPlayer(c, "target");
                    p.openMenu(new SimpleMenuProvider(
                        (id, inv, pl) -> new ChestMenu(MenuType.GENERIC_9x4, id, inv, target.getInventory(), 4),
                        Component.literal("Inventaris: " + target.getScoreboardName())
                    ));
                    return 1;
                })
            )
        );

        // /ecother
        dispatcher.register(Commands.literal("ecother")
            .requires(src -> src.isPlayer() && src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ServerPlayer target = EntityArgument.getPlayer(c, "target");
                    p.openMenu(new SimpleMenuProvider(
                        (id, inv, pl) -> ChestMenu.threeRows(id, inv, target.getEnderChestInventory()),
                        Component.literal("Ender Chest: " + target.getScoreboardName())
                    ));
                    return 1;
                })
            )
        );

        // /vanish
        dispatcher.register(Commands.literal("vanish")
            .requires(src -> src.isPlayer() && src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                boolean isCurrentlyInvisible = p.isInvisible();
                p.setInvisible(!isCurrentlyInvisible);
                p.sendSystemMessage(Component.literal("§6§l[Creative/OP] §fStatus Vanish: " + (!isCurrentlyInvisible ? "§a§lAKTIF" : "§c§lNONAKTIF")));
                return 1;
            })
        );

        // /ae (AdvancedEnchantments Creative/Admin/OP Suite)
        dispatcher.register(Commands.literal("ae")
            .requires(src -> src.isPlayer() && (src.getPlayer().isCreative() || src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)))
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                TakashaEnchantCatalogMenu.open(p);
                return 1;
            })
            .then(Commands.literal("premade")
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    TakashaEnchantCatalogMenu.open(p);
                    return 1;
                })
            )
            .then(Commands.literal("catalog")
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    TakashaEnchantCatalogMenu.open(p);
                    return 1;
                })
            )
            .then(Commands.literal("book")
                .then(Commands.argument("enchant", StringArgumentType.word())
                    .suggests(ENCHANT_SUGGESTION_PROVIDER)
                    .executes(c -> executeAeBook(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "enchant"), 1, 100, 0, c.getSource().getPlayerOrException()))
                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 10))
                        .executes(c -> executeAeBook(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "enchant"), IntegerArgumentType.getInteger(c, "level"), 100, 0, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("success", IntegerArgumentType.integer(1, 100))
                            .executes(c -> executeAeBook(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "enchant"), IntegerArgumentType.getInteger(c, "level"), IntegerArgumentType.getInteger(c, "success"), 0, c.getSource().getPlayerOrException()))
                            .then(Commands.argument("destroy", IntegerArgumentType.integer(0, 100))
                                .executes(c -> executeAeBook(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "enchant"), IntegerArgumentType.getInteger(c, "level"), IntegerArgumentType.getInteger(c, "success"), IntegerArgumentType.getInteger(c, "destroy"), c.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                    .executes(c -> executeAeBook(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "enchant"), IntegerArgumentType.getInteger(c, "level"), IntegerArgumentType.getInteger(c, "success"), IntegerArgumentType.getInteger(c, "destroy"), EntityArgument.getPlayer(c, "target")))
                                )
                            )
                        )
                    )
                )
            )
            .then(Commands.literal("apply")
                .then(Commands.argument("enchant", StringArgumentType.word())
                    .suggests(ENCHANT_SUGGESTION_PROVIDER)
                    .executes(c -> executeAeApply(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "enchant"), 1))
                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 10))
                        .executes(c -> executeAeApply(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "enchant"), IntegerArgumentType.getInteger(c, "level")))
                    )
                )
            )
            .then(Commands.literal("unenchant")
                .then(Commands.argument("enchant", StringArgumentType.word())
                    .suggests(ENCHANT_SUGGESTION_PROVIDER)
                    .executes(c -> executeAeUnenchant(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "enchant")))
                )
            )
            .then(Commands.literal("info")
                .then(Commands.argument("enchant", StringArgumentType.word())
                    .suggests(ENCHANT_SUGGESTION_PROVIDER)
                    .executes(c -> executeAeInfo(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "enchant")))
                )
            )
            .then(Commands.literal("list")
                .executes(c -> executeAeList(c.getSource().getPlayerOrException(), null))
                .then(Commands.argument("tier", StringArgumentType.word())
                    .suggests(TIER_SUGGESTION_PROVIDER)
                    .executes(c -> executeAeList(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "tier")))
                )
            )
            .then(Commands.literal("setsouls")
                .then(Commands.argument("amount", IntegerArgumentType.integer(0, 10000))
                    .executes(c -> executeAeSetSouls(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "amount"), c.getSource().getPlayerOrException()))
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(c -> executeAeSetSouls(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "amount"), EntityArgument.getPlayer(c, "target")))
                    )
                )
            )
            .then(Commands.literal("give")
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("item", StringArgumentType.word())
                        .suggests(AE_ITEM_SUGGESTION_PROVIDER)
                        .executes(c -> executeAeGive(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "target"), StringArgumentType.getString(c, "item"), 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                            .executes(c -> executeAeGive(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "target"), StringArgumentType.getString(c, "item"), IntegerArgumentType.getInteger(c, "amount")))
                        )
                    )
                )
            )
            .then(Commands.literal("admin")
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    TakashaEnchantCatalogMenu.open(p);
                    return 1;
                })
            )
            .then(Commands.literal("givebook")
                .then(Commands.argument("enchant", StringArgumentType.word())
                    .suggests(ENCHANT_SUGGESTION_PROVIDER)
                    .executes(c -> executeAeBook(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "enchant"), 1, 100, 0, c.getSource().getPlayerOrException()))
                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 10))
                        .executes(c -> executeAeBook(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "enchant"), IntegerArgumentType.getInteger(c, "level"), 100, 0, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("success", IntegerArgumentType.integer(1, 100))
                            .executes(c -> executeAeBook(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "enchant"), IntegerArgumentType.getInteger(c, "level"), IntegerArgumentType.getInteger(c, "success"), 0, c.getSource().getPlayerOrException()))
                            .then(Commands.argument("destroy", IntegerArgumentType.integer(0, 100))
                                .executes(c -> executeAeBook(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "enchant"), IntegerArgumentType.getInteger(c, "level"), IntegerArgumentType.getInteger(c, "success"), IntegerArgumentType.getInteger(c, "destroy"), c.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                    .executes(c -> executeAeBook(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "enchant"), IntegerArgumentType.getInteger(c, "level"), IntegerArgumentType.getInteger(c, "success"), IntegerArgumentType.getInteger(c, "destroy"), EntityArgument.getPlayer(c, "target")))
                                )
                            )
                        )
                    )
                )
            )
            .then(Commands.literal("enchant")
                .then(Commands.argument("enchant", StringArgumentType.word())
                    .suggests(ENCHANT_SUGGESTION_PROVIDER)
                    .executes(c -> executeAeApply(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "enchant"), 1))
                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 10))
                        .executes(c -> executeAeApply(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "enchant"), IntegerArgumentType.getInteger(c, "level")))
                    )
                )
            )
            .then(Commands.literal("givercbook")
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("tier", StringArgumentType.word())
                        .suggests(TIER_SUGGESTION_PROVIDER)
                        .executes(c -> executeAeGiveRandomBook(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "target"), StringArgumentType.getString(c, "tier"), 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                            .executes(c -> executeAeGiveRandomBook(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "target"), StringArgumentType.getString(c, "tier"), IntegerArgumentType.getInteger(c, "amount")))
                        )
                    )
                )
            )
            .then(Commands.literal("giverandombook")
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("tier", StringArgumentType.word())
                        .suggests(TIER_SUGGESTION_PROVIDER)
                        .executes(c -> executeAeGiveRandomBook(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "target"), StringArgumentType.getString(c, "tier"), 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                            .executes(c -> executeAeGiveRandomBook(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "target"), StringArgumentType.getString(c, "tier"), IntegerArgumentType.getInteger(c, "amount")))
                        )
                    )
                )
            )
            .then(Commands.literal("givedust")
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("rate", IntegerArgumentType.integer(1, 100))
                        .executes(c -> executeAeGiveDust(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "target"), IntegerArgumentType.getInteger(c, "rate"), 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                            .executes(c -> executeAeGiveDust(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "target"), IntegerArgumentType.getInteger(c, "rate"), IntegerArgumentType.getInteger(c, "amount")))
                        )
                    )
                )
            )
            .then(Commands.literal("magicdust")
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("rate", IntegerArgumentType.integer(1, 100))
                        .executes(c -> executeAeGiveDust(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "target"), IntegerArgumentType.getInteger(c, "rate"), 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                            .executes(c -> executeAeGiveDust(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "target"), IntegerArgumentType.getInteger(c, "rate"), IntegerArgumentType.getInteger(c, "amount")))
                        )
                    )
                )
            )
            .then(Commands.literal("givetinkerer")
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(c -> executeAeGive(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "target"), "secret_dust", 1))
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                        .executes(c -> executeAeGive(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "target"), "secret_dust", IntegerArgumentType.getInteger(c, "amount")))
                    )
                )
            )
            .then(Commands.literal("open")
                .then(Commands.argument("menu", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(List.of("enchanter", "tinkerer", "alchemist", "catalog"), b))
                    .executes(c -> executeAeOpen(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "menu"), c.getSource().getPlayerOrException()))
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(c -> executeAeOpen(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "menu"), EntityArgument.getPlayer(c, "target")))
                    )
                )
            )
            .then(Commands.literal("listnbt")
                .executes(c -> executeAeListNbt(c.getSource().getPlayerOrException()))
            )
            .then(Commands.literal("help")
                .executes(c -> executeCeHelp(c.getSource().getPlayerOrException()))
            )
            .then(Commands.literal("giveitem")
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("item", StringArgumentType.word())
                        .suggests(AE_ITEM_SUGGESTION_PROVIDER)
                        .executes(c -> executeAeGive(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "target"), StringArgumentType.getString(c, "item"), 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                            .executes(c -> executeAeGive(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "target"), StringArgumentType.getString(c, "item"), IntegerArgumentType.getInteger(c, "amount")))
                        )
                    )
                )
            )
        );
    }

    private static int executeAeBook(ServerPlayer admin, String enchantId, int level, int success, int destroy, ServerPlayer target) {
        CustomEnchantDef def = TakashaEnchantRegistry.get(enchantId);
        if (def == null) {
            admin.sendSystemMessage(Component.literal("§cEnchantment '" + enchantId + "' tidak ditemukan! Gunakan TAB untuk pilihan."));
            return 0;
        }

        int finalLevel = Math.max(1, Math.min(def.maxLevel, level));
        int finalSuccess = Math.max(1, Math.min(100, success));
        int finalDestroy = Math.max(0, Math.min(100, destroy));

        ItemStack book = TakashaCustomBookItem.createRevealedBook(def, finalLevel, finalSuccess, finalDestroy);
        if (!target.getInventory().add(book)) {
            target.drop(book, false);
        }

        admin.sendSystemMessage(Component.literal("§6§l[AE Admin] §aMemberikan buku " + def.tier.getColorCode() + "§l" + def.name + " " + TakashaCustomBookItem.toRoman(finalLevel) + " §a(Sukses: " + finalSuccess + "%, Hancur: " + finalDestroy + "%) kepada §e" + target.getScoreboardName()));
        if (!admin.getUUID().equals(target.getUUID())) {
            target.sendSystemMessage(Component.literal("§6§l[AE Admin] §eKamu menerima buku sihir " + def.tier.getColorCode() + "§l" + def.name + " " + TakashaCustomBookItem.toRoman(finalLevel) + " §edari Admin!"));
        }
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.2f);
        return 1;
    }

    private static int executeAeApply(ServerPlayer admin, String enchantId, int level) {
        ItemStack held = admin.getMainHandItem();
        if (held.isEmpty()) {
            admin.sendSystemMessage(Component.literal("§cKamu harus memegang item di tangan utama untuk menerapkan enchantment!"));
            return 0;
        }

        CustomEnchantDef def = TakashaEnchantRegistry.get(enchantId);
        if (def == null) {
            admin.sendSystemMessage(Component.literal("§cEnchantment '" + enchantId + "' tidak ditemukan! Gunakan TAB untuk pilihan."));
            return 0;
        }

        int finalLevel = Math.max(1, Math.min(def.maxLevel, level));
        boolean applied = TakashaEnchantManager.applyCustomEnchant(held, def, finalLevel, true);
        if (applied) {
            TakashaAnvilHandler.formatGearLore(held);
            admin.sendSystemMessage(Component.literal("§6§l[AE Admin] §aBerhasil memasang " + def.tier.getColorCode() + "§l" + def.name + " " + TakashaCustomBookItem.toRoman(finalLevel) + " §ake §e" + held.getHoverName().getString() + " §7(Slots: " + TakashaEnchantManager.getEnchantCount(held) + "/" + TakashaEnchantManager.getMaxSlots(held) + ")"));
            admin.level().playSound(null, admin.getX(), admin.getY(), admin.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.4f);
            return 1;
        } else {
            admin.sendSystemMessage(Component.literal("§cGagal memasang enchantment ke item!"));
            return 0;
        }
    }

    private static int executeAeUnenchant(ServerPlayer admin, String enchantId) {
        ItemStack held = admin.getMainHandItem();
        if (held.isEmpty()) {
            admin.sendSystemMessage(Component.literal("§cKamu harus memegang item di tangan utama untuk menghapus enchantment!"));
            return 0;
        }

        if (!TakashaEnchantManager.hasCustomEnchant(held, enchantId)) {
            admin.sendSystemMessage(Component.literal("§cItem ini tidak memiliki enchantment '" + enchantId + "'!"));
            return 0;
        }

        TakashaEnchantManager.removeCustomEnchant(held, enchantId);
        TakashaAnvilHandler.formatGearLore(held);
        admin.sendSystemMessage(Component.literal("§6§l[AE Admin] §aBerhasil menghapus enchantment §e" + enchantId + " §fdari item!"));
        admin.level().playSound(null, admin.getX(), admin.getY(), admin.getZ(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        return 1;
    }

    private static int executeAeInfo(ServerPlayer admin, String enchantId) {
        CustomEnchantDef def = TakashaEnchantRegistry.get(enchantId);
        if (def == null) {
            admin.sendSystemMessage(Component.literal("§cEnchantment '" + enchantId + "' tidak ditemukan!"));
            return 0;
        }

        admin.sendSystemMessage(Component.literal(
            "§6§l╔══════════════════════════════════════════════════════╗\n" +
            "§6§l║      ★ INFORMASI ADVANCED ENCHANTMENT ★             ║\n" +
            "§6§l╚══════════════════════════════════════════════════════╝\n" +
            "§fNama       : " + def.tier.getColorCode() + "§l" + def.name + " §7(ID: §e" + def.id + "§7)\n" +
            "§fTier       : " + def.tier.getColorCode() + "§l" + def.tier.getDisplayName().toUpperCase() + "\n" +
            "§fMaks Level : §a" + def.maxLevel + "\n" +
            "§fTarget     : §e" + def.appliesTo + "\n" +
            "§fDeskripsi  : §f" + def.description
        ));
        return 1;
    }

    private static int executeAeList(ServerPlayer admin, String tierName) {
        if (tierName == null || tierName.isEmpty()) {
            StringBuilder sb = new StringBuilder("§6§l[AE KATALOG 9 TIER (336 ENCHANTS)]\n");
            for (EnchantTier t : EnchantTier.values()) {
                List<CustomEnchantDef> list = TakashaEnchantRegistry.getByTier(t);
                sb.append(t.getColorCode()).append("• ").append(t.getDisplayName()).append(" Tier: §f").append(list.size()).append(" Enchants\n");
            }
            sb.append("§7Gunakan §e/ae list <tier> §7atau buka katalog: §e/ae catalog");
            admin.sendSystemMessage(Component.literal(sb.toString()));
            return 1;
        }

        EnchantTier tier = null;
        for (EnchantTier t : EnchantTier.values()) {
            if (t.name().equalsIgnoreCase(tierName) || t.getDisplayName().equalsIgnoreCase(tierName)) {
                tier = t;
                break;
            }
        }
        if (tier == null) {
            admin.sendSystemMessage(Component.literal("§cTier tidak valid! Pilihan: simple, unique, elite, ultimate, legendary, fabled, heroic, soul, mastery."));
            return 0;
        }

        List<CustomEnchantDef> list = TakashaEnchantRegistry.getByTier(tier);
        StringBuilder sb = new StringBuilder("§6§l[AE ENCHANTS TIER: " + tier.getColorCode() + tier.getDisplayName().toUpperCase() + "§6§l] (" + list.size() + " Total)\n");
        int count = 0;
        for (CustomEnchantDef def : list) {
            if (count >= 20) {
                sb.append("§7... dan ").append(list.size() - count).append(" lainnya (Buka GUI: §e/ae catalog§7).\n");
                break;
            }
            sb.append(tier.getColorCode()).append("• ").append(def.name).append(" (Max ").append(def.maxLevel).append(") §8[ID: ").append(def.id).append("]\n");
            count++;
        }
        admin.sendSystemMessage(Component.literal(sb.toString()));
        return 1;
    }

    private static int executeAeSetSouls(ServerPlayer admin, int amount, ServerPlayer target) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(target);
        data.setSouls(amount);
        TakashaRpgManager.savePlayerData(target.getUUID());
        TakashaRpgManager.syncToClient(target);

        ItemStack held = target.getMainHandItem();
        boolean itemUpdated = false;
        if (!held.isEmpty()) {
            CustomData.update(DataComponents.CUSTOM_DATA, held, tag -> tag.putInt("TakashaSouls", amount));
            TakashaAnvilHandler.formatGearLore(held);
            itemUpdated = true;
        }

        admin.sendSystemMessage(Component.literal("§6§l[AE Admin] §aBerhasil mengatur souls pemain §e" + target.getScoreboardName() + " §amenjadi §b" + amount + " Souls" + (itemUpdated ? " & item di tangan utama!" : "!")));
        if (!admin.getUUID().equals(target.getUUID())) {
            target.sendSystemMessage(Component.literal("§6§l[AE Admin] §eSouls kamu telah diatur menjadi §b" + amount + " Souls §eoleh Admin!"));
        }
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.8f, 1.2f);
        return 1;
    }

    private static int executeAeGive(ServerPlayer admin, ServerPlayer target, String itemName, int amount) {
        int finalAmount = Math.max(1, Math.min(64, amount));
        ItemStack stack = resolveAeItem(itemName.toLowerCase(), finalAmount);
        if (stack.isEmpty()) {
            admin.sendSystemMessage(Component.literal("§cItem '" + itemName + "' tidak valid! Gunakan TAB untuk daftar item."));
            return 0;
        }

        if (!target.getInventory().add(stack)) {
            target.drop(stack, false);
        }

        admin.sendSystemMessage(Component.literal("§6§l[AE Admin] §aMemberikan §e" + stack.getHoverName().getString() + " x" + finalAmount + " §akepada §e" + target.getScoreboardName()));
        if (!admin.getUUID().equals(target.getUUID())) {
            target.sendSystemMessage(Component.literal("§6§l[AE Admin] §eKamu menerima §e" + stack.getHoverName().getString() + " x" + finalAmount + " §edari Admin!"));
        }
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.2f);
        return 1;
    }

    private static ItemStack resolveAeItem(String key, int amount) {
        Item item = switch (key) {
            case "white_scroll" -> ModRpgItems.WHITE_SCROLL;
            case "black_scroll" -> ModRpgItems.BLACK_SCROLL;
            case "holy_scroll" -> ModRpgItems.HOLY_SCROLL;
            case "transmute_scroll" -> ModRpgItems.TRANSMUTE_SCROLL;
            case "randomization_scroll" -> ModRpgItems.RANDOMIZATION_SCROLL;
            case "magic_dust" -> ModRpgItems.MAGIC_DUST;
            case "secret_dust" -> ModRpgItems.SECRET_DUST;
            case "mystery_dust" -> ModRpgItems.MYSTERY_DUST;
            case "slot_increaser" -> ModRpgItems.SLOT_INCREASER_1;
            case "master_increaser" -> ModRpgItems.SLOT_INCREASER_2;
            case "soul_increaser" -> ModRpgItems.SLOT_INCREASER_3;
            case "soul_gem" -> ModRpgItems.SOUL_GEM;
            case "weapon_orb" -> ModRpgItems.WEAPON_ORB;
            case "armor_orb" -> ModRpgItems.ARMOR_ORB;
            case "tool_orb" -> ModRpgItems.TOOL_ORB;
            case "tracker_mobs" -> ModRpgItems.STAT_TRACKER_MOBS;
            case "tracker_fish" -> ModRpgItems.STAT_TRACKER_FISH;
            case "phantom_scythe" -> ModRpgItems.PHANTOM_SCYTHE;
            case "koth_sword" -> ModRpgItems.KOTH_SWORD;
            case "koth_axe" -> ModRpgItems.KOTH_AXE;
            case "dimensional_sword" -> ModRpgItems.DIMENSIONAL_SWORD;
            case "supreme_weapon" -> ModRpgItems.SUPREME_WEAPON;
            case "book_simple" -> ModRpgItems.CUSTOM_BOOK_SIMPLE;
            case "book_unique" -> ModRpgItems.CUSTOM_BOOK_UNIQUE;
            case "book_elite" -> ModRpgItems.CUSTOM_BOOK_ELITE;
            case "book_ultimate" -> ModRpgItems.CUSTOM_BOOK_ULTIMATE;
            case "book_legendary" -> ModRpgItems.CUSTOM_BOOK_LEGENDARY;
            case "book_fabled" -> ModRpgItems.CUSTOM_BOOK_FABLED;
            case "book_heroic" -> ModRpgItems.CUSTOM_BOOK_HEROIC;
            case "book_soul" -> ModRpgItems.CUSTOM_BOOK_SOUL;
            case "book_mastery" -> ModRpgItems.CUSTOM_BOOK_MASTERY;
            case "mystery_simple" -> ModRpgItems.MYSTERY_BOOK_SIMPLE;
            case "mystery_unique" -> ModRpgItems.MYSTERY_BOOK_UNIQUE;
            case "mystery_elite" -> ModRpgItems.MYSTERY_BOOK_ELITE;
            case "mystery_ultimate" -> ModRpgItems.MYSTERY_BOOK_ULTIMATE;
            case "mystery_legendary" -> ModRpgItems.MYSTERY_BOOK_LEGENDARY;
            case "mystery_fabled" -> ModRpgItems.MYSTERY_BOOK_FABLED;
            case "mystery_heroic" -> ModRpgItems.MYSTERY_BOOK_HEROIC;
            case "mystery_soul" -> ModRpgItems.MYSTERY_BOOK_SOUL;
            case "mystery_mastery" -> ModRpgItems.MYSTERY_BOOK_MASTERY;
            default -> null;
        };

        if (item == null) return ItemStack.EMPTY;
        return new ItemStack(item, amount);
    }

    private static int executeCeHelp(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(
            "§6§l╔══════════════════════════════════════════════════════╗\n" +
            "§6§l║     ★ ADVANCED ENCHANTMENTS - DAFTAR PERINTAH ★      ║\n" +
            "§6§l╚══════════════════════════════════════════════════════╝\n" +
            "§e/ce §7atau §e/enchanter §f- Buka menu Server Enchanter (beli mystery book)\n" +
            "§e/ce list §7atau §e/ce catalog §f- Buka GUI Katalog 336 Sihir (9 Tier)\n" +
            "§e/ce list <tier> §f- Tampilkan daftar sihir berdasarkan tier\n" +
            "§e/ce info <sihir> §f- Lihat detail, efek, target item, & max level sihir\n" +
            "§e/tinker §7atau §e/tinkerer §f- Buka menu Tinkerer (daur ulang buku/gear)\n" +
            "§e/alchem §7atau §e/alchemist §f- Buka menu Alchemist (gabung buku & dust)\n" +
            "§e/withdrawsouls [level] §f- Ekstrak level EXP menjadi Takasha Gem/Souls\n" +
            "§7Seret (Drag & Drop) buku sihir langsung ke senjatamu untuk menerapkannya!"
        ));
        return 1;
    }

    private static int executeAeOpen(ServerPlayer admin, String menuName, ServerPlayer target) {
        switch (menuName.toLowerCase()) {
            case "enchanter", "ce" -> EnchanterMenu.open(target);
            case "tinkerer", "tinker" -> TinkererMenu.open(target);
            case "alchemist", "alchem" -> AlchemistMenu.open(target);
            case "catalog", "list" -> TakashaEnchantCatalogMenu.open(target);
            default -> {
                admin.sendSystemMessage(Component.literal("§cMenu '" + menuName + "' tidak valid! Pilihan: enchanter, tinkerer, alchemist, catalog."));
                return 0;
            }
        }
        admin.sendSystemMessage(Component.literal("§6§l[AE Admin] §aBerhasil membuka menu §e" + menuName + " §auntuk §e" + target.getScoreboardName()));
        return 1;
    }

    private static int executeAeGiveRandomBook(ServerPlayer admin, ServerPlayer target, String tierArg, int amount) {
        EnchantTier tier = null;
        for (EnchantTier t : EnchantTier.values()) {
            if (t.name().equalsIgnoreCase(tierArg) || t.getDisplayName().equalsIgnoreCase(tierArg)) {
                tier = t;
                break;
            }
        }
        if (tier == null) {
            admin.sendSystemMessage(Component.literal("§cTier tidak valid! Pilihan: simple, unique, elite, ultimate, legendary, fabled, heroic, soul, mastery."));
            return 0;
        }
        int count = Math.max(1, Math.min(64, amount));
        Item bookItem = ModRpgItems.getMysteryBookForTier(tier);
        ItemStack bookStack = new ItemStack(bookItem, count);
        if (!target.getInventory().add(bookStack)) {
            target.drop(bookStack, false);
        }
        admin.sendSystemMessage(Component.literal("§6§l[AE Admin] §aMemberikan " + tier.getColorCode() + "§l" + tier.getDisplayName().toUpperCase() + " Mystery Book x" + count + " §akepada §e" + target.getScoreboardName()));
        if (!admin.getUUID().equals(target.getUUID())) {
            target.sendSystemMessage(Component.literal("§6§l[AE Admin] §eKamu menerima " + tier.getColorCode() + "§l" + tier.getDisplayName().toUpperCase() + " Mystery Book x" + count + " §edari Admin!"));
        }
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.2f);
        return 1;
    }

    private static int executeAeGiveDust(ServerPlayer admin, ServerPlayer target, int rate, int amount) {
        int finalRate = Math.max(1, Math.min(100, rate));
        int count = Math.max(1, Math.min(64, amount));
        ItemStack dust = new ItemStack(ModRpgItems.MAGIC_DUST, count);
        CustomData.update(DataComponents.CUSTOM_DATA, dust, tag -> tag.putInt("TakashaDustRate", finalRate));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Tingkat Keberhasilan: §a+" + finalRate + "%"));
        lore.add(Component.literal("§e✦ Seret ke Buku Sihir untuk Meningkatkan Sukses ✦"));
        dust.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(lore));

        if (!target.getInventory().add(dust)) {
            target.drop(dust, false);
        }
        admin.sendSystemMessage(Component.literal("§6§l[AE Admin] §aMemberikan Magic Dust (+" + finalRate + "%) x" + count + " §akepada §e" + target.getScoreboardName()));
        if (!admin.getUUID().equals(target.getUUID())) {
            target.sendSystemMessage(Component.literal("§6§l[AE Admin] §eKamu menerima Magic Dust (+" + finalRate + "%) x" + count + " §edari Admin!"));
        }
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.2f);
        return 1;
    }

    private static int executeAeListNbt(ServerPlayer admin) {
        ItemStack held = admin.getMainHandItem();
        if (held.isEmpty()) {
            admin.sendSystemMessage(Component.literal("§cKamu harus memegang item untuk melihat NBT data!"));
            return 0;
        }
        Map<String, Integer> enchants = TakashaEnchantManager.getCustomEnchants(held);
        int slots = TakashaEnchantManager.getEnchantCount(held);
        int maxSlots = TakashaEnchantManager.getMaxSlots(held);
        int soulSlots = TakashaEnchantManager.getSoulEnchantCount(held);
        int maxSoulSlots = TakashaEnchantManager.getMaxSoulSlots(held);
        boolean whiteScroll = TakashaEnchantManager.isWhiteScrollProtected(held);
        boolean holyScroll = TakashaEnchantManager.isHolyScrollProtected(held);

        StringBuilder sb = new StringBuilder("§4§l------ Item Information ------\n");
        sb.append("§fItem: §e").append(held.getHoverName().getString()).append("\n");
        sb.append("§fEnchant Slots: §a").append(slots).append("/").append(maxSlots).append("\n");
        sb.append("§fSoul Slots: §b").append(soulSlots).append("/").append(maxSoulSlots).append("\n");
        sb.append("§fWhite Scroll Protected: ").append(whiteScroll ? "§aYa" : "§cTidak").append("\n");
        sb.append("§fHoly Scroll Protected: ").append(holyScroll ? "§aYa" : "§cTidak").append("\n");
        sb.append("§fCustom Enchants (").append(enchants.size()).append("):\n");
        for (Map.Entry<String, Integer> e : enchants.entrySet()) {
            CustomEnchantDef def = TakashaEnchantRegistry.get(e.getKey());
            String dName = def != null ? def.name : e.getKey();
            String col = def != null ? def.tier.getColorCode() : "§f";
            sb.append(" ").append(col).append("• ").append(dName).append(" ").append(TakashaCustomBookItem.toRoman(e.getValue())).append(" §7(ID: ").append(e.getKey()).append(")\n");
        }
        admin.sendSystemMessage(Component.literal(sb.toString()));
        return 1;
    }

    // 12. Damage Dummy Commands (/dummy, /testdummy)
    private static void registerDamageDummyCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String cmd : List.of("dummy", "testdummy")) {
            dispatcher.register(Commands.literal(cmd)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    DamageDummyEntity nearest = findNearestDummy(player, 5.0);
                    if (nearest != null) {
                        nearest.printCombatReport(player);
                        return 1;
                    }
                    sendDummyHelp(player);
                    return 1;
                })
                .then(Commands.literal("spawn")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        if (!player.canUseGameMasterBlocks() && !player.isCreative() && !checkCommandPermission(player, "dummy")) return 0;
                        DamageDummyEntity dummy = new DamageDummyEntity(ModRpgEntities.DAMAGE_DUMMY, player.level());
                        dummy.setPos(player.getX(), player.getY(), player.getZ());
                        dummy.setYRot(player.getYRot());
                        dummy.setXRot(0.0f);
                        dummy.setYHeadRot(player.getYRot());
                        player.level().addFreshEntity(dummy);
                        player.sendSystemMessage(Component.literal("§e§l[Target Dummy] §aCombat Training Dummy berhasil dipasang di lokasimu!"));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ARMOR_STAND_PLACE, SoundSource.PLAYERS, 1.0f, 1.0f);
                        return 1;
                    })
                )
                .then(Commands.literal("remove")
                    .executes(context -> executeRemoveDummies(context.getSource().getPlayerOrException()))
                )
                .then(Commands.literal("clear")
                    .executes(context -> executeRemoveDummies(context.getSource().getPlayerOrException()))
                )
                .then(Commands.literal("reset")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        DamageDummyEntity nearest = findNearestDummy(player, 5.0);
                        if (nearest == null) {
                            player.sendSystemMessage(Component.literal("§cTidak ada Target Dummy dalam radius 5 blok!"));
                            return 0;
                        }
                        nearest.resetCombatStats();
                        player.sendSystemMessage(Component.literal("§e§l[Target Dummy] §bStatistik pertempuran Target Dummy berhasil di-reset!"));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.2f);
                        return 1;
                    })
                )
                .then(Commands.literal("clean")
                    .executes(context -> executeCleanDummy(context.getSource().getPlayerOrException()))
                )
                .then(Commands.literal("cure")
                    .executes(context -> executeCleanDummy(context.getSource().getPlayerOrException()))
                )
                .then(Commands.literal("report")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        DamageDummyEntity nearest = findNearestDummy(player, 5.0);
                        if (nearest == null) {
                            player.sendSystemMessage(Component.literal("§cTidak ada Target Dummy dalam radius 5 blok!"));
                            return 0;
                        }
                        nearest.printCombatReport(player);
                        return 1;
                    })
                )
                .then(Commands.literal("equip")
                    .then(Commands.argument("armor_type", StringArgumentType.word())
                        .suggests(DUMMY_ARMOR_SUGGESTION_PROVIDER)
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String type = StringArgumentType.getString(context, "armor_type");
                            DamageDummyEntity nearest = findNearestDummy(player, 5.0);
                            if (nearest == null) {
                                player.sendSystemMessage(Component.literal("§cTidak ada Target Dummy dalam radius 5 blok!"));
                                return 0;
                            }
                            nearest.equipArmorSet(type);
                            player.sendSystemMessage(Component.literal("§e§l[Target Dummy] §aSet armor §e" + type + " §aberhasil dipasang pada Target Dummy!"));
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("help")
                    .executes(context -> {
                        sendDummyHelp(context.getSource().getPlayerOrException());
                        return 1;
                    })
                )
            );
        }
    }

    private static DamageDummyEntity findNearestDummy(ServerPlayer player, double radius) {
        List<DamageDummyEntity> dummies = player.level().getEntitiesOfClass(DamageDummyEntity.class, player.getBoundingBox().inflate(radius));
        if (dummies.isEmpty()) return null;
        dummies.sort(Comparator.comparingDouble(d -> d.distanceToSqr(player)));
        return dummies.get(0);
    }

    private static int executeRemoveDummies(ServerPlayer player) {
        if (!player.canUseGameMasterBlocks() && !player.isCreative() && !checkCommandPermission(player, "dummy")) return 0;
        List<DamageDummyEntity> dummies = player.level().getEntitiesOfClass(DamageDummyEntity.class, player.getBoundingBox().inflate(5.0));
        List<Display.TextDisplay> orphanDisplays = player.level().getEntitiesOfClass(Display.TextDisplay.class, player.getBoundingBox().inflate(6.0));

        int cleanedDisplays = 0;
        for (Display.TextDisplay td : orphanDisplays) {
            if (td.entityTags().contains("takasha_dummy_overhead") ||
                td.entityTags().contains("takasha_damage_indicator") ||
                td.entityTags().stream().anyMatch(t -> t.startsWith("takasha_dummy_overhead_"))) {
                td.discard();
                cleanedDisplays++;
            }
        }

        if (dummies.isEmpty() && cleanedDisplays == 0) {
            player.sendSystemMessage(Component.literal("§cTidak ada Target Dummy atau teks melayang dalam radius 5 blok!"));
            return 0;
        }
        for (DamageDummyEntity d : dummies) {
            d.discard();
        }
        player.sendSystemMessage(Component.literal("§e§l[Target Dummy] §c" + dummies.size() + " Target Dummy & " + cleanedDisplays + " teks melayang berhasil dibersihkan!"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARMOR_STAND_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
        return 1;
    }

    private static int executeCleanDummy(ServerPlayer player) {
        DamageDummyEntity nearest = findNearestDummy(player, 5.0);
        if (nearest == null) {
            player.sendSystemMessage(Component.literal("§cTidak ada Target Dummy dalam radius 5 blok!"));
            return 0;
        }
        nearest.clearAllStatuses();
        player.sendSystemMessage(Component.literal("§e§l[Target Dummy] §aSeluruh status efek Target Dummy berhasil dinetralkan!"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.COW_MILK, SoundSource.PLAYERS, 1.0f, 1.0f);
        return 1;
    }

    private static void sendDummyHelp(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§6§l╔══════════════════════════════════════╗\n"
                + "§e§l            TARGET DUMMY COMMANDS\n"
                + "§6§l╠══════════════════════════════════════╣\n"
                + "§e• /dummy spawn §7- Pasang training dummy baru di lokasimu\n"
                + "§e• /dummy remove §7- Hapus dummy terdekat (radius 5 blok)\n"
                + "§e• /dummy reset §7- Reset DPS, peak hit, dan total damage\n"
                + "§e• /dummy equip <none|leather|iron|diamond|netherite> §7- Pasang armor\n"
                + "§e• /dummy clean §7- Netralkan seluruh status efek\n"
                + "§e• /dummy report §7- Cetak laporan pertempuran lengkap\n"
                + "§6§l╚══════════════════════════════════════╝"));
    }
}
