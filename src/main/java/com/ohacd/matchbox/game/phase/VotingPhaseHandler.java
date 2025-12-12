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
 * Handles the voting phase logic including timer and countdown.
 * Supports multiple parallel sessions.
 */
public class VotingPhaseHandler {
    private final Plugin plugin;
    private final MessageUtils messageUtils;
    private final Map<String, BukkitRunnable> votingTasks = new ConcurrentHashMap<>();
    private final Map<String, Collection<UUID>> currentPlayerIds = new ConcurrentHashMap<>();
    private final Map<String, Integer> requiredVotesMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> alivePlayerCountMap = new ConcurrentHashMap<>();
    private final int DEFAULT_VOTING_SECONDS = 15; // 15 seconds for voting

    public VotingPhaseHandler(Plugin plugin, MessageUtils messageUtils) {
        this.plugin = plugin;
        this.messageUtils = messageUtils;
    }

    /**
     * Starts the voting phase with a countdown timer for a specific session.
     */
    public void startVotingPhase(String sessionName, int seconds, Collection<UUID> alivePlayerIds, Runnable onPhaseEnd) {
        startVotingPhase(sessionName, seconds, alivePlayerIds, onPhaseEnd, -1, -1);
    }
    
    /**
     * Starts the voting phase with a countdown timer and threshold display for a specific session.
     */
    public void startVotingPhase(String sessionName, int seconds, Collection<UUID> alivePlayerIds, Runnable onPhaseEnd, int requiredVotes, int alivePlayerCount) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            plugin.getLogger().warning("Cannot start voting phase with null or empty session name");
            return;
        }
        if (seconds <= 0) {
            plugin.getLogger().warning("Invalid voting phase duration: " + seconds);
            return;
        }
        if (alivePlayerIds == null || alivePlayerIds.isEmpty()) {
            plugin.getLogger().warning("Cannot start voting phase with no players");
            return;
        }
        if (onPhaseEnd == null) {
            plugin.getLogger().warning("Cannot start voting phase with null callback");
            return;
        }
        
        cancelVotingTask(sessionName);

        this.currentPlayerIds.put(sessionName, alivePlayerIds);
        this.requiredVotesMap.put(sessionName, requiredVotes);
        this.alivePlayerCountMap.put(sessionName, alivePlayerCount);

        plugin.getLogger().info("Starting voting phase for " + alivePlayerIds.size() + " players (" + seconds + "s)");
        messageUtils.sendPlainMessage("§c§lVOTING PHASE! Vote for who you think is the Spark!");

        // Show title and instructions to all alive players at the start
        Collection<Player> alivePlayers = getAlivePlayerObjects(alivePlayerIds);
        
        String subtitle = "§7Vote or abstain!";
        if (requiredVotes > 0 && alivePlayerCount > 0) {
            subtitle = "§7Threshold: " + requiredVotes + "/" + alivePlayerCount;
        }
        
        messageUtils.sendTitle(
                alivePlayers,
                "§c§lVOTING",
                subtitle,
                10, // fadeIn (0.5s)
                40, // stay (2s)
                10  // fadeOut (0.5s)
        );
        
        // Send voting instructions to all players
        for (Player player : alivePlayers) {
            if (player != null && player.isOnline()) {
                player.sendMessage("§e§lHow to Vote:");
                player.sendMessage("§7- Right-click a voting paper in your inventory");
                player.sendMessage("§7- Left-click a voting paper in your inventory");
                player.sendMessage("§7- You can choose to not vote");
                if (requiredVotes > 0 && alivePlayerCount > 0) {
                    player.sendMessage("§7- Threshold: §e" + requiredVotes + "/" + alivePlayerCount + " §7votes required to eliminate a player");
                    player.sendMessage("§7- If threshold isn't met, no elimination will occur");
                }
            }
        }

        AtomicInteger remaining = new AtomicInteger(seconds);
        final String sessionKey = sessionName;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                int secs = remaining.getAndDecrement();
                if (secs <= 0) {
                    cancel();
                    votingTasks.remove(sessionKey);
                    currentPlayerIds.remove(sessionKey);
                    requiredVotesMap.remove(sessionKey);
                    alivePlayerCountMap.remove(sessionKey);
                    plugin.getLogger().info("Voting phase ended naturally for session: " + sessionKey);
                    clearActionBars(sessionKey);
                    onPhaseEnd.run();
                    return;
                }
                // Updates actionbar for all alive players in this session
                Collection<UUID> playerIds = currentPlayerIds.get(sessionKey);
                Integer requiredVotes = requiredVotesMap.get(sessionKey);
                Integer aliveCount = alivePlayerCountMap.get(sessionKey);
                if (playerIds != null) {
                    Collection<Player> alivePlayers = getAlivePlayerObjects(playerIds);
                    if (alivePlayers != null) {
                        for (Player player : alivePlayers) {
                            if (player != null && player.isOnline()) {
                                try {
                                    // Build actionbar message with timer and threshold
                                    String actionBarMessage;
                                    if (requiredVotes != null && aliveCount != null && requiredVotes > 0 && aliveCount > 0) {
                                        actionBarMessage = "§cVoting: " + secs + "s §8| §eThreshold: " + requiredVotes + "/" + aliveCount;
                                    } else {
                                        actionBarMessage = "§cVoting: " + secs + "s";
                                    }
                                    messageUtils.sendActionBar(player, actionBarMessage);
                                } catch (Exception e) {
                                    // Ignore individual player errors
                                }
                            }
                        }
                    }
                }
                // Broadcast at specific times (only to players in this session)
                if (secs == 10 || secs == 5 || secs <= 3) {
                    Collection<UUID> playerIdsForMsg = currentPlayerIds.get(sessionKey);
                    if (playerIdsForMsg != null) {
                        Collection<Player> players = getAlivePlayerObjects(playerIdsForMsg);
                        if (players != null && !players.isEmpty()) {
                            for (Player p : players) {
                                if (p != null && p.isOnline()) {
                                    p.sendMessage("§eVoting ends in " + secs + " seconds!");
                                }
                            }
                        }
                    }
                }
            }
        };
        votingTasks.put(sessionName, task);
        task.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Starts the voting phase with default duration for a specific session.
     */
    public void startVotingPhase(String sessionName, Collection<UUID> alivePlayerIds, Runnable onPhaseEnd) {
        startVotingPhase(sessionName, DEFAULT_VOTING_SECONDS, alivePlayerIds, onPhaseEnd);
    }

    /**
     * Cancels the voting phase task for a specific session.
     */
    public void cancelVotingTask(String sessionName) {
        if (sessionName == null) {
            return;
        }
        BukkitRunnable task = votingTasks.remove(sessionName);
        if (task != null) {
            try {
                plugin.getLogger().info("Cancelling voting phase task for session: " + sessionName);
                task.cancel();
                clearActionBars(sessionName);
            } catch (IllegalStateException ignored) {}
        }
        currentPlayerIds.remove(sessionName);
        requiredVotesMap.remove(sessionName);
        alivePlayerCountMap.remove(sessionName);
    }
    
    /**
     * Cancels all voting phase tasks (for cleanup).
     */
    public void cancelAllVotingTasks() {
        for (String sessionName : new HashSet<>(votingTasks.keySet())) {
            cancelVotingTask(sessionName);
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
     * Checks if voting phase is currently active for a session.
     */
    public boolean isActive(String sessionName) {
        return votingTasks.containsKey(sessionName);
    }

    public Collection<Player> getAlivePlayerObjects(Collection<UUID> playerIds) {
        return playerIds.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}

