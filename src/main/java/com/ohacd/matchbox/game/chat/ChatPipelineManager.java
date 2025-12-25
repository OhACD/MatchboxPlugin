package com.ohacd.matchbox.game.chat;

import com.ohacd.matchbox.api.ChatChannel;
import com.ohacd.matchbox.api.ChatMessage;
import com.ohacd.matchbox.api.ChatProcessor;
import com.ohacd.matchbox.api.ChatResult;
import com.ohacd.matchbox.game.GameManager;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages chat processing pipeline for all game sessions.
 * Handles registration of custom chat processors and coordinates message routing.
 *
 * <p>This class is responsible for:</p>
 * <ul>
 *   <li>Session-scoped chat processor management</li>
 *   <li>Processor registration/unregistration</li>
 *   <li>Cleanup when sessions end</li>
 *   <li>Thread-safe operations</li>
 * </ul>
 */
public class ChatPipelineManager {

    private final Plugin plugin;
    private final GameManager gameManager;

    // Session name -> List of processors for that session
    private final Map<String, List<ChatProcessor>> sessionProcessors = new ConcurrentHashMap<>();

    // Session name -> Default session chat handler
    private final Map<String, SessionChatHandler> sessionHandlers = new ConcurrentHashMap<>();

    public ChatPipelineManager(@NotNull Plugin plugin, @NotNull GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    /**
     * Registers a chat processor for a specific session.
     * Processors are called in registration order.
     *
     * @param sessionName the session name
     * @param processor the processor to register
     * @throws IllegalArgumentException if sessionName or processor is null
     */
    public void registerProcessor(@NotNull String sessionName, @NotNull ChatProcessor processor) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Session name cannot be null or empty");
        }
        if (processor == null) {
            throw new IllegalArgumentException("Chat processor cannot be null");
        }

        sessionProcessors.computeIfAbsent(sessionName, k -> new CopyOnWriteArrayList<>()).add(processor);
        plugin.getLogger().info("Registered chat processor for session '" + sessionName + "'");
    }

    /**
     * Unregisters a specific chat processor from a session.
     *
     * @param sessionName the session name
     * @param processor the processor to remove
     * @return true if the processor was removed, false if not found
     */
    public boolean unregisterProcessor(@NotNull String sessionName, @NotNull ChatProcessor processor) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            return false;
        }
        if (processor == null) {
            return false;
        }

        List<ChatProcessor> processors = sessionProcessors.get(sessionName);
        if (processors == null) {
            return false;
        }

        boolean removed = processors.remove(processor);
        if (removed) {
            plugin.getLogger().info("Unregistered chat processor from session '" + sessionName + "'");
            // Clean up empty lists
            if (processors.isEmpty()) {
                sessionProcessors.remove(sessionName);
            }
        }
        return removed;
    }

    /**
     * Clears all chat processors for a specific session.
     *
     * @param sessionName the session name
     */
    public void clearProcessors(@NotNull String sessionName) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            return;
        }

        List<ChatProcessor> removed = sessionProcessors.remove(sessionName);
        if (removed != null && !removed.isEmpty()) {
            plugin.getLogger().info("Cleared " + removed.size() + " chat processors from session '" + sessionName + "'");
        }
    }

    /**
     * Gets all registered processors for a session.
     * Returns an empty list if no processors are registered.
     *
     * @param sessionName the session name
     * @return unmodifiable list of processors for the session
     */
    @NotNull
    public List<ChatProcessor> getProcessors(@NotNull String sessionName) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatProcessor> processors = sessionProcessors.get(sessionName);
        return processors != null ? Collections.unmodifiableList(processors) : Collections.emptyList();
    }

    /**
     * Cleans up resources for a session when it ends.
     * Should be called when a game session terminates.
     *
     * @param sessionName the session name to clean up
     */
    public void cleanupSession(@NotNull String sessionName) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            return;
        }

        clearProcessors(sessionName);
        plugin.getLogger().info("Cleaned up chat pipeline for session '" + sessionName + "'");
    }

    /**
     * Gets all active session names that have registered processors.
     *
     * @return set of session names with processors
     */
    @NotNull
    public Set<String> getActiveSessions() {
        return new HashSet<>(sessionProcessors.keySet());
    }

    /**
     * Gets or creates the default chat handler for a session.
     * This handler implements the core spectator isolation logic.
     *
     * @param sessionName the session name
     * @return the session chat handler
     */
    @NotNull
    public SessionChatHandler getOrCreateSessionHandler(@NotNull String sessionName) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Session name cannot be null or empty");
        }

        return sessionHandlers.computeIfAbsent(sessionName, name ->
            new SessionChatHandler(name, gameManager, plugin));
    }

    /**
     * Gets the session handler for a session, or null if not created.
     *
     * @param sessionName the session name
     * @return the session handler or null
     */
    @Nullable
    public SessionChatHandler getSessionHandler(@NotNull String sessionName) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            return null;
        }
        return sessionHandlers.get(sessionName);
    }

    /**
     * Processes a chat message through the pipeline for a session.
     * Applies custom processors first, then the default session handler.
     *
     * @param sessionName the session name
     * @param message the message to process
     * @return the final processing result
     */
    @NotNull
    public ChatProcessor.ChatProcessingResult processMessage(@NotNull String sessionName, @NotNull ChatMessage message) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Session name cannot be null or empty");
        }
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }

        // Apply custom processors first
        List<ChatProcessor> processors = getProcessors(sessionName);
        ChatMessage currentMessage = message;

        for (ChatProcessor processor : processors) {
            try {
                var result = processor.process(currentMessage);
                switch (result.result()) {
                    case DENY -> {
                        return ChatProcessor.ChatProcessingResult.deny(currentMessage);
                    }
                    case CANCEL -> {
                        return ChatProcessor.ChatProcessingResult.cancel(currentMessage);
                    }
                    case ALLOW -> {
                        currentMessage = result.message(); // May be modified
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error in chat processor for session '" + sessionName + "': " + e.getMessage());
                // Continue with other processors
            }
        }

        // Apply default session handler
        SessionChatHandler handler = getOrCreateSessionHandler(sessionName);
        return handler.process(currentMessage);
    }

    /**
     * Emergency cleanup - clears all processors for all sessions.
     * Should only be called on plugin disable.
     */
    public void emergencyCleanup() {
        int totalProcessors = sessionProcessors.values().stream().mapToInt(List::size).sum();
        sessionProcessors.clear();
        sessionHandlers.clear();
        plugin.getLogger().info("Emergency cleanup: cleared " + totalProcessors + " chat processors across all sessions");
    }
}
