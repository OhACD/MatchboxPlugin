package com.ohacd.matchbox.game.role;

import com.ohacd.matchbox.api.RoleAssignmentStrategy;
import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.utils.Role;
import org.bukkit.entity.Player;
import java.util.concurrent.ThreadLocalRandom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles role assignment for players at the start of a round.
 */
public class RoleAssigner {
    private final GameState gameState;

    public RoleAssigner(GameState gameState) {
        this.gameState = gameState;
    }

    /**
     * Assigns roles randomly to players: one Spark, one Medic, rest Innocents.
     * Ensures ALL players get a role assigned.
     */
    public void assignRoles(List<Player> players) {
        assignRoles(players, null);
    }

    /**
     * Assigns roles using a custom strategy when provided.
     */
    public void assignRoles(List<Player> players, RoleAssignmentStrategy strategy) {
        if (players == null || players.isEmpty()) {
            return;
        }
        
        // Filter out null players
        List<Player> validPlayers = new ArrayList<>();
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                validPlayers.add(player);
            }
        }
        
        if (validPlayers.isEmpty()) {
            return;
        }

        List<Player> orderedPlayers = resolveOrderedPlayers(validPlayers, strategy);

        // Assign Spark to first player (if exists)
        if (!orderedPlayers.isEmpty()) {
            gameState.setRole(orderedPlayers.get(0).getUniqueId(), Role.SPARK);
        }

        // Assign Medic to second player (if exists)
        if (orderedPlayers.size() > 1) {
            gameState.setRole(orderedPlayers.get(1).getUniqueId(), Role.MEDIC);
        }

        // Assign INNOCENT to all remaining players (ensures every player has a role)
        for (int i = 2; i < orderedPlayers.size(); i++) {
            gameState.setRole(orderedPlayers.get(i).getUniqueId(), Role.INNOCENT);
        }
    }

    private List<Player> resolveOrderedPlayers(List<Player> validPlayers, RoleAssignmentStrategy strategy) {
        List<Player> defaultOrder = new ArrayList<>(validPlayers);
        Collections.shuffle(defaultOrder, ThreadLocalRandom.current());

        if (strategy == null) {
            return defaultOrder;
        }

        try {
            List<Player> strategyOrder = strategy.orderPlayers(new ArrayList<>(validPlayers));
            if (strategyOrder == null || strategyOrder.isEmpty()) {
                return defaultOrder;
            }

            List<Player> sanitized = new ArrayList<>();
            for (Player player : strategyOrder) {
                if (player != null && validPlayers.contains(player) && !sanitized.contains(player)) {
                    sanitized.add(player);
                }
            }

            for (Player player : validPlayers) {
                if (!sanitized.contains(player)) {
                    sanitized.add(player);
                }
            }

            return sanitized;
        } catch (Exception ignored) {
            return defaultOrder;
        }
    }
}
