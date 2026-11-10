package net.takasha.rpg.test;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.takasha.rpg.mmo.TakashaColorParser;

import java.util.Objects;

public class TakashaColorParserAutoTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   TAKASHA-RPG: AUTOMATED COLOR CODE ENGINE VERIFICATION SUITE   ");
        System.out.println("==================================================================");

        TakashaColorParserAutoTest tester = new TakashaColorParserAutoTest();
        runTest("Legacy Colors &0-&f", tester::testLegacyColorCodes);
        runTest("Formatting Codes &l, &o, &r", tester::testFormattingCodes);
        runTest("Hex Colors (&#RRGGBB, <#RRGGBB>, &x)", tester::testHexColorCodes);
        runTest("Gradients (<gradient:#HEX1:#HEX2>)", tester::testGradientParsing);
        runTest("Default Non-Italic for Items", tester::testDefaultNonItalic);
        runTest("Complex Multi-Segment Colors", tester::testComplexMultiSegment);
        runTest("Null & Empty String Safety", tester::testNullAndEmptySafety);

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

    private static void assertFalse(boolean condition) {
        assertFalse(condition, "Expected false");
    }

    private static void assertNotNull(Object obj) {
        if (obj == null) {
            throw new AssertionError("Expected non-null object");
        }
    }

    private static void assertNull(Object obj) {
        if (obj != null) {
            throw new AssertionError("Expected null but got: " + obj);
        }
    }

    void testLegacyColorCodes() {
        Component c1 = TakashaColorParser.parse("&6Gold Sword", false);
        assertNotNull(c1);
        assertEquals("Gold Sword", c1.getString());

        Component c2 = TakashaColorParser.parse("§cRed Flame", false);
        assertNotNull(c2);
        assertEquals("Red Flame", c2.getString());

        Component c3 = TakashaColorParser.parse("&aGreen &bAqua &eYellow", false);
        assertNotNull(c3);
        assertEquals("Green Aqua Yellow", c3.getString());
        // Verify child components
        assertTrue(c3.getSiblings().size() >= 3);
        assertEquals(TextColor.fromRgb(0x55FF55), c3.getSiblings().get(0).getStyle().getColor());
        assertEquals(TextColor.fromRgb(0x55FFFF), c3.getSiblings().get(1).getStyle().getColor());
        assertEquals(TextColor.fromRgb(0xFFFF55), c3.getSiblings().get(2).getStyle().getColor());
    }

    void testFormattingCodes() {
        Component bold = TakashaColorParser.parse("&6&lBold Gold", false);
        assertEquals("Bold Gold", bold.getString());
        assertTrue(bold.getSiblings().getFirst().getStyle().isBold());
        assertEquals(TextColor.fromRgb(0xFFAA00), bold.getSiblings().getFirst().getStyle().getColor());

        Component italic = TakashaColorParser.parse("&c&oItalic Red", true);
        assertEquals("Italic Red", italic.getString());
        assertTrue(italic.getSiblings().getFirst().getStyle().isItalic());

        Component reset = TakashaColorParser.parse("&6Gold &rNormal", false);
        assertEquals("Gold Normal", reset.getString());
        assertEquals(TextColor.fromRgb(0xFFAA00), reset.getSiblings().get(0).getStyle().getColor());
        assertNull(reset.getSiblings().get(1).getStyle().getColor());
    }

    void testHexColorCodes() {
        Component spigotHex = TakashaColorParser.parse("&#FFD700Golden Sun", false);
        assertNotNull(spigotHex);
        assertEquals("Golden Sun", spigotHex.getString());
        assertEquals(TextColor.fromRgb(0xFFD700), spigotHex.getSiblings().getFirst().getStyle().getColor());

        Component tagHex = TakashaColorParser.parse("<#00FF88>Mint Blade</color>", false);
        assertNotNull(tagHex);
        assertEquals("Mint Blade", tagHex.getString());
        assertEquals(TextColor.fromRgb(0x00FF88), tagHex.getSiblings().getFirst().getStyle().getColor());

        Component bungeeHex = TakashaColorParser.parse("&x&f&f&a&a&0&0Bungee Gold", false);
        assertNotNull(bungeeHex);
        assertEquals("Bungee Gold", bungeeHex.getString());
        assertEquals(TextColor.fromRgb(0xFFAA00), bungeeHex.getSiblings().getFirst().getStyle().getColor());
    }

    void testGradientParsing() {
        Component grad = TakashaColorParser.parse("<gradient:#FF0000:#0000FF>FireIce</gradient>", true);
        assertNotNull(grad);
        assertEquals("FireIce", grad.getString());
        assertEquals(7, grad.getSiblings().size()); // 7 characters interpolated
        TextColor firstColor = grad.getSiblings().getFirst().getStyle().getColor();
        TextColor lastColor = grad.getSiblings().getLast().getStyle().getColor();
        assertNotNull(firstColor);
        assertNotNull(lastColor);
        assertEquals(0xFF0000, firstColor.getValue());
        assertEquals(0x0000FF, lastColor.getValue());
    }

    void testDefaultNonItalic() {
        Component itemName = TakashaColorParser.parseItemName("&6Sakura Katana");
        assertEquals("Sakura Katana", itemName.getString());
        assertFalse(itemName.getSiblings().getFirst().getStyle().isItalic(), "Item name should have italic=false by default!");

        Component customItalic = TakashaColorParser.parseItemName("&6&oSakura Katana");
        assertTrue(customItalic.getSiblings().getFirst().getStyle().isItalic(), "Explicit &o must preserve italic=true!");
    }

    void testComplexMultiSegment() {
        String input = "&6&lLegendary &cFire &#00FF00Poison &7Blade";
        Component comp = TakashaColorParser.parseItemName(input);
        assertEquals("Legendary Fire Poison Blade", comp.getString());
        assertEquals(4, comp.getSiblings().size());

        // Segment 1: Legendary (Gold + Bold)
        assertEquals(TextColor.fromRgb(0xFFAA00), comp.getSiblings().get(0).getStyle().getColor());
        assertTrue(comp.getSiblings().get(0).getStyle().isBold());

        // Segment 2: Fire (Red + Reset bold)
        assertEquals(TextColor.fromRgb(0xFF5555), comp.getSiblings().get(1).getStyle().getColor());
        assertFalse(comp.getSiblings().get(1).getStyle().isBold());

        // Segment 3: Poison (Hex 0x00FF00)
        assertEquals(TextColor.fromRgb(0x00FF00), comp.getSiblings().get(2).getStyle().getColor());

        // Segment 4: Blade (Gray)
        assertEquals(TextColor.fromRgb(0xAAAAAA), comp.getSiblings().get(3).getStyle().getColor());
    }

    void testNullAndEmptySafety() {
        Component cNull = TakashaColorParser.parse(null);
        assertNotNull(cNull);
        assertEquals("", cNull.getString());

        Component cEmpty = TakashaColorParser.parse("");
        assertNotNull(cEmpty);
        assertEquals("", cEmpty.getString());
    }
}
