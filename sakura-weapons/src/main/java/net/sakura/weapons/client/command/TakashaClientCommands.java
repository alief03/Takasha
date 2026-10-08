package net.sakura.weapons.client.command;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;
import net.sakura.weapons.client.config.TakashaNametagConfig;

@Environment(EnvType.CLIENT)
public class TakashaClientCommands {

    public static void initialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            dispatcher.register(ClientCommands.literal("takasha")
                .then(ClientCommands.literal("nametag")
                    .executes(context -> {
                        TakashaNametagConfig.toggle(context.getSource().getPlayer());
                        return 1;
                    })
                    .then(ClientCommands.literal("on").executes(context -> {
                        TakashaNametagConfig.setPlayerNametagDisabled(false, context.getSource().getPlayer());
                        return 1;
                    }))
                    .then(ClientCommands.literal("off").executes(context -> {
                        TakashaNametagConfig.setPlayerNametagDisabled(true, context.getSource().getPlayer());
                        return 1;
                    }))
                    .then(ClientCommands.literal("status").executes(context -> {
                        boolean disabled = TakashaNametagConfig.isPlayerNametagDisabled();
                        Component msg = disabled
                            ? Component.translatable("message.sakura_weapons.nametag.disabled")
                            : Component.translatable("message.sakura_weapons.nametag.enabled");
                        context.getSource().sendFeedback(msg);
                        return 1;
                    }))
                )
            );
        });
    }
}
