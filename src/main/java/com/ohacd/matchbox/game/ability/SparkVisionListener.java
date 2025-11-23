package com.ohacd.matchbox.game.ability;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.utils.Role;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.InventoryManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Activates Hunter Vision when a Spark clicks a PAPER in slot 28 (above hotbar slot 1).
 * Supports right-click and left-click in inventory, and right-click when held in main hand.
 * Shows particles on all alive players for 15 seconds (only visible to spark).
 * Spark cannot see nametags - only particles are shown.
 * Can only be used once per round.
 * Silent by design (no messages/holograms).
 */
public class SparkVisionListener implements Listener {
    private final GameManager gameManager;

    public SparkVisionListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // Get session context for this player
        com.ohacd.matchbox.game.SessionGameContext context = gameManager.getContextForPlayer(player.getUniqueId());
        if (context == null) {
            return; // Player not in any active game
        }
        
        // Game must be active
        if (!context.getGameState().isGameActive()) {
            return;
        }
        
        // Check if clicking the vision paper slot
        int slot = event.getSlot();
        int rawSlot = event.getRawSlot();
        
        // Slot 28 is above hotbar slot 1 (raw slot 28 in player inventory)
        if (slot != InventoryManager.getVisionSightPaperSlot() && rawSlot != InventoryManager.getVisionSightPaperSlot()) {
            return;
        }
        
        if (event.getClickedInventory() == null) return;
        if (event.getSlotType() == null) return;

        // Only allow activation with PAPER in that slot, and only during active game swipe phase
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PAPER) return;
        if (!context.getPhaseManager().isPhase(GamePhase.SWIPE)) return;
        if (context.getGameState().getRole(player.getUniqueId()) != Role.SPARK) return;

        // Check if spark already used hunter vision this round
        if (context.getGameState().hasUsedHunterVisionThisRound(player.getUniqueId())) {
            return; // Silent - already used this round
        }

        // Allow both left-click and right-click for activation
        boolean isRightClick = event.getClick().isRightClick();
        boolean isLeftClick = event.getClick().isLeftClick();
        if (!isRightClick && !isLeftClick) {
            event.setCancelled(true);
            return;
        }

        // Consume the click (prevent moving the paper)
        event.setCancelled(true);

        // Activate hunter vision (shows glow on all alive players for 15 seconds)
        gameManager.activateHunterVision(player);
        
        // Replace paper with gray dye indicator
        ItemStack usedIndicator = InventoryManager.createUsedIndicator(clicked);
        player.getInventory().setItem(slot, usedIndicator);
        player.updateInventory();
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle right-click with item in hand (not block interactions)
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        
        // Check if holding paper in main hand
        if (heldItem == null || heldItem.getType() != Material.PAPER) {
            return;
        }
        
        // Get session context for this player
        com.ohacd.matchbox.game.SessionGameContext context = gameManager.getContextForPlayer(player.getUniqueId());
        if (context == null) {
            return; // Player not in any active game
        }
        
        // Game must be active
        if (!context.getGameState().isGameActive()) {
            return;
        }
        
        // Check if it's the vision paper by checking slot 28
        ItemStack slot28Item = player.getInventory().getItem(InventoryManager.getVisionSightPaperSlot());
        if (slot28Item == null || slot28Item.getType() != Material.PAPER || !slot28Item.equals(heldItem)) {
            return;
        }
        
        if (!context.getPhaseManager().isPhase(GamePhase.SWIPE)) {
            return;
        }
        if (context.getGameState().getRole(player.getUniqueId()) != Role.SPARK) {
            return;
        }
        
        // Check if spark already used hunter vision this round
        if (context.getGameState().hasUsedHunterVisionThisRound(player.getUniqueId())) {
            return; // Silent - already used this round
        }
        
        // Prevent interaction
        event.setCancelled(true);
        
        // Activate hunter vision (shows glow on all alive players for 15 seconds)
        gameManager.activateHunterVision(player);
        
        // Replace paper with gray dye indicator in slot 28
        ItemStack usedIndicator = InventoryManager.createUsedIndicator(heldItem);
        player.getInventory().setItem(InventoryManager.getVisionSightPaperSlot(), usedIndicator);
        player.updateInventory();
    }
}

