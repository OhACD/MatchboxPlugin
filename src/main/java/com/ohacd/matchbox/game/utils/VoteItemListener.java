package com.ohacd.matchbox.game.utils;

import com.ohacd.matchbox.game.GameManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles voting by right-clicking on players during the voting phase.
 * Works when player is holding a voting paper in their main hand.
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

        // Check if voter is holding a voting paper in their main hand
        ItemStack heldItem = voter.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() != Material.PAPER || !InventoryManager.isVotingPaper(heldItem)) {
            return;
        }

        // Get target player name from paper
        String targetName = InventoryManager.getVotingPaperTarget(heldItem);
        if (targetName == null || !targetName.equals(target.getName())) {
            // Paper doesn't match the clicked player - ignore
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

        // Register the vote
        boolean success = gameManager.handleVote(voter, target);
        
        if (success) {
            // Prevent any other interaction side-effects
            event.setCancelled(true);
            
            // Remove the voting paper from main hand after successful vote
            voter.getInventory().setItemInMainHand(null);
            voter.updateInventory();
        }
    }
}
