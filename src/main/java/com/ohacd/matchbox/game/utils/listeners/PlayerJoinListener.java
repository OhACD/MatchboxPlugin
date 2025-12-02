package com.ohacd.matchbox.game.utils.listeners;

import com.ohacd.matchbox.Matchbox;
import com.ohacd.matchbox.game.utils.CheckProjectVersion;
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
    private final CheckProjectVersion versionChecker;
    // Keeps track of the version and the current status of the project
    private final String currentVersion = Matchbox.getInstance().getCurrentVersion();
    private final String projectStatus = Matchbox.getInstance().getProjectStatus();
    private final String updateName = Matchbox.getInstance().getUpdateName();

    public PlayerJoinListener(Matchbox plugin, CheckProjectVersion versionChecker) {
        this.plugin = plugin;
        this.versionChecker = versionChecker;
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
                    player.sendMessage("§7Current Version: §e" + currentVersion + " §7(" + updateName + ")");
                    player.sendMessage("§7Status: §d" + projectStatus); // " §7- Ready for gameplay"
                    player.sendMessage("");
                    player.sendMessage("§7Found a bug or have suggestions?");
                    player.sendMessage("§7Join our Discord: §9§nhttps://discord.gg/BTDP3APfq8");
                    player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                }
            },
            40L // 2 seconds delay (20 ticks = 1 second)
        );

        // Checks the version that the player is running against the latest project version and notify the player
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {
                    versionChecker.checkLatestVersion(latest -> {
                        if (latest == null) {
                            player.sendMessage("§cCould not fetch version info.");
                            return;
                        }

                        if (currentVersion.equalsIgnoreCase(latest)) {
                            player.sendMessage("§aYou're all good and running the latest version! (" + latest + ")");
                        } else {
                            player.sendMessage("§eA newer version available: §b" + latest);
                            player.sendMessage("§eYou are currently on: §c" + currentVersion);
                            player.sendMessage("§eYou can download the latest version here:");
                            player.sendMessage("§9§nhttps://modrinth.com/plugin/matchboxplugin");
                        }
                    });
                },
                50L
        );
    }
}

