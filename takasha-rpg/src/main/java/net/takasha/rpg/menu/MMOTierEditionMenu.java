package net.takasha.rpg.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.takasha.rpg.mmo.MMOItemDiskManager;
import net.takasha.rpg.mmo.MMOItemTemplate;
import net.takasha.rpg.mmo.MMOItemTier;

import java.util.List;

/**
 * MMOTierEditionMenu
 * Sub-menu GUI 27-slot visual untuk memilih Rarity Tier item:
 * COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC.
 */
public class MMOTierEditionMenu {

    public static void open(ServerPlayer player, MMOItemTemplate template, int parentPage) {
        SimpleContainer container = new SimpleContainer(27);

        // Slot 4: Current Tier Display
        ItemStack cur = new ItemStack(Items.DIAMOND);
        String tierName = template.hasTier() ? (template.getTier().getPrefixColor() + template.getTier().getDisplayName()) : "§cNone";
        cur.set(DataComponents.CUSTOM_NAME, Component.literal("§6Current Tier: " + tierName));
        container.setItem(4, cur);

        // Slot 10: Common
        container.setItem(10, makeTierCard(MMOItemTier.COMMON, Items.COAL, "§fCommon"));
        // Slot 11: Uncommon
        container.setItem(11, makeTierCard(MMOItemTier.UNCOMMON, Items.IRON_INGOT, "§aUncommon"));
        // Slot 12: Rare
        container.setItem(12, makeTierCard(MMOItemTier.RARE, Items.LAPIS_LAZULI, "§9Rare"));
        // Slot 14: Epic
        container.setItem(14, makeTierCard(MMOItemTier.EPIC, Items.AMETHYST_SHARD, "§5Epic"));
        // Slot 15: Legendary
        container.setItem(15, makeTierCard(MMOItemTier.LEGENDARY, Items.GOLD_INGOT, "§6Legendary"));
        // Slot 16: Mythic
        container.setItem(16, makeTierCard(MMOItemTier.MYTHIC, Items.NETHERITE_INGOT, "§dMythic"));

        // Slot 22: Clear Tier
        ItemStack clear = new ItemStack(Items.CAULDRON);
        clear.set(DataComponents.CUSTOM_NAME, Component.literal("§c§l[Hapus Tier]"));
        clear.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Klik untuk menghapus tier dari item."),
                Component.literal("§c► Set ke None")
        )));
        container.setItem(22, clear);

        // Slot 26: Back Button
        ItemStack back = new ItemStack(Items.BARRIER);
        back.set(DataComponents.CUSTOM_NAME, Component.literal("§cBack"));
        container.setItem(26, back);

        Component title = Component.literal("Item Tier: " + template.getId());
        ReadOnlyChestMenu.openOrUpdate(player, title, container, 3, (slotId, button, clickType, pl) -> {
            if (slotId == 26) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
                MMOItemEditionMenu.open(player, template.getItemType(), template.getId(), parentPage);
                return;
            }
            if (slotId == 22) {
                template.setTier(null);
                MMOItemDiskManager.saveTemplate(template);
                player.sendSystemMessage(Component.literal("§8[§eMMOItems§8] §7Successfully removed Item Tier§7."));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
                MMOItemEditionMenu.open(player, template.getItemType(), template.getId(), parentPage);
                return;
            }

            MMOItemTier chosen = switch (slotId) {
                case 10 -> MMOItemTier.COMMON;
                case 11 -> MMOItemTier.UNCOMMON;
                case 12 -> MMOItemTier.RARE;
                case 14 -> MMOItemTier.EPIC;
                case 15 -> MMOItemTier.LEGENDARY;
                case 16 -> MMOItemTier.MYTHIC;
                default -> null;
            };

            if (chosen != null) {
                template.setTier(chosen);
                MMOItemDiskManager.saveTemplate(template);
                player.sendSystemMessage(Component.literal("§8[§eMMOItems§8] §7Tier successfully changed to " + chosen.name() + "."));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.2f);
                MMOItemEditionMenu.open(player, template.getItemType(), template.getId(), parentPage);
            }
        });
    }

    private static ItemStack makeTierCard(MMOItemTier tier, net.minecraft.world.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Rarity: " + tier.getPrefixColor() + tier.getDisplayName()),
                Component.literal("§e► Klik untuk memilih tier ini.")
        )));
        return stack;
    }
}
