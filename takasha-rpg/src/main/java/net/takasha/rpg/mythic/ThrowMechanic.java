package net.takasha.rpg.mythic;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Throws or knocks target entity vertically and horizontally (e.g. volcanic spear launch / slam).
 */
public class ThrowMechanic implements MythicMechanic {

    private final String velocityFormula;
    private final String velocityYFormula;

    public ThrowMechanic(Map<String, String> params) {
        this.velocityFormula = params.getOrDefault("velocity", params.getOrDefault("v", "0"));
        this.velocityYFormula = params.getOrDefault("velocityy", params.getOrDefault("vy", "1.0"));
    }

    @Override
    public void execute(SkillExecutionContext context) {
        LivingEntity target = context.getTarget();
        if (target == null || target == context.getCaster() || target.isDeadOrDying()) return;

        double v = context.parseDouble(velocityFormula, 0.0);
        double vy = context.parseDouble(velocityYFormula, 1.0);

        Vec3 look = context.getCaster() != null ? context.getCaster().getLookAngle() : target.getLookAngle();
        Vec3 horiz = new Vec3(look.x, 0, look.z).normalize();

        // Scale vertical velocity to standard Minecraft motion ticks (divide Spigot scale by 10)
        double motionY = vy * 0.15;
        double motionH = v * 0.15;

        Vec3 newMovement = new Vec3(
                horiz.x * motionH,
                motionY,
                horiz.z * motionH
        );

        target.setDeltaMovement(newMovement);
        target.hurtMarked = true;

        if (target instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
        }
    }
}
