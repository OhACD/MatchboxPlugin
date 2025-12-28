# Configuration Guide

This page explains all configuration options available in Matchbox.

## Configuration File

**Location**: `plugins/Matchbox/config.yml`

The configuration file is automatically created on first run with sensible defaults for the M4tchb0x map.

## Configuration Sections

### Session Settings

#### Spawn Locations
Default spawn points for players when a game starts.

```yaml
session:
  spawn-locations:
    - world: m4tchb0x
      x: 100.5
      y: 64.0
      z: 200.5
      yaw: 0.0
      pitch: 0.0
```

**Format**: List of locations with world name and coordinates  
**Required**: At least as many spawns as max player count  
**Note**: Set using `/matchbox setspawn` command

#### Player Limits

```yaml
player:
  min: 2   # Minimum players to start (2-7)
  max: 7   # Maximum players per session (2-20)
```

**min**: Minimum players required to begin a game (2-7)  
**max**: Maximum players allowed in a session (2-20)  
**Note**: Game is best with 5-9 players

### Phase Durations

Control how long each game phase lasts.

#### Swipe Phase

```yaml
swipe:
  duration: 180  # seconds (30-600)
```

**Default**: 180 seconds (3 minutes)  
**Range**: 30-600 seconds  
**Description**: Time for exploration, abilities, and Spark infections

#### Discussion Phase

```yaml
discussion:
  duration: 60  # seconds (5-300)
  seat-locations:
    1:
      world: m4tchb0x
      x: 110.5
      y: 64.0
      z: 210.5
```

**Default**: 60 seconds (1 minute)  
**Range**: 5-300 seconds  
**Description**: Time for players to discuss and share observations  
**Note**: Seat locations set using `/matchbox setseat <number>` command

#### Voting Phase

```yaml
voting:
  duration: 30  # seconds (5-120)
```

**Default**: 30 seconds  
**Range**: 5-120 seconds  
**Description**: Time for players to vote on who to eliminate

### Voting System

#### Dynamic Thresholds

Configure how many votes are needed to eliminate a player based on alive player count.

```yaml
voting:
  threshold:
    at-20-players: 0.20  # 20% of votes at 20 players
    at-7-players: 0.30   # 30% of votes at 7 players
    at-3-players: 0.50   # 50% of votes at 3 or fewer players
```

**Description**: Threshold scales logarithmically between these points  
**Example**: With 10 alive players, threshold is ~25-28%

#### Penalty System

Voting penalty when no elimination occurs.

```yaml
voting:
  penalty:
    per-phase: 0.0333      # ~3.33% reduction per phase
    max-phases: 3          # Maximum phases to accumulate
    max-reduction: 0.10    # Maximum 10% total reduction
```

**per-phase**: Threshold reduction per failed voting phase (default: 0.0333 = 3.33%)  
**max-phases**: Maximum consecutive phases that accumulate penalty (default: 3)  
**max-reduction**: Maximum total threshold reduction (default: 0.10 = 10%)  
**Reset**: Penalty resets when a successful elimination occurs

### Abilities

#### Spark Abilities

Configure which secondary ability the Spark receives.

```yaml
spark:
  secondary-ability: random  # Options: random, hunter_vision, spark_swap, delusion
```

**Options**:
- `random` — Randomly select ability each round (default)
- `hunter_vision` — Always use Hunter Vision ability
- `spark_swap` — Always use Spark Swap ability
- `delusion` — Always use Delusion ability

**Ability Descriptions**:
- **Hunter Vision**: See all players with particles for 15 seconds
- **Spark Swap**: Invisible position swap with random player
- **Delusion**: Apply fake infection that medic can see but doesn't eliminate

#### Medic Abilities

Configure medic secondary abilities.

```yaml
medic:
  secondary-ability: random  # Options: random, healing_sight
```

**Options**:
- `random` — Random selection (currently only Healing Sight available)
- `healing_sight` — Healing Sight ability

**Note**: Currently only Healing Sight is implemented. More abilities planned for future releases.

### Cosmetics

Player appearance settings during games.

```yaml
cosmetics:
  use-steve-skins: true           # Force Steve skins for all players
  random-skins-enabled: false     # Enable random skin assignment
```

**use-steve-skins**:
- `true` — All players get Steve skin (recommended for consistency)
- `false` — Use random-skins-enabled setting

**random-skins-enabled**:
- `true` — Assign random skins from a preset pool
- `false` — Use Steve skins (or default skins if use-steve-skins is false)

**Recommendation**: Use `use-steve-skins: true` and `random-skins-enabled: false` for the most consistent experience.

## Example Configuration

Here's a complete example configuration:

```yaml
# Matchbox Configuration
session:
  spawn-locations:
    - world: m4tchb0x
      x: 100.5
      y: 64.0
      z: 200.5
      yaw: 0.0
      pitch: 0.0
    # Add more spawns...

player:
  min: 2
  max: 7

swipe:
  duration: 180

discussion:
  duration: 60
  seat-locations:
    1:
      world: m4tchb0x
      x: 110.5
      y: 64.0
      z: 210.5
    # Add seats 2-8...

voting:
  duration: 30
  threshold:
    at-20-players: 0.20
    at-7-players: 0.30
    at-3-players: 0.50
  penalty:
    per-phase: 0.0333
    max-phases: 3
    max-reduction: 0.10

spark:
  secondary-ability: random

medic:
  secondary-ability: random

cosmetics:
  use-steve-skins: true
  random-skins-enabled: false
```

## Configuration Tips

### Setting Locations In-Game

The easiest way to configure spawn and seat locations is using in-game commands:

1. **For Spawns**:
   - Stand at the desired spawn point
   - Run `/matchbox setspawn`
   - Location is automatically saved to config.yml

2. **For Seats**:
   - Stand at the desired seat location
   - Run `/matchbox setseat <number>` (1-8)
   - Location is automatically saved to config.yml

### Verifying Configuration

- Use `/matchbox listspawns` to view all spawn points
- Use `/matchbox listseatspawns` to view all seat locations
- Both commands flag any missing or unloaded worlds

### Applying Changes

Most configuration changes require a server restart to take effect. Location changes made via commands are applied immediately.

### World Names

**Important**: Make sure world names in your config match your actual world folder names. The default configuration uses `m4tchb0x` as the world name.

If your world has a different name, you'll need to either:
1. Reconfigure all locations using the `/matchbox setspawn` and `/matchbox setseat` commands, or
2. Manually edit `config.yml` to update the world names

## Common Configuration Scenarios

### Faster Games

For quick testing or casual play:

```yaml
swipe:
  duration: 120      # 2 minutes
discussion:
  duration: 45       # 45 seconds
voting:
  duration: 20       # 20 seconds
```

### Larger Games

For 10+ players:

```yaml
player:
  min: 5
  max: 15

swipe:
  duration: 240      # 4 minutes (more time needed)
discussion:
  duration: 90       # 90 seconds (more discussion needed)
voting:
  duration: 45       # 45 seconds (more time to vote)
```

### Easier Voting

To make eliminations more likely:

```yaml
voting:
  threshold:
    at-20-players: 0.15  # Lower threshold (15% instead of 20%)
    at-7-players: 0.25
    at-3-players: 0.40
  penalty:
    per-phase: 0.05      # Faster penalty accumulation
```

### Always Hunter Vision

To always give Spark the Hunter Vision ability:

```yaml
spark:
  secondary-ability: hunter_vision
```

## Troubleshooting

### Spawns/Seats Not Loading

**Problem**: "Not enough spawn locations" or "Not enough seat locations"

**Solution**:
1. Check world names with `/matchbox listspawns` or `/matchbox listseatspawns`
2. Ensure world folder exists and is loaded
3. Reconfigure locations if world names don't match

### Invalid Configuration Values

**Problem**: Server logs show configuration validation errors

**Solution**: 
1. Check that values are within valid ranges
2. Ensure proper YAML syntax (indentation matters!)
3. Delete `config.yml` and restart to regenerate defaults

### Changes Not Taking Effect

**Problem**: Configuration changes don't seem to work

**Solution**:
1. Restart the server (most config changes need restart)
2. Check server logs for errors
3. Verify YAML syntax is correct

## Need Help?

- See [Commands](Commands) for command reference
- Check [FAQ](FAQ) for common issues
- Join our [Discord](https://discord.gg/BTDP3APfq8) for support
