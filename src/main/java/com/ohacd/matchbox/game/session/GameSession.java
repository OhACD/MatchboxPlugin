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
    private final Map<Integer, Location> seatLocations = new HashMap<>();
    private Location discussionLocation;
    private boolean active = false;

    public GameSession(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Session name cannot be null or empty");
        }
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
        if (player == null || !player.isOnline()) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        if (uuid == null) {
            return false;
        }
        return players.add(uuid);
    }

    /**
     * Removes a player from the session.
     */
    public boolean removePlayer(Player player) {
        if (player == null) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        if (uuid == null) {
            return false;
        }
        return players.remove(uuid);
    }

    /**
     * Checks if a player is in the session.
     */
    public boolean hasPlayer(Player player) {
        if (player == null) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        if (uuid == null) {
            return false;
        }
        return players.contains(uuid);
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
        if (players != null) {
            for (UUID uuid : players) {
                if (uuid == null) continue;
                Player player = org.bukkit.Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    playerList.add(player);
                }
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
        if (location == null || location.getWorld() == null) {
            return;
        }
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
        if (spawnLocations == null || spawnLocations.isEmpty()) {
            return null;
        }
        // Filter out null or invalid locations
        List<Location> validLocations = new ArrayList<>();
        for (Location loc : spawnLocations) {
            if (loc != null && loc.getWorld() != null) {
                validLocations.add(loc);
            }
        }
        if (validLocations.isEmpty()) {
            return null;
        }
        Collections.shuffle(validLocations);
        Location selected = validLocations.get(0);
        return selected != null ? selected.clone() : null;
    }

    /**
     * Sets the discussion location.
     */
    public void setDiscussionLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            this.discussionLocation = null;
            return;
        }
        this.discussionLocation = location.clone();
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

    /**
     * Sets a seat location for a specific seat number.
     */
    public void setSeatLocation(int seatNumber, Location location) {
        if (location == null || location.getWorld() == null) {
            seatLocations.remove(seatNumber);
            return;
        }
        seatLocations.put(seatNumber, location.clone());
    }

    /**
     * Gets the seat location for a specific seat number.
     */
    public Location getSeatLocation(int seatNumber) {
        Location loc = seatLocations.get(seatNumber);
        return loc != null ? loc.clone() : null;
    }

    /**
     * Gets all seat locations.
     */
    public Map<Integer, Location> getSeatLocations() {
        Map<Integer, Location> result = new HashMap<>();
        for (Map.Entry<Integer, Location> entry : seatLocations.entrySet()) {
            result.put(entry.getKey(), entry.getValue().clone());
        }
        return result;
    }

    /**
     * Checks if a seat location is set for the given seat number.
     */
    public boolean hasSeatLocation(int seatNumber) {
        Location loc = seatLocations.get(seatNumber);
        return loc != null && loc.getWorld() != null;
    }
}
