package com.ohacd.matchbox.game.utils.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;

/**
 * Prevents decorated pots from breaking when hit by arrows
 * during active games.
 */
public class PotBreakProtectionListener implements Listener {

    private final GameManager gameManager;

    public PotBreakProtectionListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileHit(ProjectileHitEvent event) {
        // Only care about arrows 
        if (!(event.getEntity() instanceof Arrow)) {
            return;
        }

        // Only if shot by a player
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player player)) {
            return;
        }

        // Only if player is in an active game
        if (!isPlayerInActiveGame(player)) {
            return;
        }

        // Get the block that was hit
        Block hitBlock = event.getHitBlock();
        if (hitBlock == null || hitBlock.getType() != Material.DECORATED_POT) {
            return;
        }

        // Prevent the pot from breaking
        event.setCancelled(true);

        // player.playSound(hitBlock.getLocation(), Sound.BLOCK_DECORATED_POT_HIT, 0.8f, 1.0f);
        // hitBlock.getWorld().spawnParticle(Particle.CRIT, hitBlock.getLocation().add(0.5, 0.5, 0.5), 5);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.DECORATED_POT) {
            return;
        }

        // If no player is breaking it → likely arrow / projectile
        if (event.getPlayer() == null) {
            // Check nearby arrows (rough but effective)
            boolean hasNearbyArrow = event.getBlock().getWorld()
                    .getNearbyEntities(event.getBlock().getLocation(), 1.5, 1.5, 1.5)
                    .stream()
                    .anyMatch(e -> e instanceof Arrow);

            if (hasNearbyArrow) {
                event.setCancelled(true);
            }
        }
    }

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