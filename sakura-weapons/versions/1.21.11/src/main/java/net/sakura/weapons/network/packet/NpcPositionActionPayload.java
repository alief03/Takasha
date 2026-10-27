package net.sakura.weapons.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sakura.weapons.SakuraWeaponsMod;

public record NpcPositionActionPayload(
    int entityId,
    int actionType // 0 = Reset to Origin, 1 = Lock New Origin
) implements CustomPacketPayload {
    public static final int ACTION_RESET_ORIGIN = 0;
    public static final int ACTION_LOCK_NEW_ORIGIN = 1;

    public static final Type<NpcPositionActionPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(SakuraWeaponsMod.MOD_ID, "npc_position_action"));

    public static final StreamCodec<ByteBuf, NpcPositionActionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, NpcPositionActionPayload::entityId,
        ByteBufCodecs.VAR_INT, NpcPositionActionPayload::actionType,
        NpcPositionActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
