package com.ohacd.matchbox.api;

import com.ohacd.matchbox.api.annotation.Experimental;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Session-scoped custom ability hook for integrations.
 *
 * <p>Handlers are attached to a session through {@link SessionBuilder} and are
 * routed after Matchbox's built-in abilities for that session.</p>
 *
 * @since 0.9.7
 */
@Experimental
public interface SessionAbilityHandler {

    /**
     * Handles a routed inventory click event with stable invocation context.
     *
     * @param event routed Bukkit event
     * @param context stable Matchbox invocation context
     */
    default void handleInventoryClick(InventoryClickEvent event, SessionAbilityContext context) {
        handleInventoryClick(event, context.session());
    }

    /**
     * Handles a routed inventory click event.
     *
     * @param event routed Bukkit event
     * @param session session wrapper for the active game
     */
    default void handleInventoryClick(InventoryClickEvent event, ApiGameSession session) { }

    /**
     * Handles a routed player interact event with stable invocation context.
     *
     * @param event routed Bukkit event
     * @param context stable Matchbox invocation context
     */
    default void handlePlayerInteract(PlayerInteractEvent event, SessionAbilityContext context) {
        handlePlayerInteract(event, context.session());
    }

    /**
     * Handles a routed player interact event.
     *
     * @param event routed Bukkit event
     * @param session session wrapper for the active game
     */
    default void handlePlayerInteract(PlayerInteractEvent event, ApiGameSession session) { }

    /**
     * Handles a routed player interact entity event with stable invocation context.
     *
     * @param event routed Bukkit event
     * @param context stable Matchbox invocation context
     */
    default void handlePlayerInteractEntity(PlayerInteractEntityEvent event, SessionAbilityContext context) {
        handlePlayerInteractEntity(event, context.session());
    }

    /**
     * Handles a routed player interact entity event.
     *
     * @param event routed Bukkit event
     * @param session session wrapper for the active game
     */
    default void handlePlayerInteractEntity(PlayerInteractEntityEvent event, ApiGameSession session) { }
}
