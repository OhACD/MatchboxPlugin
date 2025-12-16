package com.ohacd.matchbox.game;

import com.ohacd.matchbox.Matchbox;
import com.ohacd.matchbox.game.ability.FallbackHunterVisionAdapter;
import com.ohacd.matchbox.game.ability.HunterVisionAdapter;
import com.ohacd.matchbox.game.ability.ProtocolLibHunterVisionAdapter;
import com.ohacd.matchbox.game.ability.MedicSecondaryAbility;
import com.ohacd.matchbox.game.ability.SparkSecondaryAbility;
import com.ohacd.matchbox.game.action.PlayerActionHandler;
import com.ohacd.matchbox.game.config.ConfigManager;
import com.ohacd.matchbox.game.cosmetic.SkinManager;
import com.ohacd.matchbox.game.hologram.HologramManager;
import com.ohacd.matchbox.game.lifecycle.GameLifecycleManager;
import com.ohacd.matchbox.game.phase.DiscussionPhaseHandler;
import com.ohacd.matchbox.game.phase.PhaseManager;
import com.ohacd.matchbox.game.phase.SwipePhaseHandler;
import com.ohacd.matchbox.game.phase.VotingPhaseHandler;
import com.ohacd.matchbox.game.session.GameSession;
import com.ohacd.matchbox.game.session.SessionManager;
import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.MessageUtils;
import com.ohacd.matchbox.game.utils.ParticleUtils;
import com.ohacd.matchbox.game.utils.PlayerBackup;
import com.ohacd.matchbox.game.utils.PlayerNameUtils;
import com.ohacd.matchbox.game.utils.Role;
import com.ohacd.matchbox.game.utils.Managers.InventoryManager;
import com.ohacd.matchbox.game.utils.Managers.NameTagManager;
import com.ohacd.matchbox.game.vote.DynamicVotingThreshold;
import com.ohacd.matchbox.game.vote.VoteManager;
import com.ohacd.matchbox.game.win.WinConditionChecker;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

import static org.bukkit.Bukkit.getPlayer;

/**
 * Main game manager that coordinates all game systems.
 */
public class GameManager {
    private final Plugin plugin;
    @SuppressWarnings("unused")
    private final HologramManager hologramManager;

    // Core systems (shared across all sessions)
    private final MessageUtils messageUtils;
    private final SwipePhaseHandler swipePhaseHandler;
    private final DiscussionPhaseHandler discussionPhaseHandler;
    private final VotingPhaseHandler votingPhaseHandler;
    private final InventoryManager inventoryManager;
    private final ConfigManager configManager;

    // Helper classes for code organization
    private final GameLifecycleManager lifecycleManager;
    private final PlayerActionHandler actionHandler;
    private final SkinManager skinManager;
    private final HunterVisionAdapter hunterVisionAdapter;

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
        this.configManager = new ConfigManager(plugin);
        this.messageUtils = new MessageUtils(plugin);
        this.swipePhaseHandler = new SwipePhaseHandler(plugin, messageUtils);
        this.discussionPhaseHandler = new DiscussionPhaseHandler(plugin, messageUtils, configManager);
        this.votingPhaseHandler = new VotingPhaseHandler(plugin, messageUtils);
        this.inventoryManager = new InventoryManager(plugin);
        this.skinManager = new SkinManager(plugin);
        this.hunterVisionAdapter = createHunterVisionAdapter(plugin);

        // Initialize helper classes
        this.lifecycleManager = new GameLifecycleManager(plugin, messageUtils, swipePhaseHandler, inventoryManager, playerBackups);
        this.actionHandler = new PlayerActionHandler(plugin);

        skinManager.preloadDefaultSkins();
    }

    private HunterVisionAdapter createHunterVisionAdapter(Plugin plugin) {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
                plugin.getLogger().info("ProtocolLib detected. Using packet-based Hunter Vision.");
                return new ProtocolLibHunterVisionAdapter(plugin);
            }
            plugin.getLogger().warning("ProtocolLib not detected. Hunter Vision will use particle fallback.");
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to initialize ProtocolLib Hunter Vision (" + e.getMessage() + "). Using fallback.");
        }
        return new FallbackHunterVisionAdapter(plugin);
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
            Matchbox matchboxPlugin = (Matchbox) plugin;
            SessionManager sessionManager = matchboxPlugin.getSessionManager();
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
                    plugin.getLogger().warning("Player " + playerId + " found in multiple sessions! Sessions: " +
                        found.getSessionName() + " and " + context.getSessionName());
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

        for (Player player : players) {
            if (player == null || !player.isOnline()) {
                plugin.getLogger().warning("Skipping null or offline player during backup");
                continue;
            }

            UUID playerId = player.getUniqueId();

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
        
        // Apply skins based on config settings
        if (configManager.isUseSteveSkins()) {
            // Use Steve skins for all players (overrides random skins setting)
            skinManager.applySteveSkins(players);
        } else if (configManager.isRandomSkinsEnabled()) {
            // Apply random skins if enabled
            skinManager.applyRandomSkins(players);
        }

        // Start first round (teleport players and begin swipe phase)
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
            Matchbox matchboxPlugin = (Matchbox) plugin;
            SessionManager sessionManager = matchboxPlugin.getSessionManager();
            if (sessionManager != null) {
                GameSession session = sessionManager.getSession(sessionName);
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

        // Reapply skins based on config settings to ensure consistency
        Collection<Player> alivePlayers = swipePhaseHandler.getAlivePlayerObjects(gameState.getAlivePlayerIds());
        if (alivePlayers != null && !alivePlayers.isEmpty()) {
            if (configManager.isUseSteveSkins()) {
                // Reapply Steve skins for all players to ensure consistency
                skinManager.applySteveSkins(alivePlayers);
            } else if (configManager.isRandomSkinsEnabled()) {
                // Restore assigned skins (which should be random skins from game start)
                skinManager.restoreAssignedSkinsAfterDiscussion(alivePlayers);
            }
        }

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
            SparkSecondaryAbility sparkAbility = selectSparkSecondaryAbility(gameState);
            MedicSecondaryAbility medicAbility = selectMedicSecondaryAbility(gameState);

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
            inventoryManager.setupInventories(alivePlayers, roleMap, sparkAbility, medicAbility);
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

        // Get swipe duration from config
        int swipeDuration = configManager.getSwipeDuration();
        swipePhaseHandler.startSwipePhase(
                sessionName,
                swipeDuration,
                gameState.getAlivePlayerIds(),
                () -> endSwipePhase(sessionName)
        );
    }

    /**
     * Start a swipe window for the player (silent). Duration in seconds.
     * Can be reactivated if window expires without successful use.
     */
    public Long startSwipeWindow(Player spark, int seconds) {
        if (spark == null) return null;
        UUID id = spark.getUniqueId();

        SessionGameContext context = getContextForPlayer(id);
        if (context == null) return null;

        GameState gameState = context.getGameState();
        PhaseManager phaseManager = context.getPhaseManager();
        Map<UUID, Long> activeSwipeWindow = context.getActiveSwipeWindow();

        // Only in SWIPE phase and only if spark role
        if (!phaseManager.isPhase(GamePhase.SWIPE)) return null;
        if (gameState.getRole(id) != Role.SPARK) return null;
        // Don't check hasSwipedThisRound here - allow reactivation if not successfully used

        long expire = System.currentTimeMillis() + (seconds * 1000L);
        activeSwipeWindow.put(id, expire);

        return expire;
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
     * Start a delusion window for the player (silent). Duration in seconds.
     * Can be reactivated if window expires without successful use.
     */
    public Long startDelusionWindow(Player spark, int seconds) {
        if (spark == null) return null;
        UUID id = spark.getUniqueId();

        SessionGameContext context = getContextForPlayer(id);
        if (context == null) return null;

        GameState gameState = context.getGameState();
        PhaseManager phaseManager = context.getPhaseManager();
        Map<UUID, Long> activeDelusionWindow = context.getActiveDelusionWindow();

        // Only in SWIPE phase and only if spark role
        if (!phaseManager.isPhase(GamePhase.SWIPE)) return null;
        if (gameState.getRole(id) != Role.SPARK) return null;
        // Check if delusion ability is active
        if (gameState.getSparkSecondaryAbility() != SparkSecondaryAbility.DELUSION) return null;
        // Don't check hasUsedDelusionThisRound here - allow reactivation if not successfully used

        long expire = System.currentTimeMillis() + (seconds * 1000L);
        activeDelusionWindow.put(id, expire);

        return expire;
    }

    /**
     * Ends delusion window immediately (manual cleanup).
     */
    public void endDelusionWindow(UUID playerId) {
        SessionGameContext context = getContextForPlayer(playerId);
        if (context != null) {
            context.getActiveDelusionWindow().remove(playerId);
        }
    }

    /**
     * Returns whether the given player has an active delusion window.
     */
    public boolean isDelusionWindowActive(UUID playerId) {
        SessionGameContext context = getContextForPlayer(playerId);
        if (context == null) return false;
        return actionHandler.isDelusionWindowActive(context, playerId);
    }

    /**
     * Handles a delusion action from a spark.
     */
    public void handleDelusion(Player spark, Player target) {
        if (spark == null || target == null) return;

        SessionGameContext context = getContextForPlayer(spark.getUniqueId());
        if (context == null) return;

        actionHandler.handleDelusion(context, spark, target);
    }

    public SkinManager getSkinManager() {
        return skinManager;
    }

    public HunterVisionAdapter getHunterVisionAdapter() {
        return hunterVisionAdapter;
    }

    /**
     * Start a cure window for the medic (silent). Duration in seconds.
     * Can be reactivated if window expires without successful use.
     */
    public Long startCureWindow(Player medic, int seconds) {
        if (medic == null) return null;
        UUID id = medic.getUniqueId();

        SessionGameContext context = getContextForPlayer(id);
        if (context == null) return null;

        GameState gameState = context.getGameState();
        PhaseManager phaseManager = context.getPhaseManager();
        Map<UUID, Long> activeCureWindow = context.getActiveCureWindow();

        // Only in SWIPE phase and only if medic role
        if (!phaseManager.isPhase(GamePhase.SWIPE)) return null;
        if (gameState.getRole(id) != Role.MEDIC) return null;
        // Don't check hasCuredThisRound here - allow reactivation if not successfully used

        long expire = System.currentTimeMillis() + (seconds * 1000L);
        activeCureWindow.put(id, expire);

        return expire;
    }

    /**
     * Restores the slot-27 ability paper for Sparks/Medics when a window expires unused.
     */
    public void restoreAbilityPaper(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        UUID id = player.getUniqueId();
        SessionGameContext context = getContextForPlayer(id);
        if (context == null) {
            return;
        }
        Role role = context.getGameState().getRole(id);
        if (role == Role.SPARK || role == Role.MEDIC) {
            inventoryManager.refreshAbilityPaper(player, role);
        }
    }

    /**
     * Restores the secondary ability paper (Slot 28) for Sparks/Medics when a window expires unused.
     * @param player
     */
    public void restoreSecondaryAbilityPaper(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        UUID id = player.getUniqueId();
        SessionGameContext context = getContextForPlayer(id);
        if (context == null) {
            return;
        }
        Role role = context.getGameState().getRole(id);
        if (role == Role.SPARK || role == Role.MEDIC) {
            inventoryManager.refreshSecondaryAbilityPaper(player, role, context);
        }
    }

    /**
     * Activates Healing Sight for the medic.
     * Shows highlight particles on all infected players for 15 seconds (only visible to medic).
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

        hunterVisionAdapter.startVision(spark, context);
    }

    /**
     * Shows highlight particles on all players with pending deaths, visible only to the medic.
     * Particles last for 15 seconds.
     */
    private void showInfectedPlayersToMedic(Player medic, SessionGameContext context) {
        if (medic == null || !medic.isOnline()) return;

        GameState gameState = context.getGameState();

        // Get all alive players with pending deaths OR delusion infections
        List<Player> infectedPlayers = new ArrayList<>();
        Set<UUID> alivePlayerIds = gameState.getAlivePlayerIds();
        if (alivePlayerIds != null) {
            for (UUID playerId : alivePlayerIds) {
                if (playerId == null) continue;
                // Show both real infections and delusion infections (medic can't tell the difference)
                if (gameState.hasPendingDeath(playerId) || gameState.isDelusionInfected(playerId)) {
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

        // Show highlight particles on all infected players for 15 seconds
        // Only the medic can see these particles (recording-safe)
        ParticleUtils.showMarkerParticlesOnPlayers(medic, infectedPlayers, 15, plugin);

        plugin.getLogger().info("Showing " + infectedPlayers.size() + " infected player(s) to medic " + medic.getName());
    }

    /**
     * Shows particles on all alive players, visible only to the spark.
     * Particles last for 15 seconds.
     * Note: Spark cannot see nametags - only particles are shown.
     */
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
        List<Player> playersCuredThisRound = new ArrayList<>();
        if (alivePlayerIds != null) {
            for (UUID playerId : alivePlayerIds) {
                if (playerId != null && gameState.hasPendingDeath(playerId)) {
                    allPendingDeaths.add(playerId);
                }
                if (playerId != null && gameState.hasBeenCuredThisRound(playerId)) {
                    Player curedPlayer = getPlayer(playerId);
                    if (curedPlayer != null) {
                        playersCuredThisRound.add(curedPlayer);
                    }
                    gameState.removeBeenCuredThisRound(playerId);
                }
            }
        }

        if (!allPendingDeaths.isEmpty()) {
            plugin.getLogger().info("Applying pending deaths for " + allPendingDeaths.size() + " players at discussion start in session: " + sessionName);
        }

        if (!playersCuredThisRound.isEmpty()) {
            for (Player curedPlayer : playersCuredThisRound) {
                if (curedPlayer == null || !curedPlayer.isOnline()) {
                    continue;
                }
                messageUtils.sendPlayerMessage(curedPlayer, "§9You feel the Medic's cure take hold. You're safe this round!");
                plugin.getLogger().info("Player " + curedPlayer.getName() + " has been cured this round and will not be eliminated.");
            }
        }
        List<Player> eliminatedPlayers = new ArrayList<>();
        for (UUID victimId : allPendingDeaths) {
            if (victimId == null) continue;

            if (!gameState.isAlive(victimId)) {
                gameState.removePendingDeath(victimId);
                continue;
            }
            Player victim = getPlayer(victimId);
            if (victim != null && victim.isOnline()) {
                try {
                    eliminatedPlayers.add(victim);
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

        // Collect alive players for discussion (no nametag/skin changes here)
        Collection<Player> alivePlayersForDiscussion = new ArrayList<>();
        if (alivePlayerIds != null) {
            for (UUID playerId : alivePlayerIds) {
                if (playerId == null) continue;
                Player player = getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    alivePlayersForDiscussion.add(player);
                }
            }
        }

        // Notify players about eliminated participants and hold them in place before teleport
        if (!alivePlayersForDiscussion.isEmpty()) {
            notifyEliminationsBeforeDiscussion(alivePlayersForDiscussion, eliminatedPlayers, 10);
        }

        // Clear infected flags for the round
        gameState.clearInfectedThisRound();

        // Clear ALL inventory for all alive players (they should have nothing during discussion)
        alivePlayerIds = gameState.getAlivePlayerIds();
        if (alivePlayerIds != null) {
            for (UUID playerId : alivePlayerIds) {
                if (playerId == null) continue;
                Player player = getPlayer(playerId);
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

        // Get seat locations from session if available
        final Map<Integer, Location> seatLocations = fetchSeatLocations(sessionName);
        final Location discussionLocation = context.getCurrentDiscussionLocation();
        final int discussionDuration = configManager.getDiscussionDuration();

        // Delay teleportation and discussion start to give players time to read the elimination title
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            clearPreDiscussionEffects(alivePlayersForDiscussion);
            teleportPlayersToDiscussion(alivePlayersForDiscussion, seatLocations, discussionLocation, sessionName);
            // Start the discussion timer and supply callback to endDiscussionPhase
            discussionPhaseHandler.startDiscussionPhase(sessionName, discussionDuration, gameState.getAlivePlayerIds(), () -> endDiscussionPhase(sessionName), seatLocations);
        }, 20L * 10); // 10-second delay before teleporting to discussion
    }

    private Map<Integer, Location> fetchSeatLocations(String sessionName) {
        try {
            Matchbox matchboxPlugin = (Matchbox) plugin;
            SessionManager sessionManager = matchboxPlugin.getSessionManager();
            if (sessionManager != null) {
                GameSession session = sessionManager.getSession(sessionName);
                if (session != null && session.getSeatLocations() != null) {
                    return session.getSeatLocations();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get seat locations: " + e.getMessage());
        }
        return Collections.emptyMap();
    }

    /**
     * Sends a title about eliminated players, applies blindness/slowness, and holds players for the given delay.
     */
    private void notifyEliminationsBeforeDiscussion(Collection<Player> alivePlayers, List<Player> eliminatedPlayers, int delaySeconds) {
        if (alivePlayers == null || alivePlayers.isEmpty() || delaySeconds <= 0) {
            return;
        }

        applyPreDiscussionEffects(alivePlayers, delaySeconds);

        String subtitle = "Teleporting in " + delaySeconds + "s";
        if (eliminatedPlayers == null || eliminatedPlayers.isEmpty()) {
            messageUtils.sendTitle(alivePlayers, "§6Discussion Incoming", "§7" + subtitle, 10, delaySeconds * 20, 10);
            return;
        }

        String eliminatedNames = eliminatedPlayers.stream()
            .filter(Objects::nonNull)
            .map(PlayerNameUtils::displayName)
            .collect(Collectors.joining(", "));

        if (eliminatedNames.isEmpty()) {
            messageUtils.sendTitle(alivePlayers, "§6Discussion Incoming", "§7" + subtitle, 10, delaySeconds * 20, 10);
            return;
        }

        String eliminationSubtitle = eliminatedPlayers.size() == 1 ? "has been eliminated" : "have been eliminated";
        messageUtils.sendTitle(
            alivePlayers,
            eliminatedNames,
            "§c" + eliminationSubtitle + " §8| §7" + subtitle,
            10,
            delaySeconds * 20,
            10
        );
    }

    /**
     * Applies blindness and heavy slowness so players cannot move/see during the pre-discussion delay.
     */
    private void applyPreDiscussionEffects(Collection<Player> players, int durationSeconds) {
        if (players == null || players.isEmpty()) {
            return;
        }

        int durationTicks = Math.max(1, durationSeconds * 20 + 20); // small buffer to cover teleport tick
        PotionEffect blindness = new PotionEffect(PotionEffectType.BLINDNESS, durationTicks, 1, false, false, false);
        PotionEffect slowness = new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 6, false, false, false);

        for (Player player : players) {
            if (player != null && player.isOnline()) {
                try {
                    player.addPotionEffect(blindness);
                    player.addPotionEffect(slowness);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to apply pre-discussion effects to " + player.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Clears the pre-discussion potion effects once players are teleported.
     */
    private void clearPreDiscussionEffects(Collection<Player> players) {
        if (players == null || players.isEmpty()) {
            return;
        }

        for (Player player : players) {
            if (player != null && player.isOnline()) {
                try {
                    player.removePotionEffect(PotionEffectType.BLINDNESS);
                    player.removePotionEffect(PotionEffectType.SLOWNESS);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to clear pre-discussion effects for " + player.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Teleports players to the discussion area when seat locations are unavailable.
     * If seat locations exist, DiscussionPhaseHandler will handle teleportation.
     */
    private void teleportPlayersToDiscussion(Collection<Player> players, Map<Integer, Location> seatLocations, Location discussionLocation, String sessionName) {
        if (players == null || players.isEmpty()) {
            return;
        }

        // Seat locations exist; DiscussionPhaseHandler will teleport them when the phase starts
        if (seatLocations != null && !seatLocations.isEmpty()) {
            return;
        }

        if (discussionLocation == null || discussionLocation.getWorld() == null) {
            plugin.getLogger().warning("Cannot teleport to discussion: no seat locations or discussion location set for session: " + sessionName);
            return;
        }

        for (Player player : players) {
            if (player != null && player.isOnline()) {
                try {
                    player.teleport(discussionLocation);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to teleport player " + player.getName() + " to discussion: " + e.getMessage());
                }
            }
        }
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

        // Get voting duration from config
        int votingDuration = configManager.getVotingDuration();
        
        // Calculate threshold information for display
        int alivePlayerCount = gameState.getAlivePlayerCount();
        int consecutiveNoEliminationPhases = context.getConsecutiveNoEliminationPhases();
        DynamicVotingThreshold thresholdCalculator = new DynamicVotingThreshold(configManager);
        int requiredVotes = thresholdCalculator.getRequiredVoteCount(alivePlayerCount, consecutiveNoEliminationPhases);
        
        // Start the voting timer and supply callback to endVotingPhase
        votingPhaseHandler.startVotingPhase(sessionName, votingDuration, gameState.getAlivePlayerIds(), () -> endVotingPhase(sessionName), requiredVotes, alivePlayerCount);
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
     * Uses dynamic thresholds based on alive player count and penalty system.
     * Handles ties by checking if tie vote count meets threshold.
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

        int alivePlayerCount = gameState.getAlivePlayerCount();
        int consecutiveNoEliminationPhases = context.getConsecutiveNoEliminationPhases();
        
        // Initialize dynamic voting threshold calculator
        DynamicVotingThreshold thresholdCalculator = new DynamicVotingThreshold(configManager);
        
        UUID mostVoted = voteManager.getMostVotedPlayer();
        List<UUID> tied = voteManager.getTiedPlayers();
        int maxVotes = voteManager.getMaxVoteCount();

        if (tied == null) {
            tied = Collections.emptyList();
        }

        Collection<Player> sessionPlayers = swipePhaseHandler.getAlivePlayerObjects(gameState.getAlivePlayerIds());

        if (mostVoted == null && tied.isEmpty()) {
            // No votes cast - skip elimination and increment penalty
            context.incrementNoEliminationPhases();
            if (sessionPlayers != null) {
                for (Player p : sessionPlayers) {
                    if (p != null && p.isOnline()) {
                        p.sendMessage("§eNo votes were cast. No one is eliminated.");
                    }
                }
            }
            plugin.getLogger().info("No votes cast this round for session " + sessionName + ". Total voters: " + voteManager.getVoters().size() +
                ", Alive players: " + alivePlayerCount + ", Consecutive no-elimination phases: " + consecutiveNoEliminationPhases);
            return;
        }

        UUID toEliminate = null;
        String resultMessage;
        boolean eliminationOccurred = false;

        // Check if we have a clear winner (no tie)
        if (mostVoted != null) {
            // Check if the vote count meets the threshold
            if (thresholdCalculator.meetsThreshold(maxVotes, alivePlayerCount, consecutiveNoEliminationPhases)) {
                toEliminate = mostVoted;
                eliminationOccurred = true;
                Player eliminated = getPlayer(toEliminate);
                if (eliminated != null) {
                    resultMessage = "§c" + PlayerNameUtils.displayName(eliminated) + " was eliminated with " + maxVotes + " vote(s)!";
                } else {
                    resultMessage = "§cA player was eliminated with " + maxVotes + " vote(s)!";
                }
            } else {
                // Vote count doesn't meet threshold - no elimination
                int requiredVotes = thresholdCalculator.getRequiredVoteCount(alivePlayerCount, consecutiveNoEliminationPhases);
                context.incrementNoEliminationPhases();
                if (sessionPlayers != null) {
                    for (Player p : sessionPlayers) {
                        if (p != null && p.isOnline()) {
                            p.sendMessage("§eNot enough votes to eliminate. Required: " + requiredVotes + ", Got: " + maxVotes);
                        }
                    }
                }
                plugin.getLogger().info("Vote threshold not met for session " + sessionName + ". Required: " + requiredVotes + 
                    ", Got: " + maxVotes + ", Alive players: " + alivePlayerCount + 
                    ", Consecutive no-elimination phases: " + (consecutiveNoEliminationPhases + 1));
                return;
            }
        } else if (!tied.isEmpty()) {
            // Handle tie - check if tie vote count meets threshold
            if (thresholdCalculator.meetsThreshold(maxVotes, alivePlayerCount, consecutiveNoEliminationPhases)) {
                // Tie meets threshold - randomly eliminate one of the tied players
                Collections.shuffle(tied);
                toEliminate = tied.get(0);
                eliminationOccurred = true;
                Player eliminated = getPlayer(toEliminate);
                if (eliminated != null) {
                    resultMessage = "§cTie! " + PlayerNameUtils.displayName(eliminated) + " was randomly eliminated with " + maxVotes + " vote(s)!";
                } else {
                    resultMessage = "§cTie! A player was randomly eliminated with " + maxVotes + " vote(s)!";
                }
            } else {
                // Tie doesn't meet threshold - no elimination
                int requiredVotes = thresholdCalculator.getRequiredVoteCount(alivePlayerCount, consecutiveNoEliminationPhases);
                context.incrementNoEliminationPhases();
                if (sessionPlayers != null) {
                    for (Player p : sessionPlayers) {
                        if (p != null && p.isOnline()) {
                            p.sendMessage("§eTie occurred but not enough votes to eliminate. Required: " + requiredVotes + ", Got: " + maxVotes);
                        }
                    }
                }
                plugin.getLogger().info("Tie vote threshold not met for session " + sessionName + ". Required: " + requiredVotes + 
                    ", Got: " + maxVotes + ", Alive players: " + alivePlayerCount + 
                    ", Consecutive no-elimination phases: " + (consecutiveNoEliminationPhases + 1));
                return;
            }
        } else {
            // Should not happen, but handle gracefully
            context.incrementNoEliminationPhases();
            if (sessionPlayers != null) {
                for (Player p : sessionPlayers) {
                    if (p != null && p.isOnline()) {
                        p.sendMessage("§eVoting completed but no elimination occurred.");
                    }
                }
            }
            return;
        }

        // Eliminate the player if we have one
        if (toEliminate != null && eliminationOccurred) {
            // Reset penalty counter since elimination occurred
            context.resetNoEliminationPhases();
            
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

        // Restore nametag and cosmetic overrides
        try {
            NameTagManager.showNameTag(player);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to restore nametag for " + player.getName() + ": " + e.getMessage());
        }
        skinManager.restoreOriginalSkin(player);
        hunterVisionAdapter.stopVision(playerId);

        // Remove pending death if they had one
        if (gameState.hasPendingDeath(playerId)) {
            gameState.removePendingDeath(playerId);
        }

        // Remove from alive players
        gameState.removeAlivePlayer(playerId);

        // Also remove from session if they're in one
        try {
            Matchbox matchboxPlugin = (Matchbox) plugin;
            SessionManager sessionManager = matchboxPlugin.getSessionManager();
            if (sessionManager != null) {
                GameSession session = sessionManager.getSession(sessionName);
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
                Matchbox matchboxPlugin = (Matchbox) plugin;
                SessionManager sessionManager = matchboxPlugin.getSessionManager();
                if (sessionManager != null) {
                    GameSession session = sessionManager.getSession(sessionName);
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
        hunterVisionAdapter.stopVisionForPlayers(allParticipatingIds);
        if (allParticipatingIds != null) {
            for (UUID playerId : allParticipatingIds) {
                if (playerId == null) continue;
                Player player = getPlayer(playerId);
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

        skinManager.restoreOriginalSkins(allParticipatingIds);

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

        // Fully terminate session - remove it from SessionManager when game ends
        try {
            // Access SessionManager via plugin instance
            Matchbox matchboxPlugin = (Matchbox) plugin;
            SessionManager sessionManager = matchboxPlugin.getSessionManager();
            if (sessionManager != null) {
                GameSession session = sessionManager.getSession(sessionName);
                if (session != null) {
                    // Mark as inactive first
                    session.setActive(false);
                    // Fully remove the session from SessionManager for complete termination
                    sessionManager.removeSession(sessionName);
                    plugin.getLogger().info("Fully terminated and removed session '" + sessionName + "' after game end");
                } else {
                    plugin.getLogger().warning("Session '" + sessionName + "' not found when trying to remove after game end");
                }
            } else {
                plugin.getLogger().warning("SessionManager is null when trying to remove session after game end");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to remove session after game end: " + e.getMessage());
            e.printStackTrace();
        }

        plugin.getLogger().info("Game ended successfully for session: " + sessionName);
    }

    /**
     * Gets the game state for backward compatibility.
     * @deprecated Use getContextForPlayer() or getContext() for parallel session support
     * @return First active session's game state, or null if no active sessions
     */
    @Deprecated
    public GameState getGameState() {
        if (activeSessions.isEmpty()) {
            return null;
        }
        return activeSessions.values().iterator().next().getGameState();
    }

    /**
     * Gets the phase manager for backward compatibility.
     * @deprecated Use getContextForPlayer() or getContext() for parallel session support
     * @return First active session's phase manager, or null if no active sessions
     */
    @Deprecated
    public PhaseManager getPhaseManager() {
        if (activeSessions.isEmpty()) {
            return null;
        }
        return activeSessions.values().iterator().next().getPhaseManager();
    }

    /**
     * Gets the vote manager for backward compatibility.
     * @deprecated Use getContextForPlayer() or getContext() for parallel session support
     * @return First active session's vote manager, or null if no active sessions
     */
    @Deprecated
    public VoteManager getVoteManager() {
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

    public ConfigManager getConfigManager() {
        return configManager;
    }

    private SparkSecondaryAbility selectSparkSecondaryAbility(GameState gameState) {
        if (gameState == null) {
            return SparkSecondaryAbility.HUNTER_VISION;
        }
        UUID sparkId = gameState.getSparkUUID();
        if (sparkId == null) {
            gameState.setSparkSecondaryAbility(SparkSecondaryAbility.HUNTER_VISION);
            return SparkSecondaryAbility.HUNTER_VISION;
        }
        
        // Check config for ability selection
        String configAbility = configManager.getSparkSecondaryAbility();
        SparkSecondaryAbility choice;
        
        if (configAbility.equals("random")) {
            // Random selection (default behavior) - choose from all three abilities
            int random = ThreadLocalRandom.current().nextInt(3);
            if (random == 0) {
                choice = SparkSecondaryAbility.HUNTER_VISION;
            } else if (random == 1) {
                choice = SparkSecondaryAbility.SPARK_SWAP;
            } else {
                choice = SparkSecondaryAbility.DELUSION;
            }
        } else if (configAbility.equals("hunter_vision")) {
            choice = SparkSecondaryAbility.HUNTER_VISION;
        } else if (configAbility.equals("spark_swap")) {
            choice = SparkSecondaryAbility.SPARK_SWAP;
        } else if (configAbility.equals("delusion")) {
            choice = SparkSecondaryAbility.DELUSION;
        } else {
            // Fallback to random if invalid config value
            plugin.getLogger().warning("Invalid Spark ability config value, using random selection");
            int random = ThreadLocalRandom.current().nextInt(3);
            if (random == 0) {
                choice = SparkSecondaryAbility.HUNTER_VISION;
            } else if (random == 1) {
                choice = SparkSecondaryAbility.SPARK_SWAP;
            } else {
                choice = SparkSecondaryAbility.DELUSION;
            }
        }
        
        gameState.setSparkSecondaryAbility(choice);
        return choice;
    }

    private MedicSecondaryAbility selectMedicSecondaryAbility(GameState gameState) {
        if (gameState == null) {
            return MedicSecondaryAbility.HEALING_SIGHT;
        }
        // Check if medic exists in game
        boolean hasMedic = false;
        for (UUID playerId : gameState.getAlivePlayerIds()) {
            if (gameState.getRole(playerId) == Role.MEDIC) {
                hasMedic = true;
                break;
            }
        }
        if (!hasMedic) {
            gameState.setMedicSecondaryAbility(MedicSecondaryAbility.HEALING_SIGHT);
            return MedicSecondaryAbility.HEALING_SIGHT;
        }
        
        // Check config for ability selection
        String configAbility = configManager.getMedicSecondaryAbility();
        MedicSecondaryAbility choice;
        
        if (configAbility.equals("random")) {
            // Random selection (default behavior) - currently only HEALING_SIGHT available
            choice = MedicSecondaryAbility.HEALING_SIGHT;
        } else if (configAbility.equals("healing_sight")) {
            choice = MedicSecondaryAbility.HEALING_SIGHT;
        } else {
            // Fallback to healing_sight if invalid config value
            plugin.getLogger().warning("Invalid Medic ability config value, using healing_sight");
            choice = MedicSecondaryAbility.HEALING_SIGHT;
        }
        
        gameState.setMedicSecondaryAbility(choice);
        return choice;
    }
}
