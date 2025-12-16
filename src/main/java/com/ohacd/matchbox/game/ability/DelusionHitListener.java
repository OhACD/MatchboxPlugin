package com.ohacd.matchbox.game.ability;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.utils.GamePhase;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Detects right-clicks on other players and forwards to GameManager.handleDelusion()
 * only when the spark's delusion window is active. Silent; no feedback shown.
 */
public class DelusionHitListener implements AbilityHandler {
    private final GameManager gameManager;

    public DelusionHitListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public void handlePlayerInteractEntity(PlayerInteractEntityEvent event, SessionGameContext context) {
        if (!(event.getRightClicked() instanceof Player)) return;
        Player attacker = event.getPlayer();
        Player target = (Player) event.getRightClicked();

        // Only allow during swipe phase
        if (!context.getPhaseManager().isPhase(GamePhase.SWIPE)) return;

        // Only process if attacker has an active delusion window
        if (!gameManager.isDelusionWindowActive(attacker.getUniqueId())) return;

        // Delegate to GameManager; it will enforce Spark role and one delusion per round
        gameManager.handleDelusion(attacker, target);

        // prevent any other interaction side-effects
        event.setCancelled(true);
    }
}

