package net.sakura.weapons.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sakura.weapons.SakuraWeaponsMod;

public record NpcUpdatePhysicsPayload(
    int entityId,
    boolean pushable,
    float pushStrength,
    int permissionId
) implements CustomPacketPayload {
    public static final Type<NpcUpdatePhysicsPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(SakuraWeaponsMod.MOD_ID, "npc_update_physics"));

    public static final StreamCodec<ByteBuf, NpcUpdatePhysicsPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, NpcUpdatePhysicsPayload::entityId,
        ByteBufCodecs.BOOL, NpcUpdatePhysicsPayload::pushable,
        ByteBufCodecs.FLOAT, NpcUpdatePhysicsPayload::pushStrength,
        ByteBufCodecs.VAR_INT, NpcUpdatePhysicsPayload::permissionId,
        NpcUpdatePhysicsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
