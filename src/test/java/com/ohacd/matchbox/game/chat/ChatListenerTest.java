package com.ohacd.matchbox.game.chat;

import com.ohacd.matchbox.api.ChatChannel;
import com.ohacd.matchbox.api.ChatMessage;
import com.ohacd.matchbox.api.ChatProcessor;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.hologram.HologramManager;
import com.ohacd.matchbox.game.phase.PhaseManager;
import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.utils.GamePhase;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatListenerTest {

    @Test
    @DisplayName("Should preserve legacy color codes in nicked display names")
    void shouldPreserveLegacyColorCodesInDisplayName() {
        Player player = mock(Player.class);
        when(player.displayName()).thenReturn(LegacyComponentSerializer.legacySection().deserialize("§aGreenNick"));

        Component formatted = ChatListener.buildFormattedMessageWithName(player, Component.text("hello"));
        String legacy = LegacyComponentSerializer.legacySection().serialize(formatted);

        assertThat(legacy).contains("§aGreenNick");
        assertThat(legacy).contains("> hello");
    }

    @Test
    @DisplayName("Should not overwrite event message body for global channel")
    void shouldNotOverwriteEventMessageBodyForGlobalChannel() {
        HologramManager hologramManager = mock(HologramManager.class);
        GameManager gameManager = mock(GameManager.class);
        ChatPipelineManager pipelineManager = mock(ChatPipelineManager.class);

        SessionGameContext context = mock(SessionGameContext.class);
        GameState gameState = mock(GameState.class);
        PhaseManager phaseManager = mock(PhaseManager.class);

        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();

        AsyncChatEvent event = mock(AsyncChatEvent.class);

        when(event.isAsynchronous()).thenReturn(true);
        when(event.getPlayer()).thenReturn(player);
        when(event.message()).thenReturn(Component.text("hello"));
        when(event.originalMessage()).thenReturn(Component.text("hello"));

        when(player.getUniqueId()).thenReturn(playerId);
        when(player.displayName()).thenReturn(LegacyComponentSerializer.legacySection().deserialize("§aNick"));

        when(gameManager.getContextForPlayer(playerId)).thenReturn(context);
        when(gameManager.isSignModeEnabled()).thenReturn(false);
        when(gameManager.getChatPipelineManager()).thenReturn(pipelineManager);

        when(context.getGameState()).thenReturn(gameState);
        when(context.getPhaseManager()).thenReturn(phaseManager);
        when(context.getSessionName()).thenReturn("session-1");

        when(phaseManager.getCurrentPhase()).thenReturn(GamePhase.DISCUSSION);
        when(gameState.isAlive(playerId)).thenReturn(true);

        ChatMessage pipelineMessage = new ChatMessage(
            Component.text("hello"),
            Component.text("<Nick> hello"),
            player,
            ChatChannel.GLOBAL,
            "session-1",
            true
        );

        when(pipelineManager.processMessage(eq("session-1"), any(ChatMessage.class)))
            .thenReturn(ChatProcessor.ChatProcessingResult.allow(pipelineMessage));

        ChatListener listener = new ChatListener(hologramManager, gameManager);
        listener.onChat(event);

        verify(event, never()).message(any(Component.class));
        verify(event, never()).setCancelled(true);
    }
}
