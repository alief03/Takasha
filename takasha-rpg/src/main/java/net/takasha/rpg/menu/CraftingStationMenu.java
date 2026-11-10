package net.takasha.rpg.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.takasha.rpg.mmo.MMOItemManager;
import net.takasha.rpg.mmo.MMOItemTemplate;

import java.util.*;

/**
 * CraftingStationMenu
 * 1:1 MMOItems Crafting Station 2-Tier GUI suite:
 *   Tier 1: Station View (54 slots) - recipe grid & crafting queue
 *   Tier 2: Recipe Preview (45 slots) - ingredient matrix & instant/queued crafting
 */
public class CraftingStationMenu {

    public record Ingredient(Item item, int count, String displayName) {}

    public static class StationRecipe {
        private final String id;
        private final String displayName;
        private final ItemStack output;
        private final List<Ingredient> ingredients;
        private final int craftingTimeSeconds;

        public StationRecipe(String id, String displayName, ItemStack output, List<Ingredient> ingredients, int craftingTimeSeconds) {
            this.id = id;
            this.displayName = displayName;
            this.output = output;
            this.ingredients = ingredients;
            this.craftingTimeSeconds = craftingTimeSeconds;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public ItemStack getOutput() { return output; }
        public List<Ingredient> getIngredients() { return ingredients; }
        public int getCraftingTimeSeconds() { return craftingTimeSeconds; }
    }

    private static final Map<String, List<StationRecipe>> STATIONS = new HashMap<>();

    static {
        registerDefaultStations();
    }

    private static final int[] RECIPE_GRID_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private static final int[] PREVIEW_MATRIX_SLOTS = {
            11, 12, 13,
            20, 21, 22,
            29, 30, 31
    };

    public static void open(ServerPlayer player, String stationId) {
        String cleanStation = (stationId == null || stationId.isBlank()) ? "arcane-forge" : stationId.toLowerCase(Locale.ROOT);
        List<StationRecipe> recipes = STATIONS.getOrDefault(cleanStation, STATIONS.get("arcane-forge"));
        if (recipes == null || recipes.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c[MMOItems] Crafting station '" + cleanStation + "' tidak ditemukan!"));
            return;
        }

        SimpleContainer container = new SimpleContainer(54);
        populateStationContainer(container, cleanStation, recipes);

        if (player.containerMenu instanceof ReadOnlyChestMenu currentMenu
                && currentMenu.getContainer() instanceof SimpleContainer curCont
                && curCont.getContainerSize() == 54) {
            populateStationContainer(curCont, cleanStation, recipes);
            currentMenu.setSlotClickAction((slotId, button, clickType, pl) -> handleStationClick(player, cleanStation, recipes, slotId));
            currentMenu.broadcastChanges();
            return;
        }

        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ReadOnlyChestMenu(id, inv, container, 6, (slotId, button, clickType, pl) ->
                        handleStationClick(player, cleanStation, recipes, slotId)),
                Component.literal("§8Crafting Station > §6" + formatStationName(cleanStation))
        ));
    }

    private static void populateStationContainer(SimpleContainer container, String stationId, List<StationRecipe> recipes) {
        ItemStack filler = new ItemStack(Items.STAINED_GLASS_PANE.gray());
        filler.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        for (int i = 0; i < 54; i++) {
            container.setItem(i, filler.copy());
        }

        // Placeholders in recipe grid
        ItemStack noRecipe = new ItemStack(Items.STAINED_GLASS_PANE.gray());
        noRecipe.set(DataComponents.CUSTOM_NAME, Component.literal("§7- No Recipe -"));
        for (int slot : RECIPE_GRID_SLOTS) {
            container.setItem(slot, noRecipe.copy());
        }

        // Place recipes
        for (int i = 0; i < recipes.size() && i < RECIPE_GRID_SLOTS.length; i++) {
            StationRecipe r = recipes.get(i);
            ItemStack icon = r.getOutput().copy();
            icon.set(DataComponents.CUSTOM_NAME, Component.literal("§6§l" + r.getDisplayName()));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("§7Crafting Time: §f" + r.getCraftingTimeSeconds() + "s"));
            lore.add(Component.empty());
            lore.add(Component.literal("§eRequired Ingredients:"));
            for (Ingredient ing : r.getIngredients()) {
                lore.add(Component.literal(" §8• §7" + ing.count() + "x §f" + ing.displayName()));
            }
            lore.add(Component.empty());
            lore.add(Component.literal("§a► Click to open Crafting Preview"));

            icon.set(DataComponents.LORE, new ItemLore(lore));
            container.setItem(RECIPE_GRID_SLOTS[i], icon);
        }

        // Active Queue Display (Slots 38..42)
        for (int s = 38; s <= 42; s++) {
            ItemStack queueSlot = new ItemStack(Items.STAINED_GLASS_PANE.cyan());
            queueSlot.set(DataComponents.CUSTOM_NAME, Component.literal("§3Crafting Queue Slot"));
            queueSlot.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§7Ready for crafting."),
                    Component.literal("§7No item currently queued.")
            )));
            container.setItem(s, queueSlot);
        }

        // Slot 49: Close
        ItemStack close = new ItemStack(Items.BARRIER);
        close.set(DataComponents.CUSTOM_NAME, Component.literal("§c◄ Close"));
        container.setItem(49, close);
    }

    private static void handleStationClick(ServerPlayer player, String stationId, List<StationRecipe> recipes, int slotId) {
        if (slotId == 49) {
            player.closeContainer();
            return;
        }

        for (int i = 0; i < RECIPE_GRID_SLOTS.length; i++) {
            if (RECIPE_GRID_SLOTS[i] == slotId) {
                if (i >= recipes.size()) return;
                StationRecipe recipe = recipes.get(i);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                openPreview(player, stationId, recipe);
                return;
            }
        }
    }

    public static void openPreview(ServerPlayer player, String stationId, StationRecipe recipe) {
        SimpleContainer container = new SimpleContainer(45);
        populatePreviewContainer(container, stationId, recipe);

        if (player.containerMenu instanceof ReadOnlyChestMenu currentMenu
                && currentMenu.getContainer() instanceof SimpleContainer curCont
                && curCont.getContainerSize() == 45) {
            populatePreviewContainer(curCont, stationId, recipe);
            currentMenu.setSlotClickAction((slotId, button, clickType, pl) -> handlePreviewClick(player, stationId, recipe, slotId));
            currentMenu.broadcastChanges();
            return;
        }

        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ReadOnlyChestMenu(id, inv, container, 5, (slotId, button, clickType, pl) ->
                        handlePreviewClick(player, stationId, recipe, slotId)),
                Component.literal("§8Crafting Preview > §6" + recipe.getDisplayName())
        ));
    }

    private static void populatePreviewContainer(SimpleContainer container, String stationId, StationRecipe recipe) {
        ItemStack filler = new ItemStack(Items.STAINED_GLASS_PANE.gray());
        filler.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        for (int i = 0; i < 45; i++) {
            container.setItem(i, filler.copy());
        }

        // Place 3x3 ingredients matrix
        for (int i = 0; i < PREVIEW_MATRIX_SLOTS.length; i++) {
            int slot = PREVIEW_MATRIX_SLOTS[i];
            if (i < recipe.getIngredients().size()) {
                Ingredient ing = recipe.getIngredients().get(i);
                ItemStack ingStack = new ItemStack(ing.item(), ing.count());
                ingStack.set(DataComponents.CUSTOM_NAME, Component.literal("§e" + ing.count() + "x " + ing.displayName()));
                container.setItem(slot, ingStack);
            } else {
                ItemStack empty = new ItemStack(Items.STAINED_GLASS_PANE.black());
                empty.set(DataComponents.CUSTOM_NAME, Component.literal("§8Empty Ingredient Slot"));
                container.setItem(slot, empty);
            }
        }

        // Output Slot (Slot 24)
        container.setItem(24, recipe.getOutput().copy());

        // Slot 34: Confirm Crafting (Emerald Block)
        ItemStack confirm = new ItemStack(Items.EMERALD_BLOCK);
        confirm.set(DataComponents.CUSTOM_NAME, Component.literal("§a§lConfirm Crafting"));
        confirm.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Crafting Time: §f" + recipe.getCraftingTimeSeconds() + "s"),
                Component.empty(),
                Component.literal("§e► Click to craft this item!")
        )));
        container.setItem(34, confirm);

        // Slot 40: Back to Station (Barrier)
        ItemStack back = new ItemStack(Items.BARRIER);
        back.set(DataComponents.CUSTOM_NAME, Component.literal("§c◄ Back to Station"));
        container.setItem(40, back);
    }

    private static void handlePreviewClick(ServerPlayer player, String stationId, StationRecipe recipe, int slotId) {
        // Slot 40: Back
        if (slotId == 40) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
            open(player, stationId);
            return;
        }

        // Slot 34: Confirm Craft
        if (slotId == 34) {
            if (!player.isCreative()) {
                // Check if player has all ingredients
                var inv = player.getInventory();
                boolean canCraft = true;
                for (Ingredient ing : recipe.getIngredients()) {
                    int count = 0;
                    for (int i = 0; i < inv.getContainerSize(); i++) {
                        ItemStack st = inv.getItem(i);
                        if (!st.isEmpty() && st.is(ing.item())) {
                            count += st.getCount();
                        }
                    }
                    if (count < ing.count()) {
                        canCraft = false;
                        break;
                    }
                }

                if (!canCraft) {
                    player.sendSystemMessage(Component.literal("§c[MMOItems] Kamu tidak memiliki bahan yang cukup untuk membuat item ini!"));
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
                    return;
                }

                // Deduct ingredients
                for (Ingredient ing : recipe.getIngredients()) {
                    int needed = ing.count();
                    for (int i = 0; i < inv.getContainerSize(); i++) {
                        ItemStack st = inv.getItem(i);
                        if (!st.isEmpty() && st.is(ing.item())) {
                            int take = Math.min(st.getCount(), needed);
                            st.shrink(take);
                            needed -= take;
                            if (needed <= 0) break;
                        }
                    }
                }
            }

            // Give output item
            ItemStack crafted = recipe.getOutput().copy();
            if (!player.getInventory().add(crafted)) {
                player.drop(crafted, false);
            }

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
            player.sendSystemMessage(Component.literal("§a§l[Crafting] §fBerhasil membuat §6" + recipe.getDisplayName() + "§f!"));
            open(player, stationId);
        }
    }

    private static String formatStationName(String id) {
        String[] parts = id.split("[-_]");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static void registerDefaultStations() {
        List<StationRecipe> arcaneRecipes = new ArrayList<>();

        // 1. Netherite Claymore
        ItemStack claymore = MMOItemManager.generateItemStack("SWORD", "NETHERITE_CLAYMORE", 1);
        if (claymore.isEmpty()) claymore = new ItemStack(Items.NETHERITE_SWORD);
        arcaneRecipes.add(new StationRecipe(
                "netherite_claymore",
                "Netherite Claymore",
                claymore,
                List.of(
                        new Ingredient(Items.NETHERITE_INGOT, 2, "Netherite Ingot"),
                        new Ingredient(Items.DIAMOND, 4, "Diamond"),
                        new Ingredient(Items.BLAZE_ROD, 2, "Blaze Rod")
                ),
                10
        ));

        // 2. Crack Pickaxe
        ItemStack crackPick = MMOItemManager.generateItemStack("TOOL", "CRACK_PICKAXE", 1);
        if (crackPick.isEmpty()) crackPick = new ItemStack(Items.NETHERITE_PICKAXE);
        arcaneRecipes.add(new StationRecipe(
                "crack_pickaxe",
                "Crack Pickaxe",
                crackPick,
                List.of(
                        new Ingredient(Items.NETHERITE_PICKAXE, 1, "Netherite Pickaxe"),
                        new Ingredient(Items.TNT, 4, "TNT"),
                        new Ingredient(Items.FIRE_CHARGE, 2, "Fire Charge")
                ),
                5
        ));

        // 3. Aegis Bulwark
        ItemStack shield = MMOItemManager.generateItemStack("SHIELD", "AEGIS_BULWARK", 1);
        if (shield.isEmpty()) shield = new ItemStack(Items.SHIELD);
        arcaneRecipes.add(new StationRecipe(
                "aegis_bulwark",
                "Aegis Bulwark",
                shield,
                List.of(
                        new Ingredient(Items.SHIELD, 1, "Shield"),
                        new Ingredient(Items.NETHERITE_INGOT, 1, "Netherite Ingot"),
                        new Ingredient(Items.HEART_OF_THE_SEA, 1, "Heart of the Sea")
                ),
                8
        ));

        STATIONS.put("arcane-forge", arcaneRecipes);
        STATIONS.put("default", arcaneRecipes);
    }
}

