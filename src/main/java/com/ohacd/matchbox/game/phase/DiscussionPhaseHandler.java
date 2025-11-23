package com.ohacd.matchbox.game.phase;

import com.ohacd.matchbox.game.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Handles the discussion phase logic including timer and countdown.
 * Supports multiple parallel sessions.
 */
public class DiscussionPhaseHandler {
    private final Plugin plugin;
    private final MessageUtils messageUtils;
    private final Map<String, BukkitRunnable> discussionTasks = new ConcurrentHashMap<>();
    private final Map<String, Collection<UUID>> currentPlayerIds = new ConcurrentHashMap<>();
    private final int DEFAULT_DISCUSSION_SECONDS = 30; // 30 seconds discussion

    public DiscussionPhaseHandler(Plugin plugin, MessageUtils messageUtils) {
        this.plugin = plugin;
        this.messageUtils = messageUtils;
    }

    /**
     * Starts the discussion phase with a countdown timer for a specific session.
     */
    public void startDiscussionPhase(String sessionName, int seconds, Collection<UUID> alivePlayerIds, Runnable onPhaseEnd) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            plugin.getLogger().warning("Cannot start discussion phase with null or empty session name");
            return;
        }
        if (seconds <= 0) {
            plugin.getLogger().warning("Invalid discussion phase duration: " + seconds);
            return;
        }
        if (alivePlayerIds == null || alivePlayerIds.isEmpty()) {
            plugin.getLogger().warning("Cannot start discussion phase with no players");
            return;
        }
        if (onPhaseEnd == null) {
            plugin.getLogger().warning("Cannot start discussion phase with null callback");
            return;
        }
        
        cancelDiscussionTask(sessionName);

        this.currentPlayerIds.put(sessionName, alivePlayerIds);

        plugin.getLogger().info("Starting discussion phase for " + alivePlayerIds.size() + " players (" + seconds + "s)");
        messageUtils.sendPlainMessage("§aDiscussion phase started! You have " + seconds + " seconds to discuss.");

        // Show title to all alive players at the start
        Collection<Player> alivePlayers = getAlivePlayerObjects(alivePlayerIds);
        messageUtils.sendTitle(
                alivePlayers,
                "§e§lDISCUSSION",
                "§7Talk about what happened!",
                10, // fadeIn (0.5s)
                40, // stay (2s)
                10  // fadeOut (0.5s)
        );

        AtomicInteger remaining = new AtomicInteger(seconds);
        final String sessionKey = sessionName;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                int secs = remaining.getAndDecrement();
                if (secs <= 0) {
                    cancel();
                    discussionTasks.remove(sessionKey);
                    currentPlayerIds.remove(sessionKey);
                    plugin.getLogger().info("Discussion phase ended naturally for session: " + sessionKey);
                    clearActionBars(sessionKey);
                    onPhaseEnd.run();
                    return;
                }
                // Updates actionbar for all alive players in this session
                Collection<UUID> playerIds = currentPlayerIds.get(sessionKey);
                if (playerIds != null) {
                    Collection<Player> alivePlayers = getAlivePlayerObjects(playerIds);
                    if (alivePlayers != null) {
                        for (Player player : alivePlayers) {
                            if (player != null && player.isOnline()) {
                                try {
                                    messageUtils.sendActionBar(player, "§eDiscussion: " + secs + "s");
                                } catch (Exception e) {
                                    // Ignore individual player errors
                                }
                            }
                        }
                    }
                }
                // Broadcast at specific times (only to players in this session)
                if (secs == 20 || secs == 10 || secs == 5 || secs <= 3) {
                    Collection<UUID> playerIdsForMsg = currentPlayerIds.get(sessionKey);
                    if (playerIdsForMsg != null) {
                        Collection<Player> players = getAlivePlayerObjects(playerIdsForMsg);
                        if (players != null && !players.isEmpty()) {
                            for (Player p : players) {
                                if (p != null && p.isOnline()) {
                                    p.sendMessage("§eDiscussion phase ends in " + secs + " seconds!");
                                }
                            }
                        }
                    }
                }
            }
        };
        discussionTasks.put(sessionName, task);
        task.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Starts the discussion phase with default duration for a specific session.
     */
    public void startDiscussionPhase(String sessionName, Collection<UUID> alivePlayerIds, Runnable onPhaseEnd) {
        startDiscussionPhase(sessionName, DEFAULT_DISCUSSION_SECONDS, alivePlayerIds, onPhaseEnd);
    }

    /**
     * Cancels the discussion phase task for a specific session.
     */
    public void cancelDiscussionTask(String sessionName) {
        if (sessionName == null) {
            return;
        }
        BukkitRunnable task = discussionTasks.remove(sessionName);
        if (task != null) {
            try {
                plugin.getLogger().info("Cancelling discussion phase task for session: " + sessionName);
                task.cancel();
                clearActionBars(sessionName);
            } catch (IllegalStateException ignored) {}
        }
        currentPlayerIds.remove(sessionName);
    }
    
    /**
     * Cancels all discussion phase tasks (for cleanup).
     */
    public void cancelAllDiscussionTasks() {
        for (String sessionName : new java.util.HashSet<>(discussionTasks.keySet())) {
            cancelDiscussionTask(sessionName);
        }
    }

    /**
     * Clears action bars for all tracked players in a session.
     */
    private void clearActionBars(String sessionName) {
        Collection<UUID> playerIds = currentPlayerIds.get(sessionName);
        if (playerIds != null) {
            Collection<Player> players = getAlivePlayerObjects(playerIds);
            if (players != null) {
                for (Player player : players) {
                    if (player != null && player.isOnline()) {
                        try {
                            messageUtils.sendActionBar(player, "");
                        } catch (Exception e) {
                            // Ignore individual player errors
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if discussion phase is currently active for a session.
     */
    public boolean isActive(String sessionName) {
        return discussionTasks.containsKey(sessionName);
    }

    public Collection<Player> getAlivePlayerObjects(Collection<UUID> playerIds) {
        return playerIds.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}