package net.takasha.rpg.modelengine;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TakashaModelEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger("takasha-model-engine");
    private static final Map<String, BlockbenchModel> REGISTERED_MODELS = new ConcurrentHashMap<>();
    private static final List<ActiveModelInstance> ACTIVE_INSTANCES = new ArrayList<>();
    private static final List<TransientTextDisplay> ACTIVE_TEXT_DISPLAYS = new ArrayList<>();
    private static final Map<String, Float> BONE_SCALES = new ConcurrentHashMap<>();
    private static final Set<UUID> MANAGED_DISPLAY_UUIDS = ConcurrentHashMap.newKeySet();

    public static void initialize() {
        loadScales();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tick();
        });

        // Sweep and discard orphaned / leftover display entities on chunk or entity load
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Display.ItemDisplay display) {
                if (!isManagedDisplay(display)) {
                    boolean isVfx = display.entityTags().contains("takasha_temp_vfx");
                    if (!isVfx) {
                        try {
                            ItemStack item = display.getSlot(0).get();
                            if (item != null && !item.isEmpty()) {
                                Identifier modelId = item.get(DataComponents.ITEM_MODEL);
                                if (modelId != null) {
                                    String path = modelId.getPath().toLowerCase(Locale.ROOT);
                                    if (path.startsWith("vfx_") || path.contains("awakened_pyromancer") || "takasha_rpg".equals(modelId.getNamespace())) {
                                        isVfx = true;
                                    }
                                }
                            }
                        } catch (Throwable ignored) {}
                    }

                    if (isVfx) {
                        world.getServer().execute(() -> {
                            if (!display.isRemoved() && !isManagedDisplay(display)) {
                                display.discard();
                            }
                        });
                    }
                }
            }
        });

        // Lifecycle cleanup: ensure all display instances are cleared before saving world or stopping server
        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> {
            clearAll();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            clearAll();
        });

        LOGGER.info("TakashaModelEngine initialized with Zero-Persistence Transient VFX management.");
    }

    public static void registerActiveBoneDisplay(Display.ItemDisplay display) {
        if (display != null) {
            MANAGED_DISPLAY_UUIDS.add(display.getUUID());
        }
    }

    public static void unregisterActiveBoneDisplay(Display.ItemDisplay display) {
        if (display != null) {
            MANAGED_DISPLAY_UUIDS.remove(display.getUUID());
        }
    }

    public static boolean isManagedDisplay(Entity entity) {
        return entity != null && MANAGED_DISPLAY_UUIDS.contains(entity.getUUID());
    }

    public static void loadScales() {
        try {
            java.io.InputStream stream = TakashaModelEngine.class.getResourceAsStream("/assets/takasha_rpg/vfx_scales.json");
            if (stream == null) {
                stream = TakashaModelEngine.class.getResourceAsStream("/vfx_scales.json");
            }
            if (stream != null) {
                try (java.io.InputStreamReader reader = new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {
                    com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                    for (Map.Entry<String, com.google.gson.JsonElement> e : obj.entrySet()) {
                        BONE_SCALES.put(e.getKey().toLowerCase(), e.getValue().getAsFloat());
                    }
                }
                LOGGER.info("Loaded {} VFX bone scale mappings.", BONE_SCALES.size());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load vfx_scales.json: {}", e.getMessage());
        }
    }

    public static boolean isNonVisualBone(String boneName) {
        if (boneName == null) return true;
        String lower = boneName.toLowerCase();
        return lower.equals("hitbox") || lower.equals("shadow") || lower.equals("mount")
                || lower.startsWith("b_") || lower.startsWith("ob_") || lower.startsWith("tag_");
    }

    public static float getBoneScale(String modelId, String boneName) {
        if (modelId == null || boneName == null) return 1.0f;
        String sanitizedModel = modelId.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        String sanitizedBone = boneName.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        Float scale = BONE_SCALES.get(sanitizedModel + ":" + sanitizedBone);
        return scale != null ? scale : 1.0f;
    }

    public static void registerModel(String id, BlockbenchModel model) {
        REGISTERED_MODELS.put(id.toLowerCase(), model);
        LOGGER.info("Registered 3D Model: {}", id);
    }

    public static BlockbenchModel getModel(String id) {
        if (id == null) return null;
        return REGISTERED_MODELS.get(id.toLowerCase());
    }

    private static final Map<String, Boolean> ITEM_MODEL_EXISTS_CACHE = new ConcurrentHashMap<>();

    public static boolean doesItemModelExist(String itemKey) {
        return ITEM_MODEL_EXISTS_CACHE.computeIfAbsent(itemKey, k ->
                TakashaModelEngine.class.getResource("/assets/takasha_rpg/items/" + k + ".json") != null
        );
    }

    public static net.minecraft.world.item.ItemStack createBoneItemStack(String modelId, String boneName, String boneUuid) {
        if (isNonVisualBone(boneName) || isNonVisualBone(boneUuid)) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }

        String sanitizedModel = (modelId != null ? modelId : "unknown").toLowerCase().replaceAll("[^a-z0-9_]", "_");
        String sanitizedBone = (boneName != null ? boneName : "bone").toLowerCase().replaceAll("[^a-z0-9_]", "_");
        String itemKey = "vfx_" + sanitizedModel + "_" + sanitizedBone;

        if (!doesItemModelExist(itemKey) && boneUuid != null && !boneUuid.isBlank()) {
            String sanitizedUuid = boneUuid.toLowerCase().replaceAll("[^a-z0-9_]", "_");
            String altKey = "vfx_" + sanitizedModel + "_" + sanitizedUuid;
            if (doesItemModelExist(altKey)) {
                itemKey = altKey;
            }
        }

        if (!doesItemModelExist(itemKey)) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }

        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER);
        try {
            stack.set(net.minecraft.core.component.DataComponents.ITEM_MODEL, net.minecraft.resources.Identifier.fromNamespaceAndPath("takasha_rpg", itemKey));
        } catch (Throwable ignored) {}
        return stack;
    }

    public static net.minecraft.world.item.ItemStack createBoneItemStack(String modelId, String boneName) {
        return createBoneItemStack(modelId, boneName, null);
    }

    // Multi-Model Tracking per Entity (UUID -> (modelId -> ActiveModelInstance))
    private static final Map<UUID, Map<String, ActiveModelInstance>> ENTITY_MODELS = new ConcurrentHashMap<>();

    public static ActiveModelInstance spawnModel(ServerLevel level, String modelId, Entity anchor, Vec3 offset) {
        BlockbenchModel model = getModel(modelId);
        if (model == null) {
            LOGGER.warn("Attempted to spawn unknown model: {}", modelId);
            return null;
        }

        ActiveModelInstance instance = new ActiveModelInstance(model, level, anchor, offset);
        synchronized (ACTIVE_INSTANCES) {
            ACTIVE_INSTANCES.add(instance);
        }
        if (anchor != null) {
            ENTITY_MODELS.computeIfAbsent(anchor.getUUID(), k -> new ConcurrentHashMap<>())
                    .put(modelId.toLowerCase(Locale.ROOT), instance);
        }
        return instance;
    }

    public static ActiveModelInstance getEntityModel(UUID entityId) {
        return getEntityModel(entityId, null);
    }

    public static ActiveModelInstance getEntityModel(UUID entityId, String modelId) {
        if (entityId == null) return null;
        Map<String, ActiveModelInstance> map = ENTITY_MODELS.get(entityId);
        if (map == null || map.isEmpty()) return null;

        if (modelId != null) {
            ActiveModelInstance match = map.get(modelId.toLowerCase(Locale.ROOT));
            if (match != null && !match.isRemoved()) return match;
        }

        for (ActiveModelInstance inst : map.values()) {
            if (inst != null && !inst.isRemoved()) return inst;
        }
        return null;
    }

    public static void setEntityModelState(Entity entity, String stateName, double speed) {
        if (entity == null || stateName == null) return;
        Map<String, ActiveModelInstance> map = ENTITY_MODELS.get(entity.getUUID());
        if (map == null || map.isEmpty()) return;

        for (ActiveModelInstance inst : map.values()) {
            if (inst != null && !inst.isRemoved()) {
                inst.playAnimation(stateName, speed, true);
            }
        }
    }

    public static ActiveModelInstance spawnStaticModel(ServerLevel level, String modelId, Vec3 staticPos) {
        return spawnStaticModel(level, modelId, staticPos, 0.0f, 0.0f);
    }

    public static ActiveModelInstance spawnStaticModel(ServerLevel level, String modelId, Vec3 staticPos, float yaw, float pitch) {
        BlockbenchModel model = getModel(modelId);
        if (model == null) {
            LOGGER.warn("Attempted to spawn unknown static model: {}", modelId);
            return null;
        }

        ActiveModelInstance instance = new ActiveModelInstance(model, level, staticPos, yaw, pitch);
        synchronized (ACTIVE_INSTANCES) {
            ACTIVE_INSTANCES.add(instance);
        }
        return instance;
    }

    public static TransientTextDisplay spawnTextDisplay(ServerLevel level, Vec3 pos, List<String> frames) {
        TransientTextDisplay td = new TransientTextDisplay(level, pos, frames);
        synchronized (ACTIVE_TEXT_DISPLAYS) {
            ACTIVE_TEXT_DISPLAYS.add(td);
        }
        return td;
    }

    public static void tick() {
        synchronized (ACTIVE_INSTANCES) {
            Iterator<ActiveModelInstance> it = ACTIVE_INSTANCES.iterator();
            while (it.hasNext()) {
                ActiveModelInstance inst = it.next();
                if (inst.isRemoved()) {
                    it.remove();
                } else {
                    inst.tick();
                }
            }
        }
        synchronized (ACTIVE_TEXT_DISPLAYS) {
            ACTIVE_TEXT_DISPLAYS.removeIf(TransientTextDisplay::tick);
        }
        for (Map<String, ActiveModelInstance> map : ENTITY_MODELS.values()) {
            map.values().removeIf(ActiveModelInstance::isRemoved);
        }
    }

    public static void clearAll() {
        synchronized (ACTIVE_INSTANCES) {
            for (ActiveModelInstance inst : ACTIVE_INSTANCES) {
                inst.remove();
            }
            ACTIVE_INSTANCES.clear();
        }
        synchronized (ACTIVE_TEXT_DISPLAYS) {
            for (TransientTextDisplay td : ACTIVE_TEXT_DISPLAYS) {
                td.discard();
            }
            ACTIVE_TEXT_DISPLAYS.clear();
        }
        for (Map<String, ActiveModelInstance> map : ENTITY_MODELS.values()) {
            for (ActiveModelInstance inst : map.values()) {
                inst.remove();
            }
            map.clear();
        }
        ENTITY_MODELS.clear();
    }
}
