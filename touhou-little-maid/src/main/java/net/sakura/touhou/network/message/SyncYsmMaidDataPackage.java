package net.sakura.touhou.network.message;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.sakura.touhou.TouhouLittleMaidMod;
import net.sakura.touhou.compat.ysm.event.UpdateRemoteStructEvent;
import net.sakura.touhou.entity.EntityMaid;

/**
 * SyncYsmMaidDataPackage
 * Dikirim dari server ke client untuk menyinkronkan animasi roulette dan variabel roaming Molang YSM.
 */
public record SyncYsmMaidDataPackage(
        int entityId,
        String rouletteAnim,
        boolean isRouletteAnimPlaying,
        Object2FloatOpenHashMap<String> roamingVars
) implements CustomPacketPayload {

    public static final Type<SyncYsmMaidDataPackage> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(TouhouLittleMaidMod.MOD_ID, "sync_ysm_maid_data")
    );

    public static final StreamCodec<ByteBuf, SyncYsmMaidDataPackage> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> {
                ByteBufCodecs.VAR_INT.encode(buf, val.entityId);
                ByteBufCodecs.STRING_UTF8.encode(buf, val.rouletteAnim != null ? val.rouletteAnim : "");
                ByteBufCodecs.BOOL.encode(buf, val.isRouletteAnimPlaying);

                if (val.roamingVars != null) {
                    ByteBufCodecs.VAR_INT.encode(buf, val.roamingVars.size());
                    for (var entry : val.roamingVars.object2FloatEntrySet()) {
                        ByteBufCodecs.STRING_UTF8.encode(buf, entry.getKey());
                        ByteBufCodecs.FLOAT.encode(buf, entry.getFloatValue());
                    }
                } else {
                    ByteBufCodecs.VAR_INT.encode(buf, 0);
                }
            },
            buf -> {
                int id = ByteBufCodecs.VAR_INT.decode(buf);
                String anim = ByteBufCodecs.STRING_UTF8.decode(buf);
                boolean isPlaying = ByteBufCodecs.BOOL.decode(buf);
                int size = ByteBufCodecs.VAR_INT.decode(buf);
                Object2FloatOpenHashMap<String> map = new Object2FloatOpenHashMap<>(size);
                for (int i = 0; i < size; i++) {
                    String k = ByteBufCodecs.STRING_UTF8.decode(buf);
                    float v = ByteBufCodecs.FLOAT.decode(buf);
                    map.put(k, v);
                }
                return new SyncYsmMaidDataPackage(id, anim, isPlaying, map);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Environment(EnvType.CLIENT)
    public static void handle(SyncYsmMaidDataPackage packet, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                Entity entity = mc.level.getEntity(packet.entityId());
                if (entity instanceof EntityMaid maid) {
                    UpdateRemoteStructEvent.EVENT.invoker().post(
                            new UpdateRemoteStructEvent(maid, packet.roamingVars())
                    );
                    if (packet.isRouletteAnimPlaying()) {
                        maid.playRouletteAnim(packet.rouletteAnim());
                    } else {
                        maid.stopRouletteAnim();
                    }
                }
            }
        });
    }
}
