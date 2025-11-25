package com.ohacd.matchbox.game.ability;

import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.utils.ParticleUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Legacy Hunter Vision implementation that shows temporary particles on all alive players.
 * Used whenever ProtocolLib is unavailable so the plugin keeps functioning.
 */
public class FallbackHunterVisionAdapter implements HunterVisionAdapter {
    private final Plugin plugin;

    public FallbackHunterVisionAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void startVision(Player spark, SessionGameContext context) {
        if (spark == null || !spark.isOnline() || context == null) {
            return;
        }
        GameState gameState = context.getGameState();
        if (gameState == null) {
            return;
        }

        Set<UUID> alive = gameState.getAlivePlayerIds();
        if (alive == null || alive.isEmpty()) {
            return;
        }

        Set<Player> targets = new HashSet<>();
        UUID sparkId = spark.getUniqueId();
        for (UUID uuid : alive) {
            if (uuid == null || uuid.equals(sparkId)) {
                continue;
            }
            Player candidate = spark.getServer().getPlayer(uuid);
            if (candidate != null && candidate.isOnline()) {
                targets.add(candidate);
            }
        }

        if (targets.isEmpty()) {
            return;
        }

        for (Player target : targets) {
            ParticleUtils.showRedParticlesOnPlayer(spark, target, 15, plugin);
        }
        plugin.getLogger().info("Fallback Hunter Vision activated for spark " + spark.getName() + " (particles only).");
    }

    @Override
    public void stopVision(UUID sparkId) {
        // Nothing to clean up for the fallback implementation.
    }

    @Override
    public void stopVisionForPlayers(Collection<UUID> sparkIds) {
        // Nothing to clean up for the fallback implementation.
    }

    @Override
    public boolean isAdvanced() {
        return false;
    }
}
