package net.takasha.rpg.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.takasha.rpg.dungeon.DungeonTier;

import java.util.List;

/**
 * DungeonTierSelectorMenu
 * Antarmuka GUI 27-slot tahap pertama saat menjalankan /dungeon rewards.
 * Menampilkan 3 pilihan tingkat kesulitan dungeon utama:
 * 1. Common Dungeon (1x Multiplier)
 * 2. Nightmare Dungeon (2x Multiplier)
 * 3. Sakura Boss Dungeon (4x Multiplier)
 */
public class DungeonTierSelectorMenu {

    public static void open(ServerPlayer player) {
        SimpleContainer container = new SimpleContainer(27);

        // Slot 11: Common Dungeon
        ItemStack commonCard = new ItemStack(Items.EMERALD_BLOCK);
        commonCard.set(DataComponents.CUSTOM_NAME, Component.literal("§a§l⚔ COMMON DUNGEON"));
        commonCard.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Tingkat Kesulitan: §aMudah / Normal"),
                Component.literal("§7Multiplier Koin: §e1x"),
                Component.literal("§7Drop Pool: §f5 Wave Hadiah"),
                Component.empty(),
                Component.literal("§e► Klik Kiri untuk mengelola hadiah Common.")
        )));
        container.setItem(11, commonCard);

        // Slot 13: Nightmare Dungeon
        ItemStack nightmareCard = new ItemStack(Items.CRYING_OBSIDIAN);
        nightmareCard.set(DataComponents.CUSTOM_NAME, Component.literal("§5§l☠ NIGHTMARE DUNGEON"));
        nightmareCard.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Tingkat Kesulitan: §5Sulit / Ekstrem"),
                Component.literal("§7Multiplier Koin: §e2x"),
                Component.literal("§7Drop Pool: §f5 Wave Hadiah"),
                Component.empty(),
                Component.literal("§e► Klik Kiri untuk mengelola hadiah Nightmare.")
        )));
        container.setItem(13, nightmareCard);

        // Slot 15: Sakura Boss Dungeon
        ItemStack sakuraCard = new ItemStack(Items.CHERRY_SAPLING);
        sakuraCard.set(DataComponents.CUSTOM_NAME, Component.literal("§d§l🌸 SAKURA BOSS DUNGEON"));
        sakuraCard.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Tingkat Kesulitan: §dLegendaris / Endgame"),
                Component.literal("§7Multiplier Koin: §e4x"),
                Component.literal("§7Drop Pool: §f5 Wave Hadiah (Termasuk Boss Wave)"),
                Component.empty(),
                Component.literal("§e► Klik Kiri untuk mengelola hadiah Sakura Boss.")
        )));
        container.setItem(15, sakuraCard);

        // Slot 22: Tutup Menu
        ItemStack closeBtn = new ItemStack(Items.BARRIER);
        closeBtn.set(DataComponents.CUSTOM_NAME, Component.literal("§cTutup Menu"));
        container.setItem(22, closeBtn);

        ReadOnlyChestMenu.openOrUpdate(player, Component.literal("Pilih Tingkat Hadiah Dungeon"), container, 3, (slotId, button, clickType, pl) -> {
            if (slotId == 11) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                DungeonRewardManagerMenu.open(player, DungeonTier.COMMON, 1);
            } else if (slotId == 13) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                DungeonRewardManagerMenu.open(player, DungeonTier.NIGHTMARE, 1);
            } else if (slotId == 15) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                DungeonRewardManagerMenu.open(player, DungeonTier.SAKURA_BOSS, 1);
            } else if (slotId == 22) {
                player.closeContainer();
            }
        });
    }
}
