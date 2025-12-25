package com.ohacd.matchbox.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * Utility class for validating common API inputs and providing helpful error messages.
 * 
 * <p>This class contains static methods to validate common configurations and provide
 * detailed feedback about what went wrong during validation failures.</p>
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public final class ApiValidationHelper {
    
    private ApiValidationHelper() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Validates a collection of players for session creation.
     * 
     * @param players the players to validate
     * @return ValidationResult containing validation outcome
     */
    @NotNull
    public static ValidationResult validatePlayers(@Nullable Collection<Player> players) {
        if (players == null || players.isEmpty()) {
            return ValidationResult.error("No players specified");
        }
        
        long onlineCount = players.stream()
                .filter(p -> p != null && p.isOnline())
                .count();
        
        if (onlineCount == 0) {
            return ValidationResult.error("No valid online players specified");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validates a collection of spawn locations for session creation.
     * 
     * @param spawnPoints the spawn locations to validate
     * @return ValidationResult containing validation outcome
     */
    @NotNull
    public static ValidationResult validateSpawnPoints(@Nullable Collection<Location> spawnPoints) {
        if (spawnPoints == null || spawnPoints.isEmpty()) {
            return ValidationResult.error("No spawn points specified");
        }
        
        long validCount = spawnPoints.stream()
                .filter(loc -> loc != null && loc.getWorld() != null)
                .count();
        
        if (validCount == 0) {
            return ValidationResult.error("No valid spawn locations specified");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validates a discussion location for session creation.
     * 
     * @param discussionLocation the discussion location to validate
     * @return ValidationResult containing validation outcome
     */
    @NotNull
    public static ValidationResult validateDiscussionLocation(@Nullable Location discussionLocation) {
        if (discussionLocation != null && discussionLocation.getWorld() == null) {
            return ValidationResult.error("Invalid discussion location");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validates seat locations for session creation.
     * 
     * @param seatLocations the seat locations to validate
     * @return ValidationResult containing validation outcome
     */
    @NotNull
    public static ValidationResult validateSeatLocations(@Nullable Map<Integer, Location> seatLocations) {
        if (seatLocations == null || seatLocations.isEmpty()) {
            return ValidationResult.success(); // Empty seat locations are valid
        }
        
        boolean hasInvalid = seatLocations.values().stream()
                .anyMatch(loc -> loc == null || loc.getWorld() == null);
        
        if (hasInvalid) {
            return ValidationResult.error("Invalid seat locations detected");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validates a session name.
     * 
     * @param sessionName the session name to validate
     * @return ValidationResult containing validation outcome
     */
    @NotNull
    public static ValidationResult validateSessionName(@Nullable String sessionName) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            return ValidationResult.error("Session name cannot be null or empty");
        }
        
        if (sessionName.length() > 32) {
            return ValidationResult.error("Session name too long (max 32 characters)");
        }
        
        if (!sessionName.matches("^[a-zA-Z0-9_]+$")) {
            return ValidationResult.error("Session name can only contain letters, numbers, and underscores");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validates that the number of players is sufficient for a game.
     * 
     * @param playerCount the number of players
     * @return ValidationResult containing validation outcome
     */
    @NotNull
    public static ValidationResult validatePlayerCount(int playerCount) {
        if (playerCount < 2) {
            return ValidationResult.error("Insufficient players (minimum 2 required)");
        }
        
        if (playerCount > 20) {
            return ValidationResult.error("Too many players (maximum 20 allowed)");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validates that the number of spawn points is sufficient for players.
     * 
     * @param spawnCount the number of spawn points
     * @param playerCount the number of players
     * @return ValidationResult containing validation outcome
     */
    @NotNull
    public static ValidationResult validateSpawnCount(int spawnCount, int playerCount) {
        if (spawnCount < playerCount) {
            return ValidationResult.error("Insufficient spawn points (need at least one per player)");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Gets a summary of validation results.
     * 
     * @param results the validation results to summarize
     * @return a human-readable summary
     */
    @NotNull
    public static String getValidationSummary(@NotNull ValidationResult... results) {
        StringBuilder summary = new StringBuilder();
        boolean hasErrors = false;
        
        for (ValidationResult result : results) {
            if (!result.isValid()) {
                if (hasErrors) {
                    summary.append(", ");
                }
                summary.append(result.getErrorMessage());
                hasErrors = true;
            }
        }
        
        if (!hasErrors) {
            return "All validations passed";
        }
        
        return "Validation errors: " + summary.toString();
    }
    
    /**
     * Simple result class for validation operations.
     */
    public static final class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        /**
         * Creates a successful validation result.
         * 
         * @return a successful result
         */
        @NotNull
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        /**
         * Creates an error validation result.
         * 
         * @param errorMessage the error message
         * @return an error result
         */
        @NotNull
        public static ValidationResult error(@NotNull String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        /**
         * Gets whether the validation was successful.
         * 
         * @return true if valid, false otherwise
         */
        public boolean isValid() {
            return valid;
        }
        
        /**
         * Gets the error message if validation failed.
         * 
         * @return error message, or null if validation succeeded
         */
        @Nullable
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
