package net.takasha.rpg.client.hud;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * AuraHealthScaling
 * Authentic AuraSkills health scaling algorithm for client-side HUD.
 * Prevents high Defense/Health levels (e.g. 1000 Defense = +200 HP = 220 Max HP)
 * from stacking 11+ rows of hearts that obscure the screen and collide with the HUD.
 * Compresses visual health rendering to a maximum of 2 rows of hearts (40 visual HP),
 * while exact numerical HP and Mana remain clearly visible on the Action Bar HUD.
 */
public class AuraHealthScaling {

    public static final float MAX_VISUAL_HP = 40.0f; // Maksimal 2 baris hati (20 hati)
    private static boolean enabled = true;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static double getVisualMaxHealth(Player player) {
        if (player == null) return 20.0;
        double realMax = player.getAttributeValue(Attributes.MAX_HEALTH);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || !mc.hasSingleplayerServer() || !enabled || realMax <= MAX_VISUAL_HP) {
            return realMax;
        }
        return MAX_VISUAL_HP;
    }

    public static float getVisualHealth(Player player) {
        if (player == null) return 20.0f;
        float realHp = player.getHealth();
        float realMax = (float) player.getAttributeValue(Attributes.MAX_HEALTH);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || !mc.hasSingleplayerServer() || !enabled || realMax <= MAX_VISUAL_HP) {
            return realHp;
        }
        if (realHp <= 0.0f) return 0.0f;
        float scaled = (realHp / realMax) * MAX_VISUAL_HP;
        // Jamin minimal 1 HP visual (setengah hati) jika pemain masih bernyawa
        return Math.max(1.0f, scaled);
    }

    public static float getVisualAbsorption(Player player) {
        if (player == null) return 0.0f;
        float realAbs = player.getAbsorptionAmount();
        if (realAbs <= 0.0f) return 0.0f;
        float realMax = (float) player.getAttributeValue(Attributes.MAX_HEALTH);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || !mc.hasSingleplayerServer() || !enabled || realMax <= MAX_VISUAL_HP) {
            return realAbs;
        }
        return (realAbs / realMax) * MAX_VISUAL_HP;
    }
}
