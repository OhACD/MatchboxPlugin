package com.ohacd.matchbox.game;

import com.ohacd.matchbox.game.hologram.HologramManager;
import com.ohacd.matchbox.game.phase.PhaseManager;
import com.ohacd.matchbox.game.phase.SwipePhaseHandler;
import com.ohacd.matchbox.game.role.RoleAssigner;
import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.MessageUtils;
import com.ohacd.matchbox.game.utils.Role;
import com.ohacd.matchbox.game.win.WinConditionChecker;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Main game manager that coordinates all game systems.
 */
public class GameManager {
    private final Plugin plugin;
    private final HologramManager hologramManager;

    // Core systems
    private final GameState gameState;
    private final PhaseManager phaseManager;
    private final MessageUtils messageUtils;
    private final RoleAssigner roleAssigner;
    private final WinConditionChecker winConditionChecker;
    private final SwipePhaseHandler swipePhaseHandler;

    // Current round data
    private org.bukkit.Location currentDiscussionLocation;

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
    public void startRound(Collection<Player> players, List<org.bukkit.Location> spawnLocations) {
        startRound(players, spawnLocations, null);
    }

    /**
     * Starts a new round with the given players, spawn locations, and discussion location.
     * Players will be teleported to random spawn locations.
     */
    public void startRound(Collection<Player> players, List<org.bukkit.Location> spawnLocations, org.bukkit.Location discussionLocation) {
        this.currentDiscussionLocation = discussionLocation;
        // Clear previous round state
        gameState.clearRoundState();

        // Add players to alive set
        gameState.addAlivePlayers(players);

        // Teleport players to spawn locations if provided
        if (spawnLocations != null && !spawnLocations.isEmpty()) {
            List<org.bukkit.Location> shuffledSpawns = new ArrayList<>(spawnLocations);
            java.util.Collections.shuffle(shuffledSpawns);

            int spawnIndex = 0;
            for (Player player : players) {
                if (spawnIndex < shuffledSpawns.size()) {
                    player.teleport(shuffledSpawns.get(spawnIndex));
                    spawnIndex++;
                } else {
                    // If more players than spawns, cycle through spawns
                    player.teleport(shuffledSpawns.get(spawnIndex % shuffledSpawns.size()));
                    spawnIndex++;
                }
            }
        }

        // Assign roles
        List<Player> playerList = new ArrayList<>(players);
        roleAssigner.assignRoles(playerList);
        // TODO: Give items, set role paper to the top right slot in the inventory.

        // Start swipe phase
        startSwipePhase();
    }

    /**
     * Starts the swipe phase.
     */
    public void startSwipePhase() {
        phaseManager.setPhase(GamePhase.SWIPE);
        swipePhaseHandler.startSwipePhase(
            gameState.getAlivePlayerIds(),
            this::endSwipePhase
        );
    }

    /**
     * Ends the swipe phase and transitions to discussion.
     */
    public void endSwipePhase() {
        phaseManager.setPhase(GamePhase.DISCUSSION);
        messageUtils.sendPlainMessage("Swipe phase ended!");

        // Teleport players to discussion location if set
        if (currentDiscussionLocation != null) {
            for (UUID playerId : gameState.getAlivePlayerIds()) {
                org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.teleport(currentDiscussionLocation);
                }
            }
        }

        startDiscussionPhase(); // TODO: Death happens here
    }

    /**
     * Starts the discussion phase.
     */
    private void startDiscussionPhase() {
        // TODO: Implement discussion phase logic
        messageUtils.sendPlainMessage("Discussion phase started!");
    }

    /**
     * Handles a swipe action from a player.
     */
    public void handleSwipe(Player shooter, Player target) {
        // Verify phase
        if (!phaseManager.isPhase(GamePhase.SWIPE)) {
            shooter.sendMessage("You cannot swipe right now");
            return;
        }

        UUID shooterId = shooter.getUniqueId();
        if (gameState.getRole(shooterId) == Role.SPARK) {
            // Spark can only swipe once per round
            if (gameState.hasSwipedThisRound(shooterId)) {
                return;
            }
            gameState.markSwiped(shooterId);
        }
    }

    /**
     * Eliminates a player from the game.
     */
    public void eliminatePlayer(Player player) {
        gameState.removeAlivePlayer(player.getUniqueId());
        player.sendMessage("You have been eliminated!");

        // Set spectator mode
        player.setGameMode(GameMode.SPECTATOR);

        // Check win conditions
        checkForWin();
    }

    /**
     * Checks if a win condition has been met and handles it.
     */
    private void checkForWin() {
        WinConditionChecker.WinResult result = winConditionChecker.checkWinConditions();

        if (result != null) {
            messageUtils.broadcast(result.getMessage());
            endGame();
        }
    }

    /**
     * Ends the game and resets all state.
     */
    private void endGame() {
        messageUtils.broadcast("§eGame ended!");
        phaseManager.reset();
        gameState.clearRoundState();
        swipePhaseHandler.cancelSwipeTask();

        // TODO: Teleport players back, reset inventories, etc.
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
