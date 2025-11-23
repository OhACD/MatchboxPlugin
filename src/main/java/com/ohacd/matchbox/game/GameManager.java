package com.ohacd.matchbox.game;

import com.ohacd.matchbox.game.action.PlayerActionHandler;
import com.ohacd.matchbox.game.hologram.HologramManager;
import com.ohacd.matchbox.game.lifecycle.GameLifecycleManager;
import com.ohacd.matchbox.game.phase.DiscussionPhaseHandler;
import com.ohacd.matchbox.game.phase.PhaseManager;
import com.ohacd.matchbox.game.phase.SwipePhaseHandler;
import com.ohacd.matchbox.game.phase.VotingPhaseHandler;
import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.InventoryManager;
import com.ohacd.matchbox.game.utils.MessageUtils;
import com.ohacd.matchbox.game.utils.NameTagManager;
import com.ohacd.matchbox.game.utils.ParticleUtils;
import com.ohacd.matchbox.game.utils.PlayerBackup;
import com.ohacd.matchbox.game.utils.Role;
import com.ohacd.matchbox.game.vote.VoteManager;
import com.ohacd.matchbox.game.win.WinConditionChecker;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.bukkit.Bukkit.getPlayer;

/**
 * Main game manager that coordinates all game systems.
 */
public class GameManager {
    private final Plugin plugin;
    private final HologramManager hologramManager;

    // Core systems (shared across all sessions)
    private final MessageUtils messageUtils;
    private final SwipePhaseHandler swipePhaseHandler;
    private final DiscussionPhaseHandler discussionPhaseHandler;
    private final VotingPhaseHandler votingPhaseHandler;
    private final InventoryManager inventoryManager;
    
    // Helper classes for code organization
    private final GameLifecycleManager lifecycleManager;
    private final PlayerActionHandler actionHandler;
    
    // Active game sessions - each session has its own game state and context
    private final Map<String, SessionGameContext> activeSessions = new ConcurrentHashMap<>();
    
    // Player backups for restoration (shared, but keyed by player UUID)
    private final Map<UUID, PlayerBackup> playerBackups = new ConcurrentHashMap<>();

    public GameManager(Plugin plugin, HologramManager hologramManager) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (hologramManager == null) {
            throw new IllegalArgumentException("HologramManager cannot be null");
        }
        
        this.plugin = plugin;
        this.hologramManager = hologramManager;

        // Initialize shared systems
        this.messageUtils = new MessageUtils(plugin);
        this.swipePhaseHandler = new SwipePhaseHandler(plugin, messageUtils);
        this.discussionPhaseHandler = new DiscussionPhaseHandler(plugin, messageUtils);
        this.votingPhaseHandler = new VotingPhaseHandler(plugin, messageUtils);
        this.inventoryManager = new InventoryManager(plugin);
        
        // Initialize helper classes
        this.lifecycleManager = new GameLifecycleManager(plugin, messageUtils, swipePhaseHandler, inventoryManager, playerBackups);
        this.actionHandler = new PlayerActionHandler(plugin);
    }
    
    /**
     * Gets the game context for a session, creating it if it doesn't exist.
     * Also validates that the session exists in SessionManager.
     */
    private SessionGameContext getOrCreateContext(String sessionName) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Session name cannot be null or empty");
        }
        
        // Validate session exists in SessionManager
        try {
            com.ohacd.matchbox.Matchbox matchboxPlugin = (com.ohacd.matchbox.Matchbox) plugin;
            com.ohacd.matchbox.game.session.SessionManager sessionManager = matchboxPlugin.getSessionManager();
            if (sessionManager != null && !sessionManager.sessionExists(sessionName)) {
                plugin.getLogger().warning("Attempted to create context for non-existent session: " + sessionName);
                return null;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to validate session existence: " + e.getMessage());
        }
        
        return activeSessions.computeIfAbsent(sessionName, name -> new SessionGameContext(plugin, name));
    }
    
    /**
     * Gets the game context for a session, or null if it doesn't exist.
     */
    public SessionGameContext getContext(String sessionName) {
        if (sessionName == null) {
            return null;
        }
        return activeSessions.get(sessionName);
    }
    
    /**
     * Gets the game context for a player by finding which session they're in.
     * Returns null if player is not in any active game.
     * Note: A player should only be in one session at a time.
     */
    public SessionGameContext getContextForPlayer(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        
        SessionGameContext found = null;
        for (SessionGameContext context : activeSessions.values()) {
            if (context != null && context.getGameState().getAllParticipatingPlayerIds().contains(playerId)) {
                if (found != null) {
                    // Edge case: Player found in multiple sessions (shouldn't happen, but handle it)
                    plugin.getLogger().warning("Player " + playerId + " found in multiple sessions! Sessions: " + 
                        found.getSessionName() + " and " + context.getSessionName());
                    // Return the first one found, but log the issue
                } else {
                    found = context;
                }
            }
        }
        return found;
    }
    
    /**
     * Removes and cleans up a session context.
     * Ensures all timers are cancelled and resources are freed.
     */
    private void removeContext(String sessionName) {
        if (sessionName == null) {
            return;
        }
        
        SessionGameContext context = activeSessions.remove(sessionName);
        if (context != null) {
            // Cancel all timers for this session
            try {
                swipePhaseHandler.cancelSwipeTask(sessionName);
            } catch (Exception e) {
                plugin.getLogger().warning("Error cancelling swipe task during context cleanup: " + e.getMessage());
            }
            
            try {
                discussionPhaseHandler.cancelDiscussionTask(sessionName);
            } catch (Exception e) {
                plugin.getLogger().warning("Error cancelling discussion task during context cleanup: " + e.getMessage());
            }
            
            try {
                votingPhaseHandler.cancelVotingTask(sessionName);
            } catch (Exception e) {
                plugin.getLogger().warning("Error cancelling voting task during context cleanup: " + e.getMessage());
            }
            
            // Clean up context resources
            context.cleanup();
            plugin.getLogger().info("Cleaned up context for session: " + sessionName);
        }
    }
    
    /**
     * Validates that a player is not already in another active game session.
     * Returns true if player can join, false if they're already in a game.
     */
    public boolean canPlayerJoinSession(UUID playerId, String targetSessionName) {
        if (playerId == null || targetSessionName == null) {
            return false;
        }
        
        SessionGameContext existingContext = getContextForPlayer(playerId);
        if (existingContext != null) {
            String existingSession = existingContext.getSessionName();
            if (!existingSession.equalsIgnoreCase(targetSessionName)) {
                // Player is already in a different session
                return false;
            }
            // Player is already in this session - that's fine
        }
        return true;
    }
    
    /**
     * Emergency cleanup: Removes all orphaned contexts and cancels all timers.
     * Should only be called on plugin disable or severe errors.
     */
    public void emergencyCleanup() {
        plugin.getLogger().warning("Performing emergency cleanup of all game sessions...");
        
        // Get all session names before clearing
        Set<String> sessionNames = new HashSet<>(activeSessions.keySet());
        
        for (String sessionName : sessionNames) {
            try {
                removeContext(sessionName);
            } catch (Exception e) {
                plugin.getLogger().severe("Error during emergency cleanup of session " + sessionName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Clear all player backups
        playerBackups.clear();
        
        plugin.getLogger().info("Emergency cleanup completed. Removed " + sessionNames.size() + " session(s).");
    }

    /**
     * Starts a new round with the given players.
     */
    public void startRound(Collection<Player> players) {
        startRound(players, null);
    }

    /**
     * Starts a new round with the given players and spawn locations.
     * Players will be teleported to random spawn locations.
     */
    public void startRound(Collection<Player> players, List<Location> spawnLocations) {
        startRound(players, spawnLocations, null);
    }

    /**
     * Starts a new round with the given players, spawn locations, and discussion location.
     * Players will be teleported to random spawn locations.
     */
    public void startRound(Collection<Player> players, List<Location> spawnLocations, Location discussionLocation) {
        startRound(players, spawnLocations, discussionLocation, null);
    }

    /**
     * Starts a new GAME with the given players, spawn locations, discussion location, and session name.
     * This initializes the entire game (not just a round).
     */
    public void startRound(Collection<Player> players, List<Location> spawnLocations, Location discussionLocation, String sessionName) {
        // Validate inputs
        if (players == null || players.isEmpty()) {
            plugin.getLogger().warning("Attempted to start game with no players");
            return;
        }

        if (spawnLocations == null || spawnLocations.isEmpty()) {
            plugin.getLogger().warning("Attempted to start game with no spawn locations");
            return;
        }
        
        if (sessionName == null || sessionName.trim().isEmpty()) {
            plugin.getLogger().warning("Attempted to start game with no session name");
            return;
        }

        // Get or create session context
        SessionGameContext context = getOrCreateContext(sessionName);
        if (context == null) {
            plugin.getLogger().warning("Failed to create context for session: " + sessionName);
            return;
        }

        // Edge case: Check if any player is already in another game
        for (Player player : players) {
            if (player == null || !player.isOnline()) {
                plugin.getLogger().warning("Skipping null or offline player during backup");
                continue;
            }
            
            UUID playerId = player.getUniqueId();
            
            // Edge case: Player already in another active game
            if (!canPlayerJoinSession(playerId, sessionName)) {
                SessionGameContext existingContext = getContextForPlayer(playerId);
                plugin.getLogger().warning("Player " + player.getName() + " is already in session '" + 
                    (existingContext != null ? existingContext.getSessionName() : "unknown") + 
                    "'. Removing from previous session.");
                
                // Remove from previous session
                if (existingContext != null) {
                    String oldSession = existingContext.getSessionName();
                    removePlayerFromGame(player);
                    // Also end the old game if it's active
                    if (existingContext.getGameState().isGameActive()) {
                        plugin.getLogger().info("Ending previous game for player " + player.getName());
                        endGame(oldSession);
                    }
                }
            }
        }

        // Use lifecycle manager to start the game
        lifecycleManager.startGame(context, players, spawnLocations, discussionLocation, sessionName);
        
        // Start first round
        startNewRound(sessionName);
    }

    /**
     * Starts a new round (after discussion phase ends).
     * This does NOT reassign roles or reset player list.
     */
    private void startNewRound(String sessionName) {
        SessionGameContext context = getContext(sessionName);
        if (context == null) {
            plugin.getLogger().warning("Cannot start new round - no context for session: " + sessionName);
            return;
        }
        
        GameState gameState = context.getGameState();
        
        // Check if game is still active (not ended)
        if (!gameState.isGameActive()) {
            plugin.getLogger().info("Cannot start new round - game is not active for session: " + sessionName);
            return;
        }
        
        // Check if session is still active
        try {
            com.ohacd.matchbox.Matchbox matchboxPlugin = (com.ohacd.matchbox.Matchbox) plugin;
            com.ohacd.matchbox.game.session.SessionManager sessionManager = matchboxPlugin.getSessionManager();
            if (sessionManager != null) {
                com.ohacd.matchbox.game.session.GameSession session = sessionManager.getSession(sessionName);
                if (session != null && !session.isActive()) {
                    plugin.getLogger().info("Cannot start new round - session is not active: " + sessionName);
                    return;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check session status: " + e.getMessage());
        }
        
        // Validate state before starting new round
        if (!gameState.validateState()) {
            // Broadcast only to players in this session
            Collection<Player> sessionPlayers = swipePhaseHandler.getAlivePlayerObjects(gameState.getAlivePlayerIds());
            if (sessionPlayers != null) {
                for (Player p : sessionPlayers) {
                    if (p != null && p.isOnline()) {
                        p.sendMessage("§c§lERROR: Game state is corrupted! Ending game.");
                    }
                }
            }
            plugin.getLogger().severe("Invalid game state detected for session " + sessionName + ": " + gameState.getDebugInfo());
            endGame(sessionName);
            return;
        }

        // Use lifecycle manager to start new round
        lifecycleManager.startNewRound(context, sessionName);
        
        // Teleport players to spawns
        lifecycleManager.teleportPlayersToSpawns(context, sessionName);

        // Start swipe phase (will set up inventories with papers)
        startSwipePhase(sessionName);
    }


    /**
     * Starts the swipe phase for a session.
     */
    public void startSwipePhase(String sessionName) {
        SessionGameContext context = getContext(sessionName);
        if (context == null) {
            plugin.getLogger().warning("Cannot start swipe phase - no context for session: " + sessionName);
            return;
        }
        
        GameState gameState = context.getGameState();
        PhaseManager phaseManager = context.getPhaseManager();
        
        phaseManager.setPhase(GamePhase.SWIPE);
        plugin.getLogger().info("Starting swipe phase for session '" + sessionName + "' - Round " + gameState.getCurrentRound());

        // Setup inventories for all players with their roles (give papers now)
        Collection<Player> alivePlayers = swipePhaseHandler.getAlivePlayerObjects(gameState.getAlivePlayerIds());
        if (alivePlayers != null && !alivePlayers.isEmpty()) {
            // Announce phase start only to players in this session
            for (Player p : alivePlayers) {
                if (p != null && p.isOnline()) {
                    p.sendMessage("§6§l>> SWIPE PHASE STARTED <<");
                }
            }
            
            Map<UUID, Role> roleMap = new HashMap<>();
            for (UUID playerId : gameState.getAlivePlayerIds()) {
                Role role = gameState.getRole(playerId);
                if (role != null) {
                    roleMap.put(playerId, role);
                }
            }
            inventoryManager.setupInventories(alivePlayers, roleMap);
        }

        // Hide the name tag for all alive players on phase start
        if (alivePlayers != null) {
            for (Player player : alivePlayers) {
                if (player != null && player.isOnline()) {
                    try {
                        NameTagManager.hideNameTag(player, sessionName);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to hide nametag for " + player.getName() + ": " + e.getMessage());
                    }
                }
            }
        }

        swipePhaseHandler.startSwipePhase(
                sessionName,
                gameState.getAlivePlayerIds(),
                () -> endSwipePhase(sessionName)
        );
    }

    /**
     * Start a swipe window for the player (silent). Duration in seconds.
     * Can be reactivated if window expires without successful use.
     */
    public void startSwipeWindow(Player spark, int seconds) {
        if (spark == null) return;
        UUID id = spark.getUniqueId();
        
        SessionGameContext context = getContextForPlayer(id);
        if (context == null) return;
        
        GameState gameState = context.getGameState();
        PhaseManager phaseManager = context.getPhaseManager();
        Map<UUID, Long> activeSwipeWindow = context.getActiveSwipeWindow();

        // Only in SWIPE phase and only if spark role
        if (!phaseManager.isPhase(GamePhase.SWIPE)) return;
        if (gameState.getRole(id) != Role.SPARK) return;
        // Don't check hasSwipedThisRound here - allow reactivation if not successfully used

        long expire = System.currentTimeMillis() + (seconds * 1000L);
        activeSwipeWindow.put(id, expire);

        // ensure cleanup after expiry
        new BukkitRunnable() {
            @Override
            public void run() {
                activeSwipeWindow.remove(id);
            }
        }.runTaskLater(plugin, seconds * 20L);
    }

    /**
     * Ends swipe window immediately (manual cleanup).
     */
    public void endSwipeWindow(UUID playerId) {
        SessionGameContext context = getContextForPlayer(playerId);
        if (context != null) {
            context.getActiveSwipeWindow().remove(playerId);
        }
    }

    /**
     * Returns whether the given player has an active swipe window.
     */
    public boolean isSwipeWindowActive(UUID playerId) {
        SessionGameContext context = getContextForPlayer(playerId);
        if (context == null) return false;
        return actionHandler.isSwipeWindowActive(context, playerId);
    }

    /**
     * Start a cure window for the medic (silent). Duration in seconds.
     * Can be reactivated if window expires without successful use.
     */
    public void startCureWindow(Player medic, int seconds) {
        if (medic == null) return;
        UUID id = medic.getUniqueId();
        
        SessionGameContext context = getContextForPlayer(id);
        if (context == null) return;
        
        GameState gameState = context.getGameState();
        PhaseManager phaseManager = context.getPhaseManager();
        Map<UUID, Long> activeCureWindow = context.getActiveCureWindow();

        // Only in SWIPE phase and only if medic role
        if (!phaseManager.isPhase(GamePhase.SWIPE)) return;
        if (gameState.getRole(id) != Role.MEDIC) return;
        // Don't check hasCuredThisRound here - allow reactivation if not successfully used

        long expire = System.currentTimeMillis() + (seconds * 1000L);
        activeCureWindow.put(id, expire);

        // ensure cleanup after expiry
        new BukkitRunnable() {
            @Override
            public void run() {
                activeCureWindow.remove(id);
            }
        }.runTaskLater(plugin, seconds * 20L);
    }

    /**
     * Activates Healing Sight for the medic.
     * Shows red particles on all infected players for 15 seconds (only visible to medic).
     * This is separate from the cure ability.
     * Can only be used once per round.
     */
    public void activateHealingSight(Player medic) {
        if (medic == null || !medic.isOnline()) return;
        
        SessionGameContext context = getContextForPlayer(medic.getUniqueId());
        if (context == null) return;
        
        GameState gameState = context.getGameState();
        PhaseManager phaseManager = context.getPhaseManager();

        // Only in SWIPE phase and only if medic role
        if (!phaseManager.isPhase(GamePhase.SWIPE)) return;
        if (gameState.getRole(medic.getUniqueId()) != Role.MEDIC) return;

        // Check if already used this round
        if (gameState.hasUsedHealingSightThisRound(medic.getUniqueId())) {
            return;
        }

        // Mark as used
        gameState.markUsedHealingSight(medic.getUniqueId());

        // Show infected players to medic
        showInfectedPlayersToMedic(medic, context);
    }

    /**
     * Activates Hunter Vision for the spark.
     * Shows particles on all alive players for 15 seconds (only visible to spark).
     * Spark cannot see nametags - only particles are shown.
     * Can only be used once per round.
     */
    public void activateHunterVision(Player spark) {
        if (spark == null || !spark.isOnline()) return;
        
        SessionGameContext context = getContextForPlayer(spark.getUniqueId());
        if (context == null) return;
        
        GameState gameState = context.getGameState();
        PhaseManager phaseManager = context.getPhaseManager();

        // Only in SWIPE phase and only if spark role
        if (!phaseManager.isPhase(GamePhase.SWIPE)) return;
        if (gameState.getRole(spark.getUniqueId()) != Role.SPARK) return;

        // Check if already used this round
        if (gameState.hasUsedHunterVisionThisRound(spark.getUniqueId())) {
            return;
        }

        // Mark as used
        gameState.markUsedHunterVision(spark.getUniqueId());

        // Show glow on all alive players (only visible to spark)
        showGlowOnPlayers(spark, context);
    }

    /**
     * Shows red particles on all players with pending deaths, visible only to the medic.
     * Particles last for 15 seconds.
     */
    private void showInfectedPlayersToMedic(Player medic, SessionGameContext context) {
        if (medic == null || !medic.isOnline()) return;
        
        GameState gameState = context.getGameState();

        // Get all alive players with pending deaths
        java.util.List<Player> infectedPlayers = new java.util.ArrayList<>();
        Set<UUID> alivePlayerIds = gameState.getAlivePlayerIds();
        if (alivePlayerIds != null) {
            for (UUID playerId : alivePlayerIds) {
                if (playerId == null) continue;
                if (gameState.hasPendingDeath(playerId)) {
                    Player infected = getPlayer(playerId);
                    if (infected != null && infected.isOnline()) {
                        infectedPlayers.add(infected);
                    }
                }
            }
        }

        if (infectedPlayers.isEmpty()) {
            // No infected players - medic can see this (no particles = no one to cure)
            return;
        }

        // Show red particles on all infected players for 15 seconds
        // Only the medic can see these particles (recording-safe)
        ParticleUtils.showRedParticlesOnPlayers(medic, infectedPlayers, 15, plugin);
        
        plugin.getLogger().info("Showing " + infectedPlayers.size() + " infected player(s) to medic " + medic.getName());
    }

    /**
     * Shows particles on all alive players, visible only to the spark.
     * Particles last for 15 seconds.
     * Note: Spark cannot see nametags - only particles are shown.
     */
    private void showGlowOnPlayers(Player spark, SessionGameContext context) {
        if (spark == null || !spark.isOnline()) return;
        
        GameState gameState = context.getGameState();

        // Get all alive players (excluding spark)
        java.util.List<Player> alivePlayers = new java.util.ArrayList<>();
        Set<UUID> alivePlayerIds = gameState.getAlivePlayerIds();
        if (alivePlayerIds != null && spark != null) {
            UUID sparkId = spark.getUniqueId();
            for (UUID playerId : alivePlayerIds) {
                if (playerId == null) continue;
                if (!playerId.equals(sparkId)) {
                    Player player = getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        alivePlayers.add(player);
                    }
                }
            }
        }

        if (alivePlayers.isEmpty()) {
            return;
        }

        // Show particles around players as a visual indicator
        // This is visible only to the spark (using viewer.spawnParticle)
        // No nametag visibility changes - nametags remain hidden for spark
        for (Player target : alivePlayers) {
            ParticleUtils.showRedParticlesOnPlayer(spark, target, 15, plugin);
        }

        plugin.getLogger().info("Showing particles on " + alivePlayers.size() + " player(s) to spark " + spark.getName());
    }

    /**
     * Ends cure window immediately (manual cleanup).
     */
    public void endCureWindow(UUID playerId) {
        SessionGameContext context = getContextForPlayer(playerId);
        if (context != null) {
            context.getActiveCureWindow().remove(playerId);
        }
    }

    /**
     * Returns whether the given player has an active cure window.
     */
    public boolean isCureWindowActive(UUID playerId) {
        SessionGameContext context = getContextForPlayer(playerId);
        if (context == null) return false;
        return actionHandler.isCureWindowActive(context, playerId);
    }

    /**
     * Called to end the swipe phase. Does NOT apply pending deaths here.
     * Discussion phase start will apply pending deaths so infected players are removed
     * before discussion begins.
     */
    public void endSwipePhase(String sessionName) {
        SessionGameContext context = getContext(sessionName);
        if (context == null) {
            plugin.getLogger().warning("Cannot end swipe phase - no context for session: " + sessionName);
            return;
        }
        
        GameState gameState = context.getGameState();
        PhaseManager phaseManager = context.getPhaseManager();
        
        plugin.getLogger().info("Ending swipe phase for session '" + sessionName + "' - Round " + gameState.getCurrentRound());

        // Cancel swipe timer first to prevent it from continuing
        swipePhaseHandler.cancelSwipeTask(sessionName);

        // Transition to discussion but do not apply pending deaths here.
        phaseManager.setPhase(GamePhase.DISCUSSION);
        
        // Broadcast only to players in this session
        Collection<Player> sessionPlayers = swipePhaseHandler.getAlivePlayerObjects(gameState.getAlivePlayerIds());
        if (sessionPlayers != null) {
            for (Player p : sessionPlayers) {
                if (p != null && p.isOnline()) {
                    p.sendMessage("§e§l>> SWIPE PHASE ENDED <<");
                }
            }
        }

        // Clear actionbars, stop timers, etc. (assume swipePhaseHandler cleared by caller)
        // Start the discussion phase which will apply pending deaths at its start
        startDiscussionPhase(sessionName);
    }

    /**
     * Starts the discussion phase and applies pending deaths immediately before players join discussion.
     */
    private void startDiscussionPhase(String sessionName) {
        SessionGameContext context = getContext(sessionName);
        if (context == null) {
            plugin.getLogger().warning("Cannot start discussion phase - no context for session: " + sessionName);
            return;
        }
        
        GameState gameState = context.getGameState();
        
        // Apply ALL pending deaths now (regardless of timestamp) so infected players do not participate in discussion
        // This ensures pending deaths scheduled during swipe phase are applied at discussion start
        Set<UUID> allPendingDeaths = new HashSet<>();
        Set<UUID> alivePlayerIds = gameState.getAlivePlayerIds();
        if (alivePlayerIds != null) {
            for (UUID playerId : alivePlayerIds) {
                if (playerId != null && gameState.hasPendingDeath(playerId)) {
                    allPendingDeaths.add(playerId);
                }
            }
        }
        
        if (!allPendingDeaths.isEmpty()) {
            plugin.getLogger().info("Applying pending deaths for " + allPendingDeaths.size() + " players at discussion start in session: " + sessionName);
        }
        
        for (UUID victimId : allPendingDeaths) {
            if (victimId == null) continue;
            
            if (!gameState.isAlive(victimId)) {
                gameState.removePendingDeath(victimId);
                continue;
            }
            Player victim = getPlayer(victimId);
            if (victim != null && victim.isOnline()) {
                try {
                    eliminatePlayer(sessionName, victim); // ensure this removes them from alive and cleans state
                } catch (Exception e) {
                    plugin.getLogger().warning("Error eliminating player " + victim.getName() + ": " + e.getMessage());
                    // Fallback cleanup
                    gameState.removeAlivePlayer(victimId);
                    gameState.removePendingDeath(victimId);
                }
            } else {
                // server-side cleanup if offline
                gameState.removeAlivePlayer(victimId);
                gameState.removePendingDeath(victimId);
            }
            // Ensure pending death is removed
            gameState.removePendingDeath(victimId);
        }

        // Clear infected flags for the round
        gameState.clearInfectedThisRound();

        // Clear ALL inventory for all alive players (they should have nothing during discussion)
        alivePlayerIds = gameState.getAlivePlayerIds();
        if (alivePlayerIds != null) {
            for (UUID playerId : alivePlayerIds) {
                if (playerId == null) continue;
                Player player = org.bukkit.Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    try {
                        // Clear entire inventory
                        player.getInventory().clear();
                        player.updateInventory();
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to clear inventory for " + player.getName() + ": " + e.getMessage());
                    }
                }
            }
        }

        // Teleport alive players to discussion location
        Location discussionLocation = context.getCurrentDiscussionLocation();
        if (discussionLocation != null && discussionLocation.getWorld() != null) {
            if (alivePlayerIds != null) {
                for (UUID playerId : alivePlayerIds) {
                    if (playerId == null) continue;
                    Player player = org.bukkit.Bukkit.getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        try {
                            player.teleport(discussionLocation);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to teleport player " + player.getName() + " to discussion: " + e.getMessage());
                        }
                    }
                }
            }
        } else {
            plugin.getLogger().warning("Cannot teleport to discussion: location is null or world is null for session: " + sessionName);
        }

        // Start the discussion timer and supply callback to endDiscussionPhase
        discussionPhaseHandler.startDiscussionPhase(sessionName, gameState.getAlivePlayerIds(), () -> endDiscussionPhase(sessionName));
    }

    /**
     * Ends the discussion phase and starts voting phase.
     * Made public for skip command.
     */
    public void endDiscussionPhase(String sessionName) {
        SessionGameContext context = getContext(sessionName);
        if (context == null) {
            plugin.getLogger().warning("Cannot end discussion phase - no context for session: " + sessionName);
            return;
        }
        
        GameState gameState = context.getGameState();
        VoteManager voteManager = context.getVoteManager();
        
        plugin.getLogger().info("Ending discussion phase for session '" + sessionName + "' - Round " + gameState.getCurrentRound());
        
        // Cancel discussion timer first to prevent it from continuing
        discussionPhaseHandler.cancelDiscussionTask(sessionName);
        
        // Broadcast only to players in this session
        Collection<Player> sessionPlayers = swipePhaseHandler.getAlivePlayerObjects(gameState.getAlivePlayerIds());
        if (sessionPlayers != null) {
            for (Player p : sessionPlayers) {
                if (p != null && p.isOnline()) {
                    p.sendMessage("§e§l>> DISCUSSION PHASE ENDED <<");
                }
            }
        }

        // Clear any previous votes
        voteManager.clearVotes();

        // Start voting phase
        startVotingPhase(sessionName);
    }

    /**
     * Starts the voting phase.
     */
    private void startVotingPhase(String sessionName) {
        SessionGameContext context = getContext(sessionName);
        if (context == null) {
            plugin.getLogger().warning("Cannot start voting phase - no context for session: " + sessionName);
            return;
        }
        
        GameState gameState = context.getGameState();
        PhaseManager phaseManager = context.getPhaseManager();
        
        phaseManager.setPhase(GamePhase.VOTING);
        plugin.getLogger().info("Starting voting phase for session '" + sessionName + "' - Round " + gameState.getCurrentRound());

        // Clear all inventories first, then give ONLY voting papers
        Collection<Player> alivePlayers = swipePhaseHandler.getAlivePlayerObjects(gameState.getAlivePlayerIds());
        if (alivePlayers != null && !alivePlayers.isEmpty()) {
            for (Player player : alivePlayers) {
                if (player != null && player.isOnline()) {
                    try {
                        // Clear entire inventory first
                        player.getInventory().clear();
                        player.updateInventory();
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to clear inventory for " + player.getName() + " before voting: " + e.getMessage());
                    }
                    // Give ONLY voting papers
                    inventoryManager.giveVotingPapers(player, alivePlayers);
                }
            }
        }

        // Start the voting timer and supply callback to endVotingPhase
        votingPhaseHandler.startVotingPhase(sessionName, gameState.getAlivePlayerIds(), () -> endVotingPhase(sessionName));
    }

    /**
     * Ends the voting phase, resolves votes, and eliminates the most-voted player.
     * Made public for skip command.
     */
    public void endVotingPhase(String sessionName) {
        SessionGameContext context = getContext(sessionName);
        if (context == null) {
            plugin.getLogger().warning("Cannot end voting phase - no context for session: " + sessionName);
            return;
        }
        
        GameState gameState = context.getGameState();
        
        plugin.getLogger().info("Ending voting phase for session '" + sessionName + "' - Round " + gameState.getCurrentRound());
        
        // Cancel voting timer first to prevent it from continuing
        votingPhaseHandler.cancelVotingTask(sessionName);
        
        // Broadcast only to players in this session
        Collection<Player> sessionPlayers = swipePhaseHandler.getAlivePlayerObjects(gameState.getAlivePlayerIds());
        if (sessionPlayers != null) {
            for (Player p : sessionPlayers) {
                if (p != null && p.isOnline()) {
                    p.sendMessage("§c§l>> VOTING PHASE ENDED <<");
                }
            }
        }

        // Clear voting papers from all players
        Collection<Player> alivePlayers = swipePhaseHandler.getAlivePlayerObjects(gameState.getAlivePlayerIds());
        if (alivePlayers != null) {
            inventoryManager.clearAllVotingPapers(alivePlayers);
        }

        // Resolve votes and eliminate
        try {
            resolveVotes(sessionName);
        } catch (Exception e) {
            plugin.getLogger().severe("Error resolving votes: " + e.getMessage());
            e.printStackTrace();
        }

        // Check for win condition after voting
        try {
            if (checkForWin(sessionName)) {
                plugin.getLogger().info("Win condition met - game ended, not starting new round for session: " + sessionName);
                return; // Game ended
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error checking win conditions: " + e.getMessage());
            e.printStackTrace();
            // Don't continue to next round if check fails - end game instead
            endGame(sessionName);
            return;
        }

        // Double-check game is still active before starting new round
        if (!gameState.isGameActive()) {
            plugin.getLogger().info("Game is not active after voting - not starting new round for session: " + sessionName);
            return;
        }

        // Start next round
        try {
            startNewRound(sessionName);
        } catch (Exception e) {
            plugin.getLogger().severe("Error starting new round: " + e.getMessage());
            e.printStackTrace();
            // Attempt to end game gracefully
            endGame(sessionName);
        }
    }

    /**
     * Resolves votes and eliminates the most-voted player.
     * Handles ties by random elimination.
     */
    private void resolveVotes(String sessionName) {
        SessionGameContext context = getContext(sessionName);
        if (context == null) {
            plugin.getLogger().warning("Cannot resolve votes - no context for session: " + sessionName);
            return;
        }
        
        GameState gameState = context.getGameState();
        VoteManager voteManager = context.getVoteManager();
        
        if (voteManager == null) {
            plugin.getLogger().warning("VoteManager is null, cannot resolve votes for session: " + sessionName);
            return;
        }
        
        UUID mostVoted = voteManager.getMostVotedPlayer();
        List<UUID> tied = voteManager.getTiedPlayers();
        int maxVotes = voteManager.getMaxVoteCount();
        
        if (tied == null) {
            tied = Collections.emptyList();
        }

        Collection<Player> sessionPlayers = swipePhaseHandler.getAlivePlayerObjects(gameState.getAlivePlayerIds());
        
        if (mostVoted == null && tied.isEmpty()) {
            // No votes cast - skip elimination
            if (sessionPlayers != null) {
                for (Player p : sessionPlayers) {
                    if (p != null && p.isOnline()) {
                        p.sendMessage("§eNo votes were cast. No one is eliminated.");
                    }
                }
            }
            plugin.getLogger().info("No votes cast this round for session " + sessionName + ". Total voters: " + voteManager.getVoters().size() + 
                ", Alive players: " + gameState.getAlivePlayerCount());
            return;
        }

        UUID toEliminate = null;
        String resultMessage;

        if (mostVoted != null) {
            // Clear winner
            toEliminate = mostVoted;
            Player eliminated = getPlayer(toEliminate);
            if (eliminated != null) {
                resultMessage = "§c" + eliminated.getName() + " was eliminated with " + maxVotes + " vote(s)!";
            } else {
                resultMessage = "§cA player was eliminated with " + maxVotes + " vote(s)!";
            }
        } else if (!tied.isEmpty()) {
            // Tie - randomly eliminate one of the tied players
            Collections.shuffle(tied);
            toEliminate = tied.get(0);
            Player eliminated = getPlayer(toEliminate);
            if (eliminated != null) {
                resultMessage = "§cTie! " + eliminated.getName() + " was randomly eliminated with " + maxVotes + " vote(s)!";
            } else {
                resultMessage = "§cTie! A player was randomly eliminated with " + maxVotes + " vote(s)!";
            }
        } else {
            // Should not happen, but handle gracefully
            if (sessionPlayers != null) {
                for (Player p : sessionPlayers) {
                    if (p != null && p.isOnline()) {
                        p.sendMessage("§eVoting completed but no elimination occurred.");
                    }
                }
            }
            return;
        }

        // Eliminate the player
        if (toEliminate != null) {
            Player player = getPlayer(toEliminate);
            if (player != null && player.isOnline()) {
                try {
                    eliminatePlayer(sessionName, player);
                    if (sessionPlayers != null) {
                        for (Player p : sessionPlayers) {
                            if (p != null && p.isOnline()) {
                                p.sendMessage(resultMessage);
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error eliminating player " + player.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                    // Fallback: remove from state
                    gameState.removeAlivePlayer(toEliminate);
                    if (sessionPlayers != null) {
                        for (Player p : sessionPlayers) {
                            if (p != null && p.isOnline()) {
                                p.sendMessage(resultMessage);
                            }
                        }
                    }
                }
            } else {
                // Player offline - remove from state
                gameState.removeAlivePlayer(toEliminate);
                if (sessionPlayers != null) {
                    for (Player p : sessionPlayers) {
                        if (p != null && p.isOnline()) {
                            p.sendMessage(resultMessage);
                        }
                    }
                }
            }
        }
    }

    /**
     * Handles a vote action from a player.
     * Returns true if vote was registered, false otherwise.
     */
    public boolean handleVote(Player voter, Player target) {
        if (voter == null || target == null) return false;

        SessionGameContext context = getContextForPlayer(voter.getUniqueId());
        if (context == null) return false;
        
        // Both players must be in the same session
        if (!context.getGameState().getAllParticipatingPlayerIds().contains(target.getUniqueId())) {
            return false;
        }
        
        GameState gameState = context.getGameState();
        PhaseManager phaseManager = context.getPhaseManager();
        VoteManager voteManager = context.getVoteManager();

        // Phase check
        if (!phaseManager.isPhase(GamePhase.VOTING)) {
            return false;
        }

        UUID voterId = voter.getUniqueId();
        UUID targetId = target.getUniqueId();

        // Check if voter is alive
        if (!gameState.isAlive(voterId)) {
            return false;
        }

        // Check if target is alive
        if (!gameState.isAlive(targetId)) {
            return false;
        }

        // Register the vote
        boolean success = voteManager.registerVote(voterId, targetId);
        
        if (success) {
            // Silent success - no message to prevent giving away information
            plugin.getLogger().info("Vote registered in session '" + context.getSessionName() + "': " + voter.getName() + " voted for " + target.getName());
        }

        return success;
    }

    /**
     * Handles a cure action from a medic.
     * Silent: no messages/holograms to medic/target.
     * Removes pending death if target has one.
     */
    public void handleCure(Player medic, Player target) {
        if (medic == null || target == null) return;

        SessionGameContext context = getContextForPlayer(medic.getUniqueId());
        if (context == null) return;
        
        actionHandler.handleCure(context, medic, target);
    }

    /**
     * Handles a swipe action from a player.
     * Silent: no messages/holograms to shooter/target.
     * Infection recorded and pending death scheduled to be applied at discussion start.
     */
    public void handleSwipe(Player shooter, Player target) {
        if (shooter == null || target == null) return;

        SessionGameContext context = getContextForPlayer(shooter.getUniqueId());
        if (context == null) return;
        
        actionHandler.handleSwipe(context, shooter, target);
    }

    /**
     * Eliminates a player from the game.
     */
    public void eliminatePlayer(String sessionName, Player player) {
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Attempted to eliminate null or offline player");
            return;
        }

        SessionGameContext context = getContext(sessionName);
        if (context == null) {
            plugin.getLogger().warning("Cannot eliminate player - no context for session: " + sessionName);
            return;
        }
        
        GameState gameState = context.getGameState();
        UUID playerId = player.getUniqueId();
        
        // Remove from alive set
        gameState.removeAlivePlayer(playerId);
        
        // when a player gets eliminated they show their name tag
        try {
            NameTagManager.showNameTag(player);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to show nametag for eliminated player " + player.getName() + ": " + e.getMessage());
        }
        
        try {
            player.sendMessage("§cYou have been eliminated!");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send elimination message to " + player.getName() + ": " + e.getMessage());
        }

        // Set spectator mode
        try {
            player.setGameMode(GameMode.SPECTATOR);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to set spectator mode for " + player.getName() + ": " + e.getMessage());
        }

        // TODO: Teleport eliminated player to spectator area (if discussion location is set)
        // For now, they stay where they are

        // Log elimination
        plugin.getLogger().info("Player " + player.getName() + " eliminated in session '" + sessionName + "'. Remaining alive: " + gameState.getAlivePlayerCount());

        // Check win conditions
        checkForWin(sessionName);
    }

    /**
     * Checks if a win condition has been met and handles it.
     * Returns true if game ended.
     */
    public boolean checkForWin(String sessionName) {
        SessionGameContext context = getContext(sessionName);
        if (context == null) {
            return false;
        }
        
        WinConditionChecker winConditionChecker = context.getWinConditionChecker();
        WinConditionChecker.WinResult result = winConditionChecker.checkWinConditions();

        if (result != null) {
            // Broadcast only to players in this session
            Collection<Player> sessionPlayers = swipePhaseHandler.getAlivePlayerObjects(context.getGameState().getAlivePlayerIds());
            if (sessionPlayers != null) {
                for (Player p : sessionPlayers) {
                    if (p != null && p.isOnline()) {
                        p.sendMessage(result.getMessage());
                    }
                }
            }
            endGame(sessionName);
            return true;
        }
        return false;
    }
    
    /**
     * Removes a player from an active game and restores their state.
     * This is called when a player uses /matchbox leave during an active game.
     */
    public boolean removePlayerFromGame(Player player) {
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Cannot remove null or offline player from game");
            return false;
        }
        
        UUID playerId = player.getUniqueId();
        
        SessionGameContext context = getContextForPlayer(playerId);
        if (context == null) {
            plugin.getLogger().info("Player " + player.getName() + " is not in any active game");
            return false;
        }
        
        GameState gameState = context.getGameState();
        String sessionName = context.getSessionName();
        
        // Check if player is in an active game
        if (!gameState.isGameActive()) {
            plugin.getLogger().info("No active game to remove player from");
            return false;
        }
        
        if (!gameState.getAllParticipatingPlayerIds().contains(playerId)) {
            plugin.getLogger().info("Player " + player.getName() + " is not in the active game");
            return false;
        }
        
        plugin.getLogger().info("Removing player " + player.getName() + " from active game in session: " + sessionName);
        
        // Restore nametag
        try {
            NameTagManager.showNameTag(player);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to restore nametag for " + player.getName() + ": " + e.getMessage());
        }
        
        // Remove pending death if they had one
        if (gameState.hasPendingDeath(playerId)) {
            gameState.removePendingDeath(playerId);
        }
        
        // Remove from alive players
        gameState.removeAlivePlayer(playerId);
        
        // Also remove from session if they're in one
        try {
            com.ohacd.matchbox.Matchbox matchboxPlugin = (com.ohacd.matchbox.Matchbox) plugin;
            com.ohacd.matchbox.game.session.SessionManager sessionManager = matchboxPlugin.getSessionManager();
            if (sessionManager != null) {
                com.ohacd.matchbox.game.session.GameSession session = sessionManager.getSession(sessionName);
                if (session != null && session.hasPlayer(player)) {
                    session.removePlayer(player);
                    plugin.getLogger().info("Removed player " + player.getName() + " from session " + sessionName);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error removing player from session: " + e.getMessage());
        }
        
        // Clear game items
        try {
            inventoryManager.clearGameItems(player);
            inventoryManager.clearVotingPapers(player);
        } catch (Exception e) {
            plugin.getLogger().warning("Error clearing game items for " + player.getName() + ": " + e.getMessage());
        }
        
        // Restore from backup if available
        PlayerBackup backup = playerBackups.get(playerId);
        if (backup != null) {
            try {
                if (!backup.restore(player)) {
                    plugin.getLogger().warning("Failed to restore backup for " + player.getName());
                    // Fallback: reset to survival mode and clear inventory
                    try {
                        player.setGameMode(GameMode.SURVIVAL);
                        player.getInventory().clear();
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to reset game mode/inventory for " + player.getName() + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error restoring backup for " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
                // Fallback: reset to survival mode and clear inventory
                try {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.getInventory().clear();
                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed to reset game mode/inventory for " + player.getName() + ": " + ex.getMessage());
                }
            }
        } else {
            // Fallback: reset to survival mode and clear inventory
            try {
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to reset game mode/inventory for " + player.getName() + ": " + e.getMessage());
            }
        }
        
        // Remove from backups
        playerBackups.remove(playerId);
        
        // Send feedback
        try {
            player.sendMessage("§aYou have been removed from the game and returned to normal state.");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send removal message to " + player.getName() + ": " + e.getMessage());
        }
        
        // Check for win condition after player leaves
        if (gameState.isGameActive()) {
            checkForWin(sessionName);
            
            // Also check if session should be ended (no players left)
            try {
                com.ohacd.matchbox.Matchbox matchboxPlugin = (com.ohacd.matchbox.Matchbox) plugin;
                com.ohacd.matchbox.game.session.SessionManager sessionManager = matchboxPlugin.getSessionManager();
                if (sessionManager != null) {
                    com.ohacd.matchbox.game.session.GameSession session = sessionManager.getSession(sessionName);
                    if (session != null) {
                        // Check if session has no players left
                        if (session.getPlayerCount() == 0) {
                            session.setActive(false);
                            plugin.getLogger().info("Session '" + sessionName + "' ended - no players left after player removal");
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error checking session status after player removal: " + e.getMessage());
            }
        }
        
        return true;
    }

    /**
     * Ends the game and resets all state for a specific session.
     * This method is now PUBLIC so it can be called from commands.
     */
    public void endGame(String sessionName) {
        SessionGameContext context = getContext(sessionName);
        if (context == null) {
            plugin.getLogger().warning("Cannot end game - no context for session: " + sessionName);
            return;
        }
        
        GameState gameState = context.getGameState();
        PhaseManager phaseManager = context.getPhaseManager();
        
        plugin.getLogger().info("Ending game for session '" + sessionName + "'. Final state: " + gameState.getDebugInfo());

        // Restore all participating players' nametags, game modes, and inventories
        Set<UUID> allParticipatingIds = gameState.getAllParticipatingPlayerIds();
        if (allParticipatingIds != null) {
            for (UUID playerId : allParticipatingIds) {
                if (playerId == null) continue;
                Player player = org.bukkit.Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    try {
                        // Clear all game items first
                        inventoryManager.clearGameItems(player);
                        inventoryManager.clearVotingPapers(player);
                        player.getInventory().clear(); // Clear everything
                        player.updateInventory();
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to clear game items for " + player.getName() + ": " + e.getMessage());
                    }

                    try {
                        // Show nametag
                        NameTagManager.showNameTag(player);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to restore nametag for " + player.getName() + ": " + e.getMessage());
                    }

                    // Restore from backup if available
                    PlayerBackup backup = playerBackups.get(playerId);
                    if (backup != null) {
                        try {
                            if (!backup.restore(player)) {
                                plugin.getLogger().warning("Failed to restore backup for " + player.getName());
                                // Fallback: reset to survival mode and clear inventory
                                try {
                                    player.setGameMode(GameMode.SURVIVAL);
                                    player.getInventory().clear();
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Failed to reset game mode/inventory for " + player.getName() + ": " + e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            plugin.getLogger().severe("Error restoring backup for " + player.getName() + ": " + e.getMessage());
                            e.printStackTrace();
                            // Fallback: reset to survival mode and clear inventory
                            try {
                                player.setGameMode(GameMode.SURVIVAL);
                                player.getInventory().clear();
                            } catch (Exception ex) {
                                plugin.getLogger().warning("Failed to reset game mode/inventory for " + player.getName() + ": " + ex.getMessage());
                            }
                        }
                    } else {
                        // Fallback: reset to survival mode and clear inventory
                        try {
                            player.setGameMode(GameMode.SURVIVAL);
                            player.getInventory().clear();
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to reset game mode/inventory for " + player.getName() + ": " + e.getMessage());
                        }
                    }

                    // Send feedback
                    try {
                        player.sendMessage("§aYou have been returned to normal state.");
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to send restoration message to " + player.getName() + ": " + e.getMessage());
                    }
                    
                    // Remove from backups (only for this session's players)
                    playerBackups.remove(playerId);
                }
            }
        }

        // Cancel any running timers for this session
        try {
            swipePhaseHandler.cancelSwipeTask(sessionName);
        } catch (Exception e) {
            plugin.getLogger().warning("Error cancelling swipe task: " + e.getMessage());
        }
        
        try {
            discussionPhaseHandler.cancelDiscussionTask(sessionName);
        } catch (Exception e) {
            plugin.getLogger().warning("Error cancelling discussion task: " + e.getMessage());
        }
        
        try {
            votingPhaseHandler.cancelVotingTask(sessionName);
        } catch (Exception e) {
            plugin.getLogger().warning("Error cancelling voting task: " + e.getMessage());
        }

        // Reset phase and game state
        phaseManager.reset();
        gameState.clearGameState();
        
        // Clean up context
        removeContext(sessionName);
        
        // Mark session as inactive and ensure it stays terminated
        try {
            // Access SessionManager via plugin instance
            com.ohacd.matchbox.Matchbox matchboxPlugin = (com.ohacd.matchbox.Matchbox) plugin;
            com.ohacd.matchbox.game.session.SessionManager sessionManager = matchboxPlugin.getSessionManager();
            if (sessionManager != null) {
                com.ohacd.matchbox.game.session.GameSession session = sessionManager.getSession(sessionName);
                if (session != null) {
                    session.setActive(false);
                    // Also check if session has no players and remove it
                    if (session.getPlayerCount() == 0) {
                        sessionManager.removeSession(sessionName);
                        plugin.getLogger().info("Removed empty session '" + sessionName + "' after game end");
                    } else {
                        plugin.getLogger().info("Marked session '" + sessionName + "' as inactive after game end");
                    }
                } else {
                    plugin.getLogger().warning("Session '" + sessionName + "' not found when trying to mark inactive");
                }
            } else {
                plugin.getLogger().warning("SessionManager is null when trying to mark session inactive");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to mark session as inactive: " + e.getMessage());
            e.printStackTrace();
        }

        plugin.getLogger().info("Game ended successfully for session: " + sessionName);
    }

    // Getters for external access (for backward compatibility - returns first active session or null)
    public GameState getGameState() {
        // Return first active session's game state for backward compatibility
        // Note: This is deprecated for parallel sessions - use getContextForPlayer or getContext instead
        if (activeSessions.isEmpty()) {
            return null;
        }
        return activeSessions.values().iterator().next().getGameState();
    }

    public PhaseManager getPhaseManager() {
        // Return first active session's phase manager for backward compatibility
        // Note: This is deprecated for parallel sessions - use getContextForPlayer or getContext instead
        if (activeSessions.isEmpty()) {
            return null;
        }
        return activeSessions.values().iterator().next().getPhaseManager();
    }
    
    public VoteManager getVoteManager() {
        // Return first active session's vote manager for backward compatibility
        // Note: This is deprecated for parallel sessions - use getContextForPlayer or getContext instead
        if (activeSessions.isEmpty()) {
            return null;
        }
        return activeSessions.values().iterator().next().getVoteManager();
    }
    
    /**
     * Gets all active session names.
     */
    public Set<String> getActiveSessionNames() {
        return new HashSet<>(activeSessions.keySet());
    }

    public MessageUtils getMessageUtils() {
        return messageUtils;
    }
    
    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }
}