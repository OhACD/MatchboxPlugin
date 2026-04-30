package com.ohacd.matchbox.api;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of a session's structured log timeline.
 *
 * @since 0.9.7
 */
public final class GameSessionLog {
    private final String sessionName;
    private final List<GameLogEntry> entries;

    /**
     * Creates an immutable session log snapshot.
     *
     * @param sessionName owning session name
     * @param entries log entries in timeline order
     */
    public GameSessionLog(@NotNull String sessionName, @NotNull List<GameLogEntry> entries) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionName cannot be null or empty");
        }
        if (entries == null) {
            throw new IllegalArgumentException("entries cannot be null");
        }
        this.sessionName = sessionName;
        this.entries = Collections.unmodifiableList(List.copyOf(entries));
    }

    /**
     * Returns the owning session name.
     *
     * @return session name
     */
    @NotNull
    public String getSessionName() {
        return sessionName;
    }

    /**
     * Returns the immutable log entries.
     *
     * @return session log entries
     */
    @NotNull
    public List<GameLogEntry> getEntries() {
        return entries;
    }

    /**
     * Returns the number of entries in this snapshot.
     *
     * @return entry count
     */
    public int size() {
        return entries.size();
    }
}
