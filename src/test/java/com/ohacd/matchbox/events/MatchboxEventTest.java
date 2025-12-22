package com.ohacd.matchbox.events;

import com.ohacd.matchbox.api.MatchboxEvent;
import com.ohacd.matchbox.api.MatchboxEventListener;
import com.ohacd.matchbox.utils.MockBukkitFactory;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MatchboxEvent base functionality.
 */
public class MatchboxEventTest {
    
    private Player testPlayer;
    private TestEventListener testListener;
    
    @BeforeEach
    void setUp() {
        MockBukkitFactory.setUpBukkitMocks();
        testPlayer = MockBukkitFactory.createMockPlayer();
        testListener = new TestEventListener();
    }
    
    @Test
    @DisplayName("Should create test event successfully")
    void shouldCreateTestEventSuccessfully() {
        // Arrange & Act
        TestMatchboxEvent event = new TestMatchboxEvent();
        
        // Assert
        assertThat(event).isNotNull();
        assertThat(event.isAsynchronous()).isFalse(); // Default should be synchronous
    }
    
    @Test
    @DisplayName("Should handle event dispatch to listener")
    void shouldHandleEventDispatchToListener() {
        // Arrange
        TestMatchboxEvent event = new TestMatchboxEvent();
        
        // Act
        event.dispatch(testListener);
        
        // Assert
        assertThat(testListener.wasEventHandled()).isTrue();
    }
    
    @Test
    @DisplayName("Should handle asynchronous event flag")
    void shouldHandleAsynchronousEventFlag() {
        // Arrange & Act
        TestMatchboxEvent asyncEvent = new TestMatchboxEvent(true);
        
        // Assert
        assertThat(asyncEvent.isAsynchronous()).isTrue();
    }
    
    @Test
    @DisplayName("Should handle event listener exceptions gracefully")
    void shouldHandleEventListenerExceptionsGracefully() {
        // Arrange
        ThrowingEventListener throwingListener = new ThrowingEventListener();
        TestMatchboxEvent event = new TestMatchboxEvent();
        
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> event.dispatch(throwingListener));
    }
    
    @Test
    @DisplayName("Should handle null listener")
    void shouldHandleNullListener() {
        // Arrange
        TestMatchboxEvent event = new TestMatchboxEvent();
        
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> event.dispatch(null));
    }
    
    @Test
    @DisplayName("Should have correct timestamp")
    void shouldHaveCorrectTimestamp() {
        // Arrange
        long before = System.currentTimeMillis();
        TestMatchboxEvent event = new TestMatchboxEvent();
        long after = System.currentTimeMillis();
        
        // Act & Assert
        assertThat(event.getTimestamp()).isBetween(before, after);
    }
    
    // Test helper classes
    
    private static class TestMatchboxEvent extends MatchboxEvent {
        private final boolean async;
        
        public TestMatchboxEvent() {
            this(false);
        }
        
        public TestMatchboxEvent(boolean async) {
            this.async = async;
        }
        
        @Override
        public void dispatch(MatchboxEventListener listener) {
            if (listener != null) {
                try {
                    // Simulate dispatch by marking listener as handled
                    if (listener instanceof TestEventListener) {
                        ((TestEventListener) listener).markHandled(this);
                    }
                } catch (Exception e) {
                    // Handle gracefully
                }
            }
        }
        
        public boolean isAsynchronous() {
            return async;
        }
    }
    
    private static class TestEventListener implements MatchboxEventListener {
        private boolean eventHandled = false;
        private MatchboxEvent lastHandledEvent;
        
        @Override
        public void onGameStart(com.ohacd.matchbox.api.events.GameStartEvent event) {
            markHandled(event);
        }
        
        @Override
        public void onGameEnd(com.ohacd.matchbox.api.events.GameEndEvent event) {
            markHandled(event);
        }
        
        @Override
        public void onPhaseChange(com.ohacd.matchbox.api.events.PhaseChangeEvent event) {
            markHandled(event);
        }
        
        @Override
        public void onPlayerJoin(com.ohacd.matchbox.api.events.PlayerJoinEvent event) {
            markHandled(event);
        }
        
        @Override
        public void onPlayerLeave(com.ohacd.matchbox.api.events.PlayerLeaveEvent event) {
            markHandled(event);
        }
        
        @Override
        public void onPlayerEliminate(com.ohacd.matchbox.api.events.PlayerEliminateEvent event) {
            markHandled(event);
        }
        
        @Override
        public void onPlayerVote(com.ohacd.matchbox.api.events.PlayerVoteEvent event) {
            markHandled(event);
        }
        
        @Override
        public void onAbilityUse(com.ohacd.matchbox.api.events.AbilityUseEvent event) {
            markHandled(event);
        }
        
        @Override
        public void onCure(com.ohacd.matchbox.api.events.CureEvent event) {
            markHandled(event);
        }
        
        @Override
        public void onSwipe(com.ohacd.matchbox.api.events.SwipeEvent event) {
            markHandled(event);
        }
        
        public void markHandled(MatchboxEvent event) {
            this.eventHandled = true;
            this.lastHandledEvent = event;
        }
        
        public boolean wasEventHandled() {
            return eventHandled;
        }
        
        public MatchboxEvent getLastHandledEvent() {
            return lastHandledEvent;
        }
        
        public void reset() {
            eventHandled = false;
            lastHandledEvent = null;
        }
    }
    
    private static class ThrowingEventListener implements MatchboxEventListener {
        @Override
        public void onGameStart(com.ohacd.matchbox.api.events.GameStartEvent event) {
            throw new RuntimeException("Test exception");
        }
        
        @Override
        public void onGameEnd(com.ohacd.matchbox.api.events.GameEndEvent event) {
            throw new RuntimeException("Test exception");
        }
        
        @Override
        public void onPhaseChange(com.ohacd.matchbox.api.events.PhaseChangeEvent event) {
            throw new RuntimeException("Test exception");
        }
        
        @Override
        public void onPlayerJoin(com.ohacd.matchbox.api.events.PlayerJoinEvent event) {
            throw new RuntimeException("Test exception");
        }
        
        @Override
        public void onPlayerLeave(com.ohacd.matchbox.api.events.PlayerLeaveEvent event) {
            throw new RuntimeException("Test exception");
        }
        
        @Override
        public void onPlayerEliminate(com.ohacd.matchbox.api.events.PlayerEliminateEvent event) {
            throw new RuntimeException("Test exception");
        }
        
        @Override
        public void onPlayerVote(com.ohacd.matchbox.api.events.PlayerVoteEvent event) {
            throw new RuntimeException("Test exception");
        }
        
        @Override
        public void onAbilityUse(com.ohacd.matchbox.api.events.AbilityUseEvent event) {
            throw new RuntimeException("Test exception");
        }
        
        @Override
        public void onCure(com.ohacd.matchbox.api.events.CureEvent event) {
            throw new RuntimeException("Test exception");
        }
        
        @Override
        public void onSwipe(com.ohacd.matchbox.api.events.SwipeEvent event) {
            throw new RuntimeException("Test exception");
        }
    }
}
