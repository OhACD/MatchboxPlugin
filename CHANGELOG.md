# Changelog

All notable changes to the Matchbox plugin will be documented in this file.

## [0.9.8] - In Development

**Community +** — Development build. Full robustness and reliability sweep.

### Fixed

- **Spectators silenced during SWIPE phase** — dead players were incorrectly caught by the SWIPE phase chat gate (both in sign-mode and hologram-mode games) and had their messages cancelled. Spectators now bypass the SWIPE gate entirely and are always routed to the spectator channel, regardless of phase or sign-mode state.
- **Sign-mode games bypass the chat pipeline** — the sign-mode early-return in the chat listener exited the handler for every active phase when sign mode was on. Discussion, Voting, and all non-SWIPE phases were falling through to normal server chat, meaning players across different parallel sessions could read each other's messages. All phases now route through the pipeline correctly.
- **Eliminated player chat visible to alive players** — the chat handler used to cache each player's alive status on their first message and never update it. A freshly-eliminated player kept being routed to the game channel (visible to all alive players) until the session ended. Routing now checks live game state on every message, so the change takes effect the instant someone is eliminated.
- **Chat pipeline handler memory leak** — the per-session chat handler was never removed when a session ended, so old sessions silently accumulated in memory. The handler is now cleaned up immediately when a game ends.
- **Disconnecting player could leave a session stuck** — if a player who was the last one online disconnected, the session would become permanently inactive rather than being fully ended and cleaned up. The server now ends the full game correctly when the last online participant leaves.
- **Chat errors swallowed silently** — exceptions thrown by the chat pipeline were caught and discarded without any server log entry, making routing failures invisible. Errors are now logged with the session name and player name.
- **Sign blocks visible to players in other worlds** — when a sign-mode sign was removed, it was hidden using the global online player list instead of only players in the same world. Players in other worlds could receive spurious block-removal packets.
- **Nick save delays risked data loss on shutdown** — nick changes were written to disk synchronously on every call. The save is now batched: changes are written once per second and a final guaranteed flush runs at shutdown so no nick is ever lost.
- **Flower pot duplication with sign in hand** — right-clicking a flower pot while holding a sign-mode sign during the SWIPE phase was not being blocked, allowing players to duplicate plants. The interaction is now always cancelled for flower pots regardless of what is in hand.
- **Game items and effects could persist briefly after a game ended** — phase timers (swipe countdown, discussion timer, voting timer) were cancelled after players had already started being restored, leaving a window where a timer callback could fire mid-restore and hand out game items or trigger a phase transition on a player being returned to their pre-game state. Timers are now cancelled before any player restoration begins.
- **Event listeners remained active during game teardown** — the game phase was not updated to `ENDED` until after all cleanup had run, so event listeners that gate on phase could fire on players mid-restore during game end. The phase is now set to `ENDED` at the very start of teardown.
- **Spurious server warning logged when a session ended naturally** — if two players disconnected in quick succession and the first disconnect fully ended the session, a false "Cannot end game" warning was logged when the second disconnect's handler ran (the context was already gone). This was a false alarm with no impact on players, but it obscured real warnings. The log entry is now an informational message that correctly identifies the already-ended state.

### Added

- **Win screen broadcast** — game end now shows a title (`INNOCENTS WIN` / `SPARK WINS`) and a chat banner to every participant (alive and eliminated spectators), not just alive players. The banner reveals the Spark's identity (nick and real name if nicked), round count, and alive survivors. Replaces the plain one-liner that was sent to alive players only.

### Fixed (Win Screen)

- **Win title cancelled by immediate teleport** — the win title was sent before `endGame()`, which teleported players home in the same tick and cleared the title overlay before it could be read. The win screen now fires **after** `endGame()`: all display data is snapshotted while the game state is still live, `endGame()` runs immediately (teleporting players home), then the title and chat banner are scheduled 20 ticks later so they appear once the player is back in their home location.

### Changed

- **Project status set to Development** — build is now flagged `DEVELOPMENT` for this cycle.
- **Game state thread safety hardened** — the alive-player and all-participant sets now use concurrent collections so they can be safely read from the async chat thread without data races.
- **Session start command guard** — a missing command registration now logs a clear SEVERE message at startup rather than throwing a silent null pointer.

---

## [0.9.7.1] - 2026-04-30

Hot patch release.

### Fixed

- **Nick-integrated inventory papers** — role papers now use nickname-aware display names instead of always showing the account name.
- **Immediate paper refresh on nick changes** — role papers now refresh immediately when nicks are applied, reset, or changed during active sessions and on session startup.

### Changed

- **Mid-game nick restrictions** — non-admin players can no longer change/reset/randomize their nick during an active game. Admins can still manage nicks while a game is running.

## [0.9.7] - 2026-04-30

Stable release.

### Added

- **World-local map config system** — Matchbox now supports baked per-world map configs via `matchbox-map.yml` for seat/spawn geometry and map metadata.
- **Session flow logging subsystem** — full session lifecycle is now structured-logged, including phase transitions, votes, swipes, cures, eliminations, wins, and session termination.
- **Sign-mode message logging** — sign chat content is now captured in session logs through `SignChangeEvent` tracking for fully auditable sign-mode gameplay.
- **Session observability API** — new `MatchboxAPI.getSessionLog(sessionName)` and `MatchboxAPI.getSessionStatistics(sessionName)` methods plus `ApiGameSession.getSessionLog()` and `ApiGameSession.getStatistics()` wrappers.
- **Deterministic role assignment strategy hook** — `SessionBuilder.withRoleAssignmentStrategy(...)` now allows integrations to control Spark/Medic assignment order per session.
- **Session ability extension hook** — `SessionBuilder.withAbilityHandlers(...)` now allows integrations to attach custom session-scoped ability handlers to Matchbox's central ability router.
- **Stable session ability context** — custom ability handlers now receive `SessionAbilityContext`, exposing actor, target, phase, round, role, and alive-state without requiring internal Matchbox access.
- **Production hardening pass** — Gradle now owns JaCoCo coverage verification, CI is tag-driven for releases, and the repo includes a formal production-readiness checklist.
- **Map setup command suite** — `/mb setup` tools for map creators:
  - `init`, `info`, `validate`, `importlegacy`
  - `setspawn`, `setseat`, `listspawns`, `listseats`, `removespawn`, `removeseat`, `clearspawns`, `clearseats`
  - `seatspawns list|add|remove|set` for explicit discussion seat order control.
- **Map metadata support** — map id, display name, creator, schema version, and plugin version can be initialized and read from world-local config.
- **Validation support for map packaging** — built-in map validation now checks required metadata and required seat/spawn geometry readiness.
- **Automatic legacy seat and spawn migration on startup** — when a world's map config has seat spawn references but seat locations or spawn locations still live in the global legacy config, Matchbox automatically migrates them on plugin startup using non-overwrite semantics (existing world-local data is always preserved).

### Fixed

- **Chat pipeline global delivery no longer mutates event message body** — global chat now preserves vanilla/Paper renderer behavior without injecting preformatted `<name>` payloads.
- **Nick color formatting now renders correctly in chat pipeline prefixes** — legacy section color codes in nick display names are preserved instead of appearing as raw `§` text.
- **Nick changes are now seamless during active sessions** — changing to a new nick no longer briefly clears the nametag/display before the new nick is applied.
- **Nick reminder action bar now updates immediately on nick commands** — `/mb nick`, `/mb nick random`, and `/mb nick reset` now refresh or clear the above-hotbar reminder instantly instead of waiting for the 2-second scheduler tick.
- **Legacy import now copies spawn locations when world-local spawn list exists but is empty** — `/mb setup importlegacy` no longer skips spawn migration in pre-initialized world configs.
- **Flower pot interaction protection now applies while holding sign-mode sign items** — players can no longer remove potted plants (including torchflower) by right-clicking pots with a sign during active games.
- **Chat pipeline observability gap** — chat pipeline decisions are now logged into session flow logs (allow/deny/cancel and resulting channel routing context).

### Changed

- **SkinManager cache/fallback cleanup** — random skin application now falls back to Steve deterministically when a cached/random skin entry cannot be resolved, preventing inconsistent visual state.
- **Session startup location loading is world-aware** — game start now resolves world-local seat/spawn defaults first and falls back to global config only when map config is unavailable.
- **Discussion seat spawn selection is world-aware** — configured seat spawn order can now be resolved from the active world map config.
- **Legacy seat/spawn commands removed from top-level command surface** — map geometry management now defaults to `/mb setup ...` commands.

### Migration Notes

- Existing servers can migrate old global geometry into a world-local config with `/mb setup importlegacy`.
- Use `/mb setup importlegacy overwrite` to force replace existing world-local values.
- For map distribution, include the world folder with `matchbox-map.yml` for true drop-and-play setup.

---

## [0.9.6] - Development (Nick System, Sign Mode, Skin & Stability)

Players can now compete under custom nicknames, communicate via signs during the swipe phase, and benefit from a more consistent and reliable game experience.

### Nick System

- **Set a nick** — pick any name (3–16 characters, letters/numbers/`_`/`-`) and it becomes your identity for the duration of the game. Your nick shows in chat, the player list, and above your head.
- **Random nick** — generates a realistic-looking username from a curated word list in styles like `FrostWolf`, `frost_wolf`, `FROST_Wolf`, or `frostWolf`. A numeric suffix is added automatically if the generated name is already taken.
- **Session-scoped** — nicks activate when a game starts and are removed the moment the game ends, you leave, or you disconnect. No carry-over between games.
- **Unique per session** — no two players in the same game can share a nick. The second player to claim a taken nick is blocked and told to pick another.
- **Persistent preference** — your chosen nick is saved across restarts. You only need to set it once.
- **Action bar reminder** — while outside a game, a subtle `Currently Nicked as: [Nick]` message sits above your hotbar so you always know what name you're carrying.
- **Admins** (`matchbox.admin`) can set, randomise, or reset any player's nick.

| Command | Who | What it does |
|---|---|---|
| `/mb nick` | anyone | See your current nick |
| `/mb nick <name>` | anyone | Set your nick |
| `/mb nick random` | anyone | Get a randomly generated nick |
| `/mb nick reset` | anyone | Remove your nick |
| `/mb nick <player> <name>` | admin | Set another player's nick |
| `/mb nick reset <player>` | admin | Remove another player's nick |
| `/mb nick random <player>` | admin | Generate a random nick for another player |

### Sign Mode

- **Signs as communication** — when `sign-mode.enabled: true`, players communicate by placing signs during the swipe phase instead of typing in chat. Each player receives a sign kit at the start of swipe.
- **Automatic cleanup** — all placed signs are removed when discussion begins or the game ends. Nothing is left behind in the world.
- **Chat bypass** — normal in-game chat is suppressed when sign mode is on, so signs are the only intended channel.
- **Join message toggle** (`join-message.enabled`) — server owners can disable the welcome title/message shown on join without affecting version notifications.

### Skin & Visual Changes

- Skin rendering upgraded to use ProtocolLib player-info packet rewrites for more consistent updates across clients.
- Steve fallback improved — when skin lookups fail (offline mode or API unavailable), all players receive a consistent classic Steve skin rather than a mix of random fallbacks.
- Paper compatibility raised to **1.21.11**.

### Bug Fixes

- **Eliminated players no longer appear in game chat** — they were being cached as alive in the chat pipeline after death. The cache is now cleared on elimination.
- **Arrow tracking is now per-session** — a round reset in one game no longer clears arrow-used state for players in a different concurrent session.
- **Players with failed state backups are excluded from the game** — previously, if saving a player's inventory or location failed, they were still added to the alive roster and couldn't be restored at game end.
- **Win screen shows the Spark's name, not their UUID** — when the Spark disconnects and the game ends, the win announcement now resolves their name correctly.
- **Delusion decay timer now cancels on session end** — the 30-second task scheduled when Delusion was used was not being tracked. It is now cancelled alongside other session tasks on cleanup.
- **Random skin preloading no longer silently fails** — UUID format issues during cached skin loading are handled correctly.
- **Hologram cleanup is safe during shutdown** — no new scheduler tasks are created while the plugin is disabling.
- **Session player limit fixed** — min/max validation now correctly accepts the full `2–20` range.
- **Default spawn world corrected** — default config now references `m4tchbox` instead of the old typo `m4tchb0x`.



## [0.9.5] - Release (API Module & Testing Suite)

### Added
- **Matchbox Plugin API**: Complete API module for external integration
  - MatchboxAPI main entry point for session management, player queries, and event registration
  - SessionBuilder fluent interface for creating and configuring game sessions
  - ApiGameSession wrapper for managing active game sessions with full control capabilities
  - GameConfig builder for custom game configuration (phase durations, abilities, cosmetics)
  - Comprehensive event system with 10+ events for game lifecycle integration
  - Thread-safe design with proper resource management and error handling
  - Parallel session support for minigame servers
  - Event-driven architecture for seamless integration with external plugins
  - Complete documentation with examples and best practices
  - Future compatibility guarantees with versioned API
- **Chat Pipeline System**: Advanced spectator chat isolation and customization
  - Complete separation between alive players and spectators chat channels
  - Spectators can see game chat but have isolated spectator-only communication
  - Custom chat processors for server-specific chat filtering and routing
  - ChatChannel enum for GAME, SPECTATOR, and GLOBAL chat routing
  - ChatProcessor interface for implementing custom chat logic
  - ChatMessage record with full metadata for advanced processing
  - Session-scoped chat handling with proper cleanup
  - Thread-safe pipeline processing with error isolation
- **Bulk Session Management**: New endAllSessions() API method
  - Ends all active game sessions gracefully in one operation
  - Returns count of successfully ended sessions
  - Perfect for server maintenance, emergency shutdowns, and cleanup operations
  - Thread-safe and handles errors gracefully per session
- **Enterprise-Grade Testing Suite**: Comprehensive performance and load testing framework
  - SessionStressTest.java - Tests concurrent session creation limits and performance characteristics
  - PerformanceMetricsCollector.java - Advanced metrics collection and analysis system
  - Real-time performance monitoring with detailed console output and file reports
  - Automated performance regression detection and bottleneck identification
  - Configurable load testing with gradual concurrency scaling (5-200+ sessions)
- **Complete API Test Coverage**: Replaced placeholder tests with comprehensive real-world scenarios
  - ApiGameSessionTest.java - Complete testing of all 25+ API methods and edge cases
  - Thread-safety validation under concurrent load conditions
  - Error handling and null input validation across all components
  - Integration testing for complex game lifecycle scenarios

### Changed
- **API annotations and stability markers**: Added explicit nullability and API status annotations across the com.ohacd.matchbox.api module
  - Introduced @Internal and @Experimental to mark implementation and unstable APIs
  - Adopted JetBrains @NotNull/@Nullable consistently on public API surfaces and added @since Javadoc where appropriate
  - Updated GameConfig nullability for optional settings and annotated event classes and listeners
- **Default Configuration**: Updated default config with optimized phase durations
  - Discussion phase duration set to 60 seconds by default (was 30 seconds)
  - Voting phase duration set to 30 seconds by default (was 15 seconds)
  - Provides more balanced gameplay experience with adequate discussion and voting time
- **Session Creation Error Handling**: Improved error type mapping in SessionBuilder
  - Validation errors now properly map to specific ErrorType enums
  - Better error reporting for debugging session creation failures
  - Enhanced error messages for different failure scenarios
- **Thread Safety Architecture**: Enhanced SessionManager with ConcurrentHashMap
  - Replaced standard HashMap with thread-safe concurrent collection
  - Improved performance under concurrent access patterns
  - Maintains backward compatibility while adding thread safety
  - Eliminated race conditions in session operations
- **Test Infrastructure Modernization**: Upgraded testing framework to enterprise standards
  - Replaced placeholder tests with comprehensive real-world scenarios
  - Added performance baselines and regression testing capabilities
  - Enhanced error reporting with detailed failure analysis
  - Implemented automated test result validation and alerting
  - Added concurrent test execution with race condition detection

### Fixed
- **API Testing Issues**: Resolved comprehensive test suite problems
  - Fixed mock player UUID conflicts causing session interference
  - Corrected SessionBuilder validation error type mapping
  - Fixed concurrent session creation test isolation
  - Resolved Collection casting issues in integration tests
  - Added proper mock player creation with unique identifiers
  - Enhanced session cleanup between test executions
- **Session Validation**: Improved session existence checking in API methods
  - endSession() now properly validates session existence before attempting to end
  - Prevents false positive returns when ending non-existent sessions
  - Better error handling in bulk operations
- **Pot Break Protection**: Fixed bug where decorated pots could break when hit by arrows during active games
  - Added PotBreakProtectionListener to prevent pot destruction during gameplay
  - Protects game environment integrity by canceling arrow hits on decorated pots
  - Maintains consistent protection system alongside other block interaction protections

(For full historical details see older releases below.)

---

## [0.9.4] - Ability System

### Added
- **Medic Secondary Ability System**: Medic now uses the same ability system as Spark
  - Healing Sight is now tracked as a secondary ability (preparing for future abilities)
  - System modeled after the Spark secondary ability system for consistency
  - Configurable via `medic.secondary-ability` in config.yml
  - Options: "random" (default), "healing_sight"
  - Ready for future medic abilities to be added
- **Delusion Ability**: New Spark secondary ability that creates fake infections
  - Spark can activate an 8-second window, then right-click a player to apply a fake infection
  - Fake infections appear identical to real infections to the medic (same particles)
  - Medic can see delusion infections using Healing Sight alongside real infections
  - Medic can cure delusion infections, wasting their cure on a non-infected player
  - Delusion infections automatically decay after 30 seconds
  - Delusion infections do not cause elimination when discussion phase starts
  - Configurable via `spark.secondary-ability` in config.yml
  - Options: "random" (default), "hunter_vision", "spark_swap", "delusion"

### Changed
- **Medic Ability System Architecture**: Refactored medic abilities to match Spark's system
  - Created `MedicSecondaryAbility` enum for ability tracking
  - Added ability selection logic in GameManager
  - Updated InventoryManager to handle medic abilities dynamically
  - MedicSightListener now checks if ability is active before allowing use
  - System is now extensible for adding new medic abilities in the future
- **Config steve skin override**: Default config now comes with steve skin override as true by default
  - Random skin toggle is now set to false by default
- **Debug command**: Fake infected (Delusion infection) is tracked by the debug command
  - Added the fake infected param to `/matchbox debug` command

### Fixed
- **Steve Skin Override**: Fixed inconsistent steve skin application
  - All players now consistently receive steve skins when enabled
  - Skins are reapplied at the start of each new round to ensure consistency
  - Fixed issue where some players would get alex or random skins instead of steve
- **Invalid default seat locations**: Fixed an error where default seat locations weren't loading correctly when used with the `m4tchb0x` map
  - Default Spawn/Seat locations are no longer linked to a world folder named `world` 
  - Now linked to a world folder named `m4tchb0x`

---

## [0.9.3] - (The little quirks of life)

### Added
- **Spark Secondary Ability System**: Spark now rolls a secondary ability each round
  - Hunter Vision _or_ Spark Swap, alongside Swipe
  - Spark Swap occupies the Hunter Vision slot (slot 28) when selected
  - Spark Swap teleports with preloaded chunks, preserved velocity, and preserved look direction (designed to be invisible)
- **Dynamic Voting Threshold System**: Voting thresholds now scale dynamically based on alive player count
  - Logarithmic scaling between key points: 20 players (20%), 7 players (30%), 3 players and below (50%)
  - Works for 2-20 players with smooth threshold transitions
  - Threshold display shown during voting phase: "Threshold: X/Y" (required votes / alive players) (Dantizzle)
- **Voting Penalty System**: Penalty applied when voting phases end without elimination
  - Each phase without elimination reduces the threshold by ~3.33% (configurable)
  - Maximum penalty of 10% after 3 consecutive no-elimination phases
  - Penalty resets when a successful elimination occurs
- **Enhanced Tie Handling**: Ties are now checked against the dynamic threshold
  - If tie vote count doesn't meet threshold: no elimination occurs
  - If tie vote count meets threshold: random player from tie is eliminated
- **Voting Configuration**: All voting threshold values are now configurable in `config.yml`
  - `voting.threshold.at-20-players` - Threshold percentage at 20 players (default: 0.20)
  - `voting.threshold.at-7-players` - Threshold percentage at 7 players (default: 0.30)
  - `voting.threshold.at-3-players` - Threshold percentage at 3 players and below (default: 0.50)
  - `voting.penalty.per-phase` - Penalty percentage per phase without elimination (default: 0.0333)
  - `voting.penalty.max-phases` - Maximum phases that accumulate penalty (default: 3)
  - `voting.penalty.max-reduction` - Maximum penalty reduction percentage (default: 0.10)
- **Spark Ability Configuration**: Spark secondary ability selection is now configurable
  - `spark.secondary-ability` - Choose Spark ability: "random" (default), "hunter_vision", or "spark_swap"
  - When set to "random", ability is randomly selected each round (default behavior)
  - When set to a specific ability, that ability is always used

### Changed
- **Ability System Routing**: Ability system routing remains unified; Spark inventories are rebuilt each swipe phase with the chosen secondary
- **Voting Phase Display**: Actionbar now shows both countdown timer and threshold requirement
  - Format: "Voting: Xs | Threshold: Y/Z" (timer and required votes / alive players)
  - Updates every second alongside the countdown timer
- **Voting Phase Instructions**: Improved voting phase start messages
  - Removed counter-intuitive "right-click player while holding paper" instruction
  - Added information that players can choose to not vote
  - Added threshold requirement display in title subtitle
  - Added explanation that no elimination occurs if threshold isn't met
- **Phase Visuals**: Skins are no longer reset to original during discussion/voting, and nametags remain hidden in those phases
  - Players keep their assigned game skins across phases until swipe starts again
  - Nametags stay hidden during discussion and voting (revealed only on elimination)
- **Vote Resolution**: Voting now requires meeting the dynamic threshold instead of simple majority
  - Players see clear feedback when threshold isn't met
  - System tracks consecutive no-elimination phases for penalty calculation
  
### Fixed
- **Steve skin override**: `cosmetics.use-steve-skins` now correctly applies Steve skins to players (Dantizzle)

---

## [0.9.2] - (It's all about the base)

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
