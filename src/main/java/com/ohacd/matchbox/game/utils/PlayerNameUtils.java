package com.ohacd.matchbox.game.utils;

import org.bukkit.entity.Player;

/**
 * Utility helpers for resolving player-facing names with nickname support.
 * Prefers the Bukkit display name while safely falling back to the real name.
 */
@SuppressWarnings("deprecation")
public final class PlayerNameUtils {
    private PlayerNameUtils() {}

    /**
     * Returns the display name if available, otherwise the real player name, or "Unknown" when null.
     */
    public static String displayName(Player player) {
        if (player == null) {
            return "Unknown";
        }
        String display = safeTrim(player.getDisplayName());
        if (!display.isEmpty()) {
            return display;
        }
        String name = safeTrim(player.getName());
        return name.isEmpty() ? "Unknown" : name;
    }

    private static String safeTrim(String input) {
        return input == null ? "" : input.trim();
    }
}

