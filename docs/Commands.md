# Commands Reference

This page provides a complete reference for all Matchbox commands.

## Command Aliases

All commands can be used with the following aliases:
- `/matchbox` (full command)
- `/mb` (short alias)
- `/mbox` (alternate alias)

## Player Commands

Commands available to all players with `matchbox.play` permission.

### `/matchbox join <session>`
Join an active game session.

**Usage**: `/matchbox join mysession`  
**Permission**: `matchbox.play`  
**Aliases**: `/mb join`, `/mbox join`

### `/matchbox leave`
Leave your current game session.

**Usage**: `/matchbox leave`  
**Permission**: `matchbox.play`  
**Aliases**: `/mb leave`, `/mbox leave`

### `/matchbox list`
List all active game sessions on the server.

**Usage**: `/matchbox list`  
**Permission**: `matchbox.play`  
**Aliases**: `/mb list`, `/mbox list`

## Admin Commands

Commands requiring admin permissions.

### Session Management

#### `/matchbox start <session>`
Create a new game session with the specified name.

**Usage**: `/matchbox start arena1`  
**Permission**: `matchbox.admin`  
**Notes**: Creates the session but doesn't start the game. Players must join first.

#### `/matchbox begin <session>`
Start the game for a session with all joined players.

**Usage**: `/matchbox begin arena1`  
**Permission**: `matchbox.admin`  
**Requirements**: 
- Minimum configured players must have joined
- Sufficient spawn and seat locations must be configured

#### `/matchbox stop <session>`
Stop and remove an active game session.

**Usage**: `/matchbox stop arena1`  
**Permission**: `matchbox.admin`  
**Notes**: Restores all player states and cleans up the session

### Location Management

#### `/matchbox setspawn`
Save your current location as a spawn point in the config.

**Usage**: Stand at desired location, then `/matchbox setspawn`  
**Permission**: `matchbox.admin`  
**Notes**: Automatically saved to `config.yml`

#### `/matchbox setseat <number>`
Save your current location as a discussion seat.

**Usage**: Stand at desired location, then `/matchbox setseat 1`  
**Permission**: `matchbox.admin`  
**Parameters**: `<number>` - Seat number (1-8)  
**Notes**: Automatically saved to `config.yml`

#### `/matchbox setdiscussion <session>`
Set the discussion area location for a specific session.

**Usage**: Stand at desired location, then `/matchbox setdiscussion arena1`  
**Permission**: `matchbox.admin`  
**Notes**: Session-specific override (not saved to config)

#### `/matchbox listspawns`
List all configured spawn locations.

**Usage**: `/matchbox listspawns`  
**Permission**: `matchbox.admin`  
**Notes**: Shows world name, coordinates, and flags missing worlds

#### `/matchbox listseatspawns`
List all configured seat locations.

**Usage**: `/matchbox listseatspawns`  
**Permission**: `matchbox.admin`  
**Notes**: Shows seat numbers, world names, coordinates, and flags missing worlds

#### `/matchbox removespawn <index>`
Remove a spawn location from the config.

**Usage**: `/matchbox removespawn 1`  
**Permission**: `matchbox.admin`  
**Parameters**: `<index>` - Index from `listspawns` command

#### `/matchbox removeseat <number>`
Remove a seat location from the config.

**Usage**: `/matchbox removeseat 1`  
**Permission**: `matchbox.admin`  
**Parameters**: `<number>` - Seat number (1-8)

#### `/matchbox clearspawns`
Clear all spawn locations from the config.

**Usage**: `/matchbox clearspawns` (then confirm with same command)  
**Permission**: `matchbox.admin`  
**Notes**: Requires confirmation to prevent accidental deletion

#### `/matchbox clearseats`
Clear all seat locations from the config.

**Usage**: `/matchbox clearseats` (then confirm with same command)  
**Permission**: `matchbox.admin`  
**Notes**: Requires confirmation to prevent accidental deletion

### Game Control

#### `/matchbox skip`
Skip the current game phase.

**Usage**: `/matchbox skip`  
**Permission**: `matchbox.admin`  
**Notes**: Immediately advances to the next phase

### Debug Commands

#### `/matchbox debug`
Display debug information about the current session state.

**Usage**: `/matchbox debug`  
**Permission**: `matchbox.debug`  
**Shows**:
- Current phase and round number
- Player states and roles
- Active abilities
- Infection status

#### `/matchbox debugstart <session>`
Force-start a game session, bypassing minimum player requirements.

**Usage**: `/matchbox debugstart arena1`  
**Permission**: `matchbox.debug`  
**Notes**: 
- Useful for testing with fewer players
- Still enforces spawn and seat location requirements
- Should not be used in production

## Permission Nodes

### Player Permissions
- `matchbox.play` — Join and play games (default: true)

### Admin Permissions
- `matchbox.admin` — Access to all admin commands
- `matchbox.debug` — Access to debug commands

### Permission Groups
Most servers will want to set:
```yaml
default-group:
  permissions:
    - matchbox.play

admin-group:
  permissions:
    - matchbox.admin
    - matchbox.debug
```

## Common Command Workflows

### Setting Up a New Game Arena

1. Go to first spawn location: `/matchbox setspawn`
2. Repeat for all spawn points (at least as many as max players)
3. Go to first seat: `/matchbox setseat 1`
4. Repeat for seats 2-8
5. Create session: `/matchbox start arena1`
6. Have players join: (players use `/matchbox join arena1`)
7. Start game: `/matchbox begin arena1`

### Quick Testing

1. Create session: `/matchbox start test`
2. Join session: `/matchbox join test`
3. Force start: `/matchbox debugstart test`
4. Skip phases as needed: `/matchbox skip`

### Managing Multiple Sessions

1. List active sessions: `/matchbox list`
2. Stop specific session: `/matchbox stop arena1`
3. Debug specific session: `/matchbox debug` (while in that session)

## Tips

- Use tab completion to see available commands and parameters
- Most commands provide feedback messages to confirm success/failure
- Check server console for detailed error messages if commands fail
- Location commands (setspawn, setseat) use your exact position and rotation

## Need More Help?

- See [Configuration](Configuration) for config file options
- Check [FAQ](FAQ) for troubleshooting
- Join our [Discord](https://discord.gg/BTDP3APfq8) for support
