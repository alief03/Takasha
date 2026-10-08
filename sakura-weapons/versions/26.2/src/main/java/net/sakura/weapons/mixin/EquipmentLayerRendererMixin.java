package net.sakura.weapons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import java.util.List;
import java.util.Map;
import net.sakura.weapons.client.render.ModEquipmentAssets;
import net.sakura.weapons.util.DragonMechaOverlordArmorUtil;
import net.sakura.weapons.util.DragonMechaOverlordHatUtil;
import net.sakura.weapons.util.PinkLegacyArmorUtil;
import net.sakura.weapons.util.SakuraHatUtil;
import net.sakura.weapons.util.ValentineArmorUtil;
import net.sakura.weapons.util.ValentineHatUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin to redirect armor equipment asset lookup to the Pink Legacy, Valentine, or Dragon Mecha Overlord texture layers
 * whenever a player wears a matching armor piece (native or renamed), and to
 * suppress vanilla helmet textures when wearing a Sakura Hat, Valentine Hat, or Dragon Mecha Overlord Hat.
 */
@Mixin(EquipmentLayerRenderer.class)
public class EquipmentLayerRendererMixin {

    @Unique
    private static final EquipmentClientInfo EMPTY_EQUIPMENT_INFO = new EquipmentClientInfo(Map.of());

    @Unique
    private static final EquipmentClientInfo PINK_LEGACY_INFO = new EquipmentClientInfo(Map.of(
        EquipmentClientInfo.LayerType.HUMANOID, List.of(new EquipmentClientInfo.Layer(Identifier.fromNamespaceAndPath("pink_legacy", "pink_legacy"))),
        EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS, List.of(new EquipmentClientInfo.Layer(Identifier.fromNamespaceAndPath("pink_legacy", "pink_legacy")))
    ));

    @Unique
    private static final EquipmentClientInfo VALENTINE_INFO = new EquipmentClientInfo(Map.of(
        EquipmentClientInfo.LayerType.HUMANOID, List.of(new EquipmentClientInfo.Layer(Identifier.fromNamespaceAndPath("valentine", "valentine"))),
        EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS, List.of(new EquipmentClientInfo.Layer(Identifier.fromNamespaceAndPath("valentine", "valentine")))
    ));

    @Unique
    private static final EquipmentClientInfo DRAGON_MECHA_OVERLORD_INFO = new EquipmentClientInfo(Map.of(
        EquipmentClientInfo.LayerType.HUMANOID, List.of(new EquipmentClientInfo.Layer(Identifier.fromNamespaceAndPath("dragon_mecha_overlord", "dragon_mecha_overlord"))),
        EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS, List.of(new EquipmentClientInfo.Layer(Identifier.fromNamespaceAndPath("dragon_mecha_overlord", "dragon_mecha_overlord")))
    ));

    @Redirect(
        method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/EquipmentAssetManager;get(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/client/resources/model/EquipmentClientInfo;"
        )
    )
    private <S> EquipmentClientInfo redirectEquipmentAssetGet(
        EquipmentAssetManager manager,
        ResourceKey<EquipmentAsset> assetKey,
        EquipmentClientInfo.LayerType layerType,
        ResourceKey<EquipmentAsset> originalAssetKey,
        Model<? super S> model,
        S state,
        ItemStack itemStack,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int light,
        Identifier identifier,
        int outlineColor,
        int scale
    ) {
        if (PinkLegacyArmorUtil.isPinkLegacyArmor(itemStack)) {
            EquipmentClientInfo info = manager.get(ModEquipmentAssets.PINK_LEGACY);
            if (info != null && !info.getLayers(layerType).isEmpty()) {
                return info;
            }
            return PINK_LEGACY_INFO;
        }
        if (ValentineArmorUtil.isValentineArmor(itemStack)) {
            EquipmentClientInfo info = manager.get(ModEquipmentAssets.VALENTINE);
            if (info != null && !info.getLayers(layerType).isEmpty()) {
                return info;
            }
            return VALENTINE_INFO;
        }
        if (DragonMechaOverlordArmorUtil.isDragonMechaOverlordArmor(itemStack)) {
            EquipmentClientInfo info = manager.get(ModEquipmentAssets.DRAGON_MECHA_OVERLORD);
            if (info != null && !info.getLayers(layerType).isEmpty()) {
                return info;
            }
            return DRAGON_MECHA_OVERLORD_INFO;
        }
        if (SakuraHatUtil.isSakuraHat(itemStack) || ValentineHatUtil.isValentineHat(itemStack) || DragonMechaOverlordHatUtil.isDragonMechaOverlordHat(itemStack)) {
            return EMPTY_EQUIPMENT_INFO;
        }
        EquipmentClientInfo info = manager.get(assetKey);
        if (assetKey.equals(ModEquipmentAssets.PINK_LEGACY) && (info == null || info.getLayers(layerType).isEmpty())) {
            return PINK_LEGACY_INFO;
        }
        if (assetKey.equals(ModEquipmentAssets.VALENTINE) && (info == null || info.getLayers(layerType).isEmpty())) {
            return VALENTINE_INFO;
        }
        if (assetKey.equals(ModEquipmentAssets.DRAGON_MECHA_OVERLORD) && (info == null || info.getLayers(layerType).isEmpty())) {
            return DRAGON_MECHA_OVERLORD_INFO;
        }
        return info;
    }
}
