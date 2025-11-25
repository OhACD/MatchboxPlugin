package com.ohacd.matchbox.game.phase;

import com.ohacd.matchbox.game.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Handles the swipe phase logic including timer and countdown.
 * Supports multiple parallel sessions.
 */
public class SwipePhaseHandler {
    private final Plugin plugin;
    private final MessageUtils messageUtils;
    private final Map<String, BukkitRunnable> swipeTasks = new ConcurrentHashMap<>();
    private final Map<String, Collection<UUID>> currentPlayerIds = new ConcurrentHashMap<>();
    private final int DEFAULT_SWIPE_SECONDS = 60 * 3; // 3 minutes

    public SwipePhaseHandler(Plugin plugin, MessageUtils messageUtils) {
        this.plugin = plugin;
        this.messageUtils = messageUtils;
    }

    /**
     * Starts the swipe phase with a countdown timer for a specific session.
     */
    public void startSwipePhase(String sessionName, int seconds, Collection<UUID> alivePlayerIds, Runnable onPhaseEnd) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            plugin.getLogger().warning("Cannot start swipe phase with null or empty session name");
            return;
        }
        if (seconds <= 0) {
            plugin.getLogger().warning("Invalid swipe phase duration: " + seconds);
            return;
        }
        if (alivePlayerIds == null || alivePlayerIds.isEmpty()) {
            plugin.getLogger().warning("Cannot start swipe phase with no players");
            return;
        }
        if (onPhaseEnd == null) {
            plugin.getLogger().warning("Cannot start swipe phase with null callback");
            return;
        }
        
        cancelSwipeTask(sessionName);

        this.currentPlayerIds.put(sessionName, alivePlayerIds);

        plugin.getLogger().info("Starting swipe phase for " + alivePlayerIds.size() + " players (" + seconds + "s)");
        messageUtils.sendPlainMessage("§6Swipe phase started! You have " + seconds + " seconds to swipe.");

        AtomicInteger remaining = new AtomicInteger(seconds);
        final String sessionKey = sessionName;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                int secs = remaining.getAndDecrement();
                if (secs <= 0) {
                    cancel();
                    swipeTasks.remove(sessionKey);
                    currentPlayerIds.remove(sessionKey);
                    plugin.getLogger().info("Swipe phase ended naturally for session: " + sessionKey);
                    clearActionBars(sessionKey); // Clear action bars before ending
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
                                    messageUtils.sendActionBar(player, "§6Swipe: " + secs + "s");
                                } catch (Exception e) {
                                    // Ignore individual player errors
                                }
                            }
                        }
                    }
                }
                // Broadcast at specific times (only to players in this session)
                if (secs == 120 || secs == 60 || secs == 30 || secs == 10 || secs == 5 || secs <= 3) {
                    Collection<UUID> playerIdsForMsg = currentPlayerIds.get(sessionKey);
                    if (playerIdsForMsg != null) {
                        Collection<Player> players = getAlivePlayerObjects(playerIdsForMsg);
                        if (players != null && !players.isEmpty()) {
                            for (Player p : players) {
                                if (p != null && p.isOnline()) {
                                    p.sendMessage("§eSwipe phase ends in " + secs + " seconds!");
                                }
                            }
                        }
                    }
                }
            }
        };
        swipeTasks.put(sessionName, task);
        task.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Starts the swipe phase with default duration for a specific session.
     */
    public void startSwipePhase(String sessionName, Collection<UUID> alivePlayerIds, Runnable onPhaseEnd) {
        startSwipePhase(sessionName, DEFAULT_SWIPE_SECONDS, alivePlayerIds, onPhaseEnd);
    }

    /**
     * Cancels the swipe phase task for a specific session.
     */
    public void cancelSwipeTask(String sessionName) {
        if (sessionName == null) {
            return;
        }
        BukkitRunnable task = swipeTasks.remove(sessionName);
        if (task != null) {
            try {
                plugin.getLogger().info("Cancelling swipe phase task for session: " + sessionName);
                task.cancel();
                clearActionBars(sessionName);
            } catch (IllegalStateException ignored) {}
        }
        currentPlayerIds.remove(sessionName);
    }
    
    /**
     * Cancels all swipe phase tasks (for cleanup).
     */
    public void cancelAllSwipeTasks() {
        for (String sessionName : new HashSet<>(swipeTasks.keySet())) {
            cancelSwipeTask(sessionName);
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
     * Checks if swipe phase is currently active for a session.
     */
    public boolean isActive(String sessionName) {
        return swipeTasks.containsKey(sessionName);
    }

    public Collection<Player> getAlivePlayerObjects(Collection<UUID> playerIds) {
        return playerIds.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}