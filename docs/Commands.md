# Commands Reference

This page covers all Matchbox commands.

## Command Aliases

All commands can be used as:

- `/matchbox`
- `/mb`
- `/mbox`

## Player Commands

### `/matchbox join <session>`
Join an active game session.

### `/matchbox leave <session>`
Leave your current game session.

### `/matchbox list`
List active game sessions.

### `/matchbox nick [name|random|reset]`
Manage your in-game nick.

## Admin Commands

### Session Management

- `/matchbox start <session>`: Create a session
- `/matchbox begin <session>`: Begin a session
- `/matchbox stop <session>`: Stop and remove a session
- `/matchbox remove <session>`: Deprecated alias for stop

### Setup Tools (Recommended)

Use setup tools for map authoring and world-local map configs.

#### Metadata and Validation

- `/mb setup init <map-id> [display name]`
- `/mb setup info`
- `/mb setup validate`
- `/mb setup importlegacy [overwrite]`

#### Spawn and Seat Locations

- `/mb setup setspawn`
- `/mb setup setseat <seat-number>`
- `/mb setup listspawns`
- `/mb setup listseats`
- `/mb setup removespawn <index>`
- `/mb setup removeseat <seat-number>`
- `/mb setup clearspawns [confirm]`
- `/mb setup clearseats [confirm]`

#### Discussion Seat Order

- `/mb setup seatspawns list`
- `/mb setup seatspawns add <seat-number>`
- `/mb setup seatspawns remove <seat-number>`
- `/mb setup seatspawns set <comma-separated-seats>`

### Other Admin Commands

- `/matchbox setdiscussion <session>`: Set session discussion location
- `/matchbox skip`: Skip current phase
- `/matchbox cleanup`: Emergency nametag restore

### Debug Commands

- `/matchbox debug`: Print debug state for your session
- `/matchbox debugstart <session>`: Force-begin for testing

## Permissions

- `matchbox.play`: Player command access
- `matchbox.admin`: Admin and setup command access
- `matchbox.debug`: Debug command access

## Common Workflows

### Create a New Drop-and-Play Map Package

1. Join the map world.
2. Run `/mb setup init <map-id> [display name]`.
3. Add spawns with `/mb setup setspawn`.
4. Add seats with `/mb setup setseat <number>`.
5. Set seat order with `/mb setup seatspawns set ...`.
6. Run `/mb setup validate`.
7. Ship the world folder with `matchbox-map.yml`.

### Migrate Existing Legacy Config into World-Local

1. Enter target world.
2. Run `/mb setup importlegacy`.
3. If needed, force replace with `/mb setup importlegacy overwrite`.
4. Verify with `/mb setup validate` and `/mb setup info`.

## Notes

- Map geometry editing is provided through `/mb setup ...` subcommands.
- Most location edits apply immediately.
- Tab completion is available for setup subcommands and confirmation options.
