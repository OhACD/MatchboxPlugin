package com.ohacd.matchbox.game.ability;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.utils.GamePhase;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Detects right-clicks on other players and forwards to GameManager.handleSwipe()
 * only when the shooter's swipe window is active. Silent; no feedback shown.
 */
public class SwipeHitListener implements Listener {
    private final GameManager gameManager;

    public SwipeHitListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;
        Player attacker = event.getPlayer();
        Player target = (Player) event.getRightClicked();

        // Get session context for this player
        SessionGameContext context = gameManager.getContextForPlayer(attacker.getUniqueId());
        if (context == null) {
            return; // Player not in any active game
        }
        
        // Game must be active
        if (!context.getGameState().isGameActive()) {
            return;
        }

        // Only allow during swipe phase
        if (!context.getPhaseManager().isPhase(GamePhase.SWIPE)) return;

        // Only process if attacker has an active swipe window
        if (!gameManager.isSwipeWindowActive(attacker.getUniqueId())) return;

        // Delegate to GameManager; it will enforce Spark role and one swipe per round
        gameManager.handleSwipe(attacker, target);

        // prevent any other interaction side-effects
        event.setCancelled(true);
    }
}

