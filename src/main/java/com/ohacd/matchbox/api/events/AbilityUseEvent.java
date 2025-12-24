package com.ohacd.matchbox.api.events;

import com.ohacd.matchbox.api.MatchboxEvent;
import com.ohacd.matchbox.api.MatchboxEventListener;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when a player uses a special ability.
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public class AbilityUseEvent extends MatchboxEvent {
    
    private final String sessionName;
    private final Player player;
    private final AbilityType ability;
    private final Player target;
    
    /**
     * Types of abilities that can be used.
     */
    public enum AbilityType {
        /** Spark uses Hunter Vision to see all players */
        HUNTER_VISION,
        /** Spark swaps positions with another player */
        SPARK_SWAP,
        /** Spark causes delusion (fake infection) on a player */
        DELUSION,
        /** Medic uses Healing Sight to see infected players */
        HEALING_SIGHT,
        /** Medic cures an infected player */
        CURE,
        /** Swipe attack (used by Spark) */
        SWIPE
    }
    
    /**
     * Creates a new ability use event.
     * 
     * @param sessionName the session name
     * @param player the player using the ability
     * @param ability the type of ability used
     * @param target the target player (may be null for self-targeted abilities)
     */
    public AbilityUseEvent(@NotNull String sessionName, @NotNull Player player, @NotNull AbilityType ability, @Nullable Player target) {
        this.sessionName = sessionName;
        this.player = player;
        this.ability = ability;
        this.target = target;
    }
    
    @Override
    public void dispatch(@NotNull MatchboxEventListener listener) {
        listener.onAbilityUse(this);
    }
    
    /**
     * Gets the name of the session where the ability was used.
     * 
     * @return the session name
     */
    @NotNull
    public String getSessionName() {
        return sessionName;
    }
    
    /**
     * Gets the player who used the ability.
     * 
     * @return the player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Gets the type of ability that was used.
     * 
     * @return the ability type
     */
    @NotNull
    public AbilityType getAbility() {
        return ability;
    }
    
    /**
     * Gets the target player of the ability.
     * 
     * @return the target player, or null if the ability is self-targeted
     */
    @Nullable
    public Player getTarget() {
        return target;
    }
}
