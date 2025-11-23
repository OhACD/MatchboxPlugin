package com.ohacd.matchbox.game.utils;

import com.ohacd.matchbox.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Handles player disconnections during active games.
 */
public class PlayerQuitListener implements Listener {
    private final GameManager gameManager;

    public PlayerQuitListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // If player is in an active game, handle their disconnection
        if (gameManager.getGameState().getAllParticipatingPlayerIds().contains(playerId)) {
            // Restore their nametag
            NameTagManager.showNameTag(player);

            // Remove pending death if they had one
            if (gameManager.getGameState().hasPendingDeath(playerId)) {
                gameManager.getGameState().removePendingDeath(playerId);
            }

            // Remove them from the game state (this also cleans up infected/swiped flags)
            gameManager.getGameState().removeAlivePlayer(playerId);

            // Check for win condition after player leaves (only if game is active)
            if (gameManager.getGameState().isGameActive()) {
                gameManager.checkForWin();
            }
        }
    }
}