package net.sakura.weapons.client.food;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.sakura.weapons.client.config.TakashaMasterConfig;
import net.sakura.weapons.client.fishing.CapcayManager;

import java.util.Set;

/**
 * Universal Auto-Eat Engine for Takasha Mod (Minecraft 1.21.11).
 * Monitors player hunger bar across ALL automation modes (AutoKill, AutoFish, AutoFarm, AutoBow).
 * Selects the best safe nutritious food, pauses active automations during consumption,
 * and seamlessly restores the previous weapon/tool upon completion.
 */
public class TakashaAutoEatHandler {

    private static volatile TakashaAutoEatHandler INSTANCE;

    public enum State {
        IDLE,
        EATING,
        COOLDOWN
    }

    private State state = State.IDLE;
    private long eatStartTime = 0L;
    private long lastEatFinishedTime = 0L;
    private int previousSlot = -1;
    private InteractionHand activeHand = InteractionHand.MAIN_HAND;

    // Blacklist of dangerous, harmful, or teleporting foods to avoid during automation
    private static final Set<String> HARMFUL_FOODS = Set.of(
            "chorus_fruit",      // Teleports player randomly away from mob grinder/farm!
            "rotten_flesh",      // Inflicts Hunger
            "spider_eye",        // Inflicts Poison
            "poisonous_potato",  // Inflicts Poison
            "pufferfish",        // Inflicts Poison IV, Nausea, and Hunger
            "raw_chicken",       // Risk of Hunger
            "suspicious_stew"    // Unpredictable negative potion effects
    );

    public static TakashaAutoEatHandler getInstance() {
        if (INSTANCE == null) {
            synchronized (TakashaAutoEatHandler.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TakashaAutoEatHandler();
                }
            }
        }
        return INSTANCE;
    }

    public boolean isEating() {
        return state == State.EATING;
    }

    public void tick(Minecraft client) {
        TakashaMasterConfig config = TakashaMasterConfig.getInstance();
        boolean anyAutoRunning = config.isAnyAutoEnabled();
        if (!config.autoEatEnabled && !anyAutoRunning) {
            if (state == State.EATING) {
                stopEating(client, false);
            }
            return;
        }

        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            if (state == State.EATING) {
                stopEating(client, false);
            }
            return;
        }

        // ABSOLUTE MUTEX: If Capcay Anti-Bot solver is active or screen is open, stop eating immediately
        if (CapcayManager.getInstance().isBusy() || client.screen != null) {
            if (state == State.EATING) {
                stopEating(client, false);
            }
            return;
        }

        long now = Util.getMillis();

        switch (state) {
            case EATING -> {
                // Grace startup window (500ms): Give Minecraft time to initialize item use
                if (now - eatStartTime < 500L) {
                    if (client.options != null) {
                        client.options.keyUse.setDown(true);
                    }
                    if (!player.isUsingItem() && activeHand != null && client.gameMode != null) {
                        client.gameMode.useItem(player, activeHand);
                    }
                } else {
                    // After startup grace window: check if player is actively using item
                    if (player.isUsingItem()) {
                        if (client.options != null) {
                            client.options.keyUse.setDown(true);
                        }
                        // Max eating timeout guard (3.8 seconds)
                        if (now - eatStartTime > 3800L) {
                            stopEating(client, true);
                        }
                    } else {
                        // Food was successfully consumed
                        stopEating(client, true);
                    }
                }
            }
            case COOLDOWN -> {
                // Brief 600ms cooldown to allow food stats to sync
                if (now - lastEatFinishedTime >= 600L) {
                    state = State.IDLE;
                }
            }
            case IDLE -> {
                checkAndStartEating(client, player, config);
            }
        }
    }

    private void checkAndStartEating(Minecraft client, LocalPlayer player, TakashaMasterConfig config) {
        int foodLevel = player.getFoodData().getFoodLevel();
        boolean needsHealthRegen = config.autoEatLowHealth && player.getHealth() < player.getMaxHealth() && player.getFoodData().needsFood();
        boolean shouldEat = (foodLevel <= config.autoEatThreshold || needsHealthRegen) && player.getFoodData().needsFood();

        if (!shouldEat) {
            return;
        }

        // 1. Check Offhand (eats directly if main hand does not intercept right-click)
        ItemStack offhand = player.getOffhandItem();
        ItemStack mainHand = player.getMainHandItem();
        boolean mainHandBlocksOffhand = mainHand.getItem() instanceof net.minecraft.world.item.FishingRodItem ||
                                        mainHand.getItem() instanceof net.minecraft.world.item.BowItem ||
                                        mainHand.getItem() instanceof net.minecraft.world.item.CrossbowItem ||
                                        mainHand.getItem() instanceof net.minecraft.world.item.TridentItem;

        if (isSafeFood(offhand) && !mainHandBlocksOffhand) {
            startEating(client, player, -1, InteractionHand.OFF_HAND);
            return;
        }

        // 2. Scan Hotbar (slots 0-8)
        var inv = player.getInventory();
        int bestHotbarSlot = -1;
        int bestNutrition = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (isSafeFood(stack)) {
                FoodProperties food = stack.get(DataComponents.FOOD);
                int nutrition = food != null ? food.nutrition() : 1;
                if (nutrition > bestNutrition) {
                    bestNutrition = nutrition;
                    bestHotbarSlot = i;
                }
            }
        }

        if (bestHotbarSlot != -1) {
            startEating(client, player, bestHotbarSlot, InteractionHand.MAIN_HAND);
            return;
        }

        // 3. Scan Main Inventory (slots 9-35) and swap into hotbar
        int bestInvSlot = -1;
        bestNutrition = -1;
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (isSafeFood(stack)) {
                FoodProperties food = stack.get(DataComponents.FOOD);
                int nutrition = food != null ? food.nutrition() : 1;
                if (nutrition > bestNutrition) {
                    bestNutrition = nutrition;
                    bestInvSlot = i;
                }
            }
        }

        if (bestInvSlot != -1 && client.gameMode != null) {
            // Find an empty hotbar slot or swap into the current selected slot
            int targetHotbar = inv.getSelectedSlot();
            for (int i = 0; i < 9; i++) {
                if (inv.getItem(i).isEmpty()) {
                    targetHotbar = i;
                    break;
                }
            }

            client.gameMode.handleInventoryMouseClick(
                    0,
                    bestInvSlot,
                    targetHotbar,
                    ClickType.SWAP,
                    player
            );

            startEating(client, player, targetHotbar, InteractionHand.MAIN_HAND);
        }
    }

    private void startEating(Minecraft client, LocalPlayer player, int hotbarSlot, InteractionHand hand) {
        this.activeHand = hand;
        if (hand == InteractionHand.MAIN_HAND && hotbarSlot >= 0 && hotbarSlot < 9) {
            this.previousSlot = player.getInventory().getSelectedSlot();
            if (this.previousSlot != hotbarSlot) {
                player.getInventory().setSelectedSlot(hotbarSlot);
                if (client.getConnection() != null) {
                    client.getConnection().send(new ServerboundSetCarriedItemPacket(hotbarSlot));
                }
            }
        } else {
            this.previousSlot = -1;
        }

        this.state = State.EATING;
        this.eatStartTime = Util.getMillis();

        if (client.gameMode != null) {
            client.gameMode.useItem(player, hand);
        }
        if (client.options != null) {
            client.options.keyUse.setDown(true);
        }

        ItemStack foodStack = hand == InteractionHand.OFF_HAND ? player.getOffhandItem() : player.getMainHandItem();
        String foodName = foodStack != null && !foodStack.isEmpty() ? foodStack.getHoverName().getString() : "Makanan";
        player.displayClientMessage(Component.literal("§6[Auto Eat] Memakan " + foodName + "... (Otomasi dijeda sejenak)"), true);
    }

    private void stopEating(Minecraft client, boolean restoreSlot) {
        if (client.options != null) {
            client.options.keyUse.setDown(false);
        }

        LocalPlayer player = client.player;
        if (player != null && restoreSlot && previousSlot >= 0 && previousSlot < 9) {
            if (player.getInventory().getSelectedSlot() != previousSlot) {
                player.getInventory().setSelectedSlot(previousSlot);
                if (client.getConnection() != null) {
                    client.getConnection().send(new ServerboundSetCarriedItemPacket(previousSlot));
                }
            }
        }

        this.previousSlot = -1;
        this.state = State.COOLDOWN;
        this.lastEatFinishedTime = Util.getMillis();
    }

    public static boolean isSafeFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) return false;

        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return false;
        String path = id.getPath();

        return !HARMFUL_FOODS.contains(path);
    }
}

