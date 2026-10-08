package net.sakura.weapons.compat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.sakura.weapons.SakuraWeaponsMod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Soft-dependency integration for KosmX Emotecraft (3.4.0+ for MC 26.2).
 * Safely executes emote animations and auto-detects installed emotes from:
 * 1. Emotecraft runtime EmoteHolder.list
 * 2. .minecraft/emotes/ directory (.json and .emotecraft files)
 * 3. Standard built-in Emotecraft preset emotes
 *
 * Implements continuous loop playback and animation state bridging with PlayerAnimationLib.
 */
public class EmotecraftCompat {

    public record EmoteEntry(String id, String displayName, String author, Identifier iconId) {
        public EmoteEntry(String id, String displayName, String author) {
            this(id, displayName, author, null);
        }

        @Override
        public String toString() {
            if (author != null && !author.isBlank()) {
                return displayName + " (" + author + ")";
            }
            return displayName;
        }
    }

    private static final boolean EMOTECRAFT_LOADED;

    // Reflection handles for KosmX Emotecraft
    private static Class<?> emoteHolderClass = null;
    private static Field listField = null;
    private static Method getEmoteFromUuidMethod = null;
    private static Method holderGetUuidMethod = null;
    private static Method holderGetEmoteMethod = null;
    private static Method holderGetIconMethod = null;
    private static Field holderEmoteField = null;
    private static Field holderNameField = null;
    private static Field holderAuthorField = null;

    private static Method playEmote4ArgMethod = null;
    private static Method playEmote3ArgMethod = null;
    private static Method stopEmoteMethod = null;
    private static Method isPlayingEmoteMethod = null;
    private static Class<?> loopTypeClass = null;
    private static Object loopTypeLoop = null;
    private static Object loopTypePlayOnce = null;
    private static Object loopTypeHold = null;
    private static Method returnToTickLoopMethod = null;

    public static final Identifier EMOTECRAFT_LOGO =
        Identifier.fromNamespaceAndPath("emotecraft", "textures/emotecraft_mod_logo.png");

    // Reflection handles for PlayerAnimationLib
    private static Method getAnimManagerMethod = null;
    private static Method setAnimManagerMethod = null;

    private static volatile List<EmoteEntry> cachedEmotes = null;
    private static final java.util.concurrent.atomic.AtomicBoolean IS_DISK_SCANNING = new java.util.concurrent.atomic.AtomicBoolean(false);

    static {
        boolean loaded = FabricLoader.getInstance().isModLoaded("emotecraft");
        if (loaded) {
            try {
                // 1. Resolve EmoteHolder
                try {
                    emoteHolderClass = Class.forName("io.github.kosmx.emotes.main.EmoteHolder");
                    try {
                        listField = emoteHolderClass.getField("list");
                    } catch (NoSuchFieldException ignored) {}

                    for (Method m : emoteHolderClass.getMethods()) {
                        if (m.getName().equals("getEmoteFromUuid") && m.getParameterCount() == 1) {
                            getEmoteFromUuidMethod = m;
                        } else if (m.getName().equals("getUuid") && m.getParameterCount() == 0) {
                            holderGetUuidMethod = m;
                        } else if (m.getName().equals("getEmote") && m.getParameterCount() == 0) {
                            holderGetEmoteMethod = m;
                        } else if (m.getName().equals("getIconIdentifier") && m.getParameterCount() == 0) {
                            holderGetIconMethod = m;
                        }
                    }
                    for (Field f : emoteHolderClass.getFields()) {
                        if (f.getName().equals("emote")) {
                            holderEmoteField = f;
                        } else if (f.getName().equals("name")) {
                            holderNameField = f;
                        } else if (f.getName().equals("author")) {
                            holderAuthorField = f;
                        }
                    }
                } catch (Throwable t) {
                    SakuraWeaponsMod.LOGGER.warn("Emotecraft EmoteHolder reflection setup note: {}", t.getMessage());
                }

                // 2. Resolve IPlayerEntity methods (playEmote, stopEmote, isPlayingEmote)
                try {
                    Class<?> iPlayerEntityClass = Class.forName("io.github.kosmx.emotes.main.mixinFunctions.IPlayerEntity");
                    for (Method m : iPlayerEntityClass.getMethods()) {
                        if (m.getName().equals("emotecraft$playEmote")) {
                            if (m.getParameterCount() == 4) {
                                playEmote4ArgMethod = m;
                            } else if (m.getParameterCount() == 3) {
                                playEmote3ArgMethod = m;
                            }
                        } else if (m.getName().equals("stopEmote") && m.getParameterCount() == 0) {
                            stopEmoteMethod = m;
                        } else if (m.getName().equals("isPlayingEmote") && m.getParameterCount() == 0) {
                            isPlayingEmoteMethod = m;
                        }
                    }
                } catch (Throwable t) {
                    SakuraWeaponsMod.LOGGER.warn("Emotecraft IPlayerEntity reflection setup note: {}", t.getMessage());
                }

                // 3. Resolve Animation.LoopType (static final fields & methods on interface com.zigythebird.playeranimcore.animation.Animation$LoopType)
                try {
                    loopTypeClass = Class.forName("com.zigythebird.playeranimcore.animation.Animation$LoopType");
                    try {
                        loopTypeLoop = loopTypeClass.getField("LOOP").get(null);
                    } catch (Throwable ignored) {}
                    try {
                        loopTypePlayOnce = loopTypeClass.getField("PLAY_ONCE").get(null);
                    } catch (Throwable ignored) {}
                    try {
                        loopTypeHold = loopTypeClass.getField("HOLD_ON_LAST_FRAME").get(null);
                    } catch (Throwable ignored) {}
                    for (Method m : loopTypeClass.getMethods()) {
                        if (m.getName().equals("returnToTickLoop") && m.getParameterCount() == 1) {
                            returnToTickLoopMethod = m;
                            break;
                        }
                    }
                    SakuraWeaponsMod.LOGGER.info("Resolved Animation$LoopType for Emotecraft: loop={}, playOnce={}, hold={}, returnToTickLoop={}",
                        loopTypeLoop != null, loopTypePlayOnce != null, loopTypeHold != null, returnToTickLoopMethod != null);
                } catch (Throwable t) {
                    SakuraWeaponsMod.LOGGER.warn("Failed to resolve Animation$LoopType: {}", t.getMessage());
                }

                // 4. Resolve PlayerAnimationLib accessors
                try {
                    Class<?> animatedAvatarClass = Class.forName("com.zigythebird.playeranim.accessors.IAnimatedAvatar");
                    for (Method m : animatedAvatarClass.getMethods()) {
                        if (m.getName().equals("playerAnimLib$getAnimManager")) {
                            getAnimManagerMethod = m;
                            break;
                        }
                    }
                    Class<?> animStateClass = Class.forName("com.zigythebird.playeranim.accessors.IAvatarAnimationState");
                    for (Method m : animStateClass.getMethods()) {
                        if (m.getName().equals("playerAnimLib$setAnimManager")) {
                            setAnimManagerMethod = m;
                            break;
                        }
                    }
                } catch (Throwable ignored) {}

                SakuraWeaponsMod.LOGGER.info("Emotecraft mod detected: KosmX Emotecraft 3.4.0 integration initialized.");
            } catch (Throwable t) {
                SakuraWeaponsMod.LOGGER.warn("Emotecraft mod detected but reflection initialization failed: {}", t.getMessage());
            }
        }
        EMOTECRAFT_LOADED = loaded;
    }

    public static boolean isEmotecraftAvailable() {
        return EMOTECRAFT_LOADED;
    }

    /**
     * Checks if the entity is currently playing an Emotecraft animation.
     */
    public static boolean isPlayingEmote(LivingEntity entity) {
        if (!EMOTECRAFT_LOADED || entity == null) {
            return false;
        }
        try {
            if (isPlayingEmoteMethod != null && isPlayingEmoteMethod.getDeclaringClass().isInstance(entity)) {
                return (boolean) isPlayingEmoteMethod.invoke(entity);
            }
            // Fallback dynamic lookup
            for (Method m : entity.getClass().getMethods()) {
                if (m.getName().equals("isPlayingEmote") && m.getParameterCount() == 0 && m.getDeclaringClass().isInstance(entity)) {
                    return (boolean) m.invoke(entity);
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /**
     * Checks if the render state has an active Emotecraft / PlayerAnimationLib animation attached.
     */
    public static boolean hasActiveEmoteAnimation(AvatarRenderState state) {
        if (!EMOTECRAFT_LOADED || state == null) return false;
        try {
            for (Method m : state.getClass().getMethods()) {
                if (m.getName().equals("playerAnimLib$getAnimManager") && m.getParameterCount() == 0) {
                    Object manager = m.invoke(state);
                    if (manager != null) {
                        for (Method mm : manager.getClass().getMethods()) {
                            if ((mm.getName().equals("isActive") || mm.getName().equals("isAnimationActive") || mm.getName().equals("hasActiveAnimation")) && mm.getParameterCount() == 0) {
                                Object active = mm.invoke(manager);
                                if (Boolean.TRUE.equals(active)) return true;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static Object resolveSmartLoopType(Object animObj, String emoteId, byte playMode) {
        // Mode 2: Play Once & Stand (Berakhir dengan Berdiri)
        if (playMode == 2) {
            return loopTypePlayOnce != null ? loopTypePlayOnce : loopTypeLoop;
        }

        // Mode 1: Hold on Last Frame (Tahan di Gerakan Terakhir Selamanya)
        if (playMode == 1) {
            if (returnToTickLoopMethod != null && animObj != null) {
                try {
                    float length = getAnimationLength(animObj);
                    float returnTick = Math.max(0.0f, length - 0.05f);
                    return returnToTickLoopMethod.invoke(null, returnTick);
                } catch (Throwable ignored) {}
            }
            return loopTypeHold != null ? loopTypeHold : loopTypeLoop;
        }

        // Mode 0: Loop (Terus Menerus)
        // If animation has a beginTick defined in data, loop back to beginTick
        if (returnToTickLoopMethod != null && animObj != null) {
            try {
                float beginTick = getAnimationBeginTick(animObj);
                if (beginTick > 0.0f) {
                    return returnToTickLoopMethod.invoke(null, beginTick);
                }
            } catch (Throwable ignored) {}
        }

        return loopTypeLoop != null ? loopTypeLoop : (loopTypePlayOnce != null ? loopTypePlayOnce : null);
    }

    private static float getAnimationLength(Object animObj) {
        if (animObj == null) return 1.0f;
        try {
            for (Method m : animObj.getClass().getMethods()) {
                if (m.getName().equals("length") && m.getParameterCount() == 0) {
                    Object res = m.invoke(animObj);
                    if (res instanceof Number num) return num.floatValue();
                }
            }
        } catch (Throwable ignored) {}
        return 1.0f;
    }

    private static float getAnimationBeginTick(Object animObj) {
        if (animObj == null) return 0.0f;
        try {
            Method dataMethod = null;
            for (Method m : animObj.getClass().getMethods()) {
                if (m.getName().equals("data") && m.getParameterCount() == 0) {
                    dataMethod = m;
                    break;
                }
            }
            if (dataMethod != null) {
                Object dataObj = dataMethod.invoke(animObj);
                if (dataObj != null) {
                    for (Method m : dataObj.getClass().getMethods()) {
                        if (m.getName().equals("getRaw") && m.getParameterCount() == 1) {
                            Object val = m.invoke(dataObj, "beginTick");
                            if (val instanceof Number num) {
                                return num.floatValue();
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return 0.0f;
    }

    /**
     * Plays an emote on a living avatar entity in continuous loop mode.
     */
    public static void playEmoteSafely(LivingEntity entity, String emoteId) {
        playEmoteSafely(entity, emoteId, (byte) 0);
    }

    /**
     * Plays an emote on a living avatar entity with a specific play mode.
     * Mode 0: Loop (continuous)
     * Mode 1: Hold (freeze at last frame forever)
     * Mode 2: Stand (play once and return to standing)
     */
    public static void playEmoteSafely(LivingEntity entity, String emoteId, byte playMode) {
        if (!EMOTECRAFT_LOADED || entity == null || emoteId == null || emoteId.isBlank()) {
            return;
        }

        try {
            Object emoteAnimation = findEmoteAnimation(emoteId);
            if (emoteAnimation == null) {
                return;
            }

            // Stop any existing animation cleanly before starting a new one
            stopEmoteSafely(entity);

            Object resolvedLoopType = resolveSmartLoopType(emoteAnimation, emoteId, playMode);

            // Execute play on entity (NEVER pass null loopType!)
            if (playEmote4ArgMethod != null && resolvedLoopType != null && playEmote4ArgMethod.getDeclaringClass().isInstance(entity)) {
                playEmote4ArgMethod.invoke(entity, emoteAnimation, resolvedLoopType, 0.0f, true);
                return;
            } else if (playEmote3ArgMethod != null && playEmote3ArgMethod.getDeclaringClass().isInstance(entity)) {
                playEmote3ArgMethod.invoke(entity, emoteAnimation, 0.0f, true);
                return;
            }

            // Fallback dynamic lookup on entity class
            for (Method m : entity.getClass().getMethods()) {
                if (m.getName().equals("emotecraft$playEmote") && m.getDeclaringClass().isInstance(entity)) {
                    if (m.getParameterCount() == 4 && resolvedLoopType != null) {
                        m.invoke(entity, emoteAnimation, resolvedLoopType, 0.0f, true);
                        return;
                    } else if (m.getParameterCount() == 3) {
                        m.invoke(entity, emoteAnimation, 0.0f, true);
                        return;
                    }
                }
            }
        } catch (Throwable t) {
            SakuraWeaponsMod.LOGGER.warn("Failed to safely play emote '{}' on entity {}: {}", emoteId, entity.getId(), t.getMessage());
        }
    }

    /**
     * Continuously guarantees that an active emote is playing without freezing into a statue.
     */
    public static void ensureEmotePlaying(LivingEntity entity, String emoteId) {
        ensureEmotePlaying(entity, emoteId, (byte) 0);
    }

    public static void ensureEmotePlaying(LivingEntity entity, String emoteId, byte playMode) {
        if (!EMOTECRAFT_LOADED || entity == null || emoteId == null || emoteId.isBlank()) {
            return;
        }
        if (!isPlayingEmote(entity)) {
            playEmoteSafely(entity, emoteId, playMode);
        }
    }

    /**
     * Safely stops any currently playing emote on an avatar entity.
     */
    public static void stopEmoteSafely(LivingEntity entity) {
        if (!EMOTECRAFT_LOADED || entity == null) {
            return;
        }

        try {
            if (stopEmoteMethod != null && stopEmoteMethod.getDeclaringClass().isInstance(entity)) {
                stopEmoteMethod.invoke(entity);
                return;
            }
            // Fallback dynamic lookup
            for (Method m : entity.getClass().getMethods()) {
                if (m.getName().equals("stopEmote") && m.getParameterCount() == 0 && m.getDeclaringClass().isInstance(entity)) {
                    m.invoke(entity);
                    return;
                }
            }
        } catch (Throwable ignored) {
            // Suppressed to ensure entity stability
        }
    }

    /**
     * Completely resets and clears the animation stack / controller if corrupted.
     */
    public static void resetAnimStateSafely(LivingEntity entity) {
        if (!EMOTECRAFT_LOADED || entity == null) return;
        try {
            Method getEmoteMethod = null;
            for (Method m : entity.getClass().getMethods()) {
                if (m.getName().equals("emotecraft$getEmote") && m.getParameterCount() == 0 && m.getDeclaringClass().isInstance(entity)) {
                    getEmoteMethod = m;
                    break;
                }
            }
            if (getEmoteMethod != null) {
                Object emotePlayer = getEmoteMethod.invoke(entity);
                if (emotePlayer != null) {
                    try {
                        Method forceReset = emotePlayer.getClass().getMethod("forceAnimationReset");
                        forceReset.invoke(emotePlayer);
                    } catch (Throwable ignored) {}
                    try {
                        Method stop = emotePlayer.getClass().getMethod("stop");
                        stop.invoke(emotePlayer);
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Bridges PlayerAnimationLib animation state from entity to AvatarRenderState during extraction.
     */
    public static void extractAnimationState(LivingEntity entity, AvatarRenderState state, float partialTick) {
        if (entity == null || state == null) return;
        try {
            Object animManager = null;
            if (getAnimManagerMethod != null && getAnimManagerMethod.getDeclaringClass().isInstance(entity)) {
                animManager = getAnimManagerMethod.invoke(entity);
            } else {
                for (Method m : entity.getClass().getMethods()) {
                    if (m.getName().equals("playerAnimLib$getAnimManager") && m.getDeclaringClass().isInstance(entity)) {
                        animManager = m.invoke(entity);
                        break;
                    }
                }
            }

            if (animManager != null) {
                try {
                    Method setTickDeltaMethod = animManager.getClass().getMethod("setTickDelta", float.class);
                    setTickDeltaMethod.invoke(animManager, partialTick);
                } catch (Throwable ignored) {}

                if (setAnimManagerMethod != null) {
                    setAnimManagerMethod.invoke(state, animManager);
                } else {
                    for (Method m : state.getClass().getMethods()) {
                        if (m.getName().equals("playerAnimLib$setAnimManager")) {
                            m.invoke(state, animManager);
                            break;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Samples rotations of limbs at an arbitrary animation tick timestamp.
     * Evaluates true Emotecraft / PlayerAnimation keyframe curves if available,
     * and seamlessly falls back to the built-in procedural animation engine.
     */
    public static net.minecraft.core.Rotations[] sampleRotationsAtTick(String emoteId, float animTime) {
        if (emoteId == null || emoteId.isBlank()) {
            return null;
        }

        // 1. Try evaluating dynamic keyframe curves from KosmX Emotecraft / PlayerAnimationLib
        if (EMOTECRAFT_LOADED) {
            try {
                Object animation = findEmoteAnimation(emoteId);
                if (animation != null) {
                    net.minecraft.core.Rotations[] animRots = sampleFromAnimationKeyframesAtTick(animation, animTime);
                    if (animRots != null && hasAnyNonZero(animRots)) {
                        return animRots;
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 2. Procedural animation engine fallback (supports all 14 built-in emotes, sit, sleep, etc.)
        try {
            net.minecraft.core.Rotations[] procedural = net.sakura.weapons.client.render.entity.model.TakashaNpcModel.calculateProceduralRotations(emoteId, animTime);
            if (procedural != null && hasAnyNonZero(procedural)) {
                return procedural;
            }
        } catch (Throwable ignored) {}

        // 3. Life-like idle fallback for unknown/custom emotes so the NPC never freezes into a rigid vanilla statue
        float breathe = (float) Math.sin(animTime * 0.15f) * 1.5f;
        float sway = (float) Math.sin(animTime * 0.1f) * 1.0f;
        return new net.minecraft.core.Rotations[] {
            new net.minecraft.core.Rotations(breathe, 0.0f, 0.0f),
            new net.minecraft.core.Rotations(0.0f, 0.0f, breathe * 0.5f),
            new net.minecraft.core.Rotations(-3.0f + sway, 0.0f, 3.0f),
            new net.minecraft.core.Rotations(-3.0f - sway, 0.0f, -3.0f),
            new net.minecraft.core.Rotations(2.0f, 0.0f, 1.0f),
            new net.minecraft.core.Rotations(2.0f, 0.0f, -1.0f)
        };
    }

    /**
     * Samples the current rotations of limbs for an entity playing an emote or procedural animation.
     * Returns an array of 6 Rotations [head, body, rightArm, leftArm, rightLeg, leftLeg].
     * Guarantees non-zero natural rotations so ReplayMod/Flashback recording and timeline scrubbing
     * never revert dummy NPC entities to a rigid upright default stance.
     */
    public static net.minecraft.core.Rotations[] sampleCurrentRotations(net.sakura.weapons.entity.TakashaNpcEntity entity, String emoteId) {
        if (entity == null) {
            return new net.minecraft.core.Rotations[] {
                new net.minecraft.core.Rotations(0, 0, 0),
                new net.minecraft.core.Rotations(0, 0, 0),
                new net.minecraft.core.Rotations(0, 0, 0),
                new net.minecraft.core.Rotations(0, 0, 0),
                new net.minecraft.core.Rotations(0, 0, 0),
                new net.minecraft.core.Rotations(0, 0, 0)
            };
        }

        if (emoteId != null && !emoteId.isBlank()) {
            // 1. Try sampling from active live EmotePlayer on the entity
            net.minecraft.core.Rotations[] activeRots = sampleFromActiveEmotePlayer(entity);
            if (activeRots != null && hasAnyNonZero(activeRots)) {
                return activeRots;
            }

            return sampleRotationsAtTick(emoteId, (float) entity.tickCount);
        }

        return new net.minecraft.core.Rotations[] {
            entity.getHeadPose(),
            entity.getBodyPose(),
            entity.getRightArmPose(),
            entity.getLeftArmPose(),
            entity.getRightLegPose(),
            entity.getLeftLegPose()
        };
    }

    private static boolean hasAnyNonZero(net.minecraft.core.Rotations[] rots) {
        if (rots == null) return false;
        for (net.minecraft.core.Rotations r : rots) {
            if (r != null && (Math.abs(r.x()) > 0.001f || Math.abs(r.y()) > 0.001f || Math.abs(r.z()) > 0.001f)) {
                return true;
            }
        }
        return false;
    }

    private static net.minecraft.core.Rotations[] sampleFromActiveEmotePlayer(LivingEntity entity) {
        if (!EMOTECRAFT_LOADED || entity == null) return null;
        try {
            Object emotePlayer = null;
            for (Method m : entity.getClass().getMethods()) {
                if (m.getName().equals("emotecraft$getEmote") && m.getParameterCount() == 0) {
                    emotePlayer = m.invoke(entity);
                    break;
                }
            }
            if (emotePlayer != null) {
                Method getBoneMethod = null;
                for (Method m : emotePlayer.getClass().getMethods()) {
                    if (m.getName().equals("getBone") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class) {
                        getBoneMethod = m;
                        break;
                    }
                }
                if (getBoneMethod != null) {
                    net.minecraft.core.Rotations head = getBoneRotations(emotePlayer, getBoneMethod, "head");
                    net.minecraft.core.Rotations body = getBoneRotations(emotePlayer, getBoneMethod, "torso", "body");
                    net.minecraft.core.Rotations rightArm = getBoneRotations(emotePlayer, getBoneMethod, "right_arm", "rightArm");
                    net.minecraft.core.Rotations leftArm = getBoneRotations(emotePlayer, getBoneMethod, "left_arm", "leftArm");
                    net.minecraft.core.Rotations rightLeg = getBoneRotations(emotePlayer, getBoneMethod, "right_leg", "rightLeg");
                    net.minecraft.core.Rotations leftLeg = getBoneRotations(emotePlayer, getBoneMethod, "left_leg", "leftLeg");

                    return new net.minecraft.core.Rotations[] { head, body, rightArm, leftArm, rightLeg, leftLeg };
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static net.minecraft.core.Rotations getBoneRotations(Object controller, Method getBoneMethod, String... names) {
        for (String name : names) {
            try {
                Object bone = getBoneMethod.invoke(controller, name);
                if (bone != null) {
                    Field rotField = null;
                    for (Field f : bone.getClass().getFields()) {
                        if (f.getName().equals("rotation")) {
                            rotField = f;
                            break;
                        }
                    }
                    if (rotField != null) {
                        Object rotObj = rotField.get(bone);
                        if (rotObj instanceof org.joml.Vector3f vec) {
                            return new net.minecraft.core.Rotations(
                                (float) Math.toDegrees(vec.x()),
                                (float) Math.toDegrees(vec.y()),
                                (float) Math.toDegrees(vec.z())
                            );
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        return new net.minecraft.core.Rotations(0, 0, 0);
    }

    private static net.minecraft.core.Rotations[] sampleFromAnimationKeyframesAtTick(Object animObj, float animTime) {
        if (animObj == null) return null;
        try {
            Method getBoneMethod = null;
            for (Method m : animObj.getClass().getMethods()) {
                if (m.getName().equals("getBone") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class) {
                    getBoneMethod = m;
                    break;
                }
            }
            if (getBoneMethod == null) return null;

            float totalLength = getAnimationLength(animObj);

            net.minecraft.core.Rotations head = sampleBoneKeyframeRotationAtTick(animObj, getBoneMethod, animTime, totalLength, "head");
            net.minecraft.core.Rotations body = sampleBoneKeyframeRotationAtTick(animObj, getBoneMethod, animTime, totalLength, "torso", "body");
            net.minecraft.core.Rotations rightArm = sampleBoneKeyframeRotationAtTick(animObj, getBoneMethod, animTime, totalLength, "right_arm", "rightArm");
            net.minecraft.core.Rotations leftArm = sampleBoneKeyframeRotationAtTick(animObj, getBoneMethod, animTime, totalLength, "left_arm", "leftArm");
            net.minecraft.core.Rotations rightLeg = sampleBoneKeyframeRotationAtTick(animObj, getBoneMethod, animTime, totalLength, "right_leg", "rightLeg");
            net.minecraft.core.Rotations leftLeg = sampleBoneKeyframeRotationAtTick(animObj, getBoneMethod, animTime, totalLength, "left_leg", "leftLeg");

            return new net.minecraft.core.Rotations[] { head, body, rightArm, leftArm, rightLeg, leftLeg };
        } catch (Throwable ignored) {}
        return null;
    }

    private static net.minecraft.core.Rotations sampleBoneKeyframeRotationAtTick(Object animObj, Method getBoneMethod, float animTime, float totalLength, String... names) {
        for (String name : names) {
            try {
                Object boneAnim = getBoneMethod.invoke(animObj, name);
                if (boneAnim != null) {
                    Method rotKeyFramesMethod = null;
                    for (Method m : boneAnim.getClass().getMethods()) {
                        if (m.getName().equals("rotationKeyFrames") && m.getParameterCount() == 0) {
                            rotKeyFramesMethod = m;
                            break;
                        }
                    }
                    if (rotKeyFramesMethod != null) {
                        Object keyframeStack = rotKeyFramesMethod.invoke(boneAnim);
                        if (keyframeStack != null) {
                            float x = evaluateKeyframeStackAtTick(keyframeStack, "xKeyframes", animTime, totalLength);
                            float y = evaluateKeyframeStackAtTick(keyframeStack, "yKeyframes", animTime, totalLength);
                            float z = evaluateKeyframeStackAtTick(keyframeStack, "zKeyframes", animTime, totalLength);
                            if (x != 0f || y != 0f || z != 0f) {
                                return new net.minecraft.core.Rotations(
                                    (float) Math.toDegrees(x),
                                    (float) Math.toDegrees(y),
                                    (float) Math.toDegrees(z)
                                );
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        return new net.minecraft.core.Rotations(0, 0, 0);
    }

    private static float evaluateKeyframeStackAtTick(Object keyframeStack, String axisMethodName, float animTime, float totalLength) {
        try {
            Method m = keyframeStack.getClass().getMethod(axisMethodName);
            Object listObj = m.invoke(keyframeStack);
            if (listObj instanceof List<?> list && !list.isEmpty()) {
                if (list.size() == 1) {
                    return extractKeyframeValue(list.get(0), "startValue");
                }

                // Calculate cumulative durations
                float totalDuration = 0f;
                float[] durations = new float[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    float len = extractKeyframeLength(list.get(i));
                    durations[i] = len;
                    totalDuration += len;
                }

                float animLen = totalDuration > 0f ? totalDuration : (totalLength > 0f ? totalLength : 20.0f);
                float sampleTime = (animTime >= 0f) ? (animTime % animLen) : 0f;

                float curStart = 0f;
                for (int i = 0; i < list.size(); i++) {
                    Object kf = list.get(i);
                    float kfLen = durations[i];
                    if (kfLen <= 0f && i < list.size() - 1) {
                        continue;
                    }
                    if (sampleTime >= curStart && (sampleTime < curStart + kfLen || i == list.size() - 1)) {
                        float startVal = extractKeyframeValue(kf, "startValue");
                        float endVal = extractKeyframeValue(kf, "endValue");
                        if (endVal == 0f && i + 1 < list.size()) {
                            endVal = extractKeyframeValue(list.get(i + 1), "startValue");
                        }
                        float progress = (kfLen > 0f) ? Math.min(1.0f, Math.max(0.0f, (sampleTime - curStart) / kfLen)) : 1.0f;
                        // Smooth cosine easing
                        float ease = (float) (0.5 - 0.5 * Math.cos(progress * Math.PI));
                        return startVal + (endVal - startVal) * ease;
                    }
                    curStart += kfLen;
                }
                return extractKeyframeValue(list.get(list.size() - 1), "endValue");
            }
        } catch (Throwable ignored) {}
        return 0f;
    }

    private static float extractKeyframeValue(Object kf, String methodName) {
        if (kf == null) return 0f;
        try {
            for (Method m : kf.getClass().getMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == 0) {
                    Object res = m.invoke(kf);
                    if (res instanceof Number num) {
                        return num.floatValue();
                    }
                    if (res instanceof List<?> list && !list.isEmpty()) {
                        Object first = list.get(0);
                        if (first instanceof Number num) {
                            return num.floatValue();
                        } else if (first != null) {
                            return Float.parseFloat(first.toString().trim());
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return 0f;
    }

    private static float extractKeyframeLength(Object kf) {
        if (kf == null) return 0f;
        try {
            for (Method m : kf.getClass().getMethods()) {
                if ((m.getName().equals("length") || m.getName().equals("getLength")) && m.getParameterCount() == 0) {
                    Object res = m.invoke(kf);
                    if (res instanceof Number num) {
                        return num.floatValue();
                    }
                }
            }
        } catch (Throwable ignored) {}
        return 0f;
    }

    /**
     * Locates the underlying Emotecraft/PlayerAnim Animation object by UUID or name.
     */
    private static Object findEmoteAnimation(String emoteId) {
        try {
            if (emoteHolderClass != null) {
                // 1. Try direct UUID lookup
                try {
                    UUID uuid = UUID.fromString(emoteId);
                    if (getEmoteFromUuidMethod != null) {
                        Object holder = getEmoteFromUuidMethod.invoke(null, uuid);
                        if (holder != null) {
                            return getEmoteFromHolder(holder);
                        }
                    }
                } catch (IllegalArgumentException ignored) {}

                // 2. Iterate EmoteHolder.list
                if (listField != null) {
                    Object listObj = listField.get(null);
                    if (listObj instanceof Iterable<?> iterable) {
                        for (Object holder : iterable) {
                            if (holder == null) continue;
                            if (holderGetUuidMethod != null) {
                                Object u = holderGetUuidMethod.invoke(holder);
                                if (u != null && u.toString().equalsIgnoreCase(emoteId)) {
                                    return getEmoteFromHolder(holder);
                                }
                            }
                            if (holderNameField != null) {
                                Object n = holderNameField.get(holder);
                                String nStr = "";
                                if (n instanceof net.minecraft.network.chat.Component comp) {
                                    nStr = comp.getString();
                                } else if (n != null) {
                                    nStr = n.toString();
                                }
                                if (nStr.equalsIgnoreCase(emoteId)) {
                                    return getEmoteFromHolder(holder);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object getEmoteFromHolder(Object holder) {
        try {
            if (holderEmoteField != null) {
                return holderEmoteField.get(holder);
            }
            if (holderGetEmoteMethod != null) {
                return holderGetEmoteMethod.invoke(holder);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Preheats the emote cache asynchronously during client initialization.
     */
    public static void preheatCache() {
        if (cachedEmotes == null) {
            getDetectedEmotes();
        }
    }

    /**
     * Auto-detects all available emotes. Returns immediately (<0.1ms) using in-memory
     * and built-in emotes, while disk scanning runs asynchronously in the background.
     */
    public static List<EmoteEntry> getDetectedEmotes() {
        List<EmoteEntry> existing = cachedEmotes;
        if (existing != null) {
            return existing;
        }

        Map<String, EmoteEntry> emoteMap = new LinkedHashMap<>();

        // 1. Fast in-memory Emotecraft EmoteHolder.list
        loadInMemoryEmotes(emoteMap);

        // 2. Built-in presets
        addBuiltinEmotes(emoteMap);

        List<EmoteEntry> fastList = new ArrayList<>(emoteMap.values());
        fastList.sort(Comparator.comparing(EmoteEntry::displayName, String.CASE_INSENSITIVE_ORDER));
        cachedEmotes = Collections.unmodifiableList(fastList);

        // 3. Trigger non-blocking async disk scan
        triggerAsyncDiskScan();

        return cachedEmotes;
    }

    private static void loadInMemoryEmotes(Map<String, EmoteEntry> emoteMap) {
        if (!EMOTECRAFT_LOADED || listField == null) return;
        try {
            Object listObj = listField.get(null);
            if (listObj instanceof Iterable<?> iterable) {
                for (Object holder : iterable) {
                    if (holder == null) continue;
                    try {
                        String id = null;
                        if (holderGetUuidMethod != null) {
                            Object u = holderGetUuidMethod.invoke(holder);
                            if (u != null) id = u.toString();
                        }

                        String name = null;
                        if (holderNameField != null) {
                            Object n = holderNameField.get(holder);
                            if (n instanceof net.minecraft.network.chat.Component comp) {
                                name = comp.getString();
                            } else if (n != null) {
                                try {
                                    Method getString = n.getClass().getMethod("getString");
                                    name = (String) getString.invoke(n);
                                } catch (Throwable ignored) {
                                    name = n.toString();
                                }
                            }
                        }

                        String author = "";
                        if (holderAuthorField != null) {
                            Object a = holderAuthorField.get(holder);
                            if (a instanceof net.minecraft.network.chat.Component comp) {
                                author = comp.getString();
                            } else if (a != null) {
                                try {
                                    Method getString = a.getClass().getMethod("getString");
                                    author = (String) getString.invoke(a);
                                } catch (Throwable ignored) {
                                    author = a.toString();
                                }
                            }
                        }

                        Identifier iconId = null;
                        if (holderGetIconMethod != null) {
                            Object ic = holderGetIconMethod.invoke(holder);
                            if (ic instanceof Identifier res) {
                                iconId = res;
                            }
                        }
                        if (iconId == null) {
                            iconId = EMOTECRAFT_LOGO;
                        }

                        if (id != null) {
                            if (name == null || name.isBlank()) name = id;
                            emoteMap.put(id.toLowerCase(), new EmoteEntry(id, name, author, iconId));
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void addBuiltinEmotes(Map<String, EmoteEntry> emoteMap) {
        Object[][] builtins = {
            {"wave", "Wave / Melambai", "Built-in", Identifier.fromNamespaceAndPath("emotecraft", "emotes/waving.png")},
            {"point", "Point / Menunjuk", "Built-in", Identifier.fromNamespaceAndPath("emotecraft", "emotes/point.png")},
            {"cheer", "Cheer / Bersorak", "Built-in", EMOTECRAFT_LOGO},
            {"clap", "Clap / Tepuk Tangan", "Built-in", Identifier.fromNamespaceAndPath("emotecraft", "emotes/clap.png")},
            {"salute", "Salute / Hormat", "Built-in", EMOTECRAFT_LOGO},
            {"bow", "Bow / Membungkuk", "Built-in", EMOTECRAFT_LOGO},
            {"sit", "Sit / Duduk Santai", "Built-in", EMOTECRAFT_LOGO},
            {"sleep", "Sleep / Tidur", "Built-in", EMOTECRAFT_LOGO},
            {"tpose", "T-Pose / Pose T", "Built-in", EMOTECRAFT_LOGO},
            {"dance", "Dance / Menari", "Built-in", Identifier.fromNamespaceAndPath("emotecraft", "emotes/club_penguin_dance.png")},
            {"shrug", "Shrug / Angkat Bahu", "Built-in", EMOTECRAFT_LOGO},
            {"facepalm", "Facepalm / Tepuk Dahi", "Built-in", Identifier.fromNamespaceAndPath("emotecraft", "emotes/palm.png")},
            {"cry", "Cry / Menangis", "Built-in", Identifier.fromNamespaceAndPath("emotecraft", "emotes/crying.png")},
            {"zombie", "Zombie / Siaga Zombie", "Built-in", EMOTECRAFT_LOGO}
        };

        for (Object[] b : builtins) {
            String key = ((String) b[0]).toLowerCase();
            if (!emoteMap.containsKey(key)) {
                emoteMap.put(key, new EmoteEntry((String) b[0], (String) b[1], (String) b[2], (Identifier) b[3]));
            }
        }
    }

    private static void triggerAsyncDiskScan() {
        if (IS_DISK_SCANNING.compareAndSet(false, true)) {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    Map<String, EmoteEntry> diskMap = new LinkedHashMap<>();
                    loadInMemoryEmotes(diskMap);

                    Path emotesDir = Minecraft.getInstance().gameDirectory.toPath().resolve("emotes");
                    if (Files.exists(emotesDir) && Files.isDirectory(emotesDir)) {
                        try (Stream<Path> stream = Files.walk(emotesDir, 2)) {
                            stream.filter(Files::isRegularFile).forEach(path -> {
                                String fileName = path.getFileName().toString();
                                if (fileName.endsWith(".json")) {
                                    try {
                                        String content = Files.readString(path);
                                        JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                                        String name = json.has("name") ? json.get("name").getAsString() : fileName.replace(".json", "");
                                        String author = json.has("author") ? json.get("author").getAsString() : "";
                                        String id = json.has("uuid") ? json.get("uuid").getAsString() : fileName.replace(".json", "");

                                        if (!diskMap.containsKey(id.toLowerCase())) {
                                            diskMap.put(id.toLowerCase(), new EmoteEntry(id, name, author, EMOTECRAFT_LOGO));
                                        }
                                    } catch (Throwable ignored) {}
                                } else if (fileName.endsWith(".emotecraft")) {
                                    String base = fileName.replace(".emotecraft", "");
                                    if (!diskMap.containsKey(base.toLowerCase())) {
                                        diskMap.put(base.toLowerCase(), new EmoteEntry(base, formatDisplayName(base), "Emotecraft", EMOTECRAFT_LOGO));
                                    }
                                }
                            });
                        }
                    }

                    addBuiltinEmotes(diskMap);

                    List<EmoteEntry> fullList = new ArrayList<>(diskMap.values());
                    fullList.sort(Comparator.comparing(EmoteEntry::displayName, String.CASE_INSENSITIVE_ORDER));
                    cachedEmotes = Collections.unmodifiableList(fullList);
                } catch (Throwable t) {
                    SakuraWeaponsMod.LOGGER.debug("Async emote scan note: {}", t.getMessage());
                } finally {
                    IS_DISK_SCANNING.set(false);
                }
            });
        }
    }

    public static synchronized void refreshEmotes() {
        cachedEmotes = null;
        getDetectedEmotes();
    }

    private static String formatDisplayName(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String s = raw.replace('_', ' ').replace('-', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : s.split("\\s+")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1));
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}
