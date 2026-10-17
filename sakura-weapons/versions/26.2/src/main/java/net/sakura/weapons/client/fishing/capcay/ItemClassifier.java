package net.sakura.weapons.client.fishing.capcay;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShovelItem;

public final class ItemClassifier {

    private ItemClassifier() {}

    public static boolean isFiller(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;
        Item item = stack.getItem();
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return true;
        String path = id.getPath();
        return path.endsWith("_stained_glass_pane")
            || path.equals("glass_pane")
            || path.equals("barrier")
            || path.equals("air")
            || path.equals("structure_void");
    }

    public static ItemCategory classify(ItemStack stack) {
        if (isFiller(stack)) return ItemCategory.OTHER;

        Item item = stack.getItem();
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        String path = id != null ? id.getPath() : "";

        if (stack.is(ItemTags.SWORDS) || path.endsWith("_sword")) return ItemCategory.SWORD;
        if (stack.is(ItemTags.PICKAXES) || path.endsWith("_pickaxe")) return ItemCategory.PICKAXE;
        if (stack.is(ItemTags.AXES) || item instanceof AxeItem || path.endsWith("_axe")) return ItemCategory.AXE;
        if (item instanceof ShovelItem || path.endsWith("_shovel")) return ItemCategory.SHOVEL;
        if (item instanceof HoeItem || path.endsWith("_hoe")) return ItemCategory.HOE;
        if (item instanceof BowItem || item instanceof CrossbowItem || path.equals("bow") || path.equals("crossbow")) return ItemCategory.BOW;

        if (stack.is(ItemTags.HEAD_ARMOR) || path.endsWith("_helmet")) return ItemCategory.HELMET;
        if (stack.is(ItemTags.CHEST_ARMOR) || path.endsWith("_chestplate")) return ItemCategory.CHESTPLATE;
        if (stack.is(ItemTags.LEG_ARMOR) || path.endsWith("_leggings")) return ItemCategory.LEGGINGS;
        if (stack.is(ItemTags.FOOT_ARMOR) || path.endsWith("_boots")) return ItemCategory.BOOTS;

        if (path.startsWith("music_disc_") || path.contains("disc") || path.equals("pigstep")) return ItemCategory.MUSIC_DISC;
        if (item instanceof PotionItem || path.contains("potion")) return ItemCategory.POTION;

        if (path.endsWith("_food") || path.contains("cooked_") || path.contains("raw_")
            || path.equals("apple") || path.equals("bread") || path.equals("cookie") || path.equals("carrot")) {
            return ItemCategory.FOOD;
        }

        if (path.endsWith("_flower") || path.contains("tulip") || path.equals("dandelion") || path.equals("poppy")) {
            return ItemCategory.FLOWER;
        }

        if (path.endsWith("_dye") || path.equals("bone_meal") || path.equals("ink_sac")) {
            return ItemCategory.DYE;
        }

        if (path.endsWith("_ingot") || path.endsWith("_nugget") || path.equals("diamond") || path.equals("emerald")) {
            return ItemCategory.INGOT_OR_GEM;
        }

        if (path.endsWith("_ore")) return ItemCategory.ORE;
        if (path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_planks")) return ItemCategory.WOOD;
        if (path.contains("stone") || path.contains("cobblestone") || path.contains("granite") || path.contains("diorite")) return ItemCategory.STONE;

        return ItemCategory.OTHER;
    }
}
