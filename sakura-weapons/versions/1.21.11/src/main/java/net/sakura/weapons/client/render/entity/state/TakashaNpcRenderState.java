package net.sakura.weapons.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.Rotations;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class TakashaNpcRenderState extends AvatarRenderState {
    public Rotations headPose = new Rotations(0f, 0f, 0f);
    public Rotations bodyPose = new Rotations(0f, 0f, 0f);
    public Rotations leftArmPose = new Rotations(0f, 0f, 0f);
    public Rotations rightArmPose = new Rotations(0f, 0f, 0f);
    public Rotations leftLegPose = new Rotations(0f, 0f, 0f);
    public Rotations rightLegPose = new Rotations(0f, 0f, 0f);

    public int posePreset = 0;
    public float bedHeightOffset = 0.0f;
    public String emoteId = "";
    public float customYaw = 0.0f;
    public boolean isSmall = false;
    public boolean isLocked = false;
    public boolean showName = true;
    public Pose currentPose = Pose.STANDING;

    public Identifier customSkinTexture;
    public boolean isSlim = false;
    public ItemStack wingsItem = ItemStack.EMPTY;
    public float animTime = 0.0f;
    public boolean isEmotePlaying = false;
}
