package net.takasha.rpg.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.takasha.rpg.mmo.*;

import java.util.ArrayList;
import java.util.List;

/**
 * MMOElementsEditionMenu
 * 1:1 Parity dengan Spigot net.Indyuce.mmoitems.gui.edition.ElementsEdition.class.
 * Sub-menu GUI 27-slot yang mengelola 6 elemen RPG:
 * Fire, Ice, Lightning, Earth, Light, Darkness (Damage % & Defense %).
 */
public class MMOElementsEditionMenu {

    public record ElementEntry(
            String name,
            MMOStatType damageStat,
            MMOStatType defenseStat,
            Item damageIcon,
            Item defenseIcon,
            int damageSlot,
            int defenseSlot
    ) {}

    private static final List<ElementEntry> ELEMENTS = List.of(
            new ElementEntry("Fire", MMOStatType.FIRE_DAMAGE, MMOStatType.FIRE_DEFENSE, Items.BLAZE_POWDER, Items.MAGMA_CREAM, 10, 19),
            new ElementEntry("Ice", MMOStatType.ICE_DAMAGE, MMOStatType.ICE_DEFENSE, Items.ICE, Items.PACKED_ICE, 11, 20),
            new ElementEntry("Lightning", MMOStatType.LIGHTNING_DAMAGE, MMOStatType.LIGHTNING_DEFENSE, Items.GLOWSTONE_DUST, Items.LIGHTNING_ROD.weathering().unaffected(), 12, 21),
            new ElementEntry("Earth", MMOStatType.EARTH_DAMAGE, MMOStatType.EARTH_DEFENSE, Items.DIRT, Items.MUD_BRICKS, 14, 23),
            new ElementEntry("Light", MMOStatType.LIGHT_DAMAGE, MMOStatType.LIGHT_DEFENSE, Items.SEA_LANTERN, Items.OCHRE_FROGLIGHT, 15, 24),
            new ElementEntry("Darkness", MMOStatType.DARKNESS_DAMAGE, MMOStatType.DARKNESS_DEFENSE, Items.WITHER_ROSE, Items.SCULK, 16, 25)
    );

    public static void open(ServerPlayer player, MMOItemTemplate template, int parentPage) {
        SimpleContainer container = new SimpleContainer(27);

        // Slot 4: Item Preview
        ItemStack previewStack = MMOItemManager.generateItemStack(template.getItemType(), template.getId(), 1);
        if (previewStack.isEmpty()) {
            previewStack = new ItemStack(Items.BARRIER);
            previewStack.set(DataComponents.CUSTOM_NAME, Component.literal("§cPreview Error"));
        }
        container.setItem(4, previewStack);

        // Slot 26: Back Button (1:1 with Spigot Back)
        ItemStack backButton = new ItemStack(Items.BARRIER);
        backButton.set(DataComponents.CUSTOM_NAME, Component.literal("§cBack"));
        backButton.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Click to return to the item edition.")
        )));
        container.setItem(26, backButton);

        // Populate Element Slots
        for (ElementEntry entry : ELEMENTS) {
            // Damage Icon
            ItemStack dmgItem = new ItemStack(entry.damageIcon());
            dmgItem.set(DataComponents.CUSTOM_NAME, Component.literal("§6" + entry.name() + " Damage"));
            double dmgVal = template.getStats().getOrDefault(entry.damageStat(), 0.0);
            String dmgStr = template.getStats().containsKey(entry.damageStat()) ? ("§a" + dmgVal + "%") : "§a---";
            dmgItem.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§7Current Value: " + dmgStr),
                    Component.empty(),
                    Component.literal("§e► Left click to change this value."),
                    Component.literal("§e► Right click to remove this value.")
            )));
            container.setItem(entry.damageSlot(), dmgItem);

            // Defense Icon
            ItemStack defItem = new ItemStack(entry.defenseIcon());
            defItem.set(DataComponents.CUSTOM_NAME, Component.literal("§9" + entry.name() + " Defense"));
            double defVal = template.getStats().getOrDefault(entry.defenseStat(), 0.0);
            String defStr = template.getStats().containsKey(entry.defenseStat()) ? ("§a" + defVal + "%") : "§a---";
            defItem.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§7Current Value: " + defStr),
                    Component.empty(),
                    Component.literal("§e► Left click to change this value."),
                    Component.literal("§e► Right click to remove this value.")
            )));
            container.setItem(entry.defenseSlot(), defItem);
        }

        Component title = Component.literal("Elements: " + template.getId());
        ReadOnlyChestMenu.openOrUpdate(player, title, container, 3, (slotId, button, clickType, pl) -> {
            if (slotId == 26) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
                MMOItemEditionMenu.open(player, template.getItemType(), template.getId(), parentPage);
                return;
            }

            for (ElementEntry entry : ELEMENTS) {
                if (slotId == entry.damageSlot()) {
                    handleElementAction(player, template, parentPage, entry.name() + " Damage", entry.damageStat(), button);
                    return;
                }
                if (slotId == entry.defenseSlot()) {
                    handleElementAction(player, template, parentPage, entry.name() + " Defense", entry.defenseStat(), button);
                    return;
                }
            }
        });
    }

    private static void handleElementAction(ServerPlayer player, MMOItemTemplate template, int parentPage,
                                            String statName, MMOStatType statType, int button) {
        if (button == 1) { // Right Click: Remove
            template.getStats().remove(statType);
            MMOItemDiskManager.saveTemplate(template);
            player.sendSystemMessage(Component.literal("§8[§eMMOItems§8] §7" + statName + " successfully removed."));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
            open(player, template, parentPage);
        } else { // Left Click: Chat Edition
            List<String> promptLines = List.of(
                    "Write in the chat the numeric value you want.",
                    "Second Format: {Base} {Scaling Value} {Spread} {Max Spread} {Hard Min} {Hard Max}",
                    "Third Format: {Min Value} -> {Max Value}",
                    "Type 'cancel' to abort editing."
            );
            MMOChatEditionEngine.beginSession(player, "Elements", statName, promptLines,
                    () -> open(player, template, parentPage),
                    input -> {
                        double parsed = parseNumeric(input.trim());
                        template.getStats().put(statType, parsed);
                        MMOItemDiskManager.saveTemplate(template);
                        player.sendSystemMessage(Component.literal("§8[§eMMOItems§8] §7" + statName + " successfully changed to {" + parsed + " - 0.0 - 0.0 - 0.0 - (0.0 -> 0.0)}"));
                        open(player, template, parentPage);
                        return true;
                    });
        }
    }

    private static double parseNumeric(String input) {
        if (input.contains("->")) {
            String[] parts = input.split("->");
            double min = Double.parseDouble(parts[0].trim());
            double max = Double.parseDouble(parts[1].trim());
            return (min + max) / 2.0;
        } else if (input.contains(" ")) {
            String[] parts = input.split("\\s+");
            return Double.parseDouble(parts[0].trim());
        } else {
            return Double.parseDouble(input.trim());
        }
    }
}
