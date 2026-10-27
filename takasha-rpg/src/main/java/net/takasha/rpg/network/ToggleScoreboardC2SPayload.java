package net.takasha.rpg.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.takasha.rpg.TakashaRpgMod;

public record ToggleScoreboardC2SPayload() implements CustomPacketPayload {
    public static final Type<ToggleScoreboardC2SPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "toggle_scoreboard"));

    public static final StreamCodec<ByteBuf, ToggleScoreboardC2SPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {},
        buf -> new ToggleScoreboardC2SPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
