package net.sakura.weapons.client.fishing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.inventory.ContainerInput;
import net.sakura.weapons.client.config.TakashaMasterConfig;

import java.util.Random;
import java.util.regex.Pattern;

/**
 * Automates Kaizen /capcay challenge when triggered by chat or server prompts.
 */
public class CapcayManager {

    private static volatile CapcayManager INSTANCE;

    public enum State {
        IDLE,
        TRIGGERED,
        AWAITING_GUI,
        SOLVING,
        WAITING_FINISH,
        COOLDOWN
    }

    private State state = State.IDLE;
    private long stateEnteredAt = 0L;
    private long scheduledActionTime = 0L;
    private int lastClickedContainerId = -1;
    private final Random random = new Random();

    private Pattern cachedTriggerPattern;
    private String cachedTriggerRegex;
    private Pattern cachedTitlePattern;
    private String cachedTitleRegex;

    public static CapcayManager getInstance() {
        if (INSTANCE == null) {
            synchronized (CapcayManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CapcayManager();
                }
            }
        }
        return INSTANCE;
    }

    public void tick(Minecraft client) {
        TakashaMasterConfig config = TakashaMasterConfig.getInstance();
        if (!config.autoCapcayEnabled) {
            if (state != State.IDLE) reset();
            return;
        }

        long now = Util.getMillis();

        switch (state) {
            case TRIGGERED -> {
                if (now >= scheduledActionTime) {
                    executeCapcayCommand(client);
                }
            }
            case AWAITING_GUI -> {
                Screen screen = client.gui.screen();
                if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                    if (isCapcayScreen(containerScreen)) {
                        enterSolvingState(containerScreen, now, config);
                    }
                } else if (now - stateEnteredAt > 5000L) {
                    log("§c[AutoCapcay] Timeout menunggu GUI /capcay terbuka. Mereset state...");
                    reset();
                }
            }
            case SOLVING -> {
                Screen screen = client.gui.screen();
                if (screen instanceof AbstractContainerScreen<?> containerScreen && isCapcayScreen(containerScreen)) {
                    if (now >= scheduledActionTime) {
                        solveAndClick(client, containerScreen, config);
                    }
                } else if (now - stateEnteredAt > 4000L) {
                    reset();
                }
            }
            case WAITING_FINISH -> {
                Screen screen = client.gui.screen();
                if (screen == null || now - stateEnteredAt > 2000L) {
                    state = State.COOLDOWN;
                    stateEnteredAt = now;
                    scheduledActionTime = now + 1200L;
                }
            }
            case COOLDOWN -> {
                if (now >= scheduledActionTime) {
                    finishAndResume(client, config);
                }
            }
            case IDLE -> {
                // If user opened capcay GUI manually
                Screen screen = client.gui.screen();
                if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                    if (isCapcayScreen(containerScreen) && containerScreen.getMenu().containerId != lastClickedContainerId) {
                        enterSolvingState(containerScreen, now, config);
                    }
                }
            }
        }
    }

    public void onChatReceived(String text) {
        TakashaMasterConfig config = TakashaMasterConfig.getInstance();
        if (!config.autoCapcayEnabled || text == null || text.isBlank()) {
            return;
        }

        if (state == State.WAITING_FINISH || state == State.SOLVING || state == State.AWAITING_GUI) {
            String lower = text.toLowerCase();
            if (lower.contains("berhasil") || lower.contains("3,000") || lower.contains("bonus uang saku") || lower.contains("melewati captcha")) {
                log("§a[AutoCapcay] Server mengonfirmasi keberhasilan CAPCAY! Menyiapkan kembali joran...");
                state = State.COOLDOWN;
                stateEnteredAt = Util.getMillis();
                scheduledActionTime = Util.getMillis() + 1500L;
                return;
            }
            if (lower.contains("gagal") || lower.contains("salah") || lower.contains("coba lagi")) {
                log("§c[AutoCapcay] Peringatan: Jawaban terdeteksi keliru! Menjadwalkan percobaan ulang...");
                state = State.TRIGGERED;
                stateEnteredAt = Util.getMillis();
                scheduledActionTime = Util.getMillis() + 2000L;
                return;
            }
        }

        if (state == State.IDLE) {
            String regex = config.capcayTriggerRegex;
            if (regex != null && !regex.isBlank()) {
                if (cachedTriggerPattern == null || !regex.equals(cachedTriggerRegex)) {
                    cachedTriggerPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                    cachedTriggerRegex = regex;
                }

                if (cachedTriggerPattern.matcher(text).find()) {
                    triggerCapcay(config);
                }
            }
        }
    }

    public void triggerCapcay(TakashaMasterConfig config) {
        Minecraft client = Minecraft.getInstance();
        state = State.TRIGGERED;
        stateEnteredAt = Util.getMillis();

        long baseDelay = config.capcayCommandDelayMs;
        long jitter = config.capcayHumanJitter ? (long) (random.nextDouble() * 500L) : 0L;
        scheduledActionTime = stateEnteredAt + baseDelay + jitter;

        TakashaFishingHandler.getInstance().pauseForCapcay();

        if (client.player != null && config.capcaySoundAlert) {
            client.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        }

        log("§e[AutoCapcay] Terdeteksi prompt /capcay! Mempersiapkan command dalam " + (baseDelay + jitter) + " ms...");
    }

    private void executeCapcayCommand(Minecraft client) {
        if (client.player != null && client.player.connection != null) {
            log("§b[AutoCapcay] Mengirim perintah /capcay ke server...");
            client.player.connection.sendCommand("capcay");
            state = State.AWAITING_GUI;
            stateEnteredAt = Util.getMillis();
        } else {
            reset();
        }
    }

    private void enterSolvingState(AbstractContainerScreen<?> containerScreen, long now, TakashaMasterConfig config) {
        state = State.SOLVING;
        stateEnteredAt = now;

        long baseDelay = config.capcayDelayMs;
        long jitter = config.capcayHumanJitter ? (long) (random.nextDouble() * 400L) : 0L;
        scheduledActionTime = now + baseDelay + jitter;

        TakashaFishingHandler.getInstance().pauseForCapcay();
        log("§6[AutoCapcay] Chest CAPCAY terdeteksi! Membaca pola item dalam " + (baseDelay + jitter) + " ms...");
    }

    private void solveAndClick(Minecraft client, AbstractContainerScreen<?> containerScreen, TakashaMasterConfig config) {
        if (client.player == null || client.gameMode == null) {
            reset();
            return;
        }

        var menu = containerScreen.getMenu();
        var result = CapcaySolver.solve(menu, client.player.getInventory());

        if (result.success()) {
            int slotIdx = result.targetSlotIndex();
            lastClickedContainerId = menu.containerId;

            log("§a[AutoCapcay] Solusi: " + result.explanation());
            log("§a[AutoCapcay] Mengklik slot " + slotIdx + " (" + result.itemSummary() + ")...");

            client.gameMode.handleContainerInput(
                    menu.containerId,
                    slotIdx,
                    0,
                    ContainerInput.PICKUP,
                    client.player
            );

            if (config.capcaySoundAlert) {
                client.player.playSound(SoundEvents.PLAYER_LEVELUP, 0.8f, 1.5f);
            }

            state = State.WAITING_FINISH;
            stateEnteredAt = Util.getMillis();
        } else {
            log("§c[AutoCapcay] Gagal menyelesaikan CAPCAY: " + result.explanation());
            state = State.WAITING_FINISH;
            stateEnteredAt = Util.getMillis();
        }
    }

    private boolean isCapcayScreen(AbstractContainerScreen<?> screen) {
        String title = screen.getTitle().getString();
        if (title == null || title.isBlank()) {
            return false;
        }

        TakashaMasterConfig config = TakashaMasterConfig.getInstance();
        String regex = config.capcayTitleRegex;
        if (regex == null || regex.isBlank()) {
            return false;
        }

        if (cachedTitlePattern == null || !regex.equals(cachedTitleRegex)) {
            cachedTitlePattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            cachedTitleRegex = regex;
        }

        return cachedTitlePattern.matcher(title).find();
    }

    private void finishAndResume(Minecraft client, TakashaMasterConfig config) {
        state = State.IDLE;
        stateEnteredAt = 0L;

        if (config.capcayAutoResumeAutofish) {
            log("§2[AutoCapcay] Selesai! Melanjutkan Autofish...");
            TakashaFishingHandler.getInstance().resumeAfterCapcay();
        }
    }

    public void reset() {
        state = State.IDLE;
        stateEnteredAt = 0L;
        scheduledActionTime = 0L;
    }

    private void log(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(message));
        }
    }

    public State getState() {
        return state;
    }

    public boolean isBusy() {
        return state != State.IDLE;
    }
}
