package com.ohacd.matchbox.api;

/**
 * Represents different chat channels in the Matchbox chat system.
 * Used to route messages to appropriate recipients based on player status and game state.
 *
 * @since 0.9.5
 * @author Matchbox Team
 */
public enum ChatChannel {
    /**
     * Game chat channel - messages from alive players visible to alive players and spectators.
     * Spectators cannot send to this channel.
     */
    GAME,

    /**
     * Spectator chat channel - messages from spectators visible only to other spectators in the same session.
     * Alive players cannot see or send to this channel.
     */
    SPECTATOR,

    /**
     * Global chat channel - bypasses all game chat filtering and uses normal server chat.
     * Used for administrative messages or when chat should not be filtered.
     */
    GLOBAL
}
