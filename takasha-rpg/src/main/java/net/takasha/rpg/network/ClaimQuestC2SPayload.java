package net.takasha.rpg.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.takasha.rpg.TakashaRpgMod;

public record ClaimQuestC2SPayload(String questId) implements CustomPacketPayload {
    public static final Type<ClaimQuestC2SPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "claim_quest_c2s"));

    public static final StreamCodec<ByteBuf, ClaimQuestC2SPayload> STREAM_CODEC = StreamCodec.of(
        (buf, val) -> ByteBufCodecs.STRING_UTF8.encode(buf, val.questId),
        buf -> new ClaimQuestC2SPayload(ByteBufCodecs.STRING_UTF8.decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
