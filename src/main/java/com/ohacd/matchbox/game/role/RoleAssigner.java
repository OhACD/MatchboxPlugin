package com.ohacd.matchbox.game.role;

import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.utils.Role;
import org.bukkit.entity.Player;

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
        
        List<Player> shuffled = new ArrayList<>(validPlayers);
        Collections.shuffle(shuffled);

        // Assign Spark to first player (if exists)
        if (!shuffled.isEmpty()) {
            gameState.setRole(shuffled.get(0).getUniqueId(), Role.SPARK);
        }
        
        // Assign Medic to second player (if exists)
        if (shuffled.size() > 1) {
            gameState.setRole(shuffled.get(1).getUniqueId(), Role.MEDIC);
        }
        
        // Assign INNOCENT to all remaining players (ensures every player has a role)
        for (int i = 2; i < shuffled.size(); i++) {
            gameState.setRole(shuffled.get(i).getUniqueId(), Role.INNOCENT);
        }
        
        // Safety check: if only 1 player, they become Spark (no Medic)
        // If only 2 players, first is Spark, second is Medic (no Innocents)
        // This is already handled by the logic above
    }
}
