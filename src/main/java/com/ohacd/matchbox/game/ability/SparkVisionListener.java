package com.ohacd.matchbox.game.ability;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.utils.Role;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.InventoryManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Activates Hunter Vision when a Spark clicks a PAPER in slot 28 (above hotbar slot 1).
 * Shows glow effect on all alive players for 15 seconds (only visible to spark, works through walls).
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
        
        // Check if right-clicking the vision paper slot
        int slot = event.getSlot();
        int rawSlot = event.getRawSlot();
        
        // Slot 28 is above hotbar slot 1 (raw slot 28 in player inventory)
        if (slot != InventoryManager.getVisionSightPaperSlot() && rawSlot != InventoryManager.getVisionSightPaperSlot()) {
            return;
        }
        
        if (event.getClickedInventory() == null) return;
        if (event.getSlotType() == null) return;

        // Only allow activation with PAPER in that slot, and only during active game swipe phase
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() != Material.PAPER) return;
        if (!gameManager.getPhaseManager().isPhase(GamePhase.SWIPE)) return;
        if (gameManager.getGameState().getRole(player.getUniqueId()) != Role.SPARK) return;

        // Check if spark already used hunter vision this round
        if (gameManager.getGameState().hasUsedHunterVisionThisRound(player.getUniqueId())) {
            return; // Silent - already used this round
        }

        // Only allow right-click for activation
        if (!event.getClick().isRightClick()) {
            event.setCancelled(true);
            return;
        }

        // Consume the click (prevent moving the paper)
        event.setCancelled(true);

        // Activate hunter vision (shows glow on all alive players for 15 seconds)
        gameManager.activateHunterVision(player);
    }
}

