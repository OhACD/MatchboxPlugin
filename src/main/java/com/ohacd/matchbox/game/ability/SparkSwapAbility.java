package com.ohacd.matchbox.game.ability;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.Managers.InventoryManager;
import com.ohacd.matchbox.game.utils.Role;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import static org.bukkit.Bukkit.getLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Spark-only ability that swaps the Spark with a random alive player. Designed
 * to be invisible by preloading chunks and synchronising teleports within the
 * same tick while preserving velocity.
 */
public class SparkSwapAbility implements AbilityHandler {
    private final Plugin plugin;
    private final Random random = new Random();

    public SparkSwapAbility(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handleInventoryClick(InventoryClickEvent event, SessionGameContext context) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (!isSparkSwapActive(context, player.getUniqueId())) {
            return;
        }

        int slot = event.getSlot();
        int rawSlot = event.getRawSlot();
        if (slot != InventoryManager.getVisionSightPaperSlot() && rawSlot != InventoryManager.getVisionSightPaperSlot()) {
            return;
        }
        if (event.getClickedInventory() == null) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (!isSwapPaper(clicked)) {
            return;
        }

        event.setCancelled(true);
        attemptSwap(player, context, clicked);
    }

    @Override
    public void handlePlayerInteract(PlayerInteractEvent event, SessionGameContext context) {
        Player player = event.getPlayer();
        if (!isSparkSwapActive(context, player.getUniqueId())) {
            return;
        }

        switch (event.getAction()) {
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                break;
            default:
                return;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!isSwapPaper(heldItem)) {
            return;
        }
        ItemStack slotItem = player.getInventory().getItem(InventoryManager.getVisionSightPaperSlot());
        if (slotItem == null || !slotItem.equals(heldItem)) {
            return;
        }

        event.setCancelled(true);
        attemptSwap(player, context, heldItem);
    }

    private boolean isSparkSwapActive(SessionGameContext context, UUID playerId) {
        if (context == null || playerId == null) {
            return false;
        }
        GameState gameState = context.getGameState();
        if (!context.getPhaseManager().isPhase(GamePhase.SWIPE)) {
            return false;
        }
        if (gameState.getRole(playerId) != Role.SPARK) {
            return false;
        }
        return gameState.getSparkSecondaryAbility() == SparkSecondaryAbility.SPARK_SWAP;
    }

    private boolean isSwapPaper(ItemStack item) {
        return item != null && item.getType() == Material.PAPER;
    }

    private void attemptSwap(Player spark, SessionGameContext context, ItemStack triggerItem) {
        GameState gameState = context.getGameState();
        UUID sparkId = spark.getUniqueId();
        if (gameState.hasUsedSparkSwapThisRound(sparkId)) {
            return;
        }

        Player target = pickRandomTarget(context, sparkId);
        if (target == null) {
            return;
        }

        gameState.markUsedSparkSwap(sparkId);
        markPaperAsUsed(spark, triggerItem);

        Location sparkLoc = spark.getLocation().clone();
        Location targetLoc = target.getLocation().clone();
        Vector sparkVelocity = spark.getVelocity().clone();
        Vector targetVelocity = target.getVelocity().clone();

        CompletableFuture<Void> sparkChunk = ensureChunkLoaded(sparkLoc);
        CompletableFuture<Void> targetChunk = ensureChunkLoaded(targetLoc);

        CompletableFuture.allOf(sparkChunk, targetChunk).whenComplete((ignored, throwable) ->
            Bukkit.getScheduler().runTask(plugin, () -> performSwap(spark, target, sparkLoc, targetLoc, sparkVelocity, targetVelocity, context))
        );
    }

    private Player pickRandomTarget(SessionGameContext context, UUID sparkId) {
        List<Player> candidates = new ArrayList<>();
        for (UUID uuid : context.getGameState().getAlivePlayerIds()) {
            if (uuid == null || uuid.equals(sparkId)) {
                continue;
            }
            Player candidate = Bukkit.getPlayer(uuid);
            if (candidate != null && candidate.isOnline()) {
                candidates.add(candidate);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private CompletableFuture<Void> ensureChunkLoaded(Location location) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (location == null || location.getWorld() == null) {
            future.complete(null);
            return future;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (!location.getChunk().isLoaded()) {
                    location.getChunk().load(true);
                }
                future.complete(null);
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    private void performSwap(Player spark, Player target, Location sparkLoc, Location targetLoc,
            Vector sparkVelocity, Vector targetVelocity, SessionGameContext context) {

        Set<UUID> alivePlayerIds = context.getGameState().getAlivePlayerIds();
        if (alivePlayerIds == null || alivePlayerIds.isEmpty()) return;

        List<Player> viewers = new ArrayList<>();

        for (UUID uuid : alivePlayerIds) {
            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) continue;

            if (player.equals(spark) || player.equals(target)) continue;

            if (player.canSee(spark) || player.canSee(target)) {
                viewers.add(player);
            }
        }

        try {
            // Target and Spark destinations (swap)
            Location sparkDest = targetLoc.clone();
            Location targetDest = sparkLoc.clone();

            // Hide both players from all viewers immediately
            for (Player viewer : viewers) {
                try {
                    viewer.hidePlayer(plugin, spark);
                } catch (Exception ignored) {}
                try {
                    viewer.hidePlayer(plugin, target);
                } catch (Exception ignored) {}
            }

            // Teleport the players synchronously on main thread (same tick)
            spark.teleport(sparkDest);
            target.teleport(targetDest);

            // Preserve velocities and rotations (use original saved rotations)
            spark.setVelocity(targetVelocity);
            target.setVelocity(sparkVelocity);

            spark.setRotation(targetLoc.getYaw(), targetLoc.getPitch());
            target.setRotation(sparkLoc.getYaw(), sparkLoc.getPitch());

            spark.setFallDistance(0f);
            target.setFallDistance(0f);

            // Re-show players next tick to cause a fresh spawn on clients (no interpolation)
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player viewer : viewers) {
                    try {
                        viewer.showPlayer(plugin, spark);
                    } catch (Exception ignored) {}
                    try {
                        viewer.showPlayer(plugin, target);
                    } catch (Exception ignored) {}
                }
            });

        } catch (Exception ex) {
            plugin.getLogger().warning("Spark Swap failed: " + ex.getMessage());
        }
    }

    /**
     * Sends a spawn packet to the target and all viewers.
     * NOTE: kept for backward-compatibility / future use but unused by current hide/show implementation.
     * @param target
     * @param viewers
     */
    @SuppressWarnings("unused")
    private void sendSpawn(Player target, List<Player> viewers) {
        PacketContainer spawn = ProtocolLibrary.getProtocolManager()
                .createPacket(PacketType.Play.Server.SPAWN_ENTITY);

        spawn.getUUIDs().write(0, target.getUniqueId());
        spawn.getIntegers().write(0, target.getEntityId());

        spawn.getDoubles().write(0, target.getLocation().getX());
        spawn.getDoubles().write(1, target.getLocation().getY());
        spawn.getDoubles().write(2, target.getLocation().getZ());

        spawn.getBytes().write(0, (byte) (target.getLocation().getYaw() * 256F / 360F));
        spawn.getBytes().write(1, (byte) (target.getLocation().getPitch() * 256F / 360F));

        PacketContainer metadata = ProtocolLibrary.getProtocolManager()
                .createPacket(PacketType.Play.Server.ENTITY_METADATA);

        // WrappedDataWatcher 
        // metadata.getIntegers().write(0, target.getEntityId());
        // metadata.getWatchableCollectionModifier().write(0, WrappedDataWatcher.getEntityWatcher(target).getWatchableObjects());

        for (Player viewer : viewers) {
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, spawn);
                ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, metadata);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends a destroy packet to the target and all viewers.
     * NOTE: kept for backward-compatibility / future use but unused by current hide/show implementation.
     * @param target
     * @param viewers
     */
    @SuppressWarnings("unused")
    private void sendDestroy(Player target, List<Player> viewers) {
        PacketContainer destroy = ProtocolLibrary.getProtocolManager()
                .createPacket(PacketType.Play.Server.ENTITY_DESTROY);

        destroy.getIntLists().write(0, Collections.singletonList(target.getEntityId()));

        for (Player viewer : viewers) {
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, destroy);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void markPaperAsUsed(Player spark, ItemStack triggerItem) {
        if (spark == null || triggerItem == null) {
            return;
        }
        ItemStack usedIndicator = InventoryManager.createUsedIndicator(triggerItem);
        spark.getInventory().setItem(InventoryManager.getVisionSightPaperSlot(), usedIndicator);
        spark.updateInventory();
    }
}
