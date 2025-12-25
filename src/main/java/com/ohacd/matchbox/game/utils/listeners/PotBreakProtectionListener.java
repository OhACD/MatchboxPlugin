package com.ohacd.matchbox.game.utils.listeners;

import org.bukkit.block.Block;
import org.bukkit.block.DecoratedPot;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;

/**
 * Utility class for handling pot break protection when hit
 * by an arrow during the game.
 */
public class PotBreakProtectionListener implements Listener {
    private final GameManager gameManager;

    /**
     * Creates a listener that prevents decorated pots from breaking
     * when hit by arrows during active games.
     *
     * @param gameManager the game manager used to check active sessions
     */
    public PotBreakProtectionListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Prevents decorated pots from breaking when hit by arrows
     * during active games.
     */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        Block target = event.getHitBlock();

        // Only care about arrows hitting blocks
        if (!(projectile instanceof Arrow)) {
            return;
        }

        // Only care about decorated pots
        if (!(target instanceof DecoratedPot)) {
            return;
        }

        // Check if the arrow was shot by a player in an active game
        if (!(projectile.getShooter() instanceof Player shooter)) {
            return;
        }

        if (isPlayerInActiveGame(shooter)) {
            // Cancel pot break
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

        SessionGameContext context = gameManager.getContextForPlayer(player.getUniqueId());
        if (context == null) {
            return false;
        }

        return context.getGameState().isGameActive();
    }
}
