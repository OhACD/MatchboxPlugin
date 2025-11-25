package com.ohacd.matchbox.game.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
     * Shows subtle marker particles on a target player for a specific viewer for a duration.
     * Only the viewer can see the particles (recording-safe).
     *
     * @param viewer The player who will see the particles
     * @param target The player to show particles on
     * @param durationSeconds How long to show particles (in seconds)
     * @param plugin Plugin instance for scheduling
     */
    public static void showMarkerParticlesOnPlayer(Player viewer, Player target, int durationSeconds, Plugin plugin) {
        if (viewer == null || target == null || !viewer.isOnline() || !target.isOnline()) {
            return;
        }

        // Determine particle type (prefer dust for color support, fallback to LAVA)
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
                // Last resort: use a visible fallback particle
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
                
                // Spawn marker particles around the player (slightly above ground level)
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
                            org.bukkit.Color.fromRGB(255, 0, 0),
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
     * Shows subtle marker particles on multiple target players for a specific viewer.
     *
     * @param viewer The player who will see the particles
     * @param targets The players to show particles on
     * @param durationSeconds How long to show particles (in seconds)
     * @param plugin Plugin instance for scheduling
     */
    public static void showMarkerParticlesOnPlayers(Player viewer, Collection<Player> targets, int durationSeconds, Plugin plugin) {
        for (Player target : targets) {
            if (target != null && target.isOnline()) {
                showMarkerParticlesOnPlayer(viewer, target, durationSeconds, plugin);
            }
        }
    }
    
    /**
     * Shows colored particles on a target player visible to ALL nearby players for a brief moment.
     * Used for subtle visual cues (infection, cure) that everyone can see.
     * 
     * @param target The player to show particles on
     * @param color The color (RGB)
     * @param durationTicks How long to show particles (in ticks, typically 5-10 for split second)
     * @param plugin Plugin instance for scheduling
     */
    public static void showColoredParticlesToEveryone(Player target, org.bukkit.Color color, int durationTicks, Plugin plugin) {
        if (target == null || !target.isOnline() || target.getWorld() == null) {
            return;
        }

        // Snapshot of viewers so we can safely iterate even if players join/leave mid-effect
        List<Player> viewers = new ArrayList<>();
        for (Player viewer : target.getWorld().getPlayers()) {
            if (viewer != null && viewer.isOnline()) {
                viewers.add(viewer);
            }
        }
        if (viewers.isEmpty()) {
            return;
        }
        
        // Determine particle type
        Particle particleType;
        boolean useDustOptions = false;
        try {
            particleType = Particle.valueOf("DUST");
            useDustOptions = true;
        } catch (IllegalArgumentException e) {
            try {
                particleType = Particle.valueOf("REDSTONE");
                useDustOptions = true;
            } catch (IllegalArgumentException e2) {
                particleType = Particle.HEART; // Fallback - visible but subtle
                useDustOptions = false;
            }
        }
        
        final Particle particle = particleType;
        final boolean useDust = useDustOptions;
        final org.bukkit.Color finalColor = color;
        
        // Show particles for a very brief moment (subtle cue)
        // Use small radius and few particles to make it hard to see but not impossible
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= durationTicks || !target.isOnline() || target.getWorld() == null) {
                    cancel();
                    return;
                }
                
                Location loc = target.getLocation();
                
                // Show subtle particles - small radius, few particles, at player's body level
                // Make it hard to see but not impossible
                int particleCount = 3; // Very few particles for subtlety
                double radius = 0.3; // Small radius
                
                for (int i = 0; i < particleCount; i++) {
                    double angle = (2 * Math.PI * i) / particleCount;
                    double x = loc.getX() + Math.cos(angle) * radius;
                    double y = loc.getY() + 0.5; // At player's body level
                    double z = loc.getZ() + Math.sin(angle) * radius;
                    
                    Location particleLoc = new Location(loc.getWorld(), x, y, z);
                    
                    // Spawn the particle for every viewer individually so the cue is guaranteed
                    for (Player viewer : viewers) {
                        if (viewer.getWorld() != loc.getWorld()) {
                            continue;
                        }
                        try {
                            if (useDust) {
                                org.bukkit.Particle.DustOptions dustOptions = new org.bukkit.Particle.DustOptions(
                                    finalColor,
                                    0.5f // Small size for subtlety
                                );
                                viewer.spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0, dustOptions);
                            } else {
                                viewer.spawnParticle(particle, particleLoc, 1);
                            }
                        } catch (Exception e) {
                            viewer.spawnParticle(particle, particleLoc, 1);
                        }
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick for brief duration
    }
}
