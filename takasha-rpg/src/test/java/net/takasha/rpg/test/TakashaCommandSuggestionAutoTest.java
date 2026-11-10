package net.takasha.rpg.test;

import net.takasha.rpg.menu.MMOChatEditionEngine;

import java.util.List;
import java.util.Objects;

public class TakashaCommandSuggestionAutoTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   TAKASHA-RPG: COMMAND & CHAT SUGGESTIONS VERIFICATION SUITE   ");
        System.out.println("==================================================================");

        TakashaCommandSuggestionAutoTest tester = new TakashaCommandSuggestionAutoTest();
        runTest("All SuggestionTypes Non-Null", tester::testAllSuggestionTypes);
        runTest("336 Custom Enchants + Vanilla Suggestions", tester::testEnchantSuggestions);
        runTest("Materials Suggestions Non-Null", tester::testMaterialsSuggestions);
        runTest("Name & Lore Color Code Suggestions", tester::testNameAndLoreSuggestions);
        runTest("Numeric Stat Formula Suggestions", tester::testNumericSuggestions);
        runTest("Tier & Gem Socket Suggestions", tester::testTierAndGemSocketSuggestions);
        runTest("ID Placeholder Suggestions", tester::testIdSuggestions);

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
            System.err.println("[FAIL] " + name + " -> " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + ", but got: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) {
            throw new AssertionError("Condition failed: " + msg);
        }
    }

    private static void assertTrue(boolean condition) {
        assertTrue(condition, "Expected true");
    }

    private static void assertFalse(boolean condition, String msg) {
        if (condition) {
            throw new AssertionError("Condition failed: " + msg);
        }
    }

    private static void assertNotNull(Object obj) {
        if (obj == null) {
            throw new AssertionError("Expected non-null object");
        }
    }

    void testAllSuggestionTypes() {
        for (MMOChatEditionEngine.SuggestionType type : MMOChatEditionEngine.SuggestionType.values()) {
            List<String> suggestions = MMOChatEditionEngine.getAllSuggestionsForType(type);
            assertNotNull(suggestions);
            if (type != MMOChatEditionEngine.SuggestionType.NONE && type != MMOChatEditionEngine.SuggestionType.GENERIC) {
                assertFalse(suggestions.isEmpty(), "Suggestions for " + type + " must not be empty!");
            }
        }
    }

    void testEnchantSuggestions() {
        net.takasha.rpg.enchant.TakashaEnchantRegistry.initialize();
        List<String> enchants = MMOChatEditionEngine.getAllSuggestionsForType(MMOChatEditionEngine.SuggestionType.ENCHANTS);
        assertNotNull(enchants);
        assertTrue(enchants.size() >= 336, "Enchant suggestions must cover all custom enchants! Found: " + enchants.size());
        assertTrue(enchants.contains("lifesteal"));
        assertTrue(enchants.contains("cleave"));
        assertTrue(enchants.contains("telepathy"));
        assertTrue(enchants.contains("sharpness"));
        assertTrue(enchants.contains("unbreaking"));
    }

    void testMaterialsSuggestions() {
        List<String> materials = MMOChatEditionEngine.getAllSuggestionsForType(MMOChatEditionEngine.SuggestionType.MATERIALS);
        assertNotNull(materials);
    }

    void testNameAndLoreSuggestions() {
        List<String> nameSuggestions = MMOChatEditionEngine.getAllSuggestionsForType(MMOChatEditionEngine.SuggestionType.NAME);
        assertTrue(nameSuggestions.contains("&6"));
        assertTrue(nameSuggestions.contains("&c"));
        assertTrue(nameSuggestions.contains("&a"));
        assertTrue(nameSuggestions.contains("&#FFD700"));
        assertTrue(nameSuggestions.contains("[display_name]"));

        List<String> loreSuggestions = MMOChatEditionEngine.getAllSuggestionsForType(MMOChatEditionEngine.SuggestionType.LORE);
        assertTrue(loreSuggestions.contains("&7"));
        assertTrue(loreSuggestions.contains("&f"));
        assertTrue(loreSuggestions.contains("&7&m---------------------"));
        assertTrue(loreSuggestions.contains("[lore_text]"));
    }

    void testNumericSuggestions() {
        List<String> numeric = MMOChatEditionEngine.getAllSuggestionsForType(MMOChatEditionEngine.SuggestionType.NUMERIC);
        assertTrue(numeric.contains("10.0"));
        assertTrue(numeric.contains("25.0"));
        assertTrue(numeric.contains("10 -> 25"));
        assertTrue(numeric.contains("10 2 0 0 5 30"));
    }

    void testTierAndGemSocketSuggestions() {
        List<String> tiers = MMOChatEditionEngine.getAllSuggestionsForType(MMOChatEditionEngine.SuggestionType.TIER);
        assertTrue(tiers.contains("COMMON"));
        assertTrue(tiers.contains("MYTHIC"));

        List<String> sockets = MMOChatEditionEngine.getAllSuggestionsForType(MMOChatEditionEngine.SuggestionType.GEM_SOCKETS);
        assertTrue(sockets.contains("RED"));
        assertTrue(sockets.contains("BLUE"));
        assertTrue(sockets.contains("PRISMATIC"));
    }

    void testIdSuggestions() {
        List<String> ids = MMOChatEditionEngine.getAllSuggestionsForType(MMOChatEditionEngine.SuggestionType.ID);
        assertTrue(ids.contains("[id]"));
        assertTrue(ids.contains("NEW_ITEM"));
    }
}
