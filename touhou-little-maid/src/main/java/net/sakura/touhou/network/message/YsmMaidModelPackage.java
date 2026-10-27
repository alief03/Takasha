package net.sakura.touhou.network.message;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.sakura.touhou.TouhouLittleMaidMod;
import net.sakura.touhou.entity.EntityMaid;

/**
 * YsmMaidModelPackage
 * Dikirim dari client ke server saat pemain memilih model YSM untuk maid di GUI.
 * Persis sesuai bytecode dan protokol sumber.
 */
public record YsmMaidModelPackage(
        int maidId,
        String modelId,
        String texture,
        Component name
) implements CustomPacketPayload {

    public static final Type<YsmMaidModelPackage> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(TouhouLittleMaidMod.MOD_ID, "ysm_maid_model")
    );

    public static final StreamCodec<ByteBuf, YsmMaidModelPackage> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> {
                ByteBufCodecs.VAR_INT.encode(buf, val.maidId);
                ByteBufCodecs.STRING_UTF8.encode(buf, val.modelId);
                ByteBufCodecs.STRING_UTF8.encode(buf, val.texture);
                ByteBufCodecs.STRING_UTF8.encode(buf, val.name != null ? val.name.getString() : "");
            },
            buf -> new YsmMaidModelPackage(
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    Component.literal(ByteBufCodecs.STRING_UTF8.decode(buf))
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(YsmMaidModelPackage packet, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        context.server().execute(() -> {
            Entity entity = player.level().getEntity(packet.maidId());
            if (entity instanceof EntityMaid maid && maid.isOwnedBy(player)) {
                maid.setIsYsmModel(true);
                maid.setYsmModel(packet.modelId(), packet.texture(), packet.name());
                player.sendSystemMessage(Component.literal("§a[Touhou Maid] §fModel YSM berhasil diterapkan: §e" + packet.modelId()));
            }
        });
    }
}
