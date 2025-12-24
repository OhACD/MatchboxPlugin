package com.ohacd.matchbox.api;

import org.jetbrains.annotations.NotNull;

/**
 * Interface for custom chat processors that can modify, filter, or reroute chat messages.
 * Servers can implement this interface to add custom chat behavior for specific sessions.
 *
 * <p>Processors are called in registration order and can:</p>
 * <ul>
 *   <li>Modify message content or formatting</li>
 *   <li>Change the target channel</li>
 *   <li>Filter messages (deny/cancel)</li>
 *   <li>Add custom routing logic</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * public class CustomChatProcessor implements ChatProcessor {
 *     public ChatProcessingResult process(ChatMessage message) {
 *         // Add custom prefix for spectators
 *         if (message.channel() == ChatChannel.SPECTATOR) {
 *             Component newMessage = Component.text("[SPEC] ").append(message.formattedMessage());
 *             return ChatProcessingResult.allowModified(message.withFormattedMessage(newMessage));
 *         }
 *         return ChatProcessingResult.allow(message);
 *     }
 * }
 * </pre>
 *
 * @since 0.9.5
 * @author Matchbox Team
 */
public interface ChatProcessor {

    /**
     * Processes a chat message and returns the result.
     * This method is called for every message in the associated session.
     *
     * @param message the chat message to process (immutable)
     * @return the result of processing, including any modified message
     */
    @NotNull
    ChatProcessingResult process(@NotNull ChatMessage message);

    /**
     * Result of chat processing with optional modified message.
     *
     * @param result the processing result enum
     * @param message the (possibly modified) message
     */
    record ChatProcessingResult(@NotNull ChatResult result, @NotNull ChatMessage message) {

        /**
         * Creates an ALLOW result with the original message.
         *
         * @param message original chat message
         * @return a ChatProcessingResult indicating ALLOW with the provided message
         */
        public static ChatProcessingResult allow(@NotNull ChatMessage message) {
            return new ChatProcessingResult(ChatResult.ALLOW, message);
        }

        /**
         * Creates an ALLOW result with a modified message.
         *
         * @param modifiedMessage modified chat message
         * @return a ChatProcessingResult indicating ALLOW with the modified message
         */
        public static ChatProcessingResult allowModified(@NotNull ChatMessage modifiedMessage) {
            return new ChatProcessingResult(ChatResult.ALLOW, modifiedMessage);
        }

        /**
         * Creates a DENY result.
         *
         * @param message original chat message
         * @return a ChatProcessingResult indicating DENY
         */
        public static ChatProcessingResult deny(@NotNull ChatMessage message) {
            return new ChatProcessingResult(ChatResult.DENY, message);
        }

        /**
         * Creates a CANCEL result.
         *
         * @param message original chat message
         * @return a ChatProcessingResult indicating CANCEL
         */
        public static ChatProcessingResult cancel(@NotNull ChatMessage message) {
            return new ChatProcessingResult(ChatResult.CANCEL, message);
        }
    }
}
