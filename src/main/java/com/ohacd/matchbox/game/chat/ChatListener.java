package com.ohacd.matchbox.game.chat;

import com.ohacd.matchbox.api.ChatChannel;
import com.ohacd.matchbox.api.ChatMessage;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.hologram.HologramManager;
import com.ohacd.matchbox.game.utils.GamePhase;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Main chat event listener that integrates with the chat pipeline system.
 * Handles intercepting chat messages and routing them through the pipeline.
 */
public class ChatListener implements Listener {

    private final HologramManager hologramManager;
    private final GameManager gameManager;

    /**
     * Creates a chat listener that integrates chat pipeline and holograms.
     *
     * @param manager hologram manager used for in-game messages
     * @param gameManager central game manager for session lookups
     */
    public ChatListener(HologramManager manager, GameManager gameManager) {
        this.hologramManager = manager;
        this.gameManager = gameManager;
    }

    @EventHandler
    /**
     * Handles asynchronous chat events and routes them through the chat pipeline.
     *
     * @param event the asynchronous chat event
     */
    public void onChat(AsyncChatEvent event) {
        // Only handle asynchronous chat events
        if (!event.isAsynchronous()) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        // Find which session the player is in (if any)
        SessionGameContext context = gameManager.getContextForPlayer(player.getUniqueId());
        if (context == null) {
            // Player not in any active game - use normal server chat
            return;
        }

        // Handle SWIPE phase specially - always show holograms
        if (context.getPhaseManager().getCurrentPhase() == GamePhase.SWIPE) {
            // Cancel normal chat and show hologram instead
            event.setCancelled(true);
            String msg = PlainTextComponentSerializer.plainText().serialize(event.message());
            hologramManager.showTextAbove(player, msg, 100);
            return;
        }

        // For all other phases, route through the chat pipeline
        try {
            // Determine player's alive status for routing
            boolean isAlivePlayer = context.getGameState().isAlive(player.getUniqueId());

            // Create formatted message with player name prefix (using display name for nick plugin support)
            Component formattedMessageWithName = Component.text("<", NamedTextColor.WHITE)
                .append(Component.text(player.getDisplayName(), NamedTextColor.WHITE))
                .append(Component.text("> ", NamedTextColor.WHITE))
                .append(event.message());

            // Create chat message for pipeline processing
            ChatMessage chatMessage = new ChatMessage(
                event.originalMessage(),
                formattedMessageWithName,
                player,
                ChatChannel.GAME, // Default to game channel, pipeline will route appropriately
                context.getSessionName(),
                isAlivePlayer
            );

            // Process through pipeline
            var pipelineResult = gameManager.getChatPipelineManager()
                .processMessage(context.getSessionName(), chatMessage);

            // Handle pipeline result
            switch (pipelineResult.result()) {
                case ALLOW -> {
                    // Pipeline may have modified the message, update the event
                    if (!pipelineResult.message().formattedMessage().equals(event.message())) {
                        event.message(pipelineResult.message().formattedMessage());
                    }

                    // Send to appropriate recipients based on channel
                    SessionChatHandler handler = gameManager.getChatPipelineManager()
                        .getOrCreateSessionHandler(context.getSessionName());

                    if (pipelineResult.message().channel() != ChatChannel.GLOBAL) {
                        // Custom channel routing - cancel event and handle manually
                        event.setCancelled(true);
                        handler.deliverMessage(pipelineResult.message());
                    }
                    // For GLOBAL channel, let the event proceed normally
                }
                case DENY, CANCEL -> {
                    // Cancel the message
                    event.setCancelled(true);
                }
            }

        } catch (Exception e) {
            // On pipeline error, fall back to normal chat - use GameManager's plugin field
            // Let normal chat proceed
        }
    }
}
