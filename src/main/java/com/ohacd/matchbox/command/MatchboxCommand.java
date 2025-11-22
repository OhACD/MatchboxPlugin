package com.ohacd.matchbox.command;

import com.ohacd.matchbox.Matchbox;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.session.GameSession;
import com.ohacd.matchbox.game.session.SessionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles all matchbox commands.
 */
public class MatchboxCommand implements CommandExecutor, TabCompleter {
    private final Matchbox plugin;
    private final SessionManager sessionManager;
    private final GameManager gameManager;

    public MatchboxCommand(Matchbox plugin, SessionManager sessionManager, GameManager gameManager) {
        this.plugin = plugin;
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
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleStart(CommandSender sender, String[] args) {
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

        List<Player> players = session.getPlayers();
        if (players.size() < 2) {
            sender.sendMessage("§cYou need at least 2 players to start a game!");
            return true;
        }

        if (!session.hasSpawnLocations()) {
            sender.sendMessage("§cNo spawn locations set! Use /matchbox setspawn " + sessionName + " to add spawn locations.");
            return true;
        }

        // Mark session as active
        session.setActive(true);

        // Start the game round
        gameManager.startRound(players, session.getSpawnLocations(), session.getDiscussionLocation());

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

        if (session.hasPlayer(player)) {
            sender.sendMessage("§cYou are already in this session!");
            return true;
        }

        session.addPlayer(player);
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

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /matchbox leave <name>");
            return true;
        }

        Player player = (Player) sender;
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

        session.removePlayer(player);
        sender.sendMessage("§aYou left session '" + sessionName + "'.");
        session.getPlayers().forEach(p -> {
            p.sendMessage("§e" + player.getName() + " left the session. (" + session.getPlayerCount() + " players)");
        });
        return true;
    }

    private boolean handleSetDiscussion(CommandSender sender, String[] args) {
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
        if (sessionNames.isEmpty()) {
            sender.sendMessage("§eNo active sessions.");
            return true;
        }

        sender.sendMessage("§aActive sessions:");
        for (String name : sessionNames) {
            GameSession session = sessionManager.getSession(name);
            sender.sendMessage("§7- §e" + name + " §7(" + session.getPlayerCount() + " players)");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== Matchbox Commands ===");
        sender.sendMessage("§e/matchbox start <name> §7- Create a new game session");
        sender.sendMessage("§e/matchbox begin <name> §7- Begin the game for a session");
        sender.sendMessage("§e/matchbox join <name> §7- Join a game session");
        sender.sendMessage("§e/matchbox leave <name> §7- Leave a game session");
        sender.sendMessage("§e/matchbox setdiscussion <name> §7- Set discussion location");
        sender.sendMessage("§e/matchbox setspawn <name> §7- Add a spawn location");
        sender.sendMessage("§e/matchbox list §7- List all sessions");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("start", "begin", "join", "leave", "setdiscussion", "setspawn", "list");
            return subCommands.stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("begin") || subCommand.equals("join") || subCommand.equals("leave") ||
                subCommand.equals("setdiscussion") || subCommand.equals("setspawn")) {
                return sessionManager.getAllSessionNames().stream()
                        .filter(name -> name.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
