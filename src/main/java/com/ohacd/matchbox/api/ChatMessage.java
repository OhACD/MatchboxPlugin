package com.ohacd.matchbox.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable representation of a chat message with all metadata needed for routing.
 * Used throughout the chat pipeline system.
 *
 * @since 0.9.5
 * @author Matchbox Team
 */
public record ChatMessage(
    @NotNull Component originalMessage,
    @NotNull Component formattedMessage,
    @NotNull Player sender,
    @NotNull UUID senderId,
    @NotNull ChatChannel channel,
    @NotNull String sessionName,
    boolean isAlivePlayer,
    @NotNull Instant timestamp
) {
    /**
     * Creates a new ChatMessage with the current timestamp.
     */
    public ChatMessage(
        @NotNull Component originalMessage,
        @NotNull Component formattedMessage,
        @NotNull Player sender,
        @NotNull ChatChannel channel,
        @NotNull String sessionName,
        boolean isAlivePlayer
    ) {
        this(originalMessage, formattedMessage, sender, sender.getUniqueId(), channel, sessionName, isAlivePlayer, Instant.now());
    }

    /**
     * Creates a copy of this message with a modified formatted message.
     * Useful for processors that want to modify message content.
     */
    public ChatMessage withFormattedMessage(@NotNull Component newFormattedMessage) {
        return new ChatMessage(originalMessage, newFormattedMessage, sender, senderId, channel, sessionName, isAlivePlayer, timestamp);
    }

    /**
     * Creates a copy of this message with a modified channel.
     * Useful for processors that want to reroute messages.
     */
    public ChatMessage withChannel(@NotNull ChatChannel newChannel) {
        return new ChatMessage(originalMessage, formattedMessage, sender, senderId, newChannel, sessionName, isAlivePlayer, timestamp);
    }
}
