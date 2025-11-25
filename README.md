# Matchbox - Minecraft Social Deduction Game

A paranoia-inducing 7-player social deduction game for Minecraft, inspired by Among Us and designed with recording-proof mechanics.

---

## What is Matchbox?

Matchbox is a social deduction game where 7 players work together to identify and eliminate the impostor (Spark) before it's too late. Every player looks identical, and the Spark must eliminate others without being caught. The twist? Everything is recording-proof—even if someone streams their screen, you can't tell who the Spark is!

### Core Concept
- 7 players enter each round
- 1 Spark (impostor) tries to eliminate everyone
- 1 Medic can save infected players
- 5 Innocents must identify and vote out the Spark
- 100% recording-safe: Even if someone streams their screen, you can't tell who the Spark is

---

## How to Play

### For Players

#### Getting Started
1. Join a game session using `/matchbox join <session-name>`
2. Wait for the game to start (admin will use `/matchbox begin`)
3. You'll be assigned a role: Spark, Medic, or Innocent

#### Your Role

**Spark (Impostor)**
- Your goal: Eliminate all other players without being caught
- You can infect one player per round using your ability
- Use Hunter Vision once per round to see all players
- Infected players die at the start of the discussion phase
- Win if you eliminate everyone or if you're not voted out

**Medic**
- Your goal: Help identify the Spark and save infected players
- You can cure one infected player per round using your ability
- Use Healing Sight once per round to see who's infected
- Save players before they die at the discussion phase
- Win if the Spark is eliminated

**Innocent**
- Your goal: Survive and help identify the Spark
- You have no special abilities
- Work with others to figure out who the Spark is
- Vote to eliminate suspicious players
- Win if the Spark is eliminated

#### Game Phases

**1. Swipe Phase (3 minutes)**
- Explore the map and interact with other players
- All players receive identical inventories automatically
- Spark: Right-click the Swipe paper (above hotbar slot 0) to activate, then right-click a player to infect
- Medic: Right-click the Healing Touch paper (above hotbar slot 0) to activate, then right-click an infected player to cure
- Use Hunter Vision (Spark) or Healing Sight (Medic) by right-clicking the paper above hotbar slot 1
- Use your crossbow and arrow to reveal a player's nametag (one arrow per round)
- Chat appears as holograms above players' heads (no chat log)

**2. Discussion Phase (30 seconds)**
- All players are teleported to the discussion area
- Infected players who weren't cured will die
- Chat normally to discuss who you think the Spark is
- Share information and observations

**3. Voting Phase (15 seconds)**
- Right-click on players to vote for who you think is the Spark
- The player with the most votes is eliminated
- Ties are resolved randomly
- The game continues to the next round

**4. Repeat**
- The cycle continues until a win condition is met

#### Win Conditions

**Spark Wins:**
- All other players are eliminated
- Spark survives until the end

**Innocents & Medic Win:**
- Spark is eliminated through voting
- Spark is the last player eliminated

---

## Installation

### Requirements
- Minecraft Server: Paper/Spigot 1.21 or higher
- Java: 21 or higher
- Players: 2-7 players per game (optimal: 7)

### Setup Steps

1. Download the Plugin
   - Download the latest Matchbox.jar from releases
   - Place it in your server's plugins/ folder

2. Restart Your Server
   - Restart the server to load the plugin
   - Check the console for "Matchbox has been enabled!"

3. Configure Your Map
   - Set spawn locations where players will start each round
   - Set a discussion area where players will meet to vote

---

## Commands

### For Players

| Command | Description |
|---------|-------------|
| `/matchbox join <session>` | Join a game session |
| `/matchbox leave` | Leave your current session |
| `/matchbox list` | List all active sessions |

Aliases: `/mb` or `/mbox` work instead of `/matchbox`

### For Server Admins

| Command | Description |
|---------|-------------|
| `/matchbox start <session>` | Create a new game session |
| `/matchbox setspawn <session>` | Add a spawn location (stand where you want it) |
| `/matchbox setdiscussion <session>` | Set the discussion area location |
| `/matchbox begin <session>` | Start the game (requires 2+ players) |
| `/matchbox stop <session>` | Stop and remove a session |

---

## Quick Start Guide

### For Server Admins

1. Create a Session
   ```
   /matchbox start game1
   ```

2. Set Spawn Locations (stand at each location and run):
   ```
   /matchbox setspawn game1
   ```
   (Repeat 7+ times at different locations)

3. Set Discussion Area (stand at the discussion area):
   ```
   /matchbox setdiscussion game1
   ```

4. Have Players Join
   ```
   /matchbox join game1
   ```

5. Start the Game (when you have 2+ players):
   ```
   /matchbox begin game1
   ```

6. Stop the Game Anytime
   ```
   /matchbox stop game1
   ```

### For Players

1. Join a Session
   ```
   /matchbox join game1
   ```

2. Wait for the Game to Start
   - You'll be teleported when the game begins
   - Your role will be assigned automatically

3. Play the Game
   - Follow the phase instructions
   - Use your abilities (if you have any)
   - Work together to find the Spark!

---

## Game Features

### Recording-Proof Design
- Identical Inventories: All players have the same items in the same slots
- Paper-Based Abilities: Abilities are activated by clicking papers (same for everyone)
- No Unique Items: The Spark doesn't have special items that would show on stream
- Hologram Chat: Chat appears as holograms during swipe phase (no chat log)
- Visual Effects: All effects are recording-safe and don't reveal roles

### Current Features (v0.8.5-beta)

**Status: Beta Testing Stage** - All core features are implemented and tested. Parallel sessions now supported!

**Fully Implemented:**
- ✅ **Parallel Game Sessions**: Multiple games can run simultaneously without interference
- ✅ Session management system (create, join, leave, start, stop)
- ✅ Role assignment (Spark, Medic, Innocent) with proper distribution
- ✅ Automatic inventory system with identical layouts for all players
- ✅ Role paper in top rightmost slot (shows your role and description)
- ✅ Ability papers automatically placed (Swipe/Cure above hotbar slot 0, Vision/Sight above hotbar slot 1)
- ✅ Fixed crossbow and arrow in hotbar (unmovable, one arrow per round)
- ✅ Nametag hiding during gameplay (properly restored after game)
- ✅ Hologram-based chat bubbles during swipe phase (no chat log)
- ✅ Normal chat during discussion and voting phases
- ✅ Swipe phase with 3-minute timer (configurable in code)
- ✅ Discussion phase with teleportation (30 seconds, configurable)
- ✅ Voting phase with right-click voting (15 seconds, configurable)
- ✅ Paper-based ability system (right-click to activate)
- ✅ Spark Swipe ability (infect players via paper, 8-second activation window)
- ✅ Spark Hunter Vision (particles on all players, 15 seconds, once per round, no nametag visibility)
- ✅ Medic Healing Touch (cure infected players, 8-second activation window)
- ✅ Medic Healing Sight (see infected players via particles, 15 seconds, once per round)
- ✅ Arrow system for nametag revelation (one per round, separate from swipe)
- ✅ Infection system with delayed death (applied at discussion start)
- ✅ Win condition detection (Spark wins, Innocents win)
- ✅ Player elimination and spectator mode
- ✅ Player state backup and restore (inventory, location, health, etc.)
- ✅ Session cleanup on game end (prevents new rounds after game ends)
- ✅ Comprehensive defensive programming and error handling
- ✅ Edge case handling (offline players, 1-2 player games, parallel sessions, etc.)
- ✅ Memory leak prevention (automatic session termination, proper cleanup)

**Recent Bug Fixes (v0.8.5-beta):**
- ✅ Fixed: Timer reset bug - timers now properly cancel when phases are force-skipped
- ✅ Fixed: Memory leak - sessions now properly terminate when game ends or all players leave
- ✅ Fixed: Chat listener now works correctly with parallel sessions
- ✅ Fixed: Players can no longer join multiple sessions simultaneously
- ✅ Fixed: Sessions properly validate before context creation
- ✅ Fixed: Emergency cleanup on plugin disable prevents orphaned contexts

### Planned for Full Release (v1.0)

**Required:**
- ⏳ **Configurable Settings**: Customize phase durations and game settings via config.yml
  - Allow server admins to adjust swipe phase (default: 180s), discussion phase (default: 30s), and voting phase (default: 15s) durations


---

## Tips & Strategies

### For Spark (Impostor)
- Blend in with the crowd—act like an innocent
- Use Hunter Vision strategically to track players
- Infect players when no one is watching
- Create suspicion on others during discussion
- Don't vote suspiciously—try to vote with the majority

### For Medic
- Use Healing Sight early to identify infected players
- Save players who are likely to be valuable in voting
- Share information carefully—don't reveal you're the Medic too early
- Watch for suspicious behavior during swipe phase

### For Innocents
- Pay attention to player movements and behavior
- Use your arrow strategically to reveal someone's nametag (one per round)
- Share observations during discussion
- Don't trust anyone completely
- Vote strategically—eliminate the most suspicious player
- Work together but stay cautious

---

## Known Limitations (Beta)

- **Configuration**: Phase durations are hardcoded (180s swipe, 30s discussion, 15s voting). Config.yml support planned for v1.0
- **Spectator Teleport**: Eliminated players enter spectator mode but aren't teleported to a spectator area yet (planned for v1.0)
- **Hunter Vision**: Uses particles for visibility (works perfectly, but ProtocolLib integration would enable true single-player glow effect - optional enhancement)

## Parallel Sessions

Starting with v0.8.5-beta, Matchbox supports multiple simultaneous game sessions. This means:
- Multiple groups can play different games at the same time
- Each session has its own game state, timers, and players
- Sessions are completely isolated from each other
- Memory is properly managed with automatic cleanup

---

## Reporting Issues

Found a bug or have a suggestion? Please report it:
- Create an issue on the project repository
- Include your Minecraft version and server type
- Describe what happened and how to reproduce it

---

## License

This project is under the MIT license.

---

## Credits

Developer: OhACD  
Version: 0.8.5-beta  
Minecraft API: 1.21

---

## Beta Testing

**Current Status: Beta Testing Stage**

The plugin is feature-complete and ready for beta testing! All core game mechanics are implemented and working. We're looking for feedback on:
- Game balance and timing
- User experience and clarity
- Edge cases and bugs
- Feature requests

Please report any issues or suggestions!

---

"In Matchbox, paranoia isn't a bug—it's the core feature."

---

## Additional Resources

- Development Documentation: See DEVELOPMENT.md for technical details, architecture, and development plans
- For Developers: Check DEVELOPMENT.md for code structure, known issues, and contribution guidelines
