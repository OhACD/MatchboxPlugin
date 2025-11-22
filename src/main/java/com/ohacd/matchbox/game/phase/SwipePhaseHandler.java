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
    private final int DEFAULT_SWIPE_SECONDS = 30;

    public SwipePhaseHandler(Plugin plugin, MessageUtils messageUtils) {
        this.plugin = plugin;
        this.messageUtils = messageUtils;
    }

    /**
     * Starts the swipe phase with a countdown timer.
     */
    public void startSwipePhase(int seconds, Collection<UUID> alivePlayerIds, Runnable onPhaseEnd) {
        cancelSwipeTask();

        messageUtils.sendPlainMessage("Swipe phase started! You have " + seconds + " seconds to swipe.");
        AtomicInteger remaining = new AtomicInteger(seconds);

        swipeTask = new BukkitRunnable() {
            @Override
            public void run() {
                int secs = remaining.getAndDecrement();
                if (secs <= 0) {
                    cancel();
                    swipeTask = null;
                    onPhaseEnd.run();
                    return;
                }
                // Updates actionbar for all alive players
                for (Player player : getAlivePlayerObjects(alivePlayerIds)) {
                    messageUtils.sendActionBar(player, "Swipe: " + secs + "s");
                }
                // Broadcast at specific times
                if (secs == 10 || secs == 5 || secs <= 3) {
                    messageUtils.sendPlainMessage("Swipe phase ends in " + secs + " seconds!");
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
                swipeTask.cancel();
            } catch (IllegalStateException ignored) {}
            swipeTask = null;
        }
    }

    private Collection<Player> getAlivePlayerObjects(Collection<UUID> playerIds) {
        return playerIds.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
