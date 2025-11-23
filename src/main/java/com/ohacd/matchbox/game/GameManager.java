package com.ohacd.matchbox.game;

import com.ohacd.matchbox.game.hologram.HologramManager;
import com.ohacd.matchbox.game.phase.DiscussionPhaseHandler;
import com.ohacd.matchbox.game.phase.PhaseManager;
import com.ohacd.matchbox.game.phase.SwipePhaseHandler;
import com.ohacd.matchbox.game.role.RoleAssigner;
import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.MessageUtils;
import com.ohacd.matchbox.game.utils.NameTagManager;
import com.ohacd.matchbox.game.utils.Role;
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
    private final Map<UUID, Long> activeSwipeWindow = new ConcurrentHashMap<>();


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

        // Add players to alive set
        gameState.addAlivePlayers(players);

        // Assign roles (only once per game, not per round)
        List<Player> playerList = new ArrayList<>(players);
        roleAssigner.assignRoles(playerList);

        // Validate state after initialization
        if (!gameState.validateState()) {
            plugin.getLogger().severe("Game state invalid after initialization: " + gameState.getDebugInfo());
            return;
        }

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

        // Clear per-round state (swipes, cures) but keep roles and players
        gameState.clearRoundState();

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
     */
    public void startSwipeWindow(Player spark, int seconds) {
        if (spark == null) return;
        UUID id = spark.getUniqueId();

        // Only in SWIPE phase and only if spark role
        if (!phaseManager.isPhase(GamePhase.SWIPE)) return;
        if (gameState.getRole(id) != Role.SPARK) return;
        if (gameState.hasSwipedThisRound(id)) return; // already used this round

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
        // Apply pending deaths now so infected players do not participate in discussion
        long now = System.currentTimeMillis();
        Set<UUID> due = gameState.getPendingDeathsDueAt(now);
        if (!due.isEmpty()) {
            plugin.getLogger().info("Applying pending deaths for " + due.size() + " players at discussion start.");
        }
        for (UUID victimId : due) {
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
     * Ends the discussion phase and starts next round.
     */
    private void endDiscussionPhase() {
        plugin.getLogger().info("Ending discussion phase - Round " + gameState.getCurrentRound());
        messageUtils.broadcast("§e§l>> DISCUSSION PHASE ENDED <<");

        // TODO: Voting happens here (eliminate most voted player)

        // Check for win condition after discussion
        if (checkForWin()) {
            return; // Game ended
        }

        // Start next round
        startNewRound();
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

        // Spark can only swipe once per round
        if (gameState.hasSwipedThisRound(shooterId)) {
            // silent: do not notify
            return;
        }

        // Must have an active swipe window
        if (!isSwipeWindowActive(shooterId)) {
            return;
        }

        // If target already has pending death, ignore duplicate (silent)
        if (gameState.hasPendingDeath(targetId)) {
            // consume the swipe window but remain silent
            activeSwipeWindow.remove(shooterId);
            gameState.markSwiped(shooterId);
            return;
        }

        // Mark as swiped this round & infected
        gameState.markSwiped(shooterId);
        gameState.markInfected(targetId);

        // Schedule pending death to be applied at discussion start.
        // Use current time; we will APPLY pending deaths only when discussion begins.
        gameState.setPendingDeath(targetId, System.currentTimeMillis());

        // close the swipe window
        activeSwipeWindow.remove(shooterId);
    }

    /**
     * Eliminates a player from the game.
     */
    public void eliminatePlayer(Player player) {
        gameState.removeAlivePlayer(player.getUniqueId());
        // when a player gets eliminated they show their name tag
        NameTagManager.showNameTag(player);
        player.sendMessage("§cYou have been eliminated!");

        // Set spectator mode
        player.setGameMode(GameMode.SPECTATOR);

        // Check win conditions
        checkForWin();
    }

    /**
     * Checks if a win condition has been met and handles it.
     * Returns true if game ended.
     */
    private boolean checkForWin() {
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

        // Restore all participating players' nametags and game modes
        for (UUID playerId : gameState.getAllParticipatingPlayerIds()) {
            Player player = org.bukkit.Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                // Show nametag
                NameTagManager.showNameTag(player);

                // Reset to survival mode
                player.setGameMode(GameMode.SURVIVAL);

                // Clear inventory (TODO: Store and restore original inventories later)
                player.getInventory().clear();

                // Send feedback
                player.sendMessage("§aYou have been returned to normal state.");
            }
        }

        // Cancel any running timers
        swipePhaseHandler.cancelSwipeTask();
        discussionPhaseHandler.cancelDiscussionTask();

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
}