package com.ohacd.matchbox.game.win;

import com.ohacd.matchbox.game.state.GameState;

/**
 * Checks win conditions for the game.
 */
public class WinConditionChecker {
    private final GameState gameState;

    public WinConditionChecker(GameState gameState) {
        this.gameState = gameState;
    }

    /**
     * Checks if a win condition has been met.
     * @return WinResult containing the winner type, or null if no win condition met
     */
    public WinResult checkWinConditions() {
        java.util.UUID sparkUUID = gameState.getSparkUUID();

        if (sparkUUID == null) {
            return null; // No spark assigned, game not ready
        }

        boolean sparkAlive = gameState.isAlive(sparkUUID);
        long aliveInnocents = gameState.countAliveInnocents();
        int aliveCount = gameState.getAlivePlayerCount();

        // Condition 1: Spark is dead → Innocents win
        if (!sparkAlive) {
            return new WinResult(Winner.INNOCENTS, "§aInnocents win! The Spark has been eliminated.");
        }

        // Condition 2: No innocents alive → Spark wins
        if (aliveInnocents == 0) {
            return new WinResult(Winner.SPARK, "§cSpark wins! All innocents have been eliminated.");
        }

        // Condition 3: Spark is alone with 1 other player → Spark wins
        if (aliveCount == 2) {
            return new WinResult(Winner.SPARK, "§cSpark wins! Only one other player remains.");
        }

        return null; // No win condition met
    }

    /**
     * Represents the winner of the game.
     */
    public enum Winner {
        SPARK,
        INNOCENTS
    }

    /**
     * Contains the result of a win condition check.
     */
    public static class WinResult {
        private final Winner winner;
        private final String message;

        public WinResult(Winner winner, String message) {
            this.winner = winner;
            this.message = message;
        }

        public Winner getWinner() {
            return winner;
        }

        public String getMessage() {
            return message;
        }
    }
}
