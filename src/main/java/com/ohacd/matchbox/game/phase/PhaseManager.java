package com.ohacd.matchbox.game.phase;

import com.ohacd.matchbox.game.utils.GamePhase;
import org.bukkit.plugin.Plugin;

/**
 * Manages game phase transitions and current phase state.
 */
public class PhaseManager {
    private final Plugin plugin;
    private GamePhase currentPhase = GamePhase.WAITING;
    private GamePhase previousPhase = null;
    private long phaseStartTime = 0;

    public PhaseManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets the current game phase.
     */
    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    /**
     * Gets the previous game phase.
     */
    public GamePhase getPreviousPhase() {
        return previousPhase;
    }

    /**
     * Sets the current game phase with logging.
     */
    public void setPhase(GamePhase phase) {
        if (currentPhase == phase) {
            return; // No change needed
        }

        long phaseDuration = 0;
        if (phaseStartTime > 0) {
            phaseDuration = System.currentTimeMillis() - phaseStartTime;
        }

        plugin.getLogger().info(String.format(
                "Phase transition: %s -> %s (previous phase duration: %.2fs)",
                currentPhase,
                phase,
                phaseDuration / 1000.0
        ));

        this.previousPhase = this.currentPhase;
        this.currentPhase = phase;
        this.phaseStartTime = System.currentTimeMillis();
    }

    /**
     * Checks if the current phase matches the given phase.
     */
    public boolean isPhase(GamePhase phase) {
        return currentPhase == phase;
    }

    /**
     * Resets to waiting phase.
     */
    public void reset() {
        plugin.getLogger().info("Resetting phase manager to WAITING");
        this.previousPhase = this.currentPhase;
        this.currentPhase = GamePhase.WAITING;
        this.phaseStartTime = System.currentTimeMillis();
    }

    /**
     * Gets how long the current phase has been active (in milliseconds).
     */
    public long getCurrentPhaseDuration() {
        if (phaseStartTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - phaseStartTime;
    }

    /**
     * Gets debug information about current phase state.
     */
    public String getDebugInfo() {
        return String.format(
                "Phase[Current=%s, Previous=%s, Duration=%.2fs]",
                currentPhase,
                previousPhase != null ? previousPhase : "none",
                getCurrentPhaseDuration() / 1000.0
        );
    }
}