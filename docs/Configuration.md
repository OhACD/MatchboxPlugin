# Configuration

Configuration file: `plugins/Matchbox/config.yml` (auto-created on first run).

Key sections and settings:

- `session.spawn-locations` — list of spawn coordinates used as defaults
- `discussion.seat-locations` — seat locations for discussion phase
- `discussion.duration`, `voting.duration`, `swipe.duration` — phase durations (seconds)
- `player.min` / `player.max` — player limits
- `spark.secondary-ability` — `random` / `hunter_vision` / `spark_swap` / `delusion`
- `voting.threshold.*` — voting threshold configuration
- `cosmetics.use-steve-skins` and `cosmetics.random-skins-enabled`

Tips:
- Use in-game commands (`/matchbox setspawn`, `/matchbox setseat`) to capture coordinates reliably.
- Changes to `config.yml` are applied on service restart or via the plugin commands where applicable.

If you need a full example, see `plugins/Matchbox/config.yml` after first run or ask on Discord.
