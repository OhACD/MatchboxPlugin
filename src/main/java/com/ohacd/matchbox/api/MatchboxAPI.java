package com.ohacd.matchbox.api;

import com.ohacd.matchbox.Matchbox;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.session.GameSession;
import com.ohacd.matchbox.game.session.SessionManager;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.Role;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ohacd.matchbox.api.annotation.Experimental;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main API class for interacting with the Matchbox plugin.
 * 
 * <p>This class provides static methods for managing game sessions, players,
 * and event listeners. It serves as the primary entry point for external plugins
 * to interact with Matchbox functionality.</p>
 * 
 * <p>All methods are thread-safe and handle null inputs gracefully.</p>
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public final class MatchboxAPI {
    
    private static final Map<MatchboxEventListener, Boolean> listeners = new ConcurrentHashMap<>();
    
    // Private constructor to prevent instantiation
    private MatchboxAPI() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Creates a new session builder for the specified session name.
     * 
     * @param name the unique name for the session
     * @return a new SessionBuilder instance
     * @throws IllegalArgumentException if name is null or empty
     */
    @NotNull
    public static SessionBuilder createSessionBuilder(@NotNull String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Session name cannot be null or empty");
        }
        return new SessionBuilder(name);
    }
    
    /**
     * Gets an existing game session by name.
     * 
     * @param name the session name (case-insensitive)
     * @return Optional containing the session if found, empty otherwise
     */
    @NotNull
    public static Optional<ApiGameSession> getSession(@Nullable String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        
        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) return Optional.empty();
        
        SessionManager sessionManager = plugin.getSessionManager();
        if (sessionManager == null) return Optional.empty();
        
        GameSession session = sessionManager.getSession(name);
        return session != null ? Optional.of(new ApiGameSession(session)) : Optional.empty();
    }
    
    /**
     * Gets all active game sessions.
     * 
     * @return a collection of all active sessions
     */
    @NotNull
    public static Collection<ApiGameSession> getAllSessions() {
        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) return Collections.emptyList();
        
        SessionManager sessionManager = plugin.getSessionManager();
        if (sessionManager == null) return Collections.emptyList();
        
        List<ApiGameSession> sessions = new ArrayList<>();
        for (GameSession session : sessionManager.getAllSessions()) {
            if (session.isActive()) {
                sessions.add(new ApiGameSession(session));
            }
        }
        return Collections.unmodifiableCollection(sessions);
    }
    
    /**
     * Ends a game session gracefully.
     *
     * @param name the session name to end
     * @return true if the session was found and ended, false otherwise
     */
    public static boolean endSession(@Nullable String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) return false;

        SessionManager sessionManager = plugin.getSessionManager();
        if (sessionManager == null) return false;

        // Check if session exists before trying to end it
        if (!sessionManager.sessionExists(name)) {
            return false;
        }

        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) return false;

        try {
            gameManager.endGame(name);
            return true;
        } catch (Exception e) {
            JavaPlugin matchboxPlugin = Matchbox.getInstance();
            if (matchboxPlugin != null) {
                matchboxPlugin.getLogger().warning("Failed to end session '" + name + "': " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Ends all active game sessions gracefully.
     *
     * @return the number of sessions that were ended
     */
    public static int endAllSessions() {
        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) return 0;

        SessionManager sessionManager = plugin.getSessionManager();
        if (sessionManager == null) return 0;

        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) return 0;

        Collection<GameSession> allSessions = sessionManager.getAllSessions();
        int endedCount = 0;

        for (GameSession session : allSessions) {
            if (session != null && session.isActive()) {
                try {
                    gameManager.endGame(session.getName());
                    endedCount++;
                } catch (Exception e) {
                    JavaPlugin matchboxPlugin = Matchbox.getInstance();
                    if (matchboxPlugin != null) {
                        matchboxPlugin.getLogger().warning("Failed to end session '" + session.getName() + "': " + e.getMessage());
                    }
                }
            }
        }

        return endedCount;
    }
    
    /**
     * Gets the session a player is currently in.
     * 
     * @param player the player to check
     * @return Optional containing the session if the player is in one, empty otherwise
     */
    @NotNull
    public static Optional<ApiGameSession> getPlayerSession(@Nullable Player player) {
        if (player == null) return Optional.empty();
        
        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) return Optional.empty();
        
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) return Optional.empty();
        
        var context = gameManager.getContextForPlayer(player.getUniqueId());
        if (context == null) return Optional.empty();
        
        SessionManager sessionManager = plugin.getSessionManager();
        if (sessionManager == null) return Optional.empty();
        
        GameSession session = sessionManager.getSession(context.getSessionName());
        return session != null ? Optional.of(new ApiGameSession(session)) : Optional.empty();
    }
    
    /**
     * Gets the current role of a player if they are in an active game.
     * 
     * @param player the player to check
     * @return Optional containing the player's role if in a game, empty otherwise
     */
    @NotNull
    public static Optional<Role> getPlayerRole(@Nullable Player player) {
        if (player == null) return Optional.empty();
        
        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) return Optional.empty();
        
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) return Optional.empty();
        
        var context = gameManager.getContextForPlayer(player.getUniqueId());
        if (context == null) return Optional.empty();
        
        Role role = context.getGameState().getRole(player.getUniqueId());
        return role != null ? Optional.of(role) : Optional.empty();
    }
    
    /**
     * Gets the current game phase for a session.
     * 
     * @param sessionName the session name
     * @return Optional containing the current phase if session exists, empty otherwise
     */
    @NotNull
    public static Optional<GamePhase> getCurrentPhase(@Nullable String sessionName) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            return Optional.empty();
        }
        
        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) return Optional.empty();
        
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) return Optional.empty();
        
        var context = gameManager.getContext(sessionName);
        if (context == null) return Optional.empty();
        
        return Optional.ofNullable(context.getPhaseManager().getCurrentPhase());
    }
    
    /**
     * Adds an event listener to receive game events.
     * 
     * @param listener the listener to add
     * @throws IllegalArgumentException if listener is null
     */
    public static void addEventListener(@NotNull MatchboxEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        listeners.put(listener, true);
    }
    
    /**
     * Removes an event listener.
     * 
     * @param listener the listener to remove
     * @return true if the listener was removed, false if it wasn't found
     */
    public static boolean removeEventListener(@Nullable MatchboxEventListener listener) {
        return listener != null && listeners.remove(listener) != null;
    }
    
    /**
     * Gets all registered event listeners.
     * 
     * @return an unmodifiable copy of all registered listeners
     */
    @NotNull
    public static Set<MatchboxEventListener> getListeners() {
        return Collections.unmodifiableSet(new HashSet<>(listeners.keySet()));
    }
    
    /**
     * Registers a custom chat processor for a specific session.
     * The processor will be called for all chat messages in that session.
     *
     * @param sessionName the session name to register the processor for
     * @param processor the chat processor to register
     * @return true if the processor was registered, false if session not found
     * @throws IllegalArgumentException if sessionName or processor is null
     * @since 0.9.5
     */
    @Experimental
    public static boolean registerChatProcessor(@NotNull String sessionName, @NotNull ChatProcessor processor) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Session name cannot be null or empty");
        }
        if (processor == null) {
            throw new IllegalArgumentException("Chat processor cannot be null");
        }

        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) return false;

        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) return false;

        try {
            // Get the chat pipeline manager from game manager
            // This will be added to GameManager in the next step
            var chatPipelineManager = gameManager.getChatPipelineManager();
            if (chatPipelineManager == null) return false;

            chatPipelineManager.registerProcessor(sessionName, processor);
            return true;
        } catch (Exception e) {
            JavaPlugin matchboxPlugin = Matchbox.getInstance();
            if (matchboxPlugin != null) {
                matchboxPlugin.getLogger().warning("Failed to register chat processor for session '" + sessionName + "': " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Unregisters a custom chat processor from a specific session.
     *
     * @param sessionName the session name to unregister the processor from
     * @param processor the chat processor to unregister
     * @return true if the processor was unregistered, false if not found
     * @throws IllegalArgumentException if sessionName or processor is null
     * @since 0.9.5
     */
    @Experimental
    public static boolean unregisterChatProcessor(@NotNull String sessionName, @NotNull ChatProcessor processor) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Session name cannot be null or empty");
        }
        if (processor == null) {
            throw new IllegalArgumentException("Chat processor cannot be null");
        }

        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) return false;

        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) return false;

        try {
            var chatPipelineManager = gameManager.getChatPipelineManager();
            if (chatPipelineManager == null) return false;

            chatPipelineManager.unregisterProcessor(sessionName, processor);
            return true;
        } catch (Exception e) {
            JavaPlugin matchboxPlugin = Matchbox.getInstance();
            if (matchboxPlugin != null) {
                matchboxPlugin.getLogger().warning("Failed to unregister chat processor for session '" + sessionName + "': " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Unregisters all custom chat processors from a specific session.
     *
     * @param sessionName the session name to clear processors from
     * @return true if processors were cleared, false if session not found
     * @throws IllegalArgumentException if sessionName is null
     * @since 0.9.5
     */
    @Experimental
    public static boolean clearChatProcessors(@NotNull String sessionName) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Session name cannot be null or empty");
        }

        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) return false;

        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) return false;

        try {
            var chatPipelineManager = gameManager.getChatPipelineManager();
            if (chatPipelineManager == null) return false;

            chatPipelineManager.clearProcessors(sessionName);
            return true;
        } catch (Exception e) {
            JavaPlugin matchboxPlugin = Matchbox.getInstance();
            if (matchboxPlugin != null) {
                matchboxPlugin.getLogger().warning("Failed to clear chat processors for session '" + sessionName + "': " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Fires an event to all registered listeners.
     * This method is used internally by the plugin.
     *
     * @param event the event to fire
     */
    @com.ohacd.matchbox.api.annotation.Internal
    static void fireEvent(@NotNull MatchboxEvent event) {
        if (event == null) return;

        for (MatchboxEventListener listener : listeners.keySet()) {
            try {
                event.dispatch(listener);
            } catch (Exception e) {
                JavaPlugin plugin = Matchbox.getInstance();
                if (plugin != null) {
                    plugin.getLogger().warning("Error dispatching event " + event.getClass().getSimpleName() +
                                             " to listener: " + e.getMessage());
                }
            }
        }
    }
}
