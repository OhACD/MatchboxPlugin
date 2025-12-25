package com.ohacd.matchbox.game.ability;

import com.ohacd.matchbox.game.SessionGameContext;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Minimal contract for ability handlers so a single listener can route events.
 */
public interface AbilityHandler {
    /**
     * Handle inventory click events for this ability.
     *
     * @param event the inventory click event
     * @param context the session game context
     */
    default void handleInventoryClick(InventoryClickEvent event, SessionGameContext context) { }

    /**
     * Handle player interact events for this ability.
     *
     * @param event the player interact event
     * @param context the session game context
     */
    default void handlePlayerInteract(PlayerInteractEvent event, SessionGameContext context) { }

    /**
     * Handle player interact-entity events for this ability.
     *
     * @param event the player interact entity event
     * @param context the session game context
     */
    default void handlePlayerInteractEntity(PlayerInteractEntityEvent event, SessionGameContext context) { }
}

