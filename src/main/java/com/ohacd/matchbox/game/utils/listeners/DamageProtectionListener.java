package com.ohacd.matchbox.game.utils.listeners;

import com.ohacd.matchbox.game.GameManager;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Prevents players from taking damage or dying during active games.
 * All damage is cancelled and players are made invulnerable.
 */
public class DamageProtectionListener implements Listener {
    private final GameManager gameManager;

    public DamageProtectionListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Prevents all damage to players during active games, except arrow damage.
     * Arrow damage is allowed for nametag revelation.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        
        // Check if player is in an active game
        if (!isPlayerInActiveGame(player)) {
            return;
        }

        // Allow arrow damage (for nametag revelation)
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent entityEvent = (EntityDamageByEntityEvent) event;
            if (entityEvent.getDamager() instanceof Arrow) {
                // Allow arrow damage but set it to 0 (no actual damage)
                event.setDamage(0);
                return;
            }
        }

        // Cancel all other damage during active games
        event.setCancelled(true);
        event.setDamage(0);
    }

    /**
     * Prevents player death during active games.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Check if player is in an active game
        if (!isPlayerInActiveGame(player)) {
            return;
        }

        // Prevent death during active games
        event.setCancelled(true);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);
    }

    /**
     * Prevents hunger loss during active games.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        
        // Check if player is in an active game
        if (!isPlayerInActiveGame(player)) {
            return;
        }

        // Prevent hunger loss during active games
        event.setCancelled(true);
        player.setFoodLevel(20);
        player.setSaturation(20);
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

