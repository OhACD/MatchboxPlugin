package com.ohacd.matchbox.game.session;

import java.util.*;

/**
 * Manages all game sessions.
 */
public class SessionManager {
    private final Map<String, GameSession> sessions = new HashMap<>();

    /**
     * Creates a new game session.
     */
    public GameSession createSession(String name) {
        if (sessions.containsKey(name.toLowerCase())) {
            return null; // Session already exists
        }
        GameSession session = new GameSession(name);
        sessions.put(name.toLowerCase(), session);
        return session;
    }

    /**
     * Gets a session by name (case-insensitive).
     */
    public GameSession getSession(String name) {
        return sessions.get(name.toLowerCase());
    }

    /**
     * Removes a session.
     */
    public boolean removeSession(String name) {
        return sessions.remove(name.toLowerCase()) != null;
    }

    /**
     * Gets all session names.
     */
    public Set<String> getAllSessionNames() {
        return new HashSet<>(sessions.keySet());
    }

    /**
     * Checks if a session exists.
     */
    public boolean sessionExists(String name) {
        return sessions.containsKey(name.toLowerCase());
    }

    /**
     * Gets all sessions.
     */
    public Collection<GameSession> getAllSessions() {
        return new ArrayList<>(sessions.values());
    }
}
