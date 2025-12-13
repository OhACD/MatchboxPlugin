package com.ohacd.matchbox.game.ability;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.utils.Role;
import com.ohacd.matchbox.game.utils.Managers.InventoryManager;
import com.ohacd.matchbox.game.utils.GamePhase;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;


/**
 * Activates an 8s delusion window when a Spark clicks a PAPER in slot 28 (above hotbar slot 1).
 * Supports right-click and left-click in inventory, and right-click when held in main hand.
 * Silent by design (no messages/holograms).
 */
public class DelusionActivationListener implements AbilityHandler {
    private final GameManager gameManager;
    private final Plugin plugin;

    public DelusionActivationListener(GameManager gameManager, Plugin plugin) {
        this.gameManager = gameManager;
        this.plugin = plugin;
    }

    @Override
    public void handleInventoryClick(InventoryClickEvent event, SessionGameContext context) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if clicking the delusion paper slot
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
        
        // Check if delusion ability is active for this round
        if (context.getGameState().getSparkSecondaryAbility() != SparkSecondaryAbility.DELUSION) {
            return;
        }

        // Check if spark already used delusion this round
        if (context.getGameState().hasUsedDelusionThisRound(player.getUniqueId())) {
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

        // Start the 8 second delusion window silently
        Long windowExpiry = gameManager.startDelusionWindow(player, 8);
        if (windowExpiry == null) {
            return;
        }

        // Replace paper with gray dye indicator
        ItemStack usedIndicator = InventoryManager.createUsedIndicator(clicked);
        player.getInventory().setItem(slot, usedIndicator);
        player.updateInventory();

        // schedule cleanup that restores the paper if unused
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                SessionGameContext ctx = gameManager.getContextForPlayer(player.getUniqueId());
                if (ctx == null) {
                    return;
                }
                Long currentExpiry = ctx.getActiveDelusionWindow().get(player.getUniqueId());
                if (currentExpiry != null && currentExpiry.equals(windowExpiry)) {
                    gameManager.endDelusionWindow(player.getUniqueId());
                    gameManager.restoreSecondaryAbilityPaper(player);
                }
            }
        }.runTaskLater(plugin, 20L * 8); // 8 seconds
    }
    
    @Override
    public void handlePlayerInteract(PlayerInteractEvent event, SessionGameContext context) {
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
        
        // Check if it's the delusion paper by checking slot 28
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
        
        // Check if delusion ability is active for this round
        if (context.getGameState().getSparkSecondaryAbility() != SparkSecondaryAbility.DELUSION) {
            return;
        }
        
        // Check if spark already used delusion this round
        if (context.getGameState().hasUsedDelusionThisRound(player.getUniqueId())) {
            return; // Silent - already used this round
        }
        
        // Prevent interaction
        event.setCancelled(true);
        
        // Start the 8 second delusion window silently
        Long windowExpiry = gameManager.startDelusionWindow(player, 8);
        if (windowExpiry == null) {
            return;
        }
        
        // Replace paper with gray dye indicator in slot 28
        ItemStack usedIndicator = InventoryManager.createUsedIndicator(heldItem);
        player.getInventory().setItem(InventoryManager.getVisionSightPaperSlot(), usedIndicator);
        player.updateInventory();

        // schedule cleanup that restores the paper if unused
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                SessionGameContext ctx = gameManager.getContextForPlayer(player.getUniqueId());
                if (ctx == null) {
                    return;
                }
                Long currentExpiry = ctx.getActiveDelusionWindow().get(player.getUniqueId());
                if (currentExpiry != null && currentExpiry.equals(windowExpiry)) {
                    gameManager.endDelusionWindow(player.getUniqueId());
                    gameManager.restoreSecondaryAbilityPaper(player);
                }
            }
        }.runTaskLater(plugin, 20L * 8); // 8 seconds
    }
}

