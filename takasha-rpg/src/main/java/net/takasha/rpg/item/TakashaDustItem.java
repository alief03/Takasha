package net.takasha.rpg.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.takasha.rpg.api.RarityTier;

import java.util.function.Consumer;

public class TakashaDustItem extends Item {

    public enum DustType {
        MAGIC_DUST("Magic Dust", "Meningkatkan persentase keberhasilan (Success Rate) buku enchant khusus"),
        MYSTERY_DUST("Mystery Dust", "Serbuk misterius yang dapat diidentifikasi menjadi Magic Dust berkualitas"),
        SLOT_INCREASER_1("Slot Increaser I", "Menambahkan kapasitas slot enchantment khusus pada item sebesar +1"),
        SLOT_INCREASER_2("Slot Increaser II", "Menambahkan kapasitas slot enchantment khusus pada item sebesar +2"),
        SLOT_INCREASER_3("Slot Increaser III", "Menambahkan kapasitas slot enchantment khusus pada item sebesar +3"),
        ITEM_RENAME_TAG("Item Rename Tag", "Memberikan nama kustom berwarna dan berkilau pada senjata atau armor");

        private final String title;
        private final String description;

        DustType(String title, String description) {
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
    private final DustType dustType;

    public TakashaDustItem(Properties properties, RarityTier tier, DustType dustType) {
        super(properties);
        this.tier = tier;
        this.dustType = dustType;
    }

    public RarityTier getTier() {
        return tier;
    }

    public DustType getDustType() {
        return dustType;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, consumer, flag);
        consumer.accept(tier.getBadge());
        consumer.accept(Component.literal(dustType.getDescription()).withStyle(ChatFormatting.GRAY));
        consumer.accept(Component.literal("§7Bahan alkimia & pandai besi tingkat tinggi").withStyle(ChatFormatting.DARK_GRAY));
    }
}
