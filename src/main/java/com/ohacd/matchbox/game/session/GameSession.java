package com.ohacd.matchbox.game.session;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Represents a game session with players, spawn locations, and discussion area.
 */
public class GameSession {
    private final String name;
    private final Set<UUID> players = new HashSet<>();
    private final List<Location> spawnLocations = new ArrayList<>();
    private Location discussionLocation;
    private boolean active = false;

    public GameSession(String name) {
        this.name = name;
    }

    /**
     * Gets the session name.
     */
    public String getName() {
        return name;
    }

    /**
     * Adds a player to the session.
     */
    public boolean addPlayer(Player player) {
        return players.add(player.getUniqueId());
    }

    /**
     * Removes a player from the session.
     */
    public boolean removePlayer(Player player) {
        return players.remove(player.getUniqueId());
    }

    /**
     * Checks if a player is in the session.
     */
    public boolean hasPlayer(Player player) {
        return players.contains(player.getUniqueId());
    }

    /**
     * Gets all player UUIDs in the session.
     */
    public Set<UUID> getPlayerIds() {
        return new HashSet<>(players);
    }

    /**
     * Gets all players in the session as Player objects.
     */
    public List<Player> getPlayers() {
        List<Player> playerList = new ArrayList<>();
        for (UUID uuid : players) {
            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                playerList.add(player);
            }
        }
        return playerList;
    }

    /**
     * Gets the number of players in the session.
     */
    public int getPlayerCount() {
        return players.size();
    }

    /**
     * Adds a spawn location.
     */
    public void addSpawnLocation(Location location) {
        spawnLocations.add(location);
    }

    /**
     * Gets all spawn locations.
     */
    public List<Location> getSpawnLocations() {
        return new ArrayList<>(spawnLocations);
    }

    /**
     * Gets a random spawn location. Returns null if no spawns are set.
     */
    public Location getRandomSpawnLocation() {
        if (spawnLocations.isEmpty()) {
            return null;
        }
        Collections.shuffle(spawnLocations);
        return spawnLocations.get(0).clone();
    }

    /**
     * Sets the discussion location.
     */
    public void setDiscussionLocation(Location location) {
        this.discussionLocation = location;
    }

    /**
     * Gets the discussion location.
     */
    public Location getDiscussionLocation() {
        return discussionLocation != null ? discussionLocation.clone() : null;
    }

    /**
     * Checks if the session has spawn locations set.
     */
    public boolean hasSpawnLocations() {
        return !spawnLocations.isEmpty();
    }

    /**
     * Checks if the discussion location is set.
     */
    public boolean hasDiscussionLocation() {
        return discussionLocation != null;
    }

    /**
     * Sets whether the session is active (game in progress).
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Checks if the session is active.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Clears all players from the session.
     */
    public void clearPlayers() {
        players.clear();
    }
}
