package com.ohacd.matchbox.api;

/**
 * Result of processing a chat message through the pipeline.
 * Determines how the message should be handled after custom processing.
 *
 * @since 0.9.5
 * @author Matchbox Team
 */
public enum ChatResult {
    /**
     * Allow the message to proceed through the normal routing.
     * The message may have been modified by the processor.
     */
    ALLOW,

    /**
     * Deny the message - it will not be sent to any recipients.
     * Useful for filtering spam, muted players, etc.
     */
    DENY,

    /**
     * Cancel the message entirely - prevents any further processing.
     * Similar to DENY but stops the pipeline immediately.
     */
    CANCEL
}
