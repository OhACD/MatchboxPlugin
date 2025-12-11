package com.ohacd.matchbox.game.ability;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.utils.GamePhase;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Detects right-clicks on other players and forwards to GameManager.handleCure()
 * only when the medic's cure window is active. Silent; no feedback shown.
 */
public class MedicHitListener implements AbilityHandler {
    private final GameManager gameManager;

    public MedicHitListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public void handlePlayerInteractEntity(PlayerInteractEntityEvent event, SessionGameContext context) {
        if (!(event.getRightClicked() instanceof Player)) return;
        Player medic = event.getPlayer();
        Player target = (Player) event.getRightClicked();

        // Only allow during swipe phase
        if (!context.getPhaseManager().isPhase(GamePhase.SWIPE)) return;

        // Only process if medic has an active cure window
        if (!gameManager.isCureWindowActive(medic.getUniqueId())) return;

        // Delegate to GameManager; it will enforce Medic role and one cure per round
        gameManager.handleCure(medic, target);

        // prevent any other interaction side-effects
        event.setCancelled(true);
    }
}

