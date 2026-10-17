package net.takasha.rpg.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.takasha.rpg.api.RarityTier;

import java.util.function.Consumer;

public class TakashaScrollItem extends Item {

    public enum ScrollType {
        WHITE("White Scroll", "Melindungi perlengkapan agar tidak hancur saat gagal ditempa enchant"),
        BLACK("Black Scroll", "Mencabut enchantment khusus dari senjata/armor dan mengembalikannya ke buku"),
        TRANSMUTATION("Transmutation Scroll", "Mengacak kembali salah satu enchantment khusus pada item"),
        HOLY("Holy Scroll", "Melindungi seluruh isi inventaris dan perlengkapan agar tidak hilang saat mati (1x pakai)"),
        RANDOMIZATION("Randomization Scroll", "Mengacak ulang persentase tingkat keberhasilan (Success Rate) pada buku enchantment");

        private final String title;
        private final String functionDesc;

        ScrollType(String title, String functionDesc) {
            this.title = title;
            this.functionDesc = functionDesc;
        }

        public String getTitle() {
            return title;
        }

        public String getFunctionDesc() {
            return functionDesc;
        }
    }

    private final RarityTier tier;
    private final ScrollType scrollType;

    public TakashaScrollItem(Properties properties, RarityTier tier, ScrollType scrollType) {
        super(properties);
        this.tier = tier;
        this.scrollType = scrollType;
    }

    public RarityTier getTier() {
        return tier;
    }

    public ScrollType getScrollType() {
        return scrollType;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, consumer, flag);
        consumer.accept(tier.getBadge());
        consumer.accept(Component.literal(scrollType.getFunctionDesc()).withStyle(ChatFormatting.GRAY));
        consumer.accept(Component.literal("§7Gunakan di Anvil atau drag-and-drop pada perlengkapan").withStyle(ChatFormatting.DARK_GRAY));
    }
}
