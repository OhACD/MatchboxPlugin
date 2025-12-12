package com.ohacd.matchbox.game.vote;

import com.ohacd.matchbox.game.config.ConfigManager;

/**
 * Calculates dynamic voting thresholds based on alive player count.
 * Uses logarithmic scaling between key points:
 * - 20 players: 20% threshold
 * - 7 players: 30% threshold
 * - 3 players and below: 50% threshold
 */
public class DynamicVotingThreshold {
    private final ConfigManager configManager;
    
    // Key points for threshold calculation
    private static final int MAX_PLAYERS = 20;
    private static final int MID_PLAYERS = 7;
    private static final int MIN_PLAYERS = 3;
    
    // Default threshold percentages (can be overridden by config)
    private static final double DEFAULT_THRESHOLD_20 = 0.20;  // 20%
    private static final double DEFAULT_THRESHOLD_7 = 0.30;   // 30%
    private static final double DEFAULT_THRESHOLD_3 = 0.50;   // 50%
    
    // Penalty system defaults
    private static final double DEFAULT_PENALTY_PER_PHASE = 0.0333; // ~3.33% per phase (10% over 3 phases)
    private static final int DEFAULT_MAX_PENALTY_PHASES = 3;
    private static final double DEFAULT_MAX_PENALTY = 0.10; // 10% max penalty
    
    public DynamicVotingThreshold(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    /**
     * Calculates the base threshold percentage for a given number of alive players.
     * Uses logarithmic interpolation between key points.
     */
    public double calculateBaseThreshold(int alivePlayerCount) {
        if (alivePlayerCount < 2) {
            // Game should end before this, but handle edge case
            return 1.0; // 100% threshold (impossible)
        }
        
        if (alivePlayerCount > MAX_PLAYERS) {
            // Cap at max players
            return getThresholdAt20Players();
        }
        
        // Get config values
        double threshold20 = getThresholdAt20Players();
        double threshold7 = getThresholdAt7Players();
        double threshold3 = getThresholdAt3Players();
        
        if (alivePlayerCount <= MIN_PLAYERS) {
            // 3 players or less: use 50% threshold
            return threshold3;
        } else if (alivePlayerCount <= MID_PLAYERS) {
            // Between 4 and 7 players: logarithmic interpolation between 30% and 50%
            return logarithmicInterpolation(
                alivePlayerCount,
                MIN_PLAYERS, threshold3,
                MID_PLAYERS, threshold7
            );
        } else {
            // Between 8 and 20 players: logarithmic interpolation between 20% and 30%
            return logarithmicInterpolation(
                alivePlayerCount,
                MID_PLAYERS, threshold7,
                MAX_PLAYERS, threshold20
            );
        }
    }
    
    /**
     * Calculates the effective threshold after applying penalty for consecutive no-elimination phases.
     */
    public double calculateEffectiveThreshold(int alivePlayerCount, int consecutiveNoEliminationPhases) {
        double baseThreshold = calculateBaseThreshold(alivePlayerCount);
        
        if (consecutiveNoEliminationPhases <= 0) {
            return baseThreshold;
        }
        
        // Calculate penalty
        double penaltyPerPhase = getPenaltyPerPhase();
        int maxPenaltyPhases = getMaxPenaltyPhases();
        double maxPenalty = getMaxPenalty();
        
        int effectivePhases = Math.min(consecutiveNoEliminationPhases, maxPenaltyPhases);
        double penalty = effectivePhases * penaltyPerPhase;
        
        // Cap penalty at max
        penalty = Math.min(penalty, maxPenalty);
        
        // Apply penalty (reduce threshold)
        double effectiveThreshold = baseThreshold - penalty;
        
        // Ensure threshold doesn't go below 0 or above 1
        return Math.max(0.0, Math.min(1.0, effectiveThreshold));
    }
    
    /**
     * Performs logarithmic interpolation between two points.
     * Uses natural logarithm for smooth scaling.
     */
    private double logarithmicInterpolation(double x, double x1, double y1, double x2, double y2) {
        if (x <= x1) return y1;
        if (x >= x2) return y2;
        
        // Logarithmic interpolation formula
        // Using log scale: y = y1 + (y2 - y1) * (log(x) - log(x1)) / (log(x2) - log(x1))
        double logX = Math.log(x);
        double logX1 = Math.log(x1);
        double logX2 = Math.log(x2);
        
        if (Math.abs(logX2 - logX1) < 0.0001) {
            // Avoid division by zero
            return y1;
        }
        
        double ratio = (logX - logX1) / (logX2 - logX1);
        return y1 + (y2 - y1) * ratio;
    }
    
    /**
     * Checks if the vote count meets the threshold for elimination.
     */
    public boolean meetsThreshold(int voteCount, int alivePlayerCount, int consecutiveNoEliminationPhases) {
        if (alivePlayerCount <= 0) {
            return false;
        }
        
        double threshold = calculateEffectiveThreshold(alivePlayerCount, consecutiveNoEliminationPhases);
        double requiredVotes = alivePlayerCount * threshold;
        
        // Round up to nearest integer (ceiling)
        int requiredVotesInt = (int) Math.ceil(requiredVotes);
        
        return voteCount >= requiredVotesInt;
    }
    
    /**
     * Gets the minimum vote count required for elimination.
     */
    public int getRequiredVoteCount(int alivePlayerCount, int consecutiveNoEliminationPhases) {
        if (alivePlayerCount <= 0) {
            return Integer.MAX_VALUE; // Impossible
        }
        
        double threshold = calculateEffectiveThreshold(alivePlayerCount, consecutiveNoEliminationPhases);
        double requiredVotes = alivePlayerCount * threshold;
        
        // Round up to nearest integer (ceiling)
        return (int) Math.ceil(requiredVotes);
    }
    
    // Config getters with defaults
    private double getThresholdAt20Players() {
        return configManager.getVotingThresholdAt20Players();
    }
    
    private double getThresholdAt7Players() {
        return configManager.getVotingThresholdAt7Players();
    }
    
    private double getThresholdAt3Players() {
        return configManager.getVotingThresholdAt3Players();
    }
    
    private double getPenaltyPerPhase() {
        return configManager.getVotingPenaltyPerPhase();
    }
    
    private int getMaxPenaltyPhases() {
        return configManager.getVotingMaxPenaltyPhases();
    }
    
    private double getMaxPenalty() {
        return configManager.getVotingMaxPenalty();
    }
}

