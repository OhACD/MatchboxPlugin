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
 * @param originalMessage original message content (unmodified)
 * @param formattedMessage formatted message used for display
 * @param sender player who sent the message
 * @param senderId UUID of the sender
 * @param channel chat channel the message belongs to
 * @param sessionName session name the message was sent in
 * @param isAlivePlayer whether the sender is an alive player
 * @param timestamp instant the message was recorded
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
     *
     * @param originalMessage original message content (unmodified)
     * @param formattedMessage formatted message used for display
     * @param sender sender player
     * @param channel channel of message
     * @param sessionName session name
     * @param isAlivePlayer whether sender is alive
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
     *
     * @param newFormattedMessage the updated formatted message component
     * @return a new ChatMessage with the modified formatted message
     */
    public ChatMessage withFormattedMessage(@NotNull Component newFormattedMessage) {
        return new ChatMessage(originalMessage, newFormattedMessage, sender, senderId, channel, sessionName, isAlivePlayer, timestamp);
    }

    /**
     * Creates a copy of this message with a modified channel.
     * Useful for processors that want to reroute messages.
     *
     * @param newChannel new chat channel for the message
     * @return a new ChatMessage routed to the provided channel
     */
    public ChatMessage withChannel(@NotNull ChatChannel newChannel) {
        return new ChatMessage(originalMessage, formattedMessage, sender, senderId, newChannel, sessionName, isAlivePlayer, timestamp);
    }
}
