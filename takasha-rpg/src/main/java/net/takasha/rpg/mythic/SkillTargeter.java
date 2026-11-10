package net.takasha.rpg.mythic;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Resolves target entities and positions based on MythicMobs targeter syntax.
 */
public interface SkillTargeter {

    record TargetResult(LivingEntity entity, Vec3 position, float yaw, float pitch, boolean hasRotation) {
        public static TargetResult of(LivingEntity entity) {
            return new TargetResult(entity, entity != null ? entity.position() : Vec3.ZERO,
                    entity != null ? entity.getYRot() : 0.0f,
                    entity != null ? entity.getXRot() : 0.0f,
                    entity != null);
        }

        public static TargetResult of(Vec3 pos) {
            return new TargetResult(null, pos, 0.0f, 0.0f, false);
        }

        public static TargetResult of(Vec3 pos, float yaw, float pitch) {
            return new TargetResult(null, pos, yaw, pitch, true);
        }
    }

    List<TargetResult> resolve(SkillExecutionContext context);

    SkillTargeter INHERITED = context -> {
        if (context.getTarget() != null) {
            return Collections.singletonList(TargetResult.of(context.getTarget()));
        } else if (context.getTargetLocation() != null) {
            return Collections.singletonList(TargetResult.of(context.getTargetLocation(), context.getTargetYaw(), context.getTargetPitch()));
        } else if (context.getCaster() != null) {
            return Collections.singletonList(TargetResult.of(context.getCaster()));
        }
        return Collections.emptyList();
    };

    static SkillTargeter parse(String targeterStr) {
        if (targeterStr == null || targeterStr.isBlank()) {
            return INHERITED;
        }

        String str = targeterStr.trim();
        String name;
        Map<String, String> params = new HashMap<>();

        if (str.startsWith("@")) {
            str = str.substring(1);
        }

        int braceIndex = str.indexOf('{');
        if (braceIndex != -1 && str.endsWith("}")) {
            name = str.substring(0, braceIndex).trim().toLowerCase();
            String inner = str.substring(braceIndex + 1, str.length() - 1);
            for (String part : inner.split(";")) {
                int eq = part.indexOf('=');
                if (eq != -1) {
                    params.put(part.substring(0, eq).trim().toLowerCase(), part.substring(eq + 1).trim());
                }
            }
        } else {
            name = str.trim().toLowerCase();
        }

        return switch (name) {
            case "self" -> context -> {
                LivingEntity c = context.getCaster();
                return c != null ? Collections.singletonList(TargetResult.of(c)) : Collections.emptyList();
            };
            case "origin" -> context -> {
                double yOffset = 0.0;
                if (params.containsKey("y")) {
                    yOffset = context.parseDouble(params.get("y"), 0.0);
                }
                Vec3 base = context.getOrigin();
                float yaw = context.getTargetYaw();
                float pitch = context.getTargetPitch();
                return Collections.singletonList(TargetResult.of(base.add(0, yOffset, 0), yaw, pitch));
            };
            case "targetedtarget", "target" -> context -> {
                LivingEntity t = context.getTarget();
                return t != null ? Collections.singletonList(TargetResult.of(t)) : Collections.emptyList();
            };
            case "entitiesincone" -> context -> {
                LivingEntity caster = context.getCaster();
                if (caster == null) return Collections.emptyList();
                double angleDeg = context.parseDouble(params.getOrDefault("a", "45"), 45.0);
                double range = context.parseDouble(params.getOrDefault("r", "10"), 10.0);
                int limit = context.parseInt(params.getOrDefault("limit", "10"), 10);
                String ignore = params.getOrDefault("ignore", "");
                boolean ignorePlayers = ignore.equalsIgnoreCase("players");

                Vec3 eyePos = caster.getEyePosition();
                Vec3 lookVec = caster.getLookAngle().normalize();
                double halfAngleCos = Math.cos(Math.toRadians(angleDeg / 2.0));

                AABB searchBox = new AABB(eyePos.x - range, eyePos.y - range, eyePos.z - range,
                        eyePos.x + range, eyePos.y + range, eyePos.z + range);

                List<LivingEntity> found = context.getLevel().getEntitiesOfClass(LivingEntity.class, searchBox,
                        e -> e != caster && e.isAlive() && !e.isSpectator() && (!ignorePlayers || !(e instanceof Player)));

                List<TargetResult> results = new ArrayList<>();
                found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(caster)));

                for (LivingEntity e : found) {
                    Vec3 toTarget = e.position().add(0, e.getBbHeight() / 2.0, 0).subtract(eyePos);
                    double dist = toTarget.length();
                    if (dist <= range) {
                        double dot = toTarget.normalize().dot(lookVec);
                        if (dot >= halfAngleCos) {
                            results.add(TargetResult.of(e));
                            if (results.size() >= limit) break;
                        }
                    }
                }
                if (results.isEmpty()) {
                    // Fallback when no living entities are in cone: target a forward point in look direction
                    Vec3 forwardPos = eyePos.add(lookVec.scale(range > 0 ? range : 15.0));
                    results.add(TargetResult.of(forwardPos));
                }
                return results;
            };
            case "entitiesnearorigin" -> context -> {
                double range = context.parseDouble(params.getOrDefault("r", "5"), 5.0);
                int limit = context.parseInt(params.getOrDefault("limit", "10"), 10);
                String ignore = params.getOrDefault("ignore", "");
                boolean ignorePlayers = ignore.equalsIgnoreCase("players");
                Vec3 origin = context.getOrigin();

                AABB box = new AABB(origin.x - range, origin.y - range, origin.z - range,
                        origin.x + range, origin.y + range, origin.z + range);

                List<LivingEntity> list = context.getLevel().getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != context.getCaster() && e.isAlive() && !e.isSpectator()
                                && (!ignorePlayers || !(e instanceof Player))
                                && e.position().distanceTo(origin) <= range);

                list.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(origin)));
                List<TargetResult> results = new ArrayList<>();
                for (LivingEntity e : list) {
                    results.add(TargetResult.of(e));
                    if (results.size() >= limit) break;
                }
                // Direct hit fallback: if origin radius check found nothing but projectile directly struck an entity
                if (results.isEmpty() && context.getTarget() != null && context.getTarget().isAlive()) {
                    results.add(TargetResult.of(context.getTarget()));
                }
                return results;
            };
            case "eir", "entitiesinradius" -> context -> {
                double range = context.parseDouble(params.getOrDefault("r", params.getOrDefault("radius", "5")), 5.0);
                int limit = context.parseInt(params.getOrDefault("limit", "10"), 10);
                String ignore = params.getOrDefault("ignore", "");
                boolean ignorePlayers = ignore.equalsIgnoreCase("players");
                Vec3 center = context.getOrigin() != null ? context.getOrigin() : (context.getCaster() != null ? context.getCaster().position() : Vec3.ZERO);

                AABB box = new AABB(center.x - range, center.y - range, center.z - range,
                        center.x + range, center.y + range, center.z + range);

                List<LivingEntity> list = context.getLevel().getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != context.getCaster() && e.isAlive() && !e.isSpectator()
                                && (!ignorePlayers || !(e instanceof Player))
                                && e.position().distanceTo(center) <= range);

                list.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(center)));
                List<TargetResult> results = new ArrayList<>();
                for (LivingEntity e : list) {
                    results.add(TargetResult.of(e));
                    if (results.size() >= limit) break;
                }
                // Direct hit fallback: if origin radius check found nothing but projectile directly struck an entity
                if (results.isEmpty() && context.getTarget() != null && context.getTarget().isAlive()) {
                    results.add(TargetResult.of(context.getTarget()));
                }
                return results;
            };
            case "selfeyelocation" -> context -> {
                LivingEntity caster = context.getCaster();
                if (caster == null) return Collections.emptyList();
                double fo = context.parseDouble(params.getOrDefault("fo", "0"), 0.0);
                double yo = context.parseDouble(params.getOrDefault("yo", "0"), 0.0);

                Vec3 eyePos = caster.getEyePosition().add(0, yo, 0);
                Vec3 forward = caster.getLookAngle().scale(fo);
                return Collections.singletonList(TargetResult.of(eyePos.add(forward), caster.getYRot(), caster.getXRot()));
            };
            case "forward" -> context -> {
                LivingEntity caster = context.getCaster();
                if (caster == null) return Collections.emptyList();
                double f = context.parseDouble(params.getOrDefault("f", "5"), 5.0);
                double y = context.parseDouble(params.getOrDefault("y", "0"), 0.0);
                boolean lockPitch = Boolean.parseBoolean(params.getOrDefault("lockpitch", "false"));
                boolean uel = Boolean.parseBoolean(params.getOrDefault("uel", "false"));
                double sideOffset = context.parseDouble(params.getOrDefault("sideoffset", params.getOrDefault("sso", "0")), 0.0);

                Vec3 basePos = uel ? caster.getEyePosition() : caster.position();
                Vec3 look = lockPitch ? new Vec3(caster.getLookAngle().x, 0, caster.getLookAngle().z).normalize() : caster.getLookAngle();
                Vec3 side = new Vec3(-look.z, 0, look.x).normalize();

                Vec3 targetPos = basePos.add(look.scale(f)).add(side.scale(sideOffset)).add(0, y, 0);
                float yaw = caster.getYRot();
                float pitch = lockPitch ? 0.0f : caster.getXRot();
                return Collections.singletonList(TargetResult.of(targetPos, yaw, pitch));
            };
            default -> context -> {
                LivingEntity c = context.getCaster();
                return c != null ? Collections.singletonList(TargetResult.of(c)) : Collections.emptyList();
            };
        };
    }
}
