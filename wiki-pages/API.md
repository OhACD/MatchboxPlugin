# API (Short)

A full API reference and examples are available in `MatchboxAPI_Docs.md` in the repository.

Short example (creating a session):

```java
Optional<ApiGameSession> session = MatchboxAPI.createSession("arena1")
    .withPlayers(players)
    .withSpawnPoints(spawns)
    .withCustomConfig(GameConfig.builder().discussionDuration(120).build())
    .start();
```

For event hooks and advanced usage, consult `MatchboxAPI_Docs.md` (in-repo) or the generated JavaDoc artifact.
