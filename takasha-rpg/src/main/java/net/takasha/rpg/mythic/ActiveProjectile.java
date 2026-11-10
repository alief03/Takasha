package net.takasha.rpg.mythic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.takasha.rpg.modelengine.ActiveModelInstance;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

/**
 * Active in-flight projectile or homing missile.
 */
public class ActiveProjectile {

    private final ServerLevel level;
    private final LivingEntity caster;
    private LivingEntity target;
    private Vec3 position;
    private Vec3 velocity;
    private double speed;
    private final double inertia; // > 0 for homing missiles
    private final double hitRadiusH;
    private final double hitRadiusV;
    private final int maxLifetimeTicks;
    private final String onTickSkill;
    private final String onHitSkill;
    private final ActiveModelInstance modelInstance;
    private final net.takasha.rpg.modelengine.TransientTextDisplay textDisplay;
    private final boolean stopOnBlock;
    private final boolean stopOnEntity;
    private final Set<UUID> hitEntities = new HashSet<>();
    private Vec3 targetLocation;
    private final java.util.Map<String, Object> variables = new java.util.HashMap<>();

    private int ageTicks = 0;
    private boolean removed = false;

    public ActiveProjectile(ServerLevel level, LivingEntity caster, LivingEntity target,
                            Vec3 startPos, Vec3 initialVelocity, double speed, double inertia,
                            double hitRadiusH, double hitRadiusV, int maxLifetimeTicks,
                            String onTickSkill, String onHitSkill, ActiveModelInstance modelInstance,
                            net.takasha.rpg.modelengine.TransientTextDisplay textDisplay,
                            boolean stopOnBlock, boolean stopOnEntity) {
        this.level = level;
        this.caster = caster;
        this.target = target;
        this.position = startPos;
        this.velocity = initialVelocity.normalize().scale(speed / 20.0);
        this.speed = speed;
        this.inertia = inertia;
        this.hitRadiusH = Math.max(0.2, hitRadiusH);
        this.hitRadiusV = Math.max(0.2, hitRadiusV);
        this.maxLifetimeTicks = maxLifetimeTicks > 0 ? maxLifetimeTicks : 100;
        this.onTickSkill = onTickSkill;
        this.onHitSkill = onHitSkill;
        this.modelInstance = modelInstance;
        this.textDisplay = textDisplay;
        this.stopOnBlock = stopOnBlock;
        this.stopOnEntity = stopOnEntity;

        if (modelInstance != null) {
            modelInstance.setStaticPos(position);
        }
        if (textDisplay != null) {
            textDisplay.setPos(position);
        }
    }

    public ActiveProjectile(ServerLevel level, LivingEntity caster, LivingEntity target,
                            Vec3 startPos, Vec3 initialVelocity, double speed, double inertia,
                            double hitRadiusH, double hitRadiusV, int maxLifetimeTicks,
                            String onTickSkill, String onHitSkill, ActiveModelInstance modelInstance,
                            net.takasha.rpg.modelengine.TransientTextDisplay textDisplay) {
        this(level, caster, target, startPos, initialVelocity, speed, inertia, hitRadiusH, hitRadiusV, maxLifetimeTicks, onTickSkill, onHitSkill, modelInstance, textDisplay, true, true);
    }

    public ActiveProjectile(ServerLevel level, LivingEntity caster, LivingEntity target,
                            Vec3 startPos, Vec3 initialVelocity, double speed, double inertia,
                            double hitRadiusH, double hitRadiusV, int maxLifetimeTicks,
                            String onTickSkill, String onHitSkill, ActiveModelInstance modelInstance) {
        this(level, caster, target, startPos, initialVelocity, speed, inertia, hitRadiusH, hitRadiusV, maxLifetimeTicks, onTickSkill, onHitSkill, modelInstance, null, true, true);
    }

    public ActiveProjectile(ServerLevel level, LivingEntity caster, LivingEntity target,
                            Vec3 startPos, Vec3 initialVelocity, double speed, double inertia,
                            double hitRadiusH, double hitRadiusV, int maxLifetimeTicks,
                            String onTickSkill, String onHitSkill, ActiveModelInstance modelInstance,
                            boolean stopOnBlock, boolean stopOnEntity) {
        this(level, caster, target, startPos, initialVelocity, speed, inertia, hitRadiusH, hitRadiusV, maxLifetimeTicks, onTickSkill, onHitSkill, modelInstance, null, stopOnBlock, stopOnEntity);
    }

    public void setTargetLocation(Vec3 targetLocation) {
        this.targetLocation = targetLocation;
    }

    public void setVariables(java.util.Map<String, Object> vars) {
        if (vars != null) {
            this.variables.putAll(vars);
        }
    }

    public void setSpeed(double newSpeed) {
        this.speed = newSpeed;
        if (this.velocity != null && this.velocity.lengthSqr() > 0.0001) {
            this.velocity = this.velocity.normalize().scale(newSpeed / 20.0);
        }
    }

    public double getSpeed() {
        return speed;
    }

    public boolean tick() {
        if (removed) return false;

        ageTicks++;
        if (ageTicks > maxLifetimeTicks) {
            remove();
            return false;
        }

        // Homing missile logic: steer velocity towards target entity OR targetLocation
        if (inertia > 0) {
            Vec3 dest = null;
            if (target != null && target != caster && target.isAlive()) {
                dest = target.position().add(0, target.getBbHeight() / 2.0, 0);
            } else if (targetLocation != null) {
                dest = targetLocation;
            }

            if (dest != null) {
                Vec3 toTarget = dest.subtract(position).normalize();
                double stepSpeed = speed / 20.0;
                Vec3 newVel = velocity.scale(inertia).add(toTarget.scale(stepSpeed)).scale(1.0 / (inertia + 1.0));
                this.velocity = newVel.normalize().scale(stepSpeed);

                // Proximity check directly to destination
                if (target != null && position.distanceTo(dest) <= Math.max(0.85, hitRadiusH + target.getBbWidth() * 0.5)) {
                    if (hitEntities.add(target.getUUID())) {
                        onHit(target, position);
                    }
                    if (stopOnEntity) {
                        remove();
                        return false;
                    }
                } else if (target == null && position.distanceTo(dest) <= Math.max(0.85, hitRadiusH)) {
                    onHit(null, position);
                    if (stopOnBlock) {
                        remove();
                        return false;
                    }
                }
            }
        }

        // Advance position
        Vec3 prevPos = position;
        this.position = position.add(velocity);

        // Check block collision (terrain / ground impact)
        BlockHitResult blockHit = level.clip(new ClipContext(prevPos, position, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (blockHit.getType() != HitResult.Type.MISS) {
            Vec3 hitPos = blockHit.getLocation();
            this.position = hitPos;
            onHit(null, hitPos);
            if (stopOnBlock) {
                remove();
                return false;
            }
        }

        // Update 3D model position and orientation
        if (modelInstance != null && !modelInstance.isRemoved()) {
            modelInstance.setStaticPos(position);
            double vx = velocity.x;
            double vy = velocity.y;
            double vz = velocity.z;
            double horizDist = Math.sqrt(vx * vx + vz * vz);
            if (horizDist > 0.001 || Math.abs(vy) > 0.001) {
                float flightYaw = (float) Math.toDegrees(Math.atan2(-vx, vz));
                float flightPitch = (float) Math.toDegrees(Math.atan2(-vy, horizDist));
                modelInstance.setRotation(flightYaw, flightPitch);
            }
        }
        if (textDisplay != null) {
            textDisplay.setPos(position);
        }

        // Execute onTick skill (Parity: only emit particles explicitly declared in YAML)
        if (onTickSkill != null && !onTickSkill.isBlank()) {
            SkillExecutionContext tickCtx = new SkillExecutionContext(level, caster, target, position);
            tickCtx.setActiveProjectile(this);
            tickCtx.getVariables().putAll(this.variables);
            MythicSkillExecutor.getInstance().executeSkill(onTickSkill, tickCtx);
        }

        // Check entity collision with sweep raycast box
        AABB hitBox = new AABB(
                Math.min(prevPos.x, position.x) - hitRadiusH,
                Math.min(prevPos.y, position.y) - hitRadiusV,
                Math.min(prevPos.z, position.z) - hitRadiusH,
                Math.max(prevPos.x, position.x) + hitRadiusH,
                Math.max(prevPos.y, position.y) + hitRadiusV,
                Math.max(prevPos.z, position.z) + hitRadiusH
        );

        List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                e -> e != caster && e.isAlive() && !e.isSpectator());

        if (!hits.isEmpty()) {
            for (LivingEntity hitTarget : hits) {
                if (hitEntities.add(hitTarget.getUUID())) {
                    onHit(hitTarget, position);
                    if (stopOnEntity) {
                        remove();
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private void onHit(LivingEntity hitEntity, Vec3 hitLocation) {
        if (onHitSkill != null && !onHitSkill.isBlank()) {
            SkillExecutionContext hitCtx = new SkillExecutionContext(level, caster, hitEntity, hitLocation);
            hitCtx.setActiveProjectile(this);
            hitCtx.getVariables().putAll(this.variables);
            MythicSkillExecutor.getInstance().executeSkill(onHitSkill, hitCtx);
        }
    }

    public void remove() {
        if (removed) return;
        removed = true;
        if (modelInstance != null) {
            modelInstance.remove();
        }
        if (textDisplay != null) {
            textDisplay.discard();
        }
    }
}
