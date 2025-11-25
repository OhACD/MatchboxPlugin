package com.ohacd.matchbox.game.lifecycle;

import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.phase.SwipePhaseHandler;
import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.utils.InventoryManager;
import com.ohacd.matchbox.game.utils.MessageUtils;
import com.ohacd.matchbox.game.utils.PlayerBackup;
import com.ohacd.matchbox.game.utils.Role;
import com.ohacd.matchbox.game.vote.VoteManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getPlayer;

/**
 * Manages game and round lifecycle (starting games, starting rounds, ending games).
 * Separated from GameManager to improve code organization.
 */
public class GameLifecycleManager {
    private final Plugin plugin;
    private final MessageUtils messageUtils;
    private final SwipePhaseHandler swipePhaseHandler;
    private final InventoryManager inventoryManager;
    private final Map<UUID, PlayerBackup> playerBackups;
    
    public GameLifecycleManager(Plugin plugin, MessageUtils messageUtils, 
                                SwipePhaseHandler swipePhaseHandler,
                                InventoryManager inventoryManager,
                                Map<UUID, PlayerBackup> playerBackups) {
        this.plugin = plugin;
        this.messageUtils = messageUtils;
        this.swipePhaseHandler = swipePhaseHandler;
        this.inventoryManager = inventoryManager;
        this.playerBackups = playerBackups;
    }
    
    /**
     * Starts a new game for a session.
     */
    public void startGame(SessionGameContext context, Collection<Player> players, 
                         List<Location> spawnLocations, Location discussionLocation, String sessionName) {
        if (context == null || players == null || players.isEmpty()) {
            plugin.getLogger().warning("Cannot start game - invalid parameters");
            return;
        }
        
        if (spawnLocations == null || spawnLocations.isEmpty()) {
            plugin.getLogger().warning("Cannot start game - no spawn locations");
            return;
        }
        
        GameState gameState = context.getGameState();
        
        // Store round data in context
        context.setCurrentDiscussionLocation(discussionLocation);
        context.setCurrentSpawnLocations(spawnLocations);
        
        // Clear ALL game state (this is the first round)
        gameState.clearGameState();
        context.getPhaseManager().reset();
        
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
        context.getRoleAssigner().assignRoles(playerList);
        
        // Validate state after initialization
        if (!gameState.validateState()) {
            plugin.getLogger().severe("Game state invalid after initialization: " + gameState.getDebugInfo());
            plugin.getLogger().severe("Players: " + players.size() + ", Roles assigned: " + gameState.getAllParticipatingPlayerIds().size());
            // Attempt cleanup
            gameState.clearGameState();
            context.getPhaseManager().reset();
            return;
        }
        
        plugin.getLogger().info("Game started successfully for session '" + sessionName + "' with " + players.size() + " players. Roles: " + 
            gameState.getAllParticipatingPlayerIds().stream()
                .map(id -> {
                    Role role = gameState.getRole(id);
                    Player p = getPlayer(id);
                    return (p != null ? p.getName() : id.toString()) + "=" + (role != null ? role : "null");
                })
                .collect(Collectors.joining(", ")));
    }
    
    /**
     * Starts a new round (after discussion phase ends).
     */
    public void startNewRound(SessionGameContext context, String sessionName) {
        if (context == null) {
            plugin.getLogger().warning("Cannot start new round - no context for session: " + sessionName);
            return;
        }
        
        GameState gameState = context.getGameState();
        VoteManager voteManager = context.getVoteManager();
        
        // Check if game is still active (not ended)
        if (!gameState.isGameActive()) {
            plugin.getLogger().info("Cannot start new round - game is not active for session: " + sessionName);
            return;
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
            return;
        }
        
        // Increment round counter
        gameState.incrementRound();
        int roundNumber = gameState.getCurrentRound();
        
        // Broadcast only to players in this session
        Collection<Player> alivePlayers = swipePhaseHandler.getAlivePlayerObjects(gameState.getAlivePlayerIds());
        if (alivePlayers != null && !alivePlayers.isEmpty()) {
            for (Player p : alivePlayers) {
                if (p != null && p.isOnline()) {
                    p.sendMessage("§6§l=== ROUND " + roundNumber + " ===");
                }
            }
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
        
        // Clear all inventories for all alive players before new round
        if (alivePlayers != null) {
            for (Player player : alivePlayers) {
                if (player != null && player.isOnline()) {
                    try {
                        player.getInventory().clear();
                        player.updateInventory();
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to clear inventory for " + player.getName() + " before new round: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * Teleports players to spawn locations for a round.
     */
    public void teleportPlayersToSpawns(SessionGameContext context, String sessionName) {
        if (context == null) {
            return;
        }
        
        GameState gameState = context.getGameState();
        List<Location> spawnLocations = context.getCurrentSpawnLocations();
        
        if (spawnLocations == null || spawnLocations.isEmpty()) {
            plugin.getLogger().warning("No spawn locations set for round " + gameState.getCurrentRound() + " in session " + sessionName);
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
        List<Location> shuffledSpawns = new ArrayList<>(spawnLocations);
        Collections.shuffle(shuffledSpawns);
        Collections.shuffle(alivePlayers);
        
        // Distribute players across spawns
        int spawnCount = shuffledSpawns.size();
        for (int i = 0; i < alivePlayers.size(); i++) {
            Player player = alivePlayers.get(i);
            Location spawnLoc = shuffledSpawns.get(i % spawnCount);
            
            Location teleportLoc = spawnLoc.clone();
            if (teleportLoc == null || teleportLoc.getWorld() == null) {
                plugin.getLogger().warning("Invalid spawn location for player " + player.getName() + ", skipping teleport");
                continue;
            }
            
            // Add small random offset if multiple players at same spawn
            if (alivePlayers.size() > spawnCount) {
                int playersAtThisSpawn = (i / spawnCount) + 1;
                if (playersAtThisSpawn > 1) {
                    double angle = (2 * Math.PI * i) / alivePlayers.size();
                    double radius = 2.0;
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
}

