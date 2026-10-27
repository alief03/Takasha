package net.takasha.rpg.dungeon.ai;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.UUID;

/**
 * AI Goal khusus monster Dungeon Arena untuk memburu pemain di dalam arena secara agresif
 * tanpa batas jarak pandang vanilla (infinite tracking dalam radius arena).
 */
public class DungeonMobHunterGoal extends Goal {

    private final Mob mob;
    private final UUID targetPlayerUuid;
    private int repathDelay = 0;

    public DungeonMobHunterGoal(Mob mob, UUID targetPlayerUuid) {
        this.mob = mob;
        this.targetPlayerUuid = targetPlayerUuid;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.TARGET));
    }

    private ServerPlayer getTargetPlayer() {
        if (mob.level().isClientSide()) return null;
        if (mob.level().getServer() == null) return null;
        return mob.level().getServer().getPlayerList().getPlayer(targetPlayerUuid);
    }

    @Override
    public boolean canUse() {
        ServerPlayer player = getTargetPlayer();
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    @Override
    public boolean canContinueToUse() {
        ServerPlayer player = getTargetPlayer();
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    @Override
    public void start() {
        ServerPlayer player = getTargetPlayer();
        if (player != null) {
            mob.setTarget(player);
            mob.getNavigation().moveTo(player, 1.25);
        }
        repathDelay = 0;
    }

    @Override
    public void tick() {
        ServerPlayer player = getTargetPlayer();
        if (player == null) return;

        mob.getLookControl().setLookAt(player, 30.0f, 30.0f);

        if (mob.getTarget() != player) {
            mob.setTarget(player);
        }

        if (--repathDelay <= 0) {
            repathDelay = 10;
            mob.getNavigation().moveTo(player, 1.25);
        }
    }
}
