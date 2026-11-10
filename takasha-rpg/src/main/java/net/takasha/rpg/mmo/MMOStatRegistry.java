package net.takasha.rpg.mmo;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * MMOStatRegistry
 * Catalog of Takasha RPG item statistics.
 * Maps authentic vanilla icon materials, display names, categories, descriptions,
 * compatible item types, and interaction types.
 */
public class MMOStatRegistry {

    public enum StatCategory {
        DISPLAY("Display"),
        GENERAL("General"),
        WEAPON("Weapon"),
        EXTRA_DAMAGE("Extra Damage"),
        DEFENSE("Defense"),
        DAMAGE_REDUCTION("Damage Reduction"),
        RPG("RPG Stats"),
        TOOL("Tools"),
        CONSUMABLE("Consumables"),
        GEMS("Gems & Sockets"),
        ENCHANTS("Enchantments"),
        ABILITIES("Abilities"),
        UPGRADE("Upgrading"),
        REQUIREMENTS("Requirements"),
        RESTRICTIONS("Restrictions"),
        EFFECTS("Effects"),
        SOUNDS("Sounds");

        private final String title;
        StatCategory(String title) { this.title = title; }
        public String getTitle() { return title; }
    }

    public enum InteractionType {
        NUMERIC,
        PERCENTAGE,
        BOOLEAN,
        STRING,
        SUB_MENU_ABILITIES,
        SUB_MENU_ELEMENTS,
        SUB_MENU_UPGRADE,
        SUB_MENU_GEMS,
        SUB_MENU_ENCHANTS,
        SUB_MENU_SOUNDS,
        SUB_MENU_PARTICLES,
        SUB_MENU_RECIPE
    }

    public record StatDefinition(
            String id,
            String displayName,
            Item icon,
            StatCategory category,
            InteractionType interactionType,
            String description,
            List<String> compatibleTypes
    ) {
        public boolean isCompatible(String itemType) {
            if (compatibleTypes == null || compatibleTypes.isEmpty()) return true;
            if (itemType == null) return false;
            String upper = itemType.toUpperCase(Locale.ROOT);
            String lower = upper.toLowerCase(Locale.ROOT);

            // Spigot negative tag check (e.g. !miscellaneous, !block)
            if (compatibleTypes.contains("!" + lower)) return false;
            if (compatibleTypes.contains("!miscellaneous") && (upper.equals("MISCELLANEOUS") || upper.equals("MATERIAL"))) return false;
            if (compatibleTypes.contains("!block") && upper.equals("BLOCK")) return false;

            if (compatibleTypes.contains("all")) return true;

            for (String ct : compatibleTypes) {
                if (ct.equalsIgnoreCase(upper)) return true;
                if (ct.equalsIgnoreCase("weapon") && isWeapon(upper)) return true;
                if (ct.equalsIgnoreCase("armor") && isArmor(upper)) return true;
                if (ct.equalsIgnoreCase("tool") && isTool(upper)) return true;
                if (ct.equalsIgnoreCase("gem_stone") && upper.equals("GEM_STONE")) return true;
                if (ct.equalsIgnoreCase("consumable") && upper.equals("CONSUMABLE")) return true;
                if (ct.equalsIgnoreCase("equipment") && (isWeapon(upper) || isArmor(upper) || isEquipment(upper))) return true;
            }
            return false;
        }
    }

    public static boolean isWeapon(String type) {
        if (type == null) return false;
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "SWORD", "AXE", "PIERCE", "BLUNT", "GREATSWORD", "KATANA", "HALBERD",
                 "STAFF", "TOME", "DAGGER", "HAMMER", "GAUNTLET", "WHIP", "BOW",
                 "CROSSBOW", "MUSKET", "LANCE", "LONG_SWORD", "THRUSTING_SWORD", "LUTE",
                 "GREATAXE", "GREATHAMMER", "GREATSTAFF", "STAVE", "WAND", "GREATBOW" -> true;
            default -> false;
        };
    }

    public static boolean isArmor(String type) {
        if (type == null) return false;
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "ARMOR", "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "SHIELD",
                 "ACCESSORY", "ORNAMENT", "TALISMAN", "CATALYST", "OFF_CATALYST", "MAIN_CATALYST" -> true;
            default -> false;
        };
    }

    public static boolean isEquipment(String type) {
        if (type == null) return false;
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "ACCESSORY", "ORNAMENT", "TALISMAN", "CATALYST", "OFF_CATALYST", "MAIN_CATALYST", "SHIELD" -> true;
            default -> false;
        };
    }

    public static boolean isTool(String type) {
        if (type == null) return false;
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "TOOL", "PICKAXE", "SHOVEL", "HOE", "SHEARS", "FISHING_ROD" -> true;
            default -> false;
        };
    }

    private static final Map<String, StatDefinition> STATS_BY_ID = new LinkedHashMap<>();

    private static void register(String id, String displayName, Item icon, StatCategory category,
                                 InteractionType interactionType, String description, String... compatibleTypes) {
        StatDefinition def = new StatDefinition(id, displayName, icon, category, interactionType, description, List.of(compatibleTypes));
        STATS_BY_ID.put(id, def);
    }

    static {
        // --- 1. DISPLAY & IDENTITY ---
        register("MATERIAL", "Material", Items.GRASS_BLOCK, StatCategory.DISPLAY, InteractionType.STRING,
                "The base item material (Grass Block, Netherite Sword, Bow, etc.).", "all");
        register("NAME", "Display Name", Items.NAME_TAG, StatCategory.DISPLAY, InteractionType.STRING,
                "The item display name and color codes.", "all");
        register("CUSTOM_MODEL_DATA", "Custom Model Data", Items.PAINTING, StatCategory.DISPLAY, InteractionType.NUMERIC,
                "Integer custom model data for resourcepacks.", "all");
        register("LORE", "Lore", Items.WRITABLE_BOOK, StatCategory.DISPLAY, InteractionType.STRING,
                "Custom descriptive lore lines.", "all");
        register("TIER", "Item Tier", Items.DIAMOND, StatCategory.GENERAL, InteractionType.STRING,
                "Item rarity tier (Common, Rare, Legendary, Mythic).", "all");
        register("SET", "Item Set", Items.LEATHER_CHESTPLATE, StatCategory.GENERAL, InteractionType.STRING,
                "Equipment set bonus name (Dragon, Steel, etc.).", "armor", "weapon");
        register("DISPLAYED_TYPE", "Displayed Type", Items.NAME_TAG, StatCategory.DISPLAY, InteractionType.STRING,
                "Custom type name displayed on tooltip.", "all");
        register("LORE_FORMAT", "Lore Format", Items.BOOK, StatCategory.DISPLAY, InteractionType.STRING,
                "Custom lore template format.", "all");
        register("SHIELD_PATTERN", "Shield Pattern", Items.SHIELD, StatCategory.DISPLAY, InteractionType.STRING,
                "Custom banner shield pattern.", "SHIELD", "armor");
        register("DYE_COLOR", "Dye Color", Items.DYE.pink(), StatCategory.DISPLAY, InteractionType.STRING,
                "Custom leather armor dye color.", "armor");
        register("TRIM_MATERIAL", "Trim Material", Items.DYE.cyan(), StatCategory.DISPLAY, InteractionType.STRING,
                "Armor trim material (iron, gold, diamond, netherite, etc.).", "armor");
        register("TRIM_PATTERN", "Trim Pattern", Items.PAPER, StatCategory.DISPLAY, InteractionType.STRING,
                "Armor trim pattern (sentry, vex, wild, coast, etc.).", "armor");

        // --- 2. WEAPON & OFFENSE STATS ---
        register("ATTACK_DAMAGE", "Attack Damage", Items.IRON_SWORD, StatCategory.WEAPON, InteractionType.NUMERIC,
                "Base melee strike damage.", "weapon", "gem_stone");
        register("ATTACK_SPEED", "Attack Speed", Items.DYE.lightGray(), StatCategory.WEAPON, InteractionType.NUMERIC,
                "Strikes per second cooldown speed.", "weapon", "gem_stone");
        register("CRITICAL_STRIKE_CHANCE", "Critical Strike Chance", Items.NETHER_STAR, StatCategory.WEAPON, InteractionType.PERCENTAGE,
                "Chance in % to land a critical strike.", "weapon", "gem_stone");
        register("CRITICAL_STRIKE_POWER", "Critical Strike Power", Items.NETHER_STAR, StatCategory.WEAPON, InteractionType.PERCENTAGE,
                "Extra damage % dealt on critical strikes.", "weapon", "gem_stone");
        register("SKILL_CRITICAL_STRIKE_CHANCE", "Skill Crit Chance", Items.NETHER_STAR, StatCategory.WEAPON, InteractionType.PERCENTAGE,
                "Chance in % to land skill critical strikes.", "weapon", "armor", "gem_stone");
        register("SKILL_CRITICAL_STRIKE_POWER", "Skill Crit Power", Items.NETHER_STAR, StatCategory.WEAPON, InteractionType.PERCENTAGE,
                "Extra damage % dealt on skill crits.", "weapon", "armor", "gem_stone");
        register("RANGE", "Combat Range", Items.STICK, StatCategory.WEAPON, InteractionType.NUMERIC,
                "Attack and cast range in blocks.", "weapon", "gem_stone");
        register("MANA_COST", "Mana Cost", Items.LAPIS_LAZULI, StatCategory.WEAPON, InteractionType.NUMERIC,
                "Mana consumed per weapon attack.", "weapon");
        register("STAMINA_COST", "Stamina Cost", Items.DYE.lightGray(), StatCategory.WEAPON, InteractionType.NUMERIC,
                "Stamina spent by your weapon to strike.", "weapon");
        register("ARROW_VELOCITY", "Arrow Velocity", Items.ARROW, StatCategory.WEAPON, InteractionType.PERCENTAGE,
                "Speed and trajectory multiplier for arrows.", "BOW", "CROSSBOW", "weapon", "gem_stone");
        register("BLUNT_POWER", "Blunt Power", Items.IRON_AXE, StatCategory.WEAPON, InteractionType.NUMERIC,
                "Blunt stun and crushing power.", "weapon", "BLUNT", "HAMMER", "GAUNTLET", "gem_stone");
        register("BLUNT_RATING", "Blunt Rating", Items.IRON_AXE, StatCategory.WEAPON, InteractionType.PERCENTAGE,
                "Chance % to trigger blunt attack effects.", "weapon", "BLUNT", "HAMMER", "GAUNTLET", "gem_stone");
        register("TWO_HANDED", "Two Handed", Items.IRON_INGOT, StatCategory.WEAPON, InteractionType.BOOLEAN,
                "Significantly slower if holding offhand shield.", "weapon");
        register("HANDWORN", "Handworn", Items.STRING, StatCategory.WEAPON, InteractionType.BOOLEAN,
                "Can be held along with two-handed weapons.", "weapon", "GAUNTLET", "OFF_CATALYST", "CATALYST");
        register("KNOCKBACK", "Knockback", Items.IRON_HORSE_ARMOR, StatCategory.WEAPON, InteractionType.NUMERIC,
                "Knockback force applied on hit or self-recoil.", "weapon", "MUSKET", "gem_stone");
        register("RECOIL", "Recoil", Items.IRON_HORSE_ARMOR, StatCategory.WEAPON, InteractionType.PERCENTAGE,
                "Shooting inaccuracy and weapon kickback.", "weapon", "MUSKET", "gem_stone");
        register("LIFESTEAL", "Lifesteal", Items.REDSTONE, StatCategory.WEAPON, InteractionType.PERCENTAGE,
                "Percentage of melee damage absorbed as health.", "weapon", "gem_stone");
        register("SPELL_VAMPIRISM", "Spell Vampirism", Items.REDSTONE, StatCategory.WEAPON, InteractionType.PERCENTAGE,
                "Percentage of ability damage absorbed as health.", "weapon", "armor", "gem_stone");

        // --- 3. DAMAGE MULTIPLIERS & ELEMENTS ---
        register("PVE_DAMAGE", "PvE Damage", Items.PORKCHOP, StatCategory.EXTRA_DAMAGE, InteractionType.PERCENTAGE,
                "Bonus damage % against non-player mobs.", "weapon", "gem_stone");
        register("PVP_DAMAGE", "PvP Damage", Items.SKELETON_SKULL, StatCategory.EXTRA_DAMAGE, InteractionType.PERCENTAGE,
                "Bonus damage % against other players.", "weapon", "gem_stone");
        register("WEAPON_DAMAGE", "Weapon Damage", Items.IRON_SWORD, StatCategory.EXTRA_DAMAGE, InteractionType.PERCENTAGE,
                "Percentage modifier to base weapon damage.", "weapon", "gem_stone");
        register("SKILL_DAMAGE", "Skill Damage", Items.BOOK, StatCategory.EXTRA_DAMAGE, InteractionType.PERCENTAGE,
                "Percentage modifier to active ability damage.", "weapon", "armor", "gem_stone");
        register("PROJECTILE_DAMAGE", "Projectile Damage", Items.ARROW, StatCategory.EXTRA_DAMAGE, InteractionType.PERCENTAGE,
                "Percentage modifier to ranged projectiles.", "weapon", "gem_stone");
        register("MAGIC_DAMAGE", "Magic Damage", Items.MAGMA_CREAM, StatCategory.EXTRA_DAMAGE, InteractionType.PERCENTAGE,
                "Percentage modifier to magical spells.", "weapon", "armor", "gem_stone");
        register("PHYSICAL_DAMAGE", "Physical Damage", Items.IRON_AXE, StatCategory.EXTRA_DAMAGE, InteractionType.PERCENTAGE,
                "Percentage modifier to physical strikes.", "weapon", "gem_stone");
        register("UNDEAD_DAMAGE", "Undead Damage", Items.SKELETON_SKULL, StatCategory.EXTRA_DAMAGE, InteractionType.PERCENTAGE,
                "Bonus damage % against undead monsters.", "weapon", "gem_stone");
        register("ELEMENTS", "Elements", Items.SLIME_BALL, StatCategory.EXTRA_DAMAGE, InteractionType.SUB_MENU_ELEMENTS,
                "Configure 6 elements damage & defense (Fire, Ice, Lightning, Earth, Light, Dark).", "weapon", "armor", "gem_stone");

        // --- 4. DEFENSE & ARMOR STATS ---
        register("ARMOR", "Armor", Items.GOLDEN_CHESTPLATE, StatCategory.DEFENSE, InteractionType.NUMERIC,
                "Vanilla armor defense points.", "armor", "gem_stone");
        register("ARMOR_TOUGHNESS", "Armor Toughness", Items.DIAMOND_CHESTPLATE, StatCategory.DEFENSE, InteractionType.NUMERIC,
                "Vanilla armor toughness points.", "armor", "gem_stone");
        register("MAX_HEALTH", "Max Health", Items.GOLDEN_APPLE, StatCategory.DEFENSE, InteractionType.NUMERIC,
                "Bonus maximum health added to player.", "armor", "weapon", "gem_stone");
        register("KNOCKBACK_RESISTANCE", "Knockback Resistance", Items.CHAINMAIL_CHESTPLATE, StatCategory.DEFENSE, InteractionType.NUMERIC,
                "Resistance against knockback from creepers/explosions.", "armor", "gem_stone");
        register("MOVEMENT_SPEED", "Movement Speed", Items.LEATHER_BOOTS, StatCategory.DEFENSE, InteractionType.NUMERIC,
                "Walking and running speed multiplier.", "armor", "BOOTS", "gem_stone");
        register("BLOCK_POWER", "Block Power", Items.IRON_HELMET, StatCategory.DEFENSE, InteractionType.PERCENTAGE,
                "Percentage of damage absorbed when blocking.", "armor", "SHIELD", "gem_stone");
        register("BLOCK_RATING", "Block Rating", Items.IRON_HELMET, StatCategory.DEFENSE, InteractionType.PERCENTAGE,
                "Chance % to automatically block attacks.", "armor", "SHIELD", "gem_stone");
        register("BLOCK_COOLDOWN_REDUCTION", "Block Cooldown Reduction", Items.IRON_HELMET, StatCategory.DEFENSE, InteractionType.PERCENTAGE,
                "Reduces blocking recovery cooldown %.", "armor", "SHIELD", "gem_stone");
        register("DODGE_RATING", "Dodge Rating", Items.FEATHER, StatCategory.DEFENSE, InteractionType.PERCENTAGE,
                "Chance % to completely dodge incoming damage.", "armor", "gem_stone");
        register("DODGE_COOLDOWN_REDUCTION", "Dodge Cooldown Reduction", Items.FEATHER, StatCategory.DEFENSE, InteractionType.PERCENTAGE,
                "Reduces dodging cooldown %.", "armor", "gem_stone");
        register("PARRY_RATING", "Parry Rating", Items.BUCKET, StatCategory.DEFENSE, InteractionType.PERCENTAGE,
                "Chance % to parry melee strikes.", "weapon", "armor", "gem_stone");
        register("PARRY_COOLDOWN_REDUCTION", "Parry Cooldown Reduction", Items.BUCKET, StatCategory.DEFENSE, InteractionType.PERCENTAGE,
                "Reduces parrying recovery cooldown %.", "weapon", "armor", "gem_stone");

        // --- 5. DAMAGE REDUCTION STATS ---
        register("DEFENSE", "Defense", Items.SHIELD, StatCategory.DAMAGE_REDUCTION, InteractionType.NUMERIC,
                "PoE defense rating mitigating physical damage.", "armor", "gem_stone");
        register("DAMAGE_REDUCTION", "Damage Reduction", Items.IRON_CHESTPLATE, StatCategory.DAMAGE_REDUCTION, InteractionType.PERCENTAGE,
                "Direct percentage reduction to all damage.", "armor", "gem_stone");
        register("FALL_DAMAGE_REDUCTION", "Fall Damage Reduction", Items.FEATHER, StatCategory.DAMAGE_REDUCTION, InteractionType.PERCENTAGE,
                "Percentage reduction to fall damage.", "armor", "BOOTS", "gem_stone");
        register("FIRE_DAMAGE_REDUCTION", "Fire Damage Reduction", Items.BLAZE_POWDER, StatCategory.DAMAGE_REDUCTION, InteractionType.PERCENTAGE,
                "Percentage reduction to fire and lava damage.", "armor", "gem_stone");
        register("MAGIC_DAMAGE_REDUCTION", "Magic Damage Reduction", Items.POTION, StatCategory.DAMAGE_REDUCTION, InteractionType.PERCENTAGE,
                "Percentage reduction to potion and magic spells.", "armor", "gem_stone");
        register("PROJECTILE_DAMAGE_REDUCTION", "Projectile Damage Reduction", Items.SNOWBALL, StatCategory.DAMAGE_REDUCTION, InteractionType.PERCENTAGE,
                "Percentage reduction to incoming arrows.", "armor", "gem_stone");
        register("PHYSICAL_DAMAGE_REDUCTION", "Physical Damage Reduction", Items.LEATHER_CHESTPLATE, StatCategory.DAMAGE_REDUCTION, InteractionType.PERCENTAGE,
                "Percentage reduction to melee damage.", "armor", "gem_stone");
        register("PVE_DAMAGE_REDUCTION", "PvE Damage Reduction", Items.PORKCHOP, StatCategory.DAMAGE_REDUCTION, InteractionType.PERCENTAGE,
                "Reduces damage taken from monsters.", "armor", "gem_stone");
        register("PVP_DAMAGE_REDUCTION", "PvP Damage Reduction", Items.SKELETON_SKULL, StatCategory.DAMAGE_REDUCTION, InteractionType.PERCENTAGE,
                "Reduces damage taken from players.", "armor", "gem_stone");

        // --- 6. RPG RESOURCE & COOLDOWN STATS ---
        register("MAX_MANA", "Max Mana", Items.LAPIS_LAZULI, StatCategory.RPG, InteractionType.NUMERIC,
                "Adds mana points to player maximum mana reservoir.", "armor", "weapon", "gem_stone");
        register("COOLDOWN_REDUCTION", "Skill Cooldown Reduction", Items.BOOK, StatCategory.RPG, InteractionType.PERCENTAGE,
                "Reduces cooldown timer of skills and abilities %.", "armor", "weapon", "gem_stone");

        // --- 7. TOOLS & UTILITY STATS ---
        register("AUTOSMELT", "Autosmelt", Items.COAL, StatCategory.TOOL, InteractionType.BOOLEAN,
                "Automatically smelts mined raw ores into ingots.", "tool", "PICKAXE");
        register("BOUNCING_CRACK", "Bouncing Crack", Items.COBBLESTONE_WALL, StatCategory.TOOL, InteractionType.BOOLEAN,
                "Breaks adjacent identical ore blocks in a 3x3x3 cube.", "tool", "PICKAXE");
        register("PICKAXE_POWER", "Pickaxe Power", Items.IRON_PICKAXE, StatCategory.TOOL, InteractionType.NUMERIC,
                "Custom block breaking strength for RPG blocks.", "tool", "PICKAXE");
        register("MINING_EFFICIENCY", "Mining Efficiency", Items.DIAMOND_PICKAXE, StatCategory.TOOL, InteractionType.PERCENTAGE,
                "Increases mining and block digging speed %.", "tool", "PICKAXE");
        register("MAX_DURABILITY", "Maximum Custom Durability", Items.SHEARS, StatCategory.TOOL, InteractionType.NUMERIC,
                "Custom NBT durability before item breaks.", "all");
        register("WILL_BREAK", "Lost When Broken", Items.ANVIL, StatCategory.TOOL, InteractionType.BOOLEAN,
                "Whether item is deleted permanently when reaching 0 durability.", "all");
        register("UNBREAKABLE", "Unbreakable", Items.ANVIL, StatCategory.TOOL, InteractionType.BOOLEAN,
                "Infinite durability without wearing down.", "all");

        // --- 8. ABILITIES, GEMS & ENCHANTS ---
        register("ABILITIES", "Item Abilities", Items.BLAZE_POWDER, StatCategory.ABILITIES, InteractionType.SUB_MENU_ABILITIES,
                "Configure active/passive combat skills, triggers and cooldowns.", "all");
        register("GEM_SOCKETS", "Gem Sockets", Items.EMERALD, StatCategory.GEMS, InteractionType.SUB_MENU_GEMS,
                "Socket slots for gem stones (Red, Blue, Yellow, Empty).", "weapon", "armor");
        register("GEM_COLOR", "Gem Color", Items.DYE.lightBlue(), StatCategory.GEMS, InteractionType.STRING,
                "Socket color in which gem can be applied (Red, Blue, Yellow, Green, Prismatic, Nether, Rainbow).", "gem_stone");
        register("GEM_UPGRADE_SCALING", "Gem Upgrade Scaling", Items.DYE.lime(), StatCategory.UPGRADE, InteractionType.STRING,
                "How gem stats scale when item upgrades (SUBSEQUENT, NEVER, HISTORIC).", "gem_stone");
        register("ITEM_TYPE_RESTRICTION", "Item Type Restriction", Items.EMERALD, StatCategory.RESTRICTIONS, InteractionType.STRING,
                "Types on which this gem can be applied (ARMOR, WEAPON, SWORD, etc.).", "gem_stone");
        register("SUCCESS_RATE", "Success Rate", Items.EMERALD, StatCategory.GEMS, InteractionType.PERCENTAGE,
                "Chance % of gem to successfully apply onto item.", "gem_stone", "skin");
        register("ENCHANTS", "Enchantments", Items.ENCHANTED_BOOK, StatCategory.ENCHANTS, InteractionType.SUB_MENU_ENCHANTS,
                "Vanilla and custom enchantments and level formulas.", "all");
        register("HIDE_ENCHANTS", "Hide Enchantments", Items.ENCHANTED_BOOK, StatCategory.ENCHANTS, InteractionType.BOOLEAN,
                "Hides vanilla enchantments from item lore.", "all");
        register("UPGRADE", "Item Upgrading", Items.FLINT, StatCategory.UPGRADE, InteractionType.SUB_MENU_UPGRADE,
                "Max upgrade levels, success rate, and upgrade stone recipe.", "weapon", "armor");

        // --- 9. REQUIREMENTS & PERMISSIONS ---
        register("REQUIRED_LEVEL", "Level Requirement", Items.EXPERIENCE_BOTTLE, StatCategory.REQUIREMENTS, InteractionType.NUMERIC,
                "Minimum character level required to equip/use item.", "all");
        register("REQUIRED_CLASS", "Required Class", Items.WRITABLE_BOOK, StatCategory.REQUIREMENTS, InteractionType.STRING,
                "RPG profession/class required to use item.", "all");
        register("PERMISSION", "Permission Requirement", Items.OAK_SIGN, StatCategory.REQUIREMENTS, InteractionType.STRING,
                "Permission node required to use item.", "all");

        // --- 10. CONSUMABLES & INTERACTIONS ---
        register("RESTORE_HEALTH", "Health Restoration", Items.DYE.red(), StatCategory.CONSUMABLE, InteractionType.NUMERIC,
                "Restores health instantly upon consumption.", "consumable");
        register("RESTORE_FOOD", "Food Restoration", Items.PORKCHOP, StatCategory.CONSUMABLE, InteractionType.NUMERIC,
                "Restores food level upon consumption.", "consumable");
        register("RESTORE_SATURATION", "Saturation Restoration", Items.GOLDEN_CARROT, StatCategory.CONSUMABLE, InteractionType.NUMERIC,
                "Restores saturation points upon consumption.", "consumable");
        register("RESTORE_MANA", "Mana Restoration", Items.LAPIS_LAZULI, StatCategory.CONSUMABLE, InteractionType.NUMERIC,
                "Restores player Mana pool upon consumption.", "consumable");
        register("RESTORE_STAMINA", "Stamina Restoration", Items.DYE.lightGray(), StatCategory.CONSUMABLE, InteractionType.NUMERIC,
                "Restores player Stamina upon consumption.", "consumable");
        register("CAN_IDENTIFY", "Can Identify?", Items.PAPER, StatCategory.CONSUMABLE, InteractionType.BOOLEAN,
                "Can identify unidentified items via drag-and-drop.", "consumable");
        register("CAN_DECONSTRUCT", "Can Deconstruct?", Items.PAPER, StatCategory.CONSUMABLE, InteractionType.BOOLEAN,
                "Can deconstruct tiered items into raw essences via drag-and-drop.", "consumable");
        register("CAN_DESKIN", "Can Deskin?", Items.SHEARS, StatCategory.CONSUMABLE, InteractionType.BOOLEAN,
                "Removes custom skin from item.", "consumable");
        register("ITEM_COOLDOWN", "Item Cooldown", Items.COOKED_CHICKEN, StatCategory.CONSUMABLE, InteractionType.NUMERIC,
                "Cooldown in seconds before consumable can be used again.", "consumable");

        // --- 11. EFFECTS, SOUNDS & PARTICLES ---
        register("PERM_EFFECTS", "Permanent Effects", Items.POTION, StatCategory.EFFECTS, InteractionType.STRING,
                "Permanent potion effects granted while holding/wearing item.", "weapon", "armor");
        register("POTION_EFFECTS", "Potion Effects", Items.POTION, StatCategory.EFFECTS, InteractionType.STRING,
                "Potion effects granted on consumption.", "consumable");
        register("CUSTOM_SOUNDS", "Custom Sounds", Items.JUKEBOX, StatCategory.SOUNDS, InteractionType.SUB_MENU_SOUNDS,
                "Custom sound effects for attack, hit, and swing.", "weapon", "tool");
        register("COMMANDS", "Commands", Items.COMMAND_BLOCK_MINECART, StatCategory.GENERAL, InteractionType.STRING,
                "Server commands executed on right click.", "weapon", "consumable");
        register("ITEM_PARTICLES", "Item Particles", Items.STAINED_GLASS.pink(), StatCategory.DISPLAY, InteractionType.SUB_MENU_PARTICLES,
                "Particle trail displayed while holding/wearing.", "weapon", "armor");
        register("ARROW_PARTICLES", "Arrow Particles", Items.ARROW, StatCategory.DISPLAY, InteractionType.SUB_MENU_PARTICLES,
                "Particle trail emitted by fired arrows.", "BOW", "CROSSBOW", "weapon");
        register("DISABLE_INTERACTION", "Disable Interaction", Items.BARRIER, StatCategory.RESTRICTIONS, InteractionType.BOOLEAN,
                "Disables crafting, smelting, or enchanting interactions.", "all");
    }

    public static Collection<StatDefinition> getAllStats() {
        return Collections.unmodifiableCollection(STATS_BY_ID.values());
    }

    public static StatDefinition getStat(String id) {
        if (id == null) return null;
        return STATS_BY_ID.get(id.toUpperCase(Locale.ROOT));
    }

    public static List<StatDefinition> getApplicableStats(String itemType) {
        List<StatDefinition> list = new ArrayList<>();
        for (StatDefinition def : STATS_BY_ID.values()) {
            if (def.isCompatible(itemType)) {
                list.add(def);
            }
        }
        return list;
    }
}
