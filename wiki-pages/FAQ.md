# FAQ & Troubleshooting

## Installation & Setup

### Q: What are the requirements to run Matchbox?
**A:** You need:
- Paper server 1.21.11 or higher
- Java 21 or higher
- ProtocolLib 5.4.0 or higher
- The M4tchb0x map (optional but recommended)

### Q: Where do I download the required files?
**A:** 
- Plugin: [Modrinth](https://modrinth.com/plugin/matchboxplugin)
- ProtocolLib: [SpigotMC](https://www.spigotmc.org/resources/protocollib.1997/)
- M4tchb0x Map: [Planet Minecraft](https://www.planetminecraft.com/project/m4tchb0x-maps/)

## Configuration Issues

### Q: Spawns/seats not found after restart?
**A:** Check world folder names and make sure the target world has `matchbox-map.yml`. Use `/mb setup info` and `/mb setup validate` in that world to confirm map configuration and missing requirements.

### Q: How do I configure spawn and seat locations?
**A:** Use in-game commands:
- Run `/mb setup init <map-id> [display name]` once per map world
- Stand at each spawn and run `/mb setup setspawn`
- For seats, run `/mb setup setseat <number>` where number is 1-8
- Set seat order with `/mb setup seatspawns set 1,2,3,4,5,6,7`
- Validate with `/mb setup validate`
- Map geometry is saved to `<world>/matchbox-map.yml`

### Q: Can I customize phase durations?
**A:** Yes! Edit `plugins/Matchbox/config.yml` and modify:
- `swipe.duration` (default: 180 seconds)
- `discussion.duration` (default: 60 seconds)
- `voting.duration` (default: 30 seconds)

## Gameplay Issues

### Q: Player skins are inconsistent or wrong?
**A:** Check these config settings in `config.yml`:
- `cosmetics.use-steve-skins` — Set to `true` for consistent Steve skins (recommended)
- `cosmetics.random-skins-enabled` — Set to `false` to disable random skins

### Q: How many players do I need to start a game?
**A:** The default minimum is 2 players, but the game is best with 5-9 players. You can:
- Use `/matchbox begin <session>` to start normally
- Use `/matchbox debugstart <session>` to force-start with fewer players for testing

### Q: What happens if a player disconnects during a game?
**A:** The player is removed from the session. Their role is eliminated and the game continues with remaining players.

## Permission Issues

### Q: Permissions not working?
**A:** Verify that:
1. You have a permission plugin installed (LuckPerms, etc.)
2. The permission nodes are correctly set
3. Your server has reloaded permissions after changes

Admin permissions:
- `matchbox.admin` — Access to admin commands
- `matchbox.debug` — Access to debug commands

Player permissions:
- `matchbox.play` — Join and play games

## Technical Issues

### Q: I'm getting errors about ProtocolLib
**A:** Make sure:
1. ProtocolLib is installed in your `plugins/` folder
2. You're using ProtocolLib 5.4.0 or higher
3. ProtocolLib loaded successfully (check server logs)

### Q: The plugin isn't loading
**A:** Check:
1. You're running Paper 1.21.11+ (not Spigot or Bukkit)
2. You have Java 21 or higher
3. Check server logs for error messages

### Q: How do I report a bug?
**A:** The best way is to:
1. Join our [Discord](https://discord.gg/BTDP3APfq8) for quick help
2. Open a GitHub issue with:
   - Minecraft and plugin version
   - Server type and Java version
   - Steps to reproduce
   - Error logs if applicable

## Still Need Help?

Join our Discord server for live support: https://discord.gg/BTDP3APfq8

You can also check the other wiki pages:
- [Getting Started](Getting-Started) for setup instructions
- [Configuration](Configuration) for detailed config options
- [Commands](Commands) for command reference
