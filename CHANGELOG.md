# Changelog

All notable changes to the Matchbox plugin will be documented in this file.

## [1.0.0] - Latest Release

### Added
- **Config File Support**: Full configuration system with `config.yml`
  - Configurable phase durations (swipe, discussion, voting)
  - Configurable player limits (min/max players per session)
  - Configurable seat spawn numbers for discussion phase
  - Random skins toggle (enable/disable)
- **Seat Location System**: Discussion phase seat spawns
  - Set seat locations via `/matchbox setseat <session> <number>` command
  - List seat locations via `/matchbox listseatspawns` command
  - Remove set seat locations via `/matchbox removeseat <index>` command
  - Configure seat coordinates in config file
  - Players automatically teleported to seats during discussion
- **Spawn Location Configuration**: Game spawn locations in config
  - Set spawn locations via `/matchbox setspawn <session>` command
  - List spawn locations via `/matchbox listspawns`
  - Remove set spawn locations via `/matchbox removespawn <index>`
  - Configure spawn coordinates in config file
  - Automatic loading from config when sessions don't have locations
- **Skin Restoration System**: Enhanced skin management
  - Player skins return to normal during discussion phase
  - Assigned skins restored after discussion ends
- **Damage Protection**: Players are invulnerable during active games
  - All damage sources blocked (hits, lava, fall damage, etc.)
  - Death prevention during games
  - Hunger loss prevention
- **Block Interaction Protection**: Block interactions disabled during games
  - Right-click and left-click on blocks blocked
  - Item interactions still work (abilities, voting)
- **Config Validation**: Added bound checks to all config values
  - Phase durations: Swipe (30-600s), Discussion (5-300s), Voting (5-120s)
  - Player limits: Min (2-7), Max (2-20) with cross-validation
  - Min spawn locations: (1-50)
  

### Changed
- **Location Management**: Locations can be set via commands or config file
  - Commands automatically save to config
  - Config locations used as defaults for new sessions
- **Phase Durations**: All phase durations configurable via config file
  - Default: Swipe (180s), Discussion (30s), Voting (15s)
- **Player Limits**: Min/max players configurable via config file
  - Default: Min 2, Max 7 players

### Fixed
- Session system cleanup and state management
- Deprecated method usage in leave command
- Discussion/seat location teleportation logic
- Player state restoration on game end
- All commands have been added to tab with proper permission checks

---

## [0.9.0]

### Added
- Global swipe/cure indicators broadcast to all nearby players
- Player-focused cure notifications
- Hunter Vision resilience with modern ProtocolLib pipeline

### Fixed
- Spark disconnects from legacy glow packets
- Random skin preload with proper UUID resolution

### Changed
- Terminology updates
- Import hygiene improvements

---

## [0.8.7-beta]

### Added
- ProtocolLib Hunter Vision support
- Temporary random skins during matches
- Cured player feedback

### Fixed
- Ability paper restoration
- Phase visibility for nametags
- Plugin disable cleanup

---

## [0.8.6-beta]

### Fixed
- Double round messages
- Session cleanup and termination
- Inventory protection scope
- Voting paper activation methods
- NullPointerException in listeners

---

## [0.8.5-beta]

### Added
- Parallel game sessions support
- Session context management
- Memory leak prevention

### Fixed
- Timer reset on phase skip
- Memory leaks from active sessions
- Chat listener for parallel sessions

---

## [0.8-beta]

### Features
- Core game mechanics
- Role assignment
- Inventory system
- Win condition detection
- Player backup and restore
- Session management
