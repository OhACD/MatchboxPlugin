package com.ohacd.matchbox.game.utils;

public enum GamePhase {
    WAITING, // The phase where players are waiting for the game
    SWIPE, // The phase where the players are actively in the game, swiping, healing and things of sort happen here
    DISCUSSION,
    VOTING,
    RESOLUTION,
    ENDED
}
