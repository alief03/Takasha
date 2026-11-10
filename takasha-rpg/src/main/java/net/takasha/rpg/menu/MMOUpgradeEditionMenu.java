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
import net.takasha.rpg.mmo.MMOStatType;

import java.util.List;

/**
 * MMOUpgradeEditionMenu
 * Antarmuka Manajemen Upgrade RPG Takasha.
 * Sub-menu GUI 27-slot untuk konfigurasi upgrade level tempa:
 * Max Upgrades, Upgrade Scaling, Reference Template.
 */
public class MMOUpgradeEditionMenu {

    public static void open(ServerPlayer player, MMOItemTemplate template, int parentPage) {
        SimpleContainer container = new SimpleContainer(27);

        // Slot 11: Max Upgrades
        ItemStack maxUpgrades = new ItemStack(Items.ANVIL);
        maxUpgrades.set(DataComponents.CUSTOM_NAME, Component.literal("§6Max Upgrades"));
        double maxLevel = template.getStats().getOrDefault(MMOStatType.REQUIRED_LEVEL, 0.0);
        maxUpgrades.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Current Value: " + (maxLevel > 0 ? ("§a" + (int) maxLevel) : "§a---")),
                Component.empty(),
                Component.literal("§e► Left click to change this value."),
                Component.literal("§e► Right click to remove this value.")
        )));
        container.setItem(11, maxUpgrades);

        // Slot 15: Upgrade Reference Template
        ItemStack refTmpl = new ItemStack(Items.PAPER);
        refTmpl.set(DataComponents.CUSTOM_NAME, Component.literal("§6Upgrade Template Reference"));
        refTmpl.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Template: §fDefault"),
                Component.empty(),
                Component.literal("§e► Left click to edit reference."),
                Component.literal("§e► Right click to reset.")
        )));
        container.setItem(15, refTmpl);

        // Slot 26: Back Button
        ItemStack back = new ItemStack(Items.BARRIER);
        back.set(DataComponents.CUSTOM_NAME, Component.literal("§cBack"));
        container.setItem(26, back);

        Component title = Component.literal("Upgrading: " + template.getId());
        ReadOnlyChestMenu.openOrUpdate(player, title, container, 3, (slotId, button, clickType, pl) -> {
            if (slotId == 26) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
                MMOItemEditionMenu.open(player, template.getItemType(), template.getId(), parentPage);
                return;
            }

            if (slotId == 11) {
                if (button == 1) { // Right Click: Remove
                    template.getStats().remove(MMOStatType.REQUIRED_LEVEL);
                    MMOItemDiskManager.saveTemplate(template);
                    player.sendSystemMessage(Component.literal("§8[§eTakasha-RPG§8] §7Max Upgrades successfully removed."));
                    open(player, template, parentPage);
                } else { // Left Click: Chat
                    List<String> promptLines = List.of(
                            "Write in the chat the numeric value you want.",
                            "Type 'cancel' to abort editing."
                    );
                    MMOChatEditionEngine.beginSession(player, "Upgrading", "Max Upgrades", promptLines,
                            () -> open(player, template, parentPage),
                            input -> {
                                double val = Double.parseDouble(input.trim());
                                template.getStats().put(MMOStatType.REQUIRED_LEVEL, val);
                                MMOItemDiskManager.saveTemplate(template);
                                player.sendSystemMessage(Component.literal("§8[§eTakasha-RPG§8] §7Max Upgrades successfully changed to " + (int) val));
                                open(player, template, parentPage);
                                return true;
                            });
                }
            }
        });
    }
}
