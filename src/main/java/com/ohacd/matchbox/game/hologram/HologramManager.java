package com.ohacd.matchbox.game.hologram;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {
    private final Plugin plugin;
    private final Map<UUID, ArmorStand> active = new ConcurrentHashMap<>();

    public HologramManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void showTextAbove(Player player, String text, int ticks) {
        // Create Armor stand, set custom name and follow the player for the specified tick
        plugin.getServer().getScheduler().runTask(plugin, () -> {
           Location location  = player.getLocation().clone().add(0, 2.2, 0); // Tune height here
            ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setInvisible(true);
            stand.setCustomName(text);
            stand.setCustomNameVisible(true);

            // tracking logic
            UUID id = player.getUniqueId();
            // Overrides previous messages to allow message stacking
            active.put(id, stand);

            // Schedule a bukkitRunnable to follow the player then remove after the specified time
            new BukkitRunnable() {
                int remaining = ticks;
                @Override
                public void run() {
                    if (remaining <= 0 || stand.isDead() || !player.isOnline()) {
                        stand.remove();
                        active.remove(id);
                        cancel();
                        return;
                    }
                    Location newLocation = player.getLocation().clone().add(0, 2.2, 0);
                    stand.teleport(newLocation);
                    remaining--;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        });
    }

    public void clearAll() {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            active.values().forEach(a -> {if (!a.isDead()) a.remove(); });
            active.clear();
        });
    }
}
