package net.sakura.weapons.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.sakura.weapons.maid.model.MaidModelRegistry;

@Environment(EnvType.CLIENT)
public class TakashaMaidRenderState extends LivingEntityRenderState {
    public String modelId = MaidModelRegistry.DEFAULT_MODEL_ID;
    public boolean isSitting = false;
    public boolean isBegging = false;
    public float animTime = 0.0f;
    public float walkPos = 0.0f;
    public float walkSpeed = 0.0f;
    public float attackTime = 0.0f;
    public float netHeadYaw = 0.0f;
    public float headPitch = 0.0f;
}
