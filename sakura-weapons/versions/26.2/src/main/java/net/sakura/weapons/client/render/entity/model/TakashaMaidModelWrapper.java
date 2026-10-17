package net.sakura.weapons.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.sakura.weapons.client.render.bedrock.animation.BedrockMaidAnimation;
import net.sakura.weapons.client.render.bedrock.model.BedrockModel;
import net.sakura.weapons.client.render.entity.state.TakashaMaidRenderState;
import net.sakura.weapons.maid.model.MaidModelRegistry;

import java.util.Collections;

@Environment(EnvType.CLIENT)
public class TakashaMaidModelWrapper extends EntityModel<TakashaMaidRenderState> {

    public TakashaMaidModelWrapper() {
        super(new ModelPart(Collections.emptyList(), Collections.emptyMap()), RenderTypes::entityCutout);
    }

    @Override
    public void setupAnim(TakashaMaidRenderState state) {
        super.setupAnim(state);
        BedrockModel bedrockModel = MaidModelRegistry.getModel(state.modelId);
        if (bedrockModel != null) {
            BedrockMaidAnimation.apply(bedrockModel, state);
        }
    }
}
