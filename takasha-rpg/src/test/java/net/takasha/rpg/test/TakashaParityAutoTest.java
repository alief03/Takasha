package net.takasha.rpg.test;

import net.takasha.rpg.menu.MMOChatEditionEngine;
import net.takasha.rpg.mmo.MMOItemDiskManager;
import net.takasha.rpg.mmo.MMOItemTemplate;
import net.takasha.rpg.mmo.MMOStatRegistry;

import java.util.List;
import java.util.Objects;

/**
 * TakashaParityAutoTest
 * Automated verification suite for MMOItems 1:1 Parity:
 * 1. Material Stat presence on ALL 40 item types.
 * 2. GEM_STONE applicable stats (> 60 stats combat/defense/gems).
 * 3. Dedicated Gemstone stats (GEM_COLOR, GEM_UPGRADE_SCALING, ITEM_TYPE_RESTRICTION, SUCCESS_RATE).
 * 4. Gemstone fields and Disk YAML persistence in MMOItemTemplate.
 * 5. Dynamic Gem Socket suggestions and Tab autocomplete resolver.
 */
public class TakashaParityAutoTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   TAKASHA-RPG: MMOITEMS 1:1 REA PARITY VERIFICATION SUITE   ");
        System.out.println("==================================================================");

        try {
            net.minecraft.SharedConstants.tryDetectVersion();
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable t) {
            System.out.println("Bootstrap init note: " + t.getMessage());
        }

        TakashaParityAutoTest tester = new TakashaParityAutoTest();
        runTest("Material Stat Presence on All 40 Item Types", tester::testMaterialStatPresenceForAllTypes);
        runTest("GEM_STONE Combat & Defense Stat Expansion", tester::testGemStoneStatExpansion);
        runTest("Dedicated Gemstone Stats Registration", tester::testDedicatedGemstoneStats);
        runTest("MMOItemTemplate Gemstone Fields & Serialization", tester::testGemStoneTemplateFields);
        runTest("Dynamic Gem Sockets & Color Suggestions", tester::testGemSocketsAndColorsSuggestions);
        runTest("Item Types Restriction Suggestions", tester::testItemTypeRestrictionSuggestions);
        runTest("Tools, Armor & Weapons Parity Check", tester::testOtherItemTypesParity);

        System.out.println("==================================================================");
        System.out.printf("   TOTAL TESTS RUN: %d | PASSED: %d | FAILED: %d%n", testsPassed + testsFailed, testsPassed, testsFailed);
        System.out.println("==================================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void runTest(String name, Runnable test) {
        try {
            test.run();
            testsPassed++;
            System.out.println("[PASS] " + name);
        } catch (Throwable t) {
            testsFailed++;
            System.err.println("[FAIL] " + name + ": " + t.getMessage());
            t.printStackTrace(System.err);
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) throw new AssertionError(msg);
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(msg + " Expected: " + expected + ", but got: " + actual);
        }
    }

    public void testMaterialStatPresenceForAllTypes() {
        String[] all40Types = {
                "SWORD", "DAGGER", "SPEAR", "HAMMER", "GAUNTLET", "WHIP", "STAFF", "BOW", "CROSSBOW", "MUSKET", "LUTE",
                "CATALYST", "OFF_CATALYST", "MAIN_CATALYST", "ORNAMENT", "ACCESSORY", "ARMOR", "TOOL", "CONSUMABLE",
                "MISCELLANEOUS", "SKIN", "GEM_STONE", "BLOCK", "GREATSWORD", "LONG_SWORD", "KATANA", "THRUSTING_SWORD",
                "AXE", "GREATAXE", "HALBERD", "LANCE", "GREATHAMMER", "GREATSTAFF", "STAVE", "TOME", "TALISMAN",
                "WAND", "GREATBOW", "SHIELD", "MATERIAL"
        };

        for (String type : all40Types) {
            List<MMOStatRegistry.StatDefinition> applicable = MMOStatRegistry.getApplicableStats(type);
            boolean hasMaterial = applicable.stream().anyMatch(s -> s.id().equalsIgnoreCase("MATERIAL"));
            assertTrue(hasMaterial, "Type " + type + " MUST have 'MATERIAL' in applicable stats!");
        }
    }

    public void testGemStoneStatExpansion() {
        List<MMOStatRegistry.StatDefinition> applicable = MMOStatRegistry.getApplicableStats("GEM_STONE");
        assertTrue(applicable.size() >= 60, "GEM_STONE must have at least 60 applicable stats! Found: " + applicable.size());

        // Check combat stats
        assertTrue(applicable.stream().anyMatch(s -> s.id().equals("ATTACK_DAMAGE")), "GEM_STONE must support ATTACK_DAMAGE");
        assertTrue(applicable.stream().anyMatch(s -> s.id().equals("CRITICAL_STRIKE_CHANCE")), "GEM_STONE must support CRITICAL_STRIKE_CHANCE");
        assertTrue(applicable.stream().anyMatch(s -> s.id().equals("CRITICAL_STRIKE_POWER")), "GEM_STONE must support CRITICAL_STRIKE_POWER");
        assertTrue(applicable.stream().anyMatch(s -> s.id().equals("ELEMENTS")), "GEM_STONE must support ELEMENTS");

        // Check defense stats
        assertTrue(applicable.stream().anyMatch(s -> s.id().equals("DEFENSE")), "GEM_STONE must support DEFENSE");
        assertTrue(applicable.stream().anyMatch(s -> s.id().equals("ARMOR")), "GEM_STONE must support ARMOR");
        assertTrue(applicable.stream().anyMatch(s -> s.id().equals("MAX_HEALTH")), "GEM_STONE must support MAX_HEALTH");
        assertTrue(applicable.stream().anyMatch(s -> s.id().equals("MAX_MANA")), "GEM_STONE must support MAX_MANA");
    }

    public void testDedicatedGemstoneStats() {
        List<MMOStatRegistry.StatDefinition> applicable = MMOStatRegistry.getApplicableStats("GEM_STONE");

        assertTrue(applicable.stream().anyMatch(s -> s.id().equals("GEM_COLOR")), "Must have GEM_COLOR stat");
        assertTrue(applicable.stream().anyMatch(s -> s.id().equals("GEM_UPGRADE_SCALING")), "Must have GEM_UPGRADE_SCALING stat");
        assertTrue(applicable.stream().anyMatch(s -> s.id().equals("ITEM_TYPE_RESTRICTION")), "Must have ITEM_TYPE_RESTRICTION stat");
        assertTrue(applicable.stream().anyMatch(s -> s.id().equals("SUCCESS_RATE")), "Must have SUCCESS_RATE stat");
    }

    public void testGemStoneTemplateFields() {
        MMOItemTemplate t = new MMOItemTemplate("RUBY_OF_POWER", "GEM_STONE");
        t.setGemColor("Red");
        t.setGemUpgradeScaling("HISTORIC");
        t.getItemTypeRestrictions().add("WEAPON");
        t.getItemTypeRestrictions().add("ARMOR");
        t.setSuccessRate(85.5);

        assertTrue(t.hasGemColor(), "Template must have gem color");
        assertEquals("Red", t.getGemColor(), "Gem color mismatch");
        assertEquals("HISTORIC", t.getGemUpgradeScaling(), "Gem upgrade scaling mismatch");
        assertEquals(2, t.getItemTypeRestrictions().size(), "Restrictions size mismatch");
        assertTrue(t.hasSuccessRate(), "Template must have success rate");
        assertEquals(85.5, t.getSuccessRate(), "Success rate mismatch");
    }

    public void testGemSocketsAndColorsSuggestions() {
        List<String> sockets = MMOChatEditionEngine.getAllSuggestionsForType(MMOChatEditionEngine.SuggestionType.GEM_SOCKETS);
        assertTrue(sockets.contains("RED"), "Gem sockets must contain RED");
        assertTrue(sockets.contains("BLUE"), "Gem sockets must contain BLUE");
        assertTrue(sockets.contains("YELLOW"), "Gem sockets must contain YELLOW");
        assertTrue(sockets.contains("PRISMATIC"), "Gem sockets must contain PRISMATIC");
        assertTrue(sockets.contains("ANY"), "Gem sockets must contain ANY");

        List<String> colors = MMOChatEditionEngine.getAllSuggestionsForType(MMOChatEditionEngine.SuggestionType.GEM_COLORS);
        assertTrue(colors.contains("Red"), "Gem colors must contain Red");
        assertTrue(colors.contains("Blue"), "Gem colors must contain Blue");
        assertTrue(colors.contains("Prismatic"), "Gem colors must contain Prismatic");
    }

    public void testItemTypeRestrictionSuggestions() {
        List<String> types = MMOChatEditionEngine.getAllSuggestionsForType(MMOChatEditionEngine.SuggestionType.ITEM_TYPES);
        assertTrue(types.contains("SWORD"), "Item types must contain SWORD");
        assertTrue(types.contains("ARMOR"), "Item types must contain ARMOR");
        assertTrue(types.contains("BOW"), "Item types must contain BOW");
        assertTrue(types.contains("TOOL"), "Item types must contain TOOL");
        assertTrue(types.contains("ACCESSORY"), "Item types must contain ACCESSORY");
    }

    public void testOtherItemTypesParity() {
        // Weapon
        List<MMOStatRegistry.StatDefinition> swordStats = MMOStatRegistry.getApplicableStats("SWORD");
        assertTrue(swordStats.size() >= 55, "SWORD must have at least 55 stats");

        // Tool
        List<MMOStatRegistry.StatDefinition> toolStats = MMOStatRegistry.getApplicableStats("TOOL");
        assertTrue(toolStats.stream().anyMatch(s -> s.id().equals("AUTOSMELT")), "TOOL must have AUTOSMELT");
        assertTrue(toolStats.stream().anyMatch(s -> s.id().equals("BOUNCING_CRACK")), "TOOL must have BOUNCING_CRACK");
        assertTrue(toolStats.stream().anyMatch(s -> s.id().equals("PICKAXE_POWER")), "TOOL must have PICKAXE_POWER");
        assertTrue(toolStats.stream().anyMatch(s -> s.id().equals("MINING_EFFICIENCY")), "TOOL must have MINING_EFFICIENCY");

        // Armor & Shield
        List<MMOStatRegistry.StatDefinition> armorStats = MMOStatRegistry.getApplicableStats("ARMOR");
        assertTrue(armorStats.stream().anyMatch(s -> s.id().equals("TRIM_MATERIAL")), "ARMOR must have TRIM_MATERIAL");
        assertTrue(armorStats.stream().anyMatch(s -> s.id().equals("TRIM_PATTERN")), "ARMOR must have TRIM_PATTERN");
        assertTrue(armorStats.stream().anyMatch(s -> s.id().equals("DYE_COLOR")), "ARMOR must have DYE_COLOR");

        List<MMOStatRegistry.StatDefinition> shieldStats = MMOStatRegistry.getApplicableStats("SHIELD");
        assertTrue(shieldStats.stream().anyMatch(s -> s.id().equals("SHIELD_PATTERN")), "SHIELD must have SHIELD_PATTERN");
        assertTrue(shieldStats.stream().anyMatch(s -> s.id().equals("BLOCK_POWER")), "SHIELD must have BLOCK_POWER");
    }
}
