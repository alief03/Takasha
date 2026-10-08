package net.sakura.weapons.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sakura.weapons.registry.DragonMechaOverlordItems;
import net.sakura.weapons.registry.ModItems;
import net.sakura.weapons.registry.ValentineItems;

public class SakuraWingsFeatureRenderer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
    private final ItemModelResolver itemModelResolver;
    private final ItemStackRenderState itemRenderState = new ItemStackRenderState();
    private ItemStack cachedSakuraDisplayStack = null;
    private ItemStack cachedPinkLegacyDisplayStack = null;
    private ItemStack cachedValentineDisplayStack = null;
    private ItemStack cachedDragonMechaOverlordWingStack = null;
    private ItemStack cachedDragonMechaOverlordWing1Stack = null;

    public SakuraWingsFeatureRenderer(RenderLayerParent<S, M> context, ItemModelResolver itemModelResolver) {
        super(context);
        this.itemModelResolver = itemModelResolver;
    }

    private ItemStack getSakuraDisplayStack() {
        if (this.cachedSakuraDisplayStack == null) {
            try {
                this.cachedSakuraDisplayStack = new ItemStack(ModItems.SAKURA_WING);
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }
        return this.cachedSakuraDisplayStack;
    }

    private ItemStack getPinkLegacyDisplayStack() {
        if (this.cachedPinkLegacyDisplayStack == null) {
            try {
                this.cachedPinkLegacyDisplayStack = new ItemStack(ModItems.PINK_LEGACY_WINGS);
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }
        return this.cachedPinkLegacyDisplayStack;
    }

    private ItemStack getValentineDisplayStack() {
        if (this.cachedValentineDisplayStack == null) {
            try {
                this.cachedValentineDisplayStack = new ItemStack(ValentineItems.VALENTINE_WING);
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }
        return this.cachedValentineDisplayStack;
    }

    private ItemStack getDragonMechaOverlordWingDisplayStack() {
        if (this.cachedDragonMechaOverlordWingStack == null) {
            try {
                this.cachedDragonMechaOverlordWingStack = new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_WING);
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }
        return this.cachedDragonMechaOverlordWingStack;
    }

    private ItemStack getDragonMechaOverlordWing1DisplayStack() {
        if (this.cachedDragonMechaOverlordWing1Stack == null) {
            try {
                this.cachedDragonMechaOverlordWing1Stack = new ItemStack(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_WING_1);
            } catch (Throwable ignored) {
                return ItemStack.EMPTY;
            }
        }
        return this.cachedDragonMechaOverlordWing1Stack;
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
        ItemStack chestItem = state.chestEquipment;
        if (chestItem == null || chestItem.isEmpty()) {
            return;
        }

        ItemStack stackToRender = resolveWingsStack(chestItem);
        if (stackToRender == null || stackToRender.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // Attach directly to humanoid torso (body)
        // This ensures the decorative backpiece follows the player's body posture (sneaking, turning)
        // without any split-wing flapping or deformed rotation.
        this.getParentModel().body.translateAndRotate(poseStack);

        // Position on the outer surface of the back:
        // Torso pivot is at the neck (Y=0). Back surface is at Z = +2.0 / 16.0 = 0.125f (+ offset to avoid clipping).
        // Mid-back is around Y = 0.35f.
        poseStack.translate(0.0f, 0.35f, 0.14f);

        // Rotate so that:
        // 1. Vertical axis is flipped upright (+Y_model branches reach UP towards -Y_torso / sky): Axis.XP.rotationDegrees(180.0f)
        // 2. Wingspan (Z axis in wing.json) aligns horizontally across shoulders (X_torso): Axis.YP.rotationDegrees(90.0f)
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));

        // Uniform proportional scale
        poseStack.scale(0.85f, 0.85f, 0.85f);

        // Submit the Wings item model with NONE to avoid item frame/fixed distortion
        this.itemModelResolver.updateForTopItem(
            this.itemRenderState,
            stackToRender,
            ItemDisplayContext.NONE,
            null,
            null,
            0
        );

        this.itemRenderState.submit(
            poseStack,
            submitNodeCollector,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            state.outlineColor
        );

        poseStack.popPose();
    }

    private ItemStack resolveWingsStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.is(ModItems.SAKURA_WING)) {
            return stack;
        }
        if (stack.is(ModItems.PINK_LEGACY_WINGS)) {
            return stack;
        }
        if (stack.is(ValentineItems.VALENTINE_WING)) {
            return stack;
        }
        if (stack.is(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_WING)) {
            return stack;
        }
        if (stack.is(DragonMechaOverlordItems.DRAGON_MECHA_OVERLORD_WING_1)) {
            return stack;
        }
        // ONLY Elytra is supported as base item for wings (Chestplates are strictly ignored)
        if (stack.is(Items.ELYTRA)) {
            // Server Plugin Conflict Guard (ItemsAdder / Oraxen):
            // If the item in the chest slot already carries server-side CustomModelData
            // or specific ItemsAdder custom data, yield rendering to prevent double wings / z-fighting.
            try {
                if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA)) {
                    return null;
                }
                if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
                    var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                    if (customData != null) {
                        var tag = customData.copyTag();
                        String tagString = tag.toString().toLowerCase();
                        if (tagString.contains("itemsadder") || tagString.contains("oraxen") || tagString.contains("ia_gui")) {
                            return null;
                        }
                    }
                }
            } catch (Throwable ignored) {}

            String name = stack.getHoverName().getString().toLowerCase();
            boolean isWingWord = name.contains("wing") || name.contains("sayap");
            if (!isWingWord) {
                return null;
            }

            if (name.contains("dragon") || name.contains("mecha") || name.contains("overlord")) {
                if (name.contains("1") || name.contains("sharp") || name.contains("tajam")) {
                    return getDragonMechaOverlordWing1DisplayStack();
                }
                return getDragonMechaOverlordWingDisplayStack();
            }
            if (name.contains("valentine")) {
                return getValentineDisplayStack();
            }
            if (name.contains("pink") || name.contains("legacy")) {
                return getPinkLegacyDisplayStack();
            }
            if (name.contains("sakura")) {
                return getSakuraDisplayStack();
            }
        }
        return null;
    }
}
