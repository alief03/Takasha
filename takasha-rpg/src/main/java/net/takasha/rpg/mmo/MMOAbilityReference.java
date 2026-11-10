package net.takasha.rpg.mmo;

import net.minecraft.nbt.CompoundTag;

public class MMOAbilityReference {
    private final String skillId;
    private final MMOAbilityTrigger trigger;
    private final double cooldown;
    private final double manaCost;

    public MMOAbilityReference(String skillId, MMOAbilityTrigger trigger, double cooldown, double manaCost) {
        this.skillId = skillId;
        this.trigger = trigger;
        this.cooldown = cooldown;
        this.manaCost = manaCost;
    }

    public String getSkillId() {
        return skillId;
    }

    public MMOAbilityTrigger getTrigger() {
        return trigger;
    }

    public double getCooldown() {
        return cooldown;
    }

    public double getManaCost() {
        return manaCost;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("SkillId", skillId);
        tag.putString("Trigger", trigger.name());
        tag.putDouble("Cooldown", cooldown);
        tag.putDouble("ManaCost", manaCost);
        return tag;
    }

    public static MMOAbilityReference fromTag(CompoundTag tag) {
        String skill = tag.getStringOr("SkillId", "");
        String triggerStr = tag.getStringOr("Trigger", "RIGHT_CLICK");
        double cd = tag.getDoubleOr("Cooldown", 0.0);
        double mana = tag.getDoubleOr("ManaCost", 0.0);
        return new MMOAbilityReference(skill, MMOAbilityTrigger.fromString(triggerStr), cd, mana);
    }
}
