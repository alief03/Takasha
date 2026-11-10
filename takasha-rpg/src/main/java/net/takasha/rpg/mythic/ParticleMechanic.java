package net.takasha.rpg.mythic;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Spawns particles (flame, lava, smoke, dust transitions) based on MythicMobs particle syntax.
 */
public class ParticleMechanic implements MythicMechanic {

    private final String particleType;
    private final String amountFormula;
    private final String hSpreadFormula;
    private final String vSpreadFormula;
    private final String yOffsetFormula;
    private final String speedFormula;
    private final String color1Hex;
    private final String color2Hex;
    private final String sizeFormula;

    public ParticleMechanic(Map<String, String> params) {
        this.particleType = params.getOrDefault("p", params.getOrDefault("particle", "flame")).toLowerCase();
        this.amountFormula = params.getOrDefault("amount", "1");
        this.hSpreadFormula = params.getOrDefault("hs", params.getOrDefault("hspread", "0.2"));
        this.vSpreadFormula = params.getOrDefault("vs", params.getOrDefault("vspread", "0.2"));
        this.yOffsetFormula = params.getOrDefault("y", "0");
        this.speedFormula = params.getOrDefault("speed", "0.01");
        this.color1Hex = params.getOrDefault("color1", "#ff9900");
        this.color2Hex = params.getOrDefault("color2", "#ff0000");
        this.sizeFormula = params.getOrDefault("size", "1.0");
    }

    @Override
    public void execute(SkillExecutionContext context) {
        ServerLevel level = context.getLevel();
        Vec3 pos = context.getTargetLocation();
        if (pos == null) pos = context.getOrigin();

        double yOffset = context.parseDouble(yOffsetFormula, 0.0);
        int amount = context.parseInt(amountFormula, 1);
        double hs = context.parseDouble(hSpreadFormula, 0.2);
        double vs = context.parseDouble(vSpreadFormula, 0.2);
        double speed = context.parseDouble(speedFormula, 0.01);
        float size = context.parseFloat(sizeFormula, 1.0f);

        ParticleOptions particle = resolveParticle(size);

        level.sendParticles(
                particle,
                pos.x, pos.y + yOffset, pos.z,
                Math.max(1, amount),
                hs, vs, hs,
                speed
        );
    }

    private ParticleOptions resolveParticle(float size) {
        if (particleType.contains("dust_color_transition") || particleType.contains("dust")) {
            int fromColor = parseHexColor(color1Hex, 0xFFCD17);
            int toColor = parseHexColor(color2Hex, 0xFA2707);
            return new DustColorTransitionOptions(fromColor, toColor, size);
        }

        return switch (particleType) {
            case "lava" -> ParticleTypes.LAVA;
            case "campfire_cosy_smoke", "campfire_smoke" -> ParticleTypes.CAMPFIRE_COSY_SMOKE;
            case "smoke" -> ParticleTypes.SMOKE;
            case "crit" -> ParticleTypes.CRIT;
            case "soul_fire_flame", "soul_flame" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "explosion" -> ParticleTypes.EXPLOSION;
            default -> ParticleTypes.FLAME;
        };
    }

    private int parseHexColor(String hex, int fallback) {
        if (hex == null || hex.isBlank()) return fallback;
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            return Integer.parseInt(clean, 16);
        } catch (Exception e) {
            return fallback;
        }
    }
}
