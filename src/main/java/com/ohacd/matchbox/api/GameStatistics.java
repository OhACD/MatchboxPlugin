package com.ohacd.matchbox.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable per-session statistics snapshot for integrations.
 *
 * @since 0.9.7
 */
public final class GameStatistics {
    private final String sessionName;
    private final int roundsPlayed;
    private final Map<UUID, PlayerStats> perPlayer;

    /**
     * Creates an immutable statistics snapshot.
     *
     * @param sessionName owning session name
     * @param roundsPlayed number of rounds completed in the session
     * @param perPlayer per-player statistics map
     */
    public GameStatistics(
        @NotNull String sessionName,
        int roundsPlayed,
        @NotNull Map<UUID, PlayerStats> perPlayer
    ) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionName cannot be null or empty");
        }
        if (roundsPlayed < 0) {
            throw new IllegalArgumentException("roundsPlayed cannot be negative");
        }
        if (perPlayer == null) {
            throw new IllegalArgumentException("perPlayer cannot be null");
        }

        this.sessionName = sessionName;
        this.roundsPlayed = roundsPlayed;
        this.perPlayer = Collections.unmodifiableMap(new HashMap<>(perPlayer));
    }

    /**
     * Returns the owning session name.
     *
     * @return session name
     */
    @NotNull
    public String getSessionName() {
        return sessionName;
    }

    /**
     * Returns the number of rounds completed in the session.
     *
     * @return rounds played
     */
    public int getRoundsPlayed() {
        return roundsPlayed;
    }

    /**
     * Returns the immutable per-player statistics map.
     *
     * @return per-player statistics
     */
    @NotNull
    public Map<UUID, PlayerStats> getPerPlayer() {
        return perPlayer;
    }

    /**
     * Returns statistics for a player object.
     *
     * @param player player to inspect
     * @return player statistics or an empty snapshot when unavailable
     */
    @NotNull
    public PlayerStats getStats(@Nullable Player player) {
        if (player == null) {
            return PlayerStats.empty();
        }
        return getStats(player.getUniqueId());
    }

    /**
     * Returns statistics for a player UUID.
     *
     * @param playerId player UUID to inspect
     * @return player statistics or an empty snapshot when unavailable
     */
    @NotNull
    public PlayerStats getStats(@Nullable UUID playerId) {
        if (playerId == null) {
            return PlayerStats.empty();
        }
        return perPlayer.getOrDefault(playerId, PlayerStats.empty());
    }

    /**
     * Immutable per-player session counters.
     *
     * @param votesCast votes cast by the player
     * @param votesReceived votes received by the player
     * @param swipes successful swipe actions performed
     * @param cures successful cure actions performed
     * @param eliminations times the player was eliminated
     * @param chatsSent chat messages sent
     * @param signMessages sign messages sent
     */
    public record PlayerStats(
        int votesCast,
        int votesReceived,
        int swipes,
        int cures,
        int eliminations,
        int chatsSent,
        int signMessages
    ) {
        /**
         * Returns an empty zeroed statistics snapshot.
         *
         * @return empty player statistics
         */
        public static PlayerStats empty() {
            return new PlayerStats(0, 0, 0, 0, 0, 0, 0);
        }

        /**
         * Returns a copy with incremented votes cast.
         *
         * @param increment amount to add
         * @return updated player statistics
         */
        public PlayerStats withVotesCast(int increment) {
            return new PlayerStats(votesCast + increment, votesReceived, swipes, cures, eliminations, chatsSent, signMessages);
        }

        /**
         * Returns a copy with incremented votes received.
         *
         * @param increment amount to add
         * @return updated player statistics
         */
        public PlayerStats withVotesReceived(int increment) {
            return new PlayerStats(votesCast, votesReceived + increment, swipes, cures, eliminations, chatsSent, signMessages);
        }

        /**
         * Returns a copy with incremented swipe count.
         *
         * @param increment amount to add
         * @return updated player statistics
         */
        public PlayerStats withSwipes(int increment) {
            return new PlayerStats(votesCast, votesReceived, swipes + increment, cures, eliminations, chatsSent, signMessages);
        }

        /**
         * Returns a copy with incremented cure count.
         *
         * @param increment amount to add
         * @return updated player statistics
         */
        public PlayerStats withCures(int increment) {
            return new PlayerStats(votesCast, votesReceived, swipes, cures + increment, eliminations, chatsSent, signMessages);
        }

        /**
         * Returns a copy with incremented elimination count.
         *
         * @param increment amount to add
         * @return updated player statistics
         */
        public PlayerStats withEliminations(int increment) {
            return new PlayerStats(votesCast, votesReceived, swipes, cures, eliminations + increment, chatsSent, signMessages);
        }

        /**
         * Returns a copy with incremented chat count.
         *
         * @param increment amount to add
         * @return updated player statistics
         */
        public PlayerStats withChatsSent(int increment) {
            return new PlayerStats(votesCast, votesReceived, swipes, cures, eliminations, chatsSent + increment, signMessages);
        }

        /**
         * Returns a copy with incremented sign-message count.
         *
         * @param increment amount to add
         * @return updated player statistics
         */
        public PlayerStats withSignMessages(int increment) {
            return new PlayerStats(votesCast, votesReceived, swipes, cures, eliminations, chatsSent, signMessages + increment);
        }
    }
}
