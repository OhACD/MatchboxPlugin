package com.ohacd.matchbox.game.ability;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Single Bukkit listener that forwards relevant events to the registered
 * abilities through {@link AbilityManager}.
 */
public class AbilityEventListener implements Listener {
    private final AbilityManager abilityManager;

    /**
     * Creates a listener that forwards Bukkit events to the given {@link AbilityManager}.
     *
     * @param abilityManager manager used to dispatch ability events
     */
    public AbilityEventListener(AbilityManager abilityManager) {
        this.abilityManager = abilityManager;
    }

    @EventHandler
    /**
     * Handle inventory click events and forward to registered abilities.
     *
     * @param event the inventory click event
     */
    public void onInventoryClick(InventoryClickEvent event) {
        abilityManager.handleInventoryClick(event);
    }

    @EventHandler
    /**
     * Handle player interact (block/item) events and forward to registered abilities.
     *
     * @param event the player interact event
     */
    public void onPlayerInteract(PlayerInteractEvent event) {
        abilityManager.handlePlayerInteract(event);
    }

    @EventHandler
    /**
     * Handle player interact-entity events and forward to registered abilities.
     *
     * @param event the player interact entity event
     */
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        abilityManager.handlePlayerInteractEntity(event);
    }
}

