package com.ohacd.matchbox.command;

import com.ohacd.matchbox.Matchbox;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.session.GameSession;
import com.ohacd.matchbox.game.session.SessionManager;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.NameTagManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles all matchbox commands.
 */
public class MatchboxCommand implements CommandExecutor, TabCompleter {
    private final SessionManager sessionManager;
    private final GameManager gameManager;

    public MatchboxCommand(Matchbox plugin, SessionManager sessionManager, GameManager gameManager) {
        if (sessionManager == null) {
            throw new IllegalArgumentException("SessionManager cannot be null");
        }
        if (gameManager == null) {
            throw new IllegalArgumentException("GameManager cannot be null");
        }
        this.sessionManager = sessionManager;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
                return handleStart(sender, args);
            case "begin":
                return handleBegin(sender, args);
            case "stop":
                return handleStop(sender, args);
            case "join":
                return handleJoin(sender, args);
            case "leave":
                return handleLeave(sender, args);
            case "setdiscussion":
                return handleSetDiscussion(sender, args);
            case "setspawn":
                return handleSetSpawn(sender, args);
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
            default:
                sendHelp(sender);
                return true;
        }
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
        return true;
    }

    private boolean handleBegin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matchbox.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /matchbox begin <name>");
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
        if (players.size() < 2) {
            sender.sendMessage("§cYou need at least 2 players to start a game!");
            return true;
        }
        
        // Check if session has too many players (max 7)
        if (players.size() > 7) {
            sender.sendMessage("§cThis session has too many players! Maximum 7 players allowed.");
            return true;
        }

        if (!session.hasSpawnLocations()) {
            sender.sendMessage("§cNo spawn locations set! Use /matchbox setspawn " + sessionName + " to add spawn locations.");
            return true;
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
                String activeSession = gameManager.getGameState().getActiveSessionName();
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

        session.setDiscussionLocation(player.getLocation());
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

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /matchbox setspawn <name>");
            return true;
        }

        Player player = (Player) sender;
        String sessionName = args[1];

        GameSession session = sessionManager.getSession(sessionName);
        if (session == null) {
            sender.sendMessage("§cSession '" + sessionName + "' does not exist!");
            return true;
        }

        session.addSpawnLocation(player.getLocation());
        sender.sendMessage("§aSpawn location added for session '" + sessionName + "'! (Total: " + session.getSpawnLocations().size() + ")");
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
        sender.sendMessage("§e/matchbox start <name> §7- Create a new game session");
        sender.sendMessage("§e/matchbox begin <name> §7- Begin the game for a session");
        sender.sendMessage("§e/matchbox stop <name> §7- Stop and remove a session");
        sender.sendMessage("§e/matchbox join <name> §7- Join a game session");
        sender.sendMessage("§e/matchbox remove <name> §7- Remove a session (deprecated, use stop)");
        sender.sendMessage("§e/matchbox leave <name> §7- Leave a game session");
        sender.sendMessage("§e/matchbox setdiscussion <name> §7- Set discussion location");
        sender.sendMessage("§e/matchbox setspawn <name> §7- Add a spawn location");
        sender.sendMessage("§e/matchbox list §7- List all sessions");
        sender.sendMessage("§e/matchbox cleanup §7- Emergency nametag restore (admin only)");
        sender.sendMessage("§e/matchbox debug §7- Show debug info (admin only)");
        sender.sendMessage("§e/matchbox skip §7- Skip current phase (admin only)");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("start", "begin", "stop", "join", "leave", "setdiscussion", "setspawn", "list", "remove", "cleanup", "debug", "skip");
            return subCommands.stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("begin") || subCommand.equals("stop") || subCommand.equals("join") ||
                    subCommand.equals("leave") || subCommand.equals("setdiscussion") ||
                    subCommand.equals("setspawn") || subCommand.equals("remove")) {
                return sessionManager.getAllSessionNames().stream()
                        .filter(name -> name.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}