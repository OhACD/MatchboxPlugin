package com.ohacd.matchbox.game.phase;

import com.ohacd.matchbox.game.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Handles the voting phase logic including timer and countdown.
 */
public class VotingPhaseHandler {
    private final Plugin plugin;
    private final MessageUtils messageUtils;
    private BukkitRunnable votingTask = null;
    private final int DEFAULT_VOTING_SECONDS = 30; // 30 seconds for voting
    private Collection<UUID> currentPlayerIds = null;

    public VotingPhaseHandler(Plugin plugin, MessageUtils messageUtils) {
        this.plugin = plugin;
        this.messageUtils = messageUtils;
    }

    /**
     * Starts the voting phase with a countdown timer.
     */
    public void startVotingPhase(int seconds, Collection<UUID> alivePlayerIds, Runnable onPhaseEnd) {
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
        
        cancelVotingTask();

        this.currentPlayerIds = alivePlayerIds;

        plugin.getLogger().info("Starting voting phase for " + alivePlayerIds.size() + " players (" + seconds + "s)");
        messageUtils.sendPlainMessage("§c§lVOTING PHASE! Vote for who you think is the Spark!");

        // Show title to all alive players at the start
        Collection<Player> alivePlayers = getAlivePlayerObjects(alivePlayerIds);
        messageUtils.sendTitle(
                alivePlayers,
                "§c§lVOTING",
                "§7Right-click a player to vote!",
                10, // fadeIn (0.5s)
                40, // stay (2s)
                10  // fadeOut (0.5s)
        );

        AtomicInteger remaining = new AtomicInteger(seconds);

        votingTask = new BukkitRunnable() {
            @Override
            public void run() {
                int secs = remaining.getAndDecrement();
                if (secs <= 0) {
                    cancel();
                    votingTask = null;
                    plugin.getLogger().info("Voting phase ended naturally");
                    clearActionBars(); // Clear action bars before ending
                    onPhaseEnd.run();
                    return;
                }
                // Updates actionbar for all alive players
                Collection<Player> alivePlayers = getAlivePlayerObjects(alivePlayerIds);
                if (alivePlayers != null) {
                    for (Player player : alivePlayers) {
                        if (player != null && player.isOnline()) {
                            try {
                                messageUtils.sendActionBar(player, "§cVoting: " + secs + "s");
                            } catch (Exception e) {
                                // Ignore individual player errors
                            }
                        }
                    }
                }
                // Broadcast at specific times
                if (secs == 20 || secs == 10 || secs == 5 || secs <= 3) {
                    messageUtils.sendPlainMessage("§eVoting ends in " + secs + " seconds!");
                }
            }
        };
        votingTask.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Starts the voting phase with default duration.
     */
    public void startVotingPhase(Collection<UUID> alivePlayerIds, Runnable onPhaseEnd) {
        startVotingPhase(DEFAULT_VOTING_SECONDS, alivePlayerIds, onPhaseEnd);
    }

    /**
     * Cancels the current voting phase task.
     */
    public void cancelVotingTask() {
        if (votingTask != null) {
            try {
                plugin.getLogger().info("Cancelling voting phase task");
                votingTask.cancel();
                clearActionBars();
            } catch (IllegalStateException ignored) {}
            votingTask = null;
        }
    }

    /**
     * Clears action bars for all tracked players.
     */
    private void clearActionBars() {
        if (currentPlayerIds != null) {
            for (Player player : getAlivePlayerObjects(currentPlayerIds)) {
                messageUtils.sendActionBar(player, "");
            }
        }
    }

    /**
     * Checks if voting phase is currently active.
     */
    public boolean isActive() {
        return votingTask != null;
    }

    public Collection<Player> getAlivePlayerObjects(Collection<UUID> playerIds) {
        return playerIds.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}

