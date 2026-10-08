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
        PayloadTypeRegistry.serverboundPlay().register(UpdateNpcPayload.TYPE, UpdateNpcPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DismantleNpcPayload.TYPE, DismantleNpcPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(NpcUpdatePhysicsPayload.TYPE, NpcUpdatePhysicsPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(NpcPositionActionPayload.TYPE, NpcPositionActionPayload.STREAM_CODEC);

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
                        player.sendOverlayMessage(Component.translatable("message.sakura_weapons.npc.no_permission", npc.getOwnerName()));
                        return;
                    }

                    // Apply visual & operational updates
                    npc.setSkinUrl(payload.skinUrl());
                    npc.setSkinModel(payload.skinModel());
                    int preset = payload.posePreset();
                    npc.setPosePreset(preset);

                    if (preset > 0 && preset <= 32) {
                        // Canonical preset pose: applyPosePreset() has configured exact limb articulations
                        npc.applyPosePreset(preset);
                    } else {
                        // Custom static pose (preset 0): synchronize custom limb rotations from payload
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
                        player.sendOverlayMessage(Component.translatable("message.sakura_weapons.npc.no_permission", npc.getOwnerName()));
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
                        player.sendOverlayMessage(Component.translatable("message.sakura_weapons.npc.no_permission", npc.getOwnerName()));
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
                        player.sendOverlayMessage(Component.translatable("message.sakura_weapons.npc.no_permission", npc.getOwnerName()));
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

        SakuraWeaponsMod.LOGGER.info("Registered Takasha NPC network payloads for Minecraft 26.2");
    }
}
