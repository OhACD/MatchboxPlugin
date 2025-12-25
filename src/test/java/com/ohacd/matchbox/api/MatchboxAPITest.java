package com.ohacd.matchbox.api;

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
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MatchboxAPI main entry point.
 */
public class MatchboxAPITest {
    
    @BeforeEach
    void setUp() {
        TestPluginFactory.setUpMockPlugin();
    }
    
    @AfterEach
    void tearDown() {
        TestPluginFactory.tearDownMockPlugin();
    }
    
    @Test
    @DisplayName("Should create session builder successfully")
    void shouldCreateSessionBuilder() {
        // Act
        SessionBuilder builder = MatchboxAPI.createSessionBuilder("test-session");
        
        // Assert
        assertThat(builder).isNotNull();
    }
    
    @Test
    @DisplayName("Should get empty optional when session doesn't exist")
    void shouldReturnEmptyOptionalForNonExistentSession() {
        // Act
        Optional<ApiGameSession> result = MatchboxAPI.getSession("non-existent");
        
        // Assert
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("Should get player session when player is in session")
    void shouldGetPlayerSessionWhenPlayerInSession() {
        // Arrange
        Player player = MockBukkitFactory.createMockPlayer();
        List<Location> spawnPoints = List.of(MockBukkitFactory.createMockLocation(0, 64, 0, 0, 0));
        ApiGameSession session = MatchboxAPI.createSessionBuilder("test-session")
            .withPlayers(List.of(player))
            .withSpawnPoints(spawnPoints)
            .start()
            .orElse(null);

        // Act
        Optional<ApiGameSession> result = MatchboxAPI.getPlayerSession(player);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(session);
    }
    
    @Test
    @DisplayName("Should return empty when player not in any session")
    void shouldReturnEmptyOptionalWhenPlayerNotInSession() {
        // Arrange
        Player player = MockBukkitFactory.createMockPlayer();
        
        // Act
        Optional<ApiGameSession> result = MatchboxAPI.getPlayerSession(player);
        
        // Assert
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("Should get player role when in session")
    void shouldGetPlayerRoleWhenInSession() {
        // Arrange
        Player player = MockBukkitFactory.createMockPlayer();
        List<Location> spawnPoints = List.of(MockBukkitFactory.createMockLocation(0, 64, 0, 0, 0));
        ApiGameSession session = MatchboxAPI.createSessionBuilder("test-session")
            .withPlayers(List.of(player))
            .withSpawnPoints(spawnPoints)
            .start()
            .orElse(null);

        // Act
        Optional<Role> result = MatchboxAPI.getPlayerRole(player);

        // Assert
        assertThat(result).isPresent();
    }
    
    @Test
    @DisplayName("Should return empty when player not in session")
    void shouldReturnEmptyRoleWhenPlayerNotInSession() {
        // Arrange
        Player player = MockBukkitFactory.createMockPlayer();
        
        // Act
        Optional<Role> result = MatchboxAPI.getPlayerRole(player);
        
        // Assert
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("Should list all active sessions")
    void shouldListAllActiveSessions() {
        // Arrange
        Player player1 = MockBukkitFactory.createMockPlayer(UUID.randomUUID(), "Player1");
        Player player2 = MockBukkitFactory.createMockPlayer(UUID.randomUUID(), "Player2");
        Player player3 = MockBukkitFactory.createMockPlayer(UUID.randomUUID(), "Player3");
        Player player4 = MockBukkitFactory.createMockPlayer(UUID.randomUUID(), "Player4");
        List<Location> spawnPoints1 = List.of(MockBukkitFactory.createMockLocation(0, 64, 0, 0, 0));
        List<Location> spawnPoints2 = List.of(MockBukkitFactory.createMockLocation(20, 64, 0, 0, 0));

        // Create first session
        ApiGameSession session1 = MatchboxAPI.createSessionBuilder("test-session-1")
            .withPlayers(List.of(player1, player2))
            .withSpawnPoints(spawnPoints1)
            .start()
            .orElse(null);
        assertThat(session1).isNotNull();
        assertThat(MatchboxAPI.getAllSessions()).hasSize(1);

        // Create second session
        ApiGameSession session2 = MatchboxAPI.createSessionBuilder("test-session-2")
            .withPlayers(List.of(player3, player4))
            .withSpawnPoints(spawnPoints2)
            .start()
            .orElse(null);
        assertThat(session2).isNotNull();

        // Act
        Collection<ApiGameSession> sessions = MatchboxAPI.getAllSessions();

        // Assert
        assertThat(sessions).hasSize(2);
        assertThat(sessions).contains(session1, session2);
    }
    
    @Test
    @DisplayName("Should return empty list when no sessions")
    void shouldReturnEmptyListWhenNoSessions() {
        // Act
        Collection<ApiGameSession> sessions = MatchboxAPI.getAllSessions();
        
        // Assert
        assertThat(sessions).isEmpty();
    }
    
    @Test
    @DisplayName("Should end session successfully")
    void shouldEndSessionSuccessfully() {
        // Arrange
        String sessionName = "test-session";
        Player player = MockBukkitFactory.createMockPlayer();
        List<Location> spawnPoints = List.of(MockBukkitFactory.createMockLocation(0, 64, 0, 0, 0));
        MatchboxAPI.createSessionBuilder(sessionName)
            .withPlayers(List.of(player))
            .withSpawnPoints(spawnPoints)
            .start();

        // Act
        boolean result = MatchboxAPI.endSession(sessionName);

        // Assert
        assertThat(result).isTrue();
        assertThat(MatchboxAPI.getSession(sessionName)).isEmpty();
    }
    
    @Test
    @DisplayName("Should return false when ending non-existent session")
    void shouldReturnFalseWhenEndingNonExistentSession() {
        // Act
        boolean result = MatchboxAPI.endSession("non-existent");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should end all sessions successfully")
    void shouldEndAllSessionsSuccessfully() {
        // Arrange
        Player player1 = MockBukkitFactory.createMockPlayer(UUID.randomUUID(), "Player1");
        Player player2 = MockBukkitFactory.createMockPlayer(UUID.randomUUID(), "Player2");
        Player player3 = MockBukkitFactory.createMockPlayer(UUID.randomUUID(), "Player3");
        Player player4 = MockBukkitFactory.createMockPlayer(UUID.randomUUID(), "Player4");
        List<Location> spawnPoints1 = List.of(MockBukkitFactory.createMockLocation(0, 64, 0, 0, 0));
        List<Location> spawnPoints2 = List.of(MockBukkitFactory.createMockLocation(20, 64, 0, 0, 0));

        MatchboxAPI.createSessionBuilder("test-session-1")
            .withPlayers(List.of(player1, player2))
            .withSpawnPoints(spawnPoints1)
            .start();
        MatchboxAPI.createSessionBuilder("test-session-2")
            .withPlayers(List.of(player3, player4))
            .withSpawnPoints(spawnPoints2)
            .start();
        assertThat(MatchboxAPI.getAllSessions()).hasSize(2);

        // Act - End all sessions
        int endedCount = MatchboxAPI.endAllSessions();

        // Assert
        assertThat(endedCount).isEqualTo(2);
        assertThat(MatchboxAPI.getAllSessions()).isEmpty();
    }
    
    @Test
    @DisplayName("Should get current phase when session exists")
    void shouldGetCurrentPhaseWhenSessionExists() {
        // Arrange
        String sessionName = "test-session";
        Player player = MockBukkitFactory.createMockPlayer();
        List<Location> spawnPoints = List.of(MockBukkitFactory.createMockLocation(0, 64, 0, 0, 0));
        MatchboxAPI.createSessionBuilder(sessionName)
            .withPlayers(List.of(player))
            .withSpawnPoints(spawnPoints)
            .start();

        // Act
        Optional<GamePhase> result = MatchboxAPI.getCurrentPhase(sessionName);

        // Assert
        assertThat(result).isPresent();
    }
    
    @Test
    @DisplayName("Should return empty when session doesn't exist")
    void shouldReturnEmptyPhaseWhenSessionNotExists() {
        // Act
        Optional<GamePhase> result = MatchboxAPI.getCurrentPhase("non-existent");
        
        // Assert
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("Should handle null inputs gracefully")
    void shouldHandleNullInputsGracefully() {
        // Act & Assert - createSessionBuilder should throw for null
        assertThrows(IllegalArgumentException.class, () -> MatchboxAPI.createSessionBuilder(null));
        
        // Other methods should handle null gracefully
        assertDoesNotThrow(() -> MatchboxAPI.getSession(null));
        assertDoesNotThrow(() -> MatchboxAPI.endSession(null));
        assertDoesNotThrow(() -> MatchboxAPI.getCurrentPhase(null));
        assertDoesNotThrow(() -> MatchboxAPI.getPlayerSession(null));
        assertDoesNotThrow(() -> MatchboxAPI.getPlayerRole(null));
    }
}
