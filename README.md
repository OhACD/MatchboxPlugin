**Tags:** Paper, Minecraft 1.21.10, Minigame, Social Deduction, Plugin

# Matchbox — Social Deduction Minigame for Paper

**Matchbox** is a modern social deduction game for Minecraft (2–20 players, best with 5–9), inspired by *Among Us* and designed specifically for multiplayer servers.

One player is secretly the **Spark**. Everyone else looks identical, has the same inventory, and must use discussion, observation, and voting to uncover them — before they’re eliminated.

The game is recording-proof, world-safe, and fully session-based, making it ideal for minigame servers, events, and friend-group servers.

---

## How the Game Works (Quick Overview)

Each round cycles through three phases:

**Swipe Phase**  
- Players explore and interact  
- The Spark secretly infects a player  
- The Medic may cure infections  
- Abilities introduce misinformation and paranoia  

**Discussion Phase**  
- Survivors are seated in a circle  
- Infected players die if not cured  
- Players discuss what they saw  

**Voting Phase**  
- Players vote using in-inventory papers  
- A **dynamic threshold system** decides if someone is eliminated  
- If no one is eliminated, pressure builds next round  

The game repeats until either:
- The Spark eliminates everyone, or
- The Spark is voted out

---

## Roles & Abilities

### Spark (Impostor)
- Infects one player per round
- Gains one secondary ability each round:
  - **Hunter Vision** — briefly reveal all players in a 32-block radius
  - **Spark Swap** — invisible position swap with another player
  - **Delusion** — plant fake infections to mislead the Medic

### Medic
- Can cure one infected player per round
- Uses Healing Sight to identify infections

### Innocents
- Observe, discuss, and vote to identify the Spark

---

## Why Use Matchbox

- Recording-proof design — identical skins, inventories, and abilities
- World-safe — no damage, no block breaking, automatic state restoration
- Parallel sessions — run multiple games at once
- Drop-in ready — ships with a complete default config for the official map
- Highly configurable — phases, abilities, voting, cosmetics, chat routing
- Nickname-friendly UI — titles, holograms, voting papers use display names

---

## Setup & Installation

1. Download:
   - `Matchbox.jar`
   - `ProtocolLib-5.4.0.jar`
2. Place both in your server’s `plugins/` folder
3. Download the official **M4tchb0x map**  
   https://www.planetminecraft.com/project/m4tchb0x-maps/
4. Restart the server

The plugin ships with a fully pre-configured default setup for the M4tchb0x map:
- Spawn locations
- Discussion seats
- Phase timings
- Player limits

You can start playing immediately or customize everything in-game.

**Requirements:**  
Paper 1.21.10 · Java 21+ · 2–20 players

---

## Basic Commands

**Players**
- `/matchbox join <session>`
- `/matchbox leave`
- `/matchbox list`

**Admins**
- `/matchbox start <session>`
- `/matchbox begin <session>`
- `/matchbox stop <session>`
- `/matchbox setspawn`
- `/matchbox setseat <number>`

Aliases: `/mb`, `/mbox`

(Full command & configuration reference available in the [`docs/`](docs/) directory.)

---

## Documentation

- **[Getting Started Guide](docs/GettingStarted.md)** — Installation and setup
- **[Commands Reference](docs/Commands.md)** — All commands with examples
- **[Configuration Guide](docs/Configuration.md)** — Complete config options
- **[API Documentation](docs/API.md)** — For developers integrating with Matchbox
- **[Contributing Guide](docs/Contributing.md)** — How to contribute to the project

For detailed API documentation, see [MatchboxAPI_Docs.md](MatchboxAPI_Docs.md).

---

## Links & Support

- **Modrinth Page:** https://modrinth.com/plugin/matchboxplugin
- **Discord Support:** https://discord.gg/BTDP3APfq8
- **GitHub Wiki:** https://github.com/OhACD/MatchboxPlugin/wiki
