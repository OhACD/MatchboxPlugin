package com.ohacd.matchbox.game;

import com.ohacd.matchbox.game.phase.PhaseManager;
import com.ohacd.matchbox.game.role.RoleAssigner;
import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.vote.VoteManager;
import com.ohacd.matchbox.game.win.WinConditionChecker;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains all game state and managers for a single active game session.
 * This allows multiple games to run in parallel without interfering with each other.
 * Each session maintains its own game state, phase manager, vote manager, and ability windows.
 */
public class SessionGameContext {
    private final String sessionName;
    private final GameState gameState;
    private final PhaseManager phaseManager;
    private final RoleAssigner roleAssigner;
    private final WinConditionChecker winConditionChecker;
    private final VoteManager voteManager;
    
    /** Maps player UUID to expiry timestamp for active swipe windows */
    private final Map<UUID, Long> activeSwipeWindow = new ConcurrentHashMap<>();
    
    /** Maps player UUID to expiry timestamp for active cure windows */
    private final Map<UUID, Long> activeCureWindow = new ConcurrentHashMap<>();
    
    /** Maps player UUID to expiry timestamp for active delusion windows */
    private final Map<UUID, Long> activeDelusionWindow = new ConcurrentHashMap<>();
    
    /** Discussion location for the current round */
    private Location currentDiscussionLocation;
    
    /** Spawn locations for the current round */
    private List<Location> currentSpawnLocations;
    
    /** Number of consecutive voting phases that ended without elimination */
    private int consecutiveNoEliminationPhases = 0;
    
    public SessionGameContext(Plugin plugin, String sessionName) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Session name cannot be null or empty");
        }
        this.sessionName = sessionName;
        this.gameState = new GameState();
        this.phaseManager = new PhaseManager(plugin);
        this.roleAssigner = new RoleAssigner(gameState);
        this.winConditionChecker = new WinConditionChecker(gameState);
        this.voteManager = new VoteManager(gameState);
    }
    
    public String getSessionName() {
        return sessionName;
    }
    
    public GameState getGameState() {
        return gameState;
    }
    
    public PhaseManager getPhaseManager() {
        return phaseManager;
    }
    
    public RoleAssigner getRoleAssigner() {
        return roleAssigner;
    }
    
    public WinConditionChecker getWinConditionChecker() {
        return winConditionChecker;
    }
    
    public VoteManager getVoteManager() {
        return voteManager;
    }
    
    public Map<UUID, Long> getActiveSwipeWindow() {
        return activeSwipeWindow;
    }
    
    public Map<UUID, Long> getActiveCureWindow() {
        return activeCureWindow;
    }
    
    public Map<UUID, Long> getActiveDelusionWindow() {
        return activeDelusionWindow;
    }
    
    public Location getCurrentDiscussionLocation() {
        return currentDiscussionLocation;
    }
    
    public void setCurrentDiscussionLocation(Location location) {
        this.currentDiscussionLocation = location;
    }
    
    public List<Location> getCurrentSpawnLocations() {
        return currentSpawnLocations;
    }
    
    public void setCurrentSpawnLocations(List<Location> locations) {
        this.currentSpawnLocations = locations != null ? new ArrayList<>(locations) : null;
    }
    
    /**
     * Gets the number of consecutive voting phases that ended without elimination.
     */
    public int getConsecutiveNoEliminationPhases() {
        return consecutiveNoEliminationPhases;
    }
    
    /**
     * Increments the counter for consecutive no-elimination phases.
     * Should be called when a voting phase ends without elimination.
     */
    public void incrementNoEliminationPhases() {
        consecutiveNoEliminationPhases++;
    }
    
    /**
     * Resets the counter for consecutive no-elimination phases.
     * Should be called when a voting phase ends with elimination.
     */
    public void resetNoEliminationPhases() {
        consecutiveNoEliminationPhases = 0;
    }
    
    /**
     * Cleans up all resources for this session context.
     */
    public void cleanup() {
        activeSwipeWindow.clear();
        activeCureWindow.clear();
        activeDelusionWindow.clear();
        currentDiscussionLocation = null;
        currentSpawnLocations = null;
        consecutiveNoEliminationPhases = 0;
        gameState.clearGameState();
    }
}

