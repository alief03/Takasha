package net.sakura.touhou.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
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
import net.sakura.touhou.client.render.cache.BedrockModelCache;
import net.sakura.touhou.client.render.model.MaidModelWrapper;
import net.sakura.touhou.client.render.state.MaidRenderState;
import net.sakura.touhou.entity.EntityMaid;

/**
 * MaidRenderer
 * Minecraft 26.2 LivingEntityRenderer untuk EntityMaid.
 * Menggunakan AvatarRenderState dan PlayerModel hierarchy untuk kompatibilitas penuh dengan
 * HumanoidArmorLayer, PlayerItemInHandLayer, WingsLayer, dan CustomHeadLayer.
 */
@Environment(EnvType.CLIENT)
public class MaidRenderer extends LivingEntityRenderer<EntityMaid, AvatarRenderState, MaidModelWrapper> {

    public MaidRenderer(EntityRendererProvider.Context context) {
        super(context, new MaidModelWrapper(context.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.4f);

        // Armor Layer
        this.addLayer(new HumanoidArmorLayer<>(
            this,
            ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new),
            context.getEquipmentRenderer()
        ));

        // Held Item Layer
        this.addLayer(new PlayerItemInHandLayer<>(this));

        // Wings Layer
        this.addLayer(new WingsLayer<>(this, context.getModelSet(), context.getEquipmentRenderer()));

        // Custom Head / Skulls Layer
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new MaidRenderState();
    }

    @Override
    public void extractRenderState(EntityMaid entity, AvatarRenderState rawState, float partialTick) {
        super.extractRenderState(entity, rawState, partialTick);

        if (!(rawState instanceof MaidRenderState state)) {
            return;
        }

        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTick, this.itemModelResolver);

        state.modelId = entity.getModelId();
        state.textureLocation = BedrockModelCache.getTexture(state.modelId);
        state.isSitting = entity.isSitting();
        state.taskModeOrdinal = entity.getTaskMode().ordinal();
        state.favorability = entity.getFavorability();
        state.isBegging = entity.isBegging();
        state.activeSetResonance = entity.getActiveSetResonance();

        state.hasBeacon = net.sakura.touhou.compat.tlmo.beacon.BeaconEffectGeneratorBauble.hasBeacon(entity);
        state.isYsmModel = entity.isYsmModel();
        state.ysmModelId = entity.getYsmModelId();
        state.ysmTexture = entity.getYsmModelTexture();

        Minecraft mc = Minecraft.getInstance();
        var cam = (mc.getCameraEntity() != null) ? mc.getCameraEntity() : mc.player;
        if (cam != null) {
            state.distanceToCamera = (float) Math.sqrt(entity.distanceToSqr(cam));
        } else {
            state.distanceToCamera = 0.0f;
        }

        state.animTime = (float) entity.tickCount + partialTick;

        state.rightArmPose = !entity.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.ITEM : HumanoidModel.ArmPose.EMPTY;
        state.leftArmPose = !entity.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.ITEM : HumanoidModel.ArmPose.EMPTY;

        // Custom maid skin / texture setup
        if (state.textureLocation != null) {
            state.skin = new net.minecraft.world.entity.player.PlayerSkin(
                new net.minecraft.core.ClientAsset.ResourceTexture(state.textureLocation, state.textureLocation),
                null,
                null,
                PlayerModelType.SLIM,
                true
            );
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
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState rawState) {
        if (rawState instanceof MaidRenderState state && state.textureLocation != null) {
            return state.textureLocation;
        }
        return BedrockModelCache.getTexture("touhou_little_maid:hakurei_reimu");
    }

    @Override
    protected void scale(AvatarRenderState rawState, PoseStack poseStack) {
        super.scale(rawState, poseStack);
        // Maid proportions: petite anime silhouette (~0.85 scale)
        poseStack.scale(0.85f, 0.85f, 0.85f);
    }

    @Override
    public void submit(AvatarRenderState rawState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        try {
            super.submit(rawState, poseStack, collector, cameraRenderState);
            if (rawState instanceof MaidRenderState state) {
                net.sakura.touhou.compat.tlmo.beacon.client.HaloRenderer.renderHalo(state, poseStack, collector);
            }
        } catch (Throwable t) {
            // Anti-Crash guard against driver/shader crash on AMD Radeon 610M
        }
    }
}
