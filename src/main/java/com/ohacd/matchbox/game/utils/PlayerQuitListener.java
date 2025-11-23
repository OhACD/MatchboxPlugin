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

        // Find which session the player is in (if any)
        com.ohacd.matchbox.game.SessionGameContext context = gameManager.getContextForPlayer(playerId);
        if (context == null) {
            return; // Player not in any active game
        }
        
        String sessionName = context.getSessionName();
        com.ohacd.matchbox.game.state.GameState gameState = context.getGameState();

        // Restore their nametag
        NameTagManager.showNameTag(player);

        // Remove pending death if they had one
        if (gameState.hasPendingDeath(playerId)) {
            gameState.removePendingDeath(playerId);
        }

        // Remove them from the game state (this also cleans up infected/swiped flags)
        gameState.removeAlivePlayer(playerId);

        // Check for win condition after player leaves (only if game is active)
        if (gameState.isGameActive()) {
            gameManager.checkForWin(sessionName);
            
            // Also check if session should be ended (no players left)
            try {
                com.ohacd.matchbox.Matchbox matchboxPlugin = com.ohacd.matchbox.Matchbox.getInstance();
                if (matchboxPlugin != null) {
                    com.ohacd.matchbox.game.session.SessionManager sessionManager = matchboxPlugin.getSessionManager();
                    if (sessionManager != null) {
                        com.ohacd.matchbox.game.session.GameSession session = sessionManager.getSession(sessionName);
                        if (session != null) {
                            // Check if session has no players left
                            if (session.getPlayerCount() == 0) {
                                session.setActive(false);
                                matchboxPlugin.getLogger().info("Session '" + sessionName + "' ended - no players left");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore errors in cleanup
            }
        }
    }
}