package com.ohacd.matchbox.api;

import com.ohacd.matchbox.Matchbox;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.session.GameSession;
import com.ohacd.matchbox.game.session.SessionManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ohacd.matchbox.api.annotation.Experimental;

import java.util.*;

/**
 * Builder class for creating and configuring game sessions.
 * 
 * <p>Provides a fluent interface for setting up game sessions with custom configurations,
 * players, spawn points, and other settings.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Enhanced error handling
 * SessionCreationResult result = MatchboxAPI.createSessionBuilder("arena1")
 *     .withPlayers(arena.getPlayers())
 *     .withSpawnPoints(arena.getSpawnPoints())
 *     .withDiscussionLocation(arena.getDiscussionArea())
 *     .withSeatLocations(seatMap)
 *     .startWithResult();
 * 
 * if (result.isSuccess()) {
 *     ApiGameSession session = result.getSession().get();
 *     // Use session
 * } else {
 *     logger.warning("Failed to create session: " + result.getErrorMessage());
 * }
 * 
 * // Legacy approach
 * ApiGameSession session = MatchboxAPI.createSessionBuilder("arena1")
 *     .withPlayers(arena.getPlayers())
 *     .withSpawnPoints(arena.getSpawnPoints())
 *     .start()
 *     .orElseThrow(() -> new RuntimeException("Failed to create session"));
 * }</pre>
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public class SessionBuilder {
    
    private final String sessionName;
    private Collection<Player> players;
    private List<Location> spawnPoints;
    private Location discussionLocation;
    private Map<Integer, Location> seatLocations;
    private GameConfig gameConfig;
    
    /**
     * Creates a new session builder with the specified session name.
     * 
     * @param sessionName the unique name for the session
     * @throws IllegalArgumentException if sessionName is null or empty
     */
    public SessionBuilder(@NotNull String sessionName) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Session name cannot be null or empty");
        }
        this.sessionName = sessionName;
        this.players = new ArrayList<>();
        this.spawnPoints = new ArrayList<>();
        this.seatLocations = new HashMap<>();
        this.gameConfig = new GameConfig.Builder().build();
    }
    
    /**
     * Sets the players for this session.
     * 
     * @param players the players to include in the session
     * @return this builder instance for method chaining
     */
    @NotNull
    public SessionBuilder withPlayers(@Nullable Collection<Player> players) {
        this.players = players != null ? new ArrayList<>(players) : new ArrayList<>();
        return this;
    }
    
    /**
     * Sets the players for this session.
     * 
     * @param players the players to include in the session
     * @return this builder instance for method chaining
     */
    @NotNull
    public SessionBuilder withPlayers(@Nullable Player... players) {
        this.players = players != null ? new ArrayList<>(Arrays.asList(players)) : new ArrayList<>();
        return this;
    }
    
    /**
     * Sets the spawn points for players.
     * 
     * @param spawnPoints list of spawn locations
     * @return this builder instance for method chaining
     */
    @NotNull
    public SessionBuilder withSpawnPoints(@Nullable List<Location> spawnPoints) {
        this.spawnPoints = spawnPoints != null ? new ArrayList<>(spawnPoints) : new ArrayList<>();
        return this;
    }
    
    /**
     * Sets the spawn points for players.
     * 
     * @param spawnPoints array of spawn locations
     * @return this builder instance for method chaining
     */
    @NotNull
    public SessionBuilder withSpawnPoints(@Nullable Location... spawnPoints) {
        this.spawnPoints = spawnPoints != null ? new ArrayList<>(Arrays.asList(spawnPoints)) : new ArrayList<>();
        return this;
    }
    
    /**
     * Sets the discussion location for the session.
     * 
     * @param discussionLocation the location where discussions take place
     * @return this builder instance for method chaining
     */
    @NotNull
    public SessionBuilder withDiscussionLocation(@Nullable Location discussionLocation) {
        this.discussionLocation = discussionLocation;
        return this;
    }
    
    /**
     * Sets the seat locations for the discussion phase.
     * 
     * @param seatLocations map of seat numbers to locations
     * @return this builder instance for method chaining
     */
    @NotNull
    public SessionBuilder withSeatLocations(@Nullable Map<Integer, Location> seatLocations) {
        this.seatLocations = seatLocations != null ? new HashMap<>(seatLocations) : new HashMap<>();
        return this;
    }
    
    /**
     * Sets custom game configuration for the session.
     * 
     * @param gameConfig the game configuration to use
     * @return this builder instance for method chaining
     */
    @NotNull
    public SessionBuilder withCustomConfig(@Nullable GameConfig gameConfig) {
        this.gameConfig = gameConfig != null ? gameConfig : new GameConfig.Builder().build();
        return this;
    }
    
    /**
     * Sets custom game configuration for the session.
     * 
     * @param gameConfig the game configuration to use
     * @return this builder instance for method chaining
     */
    @NotNull
    public SessionBuilder withConfig(@Nullable GameConfig gameConfig) {
        return withCustomConfig(gameConfig);
    }
    
    /**
     * Validates the current builder configuration.
     * 
     * @return Optional containing validation error, empty if valid
     */
    @NotNull
    public Optional<String> validate() {
        // Validate players
        if (players == null || players.isEmpty()) {
            return Optional.of("No players specified");
        }
        
        boolean hasValidPlayers = players.stream()
                .anyMatch(p -> p != null && p.isOnline());
        
        if (!hasValidPlayers) {
            return Optional.of("No valid online players specified");
        }
        
        // Validate spawn points
        if (spawnPoints == null || spawnPoints.isEmpty()) {
            return Optional.of("No spawn points specified");
        }
        
        boolean hasValidSpawns = spawnPoints.stream()
                .anyMatch(loc -> loc != null && loc.getWorld() != null);
        
        if (!hasValidSpawns) {
            return Optional.of("No valid spawn locations specified");
        }
        
        // Validate discussion location if provided
        if (discussionLocation != null && discussionLocation.getWorld() == null) {
            return Optional.of("Invalid discussion location");
        }
        
        // Validate seat locations if provided
        if (seatLocations != null) {
            boolean hasInvalidSeats = seatLocations.values().stream()
                    .anyMatch(loc -> loc == null || loc.getWorld() == null);
            
            if (hasInvalidSeats) {
                return Optional.of("Invalid seat locations detected");
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Creates and starts the game session with the configured settings.
     *
     * @return Optional containing the created session, empty if creation failed
     */
    @NotNull
    public Optional<ApiGameSession> start() {
        return startWithResult().getSession();
    }

    /**
     * Creates the game session without starting the game.
     * This is useful for testing scenarios where you need a configured session
     * but don't want to trigger full game initialization.
     *
     * @return Optional containing the created session, empty if creation failed
     * @since 0.9.5 (experimental)
     */
    @NotNull
    @Experimental
    public Optional<ApiGameSession> createSessionOnly() {
        // Validate configuration first
        Optional<String> validationError = validate();
        if (validationError.isPresent()) {
            return Optional.empty();
        }

        // Get plugin components
        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) return Optional.empty();

        SessionManager sessionManager = plugin.getSessionManager();
        if (sessionManager == null) return Optional.empty();

        GameSession session = null;
        try {
            // Create the session
            session = sessionManager.createSession(sessionName);
            if (session == null) {
                return Optional.empty();
            }

            // Add players to session
            for (Player player : players) {
                if (player != null && player.isOnline()) {
                    session.addPlayer(player);
                }
            }

            // Set session locations
            for (Location spawnPoint : spawnPoints) {
                if (spawnPoint != null && spawnPoint.getWorld() != null) {
                    session.addSpawnLocation(spawnPoint);
                }
            }

            if (discussionLocation != null && discussionLocation.getWorld() != null) {
                session.setDiscussionLocation(discussionLocation);
            }

            if (seatLocations != null && !seatLocations.isEmpty()) {
                for (Map.Entry<Integer, Location> entry : seatLocations.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null && entry.getValue().getWorld() != null) {
                        session.setSeatLocation(entry.getKey(), entry.getValue());
                    }
                }
            }

            // Mark session as active but don't start the game
            session.setActive(true);

            return Optional.of(new ApiGameSession(session));

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create session '" + sessionName + "': " + e.getMessage());

            // Clean up on failure
            if (session != null) {
                try {
                    sessionManager.removeSession(sessionName);
                } catch (Exception ignored) {}
            }

            return Optional.empty();
        }
    }
    
    /**
     * Creates and starts the game session with detailed error reporting.
     * 
     * @return SessionCreationResult containing success/failure information
     */
    @NotNull
    public SessionCreationResult startWithResult() {
        // Validate configuration first
        Optional<String> validationError = validate();
        if (validationError.isPresent()) {
            String errorMsg = validationError.get();
            SessionCreationResult.ErrorType errorType;

            // Map validation error messages to appropriate error types
            if (errorMsg.contains("players")) {
                errorType = SessionCreationResult.ErrorType.NO_PLAYERS;
            } else if (errorMsg.contains("spawn")) {
                errorType = SessionCreationResult.ErrorType.NO_SPAWN_POINTS;
            } else if (errorMsg.contains("discussion")) {
                errorType = SessionCreationResult.ErrorType.INVALID_DISCUSSION_LOCATION;
            } else {
                errorType = SessionCreationResult.ErrorType.INTERNAL_ERROR;
            }

            return SessionCreationResult.failure(errorType, errorMsg);
        }
        
        // Get plugin components
        Matchbox plugin = Matchbox.getInstance();
        if (plugin == null) {
            return SessionCreationResult.failure(
                SessionCreationResult.ErrorType.PLUGIN_NOT_AVAILABLE,
                "Matchbox plugin instance is not available"
            );
        }
        
        SessionManager sessionManager = plugin.getSessionManager();
        if (sessionManager == null) {
            return SessionCreationResult.failure(
                SessionCreationResult.ErrorType.SESSION_MANAGER_NOT_AVAILABLE,
                "Session manager is not available"
            );
        }
        
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null) {
            return SessionCreationResult.failure(
                SessionCreationResult.ErrorType.GAME_MANAGER_NOT_AVAILABLE,
                "Game manager is not available"
            );
        }
        
        GameSession session = null;
        try {
            // Create the session
            session = sessionManager.createSession(sessionName);
            if (session == null) {
                return SessionCreationResult.failure(
                    SessionCreationResult.ErrorType.SESSION_EXISTS,
                    "A session with name '" + sessionName + "' already exists"
                );
            }
            
            // Add players to session
            List<Player> validPlayers = new ArrayList<>();
            for (Player player : players) {
                if (player != null && player.isOnline()) {
                    session.addPlayer(player);
                    validPlayers.add(player);
                }
            }
            
            // Set session locations
            List<Location> validSpawnPoints = new ArrayList<>();
            for (Location spawnPoint : spawnPoints) {
                if (spawnPoint != null && spawnPoint.getWorld() != null) {
                    session.addSpawnLocation(spawnPoint);
                    validSpawnPoints.add(spawnPoint);
                }
            }
            
            if (discussionLocation != null && discussionLocation.getWorld() != null) {
                session.setDiscussionLocation(discussionLocation);
            }
            
            if (seatLocations != null && !seatLocations.isEmpty()) {
                for (Map.Entry<Integer, Location> entry : seatLocations.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null && entry.getValue().getWorld() != null) {
                        session.setSeatLocation(entry.getKey(), entry.getValue());
                    }
                }
            }
            
            // Mark session as active
            session.setActive(true);
            
            // Start the game
            gameManager.startRound(validPlayers, validSpawnPoints, discussionLocation, sessionName);
            
            return SessionCreationResult.success(new ApiGameSession(session));
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create session '" + sessionName + "': " + e.getMessage());
            
            // Clean up on failure
            if (session != null) {
                try {
                    sessionManager.removeSession(sessionName);
                } catch (Exception ignored) {}
            }
            
            return SessionCreationResult.failure(
                SessionCreationResult.ErrorType.INTERNAL_ERROR,
                "Internal error: " + e.getMessage()
            );
        }
    }
    
    /**
     * Creates a GameConfig builder for this session.
     * 
     * @return a new GameConfig.Builder instance
     */
    @NotNull
    public static GameConfig.Builder configBuilder() {
        return new GameConfig.Builder();
    }
}
