# Matchbox - Minecraft Social Deduction Game

A 7-player social deduction game for Minecraft with recording-proof mechanics.

---

## Installation

1. Download `Matchbox.jar` and place it in your server's `plugins/` folder
2. Restart your server
3. Configure locations (see Configuration section)

**Requirements**: Paper/Spigot 1.21+, Java 21+, 2-7 players per game

---

## Commands

### Player Commands
- `/matchbox join <session>` - Join a game session
- `/matchbox leave` - Leave your current session
- `/matchbox list` - List all active sessions

**Aliases**: `/mb` or `/mbox`

### Admin Commands
- `/matchbox start <session>` - Create a new game session
- `/matchbox setspawn <session>` - Add a spawn location
- `/matchbox setseat <session> <number>` - Set a seat location
- `/matchbox setdiscussion <session>` - Set discussion area location
- `/matchbox begin <session>` - Start the game
- `/matchbox stop <session>` - Stop and remove a session
- `/matchbox skip` - Skip current phase
- `/matchbox debug` - Show debug info

---

## Configuration

All settings in `plugins/Matchbox/config.yml` (auto-created on first run).

### Setting Up Locations

**In-Game (Recommended)**
- Stand at location → `/matchbox setspawn <session>`
- Stand at seat → `/matchbox setseat <session> <number>`
- Locations automatically saved to config

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
- Random skins toggle

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
- Use Hunter Vision once per round

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

---

**Version**: 1.0.0  
**Minecraft API**: 1.21.10  
**License**: MIT  
**Developer**: OhACD
