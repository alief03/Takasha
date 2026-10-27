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

public class TakashaStatTrackerItem extends Item {

    public enum TrackerType {
        KILLS("Player Kills Tracker", "Melacak jumlah pemain yang telah dikalahkan dalam pertempuran PvP"),
        MOBS_KILLED("Mob Kills Tracker", "Melacak jumlah monster dan musuh liar yang telah dibasmi"),
        BLOCKS_MINED("Mining Tracker", "Melacak jumlah blok dan bijih tambang yang telah digali"),
        DAMAGE_DEALT("Damage Tracker", "Melacak total akumulasi damage serangan yang telah dilancarkan"),
        FISH_CAUGHT("Fishing Tracker", "Melacak total ikan dan harta karun perairan yang berhasil dipancing");

        private final String title;
        private final String description;

        TrackerType(String title, String description) {
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
    private final TrackerType trackerType;

    public TakashaStatTrackerItem(Properties properties, RarityTier tier, TrackerType trackerType) {
        super(properties);
        this.tier = tier;
        this.trackerType = trackerType;
    }

    public RarityTier getTier() {
        return tier;
    }

    public TrackerType getTrackerType() {
        return trackerType;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack self, Slot slot, ClickAction action, Player player) {
        if (!slot.allowModification(player)) return false;
        if (action != ClickAction.PRIMARY && action != ClickAction.SECONDARY) return false;

        ItemStack target = slot.getItem();
        if (target.isEmpty() || !target.isDamageableItem()) return false;

        if (processTrackerApplication(self, target, player)) {
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
        if (other.isEmpty() || !other.isDamageableItem()) return false;

        if (processTrackerApplication(self, other, player)) {
            if (!player.level().isClientSide()) {
                slot.setChanged();
                player.containerMenu.broadcastChanges();
            }
            return true;
        }
        return super.overrideOtherStackedOnMe(self, other, slot, action, player, access);
    }

    private boolean processTrackerApplication(ItemStack trackerStack, ItemStack gear, Player player) {
        String itemName = gear.getItem().toString().toUpperCase();
        boolean isValid = switch (trackerType) {
            case KILLS, MOBS_KILLED, DAMAGE_DEALT ->
                    itemName.contains("SWORD") || itemName.contains("AXE") || itemName.contains("KATANA") || itemName.contains("BOW") || itemName.contains("SCYTHE") || itemName.contains("DAGGER") || itemName.contains("MACE") || itemName.contains("TRIDENT");
            case BLOCKS_MINED ->
                    itemName.contains("PICKAXE") || itemName.contains("SHOVEL") || itemName.contains("AXE") || itemName.contains("HOE") || itemName.contains("SHEARS");
            case FISH_CAUGHT ->
                    itemName.contains("FISHING_ROD");
        };

        if (!isValid) {
            player.sendSystemMessage(Component.literal("§cStat Tracker ini tidak cocok untuk jenis item ini!"));
            return true;
        }

        if (TakashaEnchantManager.hasTracker(gear, trackerType)) {
            player.sendSystemMessage(Component.literal("§cItem ini sudah terpasang pelacak " + trackerType.getTitle() + "!"));
            return true;
        }

        if (!player.level().isClientSide()) {
            TakashaEnchantManager.applyTracker(gear, trackerType);
            TakashaAnvilHandler.refreshGearLore(gear);
            trackerStack.shrink(1);
            player.sendSystemMessage(Component.literal("§a§l[Stat Tracker] §fBerhasil memasang §e" + trackerType.getTitle() + " §fke perlengkapan!"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, consumer, flag);

        net.takasha.rpg.util.RpgTooltipBuilder.create(trackerType.getTitle(), tier)
                .category("STAT TRACKER")
                .description(trackerType.getDescription())
                .affordance("Terapkan", "Drag & Drop ke Senjata / Alat di inventory")
                .build(consumer);
    }
}
