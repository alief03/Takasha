package net.sakura.weapons.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.sakura.weapons.registry.DragonMechaOverlordItems;
import net.sakura.weapons.registry.ModItems;
import net.sakura.weapons.registry.ValentineItems;
import net.sakura.weapons.util.DragonMechaOverlordHatUtil;
import net.sakura.weapons.util.SakuraHatUtil;
import net.sakura.weapons.util.ValentineHatUtil;

/**
 * Feature renderer for rendering the 3D Sakura Hat, Valentine Hat, or Dragon Mecha Overlord Hat model on player entities and armor stands
 * whenever a helmet item is worn that matches the hat criteria (native or renamed).
 */
public class SakuraHatFeatureRenderer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
    private final ItemModelResolver itemModelResolver;
    private final ItemStackRenderState itemRenderState = new ItemStackRenderState();
    private ItemStack cachedHatStack = null;
    private ItemStack cachedValentineHatStack = null;
    private ItemStack cachedDragonMechaOverlordHatStack = null;

    public SakuraHatFeatureRenderer(RenderLayerParent<S, M> context, ItemModelResolver itemModelResolver) {
        super(context);
        this.itemModelResolver = itemModelResolver;
    }

    private ItemStack getHatDisplayStack() {
        if (this.cachedHatStack == null) {
            try {
                this.cachedHatStack = new ItemStack(ModItems.SAKURA_HAT);
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }
        return this.cachedHatStack;
    }

    private ItemStack getValentineHatDisplayStack() {
        if (this.cachedValentineHatStack == null) {
            try {
                this.cachedValentineHatStack = new ItemStack(ValentineItems.VALENTINE_HAT);
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }
        return this.cachedValentineHatStack;
    }

    private ItemStack getDragonMechaOverlordHatDisplayStack() {
        if (this.cachedDragonMechaOverlordHatStack == null) {
            try {
                this.cachedDragonMechaOverlordHatStack = new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_HAT);
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }
        return this.cachedDragonMechaOverlordHatStack;
    }

    @Override
    public void submit(
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int packedLight,
        S state,
        float yRot,
        float xRot
    ) {
        ItemStack headItem = state.headEquipment;
        if (headItem == null || headItem.isEmpty()) {
            return;
        }

        ItemStack displayStack = ItemStack.EMPTY;
        if (SakuraHatUtil.isSakuraHat(headItem)) {
            displayStack = getHatDisplayStack();
        } else if (ValentineHatUtil.isValentineHat(headItem)) {
            displayStack = getValentineHatDisplayStack();
        } else if (DragonMechaOverlordHatUtil.isDragonMechaOverlordHat(headItem)) {
            displayStack = getDragonMechaOverlordHatDisplayStack();
        }

        if (displayStack.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // 1. Root transform (crouching, scaling)
        M parentModel = this.getParentModel();
        parentModel.root().translateAndRotate(poseStack);

        // 2. Head transform (head rotation, pitch, yaw)
        parentModel.translateToHead(poseStack);

        // 3. Minecraft 26.2 standard head item positioning
        CustomHeadLayer.translateToHead(poseStack, CustomHeadLayer.Transforms.DEFAULT);

        // 4. Update and render 3D hat model with HEAD display context
        this.itemModelResolver.updateForTopItem(
            this.itemRenderState,
            displayStack,
            ItemDisplayContext.HEAD,
            null,
            null,
            0
        );

        this.itemRenderState.submit(
            poseStack,
            submitNodeCollector,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            0
        );

        poseStack.popPose();
    }
}
