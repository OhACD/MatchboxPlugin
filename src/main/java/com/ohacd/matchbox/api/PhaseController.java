package com.ohacd.matchbox.api;

import com.ohacd.matchbox.Matchbox;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.session.GameSession;
import com.ohacd.matchbox.game.utils.GamePhase;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for managing game phases with simplified operations.
 * 
 * <p>This class provides a clean interface for phase control operations,
 * abstracting away the complexity of direct phase manipulation.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * PhaseController controller = new PhaseController(session);
 * 
 * // Skip to next phase
 * boolean success = controller.skipToNextPhase();
 * 
 * // Force specific phase
 * success = controller.forcePhase(GamePhase.DISCUSSION);
 * 
 * // Check if phase transition is valid
 * boolean canTransition = controller.canTransitionTo(GamePhase.VOTING);
 * }</pre>
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public final class PhaseController {
    
    private final GameSession session;
    private final String sessionName;
    
    /**
     * Creates a new phase controller for the specified session.
     * 
     * @param session the game session to control
     * @throws IllegalArgumentException if session is null
     */
    public PhaseController(@NotNull GameSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Game session cannot be null");
        }
        this.session = session;
        this.sessionName = session.getName();
    }
    
    /**
     * Gets the current game phase.
     * 
     * @return the current phase, or null if not available
     */
    @Nullable
    public GamePhase getCurrentPhase() {
        return getGameContext()
                .map(context -> context.getPhaseManager().getCurrentPhase())
                .orElse(null);
    }
    
    /**
     * Skips to the next phase in the natural progression.
     * 
     * @return true if the phase was skipped successfully
     */
    public boolean skipToNextPhase() {
        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) {
            logError("Plugin instance not available");
            return false;
        }
        
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) {
            logError("Game manager not available");
            return false;
        }
        
        GamePhase currentPhase = getCurrentPhase();
        if (currentPhase == null) {
            logError("Current phase not available");
            return false;
        }
        
        try {
            switch (currentPhase) {
                case SWIPE:
                    gameManager.endSwipePhase(sessionName);
                    return true;
                case DISCUSSION:
                    gameManager.endDiscussionPhase(sessionName);
                    return true;
                case VOTING:
                    gameManager.endVotingPhase(sessionName);
                    return true;
                default:
                    logError("Cannot skip from phase: " + currentPhase);
                    return false;
            }
        } catch (Exception e) {
            logError("Failed to skip phase: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Forces the game to a specific phase.
     * 
     * @param targetPhase the phase to force
     * @return true if the phase was forced successfully
     */
    public boolean forcePhase(@NotNull GamePhase targetPhase) {
        if (targetPhase == null) {
            logError("Target phase cannot be null");
            return false;
        }
        
        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) {
            logError("Plugin instance not available");
            return false;
        }
        
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) {
            logError("Game manager not available");
            return false;
        }
        
        GamePhase currentPhase = getCurrentPhase();
        if (currentPhase == null) {
            logError("Current phase not available");
            return false;
        }
        
        try {
            // End current phase first
            if (!endCurrentPhase(gameManager, currentPhase)) {
                return false;
            }
            
            // Start target phase
            return startTargetPhase(gameManager, targetPhase);
            
        } catch (Exception e) {
            logError("Failed to force phase to " + targetPhase + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if transitioning to a specific phase is valid.
     * 
     * @param targetPhase the phase to check
     * @return true if the transition is valid
     */
    public boolean canTransitionTo(@NotNull GamePhase targetPhase) {
        if (targetPhase == null) {
            return false;
        }
        
        GamePhase currentPhase = getCurrentPhase();
        if (currentPhase == null) {
            return targetPhase == GamePhase.SWIPE; // Only SWIPE is valid from null
        }
        
        // Define valid transitions
        switch (currentPhase) {
            case SWIPE:
                return targetPhase == GamePhase.DISCUSSION;
            case DISCUSSION:
                return targetPhase == GamePhase.VOTING;
            case VOTING:
                return targetPhase == GamePhase.SWIPE; // Next round
            default:
                return false;
        }
    }
    
    /**
     * Gets a description of the current phase state.
     * 
     * @return human-readable phase description
     */
    @NotNull
    public String getPhaseDescription() {
        GamePhase currentPhase = getCurrentPhase();
        if (currentPhase == null) {
            return "No active game phase";
        }
        
        switch (currentPhase) {
            case SWIPE:
                return "Swipe phase - Players can attack each other";
            case DISCUSSION:
                return "Discussion phase - Players can discuss and use abilities";
            case VOTING:
                return "Voting phase - Players vote to eliminate suspects";
            default:
                return "Unknown phase: " + currentPhase;
        }
    }
    
    /**
     * Gets the estimated time remaining in the current phase.
     * 
     * @return estimated seconds remaining, or -1 if not available
     */
    public long getTimeRemaining() {
        // This would require access to phase timers
        // For now, return -1 to indicate not available
        return -1;
    }
    
    /**
     * Ends the current phase.
     */
    private boolean endCurrentPhase(@NotNull GameManager gameManager, @NotNull GamePhase currentPhase) {
        try {
            switch (currentPhase) {
                case SWIPE:
                    gameManager.endSwipePhase(sessionName);
                    return true;
                case DISCUSSION:
                    gameManager.endDiscussionPhase(sessionName);
                    return true;
                case VOTING:
                    gameManager.endVotingPhase(sessionName);
                    return true;
                default:
                    logError("Cannot end phase: " + currentPhase);
                    return false;
            }
        } catch (Exception e) {
            logError("Failed to end current phase " + currentPhase + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Starts the target phase.
     */
    private boolean startTargetPhase(@NotNull GameManager gameManager, @NotNull GamePhase targetPhase) {
        try {
            switch (targetPhase) {
                case SWIPE:
                    gameManager.startSwipePhase(sessionName);
                    return true;
                case DISCUSSION:
                    // Discussion phase is started automatically after swipe ends
                    return true;
                case VOTING:
                    // Voting phase is started automatically after discussion ends
                    return true;
                default:
                    logError("Cannot start phase: " + targetPhase);
                    return false;
            }
        } catch (Exception e) {
            logError("Failed to start target phase " + targetPhase + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the game context for this session.
     */
    private java.util.Optional<SessionGameContext> getGameContext() {
        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) {
            return java.util.Optional.empty();
        }
        
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) {
            return java.util.Optional.empty();
        }
        
        return java.util.Optional.ofNullable(gameManager.getContext(sessionName));
    }
    
    /**
     * Logs an error message.
     */
    private void logError(@NotNull String message) {
        JavaPlugin plugin = Matchbox.getInstance();
        if (plugin != null) {
            plugin.getLogger().warning("PhaseController [" + sessionName + "]: " + message);
        }
    }
    
    @Override
    public String toString() {
        return "PhaseController{sessionName='" + sessionName + "', currentPhase=" + getCurrentPhase() + "}";
    }
}
