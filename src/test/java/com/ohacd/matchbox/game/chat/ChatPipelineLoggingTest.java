package com.ohacd.matchbox.game.chat;

import com.ohacd.matchbox.api.ChatChannel;
import com.ohacd.matchbox.api.ChatMessage;
import com.ohacd.matchbox.api.ChatProcessor;
import com.ohacd.matchbox.api.ChatResult;
import com.ohacd.matchbox.game.GameManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatPipelineLoggingTest {

    @Test
    @DisplayName("Should log deny processor outcomes to game manager")
    void shouldLogDenyProcessorOutcomes() {
        Plugin plugin = mock(Plugin.class);
        GameManager gameManager = mock(GameManager.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());

        ChatPipelineManager manager = new ChatPipelineManager(plugin, gameManager);
        manager.registerProcessor("s1", msg -> ChatProcessor.ChatProcessingResult.deny(msg));

        Player sender = mock(Player.class);
        UUID senderId = UUID.randomUUID();
        when(sender.getUniqueId()).thenReturn(senderId);
        when(sender.getName()).thenReturn("Sender");

        ChatMessage message = new ChatMessage(
            Component.text("hello"),
            Component.text("<Sender> hello"),
            sender,
            ChatChannel.GAME,
            "s1",
            true
        );

        var result = manager.processMessage("s1", message);

        assertThat(result.result()).isEqualTo(ChatResult.DENY);
        verify(gameManager).logChatMessage(anyString(), org.mockito.ArgumentMatchers.eq(senderId), org.mockito.ArgumentMatchers.eq("Sender"), org.mockito.ArgumentMatchers.eq("GAME"), anyString());
    }
}
