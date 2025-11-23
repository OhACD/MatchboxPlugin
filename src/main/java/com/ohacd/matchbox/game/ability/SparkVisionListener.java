package com.ohacd.matchbox.game.ability;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.utils.Role;
import com.ohacd.matchbox.game.utils.GamePhase;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Activates Hunter Vision when a Spark clicks a PAPER in slot 8 (raw slot 8).
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
        // Only care about top-level player inventory clicks (rawSlot indexes player inventory)
        int raw = event.getRawSlot();
        if (raw != 8) return; // slot 8 (different from swipe which is slot 9)
        if (event.getClickedInventory() == null) return;
        if (event.getSlotType() == null) return;

        Player player = (Player) event.getWhoClicked();

        // Only allow activation with PAPER in that slot, and only during active game swipe phase
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() != Material.PAPER) return;
        if (!gameManager.getPhaseManager().isPhase(GamePhase.SWIPE)) return;
        if (gameManager.getGameState().getRole(player.getUniqueId()) != Role.SPARK) return;

        // Check if spark already used hunter vision this round
        if (gameManager.getGameState().hasUsedHunterVisionThisRound(player.getUniqueId())) {
            return; // Silent - already used this round
        }

        // Consume the click (prevent moving the paper)
        event.setCancelled(true);

        // Activate hunter vision (shows glow on all alive players for 15 seconds)
        gameManager.activateHunterVision(player);
    }
}

