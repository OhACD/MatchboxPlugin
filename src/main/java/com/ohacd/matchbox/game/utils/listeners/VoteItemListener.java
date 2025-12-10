package com.ohacd.matchbox.game.utils.listeners;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.Managers.InventoryManager;
import com.ohacd.matchbox.game.utils.PlayerNameUtils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Handles voting by right-clicking on players or right-clicking when holding voting paper.
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

        // Check if voter is holding a voting paper in their main hand
        ItemStack heldItem = voter.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() != Material.PAPER || !InventoryManager.isVotingPaper(heldItem)) {
            return;
        }

        UUID targetId = InventoryManager.getVotingPaperTargetId(heldItem);
        String targetDisplay = InventoryManager.getVotingPaperTargetDisplay(heldItem);
        boolean matchesUuid = targetId != null && targetId.equals(target.getUniqueId());
        boolean matchesDisplay = targetDisplay != null && targetDisplay.equals(PlayerNameUtils.displayName(target));
        if (!matchesUuid && !matchesDisplay) {
            return; // Paper doesn't match clicked player
        }

        // Check if voter is alive
        if (!context.getGameState().isAlive(voter.getUniqueId())) {
            return;
        }

        // Check if target is alive
        if (!context.getGameState().isAlive(target.getUniqueId())) {
            return;
        }

        // Register the vote
        boolean success = gameManager.handleVote(voter, target);
        
        if (success) {
            // Prevent any other interaction side-effects
            event.setCancelled(true);
            
            // Replace voting paper with gray dye indicator
            ItemStack usedIndicator = InventoryManager.createUsedIndicator(heldItem);
            voter.getInventory().setItemInMainHand(usedIndicator);
            voter.updateInventory();
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle right-click with item in hand (not block interactions)
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player voter = event.getPlayer();
        ItemStack heldItem = voter.getInventory().getItemInMainHand();
        
        // Check if holding a voting paper
        if (heldItem == null || heldItem.getType() != Material.PAPER || !InventoryManager.isVotingPaper(heldItem)) {
            return;
        }
        
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
        
        UUID targetId = InventoryManager.getVotingPaperTargetId(heldItem);
        String targetDisplay = InventoryManager.getVotingPaperTargetDisplay(heldItem);
        Player target = null;
        if (targetId != null) {
            target = Bukkit.getPlayer(targetId);
        }
        if (target == null && targetDisplay != null) {
            target = Bukkit.getOnlinePlayers().stream()
                .filter(p -> targetDisplay.equals(PlayerNameUtils.displayName(p)))
                .findFirst()
                .orElse(null);
        }
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

        // Prevent interaction
        event.setCancelled(true);

        // Register the vote
        boolean success = gameManager.handleVote(voter, target);
        
        if (success) {
            // Replace voting paper with gray dye indicator
            ItemStack usedIndicator = InventoryManager.createUsedIndicator(heldItem);
            voter.getInventory().setItemInMainHand(usedIndicator);
            voter.updateInventory();
        }
    }
}
