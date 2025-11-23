package com.ohacd.matchbox.game;

import com.ohacd.matchbox.game.hologram.HologramManager;
import com.ohacd.matchbox.game.phase.DiscussionPhaseHandler;
import com.ohacd.matchbox.game.phase.PhaseManager;
import com.ohacd.matchbox.game.phase.SwipePhaseHandler;
import com.ohacd.matchbox.game.phase.VotingPhaseHandler;
import com.ohacd.matchbox.game.role.RoleAssigner;
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

    // Core systems
    private final GameState gameState;
    public final PhaseManager phaseManager;
    private final MessageUtils messageUtils;
    private final RoleAssigner roleAssigner;
    private final WinConditionChecker winConditionChecker;
    private final SwipePhaseHandler swipePhaseHandler;
    private final DiscussionPhaseHandler discussionPhaseHandler;
    private final VotingPhaseHandler votingPhaseHandler;
    private final VoteManager voteManager;
    private final InventoryManager inventoryManager;
    private final Map<UUID, Long> activeSwipeWindow = new ConcurrentHashMap<>();
    private final Map<UUID, Long> activeCureWindow = new ConcurrentHashMap<>();

    // Player backups for restoration
    private final Map<UUID, PlayerBackup> playerBackups = new ConcurrentHashMap<>();

    // Current round data
    private Location currentDiscussionLocation;
    private List<Location> currentSpawnLocations;

    public GameManager(Plugin plugin, HologramManager hologramManager) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (hologramManager == null) {
            throw new IllegalArgumentException("HologramManager cannot be null");
        }
        
        this.plugin = plugin;
        this.hologramManager = hologramManager;

        // Initialize systems
        this.gameState = new GameState();
        this.phaseManager = new PhaseManager(plugin);
        this.messageUtils = new MessageUtils(plugin);
        this.roleAssigner = new RoleAssigner(gameState);
        this.winConditionChecker = new WinConditionChecker(gameState);
        this.swipePhaseHandler = new SwipePhaseHandler(plugin, messageUtils);
        this.discussionPhaseHandler = new DiscussionPhaseHandler(plugin, messageUtils);
        this.votingPhaseHandler = new VotingPhaseHandler(plugin, messageUtils);
        this.voteManager = new VoteManager(gameState);
        this.inventoryManager = new InventoryManager(plugin);
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

        this.currentDiscussionLocation = discussionLocation;
        this.currentSpawnLocations = new ArrayList<>(spawnLocations); // Defensive copy

        // Clear ALL game state (this is the first round)
        gameState.clearGameState();
        phaseManager.reset();

        // Store session name
        gameState.setActiveSessionName(sessionName);

        // Backup player states before game starts
        for (Player player : players) {
            if (player == null || !player.isOnline()) {
                plugin.getLogger().warning("Skipping null or offline player during backup");
                continue;
            }
            try {
                playerBackups.put(player.getUniqueId(), new PlayerBackup(player));
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to backup player " + (player != null ? player.getName() : "null") + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Add players to alive set
        gameState.addAlivePlayers(players);

        // Assign roles (only once per game, not per round)
        List<Player> playerList = new ArrayList<>(players);
        roleAssigner.assignRoles(playerList);

        // Validate state after initialization
        if (!gameState.validateState()) {
            plugin.getLogger().severe("Game state invalid after initialization: " + gameState.getDebugInfo());
            plugin.getLogger().severe("Players: " + players.size() + ", Roles assigned: " + gameState.getAllParticipatingPlayerIds().size());
            // Attempt cleanup
            gameState.clearGameState();
            phaseManager.reset();
            return;
        }

        plugin.getLogger().info("Game started successfully with " + players.size() + " players. Roles: " + 
            gameState.getAllParticipatingPlayerIds().stream()
                .map(id -> {
                    Role role = gameState.getRole(id);
                    Player p = getPlayer(id);
                    return (p != null ? p.getName() : id.toString()) + "=" + (role != null ? role : "null");
                })
                .collect(java.util.stream.Collectors.joining(", ")));

        // Setup inventories for all players with their roles
        Map<UUID, Role> roleMap = new HashMap<>();
        for (UUID playerId : gameState.getAllParticipatingPlayerIds()) {
            Role role = gameState.getRole(playerId);
            if (role != null) {
                roleMap.put(playerId, role);
            }
        }
        inventoryManager.setupInventories(players, roleMap);

        // Start first round
        startNewRound();
    }

    /**
     * Starts a new round (after discussion phase ends).
     * This does NOT reassign roles or reset player list.
     */
    private void startNewRound() {
        // Check if game is still active (not ended)
        if (!gameState.isGameActive()) {
            plugin.getLogger().info("Cannot start new round - game is not active");
            return;
        }
        
        // Check if session is still active
        String activeSessionName = gameState.getActiveSessionName();
        if (activeSessionName != null) {
            try {
                com.ohacd.matchbox.Matchbox matchboxPlugin = (com.ohacd.matchbox.Matchbox) plugin;
                com.ohacd.matchbox.game.session.SessionManager sessionManager = matchboxPlugin.getSessionManager();
                if (sessionManager != null) {
                    com.ohacd.matchbox.game.session.GameSession session = sessionManager.getSession(activeSessionName);
                    if (session != null && !session.isActive()) {
                        plugin.getLogger().info("Cannot start new round - session is not active");
                        return;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check session status: " + e.getMessage());
            }
        }
        
        // Validate state before starting new round
        if (!gameState.validateState()) {
            messageUtils.broadcast("§c§lERROR: Game state is corrupted! Ending game.");
            plugin.getLogger().severe("Invalid game state detected: " + gameState.getDebugInfo());
            endGame();
            return;
        }

        // Increment round counter
        gameState.incrementRound();
        int roundNumber = gameState.getCurrentRound();

        messageUtils.broadcast("§6§l=== ROUND " + roundNumber + " ===");

        // Show title to all alive players
        Collection<Player> alivePlayers = swipePhaseHandler.getAlivePlayerObjects(gameState.getAlivePlayerIds());
        if (alivePlayers != null && !alivePlayers.isEmpty()) {
            messageUtils.sendTitle(
                    alivePlayers,
                    "§6§lROUND " + roundNumber,
                    "§eGet ready to swipe!",
                    10, // fadeIn (0.5s)
                    40, // stay (2s)
                    10  // fadeOut (0.5s)
            );
        }

        // Clear per-round state (swipes, cures, votes) but keep roles and players
        gameState.clearRoundState();
        voteManager.clearVotes();
        inventoryManager.resetArrowUsage();
        
        // Give arrows to all alive players for the new round (reuse alivePlayers from above)
        if (alivePlayers != null) {
            for (Player player : alivePlayers) {
                if (player != null && player.isOnline()) {
                    inventoryManager.giveArrowIfNeeded(player);
                }
            }
        }

        // Teleport alive players to spawn locations with better distribution
        teleportPlayersToSpawns();

        // Start swipe phase
        startSwipePhase();
    }

    /**
     * Teleports alive players to spawn locations.
     * Uses a better algorithm to distribute players evenly across spawns.
     */
    private void teleportPlayersToSpawns() {
        if (currentSpawnLocations == null || currentSpawnLocations.isEmpty()) {
            plugin.getLogger().warning("No spawn locations set for round " + gameState.getCurrentRound());
            return;
        }

        // Get all alive players
        List<Player> alivePlayers = new ArrayList<>();
        for (UUID playerId : gameState.getAlivePlayerIds()) {
            Player player = getPlayer(playerId);
            if (player != null && player.isOnline()) {
                alivePlayers.add(player);
            }
        }

        if (alivePlayers.isEmpty()) {
            return;
        }

        // Shuffle spawn locations for randomness
        List<Location> shuffledSpawns = new ArrayList<>(currentSpawnLocations);
        java.util.Collections.shuffle(shuffledSpawns);

        // Shuffle players too for extra randomness
        java.util.Collections.shuffle(alivePlayers);

        // Distribute players across spawns
        int spawnCount = shuffledSpawns.size();
        for (int i = 0; i < alivePlayers.size(); i++) {
            Player player = alivePlayers.get(i);
            Location spawnLoc = shuffledSpawns.get(i % spawnCount);

            // Clone location to avoid reference issues
            Location teleportLoc = spawnLoc.clone();
            if (teleportLoc == null || teleportLoc.getWorld() == null) {
                plugin.getLogger().warning("Invalid spawn location for player " + player.getName() + ", skipping teleport");
                continue;
            }

            // Add small random offset if multiple players at same spawn
            if (alivePlayers.size() > spawnCount) {
                int playersAtThisSpawn = (i / spawnCount) + 1;
                if (playersAtThisSpawn > 1) {
                    // Spread players in a circle around the spawn point
                    double angle = (2 * Math.PI * i) / alivePlayers.size();
                    double radius = 2.0; // 2 blocks radius
                    teleportLoc.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                }
            }

            try {
                player.teleport(teleportLoc);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to teleport player " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Starts the swipe phase.
     */
    public void startSwipePhase() {
        phaseManager.setPhase(GamePhase.SWIPE);
        plugin.getLogger().info("Starting swipe phase - Round " + gameState.getCurrentRound());

        // Announce phase start
        messageUtils.broadcast("§6§l>> SWIPE PHASE STARTED <<");

        // Hide the name tag for all alive players on phase start
        String sessionName = gameState.getActiveSessionName();
        Collection<Player> alivePlayers = swipePhaseHandler.getAlivePlayerObjects(gameState.getAlivePlayerIds());
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
                gameState.getAlivePlayerIds(),
                this::endSwipePhase
        );
    }

    /**
     * Start a swipe window for the player (silent). Duration in seconds.
     * Can be reactivated if window expires without successful use.
     */
    public void startSwipeWindow(Player spark, int seconds) {
        if (spark == null) return;
        UUID id = spark.getUniqueId();

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
        activeSwipeWindow.remove(playerId);
    }

    /**
     * Returns whether the given player has an active swipe window.
     */
    public boolean isSwipeWindowActive(UUID playerId) {
        Long expiry = activeSwipeWindow.get(playerId);
        if (expiry == null) return false;
        if (expiry <= System.currentTimeMillis()) {
            activeSwipeWindow.remove(playerId);
            return false;
        }
        return true;
    }

    /**
     * Start a cure window for the medic (silent). Duration in seconds.
     * Can be reactivated if window expires without successful use.
     */
    public void startCureWindow(Player medic, int seconds) {
        if (medic == null) return;
        UUID id = medic.getUniqueId();

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
        showInfectedPlayersToMedic(medic);
    }

    /**
     * Activates Hunter Vision for the spark.
     * Shows particles on all alive players for 15 seconds (only visible to spark).
     * Spark cannot see nametags - only particles are shown.
     * Can only be used once per round.
     */
    public void activateHunterVision(Player spark) {
        if (spark == null || !spark.isOnline()) return;

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
        showGlowOnPlayers(spark);
    }

    /**
     * Shows red particles on all players with pending deaths, visible only to the medic.
     * Particles last for 15 seconds.
     */
    private void showInfectedPlayersToMedic(Player medic) {
        if (medic == null || !medic.isOnline()) return;

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
    private void showGlowOnPlayers(Player spark) {
        if (spark == null || !spark.isOnline()) return;

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
        activeCureWindow.remove(playerId);
    }

    /**
     * Returns whether the given player has an active cure window.
     */
    public boolean isCureWindowActive(UUID playerId) {
        Long expiry = activeCureWindow.get(playerId);
        if (expiry == null) return false;
        if (expiry <= System.currentTimeMillis()) {
            activeCureWindow.remove(playerId);
            return false;
        }
        return true;
    }

    /**
     * Called to end the swipe phase. Does NOT apply pending deaths here.
     * Discussion phase start will apply pending deaths so infected players are removed
     * before discussion begins.
     */
    public void endSwipePhase() {
        plugin.getLogger().info("Ending swipe phase - Round " + gameState.getCurrentRound());

        // Transition to discussion but do not apply pending deaths here.
        phaseManager.setPhase(GamePhase.DISCUSSION);
        messageUtils.broadcast("§e§l>> SWIPE PHASE ENDED <<");

        // Clear actionbars, stop timers, etc. (assume swipePhaseHandler cleared by caller)
        // Start the discussion phase which will apply pending deaths at its start
        startDiscussionPhase();
    }

    /**
     * Starts the discussion phase and applies pending deaths immediately before players join discussion.
     */
    private void startDiscussionPhase() {
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
            plugin.getLogger().info("Applying pending deaths for " + allPendingDeaths.size() + " players at discussion start.");
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
                    eliminatePlayer(victim); // ensure this removes them from alive and cleans state
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

        // Teleport alive players to discussion location
        if (currentDiscussionLocation != null && currentDiscussionLocation.getWorld() != null) {
            alivePlayerIds = gameState.getAlivePlayerIds();
            if (alivePlayerIds != null) {
                for (UUID playerId : alivePlayerIds) {
                    if (playerId == null) continue;
                    Player player = org.bukkit.Bukkit.getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        try {
                            player.teleport(currentDiscussionLocation);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to teleport player " + player.getName() + " to discussion: " + e.getMessage());
                        }
                    }
                }
            }
        } else {
            plugin.getLogger().warning("Cannot teleport to discussion: location is null or world is null");
        }

        // Start the discussion timer and supply callback to endDiscussionPhase
        discussionPhaseHandler.startDiscussionPhase(gameState.getAlivePlayerIds(), this::endDiscussionPhase);
    }

    /**
     * Ends the discussion phase and starts voting phase.
     */
    private void endDiscussionPhase() {
        plugin.getLogger().info("Ending discussion phase - Round " + gameState.getCurrentRound());
        messageUtils.broadcast("§e§l>> DISCUSSION PHASE ENDED <<");

        // Clear any previous votes
        voteManager.clearVotes();

        // Start voting phase
        startVotingPhase();
    }

    /**
     * Starts the voting phase.
     */
    private void startVotingPhase() {
        phaseManager.setPhase(GamePhase.VOTING);
        plugin.getLogger().info("Starting voting phase - Round " + gameState.getCurrentRound());

        // Give voting papers to all alive players
        Collection<Player> alivePlayers = swipePhaseHandler.getAlivePlayerObjects(gameState.getAlivePlayerIds());
        if (alivePlayers != null && !alivePlayers.isEmpty()) {
            for (Player player : alivePlayers) {
                if (player != null && player.isOnline()) {
                    inventoryManager.giveVotingPapers(player, alivePlayers);
                }
            }
        }

        // Start the voting timer and supply callback to endVotingPhase
        votingPhaseHandler.startVotingPhase(gameState.getAlivePlayerIds(), this::endVotingPhase);
    }

    /**
     * Ends the voting phase, resolves votes, and eliminates the most-voted player.
     */
    private void endVotingPhase() {
        plugin.getLogger().info("Ending voting phase - Round " + gameState.getCurrentRound());
        messageUtils.broadcast("§c§l>> VOTING PHASE ENDED <<");

        // Clear voting papers from all players
        Collection<Player> alivePlayers = swipePhaseHandler.getAlivePlayerObjects(gameState.getAlivePlayerIds());
        if (alivePlayers != null) {
            inventoryManager.clearAllVotingPapers(alivePlayers);
        }

        // Resolve votes and eliminate
        try {
            resolveVotes();
        } catch (Exception e) {
            plugin.getLogger().severe("Error resolving votes: " + e.getMessage());
            e.printStackTrace();
        }

        // Check for win condition after voting
        try {
            if (checkForWin()) {
                plugin.getLogger().info("Win condition met - game ended, not starting new round");
                return; // Game ended
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error checking win conditions: " + e.getMessage());
            e.printStackTrace();
            // Don't continue to next round if check fails - end game instead
            endGame();
            return;
        }

        // Double-check game is still active before starting new round
        if (!gameState.isGameActive()) {
            plugin.getLogger().info("Game is not active after voting - not starting new round");
            return;
        }

        // Start next round
        try {
            startNewRound();
        } catch (Exception e) {
            plugin.getLogger().severe("Error starting new round: " + e.getMessage());
            e.printStackTrace();
            // Attempt to end game gracefully
            endGame();
        }
    }

    /**
     * Resolves votes and eliminates the most-voted player.
     * Handles ties by random elimination.
     */
    private void resolveVotes() {
        if (voteManager == null) {
            plugin.getLogger().warning("VoteManager is null, cannot resolve votes");
            return;
        }
        
        UUID mostVoted = voteManager.getMostVotedPlayer();
        List<UUID> tied = voteManager.getTiedPlayers();
        int maxVotes = voteManager.getMaxVoteCount();
        
        if (tied == null) {
            tied = Collections.emptyList();
        }

        if (mostVoted == null && tied.isEmpty()) {
            // No votes cast - skip elimination
            messageUtils.broadcast("§eNo votes were cast. No one is eliminated.");
            plugin.getLogger().info("No votes cast this round. Total voters: " + voteManager.getVoters().size() + 
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
            messageUtils.broadcast("§eVoting completed but no elimination occurred.");
            return;
        }

        // Eliminate the player
        if (toEliminate != null) {
            Player player = getPlayer(toEliminate);
            if (player != null && player.isOnline()) {
                try {
                    eliminatePlayer(player);
                    messageUtils.broadcast(resultMessage);
                } catch (Exception e) {
                    plugin.getLogger().severe("Error eliminating player " + player.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                    // Fallback: remove from state
                    gameState.removeAlivePlayer(toEliminate);
                    messageUtils.broadcast(resultMessage);
                }
            } else {
                // Player offline - remove from state
                gameState.removeAlivePlayer(toEliminate);
                messageUtils.broadcast(resultMessage);
            }
        }
    }

    /**
     * Handles a vote action from a player.
     * Returns true if vote was registered, false otherwise.
     */
    public boolean handleVote(Player voter, Player target) {
        if (voter == null || target == null) return false;

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
            plugin.getLogger().info("Vote registered: " + voter.getName() + " voted for " + target.getName());
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

        // Phase and permission checks
        if (!phaseManager.isPhase(GamePhase.SWIPE)) return;

        UUID medicId = medic.getUniqueId();
        UUID targetId = target.getUniqueId();

        // Only a Medic can cure
        if (gameState.getRole(medicId) != Role.MEDIC) {
            return;
        }

        // Must have an active cure window
        if (!isCureWindowActive(medicId)) {
            return;
        }

        // Check if target has a pending death to cure
        if (!gameState.hasPendingDeath(targetId)) {
            // No pending death to cure - close window but don't mark as cured (allow retry)
            activeCureWindow.remove(medicId);
            return;
        }

        // Only mark as cured if successfully applied to a player
        // This allows reactivation if window expires without use
        gameState.markCured(medicId);
        gameState.removePendingDeath(targetId);

        // Show subtle blue particles to everyone (hard to see but not impossible)
        // This provides a visual cue that someone was cured
        ParticleUtils.showColoredParticlesToEveryone(
            target,
            org.bukkit.Color.fromRGB(0, 100, 255), // Blue
            8, // 8 ticks = 0.4 seconds (split second)
            plugin
        );

        // Close the cure window
        activeCureWindow.remove(medicId);

        // Log for debugging (silent to players)
        plugin.getLogger().info("Medic " + medic.getName() + " cured " + target.getName());
    }

    /**
     * Handles a swipe action from a player.
     * Silent: no messages/holograms to shooter/target.
     * Infection recorded and pending death scheduled to be applied at discussion start.
     */
    public void handleSwipe(Player shooter, Player target) {
        if (shooter == null || target == null) return;

        // Phase and permission checks
        if (!phaseManager.isPhase(GamePhase.SWIPE)) return;

        UUID shooterId = shooter.getUniqueId();
        UUID targetId = target.getUniqueId();

        // Only a Spark can swipe
        if (gameState.getRole(shooterId) != Role.SPARK) {
            return;
        }

        // Must have an active swipe window
        if (!isSwipeWindowActive(shooterId)) {
            return;
        }

        // If target already has pending death, ignore duplicate (silent)
        // Don't mark as swiped - allow retry
        if (gameState.hasPendingDeath(targetId)) {
            activeSwipeWindow.remove(shooterId);
            return;
        }

        // Only mark as swiped if successfully applied to a player
        // This allows reactivation if window expires without use
        gameState.markSwiped(shooterId);
        gameState.markInfected(targetId);

        // Schedule pending death to be applied at discussion start.
        // We set the pending death time to a future time (discussion start),
        // but since we don't know exactly when discussion will start, we use a flag-based approach.
        // The pending death will be applied when startDiscussionPhase() is called.
        // For now, we mark it with current time but it will only be processed at discussion start.
        gameState.setPendingDeath(targetId, System.currentTimeMillis());

        // Show subtle lime particles to everyone (hard to see but not impossible)
        // This makes it harder for Spark to swipe players in front of everyone
        ParticleUtils.showColoredParticlesToEveryone(
            target,
            org.bukkit.Color.fromRGB(50, 205, 50), // Lime green
            8, // 8 ticks = 0.4 seconds (split second)
            plugin
        );

        // close the swipe window
        activeSwipeWindow.remove(shooterId);
    }

    /**
     * Eliminates a player from the game.
     */
    public void eliminatePlayer(Player player) {
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Attempted to eliminate null or offline player");
            return;
        }

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
        plugin.getLogger().info("Player " + player.getName() + " eliminated. Remaining alive: " + gameState.getAlivePlayerCount());

        // Check win conditions
        checkForWin();
    }

    /**
     * Checks if a win condition has been met and handles it.
     * Returns true if game ended.
     */
    public boolean checkForWin() {
        WinConditionChecker.WinResult result = winConditionChecker.checkWinConditions();

        if (result != null) {
            messageUtils.broadcast(result.getMessage());
            endGame();
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
        
        // Check if player is in an active game
        if (!gameState.isGameActive()) {
            plugin.getLogger().info("No active game to remove player from");
            return false;
        }
        
        if (!gameState.getAllParticipatingPlayerIds().contains(playerId)) {
            plugin.getLogger().info("Player " + player.getName() + " is not in the active game");
            return false;
        }
        
        plugin.getLogger().info("Removing player " + player.getName() + " from active game");
        
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
            checkForWin();
        }
        
        return true;
    }

    /**
     * Ends the game and resets all state.
     * This method is now PUBLIC so it can be called from commands.
     */
    public void endGame() {
        plugin.getLogger().info("Ending game. Final state: " + gameState.getDebugInfo());

        messageUtils.broadcast("§e§lGame ended!");

        // Restore all participating players' nametags, game modes, and inventories
        Set<UUID> allParticipatingIds = gameState.getAllParticipatingPlayerIds();
        if (allParticipatingIds != null) {
            for (UUID playerId : allParticipatingIds) {
                if (playerId == null) continue;
                Player player = org.bukkit.Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
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
                }
            }
        }

        // Clear game items from all players (reuse allParticipatingIds from above)
        if (allParticipatingIds != null) {
            for (UUID playerId : allParticipatingIds) {
                if (playerId == null) continue;
                Player player = org.bukkit.Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    try {
                        inventoryManager.clearGameItems(player);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error clearing game items for " + player.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
        
        // Clear backups
        playerBackups.clear();

        // Cancel any running timers
        try {
            if (swipePhaseHandler != null) {
                swipePhaseHandler.cancelSwipeTask();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error cancelling swipe task: " + e.getMessage());
        }
        
        try {
            if (discussionPhaseHandler != null) {
                discussionPhaseHandler.cancelDiscussionTask();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error cancelling discussion task: " + e.getMessage());
        }
        
        try {
            if (votingPhaseHandler != null) {
                votingPhaseHandler.cancelVotingTask();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error cancelling voting task: " + e.getMessage());
        }

        // Clear holograms
        try {
            if (hologramManager != null) {
                hologramManager.clearAll();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error clearing holograms: " + e.getMessage());
        }

        // Get session name BEFORE clearing game state
        String activeSessionName = gameState.getActiveSessionName();
        
        // Reset phase and game state
        phaseManager.reset();
        gameState.clearGameState();
        
        // Mark session as inactive and ensure it stays terminated
        if (activeSessionName != null) {
            try {
                // Access SessionManager via plugin instance
                com.ohacd.matchbox.Matchbox matchboxPlugin = (com.ohacd.matchbox.Matchbox) plugin;
                com.ohacd.matchbox.game.session.SessionManager sessionManager = matchboxPlugin.getSessionManager();
                if (sessionManager != null) {
                    com.ohacd.matchbox.game.session.GameSession session = sessionManager.getSession(activeSessionName);
                    if (session != null) {
                        session.setActive(false);
                        plugin.getLogger().info("Marked session '" + activeSessionName + "' as inactive after game end");
                    } else {
                        plugin.getLogger().warning("Session '" + activeSessionName + "' not found when trying to mark inactive");
                    }
                } else {
                    plugin.getLogger().warning("SessionManager is null when trying to mark session inactive");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to mark session as inactive: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            plugin.getLogger().info("No active session name found when ending game");
        }

        plugin.getLogger().info("Game ended successfully");
    }

    // Getters for external access
    public GameState getGameState() {
        return gameState;
    }

    public PhaseManager getPhaseManager() {
        return phaseManager;
    }

    public MessageUtils getMessageUtils() {
        return messageUtils;
    }

    public VoteManager getVoteManager() {
        return voteManager;
    }
    
    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }
}