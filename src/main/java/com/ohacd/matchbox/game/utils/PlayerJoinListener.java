package com.ohacd.matchbox.game.utils;

import com.ohacd.matchbox.Matchbox;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.Duration;

/**
 * Handles welcome messages when players join the server.
 */
public class PlayerJoinListener implements Listener {
    private final Matchbox plugin;
    
    public PlayerJoinListener(Matchbox plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Show title welcome message
        Component title = Component.text("§6§lWelcome to Matchbox!");
        Component subtitle = Component.text("§7A Social Deduction Game for Minecraft");
        
        Title welcomeTitle = Title.title(
            title,
            subtitle,
            Title.Times.times(
                Duration.ofMillis(500),  // fade in
                Duration.ofSeconds(3),    // stay
                Duration.ofMillis(500)   // fade out
            )
        );
        
        player.showTitle(welcomeTitle);
        
        // Send welcome message after a short delay
        plugin.getServer().getScheduler().runTaskLater(
            plugin,
            () -> {
                if (player.isOnline()) {
                    player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    player.sendMessage("§e§lMatchbox §7- Social Deduction Game");
                    player.sendMessage("");
                    player.sendMessage("§7Matchbox is a §e7-player social deduction game §7where");
                    player.sendMessage("§7players must work together to identify the §cSpark §7(impostor)");
                    player.sendMessage("§7while the Spark tries to eliminate everyone.");
                    player.sendMessage("");
                    player.sendMessage("§7Current Version: §e0.9.1 §7(Config and QOL Update)");
                    player.sendMessage("§7Status: §aStable §7- Ready for gameplay");
                    player.sendMessage("");
                    player.sendMessage("§7Found a bug or have suggestions?");
                    player.sendMessage("§7Join our Discord: §9§nhttps://discord.gg/BTDP3APfq8");
                    player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                }
            },
            40L // 2 seconds delay (20 ticks = 1 second)
        );
    }
}

