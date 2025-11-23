package com.ohacd.matchbox.game.state;

import com.ohacd.matchbox.game.utils.Role;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Manages the current game state including player roles, alive players, and round-specific tracking.
 */
public class GameState {
    private final Map<UUID, Role> roles = new HashMap<>();
    private final Set<UUID> alivePlayers = new HashSet<>();
    private final Set<UUID> swipedThisRound = new HashSet<>();
    private final Set<UUID> curedThisRound = new HashSet<>();
    private final Set<UUID> usedHealingSightThisRound = new HashSet<>();
    private final Set<UUID> usedHunterVisionThisRound = new HashSet<>();

    // NEW: track infected (swiped successfully) this round (before pending death application)
    private final Set<UUID> infectedThisRound = new HashSet<>();

    // NEW: pending death times (epoch millis when the pending elimination should be applied)
    private final Map<UUID, Long> pendingDeathTime = new HashMap<>();

    // Track ALL players who started the round (for nametag restoration)
    private final Set<UUID> allParticipatingPlayers = new HashSet<>();

    // Track which session is currently active
    private String activeSessionName = null;

    // Track current round number
    private int currentRound = 0;

    /**
     * Clears all state for a new game (not just round).
     * This should only be called when starting a brand new game or ending a game.
     */
    public void clearGameState() {
        roles.clear();
        alivePlayers.clear();
        swipedThisRound.clear();
        curedThisRound.clear();
        infectedThisRound.clear();
        usedHealingSightThisRound.clear();
        usedHunterVisionThisRound.clear();
        pendingDeathTime.clear();
        allParticipatingPlayers.clear();
        activeSessionName = null;
        currentRound = 0;
    }

    /**
     * Clears only per-round state (swipes, cures, infections).
     * Keeps player list, roles, and session intact.
     * This should be called at the start of each new round.
     */
    public void clearRoundState() {
        swipedThisRound.clear();
        curedThisRound.clear();
        infectedThisRound.clear();
        usedHealingSightThisRound.clear();
        usedHunterVisionThisRound.clear();
        // Note: do NOT clear pendingDeathTime here — pending deaths may span rounds.
    }

    /**
     * Increments the round counter.
     */
    public void incrementRound() {
        currentRound++;
    }

    /**
     * Gets the current round number.
     */
    public int getCurrentRound() {
        return currentRound;
    }

    /**
     * Adds a player to the alive players set.
     */
    public void addAlivePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        alivePlayers.add(uuid);
        allParticipatingPlayers.add(uuid);
    }

    /**
     * Adds multiple players to the alive players set.
     */
    public void addAlivePlayers(Collection<Player> players) {
        for (Player player : players) {
            addAlivePlayer(player);
        }
    }

    /**
     * Removes a player from the alive players set.
     * Note: This does NOT remove them from allParticipatingPlayers.
     */
    public void removeAlivePlayer(UUID playerId) {
        alivePlayers.remove(playerId);
        // If they had pending death or infected flags, keep pending handling separate.
        infectedThisRound.remove(playerId);
        pendingDeathTime.remove(playerId);
        swipedThisRound.remove(playerId);
        curedThisRound.remove(playerId);
    }

    /**
     * Checks if a player is alive.
     */
    public boolean isAlive(UUID playerId) {
        return alivePlayers.contains(playerId);
    }

    /**
     * Gets the role of a player.
     */
    public Role getRole(UUID playerId) {
        return roles.get(playerId);
    }

    /**
     * Sets the role of a player.
     */
    public void setRole(UUID playerId, Role role) {
        roles.put(playerId, role);
    }

    /**
     * Gets all alive player UUIDs.
     */
    public Set<UUID> getAlivePlayerIds() {
        return new HashSet<>(alivePlayers);
    }

    /**
     * Gets the count of alive players.
     */
    public int getAlivePlayerCount() {
        return alivePlayers.size();
    }

    /**
     * Marks that a player has swiped this round.
     */
    public void markSwiped(UUID playerId) {
        swipedThisRound.add(playerId);
    }

    /**
     * Checks if a player has swiped this round.
     */
    public boolean hasSwipedThisRound(UUID playerId) {
        return swipedThisRound.contains(playerId);
    }

    /**
     * Marks that a player has cured this round.
     */
    public void markCured(UUID playerId) {
        curedThisRound.add(playerId);
    }

    /**
     * Checks if a player has cured this round.
     */
    public boolean hasCuredThisRound(UUID playerId) {
        return curedThisRound.contains(playerId);
    }

    /**
     * Marks that a player was infected (swiped successfully) this round.
     */
    public void markInfected(UUID playerId) {
        infectedThisRound.add(playerId);
    }

    /**
     * Checks if a player was infected this round.
     */
    public boolean wasInfectedThisRound(UUID playerId) {
        return infectedThisRound.contains(playerId);
    }

    /**
     * Clears infected flags for the round.
     */
    public void clearInfectedThisRound() {
        infectedThisRound.clear();
    }

    /**
     * Marks that a player has used healing sight this round.
     */
    public void markUsedHealingSight(UUID playerId) {
        usedHealingSightThisRound.add(playerId);
    }

    /**
     * Checks if a player has used healing sight this round.
     */
    public boolean hasUsedHealingSightThisRound(UUID playerId) {
        return usedHealingSightThisRound.contains(playerId);
    }

    /**
     * Marks that a player has used hunter vision this round.
     */
    public void markUsedHunterVision(UUID playerId) {
        usedHunterVisionThisRound.add(playerId);
    }

    /**
     * Checks if a player has used hunter vision this round.
     */
    public boolean hasUsedHunterVisionThisRound(UUID playerId) {
        return usedHunterVisionThisRound.contains(playerId);
    }

    /**
     * Schedules a pending death for a player at the given epoch millis.
     * Use removePendingDeath to cancel (e.g. cured).
     */
    public void setPendingDeath(UUID playerId, long epochMillis) {
        pendingDeathTime.put(playerId, epochMillis);
    }

    /**
     * Removes pending death for a player (e.g. cured).
     */
    public void removePendingDeath(UUID playerId) {
        pendingDeathTime.remove(playerId);
    }

    /**
     * Checks if a player currently has a pending death scheduled.
     */
    public boolean hasPendingDeath(UUID playerId) {
        return pendingDeathTime.containsKey(playerId);
    }

    /**
     * Gets the scheduled pending death time for a player (epoch millis), or null if none.
     */
    public Long getPendingDeathTime(UUID playerId) {
        return pendingDeathTime.get(playerId);
    }

    /**
     * Returns a snapshot of player UUIDs whose pending death time is <= provided epoch millis.
     * Useful for processing due pending deaths.
     */
    public Set<UUID> getPendingDeathsDueAt(long epochMillis) {
        Set<UUID> due = new HashSet<>();
        for (Map.Entry<UUID, Long> e : pendingDeathTime.entrySet()) {
            if (e.getValue() <= epochMillis) {
                due.add(e.getKey());
            }
        }
        return due;
    }

    /**
     * Gets the UUID of the Spark player.
     */
    public UUID getSparkUUID() {
        return roles.entrySet().stream()
                .filter(entry -> entry.getValue() == Role.SPARK)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * Counts the number of alive innocents.
     */
    public long countAliveInnocents() {
        return alivePlayers.stream()
                .filter(uuid -> roles.get(uuid) == Role.INNOCENT)
                .count();
    }

    /**
     * Gets all participating player UUIDs (including eliminated ones).
     */
    public Set<UUID> getAllParticipatingPlayerIds() {
        return new HashSet<>(allParticipatingPlayers);
    }

    /**
     * Sets the active session name.
     */
    public void setActiveSessionName(String sessionName) {
        this.activeSessionName = sessionName;
    }

    /**
     * Gets the active session name.
     */
    public String getActiveSessionName() {
        return activeSessionName;
    }

    /**
     * Checks if there's an active game.
     */
    public boolean isGameActive() {
        return !allParticipatingPlayers.isEmpty();
    }

    /**
     * Validates that the game state is consistent.
     * Returns true if state is valid, false otherwise.
     */
    public boolean validateState() {
        // All alive players must be in participating players
        for (UUID alivePlayer : alivePlayers) {
            if (!allParticipatingPlayers.contains(alivePlayer)) {
                return false;
            }
        }

        // All players with roles must be in participating players
        for (UUID playerWithRole : roles.keySet()) {
            if (!allParticipatingPlayers.contains(playerWithRole)) {
                return false;
            }
        }

        // If there are participating players, there must be at least one role assigned
        if (!allParticipatingPlayers.isEmpty() && roles.isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Gets a debug string representation of the current state.
     */
    public String getDebugInfo() {
        return String.format(
                "GameState[Round=%d, Session=%s, Participating=%d, Alive=%d, Roles=%d, Swiped=%d, Cured=%d, Infected=%d, Pending=%d]",
                currentRound,
                activeSessionName != null ? activeSessionName : "none",
                allParticipatingPlayers.size(),
                alivePlayers.size(),
                roles.size(),
                swipedThisRound.size(),
                curedThisRound.size(),
                infectedThisRound.size(),
                pendingDeathTime.size()
        );
    }
}