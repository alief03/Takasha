package net.sakura.weapons.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.sakura.weapons.client.render.bedrock.animation.BedrockMaidAnimation;
import net.sakura.weapons.client.render.bedrock.model.BedrockModel;
import net.sakura.weapons.client.render.entity.model.TakashaMaidModelWrapper;
import net.sakura.weapons.client.render.entity.state.TakashaMaidRenderState;
import net.sakura.weapons.entity.TakashaMaidEntity;
import net.sakura.weapons.maid.model.MaidModelRegistry;

@Environment(EnvType.CLIENT)
public class TakashaMaidRenderer extends LivingEntityRenderer<TakashaMaidEntity, TakashaMaidRenderState, TakashaMaidModelWrapper> {

    public TakashaMaidRenderer(EntityRendererProvider.Context context) {
        super(context, new TakashaMaidModelWrapper(), 0.5f);
    }

    @Override
    public TakashaMaidRenderState createRenderState() {
        return new TakashaMaidRenderState();
    }

    @Override
    public void extractRenderState(TakashaMaidEntity entity, TakashaMaidRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        state.modelId = entity.getModelId();
        state.isSitting = entity.isInSittingPose() || entity.isOrderedToSit();
        state.isBegging = entity.isBegging();
        state.animTime = (float) entity.tickCount + partialTick;
        state.walkPos = state.walkAnimationPos;
        state.walkSpeed = state.walkAnimationSpeed;
        state.attackTime = entity.getAttackAnim(partialTick);
        state.netHeadYaw = state.yRot;
        state.headPitch = state.xRot;
    }

    @Override
    public Identifier getTextureLocation(TakashaMaidRenderState state) {
        return MaidModelRegistry.getTexture(state.modelId);
    }

    @Override
    public void submit(TakashaMaidRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        BedrockModel bedrockModel = MaidModelRegistry.getModel(state.modelId);
        if (bedrockModel != null) {
            Identifier texture = getTextureLocation(state);
            RenderType renderType = RenderTypes.entityCutout(texture);
            int overlay = getOverlayCoords(state, getWhiteOverlayProgress(state));
            int light = state.lightCoords;

            poseStack.pushPose();
            this.scale(state, poseStack);
            this.setupRotations(state, poseStack, state.bodyRot, state.scale);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(0.0F, -1.501F, 0.0F);

            collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
                PoseStack stack = new PoseStack();
                stack.last().set(pose);
                BedrockMaidAnimation.apply(bedrockModel, state);
                bedrockModel.render(stack, consumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
            });

            poseStack.popPose();
        }

        // Delegate to super.submit for nametag and leash rendering
        super.submit(state, poseStack, collector, cameraRenderState);
    }
}
