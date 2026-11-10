package net.takasha.rpg.combat;

import net.minecraft.world.entity.LivingEntity;

public class TakashaDamageMetadata {

    private final LivingEntity attacker;
    private final LivingEntity target;
    private double physicalDamage;
    private double fireDamage;
    private double magicDamage;
    private int burnDurationTicks;
    private double burnDamagePerTick;
    private boolean critical;
    private double critMultiplier = 1.5;
    private String sourceSkill = "";

    public TakashaDamageMetadata(LivingEntity attacker, LivingEntity target) {
        this.attacker = attacker;
        this.target = target;
    }

    public LivingEntity getAttacker() { return attacker; }
    public LivingEntity getTarget() { return target; }

    public double getPhysicalDamage() { return physicalDamage; }
    public void setPhysicalDamage(double physicalDamage) { this.physicalDamage = physicalDamage; }

    public double getFireDamage() { return fireDamage; }
    public void setFireDamage(double fireDamage) { this.fireDamage = fireDamage; }

    public double getMagicDamage() { return magicDamage; }
    public void setMagicDamage(double magicDamage) { this.magicDamage = magicDamage; }

    public int getBurnDurationTicks() { return burnDurationTicks; }
    public void setBurnDurationTicks(int burnDurationTicks) { this.burnDurationTicks = burnDurationTicks; }

    public double getBurnDamagePerTick() { return burnDamagePerTick; }
    public void setBurnDamagePerTick(double burnDamagePerTick) { this.burnDamagePerTick = burnDamagePerTick; }

    public boolean isCritical() { return critical; }
    public void setCritical(boolean critical) { this.critical = critical; }

    public double getCritMultiplier() { return critMultiplier; }
    public void setCritMultiplier(double critMultiplier) { this.critMultiplier = critMultiplier; }

    public String getSourceSkill() { return sourceSkill; }
    public void setSourceSkill(String sourceSkill) { this.sourceSkill = sourceSkill; }

    public double getTotalPreMitigationDamage() {
        double total = physicalDamage + fireDamage + magicDamage;
        if (critical) {
            total *= critMultiplier;
        }
        return total;
    }
}
