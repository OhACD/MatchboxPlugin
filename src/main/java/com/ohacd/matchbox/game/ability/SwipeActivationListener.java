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
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Activates an 8s swipe window when a Spark clicks a PAPER in slot 27 (above hotbar slot 0).
 * Silent by design (no messages/holograms).
 */
public class SwipeActivationListener implements Listener {
    private final GameManager gameManager;
    private final Plugin plugin;

    public SwipeActivationListener(GameManager gameManager, Plugin plugin) {
        this.gameManager = gameManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if right-clicking the swipe paper slot
        int slot = event.getSlot();
        int rawSlot = event.getRawSlot();
        
        // Slot 27 is above hotbar slot 0 (raw slot 27 in player inventory)
        if (slot != InventoryManager.getSwipeCurePaperSlot() && rawSlot != InventoryManager.getSwipeCurePaperSlot()) {
            return;
        }
        
        if (event.getClickedInventory() == null) return;
        if (event.getSlotType() == null) return;

        // Only allow activation with PAPER in that slot, and only during active game swipe phase
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() != Material.PAPER) return;
        if (!gameManager.getPhaseManager().isPhase(GamePhase.SWIPE)) return;
        if (gameManager.getGameState().getRole(player.getUniqueId()) != Role.SPARK) return;

        // Only allow right-click for activation
        if (!event.getClick().isRightClick()) {
            event.setCancelled(true);
            return;
        }

        // Consume the click (prevent moving the paper)
        event.setCancelled(true);

        // Start the 8 second swipe window silently
        gameManager.startSwipeWindow(player, 8);

        // schedule explicit removal to be safe (gameManager maintains removal internally but keep a cleanup)
        new BukkitRunnable() {
            @Override
            public void run() {
                gameManager.endSwipeWindow(player.getUniqueId());
            }
        }.runTaskLater(plugin, 8L * 20L);
    }
}

