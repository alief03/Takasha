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

import java.util.*;

public class TakashaRpgData {

    public static final int DEFAULT_MAX_HOMES = 1;
    public static final int ABSOLUTE_MAX_HOMES = 10;
    public static final double DEFAULT_BALANCE = 100.0;
    public static final double BASE_MANA = 100.0;

    private String playerName = "";
    private UUID playerUuid = UUID.randomUUID();

    // Economy
    private double balance = DEFAULT_BALANCE;
    private int gems = 0;

    // Homes & Essentials
    private int maxHomes = DEFAULT_MAX_HOMES;
    private final Map<String, HomeLocation> homes = new LinkedHashMap<>();
    private HomeLocation lastDeathLocation = null;

    // Command permissions
    private final Set<String> unlockedCommands = new HashSet<>();

    // AuraSkills
    private final Map<AuraSkillType, Integer> skillLevels = new EnumMap<>(AuraSkillType.class);
    private final Map<AuraSkillType, Double> skillXp = new EnumMap<>(AuraSkillType.class);

    // Mana
    private double currentMana = BASE_MANA;
    private double maxMana = BASE_MANA;

    // Client Preferences
    private boolean showScoreboard = true;

    public TakashaRpgData() {
        for (AuraSkillType skill : AuraSkillType.values()) {
            skillLevels.put(skill, 1);
            skillXp.put(skill, 0.0);
        }
        // Default unlocked commands
        unlockedCommands.add("money");
        unlockedCommands.add("pay");
        unlockedCommands.add("worth");
        unlockedCommands.add("bal");
        unlockedCommands.add("home");
        unlockedCommands.add("sethome");
        unlockedCommands.add("delhome");
        unlockedCommands.add("homes");
        unlockedCommands.add("back");
        unlockedCommands.add("skills");
        unlockedCommands.add("shop");
        unlockedCommands.add("arena");
        unlockedCommands.add("rpg");
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

    public int getMaxHomes() {
        return maxHomes;
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
        String cleanName = name.toLowerCase().trim();
        if (homes.size() >= maxHomes && !homes.containsKey(cleanName)) {
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

    public HomeLocation getLastDeathLocation() {
        return lastDeathLocation;
    }

    public void setLastDeathLocation(ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        this.lastDeathLocation = new HomeLocation(level.dimension().identifier().toString(), x, y, z, yaw, pitch);
    }

    public boolean isCommandUnlocked(String command) {
        return unlockedCommands.contains(command.toLowerCase().trim());
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
        for (int lvl : skillLevels.values()) {
            total += lvl;
        }
        return total;
    }

    public boolean addSkillXp(AuraSkillType skill, double amount) {
        double current = skillXp.getOrDefault(skill, 0.0) + amount;
        int currentLvl = skillLevels.getOrDefault(skill, 1);
        boolean leveledUp = false;

        double required = skill.getXpForNextLevel(currentLvl);
        while (current >= required && currentLvl < 100) {
            current -= required;
            currentLvl++;
            leveledUp = true;
            required = skill.getXpForNextLevel(currentLvl);
        }

        skillLevels.put(skill, currentLvl);
        skillXp.put(skill, current);
        return leveledUp;
    }

    public int getStatValue(AuraStatType stat) {
        int value = 0;
        for (AuraSkillType skill : AuraSkillType.values()) {
            int lvl = getSkillLevel(skill);
            if (skill.getPrimaryStat() == stat) {
                value += (lvl - 1) * 2;
            } else if (skill.getSecondaryStat() == stat) {
                value += (lvl - 1);
            }
        }
        return value;
    }

    public double getCurrentMana() {
        return currentMana;
    }

    public void setCurrentMana(double currentMana) {
        this.currentMana = Math.max(0.0, Math.min(this.maxMana, currentMana));
    }

    public double getMaxMana() {
        return maxMana;
    }

    public void setMaxMana(double maxMana) {
        this.maxMana = maxMana;
        if (this.currentMana > this.maxMana) {
            this.currentMana = this.maxMana;
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
        this.currentMana = Math.min(this.maxMana, this.currentMana + amount);
    }

    public boolean isShowScoreboard() {
        return showScoreboard;
    }

    public void setShowScoreboard(boolean showScoreboard) {
        this.showScoreboard = showScoreboard;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("playerName", playerName);
        json.addProperty("playerUuid", playerUuid.toString());
        json.addProperty("balance", balance);
        json.addProperty("gems", gems);
        json.addProperty("maxHomes", maxHomes);
        json.addProperty("showScoreboard", showScoreboard);

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

        // Unlocked commands
        JsonArray cmdArray = new JsonArray();
        for (String cmd : unlockedCommands) {
            cmdArray.add(cmd);
        }
        json.add("unlockedCommands", cmdArray);

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
        if (json.has("maxHomes")) maxHomes = json.get("maxHomes").getAsInt();
        if (json.has("showScoreboard")) showScoreboard = json.get("showScoreboard").getAsBoolean();

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

        // Unlocked commands
        if (json.has("unlockedCommands") && json.get("unlockedCommands").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("unlockedCommands")) {
                unlockedCommands.add(el.getAsString().toLowerCase().trim());
            }
        }

        // Skills
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
