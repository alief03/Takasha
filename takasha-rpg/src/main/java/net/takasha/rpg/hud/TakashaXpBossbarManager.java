package net.takasha.rpg.hud;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.takasha.rpg.skills.AuraSkillType;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AuraSkills & AdvancedEnchantments Parity: Real-Time Dynamic XP Progress Bossbar
 * Displays a progressive BossBar at the top of the screen whenever the player earns skill XP.
 * Auto-fades after 4 seconds (80 ticks) if no new XP is gained.
 */
public class TakashaXpBossbarManager {

    private record PlayerBossbarEntry(ServerBossEvent bossEvent, long expiryTick) {}

    private static final Map<UUID, PlayerBossbarEntry> ACTIVE_BARS = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;
        initialized = true;

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = server.overworld().getGameTime();
            ACTIVE_BARS.entrySet().removeIf(entry -> {
                UUID playerId = entry.getKey();
                PlayerBossbarEntry record = entry.getValue();
                if (now >= record.expiryTick()) {
                    ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                    if (player != null) {
                        record.bossEvent().removePlayer(player);
                    }
                    return true;
                }
                return false;
            });
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PlayerBossbarEntry removed = ACTIVE_BARS.remove(handler.player.getUUID());
            if (removed != null) {
                removed.bossEvent().removePlayer(handler.player);
            }
        });
    }

    /**
     * Updates or creates the player's XP Progress BossBar
     */
    public static void showXpProgress(ServerPlayer player, AuraSkillType skill, double addedXp,
                                      int currentLvl, double currentXp, double reqXp, boolean leveledUp) {
        if (player == null || player.level().isClientSide()) return;
        initialize();

        float ratio;
        Component title;
        BossEvent.BossBarColor color;

        if (leveledUp) {
            ratio = 1.0f;
            color = BossEvent.BossBarColor.GREEN;
            title = Component.literal("§a§l✦ LEVEL UP! §6§l" + skill.getDisplayName().toUpperCase(Locale.ROOT)
                    + " §7» §eLvl " + currentLvl + " §a§l✦");
        } else {
            ratio = (float) Math.max(0.0, Math.min(1.0, currentXp / (reqXp > 0 ? reqXp : 1.0)));
            int pct = Math.round(ratio * 100.0f);
            color = BossEvent.BossBarColor.YELLOW;
            title = Component.literal("§6§l" + skill.getDisplayName().toUpperCase(Locale.ROOT)
                    + " §7» §eLvl " + currentLvl
                    + " §7(§a" + (int) currentXp + " §7/ §e" + (int) reqXp + " XP §7- §a" + pct + "%§7)");
        }

        long expiry = player.level().getGameTime() + 80; // 4 seconds (80 ticks)

        ACTIVE_BARS.compute(player.getUUID(), (uuid, existing) -> {
            ServerBossEvent bossEvent;
            if (existing != null) {
                bossEvent = existing.bossEvent();
                bossEvent.setName(title);
                bossEvent.setColor(color);
                bossEvent.setProgress(ratio);
            } else {
                bossEvent = new ServerBossEvent(UUID.randomUUID(), title, color, BossEvent.BossBarOverlay.PROGRESS);
                bossEvent.setProgress(ratio);
                bossEvent.addPlayer(player);
            }
            return new PlayerBossbarEntry(bossEvent, expiry);
        });
    }
}

