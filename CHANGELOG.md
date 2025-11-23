# Changelog

All notable changes to the Matchbox plugin will be documented in this file.


## [0.8.5-beta] - 2024-12-19 

### Added
- **Parallel Game Sessions**: Multiple games can now run simultaneously without interfering with each other
- **Session Context Management**: Introduced `SessionGameContext` to encapsulate all game state per session
- **Edge Case Handling**: Added comprehensive edge case handling for parallel sessions
  - Players cannot join multiple sessions simultaneously
  - Automatic cleanup of orphaned contexts
  - Session validation before context creation
  - Emergency cleanup on plugin disable
- **Memory Leak Prevention**: Enhanced session termination and cleanup
  - Sessions automatically end when all players leave
  - Sessions automatically end when game ends and winner is announced
  - Proper cleanup of timers, tasks, and player backups
  - Emergency cleanup method for plugin shutdown
- **Code Refactoring**: Refactored `GameManager` into smaller, focused classes
  - `GameLifecycleManager`: Handles game and round lifecycle (fully integrated)
  - `PlayerActionHandler`: Handles player actions (swipe, cure, vote) (fully integrated)

### Fixed
- **Timer Reset Bug**: Fixed issue where timers didn't reset when phases were force-skipped
  - Swipe timer now properly cancels when discussion phase starts
  - Discussion timer now properly cancels when voting phase starts
  - Voting timer now properly cancels when round ends
- **Memory Leak**: Fixed issue where sessions remained active after game end
  - Sessions now properly marked inactive when game ends
  - Sessions now properly removed when empty
  - Player backups now properly cleaned up
- **Chat Listener**: Fixed chat listener to work with parallel sessions
  - Now correctly identifies which session a player is in
  - Uses session-specific phase manager
- **Player Join Validation**: Added validation to prevent players from joining:
  - Full sessions (7 players)
  - Already started games
  - Multiple sessions simultaneously

### Changed
- **Command Permissions**: Updated command access
  - Normal players can use: `join`, `leave`, `list`
  - Operators only: `start`, `setspawn`, `setdiscussion`, `begin`, `stop`, `delete`, `debug`
- **Phase Handlers**: Refactored to support parallel sessions
  - Each phase handler now manages tasks per session using `Map<String, BukkitTask>`
  - Timers can be cancelled for specific sessions without affecting others
- **GameManager**: Major refactoring for parallel session support
  - Now manages `Map<String, SessionGameContext>` instead of single game state
  - Methods now take `sessionName` parameter or derive it from player UUID
  - Added helper methods: `getContext()`, `getContextForPlayer()`, `canPlayerJoinSession()`
  - Added `emergencyCleanup()` for plugin shutdown
- **Code Organization**: Refactored into smaller, focused classes
  - `GameLifecycleManager` fully integrated - handles game/round lifecycle management
  - `PlayerActionHandler` fully integrated - handles player actions (swipe, cure, vote)
  - `GameManager` significantly reduced in size and complexity

### Security
- Added validation to prevent context creation for non-existent sessions
- Added checks to prevent players from being in multiple sessions

---

## [0.8-beta] - Previous Version

### Features
- Core game mechanics (swipe, discussion, voting phases)
- Role assignment (Spark, Medic, Innocent)
- Inventory system with paper-based abilities
- Win condition detection
- Player backup and restore
- Session management

