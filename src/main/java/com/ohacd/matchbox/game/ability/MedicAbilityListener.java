package com.ohacd.matchbox.game.ability;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Activates an 8s cure window when a Medic clicks a PAPER in slot 27 (above hotbar slot 0).
 * Supports right-click and left-click in inventory, and right-click when held in main hand.
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
        
        Player player = (Player) event.getWhoClicked();
        
        // Get session context for this player
        SessionGameContext context = gameManager.getContextForPlayer(player.getUniqueId());
        if (context == null) {
            return; // Player not in any active game
        }
        
        // Game must be active
        if (!context.getGameState().isGameActive()) {
            return;
        }
        
        // Check if clicking the cure paper slot
        int slot = event.getSlot();
        int rawSlot = event.getRawSlot();
        
        // Slot 27 is above hotbar slot 0 (raw slot 27 in player inventory)
        if (slot != InventoryManager.getSwipeCurePaperSlot() && rawSlot != InventoryManager.getSwipeCurePaperSlot()) {
            return;
        }
        
        if (event.getClickedInventory() == null) return;
        if (event.getSlotType() == null) return;

        // Only allow activation with PAPER in that slot, and only during active game swipe phase
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PAPER) return;
        if (!context.getPhaseManager().isPhase(GamePhase.SWIPE)) return;
        if (context.getGameState().getRole(player.getUniqueId()) != Role.MEDIC) return;

        // Check if medic already cured this round
        if (context.getGameState().hasCuredThisRound(player.getUniqueId())) {
            return; // Silent - already used cure this round
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

        // Start the 8 second cure window silently
        Long windowExpiry = gameManager.startCureWindow(player, 8);
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
                Long currentExpiry = ctx.getActiveCureWindow().get(player.getUniqueId());
                if (currentExpiry != null && currentExpiry.equals(windowExpiry)) {
                    gameManager.endCureWindow(player.getUniqueId());
                    gameManager.restoreAbilityPaper(player);
                }
            }
        }.runTaskLater(plugin, 8L * 20L);
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
        SessionGameContext context = gameManager.getContextForPlayer(player.getUniqueId());
        if (context == null) {
            return; // Player not in any active game
        }
        
        // Game must be active
        if (!context.getGameState().isGameActive()) {
            return;
        }
        
        // Check if it's the cure paper by checking slot 27
        ItemStack slot27Item = player.getInventory().getItem(InventoryManager.getSwipeCurePaperSlot());
        if (slot27Item == null || slot27Item.getType() != Material.PAPER || !slot27Item.equals(heldItem)) {
            return;
        }
        
        if (!context.getPhaseManager().isPhase(GamePhase.SWIPE)) {
            return;
        }
        if (context.getGameState().getRole(player.getUniqueId()) != Role.MEDIC) {
            return;
        }
        
        // Check if medic already cured this round
        if (context.getGameState().hasCuredThisRound(player.getUniqueId())) {
            return; // Silent - already used cure this round
        }
        
        // Prevent interaction
        event.setCancelled(true);
        
        // Start the 8 second cure window silently
        Long windowExpiry = gameManager.startCureWindow(player, 8);
        if (windowExpiry == null) {
            return;
        }
        
        // Replace paper with gray dye indicator in slot 27
        ItemStack usedIndicator = InventoryManager.createUsedIndicator(heldItem);
        player.getInventory().setItem(InventoryManager.getSwipeCurePaperSlot(), usedIndicator);
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
                Long currentExpiry = ctx.getActiveCureWindow().get(player.getUniqueId());
                if (currentExpiry != null && currentExpiry.equals(windowExpiry)) {
                    gameManager.endCureWindow(player.getUniqueId());
                    gameManager.restoreAbilityPaper(player);
                }
            }
        }.runTaskLater(plugin, 8L * 20L);
    }
}

