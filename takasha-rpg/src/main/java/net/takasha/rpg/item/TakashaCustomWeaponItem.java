package net.takasha.rpg.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.takasha.rpg.api.RarityTier;
import net.takasha.rpg.util.RpgTooltipBuilder;

import java.util.function.Consumer;

/**
 * TakashaCustomWeaponItem
 * Senjata Legendaris & Mythic khusus autentik AdvancedEnchantments & CosmicPvP.
 */
public class TakashaCustomWeaponItem extends Item {

    public enum WeaponType {
        DIMENSIONAL_TRAVELLER("Dimensional Traveller Sword", "Pedang penembus dimensi kosmik. Memberikan +30% Attack Damage dan efek pasif Dimensional Shift saat bertarung."),
        KOTH_SWORD("King of the Hill Sword", "Pedang pusaka raja penguasa bukit pertempuran. Memberikan +20% PvP Damage, +50% PvE Damage, dan berkat suci Auto Bless."),
        KOTH_AXE("King of the Hill Battleaxe", "Kapak perang penghancur zirah penguasa bukit. Memberikan +20% PvP Damage dan kemampuan sapuan Cleave AoE."),
        PHANTOM_SCYTHE("Phantom Reaper Scythe", "Sabit pencabut nyawa bayangan malam. Memberikan +25% Damage ke seluruh musuh dan kecepatan melayang Phased Shadow."),
        SUPREME_WEAPON("Supreme Vanguard Dagger", "Belati kehormatan perwira tertinggi. Memberikan +15% Damage, +200% Aura Kemuliaan, serta membebaskan player dari lapar dan luka jatuh.");

        private final String title;
        private final String description;

        WeaponType(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }
    }

    private final RarityTier tier;
    private final WeaponType weaponType;

    public TakashaCustomWeaponItem(Properties properties, RarityTier tier, WeaponType weaponType) {
        super(properties);
        this.tier = tier;
        this.weaponType = weaponType;
    }

    public RarityTier getTier() {
        return tier;
    }

    public WeaponType getWeaponType() {
        return weaponType;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, consumer, flag);

        RpgTooltipBuilder.create(weaponType.getTitle(), tier)
                .category("LEGENDARY WEAPON")
                .description(weaponType.getDescription())
                .affordance("Efek Tempur", "Dilengkapi slot enchant master dan kompatibel penuh dengan Custom Enchants")
                .build(consumer);
    }
}
