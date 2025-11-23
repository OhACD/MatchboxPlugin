package com.ohacd.matchbox.game;

import com.ohacd.matchbox.game.hologram.HologramManager;
import com.ohacd.matchbox.game.phase.DiscussionPhaseHandler;
import com.ohacd.matchbox.game.phase.PhaseManager;
import com.ohacd.matchbox.game.phase.SwipePhaseHandler;
import com.ohacd.matchbox.game.phase.VotingPhaseHandler;
import com.ohacd.matchbox.game.role.RoleAssigner;
import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.utils.GamePhase;
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
    private final Map<UUID, Long> activeSwipeWindow = new ConcurrentHashMap<>();
    private final Map<UUID, Long> activeCureWindow = new ConcurrentHashMap<>();

    // Player backups for restoration
    private final Map<UUID, PlayerBackup> playerBackups = new ConcurrentHashMap<>();

    // Current round data
    private Location currentDiscussionLocation;
    private List<Location> currentSpawnLocations;

    public GameManager(Plugin plugin, HologramManager hologramManager) {
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
            playerBackups.put(player.getUniqueId(), new PlayerBackup(player));
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

        // TODO: Give items, set role paper to the top right slot in the inventory.

        // Start first round
        startNewRound();
    }

    /**
     * Starts a new round (after discussion phase ends).
     * This does NOT reassign roles or reset player list.
     */
    private void startNewRound() {
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
        messageUtils.sendTitle(
                alivePlayers,
                "§6§lROUND " + roundNumber,
                "§eGet ready to swipe!",
                10, // fadeIn (0.5s)
                40, // stay (2s)
                10  // fadeOut (0.5s)
        );

        // Clear per-round state (swipes, cures, votes) but keep roles and players
        gameState.clearRoundState();
        voteManager.clearVotes();

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

            player.teleport(teleportLoc);
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
        for (Player player : swipePhaseHandler.getAlivePlayerObjects(gameState.getAlivePlayerIds())) {
            NameTagManager.hideNameTag(player, sessionName);
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
     * Shows glow effect on all alive players for 15 seconds (only visible to spark, works through walls).
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
        for (UUID playerId : gameState.getAlivePlayerIds()) {
            if (gameState.hasPendingDeath(playerId)) {
                Player infected = getPlayer(playerId);
                if (infected != null && infected.isOnline()) {
                    infectedPlayers.add(infected);
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
     * Shows glow effect on all alive players, visible only to the spark.
     * Glow works through walls and lasts for 15 seconds.
     * Note: Currently uses scoreboard teams. For true single-player visibility, ProtocolLib is needed.
     */
    private void showGlowOnPlayers(Player spark) {
        if (spark == null || !spark.isOnline()) return;

        // Get all alive players (excluding spark)
        java.util.List<Player> alivePlayers = new java.util.ArrayList<>();
        for (UUID playerId : gameState.getAlivePlayerIds()) {
            if (!playerId.equals(spark.getUniqueId())) {
                Player player = getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    alivePlayers.add(player);
                }
            }
        }

        if (alivePlayers.isEmpty()) {
            return;
        }

        // Create a temporary team with glow effect
        // Note: This is visible to all players. For true single-player visibility, ProtocolLib is needed.
        String teamName = "matchbox_glow_" + spark.getUniqueId().toString().substring(0, 8);
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        org.bukkit.scoreboard.Scoreboard board = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
        if (board == null) {
            plugin.getLogger().warning("Scoreboard not available for glow effect");
            return;
        }

        org.bukkit.scoreboard.Team team = board.getTeam(teamName);
        
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }

        final org.bukkit.scoreboard.Team finalTeam = team;
        final java.util.List<Player> finalAlivePlayers = new java.util.ArrayList<>(alivePlayers);

        // Enable glow effect using team color
        try {
            // Note: True glow effect requires ProtocolLib for single-player visibility
            // TODO: Use ProtocolLib for true single-player glow visibility
            // For now, we'll use particles as a workaround for visibility
        } catch (Exception e) {
            plugin.getLogger().warning("Could not set glow effect: " + e.getMessage());
        }

        // Add all alive players to the team
        for (Player player : finalAlivePlayers) {
            try {
                finalTeam.addEntry(player.getName());
            } catch (Exception e) {
                plugin.getLogger().warning("Could not add " + player.getName() + " to glow team: " + e.getMessage());
            }
        }

        // Show particles around players as a visual indicator (glow workaround)
        // This is visible only to the spark (using viewer.spawnParticle)
        for (Player target : finalAlivePlayers) {
            ParticleUtils.showRedParticlesOnPlayer(spark, target, 15, plugin);
        }

        // Remove glow after 15 seconds
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            try {
                // Remove players from team
                for (Player player : finalAlivePlayers) {
                    if (player != null && player.isOnline()) {
                        finalTeam.removeEntry(player.getName());
                    }
                }
                // Unregister team if empty
                if (finalTeam.getEntries().isEmpty()) {
                    finalTeam.unregister();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error removing glow effect: " + e.getMessage());
            }
        }, 15 * 20L); // 15 seconds

        plugin.getLogger().info("Showing glow on " + alivePlayers.size() + " player(s) to spark " + spark.getName());
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
        for (UUID playerId : gameState.getAlivePlayerIds()) {
            if (gameState.hasPendingDeath(playerId)) {
                allPendingDeaths.add(playerId);
            }
        }
        
        if (!allPendingDeaths.isEmpty()) {
            plugin.getLogger().info("Applying pending deaths for " + allPendingDeaths.size() + " players at discussion start.");
        }
        
        for (UUID victimId : allPendingDeaths) {
            if (!gameState.isAlive(victimId)) {
                gameState.removePendingDeath(victimId);
                continue;
            }
            Player victim = getPlayer(victimId);
            if (victim != null && victim.isOnline()) {
                eliminatePlayer(victim); // ensure this removes them from alive and cleans state
            } else {
                // server-side cleanup if offline
                gameState.removeAlivePlayer(victimId);
                gameState.removePendingDeath(victimId);
            }
            gameState.removePendingDeath(victimId);
        }

        // Clear infected flags for the round
        gameState.clearInfectedThisRound();

        // Teleport alive players to discussion location
        if (currentDiscussionLocation != null) {
            for (UUID playerId : gameState.getAlivePlayerIds()) {
                Player player = org.bukkit.Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.teleport(currentDiscussionLocation);
                }
            }
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

        // Start the voting timer and supply callback to endVotingPhase
        votingPhaseHandler.startVotingPhase(gameState.getAlivePlayerIds(), this::endVotingPhase);
    }

    /**
     * Ends the voting phase, resolves votes, and eliminates the most-voted player.
     */
    private void endVotingPhase() {
        plugin.getLogger().info("Ending voting phase - Round " + gameState.getCurrentRound());
        messageUtils.broadcast("§c§l>> VOTING PHASE ENDED <<");

        // Resolve votes and eliminate
        resolveVotes();

        // Check for win condition after voting
        if (checkForWin()) {
            return; // Game ended
        }

        // Start next round
        startNewRound();
    }

    /**
     * Resolves votes and eliminates the most-voted player.
     * Handles ties by random elimination.
     */
    private void resolveVotes() {
        UUID mostVoted = voteManager.getMostVotedPlayer();
        List<UUID> tied = voteManager.getTiedPlayers();
        int maxVotes = voteManager.getMaxVoteCount();

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
                eliminatePlayer(player);
                messageUtils.broadcast(resultMessage);
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
        NameTagManager.showNameTag(player);
        player.sendMessage("§cYou have been eliminated!");

        // Set spectator mode
        player.setGameMode(GameMode.SPECTATOR);

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
     * Ends the game and resets all state.
     * This method is now PUBLIC so it can be called from commands.
     */
    public void endGame() {
        plugin.getLogger().info("Ending game. Final state: " + gameState.getDebugInfo());

        messageUtils.broadcast("§e§lGame ended!");

        // Restore all participating players' nametags, game modes, and inventories
        for (UUID playerId : gameState.getAllParticipatingPlayerIds()) {
            Player player = org.bukkit.Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                // Show nametag
                NameTagManager.showNameTag(player);

                // Restore from backup if available
                PlayerBackup backup = playerBackups.get(playerId);
                if (backup != null) {
                    backup.restore(player);
                } else {
                    // Fallback: reset to survival mode and clear inventory
                    player.setGameMode(GameMode.SURVIVAL);
                    player.getInventory().clear();
                }

                // Send feedback
                player.sendMessage("§aYou have been returned to normal state.");
            }
        }

        // Clear backups
        playerBackups.clear();

        // Cancel any running timers
        swipePhaseHandler.cancelSwipeTask();
        discussionPhaseHandler.cancelDiscussionTask();
        votingPhaseHandler.cancelVotingTask();

        // Clear holograms
        hologramManager.clearAll();

        // Reset phase and game state
        phaseManager.reset();
        gameState.clearGameState();

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
}