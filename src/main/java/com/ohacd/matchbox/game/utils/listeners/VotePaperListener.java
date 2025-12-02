package com.ohacd.matchbox.game.utils.listeners;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.Managers.InventoryManager;

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
        
        // Get session context for this player
        SessionGameContext context = gameManager.getContextForPlayer(voter.getUniqueId());
        if (context == null) {
            return; // Player not in any active game
        }
        
        // Game must be active
        if (!context.getGameState().isGameActive()) {
            return;
        }
        
        // Only allow during voting phase
        if (!context.getPhaseManager().isPhase(GamePhase.VOTING)) {
            return;
        }
        
        // Allow both left-click and right-click for voting in inventory
        boolean isRightClick = event.getClick().isRightClick();
        boolean isLeftClick = event.getClick().isLeftClick();
        if (!isRightClick && !isLeftClick) {
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
        if (!context.getGameState().isAlive(voter.getUniqueId())) {
            return;
        }
        
        // Check if target is alive
        if (!context.getGameState().isAlive(target.getUniqueId())) {
            return;
        }
        
        // Prevent moving the paper
        event.setCancelled(true);
        
        // Register the vote
        boolean success = gameManager.handleVote(voter, target);
        
        if (success) {
            // Replace voting paper with gray dye indicator
            ItemStack usedIndicator = InventoryManager.createUsedIndicator(clicked);
            voter.getInventory().setItem(event.getSlot(), usedIndicator);
            voter.updateInventory();
        }
    }
}

