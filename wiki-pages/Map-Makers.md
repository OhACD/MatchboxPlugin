# Map Makers

This guide is for builders who want to ship Matchbox maps as true drop-and-play world folders.

## Goal

Bundle your map with a world-local Matchbox config so server owners can copy one world folder and run games immediately.

The world-local config file is:

- `matchbox-map.yml` (inside your world folder)

## Recommended Workflow

1. Load your target world.
2. Run `/mb setup init <map-id> [display name]`.
3. Add spawn locations with `/mb setup setspawn`.
4. Add seat locations with `/mb setup setseat <seat-number>`.
5. Define seat order with `/mb setup seatspawns set 1,2,3,4,5,6,7`.
6. Run `/mb setup validate` until it passes.
7. Run `/mb setup info` and confirm metadata.
8. Package the world folder including `matchbox-map.yml`.

## Setup Commands

### Metadata and Validation

- `/mb setup init <map-id> [display name]`
- `/mb setup info`
- `/mb setup validate`
- `/mb setup importlegacy [overwrite]`

### Spawns and Seats

- `/mb setup setspawn`
- `/mb setup setseat <seat-number>`
- `/mb setup listspawns`
- `/mb setup listseats`
- `/mb setup removespawn <index>`
- `/mb setup removeseat <seat-number>`
- `/mb setup clearspawns [confirm]`
- `/mb setup clearseats [confirm]`

### Seat Order

- `/mb setup seatspawns list`
- `/mb setup seatspawns add <seat-number>`
- `/mb setup seatspawns remove <seat-number>`
- `/mb setup seatspawns set <comma-separated-seats>`

## Validation Checklist

Before release:

- `map.id`, `map.display-name`, and `map.creator` are set
- `map.schema-version` and `map.plugin-version` are set
- At least one `session.spawn-locations` entry exists
- Seat locations exist for all configured seat spawns
- `/mb setup validate` returns no issues

## Legacy Migration

If map data currently lives in `plugins/Matchbox/config.yml`:

- `/mb setup importlegacy`
- `/mb setup importlegacy overwrite` (force replace world-local values)
