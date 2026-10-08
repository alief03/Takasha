package net.sakura.weapons.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.geom.ModelPart;
import net.sakura.weapons.SakuraWeaponsMod;

import java.lang.reflect.Method;

/**
 * Compatibility layer for Bendable Cuboids mod.
 * Allows smooth articulation and bending of limbs (elbows, knees, and torso)
 * on Takasha NPC mannequins without hard runtime dependencies.
 */
@Environment(EnvType.CLIENT)
public class BendableCuboidsCompat {

    private static final boolean BENDABLE_CUBOIDS_LOADED;
    private static Method bendMethod = null;
    private static Method resetBendMethod = null;
    private static Method applyTorsoBendMethod = null;
    private static Method useSodiumRenderingMethod = null;

    static {
        boolean loaded = FabricLoader.getInstance().isModLoaded("bendable_cuboids");
        if (loaded) {
            try {
                Class<?> helperClass = Class.forName("com.zigythebird.bendable_cuboids.impl.compatibility.PlayerBendHelper");
                bendMethod = helperClass.getMethod("bend", ModelPart.class, float.class);
                resetBendMethod = helperClass.getMethod("resetBend", ModelPart.class);
                applyTorsoBendMethod = helperClass.getMethod("applyTorsoBendToMatrix", PoseStack.class, float.class);
                SakuraWeaponsMod.LOGGER.info("Bendable Cuboids mod detected: Realistic joint bending enabled for Takasha NPCs!");
            } catch (Throwable t) {
                SakuraWeaponsMod.LOGGER.warn("Bendable Cuboids detected but PlayerBendHelper reflection failed: {}", t.getMessage());
            }

            try {
                Class<?> sodiumHelperClass = Class.forName("com.zigythebird.bendable_cuboids.api.SodiumHelper");
                useSodiumRenderingMethod = sodiumHelperClass.getMethod("bc$useSodiumRendering", boolean.class);
            } catch (Throwable ignored) {}
        }
        BENDABLE_CUBOIDS_LOADED = loaded && bendMethod != null;
    }

    public static boolean isAvailable() {
        return BENDABLE_CUBOIDS_LOADED;
    }

    /**
     * Disables Sodium's cached static quad rendering on the model root,
     * ensuring dynamic bent vertices are rendered when Sodium is active.
     */
    public static void disableSodiumCache(ModelPart part) {
        if (!BENDABLE_CUBOIDS_LOADED || part == null) {
            return;
        }
        try {
            if (useSodiumRenderingMethod != null) {
                useSodiumRenderingMethod.invoke(part, false);
            } else {
                for (Method m : part.getClass().getMethods()) {
                    if (m.getName().equals("bc$useSodiumRendering") && m.getParameterCount() == 1) {
                        m.invoke(part, false);
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Bends a ModelPart cuboid by the given angle in radians with safety clamping.
     * Angles exceeding 1.45 rad (~83 deg) cause vertex flipping in BendUtil.
     */
    public static void bend(ModelPart part, float radians) {
        if (!BENDABLE_CUBOIDS_LOADED || part == null) {
            return;
        }
        float safeRadians = Math.max(-1.45f, Math.min(1.45f, radians));
        try {
            bendMethod.invoke(null, part, safeRadians);
        } catch (Throwable ignored) {}
    }

    /**
     * Resets any bend applied to a ModelPart back to 0.
     */
    public static void resetBend(ModelPart part) {
        if (!BENDABLE_CUBOIDS_LOADED || part == null) {
            return;
        }
        try {
            resetBendMethod.invoke(null, part);
        } catch (Throwable ignored) {}
    }

    /**
     * Applies torso bend transformation directly onto a PoseStack.
     */
    public static void applyTorsoBendToMatrix(PoseStack poseStack, float radians) {
        if (!BENDABLE_CUBOIDS_LOADED || poseStack == null) {
            return;
        }
        float safeRadians = Math.max(-1.45f, Math.min(1.45f, radians));
        try {
            applyTorsoBendMethod.invoke(null, poseStack, safeRadians);
        } catch (Throwable ignored) {}
    }

    /**
     * Helper to bend an arm and its outer sleeve layer synchronously.
     */
    public static void bendArm(ModelPart arm, ModelPart sleeve, float radians) {
        bend(arm, radians);
        if (sleeve != null) {
            bend(sleeve, radians);
        }
    }

    /**
     * Helper to bend a leg and its outer pants layer synchronously.
     */
    public static void bendLeg(ModelPart leg, ModelPart pants, float radians) {
        bend(leg, radians);
        if (pants != null) {
            bend(pants, radians);
        }
    }

    /**
     * Helper to bend the torso and its outer jacket layer synchronously.
     */
    public static void bendBody(ModelPart body, ModelPart jacket, float radians) {
        bend(body, radians);
        if (jacket != null) {
            bend(jacket, radians);
        }
    }
}
