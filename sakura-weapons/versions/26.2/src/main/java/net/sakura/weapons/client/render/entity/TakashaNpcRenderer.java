package net.sakura.weapons.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.sakura.weapons.client.render.entity.model.TakashaNpcModel;
import net.sakura.weapons.client.render.entity.state.TakashaNpcRenderState;
import net.sakura.weapons.client.skin.NpcSkinManager;
import net.sakura.weapons.entity.TakashaNpcEntity;

@Environment(EnvType.CLIENT)
public class TakashaNpcRenderer extends LivingEntityRenderer<TakashaNpcEntity, AvatarRenderState, TakashaNpcModel> {

    private final TakashaNpcModel defaultModel;
    private final TakashaNpcModel slimModel;

    public TakashaNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new TakashaNpcModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.defaultModel = this.model;
        this.slimModel = new TakashaNpcModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);

        // Standard Armor Layer
        this.addLayer(new HumanoidArmorLayer<>(
            this,
            ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new),
            context.getEquipmentRenderer()
        ));

        // Player Item in Hand Layer
        this.addLayer(new PlayerItemInHandLayer<>(this));

        // Wings / Elytra Layer
        this.addLayer(new WingsLayer<>(this, context.getModelSet(), context.getEquipmentRenderer()));

        // Custom Head / Skulls Layer
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new TakashaNpcRenderState();
    }

    @Override
    public void extractRenderState(TakashaNpcEntity entity, AvatarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTick, this.itemModelResolver);

        state.rightArmPose = !entity.getMainHandItem().isEmpty() ? net.minecraft.client.model.HumanoidModel.ArmPose.ITEM : net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
        state.leftArmPose = !entity.getOffhandItem().isEmpty() ? net.minecraft.client.model.HumanoidModel.ArmPose.ITEM : net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;

        if (state instanceof TakashaNpcRenderState npcState) {
            int currentReplayTick = net.sakura.weapons.compat.flashback.FlashbackCompatHelper.getCurrentReplayTick();
            var override = net.sakura.weapons.client.replay.ReplayPoseOverrideManager.getOverride(entity.getUUID(), currentReplayTick);
            int preset = (override != null && override.hasPreset()) ? override.preset() : entity.getPosePreset();
            String emote = (override != null && override.hasEmote()) ? override.emoteId() : ((override != null && override.hasPreset()) ? "" : entity.getEmoteId());
            byte emoteMode = (override != null) ? override.playMode() : entity.getEmotePlayMode();

            if (override != null && override.hasCustomRotations()) {
                if (override.getHeadRotations() != null) entity.setHeadPose(override.getHeadRotations());
                if (override.getBodyRotations() != null) entity.setBodyPose(override.getBodyRotations());
                if (override.getLeftArmRotations() != null) entity.setLeftArmPose(override.getLeftArmRotations());
                if (override.getRightArmRotations() != null) entity.setRightArmPose(override.getRightArmRotations());
                if (override.getLeftLegRotations() != null) entity.setLeftLegPose(override.getLeftLegRotations());
                if (override.getRightLegRotations() != null) entity.setRightLegPose(override.getRightLegRotations());
            } else if (preset > 0 && preset <= 32) {
                entity.applyPosePreset(preset);
            } else if (preset == 0 && emote != null && !emote.isBlank()) {
                if (override != null && override.hasEmote()) {
                    net.minecraft.core.Rotations[] rots = net.sakura.weapons.compat.EmotecraftCompat.sampleRotationsAtTick(emote, npcState.animTime);
                    if (rots != null && rots.length >= 6) {
                        entity.setHeadPose(rots[0]);
                        entity.setBodyPose(rots[1]);
                        entity.setRightArmPose(rots[2]);
                        entity.setLeftArmPose(rots[3]);
                        entity.setRightLegPose(rots[4]);
                        entity.setLeftLegPose(rots[5]);
                    }
                }
            }

            npcState.headPose = entity.getHeadPose();
            npcState.bodyPose = entity.getBodyPose();
            npcState.leftArmPose = entity.getLeftArmPose();
            npcState.rightArmPose = entity.getRightArmPose();
            npcState.leftLegPose = entity.getLeftLegPose();
            npcState.rightLegPose = entity.getRightLegPose();

            npcState.posePreset = preset;
            npcState.bedHeightOffset = (override != null && override.bedHeightOffset != 0f) ? override.bedHeightOffset : entity.getBedHeightOffset();
            npcState.emoteId = emote;
            npcState.customYaw = (override != null && override.yawRotation != 0f) ? override.yawRotation : entity.getYawRotation();
            npcState.isSmall = entity.isSmall();
            npcState.isLocked = entity.isLocked();
            npcState.showName = entity.shouldShowName();
            npcState.currentPose = entity.getPose();
            npcState.wingsItem = entity.getWingsItem();

            if (net.sakura.weapons.compat.flashback.FlashbackCompatHelper.isFlashbackActive()) {
                npcState.animTime = (float) net.sakura.weapons.compat.flashback.FlashbackCompatHelper.getPartialReplayTick();
            } else {
                long gameTime = entity.level() != null ? entity.level().getGameTime() : 0L;
                if (gameTime > 0) {
                    npcState.animTime = (float) gameTime + partialTick;
                } else if (entity.tickCount > 0) {
                    npcState.animTime = (float) entity.tickCount + partialTick;
                } else {
                    // In ReplayMod timeline editor when scrubbing or paused, provide smooth live time
                    npcState.animTime = ((float) (System.currentTimeMillis() % 1000000L) / 50.0f) + partialTick;
                }
            }

            if (emote != null && !emote.isBlank()) {
                net.sakura.weapons.compat.EmotecraftCompat.ensureEmotePlaying(entity, emote, emoteMode);
            }
            npcState.isEmotePlaying = net.sakura.weapons.compat.EmotecraftCompat.isPlayingEmote(entity);

            String skinInput = entity.getSkinUrl();
            npcState.customSkinTexture = NpcSkinManager.getSkinTexture(skinInput);
            PlayerModelType modelType = NpcSkinManager.getSkinModel(skinInput, entity.getSkinModel());
            npcState.isSlim = modelType == PlayerModelType.SLIM;

            if (npcState.customSkinTexture != null) {
                state.skin = new net.minecraft.world.entity.player.PlayerSkin(
                    new net.minecraft.core.ClientAsset.ResourceTexture(npcState.customSkinTexture, npcState.customSkinTexture),
                    null,
                    null,
                    modelType,
                    true
                );
            } else {
                state.skin = DefaultPlayerSkin.getDefaultSkin();
            }
        } else {
            state.skin = DefaultPlayerSkin.getDefaultSkin();
        }

        state.showHat = true;
        state.showJacket = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showCape = true;

        net.sakura.weapons.compat.EmotecraftCompat.extractAnimationState(entity, state, partialTick);

        // Clear any 3D voxel meshes stored on the entity by skinlayers3d
        try {
            for (java.lang.reflect.Method m : entity.getClass().getMethods()) {
                if (m.getName().equals("clearMeshes") && m.getParameterCount() == 0) {
                    m.invoke(entity);
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        if (state.skin != null && state.skin.body() != null) {
            return state.skin.body().texturePath();
        }
        return DefaultPlayerSkin.getDefaultTexture();
    }

    @Override
    public void submit(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        boolean isSlim = (state instanceof TakashaNpcRenderState npcState) && npcState.isSlim;
        this.model = isSlim ? this.slimModel : this.defaultModel;
        this.defaultModel.disableSkinLayersCompat();
        this.slimModel.disableSkinLayersCompat();
        try {
            super.submit(state, poseStack, collector, cameraRenderState);
        } catch (Throwable t) {
            net.sakura.weapons.SakuraWeaponsMod.LOGGER.warn("Suppressed rendering error for Takasha NPC: {}", t.getMessage());
        } finally {
            this.defaultModel.clearInjectedMeshes();
            this.slimModel.clearInjectedMeshes();
        }
    }

    @Override
    protected void setupRotations(AvatarRenderState state, PoseStack poseStack, float bodyYaw, float scale) {
        if (state instanceof TakashaNpcRenderState npcState) {
            float yaw = npcState.customYaw;
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - yaw));
        } else {
            super.setupRotations(state, poseStack, bodyYaw, scale);
        }
    }

    @Override
    protected void scale(AvatarRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        if (state instanceof TakashaNpcRenderState npcState) {
            if (npcState.bedHeightOffset != 0.0f) {
                poseStack.translate(0.0, npcState.bedHeightOffset, 0.0);
            }
            if (npcState.isSmall) {
                poseStack.scale(0.5f, 0.5f, 0.5f);
            }
            if (npcState.posePreset >= 1 && npcState.posePreset <= 7) {
                // First-class Lying Down / Bed Alignment:
                // When entity is in sleeping/lying pose without a vanilla bed block or during ReplayMod playback,
                // apply PoseStack rotation directly so the NPC rests horizontally flat on ground or mattress.
                switch (npcState.posePreset) {
                    case 2 -> { // Prone / Tengkurap: face and chest down
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
                        poseStack.translate(0.0, 0.2, 0.25);
                    }
                    case 3 -> { // Right side / Miring Kanan: roll onto right side along spine (YP)
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
                        poseStack.translate(0.0, 0.2, -0.3125);
                        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
                    }
                    case 4 -> { // Left side / Miring Kiri: roll onto left side along spine (YP)
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
                        poseStack.translate(0.0, 0.2, -0.3125);
                        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90.0F));
                    }
                    default -> { // 1 (Bed), 5 (Lazing), 6 (Warrior's Rest), 7 (KO): On back / Terlentang
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
                        poseStack.translate(0.0, 0.2, -0.25);
                    }
                }
            } else if (npcState.posePreset == 0 && npcState.emoteId != null && !npcState.emoteId.isBlank()) {
                String eKey = npcState.emoteId.toLowerCase(java.util.Locale.ROOT);
                if (eKey.contains("sleep") || eKey.contains("tidur") || eKey.contains("rebahan") || eKey.contains("lay") || eKey.contains("rest")) {
                    if (eKey.contains("miring_kanan") || eKey.contains("side_right")) {
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
                        poseStack.translate(0.0, 0.2, -0.3125);
                        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
                    } else if (eKey.contains("miring_kiri") || eKey.contains("side_left") || eKey.contains("miring")) {
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
                        poseStack.translate(0.0, 0.2, -0.3125);
                        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90.0F));
                    } else if (eKey.contains("tengkurap") || eKey.contains("prone")) {
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
                        poseStack.translate(0.0, 0.2, 0.25);
                    } else {
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
                        poseStack.translate(0.0, 0.2, -0.25);
                    }
                }
            }
        }
    }

    @Override
    protected boolean shouldShowName(TakashaNpcEntity entity, double distanceSq) {
        if (!entity.shouldShowName()) {
            return false;
        }
        return super.shouldShowName(entity, distanceSq);
    }
}
