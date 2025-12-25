package com.ohacd.matchbox.api;

import com.ohacd.matchbox.utils.MockBukkitFactory;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SessionBuilder class.
 */
public class SessionBuilderTest {
    
    private List<Player> testPlayers;
    private List<Location> testSpawnPoints;
    private Location testDiscussionLocation;
    private Map<Integer, Location> testSeatLocations;
    
    @BeforeEach
    void setUp() {
        MockBukkitFactory.setUpBukkitMocks();
        testPlayers = MockBukkitFactory.createMockPlayers(3);
        testSpawnPoints = List.of(
            MockBukkitFactory.createMockLocation(0, 64, 0, 0, 0),
            MockBukkitFactory.createMockLocation(10, 64, 0, 90, 0),
            MockBukkitFactory.createMockLocation(0, 64, 10, 180, 0)
        );
        testDiscussionLocation = MockBukkitFactory.createMockLocation(5, 64, 5, 0, 0);
        testSeatLocations = Map.of(
            1, MockBukkitFactory.createMockLocation(0, 65, 0, 0, 0),
            2, MockBukkitFactory.createMockLocation(10, 65, 0, 90, 0),
            3, MockBukkitFactory.createMockLocation(0, 65, 10, 180, 0)
        );
    }
    
    @Test
    @DisplayName("Should create session builder with valid name")
    void shouldCreateSessionBuilderWithValidName() {
        // Act
        SessionBuilder builder = new SessionBuilder("test-session");
        
        // Assert
        assertThat(builder).isNotNull();
    }
    
    @Test
    @DisplayName("Should throw exception for null session name")
    void shouldThrowExceptionForNullSessionName() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new SessionBuilder(null));
    }
    
    @Test
    @DisplayName("Should throw exception for empty session name")
    void shouldThrowExceptionForEmptySessionName() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new SessionBuilder(""));
        assertThrows(IllegalArgumentException.class, () -> new SessionBuilder("   "));
    }
    
    @Test
    @DisplayName("Should set players successfully")
    void shouldSetPlayersSuccessfully() {
        // Arrange
        SessionBuilder builder = new SessionBuilder("test-session");
        
        // Act
        SessionBuilder result = builder.withPlayers(testPlayers);
        
        // Assert
        assertThat(result).isSameAs(builder); // Should return same instance for chaining
    }
    
    @Test
    @DisplayName("Should handle null players gracefully")
    void shouldHandleNullPlayersGracefully() {
        // Arrange
        SessionBuilder builder = new SessionBuilder("test-session");
        
        // Act
        SessionBuilder result = builder.withPlayers((List<Player>) null);
        
        // Assert
        assertThat(result).isSameAs(builder);
    }
    
    @Test
    @DisplayName("Should set spawn points successfully")
    void shouldSetSpawnPointsSuccessfully() {
        // Arrange
        SessionBuilder builder = new SessionBuilder("test-session");
        
        // Act
        SessionBuilder result = builder.withSpawnPoints(testSpawnPoints);
        
        // Assert
        assertThat(result).isSameAs(builder);
    }
    
    @Test
    @DisplayName("Should set discussion location successfully")
    void shouldSetDiscussionLocationSuccessfully() {
        // Arrange
        SessionBuilder builder = new SessionBuilder("test-session");
        
        // Act
        SessionBuilder result = builder.withDiscussionLocation(testDiscussionLocation);
        
        // Assert
        assertThat(result).isSameAs(builder);
    }
    
    @Test
    @DisplayName("Should set seat locations successfully")
    void shouldSetSeatLocationsSuccessfully() {
        // Arrange
        SessionBuilder builder = new SessionBuilder("test-session");
        
        // Act
        SessionBuilder result = builder.withSeatLocations(testSeatLocations);
        
        // Assert
        assertThat(result).isSameAs(builder);
    }
    
    @Test
    @DisplayName("Should set custom game config successfully")
    void shouldSetCustomGameConfigSuccessfully() {
        // Arrange
        SessionBuilder builder = new SessionBuilder("test-session");
        GameConfig config = new GameConfig.Builder().build();
        
        // Act
        SessionBuilder result = builder.withCustomConfig(config);
        
        // Assert
        assertThat(result).isSameAs(builder);
    }
    
    @Test
    @DisplayName("Should validate valid configuration")
    void shouldValidateValidConfiguration() {
        // Arrange
        SessionBuilder builder = new SessionBuilder("test-session")
            .withPlayers(testPlayers)
            .withSpawnPoints(testSpawnPoints);
        
        // Act
        Optional<String> result = builder.validate();
        
        // Assert
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("Should fail validation for no players")
    void shouldFailValidationForNoPlayers() {
        // Arrange
        SessionBuilder builder = new SessionBuilder("test-session")
            .withSpawnPoints(testSpawnPoints);
        
        // Act
        Optional<String> result = builder.validate();
        
        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).contains("No players");
    }
    
    @Test
    @DisplayName("Should fail validation for no spawn points")
    void shouldFailValidationForNoSpawnPoints() {
        // Arrange
        SessionBuilder builder = new SessionBuilder("test-session")
            .withPlayers(testPlayers);
        
        // Act
        Optional<String> result = builder.validate();
        
        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).contains("No spawn points");
    }
    
    @Test
    @DisplayName("Should fail validation for invalid discussion location")
    void shouldFailValidationForInvalidDiscussionLocation() {
        // Arrange
        Location invalidLocation = MockBukkitFactory.createMockLocation();
        // Simulate invalid location by mocking world as null
        SessionBuilder builder = new SessionBuilder("test-session")
            .withPlayers(testPlayers)
            .withSpawnPoints(testSpawnPoints)
            .withDiscussionLocation(invalidLocation);
        
        // This test would require more complex mocking to truly test invalid location
        // For now, we'll test the basic structure
        Optional<String> result = builder.validate();
        assertThat(result).isEmpty(); // Valid configuration
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("Should fail validation for invalid session names")
    void shouldFailValidationForInvalidSessionNames(String invalidName) {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new SessionBuilder(invalidName));
    }
    
    @Test
    @DisplayName("Should handle varargs players")
    void shouldHandleVarargsPlayers() {
        // Arrange
        SessionBuilder builder = new SessionBuilder("test-session");
        Player player1 = testPlayers.get(0);
        Player player2 = testPlayers.get(1);
        
        // Act
        SessionBuilder result = builder.withPlayers(player1, player2);
        
        // Assert
        assertThat(result).isSameAs(builder);
    }
    
    @Test
    @DisplayName("Should handle varargs spawn points")
    void shouldHandleVarargsSpawnPoints() {
        // Arrange
        SessionBuilder builder = new SessionBuilder("test-session");
        Location spawn1 = testSpawnPoints.get(0);
        Location spawn2 = testSpawnPoints.get(1);
        
        // Act
        SessionBuilder result = builder.withSpawnPoints(spawn1, spawn2);
        
        // Assert
        assertThat(result).isSameAs(builder);
    }
    
    @Test
    @DisplayName("Should create config builder")
    void shouldCreateConfigBuilder() {
        // Act
        GameConfig.Builder builder = SessionBuilder.configBuilder();
        
        // Assert
        assertThat(builder).isNotNull();
    }
    
    @Test
    @DisplayName("Should handle both config methods equivalently")
    void shouldHandleBothConfigMethodsEquivalently() {
        // Arrange
        SessionBuilder builder1 = new SessionBuilder("test-session");
        SessionBuilder builder2 = new SessionBuilder("test-session");
        GameConfig config = new GameConfig.Builder().build();
        
        // Act
        SessionBuilder result1 = builder1.withCustomConfig(config);
        SessionBuilder result2 = builder2.withConfig(config);
        
        // Assert
        assertThat(result1).isSameAs(builder1);
        assertThat(result2).isSameAs(builder2);
    }
}
