# Changelog

All notable changes to the Matchbox plugin will be documented in this file.

## [0.9.1] - Latest Release (Config and QOL update)

### Added
- **Config File Support**: Full configuration system with `config.yml`
  - Configurable phase durations (swipe, discussion, voting)
  - Configurable player limits (min/max players per session)
  - Configurable seat spawn numbers for discussion phase
  - Random skins toggle (enable/disable)
- **Seat Location System**: Discussion phase seat spawns
  - Set seat locations via `/matchbox setseat <number>` command (saves to config)
  - List seat locations via `/matchbox listseatspawns` command
  - Remove set seat locations via `/matchbox removeseat <number>` command
  - Configure seat coordinates in config file
  - Players automatically teleported to seats during discussion
- **Spawn Location Configuration**: Game spawn locations in config
  - Set spawn locations via `/matchbox setspawn` command (saves to config)
  - List spawn locations via `/matchbox listspawns`
  - Remove set spawn locations via `/matchbox removespawn <index>`
  - Configure spawn coordinates in config file
  - Automatic loading from config when sessions don't have locations
- **Skin Restoration System**: Enhanced skin management
  - Player skins return to normal during discussion phase
  - Assigned skins restored after discussion ends
- **Damage Protection**: Players are invulnerable during active games
  - All damage sources blocked (hits, lava, fall damage, etc.)
  - Arrows hit players (for nametag revelation) but deal no damage
  - Death prevention during games
  - Hunger loss prevention
- **Block Interaction Protection**: Block interactions disabled during games
  - Right-click and left-click on blocks blocked
  - Item interactions still work (abilities, voting)
- **Config Validation**: Added bound checks to all config values
  - Phase durations: Swipe (30-600s), Discussion (5-300s), Voting (5-120s)
  - Player limits: Min (2-7), Max (2-20) with cross-validation
  - Min spawn locations: (1-50)
- **Clear Commands**: Added commands to clear all spawn/seat locations with confirmation
  - `/matchbox clearspawns` - Clear all spawn locations (requires confirmation)
  - `/matchbox clearseats` - Clear all seat locations (requires confirmation)
  - Both commands require typing the command again with `confirm` to execute
- **Config Usage Notification**: Players are notified when a game starts using config defaults
  - Shows how many spawn and seat locations were loaded from config
  - Helps players understand when config is being used vs session-specific settings
- **Default Configuration**: Plugin ships with complete default config for M4tchbox map
  - 11 pre-configured spawn locations
  - 8 pre-configured seat locations
  - Optimized phase durations and player limits
  - Ready to play immediately without setup
- **Welcome Message System**: Players receive a welcome message when joining the server
  - Title animation welcoming players to Matchbox
  - Information about the game, current version, and status
  - Discord link for bug reports and suggestions (https://discord.gg/BTDP3APfq8)

### Changed
- **Location Management**: Locations can be set via commands or config file
  - Commands automatically save to config (config-first approach)
  - Config locations used as defaults for new sessions
  - Session-specific locations can still be set via `setdiscussion` command
- **setspawn Command**: No longer requires session name - saves directly to config
  - Usage: `/matchbox setspawn` (was: `/matchbox setspawn <session>`)
- **setseat Command**: No longer requires session name - saves directly to config
  - Usage: `/matchbox setseat <number>` (was: `/matchbox setseat <session> <number>`)
- **Phase Durations**: All phase durations configurable via config file
  - Default: Swipe (180s), Discussion (30s), Voting (15s)
- **Player Limits**: Min/max players configurable via config file
  - Default: Min 2, Max 7 players

### Fixed
- **Spawn Location Loading**: Fixed issue where config spawn locations weren't checked before requiring manual spawn setup
  - Config spawns are now loaded first before checking if spawns are sufficient
  - Players are notified when starting from config defaults
- **Arrow Damage**: Fixed arrows passing through players due to invincibility
  - Arrows now properly hit players (for nametag revelation) but deal no damage
  - Other damage sources remain blocked during active games
- **Skin Manager**: Improved offline mode compatibility
  - Better error handling for players without valid skin data
  - Graceful fallback when skin properties are missing
- **Spawn Distribution**: Ensured all config spawn locations are properly loaded and used
  - Fixed issue where only 1 spawn location was used even when multiple were in config
  - Fixed players spawning at same position when multiple players share one spawn location
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
