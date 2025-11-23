package com.ohacd.matchbox.game.vote;

import com.ohacd.matchbox.game.state.GameState;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Manages vote collection and tallying for the voting phase.
 */
public class VoteManager {
    private final GameState gameState;
    
    // Maps voter UUID -> voted target UUID
    private final Map<UUID, UUID> votes = new HashMap<>();
    
    // Maps target UUID -> vote count
    private final Map<UUID, Integer> voteCounts = new HashMap<>();
    
    public VoteManager(GameState gameState) {
        this.gameState = gameState;
    }

    /**
     * Registers a vote from a voter to a target.
     * Returns true if vote was registered, false if voter already voted or target is invalid.
     */
    public boolean registerVote(UUID voterId, UUID targetId) {
        if (voterId == null || targetId == null) {
            return false;
        }
        
        if (gameState == null) {
            return false;
        }
        
        // Check if voter is alive
        if (!gameState.isAlive(voterId)) {
            return false;
        }
        
        // Check if target is alive
        if (!gameState.isAlive(targetId)) {
            return false;
        }
        
        // Check if voter already voted
        if (votes.containsKey(voterId)) {
            return false;
        }
        
        // Cannot vote for yourself
        if (voterId.equals(targetId)) {
            return false;
        }
        
        // Register the vote
        votes.put(voterId, targetId);
        voteCounts.put(targetId, voteCounts.getOrDefault(targetId, 0) + 1);
        
        return true;
    }

    /**
     * Checks if a player has already voted.
     */
    public boolean hasVoted(UUID voterId) {
        return votes.containsKey(voterId);
    }

    /**
     * Gets the target a player voted for, or null if they haven't voted.
     */
    public UUID getVoteTarget(UUID voterId) {
        return votes.get(voterId);
    }

    /**
     * Gets the number of votes a target has received.
     */
    public int getVoteCount(UUID targetId) {
        return voteCounts.getOrDefault(targetId, 0);
    }

    /**
     * Gets all players who have voted.
     */
    public Set<UUID> getVoters() {
        return new HashSet<>(votes.keySet());
    }

    /**
     * Gets the UUID of the player with the most votes.
     * Returns null if there's a tie or no votes.
     */
    public UUID getMostVotedPlayer() {
        if (voteCounts == null || voteCounts.isEmpty()) {
            return null;
        }
        
        // Find maximum vote count
        int maxVotes = Collections.max(voteCounts.values());
        
        // Find all players with max votes
        List<UUID> topVoted = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : voteCounts.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue() == maxVotes) {
                topVoted.add(entry.getKey());
            }
        }
        
        // If tie, return null (caller should handle tie resolution)
        if (topVoted.size() > 1) {
            return null;
        }
        
        if (topVoted.isEmpty()) {
            return null;
        }
        
        return topVoted.get(0);
    }

    /**
     * Gets all players tied for most votes.
     * Returns empty list if no tie or no votes.
     */
    public List<UUID> getTiedPlayers() {
        if (voteCounts == null || voteCounts.isEmpty()) {
            return Collections.emptyList();
        }
        
        int maxVotes = Collections.max(voteCounts.values());
        List<UUID> tied = new ArrayList<>();
        
        for (Map.Entry<UUID, Integer> entry : voteCounts.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue() == maxVotes) {
                tied.add(entry.getKey());
            }
        }
        
        // Only return if there's actually a tie (2+ players)
        return tied.size() > 1 ? tied : Collections.emptyList();
    }

    /**
     * Gets the vote count for the most voted player.
     */
    public int getMaxVoteCount() {
        if (voteCounts == null || voteCounts.isEmpty()) {
            return 0;
        }
        return Collections.max(voteCounts.values());
    }

    /**
     * Clears all votes (for new voting round).
     */
    public void clearVotes() {
        votes.clear();
        voteCounts.clear();
    }

    /**
     * Gets a summary of all votes as a string for debugging.
     */
    public String getVoteSummary() {
        if (votes.isEmpty()) {
            return "No votes cast";
        }
        
        StringBuilder sb = new StringBuilder("Votes: ");
        for (Map.Entry<UUID, UUID> entry : votes.entrySet()) {
            Player voter = org.bukkit.Bukkit.getPlayer(entry.getKey());
            Player target = org.bukkit.Bukkit.getPlayer(entry.getValue());
            if (voter != null && target != null) {
                sb.append(voter.getName()).append("->").append(target.getName()).append(", ");
            }
        }
        return sb.toString();
    }
}

