# Changelog

All notable changes to the Matchbox plugin will be documented in this file.

## [0.9.2] - Latest Release (It's all about the base)

This is a quick patch that focuses on cleanup/QOL features and insuring everything works as intended.
Due to how Skins are handled, the steve skin override falls back to using Alex/Steve skins depending on
the player model, this will hopefully be handled better next update.

### Added
- **Cleaner version handling/project status handling**: Project status and versioning are now handled dynamically
    - The player will get notified if a newer version is available for the plugin
    - Under the hood cleanup for dynamic project status display and project versioning/version checking
- **Pre-discussion elimination notice**: Eliminations are now announced as titles 10 seconds before teleporting to discussion
    - Uses the MessageUtils title pipeline, matching other UI
    - Applies blindness and heavy slowness during the 10s hold, cleared on teleport
    - Works alongside seat teleports and discussion timers
- **Nickname support**: Voting papers, elimination titles, and hologram reveals now use display names with UUID-backed targeting to remain compatible with nick plugins (Dantizzle)
- **Debug force start**: `/matchbox debugstart <session>` lets admins start a game with fewer than the configured minimum players (still enforces spawn/seat validity)

### Fixed
- **Steve skin override**: `cosmetics.use-steve-skins` now reapplies Steve skins for gameplay while restoring players’ original skins during discussion (no more partial overrides), currently falls back to Alex skin; will be fixed in the next update (Dantizzle)
- **Location listing clarity**: `/matchbox listspawns` and `/matchbox listseatspawns` now display all configured entries and mark any with missing/not-loaded worlds instead of reporting none exist


## [0.9.1] - (Config and QOL update)

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
- **Steve Skins Option**: Config option to use default Steve skins for all players
  - `cosmetics.use-steve-skins` in config.yml
  - When enabled, all players get Steve skin regardless of random-skins-enabled setting
  - Works alongside existing random skins system
- **Gamemode Management**: Players automatically set to Adventure mode during games
  - Players set to Adventure mode when game starts
  - Original gamemode restored when game ends
  - Prevents block breaking and other survival interactions during gameplay
- **Session Creation Broadcast**: Upon new session creation via `/matchbox start <name>`, a broadcast message is sent to all players
  - Informs players about the new session and how to join
  - Encourages player participation and awareness of new games

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
- **Flower Pot Duplication Bug**: Fixed exploit where players could duplicate flowers from flower pots
  - All flower pot interactions (right-click) are now blocked during active games
  - Prevents players from extracting and stacking flowers from flower pots

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
