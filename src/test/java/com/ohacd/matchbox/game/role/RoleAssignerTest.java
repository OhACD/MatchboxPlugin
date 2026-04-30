package com.ohacd.matchbox.game.role;

import com.ohacd.matchbox.api.RoleAssignmentStrategy;
import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.utils.Role;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleAssignerTest {

    @Test
    @DisplayName("Should assign roles using provided strategy order")
    void shouldAssignRolesUsingProvidedStrategyOrder() {
        GameState gameState = new GameState();
        RoleAssigner assigner = new RoleAssigner(gameState);

        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        Player p3 = mock(Player.class);
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        when(p1.isOnline()).thenReturn(true);
        when(p2.isOnline()).thenReturn(true);
        when(p3.isOnline()).thenReturn(true);
        when(p1.getUniqueId()).thenReturn(id1);
        when(p2.getUniqueId()).thenReturn(id2);
        when(p3.getUniqueId()).thenReturn(id3);

        RoleAssignmentStrategy strategy = players -> List.of(players.get(2), players.get(1), players.get(0));

        assigner.assignRoles(List.of(p1, p2, p3), strategy);

        assertThat(gameState.getRole(id3)).isEqualTo(Role.SPARK);
        assertThat(gameState.getRole(id2)).isEqualTo(Role.MEDIC);
        assertThat(gameState.getRole(id1)).isEqualTo(Role.INNOCENT);
    }

    @Test
    @DisplayName("Should fallback to default ordering when strategy returns empty")
    void shouldFallbackToDefaultWhenStrategyReturnsEmpty() {
        GameState gameState = new GameState();
        RoleAssigner assigner = new RoleAssigner(gameState);

        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        when(p1.isOnline()).thenReturn(true);
        when(p2.isOnline()).thenReturn(true);
        when(p1.getUniqueId()).thenReturn(id1);
        when(p2.getUniqueId()).thenReturn(id2);

        RoleAssignmentStrategy strategy = players -> List.of();

        assigner.assignRoles(List.of(p1, p2), strategy);

        assertThat(gameState.getRole(id1)).isNotNull();
        assertThat(gameState.getRole(id2)).isNotNull();
    }
}
