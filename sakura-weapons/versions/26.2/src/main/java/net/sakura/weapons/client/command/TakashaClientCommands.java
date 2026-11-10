package net.sakura.weapons.client.command;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.sakura.weapons.client.config.TakashaMasterConfig;
import net.sakura.weapons.client.config.TakashaNametagConfig;
import net.sakura.weapons.client.gui.TakashaDashboardScreen;

@Environment(EnvType.CLIENT)
public class TakashaClientCommands {

    public static void initialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            dispatcher.register(buildTakashaNode("takasha"));
            dispatcher.register(buildTakashaNode("sakura"));
        });
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> buildTakashaNode(String root) {
        return literal(root)
                .executes(context -> {
                    openDashboard();
                    return 1;
                })
                .then(literal("menu").executes(context -> {
                    openDashboard();
                    return 1;
                }))
                .then(literal("help").executes(context -> {
                    var cfg = TakashaMasterConfig.getInstance();
                    context.getSource().sendFeedback(Component.literal("§6§l✦ TAKASHA CLIENT SUITE ✦"));
                    context.getSource().sendFeedback(Component.literal(" §e/" + root + " menu §7- Membuka Dashboard GUI"));
                    context.getSource().sendFeedback(Component.literal(" §e/" + root + " autofarm [on|off|toggle|status] §7(" + (cfg.autoFarmEnabled ? "§aAKTIF" : "§cNONAKTIF") + "§7)"));
                    context.getSource().sendFeedback(Component.literal(" §e/" + root + " autosell [on|off|toggle|status] §7(" + (cfg.autoSellGuiOnFull ? "§aAKTIF" : "§cNONAKTIF") + "§7)"));
                    context.getSource().sendFeedback(Component.literal(" §e/" + root + " autofish [on|off|toggle|status] §7(" + (cfg.autoFishEnabled ? "§aAKTIF" : "§cNONAKTIF") + "§7)"));
                    context.getSource().sendFeedback(Component.literal(" §e/" + root + " autokill [on|off|toggle|status] §7(" + (cfg.autoKillEnabled ? "§aAKTIF" : "§cNONAKTIF") + "§7)"));
                    context.getSource().sendFeedback(Component.literal(" §e/" + root + " autobow [on|off|toggle|status] §7(" + (cfg.autoBowEnabled ? "§aAKTIF" : "§cNONAKTIF") + "§7)"));
                    context.getSource().sendFeedback(Component.literal(" §e/" + root + " nametag [on|off|self|all|status]"));
                    return 1;
                }))
                .then(literal("autofarm")
                    .executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoFarmEnabled = !cfg.autoFarmEnabled;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal(
                            cfg.autoFarmEnabled ? "§a[Takasha] Auto Farm: AKTIF" : "§c[Takasha] Auto Farm: NONAKTIF"
                        ));
                        return 1;
                    })
                    .then(literal("toggle").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoFarmEnabled = !cfg.autoFarmEnabled;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal(
                            cfg.autoFarmEnabled ? "§a[Takasha] Auto Farm: AKTIF" : "§c[Takasha] Auto Farm: NONAKTIF"
                        ));
                        return 1;
                    }))
                    .then(literal("on").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoFarmEnabled = true;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal("§a[Takasha] Auto Farm: AKTIF"));
                        return 1;
                    }))
                    .then(literal("off").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoFarmEnabled = false;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal("§c[Takasha] Auto Farm: NONAKTIF"));
                        return 1;
                    }))
                    .then(literal("status").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        context.getSource().sendFeedback(Component.literal(
                            cfg.autoFarmEnabled ? "§a[Takasha] Auto Farm saat ini: AKTIF" : "§c[Takasha] Auto Farm saat ini: NONAKTIF"
                        ));
                        return 1;
                    }))
                )
                .then(literal("autosell")
                    .executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoSellGuiOnFull = !cfg.autoSellGuiOnFull;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal(
                            cfg.autoSellGuiOnFull ? "§a[Takasha] Auto SellGUI: AKTIF" : "§c[Takasha] Auto SellGUI: NONAKTIF"
                        ));
                        return 1;
                    })
                    .then(literal("toggle").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoSellGuiOnFull = !cfg.autoSellGuiOnFull;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal(
                            cfg.autoSellGuiOnFull ? "§a[Takasha] Auto SellGUI: AKTIF" : "§c[Takasha] Auto SellGUI: NONAKTIF"
                        ));
                        return 1;
                    }))
                    .then(literal("on").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoSellGuiOnFull = true;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal("§a[Takasha] Auto SellGUI: AKTIF"));
                        return 1;
                    }))
                    .then(literal("off").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoSellGuiOnFull = false;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal("§c[Takasha] Auto SellGUI: NONAKTIF"));
                        return 1;
                    }))
                    .then(literal("status").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        context.getSource().sendFeedback(Component.literal(
                            cfg.autoSellGuiOnFull ? "§a[Takasha] Auto SellGUI saat ini: AKTIF" : "§c[Takasha] Auto SellGUI saat ini: NONAKTIF"
                        ));
                        return 1;
                    }))
                )
                .then(literal("autofish")
                    .executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoFishEnabled = !cfg.autoFishEnabled;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal(
                            cfg.autoFishEnabled ? "§a[Takasha] Auto Fish: AKTIF" : "§c[Takasha] Auto Fish: NONAKTIF"
                        ));
                        return 1;
                    })
                    .then(literal("toggle").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoFishEnabled = !cfg.autoFishEnabled;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal(
                            cfg.autoFishEnabled ? "§a[Takasha] Auto Fish: AKTIF" : "§c[Takasha] Auto Fish: NONAKTIF"
                        ));
                        return 1;
                    }))
                    .then(literal("on").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoFishEnabled = true;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal("§a[Takasha] Auto Fish: AKTIF"));
                        return 1;
                    }))
                    .then(literal("off").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoFishEnabled = false;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal("§c[Takasha] Auto Fish: NONAKTIF"));
                        return 1;
                    }))
                    .then(literal("status").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        context.getSource().sendFeedback(Component.literal(
                            cfg.autoFishEnabled ? "§a[Takasha] Auto Fish saat ini: AKTIF" : "§c[Takasha] Auto Fish saat ini: NONAKTIF"
                        ));
                        return 1;
                    }))
                )
                .then(literal("autokill")
                    .executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoKillEnabled = !cfg.autoKillEnabled;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal(
                            cfg.autoKillEnabled ? "§a[Takasha] Auto Kill: AKTIF" : "§c[Takasha] Auto Kill: NONAKTIF"
                        ));
                        return 1;
                    })
                    .then(literal("toggle").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoKillEnabled = !cfg.autoKillEnabled;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal(
                            cfg.autoKillEnabled ? "§a[Takasha] Auto Kill: AKTIF" : "§c[Takasha] Auto Kill: NONAKTIF"
                        ));
                        return 1;
                    }))
                    .then(literal("on").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoKillEnabled = true;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal("§a[Takasha] Auto Kill: AKTIF"));
                        return 1;
                    }))
                    .then(literal("off").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoKillEnabled = false;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal("§c[Takasha] Auto Kill: NONAKTIF"));
                        return 1;
                    }))
                    .then(literal("status").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        context.getSource().sendFeedback(Component.literal(
                            cfg.autoKillEnabled ? "§a[Takasha] Auto Kill saat ini: AKTIF" : "§c[Takasha] Auto Kill saat ini: NONAKTIF"
                        ));
                        return 1;
                    }))
                )
                .then(literal("autobow")
                    .executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoBowEnabled = !cfg.autoBowEnabled;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal(
                            cfg.autoBowEnabled ? "§a[Takasha] Auto Bow: AKTIF" : "§c[Takasha] Auto Bow: NONAKTIF"
                        ));
                        return 1;
                    })
                    .then(literal("toggle").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoBowEnabled = !cfg.autoBowEnabled;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal(
                            cfg.autoBowEnabled ? "§a[Takasha] Auto Bow: AKTIF" : "§c[Takasha] Auto Bow: NONAKTIF"
                        ));
                        return 1;
                    }))
                    .then(literal("on").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoBowEnabled = true;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal("§a[Takasha] Auto Bow: AKTIF"));
                        return 1;
                    }))
                    .then(literal("off").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        cfg.autoBowEnabled = false;
                        cfg.save();
                        context.getSource().sendFeedback(Component.literal("§c[Takasha] Auto Bow: NONAKTIF"));
                        return 1;
                    }))
                    .then(literal("status").executes(context -> {
                        var cfg = TakashaMasterConfig.getInstance();
                        context.getSource().sendFeedback(Component.literal(
                            cfg.autoBowEnabled ? "§a[Takasha] Auto Bow saat ini: AKTIF" : "§c[Takasha] Auto Bow saat ini: NONAKTIF"
                        ));
                        return 1;
                    }))
                )
                .then(literal("nametag")
                    .executes(context -> {
                        TakashaNametagConfig.toggle(context.getSource().getPlayer());
                        return 1;
                    })
                    .then(literal("toggle").executes(context -> {
                        TakashaNametagConfig.toggle(context.getSource().getPlayer());
                        return 1;
                    }))
                    .then(literal("on").executes(context -> {
                        TakashaNametagConfig.setPlayerNametagDisabled(false, context.getSource().getPlayer());
                        return 1;
                    }))
                    .then(literal("off").executes(context -> {
                        TakashaNametagConfig.setPlayerNametagDisabled(true, context.getSource().getPlayer());
                        return 1;
                    }))
                    .then(literal("self").executes(context -> {
                        TakashaNametagConfig.setHideSelfOnly(true);
                        context.getSource().sendFeedback(Component.literal("§a[Takasha] Nametag mode: Sembunyikan diri sendiri saja (aman untuk multiplayer)."));
                        return 1;
                    }))
                    .then(literal("all").executes(context -> {
                        TakashaNametagConfig.setHideSelfOnly(false);
                        context.getSource().sendFeedback(Component.literal("§e[Takasha] Nametag mode: Sembunyikan semua nametag (cinematic)."));
                        return 1;
                    }))
                    .then(literal("status").executes(context -> {
                        boolean disabled = TakashaNametagConfig.isPlayerNametagDisabled();
                        boolean selfOnly = TakashaNametagConfig.isHideSelfOnly();
                        Component msg = disabled
                            ? Component.literal("§c[Takasha] Nametag: TERSEMBUNYI (" + (selfOnly ? "Diri Sendiri" : "Semua Pemain") + ")")
                            : Component.literal("§a[Takasha] Nametag: MUNCUL");
                        context.getSource().sendFeedback(msg);
                        return 1;
                    }))
                );
    }

    private static void openDashboard() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.gui.setScreen(new TakashaDashboardScreen(null)));
    }
}
