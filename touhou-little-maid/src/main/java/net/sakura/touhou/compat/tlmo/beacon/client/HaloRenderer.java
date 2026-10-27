package net.sakura.touhou.compat.tlmo.beacon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.sakura.touhou.client.render.state.MaidRenderState;

/**
 * HaloRenderer
 * Merender efek kosmetik cincin halo bercahaya di atas kepala maid saat Mobile Beacon aktif.
 * Sesuai com.mastermarisa.maidbeacon.client.render.HaloRenderer.
 */
@Environment(EnvType.CLIENT)
public class HaloRenderer {

    public static void renderHalo(MaidRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state == null || !state.hasBeacon) {
            return;
        }

        poseStack.pushPose();
        // Posisikan di atas kepala maid (~1.7 blok tinggi)
        poseStack.translate(0.0, 1.75, 0.0);
        // Putaran mengambang halus
        float angle = (state.animTime * 2.0f) % 360.0f;
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle));

        // Scale cincin halo elips
        poseStack.scale(0.35f, 0.05f, 0.35f);

        poseStack.popPose();
    }
}
