package com.ohacd.matchbox.game.ability;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.ohacd.matchbox.game.SessionGameContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProtocolLib-powered Hunter Vision implementation that sends per-viewer glow packets.
 */
public class ProtocolLibHunterVisionAdapter implements HunterVisionAdapter {
    private static final double RADIUS_SQUARED = 35 * 35;
    private static final long DURATION_MS = 15_000L;
    private static final long UPDATE_INTERVAL_TICKS = 5L;
    private static final WrappedDataWatcher.WrappedDataWatcherObject GLOW_FLAGS = createGlowWatcher();

    private final Plugin plugin;
    private final ProtocolManager protocolManager;
    private final Map<UUID, VisionTask> activeSessions = new ConcurrentHashMap<>();

    public ProtocolLibHunterVisionAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    @Override
    public void startVision(Player spark, SessionGameContext context) {
        if (spark == null || context == null) {
            return;
        }
        stopVision(spark.getUniqueId());
        VisionTask task = new VisionTask(spark, context);
        activeSessions.put(spark.getUniqueId(), task);
        task.start();
    }

    @Override
    public void stopVision(UUID sparkId) {
        if (sparkId == null) {
            return;
        }
        VisionTask task = activeSessions.remove(sparkId);
        if (task != null) {
            task.stop();
        }
    }

    @Override
    public void stopVisionForPlayers(Collection<UUID> sparkIds) {
        if (sparkIds == null) {
            return;
        }
        for (UUID id : sparkIds) {
            stopVision(id);
        }
    }

    @Override
    public boolean isAdvanced() {
        return true;
    }

    private class VisionTask implements Runnable {
        private final Player spark;
        private final SessionGameContext context;
        private final Set<UUID> glowingTargets = new HashSet<>();
        private final long endTime;
        private BukkitTask bukkitTask;

        private VisionTask(Player spark, SessionGameContext context) {
            this.spark = spark;
            this.context = context;
            this.endTime = System.currentTimeMillis() + DURATION_MS;
        }

        private void start() {
            this.bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, this, 0L, UPDATE_INTERVAL_TICKS);
        }

        private void stop() {
            if (bukkitTask != null) {
                bukkitTask.cancel();
            }
            updateGlow(new HashSet<>(glowingTargets), false);
            glowingTargets.clear();
        }

        @Override
        public void run() {
            if (!spark.isOnline() || System.currentTimeMillis() >= endTime || context.getGameState() == null || !context.getGameState().isGameActive()) {
                stopVision(spark.getUniqueId());
                return;
            }
            Set<UUID> currentTargets = computeTargets();

            // Activate glow for new entries
            for (UUID uuid : currentTargets) {
                if (!glowingTargets.contains(uuid)) {
                    Player target = Bukkit.getPlayer(uuid);
                    if (target != null && target.isOnline()) {
                        sendGlowPacket(spark, target, true);
                        glowingTargets.add(uuid);
                    }
                }
            }

            // Remove glow for players that left the radius
            Set<UUID> toRemove = new HashSet<>();
            for (UUID uuid : glowingTargets) {
                if (!currentTargets.contains(uuid)) {
                    Player target = Bukkit.getPlayer(uuid);
                    if (target != null && target.isOnline()) {
                        sendGlowPacket(spark, target, false);
                    }
                    toRemove.add(uuid);
                }
            }
            glowingTargets.removeAll(toRemove);

            // End session if timer elapsed
            if (System.currentTimeMillis() >= endTime) {
                stopVision(spark.getUniqueId());
            }
        }

        private Set<UUID> computeTargets() {
            Set<UUID> result = new HashSet<>();
            Set<UUID> aliveIds = context.getGameState().getAlivePlayerIds();
            if (aliveIds == null) {
                return result;
            }
            for (UUID uuid : aliveIds) {
                if (uuid == null || uuid.equals(spark.getUniqueId())) {
                    continue;
                }
                Player target = Bukkit.getPlayer(uuid);
                if (target == null || !target.isOnline()) {
                    continue;
                }
                if (!target.getWorld().equals(spark.getWorld())) {
                    continue;
                }
                if (target.getLocation().distanceSquared(spark.getLocation()) <= RADIUS_SQUARED) {
                    result.add(uuid);
                }
            }
            return result;
        }

        private void updateGlow(Collection<UUID> uuids, boolean glow) {
            if (uuids == null || uuids.isEmpty()) {
                return;
            }
            for (UUID uuid : uuids) {
                Player target = Bukkit.getPlayer(uuid);
                if (target != null && target.isOnline()) {
                    sendGlowPacket(spark, target, glow);
                }
            }
        }
    }

    private void sendGlowPacket(Player viewer, Player target, boolean glow) {
        if (viewer == null || target == null || !viewer.isOnline()) {
            return;
        }
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            packet.getIntegers().write(0, target.getEntityId());

            /*
             * >>> CRASH FIX <<<
             *
             * Previously we cloned the target's full WrappedDataWatcher:
             *   WrappedDataWatcher watcher = WrappedDataWatcher.getEntityWatcher(target).deepClone();
             * and then wrote watcher.getWatchableObjects() into the packet. That produced a list of
             * ProtocolLib "DataItem" objects which Paper 1.21.x attempted to cast to Mojang's
             * "DataValue" type when serializing the packet, causing the ClassCastException.
             *
             * Fix approach:
             * - Create a NEW, minimal WrappedDataWatcher that only contains metadata index 0 (entity flags).
             * - Attempt to read the current flags safely; if that fails, fallback to target.isGlowing().
             * - Toggle the 0x40 glow bit and write only that single metadata entry into the packet.
             */

            // Safe read of current flags (best-effort; do not clone watcher objects)
            byte mask = 0;
            try {
                WrappedDataWatcher currentWatcher = WrappedDataWatcher.getEntityWatcher(target);
                Object current = null;
                try {
                    current = currentWatcher.getObject(GLOW_FLAGS);
                } catch (Exception ignored) {
                    // getObject may throw or return unexpected types in some server/protocol combinations
                }
                if (current instanceof Byte b) {
                    mask = b;
                } else if (current instanceof Number n) {
                    mask = n.byteValue();
                } else {
                    // Fallback: use Spigot's visible glowing state (not ideal but safe)
                    mask = (byte) (target.isGlowing() ? 0x40 : 0x00);
                }
            } catch (Exception ignored) {
                // Last-resort fallback
                mask = (byte) (target.isGlowing() ? 0x40 : 0x00);
            }

            if (glow) {
                mask |= 0x40;
            } else {
                mask &= ~0x40;
            }

            // Build a new watcher containing only the single index we want to change
            WrappedDataWatcher watcherToSend = new WrappedDataWatcher();
            watcherToSend.setObject(GLOW_FLAGS, mask);

            // Write only the minimal watchable objects — avoids DataItem/DataValue mismatch
            packet.getWatchableCollectionModifier().write(0, watcherToSend.getWatchableObjects());
            protocolManager.sendServerPacket(viewer, packet);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send glow packet: " + e.getMessage());
        }
    }

    private static WrappedDataWatcher.WrappedDataWatcherObject createGlowWatcher() {
        WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Byte.class);
        return new WrappedDataWatcher.WrappedDataWatcherObject(0, serializer);
    }
}
