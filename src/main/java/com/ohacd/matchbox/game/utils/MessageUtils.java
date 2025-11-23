package com.ohacd.matchbox.game.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Utility class for sending messages to players and broadcasting.
 */
public class MessageUtils {
    private final Plugin plugin;

    public MessageUtils(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Sends an action bar message to a player.
     */
    public void sendActionBar(Player player, String message) {
        try {
            player.sendActionBar(Component.text(message));
        } catch (NoSuchMethodError | NoClassDefFoundError error) {
            player.sendActionBar(message);
        }
    }

    /**
     * Broadcasts a message to all players on the server.
     */
    public void broadcast(String message) {
        plugin.getServer().broadcastMessage(message);
    }

    /**
     * Sends a plain message to all players on the server.
     */
    public void sendPlainMessage(String message) {
        plugin.getServer().sendPlainMessage(message);
    }
}
