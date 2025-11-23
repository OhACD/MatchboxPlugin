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

    // Track ALL players who started the round (for nametag restoration)
    private final Set<UUID> allParticipatingPlayers = new HashSet<>();

    // Track which session is currently active
    private String activeSessionName = null;

    /**
     * Clears all state for a new round.
     */
    public void clearRoundState() {
        roles.clear();
        alivePlayers.clear();
        swipedThisRound.clear();
        curedThisRound.clear();
        allParticipatingPlayers.clear();
        activeSessionName = null;
    }

    /**
     * Adds a player to the alive players set.
     */
    public void addAlivePlayer(Player player) {
        alivePlayers.add(player.getUniqueId());
        allParticipatingPlayers.add(player.getUniqueId());
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
     */
    public void removeAlivePlayer(UUID playerId) {
        alivePlayers.remove(playerId);
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
}