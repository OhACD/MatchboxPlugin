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
 * Handles the swipe phase logic including timer and countdown.
 */
public class SwipePhaseHandler {
    private final Plugin plugin;
    private final MessageUtils messageUtils;
    private BukkitRunnable swipeTask = null;
    private final int DEFAULT_SWIPE_SECONDS = 60 * 2; // 2 minutes
    private Collection<UUID> currentPlayerIds = null;

    public SwipePhaseHandler(Plugin plugin, MessageUtils messageUtils) {
        this.plugin = plugin;
        this.messageUtils = messageUtils;
    }

    /**
     * Starts the swipe phase with a countdown timer.
     */
    public void startSwipePhase(int seconds, Collection<UUID> alivePlayerIds, Runnable onPhaseEnd) {
        cancelSwipeTask();

        this.currentPlayerIds = alivePlayerIds;

        plugin.getLogger().info("Starting swipe phase for " + alivePlayerIds.size() + " players (" + seconds + "s)");
        messageUtils.sendPlainMessage("§6Swipe phase started! You have " + seconds + " seconds to swipe.");

        AtomicInteger remaining = new AtomicInteger(seconds);

        swipeTask = new BukkitRunnable() {
            @Override
            public void run() {
                int secs = remaining.getAndDecrement();
                if (secs <= 0) {
                    cancel();
                    swipeTask = null;
                    plugin.getLogger().info("Swipe phase ended naturally");
                    clearActionBars(); // Clear action bars before ending
                    onPhaseEnd.run();
                    return;
                }
                // Updates actionbar for all alive players
                for (Player player : getAlivePlayerObjects(alivePlayerIds)) {
                    messageUtils.sendActionBar(player, "§6Swipe: " + secs + "s");
                }
                // Broadcast at specific times
                if (secs == 60 || secs == 30 || secs == 10 || secs == 5 || secs <= 3) {
                    messageUtils.sendPlainMessage("§eSwipe phase ends in " + secs + " seconds!");
                }
            }
        };
        swipeTask.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Starts the swipe phase with default duration.
     */
    public void startSwipePhase(Collection<UUID> alivePlayerIds, Runnable onPhaseEnd) {
        startSwipePhase(DEFAULT_SWIPE_SECONDS, alivePlayerIds, onPhaseEnd);
    }

    /**
     * Cancels the current swipe phase task.
     */
    public void cancelSwipeTask() {
        if (swipeTask != null) {
            try {
                plugin.getLogger().info("Cancelling swipe phase task");
                swipeTask.cancel();
                clearActionBars();
            } catch (IllegalStateException ignored) {}
            swipeTask = null;
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
     * Checks if swipe phase is currently active.
     */
    public boolean isActive() {
        return swipeTask != null;
    }

    public Collection<Player> getAlivePlayerObjects(Collection<UUID> playerIds) {
        return playerIds.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}