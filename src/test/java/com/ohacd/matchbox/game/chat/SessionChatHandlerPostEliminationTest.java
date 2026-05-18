package com.ohacd.matchbox.game.chat;

import com.ohacd.matchbox.api.ChatChannel;
import com.ohacd.matchbox.api.ChatMessage;
import com.ohacd.matchbox.api.ChatProcessor;
import com.ohacd.matchbox.api.ChatResult;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.state.GameState;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression for the post-elimination chat leak. After a player is eliminated,
 * the very next message must route to the SPECTATOR channel — no per-handler
 * alive-cache is allowed to keep them on GAME (fixed in 0.9.8).
 */
class SessionChatHandlerPostEliminationTest {

    @Test
    @DisplayName("Eliminated player's next message routes to SPECTATOR (no stale alive cache)")
    void eliminatedPlayerRoutesToSpectatorImmediately() {
        Plugin plugin = mock(Plugin.class);
        SessionGameContext context = mock(SessionGameContext.class);
        GameState gameState = mock(GameState.class);

        when(context.getSessionName()).thenReturn("s1");
        when(context.getGameState()).thenReturn(gameState);

        UUID senderId = UUID.randomUUID();
        when(gameState.getAllParticipatingPlayerIds()).thenReturn(Set.of(senderId));

        Player sender = mock(Player.class);
        when(sender.getUniqueId()).thenReturn(senderId);
        ChatMessage message = new ChatMessage(
            Component.text("hello"),
            Component.text("<P> hello"),
            sender,
            ChatChannel.GAME,
            "s1",
            true
        );

        SessionChatHandler handler = new SessionChatHandler(context, plugin);

        // First message: player alive -> routes to GAME
        when(gameState.isAlive(senderId)).thenReturn(true);
        ChatProcessor.ChatProcessingResult first = handler.process(message);
        assertThat(first.result()).isEqualTo(ChatResult.ALLOW);
        assertThat(first.message().channel()).isEqualTo(ChatChannel.GAME);

        // Then they die. The very next message must read live state and route to SPECTATOR.
        when(gameState.isAlive(senderId)).thenReturn(false);
        ChatProcessor.ChatProcessingResult second = handler.process(message);
        assertThat(second.result()).isEqualTo(ChatResult.ALLOW);
        assertThat(second.message().channel()).isEqualTo(ChatChannel.SPECTATOR);
    }
}
