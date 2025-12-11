package com.ohacd.matchbox.game.ability;

import com.ohacd.matchbox.game.SessionGameContext;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Minimal contract for ability handlers so a single listener can route events.
 */
public interface AbilityHandler {
    default void handleInventoryClick(InventoryClickEvent event, SessionGameContext context) { }

    default void handlePlayerInteract(PlayerInteractEvent event, SessionGameContext context) { }

    default void handlePlayerInteractEntity(PlayerInteractEntityEvent event, SessionGameContext context) { }
}

