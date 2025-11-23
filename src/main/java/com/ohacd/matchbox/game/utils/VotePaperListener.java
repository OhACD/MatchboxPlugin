package com.ohacd.matchbox.game.utils;

import com.ohacd.matchbox.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles voting by right-clicking voting papers during the voting phase.
 */
public class VotePaperListener implements Listener {
    private final GameManager gameManager;

    public VotePaperListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player voter = (Player) event.getWhoClicked();
        
        // Only allow during voting phase
        if (!gameManager.getPhaseManager().isPhase(GamePhase.VOTING)) {
            return;
        }
        
        // Only allow right-click for voting
        if (!event.getClick().isRightClick()) {
            return;
        }
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !InventoryManager.isVotingPaper(clicked)) {
            return;
        }
        
        // Get target player name from paper
        String targetName = InventoryManager.getVotingPaperTarget(clicked);
        if (targetName == null) {
            return;
        }
        
        // Find target player
        Player target = org.bukkit.Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            return;
        }
        
        // Check if voter is alive
        if (!gameManager.getGameState().isAlive(voter.getUniqueId())) {
            return;
        }
        
        // Check if target is alive
        if (!gameManager.getGameState().isAlive(target.getUniqueId())) {
            return;
        }
        
        // Prevent moving the paper
        event.setCancelled(true);
        
        // Register the vote
        boolean success = gameManager.handleVote(voter, target);
        
        if (success) {
            // Remove the voting paper after successful vote
            voter.getInventory().setItem(event.getSlot(), null);
            voter.updateInventory();
        }
    }
}

