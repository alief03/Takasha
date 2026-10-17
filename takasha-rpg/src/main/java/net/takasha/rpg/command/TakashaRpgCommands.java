package net.takasha.rpg.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;
import net.takasha.rpg.dungeon.TakashaArenaManager;
import net.takasha.rpg.economy.DynamicPricingEngine;
import net.takasha.rpg.economy.TakashaDailyShopManager;
import net.takasha.rpg.registry.ModRpgItems;
import net.takasha.rpg.skills.AuraSkillType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TakashaRpgCommands {

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerEconomyCommands(dispatcher);
            registerHomeCommands(dispatcher);
            registerAuraSkillsCommands(dispatcher);
            registerShopCommands(dispatcher);
            registerArenaCommands(dispatcher);
            registerUtilityCommands(dispatcher);
            registerRpgConfigCommands(dispatcher);
        });
    }

    private static boolean checkCommandPermission(ServerPlayer player, String commandId) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        if (!data.isCommandUnlocked(commandId)) {
            player.sendSystemMessage(Component.literal("§c§l[Takasha-RPG] §cPerintah ini terkunci!\n§eTemukan tomes/artifak §6LEGENDARY/MYTHIC §edi dungeon atau eksplorasi dunia untuk membuka perintah: §b/" + commandId));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            return false;
        }
        return true;
    }

    // 1. Economy Commands: /money, /bal, /pay, /worth
    private static void registerEconomyCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("money")
            .executes(context -> showBalance(context.getSource().getPlayerOrException()))
        );

        dispatcher.register(Commands.literal("bal")
            .executes(context -> showBalance(context.getSource().getPlayerOrException()))
        );

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
                        "§8Gunakan /shop sell untuk menjual ke pasar"));
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

    // 2. Multi-Home & Essentials Commands: /home, /sethome, /delhome, /homes, /back
    private static void registerHomeCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("home")
            .executes(context -> teleportHome(context.getSource().getPlayerOrException(), "home"))
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> teleportHome(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
            )
        );

        dispatcher.register(Commands.literal("sethome")
            .executes(context -> setHome(context.getSource().getPlayerOrException(), "home"))
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> setHome(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
            )
        );

        dispatcher.register(Commands.literal("delhome")
            .then(Commands.argument("name", StringArgumentType.word())
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

        dispatcher.register(Commands.literal("back")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
                TakashaRpgData.HomeLocation death = data.getLastDeathLocation();

                if (death == null) {
                    player.sendSystemMessage(Component.literal("§cTidak ada catatan lokasi kematian sebelumnya!"));
                    return 0;
                }

                ServerLevel targetLevel = death.resolveLevel(player);
                player.teleportTo(targetLevel, death.x(), death.y(), death.z(), java.util.Set.of(), death.yaw(), death.pitch(), true);
                player.sendSystemMessage(Component.literal("§aBerhasil kembali ke lokasi kematian terakhir!"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
                return 1;
            })
        );
    }

    private static int setHome(ServerPlayer player, String name) {
        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        if (data.getHomes().size() >= data.getMaxHomes() && !data.getHomes().containsKey(name.toLowerCase().trim())) {
            player.sendSystemMessage(Component.literal("§cKapasitas /sethome kamu sudah penuh ("
                    + data.getHomes().size() + "/" + data.getMaxHomes() + ")!\n§eGunakan §6Home Expansion Scroll §e(Tier Legendary) untuk menambah slot home hingga maksimal 10."));
            return 0;
        }

        data.setHome(name, player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
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

        ServerLevel targetLevel = loc.resolveLevel(player);
        player.teleportTo(targetLevel, loc.x(), loc.y(), loc.z(), java.util.Set.of(), loc.yaw(), loc.pitch(), true);
        player.sendSystemMessage(Component.literal("§aTeleportasi ke home '§e" + name + "§a'!"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        return 1;
    }

    // 3. AuraSkills Commands: /skills
    private static void registerAuraSkillsCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("skills")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
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
            })
        );
    }

    // 4. Shop Commands: /shop, /dailyshop
    private static void registerShopCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shop")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                player.sendSystemMessage(Component.literal(
                        "§6§l╔══════════════════════════════════╗\n" +
                        "§6§l║      ★ PASAR TAKASHA SHOP ★      ║\n" +
                        "§6§l╚══════════════════════════════════╝\n" +
                        "§fPerintah Toko:\n" +
                        "§e/dailyshop §7- Buka penawaran diskon harian berganti fajar\n" +
                        "§e/worth §7- Cek harga jual item di tanganmu\n" +
                        "§e/shop sell §7- Jual item yang dipegang di tangan utama\n" +
                        "§e/money §7- Cek saldo balance dan koin saat ini"
                ));
                return 1;
            })
            .then(Commands.literal("sell")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ItemStack held = player.getMainHandItem();
                    if (held.isEmpty()) {
                        player.sendSystemMessage(Component.literal("§cKamu tidak memegang item apapun untuk dijual!"));
                        return 0;
                    }

                    String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
                    double unitPrice = DynamicPricingEngine.getDynamicSellPrice(itemId, 10.0);
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
                })
            )
        );

        dispatcher.register(Commands.literal("dailyshop")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                List<TakashaDailyShopManager.DailyDeal> deals = TakashaDailyShopManager.getCurrentDeals();

                StringBuilder sb = new StringBuilder();
                sb.append("§6§l╔══════════════════════════════════════════╗\n");
                sb.append("§6§l║      ★ TOKO HARIAN (ROTASI FAJAR) ★      ║\n");
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
                sb.append("\n§7Beli penawaran dengan: §e/dailyshop buy <1-6>");
                player.sendSystemMessage(Component.literal(sb.toString()));
                return 1;
            })
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

                        player.sendSystemMessage(Component.literal("§a§l[Daily Shop] §fBerhasil membeli §e"
                                + deal.stack().getHoverName().getString() + " x" + deal.stack().getCount() + "§f!"));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
                        return 1;
                    })
                )
            )
        );
    }

    // 5. Dungeon Arena: /arena <start|stop>
    private static void registerArenaCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("arena")
            .then(Commands.literal("start")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (TakashaArenaManager.startArena(player)) {
                        return 1;
                    }
                    return 0;
                })
            )
            .then(Commands.literal("stop")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (!TakashaArenaManager.isArenaActive()) {
                        player.sendSystemMessage(Component.literal("§cTidak ada Dungeon Arena yang sedang berjalan!"));
                        return 0;
                    }
                    TakashaArenaManager.endArena(false, "Arena dihentikan secara manual oleh pemain.");
                    player.sendSystemMessage(Component.literal("§eTakasha Dungeon Arena berhasil dihentikan."));
                    return 1;
                })
            )
        );
    }

    // 6. Executable Command Tomes: /workbench, /trash, /enderchest, /feed, /heal, /ce, /tinker, /alchem, /withdrawsouls
    private static void registerUtilityCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /workbench or /wb
        dispatcher.register(Commands.literal("workbench")
            .executes(context -> openWorkbench(context.getSource().getPlayerOrException()))
        );
        dispatcher.register(Commands.literal("wb")
            .executes(context -> openWorkbench(context.getSource().getPlayerOrException()))
        );

        // /trash
        dispatcher.register(Commands.literal("trash")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                if (!checkCommandPermission(player, "trash")) return 0;

                ItemStack held = player.getMainHandItem();
                if (!held.isEmpty()) {
                    String name = held.getHoverName().getString();
                    int count = held.getCount();
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    player.sendSystemMessage(Component.literal("§c§l[Tong Sampah] §fItem §e" + name + " x" + count + " §ftelah dimusnahkan!"));
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
                    return 1;
                } else {
                    player.sendSystemMessage(Component.literal("§ePegang item yang ingin kamu buang di tangan utama, lalu ketik /trash"));
                    return 0;
                }
            })
        );

        // /enderchest or /ec
        dispatcher.register(Commands.literal("enderchest")
            .executes(context -> openEnderChest(context.getSource().getPlayerOrException()))
        );
        dispatcher.register(Commands.literal("ec")
            .executes(context -> openEnderChest(context.getSource().getPlayerOrException()))
        );

        // /feed
        dispatcher.register(Commands.literal("feed")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                if (!checkCommandPermission(player, "feed")) return 0;

                player.getFoodData().setFoodLevel(20);
                player.getFoodData().setSaturation(20.0f);
                player.sendSystemMessage(Component.literal("§a§l[Takasha-RPG] §fRasa lapar telah dipulihkan sepenuhnya oleh Segel Gourmet!"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0f, 1.0f);
                return 1;
            })
        );

        // /heal
        dispatcher.register(Commands.literal("heal")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                if (!checkCommandPermission(player, "heal")) return 0;

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

        // /ce
        dispatcher.register(Commands.literal("ce")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                if (!checkCommandPermission(player, "ce")) return 0;

                player.sendSystemMessage(Component.literal(
                        "§6§l╔══════════════════════════════════════╗\n" +
                        "§6§l║      ★ ENCHANTER MARKETPLACE ★       ║\n" +
                        "§6§l╚══════════════════════════════════════╝\n" +
                        "§fKatalog Gulungan & Serbuk Sihir Kuno:\n" +
                        "§e/ce buy white_scroll §7- $300 (Perlindungan Enchant Rusak)\n" +
                        "§e/ce buy black_scroll §7- $650 (Ekstrak Enchant Menjadi Buku)\n" +
                        "§e/ce buy transmute_scroll §7- $280 (Acak Ulang Enchantment)\n" +
                        "§e/ce buy magic_dust §7- $150 (Tingkatkan Peluang Sukses)\n" +
                        "§e/ce buy slot_increaser §7- 5 Gems (Tambah Slot Enchant Perlengkapan)"
                ));
                return 1;
            })
            .then(Commands.literal("buy")
                .then(Commands.literal("white_scroll").executes(c -> buyCeItem(c.getSource().getPlayerOrException(), new ItemStack(ModRpgItems.WHITE_SCROLL), 300.0, false)))
                .then(Commands.literal("black_scroll").executes(c -> buyCeItem(c.getSource().getPlayerOrException(), new ItemStack(ModRpgItems.BLACK_SCROLL), 650.0, false)))
                .then(Commands.literal("transmute_scroll").executes(c -> buyCeItem(c.getSource().getPlayerOrException(), new ItemStack(ModRpgItems.TRANSMUTE_SCROLL), 280.0, false)))
                .then(Commands.literal("magic_dust").executes(c -> buyCeItem(c.getSource().getPlayerOrException(), new ItemStack(ModRpgItems.MAGIC_DUST, 2), 150.0, false)))
                .then(Commands.literal("slot_increaser").executes(c -> buyCeItem(c.getSource().getPlayerOrException(), new ItemStack(ModRpgItems.SLOT_INCREASER_1), 5.0, true)))
            )
        );

        // /tinker
        dispatcher.register(Commands.literal("tinker")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                if (!checkCommandPermission(player, "tinker")) return 0;

                ItemStack held = player.getMainHandItem();
                if (!held.isEnchanted()) {
                    player.sendSystemMessage(Component.literal("§cPegang perlengkapan yang memiliki enchantment di tangan untuk di-tinker!"));
                    return 0;
                }

                held.consume(1, player);
                player.getInventory().add(new ItemStack(ModRpgItems.MAGIC_DUST, 2));
                player.getInventory().add(new ItemStack(ModRpgItems.MYSTERY_DUST, 1));

                player.sendSystemMessage(Component.literal("§a§l[Tinkerer] §fPerlengkapan berhasil dibongkar menjadi §e2x Magic Dust §fdan §b1x Mystery Dust§f!"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
                return 1;
            })
        );

        // /alchem
        dispatcher.register(Commands.literal("alchem")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                if (!checkCommandPermission(player, "alchem")) return 0;

                int dustCount = player.getInventory().countItem(ModRpgItems.MAGIC_DUST);
                if (dustCount < 4) {
                    player.sendSystemMessage(Component.literal("§cKamu membutuhkan minimal 4x Magic Dust di inventory untuk menggabungkannya menjadi Slot Increaser!"));
                    return 0;
                }

                player.getInventory().clearOrCountMatchingItems(p -> p.is(ModRpgItems.MAGIC_DUST), 4, player.inventoryMenu.getCraftSlots());
                player.getInventory().add(new ItemStack(ModRpgItems.SLOT_INCREASER_1, 1));

                player.sendSystemMessage(Component.literal("§a§l[Alchemist] §fBerhasil menggabungkan 4x Magic Dust menjadi §b1x Slot Increaser I§f!"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0f, 1.2f);
                return 1;
            })
        );

        // /withdrawsouls [levels]
        dispatcher.register(Commands.literal("withdrawsouls")
            .executes(context -> withdrawSouls(context.getSource().getPlayerOrException(), 5))
            .then(Commands.argument("levels", IntegerArgumentType.integer(5, 50))
                .executes(context -> withdrawSouls(context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "levels")))
            )
        );
    }

    private static int openWorkbench(ServerPlayer player) {
        if (!checkCommandPermission(player, "workbench")) return 0;
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new CraftingMenu(containerId, playerInventory, ContainerLevelAccess.create(player.level(), player.blockPosition())),
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

    // 7. RPG Config: /rpg scoreboard toggle, /rpg reload
    private static void registerRpgConfigCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rpg")
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
        );
    }
}
