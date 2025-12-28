# API Documentation

Matchbox provides a comprehensive API for server plugins to integrate with game sessions, manage events, and customize gameplay.

## Overview

The Matchbox API allows you to:
- Create and manage game sessions programmatically
- Listen to game events (start, end, eliminations, etc.)
- Customize game configuration per session
- Integrate with minigame frameworks and arena systems
- Build custom chat processors and filters

## Quick Start

### Maven Dependency

Add Matchbox as a dependency in your `pom.xml`:

```xml
<dependency>
    <groupId>com.ohacd</groupId>
    <artifactId>matchbox</artifactId>
    <version>0.9.5</version>
    <scope>provided</scope>
</dependency>
```

### Gradle Dependency

Add Matchbox as a dependency in your `build.gradle`:

```gradle
dependencies {
    compileOnly 'com.ohacd:matchbox:0.9.5'
}
```

## Simple Example

### Creating a Session

```java
import com.ohacd.matchbox.api.MatchboxAPI;
import com.ohacd.matchbox.api.ApiGameSession;
import java.util.Optional;

// Create a new game session
Optional<ApiGameSession> session = MatchboxAPI.createSession("arena1")
    .withPlayers(players)
    .withSpawnPoints(spawnLocations)
    .withDiscussionLocation(discussionArea)
    .start();

if (session.isPresent()) {
    // Session created successfully
    ApiGameSession gameSession = session.get();
    plugin.getLogger().info("Game started with " + gameSession.getTotalPlayerCount() + " players");
} else {
    // Failed to create session
    plugin.getLogger().warning("Failed to create game session");
}
```

### Listening to Events

```java
import com.ohacd.matchbox.api.events.*;

public class MyEventListener implements MatchboxEventListener {
    
    @Override
    public void onGameStart(GameStartEvent event) {
        // Game has started
        String sessionName = event.getSessionName();
        Collection<Player> players = event.getPlayers();
        
        // Send custom messages, update scoreboards, etc.
        Bukkit.broadcastMessage("Game " + sessionName + " has started!");
    }
    
    @Override
    public void onGameEnd(GameEndEvent event) {
        // Game has ended
        GameEndEvent.EndReason reason = event.getReason();
        Map<Player, Role> finalRoles = event.getFinalRoles();
        
        // Award points, update statistics, etc.
        handleGameEnd(event.getSessionName(), reason, finalRoles);
    }
    
    @Override
    public void onPlayerEliminate(PlayerEliminateEvent event) {
        // A player was eliminated
        Player eliminated = event.getPlayer();
        Role role = event.getRole();
        PlayerEliminateEvent.EliminationReason reason = event.getReason();
        
        // Update player stats, send messages, etc.
        plugin.getLogger().info(eliminated.getName() + " was eliminated as " + role);
    }
}

// Register the listener
@Override
public void onEnable() {
    MatchboxAPI.addEventListener(new MyEventListener());
}
```

### Custom Configuration

```java
import com.ohacd.matchbox.api.GameConfig;

// Create custom game configuration
GameConfig customConfig = GameConfig.builder()
    .swipeDuration(180)           // 3 minutes
    .discussionDuration(120)       // 2 minutes
    .votingDuration(60)            // 1 minute
    .sparkAbility("hunter_vision") // Always use Hunter Vision
    .randomSkins(false)            // Disable random skins
    .build();

// Create session with custom config
Optional<ApiGameSession> session = MatchboxAPI.createSession("custom_game")
    .withPlayers(players)
    .withSpawnPoints(spawns)
    .withCustomConfig(customConfig)
    .start();
```

## Main API Classes

### MatchboxAPI
Main entry point for all API operations.

**Key Methods**:
- `createSession(String name)` — Create a new session builder
- `getSession(String name)` — Get an active session by name
- `getPlayerSession(Player player)` — Get the session a player is in
- `getPlayerRole(Player player)` — Get a player's role
- `getAllSessions()` — Get all active sessions
- `endSession(String name)` — End a specific session
- `endAllSessions()` — End all active sessions
- `addEventListener(MatchboxEventListener listener)` — Register event listener
- `removeEventListener(MatchboxEventListener listener)` — Unregister event listener

### SessionBuilder
Fluent builder for creating game sessions.

**Key Methods**:
- `withPlayers(Collection<Player>)` — Set initial players
- `withSpawnPoints(List<Location>)` — Set spawn locations
- `withDiscussionLocation(Location)` — Set discussion area
- `withSeatLocations(Map<Integer, Location>)` — Set discussion seats
- `withCustomConfig(GameConfig)` — Apply custom configuration
- `start()` — Start the session (returns Optional<ApiGameSession>)

### ApiGameSession
Wrapper for active game sessions.

**Key Methods**:
- `getName()` — Get session name
- `isActive()` — Check if session is active
- `getCurrentPhase()` — Get current game phase
- `getCurrentRound()` — Get current round number
- `getPlayers()` — Get all players in session
- `getAlivePlayerCount()` — Get count of alive players
- `getTotalPlayerCount()` — Get total player count
- `addPlayer(Player)` — Add a player to the session
- `removePlayer(Player)` — Remove a player from the session
- `endGame()` — End the game session

### GameConfig
Configuration builder for custom game settings.

**Key Methods**:
- `swipeDuration(int seconds)` — Set swipe phase duration
- `discussionDuration(int seconds)` — Set discussion phase duration
- `votingDuration(int seconds)` — Set voting phase duration
- `sparkAbility(String ability)` — Set Spark ability (random, hunter_vision, spark_swap, delusion)
- `randomSkins(boolean enabled)` — Enable/disable random skins
- `build()` — Build the configuration

## Event System

### Available Events

1. **GameStartEvent** — Game initialization
2. **GameEndEvent** — Game completion (with end reason)
3. **PhaseChangeEvent** — Phase transitions
4. **PlayerJoinEvent** — Player joins session
5. **PlayerLeaveEvent** — Player leaves session
6. **PlayerEliminateEvent** — Player elimination (with reason)
7. **PlayerVoteEvent** — Voting actions
8. **AbilityUseEvent** — Special ability usage
9. **SwipeEvent** — Spark attack actions
10. **CureEvent** — Medic healing actions

### Event Listener Interface

Implement `MatchboxEventListener` and override methods for events you care about:

```java
public interface MatchboxEventListener {
    default void onGameStart(GameStartEvent event) {}
    default void onGameEnd(GameEndEvent event) {}
    default void onPhaseChange(PhaseChangeEvent event) {}
    default void onPlayerJoin(PlayerJoinEvent event) {}
    default void onPlayerLeave(PlayerLeaveEvent event) {}
    default void onPlayerEliminate(PlayerEliminateEvent event) {}
    default void onPlayerVote(PlayerVoteEvent event) {}
    default void onAbilityUse(AbilityUseEvent event) {}
    default void onSwipe(SwipeEvent event) {}
    default void onCure(CureEvent event) {}
}
```

## Advanced Features

### Chat Pipeline System

Custom chat processors for filtering and routing chat messages:

```java
import com.ohacd.matchbox.api.chat.*;

public class CustomChatProcessor implements ChatProcessor {
    @Override
    public ChatProcessingResult process(ChatMessage message) {
        // Add custom formatting
        Component modified = Component.text("[GAME] ")
            .append(message.formattedMessage());
        
        return ChatProcessingResult.allowModified(
            message.withFormattedMessage(modified)
        );
    }
}

// Register the processor
MatchboxAPI.registerChatProcessor("arena1", new CustomChatProcessor());
```

### Bulk Session Management

```java
// End all sessions (useful for server maintenance)
int endedCount = MatchboxAPI.endAllSessions();
plugin.getLogger().info("Ended " + endedCount + " game sessions");
```

### Multi-Arena Integration

```java
public class ArenaManager {
    private final Map<String, ApiGameSession> activeSessions = new HashMap<>();
    
    public boolean startArena(String arenaName, Collection<Player> players) {
        Arena arena = getArena(arenaName);
        
        Optional<ApiGameSession> session = MatchboxAPI.createSession(arenaName)
            .withPlayers(players)
            .withSpawnPoints(arena.getSpawns())
            .withDiscussionLocation(arena.getDiscussion())
            .withSeatLocations(arena.getSeats())
            .start();
        
        session.ifPresent(s -> activeSessions.put(arenaName, s));
        return session.isPresent();
    }
    
    public void stopArena(String arenaName) {
        ApiGameSession session = activeSessions.remove(arenaName);
        if (session != null) {
            session.endGame();
        }
    }
}
```

## Complete Documentation

For comprehensive API documentation with detailed examples, advanced usage patterns, and best practices, see:

**[MatchboxAPI_Docs.md](https://github.com/OhACD/MatchboxPlugin/blob/main/MatchboxAPI_Docs.md)** in the repository.

This includes:
- Detailed method documentation
- Arena integration examples
- Event-driven minigame integration
- Multi-arena management
- Thread safety guidelines
- Migration guides

## Support

### Questions?
- Join our [Discord](https://discord.gg/BTDP3APfq8)
- Check the [Developer Notes](Developer-Notes) page
- Review example code in MatchboxAPI_Docs.md

### Contributing
See the [Contributing](Contributing) page for information on submitting API improvements or bug fixes.
