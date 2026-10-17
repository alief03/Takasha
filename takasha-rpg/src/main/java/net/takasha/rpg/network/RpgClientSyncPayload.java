package net.takasha.rpg.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.takasha.rpg.TakashaRpgMod;

public record RpgClientSyncPayload(
    String worldName,
    String playerName,
    double balance,
    int gems,
    int totalSkillLevel,
    int usedHomes,
    int maxHomes,
    boolean showScoreboard
) implements CustomPacketPayload {
    public static final Type<RpgClientSyncPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "rpg_client_sync"));

    public static final StreamCodec<ByteBuf, RpgClientSyncPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, RpgClientSyncPayload::worldName,
        ByteBufCodecs.STRING_UTF8, RpgClientSyncPayload::playerName,
        ByteBufCodecs.DOUBLE, RpgClientSyncPayload::balance,
        ByteBufCodecs.VAR_INT, RpgClientSyncPayload::gems,
        ByteBufCodecs.VAR_INT, RpgClientSyncPayload::totalSkillLevel,
        ByteBufCodecs.VAR_INT, RpgClientSyncPayload::usedHomes,
        ByteBufCodecs.VAR_INT, RpgClientSyncPayload::maxHomes,
        ByteBufCodecs.BOOL, RpgClientSyncPayload::showScoreboard,
        RpgClientSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
