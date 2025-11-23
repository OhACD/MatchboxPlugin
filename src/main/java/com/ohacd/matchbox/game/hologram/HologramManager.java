package com.ohacd.matchbox.game.hologram;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages short-lived holograms (armor stands) shown above players.
 * Ensures previously created holograms for the same player are canceled and removed,
 * avoids race conditions where an old task could remove a newly created hologram.
 */
public class HologramManager {
    private final Plugin plugin;

    // Track both ArmorStand and the task that updates/removes it
    private static class HologramEntry {
        final ArmorStand stand;
        final BukkitRunnable task;

        HologramEntry(ArmorStand stand, BukkitRunnable task) {
            this.stand = stand;
            this.task = task;
        }
    }

    private final Map<UUID, HologramEntry> active = new ConcurrentHashMap<>();

    public HologramManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void showTextAbove(Player player, String text, int ticks) {
        // Run on main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            UUID id = player.getUniqueId();

            // Remove and cancel any previous hologram for this player
            HologramEntry prev = active.remove(id);
            if (prev != null) {
                try {
                    if (prev.task != null) prev.task.cancel();
                } catch (Exception ignored) {}
                try {
                    if (prev.stand != null && !prev.stand.isDead()) prev.stand.remove();
                } catch (Exception ignored) {}
            }

            Location location = player.getLocation().clone().add(0, 2.2, 0); // Tune height here
            ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setInvisible(true);
            stand.customName(Component.text(text));
            stand.setCustomNameVisible(true);

            // Create the updating task and store the entry
            HologramEntry entry = new HologramEntry(stand, null);

            BukkitRunnable task = new BukkitRunnable() {
                int remaining = ticks;

                @Override
                public void run() {
                    if (remaining <= 0 || stand.isDead() || !player.isOnline()) {
                        try {
                            if (!stand.isDead()) stand.remove();
                        } catch (Exception ignored) {}
                        // Only remove the map entry if it still maps to this entry (avoid removing newer entries)
                        active.remove(id, entry);
                        cancel();
                        return;
                    }
                    Location newLocation = player.getLocation().clone().add(0, 2.2, 0);
                    try {
                        stand.teleport(newLocation);
                    } catch (Exception ignored) {}
                    remaining--;
                }
            };

            // assign task reference and store
            HologramEntry finalEntry = new HologramEntry(stand, task);
            active.put(id, finalEntry);
            task.runTaskTimer(plugin, 0L, 1L);
        });
    }

    public void clearAll() {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (HologramEntry entry : active.values()) {
                try {
                    if (entry.task != null) entry.task.cancel();
                } catch (Exception ignored) {}
                try {
                    if (entry.stand != null && !entry.stand.isDead()) entry.stand.remove();
                } catch (Exception ignored) {}
            }
            active.clear();
        });
    }
}
