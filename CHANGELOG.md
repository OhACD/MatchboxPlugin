# Changelog

All notable changes to the Matchbox plugin will be documented in this file.


## [0.8.7-beta] - Latest

### Added
- **ProtocolLib Hunter Vision**: Sparks now get a live 35-block glow of players through walls. Automatically falls back to particle indicators (with a startup log) when ProtocolLib is missing.
- **Temporary Random Skins**: Players in an active game receive a curated random skin for the entire match and are restored afterwards.
- **Cured Player Feedback**: Players cured by the Medic are notified via title + log at the start of discussion.

### Fixed
- **Ability Paper Loss**: Swipe/Cure papers are restored when the activation window expires unused.
- **Phase Visibility**: Nametags are now visible during discussion/voting phases while remaining hidden during swipe.
- **Plugin Disable Runnable**: All scheduled tasks are cancelled during plugin shutdown to prevent asynchronous errors.
- **Import Hygiene**: Replaced fully qualified references with imports and tightened null/phase guards across listeners.

### Changed
- **Hunter Vision API**: Introduced `HunterVisionAdapter` abstraction, enabling clean separation between ProtocolLib and fallback implementations.

---

## [0.8.6-beta]

### Fixed
- **Double Round Messages**: Fixed issue where players received two round messages (round 1 and round 2) when starting a game
  - Removed duplicate `startNewRound()` call in `GameManager.startRound()`
  - Round counter now correctly starts at 1 instead of jumping to 2
- **Session Cleanup & Termination**: Fixed multiple session management issues
  - Sessions are now fully removed from SessionManager when game ends (complete termination)
  - Sessions properly terminated on game end - marked inactive and removed from SessionManager
  - Fixed memory leak where valid waiting sessions were being deleted by list command
  - Sessions are now only removed if they have 0 players (truly empty)
  - Inactive sessions with players are now correctly shown as "[Waiting]" in list command
- **Session List Command**: Fixed critical bug where valid sessions were being removed from the list
  - Sessions created with `/matchbox start` now properly appear in the list
  - Sessions persist until they're terminated or become empty
  - Fixed session name display - now shows case-preserved names correctly
  - `getAllSessionNames()` now returns case-preserved session names instead of lowercase keys
- **Inventory Protection**: Fixed issue where inventory protection was active for all players, even when not in a game
  - Inventory protection now only activates when player is in an active game session
  - Players can now freely move items when not in a game
  - Added `isPlayerInActiveGame()` check to all protection event handlers
  - `GameItemProtectionListener` now requires `GameManager` to check player game state
- **Spark Name Announcement**: Fixed win message to include the Spark's player name
  - Win messages now show: `"[PlayerName] (Spark) wins!"` instead of just "Spark wins!"
  - Applies to all Spark win conditions
  - Spark name is retrieved from UUID and displayed in all win scenarios
- **Voting Paper Activation**: Enhanced voting paper interaction support
  - Added left-click support in inventory (previously only right-click)
  - Right-click in inventory still works
  - Right-click when held in main hand still works (via VoteItemListener)
  - Players can now vote using any of these methods
- **VoteManager Logic**: Fixed redundant vote count update logic
  - Removed unreachable code that checked for previous vote targets
  - Simplified vote registration logic for better performance
- **NullPointerException in Listeners**: Fixed critical bug where listeners crashed when no active sessions exist
  - `VotePaperListener` now uses session-specific context instead of deprecated `getPhaseManager()`
  - `VoteItemListener` now uses session-specific context instead of deprecated methods
  - `HitRevealListener` now uses session-specific context instead of deprecated methods
  - All listeners now properly check if player is in an active game before processing events
  - Prevents crashes when players interact with items outside of active games
- **Voting & Ability Activation**: Enhanced activation methods for all papers
  - Voting papers can now be activated via: right-click in inventory, left-click in inventory, or right-click when held in main hand
  - Ability papers (Swipe, Cure, Hunter Vision, Healing Sight) can now be activated via: right-click in inventory, left-click in inventory, or right-click when held in main hand
  - Fixed right-click on main hand for voting papers - now properly works when holding paper
  - All activation methods now work consistently across all papers
- **Visual Ability Indicators**: Added visual feedback for used abilities and votes
  - Used papers are now replaced with gray dye items showing "[USED]" status
  - Gray dye maintains the same display name and lore as the original paper
  - Provides clear visual indication that an ability/vote has been used this round
  - Integrated seamlessly into existing logic without breaking functionality
- **Game State Safety Checks**: Enhanced all event listeners with comprehensive game state validation
  - All action handlers now verify game is active before processing any game logic
  - All ability listeners check `isGameActive()` before allowing ability activation
  - All voting listeners check `isGameActive()` before processing votes
  - All hit listeners check `isGameActive()` before processing player interactions
  - Prevents game logic from executing when players are not in active games
  - Prevents phase-specific logic from executing in wrong phases
  - Ensures robust separation between active games and inactive players

### Changed
- **Session Termination**: Sessions are now fully removed when game ends, not just marked inactive
  - Ensures complete cleanup and prevents memory leaks
  - Sessions automatically removed from SessionManager on game end
  - No need to use `stop` command for full session termination
- **Voting Phase Instructions**: Added helpful voting instructions during voting phase
  - Players now receive clear instructions on how to vote when voting phase begins
  - Instructions explain: right-click paper in inventory, left-click paper in inventory, or right-click player while holding their paper
  - Helps players understand voting mechanics, especially useful with custom skins where names might not be visible

### Code Quality & Documentation
- **Code Cleanup**: Comprehensive codebase cleanup and refactoring
  - Removed non-essential comments ("NEW:", "Note:", "Edge case:", etc.)
  - Removed obsolete TODO comments
  - Cleaned up verbose inline comments that didn't add value
  - Removed redundant code and dead branches
- **Documentation Improvements**: Enhanced code documentation
  - Added comprehensive JavaDoc to main plugin class (Matchbox)
  - Added detailed JavaDoc to enums (GamePhase, Role)
  - Improved class-level documentation (SessionGameContext, etc.)
  - Added proper @Deprecated annotations with migration guidance
  - Enhanced field documentation with clear descriptions
- **Code Quality**: Improved code clarity and maintainability
  - Removed dead code branches that could never execute
  - Better organized event listener registration
  - Improved error handling and logging

---

## [0.8.5-beta]  

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

