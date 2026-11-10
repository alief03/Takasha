package net.takasha.rpg.mythic;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Pushes or lunges entity in look direction or backward recoil.
 */
public class LungeMechanic implements MythicMechanic {

    private final String velocityFormula;
    private final String velocityYFormula;

    public LungeMechanic(Map<String, String> params) {
        this.velocityFormula = params.getOrDefault("velocity", params.getOrDefault("v", "0.5"));
        this.velocityYFormula = params.getOrDefault("velocityy", params.getOrDefault("vy", "0.0"));
    }

    @Override
    public void execute(SkillExecutionContext context) {
        LivingEntity ent = context.getTarget() != null ? context.getTarget() : context.getCaster();
        if (ent == null) return;

        double v = context.parseDouble(velocityFormula, 0.5);
        double vy = context.parseDouble(velocityYFormula, 0.0);

        if (Math.abs(v) <= 0.05 && Math.abs(vy) <= 0.05) {
            // Midair suspension hover: counteract vanilla gravity and reset fall distance
            ent.setDeltaMovement(new Vec3(ent.getDeltaMovement().x * 0.2, 0.005, ent.getDeltaMovement().z * 0.2));
            ent.resetFallDistance();
        } else {
            Vec3 look = ent.getLookAngle();
            Vec3 horiz = new Vec3(look.x, 0, look.z).normalize();
            Vec3 delta = horiz.scale(v).add(0, vy, 0);
            ent.setDeltaMovement(ent.getDeltaMovement().add(delta));
        }
        ent.hurtMarked = true;
        if (ent instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
        }
    }
}
