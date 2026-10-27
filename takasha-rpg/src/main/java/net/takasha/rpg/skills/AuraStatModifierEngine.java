package net.takasha.rpg.skills;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.takasha.rpg.TakashaRpgMod;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;

/**
 * AuraStatModifierEngine
 * Mengaplikasikan peningkatan atribut fisik pemain berdasarkan level 11 cabang AuraSkills:
 * - MAX_HEALTH: +0.2 HP per level (Farming & Defense)
 * - ATTACK_DAMAGE: +0.05 Damage per level (Combat & Foraging)
 * - MOVEMENT_SPEED: +0.00015 Speed per level (Agility & Archery)
 * - ARMOR_TOUGHNESS: +0.02 Toughness per level (Defense & Mining)
 * - LUCK: +0.02 Luck per level (Fishing) & +0.01 (Mining)
 */
public class AuraStatModifierEngine {

    private static final Identifier HEALTH_MOD_ID = Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "auraskills_health");
    private static final Identifier DAMAGE_MOD_ID = Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "auraskills_damage");
    private static final Identifier SPEED_MOD_ID = Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "auraskills_speed");
    private static final Identifier TOUGHNESS_MOD_ID = Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "auraskills_toughness");
    private static final Identifier LUCK_MOD_ID = Identifier.fromNamespaceAndPath(TakashaRpgMod.MOD_ID, "auraskills_luck");

    public static void applyStats(ServerPlayer player) {
        if (player == null || player.isRemoved()) return;

        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        if (data == null) return;

        // 1. Max Health (+1.0 HP per Health stat point, scaled cleanly by AuraHealthScaling)
        double healthStat = data.getStatValue(AuraStatType.HEALTH);
        double healthBonus = healthStat * 1.0;
        updateModifier(player, Attributes.MAX_HEALTH, HEALTH_MOD_ID, healthBonus);

        // 2. Attack Damage (+0.2 Dmg per Strength stat point)
        double strengthStat = data.getStatValue(AuraStatType.STRENGTH);
        double damageBonus = strengthStat * 0.2;
        updateModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_MOD_ID, damageBonus);

        // 3. Movement Speed (+0.0005 Speed per Speed stat point, where base is 0.1)
        double speedStat = data.getStatValue(AuraStatType.SPEED);
        double speedBonus = speedStat * 0.0005;
        updateModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MOD_ID, speedBonus);

        // 4. Armor Toughness (+0.1 Toughness per Toughness stat point)
        double toughnessStat = data.getStatValue(AuraStatType.TOUGHNESS);
        double toughnessBonus = toughnessStat * 0.1;
        updateModifier(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_MOD_ID, toughnessBonus);

        // 5. Luck (+0.1 Luck per Luck stat point)
        double luckStat = data.getStatValue(AuraStatType.LUCK);
        double luckBonus = luckStat * 0.1;
        updateModifier(player, Attributes.LUCK, LUCK_MOD_ID, luckBonus);
    }

    private static void updateModifier(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, Identifier id, double amount) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;

        inst.removeModifier(id);
        if (amount > 0.0001) {
            inst.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
