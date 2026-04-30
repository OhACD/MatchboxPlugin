package com.ohacd.matchbox.api;

import com.ohacd.matchbox.api.annotation.Experimental;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.Role;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable context passed to session-scoped custom ability handlers.
 *
 * <p>This provides stable, integration-safe state for the current routed
 * invocation so custom ability handlers do not need to reach into internal
 * Matchbox classes.</p>
 *
 * @param session current session wrapper
 * @param actor player that triggered the routed event
 * @param target interacted player target when applicable
 * @param currentPhase current game phase when available
 * @param currentRound current round number
 * @param actorRole acting player's role when assigned
 * @param actorAlive whether the acting player is currently alive
 *
 * @since 0.9.7
 */
@Experimental
public record SessionAbilityContext(
    @NotNull ApiGameSession session,
    @NotNull Player actor,
    @Nullable Player target,
    @Nullable GamePhase currentPhase,
    int currentRound,
    @Nullable Role actorRole,
    boolean actorAlive
) {
    /**
     * Creates a validated immutable ability invocation context.
     */
    public SessionAbilityContext {
        if (session == null) {
            throw new IllegalArgumentException("session cannot be null");
        }
        if (actor == null) {
            throw new IllegalArgumentException("actor cannot be null");
        }
    }
}
