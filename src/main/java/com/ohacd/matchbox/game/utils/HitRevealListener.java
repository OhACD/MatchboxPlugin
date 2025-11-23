package com.ohacd.matchbox.game.utils;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.hologram.HologramManager;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Handles arrow hits for nametag revelation.
 * Delinked from swipe ability - arrows only reveal nametags now.
 * One arrow per round per player.
 */
public class HitRevealListener implements Listener {
    private final GameManager gameManager;
    private final HologramManager hologramManager;
    private final InventoryManager inventoryManager;

    public HitRevealListener(GameManager gameManager, HologramManager hologramManager, InventoryManager inventoryManager) {
        this.gameManager = gameManager;
        this.hologramManager = hologramManager;
        this.inventoryManager = inventoryManager;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        
        Player target = (Player) event.getEntity();
        
        // Get session context for this player
        com.ohacd.matchbox.game.SessionGameContext context = gameManager.getContextForPlayer(shooter.getUniqueId());
        if (context == null) {
            return; // Player not in any active game
        }
        
        // Game must be active
        if (!context.getGameState().isGameActive()) {
            return;
        }
        
        // Only work during swipe phase
        if (!context.getPhaseManager().isPhase(GamePhase.SWIPE)) {
            return;
        }
        
        // Check if shooter has used their arrow this round
        if (inventoryManager.hasUsedArrow(shooter.getUniqueId())) {
            return; // Already used arrow this round
        }
        
        // Check if shooter is alive and in the game
        if (!context.getGameState().isAlive(shooter.getUniqueId())) {
            return;
        }
        
        // Check if target is alive and in the game
        if (!context.getGameState().isAlive(target.getUniqueId())) {
            return;
        }
        
        // Mark arrow as used
        inventoryManager.markArrowUsed(shooter.getUniqueId());
        
        // Reveal player identity for 10 seconds (200 ticks)
        hologramManager.showTextAbove(target, target.getName(), 200);
        
        // Remove arrow from inventory (they used it)
        shooter.getInventory().setItem(InventoryManager.getArrowHotbarSlot(), null);
        shooter.updateInventory();
    }
}
