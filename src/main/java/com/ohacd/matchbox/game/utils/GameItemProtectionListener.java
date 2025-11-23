package com.ohacd.matchbox.game.utils;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents players from moving or dropping game items.
 * Game items are completely locked in place - no moving, dropping, hotkeying, or shift-clicking.
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
            // For voting papers, allow right-click ONLY during voting phase
            if (InventoryManager.isVotingPaper(clicked)) {
                // Right-click is handled by VotePaperListener - prevent all other interactions
                if (!event.getClick().isRightClick()) {
                    event.setCancelled(true);
                    return;
                }
                // Right-click will be handled by VotePaperListener
                return;
            }
            
            // For all other game items (role paper, ability papers, crossbow, arrow):
            // Prevent ALL interactions except right-click for ability activation
            // This includes: left-click, shift-click, number key (hotkey), middle-click, etc.
            if (!event.getClick().isRightClick() || event.getClick().isShiftClick() || 
                event.getClick().isKeyboardClick() || event.getClick().isCreativeAction()) {
                event.setCancelled(true);
                return;
            }
            // Right-click is allowed for ability activation (handled by ability listeners)
        }
        
        // Check if trying to move a game item with cursor
        ItemStack cursor = event.getCursor();
        if (cursor != null && InventoryManager.isGameItem(cursor)) {
            // Prevent ALL movement of game items (including right-click moving)
            event.setCancelled(true);
            return;
        }
        
        // Prevent hotkey swapping (number keys) with game items
        if (event.getClick().isKeyboardClick() || event.getClick().isShiftClick()) {
            // Check if the item being moved is a game item
            if (clicked != null && InventoryManager.isGameItem(clicked)) {
                event.setCancelled(true);
                return;
            }
            // Check if trying to swap with a game item in hotbar
            if (event.getHotbarButton() >= 0) {
                ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                if (hotbarItem != null && InventoryManager.isGameItem(hotbarItem)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        
        // Prevent moving items into game item slots
        int slot = event.getSlot();
        if (slot == InventoryManager.getRolePaperSlot() ||
            slot == InventoryManager.getSwipeCurePaperSlot() ||
            slot == InventoryManager.getVisionSightPaperSlot() ||
            slot == InventoryManager.getCrossbowHotbarSlot() ||
            slot == InventoryManager.getArrowHotbarSlot()) {
            // If there's a cursor item (trying to place something), always prevent
            if (cursor != null && !InventoryManager.isGameItem(cursor)) {
                event.setCancelled(true);
                return;
            }
            // If clicking on an empty slot with a game item in cursor, prevent
            if (clicked == null && cursor != null && InventoryManager.isGameItem(cursor)) {
                event.setCancelled(true);
                return;
            }
            // For right-click activation, only allow if no cursor item and clicking existing game item
            if (event.getClick().isRightClick() && cursor == null && clicked != null && InventoryManager.isGameItem(clicked)) {
                // Allow - will be handled by ability listeners
                return;
            }
            // Prevent all other interactions
            if (!event.getClick().isRightClick() || cursor != null) {
                event.setCancelled(true);
            }
        }
        
        // Prevent moving items into voting paper slots (0-6) during voting phase
        if (slot >= 0 && slot <= 6) {
            ItemStack item = event.getCurrentItem();
            if (item != null && InventoryManager.isVotingPaper(item)) {
                // Allow right-click for voting, prevent all other interactions
                if (!event.getClick().isRightClick()) {
                    event.setCancelled(true);
                }
            }
            // Prevent placing items in voting slots
            if (cursor != null && !InventoryManager.isVotingPaper(cursor)) {
                event.setCancelled(true);
            }
        }
        
        // Prevent moving items into hotbar slots 7 and 8 (crossbow and arrow)
        if (event.getInventory().getType() == InventoryType.PLAYER) {
            int rawSlot = event.getRawSlot();
            // Hotbar slots 7 and 8 in player inventory (raw slots 43 and 44)
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
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        // Prevent dragging game items
        ItemStack dragged = event.getOldCursor();
        if (dragged != null && InventoryManager.isGameItem(dragged)) {
            event.setCancelled(true);
            return;
        }
        
        // Prevent dragging items into game item slots
        for (int slot : event.getRawSlots()) {
            if (slot == InventoryManager.getRolePaperSlot() ||
                slot == InventoryManager.getSwipeCurePaperSlot() ||
                slot == InventoryManager.getVisionSightPaperSlot() ||
                slot == InventoryManager.getCrossbowHotbarSlot() ||
                slot == InventoryManager.getArrowHotbarSlot() ||
                (slot >= 0 && slot <= 6)) { // Voting paper slots
                event.setCancelled(true);
                return;
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
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        // Prevent swapping if either hand has a game item
        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();
        
        if ((mainHand != null && InventoryManager.isGameItem(mainHand)) ||
            (offHand != null && InventoryManager.isGameItem(offHand))) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        }
    }
}

