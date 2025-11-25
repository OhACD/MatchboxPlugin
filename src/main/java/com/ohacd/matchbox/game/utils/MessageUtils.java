package com.ohacd.matchbox.game.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Collection;

/**
 * Utility class for sending messages to players and broadcasting.
 * Uses deprecated methods as fallbacks for compatibility.
 */
@SuppressWarnings("deprecation")
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
     * Sends a title to a player.
     */
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            Title.Times times = Title.Times.times(
                    Duration.ofMillis(fadeIn * 50L),
                    Duration.ofMillis(stay * 50L),
                    Duration.ofMillis(fadeOut * 50L)
            );

            Component titleComponent = Component.text(title);
            Component subtitleComponent = subtitle != null ? Component.text(subtitle) : Component.empty();

            Title titleObj = Title.title(titleComponent, subtitleComponent, times);
            player.showTitle(titleObj);
        } catch (NoSuchMethodError | NoClassDefFoundError error) {
            // Fallback for older APIs
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    /**
     * Sends a title to multiple players.
     */
    public void sendTitle(Collection<Player> players, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player player : players) {
            sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
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

    /**
     * Sends a plain chat message to a single player, falling back gracefully if the API call fails.
     */
    public void sendPlayerMessage(Player player, String message) {
        if (player == null || message == null) {
            return;
        }
        try {
            player.sendMessage(message);
        } catch (Exception ignored) {
            // swallow – message delivery isn't critical enough to interrupt gameplay
        }
    }
}