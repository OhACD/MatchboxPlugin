package com.ohacd.matchbox.game.utils;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents players from moving or dropping game items.
 */
public class GameItemProtectionListener implements Listener {
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        // Check if clicking a game item
        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && InventoryManager.isGameItem(clicked)) {
            // Allow right-click for activation, but prevent moving
            if (event.getClick().isShiftClick() || event.getClick().isLeftClick()) {
                event.setCancelled(true);
                return;
            }
            // Right-click is allowed for ability activation
        }
        
        // Check if trying to move a game item
        ItemStack cursor = event.getCursor();
        if (cursor != null && InventoryManager.isGameItem(cursor)) {
            // Prevent moving game items
            if (event.getClick().isShiftClick() || event.getClick().isLeftClick()) {
                event.setCancelled(true);
                return;
            }
        }
        
        // Prevent moving items into game item slots
        int slot = event.getSlot();
        if (slot == InventoryManager.getRolePaperSlot() ||
            slot == InventoryManager.getSwipeCurePaperSlot() ||
            slot == InventoryManager.getVisionSightPaperSlot()) {
            // Only allow right-click on these slots (for ability activation)
            if (event.getClick().isShiftClick() || event.getClick().isLeftClick()) {
                event.setCancelled(true);
            }
        }
        
        // Prevent moving items into voting paper slots (0-6) during voting phase
        // But allow right-click for voting
        if (slot >= 0 && slot <= 6) {
            ItemStack item = event.getCurrentItem();
            if (item != null && InventoryManager.isVotingPaper(item)) {
                // Allow right-click for voting, prevent moving
                if (event.getClick().isShiftClick() || event.getClick().isLeftClick()) {
                    event.setCancelled(true);
                }
            }
        }
        
        // Prevent moving items into hotbar slots 7 and 8
        if (event.getInventory().getType() == InventoryType.PLAYER) {
            int rawSlot = event.getRawSlot();
            // Hotbar slots 7 and 8 in player inventory
            if (rawSlot == 43 || rawSlot == 44) {
                ItemStack item = event.getCursor();
                if (item != null && !InventoryManager.isGameItem(item)) {
                    // Prevent placing non-game items in these slots
                    event.setCancelled(true);
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (InventoryManager.isGameItem(item)) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        }
    }
}

