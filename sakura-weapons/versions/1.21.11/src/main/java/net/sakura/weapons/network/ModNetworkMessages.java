package net.sakura.weapons.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.sakura.weapons.SakuraWeaponsMod;
import net.sakura.weapons.entity.TakashaNpcEntity;
import net.sakura.weapons.network.packet.NpcPositionActionPayload;
import net.sakura.weapons.network.packet.NpcUpdatePhysicsPayload;

public class ModNetworkMessages {

    public static void initialize() {
        PayloadTypeRegistry.playC2S().register(UpdateNpcPayload.TYPE, UpdateNpcPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DismantleNpcPayload.TYPE, DismantleNpcPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NpcUpdatePhysicsPayload.TYPE, NpcUpdatePhysicsPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(NpcPositionActionPayload.TYPE, NpcPositionActionPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(UpdateNpcPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                Entity entity = player.level().getEntity(payload.entityId());
                if (entity instanceof TakashaNpcEntity npc) {
                    // Security & anti-conflict check 1: Distance limit (max 8 blocks = 64 sq dist)
                    if (player.distanceToSqr(npc) > 64.0) {
                        return;
                    }

                    // Security & anti-conflict check 2: Permissions
                    if (!npc.canPlayerManage(player)) {
                        player.displayClientMessage(Component.translatable("message.sakura_weapons.npc.no_permission", npc.getOwnerName()), true);
                        return;
                    }

                    // Apply visual & operational updates
                    npc.setSkinUrl(payload.skinUrl());
                    npc.setSkinModel(payload.skinModel());
                    int preset = payload.posePreset();
                    npc.setPosePreset(preset);

                    if (preset > 0 && preset <= 32) {
                        npc.applyPosePreset(preset);
                    } else if (preset == 0 && (payload.emoteId() == null || payload.emoteId().isBlank())) {
                        npc.applyPosePreset(0);
                        if (payload.headPose() != null) npc.setHeadPose(payload.headPose());
                        if (payload.bodyPose() != null) npc.setBodyPose(payload.bodyPose());
                        if (payload.leftArmPose() != null) npc.setLeftArmPose(payload.leftArmPose());
                        if (payload.rightArmPose() != null) npc.setRightArmPose(payload.rightArmPose());
                        if (payload.leftLegPose() != null) npc.setLeftLegPose(payload.leftLegPose());
                        if (payload.rightLegPose() != null) npc.setRightLegPose(payload.rightLegPose());
                    } else {
                        npc.setHeadPose(payload.headPose());
                        npc.setBodyPose(payload.bodyPose());
                        npc.setLeftArmPose(payload.leftArmPose());
                        npc.setRightArmPose(payload.rightArmPose());
                        npc.setLeftLegPose(payload.leftLegPose());
                        npc.setRightLegPose(payload.rightLegPose());
                    }

                    npc.setBedHeightOffset(payload.bedHeightOffset());
                    npc.setEmoteId(payload.emoteId());
                    npc.setEmotePlayMode(payload.emotePlayMode());
                    npc.setYawRotation(payload.yawRotation());
                    npc.setSmall(payload.isSmall());
                    npc.setLocked(payload.isLocked());
                    npc.setShowName(payload.showName());

                    if (payload.customName() != null && !payload.customName().isBlank()) {
                        npc.setCustomName(Component.literal(payload.customName()));
                    } else {
                        npc.setCustomName(null);
                    }

                    npc.setPermissionMode(payload.permissionMode());
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(DismantleNpcPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                Entity entity = player.level().getEntity(payload.entityId());
                if (entity instanceof TakashaNpcEntity npc) {
                    if (player.distanceToSqr(npc) > 64.0) {
                        return;
                    }
                    if (!npc.canPlayerManage(player)) {
                        player.displayClientMessage(Component.translatable("message.sakura_weapons.npc.no_permission", npc.getOwnerName()), true);
                        return;
                    }
                    npc.dismantle(player);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NpcUpdatePhysicsPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                Entity entity = player.level().getEntity(payload.entityId());
                if (entity instanceof TakashaNpcEntity npc) {
                    if (player.distanceToSqr(npc) > 64.0) {
                        return;
                    }
                    if (!npc.canPlayerManage(player)) {
                        player.displayClientMessage(Component.translatable("message.sakura_weapons.npc.no_permission", npc.getOwnerName()), true);
                        return;
                    }
                    npc.setPushableMode(payload.pushable());
                    npc.setPushStrength(payload.pushStrength());
                    npc.setPushPermissionId(payload.permissionId());
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NpcPositionActionPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                Entity entity = player.level().getEntity(payload.entityId());
                if (entity instanceof TakashaNpcEntity npc) {
                    if (player.distanceToSqr(npc) > 64.0) {
                        return;
                    }
                    if (!npc.canPlayerManage(player)) {
                        player.displayClientMessage(Component.translatable("message.sakura_weapons.npc.no_permission", npc.getOwnerName()), true);
                        return;
                    }
                    if (payload.actionType() == 0) {
                        npc.resetToOriginPosition();
                    } else if (payload.actionType() == 1) {
                        npc.lockCurrentAsNewOrigin();
                    }
                }
            });
        });
        SakuraWeaponsMod.LOGGER.info("Registered Takasha NPC network payloads for Minecraft 1.21.11");
    }
}
