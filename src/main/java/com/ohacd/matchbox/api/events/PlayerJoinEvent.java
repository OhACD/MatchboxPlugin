package com.ohacd.matchbox.api.events;

import com.ohacd.matchbox.api.MatchboxEvent;
import com.ohacd.matchbox.api.MatchboxEventListener;
import org.bukkit.entity.Player;

/**
 * Event fired when a player joins a game session.
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public class PlayerJoinEvent extends MatchboxEvent {
    
    private final String sessionName;
    private final Player player;
    
    /**
     * Creates a new player join event.
     * 
     * @param sessionName the session name
     * @param player the player who joined
     */
    public PlayerJoinEvent(String sessionName, Player player) {
        this.sessionName = sessionName;
        this.player = player;
    }
    
    @Override
    public void dispatch(MatchboxEventListener listener) {
        listener.onPlayerJoin(this);
    }
    
    /**
     * Gets the name of the session the player joined.
     * 
     * @return the session name
     */
    public String getSessionName() {
        return sessionName;
    }
    
    /**
     * Gets the player who joined the session.
     * 
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }
}
