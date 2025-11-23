package com.ohacd.matchbox.game.utils;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;

/**
 * Stores a backup of a player's state (inventory, location, game mode, etc.)
 * for restoration when the game ends.
 * Uses deprecated getMaxHealth() method for compatibility.
 */
@SuppressWarnings("deprecation")
public class PlayerBackup {
    private final UUID playerId;
    private final ItemStack[] inventoryContents;
    private final ItemStack[] armorContents;
    private final ItemStack[] extraContents;
    private final Location location;
    private final GameMode gameMode;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final int level;
    private final float exp;

    public PlayerBackup(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        
        this.playerId = player.getUniqueId();
        PlayerInventory inv = player.getInventory();
        
        if (inv == null) {
            throw new IllegalStateException("Player inventory is null");
        }
        
        // Backup inventory (defensive cloning)
        ItemStack[] contents = inv.getContents();
        this.inventoryContents = contents != null ? contents.clone() : new ItemStack[0];
        
        ItemStack[] armor = inv.getArmorContents();
        this.armorContents = armor != null ? armor.clone() : new ItemStack[0];
        
        ItemStack[] extra = inv.getExtraContents();
        this.extraContents = extra != null ? extra.clone() : new ItemStack[0];
        
        // Backup location (defensive cloning)
        Location loc = player.getLocation();
        if (loc == null || loc.getWorld() == null) {
            throw new IllegalStateException("Player location or world is null");
        }
        this.location = loc.clone();
        
        // Backup game state
        this.gameMode = player.getGameMode();
        this.health = Math.max(0, Math.min(player.getHealth(), player.getMaxHealth()));
        this.foodLevel = Math.max(0, Math.min(player.getFoodLevel(), 20));
        this.saturation = Math.max(0, player.getSaturation());
        this.level = Math.max(0, player.getLevel());
        this.exp = Math.max(0, Math.min(player.getExp(), 1.0f));
    }

    /**
     * Restores the player's state from this backup.
     * Returns true if player was found and restored, false otherwise.
     */
    public boolean restore(Player player) {
        if (player == null) {
            return false;
        }
        
        if (!player.getUniqueId().equals(playerId)) {
            return false;
        }

        if (!player.isOnline()) {
            return false;
        }

        try {
            PlayerInventory inv = player.getInventory();
            if (inv == null) {
                return false;
            }
            
            // Restore inventory (defensive null checks)
            if (inventoryContents != null) {
                inv.setContents(inventoryContents);
            }
            if (armorContents != null) {
                inv.setArmorContents(armorContents);
            }
            if (extraContents != null) {
                inv.setExtraContents(extraContents);
            }
            
            // Restore location (defensive checks)
            if (location != null && location.getWorld() != null) {
                player.teleport(location);
            }
            
            // Restore game state (defensive bounds checking)
            if (gameMode != null) {
                player.setGameMode(gameMode);
            }
            player.setHealth(Math.min(Math.max(0, health), player.getMaxHealth()));
            player.setFoodLevel(Math.min(Math.max(0, foodLevel), 20));
            player.setSaturation(Math.max(0, saturation));
            player.setLevel(Math.max(0, level));
            player.setExp(Math.min(Math.max(0, exp), 1.0f));

            return true;
        } catch (Exception e) {
            // Log error but don't throw - return false to indicate failure
            return false;
        }
    }

    public UUID getPlayerId() {
        return playerId;
    }
}

