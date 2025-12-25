package com.ohacd.matchbox.game.state;

import com.ohacd.matchbox.game.ability.MedicSecondaryAbility;
import com.ohacd.matchbox.game.ability.SparkSecondaryAbility;
import com.ohacd.matchbox.game.utils.Role;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Manages the current game state including player roles, alive players, and round-specific tracking.
 */
public class GameState {
    private final Map<UUID, Role> roles = new HashMap<>();
    private final Set<UUID> alivePlayers = new HashSet<>();
    private final Set<UUID> swipedThisRound = new HashSet<>();
    private final Set<UUID> curedThisRound = new HashSet<>();
    // NEW: 0.8.7 - Tracks players who have been cured this round, but not yet removed from alive players list
    private final Set<UUID> beenCuredThisRound = new HashSet<>();
    private final Set<UUID> usedHealingSightThisRound = new HashSet<>();
    private final Set<UUID> usedHunterVisionThisRound = new HashSet<>();
    private final Set<UUID> usedSparkSwapThisRound = new HashSet<>();
    private final Set<UUID> usedDelusionThisRound = new HashSet<>();
    private final Set<UUID> infectedThisRound = new HashSet<>();
    private final Set<UUID> delusionInfectedThisRound = new HashSet<>();
    private final Map<UUID, Long> pendingDeathTime = new HashMap<>();
    private final Set<UUID> allParticipatingPlayers = new HashSet<>();
    private SparkSecondaryAbility sparkSecondaryAbility = SparkSecondaryAbility.HUNTER_VISION;
    private MedicSecondaryAbility medicSecondaryAbility = MedicSecondaryAbility.HEALING_SIGHT;
    private String activeSessionName = null;
    private int currentRound = 0;

    /**
     * Clears all state for a new game (not just round).
     * This should only be called when starting a brand new game or ending a game.
     */
    public void clearGameState() {
        roles.clear();
        alivePlayers.clear();
        swipedThisRound.clear();
        curedThisRound.clear();
        beenCuredThisRound.clear();
        infectedThisRound.clear();
        usedHealingSightThisRound.clear();
        usedHunterVisionThisRound.clear();
        usedSparkSwapThisRound.clear();
        usedDelusionThisRound.clear();
        delusionInfectedThisRound.clear();
        pendingDeathTime.clear();
        allParticipatingPlayers.clear();
        activeSessionName = null;
        currentRound = 0;
    }

    /**
     * Clears only per-round state (swipes, cures, infections).
     * Keeps player list, roles, and session intact.
     * This should be called at the start of each new round.
     */
    public void clearRoundState() {
        swipedThisRound.clear();
        curedThisRound.clear();
        beenCuredThisRound.clear();
        infectedThisRound.clear();
        usedHealingSightThisRound.clear();
        usedHunterVisionThisRound.clear();
        usedSparkSwapThisRound.clear();
        usedDelusionThisRound.clear();
        delusionInfectedThisRound.clear();
        sparkSecondaryAbility = SparkSecondaryAbility.HUNTER_VISION;
        medicSecondaryAbility = MedicSecondaryAbility.HEALING_SIGHT;
    }

    /**
     * Increments the round counter.
     */
    public void incrementRound() {
        currentRound++;
    }

    /**
     * Gets the current round number.
     */
    public int getCurrentRound() {
        return currentRound;
    }

    /**
     * Adds a player to the alive players set.
     */
    public void addAlivePlayer(Player player) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (uuid != null) {
            alivePlayers.add(uuid);
            allParticipatingPlayers.add(uuid);
        }
    }

    /**
     * Adds multiple players to the alive players set.
     */
    public void addAlivePlayers(Collection<Player> players) {
        if (players == null) {
            return;
        }
        for (Player player : players) {
            if (player != null) {
                addAlivePlayer(player);
            }
        }
    }

    /**
     * Removes a player from the alive players set.
     * Does not remove them from allParticipatingPlayers.
     */
    public void removeAlivePlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }
        alivePlayers.remove(playerId);
        // If they had pending death or infected flags, keep pending handling separate.
        infectedThisRound.remove(playerId);
        pendingDeathTime.remove(playerId);
        swipedThisRound.remove(playerId);
        curedThisRound.remove(playerId);
        beenCuredThisRound.remove(playerId);
    }

    /**
     * Checks if a player is alive.
     */
    public boolean isAlive(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        return alivePlayers.contains(playerId);
    }

    /**
     * Gets the role of a player.
     */
    public Role getRole(UUID playerId) {
        return roles.get(playerId);
    }

    /**
     * Sets the role of a player.
     */
    public void setRole(UUID playerId, Role role) {
        if (playerId == null || role == null) {
            return;
        }
        roles.put(playerId, role);
    }

    /**
     * Gets all alive player UUIDs.
     */
    public Set<UUID> getAlivePlayerIds() {
        return new HashSet<>(alivePlayers); // Defensive copy
    }

    /**
     * Gets the count of alive players.
     */
    public int getAlivePlayerCount() {
        return alivePlayers.size();
    }

    /**
     * Marks that a player has swiped this round.
     */
    public void markSwiped(UUID playerId) {
        swipedThisRound.add(playerId);
    }

    /**
     * Checks if a player has swiped this round.
     */
    public boolean hasSwipedThisRound(UUID playerId) {
        return swipedThisRound.contains(playerId);
    }

    /**
     * Marks that a player has cured this round.
     */
    public void markCured(UUID playerId) {
        curedThisRound.add(playerId);
    }

    /**
     * Checks if a player has cured this round.
     */
    public boolean hasCuredThisRound(UUID playerId) {
        return curedThisRound.contains(playerId);
    }

    /**
     * NEW: 0.8.7 - Marks that a player has been cured this round, but not yet removed from an alive players list.
     */
    public void markBeenCured(UUID playerId) {
        beenCuredThisRound.add(playerId);
    }
    /**
     * NEW: 0.8.7 - Checks if a player has been cured this round.
     */
    public boolean hasBeenCuredThisRound(UUID playerId) { return beenCuredThisRound.contains(playerId); }

    /**
     * Marks that a player was infected (swiped successfully) this round.
     */
    public void markInfected(UUID playerId) {
        infectedThisRound.add(playerId);
    }

    /**
     * Checks if a player was infected this round.
     */
    public boolean wasInfectedThisRound(UUID playerId) {
        return infectedThisRound.contains(playerId);
    }

    /**
     * Clears infected flags for the round.
     */
    public void clearInfectedThisRound() {
        infectedThisRound.clear();
    }

    /**
     * Marks that a player has used healing sight this round.
     */
    public void markUsedHealingSight(UUID playerId) {
        usedHealingSightThisRound.add(playerId);
    }

    /**
     * Checks if a player has used healing sight this round.
     */
    public boolean hasUsedHealingSightThisRound(UUID playerId) {
        return usedHealingSightThisRound.contains(playerId);
    }

    /**
     * Marks that a player has used hunter vision this round.
     */
    public void markUsedHunterVision(UUID playerId) {
        usedHunterVisionThisRound.add(playerId);
    }

    /**
     * Checks if a player has used hunter vision this round.
     */
    public boolean hasUsedHunterVisionThisRound(UUID playerId) {
        return usedHunterVisionThisRound.contains(playerId);
    }

    /**
     * Marks that a player has used the Spark Swap ability this round.
     */
    public void markUsedSparkSwap(UUID playerId) {
        usedSparkSwapThisRound.add(playerId);
    }

    /**
     * Checks if a player has already used Spark Swap this round.
     */
    public boolean hasUsedSparkSwapThisRound(UUID playerId) {
        return usedSparkSwapThisRound.contains(playerId);
    }

    /**
     * Marks that a player has used the Delusion ability this round.
     */
    public void markUsedDelusion(UUID playerId) {
        usedDelusionThisRound.add(playerId);
    }

    /**
     * Checks if a player has already used Delusion this round.
     */
    public boolean hasUsedDelusionThisRound(UUID playerId) {
        return usedDelusionThisRound.contains(playerId);
    }

    /**
     * Marks that a player was infected with delusion (fake infection) this round.
     */
    public void markDelusionInfected(UUID playerId) {
        delusionInfectedThisRound.add(playerId);
    }

    /**
     * Checks if a player was infected with delusion this round.
     */
    public boolean isDelusionInfected(UUID playerId) {
        return delusionInfectedThisRound.contains(playerId);
    }

    /**
     * Removes delusion infection from a player (when cured).
     */
    public void removeDelusionInfection(UUID playerId) {
        delusionInfectedThisRound.remove(playerId);
    }

    public SparkSecondaryAbility getSparkSecondaryAbility() {
        return sparkSecondaryAbility;
    }

    public void setSparkSecondaryAbility(SparkSecondaryAbility sparkSecondaryAbility) {
        if (sparkSecondaryAbility != null) {
            this.sparkSecondaryAbility = sparkSecondaryAbility;
        }
    }

    public MedicSecondaryAbility getMedicSecondaryAbility() {
        return medicSecondaryAbility;
    }

    public void setMedicSecondaryAbility(MedicSecondaryAbility medicSecondaryAbility) {
        if (medicSecondaryAbility != null) {
            this.medicSecondaryAbility = medicSecondaryAbility;
        }
    }

    /**
     * Schedules a pending death for a player at the given epoch millis.
     * Use removePendingDeath to cancel (e.g. cured).
     */
    public void setPendingDeath(UUID playerId, long epochMillis) {
        pendingDeathTime.put(playerId, epochMillis);
    }

    /**
     * Removes pending death for a player (e.g. cured).
     */
    public void removePendingDeath(UUID playerId) {
        pendingDeathTime.remove(playerId);
    }

    /**
     * Removes a player from the beenCuredThisRound list
     */
    public void removeBeenCuredThisRound(UUID victimId) { beenCuredThisRound.remove(victimId);}

    /**
     * Checks if a player currently has a pending death scheduled.
     */
    public boolean hasPendingDeath(UUID playerId) {
        return pendingDeathTime.containsKey(playerId);
    }

    /**
     * Gets the scheduled pending death time for a player (epoch millis), or null if none.
     */
    public Long getPendingDeathTime(UUID playerId) {
        return pendingDeathTime.get(playerId);
    }

    /**
     * Returns a snapshot of player UUIDs whose pending death time is {@code <=} provided epoch millis.
     * Useful for processing due pending deaths.
     */
    public Set<UUID> getPendingDeathsDueAt(long epochMillis) {
        Set<UUID> due = new HashSet<>();
        for (Map.Entry<UUID, Long> e : pendingDeathTime.entrySet()) {
            if (e.getValue() <= epochMillis) {
                due.add(e.getKey());
            }
        }
        return due;
    }

    /**
     * Gets the UUID of the Spark player.
     */
    public UUID getSparkUUID() {
        return roles.entrySet().stream()
                .filter(entry -> entry.getValue() == Role.SPARK)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * Counts the number of alive innocents.
     */
    public long countAliveInnocents() {
        return alivePlayers.stream()
                .filter(uuid -> roles.get(uuid) == Role.INNOCENT)
                .count();
    }

    /**
     * Gets all participating player UUIDs (including eliminated ones).
     */
    public Set<UUID> getAllParticipatingPlayerIds() {
        return new HashSet<>(allParticipatingPlayers);
    }

    /**
     * Sets the active session name.
     */
    public void setActiveSessionName(String sessionName) {
        this.activeSessionName = sessionName;
    }

    /**
     * Gets the active session name.
     */
    public String getActiveSessionName() {
        return activeSessionName;
    }

    /**
     * Checks if there's an active game.
     */
    public boolean isGameActive() {
        return !allParticipatingPlayers.isEmpty();
    }

    /**
     * Validates that the game state is consistent.
     * Returns true if state is valid, false otherwise.
     */
    public boolean validateState() {
        if (alivePlayers == null || allParticipatingPlayers == null || roles == null) {
            return false; // Null collections indicate corruption
        }
        
        // All alive players must be in participating players
        for (UUID alivePlayer : alivePlayers) {
            if (alivePlayer == null) {
                return false; // Null UUIDs indicate corruption
            }
            if (!allParticipatingPlayers.contains(alivePlayer)) {
                return false;
            }
        }

        // All players with roles must be in participating players
        for (UUID playerWithRole : roles.keySet()) {
            if (playerWithRole == null) {
                return false; // Null UUIDs indicate corruption
            }
            if (!allParticipatingPlayers.contains(playerWithRole)) {
                return false;
            }
        }

        // If there are participating players, there must be at least one role assigned
        if (!allParticipatingPlayers.isEmpty() && roles.isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Gets a debug string representation of the current state.
     */
    public String getDebugInfo() {
        return String.format(
                "GameState[Round=%d, Session=%s, Participating=%d, Alive=%d, Roles=%d, Swiped=%d, Cured=%d, Infected=%d, Fake-Infected=%d, Pending=%d, beenCured=%d]",
                currentRound,
                activeSessionName != null ? activeSessionName : "none",
                allParticipatingPlayers.size(),
                alivePlayers.size(),
                roles.size(),
                swipedThisRound.size(),
                curedThisRound.size(),
                infectedThisRound.size(),
                delusionInfectedThisRound.size(),
                pendingDeathTime.size(),
                beenCuredThisRound.size()
        );
    }
}