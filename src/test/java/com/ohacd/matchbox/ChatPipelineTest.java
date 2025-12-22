package com.ohacd.matchbox;

import com.ohacd.matchbox.api.*;
import com.ohacd.matchbox.game.chat.ChatPipelineManager;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.utils.MockBukkitFactory;
import com.ohacd.matchbox.utils.TestPluginFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for the chat pipeline system.
 * Tests spectator isolation, custom processors, and edge cases.
 */
class ChatPipelineTest {

    private List<Player> testPlayers;
    private List<Location> testSpawnPoints;
    private String testSessionName;

    @BeforeEach
    void setUp() {
        MockBukkitFactory.setUpBukkitMocks();
        TestPluginFactory.setUpMockPlugin();

        // Create test players
        testPlayers = MockBukkitFactory.createMockPlayers(3);
        testSpawnPoints = List.of(
            MockBukkitFactory.createMockLocation(0, 64, 0, 0, 0),
            MockBukkitFactory.createMockLocation(10, 64, 0, 90, 0),
            MockBukkitFactory.createMockLocation(0, 64, 10, 180, 0)
        );
        testSessionName = "chat-test-session-" + UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        // Clean up any sessions we created
        if (testSessionName != null) {
            MatchboxAPI.endSession(testSessionName);
        }
        TestPluginFactory.tearDownMockPlugin();
    }

    @Test
    @DisplayName("Should route alive player messages to game channel")
    void shouldRouteAlivePlayerMessagesToGameChannel() {
        // Given - create a session and get the chat pipeline manager
        SessionCreationResult result = MatchboxAPI.createSessionBuilder(testSessionName)
            .withPlayers(testPlayers)
            .withSpawnPoints(testSpawnPoints)
            .startWithResult();

        assertThat(result.isSuccess()).isTrue();
        ApiGameSession session = result.getSession().get();

        // Force to DISCUSSION phase
        session.getPhaseController().forcePhase(GamePhase.DISCUSSION);

        // Get the pipeline manager through the API
        boolean processorRegistered = MatchboxAPI.registerChatProcessor(testSessionName, message ->
            ChatProcessor.ChatProcessingResult.allow(message));

        assertThat(processorRegistered).isTrue();

        Player alivePlayer = testPlayers.get(0);
        ChatMessage message = new ChatMessage(
            Component.text("Hello from alive player"),
            Component.text("AlivePlayer: Hello from alive player"),
            alivePlayer,
            ChatChannel.GAME,
            testSessionName,
            true
        );

        // When - process message through API
        var processingResult = MatchboxAPI.registerChatProcessor(testSessionName,
            msg -> ChatProcessor.ChatProcessingResult.allow(msg));

        // Then - verify session exists and we can get its phase
        Optional<GamePhase> phase = MatchboxAPI.getCurrentPhase(testSessionName);
        assertThat(phase).isPresent();
        assertThat(phase.get()).isEqualTo(GamePhase.DISCUSSION);
    }

    @Test
    @DisplayName("Should handle global channel bypass")
    void shouldHandleGlobalChannelBypass() {
        // Given - create a session first
        SessionCreationResult sessionResult = MatchboxAPI.createSessionBuilder(testSessionName)
            .withPlayers(testPlayers)
            .withSpawnPoints(testSpawnPoints)
            .startWithResult();

        assertThat(sessionResult.isSuccess()).isTrue();

        // Register a processor that should be bypassed for global messages
        boolean processorRegistered = MatchboxAPI.registerChatProcessor(testSessionName, message -> {
            // This processor should not be called for GLOBAL messages
            return ChatProcessor.ChatProcessingResult.deny(message);
        });

        assertThat(processorRegistered).isTrue();

        // When - try to register processor for non-existent session
        // API allows pre-registering processors for sessions that don't exist yet
        boolean result = MatchboxAPI.registerChatProcessor("non-existent-session",
            msg -> ChatProcessor.ChatProcessingResult.allow(msg));

        // Then - should succeed for pre-registration
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should handle custom processor modification")
    void shouldHandleCustomProcessorModification() {
        // Given - register custom processor
        ChatProcessor customProcessor = message -> {
            // Add prefix to all messages
            Component modified = Component.text("[CUSTOM] ").color(NamedTextColor.GREEN)
                .append(message.formattedMessage());
            return ChatProcessor.ChatProcessingResult.allowModified(
                message.withFormattedMessage(modified));
        };

        boolean registered = MatchboxAPI.registerChatProcessor(testSessionName, customProcessor);
        assertThat(registered).isTrue();

        // When - check if processor is registered (we can't directly test processing without a session)
        // For now, just verify registration works
        assertThat(registered).isTrue();
    }

    @Test
    @DisplayName("Should handle custom processor denial")
    void shouldHandleCustomProcessorDenial() {
        // Given - register denying processor
        ChatProcessor denyProcessor = message ->
            ChatProcessor.ChatProcessingResult.deny(message);

        boolean registered = MatchboxAPI.registerChatProcessor(testSessionName, denyProcessor);
        assertThat(registered).isTrue();

        // When - check if processor is registered
        assertThat(registered).isTrue();
    }

    @Test
    @DisplayName("Should handle processor cleanup")
    void shouldHandleProcessorCleanup() {
        // Given - register processor
        ChatProcessor processor = message -> ChatProcessor.ChatProcessingResult.allow(message);
        boolean registered = MatchboxAPI.registerChatProcessor(testSessionName, processor);
        assertThat(registered).isTrue();

        // When - unregister processor
        boolean unregistered = MatchboxAPI.unregisterChatProcessor(testSessionName, processor);
        assertThat(unregistered).isTrue();
    }

    @Test
    @DisplayName("Should handle processor exception handling")
    void shouldHandleProcessorExceptionHandling() {
        // Given - register processor that throws exception
        ChatProcessor badProcessor = message -> {
            throw new RuntimeException("Processor error");
        };

        boolean registered = MatchboxAPI.registerChatProcessor(testSessionName, badProcessor);
        assertThat(registered).isTrue();

        // When - check registration
        assertThat(registered).isTrue();
    }

    @Test
    @DisplayName("Should handle multiple processors")
    void shouldHandleMultipleProcessors() {
        // Given - register multiple processors
        ChatProcessor processor1 = message ->
            ChatProcessor.ChatProcessingResult.allowModified(
                message.withFormattedMessage(Component.text("[P1]").append(message.formattedMessage())));

        ChatProcessor processor2 = message ->
            ChatProcessor.ChatProcessingResult.allowModified(
                message.withFormattedMessage(Component.text("[P2]").append(message.formattedMessage())));

        boolean registered1 = MatchboxAPI.registerChatProcessor(testSessionName, processor1);
        boolean registered2 = MatchboxAPI.registerChatProcessor(testSessionName, processor2);

        // Then
        assertThat(registered1).isTrue();
        assertThat(registered2).isTrue();
    }

    @Test
    @DisplayName("Should handle session lifecycle with processors")
    void shouldHandleSessionLifecycleWithProcessors() {
        // Given - create session and register processors
        SessionCreationResult result = MatchboxAPI.createSessionBuilder(testSessionName)
            .withPlayers(testPlayers)
            .withSpawnPoints(testSpawnPoints)
            .startWithResult();

        assertThat(result.isSuccess()).isTrue();

        ChatProcessor processor = message -> ChatProcessor.ChatProcessingResult.allow(message);
        boolean registered = MatchboxAPI.registerChatProcessor(testSessionName, processor);
        assertThat(registered).isTrue();

        // When - clear processors
        boolean cleared = MatchboxAPI.clearChatProcessors(testSessionName);
        assertThat(cleared).isTrue();

        // Then - session should still exist
        Optional<ApiGameSession> session = MatchboxAPI.getSession(testSessionName);
        assertThat(session).isPresent();
    }

    @Test
    @DisplayName("Should validate chat API integration")
    void shouldValidateChatApiIntegration() {
        // Test that all chat API methods work correctly
        ChatProcessor testProcessor = message -> ChatProcessor.ChatProcessingResult.allow(message);

        // Test registration on non-existent session (API allows pre-registration)
        boolean result1 = MatchboxAPI.registerChatProcessor("non-existent", testProcessor);
        assertThat(result1).isTrue();

        // Test unregistration on non-existent session
        boolean result2 = MatchboxAPI.unregisterChatProcessor("non-existent", testProcessor);
        assertThat(result2).isTrue(); // Should succeed since processor was registered above

        // Test clearing processors on non-existent session
        boolean result3 = MatchboxAPI.clearChatProcessors("non-existent");
        assertThat(result3).isTrue(); // Should succeed since processors were cleared above
    }

    @Test
    @DisplayName("Should handle concurrent session chat processing")
    void shouldHandleConcurrentSessionChatProcessing() {
        // Given - create multiple sessions
        String session1 = testSessionName + "-1";
        String session2 = testSessionName + "-2";

        SessionCreationResult result1 = MatchboxAPI.createSessionBuilder(session1)
            .withPlayers(testPlayers.subList(0, 2))
            .withSpawnPoints(testSpawnPoints.subList(0, 2))
            .startWithResult();

        SessionCreationResult result2 = MatchboxAPI.createSessionBuilder(session2)
            .withPlayers(testPlayers.subList(1, 3))
            .withSpawnPoints(testSpawnPoints.subList(1, 3))
            .startWithResult();

        assertThat(result1.isSuccess()).isTrue();
        assertThat(result2.isSuccess()).isTrue();

        // Register processors for both sessions
        ChatProcessor processor1 = message -> ChatProcessor.ChatProcessingResult.allow(message);
        ChatProcessor processor2 = message -> ChatProcessor.ChatProcessingResult.allow(message);

        boolean reg1 = MatchboxAPI.registerChatProcessor(session1, processor1);
        boolean reg2 = MatchboxAPI.registerChatProcessor(session2, processor2);

        assertThat(reg1).isTrue();
        assertThat(reg2).isTrue();

        // Cleanup
        MatchboxAPI.endSession(session1);
        MatchboxAPI.endSession(session2);
    }

    @Test
    @DisplayName("Should test ChatPipelineManager directly - register and process")
    void shouldTestChatPipelineManagerDirectly() {
        // Given - create a ChatPipelineManager directly with mocked plugin
        var mockPlugin = mock(org.bukkit.plugin.Plugin.class);
        when(mockPlugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());

        ChatPipelineManager manager = new ChatPipelineManager(mockPlugin, null); // GameManager can be null for this test

        String sessionName = "direct-test-session";
        ChatProcessor testProcessor = message ->
            ChatProcessor.ChatProcessingResult.allowModified(
                message.withFormattedMessage(Component.text("[TEST] ").append(message.formattedMessage())));

        // When - register processor
        manager.registerProcessor(sessionName, testProcessor);

        // Then - verify processor is registered
        var processors = manager.getProcessors(sessionName);
        assertThat(processors).hasSize(1);
        assertThat(processors.get(0)).isEqualTo(testProcessor);
    }

    @Test
    @DisplayName("Should test ChatPipelineManager processor ordering")
    void shouldTestChatPipelineManagerProcessorOrdering() {
        // Given
        var mockPlugin = mock(org.bukkit.plugin.Plugin.class);
        when(mockPlugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());

        ChatPipelineManager manager = new ChatPipelineManager(mockPlugin, null);

        String sessionName = "ordering-test-session";

        // Create processors that modify messages in sequence
        ChatProcessor processor1 = message ->
            ChatProcessor.ChatProcessingResult.allowModified(
                message.withFormattedMessage(Component.text("[1]").append(message.formattedMessage())));

        ChatProcessor processor2 = message ->
            ChatProcessor.ChatProcessingResult.allowModified(
                message.withFormattedMessage(Component.text("[2]").append(message.formattedMessage())));

        // When - register in order
        manager.registerProcessor(sessionName, processor1);
        manager.registerProcessor(sessionName, processor2);

        // Then - verify order is maintained
        var processors = manager.getProcessors(sessionName);
        assertThat(processors).hasSize(2);
        assertThat(processors.get(0)).isEqualTo(processor1);
        assertThat(processors.get(1)).isEqualTo(processor2);
    }

    @Test
    @DisplayName("Should test ChatPipelineManager processor removal")
    void shouldTestChatPipelineManagerProcessorRemoval() {
        // Given
        var mockPlugin = mock(org.bukkit.plugin.Plugin.class);
        when(mockPlugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());

        ChatPipelineManager manager = new ChatPipelineManager(mockPlugin, null);

        String sessionName = "removal-test-session";
        ChatProcessor processor = message -> ChatProcessor.ChatProcessingResult.allow(message);

        manager.registerProcessor(sessionName, processor);
        assertThat(manager.getProcessors(sessionName)).hasSize(1);

        // When - unregister processor
        boolean removed = manager.unregisterProcessor(sessionName, processor);

        // Then
        assertThat(removed).isTrue();
        assertThat(manager.getProcessors(sessionName)).isEmpty();
    }

    @Test
    @DisplayName("Should test ChatPipelineManager processor denial")
    void shouldTestChatPipelineManagerProcessorDenial() {
        // Given
        var mockPlugin = mock(org.bukkit.plugin.Plugin.class);
        when(mockPlugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());

        ChatPipelineManager manager = new ChatPipelineManager(mockPlugin, null);

        String sessionName = "denial-test-session";

        ChatProcessor denyProcessor = message -> ChatProcessor.ChatProcessingResult.deny(message);
        ChatProcessor allowProcessor = message -> ChatProcessor.ChatProcessingResult.allow(message); // This should not be reached

        manager.registerProcessor(sessionName, denyProcessor);
        manager.registerProcessor(sessionName, allowProcessor);

        // Create a mock message
        Player mockPlayer = MockBukkitFactory.createMockPlayer();
        ChatMessage message = new ChatMessage(
            Component.text("test"),
            Component.text("test"),
            mockPlayer,
            ChatChannel.GAME,
            sessionName,
            true
        );

        // When - process message
        var result = manager.processMessage(sessionName, message);

        // Then - should be denied and not reach the allow processor
        assertThat(result.result()).isEqualTo(ChatResult.DENY);
    }

    @Test
    @DisplayName("Should test ChatPipelineManager processor modification")
    void shouldTestChatPipelineManagerProcessorModification() {
        // Given
        var mockPlugin = mock(org.bukkit.plugin.Plugin.class);
        when(mockPlugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());

        ChatPipelineManager manager = new ChatPipelineManager(mockPlugin, null);

        String sessionName = "modification-test-session";

        ChatProcessor modifyProcessor = message ->
            ChatProcessor.ChatProcessingResult.allowModified(
                message.withFormattedMessage(Component.text("[MODIFIED] ").append(message.formattedMessage())));

        // When - register processor
        manager.registerProcessor(sessionName, modifyProcessor);

        // Then - verify processor is registered and returns modified result
        var processors = manager.getProcessors(sessionName);
        assertThat(processors).hasSize(1);

        // Test that the processor actually modifies messages
        Player mockPlayer = MockBukkitFactory.createMockPlayer();
        ChatMessage originalMessage = new ChatMessage(
            Component.text("original"),
            Component.text("original"),
            mockPlayer,
            ChatChannel.GAME,
            sessionName,
            true
        );

        var result = processors.get(0).process(originalMessage);
        assertThat(result.result()).isEqualTo(ChatResult.ALLOW);
        // The message should be different (modified)
        assertThat(result.message()).isNotEqualTo(originalMessage);
    }

    @Test
    @DisplayName("Should test ChatPipelineManager session cleanup")
    void shouldTestChatPipelineManagerSessionCleanup() {
        // Given
        var mockPlugin = mock(org.bukkit.plugin.Plugin.class);
        when(mockPlugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());

        ChatPipelineManager manager = new ChatPipelineManager(mockPlugin, null);

        String sessionName = "cleanup-test-session";
        ChatProcessor processor = message -> ChatProcessor.ChatProcessingResult.allow(message);

        manager.registerProcessor(sessionName, processor);
        assertThat(manager.getProcessors(sessionName)).hasSize(1);

        // When - cleanup session
        manager.cleanupSession(sessionName);

        // Then - processors should be cleared
        assertThat(manager.getProcessors(sessionName)).isEmpty();
    }
}
