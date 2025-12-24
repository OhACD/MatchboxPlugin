package com.ohacd.matchbox.api.events;

import com.google.common.annotations.Beta;
import com.ohacd.matchbox.api.MatchboxEvent;
import com.ohacd.matchbox.api.MatchboxEventListener;
import com.ohacd.matchbox.game.utils.Role;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

/**
 * Event fired when a new game starts.
 * 
 * @since 0.9.5
 * @author OhACD
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
    public GameStartEvent(@NotNull String sessionName, @NotNull Collection<Player> players, @NotNull Map<Player, Role> roleAssignments) {
        this.sessionName = sessionName;
        this.players = players;
        this.roleAssignments = roleAssignments;
    }
    
    @Override
    public void dispatch(@NotNull MatchboxEventListener listener) {
        listener.onGameStart(this);
    }
    
    /**
     * Gets the name of the session where the game started.
     * 
     * @return the session name
     */
    @NotNull
    public String getSessionName() {
        return sessionName;
    }
    
    /**
     * Gets all players participating in the game.
     * 
     * @return collection of all players
     */
    @NotNull
    public Collection<Player> getPlayers() {
        return players;
    }
    
    /**
     * Gets the role assignments for all players.
     * 
     * @return mapping of players to their assigned roles
     */
    @NotNull
    public Map<Player, Role> getRoleAssignments() {
        return roleAssignments;
    }
}
