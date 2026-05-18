package com.ohacd.matchbox.game;

import com.ohacd.matchbox.game.chat.ChatPipelineManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Read-only query surface for listeners that need to inspect game state
 * but must not trigger game actions.
 *
 * <p>Protection and utility listeners should accept this interface rather
 * than the full {@link GameManager} to prevent accidental action calls.</p>
 */
public interface GameSessionQuery {

    /** Returns the active session context for the given player, or null if not in a game. */
    SessionGameContext getContextForPlayer(UUID playerId);

    /** Returns the active session context for the given session name, or null if not active. */
    SessionGameContext getContext(String sessionName);

    /** Returns true if sign-mode is currently enabled and configured. */
    boolean isSignModeEnabled();

    /** Returns true if the given item stack is a sign-mode sign item. */
    boolean isSignModeItem(ItemStack item);

    /** Returns the chat pipeline manager. */
    ChatPipelineManager getChatPipelineManager();

    /** Shows a floating hologram above the player with the given message when chat is blocked. */
    void showChatBlockedHologram(Player player, String message);

    /** Returns the owning plugin. */
    Plugin getPlugin();
}
