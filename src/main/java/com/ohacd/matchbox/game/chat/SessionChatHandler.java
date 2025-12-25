package com.ohacd.matchbox.game.chat;

import com.ohacd.matchbox.api.ChatChannel;
import com.ohacd.matchbox.api.ChatMessage;
import com.ohacd.matchbox.api.ChatProcessor;
import com.ohacd.matchbox.api.ChatResult;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.utils.GamePhase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default chat handler for a game session that implements spectator isolation.
 * Routes messages based on player status and game phase.
 *
 * <p>Routing rules:</p>
 * <ul>
 *   <li>Alive players → Game channel (visible to alive + spectators)</li>
 *   <li>Spectators → Spectator channel (visible only to spectators)</li>
 *   <li>SWIPE phase → Holograms (no chat)</li>
 *   <li>GLOBAL channel → Bypasses all filtering</li>
 * </ul>
 */
public class SessionChatHandler implements ChatProcessor {

    private final String sessionName;
    private final GameManager gameManager;
    private final Plugin plugin;

    // Cache for frequently accessed data
    private final Map<UUID, Boolean> aliveStatusCache = new ConcurrentHashMap<>();

    public SessionChatHandler(@NotNull String sessionName, @NotNull GameManager gameManager, @NotNull Plugin plugin) {
        this.sessionName = sessionName;
        this.gameManager = gameManager;
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public ChatProcessingResult process(@NotNull ChatMessage message) {
        // Handle GLOBAL channel bypass
        if (message.channel() == ChatChannel.GLOBAL) {
            return ChatProcessingResult.allow(message);
        }

        // Get session context
        SessionGameContext context = gameManager.getContext(sessionName);
        if (context == null) {
            // Session ended, allow normal chat
            return ChatProcessingResult.allow(message.withChannel(ChatChannel.GLOBAL));
        }

        GameState gameState = context.getGameState();

        // Check if player is in this session
        if (!gameState.getAllParticipatingPlayerIds().contains(message.senderId())) {
            // Player not in this session, allow normal chat
            return ChatProcessingResult.allow(message.withChannel(ChatChannel.GLOBAL));
        }

        // Determine if player is alive (with caching for performance)
        boolean isAlive = aliveStatusCache.computeIfAbsent(message.senderId(),
            id -> gameState.isAlive(id));

        // Handle SWIPE phase - use holograms instead of chat
        if (context.getPhaseManager().getCurrentPhase() == GamePhase.SWIPE) {
            // During SWIPE phase, cancel normal chat and let hologram system handle it
            return ChatProcessingResult.cancel(message);
        }

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
        SessionGameContext context = gameManager.getContext(sessionName);
        if (context == null) {
            return; // Session ended
        }

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
     * Invalidates the alive status cache for a player.
     * Should be called when a player's status changes (elimination, etc.).
     *
     * @param playerId the player whose cache to invalidate
     */
    public void invalidateCache(@NotNull UUID playerId) {
        aliveStatusCache.remove(playerId);
    }

    /**
     * Clears all cached data.
     * Should be called when the session ends.
     */
    public void clearCache() {
        aliveStatusCache.clear();
    }

    /**
     * Gets the session name this handler is responsible for.
     */
    @NotNull
    public String getSessionName() {
        return sessionName;
    }
}
