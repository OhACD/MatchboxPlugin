package com.ohacd.matchbox.game.chat;

import com.ohacd.matchbox.game.GameManager;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression: {@code cleanupSession} must drop both processor lists AND the
 * cached {@link SessionChatHandler} instance — prior to 0.9.8 the handler
 * map leaked per ended session.
 */
class ChatPipelineCleanupTest {

    @Test
    @DisplayName("cleanupSession drops the cached SessionChatHandler instance")
    void cleanupSessionDropsCachedHandler() {
        Plugin plugin = mock(Plugin.class);
        GameManager gameManager = mock(GameManager.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());

        ChatPipelineManager manager = new ChatPipelineManager(plugin, gameManager);

        SessionChatHandler first = manager.getOrCreateSessionHandler("s1");
        assertThat(manager.getSessionHandler("s1")).isSameAs(first);

        manager.cleanupSession("s1");

        // Handler must be gone — getSessionHandler returns null and getOrCreate produces a fresh one.
        assertThat(manager.getSessionHandler("s1")).isNull();
        SessionChatHandler second = manager.getOrCreateSessionHandler("s1");
        assertThat(second).isNotSameAs(first);
    }
}
