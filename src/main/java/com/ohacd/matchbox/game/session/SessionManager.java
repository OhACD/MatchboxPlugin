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
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String key = name.toLowerCase();
        if (sessions.containsKey(key)) {
            return null; // Session already exists
        }
        try {
            GameSession session = new GameSession(name);
            sessions.put(key, session);
            return session;
        } catch (IllegalArgumentException e) {
            return null; // Invalid session name
        }
    }

    /**
     * Gets a session by name (case-insensitive).
     */
    public GameSession getSession(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return sessions.get(name.toLowerCase());
    }

    /**
     * Removes a session.
     */
    public boolean removeSession(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return sessions.remove(name.toLowerCase()) != null;
    }

    /**
     * Gets all session names (case-preserved, not lowercase keys).
     */
    public Set<String> getAllSessionNames() {
        Set<String> names = new HashSet<>();
        for (GameSession session : sessions.values()) {
            if (session != null) {
                names.add(session.getName());
            }
        }
        return names;
    }

    /**
     * Checks if a session exists.
     */
    public boolean sessionExists(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return sessions.containsKey(name.toLowerCase());
    }

    /**
     * Gets all sessions.
     */
    public Collection<GameSession> getAllSessions() {
        return new ArrayList<>(sessions.values());
    }
}
