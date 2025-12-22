package com.ohacd.matchbox.api.events;

import com.ohacd.matchbox.api.MatchboxEvent;
import com.ohacd.matchbox.api.MatchboxEventListener;
import org.bukkit.entity.Player;

/**
 * Event fired when a player casts a vote during the voting phase.
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public class PlayerVoteEvent extends MatchboxEvent {
    
    private final String sessionName;
    private final Player voter;
    private final Player target;
    
    /**
     * Creates a new player vote event.
     * 
     * @param sessionName the session name
     * @param voter the player who voted
     * @param target the player who was voted for
     */
    public PlayerVoteEvent(String sessionName, Player voter, Player target) {
        this.sessionName = sessionName;
        this.voter = voter;
        this.target = target;
    }
    
    @Override
    public void dispatch(MatchboxEventListener listener) {
        listener.onPlayerVote(this);
    }
    
    /**
     * Gets the name of the session where the vote occurred.
     * 
     * @return the session name
     */
    public String getSessionName() {
        return sessionName;
    }
    
    /**
     * Gets the player who cast the vote.
     * 
     * @return the voter
     */
    public Player getVoter() {
        return voter;
    }
    
    /**
     * Gets the player who was voted for.
     * 
     * @return the voted target
     */
    public Player getTarget() {
        return target;
    }
}
