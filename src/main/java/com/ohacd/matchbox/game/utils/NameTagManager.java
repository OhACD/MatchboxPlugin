package com.ohacd.matchbox.game.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Manages nametag visibility using scoreboard teams.
 * Uses session-specific teams to avoid conflicts between multiple games.
 */
public class NameTagManager {

    /**
     * Hides a player's nametag using a session-specific team.
     */
    public static void hideNameTag(Player player, String sessionName) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "matchbox_" + (sessionName != null ? sessionName : "default");

        // Limit team name to 16 characters (Minecraft limit)
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        Team team = board.getTeam(teamName);

        if (team == null) {
            team = board.registerNewTeam(teamName);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        }

        team.addEntry(player.getName());
    }

    /**
     * Hides a player's nametag using the default team.
     */
    public static void hideNameTag(Player player) {
        hideNameTag(player, null);
    }

    /**
     * Shows a player's nametag by removing them from all Matchbox teams.
     */
    public static void showNameTag(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        // Remove from all teams that start with "matchbox_"
        for (Team team : board.getTeams()) {
            if (team.getName().startsWith("matchbox_")) {
                team.removeEntry(player.getName());
            }
        }
    }

    /**
     * Cleans up all Matchbox-related scoreboard teams.
     * Should be called on plugin disable or when clearing all games.
     */
    public static void cleanupAllTeams() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        // Unregister all Matchbox teams
        for (Team team : board.getTeams()) {
            if (team.getName().startsWith("matchbox_")) {
                team.unregister();
            }
        }
    }

    /**
     * Restores nametags for all players in all Matchbox teams.
     * Emergency cleanup method.
     */
    public static void restoreAllNameTags() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        for (Team team : board.getTeams()) {
            if (team.getName().startsWith("matchbox_")) {
                // Get all entries before unregistering
                for (String entry : team.getEntries()) {
                    team.removeEntry(entry);
                }
                team.unregister();
            }
        }
    }
}