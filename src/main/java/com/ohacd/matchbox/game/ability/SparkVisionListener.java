package com.ohacd.matchbox.game.ability;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.utils.Role;
import com.ohacd.matchbox.game.utils.Managers.InventoryManager;
import com.ohacd.matchbox.game.utils.GamePhase;

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
        SessionGameContext context = gameManager.getContextForPlayer(player.getUniqueId());
        if (context == null) return;
        if (!context.getGameState().isGameActive()) return;

        int slot = event.getSlot();
        int rawSlot = event.getRawSlot();
        if (slot != InventoryManager.getVisionSightPaperSlot() && rawSlot != InventoryManager.getVisionSightPaperSlot()) return;
        if (event.getClickedInventory() == null) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PAPER) return;
        if (!context.getPhaseManager().isPhase(GamePhase.SWIPE)) return;
        if (context.getGameState().getRole(player.getUniqueId()) != Role.SPARK) return;
        if (context.getGameState().hasUsedHunterVisionThisRound(player.getUniqueId())) return;

        event.setCancelled(true);
        gameManager.activateHunterVision(player);

        ItemStack usedIndicator = InventoryManager.createUsedIndicator(clicked);
        player.getInventory().setItem(slot, usedIndicator);
        player.updateInventory();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() != Material.PAPER) return;

        SessionGameContext context = gameManager.getContextForPlayer(player.getUniqueId());
        if (context == null) return;
        if (!context.getGameState().isGameActive()) return;

        ItemStack slot28Item = player.getInventory().getItem(InventoryManager.getVisionSightPaperSlot());
        if (slot28Item == null || slot28Item.getType() != Material.PAPER || !slot28Item.equals(heldItem)) return;
        if (!context.getPhaseManager().isPhase(GamePhase.SWIPE)) return;
        if (context.getGameState().getRole(player.getUniqueId()) != Role.SPARK) return;
        if (context.getGameState().hasUsedHunterVisionThisRound(player.getUniqueId())) return;

        event.setCancelled(true);
        gameManager.activateHunterVision(player);

        ItemStack usedIndicator = InventoryManager.createUsedIndicator(heldItem);
        player.getInventory().setItem(InventoryManager.getVisionSightPaperSlot(), usedIndicator);
        player.updateInventory();
    }
}
