package com.ohacd.matchbox.command;

import com.ohacd.matchbox.Matchbox;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.nick.NickManager;
import com.ohacd.matchbox.game.nick.RandomNickGenerator;
import com.ohacd.matchbox.game.session.GameSession;
import com.ohacd.matchbox.game.session.SessionManager;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.Managers.NameTagManager;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Handles all matchbox commands.
 */
public class MatchboxCommand implements CommandExecutor, TabCompleter {
    private final SessionManager sessionManager;
    private final GameManager gameManager;
    private final NickManager nickManager;
    // Track pending confirmations for destructive commands
    private final Map<UUID, String> pendingConfirmations = new ConcurrentHashMap<>();

    public MatchboxCommand(Matchbox plugin, SessionManager sessionManager, GameManager gameManager, NickManager nickManager) {
        if (sessionManager == null) {
            throw new IllegalArgumentException("SessionManager cannot be null");
        }
        if (gameManager == null) {
            throw new IllegalArgumentException("GameManager cannot be null");
        }
        this.sessionManager = sessionManager;
        this.gameManager = gameManager;
        this.nickManager = nickManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "setup":
                return handleSetup(sender, args);
            case "start":
                return handleStart(sender, args);
            case "begin":
                return handleBegin(sender, args);
            case "debugstart":
                return handleDebugBegin(sender, args);
            case "stop":
                return handleStop(sender, args);
            case "join":
                return handleJoin(sender, args);
            case "leave":
                return handleLeave(sender, args);
            case "setdiscussion":
                return handleSetDiscussion(sender, args);
            case "list":
                return handleList(sender);
            case "remove":
                return handleRemove(sender, args);
            case "cleanup":
                return handleCleanup(sender);
            case "debug":
                return handleDebug(sender);
            case "skip":
                return handleSkip(sender);
            case "nick":
                return handleNick(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleSetup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matchbox.admin")) {
            sender.sendMessage("§cYou don't have permission to use setup tools.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cSetup tools can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        World world = player.getWorld();
        com.ohacd.matchbox.game.config.ConfigManager configManager = gameManager.getConfigManager();

        if (args.length < 2) {
            sendSetupHelp(sender);
            return true;
        }

        String setupCommand = args[1].toLowerCase();
        switch (setupCommand) {
            case "help":
                sendSetupHelp(sender);
                return true;
            case "init": {
                String mapId = args.length >= 3 ? args[2] : world.getName();
                String displayName = args.length >= 4 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : world.getName();
                configManager.initializeWorldMapMetadata(world, mapId, displayName, player.getName());
                sender.sendMessage("§aInitialized map config for world '" + world.getName() + "'.");
                sender.sendMessage("§7Map ID: §e" + mapId.toLowerCase() + "§7, Display Name: §e" + displayName);
                sender.sendMessage("§7Config Path: §e" + configManager.getWorldMapConfigPath(world));
                return true;
            }
            case "info": {
                Map<String, String> metadata = configManager.getWorldMapMetadata(world);
                if (metadata.isEmpty()) {
                    sender.sendMessage("§cNo world map config found for '" + world.getName() + "'. Use /mb setup init first.");
                    return true;
                }
                sender.sendMessage("§6=== Map Setup Info (" + world.getName() + ") ===");
                sender.sendMessage("§7Map ID: §e" + metadata.getOrDefault("id", ""));
                sender.sendMessage("§7Display Name: §e" + metadata.getOrDefault("display-name", ""));
                sender.sendMessage("§7Creator: §e" + metadata.getOrDefault("creator", ""));
                sender.sendMessage("§7Schema Version: §e" + metadata.getOrDefault("schema-version", ""));
                sender.sendMessage("§7Plugin Version: §e" + metadata.getOrDefault("plugin-version", ""));
                sender.sendMessage("§7Config Path: §e" + configManager.getWorldMapConfigPath(world));
                return true;
            }
            case "validate": {
                List<String> issues = configManager.validateWorldMapConfig(world);
                if (issues.isEmpty()) {
                    sender.sendMessage("§aMap config validation passed for world '" + world.getName() + "'.");
                } else {
                    sender.sendMessage("§cMap config validation failed for world '" + world.getName() + "':");
                    for (String issue : issues) {
                        sender.sendMessage("§7- §c" + issue);
                    }
                }
                return true;
            }
            case "importlegacy": {
                boolean overwrite = args.length >= 3 && args[2].equalsIgnoreCase("overwrite");
                boolean changed = configManager.importLegacyGlobalConfigToWorld(world, overwrite);
                if (changed) {
                    sender.sendMessage("§aImported legacy global config into world map config for '" + world.getName() + "'.");
                    sender.sendMessage("§7Config Path: §e" + configManager.getWorldMapConfigPath(world));
                    if (!overwrite) {
                        sender.sendMessage("§7Tip: use §e/mb setup importlegacy overwrite §7to force replace existing world-local values.");
                    }
                } else {
                    sender.sendMessage("§eNo changes were made during import. World map config already had values.");
                }
                return true;
            }
            case "setspawn":
                return handleSetSpawn(sender, new String[]{"setspawn"});
            case "setseat": {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /mb setup setseat <seat-number>");
                    return true;
                }
                return handleSetSeat(sender, new String[]{"setseat", args[2]});
            }
            case "listspawns":
                return handleListSpawns(sender);
            case "listseats":
            case "listseatspawns":
                return handleListSeatSpawns(sender);
            case "removespawn": {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /mb setup removespawn <index>");
                    return true;
                }
                return handleRemoveSpawn(sender, new String[]{"removespawn", args[2]});
            }
            case "removeseat": {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /mb setup removeseat <seat-number>");
                    return true;
                }
                return handleRemoveSeat(sender, new String[]{"removeseat", args[2]});
            }
            case "clearspawns": {
                String confirm = args.length >= 3 ? args[2] : "";
                return handleClearSpawns(sender, new String[]{"clearspawns", confirm});
            }
            case "clearseats": {
                String confirm = args.length >= 3 ? args[2] : "";
                return handleClearSeats(sender, new String[]{"clearseats", confirm});
            }
            case "seatspawns":
                return handleSetupSeatSpawns(sender, args, world, configManager);
            default:
                sendSetupHelp(sender);
                return true;
        }
    }

    private boolean handleSetupSeatSpawns(CommandSender sender, String[] args, World world, com.ohacd.matchbox.game.config.ConfigManager configManager) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /mb setup seatspawns <list|add|remove|set>");
            return true;
        }

        String action = args[2].toLowerCase();
        switch (action) {
            case "list": {
                List<Integer> seats = configManager.getDiscussionSeatSpawns(world);
                sender.sendMessage("§6Seat Spawn Order (" + world.getName() + "): §e" + seats);
                return true;
            }
            case "add": {
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /mb setup seatspawns add <seat-number>");
                    return true;
                }
                Integer seat = parsePositiveInt(args[3]);
                if (seat == null) {
                    sender.sendMessage("§cInvalid seat number: " + args[3]);
                    return true;
                }
                configManager.addDiscussionSeatSpawn(world, seat);
                sender.sendMessage("§aAdded seat " + seat + " to discussion.seat-spawns for world '" + world.getName() + "'.");
                return true;
            }
            case "remove": {
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /mb setup seatspawns remove <seat-number>");
                    return true;
                }
                Integer seat = parsePositiveInt(args[3]);
                if (seat == null) {
                    sender.sendMessage("§cInvalid seat number: " + args[3]);
                    return true;
                }
                configManager.removeDiscussionSeatSpawn(world, seat);
                sender.sendMessage("§aRemoved seat " + seat + " from discussion.seat-spawns for world '" + world.getName() + "'.");
                return true;
            }
            case "set": {
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /mb setup seatspawns set <comma-separated-seat-numbers>");
                    sender.sendMessage("§7Example: /mb setup seatspawns set 1,2,3,4,5,6,7");
                    return true;
                }

                String joined = String.join("", Arrays.copyOfRange(args, 3, args.length));
                String[] rawSeats = joined.split(",");
                List<Integer> seats = new ArrayList<>();
                for (String raw : rawSeats) {
                    Integer seat = parsePositiveInt(raw.trim());
                    if (seat == null) {
                        sender.sendMessage("§cInvalid seat number in list: " + raw.trim());
                        return true;
                    }
                    seats.add(seat);
                }

                configManager.setDiscussionSeatSpawns(world, seats);
                sender.sendMessage("§aUpdated discussion.seat-spawns for world '" + world.getName() + "' to §e" + seats + "§a.");
                return true;
            }
            default:
                sender.sendMessage("§cUnknown seatspawns action. Use list, add, remove, or set.");
                return true;
        }
    }

    private Integer parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void sendSetupHelp(CommandSender sender) {
        sender.sendMessage("§6=== Matchbox Setup Tools ===");
        sender.sendMessage("§e/mb setup init <map-id> [display name] §7- Initialize baked map config in current world");
        sender.sendMessage("§e/mb setup info §7- Show map metadata for current world");
        sender.sendMessage("§e/mb setup validate §7- Validate map config for drop-and-play readiness");
        sender.sendMessage("§e/mb setup importlegacy [overwrite] §7- Import legacy global seat/spawn config into this world");
        sender.sendMessage("§e/mb setup setspawn §7- Add spawn point to current world map config");
        sender.sendMessage("§e/mb setup setseat <seat> §7- Add/update seat location in current world map config");
        sender.sendMessage("§e/mb setup listspawns §7- List spawn points in current world map config");
        sender.sendMessage("§e/mb setup listseats §7- List seat locations in current world map config");
        sender.sendMessage("§e/mb setup removespawn <index> §7- Remove spawn point by index");
        sender.sendMessage("§e/mb setup removeseat <seat> §7- Remove seat location by seat number");
        sender.sendMessage("§e/mb setup seatspawns <list|add|remove|set> ... §7- Manage discussion seat order");
        sender.sendMessage("§e/mb setup clearspawns [confirm] §7- Clear all spawns in current world map config");
        sender.sendMessage("§e/mb setup clearseats [confirm] §7- Clear all seats in current world map config");
    }

    private boolean handleSkip(CommandSender sender) {
        if (!sender.hasPermission("matchbox.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        
        // Find which session the player is in
        SessionGameContext context = gameManager.getContextForPlayer(player.getUniqueId());
        if (context == null) {
            sender.sendMessage("§cYou are not in an active game.");
            return true;
        }
        
        String sessionName = context.getSessionName();
        GamePhase currentPhase = context.getPhaseManager().getCurrentPhase();

        if (currentPhase == GamePhase.WAITING) {
            sender.sendMessage("§cNo active game to skip phases in.");
            return true;
        }

        sender.sendMessage("§eForce-skipping current phase: " + currentPhase + " in session: " + sessionName);

        // Force end current phase by cancelling timers and calling callbacks
        if (currentPhase == GamePhase.SWIPE) {
            gameManager.endSwipePhase(sessionName);
        } else if (currentPhase == GamePhase.DISCUSSION) {
            // End discussion phase and move to voting
            gameManager.endDiscussionPhase(sessionName);
        } else if (currentPhase == GamePhase.VOTING) {
            // End voting phase and move to next round or end game
            gameManager.endVotingPhase(sessionName);
        } else {
            sender.sendMessage("§cCannot skip phase: " + currentPhase);
        }

        return true;
    }

    private boolean handleDebug(CommandSender sender) {
        if (!sender.hasPermission("matchbox.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        sender.sendMessage("§6=== Matchbox Debug Info ===");
        
        // Show active game sessions
        Set<String> activeSessionNames = gameManager.getActiveSessionNames();
        sender.sendMessage("§eActive Game Sessions: " + activeSessionNames.size());
        
        for (String sessionName : activeSessionNames) {
            SessionGameContext context = gameManager.getContext(sessionName);
            if (context != null) {
                sender.sendMessage("  §7- " + sessionName + ":");
                sender.sendMessage("    §7Game State: " + context.getGameState().getDebugInfo());
                sender.sendMessage("    §7State Valid: " + (context.getGameState().validateState() ? "§aYes" : "§cNo"));
                sender.sendMessage("    §7Phase Info: " + context.getPhaseManager().getDebugInfo());
            }
        }

        // List all sessions (including inactive)
        sender.sendMessage("§eAll Sessions: " + sessionManager.getAllSessionNames().size());
        for (String sessionName : sessionManager.getAllSessionNames()) {
            GameSession session = sessionManager.getSession(sessionName);
            sender.sendMessage("  §7- " + sessionName + " (Active: " + session.isActive() + ", Players: " + session.getPlayerCount() + ")");
        }

        return true;
    }
    private boolean handleCleanup(CommandSender sender) {
        if (!sender.hasPermission("matchbox.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        sender.sendMessage("§eRestoring all nametags and cleaning up teams...");
        NameTagManager.restoreAllNameTags();
        sender.sendMessage("§aCleanup complete! All nametags restored.");
        return true;
    }

    private boolean handleStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matchbox.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /matchbox stop <name>");
            return true;
        }

        String sessionName = args[1];

        // Check if session exists
        if (!sessionManager.sessionExists(sessionName)) {
            sender.sendMessage("§cSession '" + sessionName + "' doesn't exist.");
            return true;
        }

        GameSession session = sessionManager.getSession(sessionName);
        if (session == null) {
            sender.sendMessage("§cSession '" + sessionName + "' doesn't exist.");
            return true;
        }

        // Get players list before removing session
        List<Player> playersInSession = new ArrayList<>(session.getPlayers());

        // If game is active, end it first
        if (session.isActive()) {
            sender.sendMessage("§eEnding active game for session '" + sessionName + "'...");
            gameManager.endGame(sessionName);
        }

        // Remove the session
        sessionManager.removeSession(sessionName);
        sender.sendMessage("§aSession '" + sessionName + "' has been stopped and removed.");

        // Notify all players who were in the session
        for (Player player : playersInSession) {
            if (player != null && player.isOnline() && !player.equals(sender)) {
                try {
                    player.sendMessage("§cSession '" + sessionName + "' has been stopped by " + sender.getName() + ".");
                } catch (Exception e) {
                    // Ignore errors sending messages
                }
            }
        }

        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matchbox.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /matchbox remove <name>");
            return true;
        }

        String sessionName = args[1];

        // If session doesn't exist
        if (!sessionManager.sessionExists(sessionName)) {
            sender.sendMessage("§cSession doesn't exist.");
            return true;
        }
        // Remove session
        sessionManager.removeSession(sessionName);
        sender.sendMessage("§cSession removed.");
        return true;
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matchbox.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /matchbox start <name>");
            return true;
        }

        String sessionName = args[1];

        // Check if session already exists
        if (sessionManager.sessionExists(sessionName)) {
            sender.sendMessage("§cA session with that name already exists!");
            return true;
        }

        // Create session
        GameSession session = sessionManager.createSession(sessionName);
        if (session == null) {
            sender.sendMessage("§cFailed to create session!");
            return true;
        }

        // Add creator to session
        session.addPlayer((Player) sender);
        sender.sendMessage("§aSession '" + sessionName + "' created! Use /matchbox join " + sessionName + " to join.");
        
        // Broadcast to all players that a new session was created
        String creatorName = sender.getName();
        Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize("§6§l[Matchbox] §e" + creatorName + " §7created a new game session: §a" + sessionName));
        Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize("§7Join with: §e/matchbox join " + sessionName));
        
        return true;
    }

    private boolean handleBegin(CommandSender sender, String[] args) {
        return handleBeginInternal(sender, args, false);
    }

    private boolean handleDebugBegin(CommandSender sender, String[] args) {
        return handleBeginInternal(sender, args, true);
    }

    private boolean handleBeginInternal(CommandSender sender, String[] args, boolean allowSinglePlayer) {
        if (!sender.hasPermission("matchbox.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /matchbox " + (allowSinglePlayer ? "debugstart" : "begin") + " <name>");
            return true;
        }

        String sessionName = args[1];

        GameSession session = sessionManager.getSession(sessionName);
        if (session == null) {
            sender.sendMessage("§cSession '" + sessionName + "' does not exist!");
            return true;
        }

        if (session.isActive()) {
            sender.sendMessage("§cThis session is already active!");
            return true;
        }

        // Parallel sessions are supported - each session can run independently

        List<Player> players = session.getPlayers();
        if (players.isEmpty()) {
            sender.sendMessage("§cNo players in this session to start a game.");
            return true;
        }
        
        // Get config values
        com.ohacd.matchbox.game.config.ConfigManager configManager = gameManager.getConfigManager();
        int minPlayers = configManager.getMinPlayers();
        int maxPlayers = configManager.getMaxPlayers();
        int minSpawnLocations = configManager.getMinSpawnLocations();
        
        if (players.size() < minPlayers) {
            if (!allowSinglePlayer) {
                sender.sendMessage("§cYou need at least " + minPlayers + " players to start a game!");
                return true;
            }
            sender.sendMessage("§eDebug override: starting with " + players.size() + " player(s). Normal minimum is " + minPlayers + ".");
        }
        
        // Check if session has too many players
        if (players.size() > maxPlayers) {
            sender.sendMessage("§cThis session has too many players! Maximum " + maxPlayers + " players allowed.");
            return true;
        }

        // Load locations from world-local map config first (fallback to global config)
        com.ohacd.matchbox.game.config.ConfigManager configMgr = gameManager.getConfigManager();
        World targetWorld = resolveSessionTargetWorld(session, players);
        
        // Load spawn locations from config if session has none
        boolean usingConfigSpawns = session.getSpawnLocations().isEmpty();
        if (usingConfigSpawns) {
            List<Location> configSpawns = configMgr.loadSpawnLocations(targetWorld);
            int validSpawns = 0;
            for (Location loc : configSpawns) {
                if (loc != null && loc.getWorld() != null) {
                    session.addSpawnLocation(loc);
                    validSpawns++;
                }
            }
            if (configSpawns.size() > validSpawns) {
                sender.sendMessage("§eWarning: " + (configSpawns.size() - validSpawns) + " invalid spawn location(s) in config were skipped.");
            }
        }
        
        // Check total spawn locations (session + config)
        int totalSpawns = session.getSpawnLocations().size();
        if (totalSpawns < minSpawnLocations) {
            sender.sendMessage("§cNot enough spawn locations! You need at least " + minSpawnLocations + " spawn location(s).");
            sender.sendMessage("§7Use /matchbox setspawn to add spawn locations to config.");
            return true;
        }
        
        // Load seat locations from config if session has none
        boolean usingConfigSeats = true;
        Map<Integer, Location> configSeats = configMgr.loadSeatLocations(targetWorld);
        int validSeats = 0;
        for (Map.Entry<Integer, Location> entry : configSeats.entrySet()) {
            Location seatLoc = entry.getValue();
            if (seatLoc != null && seatLoc.getWorld() != null) {
                if (!session.hasSeatLocation(entry.getKey())) {
                    session.setSeatLocation(entry.getKey(), seatLoc);
                    validSeats++;
                } else {
                    usingConfigSeats = false;
                }
            }
        }
        if (configSeats.size() > validSeats) {
            sender.sendMessage("§eWarning: " + (configSeats.size() - validSeats) + " invalid seat location(s) in config were skipped.");
        }
        
        // Notify if using config defaults
        if (usingConfigSpawns || (usingConfigSeats && !configSeats.isEmpty())) {
            if (targetWorld != null && configMgr.hasWorldMapConfig(targetWorld)) {
                sender.sendMessage("§7Starting game using map config from world '" + targetWorld.getName() + "'.");
            } else {
                sender.sendMessage("§7Starting game using global config defaults (no map config found).");
            }
            if (usingConfigSpawns && session.getSpawnLocations().size() > 0) {
                sender.sendMessage("§7  Using " + session.getSpawnLocations().size() + " spawn location(s) from config.");
            }
            if (usingConfigSeats && validSeats > 0) {
                sender.sendMessage("§7  Using " + validSeats + " seat location(s) from config.");
            }
        }

        // Mark session as active
        session.setActive(true);

        // Start the game round with session name
        gameManager.startRound(players, session.getSpawnLocations(), session.getDiscussionLocation(), sessionName);

        // Notify all players
        for (Player p : players) {
            p.sendMessage("§aGame started! Good luck!");
        }

        return true;
    }

    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /matchbox join <name>");
            return true;
        }

        Player player = (Player) sender;
        String sessionName = args[1];

        GameSession session = sessionManager.getSession(sessionName);
        if (session == null) {
            sender.sendMessage("§cSession '" + sessionName + "' does not exist!");
            return true;
        }

        if (session.isActive()) {
            sender.sendMessage("§cThis session is currently active. You cannot join right now.");
            return true;
        }

        if (player == null || !player.isOnline()) {
            sender.sendMessage("§cYou must be online to join a session!");
            return true;
        }

        if (session.hasPlayer(player)) {
            sender.sendMessage("§cYou are already in this session!");
            return true;
        }

        if (session.isActive()) {
            SessionGameContext context = gameManager.getContext(sessionName);
            if (context != null && context.getGameState().isGameActive()) {
                sender.sendMessage("§cThis session's game has already started! You cannot join an active game.");
                sender.sendMessage("§7Wait for the game to end or join a different session.");
                return true;
            }
        }

        if (!gameManager.canPlayerJoinSession(player.getUniqueId(), sessionName)) {
            SessionGameContext existingContext = gameManager.getContextForPlayer(player.getUniqueId());
            if (existingContext != null) {
                sender.sendMessage("§cYou are already in an active game in session '" + existingContext.getSessionName() + "'!");
                sender.sendMessage("§7Leave that game first using /matchbox leave");
                return true;
            }
        }

        // Check if session is full (max 7 players)
        if (session.getPlayerCount() >= 7) {
            sender.sendMessage("§cThis session is full! Maximum 7 players allowed.");
            return true;
        }

        if (!session.addPlayer(player)) {
            sender.sendMessage("§cFailed to join session. Please try again.");
            return true;
        }
        sender.sendMessage("§aYou joined session '" + sessionName + "'!");
        session.getPlayers().forEach(p -> {
            if (!p.equals(player)) {
                p.sendMessage("§e" + player.getName() + " joined the session. (" + session.getPlayerCount() + " players)");
            }
        });
        return true;
    }

    private boolean handleLeave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        
        // Check if player is in an active game
        SessionGameContext context = gameManager.getContextForPlayer(player.getUniqueId());
        if (context != null && context.getGameState().isGameActive()) {
            // Player is in an active game - remove them from the game
            boolean removed = gameManager.removePlayerFromGame(player);
            if (removed) {
                sender.sendMessage("§aYou have been removed from the active game.");
                
                // Also remove from session if they're in one
                String activeSession = context.getSessionName();
                if (activeSession != null) {
                    GameSession session = sessionManager.getSession(activeSession);
                    if (session != null && session.hasPlayer(player)) {
                        session.removePlayer(player);
                    }
                }
            } else {
                sender.sendMessage("§cFailed to remove you from the game. Please contact an admin.");
            }
            return true;
        }
        
        // Player is not in an active game - handle session removal
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /matchbox leave <session-name>");
            sender.sendMessage("§7Or use /matchbox leave without arguments if you're in an active game.");
            return true;
        }

        String sessionName = args[1];

        GameSession session = sessionManager.getSession(sessionName);
        if (session == null) {
            sender.sendMessage("§cSession '" + sessionName + "' does not exist!");
            return true;
        }

        if (!session.hasPlayer(player)) {
            sender.sendMessage("§cYou are not in this session!");
            return true;
        }
        
        if (session.isActive()) {
            sender.sendMessage("§cThis session is currently active. You cannot leave during a game.");
            sender.sendMessage("§7If you want to leave the active game, use /matchbox leave without arguments.");
            return true;
        }

        session.removePlayer(player);
        sender.sendMessage("§aYou left session '" + sessionName + "'.");
        session.getPlayers().forEach(p -> {
            if (!p.equals(player)) {
                p.sendMessage("§e" + player.getName() + " left the session. (" + session.getPlayerCount() + " players)");
            }
        });
        
        // If session is now empty, remove it
        if (session.getPlayerCount() == 0) {
            sessionManager.removeSession(sessionName);
            sender.sendMessage("§7Session '" + sessionName + "' was removed (no players left).");
        }
        
        return true;
    }

    private boolean handleSetDiscussion(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matchbox.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /matchbox setdiscussion <name>");
            return true;
        }

        Player player = (Player) sender;
        String sessionName = args[1];

        GameSession session = sessionManager.getSession(sessionName);
        if (session == null) {
            sender.sendMessage("§cSession '" + sessionName + "' does not exist!");
            return true;
        }

        Location location = player.getLocation();
        session.setDiscussionLocation(location);
        
        // Note: Discussion location is session-specific, so we don't save it globally to config
        
        sender.sendMessage("§aDiscussion location set for session '" + sessionName + "'!");
        return true;
    }

    private boolean handleSetSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matchbox.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        Location location = player.getLocation();
        
        // Save to config only (no session needed)
        com.ohacd.matchbox.game.config.ConfigManager configManager = gameManager.getConfigManager();
        configManager.addSpawnLocation(location);
        
        List<Location> allSpawns = configManager.loadSpawnLocations(location.getWorld());
        sender.sendMessage("§aSpawn location added to world map config ('" + location.getWorld().getName() + "')! (Total: " + allSpawns.size() + ")");
        return true;
    }

    private boolean handleSetSeat(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matchbox.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /matchbox setseat <seat-number>");
            sender.sendMessage("§7Example: /matchbox setseat 1");
            return true;
        }

        Player player = (Player) sender;
        
        int seatNumber;
        try {
            seatNumber = Integer.parseInt(args[1]);
            if (seatNumber < 1) {
                sender.sendMessage("§cSeat number must be 1 or greater!");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid seat number: " + args[1]);
            return true;
        }

        Location location = player.getLocation();
        
        // Save to config only (no session needed)
        com.ohacd.matchbox.game.config.ConfigManager configManager = gameManager.getConfigManager();
        configManager.saveSeatLocation(seatNumber, location);
        
        sender.sendMessage("§aSeat " + seatNumber + " location set in world map config ('" + location.getWorld().getName() + "')!");
        return true;
    }

    private boolean handleListSeatSpawns(CommandSender sender) {
        if (!sender.hasPermission("matchbox.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players so Matchbox can resolve your current world map config.");
            return true;
        }

        Player player = (Player) sender;
        World world = player.getWorld();

        com.ohacd.matchbox.game.config.ConfigManager configManager = gameManager.getConfigManager();
        List<String> display = configManager.getSeatLocationsDisplay(world);
        
        sender.sendMessage("§6=== Seat Spawns (" + world.getName() + ") ===");
        for (String line : display) {
            sender.sendMessage(line);
        }
        
        // Also show valid seat spawn numbers
        List<Integer> validSeats = configManager.getDiscussionSeatSpawns(world);
        if (!validSeats.isEmpty()) {
            sender.sendMessage("§6Valid Seat Numbers: §e" + validSeats.toString());
        }
        
        return true;
    }

    private boolean handleListSpawns(CommandSender sender) {
        if (!sender.hasPermission("matchbox.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players so Matchbox can resolve your current world map config.");
            return true;
        }

        Player player = (Player) sender;
        World world = player.getWorld();

        com.ohacd.matchbox.game.config.ConfigManager configManager = gameManager.getConfigManager();
        List<String> display = configManager.getSpawnLocationsDisplay(world);
        
        sender.sendMessage("§6=== Spawn Locations (" + world.getName() + ") ===");
        for (String line : display) {
            sender.sendMessage(line);
        }
        
        return true;
    }

    private boolean handleRemoveSeat(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matchbox.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players so Matchbox can resolve your current world map config.");
            return true;
        }

        Player player = (Player) sender;
        World world = player.getWorld();

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /matchbox removeseat <seat-number>");
            sender.sendMessage("§7Example: /matchbox removeseat 1");
            return true;
        }

        int seatNumber;
        try {
            seatNumber = Integer.parseInt(args[1]);
            if (seatNumber < 1) {
                sender.sendMessage("§cSeat number must be 1 or greater!");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid seat number: " + args[1]);
            return true;
        }

        com.ohacd.matchbox.game.config.ConfigManager configManager = gameManager.getConfigManager();
        Map<Integer, Location> existingSeats = configManager.loadSeatLocations(world);
        
        if (!existingSeats.containsKey(seatNumber)) {
            sender.sendMessage("§cSeat " + seatNumber + " is not configured for world '" + world.getName() + "'.");
            return true;
        }

        configManager.removeSeatLocation(seatNumber, world);
        sender.sendMessage("§aSeat " + seatNumber + " removed from world map config ('" + world.getName() + "').");
        return true;
    }

    private boolean handleRemoveSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matchbox.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players so Matchbox can resolve your current world map config.");
            return true;
        }

        Player player = (Player) sender;
        World world = player.getWorld();

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /matchbox removespawn <index>");
            sender.sendMessage("§7Example: /matchbox removespawn 1");
            sender.sendMessage("§7Use /matchbox listspawns to see available spawns.");
            return true;
        }

        int index;
        try {
            index = Integer.parseInt(args[1]);
            if (index < 1) {
                sender.sendMessage("§cIndex must be 1 or greater!");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid index: " + args[1]);
            return true;
        }

        com.ohacd.matchbox.game.config.ConfigManager configManager = gameManager.getConfigManager();
        List<Location> existingSpawns = configManager.loadSpawnLocations(world);
        
        if (index > existingSpawns.size()) {
            sender.sendMessage("§cIndex " + index + " is out of range. There are only " + existingSpawns.size() + " spawn location(s).");
            sender.sendMessage("§7Use /matchbox listspawns to see available spawns.");
            return true;
        }

        // Convert to 0-based index
        int actualIndex = index - 1;
        boolean removed = configManager.removeSpawnLocation(actualIndex, world);
        
        if (removed) {
            sender.sendMessage("§aSpawn location #" + index + " removed from world map config ('" + world.getName() + "').");
        } else {
            sender.sendMessage("§cFailed to remove spawn location.");
        }
        
        return true;
    }

    private boolean handleClearSpawns(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matchbox.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        String pending = pendingConfirmations.get(playerId);

        // Check if this is a confirmation
        if (pending != null && pending.equals("clearspawns")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
                com.ohacd.matchbox.game.config.ConfigManager configManager = gameManager.getConfigManager();
                int count = configManager.loadSpawnLocations(player.getWorld()).size();
                configManager.clearSpawnLocations(player.getWorld());
                pendingConfirmations.remove(playerId);
                sender.sendMessage("§aCleared all " + count + " spawn location(s) from world map config ('" + player.getWorld().getName() + "').");
                return true;
            } else {
                pendingConfirmations.remove(playerId);
                sender.sendMessage("§cClear operation cancelled.");
                return true;
            }
        }

        // First time - request confirmation
        com.ohacd.matchbox.game.config.ConfigManager configManager = gameManager.getConfigManager();
        int count = configManager.loadSpawnLocations(player.getWorld()).size();
        if (count == 0) {
            sender.sendMessage("§cNo spawn locations to clear.");
            return true;
        }

        pendingConfirmations.put(playerId, "clearspawns");
        sender.sendMessage("§c§lWARNING: This will remove all " + count + " spawn location(s) from config!");
        sender.sendMessage("§7Type §e/matchbox clearspawns confirm §7to confirm, or run the command again to cancel.");
        return true;
    }

    private boolean handleClearSeats(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matchbox.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        String pending = pendingConfirmations.get(playerId);

        // Check if this is a confirmation
        if (pending != null && pending.equals("clearseats")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
                com.ohacd.matchbox.game.config.ConfigManager configManager = gameManager.getConfigManager();
                int count = configManager.loadSeatLocations(player.getWorld()).size();
                // Remove all seat locations
                Map<Integer, Location> seats = configManager.loadSeatLocations(player.getWorld());
                for (Integer seatNum : new ArrayList<>(seats.keySet())) {
                    configManager.removeSeatLocation(seatNum, player.getWorld());
                }
                pendingConfirmations.remove(playerId);
                sender.sendMessage("§aCleared all " + count + " seat location(s) from world map config ('" + player.getWorld().getName() + "').");
                return true;
            } else {
                pendingConfirmations.remove(playerId);
                sender.sendMessage("§cClear operation cancelled.");
                return true;
            }
        }

        // First time - request confirmation
        com.ohacd.matchbox.game.config.ConfigManager configManager = gameManager.getConfigManager();
        int count = configManager.loadSeatLocations(player.getWorld()).size();
        if (count == 0) {
            sender.sendMessage("§cNo seat locations to clear.");
            return true;
        }

        pendingConfirmations.put(playerId, "clearseats");
        sender.sendMessage("§c§lWARNING: This will remove all " + count + " seat location(s) from config!");
        sender.sendMessage("§7Type §e/matchbox clearseats confirm §7to confirm, or run the command again to cancel.");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        Set<String> sessionNames = sessionManager.getAllSessionNames();
        
        // Filter out only empty sessions (sessions with 0 players)
        // Do NOT remove inactive sessions - they are valid waiting sessions
        List<String> validSessions = new ArrayList<>();
        for (String name : sessionNames) {
            GameSession session = sessionManager.getSession(name);
            if (session != null) {
                // Only remove truly empty sessions (no players)
                if (session.getPlayerCount() == 0) {
                    sessionManager.removeSession(name);
                    continue;
                }
                // All sessions with players are valid (whether active or waiting)
                validSessions.add(name);
            }
        }
        
        if (validSessions.isEmpty()) {
            sender.sendMessage("§eNo active sessions.");
            return true;
        }

        sender.sendMessage("§aActive sessions:");
        for (String name : validSessions) {
            GameSession session = sessionManager.getSession(name);
            if (session != null) {
                String status = session.isActive() ? "§c[ACTIVE]" : "§7[Waiting]";
                sender.sendMessage("§7- §e" + name + " §7(" + session.getPlayerCount() + " players) " + status);
            }
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== Matchbox Commands ===");
        sender.sendMessage("§e/matchbox setup ... §7- Map maker tools for baked world map configs");
        sender.sendMessage("§e/matchbox start <name> §7- Create a new game session");
        sender.sendMessage("§e/matchbox begin <name> §7- Begin the game for a session");
        sender.sendMessage("§e/matchbox stop <name> §7- Stop and remove a session");
        sender.sendMessage("§e/matchbox join <name> §7- Join a game session");
        sender.sendMessage("§e/matchbox remove <name> §7- Remove a session (deprecated, use stop)");
        sender.sendMessage("§e/matchbox leave <name> §7- Leave a game session");
        sender.sendMessage("§e/matchbox nick [name|random|reset] §7- Manage your in-game nick");
        sender.sendMessage("§e/matchbox setdiscussion <name> §7- Set discussion location");
        sender.sendMessage("§e/matchbox list §7- List all sessions");
        sender.sendMessage("§e/matchbox cleanup §7- Emergency nametag restore (admin only)");
        sender.sendMessage("§e/matchbox debug §7- Show debug info (admin only)");
        sender.sendMessage("§e/matchbox debugstart <name> §7- Force begin with debug override (admin only)");
        sender.sendMessage("§e/matchbox skip §7- Skip current phase (admin only)");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("setup", "start", "begin", "debugstart", "stop", "join", "leave", "nick", "setdiscussion", "list", "remove", "cleanup", "debug", "skip");
            return subCommands.stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("setup")) {
            List<String> setupSubCommands = Arrays.asList(
                "help", "init", "info", "validate", "importlegacy",
                    "setspawn", "setseat", "listspawns", "listseats",
                    "removespawn", "removeseat", "clearspawns", "clearseats", "seatspawns"
            );
            return setupSubCommands.stream()
                    .filter(cmd -> cmd.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setup") && args[1].equalsIgnoreCase("importlegacy")) {
            return List.of("overwrite").stream()
                .filter(cmd -> cmd.startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setup") && args[1].equalsIgnoreCase("seatspawns")) {
            List<String> seatSpawnActions = Arrays.asList("list", "add", "remove", "set");
            return seatSpawnActions.stream()
                    .filter(cmd -> cmd.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setup") &&
                (args[1].equalsIgnoreCase("clearspawns") || args[1].equalsIgnoreCase("clearseats"))) {
            return List.of("confirm").stream()
                    .filter(cmd -> cmd.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("nick")) {
                List<String> nickSubs = Arrays.asList("reset", "random");
                return nickSubs.stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (subCommand.equals("begin") || subCommand.equals("debugstart") || subCommand.equals("stop") || subCommand.equals("join") ||
                    subCommand.equals("leave") || subCommand.equals("setdiscussion") ||
                    subCommand.equals("setspawn") || subCommand.equals("setseat") || subCommand.equals("remove")) {
                return sessionManager.getAllSessionNames().stream()
                        .filter(name -> name.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("nick")) {
            // /mb nick reset <player> or /mb nick random <player> or /mb nick <player> <nick>
            String arg1 = args[1].toLowerCase();
            if (arg1.equals("reset") || arg1.equals("random") || sender.hasPermission("matchbox.admin")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    // =========================================================
    // /mb nick handler
    // =========================================================

    private boolean handleNick(CommandSender sender, String[] args) {
        if (nickManager == null) {
            sender.sendMessage("§cNick system is not available.");
            return true;
        }

        // /mb nick  — show current nick
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cThis command can only be used by players.");
                return true;
            }
            String current = nickManager.getNick(player.getUniqueId());
            if (current == null) {
                sender.sendMessage("§7You have no nick set. Use §e/mb nick <name>§7 to set one, or §e/mb nick random§7 to get one.");
            } else {
                sender.sendMessage("§7Your current nick: " + current + "§7. Use §e/mb nick reset§7 to remove it.");
            }
            return true;
        }

        String arg1 = args[1];

        // /mb nick reset [player]  — remove nick
        if (arg1.equalsIgnoreCase("reset")) {
            if (args.length >= 3) {
                // Admin: reset another player's nick
                if (!sender.hasPermission("matchbox.admin")) {
                    sender.sendMessage("§cYou don't have permission to reset other players' nicks.");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage("§cPlayer '§e" + args[2] + "§c' not found or is offline.");
                    return true;
                }
                nickManager.removeNick(target.getUniqueId());
                applyOrRestoreNickInSession(target);
                refreshNickReminderActionBar(target);
                sender.sendMessage("§aNick reset for §e" + target.getName() + "§a.");
                target.sendMessage("§7Your nick has been reset by an admin.");
            } else {
                // Self: reset own nick
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cThis command can only be used by players.");
                    return true;
                }
                nickManager.removeNick(player.getUniqueId());
                applyOrRestoreNickInSession(player);
                refreshNickReminderActionBar(player);
                sender.sendMessage("§aYour nick has been reset.");
            }
            return true;
        }

        // /mb nick random [player]  — generate a random nick
        if (arg1.equalsIgnoreCase("random")) {
            if (args.length >= 3) {
                // Admin: random nick for another player
                if (!sender.hasPermission("matchbox.admin")) {
                    sender.sendMessage("§cYou don't have permission to set nicks for other players.");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage("§cPlayer '§e" + args[2] + "§c' not found or is offline.");
                    return true;
                }
                String nick = RandomNickGenerator.generateUnique(buildGlobalTakenNicks(target.getUniqueId()));
                nickManager.setNick(target.getUniqueId(), nick, true);
                applyOrRestoreNickInSession(target);
                refreshNickReminderActionBar(target);
                sender.sendMessage("§aGenerated nick §e" + nick + "§a for §e" + target.getName() + "§a.");
                target.sendMessage("§7Your nick has been set to §e" + nick + "§7 by an admin.");
            } else {
                // Self: random nick
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cThis command can only be used by players.");
                    return true;
                }
                String nick = RandomNickGenerator.generateUnique(buildGlobalTakenNicks(player.getUniqueId()));
                nickManager.setNick(player.getUniqueId(), nick, player.hasPermission("matchbox.admin"));
                applyOrRestoreNickInSession(player);
                refreshNickReminderActionBar(player);
                sender.sendMessage("§aYour nick has been set to §e" + nick + "§a.");
            }
            return true;
        }

        // /mb nick <player> <nick>  — admin set nick for another player (3 args)
        if (args.length >= 3 && sender.hasPermission("matchbox.admin")) {
            Player target = Bukkit.getPlayer(arg1);
            if (target != null) {
                String nick = args[2];
                NickManager.NickResult result = nickManager.setNick(target.getUniqueId(), nick, true);
                if (result != NickManager.NickResult.SUCCESS) {
                    sendNickError(sender, result);
                    return true;
                }
                applyOrRestoreNickInSession(target);
                refreshNickReminderActionBar(target);
                sender.sendMessage("§aSet nick §e" + nick + "§a for §e" + target.getName() + "§a.");
                target.sendMessage("§7Your nick has been set to §e" + nick + "§7 by an admin.");
                return true;
            }
        }

        // /mb nick <nickname>  — set own nick
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }
        boolean isAdmin = player.hasPermission("matchbox.admin");
        NickManager.NickResult result = nickManager.setNick(player.getUniqueId(), arg1, isAdmin);
        if (result != NickManager.NickResult.SUCCESS) {
            sendNickError(sender, result);
            return true;
        }
        applyOrRestoreNickInSession(player);
        refreshNickReminderActionBar(player);
        sender.sendMessage("§aYour nick has been set to §e" + arg1 + "§a.");
        return true;
    }

    /**
     * Refreshes the nick reminder action bar immediately after a successful nick command,
     * so players do not wait for the scheduled 2-second ticker.
     */
    private void refreshNickReminderActionBar(Player player) {
        SessionGameContext context = gameManager.getContextForPlayer(player.getUniqueId());
        if (context != null && context.getGameState().isGameActive()) {
            return;
        }

        String nick = nickManager.getNick(player.getUniqueId());
        if (nick == null) {
            player.sendActionBar(Component.text(""));
            return;
        }

        player.sendActionBar(Component.text("§7Currently Nicked as: §a" + nick));
    }

    /**
     * If the player is currently in an active game session, applies (or restores) their
     * nick immediately so the change is visible without waiting for the next game start.
     */
    private void applyOrRestoreNickInSession(Player player) {
        SessionGameContext context = gameManager.getContextForPlayer(player.getUniqueId());
        if (context == null || !context.getGameState().isGameActive()) return;

        String nick = nickManager.getNick(player.getUniqueId());
        if (nick == null) {
            // Nick was removed — restore real name
            nickManager.restoreNick(player);
            return;
        }

        // Re-apply in-place so there is no visual "blank" frame between old/new nick.
        Set<String> taken = nickManager.getTakenNicksInSession(
                context.getGameState().getAllParticipatingPlayerIds());
        taken.remove(nick.toLowerCase()); // allow the player to (re-)claim their own nick
        if (!nickManager.applyNick(player, taken)) {
            player.sendMessage("§cThat nick is already used by another player in this session. Use §e/mb nick random§c for a unique one.");
            nickManager.restoreNick(player);
            nickManager.removeNick(player.getUniqueId());
        }
    }

    /**
     * Builds a lower-case set of all nicks currently in use across every active session,
     * excluding the given UUID (so a player can re-nick themselves without self-conflict).
     */
    private Set<String> buildGlobalTakenNicks(UUID exclude) {
        Set<String> taken = new HashSet<>();
        for (String sessionName : gameManager.getActiveSessionNames()) {
            SessionGameContext ctx = gameManager.getContext(sessionName);
            if (ctx == null) continue;
            for (UUID id : ctx.getGameState().getAllParticipatingPlayerIds()) {
                if (id.equals(exclude)) continue;
                String nick = nickManager.getNick(id);
                if (nick != null) taken.add(nick.toLowerCase());
            }
        }
        return taken;
    }

    private World resolveSessionTargetWorld(GameSession session, List<Player> players) {
        if (session != null && session.hasDiscussionLocation() && session.getDiscussionLocation().getWorld() != null) {
            return session.getDiscussionLocation().getWorld();
        }

        if (session != null && !session.getSpawnLocations().isEmpty()) {
            Location firstSpawn = session.getSpawnLocations().get(0);
            if (firstSpawn != null && firstSpawn.getWorld() != null) {
                return firstSpawn.getWorld();
            }
        }

        if (players != null && !players.isEmpty()) {
            Player firstPlayer = players.get(0);
            if (firstPlayer != null && firstPlayer.getWorld() != null) {
                return firstPlayer.getWorld();
            }
        }

        return null;
    }

    private void sendNickError(CommandSender sender, NickManager.NickResult result) {
        switch (result) {
            case TOO_SHORT  -> sender.sendMessage("§cNick too short. Minimum 3 characters.");
            case TOO_LONG   -> sender.sendMessage("§cNick too long. Maximum 16 characters.");
            case INVALID_CHARS -> sender.sendMessage("§cInvalid characters. Use only letters, numbers, §e_§c and §e-§c.");
            default         -> sender.sendMessage("§cFailed to set nick.");
        }
    }
}