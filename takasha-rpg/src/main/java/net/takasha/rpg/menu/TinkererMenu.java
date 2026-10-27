package net.takasha.rpg.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.takasha.rpg.enchant.TakashaEnchantRegistry.EnchantTier;
import net.takasha.rpg.item.TakashaCustomBookItem;
import net.takasha.rpg.item.TakashaMysteryBookItem;
import net.takasha.rpg.registry.ModRpgItems;

import java.util.ArrayList;
import java.util.List;

/**
 * TinkererMenu
 * Antarmuka GUI Tinkerer 54-slot autentik 100% identik dengan AdvancedEnchantments tinkerer.yml (/tinker).
 * - Judul: "You                   Tinkerer" (54 slot, 6 baris)
 * - Splitter Slots: 4, 13, 22, 31, 40, 49
 * - Player Deposit Slots (kiri): 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30, 36, 37, 38, 39, 45, 46, 47, 48
 * - Tinkerer Offer Slots (kanan): 5, 6, 7, 14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35, 41, 42, 43, 44, 50, 51, 52, 53
 * - Slot 0: Tombol Accept / Confirm Trade
 * - Slot 8: Status Tinkerer Confirm
 */
public class TinkererMenu extends ChestMenu {

    public static final int[] SPLITTER_SLOTS = {4, 13, 22, 31, 40, 49};

    public static final int[] PLAYER_SLOTS = {
        1, 2, 3,
        9, 10, 11, 12,
        18, 19, 20, 21,
        27, 28, 29, 30,
        36, 37, 38, 39,
        45, 46, 47, 48
    };

    public static final int[] TINKERER_SLOTS = {
        5, 6, 7,
        14, 15, 16, 17,
        23, 24, 25, 26,
        32, 33, 34, 35,
        41, 42, 43, 44,
        50, 51, 52, 53
    };

    private final SimpleContainer tinkerContainer;
    private boolean playerAccepted = false;

    public static void open(ServerPlayer player) {
        SimpleContainer container = new SimpleContainer(54);
        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new TinkererMenu(id, inv, container),
            Component.literal("You                   Tinkerer")
        ));
    }

    public TinkererMenu(int containerId, Inventory playerInventory, SimpleContainer container) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6);
        this.tinkerContainer = container;
        setupGui();
    }

    private void setupGui() {
        // Setup Splitter Slots
        ItemStack splitter = makeItem(Items.GLASS_PANE, " ", List.of());
        for (int slot : SPLITTER_SLOTS) {
            tinkerContainer.setItem(slot, splitter.copy());
        }

        updateButtons();
    }

    private boolean isPlayerSlot(int slot) {
        for (int s : PLAYER_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    private int countDepositedItems() {
        int count = 0;
        for (int slot : PLAYER_SLOTS) {
            if (!tinkerContainer.getItem(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public void updateTradeOffers() {
        for (int i = 0; i < PLAYER_SLOTS.length; i++) {
            int pSlot = PLAYER_SLOTS[i];
            int tSlot = TINKERER_SLOTS[i];

            ItemStack inStack = tinkerContainer.getItem(pSlot);
            if (inStack.isEmpty()) {
                tinkerContainer.setItem(tSlot, ItemStack.EMPTY);
            } else {
                ItemStack offer = calculateReward(inStack);
                tinkerContainer.setItem(tSlot, offer);
            }
        }
        updateButtons();
    }

    private ItemStack calculateReward(ItemStack inStack) {
        // 1. Custom Enchantment Book / Mystery Book -> Magic Dust / Secret Dust
        EnchantTier bookTier = null;
        if (inStack.getItem() instanceof TakashaCustomBookItem book) {
            bookTier = book.getTier();
        } else if (inStack.getItem() instanceof TakashaMysteryBookItem mysteryBook) {
            bookTier = mysteryBook.getTier();
        }

        if (bookTier != null) {
            int bonus = switch (bookTier) {
                case SIMPLE -> 5;
                case UNIQUE -> 10;
                case ELITE -> 15;
                case ULTIMATE -> 20;
                case LEGENDARY -> 30;
                case FABLED -> 40;
                case HEROIC -> 50;
                case SOUL -> 60;
                case MASTERY -> 75;
            };

            ItemStack dust = new ItemStack(ModRpgItems.MAGIC_DUST);
            dust.set(DataComponents.CUSTOM_NAME, Component.literal("§a§lMagic Dust (" + bonus + "%)"));
            dust.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Increases success rate of enchantment books by §a+" + bonus + "%§7."),
                Component.literal("§eDrag n drop on custom book to apply.")
            )));
            return dust;
        }

        // 2. Secret Dust or other custom dust -> Experience Bottle
        if (inStack.is(ModRpgItems.SECRET_DUST) || inStack.is(ModRpgItems.MYSTERY_DUST)) {
            ItemStack expBottle = new ItemStack(Items.EXPERIENCE_BOTTLE, Math.min(64, inStack.getCount() * 2));
            expBottle.set(DataComponents.CUSTOM_NAME, Component.literal("§a§lExperience Bottle §7(throw)"));
            expBottle.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§dValue §f" + (inStack.getCount() * 52) + " XP"),
                Component.literal("§dEnchanter §fServer Enchanter")
            )));
            return expBottle;
        }

        // 3. Armor, Weapon, Tool, or Enchanted Items -> Experience Bottles
        int enchantCount = inStack.isEnchanted() ? 3 : 1;
        int count = Math.min(64, Math.max(1, inStack.getCount() * enchantCount * 2));
        ItemStack expBottle = new ItemStack(Items.EXPERIENCE_BOTTLE, count);
        expBottle.set(DataComponents.CUSTOM_NAME, Component.literal("§a§lExperience Bottle §7(throw)"));
        expBottle.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§dValue §f" + (count * 26) + " XP"),
            Component.literal("§dEnchanter §fServer Enchanter")
        )));
        return expBottle;
    }

    private void updateButtons() {
        int items = countDepositedItems();

        if (items == 0) {
            playerAccepted = false;
            // Slot 0: Red Pane (No trade)
            tinkerContainer.setItem(0, makeItem(Items.STAINED_GLASS_PANE.red(), "§c§lClick to ACCEPT trade", List.of(
                Component.literal("§7Place items in your inventory slots on the left.")
            )));
            // Slot 8: Red Pane (Tinkerer waiting)
            tinkerContainer.setItem(8, makeItem(Items.STAINED_GLASS_PANE.red(), "§c§lTinkerer is waiting...", List.of(
                Component.literal("§7Waiting for items on the left.")
            )));
        } else {
            if (!playerAccepted) {
                // Slot 0: Red/Yellow Pane (Ready to click accept)
                tinkerContainer.setItem(0, makeItem(Items.STAINED_GLASS_PANE.red(), "§e§lClick to ACCEPT trade", List.of(
                    Component.literal("§7Click to accept the current trade offers.")
                )));
                // Slot 8: Lime Pane (Tinkerer ready)
                tinkerContainer.setItem(8, makeItem(Items.STAINED_GLASS_PANE.lime(), "§a§lTrade is ready", List.of(
                    Component.literal("§7Tinkerer is ready to trade " + items + " item(s).")
                )));
            } else {
                // Slot 0: Lime Pane (Player confirmed)
                tinkerContainer.setItem(0, makeItem(Items.STAINED_GLASS_PANE.lime(), "§a§lClick to confirm the transaction", List.of(
                    Component.literal("§7Click again to finalize the exchange!")
                )));
                // Slot 8: Lime Pane
                tinkerContainer.setItem(8, makeItem(Items.STAINED_GLASS_PANE.lime(), "§a§lClick to confirm the transaction", List.of(
                    Component.literal("§7Finalizing transaction...")
                )));
            }
        }
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        // Slot 0: Accept / Confirm Button
        if (slotId == 0 && player instanceof ServerPlayer sp) {
            handleAcceptClick(sp);
            return;
        }

        // Disallow clicking splitter slots, offer slots, or slot 8
        if (slotId >= 0 && slotId < 54 && !isPlayerSlot(slotId)) {
            return;
        }

        // When player interacts with deposit slot, reset confirm status
        if (playerAccepted) {
            playerAccepted = false;
        }

        super.clicked(slotId, button, clickType, player);
        updateTradeOffers();
    }

    private void handleAcceptClick(ServerPlayer sp) {
        int items = countDepositedItems();
        if (items == 0) {
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }

        if (!playerAccepted) {
            playerAccepted = true;
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 1.0f, 1.0f);
            updateButtons();
        } else {
            // Finalize trade!
            List<ItemStack> rewards = new ArrayList<>();
            for (int tSlot : TINKERER_SLOTS) {
                ItemStack off = tinkerContainer.getItem(tSlot);
                if (!off.isEmpty()) {
                    rewards.add(off.copy());
                    tinkerContainer.setItem(tSlot, ItemStack.EMPTY);
                }
            }

            // Consume player deposit items
            for (int pSlot : PLAYER_SLOTS) {
                tinkerContainer.setItem(pSlot, ItemStack.EMPTY);
            }

            // Give rewards
            for (ItemStack reward : rewards) {
                if (!sp.getInventory().add(reward)) {
                    sp.drop(reward, false);
                }
            }

            playerAccepted = false;
            sp.sendSystemMessage(Component.literal("§a§l[Tinkerer] §fTrade finalized! Received §a" + rewards.size() + " offer items§f!"));
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);

            updateButtons();
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer sp) {
            // Return any deposited items back to player
            for (int pSlot : PLAYER_SLOTS) {
                ItemStack stack = tinkerContainer.getItem(pSlot);
                if (!stack.isEmpty()) {
                    if (!sp.getInventory().add(stack)) {
                        sp.drop(stack, false);
                    }
                    tinkerContainer.setItem(pSlot, ItemStack.EMPTY);
                }
            }
            // Clear offer slots
            for (int tSlot : TINKERER_SLOTS) {
                tinkerContainer.setItem(tSlot, ItemStack.EMPTY);
            }
        }
    }

    private static ItemStack makeItem(Item item, String name, List<Component> lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        if (lore != null && !lore.isEmpty()) {
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }
        return stack;
    }
}
