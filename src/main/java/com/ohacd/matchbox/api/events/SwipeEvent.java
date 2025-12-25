package com.ohacd.matchbox.api.events;

import com.ohacd.matchbox.api.MatchboxEvent;
import com.ohacd.matchbox.api.MatchboxEventListener;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when the swipe action is performed (Spark attacks another player).
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public class SwipeEvent extends MatchboxEvent {
    
    private final String sessionName;
    private final Player attacker;
    private final Player victim;
    private final boolean successful;
    
    /**
     * Creates a new swipe event.
     * 
     * @param sessionName the session name
     * @param attacker the player performing the swipe (should be Spark)
     * @param victim the player being attacked
     * @param successful whether the swipe was successful (not blocked/cured)
     */
    public SwipeEvent(@NotNull String sessionName, @NotNull Player attacker, @NotNull Player victim, boolean successful) {
        this.sessionName = sessionName;
        this.attacker = attacker;
        this.victim = victim;
        this.successful = successful;
    }
    
    @Override
    public void dispatch(@NotNull MatchboxEventListener listener) {
        listener.onSwipe(this);
    }
    
    /**
     * Gets the name of the session where the swipe occurred.
     * 
     * @return the session name
     */
    @NotNull
    public String getSessionName() {
        return sessionName;
    }
    
    /**
     * Gets the player who performed the swipe attack.
     * 
     * @return the attacker
     */
    @NotNull
    public Player getAttacker() {
        return attacker;
    }
    
    /**
     * Gets the player who was attacked.
     * 
     * @return the victim
     */
    @NotNull
    public Player getVictim() {
        return victim;
    }
    
    /**
     * Gets whether the swipe was successful.
     * 
     * @return true if the swipe infected the target, false if it was blocked or cured
     */
    public boolean isSuccessful() {
        return successful;
    }
}
