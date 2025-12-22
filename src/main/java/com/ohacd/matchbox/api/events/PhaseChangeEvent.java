package com.ohacd.matchbox.api.events;

import com.ohacd.matchbox.api.MatchboxEvent;
import com.ohacd.matchbox.api.MatchboxEventListener;
import com.ohacd.matchbox.game.utils.GamePhase;

/**
 * Event fired when the game phase changes.
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public class PhaseChangeEvent extends MatchboxEvent {
    
    private final String sessionName;
    private final GamePhase fromPhase;
    private final GamePhase toPhase;
    private final int currentRound;
    
    /**
     * Creates a new phase change event.
     * 
     * @param sessionName the session name
     * @param fromPhase the previous phase
     * @param toPhase the new phase
     * @param currentRound the current round number
     */
    public PhaseChangeEvent(String sessionName, GamePhase fromPhase, GamePhase toPhase, int currentRound) {
        this.sessionName = sessionName;
        this.fromPhase = fromPhase;
        this.toPhase = toPhase;
        this.currentRound = currentRound;
    }
    
    @Override
    public void dispatch(MatchboxEventListener listener) {
        listener.onPhaseChange(this);
    }
    
    /**
     * Gets the name of the session where the phase changed.
     * 
     * @return the session name
     */
    public String getSessionName() {
        return sessionName;
    }
    
    /**
     * Gets the previous game phase.
     * 
     * @return the previous phase
     */
    public GamePhase getFromPhase() {
        return fromPhase;
    }
    
    /**
     * Gets the new game phase.
     * 
     * @return the new phase
     */
    public GamePhase getToPhase() {
        return toPhase;
    }
    
    /**
     * Gets the current round number.
     * 
     * @return the current round
     */
    public int getCurrentRound() {
        return currentRound;
    }
}
