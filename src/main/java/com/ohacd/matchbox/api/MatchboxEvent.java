package com.ohacd.matchbox.api;

import org.jetbrains.annotations.NotNull;

/**
 * Base class for all Matchbox events.
 * 
 * <p>All events extend this class and implement {@link #dispatch(MatchboxEventListener)}
 * method to call the appropriate method on the listener.</p>
 * 
 * <p>This follows the visitor pattern for type-safe event handling.</p>
 * 
 * @since 0.9.5
 * @author Matchbox Team
 */
public abstract class MatchboxEvent {
    
    private final long timestamp;
    
    /**
     * Creates a new MatchboxEvent with the current timestamp.
     */
    protected MatchboxEvent() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Creates a new MatchboxEvent with a specific timestamp.
     * 
     * @param timestamp the event timestamp in milliseconds
     */
    protected MatchboxEvent(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Dispatches this event to the appropriate listener method.
     * 
     * <p>This method uses the visitor pattern to call the correct handler
     * method based on the concrete event type. Implementing classes should
     * call {@code super.dispatch(listener)} as the first line.</p>
     * 
     * @param listener the listener to dispatch to
     * @throws IllegalArgumentException if listener is null
     */
    public abstract void dispatch(@NotNull MatchboxEventListener listener);
    
    /**
     * Gets the timestamp when this event was created.
     * 
     * @return event timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{timestamp=" + timestamp + "}";
    }
}
