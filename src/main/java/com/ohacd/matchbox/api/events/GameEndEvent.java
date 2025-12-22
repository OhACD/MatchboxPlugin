package com.ohacd.matchbox.api.events;

import com.ohacd.matchbox.api.MatchboxEvent;
import com.ohacd.matchbox.api.MatchboxEventListener;
import com.ohacd.matchbox.game.utils.Role;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;

/**
 * Event fired when a game ends (either by win condition or manual termination).
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public class GameEndEvent extends MatchboxEvent {
    
    private final String sessionName;
    private final EndReason reason;
    private final Collection<Player> remainingPlayers;
    private final Map<Player, Role> finalRoles;
    private final int totalRounds;
    
    /**
     * Reasons why a game can end.
     */
    public enum EndReason {
        /** Spark won (all innocents eliminated) */
        SPARK_WIN,
        /** Innocents won (spark voted out) */
        INNOCENTS_WIN,
        /** Game was ended manually by admin */
        MANUAL_END,
        /** Game ended due to lack of players */
        INSUFFICIENT_PLAYERS,
        /** Other reasons */
        OTHER
    }
    
    /**
     * Creates a new game end event.
     * 
     * @param sessionName session name
     * @param reason reason for game ending
     * @param remainingPlayers players still in the game when it ended
     * @param finalRoles mapping of players to their final roles
     * @param totalRounds total number of rounds played
     */
    public GameEndEvent(String sessionName, EndReason reason, Collection<Player> remainingPlayers, 
                     Map<Player, Role> finalRoles, int totalRounds) {
        this.sessionName = sessionName;
        this.reason = reason;
        this.remainingPlayers = remainingPlayers;
        this.finalRoles = finalRoles;
        this.totalRounds = totalRounds;
    }
    
    @Override
    public void dispatch(MatchboxEventListener listener) {
        listener.onGameEnd(this);
    }
    
    /**
     * Gets the name of the session that ended.
     * 
     * @return the session name
     */
    public String getSessionName() {
        return sessionName;
    }
    
    /**
     * Gets the reason why the game ended.
     * 
     * @return the end reason
     */
    public EndReason getReason() {
        return reason;
    }
    
    /**
     * Gets all players still in the game when it ended.
     * 
     * @return collection of remaining players
     */
    public Collection<Player> getRemainingPlayers() {
        return remainingPlayers;
    }
    
    /**
     * Gets the final roles of all players who participated.
     * 
     * @return mapping of players to their final roles
     */
    public Map<Player, Role> getFinalRoles() {
        return finalRoles;
    }
    
    /**
     * Gets the total number of rounds played.
     * 
     * @return total rounds
     */
    public int getTotalRounds() {
        return totalRounds;
    }
}
