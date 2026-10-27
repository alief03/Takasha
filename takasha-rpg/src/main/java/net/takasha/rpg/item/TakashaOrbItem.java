package net.takasha.rpg.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.takasha.rpg.api.RarityTier;
import net.takasha.rpg.enchant.TakashaAnvilHandler;
import net.takasha.rpg.enchant.TakashaEnchantManager;

import java.util.function.Consumer;

/**
 * TakashaOrbItem
 * Orbs & Soul Gem autentik AdvancedEnchantments untuk meningkatkan kapasitas slot gear.
 */
public class TakashaOrbItem extends Item {

    public enum OrbType {
        WEAPON_ORB("Weapon Enchantment Orb", "Meningkatkan batas kapasitas slot custom enchant pada Senjata (+1 Slot, Maks 20)"),
        ARMOR_ORB("Armor Enchantment Orb", "Meningkatkan batas kapasitas slot custom enchant pada Armor (+1 Slot, Maks 20)"),
        TOOL_ORB("Tool Enchantment Orb", "Meningkatkan batas kapasitas slot custom enchant pada Alat (+1 Slot, Maks 20)"),
        SOUL_GEM("Soul Gem", "Batu kristal jiwa murni. Digunakan untuk mengisi daya sihir jiwa atau menambah batas Soul Slot (+1 Slot, Maks 5)");

        private final String title;
        private final String description;

        OrbType(String title, String description) {
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
    private final OrbType orbType;

    public TakashaOrbItem(Properties properties, RarityTier tier, OrbType orbType) {
        super(properties);
        this.tier = tier;
        this.orbType = orbType;
    }

    public RarityTier getTier() {
        return tier;
    }

    public OrbType getOrbType() {
        return orbType;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack self, Slot slot, ClickAction action, Player player) {
        if (!slot.allowModification(player)) return false;
        if (action != ClickAction.PRIMARY && action != ClickAction.SECONDARY) return false;

        ItemStack target = slot.getItem();
        if (target.isEmpty() || (!target.isDamageableItem() && !(target.getItem() instanceof TakashaCustomWeaponItem) && !(target.getItem() instanceof TakashaArmorSetItem))) return false;

        if (processOrbApplication(self, target, player)) {
            if (!player.level().isClientSide()) {
                slot.setChanged();
                player.containerMenu.broadcastChanges();
            }
            return true;
        }
        return super.overrideStackedOnOther(self, slot, action, player);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (!slot.allowModification(player)) return false;
        if (action != ClickAction.PRIMARY && action != ClickAction.SECONDARY) return false;
        if (other.isEmpty() || (!other.isDamageableItem() && !(other.getItem() instanceof TakashaCustomWeaponItem) && !(other.getItem() instanceof TakashaArmorSetItem))) return false;

        if (processOrbApplication(self, other, player)) {
            if (!player.level().isClientSide()) {
                slot.setChanged();
                player.containerMenu.broadcastChanges();
            }
            return true;
        }
        return super.overrideOtherStackedOnMe(self, other, slot, action, player, access);
    }

    private boolean processOrbApplication(ItemStack orb, ItemStack target, Player player) {
        String itemName = target.getItem().toString().toUpperCase();

        if (orbType == OrbType.WEAPON_ORB) {
            boolean isWeapon = itemName.contains("SWORD") || itemName.contains("AXE") || itemName.contains("KATANA") || itemName.contains("BOW") || itemName.contains("SCYTHE") || itemName.contains("DAGGER") || itemName.contains("MACE") || itemName.contains("TRIDENT") || target.getItem() instanceof TakashaCustomWeaponItem;
            if (!isWeapon) {
                player.sendSystemMessage(Component.literal("§cOrb ini khusus untuk senjata!"));
                return true;
            }
            return applySlotIncrease(orb, target, player, TakashaEnchantManager.GRAND_HARD_CAP_SLOTS, false);
        } else if (orbType == OrbType.ARMOR_ORB) {
            boolean isArmor = itemName.contains("HELMET") || itemName.contains("CHESTPLATE") || itemName.contains("LEGGINGS") || itemName.contains("BOOTS") || itemName.contains("ELYTRA") || target.getItem() instanceof TakashaArmorSetItem;
            if (!isArmor) {
                player.sendSystemMessage(Component.literal("§cOrb ini khusus untuk armor!"));
                return true;
            }
            return applySlotIncrease(orb, target, player, TakashaEnchantManager.GRAND_HARD_CAP_SLOTS, false);
        } else if (orbType == OrbType.TOOL_ORB) {
            boolean isTool = itemName.contains("PICKAXE") || itemName.contains("SHOVEL") || itemName.contains("HOE") || itemName.contains("FISHING_ROD") || itemName.contains("SHEARS");
            if (!isTool) {
                player.sendSystemMessage(Component.literal("§cOrb ini khusus untuk alat perkakas!"));
                return true;
            }
            return applySlotIncrease(orb, target, player, TakashaEnchantManager.GRAND_HARD_CAP_SLOTS, false);
        } else if (orbType == OrbType.SOUL_GEM) {
            int curSoul = TakashaEnchantManager.getMaxSoulSlots(target);
            if (curSoul >= TakashaEnchantManager.MAX_SOUL_SLOTS) {
                player.sendSystemMessage(Component.literal("§cItem ini sudah mencapai batas maksimal Soul Slot (" + TakashaEnchantManager.MAX_SOUL_SLOTS + " slot)!"));
                return true;
            }
            if (!player.level().isClientSide()) {
                TakashaEnchantManager.applySlotIncreaser(target, 3);
                TakashaAnvilHandler.refreshGearLore(target);
                orb.shrink(1);
                player.sendSystemMessage(Component.literal("§3§l[Soul Gem] §fKapasitas Soul Slot bertambah menjadi §b" + (curSoul + 1) + " Slot§f!"));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.5f);
            }
            return true;
        }
        return false;
    }

    private boolean applySlotIncrease(ItemStack orb, ItemStack target, Player player, int hardCap, boolean isSoul) {
        int currentMax = isSoul ? TakashaEnchantManager.getMaxSoulSlots(target) : TakashaEnchantManager.getMaxSlots(target);
        if (currentMax >= hardCap) {
            player.sendSystemMessage(Component.literal("§cItem ini sudah mencapai Grand Hard Cap (" + hardCap + " Slot)!"));
            return true;
        }

        if (!player.level().isClientSide()) {
            int incTier = currentMax < TakashaEnchantManager.REGULAR_CAP_SLOTS ? 1 : 2;
            TakashaEnchantManager.applySlotIncreaser(target, incTier);
            TakashaAnvilHandler.refreshGearLore(target);
            orb.shrink(1);
            player.sendSystemMessage(Component.literal("§a§l[Enchantment Orb] §fBatas slot berhasil diperluas menjadi §e" + (currentMax + 1) + " Slot§f!"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.3f);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, consumer, flag);

        net.takasha.rpg.util.RpgTooltipBuilder.create(orbType.getTitle(), tier)
                .category("ENCHANT ORB")
                .description(orbType.getDescription())
                .affordance("Terapkan", "Drag & Drop ke Perlengkapan di inventory")
                .build(consumer);
    }
}
