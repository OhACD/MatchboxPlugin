# Map Maker Guide

This guide is for builders who want to ship Matchbox maps that are truly drop-and-play.

## Goal

Bundle your map with a world-local Matchbox config so server owners can copy one world folder and run games immediately.

The world-local config file is:

- `matchbox-map.yml` (inside your world folder)

## Core Idea

Matchbox now supports two config sources:

- World-local map config (`<world>/matchbox-map.yml`) for map geometry and map metadata
- Global plugin config (`plugins/Matchbox/config.yml`) for server-level tuning and legacy fallback

For map creators, always target world-local config.

## Recommended Workflow

1. Load your target world.
2. Run `/mb setup init <map-id> [display name]`.
3. Add all spawn locations with `/mb setup setspawn`.
4. Add all seat locations with `/mb setup setseat <seat-number>`.
5. Define discussion seat order with `/mb setup seatspawns set <comma-separated-seats>`.
6. Run `/mb setup validate` until it passes.
7. Run `/mb setup info` and confirm metadata.
8. Package and distribute the world folder including `matchbox-map.yml`.

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

### Discussion Seat Order

- `/mb setup seatspawns list`
- `/mb setup seatspawns add <seat-number>`
- `/mb setup seatspawns remove <seat-number>`
- `/mb setup seatspawns set <comma-separated-seats>`

Example:

`/mb setup seatspawns set 1,2,3,4,5,6,7`

## Validation Checklist

Before shipping your map, make sure all of these are true:

- `map.id`, `map.display-name`, and `map.creator` are set
- `map.schema-version` and `map.plugin-version` are set
- At least one `session.spawn-locations` entry exists
- Seat locations exist for your configured `discussion.seat-spawns`
- `/mb setup validate` returns no issues

## Legacy Migration

If your map data currently lives in `plugins/Matchbox/config.yml`, migrate it into your current world with:

- `/mb setup importlegacy`
- `/mb setup importlegacy overwrite` (force replace world-local values)

## Packaging Tips

- Include the complete world folder in your release archive
- Do not remove `matchbox-map.yml`
- Keep map IDs lowercase and stable once published
- Include a short README with recommended player counts and version tested

## Compatibility Notes

Legacy location commands (`/mb setspawn`, `/mb setseat`, etc.) still work and now target the executing player's current world map config.

For map creation and publishing workflows, prefer `/mb setup ...` commands.
