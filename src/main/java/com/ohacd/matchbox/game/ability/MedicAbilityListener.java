package com.ohacd.matchbox.game.ability;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.utils.Role;
import com.ohacd.matchbox.game.utils.GamePhase;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Activates an 8s cure window when a Medic clicks a PAPER in slot 9 (raw slot 9).
 * Silent by design (no messages/holograms).
 */
public class MedicAbilityListener implements Listener {
    private final GameManager gameManager;
    private final Plugin plugin;

    public MedicAbilityListener(GameManager gameManager, Plugin plugin) {
        this.gameManager = gameManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        // Only care about top-level player inventory clicks (rawSlot indexes player inventory)
        int raw = event.getRawSlot();
        if (raw != 9) return; // slot right above hotbar first slot
        if (event.getClickedInventory() == null) return;
        if (event.getSlotType() == null) return;

        Player player = (Player) event.getWhoClicked();

        // Only allow activation with PAPER in that slot, and only during active game swipe phase
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() != Material.PAPER) return;
        if (!gameManager.getPhaseManager().isPhase(GamePhase.SWIPE)) return;
        if (gameManager.getGameState().getRole(player.getUniqueId()) != Role.MEDIC) return;

        // Check if medic already cured this round
        if (gameManager.getGameState().hasCuredThisRound(player.getUniqueId())) {
            return; // Silent - already used cure this round
        }

        // Consume the click (prevent moving the paper)
        event.setCancelled(true);

        // Start the 8 second cure window silently
        gameManager.startCureWindow(player, 8);

        // schedule explicit removal to be safe (gameManager maintains removal internally but keep a cleanup)
        new BukkitRunnable() {
            @Override
            public void run() {
                gameManager.endCureWindow(player.getUniqueId());
            }
        }.runTaskLater(plugin, 8L * 20L);
    }
}

