package com.ohacd.matchbox.api;

import org.jetbrains.annotations.NotNull;

import com.ohacd.matchbox.api.events.*;

/**
 * Interface for listening to Matchbox game events.
 * 
 * <p>Implement this interface and register it using {@link MatchboxAPI#addEventListener(MatchboxEventListener)}
 * to receive notifications about game state changes, player actions, and other significant events.</p>
 * 
 * <p>All methods have default empty implementations, so you only need to override the events
 * you're interested in. This follows the interface segregation principle.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * MatchboxAPI.addEventListener(new MatchboxEventListener() {
 *     @Override
 *     public void onGameStart(GameStartEvent event) {
 *         getLogger().info("Game started in session: " + event.getSessionName());
 *         // Handle game start - initialize UI, start timers, etc.
 *     }
 *     
 *     @Override
 *     public void onPlayerEliminate(PlayerEliminateEvent event) {
 *         // Handle player elimination - update scores, send messages, etc.
 *         Player eliminated = event.getPlayer();
 *         scoreboardManager.updatePlayerScore(eliminated, -10);
 *         getLogger().info("Player " + eliminated.getName() + " was eliminated");
 *     }
 * });
 * }</pre>
 * 
 * <p><strong>Important Notes:</strong></p>
 * <ul>
 * <li>All event handlers are executed on the main server thread. Avoid long-running operations.</li>
 * <li>Exceptions in event handlers will be caught and logged, but won't stop other listeners.</li>
 * <li>Event objects contain contextual information - use them instead of querying global state.</li>
 * </ul>
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public interface MatchboxEventListener {
    
    /**
     * Called when a new game starts.
     * 
     * @param event the game start event containing session information
     */
    default void onGameStart(@NotNull GameStartEvent event) {}
    
    /**
     * Called when a game phase changes.
     * 
     * @param event the phase change event containing old and new phases
     */
    default void onPhaseChange(@NotNull PhaseChangeEvent event) {}
    
    /**
     * Called when a player is eliminated from the game.
     * 
     * @param event the player elimination event containing player and elimination details
     */
    default void onPlayerEliminate(@NotNull PlayerEliminateEvent event) {}
    
    /**
     * Called when a player casts a vote during the voting phase.
     * 
     * @param event the player vote event containing voter, target, and vote details
     */
    default void onPlayerVote(@NotNull PlayerVoteEvent event) {}
    
    /**
     * Called when a player uses a special ability.
     * 
     * @param event the ability use event containing player, ability type, and usage details
     */
    default void onAbilityUse(@NotNull AbilityUseEvent event) {}
    
    /**
     * Called when a game ends (either by win condition or manual termination).
     * 
     * @param event the game end event containing session, winner, and end reason
     */
    default void onGameEnd(@NotNull GameEndEvent event) {}
    
    /**
     * Called when a player joins a game session.
     * 
     * @param event the player join event containing player and session information
     */
    default void onPlayerJoin(@NotNull PlayerJoinEvent event) {}
    
    /**
     * Called when a player leaves a game session.
     * 
     * @param event the player leave event containing player, session, and leave reason
     */
    default void onPlayerLeave(@NotNull PlayerLeaveEvent event) {}
    
    /**
     * Called when a swipe action is performed.
     * 
     * @param event the swipe event containing attacker, target, and swipe details
     */
    default void onSwipe(@NotNull SwipeEvent event) {}
    
    /**
     * Called when a cure action is performed.
     * 
     * @param event the cure event containing medic, target, and cure details
     */
    default void onCure(@NotNull CureEvent event) {}
}
