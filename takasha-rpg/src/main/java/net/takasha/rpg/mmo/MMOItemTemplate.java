package net.takasha.rpg.mmo;

import java.util.*;

public class MMOItemTemplate {

    public static class AbilityConfig {
        private final String abilityKey;
        private final String type;
        private final MMOAbilityTrigger mode;
        private final double cooldown;
        private final double skillCooldown;
        private final double timer;
        private final double damage;
        private final double burnDuration;
        private final double burnDamage;
        private final double manaCost;
        private final Map<String, Object> extraParams = new HashMap<>();

        public AbilityConfig(String abilityKey, String type, MMOAbilityTrigger mode,
                             double cooldown, double skillCooldown, double timer,
                             double damage, double burnDuration, double burnDamage, double manaCost) {
            this.abilityKey = abilityKey;
            this.type = type;
            this.mode = mode;
            this.cooldown = cooldown;
            this.skillCooldown = skillCooldown;
            this.timer = timer;
            this.damage = damage;
            this.burnDuration = burnDuration;
            this.burnDamage = burnDamage;
            this.manaCost = manaCost;
        }

        public String getAbilityKey() { return abilityKey; }
        public String getType() { return type; }
        public MMOAbilityTrigger getMode() { return mode; }
        public double getCooldown() { return cooldown > 0 ? cooldown : skillCooldown; }
        public double getSkillCooldown() { return skillCooldown; }
        public double getTimer() { return timer; }
        public double getDamage() { return damage; }
        public double getBurnDuration() { return burnDuration; }
        public double getBurnDamage() { return burnDamage; }
        public double getManaCost() { return manaCost; }
        public Map<String, Object> getExtraParams() { return extraParams; }
    }

    private final String id;
    private final String itemType;
    private String material = "STONE_SWORD";
    private Integer customModelData = null;
    private String name = null;
    private final List<String> lore = new ArrayList<>();
    private String displayedType = "";
    private Double attackDamage = null;
    private Double attackSpeed = null;
    private Boolean unbreakable = null;
    private Boolean disableAttackPassive = null;
    private final Map<MMOStatType, Double> stats = new EnumMap<>(MMOStatType.class);
    private final List<AbilityConfig> abilities = new ArrayList<>();
    private MMOItemTier tier = null;

    // Tool Attributes & Special Abilities (MMOItems 1:1)
    private Boolean autosmelt = null;
    private Boolean bouncingCrack = null;
    private Integer pickaxePower = null;
    private Double miningEfficiency = null;
    private Integer maxDurability = null;
    private Boolean willBreak = null;

    // Enchantments & Modifiers
    private final Map<String, Integer> vanillaEnchants = new LinkedHashMap<>();
    private final Map<String, Integer> advancedEnchants = new LinkedHashMap<>();
    private final List<String> gemSockets = new ArrayList<>();
    private final Map<String, Double> auraSkillRequirements = new LinkedHashMap<>();
    private final Map<String, Double> auraSkillStats = new LinkedHashMap<>();
    private int revisionId = 1;

    // Gem Stone Specific Attributes (MMOItems 1:1)
    private String gemColor = null;
    private String gemUpgradeScaling = "SUBSEQUENT";
    private final List<String> itemTypeRestrictions = new ArrayList<>();
    private Double successRate = null;

    // Advanced & Cosmetic Attributes (MMOItems 1:1)
    private String itemSet = null;
    private String loreFormat = null;
    private String shieldPattern = null;
    private String dyeColor = null;
    private String trimMaterial = null;
    private String trimPattern = null;
    private String requiredClass = null;
    private String permission = null;
    private Boolean twoHanded = null;
    private Boolean handworn = null;
    private Boolean hideEnchants = null;
    private Boolean canIdentify = null;
    private Boolean canDeconstruct = null;
    private Boolean canDeskin = null;
    private Double knockback = null;
    private Double recoil = null;
    private final List<String> commands = new ArrayList<>();
    private final List<String> permEffects = new ArrayList<>();
    private final List<String> potionEffects = new ArrayList<>();
    private final List<String> customSounds = new ArrayList<>();
    private String itemParticles = null;
    private String arrowParticles = null;

    public MMOItemTemplate(String id, String itemType) {
        this.id = id;
        this.itemType = itemType != null ? itemType.toUpperCase(Locale.ROOT) : "TOOL";
    }

    public String getId() { return id; }
    public String getItemType() { return itemType; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public int getCustomModelData() { return customModelData != null ? customModelData : 0; }
    public Integer getRawCustomModelData() { return customModelData; }
    public boolean hasCustomModelData() { return customModelData != null && customModelData > 0; }
    public void setCustomModelData(Integer customModelData) { this.customModelData = customModelData; }

    public String getName() { return name != null ? name : ""; }
    public String getRawName() { return name; }
    public boolean hasName() { return name != null && !name.isEmpty(); }
    public void setName(String name) { this.name = name; }

    public List<String> getLore() { return lore; }
    public String getDisplayedType() { return displayedType; }
    public void setDisplayedType(String displayedType) { this.displayedType = displayedType; }

    public double getAttackDamage() { return attackDamage != null ? attackDamage : 0.0; }
    public Double getRawAttackDamage() { return attackDamage; }
    public boolean hasAttackDamage() { return attackDamage != null; }
    public void setAttackDamage(Double attackDamage) { this.attackDamage = attackDamage; }

    public double getAttackSpeed() { return attackSpeed != null ? attackSpeed : 4.0; }
    public Double getRawAttackSpeed() { return attackSpeed; }
    public boolean hasAttackSpeed() { return attackSpeed != null; }
    public void setAttackSpeed(Double attackSpeed) { this.attackSpeed = attackSpeed; }

    public boolean isUnbreakable() { return Boolean.TRUE.equals(unbreakable); }
    public Boolean getRawUnbreakable() { return unbreakable; }
    public boolean hasUnbreakable() { return unbreakable != null; }
    public void setUnbreakable(Boolean unbreakable) { this.unbreakable = unbreakable; }

    public boolean isDisableAttackPassive() { return Boolean.TRUE.equals(disableAttackPassive); }
    public Boolean getRawDisableAttackPassive() { return disableAttackPassive; }
    public boolean hasDisableAttackPassive() { return disableAttackPassive != null; }
    public void setDisableAttackPassive(Boolean disableAttackPassive) { this.disableAttackPassive = disableAttackPassive; }

    public Map<MMOStatType, Double> getStats() { return stats; }
    public List<AbilityConfig> getAbilities() { return abilities; }

    public MMOItemTier getTier() { return tier; }
    public boolean hasTier() { return tier != null; }
    public void setTier(MMOItemTier tier) { this.tier = tier; }

    public boolean isAutosmelt() { return Boolean.TRUE.equals(autosmelt); }
    public Boolean getRawAutosmelt() { return autosmelt; }
    public boolean hasAutosmelt() { return autosmelt != null; }
    public void setAutosmelt(Boolean autosmelt) { this.autosmelt = autosmelt; }

    public boolean isBouncingCrack() { return Boolean.TRUE.equals(bouncingCrack); }
    public Boolean getRawBouncingCrack() { return bouncingCrack; }
    public boolean hasBouncingCrack() { return bouncingCrack != null; }
    public void setBouncingCrack(Boolean bouncingCrack) { this.bouncingCrack = bouncingCrack; }

    public int getPickaxePower() { return pickaxePower != null ? pickaxePower : 0; }
    public Integer getRawPickaxePower() { return pickaxePower; }
    public boolean hasPickaxePower() { return pickaxePower != null; }
    public void setPickaxePower(Integer pickaxePower) { this.pickaxePower = pickaxePower; }

    public double getMiningEfficiency() { return miningEfficiency != null ? miningEfficiency : 0.0; }
    public Double getRawMiningEfficiency() { return miningEfficiency; }
    public boolean hasMiningEfficiency() { return miningEfficiency != null; }
    public void setMiningEfficiency(Double miningEfficiency) { this.miningEfficiency = miningEfficiency; }

    public int getMaxDurability() { return maxDurability != null ? maxDurability : 0; }
    public Integer getRawMaxDurability() { return maxDurability; }
    public boolean hasMaxDurability() { return maxDurability != null && maxDurability > 0; }
    public void setMaxDurability(Integer maxDurability) { this.maxDurability = maxDurability; }

    public boolean isWillBreak() { return willBreak == null || willBreak; }
    public Boolean getRawWillBreak() { return willBreak; }
    public boolean hasWillBreak() { return willBreak != null; }
    public void setWillBreak(Boolean willBreak) { this.willBreak = willBreak; }

    public Map<String, Integer> getVanillaEnchants() { return vanillaEnchants; }
    public Map<String, Integer> getAdvancedEnchants() { return advancedEnchants; }
    public List<String> getGemSockets() { return gemSockets; }
    public Map<String, Double> getAuraSkillRequirements() { return auraSkillRequirements; }
    public Map<String, Double> getAuraSkillStats() { return auraSkillStats; }
    public int getRevisionId() { return revisionId; }
    public void setRevisionId(int revisionId) { this.revisionId = revisionId; }

    public String getGemColor() { return gemColor; }
    public boolean hasGemColor() { return gemColor != null && !gemColor.isEmpty(); }
    public void setGemColor(String gemColor) { this.gemColor = gemColor; }

    public String getGemUpgradeScaling() { return gemUpgradeScaling != null ? gemUpgradeScaling : "SUBSEQUENT"; }
    public void setGemUpgradeScaling(String gemUpgradeScaling) { this.gemUpgradeScaling = gemUpgradeScaling; }

    public List<String> getItemTypeRestrictions() { return itemTypeRestrictions; }

    public Double getSuccessRate() { return successRate != null ? successRate : 100.0; }
    public Double getRawSuccessRate() { return successRate; }
    public boolean hasSuccessRate() { return successRate != null; }
    public void setSuccessRate(Double successRate) { this.successRate = successRate; }

    // Advanced & Cosmetic Getters and Setters
    public String getItemSet() { return itemSet != null ? itemSet : ""; }
    public boolean hasItemSet() { return itemSet != null && !itemSet.isEmpty(); }
    public void setItemSet(String itemSet) { this.itemSet = itemSet; }

    public String getLoreFormat() { return loreFormat != null ? loreFormat : ""; }
    public boolean hasLoreFormat() { return loreFormat != null && !loreFormat.isEmpty(); }
    public void setLoreFormat(String loreFormat) { this.loreFormat = loreFormat; }

    public String getShieldPattern() { return shieldPattern != null ? shieldPattern : ""; }
    public boolean hasShieldPattern() { return shieldPattern != null && !shieldPattern.isEmpty(); }
    public void setShieldPattern(String shieldPattern) { this.shieldPattern = shieldPattern; }

    public String getDyeColor() { return dyeColor != null ? dyeColor : ""; }
    public boolean hasDyeColor() { return dyeColor != null && !dyeColor.isEmpty(); }
    public void setDyeColor(String dyeColor) { this.dyeColor = dyeColor; }

    public String getTrimMaterial() { return trimMaterial != null ? trimMaterial : ""; }
    public boolean hasTrimMaterial() { return trimMaterial != null && !trimMaterial.isEmpty(); }
    public void setTrimMaterial(String trimMaterial) { this.trimMaterial = trimMaterial; }

    public String getTrimPattern() { return trimPattern != null ? trimPattern : ""; }
    public boolean hasTrimPattern() { return trimPattern != null && !trimPattern.isEmpty(); }
    public void setTrimPattern(String trimPattern) { this.trimPattern = trimPattern; }

    public String getRequiredClass() { return requiredClass != null ? requiredClass : ""; }
    public boolean hasRequiredClass() { return requiredClass != null && !requiredClass.isEmpty(); }
    public void setRequiredClass(String requiredClass) { this.requiredClass = requiredClass; }

    public String getPermission() { return permission != null ? permission : ""; }
    public boolean hasPermission() { return permission != null && !permission.isEmpty(); }
    public void setPermission(String permission) { this.permission = permission; }

    public boolean isTwoHanded() { return Boolean.TRUE.equals(twoHanded); }
    public Boolean getRawTwoHanded() { return twoHanded; }
    public boolean hasTwoHanded() { return twoHanded != null; }
    public void setTwoHanded(Boolean twoHanded) { this.twoHanded = twoHanded; }

    public boolean isHandworn() { return Boolean.TRUE.equals(handworn); }
    public Boolean getRawHandworn() { return handworn; }
    public boolean hasHandworn() { return handworn != null; }
    public void setHandworn(Boolean handworn) { this.handworn = handworn; }

    public boolean isHideEnchants() { return Boolean.TRUE.equals(hideEnchants); }
    public Boolean getRawHideEnchants() { return hideEnchants; }
    public boolean hasHideEnchants() { return hideEnchants != null; }
    public void setHideEnchants(Boolean hideEnchants) { this.hideEnchants = hideEnchants; }

    public boolean isCanIdentify() { return Boolean.TRUE.equals(canIdentify); }
    public Boolean getRawCanIdentify() { return canIdentify; }
    public boolean hasCanIdentify() { return canIdentify != null; }
    public void setCanIdentify(Boolean canIdentify) { this.canIdentify = canIdentify; }

    public boolean isCanDeconstruct() { return Boolean.TRUE.equals(canDeconstruct); }
    public Boolean getRawCanDeconstruct() { return canDeconstruct; }
    public boolean hasCanDeconstruct() { return canDeconstruct != null; }
    public void setCanDeconstruct(Boolean canDeconstruct) { this.canDeconstruct = canDeconstruct; }

    public boolean isCanDeskin() { return Boolean.TRUE.equals(canDeskin); }
    public Boolean getRawCanDeskin() { return canDeskin; }
    public boolean hasCanDeskin() { return canDeskin != null; }
    public void setCanDeskin(Boolean canDeskin) { this.canDeskin = canDeskin; }

    public double getKnockback() { return knockback != null ? knockback : 0.0; }
    public Double getRawKnockback() { return knockback; }
    public boolean hasKnockback() { return knockback != null; }
    public void setKnockback(Double knockback) { this.knockback = knockback; }

    public double getRecoil() { return recoil != null ? recoil : 0.0; }
    public Double getRawRecoil() { return recoil; }
    public boolean hasRecoil() { return recoil != null; }
    public void setRecoil(Double recoil) { this.recoil = recoil; }

    public List<String> getCommands() { return commands; }
    public List<String> getPermEffects() { return permEffects; }
    public List<String> getPotionEffects() { return potionEffects; }
    public List<String> getCustomSounds() { return customSounds; }

    public String getItemParticles() { return itemParticles != null ? itemParticles : ""; }
    public boolean hasItemParticles() { return itemParticles != null && !itemParticles.isEmpty(); }
    public void setItemParticles(String itemParticles) { this.itemParticles = itemParticles; }

    public String getArrowParticles() { return arrowParticles != null ? arrowParticles : ""; }
    public boolean hasArrowParticles() { return arrowParticles != null && !arrowParticles.isEmpty(); }
    public void setArrowParticles(String arrowParticles) { this.arrowParticles = arrowParticles; }
}
