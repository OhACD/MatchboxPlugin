package com.ohacd.matchbox.game.phase;

import com.ohacd.matchbox.game.utils.GamePhase;
import org.bukkit.plugin.Plugin;

/**
 * Manages game phase transitions and current phase state.
 */
public class PhaseManager {
    private final Plugin plugin;
    private GamePhase currentPhase = GamePhase.WAITING;

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
     * Sets the current game phase.
     */
    public void setPhase(GamePhase phase) {
        this.currentPhase = phase;
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
        currentPhase = GamePhase.WAITING;
    }
}
