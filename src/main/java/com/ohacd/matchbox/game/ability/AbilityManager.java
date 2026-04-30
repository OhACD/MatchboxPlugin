package com.ohacd.matchbox.game.ability;

import com.ohacd.matchbox.api.ApiGameSession;
import com.ohacd.matchbox.api.SessionAbilityContext;
import com.ohacd.matchbox.api.SessionAbilityHandler;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.session.GameSession;
import com.ohacd.matchbox.game.utils.Role;
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

    /**
     * Creates a manager responsible for routing ability events.
     *
     * @param gameManager the central {@link GameManager} used to obtain contexts
     */
    public AbilityManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Registers an ability handler to receive routed events.
     *
     * @param ability the ability handler to register (ignored if null)
     */
    public void registerAbility(AbilityHandler ability) {
        if (ability != null) {
            abilities.add(ability);
        }
    }

    /**
     * Returns an unmodifiable list of registered ability handlers.
     *
     * @return list of registered {@link AbilityHandler} instances
     */
    public List<AbilityHandler> getAbilities() {
        return Collections.unmodifiableList(abilities);
    }

    /**
     * Routes an inventory click event to registered abilities when applicable.
     *
     * @param event the inventory click event from Bukkit
     */
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

        if (!event.isCancelled()) {
            routeSessionInventoryHandlers(event, context);
        }
    }

    /**
     * Routes a player interact event to registered abilities when applicable.
     *
     * @param event the player interact event from Bukkit
     */
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

        if (event.useInteractedBlock() != Event.Result.DENY && event.useItemInHand() != Event.Result.DENY) {
            routeSessionInteractHandlers(event, context);
        }
    }

    /**
     * Routes a player-interact-entity event to registered abilities when applicable.
     *
     * @param event the player interact entity event from Bukkit
     */
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

        if (!event.isCancelled()) {
            routeSessionInteractEntityHandlers(event, context);
        }
    }

    private void routeSessionInventoryHandlers(InventoryClickEvent event, SessionGameContext context) {
        SessionAbilityContext abilityContext = createAbilityContext(context, getPlayer(event.getWhoClicked()), null);
        if (abilityContext == null) {
            return;
        }

        for (SessionAbilityHandler handler : gameManager.getSessionAbilityHandlers(context.getSessionName())) {
            handler.handleInventoryClick(event, abilityContext);
            if (event.isCancelled()) {
                break;
            }
        }
    }

    private void routeSessionInteractHandlers(PlayerInteractEvent event, SessionGameContext context) {
        SessionAbilityContext abilityContext = createAbilityContext(context, getPlayer(event.getPlayer()), null);
        if (abilityContext == null) {
            return;
        }

        for (SessionAbilityHandler handler : gameManager.getSessionAbilityHandlers(context.getSessionName())) {
            handler.handlePlayerInteract(event, abilityContext);
            if (event.useInteractedBlock() == Event.Result.DENY || event.useItemInHand() == Event.Result.DENY) {
                break;
            }
        }
    }

    private void routeSessionInteractEntityHandlers(PlayerInteractEntityEvent event, SessionGameContext context) {
        SessionAbilityContext abilityContext = createAbilityContext(
            context,
            getPlayer(event.getPlayer()),
            getPlayer(event.getRightClicked())
        );
        if (abilityContext == null) {
            return;
        }

        for (SessionAbilityHandler handler : gameManager.getSessionAbilityHandlers(context.getSessionName())) {
            handler.handlePlayerInteractEntity(event, abilityContext);
            if (event.isCancelled()) {
                break;
            }
        }
    }

    private SessionAbilityContext createAbilityContext(SessionGameContext context, Player actor, Player target) {
        if (actor == null) {
            return null;
        }

        ApiGameSession session = getApiSession(context.getSessionName());
        if (session == null) {
            return null;
        }

        Role actorRole = context.getGameState().getRole(actor.getUniqueId());
        var phaseManager = context.getPhaseManager();
        return new SessionAbilityContext(
            session,
            actor,
            target,
            phaseManager != null ? phaseManager.getCurrentPhase() : null,
            context.getGameState().getCurrentRound(),
            actorRole,
            context.getGameState().isAlive(actor.getUniqueId())
        );
    }

    private ApiGameSession getApiSession(String sessionName) {
        GameSession session = gameManager.getSessionForAbilityRouting(sessionName);
        if (session == null) {
            return null;
        }
        return new ApiGameSession(session);
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

