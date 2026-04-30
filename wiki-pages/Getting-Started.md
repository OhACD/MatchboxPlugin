# Getting Started

This guide will help you install and set up Matchbox on your server.

## Requirements

Before you begin, make sure you have:

- **Minecraft Server**: Paper 1.21.11 or higher ([Download Paper](https://papermc.io/downloads))
- **Java**: Version 21 or higher
- **Dependencies**: ProtocolLib 5.4.0 or higher ([Download](https://www.spigotmc.org/resources/protocollib.1997/))
- **Map** (Optional): M4tchb0x official map ([Download](https://www.planetminecraft.com/project/m4tchb0x-maps/))

## Installation

### Step 1: Download Required Files

1. Download `Matchbox.jar` from [Modrinth](https://modrinth.com/plugin/matchboxplugin)
2. Download `ProtocolLib-5.4.0.jar` from [SpigotMC](https://www.spigotmc.org/resources/protocollib.1997/)
3. (Optional) Download the M4tchb0x map from [Planet Minecraft](https://www.planetminecraft.com/project/m4tchb0x-maps/)

### Step 2: Install the Plugin

1. Place both `Matchbox.jar` and `ProtocolLib-5.4.0.jar` in your server's `plugins/` folder
2. If using the M4tchb0x map, place it in your server's root directory
3. Start or restart your server

### Step 3: First-Time Setup

On first run, Matchbox will:
- Create `plugins/Matchbox/config.yml` with default settings
- Pre-configure spawn and seat locations (if using the M4tchb0x map)
- Enable the plugin and register commands

The plugin ships with a **complete default configuration** for the M4tchb0x map, so you can start playing immediately!

## Quick Start Guide

### Creating Your First Game

1. **Create a session**:
   ```
   /matchbox start mysession
   ```

2. **Have players join**:
   ```
   /matchbox join mysession
   ```

3. **Begin the game**:
   ```
   /matchbox begin mysession
   ```

### Configuring Locations (Optional)

If you're using a custom map or want to adjust locations:

#### Initialize Map Config
1. Join the map world
2. Run `/mb setup init <map-id> [display name]`

#### Set Spawn and Seat Locations
1. Stand at each desired spawn and run `/mb setup setspawn`
2. Stand at each seat and run `/mb setup setseat <seat-number>`
3. Set seat order with `/mb setup seatspawns set 1,2,3,4,5,6,7`

#### Validate
- Run `/mb setup validate` to verify map readiness
- Run `/mb setup info` to verify metadata and config path

All map geometry is saved to `<world>/matchbox-map.yml`.

## Basic Configuration

The plugin comes with sensible defaults, but you can customize:

### Phase Durations
Edit `plugins/Matchbox/config.yml`:
```yaml
swipe:
  duration: 180  # 3 minutes
discussion:
  duration: 60   # 1 minute
voting:
  duration: 30   # 30 seconds
```

### Player Limits
```yaml
player:
  min: 2  # Minimum players
  max: 7  # Maximum players (supports up to 20)
```

### Cosmetics
```yaml
cosmetics:
  use-steve-skins: true      # Use Steve skins (recommended)
  random-skins-enabled: false # Random skins for variety
```

For more configuration options, see the [Configuration](Configuration) page.

## Testing Your Setup

### Debug Start
To test with fewer players:
```
/matchbox debugstart mysession
```
This bypasses the minimum player requirement.

### Check Session Status
```
/matchbox debug
```
Shows current session information and player states.

## Common Issues

### "Not enough spawn locations"
- Add more spawn points with `/mb setup setspawn`
- You need at least as many spawns as your max player count

### "Not enough seat locations"
- Add seats with `/mb setup setseat <number>`
- You need 8 seats for discussion phase

### World not found after restart
- Check that your world folder contains `matchbox-map.yml`
- Use `/mb setup info` and `/mb setup validate` in that world

## Next Steps

Now that you have Matchbox installed:

1. Read the [Commands](Commands) page for a full command reference
2. Check out [Configuration](Configuration) for advanced options
3. Review [FAQ](FAQ) for troubleshooting help
4. Join our [Discord](https://discord.gg/BTDP3APfq8) for support and updates

## Need Help?

- **Discord**: Join https://discord.gg/BTDP3APfq8 for live support
- **GitHub Issues**: Report bugs at the [GitHub repository](https://github.com/OhACD/MatchboxPlugin/issues)
- **FAQ**: Check the [FAQ page](FAQ) for common solutions

---

**Ready to play?** Use `/matchbox start <session>` to create your first game!
