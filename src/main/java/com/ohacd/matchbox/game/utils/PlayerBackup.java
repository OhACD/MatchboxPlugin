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
 */
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
        this.playerId = player.getUniqueId();
        PlayerInventory inv = player.getInventory();
        
        // Backup inventory
        this.inventoryContents = inv.getContents().clone();
        this.armorContents = inv.getArmorContents().clone();
        this.extraContents = inv.getExtraContents().clone();
        
        // Backup location
        this.location = player.getLocation().clone();
        
        // Backup game state
        this.gameMode = player.getGameMode();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.level = player.getLevel();
        this.exp = player.getExp();
    }

    /**
     * Restores the player's state from this backup.
     * Returns true if player was found and restored, false otherwise.
     */
    public boolean restore(Player player) {
        if (player == null || !player.getUniqueId().equals(playerId)) {
            return false;
        }

        if (!player.isOnline()) {
            return false;
        }

        PlayerInventory inv = player.getInventory();
        
        // Restore inventory
        inv.setContents(inventoryContents);
        inv.setArmorContents(armorContents);
        inv.setExtraContents(extraContents);
        
        // Restore location
        player.teleport(location);
        
        // Restore game state
        player.setGameMode(gameMode);
        player.setHealth(Math.min(health, player.getMaxHealth()));
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setLevel(level);
        player.setExp(exp);

        return true;
    }

    public UUID getPlayerId() {
        return playerId;
    }
}

