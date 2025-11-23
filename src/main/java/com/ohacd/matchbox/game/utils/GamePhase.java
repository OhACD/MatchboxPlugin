package com.ohacd.matchbox.game.utils;

/**
 * Represents the different phases of a game round.
 */
public enum GamePhase {
    /** Players are waiting for the game to start */
    WAITING,
    /** Active gameplay phase where players can swipe, cure, and use abilities */
    SWIPE,
    /** Discussion phase where players can talk and strategize */
    DISCUSSION,
    /** Voting phase where players vote to eliminate suspects */
    VOTING,
    /** Resolution phase (currently unused) */
    RESOLUTION,
    /** Game has ended */
    ENDED
}
