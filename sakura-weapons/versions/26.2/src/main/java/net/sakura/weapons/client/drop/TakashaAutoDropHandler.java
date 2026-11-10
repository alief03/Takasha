package net.sakura.weapons.client.drop;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Util;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sakura.weapons.client.config.TakashaMasterConfig;
import net.sakura.weapons.client.fishing.CapcayManager;
import net.sakura.weapons.client.food.TakashaAutoEatHandler;

import java.util.*;

/**
 * Universal Auto Drop Engine supporting:
 * 1. 3 Discard Categories: Mining, Fishing, and Mob Kill.
 * 2. Scalable rule matching via DropFilterEntry (Item ID, CustomModelData, Display Name).
 * 3. Kaizen Server /drop gatekeeper: verifies /drop status before dropping items with anti-kick spam guard.
 * 4. Smart Food Reserve: guarantees at least 1 slot of food remains untouched in inventory for AutoEat.
 * 5. Valuables safety net: prevents dropping enchanted gear, active tools, or rare items.
 * 6. Anti-cheat safe throttled inventory dropping (1 stack per 160ms).
 */
public class TakashaAutoDropHandler {

    private static volatile TakashaAutoDropHandler INSTANCE;

    public enum ServerDropStatus {
        UNKNOWN,
        CHECKING,
        ACTIVE,
        INACTIVE,
        UNSUPPORTED
    }

    private ServerDropStatus serverDropStatus = ServerDropStatus.UNKNOWN;
    private long lastDropCommandTime = 0L;
    private long lastDropActionTime = 0L;
    private int dropCommandAttempts = 0;

    public static TakashaAutoDropHandler getInstance() {
        if (INSTANCE == null) {
            synchronized (TakashaAutoDropHandler.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TakashaAutoDropHandler();
                }
            }
        }
        return INSTANCE;
    }

    public ServerDropStatus getServerDropStatus() {
        return serverDropStatus;
    }

    public void setServerDropStatus(ServerDropStatus status) {
        this.serverDropStatus = status;
    }

    public void onChatReceived(String text) {
        if (text == null || text.isEmpty()) return;
        String clean = text.replaceAll("§[0-9a-fk-or]", "").trim().toLowerCase();

        // Check if command is invalid or no permission
        if (clean.contains("command invalid") || clean.contains("tidak ada permission") || clean.contains("unknown command")) {
            this.serverDropStatus = ServerDropStatus.UNSUPPORTED;
            this.dropCommandAttempts = 0;
            return;
        }

        // Server Kaizen: "No Drop dimatikan" means Drop Protection is OFF -> Dropping is ALLOWED (ACTIVE)
        if (clean.contains("no drop dimatikan") || clean.contains("drop dimatikan") ||
            clean.contains("bisa membuang") || clean.contains("dapat membuang") ||
            clean.contains("drop diaktifkan") || clean.contains("drop enabled")) {
            this.serverDropStatus = ServerDropStatus.ACTIVE;
            TakashaMasterConfig.getInstance().serverDropActive = true;
            this.dropCommandAttempts = 0;
            return;
        }

        // Server Kaizen: "No Drop dinyalakan" means Drop Protection is ON -> Dropping is BLOCKED (INACTIVE)
        if (clean.contains("no drop dinyalakan") || clean.contains("drop dinyalakan") ||
            clean.contains("tidak bisa membuang") || clean.contains("drop dinonaktifkan") ||
            clean.contains("drop disabled")) {
            this.serverDropStatus = ServerDropStatus.INACTIVE;
            TakashaMasterConfig.getInstance().serverDropActive = false;
        }
    }

    public void ensureServerDropActive(Minecraft client) {
        if (serverDropStatus == ServerDropStatus.ACTIVE || serverDropStatus == ServerDropStatus.UNSUPPORTED) {
            return;
        }
        long now = Util.getMillis();
        // Anti-spam backoff: If tried 2 times without response, back off for 30s to prevent command spam kick
        if (dropCommandAttempts >= 2) {
            if (now - lastDropCommandTime < 30000L) {
                return;
            }
            dropCommandAttempts = 0;
        }

        if (now - lastDropCommandTime > 3000L) {
            lastDropCommandTime = now;
            dropCommandAttempts++;
            this.serverDropStatus = ServerDropStatus.CHECKING;
            if (client.getConnection() != null) {
                client.getConnection().sendCommand("drop");
            }
        }
    }

    public static boolean isFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.has(DataComponents.FOOD);
    }

    public void tick(Minecraft client) {
        TakashaMasterConfig config = TakashaMasterConfig.getInstance();
        boolean anyDropEnabled = config.autoDropMiningEnabled || config.autoDropFishingEnabled || config.autoDropKillEnabled;
        if (!anyDropEnabled) {
            return;
        }

        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return;
        }

        // Mutex: Do not drop if container screen is open, Capcay is solving, or player is Auto-Eating
        if (client.gui.screen() != null || CapcayManager.getInstance().isBusy() || TakashaAutoEatHandler.getInstance().isEating()) {
            return;
        }

        long now = Util.getMillis();
        if (now - lastDropActionTime < 160L) {
            return; // Anti-cheat throttle: 160ms between dropped stacks
        }

        // Compile active filter rules
        List<DropFilterEntry> activeRules = new ArrayList<>();
        if (config.autoDropMiningEnabled) {
            for (String pattern : config.autoDropMiningList) {
                activeRules.add(DropFilterEntry.parse(pattern));
            }
        }
        if (config.autoDropFishingEnabled) {
            for (String pattern : config.autoDropFishingList) {
                activeRules.add(DropFilterEntry.parse(pattern));
            }
        }
        if (config.autoDropKillEnabled) {
            for (String pattern : config.autoDropKillList) {
                activeRules.add(DropFilterEntry.parse(pattern));
            }
        }

        if (activeRules.isEmpty()) {
            return;
        }

        var inv = player.getInventory();

        // Smart Food Reserve: identify 1 reserved slot for each unique food item type in inventory
        Set<Integer> reservedFoodSlots = new HashSet<>();
        if (config.autoDropSmartFoodReserve) {
            Set<Item> seenFoodTypes = new HashSet<>();
            for (int i = 0; i < 36; i++) {
                ItemStack s = inv.getItem(i);
                if (s != null && !s.isEmpty() && isFood(s)) {
                    if (seenFoodTypes.add(s.getItem())) {
                        // First slot containing this food is protected as the 1 reserved food slot!
                        reservedFoodSlots.add(i);
                    }
                }
            }
        }

        // Scan slots 0..35 for droppable candidate
        int candidateSlot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            // Check security safety net
            if (isProtectedItem(stack, config)) {
                continue;
            }

            // Check Smart Food Reserve (never drop the 1 reserved food slot)
            if (config.autoDropSmartFoodReserve && reservedFoodSlots.contains(i)) {
                continue;
            }

            // Check if stack matches any active rule
            boolean matched = false;
            for (DropFilterEntry rule : activeRules) {
                if (rule.matches(stack)) {
                    matched = true;
                    break;
                }
            }

            if (matched) {
                candidateSlot = i;
                break; // Found target slot to drop
            }
        }

        // If no items in inventory need dropping, do NOTHING!
        if (candidateSlot == -1) {
            return;
        }

        // Demand-Driven Kaizen Server Gatekeeper: ONLY check/send /drop if gatekeeper is explicitly enabled
        boolean isKaizen = config.fishingMode == TakashaMasterConfig.FishingMode.KAIZEN || config.mobKillMode == TakashaMasterConfig.MobKillMode.KAIZEN;
        if (config.serverDropGatekeeperEnabled && isKaizen) {
            if (serverDropStatus != ServerDropStatus.ACTIVE && serverDropStatus != ServerDropStatus.UNSUPPORTED) {
                ensureServerDropActive(client);
                return; // Wait for /drop confirmation
            }
        }

        // Execute drop
        lastDropActionTime = now;
        int containerSlot = (candidateSlot < 9 ? 36 + candidateSlot : candidateSlot);
        client.gameMode.handleContainerInput(
                0,
                containerSlot,
                1, // Throw entire stack
                ContainerInput.THROW,
                player
        );
    }

    public static boolean isProtectedItem(ItemStack stack, TakashaMasterConfig config) {
        if (stack == null || stack.isEmpty()) return true;

        // Never drop weapons or tools
        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(ItemTags.PICKAXES) ||
                stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES) || stack.is(Items.BOW) ||
                stack.is(Items.CROSSBOW) || stack.is(Items.FISHING_ROD) || stack.is(Items.MACE)) {
            return true;
        }

        // Never drop wearable armor
        if (stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.CHEST_ARMOR) ||
                stack.is(ItemTags.LEG_ARMOR) || stack.is(ItemTags.FOOT_ARMOR)) {
            return true;
        }

        // Valuables & enchantment protection
        if (config.protectEnchantedAndValuables) {
            if (stack.isEnchanted() || stack.has(DataComponents.ENCHANTMENTS) || stack.has(DataComponents.STORED_ENCHANTMENTS)) {
                return true;
            }
            var lore = stack.get(DataComponents.LORE);
            if (lore != null) {
                String loreStr = lore.toString().toLowerCase();
                if (loreStr.contains("mythic") || loreStr.contains("legendary") || loreStr.contains("keystone") ||
                        loreStr.contains("ancient") || loreStr.contains("dust") || loreStr.contains("scroll") ||
                        loreStr.contains("untradable") || loreStr.contains("relic")) {
                    return true;
                }
            }
        }
        return false;
    }
}
