package com.ohacd.matchbox.game.utils.listeners;

import com.ohacd.matchbox.Matchbox;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.session.GameSession;
import com.ohacd.matchbox.game.session.SessionManager;
import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.utils.Managers.NameTagManager;

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

        SessionGameContext context = gameManager.getContextForPlayer(playerId);
        if (context == null) {
            return; // Player not in any active game
        }

        String sessionName = context.getSessionName();
        GameState gameState = context.getGameState();

        NameTagManager.showNameTag(player);
        gameManager.getSkinManager().restoreOriginalSkin(player);
        gameManager.getHunterVisionAdapter().stopVision(playerId);

        if (gameState.hasPendingDeath(playerId)) {
            gameState.removePendingDeath(playerId);
        }
        gameState.removeAlivePlayer(playerId);

        if (!gameState.isGameActive()) {
            return;
        }

        gameManager.checkForWin(sessionName);

        try {
            Matchbox matchboxPlugin = Matchbox.getInstance();
            if (matchboxPlugin == null) {
                return;
            }
            SessionManager sessionManager = matchboxPlugin.getSessionManager();
            if (sessionManager == null) {
                return;
            }
            GameSession session = sessionManager.getSession(sessionName);
            if (session != null && session.getPlayerCount() == 0) {
                session.setActive(false);
                matchboxPlugin.getLogger().info("Session '" + sessionName + "' ended - no players left");
            }
        } catch (Exception ignored) {
            // Suppress cleanup issues on quit to avoid disconnect spam.
        }
    }
}
