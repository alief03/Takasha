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
    boolean showScoreboard,
    double currentMana,
    double maxMana
) implements CustomPacketPayload {
    public static final Type<RpgClientSyncPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "rpg_client_sync"));

    public static final StreamCodec<ByteBuf, RpgClientSyncPayload> STREAM_CODEC = StreamCodec.of(
        (buf, val) -> {
            ByteBufCodecs.STRING_UTF8.encode(buf, val.worldName);
            ByteBufCodecs.STRING_UTF8.encode(buf, val.playerName);
            ByteBufCodecs.DOUBLE.encode(buf, val.balance);
            ByteBufCodecs.VAR_INT.encode(buf, val.gems);
            ByteBufCodecs.VAR_INT.encode(buf, val.totalSkillLevel);
            ByteBufCodecs.VAR_INT.encode(buf, val.usedHomes);
            ByteBufCodecs.VAR_INT.encode(buf, val.maxHomes);
            ByteBufCodecs.BOOL.encode(buf, val.showScoreboard);
            ByteBufCodecs.DOUBLE.encode(buf, val.currentMana);
            ByteBufCodecs.DOUBLE.encode(buf, val.maxMana);
        },
        buf -> new RpgClientSyncPayload(
            ByteBufCodecs.STRING_UTF8.decode(buf),
            ByteBufCodecs.STRING_UTF8.decode(buf),
            ByteBufCodecs.DOUBLE.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.BOOL.decode(buf),
            ByteBufCodecs.DOUBLE.decode(buf),
            ByteBufCodecs.DOUBLE.decode(buf)
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
