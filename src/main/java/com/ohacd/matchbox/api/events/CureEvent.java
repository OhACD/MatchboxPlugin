package com.ohacd.matchbox.api.events;

import com.ohacd.matchbox.api.MatchboxEvent;
import com.ohacd.matchbox.api.MatchboxEventListener;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a cure action is performed (Medic cures an infected player).
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public class CureEvent extends MatchboxEvent {
    
    private final String sessionName;
    private final Player medic;
    private final Player target;
    private final boolean realInfection;
    
    /**
     * Creates a new cure event.
     * 
     * @param sessionName the session name
     * @param medic the player performing the cure
     * @param target the player being cured
     * @param realInfection whether the target had a real infection (false if it was delusion)
     */
    public CureEvent(@NotNull String sessionName, @NotNull Player medic, @NotNull Player target, boolean realInfection) {
        this.sessionName = sessionName;
        this.medic = medic;
        this.target = target;
        this.realInfection = realInfection;
    }
    
    @Override
    public void dispatch(@NotNull MatchboxEventListener listener) {
        listener.onCure(this);
    }
    
    /**
     * Gets the name of the session where the cure occurred.
     * 
     * @return the session name
     */
    public String getSessionName() {
        return sessionName;
    }
    
    /**
     * Gets the player who performed the cure.
     * 
     * @return the medic
     */
    public Player getMedic() {
        return medic;
    }
    
    /**
     * Gets the player who was cured.
     * 
     * @return the target
     */
    public Player getTarget() {
        return target;
    }
    
    /**
     * Gets whether the target had a real infection.
     * 
     * @return true if the target was actually infected, false if it was a delusion
     */
    public boolean isRealInfection() {
        return realInfection;
    }
}
