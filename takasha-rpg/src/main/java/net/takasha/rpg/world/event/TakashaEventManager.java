package net.takasha.rpg.world.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TakashaEventManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Takasha-Events");
    private static final Random RANDOM = new Random();

    // Default intervals: Periodic setiap 30 menit (36000 ticks), Random dice check tiap 12 menit (14400 ticks)
    private static final int PERIODIC_INTERVAL_TICKS = 36000;
    private static final int RANDOM_CHECK_INTERVAL_TICKS = 14400;

    private static RpgEventInstance currentEvent = null;
    private static int ticksUntilNextPeriodic = PERIODIC_INTERVAL_TICKS;
    private static int ticksUntilNextRandomCheck = RANDOM_CHECK_INTERVAL_TICKS;

    private static final RpgEventType[] PERIODIC_CYCLE = new RpgEventType[]{
            RpgEventType.DOUBLE_XP,
            RpgEventType.MYTHIC_MERCHANT,
            RpgEventType.DUNGEON_RUSH,
            RpgEventType.HARVEST_FESTIVAL
    };
    private static int periodicCycleIndex = 0;

    public static void initialize() {
        LOGGER.info("Takasha World Events Engine Initialized! 4 Random & 4 Periodic Events loaded.");
    }

    public static void serverTick(MinecraftServer server) {
        if (server == null) return;

        if (currentEvent != null) {
            currentEvent.tick(server);
            if (currentEvent.getRemainingTicks() <= 0) {
                currentEvent.onEnd(server);
                currentEvent = null;
            }
            return;
        }

        // Ticking countdown event berkala
        ticksUntilNextPeriodic--;
        if (ticksUntilNextPeriodic <= 0) {
            ticksUntilNextPeriodic = PERIODIC_INTERVAL_TICKS;
            RpgEventType type = PERIODIC_CYCLE[periodicCycleIndex % PERIODIC_CYCLE.length];
            periodicCycleIndex++;
            startEvent(type, type.getDefaultDurationTicks(), server);
            return;
        }

        // Ticking countdown pelemparan dadu event acak
        ticksUntilNextRandomCheck--;
        if (ticksUntilNextRandomCheck <= 0) {
            ticksUntilNextRandomCheck = RANDOM_CHECK_INTERVAL_TICKS;
            // 40% peluang memicu event acak
            if (RANDOM.nextDouble() < 0.40) {
                RpgEventType[] randomEvents = new RpgEventType[]{
                        RpgEventType.BLOOD_MOON,
                        RpgEventType.GOBLIN_HUNT,
                        RpgEventType.STARFALL,
                        RpgEventType.RIFT_INCURSION
                };
                RpgEventType selected = randomEvents[RANDOM.nextInt(randomEvents.length)];
                startEvent(selected, selected.getDefaultDurationTicks(), server);
            }
        }
    }

    public static boolean startEvent(RpgEventType type, int durationTicks, MinecraftServer server) {
        if (type == null || server == null) return false;

        if (currentEvent != null) {
            currentEvent.onEnd(server);
        }

        currentEvent = new RpgEventInstance(type, durationTicks);
        currentEvent.onStart(server);
        LOGGER.info("World Event started: {} for {} ticks", type.getId(), durationTicks);
        return true;
    }

    public static boolean stopEvent(MinecraftServer server) {
        if (currentEvent == null || server == null) return false;
        currentEvent.onEnd(server);
        currentEvent = null;
        LOGGER.info("Active World Event manually stopped.");
        return true;
    }

    public static void handleServerStopping(MinecraftServer server) {
        if (currentEvent != null) {
            currentEvent.onEnd(server);
            currentEvent = null;
        }
    }

    public static RpgEventInstance getCurrentEvent() {
        return currentEvent;
    }

    public static boolean isEventActive(RpgEventType type) {
        return currentEvent != null && currentEvent.getType() == type;
    }

    public static double getExpMultiplier() {
        return currentEvent != null ? currentEvent.getExpMultiplier() : 1.0;
    }

    public static double getCoinMultiplier() {
        return currentEvent != null ? currentEvent.getCoinMultiplier() : 1.0;
    }

    public static double getLootMultiplier() {
        return currentEvent != null ? currentEvent.getLootMultiplier() : 1.0;
    }

    public static double getHarvestMultiplier() {
        return currentEvent != null ? currentEvent.getHarvestMultiplier() : 1.0;
    }

    public static int getTicksUntilNextPeriodic() {
        return ticksUntilNextPeriodic;
    }

    public static RpgEventType getNextPeriodicType() {
        return PERIODIC_CYCLE[periodicCycleIndex % PERIODIC_CYCLE.length];
    }
}
