package com.ohacd.matchbox.game.ability;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Central registry and dispatcher for all abilities so we only maintain one
 * Bukkit listener. Abilities remain self contained while sharing the same
 * routing logic.
 */
public class AbilityManager {
    private final GameManager gameManager;
    private final List<AbilityHandler> abilities = new ArrayList<>();

    public AbilityManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void registerAbility(AbilityHandler ability) {
        if (ability != null) {
            abilities.add(ability);
        }
    }

    public List<AbilityHandler> getAbilities() {
        return Collections.unmodifiableList(abilities);
    }

    public void handleInventoryClick(InventoryClickEvent event) {
        Player player = getPlayer(event.getWhoClicked());
        if (player == null) {
            return;
        }
        SessionGameContext context = getContext(player.getUniqueId());
        if (context == null) {
            return;
        }
        for (AbilityHandler ability : abilities) {
            ability.handleInventoryClick(event, context);
            if (event.isCancelled()) {
                break;
            }
        }
    }

    public void handlePlayerInteract(PlayerInteractEvent event) {
        Player player = getPlayer(event.getPlayer());
        if (player == null) {
            return;
        }
        SessionGameContext context = getContext(player.getUniqueId());
        if (context == null) {
            return;
        }
        for (AbilityHandler ability : abilities) {
            ability.handlePlayerInteract(event, context);
            if (event.useInteractedBlock() == Event.Result.DENY || event.useItemInHand() == Event.Result.DENY) {
                break;
            }
        }
    }

    public void handlePlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = getPlayer(event.getPlayer());
        if (player == null) {
            return;
        }
        SessionGameContext context = getContext(player.getUniqueId());
        if (context == null) {
            return;
        }
        for (AbilityHandler ability : abilities) {
            ability.handlePlayerInteractEntity(event, context);
            if (event.isCancelled()) {
                break;
            }
        }
    }

    private SessionGameContext getContext(UUID playerId) {
        SessionGameContext context = gameManager.getContextForPlayer(playerId);
        if (context == null) {
            return null;
        }
        if (!context.getGameState().isGameActive()) {
            return null;
        }
        return context;
    }

    private Player getPlayer(Object maybePlayer) {
        if (maybePlayer instanceof Player) {
            return (Player) maybePlayer;
        }
        return null;
    }
}

