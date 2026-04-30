package com.ohacd.matchbox.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable log entry for a single session flow event.
 *
 * @param timestamp time the event was recorded
 * @param sessionName owning session name
 * @param category event category
 * @param message human-readable event message
 * @param actorId acting player, when applicable
 * @param targetId target player, when applicable
 * @param attributes structured event attributes
 *
 * @since 0.9.7
 */
public record GameLogEntry(
    @NotNull Instant timestamp,
    @NotNull String sessionName,
    @NotNull String category,
    @NotNull String message,
    @Nullable UUID actorId,
    @Nullable UUID targetId,
    @NotNull Map<String, String> attributes
) {
    /**
     * Creates a validated immutable log entry.
     */
    public GameLogEntry {
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp cannot be null");
        }
        if (sessionName == null || sessionName.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionName cannot be null or empty");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("category cannot be null or empty");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("message cannot be null or empty");
        }

        attributes = attributes == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new HashMap<>(attributes));
    }
}
