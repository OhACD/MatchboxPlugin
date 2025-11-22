package com.ohacd.matchbox.game.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class NameTagManager {

    public static void hideNameTag(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam("hidden_nametags");

        if (team == null) {
            team = board.registerNewTeam("hidden_nametags");
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        }

        team.addEntry(player.getName());
    }

    public static void showNameTag(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam("hidden_nametags");

        if (team != null) {
            team.removeEntry(player.getName());
        }
    }

}
