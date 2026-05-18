package com.ohacd.matchbox.game.utils.listeners;

import com.ohacd.matchbox.Matchbox;
import com.ohacd.matchbox.api.MatchboxAPI;
import com.ohacd.matchbox.api.events.PlayerLeaveEvent;
import com.ohacd.matchbox.game.GameActionPort;
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
 *
 * <p>Policy (CHANGELOG 0.9.8): a disconnect is treated as a leave, not an
 * elimination. We fire {@link PlayerLeaveEvent} with {@code DISCONNECTED},
 * log it to the session flow logger, remove the player from the alive set,
 * and if the session has no remaining online participants we run the full
 * {@link GameManager#endGame(String)} so signs, chat-pipeline state, skins,
 * holograms, and timers are all cleaned up rather than just marking the
 * {@link GameSession} inactive.</p>
 */
public class PlayerQuitListener implements Listener {
    private final GameActionPort gameManager;

    public PlayerQuitListener(GameActionPort gameManager) {
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
        boolean wasAlive = gameState.isAlive(playerId);

        try {
            gameManager.restorePlayerNick(player);
        } catch (Exception e) {
            gameManager.getPlugin().getLogger().warning(
                "Failed to restore nick for quitting player " + player.getName() + ": " + e.getMessage());
        }
        try {
            NameTagManager.showNameTag(player);
        } catch (Exception e) {
            gameManager.getPlugin().getLogger().warning(
                "Failed to restore nametag for quitting player " + player.getName() + ": " + e.getMessage());
        }
        try {
            gameManager.getSkinManager().restoreOriginalSkin(player);
        } catch (Exception e) {
            gameManager.getPlugin().getLogger().warning(
                "Failed to restore skin for quitting player " + player.getName() + ": " + e.getMessage());
        }
        try {
            gameManager.getHunterVisionAdapter().stopVision(playerId);
        } catch (Exception e) {
            gameManager.getPlugin().getLogger().warning(
                "Failed to stop hunter vision for quitting player " + player.getName() + ": " + e.getMessage());
        }

        if (gameState.hasPendingDeath(playerId)) {
            gameState.removePendingDeath(playerId);
        }
        if (wasAlive) {
            gameState.removeAlivePlayer(playerId);
        }

        // Log + dispatch leave (not an elimination — quitting doesn't credit a kill).
        try {
            gameManager.logSessionEvent(sessionName, "LEAVE",
                player.getName() + " disconnected" + (wasAlive ? " (was alive)" : " (was spectator)"));
        } catch (Exception e) {
            gameManager.getPlugin().getLogger().warning(
                "Failed to log leave for " + player.getName() + ": " + e.getMessage());
        }
        try {
            MatchboxAPI.fireEvent(new PlayerLeaveEvent(sessionName, player, PlayerLeaveEvent.LeaveReason.DISCONNECTED));
        } catch (Exception e) {
            gameManager.getPlugin().getLogger().warning(
                "Failed to fire PlayerLeaveEvent for " + player.getName() + ": " + e.getMessage());
        }

        if (!gameState.isGameActive()) {
            return;
        }

        // If the disconnect drops alive-count below the win threshold, this resolves the round.
        if (wasAlive) {
            try {
                gameManager.checkForWin(sessionName);
            } catch (Exception e) {
                gameManager.getPlugin().getLogger().warning(
                    "Failed to check win condition after quit for session " + sessionName + ": " + e.getMessage());
            }
        }

        // If no players remain online in the session, fully end the game so all
        // subsystems (signs, chat pipeline, skins, holograms, timers) are cleaned up.
        try {
            Matchbox matchboxPlugin = Matchbox.getInstance();
            if (matchboxPlugin == null) {
                return;
            }
            SessionManager sessionManager = matchboxPlugin.getSessionManager();
            GameSession session = sessionManager == null ? null : sessionManager.getSession(sessionName);

            int remainingOnline = 0;
            for (UUID participantId : gameState.getAllParticipatingPlayerIds()) {
                Player p = matchboxPlugin.getServer().getPlayer(participantId);
                if (p != null && p.isOnline() && !p.getUniqueId().equals(playerId)) {
                    remainingOnline++;
                }
            }

            if (remainingOnline == 0) {
                matchboxPlugin.getLogger().info(
                    "Session '" + sessionName + "' has no remaining online players — ending game.");
                gameManager.endGame(sessionName);
            } else if (session != null && session.getPlayerCount() == 0) {
                // Session has no registered players but context still exists — safety end.
                gameManager.endGame(sessionName);
            }
        } catch (Exception e) {
            gameManager.getPlugin().getLogger().warning(
                "Error finalising session '" + sessionName + "' after quit: " + e.getMessage());
        }
    }
}
