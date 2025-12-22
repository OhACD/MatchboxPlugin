package com.ohacd.matchbox.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Result object for session creation operations that provides detailed success/failure information.
 * 
 * <p>This class enhances error reporting compared to simple Optional returns,
 * allowing developers to understand exactly why a session creation failed.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * SessionCreationResult result = MatchboxAPI.createSessionBuilder("arena1")
 *     .withPlayers(players)
 *     .withSpawnPoints(spawns)
 *     .startWithResult();
 * 
 * if (result.isSuccess()) {
 *     ApiGameSession session = result.getSession();
 *     // Use session
 * } else {
 *     SessionCreationResult.ErrorType error = result.getErrorType();
 *     String message = result.getErrorMessage();
 *     logger.warning("Failed to create session: " + error + " - " + message);
 * }
 * }</pre>
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public final class SessionCreationResult {
    
    /**
     * Enumeration of possible error types during session creation.
     */
    public enum ErrorType {
        /** No valid players were provided */
        NO_PLAYERS("No valid online players specified"),
        
        /** No valid spawn points were provided */
        NO_SPAWN_POINTS("No valid spawn locations specified"),
        
        /** A session with the given name already exists */
        SESSION_EXISTS("A session with this name already exists"),
        
        /** The plugin instance is not available */
        PLUGIN_NOT_AVAILABLE("Matchbox plugin is not available"),
        
        /** Session manager is not available */
        SESSION_MANAGER_NOT_AVAILABLE("Session manager is not available"),
        
        /** Game manager is not available */
        GAME_MANAGER_NOT_AVAILABLE("Game manager is not available"),
        
        /** Discussion location is invalid */
        INVALID_DISCUSSION_LOCATION("Discussion location is invalid"),
        
        /** Internal error during session creation */
        INTERNAL_ERROR("Internal error occurred during session creation");
        
        private final String defaultMessage;
        
        ErrorType(String defaultMessage) {
            this.defaultMessage = defaultMessage;
        }
        
        public String getDefaultMessage() {
            return defaultMessage;
        }
    }
    
    private final ApiGameSession session;
    private final ErrorType errorType;
    private final String errorMessage;
    private final boolean success;
    
    private SessionCreationResult(ApiGameSession session, ErrorType errorType, String errorMessage) {
        this.session = session;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.success = session != null;
    }
    
    /**
     * Creates a successful result.
     * 
     * @param session the created session
     * @return a successful result
     */
    @NotNull
    public static SessionCreationResult success(@NotNull ApiGameSession session) {
        return new SessionCreationResult(session, null, null);
    }
    
    /**
     * Creates a failure result.
     * 
     * @param errorType the type of error that occurred
     * @param errorMessage detailed error message (can be null for default message)
     * @return a failure result
     */
    @NotNull
    public static SessionCreationResult failure(@NotNull ErrorType errorType, @Nullable String errorMessage) {
        String message = errorMessage != null ? errorMessage : errorType.getDefaultMessage();
        return new SessionCreationResult(null, errorType, message);
    }
    
    /**
     * Gets whether the session creation was successful.
     * 
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Gets whether the session creation failed.
     * 
     * @return true if failed, false otherwise
     */
    public boolean isFailure() {
        return !success;
    }
    
    /**
     * Gets the created session if successful.
     * 
     * @return Optional containing the session if successful, empty otherwise
     */
    @NotNull
    public Optional<ApiGameSession> getSession() {
        return Optional.ofNullable(session);
    }
    
    /**
     * Gets the error type if the creation failed.
     * 
     * @return Optional containing the error type if failed, empty otherwise
     */
    @NotNull
    public Optional<ErrorType> getErrorType() {
        return Optional.ofNullable(errorType);
    }
    
    /**
     * Gets the error message if the creation failed.
     * 
     * @return Optional containing the error message if failed, empty otherwise
     */
    @NotNull
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }
    
    /**
     * Converts this result to the legacy Optional format for backward compatibility.
     * 
     * @return Optional containing the session if successful, empty otherwise
     * @deprecated Use {@link #getSession()} for more detailed information
     */
    @Deprecated
    @NotNull
    public Optional<ApiGameSession> toOptional() {
        return getSession();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        SessionCreationResult that = (SessionCreationResult) obj;
        return success == that.success &&
               (session != null ? session.equals(that.session) : that.session == null) &&
               errorType == that.errorType &&
               (errorMessage != null ? errorMessage.equals(that.errorMessage) : that.errorMessage == null);
    }
    
    @Override
    public int hashCode() {
        int result = session != null ? session.hashCode() : 0;
        result = 31 * result + (errorType != null ? errorType.hashCode() : 0);
        result = 31 * result + (errorMessage != null ? errorMessage.hashCode() : 0);
        result = 31 * result + (success ? 1 : 0);
        return result;
    }
    
    @Override
    public String toString() {
        if (success) {
            return "SessionCreationResult{success=true, session=" + session + "}";
        } else {
            return "SessionCreationResult{success=false, errorType=" + errorType + 
                   ", errorMessage='" + errorMessage + "'}";
        }
    }
}
