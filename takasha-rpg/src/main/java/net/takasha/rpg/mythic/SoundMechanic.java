package net.takasha.rpg.mythic;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Plays sound effects at target location or caster location.
 */
public class SoundMechanic implements MythicMechanic {

    private final String soundId;
    private final String volumeFormula;
    private final String pitchFormula;

    public SoundMechanic(Map<String, String> params) {
        this.soundId = params.getOrDefault("s", params.getOrDefault("sound", "entity.blaze.shoot"));
        this.volumeFormula = params.getOrDefault("volume", params.getOrDefault("v", "1.0"));
        this.pitchFormula = params.getOrDefault("pitch", params.getOrDefault("p", "1.0"));
    }

    @Override
    public void execute(SkillExecutionContext context) {
        Vec3 pos = context.getTargetLocation();
        if (pos == null) pos = context.getOrigin();

        float volume = context.parseFloat(volumeFormula, 1.0f);
        float pitch = context.parseFloat(pitchFormula, 1.0f);

        try {
            Identifier id = Identifier.parse(soundId);
            SoundEvent event = SoundEvent.createVariableRangeEvent(id);
            context.getLevel().playSound(
                    null,
                    pos.x, pos.y, pos.z,
                    event,
                    SoundSource.PLAYERS,
                    volume,
                    pitch
            );
        } catch (Exception e) {
            // Ignored if sound format is invalid
        }
    }
}
