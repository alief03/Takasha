package net.sakura.weapons.network;

import net.minecraft.core.Rotations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sakura.weapons.SakuraWeaponsMod;

public record UpdateNpcPayload(
    int entityId,
    String skinUrl,
    String skinModel,
    int posePreset,
    Rotations headPose,
    Rotations bodyPose,
    Rotations leftArmPose,
    Rotations rightArmPose,
    Rotations leftLegPose,
    Rotations rightLegPose,
    float bedHeightOffset,
    String emoteId,
    byte emotePlayMode,
    float yawRotation,
    boolean isSmall,
    boolean isLocked,
    boolean showName,
    String customName,
    byte permissionMode
) implements CustomPacketPayload {

    public static final Type<UpdateNpcPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(SakuraWeaponsMod.MOD_ID, "update_npc")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateNpcPayload> CODEC = StreamCodec.of(
        UpdateNpcPayload::write,
        UpdateNpcPayload::read
    );

    private static void write(RegistryFriendlyByteBuf buf, UpdateNpcPayload payload) {
        buf.writeVarInt(payload.entityId);
        buf.writeUtf(payload.skinUrl, 512);
        buf.writeUtf(payload.skinModel, 16);
        buf.writeVarInt(payload.posePreset);

        writeRotations(buf, payload.headPose);
        writeRotations(buf, payload.bodyPose);
        writeRotations(buf, payload.leftArmPose);
        writeRotations(buf, payload.rightArmPose);
        writeRotations(buf, payload.leftLegPose);
        writeRotations(buf, payload.rightLegPose);

        buf.writeFloat(payload.bedHeightOffset);
        buf.writeUtf(payload.emoteId, 128);
        buf.writeByte(payload.emotePlayMode);
        buf.writeFloat(payload.yawRotation);
        buf.writeBoolean(payload.isSmall);
        buf.writeBoolean(payload.isLocked);
        buf.writeBoolean(payload.showName);
        buf.writeUtf(payload.customName, 64);
        buf.writeByte(payload.permissionMode);
    }

    private static UpdateNpcPayload read(RegistryFriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        String skinUrl = buf.readUtf(512);
        String skinModel = buf.readUtf(16);
        int posePreset = buf.readVarInt();

        Rotations headPose = readRotations(buf);
        Rotations bodyPose = readRotations(buf);
        Rotations leftArmPose = readRotations(buf);
        Rotations rightArmPose = readRotations(buf);
        Rotations leftLegPose = readRotations(buf);
        Rotations rightLegPose = readRotations(buf);

        float bedHeightOffset = buf.readFloat();
        String emoteId = buf.readUtf(128);
        byte emotePlayMode = buf.readByte();
        float yawRotation = buf.readFloat();
        boolean isSmall = buf.readBoolean();
        boolean isLocked = buf.readBoolean();
        boolean showName = buf.readBoolean();
        String customName = buf.readUtf(64);
        byte permissionMode = buf.readByte();

        return new UpdateNpcPayload(
            entityId, skinUrl, skinModel, posePreset,
            headPose, bodyPose, leftArmPose, rightArmPose, leftLegPose, rightLegPose,
            bedHeightOffset, emoteId, emotePlayMode, yawRotation, isSmall, isLocked, showName, customName, permissionMode
        );
    }

    private static void writeRotations(RegistryFriendlyByteBuf buf, Rotations rot) {
        buf.writeFloat(rot.x());
        buf.writeFloat(rot.y());
        buf.writeFloat(rot.z());
    }

    private static Rotations readRotations(RegistryFriendlyByteBuf buf) {
        return new Rotations(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
