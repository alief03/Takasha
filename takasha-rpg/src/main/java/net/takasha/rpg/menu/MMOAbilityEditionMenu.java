package net.takasha.rpg.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.takasha.rpg.mmo.MMOAbilityTrigger;
import net.takasha.rpg.mmo.MMOItemManager;
import net.takasha.rpg.mmo.MMOItemTemplate;

import java.util.*;

/**
 * MMOAbilityEditionMenu
 * Takasha RPG Ability Edition GUI (54 slots).
 * Mengelola kemampuan aktif dan pasif (Trigger, Cooldown, Mana Cost) pada item template.
 */
public class MMOAbilityEditionMenu {

    private static final int[] GRID_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public static void open(ServerPlayer player, MMOItemTemplate template) {
        if (!player.isCreative()) {
            player.sendSystemMessage(Component.literal("§c[Takasha-RPG] Menu ini hanya dapat diakses dalam Mode Kreatif!"));
            return;
        }

        SimpleContainer container = new SimpleContainer(54);
        populateContainer(container, template);

        if (player.containerMenu instanceof ReadOnlyChestMenu currentMenu
                && currentMenu.getContainer() instanceof SimpleContainer curCont
                && curCont.getContainerSize() == 54) {
            populateContainer(curCont, template);
            currentMenu.setSlotClickAction((slotId, button, clickType, pl) -> handleClick(player, template, slotId, button));
            currentMenu.broadcastChanges();
            return;
        }

        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ReadOnlyChestMenu(id, inv, container, 6, (slotId, button, clickType, pl) ->
                        handleClick(player, template, slotId, button)),
                Component.literal("§8Item Edition > §6Abilities (" + template.getId() + ")")
        ));
    }

    private static void populateContainer(SimpleContainer container, MMOItemTemplate template) {
        ItemStack filler = new ItemStack(Items.STAINED_GLASS_PANE.gray());
        filler.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        for (int i = 0; i < 54; i++) {
            container.setItem(i, filler.copy());
        }

        // Slot 4: Live Item Preview
        ItemStack preview = MMOItemManager.generateItemStack(template.getItemType(), template.getId(), 1);
        container.setItem(4, preview);

        // Placeholders in grid
        ItemStack noAb = new ItemStack(Items.STAINED_GLASS_PANE.gray());
        noAb.set(DataComponents.CUSTOM_NAME, Component.literal("§7- No Ability -"));
        for (int slot : GRID_SLOTS) {
            container.setItem(slot, noAb.copy());
        }

        // Populate existing Abilities
        List<MMOItemTemplate.AbilityConfig> list = template.getAbilities();
        for (int i = 0; i < list.size() && i < GRID_SLOTS.length; i++) {
            MMOItemTemplate.AbilityConfig ab = list.get(i);

            ItemStack rod = new ItemStack(Items.BLAZE_ROD);
            rod.set(DataComponents.CUSTOM_NAME, Component.literal("§6§l" + ab.getType().replace('_', ' ')));
            rod.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§7Trigger Mode: §e[" + ab.getMode().getDisplay() + "]"),
                    Component.literal("§7Cooldown: §f" + ab.getCooldown() + "s"),
                    Component.literal("§7Mana Cost: §b" + (int) ab.getManaCost()),
                    Component.literal("§7Damage: §c" + ab.getDamage()),
                    Component.empty(),
                    Component.literal("§c► Right click to remove this ability")
            )));
            container.setItem(GRID_SLOTS[i], rod);
        }

        // Slot 49: Back to Item Edition
        ItemStack back = new ItemStack(Items.BARRIER);
        back.set(DataComponents.CUSTOM_NAME, Component.literal("§c◄ Back"));
        container.setItem(49, back);

        // Slot 51: Add New Ability (Nether Star)
        ItemStack addBtn = new ItemStack(Items.NETHER_STAR);
        addBtn.set(DataComponents.CUSTOM_NAME, Component.literal("§a§lAdd Ability"));
        addBtn.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Click to attach an ability to this item."),
                Component.empty(),
                Component.literal("§e► Click to input via chat (e.g. 'FIRE_BURST RIGHT_CLICK 5 10')")
        )));
        container.setItem(51, addBtn);
    }

    private static void handleClick(ServerPlayer player, MMOItemTemplate template, int slotId, int button) {
        // Slot 49: Back
        if (slotId == 49) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
            MMOItemEditionMenu.open(player, template.getItemType(), template.getId());
            return;
        }

        // Slot 51: Add Ability via Chat
        if (slotId == 51) {
            player.closeContainer();
            MMOChatEditionEngine.beginSession(
                    player,
                    "Add Ability",
                    "TYPE TRIGGER COOLDOWN MANA",
                    "Write in the chat the Ability configuration (e.g. 'FIRE_BURST RIGHT_CLICK 5 10' or 'LIGHTNING_STRIKE LEFT_CLICK 3 15').",
                    () -> open(player, template),
                    input -> {
                        String[] parts = input.trim().split("\\s+");
                        if (parts.length == 0 || parts[0].isEmpty()) {
                            open(player, template);
                            return;
                        }

                        String abType = parts[0].toUpperCase(Locale.ROOT);
                        MMOAbilityTrigger trigger = MMOAbilityTrigger.RIGHT_CLICK;
                        if (parts.length > 1) {
                            trigger = MMOAbilityTrigger.fromString(parts[1]);
                        }
                        double cd = 0.0;
                        if (parts.length > 2) {
                            try { cd = Double.parseDouble(parts[2]); } catch (NumberFormatException ignored) {}
                        }
                        double mana = 0.0;
                        if (parts.length > 3) {
                            try { mana = Double.parseDouble(parts[3]); } catch (NumberFormatException ignored) {}
                        }

                        MMOItemTemplate.AbilityConfig newAb = new MMOItemTemplate.AbilityConfig(
                                abType.toLowerCase(Locale.ROOT), abType, trigger, cd, cd, 0.0, 0.0, 0.0, 0.0, mana
                        );
                        template.getAbilities().add(newAb);
                        net.takasha.rpg.mmo.MMOItemDiskManager.saveTemplate(template);

                        player.sendSystemMessage(Component.literal("§a[Takasha-RPG] Ability §6" + abType + " §7[" + trigger.getDisplay() + "] §aberhasil ditambahkan ke template!"));
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
                        open(player, template);
                    }
            );
            return;
        }

        // Existing abilities in grid
        List<MMOItemTemplate.AbilityConfig> list = template.getAbilities();
        for (int i = 0; i < list.size() && i < GRID_SLOTS.length; i++) {
            if (GRID_SLOTS[i] == slotId) {
                if (button == 1) { // Right Click: Remove
                    MMOItemTemplate.AbilityConfig removed = list.remove(i);
                    net.takasha.rpg.mmo.MMOItemDiskManager.saveTemplate(template);
                    player.sendSystemMessage(Component.literal("§c[Takasha-RPG] Ability §e" + removed.getType() + " §ctelah dihapus dari template."));
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 0.7f, 1.2f);
                    open(player, template);
                    return;
                }
            }
        }
    }
}

