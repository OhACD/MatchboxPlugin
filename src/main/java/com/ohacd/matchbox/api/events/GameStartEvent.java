package com.ohacd.matchbox.api.events;

import com.ohacd.matchbox.api.MatchboxEvent;
import com.ohacd.matchbox.api.MatchboxEventListener;
import com.ohacd.matchbox.game.utils.Role;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;

/**
 * Event fired when a new game starts.
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public class GameStartEvent extends MatchboxEvent {
    
    private final String sessionName;
    private final Collection<Player> players;
    private final Map<Player, Role> roleAssignments;
    
    /**
     * Creates a new game start event.
     * 
     * @param sessionName the session name
     * @param players all players in the game
     * @param roleAssignments mapping of players to their roles
     */
    public GameStartEvent(String sessionName, Collection<Player> players, Map<Player, Role> roleAssignments) {
        this.sessionName = sessionName;
        this.players = players;
        this.roleAssignments = roleAssignments;
    }
    
    @Override
    public void dispatch(MatchboxEventListener listener) {
        listener.onGameStart(this);
    }
    
    /**
     * Gets the name of the session where the game started.
     * 
     * @return the session name
     */
    public String getSessionName() {
        return sessionName;
    }
    
    /**
     * Gets all players participating in the game.
     * 
     * @return collection of all players
     */
    public Collection<Player> getPlayers() {
        return players;
    }
    
    /**
     * Gets the role assignments for all players.
     * 
     * @return mapping of players to their assigned roles
     */
    public Map<Player, Role> getRoleAssignments() {
        return roleAssignments;
    }
}
