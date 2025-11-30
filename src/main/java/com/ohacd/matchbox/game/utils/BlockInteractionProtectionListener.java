package com.ohacd.matchbox.game.utils;

import com.ohacd.matchbox.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Prevents players from interacting with blocks during active games.
 * Only allows interactions with items (for abilities and voting).
 */
public class BlockInteractionProtectionListener implements Listener {
    private final GameManager gameManager;

    public BlockInteractionProtectionListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Prevents block interactions during active games.
     * Allows item interactions (for abilities and voting).
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in an active game
        if (!isPlayerInActiveGame(player)) {
            return;
        }

        // Allow item interactions (abilities, voting papers, etc.)
        // These are handled by other listeners
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_AIR) {
            return;
        }

        // Block all block interactions (right-click on blocks, left-click on blocks)
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
        }
    }

    /**
     * Checks if a player is in an active game.
     */
    private boolean isPlayerInActiveGame(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        com.ohacd.matchbox.game.SessionGameContext context = gameManager.getContextForPlayer(player.getUniqueId());
        if (context == null) {
            return false;
        }

        return context.getGameState().isGameActive();
    }
}

