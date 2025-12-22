# Matchbox Plugin API Documentation

## Overview

The Matchbox Plugin API provides a clean, intuitive interface for minigame servers to integrate with Matchbox social deduction games. This API supports parallel game sessions, event-driven architecture, and flexible configuration options.

## Quick Start

```java
// Basic session creation
GameSession session = MatchboxAPI.createSessionBuilder("arena1")
    .withPlayers(arena.getPlayers())
    .withSpawnPoints(arena.getSpawnPoints())
    .withDiscussionLocation(arena.getDiscussionArea())
    .start()
    .orElseThrow(() -> new RuntimeException("Failed to create session"));

// Event listening
MatchboxAPI.addEventListener(new MatchboxEventListener() {
    @Override
    public void onGameStart(GameStartEvent event) {
        getLogger().info("Game started in session: " + event.getSessionName());
        // Handle game start - initialize UI, start timers, etc.
    }
    
    @Override
    public void onPlayerEliminate(PlayerEliminateEvent event) {
        // Handle player elimination - update stats, send messages, etc.
        Player eliminated = event.getPlayer();
        scoreboardManager.updatePlayerScore(eliminated, -10);
        getLogger().info("Player " + eliminated.getName() + " was eliminated");
    }
});
```

## Core Components

### MatchboxAPI
Main entry point for all API operations:
- Session management (create, get, end)
- Player queries (get session, get role)
- Event registration and management
- Phase information

### SessionBuilder
Fluent builder for game session configuration:
- Player management
- Spawn point configuration
- Discussion and seating locations
- Custom game configuration
- Session lifecycle (start, end)

### ApiGameSession
Wrapper for active game sessions:
- Session information (name, active status, phase, round)
- Player management (add, remove, get players)
- Game control (start, end, phase control)
- Role queries

### GameConfig
Configuration builder for game settings:
- Phase durations (swipe, discussion, voting)
- Ability settings (Spark/Medic secondary abilities)
- Cosmetic settings (skins, Steve skins)

### Event System
Comprehensive event system with these events:
- **GameStartEvent** - Game initialization
- **GameEndEvent** - Game completion
- **PhaseChangeEvent** - Phase transitions
- **PlayerJoinEvent** - Player joins session
- **PlayerLeaveEvent** - Player leaves session
- **PlayerEliminateEvent** - Player elimination
- **PlayerVoteEvent** - Voting actions
- **AbilityUseEvent** - Special ability usage
- **SwipeEvent** - Attack actions
- **CureEvent** - Healing actions

## Detailed Usage Examples

### 1. Arena Integration

```java
public class MatchboxArena {
    private final String arenaName;
    private final List<Location> spawnPoints;
    private final Location discussionArea;
    private final Map<Integer, Location> seats;
    private ApiGameSession currentSession;
    
    public MatchboxArena(String name, List<Location> spawns, Location discussion) {
        this.arenaName = name;
        this.spawnPoints = spawns;
        this.discussionArea = discussion;
        this.seats = new HashMap<>();
    }
    
    public boolean startGame(Collection<Player> players) {
        // Clean up any existing session
        if (currentSession != null) {
            MatchboxAPI.endSession(currentSession.getName());
            currentSession = null;
        }
        
        // Create new session
        Optional<ApiGameSession> session = MatchboxAPI.createSession(arenaName)
            .withPlayers(players)
            .withSpawnPoints(spawnPoints)
            .withDiscussionLocation(discussionArea)
            .withSeatLocations(seats)
            .start()
            .orElse(null);
            
        if (session.isPresent()) {
            currentSession = session.get();
            return true;
        }
        return false;
    }
    
    public void endGame() {
        if (currentSession != null) return;
        
        currentSession.endGame();
        currentSession = null;
    }
    
    public ApiGameSession getCurrentSession() {
        return currentSession;
    }
    
    public String getArenaName() {
        return arenaName;
    }
}
```

### 2. Custom Game Configuration

```java
// Create custom configuration
GameConfig customConfig = GameSession.configBuilder()
    .swipeDuration(180)        // 3 minutes
    .discussionDuration(120)     // 2 minutes  
    .votingDuration(60)          // 1 minute
    .sparkAbility("hunter_vision") // Force specific ability
    .randomSkins(true)          // Enable random skins
    .build();

// Use custom configuration
GameSession session = MatchboxAPI.createSession("custom_game")
    .withPlayers(players)
    .withSpawnPoints(spawns)
    .withCustomConfig(customConfig)
    .start()
    .orElse(null);
```

### 3. Event-Driven Minigame Integration

```java
public class MinigameManager implements MatchboxEventListener {
    private final Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private final Map<String, ArenaStats> arenaStats = new HashMap<>();
    
    @Override
    public void onGameStart(GameStartEvent event) {
        // Record game start
        String arenaName = extractArenaName(event.getSessionName());
        ArenaStats stats = arenaStats.computeIfAbsent(arenaName, k -> new ArenaStats());
        stats.gamesPlayed++;
        
        // Initialize player stats
        for (Player player : event.getPlayers()) {
            PlayerStats pStats = playerStats.computeIfAbsent(
                player.getUniqueId(), k -> new PlayerStats());
            pStats.gamesPlayed++;
        }
    }
    
    @Override
    public void onGameEnd(GameEndEvent event) {
        // Record game completion
        String arenaName = extractArenaName(event.getSessionName());
        ArenaStats stats = arenaStats.get(arenaName);
        
        if (stats != null && event.getReason() == GameEndEvent.EndReason.INNOCENTS_WIN) {
            stats.innocentWins++;
        } else if (stats != null && event.getReason() == GameEndEvent.EndReason.SPARK_WIN) {
            stats.sparkWins++;
        }
        
        // Update player participation stats
        for (Map.Entry<Player, Role> entry : event.getFinalRoles().entrySet()) {
            Player player = entry.getKey();
            Role role = entry.getValue();
            
            PlayerStats pStats = playerStats.get(player.getUniqueId());
            if (pStats != null) {
                if (role == Role.SPARK) {
                    pStats.sparkGames++;
                } else if (role == Role.MEDIC) {
                    pStats.medicGames++;
                } else {
                    pStats.innocentGames++;
                }
            }
        }
    }
    
    @Override
    public void onPlayerEliminate(PlayerEliminateEvent event) {
        // Record elimination
        PlayerStats pStats = playerStats.get(event.getPlayer().getUniqueId());
        if (pStats != null) {
            pStats.eliminations++;
        }
        
        // Award elimination points based on role and reason
        int points = calculateEliminationPoints(event);
        // Add to your points/reward system
        pointsManager.addPoints(event.getPlayer(), points);
    }
    
    private int calculateEliminationPoints(PlayerEliminateEvent event) {
        // Example point calculation
        switch (event.getReason()) {
            case VOTED_OUT:
                return 5; // Base elimination points
            case KILLED_BY_SPARK:
                return event.getRole() == Role.SPARK ? 15 : -5; // Bonus for Spark kill
            default:
                return 3; // Other eliminations
        }
    }
    
    private String extractArenaName(String sessionName) {
        // Extract arena name from session (implementation specific)
        return sessionName.replaceAll("_\\d+", "").trim();
    }
    
    // Stats classes
    private static class PlayerStats {
        int gamesPlayed;
        int sparkGames;
        int medicGames;
        int innocentGames;
        int eliminations;
    }
    
    private static class ArenaStats {
        int gamesPlayed;
        int innocentWins;
        int sparkWins;
    }
}
```

### 4. Phase Control Integration

```java
// Advanced phase control
public class PhaseController {
    public void forceDiscussion(ApiGameSession session) {
        if (session.getCurrentPhase() != GamePhase.DISCUSSION) {
            session.forcePhase(GamePhase.DISCUSSION);
            // Notify players, update HUD, etc.
            session.getPlayers().forEach(p -> 
                p.sendMessage("§eDiscussion phase forced by admin"));
            });
        }
    }
    
    public void skipToVoting(ApiGameSession session) {
        while (session.getCurrentPhase() != GamePhase.VOTING) {
            session.skipToNextPhase();
            // Add delay between phase skips
            try {
                Thread.sleep(1000); // 1 second delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
```

### 5. Multi-Arena Management

```java
public class MultiArenaManager {
    private final Map<String, MatchboxArena> arenas = new HashMap<>();
    private final List<ApiGameSession> activeSessions = new ArrayList<>();
    
    public boolean createArena(String name, List<Location> spawns, Location discussion) {
        if (arenas.containsKey(name)) {
            return false; // Arena already exists
        }
        
        MatchboxArena arena = new MatchboxArena(name, spawns, discussion);
        arenas.put(name, arena);
        return true;
    }
    
    public Optional<MatchboxArena> getArena(String name) {
        return Optional.ofNullable(arenas.get(name));
    }
    
    public Collection<MatchboxArena> getAllArenas() {
        return new ArrayList<>(arenas.values());
    }
    
    public boolean startGameInArena(String name, Collection<Player> players) {
        return getArena(name)
            .map(arena -> arena.startGame(players))
            .orElse(false);
    }
    
    public void endAllGames() {
        // End all active sessions
        for (ApiGameSession session : new ArrayList<>(activeSessions)) {
            try {
                session.endGame();
            } catch (Exception e) {
                logger.severe("Error ending session " + session.getName() + ": " + e.getMessage());
            }
        }
        activeSessions.clear();
    }
    
    public void cleanupArena(String name) {
        getArena(name).ifPresent(arena -> {
            arena.endGame();
        });
        
        // Remove any associated sessions
        activeSessions.removeIf(session -> 
            session.getName().startsWith(name));
    }
}
```

## Best Practices

### 1. Error Handling
Always check return values and handle failures gracefully:

```java
// Good
Optional<ApiGameSession> session = MatchboxAPI.createSession("arena1")
    .withPlayers(players)
    .start();

if (!session.isPresent()) {
    logger.warning("Failed to create session: insufficient players or spawns");
    return;
}

// Bad - will throw exception
GameSession session = MatchboxAPI.createSession("arena1")
    .withPlayers(players)
    .start()
    .orElseThrow(() -> new RuntimeException("Failed to create session"));
```

### 2. Resource Management
Clean up resources properly:

```java
public class SessionManager {
    public boolean endGame(String sessionName) {
        Optional<ApiGameSession> session = getSession(sessionName);
        if (session.isPresent()) {
            session.get().endGame();
            removeActiveSession(sessionName);
            return true;
        }
        return false;
    }
    
    // Proper cleanup in plugin disable
    public void shutdown() {
        // End all active games
        for (ApiGameSession session : new ArrayList<>(activeSessions)) {
            try {
                session.endGame();
            } catch (Exception e) {
                logger.severe("Error ending session " + session.getName() + ": " + e.getMessage());
            }
        }
        activeSessions.clear();
    }
}
```

### 3. Event Management
Register and unregister listeners properly:

```java
public class MyPlugin extends JavaPlugin {
    private final MatchboxEventListener listener = new MyEventListener();
    
    @Override
    public void onEnable() {
        MatchboxAPI.addEventListener(listener);
    }
    
    @Override
    public void onDisable() {
        MatchboxAPI.removeEventListener(listener);
    }
}

private class MyEventListener implements MatchboxEventListener {
    // Implement only the events you care about
    @Override
    public void onGameStart(GameStartEvent event) { /* ... */ }
    
    @Override
    public void onGameEnd(GameEndEvent event) { /* ... */ }
}
```

## Configuration Reference

### GameConfig Settings

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| swipeDuration | int (seconds) | 180 | Length of swipe phase |
| discussionDuration | int (seconds) | 60 | Length of discussion phase |
| votingDuration | int (seconds) | 30 | Length of voting phase |
| sparkSecondaryAbility | String | "random" | Spark's secondary ability |
| medicSecondaryAbility | String | "random" | Medic's secondary ability |
| randomSkinsEnabled | boolean | false | Enable random skins |
| useSteveSkins | boolean | true | Force Steve skins |

### Ability Values

#### Spark Secondary Abilities
- `"random"` - Random selection
- `"hunter_vision"` - Hunter Vision ability
- `"spark_swap"` - Position swap ability  
- `"delusion"` - Delusion ability

#### Medic Secondary Abilities
- `"random"` - Random selection
- `"healing_sight"` - Healing Sight ability

## Thread Safety

The API is designed to be thread-safe:
- All collections are thread-safe (ConcurrentHashMap, etc.)
- Operations are atomic where possible
- Event dispatching is synchronized
- Session operations include proper synchronization

## Version Compatibility

This API is versioned and designed for backward compatibility:
- `@since` tags indicate version features were introduced
- Deprecated methods are provided for legacy support
- Configuration defaults maintain compatibility
- Event system is extensible for future additions

## Support

For issues, questions, or feature requests:
- Check the main plugin documentation
- Review existing event implementations
- Use the provided examples as starting points
- Consider contributing to the plugin repository

## Migration Guide

### From Direct Plugin Access
If you were previously accessing Matchbox internals directly:

```java
// Old approach
GameManager gameManager = Matchbox.getInstance().getGameManager();
gameManager.startRound(players, spawns, discussion, sessionName);

// New API approach
Optional<ApiGameSession> session = MatchboxAPI.createSession(sessionName)
    .withPlayers(players)
    .withSpawnPoints(spawns)
    .withDiscussionLocation(discussion)
    .start()
    .orElse(null);
```

The API provides:
- Cleaner interface
- Better error handling
- Event-driven architecture
- Future compatibility guarantees
- Comprehensive documentation

---

*This API documentation covers the complete Matchbox Plugin public interface as of version 0.9.5*
