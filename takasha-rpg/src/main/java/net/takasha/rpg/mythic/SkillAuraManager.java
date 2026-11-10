package net.takasha.rpg.mythic;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Mythic/MMO active auras, stacks, and durations on entities.
 */
public class SkillAuraManager {

    private static final SkillAuraManager INSTANCE = new SkillAuraManager();

    public static SkillAuraManager getInstance() {
        return INSTANCE;
    }

    public static class AuraData {
        private final String name;
        private int stacks;
        private final int maxStacks;
        private int ticksRemaining;

        public AuraData(String name, int initialStacks, int maxStacks, int durationTicks) {
            this.name = name;
            this.stacks = Math.min(initialStacks, maxStacks);
            this.maxStacks = maxStacks;
            this.ticksRemaining = durationTicks;
        }

        public String getName() {
            return name;
        }

        public int getStacks() {
            return stacks;
        }

        public void addStack(int amount) {
            this.stacks = Math.min(this.stacks + amount, maxStacks);
        }

        public void refresh(int durationTicks) {
            this.ticksRemaining = Math.max(this.ticksRemaining, durationTicks);
        }

        public boolean tick() {
            return --ticksRemaining <= 0;
        }
    }

    private final Map<UUID, Map<String, AuraData>> activeAuras = new ConcurrentHashMap<>();

    private SkillAuraManager() {}

    public void addAura(LivingEntity entity, String auraName, int durationTicks, int maxStacks) {
        if (entity == null || auraName == null || auraName.isBlank()) return;
        String key = auraName.toUpperCase();
        Map<String, AuraData> entityAuras = activeAuras.computeIfAbsent(entity.getUUID(), u -> new ConcurrentHashMap<>());
        
        AuraData existing = entityAuras.get(key);
        if (existing != null) {
            existing.addStack(1);
            existing.refresh(durationTicks);
        } else {
            entityAuras.put(key, new AuraData(key, 1, Math.max(1, maxStacks), durationTicks));
        }
    }

    public void removeAura(LivingEntity entity, String auraName) {
        if (entity == null || auraName == null) return;
        Map<String, AuraData> entityAuras = activeAuras.get(entity.getUUID());
        if (entityAuras != null) {
            entityAuras.remove(auraName.toUpperCase());
            if (entityAuras.isEmpty()) {
                activeAuras.remove(entity.getUUID());
            }
        }
    }

    public int getStacks(LivingEntity entity, String auraName) {
        if (entity == null || auraName == null) return 0;
        Map<String, AuraData> entityAuras = activeAuras.get(entity.getUUID());
        if (entityAuras == null) return 0;
        AuraData data = entityAuras.get(auraName.toUpperCase());
        return data != null ? data.getStacks() : 0;
    }

    public boolean hasAura(LivingEntity entity, String auraName) {
        return getStacks(entity, auraName) > 0;
    }

    public void tick() {
        activeAuras.entrySet().removeIf(entry -> {
            Map<String, AuraData> auras = entry.getValue();
            auras.entrySet().removeIf(auraEntry -> auraEntry.getValue().tick());
            return auras.isEmpty();
        });
    }

    public void clear() {
        activeAuras.clear();
    }
}
