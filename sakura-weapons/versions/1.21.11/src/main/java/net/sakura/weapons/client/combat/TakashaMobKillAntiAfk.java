package net.sakura.weapons.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Util;
import net.sakura.weapons.client.config.TakashaMasterConfig;

/**
 * Handles idle-only Anti-AFK oscillation (subtle left-right movement).
 * GUARANTEES 100% FREE PLAYER MOVEMENT: Yields instantly whenever player presses
 * WASD, Jump, or Sneak, so combat movement is never locked or disrupted.
 */
public class TakashaMobKillAntiAfk {

    public enum State {
        IDLE,
        MOVING_LEFT,
        PAUSE_BETWEEN,
        MOVING_RIGHT
    }

    private State state = State.IDLE;
    private long stateStartTime = 0L;
    private long nextCycleTime = 0L;

    private boolean wePressedLeft = false;
    private boolean wePressedRight = false;
    private boolean wePressedSneak = false;

    public void tick(Minecraft client, LocalPlayer player) {
        TakashaMasterConfig config = TakashaMasterConfig.getInstance();
        if (!config.antiAfkEnabled || !config.autoKillEnabled) {
            reset(client);
            return;
        }

        if (client.options == null || player == null) {
            reset(client);
            return;
        }

        // Never simulate Anti-AFK keys if a container/screen is open, if Capcay solver is active, or if eating
        if (client.screen != null || net.sakura.weapons.client.fishing.CapcayManager.getInstance().isBusy() || net.sakura.weapons.client.food.TakashaAutoEatHandler.getInstance().isEating()) {
            reset(client);
            return;
        }

        // CRITICAL: Check if player is actively pressing any movement key
        boolean isPlayerMoving = client.options.keyUp.isDown()
                || client.options.keyDown.isDown()
                || (client.options.keyLeft.isDown() && !wePressedLeft)
                || (client.options.keyRight.isDown() && !wePressedRight)
                || client.options.keyJump.isDown()
                || (client.options.keyShift.isDown() && !wePressedSneak);

        if (isPlayerMoving) {
            // Player is controlling the character: yield instantly!
            releaseMovementKeys(client);
            state = State.IDLE;
            stateStartTime = Util.getMillis();
            return;
        }

        long now = Util.getMillis();
        int durationMs = Math.max(50, config.antiAfkDurationMs);

        // Sneak lock if configured
        if (config.antiAfkAlwaysSneak) {
            if (!client.options.keyShift.isDown()) {
                client.options.keyShift.setDown(true);
                wePressedSneak = true;
            }
        }

        // If strafe is disabled, only keep sneak and do not press A/D
        if (!config.antiAfkStrafeEnabled) {
            stopLeft(client);
            stopRight(client);
            state = State.IDLE;
            return;
        }

        boolean isContinuous = config.antiAfkContinuous && config.antiAfkIntervalMs <= 0;
        long interval = config.antiAfkIntervalMs > 0 ? config.antiAfkIntervalMs : 2000L;

        switch (state) {
            case IDLE -> {
                if (isContinuous || now >= nextCycleTime) {
                    if (config.antiAfkPattern == TakashaMasterConfig.AntiAfkPattern.RIGHT_ONLY) {
                        state = State.MOVING_RIGHT;
                        stateStartTime = now;
                        pressRight(client);
                    } else if (config.antiAfkPattern == TakashaMasterConfig.AntiAfkPattern.RANDOM_JITTER) {
                        if (Math.random() < 0.5) {
                            state = State.MOVING_LEFT;
                            stateStartTime = now;
                            pressLeft(client);
                        } else {
                            state = State.MOVING_RIGHT;
                            stateStartTime = now;
                            pressRight(client);
                        }
                    } else {
                        state = State.MOVING_LEFT;
                        stateStartTime = now;
                        pressLeft(client);
                    }
                }
            }
            case MOVING_LEFT -> {
                pressLeft(client);
                if (now - stateStartTime >= durationMs) {
                    stopLeft(client);
                    state = State.PAUSE_BETWEEN;
                    stateStartTime = now;
                }
            }
            case PAUSE_BETWEEN -> {
                if (now - stateStartTime >= 40L) {
                    if (config.antiAfkPattern == TakashaMasterConfig.AntiAfkPattern.LEFT_ONLY) {
                        if (isContinuous) {
                            state = State.MOVING_LEFT;
                            stateStartTime = now;
                            pressLeft(client);
                        } else {
                            state = State.IDLE;
                            stateStartTime = now;
                            nextCycleTime = now + interval;
                        }
                    } else {
                        state = State.MOVING_RIGHT;
                        stateStartTime = now;
                        pressRight(client);
                    }
                }
            }
            case MOVING_RIGHT -> {
                pressRight(client);
                if (now - stateStartTime >= durationMs) {
                    stopRight(client);
                    if (isContinuous) {
                        if (config.antiAfkPattern == TakashaMasterConfig.AntiAfkPattern.RIGHT_ONLY) {
                            state = State.MOVING_RIGHT;
                        } else {
                            state = State.MOVING_LEFT;
                        }
                        stateStartTime = now;
                        if (state == State.MOVING_LEFT) pressLeft(client);
                        else pressRight(client);
                    } else {
                        state = State.IDLE;
                        stateStartTime = now;
                        nextCycleTime = now + interval;
                    }
                }
            }
        }
    }

    private void pressLeft(Minecraft client) {
        if (client.options != null) {
            if (wePressedRight) stopRight(client);
            client.options.keyLeft.setDown(true);
            wePressedLeft = true;
        }
    }

    private void stopLeft(Minecraft client) {
        if (client.options != null && wePressedLeft) {
            client.options.keyLeft.setDown(false);
            wePressedLeft = false;
        }
    }

    private void pressRight(Minecraft client) {
        if (client.options != null) {
            if (wePressedLeft) stopLeft(client);
            client.options.keyRight.setDown(true);
            wePressedRight = true;
        }
    }

    private void stopRight(Minecraft client) {
        if (client.options != null && wePressedRight) {
            client.options.keyRight.setDown(false);
            wePressedRight = false;
        }
    }

    public void releaseMovementKeys(Minecraft client) {
        stopLeft(client);
        stopRight(client);
        if (wePressedSneak && client.options != null) {
            client.options.keyShift.setDown(false);
            wePressedSneak = false;
        }
    }

    public void reset(Minecraft client) {
        releaseMovementKeys(client);
        state = State.IDLE;
        stateStartTime = 0L;
        nextCycleTime = 0L;
    }
}
