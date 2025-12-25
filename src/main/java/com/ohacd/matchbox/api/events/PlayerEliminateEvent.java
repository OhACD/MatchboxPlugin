package com.ohacd.matchbox.api.events;

import com.ohacd.matchbox.api.MatchboxEvent;
import com.ohacd.matchbox.api.MatchboxEventListener;
import com.ohacd.matchbox.game.utils.Role;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when a player is eliminated from the game.
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public class PlayerEliminateEvent extends MatchboxEvent {
    
    private final String sessionName;
    private final Player eliminatedPlayer;
    private final Role role;
    private final EliminationReason reason;
    
    /**
     * Reasons why a player can be eliminated.
     */
    public enum EliminationReason {
        /** Player was voted out during voting phase */
        VOTED_OUT,
        /** Player was killed by a Spark */
        KILLED_BY_SPARK,
        /** Player left the game voluntarily */
        LEFT_GAME,
        /** Player was disconnected */
        DISCONNECTED,
        /** Other reasons */
        OTHER
    }
    
    /**
     * Creates a new player elimination event.
     * 
     * @param sessionName the session where elimination occurred
     * @param eliminatedPlayer the player who was eliminated
     * @param role the role of the eliminated player
     * @param reason the reason for elimination
     */
    public PlayerEliminateEvent(String sessionName, Player eliminatedPlayer, Role role, EliminationReason reason) {
        super();
        this.sessionName = sessionName;
        this.eliminatedPlayer = eliminatedPlayer;
        this.role = role;
        this.reason = reason;
    }
    
    /**
     * Creates a new player elimination event with explicit timestamp.
     * 
     * @param sessionName the session where elimination occurred
     * @param eliminatedPlayer the player who was eliminated
     * @param role the role of the eliminated player
     * @param reason the reason for elimination
     * @param timestamp epoch millis when the event occurred
     */
    public PlayerEliminateEvent(String sessionName, Player eliminatedPlayer, Role role, EliminationReason reason, long timestamp) {
        super(timestamp);
        this.sessionName = sessionName;
        this.eliminatedPlayer = eliminatedPlayer;
        this.role = role;
        this.reason = reason;
    }
    
    @Override
    public void dispatch(MatchboxEventListener listener) {
        listener.onPlayerEliminate(this);
    }
    
    /**
     * Gets the name of the session where the elimination occurred.
     * 
     * @return the session name
     */
    @NotNull
    public String getSessionName() {
        return sessionName;
    }
    
    /**
     * Gets the player who was eliminated.
     * 
     * @return the eliminated player
     */
    @NotNull
    public Player getPlayer() {
        return eliminatedPlayer;
    }
    
    /**
     * Gets the role of the eliminated player.
     * 
     * @return the player's role
     */
    @NotNull
    public Role getRole() {
        return role;
    }
    
    /**
     * Gets the reason for the elimination.
     * 
     * @return the elimination reason
     */
    @NotNull
    public EliminationReason getReason() {
        return reason;
    }
    
    @Override
    public String toString() {
        return "PlayerEliminateEvent{session='" + sessionName + 
               "', player=" + (eliminatedPlayer != null ? eliminatedPlayer.getName() : "null") + 
               "', role=" + role + 
               "', reason=" + reason + "'}";
    }
}
