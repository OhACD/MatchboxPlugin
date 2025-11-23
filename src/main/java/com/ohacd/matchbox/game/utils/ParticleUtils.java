package com.ohacd.matchbox.game.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Utility class for displaying particles to players.
 */
public class ParticleUtils {
    
    /**
     * Shows red particles on a target player for a specific viewer for a duration.
     * Only the viewer can see the particles (recording-safe).
     * 
     * @param viewer The player who will see the particles
     * @param target The player to show particles on
     * @param durationSeconds How long to show particles (in seconds)
     * @param plugin Plugin instance for scheduling
     */
    public static void showRedParticlesOnPlayer(Player viewer, Player target, int durationSeconds, Plugin plugin) {
        if (viewer == null || target == null || !viewer.isOnline() || !target.isOnline()) {
            return;
        }

        // Determine particle type (DUST for colored particles, fallback to LAVA)
        Particle particleType;
        boolean useDustOptions = false;
        try {
            // Try DUST first (1.17+)
            particleType = Particle.valueOf("DUST");
            useDustOptions = true;
        } catch (IllegalArgumentException e) {
            // Fallback to REDSTONE (older versions)
            try {
                particleType = Particle.valueOf("REDSTONE");
                useDustOptions = true;
            } catch (IllegalArgumentException e2) {
                // Last resort: use a visible red particle
                particleType = Particle.LAVA;
                useDustOptions = false;
            }
        }
        
        final Particle particle = particleType;
        final boolean useDust = useDustOptions;
        
        // Calculate ticks (20 ticks per second)
        int ticks = durationSeconds * 20;
        int interval = 5; // Show particles every 5 ticks (4 times per second)
        int iterations = ticks / interval;

        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= iterations || !viewer.isOnline() || !target.isOnline()) {
                    cancel();
                    return;
                }

                // Show particles around the target player
                Location loc = target.getLocation();
                
                // Spawn red particles around the player (slightly above ground level)
                // Show particles in a circle around the player
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI * i) / 8;
                    double radius = 0.5;
                    double x = loc.getX() + Math.cos(angle) * radius;
                    double y = loc.getY() + 0.5; // At player's body level
                    double z = loc.getZ() + Math.sin(angle) * radius;
                    
                    Location particleLoc = new Location(loc.getWorld(), x, y, z);
                    
                    // Spawn particle visible only to the viewer
                    // Try to use dust options if available (for colored particles)
                    try {
                        if (useDust) {
                            org.bukkit.Particle.DustOptions dustOptions = new org.bukkit.Particle.DustOptions(
                                org.bukkit.Color.fromRGB(255, 0, 0), // Red color
                                1.0f // Size
                            );
                            viewer.spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0, dustOptions);
                        } else {
                            // Fallback: use particle without color options
                            viewer.spawnParticle(particle, particleLoc, 1);
                        }
                    } catch (Exception e) {
                        // Fallback: use simple particle spawn
                        viewer.spawnParticle(particle, particleLoc, 1);
                    }
                }

                count++;
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    /**
     * Shows red particles on multiple target players for a specific viewer.
     * 
     * @param viewer The player who will see the particles
     * @param targets The players to show particles on
     * @param durationSeconds How long to show particles (in seconds)
     * @param plugin Plugin instance for scheduling
     */
    public static void showRedParticlesOnPlayers(Player viewer, java.util.Collection<Player> targets, int durationSeconds, Plugin plugin) {
        for (Player target : targets) {
            if (target != null && target.isOnline()) {
                showRedParticlesOnPlayer(viewer, target, durationSeconds, plugin);
            }
        }
    }
}
