package com.ohacd.matchbox.api;

import com.ohacd.matchbox.api.annotation.Experimental;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Strategy contract for deterministic role ordering.
 *
 * <p>The returned list order maps to roles as:
 * index 0 => SPARK, index 1 => MEDIC, remaining => INNOCENT.</p>
 *
 * @since 0.9.7
 */
@Experimental
@FunctionalInterface
public interface RoleAssignmentStrategy {

    /**
     * Returns players in role assignment order.
     *
     * @param players validated online players for the session
     * @return ordered player list
     */
    @NotNull
    List<Player> orderPlayers(@NotNull List<Player> players);
}
