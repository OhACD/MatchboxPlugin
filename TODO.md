# TODO - Matchbox (comprehensive)

Notes:
- Each task includes: Priority, Short description, Affected files and Acceptance criteria.
- Priorities: P0 (blocker/required), P1 (high), P2 (medium), P3 (low / nice-to-have).

---

## P0 - Critical / Game-breaking fixes
1. Validate `startRound` contract to not require spawnLocations when using `GameSession` defaults
    - Files: `GameManager.java`, `MatchboxCommand.java`
    - Description: `startRound` currently rejects null/empty spawns. When starting from a `GameSession` that has spawns, ensure `begin` passes them and `startRound` accepts.
    - Acceptance: Starting via `/matchbox begin` works when session has spawns; no warnings in server log.

2. Ensure `GameState.validateState()` passes right after `startRound` role assignment
    - Files: `GameState.java`, `RoleAssigner.java`, `GameManager.java`
    - Description: `validateState` requires roles present if `allParticipatingPlayers` non-empty. `assignRoles` must cover all players (set INNOCENT for players beyond spark/medic).
    - Acceptance: After `startRound(...)` debug shows roles count equals participating players.

3. Prevent `NullPointerException` in scoreboard/team operations (scoreboard may be null)
    - Files: `NameTagManager.java`
    - Description: Add null checks for `Bukkit.getScoreboardManager()` and `getMainScoreboard()`. Safely handle registration errors.
    - Acceptance: No NPEs when server startup or plugin disable; nametag cleanup works.

4. Fix pending death scheduling semantics
    - Files: `GameState.java`, `GameManager.java`, `SwipePhaseHandler.java`, `DiscussionPhaseHandler` (if present)
    - Description: `setPendingDeath(targetId, System.currentTimeMillis())` marks immediate expiry. Pending deaths should be scheduled for application at discussion start (use a flag or set expiry to discussion start time).
    - Acceptance: Swipes during swipe phase do not eliminate players until discussion start; `getPendingDeathsDueAt(now)` returns due only when discussion begins.

---

## P1 - High priority features / UX / Stability
5. Implement voting phase flow (discussion -> voting -> resolution)
    - Files: `GameManager.java`, `VoteItemListener.java`, `VoteManager` (new), `DiscussionPhaseHandler` (expand)
    - Description: At end of discussion, open voting window, collect votes, resolve ties, apply elimination.
    - Acceptance: Voting works by item click or GUI; most-voted eliminated; logs and messages confirm result.

6. Implement Medic cure action
    - Files: `GameState.java`, `GameManager.java`, `SwipePhaseHandler.java`, new `MedicAbilityListener`
    - Description: Medic can cure a pending death before discussion (consume cure once per round). Ensure pendingDeathTime removal on cure.
    - Acceptance: Medic cure cancels pending death and notifies cured player only via server log (silent UI optional).

7. Fix offline player handling during pending death / elimination
    - Files: `GameManager.java`, `GameState.java`, `PlayerQuitListener.java`
    - Description: If a player goes offline with pending death, ensure they're removed from alive set; later rejoin shows spectator/removed state.
    - Acceptance: No inconsistent alive counts and no exceptions.

8. Persist and restore player inventories & locations when joining/leaving game
    - Files: `GameManager.java`, new `PlayerBackup` util, `PlayerQuitListener.java`
    - Description: When starting a game, store player inventories and restore on end or quit.
    - Acceptance: Inventories restored on `endGame()` and user quit.

9. Ensure tasks are canceled on plugin disable
    - Files: `Matchbox.java` (plugin main), `SwipePhaseHandler.java`, `DiscussionPhaseHandler.java`, `HologramManager.java`
    - Description: Cancel BukkitRunnables and remove armor stands on disable to avoid lingering tasks.
    - Acceptance: No scheduled tasks after plugin disable; clean removal of holograms.

---

## P2 - Medium priority: polish / refactor / tests
10. Add unit tests for `GameState` and `WinConditionChecker`
    - Files: `src/test/java/...`, `GameState.java`, `WinConditionChecker.java`
    - Acceptance: Tests for role assignment, pending death lifecycle, win detection pass in CI.

11. Add integration tests / mocks for `SwipePhaseHandler` timers
    - Files: tests + small refactor to allow injection of scheduler
    - Acceptance: Timers can be simulated in tests without real tick delays.

12. Add `VoteItemListener` implementation (skeleton present)
    - Files: `VoteItemListener.java`, `VoteManager.java`
    - Acceptance: Clicking vote item registers vote; voting UI optional.

13. Improve `HologramManager` stacking and lifetime control
    - Files: `HologramManager.java`
    - Description: Allow multiple stacked lines per player, configurable height and duration; ensure armor stands removed on plugin disable.
    - Acceptance: Holograms show expected text; no leaks. (FIXED)

14. Make `NameTagManager.restoreAllNameTags()` more conservative
    - Files: `NameTagManager.java`
    - Description: Use `cleanupAllTeams()` on disable and ensure only matchbox teams removed.
    - Acceptance: Teams cleaned; other plugins unaffected.

15. Add comprehensive JavaDocs and method-level comments
    - Files: project-wide
    - Acceptance: Core classes documented.

---

## P3 - Low / nice-to-have
16. Add spectator teleport to eliminated players (view map)
    - Files: `GameManager.java`
    - Acceptance: Eliminated players enter spectator and are teleported to discussion center or a spectator area.

17. Add config file (`config.yml`) for durations, radii, hologram heights
    - Files: `plugin.yml`, `Matchbox.java`, config resource
    - Acceptance: Values loaded and used instead of hardcoded constants.

18. Add per-session team naming strategy to avoid collisions and longer names (encode hash)
    - Files: `NameTagManager.java`
    - Acceptance: Team names stable and unique; respect 16-char limit.

19. Add metrics and telemetry (opt-in)
    - Files: plugin main, external metrics service
    - Acceptance: Opt-in toggle in config.

20. Improve start/stop command UX and tab completion edge-cases
    - Files: `MatchboxCommand.java`
    - Acceptance: Tab completion shows friendly names; localized messages.

---

## Tests / CI
- Add GitHub Actions workflow:
    - Run `mvn -DskipTests=false test` or Gradle equivalent
    - Run static analysis (SpotBugs / Checkstyle)

## Files/classes to add
- `VoteManager` — manage vote collection, ties, and elimination
- `DiscussionPhaseHandler` — timer + transition to voting
- `MedicAbilityListener` — medic cure handling
- `PlayerBackup` — store player inventory/location/health pre-game
- `plugin.yml` review/update

## Known issues / TODO checklist (quick)
- [ ] `setPendingDeath` uses current time (fix to delay until discussion)
- [ ] `RoleAssigner.assignRoles` must set `INNOCENT` explicitly for all remaining players
- [ ] `NameTagManager` missing null checks for scoreboard manager
- [ ] `VoteItemListener` is empty
- [ ] Handle players rejoining mid-game
- [ ] Add tests for edge-cases (1-player, 2-player, spark null)
- [ ] Ensure `PhaseManager.reset()` sets sensible `phaseStartTime`

---

## Recommended next immediate step
- Implement P0 items (validate start, roles assignment, name-tag null-safety, pending death semantics). After that, implement voting flow (P1 #5).
