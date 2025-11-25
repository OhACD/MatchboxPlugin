package com.ohacd.matchbox.game.ability;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.state.GameState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProtocolLib-powered Hunter Vision implementation with a dynamic wallhack-style glow.
 * Only visible to the spark.
 */
public class ProtocolLibHunterVisionAdapter implements HunterVisionAdapter {
    private static final double RADIUS_SQUARED = 35 * 35;
    private static final long DURATION_MS = 15_000L;
    private static final long UPDATE_INTERVAL_TICKS = 5L;
    private static final WrappedDataWatcher.WrappedDataWatcherObject GLOW_FLAGS = createGlowWatcher();

    private final Plugin plugin;
    private final ProtocolManager protocolManager;
    private final FallbackHunterVisionAdapter particleFallback;
    private final Map<UUID, VisionTask> activeSessions = new ConcurrentHashMap<>();

    public ProtocolLibHunterVisionAdapter(Plugin plugin) {
        this.plugin = plugin;
        ProtocolManager manager = null;
        try {
            manager = ProtocolLibrary.getProtocolManager();
        } catch (Throwable t) {
            plugin.getLogger().warning("ProtocolLib not ready (" + t.getMessage() + "). Hunter Vision will fall back to particles.");
        }
        this.protocolManager = manager;
        this.particleFallback = new FallbackHunterVisionAdapter(plugin);
    }

    @Override
    public void startVision(Player spark, SessionGameContext context) {
        if (spark == null || context == null) return;
        if (protocolManager == null) {
            particleFallback.startVision(spark, context);
            return;
        }
        stopVision(spark.getUniqueId());
        try {
            VisionTask task = new VisionTask(spark, context);
            activeSessions.put(spark.getUniqueId(), task);
            task.start();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to start Hunter Vision glow for " + spark.getName() + ": " + e.getMessage());
            particleFallback.startVision(spark, context);
        }
    }

    @Override
    public void stopVision(UUID sparkId) {
        if (sparkId == null) return;
        VisionTask task = activeSessions.remove(sparkId);
        if (task != null) task.stop();
    }

    @Override
    public void stopVisionForPlayers(Collection<UUID> sparkIds) {
        if (sparkIds == null) return;
        for (UUID id : sparkIds) stopVision(id);
    }

    @Override
    public boolean isAdvanced() { return true; }

    private class VisionTask implements Runnable {
        private final Player spark;
        private final SessionGameContext context;
        private final Set<UUID> glowingTargets = new HashSet<>();
        private final long endTime;
        private final GameState gameState;
        private BukkitTask bukkitTask;

        private VisionTask(Player spark, SessionGameContext context) {
            this.spark = spark;
            this.context = context;
            this.endTime = System.currentTimeMillis() + DURATION_MS;
            this.gameState = context.getGameState();
        }

        private void start() {
            this.bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, this, 0L, UPDATE_INTERVAL_TICKS);
        }

        private void stop() {
            if (bukkitTask != null) bukkitTask.cancel();
            updateGlow(new HashSet<>(glowingTargets), false);
            glowingTargets.clear();
        }

        @Override
        public void run() {
            try {
                if (!spark.isOnline() || System.currentTimeMillis() >= endTime || gameState == null
                        || !gameState.isGameActive()) {
                    stopVision(spark.getUniqueId());
                    return;
                }

                Set<UUID> currentTargets = computeTargets();

                // Activate glow for new entries
                for (UUID uuid : currentTargets) {
                    if (!glowingTargets.contains(uuid)) {
                        Player target = Bukkit.getPlayer(uuid);
                        if (target != null && target.isOnline()) {
                            if (!sendGlowPacket(spark, target, true)) {
                                failover("Unable to glow " + target.getName());
                                return;
                            }
                            glowingTargets.add(uuid);
                        }
                    }
                }

                // Remove glow for players that left the radius
                Set<UUID> toRemove = new HashSet<>();
                for (UUID uuid : glowingTargets) {
                    if (!currentTargets.contains(uuid)) {
                        Player target = Bukkit.getPlayer(uuid);
                        if (target != null && target.isOnline() && !sendGlowPacket(spark, target, false)) {
                            failover("Unable to clear glow for " + target.getName());
                            return;
                        }
                        toRemove.add(uuid);
                    }
                }
                glowingTargets.removeAll(toRemove);

                // End session if timer elapsed
                if (System.currentTimeMillis() >= endTime) stopVision(spark.getUniqueId());
            } catch (Exception e) {
                failover("Hunter Vision task failed: " + e.getMessage());
            }
        }

        private void failover(String reason) {
            plugin.getLogger().warning(reason + ". Falling back to particle Hunter Vision for " + spark.getName());
            stopVision(spark.getUniqueId());
            particleFallback.startVision(spark, context);
        }

        private Set<UUID> computeTargets() {
            Set<UUID> result = new HashSet<>();
            Set<UUID> aliveIds = gameState.getAlivePlayerIds();
            if (aliveIds == null) return result;
            for (UUID uuid : aliveIds) {
                if (uuid == null || uuid.equals(spark.getUniqueId())) continue;
                Player target = Bukkit.getPlayer(uuid);
                if (target == null || !target.isOnline()) continue;
                if (!target.getWorld().equals(spark.getWorld())) continue;
                if (target.getLocation().distanceSquared(spark.getLocation()) <= RADIUS_SQUARED) result.add(uuid);
            }
            return result;
        }

        private void updateGlow(Collection<UUID> uuids, boolean glow) {
            if (uuids == null || uuids.isEmpty()) return;
            for (UUID uuid : uuids) {
                Player target = Bukkit.getPlayer(uuid);
                if (target != null && target.isOnline()) sendGlowPacket(spark, target, glow);
            }
        }
    }

    private boolean sendGlowPacket(Player viewer, Player target, boolean glow) {
        if (viewer == null || target == null || protocolManager == null) return false;
        if (!viewer.isOnline() || !target.isOnline()) return false;
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            packet.getIntegers().write(0, target.getEntityId());

            WrappedDataWatcher liveWatcher = WrappedDataWatcher.getEntityWatcher(target);
            Byte currentMask = liveWatcher != null ? liveWatcher.getByte(0) : null;
            byte mask = currentMask != null ? currentMask : 0x00;
            byte updated = glow ? (byte) (mask | 0x40) : (byte) (mask & ~0x40);

            WrappedDataValue glowValue = new WrappedDataValue(GLOW_FLAGS.getIndex(), GLOW_FLAGS.getSerializer(), updated);
            List<Object> metadata = new ArrayList<>(1);
            metadata.add(glowValue.getHandle());
            packet.getModifier().write(1, metadata);
            protocolManager.sendServerPacket(viewer, packet);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send glow packet to " + viewer.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private static WrappedDataWatcher.WrappedDataWatcherObject createGlowWatcher() {
        WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get((java.lang.reflect.Type) Byte.class);
        return new WrappedDataWatcher.WrappedDataWatcherObject(0, serializer);
    }
}
