# Matchbox - Minecraft Social Deduction Game

A 7-player social deduction game for Minecraft with recording-proof mechanics.

---

## Installation

1. Download `Matchbox.jar` and `ProtocolLib-5.4.0.jar` and place them in your server's `plugins/` folder
2. Downlod the `Map` from https://www.planetminecraft.com/project/m4tchb0x-maps/ and place it in your server folder 
3. Restart your server
4. The plugin comes with a **default configuration** pre-configured for the **M4tchbox map**
   - All spawn locations and seat positions are already set up
   - You can start playing immediately or customize locations as needed

**Requirements**: Paper 1.21.10, Java 21+, 2-7 players per game

---

## Commands

### Player Commands
- `/matchbox join <session>` - Join a game session
- `/matchbox leave` - Leave your current session
- `/matchbox list` - List all active sessions

**Aliases**: `/mb` or `/mbox`

### Admin Commands
- `/matchbox start <session>` - Create a new game session
- `/matchbox setspawn` - Add a spawn location to config
- `/matchbox setseat <number>` - Set a seat location to config
- `/matchbox setdiscussion <session>` - Set discussion area location (session-specific)
- `/matchbox listspawns` - List all spawn locations in config
- `/matchbox listseatspawns` - List all seat locations in config
- `/matchbox removespawn <index>` - Remove a spawn location from config
- `/matchbox removeseat <number>` - Remove a seat location from config
- `/matchbox clearspawns` - Clear all spawn locations (requires confirmation)
- `/matchbox clearseats` - Clear all seat locations (requires confirmation)
- `/matchbox begin <session>` - Start the game
- `/matchbox debugstart <session>` - Force-start a game with debug override (allows starting with fewer than the configured minimum players; still enforces spawn/seat validity)
- `/matchbox stop <session>` - Stop and remove a session
- `/matchbox skip` - Skip current phase
- `/matchbox debug` - Show debug info

---

## Configuration

All settings in `plugins/Matchbox/config.yml` (auto-created on first run).

### Default Configuration

**The plugin ships with a complete default configuration for the M4tchbox map:**
-  11 pre-configured spawn locations
-  8 pre-configured seat locations for discussion phase
-  Optimized phase durations (Swipe: 180s, Discussion: 30s, Voting: 15s)
-  Player limits set (Min: 2, Max: 7)
-  Random skins enabled by default

**You can start playing immediately without any setup!** The default config works out-of-the-box with the M4tchbox map.

### Customizing Locations

**In-Game (Recommended)**
- Stand at location → `/matchbox setspawn`
- Stand at seat → `/matchbox setseat <number>`
- Locations automatically saved to config and persist across sessions
- Use `/matchbox listspawns` or `/matchbox listseatspawns` to view configured locations
  - These commands also flag entries whose worlds are missing or not loaded so you can fix them quickly
- Use `/matchbox clearspawns` or `/matchbox clearseats` to reset (requires confirmation)

**Config File**
```yaml
session:
  spawn-locations:
    - world: world
      x: 0.5
      y: 64.0
      z: 0.5

discussion:
  seat-locations:
    1:
      world: world
      x: 10.5
      y: 64.0
      z: 10.5
```

### Configurable Settings
- Phase durations (swipe, discussion, voting)
- Player limits (min/max)
- Seat spawn numbers
- Random skins toggle (`cosmetics.random-skins-enabled`)
- Steve skins option (`cosmetics.use-steve-skins`) - Use default Steve skin for all players

---

## How to Play

### Game Phases

**1. Swipe Phase (3 min)**
- Explore and interact
- Spark: Infect players
- Medic: Cure infected players
- Use abilities (Hunter Vision, Healing Sight)
- Chat appears as holograms

**2. Discussion Phase (30s)**
- Teleported to discussion seats
- Infected players die (if not cured)
- Discuss and share observations
- Skins return to normal

**3. Voting Phase (15s)**
- Right-click players to vote
- Most votes = eliminated
- Game continues to next round

### Roles

**Spark (Impostor)**
- Eliminate all players without being caught
- Infect one player per round
 - Each round, you roll one secondary ability: Hunter Vision **or** Spark Swap (teleport swap)

**Medic**
- Identify Spark and save infected players
- Cure one infected player per round
- Use Healing Sight once per round

**Innocent**
- Survive and identify the Spark
- Work together to vote out suspicious players

---

## Features

- Parallel sessions (multiple games simultaneously)
- Recording-proof design (identical inventories/abilities)
- Full configuration support
- Automatic location loading from config
- Damage protection during games
- Block interaction protection during games
- Skin system with phase-based restoration
- Nickname-friendly UI (titles, voting papers, holograms use player display names)
- Welcome message system for new players

---

## Support & Bug Reports

Found a bug or have suggestions? Join our Discord server:
**https://discord.gg/BTDP3APfq8**

Players will also see a welcome message when joining the server with information about the plugin and Discord link.

---

**Version**: 0.9.3  
**Minecraft API**: 1.21.10  
**License**: MIT  
**Developer**: OhACD
