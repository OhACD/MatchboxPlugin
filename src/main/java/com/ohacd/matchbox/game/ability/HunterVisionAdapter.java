package com.ohacd.matchbox.game.ability;

import com.ohacd.matchbox.game.SessionGameContext;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * Abstraction over the Spark's Hunter Vision effect so implementations can
 * provide either ProtocolLib-powered glow or legacy particle indicators.
 */
public interface HunterVisionAdapter {
    /**
     * Starts Hunter Vision for the given spark in the provided context.
     */
    void startVision(Player spark, SessionGameContext context);

    /**
     * Stops any active Hunter Vision tracking for the specified spark.
     */
    void stopVision(UUID sparkId);

    /**
     * Stops active Hunter Vision tracking for multiple spark players.
     */
    void stopVisionForPlayers(Collection<UUID> sparkIds);

    /**
     * @return true if this adapter uses the advanced ProtocolLib glow pipeline.
     */
    boolean isAdvanced();
}

