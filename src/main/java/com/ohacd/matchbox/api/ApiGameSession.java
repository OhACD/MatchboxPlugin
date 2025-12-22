package com.ohacd.matchbox.api;

import com.ohacd.matchbox.Matchbox;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.session.GameSession;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.Role;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * API wrapper for GameSession that provides a clean interface for external integration.
 * 
 * <p>This class wraps the internal GameSession class and provides methods for
 * managing game state, players, and phases without exposing internal implementation details.</p>
 * 
 * <p>All methods are thread-safe and handle null inputs gracefully.</p>
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public class ApiGameSession {
    
    private final GameSession session;
    private PhaseController phaseController;
    
    /**
     * Creates a new API game session wrapper.
     * 
     * @param session the internal game session to wrap
     * @throws IllegalArgumentException if session is null
     */
    public ApiGameSession(@NotNull GameSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Game session cannot be null");
        }
        this.session = session;
    }
    
    /**
     * Gets the name of this session.
     * 
     * @return the session name, never null
     */
    @NotNull
    public String getName() {
        return session.getName();
    }
    
    /**
     * Gets whether this session is currently active.
     * 
     * @return true if the session is active
     */
    public boolean isActive() {
        return session != null && session.isActive();
    }
    
    /**
     * Gets the current game phase.
     * 
     * @return the current phase, or null if no game is active
     */
    @Nullable
    public GamePhase getCurrentPhase() {
        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) return null;
        
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) return null;
        
        SessionGameContext context = gameManager.getContext(session.getName());
        if (context == null) return null;
        
        return context.getPhaseManager().getCurrentPhase();
    }
    
    /**
     * Gets the current round number.
     * 
     * @return the current round number, or -1 if no game is active
     */
    public int getCurrentRound() {
        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) return -1;
        
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) return -1;
        
        SessionGameContext context = gameManager.getContext(session.getName());
        if (context == null) return -1;
        
        return context.getGameState().getCurrentRound();
    }
    
    /**
     * Gets all players in this session.
     * 
     * @return an unmodifiable collection of all players in the session
     */
    @NotNull
    public Collection<Player> getPlayers() {
        return Collections.unmodifiableCollection(new ArrayList<>(session.getPlayers()));
    }
    
    /**
     * Gets all currently alive players in this session.
     * 
     * @return an unmodifiable collection of alive players
     */
    @NotNull
    public Collection<Player> getAlivePlayers() {
        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) return Collections.emptyList();
        
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) return Collections.emptyList();
        
        SessionGameContext context = gameManager.getContext(session.getName());
        if (context == null) return Collections.emptyList();
        
        try {
            // Manually get alive players since GameManager doesn't expose getSwipePhaseHandler()
            Collection<Player> alivePlayers = new ArrayList<>();
            Set<UUID> aliveIds = context.getGameState().getAlivePlayerIds();
            if (aliveIds != null) {
                for (UUID id : aliveIds) {
                    if (id != null) {
                        Player player = org.bukkit.Bukkit.getPlayer(id);
                        if (player != null && player.isOnline()) {
                            alivePlayers.add(player);
                        }
                    }
                }
            }
            return Collections.unmodifiableCollection(alivePlayers);
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting alive players for session '" + getName() + "': " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Gets the role of a player in this session.
     * 
     * @param player the player to check
     * @return optional containing the player's role, empty if not found or not in game
     */
    @NotNull
    public Optional<Role> getPlayerRole(@Nullable Player player) {
        if (player == null) return Optional.empty();
        
        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) return Optional.empty();
        
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) return Optional.empty();
        
        SessionGameContext context = gameManager.getContext(session.getName());
        if (context == null) return Optional.empty();
        
        Role role = context.getGameState().getRole(player.getUniqueId());
        return role != null ? Optional.of(role) : Optional.empty();
    }
    
    /**
     * Starts the game for this session.
     * 
     * @return true if the game was started successfully
     */
    public boolean startGame() {
        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) {
            return false;
        }
        
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) {
            return false;
        }
        
        try {
            Collection<Player> players = session.getPlayers();
            List<org.bukkit.Location> spawnLocations = session.getSpawnLocations();
            org.bukkit.Location discussionLocation = session.getDiscussionLocation();
            
            if (players.isEmpty() || spawnLocations.isEmpty()) {
                plugin.getLogger().warning("Cannot start game for session '" + getName() + "': Missing players or spawn locations");
                return false;
            }
            
            gameManager.startRound(players, spawnLocations, discussionLocation, session.getName());
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to start game for session '" + getName() + "': " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Ends the game for this session.
     * 
     * @return true if the game was ended successfully
     */
    public boolean endGame() {
        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) {
            return false;
        }
        
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) {
            return false;
        }
        
        try {
            gameManager.endGame(session.getName());
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to end game for session '" + getName() + "': " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Adds a player to this session.
     * 
     * @param player the player to add
     * @return true if the player was added successfully
     */
    public boolean addPlayer(@Nullable Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        
        try {
            return session.addPlayer(player);
        } catch (Exception e) {
            JavaPlugin plugin = Matchbox.getInstance();
            if (plugin != null) {
                plugin.getLogger().warning("Failed to add player " + player.getName() + 
                                       " to session '" + getName() + "': " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Removes a player from this session.
     * 
     * @param player the player to remove
     * @return true if the player was removed successfully
     */
    public boolean removePlayer(@Nullable Player player) {
        if (player == null) {
            return false;
        }
        
        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) {
            return false;
        }
        
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) {
            return false;
        }
        
        try {
            gameManager.removePlayerFromGame(player);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to remove player " + player.getName() + 
                                   " from session '" + getName() + "': " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the phase controller for this session.
     * 
     * @return a phase controller instance for managing game phases
     */
    @NotNull
    public PhaseController getPhaseController() {
        if (phaseController == null) {
            phaseController = new PhaseController(session);
        }
        return phaseController;
    }
    
    /**
     * Skips to the next phase in the game.
     * 
     * @return true if the phase was skipped successfully
     * @deprecated Use {@link #getPhaseController()} and {@link PhaseController#skipToNextPhase()} for better error handling
     */
    @Deprecated
    public boolean skipToNextPhase() {
        return getPhaseController().skipToNextPhase();
    }
    
    /**
     * Forces the game to a specific phase.
     * 
     * @param phase the phase to force
     * @return true if the phase was forced successfully
     * @deprecated Use {@link #getPhaseController()} and {@link PhaseController#forcePhase(GamePhase)} for better error handling
     */
    @Deprecated
    public boolean forcePhase(@Nullable GamePhase phase) {
        if (phase == null) {
            return false;
        }
        return getPhaseController().forcePhase(phase);
    }
    
    /**
     * Checks if a specific player is alive in this session.
     * 
     * @param player the player to check
     * @return true if the player is alive, false if dead or not in session
     */
    public boolean isPlayerAlive(@Nullable Player player) {
        if (player == null) return false;
        
        return getAlivePlayers().stream()
                .anyMatch(alive -> alive.getUniqueId().equals(player.getUniqueId()));
    }
    
    /**
     * Gets the number of alive players in this session.
     * 
     * @return the count of alive players
     */
    public int getAlivePlayerCount() {
        return getAlivePlayers().size();
    }
    
    /**
     * Gets the total number of players in this session.
     * 
     * @return the total player count
     */
    public int getTotalPlayerCount() {
        return getPlayers().size();
    }
    
    /**
     * Checks if the session is currently in an active game phase.
     * 
     * @return true if in a game phase, false if not started or ended
     */
    public boolean isInGamePhase() {
        GamePhase currentPhase = getCurrentPhase();
        return currentPhase != null && currentPhase != GamePhase.WAITING;
    }
    
    /**
     * Gets a human-readable status description of the session.
     * 
     * @return a descriptive status string
     */
    @NotNull
    public String getStatusDescription() {
        if (!isActive()) {
            return "Session inactive";
        }
        
        GamePhase phase = getCurrentPhase();
        if (phase == null) {
            return "No active game";
        }
        
        int aliveCount = getAlivePlayerCount();
        int totalCount = getTotalPlayerCount();
        
        return String.format("Phase: %s, Players: %d/%d", phase, aliveCount, totalCount);
    }
    
    /**
     * Gets the internal GameSession object.
     * This method is for internal use only and should not be used by external plugins.
     * 
     * @return the wrapped GameSession
     * @deprecated This method exposes internal implementation details. Use the provided API methods instead.
     */
    @Deprecated
    public GameSession getInternalSession() {
        return session;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ApiGameSession that = (ApiGameSession) obj;
        return session.equals(that.session);
    }
    
    @Override
    public int hashCode() {
        return session.hashCode();
    }
    
    @Override
    public String toString() {
        return "ApiGameSession{name='" + getName() + "', active=" + isActive() + "}";
    }
}
