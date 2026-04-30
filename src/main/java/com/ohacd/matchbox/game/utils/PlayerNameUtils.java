package com.ohacd.matchbox.game.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

/**
 * Utility helpers for resolving player-facing names with nickname support.
 * Prefers the Bukkit display name while safely falling back to the real name.
 */
public final class PlayerNameUtils {
    private PlayerNameUtils() {}

    /**
     * Returns the display name if available, otherwise the real player name, or "Unknown" when null.
     */
    public static String displayName(Player player) {
        if (player == null) {
            return "Unknown";
        }
        Component displayComponent = player.displayName();
        String display = safeTrim(displayComponent == null ? "" : LegacyComponentSerializer.legacySection().serialize(displayComponent));
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

