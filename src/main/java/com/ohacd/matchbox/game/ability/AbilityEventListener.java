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

    public AbilityEventListener(AbilityManager abilityManager) {
        this.abilityManager = abilityManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        abilityManager.handleInventoryClick(event);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        abilityManager.handlePlayerInteract(event);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        abilityManager.handlePlayerInteractEntity(event);
    }
}

