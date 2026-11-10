package net.takasha.rpg.mmo;

import net.minecraft.network.chat.Component;

/**
 * Bridge for backward compatibility with MiniMessageParser, delegating to TakashaColorParser.
 */
public class MiniMessageParser {

    public static Component parse(String input) {
        return TakashaColorParser.parse(input, false);
    }

    public static Component parse(String input, boolean defaultNonItalic) {
        return TakashaColorParser.parse(input, defaultNonItalic);
    }
}
