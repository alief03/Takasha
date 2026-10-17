package net.takasha.rpg.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.takasha.rpg.api.RarityTier;

import java.util.function.Consumer;

public class TakashaStatTrackerItem extends Item {

    public enum TrackerType {
        KILLS("Kills Tracker", "Melacak jumlah mob dan monster yang telah dibasmi"),
        BLOCKS_MINED("Mining Tracker", "Melacak jumlah blok dan bijih tambang yang telah digali"),
        DAMAGE_DEALT("Damage Tracker", "Melacak total akumulasi damage serangan yang telah dilancarkan");

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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, consumer, flag);
        consumer.accept(tier.getBadge());
        consumer.accept(Component.literal(trackerType.getDescription()).withStyle(ChatFormatting.GRAY));
        consumer.accept(Component.literal("§6Stat Tracker: Menambahkan penghitung permanen ke statistik item").withStyle(ChatFormatting.YELLOW));
    }
}
