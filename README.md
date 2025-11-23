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

**1. Swipe Phase (2 minutes)**
- Explore the map and interact with other players
- Spark: Use your ability to infect someone (right-click with paper in slot 9)
- Medic: Use your ability to cure infected players (right-click with paper in slot 9)
- Chat appears as holograms above players' heads (no chat log)
- Use Hunter Vision (Spark) or Healing Sight (Medic) from slot 8 once per round

**2. Discussion Phase (1 minute)**
- All players are teleported to the discussion area
- Infected players who weren't cured will die
- Chat normally to discuss who you think the Spark is
- Share information and observations

**3. Voting Phase (30 seconds)**
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

### Current Features (v1.0-SNAPSHOT)

Fully Implemented:
- Session management system
- Role assignment (Spark, Medic, Innocent)
- Nametag hiding during gameplay
- Hologram-based chat bubbles during swipe phase
- Normal chat during discussion and voting phases
- Swipe phase with 2-minute timer
- Discussion phase with teleportation (1 minute)
- Voting phase with right-click voting (30 seconds)
- Paper-based ability system (slots 8 and 9)
- Spark Swipe ability (infect players)
- Spark Hunter Vision (see all players once per round)
- Medic Healing Touch (cure infected players)
- Medic Healing Sight (see infected players once per round)
- Infection system with delayed death
- Win condition detection
- Player elimination and spectator mode
- Player state backup and restore

### Coming Soon
- Automatic Inventory System: Papers will be automatically placed in the correct slots
- Configurable Settings: Customize phase durations and game settings
- Enhanced Spectator Mode: Better experience for eliminated players

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
- Share observations during discussion
- Don't trust anyone completely
- Vote strategically—eliminate the most suspicious player
- Work together but stay cautious

---

## Known Limitations

- Manual Paper Placement: Currently, players need to manually place papers in slots 8 and 9 (will be automated soon)
- Hunter Vision: Uses a particle workaround for single-player visibility (ProtocolLib integration coming)
- Spectator Mode: Eliminated players enter spectator mode but aren't teleported to a spectator area yet

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
Version: 1.0-SNAPSHOT  
Minecraft API: 1.21

---

## Disclaimer

This is an early development build. Expect bugs, missing features, and potential breaking changes. Do not use on production servers without thorough testing.

---

"In Matchbox, paranoia isn't a bug—it's the core feature."

---

## Additional Resources

- Development Documentation: See DEVELOPMENT.md for technical details, architecture, and development plans
- For Developers: Check DEVELOPMENT.md for code structure, known issues, and contribution guidelines
