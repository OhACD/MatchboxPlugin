package com.ohacd.matchbox.api.events;

import com.ohacd.matchbox.api.MatchboxEvent;
import com.ohacd.matchbox.api.MatchboxEventListener;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player leaves a game session.
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public class PlayerLeaveEvent extends MatchboxEvent {
    
    private final String sessionName;
    private final Player player;
    private final LeaveReason reason;
    
    /**
     * Reasons why a player can leave a session.
     */
    public enum LeaveReason {
        /** Player voluntarily left the game */
        VOLUNTARY,
        /** Player was eliminated from the game */
        ELIMINATED,
        /** Player disconnected from the server */
        DISCONNECTED,
        /** Player was removed by admin */
        KICKED,
        /** Other reasons */
        OTHER
    }
    
    /**
     * Creates a new player leave event.
     * 
     * @param sessionName the session name
     * @param player the player who left
     * @param reason the reason for leaving
     */
    public PlayerLeaveEvent(@NotNull String sessionName, @NotNull Player player, @NotNull LeaveReason reason) {
        this.sessionName = sessionName;
        this.player = player;
        this.reason = reason;
    }
    
    @Override
    public void dispatch(@NotNull MatchboxEventListener listener) {
        listener.onPlayerLeave(this);
    }
    
    /**
     * Gets the name of the session the player left.
     * 
     * @return the session name
     */
    public @NotNull String getSessionName() {
        return sessionName;
    }
    
    /**
     * Gets the player who left the session.
     * 
     * @return the player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Gets the reason why the player left.
     * 
     * @return the leave reason
     */
    @NotNull
    public LeaveReason getReason() {
        return reason;
    }
}
