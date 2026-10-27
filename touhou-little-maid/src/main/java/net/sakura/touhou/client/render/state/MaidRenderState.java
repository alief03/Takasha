package net.sakura.touhou.client.render.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class MaidRenderState extends AvatarRenderState {
    public String modelId = "touhou_little_maid:hakurei_reimu";
    public Identifier textureLocation;
    public boolean isSitting = false;
    public int taskModeOrdinal = 0;
    public int favorability = 0;
    public float distanceToCamera = 0.0f;
    public boolean isOffscreen = false;
    public String activeSetResonance = "";
    public boolean isBegging = false;
    public float animTime = 0.0f;
    public ItemStack mainHandStack = ItemStack.EMPTY;
    public ItemStack offHandStack = ItemStack.EMPTY;
    public boolean hasBeacon = false;
    public boolean isYsmModel = false;
    public String ysmModelId = "";
    public String ysmTexture = "";
}
