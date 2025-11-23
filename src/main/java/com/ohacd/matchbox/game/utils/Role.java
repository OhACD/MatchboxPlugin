package com.ohacd.matchbox.game.utils;

/**
 * Represents the different roles players can have in the game.
 */
public enum Role {
    /** The impostor who tries to eliminate all other players */
    SPARK,
    /** Can cure infected players and see who is infected */
    MEDIC,
    /** Regular player who must identify and vote out the Spark */
    INNOCENT
}
