package com.ohacd.matchbox.game.utils;

import com.ohacd.matchbox.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player disconnections during active games.
 */
public class PlayerQuitListener implements Listener {
    private final GameManager gameManager;

    public PlayerQuitListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // If player is in an active game, restore their nametag
        if (gameManager.getGameState().getAllParticipatingPlayerIds().contains(player.getUniqueId())) {
            NameTagManager.showNameTag(player);

            // Remove them from the game state
            gameManager.getGameState().removeAlivePlayer(player.getUniqueId());
        }
    }
}