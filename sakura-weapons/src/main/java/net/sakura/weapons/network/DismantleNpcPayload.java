package net.sakura.weapons.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sakura.weapons.SakuraWeaponsMod;

public record DismantleNpcPayload(int entityId) implements CustomPacketPayload {

    public static final Type<DismantleNpcPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(SakuraWeaponsMod.MOD_ID, "dismantle_npc")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, DismantleNpcPayload> CODEC = StreamCodec.of(
        DismantleNpcPayload::write,
        DismantleNpcPayload::read
    );

    private static void write(RegistryFriendlyByteBuf buf, DismantleNpcPayload payload) {
        buf.writeVarInt(payload.entityId);
    }

    private static DismantleNpcPayload read(RegistryFriendlyByteBuf buf) {
        return new DismantleNpcPayload(buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
