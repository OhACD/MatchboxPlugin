package com.ohacd.matchbox.game.utils;

import com.ohacd.matchbox.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Handles voting by right-clicking on players during the voting phase.
 * Similar to SwipeHitListener but for voting.
 */
public class VoteItemListener implements Listener {
    private final GameManager gameManager;

    public VoteItemListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;
        
        Player voter = event.getPlayer();
        Player target = (Player) event.getRightClicked();

        // Only allow during voting phase
        if (!gameManager.getPhaseManager().isPhase(GamePhase.VOTING)) {
            return;
        }

        // Delegate to GameManager; it will enforce voting rules
        boolean success = gameManager.handleVote(voter, target);
        
        if (success) {
            // Prevent any other interaction side-effects
            event.setCancelled(true);
        }
    }
}
