# Matchbox - Minecraft Social Deduction Game

Matchbox is a paranoia-inducing 7-player social deduction game for Minecraft, inspired by Among Us and designed with recording-proof mechanics. Every player looks identical (Steve skin), and the impostor (Spark) must eliminate others without being caught.

---

## Game Overview

### Core Concept
- 7 players enter each round
- 1 Spark (impostor) tries to eliminate everyone
- 1 Medic can save infected players
- 5 Innocents must identify and vote out the Spark
- 100% recording-safe: Even if someone streams their screen, you can't tell who the Spark is

### Roles
- **Spark**: The impostor. Can infect one player per round using abilities.
- **Medic**: Can cure infected players before they die.
- **Innocent**: Regular players trying to survive and identify the Spark.

### Game Phases
1. **Swipe Phase** (5 minutes): Players explore, Spark can infect someone
2. **Discussion Phase**: Players teleport to discussion area and vote on who to eliminate
3. Repeat until win condition is met

---

## Installation

### Requirements
- **Minecraft Server**: Paper/Spigot 1.21+
- **Java**: 21+

### Setup
1. Download the latest `Matchbox.jar` from releases
2. Place in your server's `plugins/` folder
3. Restart the server
4. Configure spawn points and discussion areas

---

## Commands

### Session Management
/matchbox start - Create a new game session \
/matchbox join - Join an existing session \
/matchbox leave - Leave a session \
/matchbox list - List all active sessions

### Game Setup
/matchbox setspawn - Add a spawn location (use multiple times for 7+ spawns) \
/matchbox setdiscussion - Set the discussion area location

### Game Control
/matchbox begin - Start the game (requires 2+ players and spawn points) \ 
/matchbox stop - Stop and remove a session (restores all player states)

**Aliases**: `/mb` or `/mbox` work instead of `/matchbox`

---

## Quick Start Guide

### For Server Admins

1. **Create a Session**
   ```
   /matchbox start game1
   ```

2. **Set Spawn Locations** (stand at each location and run):
   ```
   /matchbox setspawn game1
   ```
   *(Repeat 7+ times at different locations)*

3. **Set Discussion Area**
   ```
   /matchbox setdiscussion game1
   ```

4. **Have Players Join**
   ```
   /matchbox join game1
   ```

5. **Start the Game**
   ```
   /matchbox begin game1
   ```

6. **Stop the Game Anytime**
   ```
   /matchbox stop game1
   ```

---

## Current Features (v1.0-SNAPSHOT)

### Implemented
- Session management system
- Role assignment (Spark, Medic, Innocent)
- Nametag hiding during gameplay
- Hologram-based chat bubbles (Roblox-style)
- Swipe phase with 90-second timer
- Discussion phase with teleportation
- Arrow-based hit detection and name reveal
- One swipe per round tracking
- Win condition checking
- Proper game end with state restoration
- Player elimination and spectator mode

### Planned (Coming Soon)
- Paper-based ability system (no hotbar giveaways)
- Spark Vision (Eagle Vision) ability
- Infection system with delayed death
- Medic cure ability with particle indicators
- Voting system with paper ballots
- Identical inventory layouts for all players
- Cooldown system for abilities
- ProtocolLib integration for client-side effects

---

## Design Philosophy

### Recording-Proof Paranoia
Every mechanic is designed to be **impossible to detect on stream**:
- All players have **identical inventories**
- Abilities are activated by **clicking papers** (same for everyone)
- No unique hotbar items for the Spark
- No particles, sounds, or titles visible to spectators
- Chat uses **holograms only** (no chat log)

### Vanilla Client Compatible
- No client-side mods required
- Works with completely vanilla Minecraft clients
- All special effects use server-side mechanics

---

## Technical Architecture

### Core Systems

### Key Design Decisions
- **Stateless rounds**: Each round is independent
- **UUID-based tracking**: Players tracked by UUID, not objects
- **Phase-driven logic**: All mechanics respect current game phase
- **Modular systems**: Each subsystem can be extended independently

---

## Known Issues
- Swipe currently uses arrows (will be replaced with paper-click system)
- Infection/death system not yet implemented
- Voting system not yet implemented
- Inventory system placeholder only

---

## Contributing

This is a personal project, but feel free to:
- Report bugs via issues
- Suggest features
- Fork and experiment

---

## License

*This project is under the **MIT** license*

---

## Roadmap

### Phase 1: Core Mechanics (Current)
- [x] Session system
- [x] Role assignment
- [x] Phase management
- [x] Nametag system
- [x] Chat holograms
- [x] Game stop command

### Phase 2: Ability System (Next)
- [ ] Inventory manager
- [ ] Paper-based abilities
- [ ] Spark Vision implementation
- [ ] Swipe mode system

### Phase 3: Infection & Cure
- [ ] Infection tracking
- [ ] Delayed death timer
- [ ] Medic cure ability
- [ ] ProtocolLib particles

### Phase 4: Voting System
- [ ] Voting papers
- [ ] Vote collection
- [ ] Tally and elimination

### Phase 5: Polish
- [ ] Fake names
- [ ] Ability cooldowns
- [ ] GUI improvements
- [ ] Sound effects

---

## Contact

**Developer**: OhACD  
**Version**: 1.0-SNAPSHOT  
**Minecraft API**: 1.21

---

## Disclaimer

This is an **early development build**. Expect bugs, missing features, and breaking changes. Do not use on production servers without thorough testing.

---

*"In Matchbox, paranoia isn't a bug—it's the core feature."*