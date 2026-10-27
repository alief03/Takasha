package net.takasha.rpg.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.takasha.rpg.skills.AuraSkillType;
import net.takasha.rpg.skills.AuraStatType;
import net.takasha.rpg.skills.AuraTraitType;

import java.util.*;

public class TakashaRpgData {

    public static final int DEFAULT_MAX_HOMES = 1;
    public static final int ABSOLUTE_MAX_HOMES = 10;
    public static final double DEFAULT_BALANCE = 100.0;
    public static final double BASE_MANA = 20.0;

    public static final Set<String> DEFAULT_SURVIVAL_COMMANDS = Set.of(
        "money", "bal", "balance", "home", "sethome", "delhome", "homes", "back", "return", "help",
        "shop", "arena", "rpg",
        "skills", "sk", "stats", "mana", "sources", "top", "skilltop", "rank", "abtoggle", "abilitytoggle", "claimitems",
        "farming", "foraging", "mining", "fishing", "excavation", "archery", "defense", "fighting", "combat", "agility", "alchemy", "enchanting", "healing"
    );

    public enum ResetScope {
        ALL,
        SKILLS,
        COMMANDS,
        HOMES,
        ECONOMY
    }

    private String playerName = "";
    private UUID playerUuid = UUID.randomUUID();

    // Economy
    private double balance = DEFAULT_BALANCE;
    private int gems = 0;
    private int souls = 0;

    // Homes & Essentials
    private int maxHomes = DEFAULT_MAX_HOMES;
    private final Map<String, HomeLocation> homes = new LinkedHashMap<>();
    private HomeLocation lastDeathLocation = null;
    private HomeLocation lastTeleportLocation = null;

    // Command permissions
    private final Set<String> unlockedCommands = new HashSet<>();

    // AuraSkills
    private final Map<AuraSkillType, Integer> skillLevels = new EnumMap<>(AuraSkillType.class);
    private final Map<AuraSkillType, Double> skillXp = new EnumMap<>(AuraSkillType.class);

    // Ability Toggles (/abtoggle)
    private final Map<String, Boolean> abilityToggles = new HashMap<>();

    // Mana
    private double currentMana = BASE_MANA;

    // Client Preferences
    private boolean showScoreboard = true;

    // Active Sovereignty Title
    private String activeTitle = "";

    // Quest & RPG Progress Engine
    private final Map<String, Integer> questProgress = new LinkedHashMap<>();
    private final Set<String> completedQuests = new HashSet<>();
    private final Set<String> unlockedTitles = new HashSet<>();
    private final Map<String, Long> rpgStats = new LinkedHashMap<>();

    // Dungeon Crash Recovery
    private String pendingKeystoneRefund = "";
    private boolean pendingSafeTeleport = false;
    private double safeTeleportX = 0.0;
    private double safeTeleportY = 64.0;
    private double safeTeleportZ = 0.0;
    private String safeTeleportDim = "minecraft:overworld";

    // Active AuraSkills Mana Abilities States
    private boolean treecapitatorActive = false;
    private long treecapitatorCooldownUntil = 0;
    private long treecapitatorReadyUntil = 0;

    private boolean speedMineActive = false;
    private long speedMineCooldownUntil = 0;
    private long speedMineReadyUntil = 0;

    private boolean replenishActive = false;
    private long replenishCooldownUntil = 0;
    private long replenishReadyUntil = 0;

    private long sharpHookCooldownUntil = 0;
    private long sharpHookReadyUntil = 0;

    private long absorptionCooldownUntil = 0;
    private long absorptionReadyUntil = 0;

    private boolean terraformActive = false;
    private long terraformCooldownUntil = 0;
    private long terraformReadyUntil = 0;

    private boolean chargedShotActive = false;
    private long chargedShotCooldownUntil = 0;
    private long chargedShotReadyUntil = 0;

    private boolean lightningBladeActive = false;
    private long lightningBladeCooldownUntil = 0;
    private long lightningBladeReadyUntil = 0;

    public TakashaRpgData() {
        for (AuraSkillType skill : AuraSkillType.values()) {
            skillLevels.put(skill, 1);
            skillXp.put(skill, 0.0);
        }
        initDefaultCommands();
        unlockedTitles.add("G-01");
    }

    private void initDefaultCommands() {
        unlockedCommands.addAll(DEFAULT_SURVIVAL_COMMANDS);
    }

    public void resetProgress(ResetScope scope) {
        if (scope == ResetScope.ALL || scope == ResetScope.SKILLS) {
            for (AuraSkillType skill : AuraSkillType.values()) {
                skillLevels.put(skill, 1);
                skillXp.put(skill, 0.0);
            }
            this.currentMana = this.getMaxMana();
        }
        if (scope == ResetScope.ALL || scope == ResetScope.COMMANDS) {
            unlockedCommands.clear();
            initDefaultCommands();
        }
        if (scope == ResetScope.ALL || scope == ResetScope.HOMES) {
            homes.clear();
            maxHomes = DEFAULT_MAX_HOMES;
            lastDeathLocation = null;
            lastTeleportLocation = null;
        }
        if (scope == ResetScope.ALL || scope == ResetScope.ECONOMY) {
            balance = 0.0;
            gems = 0;
            souls = 0;
        }
        if (scope == ResetScope.ALL) {
            activeTitle = "";
            questProgress.clear();
            completedQuests.clear();
            unlockedTitles.clear();
            unlockedTitles.add("G-01");
            rpgStats.clear();
        }
    }

    // Quest & Title Progress Engine Methods
    public int getQuestProgress(String questId) {
        return questProgress.getOrDefault(questId, 0);
    }

    public void setQuestProgress(String questId, int progress) {
        questProgress.put(questId, Math.max(0, progress));
    }

    public int addQuestProgress(String questId, int amount) {
        int current = getQuestProgress(questId);
        int next = Math.max(0, current + amount);
        questProgress.put(questId, next);
        return next;
    }

    public Map<String, Integer> getQuestProgressMap() {
        return Collections.unmodifiableMap(questProgress);
    }

    public boolean isQuestCompleted(String questId) {
        return completedQuests.contains(questId);
    }

    public void setQuestCompleted(String questId) {
        completedQuests.add(questId);
    }

    public Set<String> getCompletedQuests() {
        return Collections.unmodifiableSet(completedQuests);
    }

    public boolean isTitleUnlocked(String titleCode) {
        if (titleCode == null || titleCode.isEmpty()) return true;
        String clean = titleCode.trim().toUpperCase();
        if (clean.equals("G-01") || clean.equals("G01")) return true;
        return unlockedTitles.contains(clean) || unlockedTitles.contains(clean.replace("-", ""));
    }

    public void unlockTitle(String titleCode) {
        if (titleCode != null && !titleCode.isEmpty()) {
            unlockedTitles.add(titleCode.trim().toUpperCase());
        }
    }

    public Set<String> getUnlockedTitles() {
        return Collections.unmodifiableSet(unlockedTitles);
    }

    public long getStat(String key) {
        return rpgStats.getOrDefault(key, 0L);
    }

    public void setStat(String key, long value) {
        rpgStats.put(key, Math.max(0L, value));
    }

    public long addStat(String key, long amount) {
        long current = getStat(key);
        long next = Math.max(0L, current + amount);
        rpgStats.put(key, next);
        return next;
    }

    public Map<String, Long> getStatsMap() {
        return Collections.unmodifiableMap(rpgStats);
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = Math.max(0.0, balance);
    }

    public void addBalance(double amount) {
        this.balance = Math.max(0.0, this.balance + amount);
    }

    public boolean removeBalance(double amount) {
        if (amount <= 0) return true;
        if (this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    public int getGems() {
        return gems;
    }

    public void setGems(int gems) {
        this.gems = Math.max(0, gems);
    }

    public void addGems(int amount) {
        this.gems = Math.max(0, this.gems + amount);
    }

    public boolean removeGems(int amount) {
        if (amount <= 0) return true;
        if (this.gems >= amount) {
            this.gems -= amount;
            return true;
        }
        return false;
    }

    public int getSouls() {
        return souls;
    }

    public void setSouls(int souls) {
        this.souls = Math.max(0, souls);
    }

    public void addSouls(int amount) {
        this.souls = Math.max(0, this.souls + amount);
    }

    public boolean removeSouls(int amount) {
        if (amount <= 0) return true;
        if (this.souls >= amount) {
            this.souls -= amount;
            return true;
        }
        return false;
    }

    public int getMaxHomes() {
        return maxHomes;
    }

    public String getActiveTitle() {
        return activeTitle != null ? activeTitle : "";
    }

    public void setActiveTitle(String activeTitle) {
        this.activeTitle = activeTitle != null ? activeTitle : "";
    }

    public boolean increaseMaxHomes(int count) {
        if (this.maxHomes < ABSOLUTE_MAX_HOMES) {
            this.maxHomes = Math.min(ABSOLUTE_MAX_HOMES, this.maxHomes + count);
            return true;
        }
        return false;
    }

    public Map<String, HomeLocation> getHomes() {
        return homes;
    }

    public boolean setHome(String name, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        return setHome(name, level, x, y, z, yaw, pitch, false);
    }

    public boolean setHome(String name, ServerLevel level, double x, double y, double z, float yaw, float pitch, boolean bypassLimit) {
        String cleanName = name.toLowerCase().trim();
        if (!bypassLimit && homes.size() >= maxHomes && !homes.containsKey(cleanName)) {
            return false;
        }
        homes.put(cleanName, new HomeLocation(level.dimension().identifier().toString(), x, y, z, yaw, pitch));
        return true;
    }

    public boolean delHome(String name) {
        return homes.remove(name.toLowerCase().trim()) != null;
    }

    public HomeLocation getHome(String name) {
        return homes.get(name.toLowerCase().trim());
    }

    public boolean hasHome(String name) {
        return homes.containsKey(name.toLowerCase().trim());
    }

    public HomeLocation getLastDeathLocation() {
        return lastDeathLocation;
    }

    public void setLastDeathLocation(ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        this.lastDeathLocation = new HomeLocation(level.dimension().identifier().toString(), x, y, z, yaw, pitch);
    }

    public HomeLocation getLastTeleportLocation() {
        return lastTeleportLocation;
    }

    public void setLastTeleportLocation(ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        this.lastTeleportLocation = new HomeLocation(level.dimension().identifier().toString(), x, y, z, yaw, pitch);
    }

    public boolean isCommandUnlocked(String command) {
        String clean = command.toLowerCase().trim();
        if (DEFAULT_SURVIVAL_COMMANDS.contains(clean)) {
            return true;
        }
        return unlockedCommands.contains(clean);
    }

    public boolean unlockCommand(String command) {
        return unlockedCommands.add(command.toLowerCase().trim());
    }

    public Set<String> getUnlockedCommands() {
        return Collections.unmodifiableSet(unlockedCommands);
    }

    public int getSkillLevel(AuraSkillType skill) {
        return skillLevels.getOrDefault(skill, 1);
    }

    public double getSkillXp(AuraSkillType skill) {
        return skillXp.getOrDefault(skill, 0.0);
    }

    public int getTotalSkillLevel() {
        int total = 0;
        for (AuraSkillType skill : AuraSkillType.values()) {
            total += getSkillLevel(skill);
        }
        return total;
    }

    public boolean addSkillXp(AuraSkillType skill, double amount) {
        double current = skillXp.getOrDefault(skill, 0.0) + amount;
        int currentLvl = skillLevels.getOrDefault(skill, 1);
        boolean leveledUp = false;

        double required = skill.getXpForNextLevel(currentLvl);
        while (current >= required && currentLvl < 1000) {
            current -= required;
            currentLvl++;
            leveledUp = true;
            required = skill.getXpForNextLevel(currentLvl);
        }

        skillLevels.put(skill, currentLvl);
        skillXp.put(skill, current);
        return leveledUp;
    }

    public void setSkillLevel(AuraSkillType skill, int level) {
        int clamped = Math.max(1, Math.min(1000, level));
        skillLevels.put(skill, clamped);
    }

    public void setSkillXp(AuraSkillType skill, double xp) {
        double clamped = Math.max(0.0, xp);
        skillXp.put(skill, clamped);
    }

    public double getStatValue(AuraStatType stat) {
        double value = 0.0;

        // FARMING: health 0.4 (intv 1), strength 1.0 (intv 2)
        int farmingLvl = getSkillLevel(AuraSkillType.FARMING);
        if (stat == AuraStatType.HEALTH) value += (farmingLvl - 1) * 0.4;
        if (stat == AuraStatType.STRENGTH) value += ((farmingLvl - 1) / 2) * 1.0;

        // FORAGING: strength 1.0 (intv 1), toughness 1.0 (intv 2)
        int foragingLvl = getSkillLevel(AuraSkillType.FORAGING);
        if (stat == AuraStatType.STRENGTH) value += (foragingLvl - 1) * 1.0;
        if (stat == AuraStatType.TOUGHNESS) value += ((foragingLvl - 1) / 2) * 1.0;

        // MINING: toughness 1.0 (intv 1), luck 1.0 (intv 2)
        int miningLvl = getSkillLevel(AuraSkillType.MINING);
        if (stat == AuraStatType.TOUGHNESS) value += (miningLvl - 1) * 1.0;
        if (stat == AuraStatType.LUCK) value += ((miningLvl - 1) / 2) * 1.0;

        // FISHING: luck 1.0 (intv 1), health 0.4 (intv 2)
        int fishingLvl = getSkillLevel(AuraSkillType.FISHING);
        if (stat == AuraStatType.LUCK) value += (fishingLvl - 1) * 1.0;
        if (stat == AuraStatType.HEALTH) value += ((fishingLvl - 1) / 2) * 0.4;

        // EXCAVATION: regeneration 1.0 (intv 1), strength 1.0 (intv 2)
        int excavationLvl = getSkillLevel(AuraSkillType.EXCAVATION);
        if (stat == AuraStatType.REGENERATION) value += (excavationLvl - 1) * 1.0;
        if (stat == AuraStatType.STRENGTH) value += ((excavationLvl - 1) / 2) * 1.0;

        // ARCHERY: crit_chance 1.0 (intv 1), strength 1.0 (intv 2)
        int archeryLvl = getSkillLevel(AuraSkillType.ARCHERY);
        if (stat == AuraStatType.CRIT_CHANCE) value += (archeryLvl - 1) * 1.0;
        if (stat == AuraStatType.STRENGTH) value += ((archeryLvl - 1) / 2) * 1.0;

        // DEFENSE: toughness 1.0 (intv 1), health 0.4 (intv 2)
        int defenseLvl = getSkillLevel(AuraSkillType.DEFENSE);
        if (stat == AuraStatType.TOUGHNESS) value += (defenseLvl - 1) * 1.0;
        if (stat == AuraStatType.HEALTH) value += ((defenseLvl - 1) / 2) * 0.4;

        // FIGHTING: crit_damage 1.0 (intv 1), regeneration 1.0 (intv 2)
        int fightingLvl = getSkillLevel(AuraSkillType.FIGHTING);
        if (stat == AuraStatType.CRIT_DAMAGE) value += (fightingLvl - 1) * 1.0;
        if (stat == AuraStatType.REGENERATION) value += ((fightingLvl - 1) / 2) * 1.0;

        // AGILITY: regeneration 1.0 (intv 1), wisdom 1.0 (intv 2), speed 1.0 (intv 1)
        int agilityLvl = getSkillLevel(AuraSkillType.AGILITY);
        if (stat == AuraStatType.REGENERATION) value += (agilityLvl - 1) * 1.0;
        if (stat == AuraStatType.WISDOM) value += ((agilityLvl - 1) / 2) * 1.0;
        if (stat == AuraStatType.SPEED) value += (agilityLvl - 1) * 1.0;

        // ALCHEMY: wisdom 1.0 (intv 1), health 0.4 (intv 2)
        int alchemyLvl = getSkillLevel(AuraSkillType.ALCHEMY);
        if (stat == AuraStatType.WISDOM) value += (alchemyLvl - 1) * 1.0;
        if (stat == AuraStatType.HEALTH) value += ((alchemyLvl - 1) / 2) * 0.4;

        // ENCHANTING: wisdom 1.0 (intv 1), luck 1.0 (intv 2)
        int enchantingLvl = getSkillLevel(AuraSkillType.ENCHANTING);
        if (stat == AuraStatType.WISDOM) value += (enchantingLvl - 1) * 1.0;
        if (stat == AuraStatType.LUCK) value += ((enchantingLvl - 1) / 2) * 1.0;

        return value;
    }

    public int getStatValueInt(AuraStatType stat) {
        return (int) Math.round(getStatValue(stat));
    }

    public int getPassiveAbilityLevel(AuraSkillType skill, int slot) {
        int lvl = getSkillLevel(skill);
        int unlock = slot;
        if (lvl < unlock) return 0;
        return 1 + ((lvl - unlock) / 5);
    }

    public int getManaAbilityLevel(AuraSkillType skill) {
        int lvl = getSkillLevel(skill);
        if (lvl < 6) return 0;
        return 1 + ((lvl - 6) / 6);
    }

    public double getManaAbilityCost(AuraSkillType skill) {
        int abilityLvl = getManaAbilityLevel(skill);
        if (abilityLvl <= 0) return 20.0;
        return switch (skill) {
            case FISHING -> 5.0 + (abilityLvl - 1) * 2.0;
            case ARCHERY -> 10.0 + (abilityLvl - 1) * 5.0;
            case DEFENSE -> 10.0 + (abilityLvl - 1) * 10.0;
            default -> 20.0 + (abilityLvl - 1) * 12.0;
        };
    }

    public double getManaAbilityCooldownSeconds(AuraSkillType skill) {
        int abilityLvl = getManaAbilityLevel(skill);
        if (abilityLvl <= 0) return 20.0;
        return switch (skill) {
            case FISHING -> Math.max(0.5, 2.0 - (abilityLvl - 1) * 0.1);
            case ARCHERY -> 0.0;
            default -> Math.max(5.0, 20.0 - (abilityLvl - 1) * 0.5);
        };
    }

    public double getTraitValue(AuraTraitType trait) {
        double statVal = getStatValue(trait.getPrimaryStat());
        return switch (trait) {
            case ATTACK_DAMAGE -> statVal * 0.4;
            case HP -> statVal * 1.0;
            case SATURATION_REGEN -> statVal * 0.02;
            case HUNGER_REGEN -> statVal * 0.02;
            case MANA_REGEN -> 0.1 + (statVal * 0.02);
            case FARMING_LUCK, FORAGING_LUCK, MINING_LUCK, FISHING_LUCK, EXCAVATION_LUCK -> statVal * 0.5;
            case EXPERIENCE_BONUS -> statVal * 0.4;
            case ANVIL_DISCOUNT -> Math.min(50.0, (-1.0 * Math.pow(1.025, -statVal) + 1.0) * 100.0);
            case MAX_MANA -> 20.0 + (statVal * 2.0);
            case DAMAGE_REDUCTION -> Math.min(80.0, (-1.0 * Math.pow(1.01, -statVal) + 1.0) * 100.0);
            case CRIT_CHANCE -> Math.min(100.0, statVal * 1.0);
            case CRIT_DAMAGE -> statVal * 1.0;
            case MOVEMENT_SPEED -> Math.min(300.0, statVal * 1.0);
            case LUCK -> statVal * 0.1;
            case DOUBLE_DROP -> Math.min(100.0, statVal * 0.2);
        };
    }

    public boolean isAbilityEnabled(String ability) {
        return abilityToggles.getOrDefault(ability.toLowerCase().trim(), true);
    }

    public boolean toggleAbility(String ability) {
        String key = ability.toLowerCase().trim();
        boolean current = isAbilityEnabled(key);
        abilityToggles.put(key, !current);
        return !current;
    }

    public double getCurrentMana() {
        return currentMana;
    }

    public void setCurrentMana(double currentMana) {
        this.currentMana = Math.max(0.0, Math.min(this.getMaxMana(), currentMana));
    }

    public double getMaxMana() {
        return getTraitValue(AuraTraitType.MAX_MANA);
    }

    public void setMaxMana(double maxMana) {
        if (this.currentMana > this.getMaxMana()) {
            this.currentMana = this.getMaxMana();
        }
    }

    public boolean consumeMana(double amount) {
        if (this.currentMana >= amount) {
            this.currentMana -= amount;
            return true;
        }
        return false;
    }

    public void restoreMana(double amount) {
        this.currentMana = Math.min(this.getMaxMana(), this.currentMana + amount);
    }

    public boolean isTreecapitatorActive() {
        return treecapitatorActive;
    }

    public void setTreecapitatorActive(boolean active) {
        this.treecapitatorActive = active;
    }

    public long getTreecapitatorCooldownUntil() {
        return treecapitatorCooldownUntil;
    }

    public void setTreecapitatorCooldownUntil(long timestamp) {
        this.treecapitatorCooldownUntil = timestamp;
    }

    public long getTreecapitatorReadyUntil() {
        return treecapitatorReadyUntil;
    }

    public void setTreecapitatorReadyUntil(long timestamp) {
        this.treecapitatorReadyUntil = timestamp;
    }

    public boolean isTreecapitatorReady() {
        return System.currentTimeMillis() < treecapitatorReadyUntil;
    }

    // Speed Mine
    public boolean isSpeedMineActive() {
        return speedMineActive;
    }

    public void setSpeedMineActive(boolean active) {
        this.speedMineActive = active;
    }

    public long getSpeedMineCooldownUntil() {
        return speedMineCooldownUntil;
    }

    public void setSpeedMineCooldownUntil(long timestamp) {
        this.speedMineCooldownUntil = timestamp;
    }

    public long getSpeedMineReadyUntil() {
        return speedMineReadyUntil;
    }

    public void setSpeedMineReadyUntil(long timestamp) {
        this.speedMineReadyUntil = timestamp;
    }

    public boolean isSpeedMineReady() {
        return System.currentTimeMillis() < speedMineReadyUntil;
    }

    // Replenish
    public boolean isReplenishActive() {
        return replenishActive;
    }

    public void setReplenishActive(boolean active) {
        this.replenishActive = active;
    }

    public long getReplenishCooldownUntil() {
        return replenishCooldownUntil;
    }

    public void setReplenishCooldownUntil(long timestamp) {
        this.replenishCooldownUntil = timestamp;
    }

    public long getReplenishReadyUntil() {
        return replenishReadyUntil;
    }

    public void setReplenishReadyUntil(long timestamp) {
        this.replenishReadyUntil = timestamp;
    }

    public boolean isReplenishReady() {
        return System.currentTimeMillis() < replenishReadyUntil;
    }

    // Sharp Hook
    public long getSharpHookCooldownUntil() {
        return sharpHookCooldownUntil;
    }

    public void setSharpHookCooldownUntil(long timestamp) {
        this.sharpHookCooldownUntil = timestamp;
    }

    public long getSharpHookReadyUntil() {
        return sharpHookReadyUntil;
    }

    public void setSharpHookReadyUntil(long timestamp) {
        this.sharpHookReadyUntil = timestamp;
    }

    public boolean isSharpHookReady() {
        return System.currentTimeMillis() < sharpHookReadyUntil;
    }

    // Absorption
    public long getAbsorptionCooldownUntil() {
        return absorptionCooldownUntil;
    }

    public void setAbsorptionCooldownUntil(long timestamp) {
        this.absorptionCooldownUntil = timestamp;
    }

    public long getAbsorptionReadyUntil() {
        return absorptionReadyUntil;
    }

    public void setAbsorptionReadyUntil(long timestamp) {
        this.absorptionReadyUntil = timestamp;
    }

    public boolean isAbsorptionReady() {
        return System.currentTimeMillis() < absorptionReadyUntil;
    }

    // Terraform (Shovels)
    public boolean isTerraformActive() {
        return terraformActive;
    }

    public void setTerraformActive(boolean active) {
        this.terraformActive = active;
    }

    public long getTerraformCooldownUntil() {
        return terraformCooldownUntil;
    }

    public void setTerraformCooldownUntil(long timestamp) {
        this.terraformCooldownUntil = timestamp;
    }

    public long getTerraformReadyUntil() {
        return terraformReadyUntil;
    }

    public void setTerraformReadyUntil(long timestamp) {
        this.terraformReadyUntil = timestamp;
    }

    public boolean isTerraformReady() {
        return System.currentTimeMillis() < terraformReadyUntil;
    }

    // Charged Shot (Bows & Crossbows)
    public boolean isChargedShotActive() {
        return chargedShotActive;
    }

    public void setChargedShotActive(boolean active) {
        this.chargedShotActive = active;
    }

    public long getChargedShotCooldownUntil() {
        return chargedShotCooldownUntil;
    }

    public void setChargedShotCooldownUntil(long timestamp) {
        this.chargedShotCooldownUntil = timestamp;
    }

    public long getChargedShotReadyUntil() {
        return chargedShotReadyUntil;
    }

    public void setChargedShotReadyUntil(long timestamp) {
        this.chargedShotReadyUntil = timestamp;
    }

    public boolean isChargedShotReady() {
        return System.currentTimeMillis() < chargedShotReadyUntil;
    }

    // Lightning Blade (Swords)
    public boolean isLightningBladeActive() {
        return lightningBladeActive;
    }

    public void setLightningBladeActive(boolean active) {
        this.lightningBladeActive = active;
    }

    public long getLightningBladeCooldownUntil() {
        return lightningBladeCooldownUntil;
    }

    public void setLightningBladeCooldownUntil(long timestamp) {
        this.lightningBladeCooldownUntil = timestamp;
    }

    public long getLightningBladeReadyUntil() {
        return lightningBladeReadyUntil;
    }

    public void setLightningBladeReadyUntil(long timestamp) {
        this.lightningBladeReadyUntil = timestamp;
    }

    public boolean isLightningBladeReady() {
        return System.currentTimeMillis() < lightningBladeReadyUntil;
    }

    public boolean isShowScoreboard() {
        return showScoreboard;
    }

    public void setShowScoreboard(boolean showScoreboard) {
        this.showScoreboard = showScoreboard;
    }

    public String getPendingKeystoneRefund() {
        return pendingKeystoneRefund;
    }

    public void setPendingKeystoneRefund(String tier) {
        this.pendingKeystoneRefund = tier != null ? tier : "";
    }

    public boolean isPendingSafeTeleport() {
        return pendingSafeTeleport;
    }

    public void setPendingSafeTeleport(boolean pending, String dim, double x, double y, double z) {
        this.pendingSafeTeleport = pending;
        this.safeTeleportDim = dim != null ? dim : "minecraft:overworld";
        this.safeTeleportX = x;
        this.safeTeleportY = y;
        this.safeTeleportZ = z;
    }

    public String getSafeTeleportDim() {
        return safeTeleportDim;
    }

    public double getSafeTeleportX() {
        return safeTeleportX;
    }

    public double getSafeTeleportY() {
        return safeTeleportY;
    }

    public double getSafeTeleportZ() {
        return safeTeleportZ;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("playerName", playerName);
        json.addProperty("playerUuid", playerUuid.toString());
        json.addProperty("balance", balance);
        json.addProperty("gems", gems);
        json.addProperty("souls", souls);
        json.addProperty("maxHomes", maxHomes);
        json.addProperty("showScoreboard", showScoreboard);

        // Ability Toggles
        JsonObject togglesObj = new JsonObject();
        for (Map.Entry<String, Boolean> entry : abilityToggles.entrySet()) {
            togglesObj.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("abilityToggles", togglesObj);

        // Homes
        JsonObject homesObj = new JsonObject();
        for (Map.Entry<String, HomeLocation> entry : homes.entrySet()) {
            homesObj.add(entry.getKey(), entry.getValue().toJson());
        }
        json.add("homes", homesObj);

        // Last death
        if (lastDeathLocation != null) {
            json.add("lastDeath", lastDeathLocation.toJson());
        }

        // Last teleport
        if (lastTeleportLocation != null) {
            json.add("lastTeleport", lastTeleportLocation.toJson());
        }

        // Unlocked commands
        JsonArray cmdArray = new JsonArray();
        for (String cmd : unlockedCommands) {
            cmdArray.add(cmd);
        }
        json.add("unlockedCommands", cmdArray);

        // Active Title
        json.addProperty("activeTitle", activeTitle);

        // Quests & Titles Progress
        JsonObject qProgObj = new JsonObject();
        for (Map.Entry<String, Integer> entry : questProgress.entrySet()) {
            qProgObj.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("questProgress", qProgObj);

        JsonArray compQuestsArr = new JsonArray();
        for (String qId : completedQuests) {
            compQuestsArr.add(qId);
        }
        json.add("completedQuests", compQuestsArr);

        JsonArray unlTitlesArr = new JsonArray();
        for (String tCode : unlockedTitles) {
            unlTitlesArr.add(tCode);
        }
        json.add("unlockedTitles", unlTitlesArr);

        JsonObject statsObj = new JsonObject();
        for (Map.Entry<String, Long> entry : rpgStats.entrySet()) {
            statsObj.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("rpgStats", statsObj);

        // Dungeon Crash Recovery
        if (!pendingKeystoneRefund.isEmpty()) {
            json.addProperty("pendingKeystoneRefund", pendingKeystoneRefund);
        }
        if (pendingSafeTeleport) {
            json.addProperty("pendingSafeTeleport", true);
            json.addProperty("safeTeleportX", safeTeleportX);
            json.addProperty("safeTeleportY", safeTeleportY);
            json.addProperty("safeTeleportZ", safeTeleportZ);
            json.addProperty("safeTeleportDim", safeTeleportDim);
        }

        // Skills
        JsonObject skillsObj = new JsonObject();
        for (AuraSkillType skill : AuraSkillType.values()) {
            JsonObject sObj = new JsonObject();
            sObj.addProperty("level", getSkillLevel(skill));
            sObj.addProperty("xp", getSkillXp(skill));
            skillsObj.add(skill.name(), sObj);
        }
        json.add("skills", skillsObj);

        return json;
    }

    public void fromJson(JsonObject json) {
        if (json.has("playerName")) playerName = json.get("playerName").getAsString();
        if (json.has("playerUuid")) playerUuid = UUID.fromString(json.get("playerUuid").getAsString());
        if (json.has("balance")) balance = json.get("balance").getAsDouble();
        if (json.has("gems")) gems = json.get("gems").getAsInt();
        if (json.has("souls")) souls = json.get("souls").getAsInt();
        if (json.has("maxHomes")) maxHomes = json.get("maxHomes").getAsInt();
        if (json.has("showScoreboard")) showScoreboard = json.get("showScoreboard").getAsBoolean();
        if (json.has("activeTitle")) activeTitle = json.get("activeTitle").getAsString();

        questProgress.clear();
        if (json.has("questProgress") && json.get("questProgress").isJsonObject()) {
            JsonObject qObj = json.getAsJsonObject("questProgress");
            for (Map.Entry<String, JsonElement> e : qObj.entrySet()) {
                questProgress.put(e.getKey(), e.getValue().getAsInt());
            }
        }

        completedQuests.clear();
        if (json.has("completedQuests") && json.get("completedQuests").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("completedQuests")) {
                completedQuests.add(el.getAsString());
            }
        }

        unlockedTitles.clear();
        unlockedTitles.add("G-01");
        if (json.has("unlockedTitles") && json.get("unlockedTitles").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("unlockedTitles")) {
                unlockedTitles.add(el.getAsString().toUpperCase());
            }
        }

        rpgStats.clear();
        if (json.has("rpgStats") && json.get("rpgStats").isJsonObject()) {
            JsonObject stObj = json.getAsJsonObject("rpgStats");
            for (Map.Entry<String, JsonElement> e : stObj.entrySet()) {
                rpgStats.put(e.getKey(), e.getValue().getAsLong());
            }
        }

        abilityToggles.clear();
        if (json.has("abilityToggles") && json.get("abilityToggles").isJsonObject()) {
            JsonObject togglesObj = json.getAsJsonObject("abilityToggles");
            for (Map.Entry<String, JsonElement> e : togglesObj.entrySet()) {
                abilityToggles.put(e.getKey(), e.getValue().getAsBoolean());
            }
        }

        if (json.has("pendingKeystoneRefund")) pendingKeystoneRefund = json.get("pendingKeystoneRefund").getAsString();
        if (json.has("pendingSafeTeleport")) {
            pendingSafeTeleport = json.get("pendingSafeTeleport").getAsBoolean();
            if (json.has("safeTeleportX")) safeTeleportX = json.get("safeTeleportX").getAsDouble();
            if (json.has("safeTeleportY")) safeTeleportY = json.get("safeTeleportY").getAsDouble();
            if (json.has("safeTeleportZ")) safeTeleportZ = json.get("safeTeleportZ").getAsDouble();
            if (json.has("safeTeleportDim")) safeTeleportDim = json.get("safeTeleportDim").getAsString();
        }

        // Homes
        homes.clear();
        if (json.has("homes") && json.get("homes").isJsonObject()) {
            JsonObject homesObj = json.getAsJsonObject("homes");
            for (Map.Entry<String, JsonElement> entry : homesObj.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    homes.put(entry.getKey(), HomeLocation.fromJson(entry.getValue().getAsJsonObject()));
                }
            }
        }

        // Last death
        if (json.has("lastDeath") && json.get("lastDeath").isJsonObject()) {
            lastDeathLocation = HomeLocation.fromJson(json.getAsJsonObject("lastDeath"));
        }

        // Last teleport
        if (json.has("lastTeleport") && json.get("lastTeleport").isJsonObject()) {
            lastTeleportLocation = HomeLocation.fromJson(json.getAsJsonObject("lastTeleport"));
        }

        // Unlocked commands
        unlockedCommands.addAll(DEFAULT_SURVIVAL_COMMANDS);
        if (json.has("unlockedCommands") && json.get("unlockedCommands").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("unlockedCommands")) {
                unlockedCommands.add(el.getAsString().toLowerCase().trim());
            }
        }

        // Skills (11 Authentic AuraSkills)
        if (json.has("skills") && json.get("skills").isJsonObject()) {
            JsonObject skillsObj = json.getAsJsonObject("skills");
            for (AuraSkillType skill : AuraSkillType.values()) {
                if (skillsObj.has(skill.name())) {
                    JsonObject sObj = skillsObj.getAsJsonObject(skill.name());
                    int lvl = sObj.has("level") ? sObj.get("level").getAsInt() : 1;
                    double xp = sObj.has("xp") ? sObj.get("xp").getAsDouble() : 0.0;
                    skillLevels.put(skill, Math.max(1, lvl));
                    skillXp.put(skill, Math.max(0.0, xp));
                }
            }
            // Auto-merge legacy COMBAT data into canonical FIGHTING
            if (skillsObj.has("COMBAT")) {
                JsonObject cObj = skillsObj.getAsJsonObject("COMBAT");
                int cLvl = cObj.has("level") ? cObj.get("level").getAsInt() : 1;
                double cXp = cObj.has("xp") ? cObj.get("xp").getAsDouble() : 0.0;
                int currentFightingLvl = skillLevels.getOrDefault(AuraSkillType.FIGHTING, 1);
                double currentFightingXp = skillXp.getOrDefault(AuraSkillType.FIGHTING, 0.0);
                if (cLvl > currentFightingLvl || (cLvl == currentFightingLvl && cXp > currentFightingXp)) {
                    skillLevels.put(AuraSkillType.FIGHTING, Math.max(1, cLvl));
                    skillXp.put(AuraSkillType.FIGHTING, Math.max(0.0, cXp));
                }
            }
            // Auto-merge legacy HEALING data into canonical ALCHEMY
            if (skillsObj.has("HEALING")) {
                JsonObject hObj = skillsObj.getAsJsonObject("HEALING");
                int hLvl = hObj.has("level") ? hObj.get("level").getAsInt() : 1;
                double hXp = hObj.has("xp") ? hObj.get("xp").getAsDouble() : 0.0;
                int currentAlchemyLvl = skillLevels.getOrDefault(AuraSkillType.ALCHEMY, 1);
                double currentAlchemyXp = skillXp.getOrDefault(AuraSkillType.ALCHEMY, 0.0);
                if (hLvl > currentAlchemyLvl || (hLvl == currentAlchemyLvl && hXp > currentAlchemyXp)) {
                    skillLevels.put(AuraSkillType.ALCHEMY, Math.max(1, hLvl));
                    skillXp.put(AuraSkillType.ALCHEMY, Math.max(0.0, hXp));
                }
            }
        }
    }

    public record HomeLocation(String dimension, double x, double y, double z, float yaw, float pitch) {
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("dim", dimension);
            json.addProperty("x", x);
            json.addProperty("y", y);
            json.addProperty("z", z);
            json.addProperty("yaw", yaw);
            json.addProperty("pitch", pitch);
            return json;
        }

        public static HomeLocation fromJson(JsonObject json) {
            String dim = json.has("dim") ? json.get("dim").getAsString() : "minecraft:overworld";
            double x = json.has("x") ? json.get("x").getAsDouble() : 0.0;
            double y = json.has("y") ? json.get("y").getAsDouble() : 64.0;
            double z = json.has("z") ? json.get("z").getAsDouble() : 0.0;
            float yaw = json.has("yaw") ? json.get("yaw").getAsFloat() : 0.0f;
            float pitch = json.has("pitch") ? json.get("pitch").getAsFloat() : 0.0f;
            return new HomeLocation(dim, x, y, z, yaw, pitch);
        }

        public ServerLevel resolveLevel(ServerPlayer player) {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimension));
            if (player.level().getServer() == null) return player.level();
            ServerLevel target = player.level().getServer().getLevel(key);
            return target != null ? target : player.level();
        }
    }
}
