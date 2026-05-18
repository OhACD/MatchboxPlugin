package com.ohacd.matchbox.game.chat;

import com.ohacd.matchbox.api.ChatChannel;
import com.ohacd.matchbox.api.ChatMessage;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.hologram.HologramManager;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.PlayerNameUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

    static Component buildFormattedMessageWithName(Player player, Component messageBody) {
        String displayName = PlayerNameUtils.displayName(player);
        Component playerName = LegacyComponentSerializer.legacySection().deserialize(displayName);

        return Component.text("<", NamedTextColor.WHITE)
            .append(playerName)
            .append(Component.text("> ", NamedTextColor.WHITE))
            .append(messageBody);
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

        // Determine player type once — drives all gate decisions below.
        boolean isSpectator = !context.getGameState().isAlive(player.getUniqueId());

        // Gate: alive players during SWIPE phase are silenced via signs or holograms.
        // Spectators are intentionally exempted and fall through to the pipeline so
        // they can still communicate on the SPECTATOR channel.
        if (!isSpectator && context.getPhaseManager().getCurrentPhase() == GamePhase.SWIPE) {
            event.setCancelled(true);
            if (!gameManager.isSignModeEnabled()) {
                // No sign mode: show floating hologram above the player's head.
                String msg = PlainTextComponentSerializer.plainText().serialize(event.message());
                hologramManager.showTextAbove(player, msg, 100);
            }
            // Sign mode: alive players communicate via placed signs — no fallback.
            return;
        }

        // For all other cases (alive players outside SWIPE, spectators in any phase),
        // route through the chat pipeline.
        try {
            // isSpectator already computed above; invert for the ChatMessage flag.
            boolean isAlivePlayer = !isSpectator;

            // Create formatted message with player name prefix (supports legacy colour codes in nicks)
            Component formattedMessageWithName = buildFormattedMessageWithName(player, event.message());

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
                    if (pipelineResult.message().channel() != ChatChannel.GLOBAL) {
                        // Send to appropriate recipients based on channel
                        SessionChatHandler handler = gameManager.getChatPipelineManager()
                            .getOrCreateSessionHandler(context.getSessionName());

                        // Custom channel routing - cancel event and handle manually
                        event.setCancelled(true);
                        handler.deliverMessage(pipelineResult.message());
                    }
                    // For GLOBAL channel, let the event proceed normally using the event body
                }
                case DENY, CANCEL -> {
                    // Cancel the message
                    event.setCancelled(true);
                }
            }

        } catch (Exception e) {
            // On pipeline error: surface for diagnosis and let the event proceed.
            // SWIPE-phase cancellation already happened above the try, so the only
            // risk here is a transient global-leak for the offending message.
            gameManager.getPlugin().getLogger().warning(
                "Chat pipeline error for session '" + context.getSessionName()
                    + "' sender=" + player.getName() + ": " + e.getMessage());
        }
    }
}
