package com.ohacd.matchbox.game.chat;

import com.ohacd.matchbox.api.ChatChannel;
import com.ohacd.matchbox.api.ChatMessage;
import com.ohacd.matchbox.api.ChatProcessor;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.state.GameState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Default chat handler for a game session that implements spectator isolation.
 * Routes messages based on player status and game phase.
 *
 * <p>Routing rules:</p>
 * <ul>
 *   <li>Alive players → Game channel (visible to alive players + spectators)</li>
 *   <li>Spectators → Spectator channel (visible only to spectators in the same session)</li>
 *   <li>GLOBAL channel → Bypasses all filtering</li>
 * </ul>
 * <p>SWIPE-phase gating is handled upstream in {@code ChatListener} before messages
 * reach this handler, so no phase check is needed here. Alive-status is always
 * queried live from {@link GameState} — there is no cache (see CHANGELOG 0.9.8).</p>
 */
public class SessionChatHandler implements ChatProcessor {

    private final SessionGameContext context;
    private final Plugin plugin;

    public SessionChatHandler(@NotNull SessionGameContext context, @NotNull Plugin plugin) {
        this.context = context;
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public ChatProcessingResult process(@NotNull ChatMessage message) {
        // Handle GLOBAL channel bypass
        if (message.channel() == ChatChannel.GLOBAL) {
            return ChatProcessingResult.allow(message);
        }

        GameState gameState = context.getGameState();

        // Check if player is in this session
        if (!gameState.getAllParticipatingPlayerIds().contains(message.senderId())) {
            // Player not in this session, allow normal chat
            return ChatProcessingResult.allow(message.withChannel(ChatChannel.GLOBAL));
        }

        // Always query live status — never cache. A stale cached value would route
        // a freshly-eliminated player to the GAME channel, leaking their messages.
        boolean isAlive = gameState.isAlive(message.senderId());

        // Route based on player status
        if (isAlive) {
            // Alive player - route to game channel
            return ChatProcessingResult.allow(message.withChannel(ChatChannel.GAME));
        } else {
            // Spectator - route to spectator channel
            return ChatProcessingResult.allow(message.withChannel(ChatChannel.SPECTATOR));
        }
    }

    /**
     * Delivers a processed chat message to the appropriate recipients.
     * This method is called after custom processors have been applied.
     *
     * @param message the processed message to deliver
     */
    public void deliverMessage(@NotNull ChatMessage message) {
        GameState gameState = context.getGameState();
        Set<UUID> recipients = getChannelRecipients(message.channel(), gameState);

        // Send to all recipients
        for (UUID recipientId : recipients) {
            Player recipient = Bukkit.getPlayer(recipientId);
            if (recipient != null && recipient.isOnline()) {
                try {
                    recipient.sendMessage(message.formattedMessage());
                } catch (Exception e) {
                    plugin.getLogger().warning(
                        "Failed to send chat message to " + recipient.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Gets the recipients for a given chat channel.
     *
     * @param channel the channel to get recipients for
     * @param gameState the current game state
     * @return set of player UUIDs who should receive messages on this channel
     */
    @NotNull
    private Set<UUID> getChannelRecipients(@NotNull ChatChannel channel, @NotNull GameState gameState) {
        Set<UUID> allParticipants = gameState.getAllParticipatingPlayerIds();
        Set<UUID> alivePlayers = gameState.getAlivePlayerIds();

        return switch (channel) {
            case GAME -> {
                // Game channel: alive players + all spectators
                Set<UUID> recipients = new HashSet<>(alivePlayers);
                for (UUID participant : allParticipants) {
                    if (!alivePlayers.contains(participant)) {
                        recipients.add(participant); // Add spectators
                    }
                }
                yield recipients;
            }
            case SPECTATOR -> {
                // Spectator channel: only spectators
                Set<UUID> recipients = new HashSet<>();
                for (UUID participant : allParticipants) {
                    if (!alivePlayers.contains(participant)) {
                        recipients.add(participant); // Add spectators
                    }
                }
                yield recipients;
            }
            case GLOBAL -> {
                // Global channel: everyone on the server (handled by normal chat)
                yield Collections.emptySet();
            }
        };
    }

    /**
     * Gets the session name this handler is responsible for.
     */
    @NotNull
    public String getSessionName() {
        return context.getSessionName();
    }
}
