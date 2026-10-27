package net.sakura.weapons.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sakura.weapons.SakuraWeaponsMod;

public record OpenNpcScreenPayload(int entityId) implements CustomPacketPayload {
    public static final Type<OpenNpcScreenPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(SakuraWeaponsMod.MOD_ID, "open_npc_screen"));

    public static final StreamCodec<ByteBuf, OpenNpcScreenPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, OpenNpcScreenPayload::entityId,
        OpenNpcScreenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
