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
 * Handles the discussion phase logic including timer and countdown.
 */
public class DiscussionPhaseHandler {
    private final Plugin plugin;
    private final MessageUtils messageUtils;
    private BukkitRunnable discussionTask = null;
    private final int DEFAULT_DISCUSSION_SECONDS = 60; // 1 minute discussion
    private Collection<UUID> currentPlayerIds = null;

    public DiscussionPhaseHandler(Plugin plugin, MessageUtils messageUtils) {
        this.plugin = plugin;
        this.messageUtils = messageUtils;
    }

    /**
     * Starts the discussion phase with a countdown timer.
     */
    public void startDiscussionPhase(int seconds, Collection<UUID> alivePlayerIds, Runnable onPhaseEnd) {
        cancelDiscussionTask();

        this.currentPlayerIds = alivePlayerIds;

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

        discussionTask = new BukkitRunnable() {
            @Override
            public void run() {
                int secs = remaining.getAndDecrement();
                if (secs <= 0) {
                    cancel();
                    discussionTask = null;
                    plugin.getLogger().info("Discussion phase ended naturally");
                    clearActionBars(); // Clear action bars before ending
                    onPhaseEnd.run();
                    return;
                }
                // Updates actionbar for all alive players
                for (Player player : getAlivePlayerObjects(alivePlayerIds)) {
                    messageUtils.sendActionBar(player, "§eDiscussion: " + secs + "s");
                }
                // Broadcast at specific times
                if (secs == 30 || secs == 10 || secs == 5 || secs <= 3) {
                    messageUtils.sendPlainMessage("§eDiscussion phase ends in " + secs + " seconds!");
                }
            }
        };
        discussionTask.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Starts the discussion phase with default duration.
     */
    public void startDiscussionPhase(Collection<UUID> alivePlayerIds, Runnable onPhaseEnd) {
        startDiscussionPhase(DEFAULT_DISCUSSION_SECONDS, alivePlayerIds, onPhaseEnd);
    }

    /**
     * Cancels the current discussion phase task.
     */
    public void cancelDiscussionTask() {
        if (discussionTask != null) {
            try {
                plugin.getLogger().info("Cancelling discussion phase task");
                discussionTask.cancel();
                clearActionBars();
            } catch (IllegalStateException ignored) {}
            discussionTask = null;
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
     * Checks if discussion phase is currently active.
     */
    public boolean isActive() {
        return discussionTask != null;
    }

    public Collection<Player> getAlivePlayerObjects(Collection<UUID> playerIds) {
        return playerIds.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}