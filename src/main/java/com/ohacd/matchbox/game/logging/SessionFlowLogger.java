package com.ohacd.matchbox.game.logging;

import com.ohacd.matchbox.api.GameLogEntry;
import com.ohacd.matchbox.api.GameSessionLog;
import com.ohacd.matchbox.api.GameStatistics;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Centralized structured logging and stats collection per session.
 */
public final class SessionFlowLogger {

    private final Plugin plugin;
    private final Map<String, List<GameLogEntry>> sessionEntries = new ConcurrentHashMap<>();
    private final Map<String, Map<UUID, GameStatistics.PlayerStats>> sessionStats = new ConcurrentHashMap<>();
    private final Map<String, Integer> roundsPlayed = new ConcurrentHashMap<>();

    public SessionFlowLogger(@NotNull Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin cannot be null");
        }
        this.plugin = plugin;
    }

    public void incrementRound(@NotNull String sessionName) {
        if (!isValidSessionName(sessionName)) {
            return;
        }
        roundsPlayed.merge(sessionName, 1, Integer::sum);
    }

    public void record(
        @NotNull String sessionName,
        @NotNull String category,
        @NotNull String message,
        @Nullable UUID actorId,
        @Nullable UUID targetId,
        @Nullable Map<String, String> attributes
    ) {
        if (!isValidSessionName(sessionName) || category == null || category.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            return;
        }

        GameLogEntry entry = new GameLogEntry(
            Instant.now(),
            sessionName,
            category,
            message,
            actorId,
            targetId,
            attributes == null ? Collections.emptyMap() : attributes
        );

        sessionEntries.computeIfAbsent(sessionName, ignored -> Collections.synchronizedList(new ArrayList<>())).add(entry);
        plugin.getLogger().info("[session=" + sessionName + "] [" + category + "] " + message);
    }

    public void recordVote(
        @NotNull String sessionName,
        @NotNull UUID voterId,
        @NotNull UUID targetId,
        @NotNull String voterName,
        @NotNull String targetName
    ) {
        if (!isValidSessionName(sessionName)) {
            return;
        }
        updateStats(sessionName, voterId, s -> s.withVotesCast(1));
        updateStats(sessionName, targetId, s -> s.withVotesReceived(1));
        record(sessionName, "VOTE", voterName + " voted for " + targetName, voterId, targetId, Collections.emptyMap());
    }

    public void recordSwipe(
        @NotNull String sessionName,
        @NotNull UUID sparkId,
        @NotNull UUID targetId,
        @NotNull String sparkName,
        @NotNull String targetName
    ) {
        if (!isValidSessionName(sessionName)) {
            return;
        }
        updateStats(sessionName, sparkId, s -> s.withSwipes(1));
        record(sessionName, "SWIPE", sparkName + " swiped " + targetName, sparkId, targetId, Collections.emptyMap());
    }

    public void recordCure(
        @NotNull String sessionName,
        @NotNull UUID medicId,
        @NotNull UUID targetId,
        @NotNull String medicName,
        @NotNull String targetName
    ) {
        if (!isValidSessionName(sessionName)) {
            return;
        }
        updateStats(sessionName, medicId, s -> s.withCures(1));
        record(sessionName, "CURE", medicName + " cured " + targetName, medicId, targetId, Collections.emptyMap());
    }

    public void recordElimination(@NotNull String sessionName, @NotNull UUID targetId, @NotNull String targetName) {
        if (!isValidSessionName(sessionName)) {
            return;
        }
        updateStats(sessionName, targetId, s -> s.withEliminations(1));
        record(sessionName, "ELIMINATION", targetName + " eliminated", null, targetId, Collections.emptyMap());
    }

    public void recordChat(
        @NotNull String sessionName,
        @NotNull UUID senderId,
        @NotNull String senderName,
        @NotNull String channel,
        @NotNull String message
    ) {
        if (!isValidSessionName(sessionName)) {
            return;
        }
        updateStats(sessionName, senderId, s -> s.withChatsSent(1));
        record(sessionName, "CHAT", senderName + " [" + channel + "]: " + message, senderId, null, Map.of("channel", channel));
    }

    public void recordSignMessage(
        @NotNull String sessionName,
        @NotNull UUID senderId,
        @NotNull String senderName,
        @NotNull String message
    ) {
        if (!isValidSessionName(sessionName)) {
            return;
        }
        updateStats(sessionName, senderId, s -> s.withSignMessages(1));
        record(sessionName, "SIGN", senderName + ": " + message, senderId, null, Collections.emptyMap());
    }

    @NotNull
    public GameSessionLog getSessionLog(@NotNull String sessionName) {
        if (!isValidSessionName(sessionName)) {
            return new GameSessionLog("unknown", Collections.emptyList());
        }
        List<GameLogEntry> entries = sessionEntries.getOrDefault(sessionName, Collections.emptyList());
        synchronized (entries) {
            return new GameSessionLog(sessionName, List.copyOf(entries));
        }
    }

    @NotNull
    public GameStatistics getSessionStatistics(@NotNull String sessionName) {
        if (!isValidSessionName(sessionName)) {
            return new GameStatistics("unknown", 0, Collections.emptyMap());
        }

        Map<UUID, GameStatistics.PlayerStats> stats = sessionStats.getOrDefault(sessionName, Collections.emptyMap());
        return new GameStatistics(
            sessionName,
            roundsPlayed.getOrDefault(sessionName, 0),
            new ConcurrentHashMap<>(stats)
        );
    }

    private void updateStats(
        @NotNull String sessionName,
        @NotNull UUID playerId,
        @NotNull Function<GameStatistics.PlayerStats, GameStatistics.PlayerStats> updater
    ) {
        sessionStats
            .computeIfAbsent(sessionName, ignored -> new ConcurrentHashMap<>())
            .compute(playerId, (ignored, current) -> {
                GameStatistics.PlayerStats base = current == null ? GameStatistics.PlayerStats.empty() : current;
                return updater.apply(base);
            });
    }

    private boolean isValidSessionName(String sessionName) {
        return sessionName != null && !sessionName.trim().isEmpty();
    }
}
