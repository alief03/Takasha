package net.sakura.touhou.compat.rpg;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.sakura.touhou.entity.EntityMaid;
import net.takasha.rpg.data.TakashaRpgData;
import net.takasha.rpg.data.TakashaRpgManager;
import net.takasha.rpg.enchant.ArmorSetBonusHandler;
import net.takasha.rpg.skills.AuraSkillType;
import net.takasha.rpg.skills.AuraStatType;

/**
 * MaidRpgBondingBridge
 * Jembatan sinkronisasi atribut dan ikatan antara Pemain (Owner) dan EntityMaid.
 * Menyinkronkan 7 Atribut AuraSkills dan memberikan Resonance Aura dari 7 Legendary Armor Sets Takasha-RPG.
 */
public class MaidRpgBondingBridge {

    private static final Identifier HEALTH_MOD = Identifier.fromNamespaceAndPath("touhou_little_maid", "owner_health");
    private static final Identifier DAMAGE_MOD = Identifier.fromNamespaceAndPath("touhou_little_maid", "owner_strength");
    private static final Identifier SPEED_MOD = Identifier.fromNamespaceAndPath("touhou_little_maid", "owner_speed");
    private static final Identifier TOUGH_MOD = Identifier.fromNamespaceAndPath("touhou_little_maid", "owner_toughness");
    private static final Identifier LUCK_MOD = Identifier.fromNamespaceAndPath("touhou_little_maid", "owner_luck");

    public static boolean isRpgActive() {
        return FabricLoader.getInstance().isModLoaded("takasha_rpg");
    }

    /**
     * Menyinkronkan status atribut pemain ke maid secara berkala (tiap 40 tick).
     */
    public static void syncOwnerStats(EntityMaid maid) {
        if (!isRpgActive()) return;
        if (maid == null || maid.level().isClientSide()) return;
        if (!(maid.getOwner() instanceof ServerPlayer player)) return;

        TakashaRpgData data = TakashaRpgManager.getOrCreateData(player);
        if (data == null) return;

        // 1. Max Health (+1.0 HP per stat Health)
        double healthBonus = data.getStatValue(AuraStatType.HEALTH) * 1.0;
        updateModifier(maid, Attributes.MAX_HEALTH, HEALTH_MOD, healthBonus);

        // 2. Attack Damage (+0.2 Dmg per stat Strength)
        double dmgBonus = data.getStatValue(AuraStatType.STRENGTH) * 0.2;
        updateModifier(maid, Attributes.ATTACK_DAMAGE, DAMAGE_MOD, dmgBonus);

        // 3. Movement Speed (+0.0003 Speed per stat Speed)
        double spdBonus = data.getStatValue(AuraStatType.SPEED) * 0.0003;
        updateModifier(maid, Attributes.MOVEMENT_SPEED, SPEED_MOD, spdBonus);

        // 4. Armor Toughness (+0.1 Toughness per stat Toughness)
        double toughBonus = data.getStatValue(AuraStatType.TOUGHNESS) * 0.1;
        updateModifier(maid, Attributes.ARMOR_TOUGHNESS, TOUGH_MOD, toughBonus);

        // 5. Luck (+0.1 Luck per stat Luck untuk panen ganda dan mancing)
        double luckBonus = data.getStatValue(AuraStatType.LUCK) * 0.1;
        updateModifier(maid, Attributes.LUCK, LUCK_MOD, luckBonus);

        // 6. Set Resonance Aura Detection
        String activeSet = ArmorSetBonusHandler.getEquippedSet(player);
        maid.setActiveSetResonance(activeSet);
    }

    /**
     * Mentransfer 20% EXP perolehan maid ke pool skill pemain pemilik (Wisdom Scaling).
     */
    public static void shareWisdomExp(EntityMaid maid, AuraSkillType skill, double xp) {
        if (!isRpgActive() || maid == null || skill == null || xp <= 0) return;
        if (!(maid.getOwner() instanceof ServerPlayer player)) return;

        double shareAmount = xp * 0.20;
        TakashaRpgManager.giveSkillXp(player, skill, shareAmount);
    }

    private static void updateModifier(EntityMaid maid, Holder<Attribute> attr, Identifier id, double value) {
        AttributeInstance inst = maid.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (value > 0) {
            inst.addPermanentModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
