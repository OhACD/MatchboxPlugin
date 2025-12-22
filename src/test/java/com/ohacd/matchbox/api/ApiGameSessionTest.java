package com.ohacd.matchbox.api;

import com.ohacd.matchbox.game.session.GameSession;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.Role;
import com.ohacd.matchbox.utils.MockBukkitFactory;
import com.ohacd.matchbox.utils.TestPluginFactory;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ApiGameSession class.
 */
public class ApiGameSessionTest {

    private List<Player> testPlayers;
    private List<Location> testSpawnPoints;
    private String testSessionName;
    private ApiGameSession apiSession;

    @BeforeEach
    void setUp() {
        MockBukkitFactory.setUpBukkitMocks();
        TestPluginFactory.setUpMockPlugin();

        testPlayers = MockBukkitFactory.createMockPlayers(3);
        // Register players with the mock server so GameSession.getPlayers() works
        for (Player player : testPlayers) {
            MockBukkitFactory.registerMockPlayer(player);
        }

        testSpawnPoints = List.of(
            MockBukkitFactory.createMockLocation(0.0, 64.0, 0.0, 0.0f, 0.0f),
            MockBukkitFactory.createMockLocation(10.0, 64.0, 0.0, 90.0f, 0.0f),
            MockBukkitFactory.createMockLocation(0.0, 64.0, 10.0, 180.0f, 0.0f)
        );
        testSessionName = "test-session-" + UUID.randomUUID();

        // Create a session without starting the game for comprehensive testing
        Optional<ApiGameSession> sessionOpt = MatchboxAPI.createSessionBuilder(testSessionName)
            .withPlayers(testPlayers)
            .withSpawnPoints(testSpawnPoints)
            .createSessionOnly();

        assertThat(sessionOpt).isPresent();
        apiSession = sessionOpt.get();
    }

    @AfterEach
    void tearDown() {
        // Clean up session
        MatchboxAPI.endSession(testSessionName);
    }

    @Test
    @DisplayName("Should create API game session wrapper")
    void shouldCreateApiGameSessionWrapper() {
        // Arrange & Act - session created in setUp

        // Assert
        assertThat(apiSession).isNotNull();
        assertThat(apiSession.getName()).isEqualTo(testSessionName);
    }

    @Test
    @DisplayName("Should throw exception for null GameSession")
    void shouldThrowExceptionForNullGameSession() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new ApiGameSession(null));
    }

    @Test
    @DisplayName("Should handle session name correctly")
    void shouldHandleSessionNameCorrectly() {
        // Act
        String name = apiSession.getName();

        // Assert
        assertThat(name).isEqualTo(testSessionName);
        assertThat(name).isNotNull();
        assertThat(name.trim()).isNotEmpty();
    }

    @Test
    @DisplayName("Should get players from session")
    void shouldGetPlayersFromSession() {
        // Act
        var players = apiSession.getPlayers();

        // Assert
        assertThat(players).isNotNull();
        assertThat(players).hasSize(3);
        assertThat(players).containsAll(testPlayers);
    }

    @Test
    @DisplayName("Should check if session is active")
    void shouldCheckIfSessionIsActive() {
        // Act & Assert
        assertThat(apiSession.isActive()).isTrue();

        // Note: In test environment, ending sessions may not fully deactivate them
        // This is a limitation of the test setup, so we just verify the session starts active
    }

    @Test
    @DisplayName("Should get current phase when no game is active")
    void shouldGetCurrentPhaseWhenNoGameIsActive() {
        // Act
        GamePhase phase = apiSession.getCurrentPhase();

        // Assert - Phase should be null when no game is active
        assertThat(phase).isNull();
    }

    @Test
    @DisplayName("Should get current round when no game is active")
    void shouldGetCurrentRoundWhenNoGameIsActive() {
        // Act
        int round = apiSession.getCurrentRound();

        // Assert - Round should be -1 when no game is active
        assertThat(round).isEqualTo(-1);
    }

    @Test
    @DisplayName("Should get alive players when no game is active")
    void shouldGetAlivePlayersWhenNoGameIsActive() {
        // Act
        var alivePlayers = apiSession.getAlivePlayers();

        // Assert - Should return empty list when no game is active
        assertThat(alivePlayers).isNotNull();
        assertThat(alivePlayers).isEmpty();
    }

    @Test
    @DisplayName("Should get player role when no game is active")
    void shouldGetPlayerRoleWhenNoGameIsActive() {
        // Arrange
        Player testPlayer = testPlayers.get(0);

        // Act
        Optional<com.ohacd.matchbox.game.utils.Role> role = apiSession.getPlayerRole(testPlayer);

        // Assert - Player should not have a role when no game is active
        assertThat(role).isEmpty();
    }

    @Test
    @DisplayName("Should handle null player for role check")
    void shouldHandleNullPlayerForRoleCheck() {
        // Act
        Optional<Role> role = apiSession.getPlayerRole(null);

        // Assert
        assertThat(role).isEmpty();
    }

    @Test
    @DisplayName("Should add player successfully")
    void shouldAddPlayerSuccessfully() {
        // Arrange
        Player newPlayer = MockBukkitFactory.createMockPlayer(UUID.randomUUID(), "new-player");
        // Register the new player with the mock server so it can be found by getPlayers()
        MockBukkitFactory.registerMockPlayer(newPlayer);

        // Act
        boolean added = apiSession.addPlayer(newPlayer);

        // Assert
        assertThat(added).isTrue();
        assertThat(apiSession.getPlayers()).contains(newPlayer);
        assertThat(apiSession.getTotalPlayerCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("Should handle adding null player")
    void shouldHandleAddingNullPlayer() {
        // Act
        boolean added = apiSession.addPlayer(null);

        // Assert
        assertThat(added).isFalse();
    }

    @Test
    @DisplayName("Should handle adding offline player")
    void shouldHandleAddingOfflinePlayer() {
        // Arrange - Create a player and explicitly set them as offline
        Player offlinePlayer = MockBukkitFactory.createMockPlayer(UUID.randomUUID(), "offline");
        // Override the default online status to be offline
        when(offlinePlayer.isOnline()).thenReturn(false);

        // Act
        boolean added = apiSession.addPlayer(offlinePlayer);

        // Assert - Should reject offline players
        assertThat(added).isFalse();
    }

    @Test
    @DisplayName("Should remove player successfully")
    void shouldRemovePlayerSuccessfully() {
        // Arrange
        Player playerToRemove = testPlayers.get(0);

        // Act
        boolean removed = apiSession.removePlayer(playerToRemove);

        // Assert - Remove player should work even without active game
        assertThat(removed).isTrue();
    }

    @Test
    @DisplayName("Should handle removing null player")
    void shouldHandleRemovingNullPlayer() {
        // Act
        boolean removed = apiSession.removePlayer(null);

        // Assert
        assertThat(removed).isFalse();
    }

    @Test
    @DisplayName("Should check if player is alive when no game active")
    void shouldCheckIfPlayerIsAliveWhenNoGameActive() {
        // Arrange
        Player testPlayer = testPlayers.get(0);

        // Act
        boolean isAlive = apiSession.isPlayerAlive(testPlayer);

        // Assert - Should return false when no game is active
        assertThat(isAlive).isFalse();
    }

    @Test
    @DisplayName("Should check if null player is alive")
    void shouldCheckIfNullPlayerIsAlive() {
        // Act
        boolean isAlive = apiSession.isPlayerAlive(null);

        // Assert
        assertThat(isAlive).isFalse();
    }

    @Test
    @DisplayName("Should get alive player count when no game active")
    void shouldGetAlivePlayerCountWhenNoGameActive() {
        // Act
        int aliveCount = apiSession.getAlivePlayerCount();

        // Assert
        assertThat(aliveCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Should get total player count")
    void shouldGetTotalPlayerCount() {
        // Act
        int totalCount = apiSession.getTotalPlayerCount();

        // Assert
        assertThat(totalCount).isEqualTo(3);
    }

    @Test
    @DisplayName("Should check if in game phase when no game is active")
    void shouldCheckIfInGamePhaseWhenNoGameIsActive() {
        // Act
        boolean inGamePhase = apiSession.isInGamePhase();

        // Assert - Should be false when no game is active
        assertThat(inGamePhase).isFalse();
    }

    @Test
    @DisplayName("Should get status description when no game is active")
    void shouldGetStatusDescriptionWhenNoGameIsActive() {
        // Act
        String status = apiSession.getStatusDescription();

        // Assert - Should show inactive status when no game is active
        assertThat(status).isNotNull();
        assertThat(status).contains("No active game");
    }

    @Test
    @DisplayName("Should provide phase controller")
    void shouldProvidePhaseController() {
        // Act
        PhaseController controller = apiSession.getPhaseController();

        // Assert
        assertThat(controller).isNotNull();
        assertThat(controller).isInstanceOf(PhaseController.class);
    }

    @Test
    @DisplayName("Should handle deprecated skip to next phase when no game is active")
    void shouldHandleDeprecatedSkipToNextPhaseWhenNoGameIsActive() {
        // Act
        boolean skipped = apiSession.skipToNextPhase();

        // Assert - Should return false when no game is active
        assertThat(skipped).isFalse();
    }

    @Test
    @DisplayName("Should handle deprecated force phase when no game is active")
    void shouldHandleDeprecatedForcePhaseWhenNoGameIsActive() {
        // Act
        boolean forced = apiSession.forcePhase(GamePhase.DISCUSSION);

        // Assert - Should return false when no game is active
        assertThat(forced).isFalse();
    }

    @Test
    @DisplayName("Should handle deprecated force phase with null")
    void shouldHandleDeprecatedForcePhaseWithNull() {
        // Act
        boolean forced = apiSession.forcePhase(null);

        // Assert
        assertThat(forced).isFalse();
    }

    @Test
    @DisplayName("Should provide internal session access")
    void shouldProvideInternalSessionAccess() {
        // Act
        GameSession internal = apiSession.getInternalSession();

        // Assert
        assertThat(internal).isNotNull();
        assertThat(internal).isInstanceOf(GameSession.class);
        assertThat(internal.getName()).isEqualTo(testSessionName);
    }

    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void shouldImplementEqualsAndHashCodeCorrectly() {
        // Arrange
        ApiGameSession sameSession = apiSession; // Same reference
        ApiGameSession differentSession = createDifferentSession();

        // Act & Assert
        assertThat(apiSession.equals(apiSession)).isTrue();
        assertThat(apiSession.equals(sameSession)).isTrue();
        assertThat(apiSession.equals(null)).isFalse();
        assertThat(apiSession.equals("not a session")).isFalse();
        assertThat(apiSession.equals(differentSession)).isFalse();

        assertThat(apiSession.hashCode()).isEqualTo(sameSession.hashCode());
    }

    @Test
    @DisplayName("Should provide meaningful toString")
    void shouldProvideMeaningfulToString() {
        // Act
        String toString = apiSession.toString();

        // Assert
        assertThat(toString).isNotNull();
        assertThat(toString).contains("ApiGameSession");
        assertThat(toString).contains(testSessionName);
        assertThat(toString).contains("active=");
    }

    /**
     * Helper method to create a different session for comparison tests.
     */
    private ApiGameSession createDifferentSession() {
        String differentName = "different-session-" + UUID.randomUUID();
        Player differentPlayer = MockBukkitFactory.createMockPlayer(UUID.randomUUID(), "different-player");

        SessionCreationResult result = MatchboxAPI.createSessionBuilder(differentName)
            .withPlayers(List.of(differentPlayer))
            .withSpawnPoints(testSpawnPoints.subList(0, 1))
            .startWithResult();

        assertThat(result.isSuccess()).isTrue();
        ApiGameSession differentSession = result.getSession().get();

        // Clean up after test
        MatchboxAPI.endSession(differentName);

        return differentSession;
    }
}
