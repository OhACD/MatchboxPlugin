# Configuration Guide

Matchbox separates server tuning from map geometry.

## Config Sources

### Global Server Config

Location: `plugins/Matchbox/config.yml`

Use this for server-level settings:

- Phase durations
- Voting thresholds and penalties
- Cosmetic toggles
- Ability selection preferences
- Session player limits

### World-Local Map Config

Location: `<world-folder>/matchbox-map.yml`

Use this for map-specific data:

- Map metadata (`map.id`, `map.display-name`, `map.creator`)
- Seat locations
- Spawn locations
- Discussion seat spawn order

This is the preferred source for map packages and multi-map servers.

## Global Config Fields

### Session and Player Limits

```yaml
session:
  min-players: 2
  max-players: 7
  min-spawn-locations: 1
```

### Phase Durations

```yaml
swipe:
  duration: 180

discussion:
  duration: 60

voting:
  duration: 30
```

### Voting Threshold and Penalty

```yaml
voting:
  threshold:
    at-20-players: 0.20
    at-7-players: 0.30
    at-3-players: 0.50
  penalty:
    per-phase: 0.0333
    max-phases: 3
    max-reduction: 0.10
```

### Abilities

```yaml
spark:
  secondary-ability: random  # random, hunter_vision, spark_swap, delusion

medic:
  secondary-ability: random  # random, healing_sight
```

### Cosmetics and Sign Mode

```yaml
cosmetics:
  use-steve-skins: true
  random-skins-enabled: false

sign-mode:
  enabled: true
```

## World-Local Map Config Example

```yaml
map:
  id: creator_map
  display-name: Creator Map
  creator: MapMaker
  schema-version: 1
  plugin-version: 0.9.7

discussion:
  seat-spawns: [1, 2, 3, 4, 5, 6, 7]
  seat-locations:
    1:
      world: creator-world
      x: 110.5
      y: 64.0
      z: 210.5
      yaw: 90.0
      pitch: 0.0

session:
  spawn-locations:
    - world: creator-world
      x: 100.5
      y: 64.0
      z: 200.5
      yaw: 0.0
      pitch: 0.0
```

## Editing Strategy

Recommended: use setup commands instead of manual file editing.

- `/mb setup init <map-id> [display name]`
- `/mb setup setspawn`
- `/mb setup setseat <seat-number>`
- `/mb setup seatspawns set ...`
- `/mb setup validate`

## Migration From Legacy Global Geometry

If old seat/spawn config exists in `plugins/Matchbox/config.yml`:

1. Enter the target world.
2. Run `/mb setup importlegacy`.
3. Use `/mb setup importlegacy overwrite` to force replacement.
4. Validate with `/mb setup validate`.

## Troubleshooting

### Game starts with global fallback instead of map config

- Check whether `<world>/matchbox-map.yml` exists.
- Run `/mb setup info` in that world.
- Run `/mb setup validate` and fix reported issues.

### Import copied seats but not spawns

- Use latest 0.9.7 build with the import empty-list fix.
- Re-run `/mb setup importlegacy overwrite`.

### Missing worlds in saved locations

- Ensure location worlds are loaded on server start.
- Re-save locations in the correct target world.

## Related

- [Commands](Commands)
- [Map Makers](Map-Makers)
- [FAQ](FAQ)
