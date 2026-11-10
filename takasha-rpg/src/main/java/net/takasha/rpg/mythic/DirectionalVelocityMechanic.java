package net.takasha.rpg.mythic;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Applies directional push based on specified yaw angle or entity orientation.
 */
public class DirectionalVelocityMechanic implements MythicMechanic {

    private final String velocityFormula;
    private final String yawFormula;

    public DirectionalVelocityMechanic(Map<String, String> params) {
        this.velocityFormula = params.getOrDefault("v", params.getOrDefault("velocity", "0.2"));
        this.yawFormula = params.getOrDefault("yaw", "");
    }

    @Override
    public void execute(SkillExecutionContext context) {
        LivingEntity ent = context.getTarget() != null ? context.getTarget() : context.getCaster();
        if (ent == null || ent.isDeadOrDying()) return;

        double v = context.parseDouble(velocityFormula, 0.2);
        float yaw;
        if (!yawFormula.isBlank()) {
            yaw = (float) context.parseDouble(yawFormula, ent.getYRot());
        } else {
            yaw = ent.getYRot();
        }

        double rad = Math.toRadians(yaw);
        double mx = -Math.sin(rad) * v;
        double mz = Math.cos(rad) * v;

        ent.setDeltaMovement(ent.getDeltaMovement().add(mx, 0, mz));
        ent.hurtMarked = true;

        if (ent instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
        }
    }
}
