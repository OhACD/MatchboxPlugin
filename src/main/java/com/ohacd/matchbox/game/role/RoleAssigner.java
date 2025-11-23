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
     */
    public void assignRoles(List<Player> players) {
        List<Player> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);

        if (!shuffled.isEmpty()) {
            gameState.setRole(shuffled.get(0).getUniqueId(), Role.SPARK);
        }
        if (shuffled.size() > 1) {
            gameState.setRole(shuffled.get(1).getUniqueId(), Role.MEDIC);
        }
        for (int i = 2; i < shuffled.size(); i++) {
            gameState.setRole(shuffled.get(i).getUniqueId(), Role.INNOCENT);
        }
    }
}
