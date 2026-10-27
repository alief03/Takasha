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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.nbt.CompoundTag;
import net.takasha.rpg.enchant.TakashaEnchantRegistry;
import net.takasha.rpg.enchant.TakashaEnchantRegistry.CustomEnchantDef;
import net.takasha.rpg.item.TakashaCustomBookItem;
import net.takasha.rpg.registry.ModRpgItems;

import java.util.ArrayList;
import java.util.List;

/**
 * AlchemistMenu
 * Antarmuka GUI Alchemist 27-slot autentik 100% identik dengan AdvancedEnchantments alchemist.yml (/alchem).
 * - Judul: "&8Alchemist" (27 slot, 3 baris)
 * - Filler: Black Stained Glass Pane
 * - Slot 3: Bahan Masukan 1 (Input 1)
 * - Slot 5: Bahan Masukan 2 (Input 2)
 * - Slot 13: Preview Hasil Alkimia (Item Preview)
 * - Slot 22: Penjelasan (Red Pane) atau Konfirmasi Tukar (Lime Pane: CLICK TO EXCHANGE)
 * - Pembayaran: Menggunakan EXP points autentik.
 */
public class AlchemistMenu extends ChestMenu {

    public static final int INPUT_SLOT_1 = 3;
    public static final int INPUT_SLOT_2 = 5;
    public static final int PREVIEW_SLOT = 13;
    public static final int ACTION_SLOT = 22;

    private final SimpleContainer alchemContainer;

    public static void open(ServerPlayer player) {
        SimpleContainer container = new SimpleContainer(27);
        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new AlchemistMenu(id, inv, container),
            Component.literal("§8Alchemist")
        ));
    }

    public AlchemistMenu(int containerId, Inventory playerInventory, SimpleContainer container) {
        super(MenuType.GENERIC_9x3, containerId, playerInventory, container, 3);
        this.alchemContainer = container;
        setupGui();
    }

    private void setupGui() {
        ItemStack filler = makeItem(Items.STAINED_GLASS_PANE.black(), " ", List.of());
        for (int i = 0; i < 27; i++) {
            if (i == INPUT_SLOT_1 || i == INPUT_SLOT_2 || i == PREVIEW_SLOT || i == ACTION_SLOT) continue;
            alchemContainer.setItem(i, filler.copy());
        }
        updateCombinePreview();
    }

    private boolean isInputSlot(int slot) {
        return slot == INPUT_SLOT_1 || slot == INPUT_SLOT_2;
    }

    public void updateCombinePreview() {
        ItemStack in1 = alchemContainer.getItem(INPUT_SLOT_1);
        ItemStack in2 = alchemContainer.getItem(INPUT_SLOT_2);

        CombineResult result = computeResult(in1, in2);

        if (result != null && !result.resultStack.isEmpty()) {
            // Valid recipe! Show output in Slot 13
            alchemContainer.setItem(PREVIEW_SLOT, result.resultStack.copy());

            // Slot 22: Lime Pane (CLICK TO EXCHANGE)
            List<Component> confirmLore = new ArrayList<>();
            confirmLore.add(Component.literal("§c§lCOSTS §c" + result.expCost + " EXP"));
            confirmLore.add(Component.literal(""));
            confirmLore.add(Component.literal("§7Click here to confirm this"));
            confirmLore.add(Component.literal("§7item exchange and receive your"));
            confirmLore.add(Component.literal("§7epic new item shown above!"));

            alchemContainer.setItem(ACTION_SLOT, makeItem(Items.STAINED_GLASS_PANE.lime(), "§b§lCLICK TO EXCHANGE", confirmLore));
        } else {
            // Invalid / empty recipe
            // Slot 13: Default Item Preview item
            List<Component> previewLore = new ArrayList<>();
            previewLore.add(Component.literal("§7A preview of item you will"));
            previewLore.add(Component.literal("§7receive from the Alchemist"));
            previewLore.add(Component.literal("§7will be displayed here."));
            alchemContainer.setItem(PREVIEW_SLOT, makeItem(Items.STAINED_GLASS_PANE.white(), "§e§lItem Preview", previewLore));

            // Slot 22: Red Explanation item
            List<Component> explainLore = new ArrayList<>();
            explainLore.add(Component.literal("§7The Alchemist will Exchange..."));
            explainLore.add(Component.literal(""));
            explainLore.add(Component.literal("§b§l* §f2X Enchantment Books"));
            explainLore.add(Component.literal("§7(of the same type and level)"));
            explainLore.add(Component.literal("§d= §f1X Enchantment Book"));
            explainLore.add(Component.literal("§7(of higher level)"));
            explainLore.add(Component.literal(""));
            explainLore.add(Component.literal("§b§l* §f2X Magic Dust"));
            explainLore.add(Component.literal("§7(of the same rarity)"));
            explainLore.add(Component.literal("§d= §f1X Magic Dust"));
            explainLore.add(Component.literal("§7(of higher chances)"));

            alchemContainer.setItem(ACTION_SLOT, makeItem(Items.STAINED_GLASS_PANE.red(), "§e§lAlchemist", explainLore));
        }
    }

    public static record CombineResult(ItemStack resultStack, int expCost) {}

    private CombineResult computeResult(ItemStack in1, ItemStack in2) {
        if (in1.isEmpty() || in2.isEmpty()) return null;

        // 1. Combine 2 Custom Enchantment Books
        if (in1.getItem() instanceof TakashaCustomBookItem && in2.getItem() instanceof TakashaCustomBookItem) {
            String enchant1 = TakashaCustomBookItem.getEnchantId(in1);
            String enchant2 = TakashaCustomBookItem.getEnchantId(in2);
            int lvl1 = TakashaCustomBookItem.getEnchantLevel(in1);
            int lvl2 = TakashaCustomBookItem.getEnchantLevel(in2);

            if (enchant1 != null && enchant1.equals(enchant2) && lvl1 == lvl2) {
                CustomEnchantDef def = TakashaEnchantRegistry.get(enchant1);
                if (def != null && lvl1 < def.maxLevel) {
                    int newLvl = lvl1 + 1;
                    int s1 = TakashaCustomBookItem.getSuccessRate(in1);
                    int s2 = TakashaCustomBookItem.getSuccessRate(in2);
                    int d1 = TakashaCustomBookItem.getDestroyRate(in1);
                    int d2 = TakashaCustomBookItem.getDestroyRate(in2);

                    int newSuccess = Math.min(100, (s1 + s2) / 2);
                    int newDestroy = Math.max(0, (d1 + d2) / 2);

                    ItemStack resBook = TakashaCustomBookItem.createRevealedBook(def, newLvl, newSuccess, newDestroy);
                    return new CombineResult(resBook, 250);
                }
            }
        }

        // 2. Combine 2 Magic Dust
        if (in1.is(ModRpgItems.MAGIC_DUST) && in2.is(ModRpgItems.MAGIC_DUST)) {
            ItemStack resDust = new ItemStack(ModRpgItems.MAGIC_DUST);
            resDust.set(DataComponents.CUSTOM_NAME, Component.literal("§a§lMagic Dust (Upgraded)"));
            resDust.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Increases success rate of enchantment books."),
                Component.literal("§eDrag n drop on custom book to apply.")
            )));
            return new CombineResult(resDust, 100);
        }

        // 3. Combine 2 Slot Increaser Tier I -> Tier II
        if (in1.is(ModRpgItems.SLOT_INCREASER_1) && in2.is(ModRpgItems.SLOT_INCREASER_1)) {
            ItemStack res = new ItemStack(ModRpgItems.SLOT_INCREASER_2);
            return new CombineResult(res, 250);
        }

        // 4. Combine 2 Slot Increaser Tier II -> Tier III (Soul)
        if (in1.is(ModRpgItems.SLOT_INCREASER_2) && in2.is(ModRpgItems.SLOT_INCREASER_2)) {
            ItemStack res = new ItemStack(ModRpgItems.SLOT_INCREASER_3);
            return new CombineResult(res, 500);
        }

        // 5. Combine 2 White Scrolls -> Holy Scroll
        if (in1.is(ModRpgItems.WHITE_SCROLL) && in2.is(ModRpgItems.WHITE_SCROLL)) {
            ItemStack res = new ItemStack(ModRpgItems.HOLY_SCROLL);
            return new CombineResult(res, 500);
        }

        return null;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId == ACTION_SLOT && player instanceof ServerPlayer sp) {
            handleExchange(sp);
            return;
        }

        // Prevent moving items from preview, filler, or action slots
        if (slotId >= 0 && slotId < 27 && !isInputSlot(slotId)) {
            return;
        }

        super.clicked(slotId, button, clickType, player);
        updateCombinePreview();
    }

    private void handleExchange(ServerPlayer sp) {
        ItemStack in1 = alchemContainer.getItem(INPUT_SLOT_1);
        ItemStack in2 = alchemContainer.getItem(INPUT_SLOT_2);
        CombineResult result = computeResult(in1, in2);

        if (result == null || result.resultStack.isEmpty()) {
            sp.sendSystemMessage(Component.literal("§cNothing to exchange! Place 2 valid items in slots 3 and 5."));
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }

        int playerExp = EnchanterMenu.getPlayerTotalExp(sp);
        if (playerExp < result.expCost) {
            sp.sendSystemMessage(Component.literal("§cYou do not have enough experience! Requires §e" + result.expCost + " EXP§c. (You have §e" + playerExp + " EXP§c)"));
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }

        // Deduct EXP
        EnchanterMenu.deductPlayerExp(sp, result.expCost);

        // Consume inputs
        in1.shrink(1);
        in2.shrink(1);

        // Give result item
        ItemStack out = result.resultStack.copy();
        if (!sp.getInventory().add(out)) {
            sp.drop(out, false);
        }

        sp.sendSystemMessage(Component.literal("§d§l[Alchemist] §fExchange successful! Received §e" + out.getHoverName().getString() + "§f!"));
        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0f, 1.0f);
        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.5f);

        updateCombinePreview();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer sp) {
            for (int slot : new int[]{INPUT_SLOT_1, INPUT_SLOT_2}) {
                ItemStack stack = alchemContainer.getItem(slot);
                if (!stack.isEmpty()) {
                    if (!sp.getInventory().add(stack)) {
                        sp.drop(stack, false);
                    }
                    alchemContainer.setItem(slot, ItemStack.EMPTY);
                }
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
