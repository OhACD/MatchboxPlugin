package com.ohacd.matchbox.game.ability;

import com.ohacd.matchbox.api.ApiGameSession;
import com.ohacd.matchbox.api.SessionAbilityContext;
import com.ohacd.matchbox.api.SessionAbilityHandler;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.session.GameSession;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.Role;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbilityManagerTest {

    @Test
    @DisplayName("Should route session ability handlers for active session inventory clicks")
    void shouldRouteSessionAbilityHandlersForActiveSessionInventoryClicks() {
        GameManager gameManager = mock(GameManager.class);
        AbilityManager abilityManager = new AbilityManager(gameManager);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        Player player = mock(Player.class);
        SessionGameContext context = mock(SessionGameContext.class);
        GameState gameState = mock(GameState.class);
        GameSession gameSession = mock(GameSession.class);
        UUID playerId = UUID.randomUUID();
        AtomicBoolean invoked = new AtomicBoolean(false);

        when(event.getWhoClicked()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(gameManager.getContextForPlayer(playerId)).thenReturn(context);
        when(context.getGameState()).thenReturn(gameState);
        when(gameState.isGameActive()).thenReturn(true);
        when(context.getSessionName()).thenReturn("ability-session");
        when(gameManager.getSessionAbilityHandlers("ability-session")).thenReturn(List.of(new SessionAbilityHandler() {
            @Override
            public void handleInventoryClick(InventoryClickEvent routedEvent, ApiGameSession session) {
                invoked.set(true);
            }
        }));
        when(gameManager.getSessionForAbilityRouting("ability-session")).thenReturn(gameSession);

        abilityManager.handleInventoryClick(event);

        assertThat(invoked).isTrue();
    }

    @Test
    @DisplayName("Should stop routing session ability handlers after cancellation")
    void shouldStopRoutingSessionAbilityHandlersAfterCancellation() {
        GameManager gameManager = mock(GameManager.class);
        AbilityManager abilityManager = new AbilityManager(gameManager);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        Player player = mock(Player.class);
        SessionGameContext context = mock(SessionGameContext.class);
        GameState gameState = mock(GameState.class);
        GameSession gameSession = mock(GameSession.class);
        UUID playerId = UUID.randomUUID();
        AtomicBoolean secondInvoked = new AtomicBoolean(false);
        AtomicBoolean cancelled = new AtomicBoolean(false);

        when(event.getWhoClicked()).thenReturn(player);
        when(event.isCancelled()).thenAnswer(invocation -> cancelled.get());
        doAnswer(invocation -> {
            cancelled.set(invocation.getArgument(0));
            return null;
        }).when(event).setCancelled(anyBoolean());
        when(player.getUniqueId()).thenReturn(playerId);
        when(gameManager.getContextForPlayer(playerId)).thenReturn(context);
        when(context.getGameState()).thenReturn(gameState);
        when(gameState.isGameActive()).thenReturn(true);
        when(context.getSessionName()).thenReturn("ability-session");
        when(gameManager.getSessionForAbilityRouting("ability-session")).thenReturn(gameSession);
        when(gameManager.getSessionAbilityHandlers("ability-session")).thenReturn(List.of(
            new SessionAbilityHandler() {
                @Override
                public void handleInventoryClick(InventoryClickEvent routedEvent, ApiGameSession session) {
                    routedEvent.setCancelled(true);
                }
            },
            new SessionAbilityHandler() {
                @Override
                public void handleInventoryClick(InventoryClickEvent routedEvent, ApiGameSession session) {
                    secondInvoked.set(true);
                }
            }
        ));

        abilityManager.handleInventoryClick(event);

        verify(event).setCancelled(true);
        assertThat(cancelled.get()).isTrue();
        assertThat(secondInvoked).isFalse();
    }

    @Test
    @DisplayName("Should expose stable session ability context to custom handlers")
    void shouldExposeStableSessionAbilityContextToCustomHandlers() {
        GameManager gameManager = mock(GameManager.class);
        AbilityManager abilityManager = new AbilityManager(gameManager);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        Player player = mock(Player.class);
        SessionGameContext context = mock(SessionGameContext.class);
        GameState gameState = mock(GameState.class);
        GameSession gameSession = mock(GameSession.class);
        UUID playerId = UUID.randomUUID();
        AtomicBoolean invoked = new AtomicBoolean(false);

        when(event.getWhoClicked()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerId);
        when(gameManager.getContextForPlayer(playerId)).thenReturn(context);
        when(context.getGameState()).thenReturn(gameState);
        when(gameState.isGameActive()).thenReturn(true);
        when(context.getSessionName()).thenReturn("ability-session");
        when(gameManager.getSessionForAbilityRouting("ability-session")).thenReturn(gameSession);
        when(gameSession.getName()).thenReturn("ability-session");
        when(gameSession.isActive()).thenReturn(true);
        when(gameManager.getContext("ability-session")).thenReturn(context);
        when(gameState.getCurrentRound()).thenReturn(3);
        when(gameState.getRole(playerId)).thenReturn(Role.SPARK);
        when(gameState.isAlive(playerId)).thenReturn(true);
        when(context.getPhaseManager()).thenReturn(mock(com.ohacd.matchbox.game.phase.PhaseManager.class));
        when(context.getPhaseManager().getCurrentPhase()).thenReturn(GamePhase.SWIPE);
        when(gameManager.getSessionAbilityHandlers("ability-session")).thenReturn(List.of(new SessionAbilityHandler() {
            @Override
            public void handleInventoryClick(InventoryClickEvent routedEvent, SessionAbilityContext abilityContext) {
                invoked.set(true);
                assertThat(abilityContext.session().getName()).isEqualTo("ability-session");
                assertThat(abilityContext.actor()).isSameAs(player);
                assertThat(abilityContext.target()).isNull();
                assertThat(abilityContext.currentPhase()).isEqualTo(GamePhase.SWIPE);
                assertThat(abilityContext.currentRound()).isEqualTo(3);
                assertThat(abilityContext.actorRole()).isEqualTo(Role.SPARK);
                assertThat(abilityContext.actorAlive()).isTrue();
            }
        }));

        abilityManager.handleInventoryClick(event);

        assertThat(invoked).isTrue();
    }
}
