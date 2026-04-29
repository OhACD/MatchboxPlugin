package com.ohacd.matchbox.game.sign;

import io.papermc.paper.block.BlockPredicate;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAdventurePredicate;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockType;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages sign-mode items and per-session sign placement tracking.
 *
 * <p>When sign mode is enabled in config, players receive an axe and two stacks
 * of oak signs during the swipe phase. Placed signs are tracked per session and
 * automatically removed from the world when discussion starts.</p>
 */
public class SignModeManager {

    /** Hotbar slot for the wooden axe. */
    public static final int SIGN_AXE_SLOT = 0;
    /** Hotbar slot for the first sign stack. */
    public static final int SIGN_STACK_1_SLOT = 1;
    /** Hotbar slot for the second sign stack. */
    public static final int SIGN_STACK_2_SLOT = 2;

    private static final int SIGN_STACK_SIZE = 16;

    private final Plugin plugin;

    /**
     * PDC key used to mark items that belong to sign mode so they can be
     * identified reliably without relying on display names.
     */
    private final NamespacedKey signModeItemKey;

    /**
     * Per-session tracking: sessionName → (block location → UUID of the player who placed it).
     * ConcurrentHashMap is used to allow safe access from multiple threads (e.g. async events).
     */
    private final Map<String, Map<Location, UUID>> sessionSigns = new ConcurrentHashMap<>();

    public SignModeManager(Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        this.plugin = plugin;
        this.signModeItemKey = new NamespacedKey(plugin, "sign-mode-item");
    }

    // -------------------------------------------------------------------------
    // Item creation
    // -------------------------------------------------------------------------

    /**
     * Creates the wooden axe given to each player in sign mode.
     * The item is marked with a PDC tag and set unbreakable so the existing
     * {@link com.ohacd.matchbox.game.utils.listeners.GameItemProtectionListener}
     * will prevent players from dropping or moving it.
     */
    public ItemStack createSignAxe() {
        ItemStack axe = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = axe.getItemMeta();
        if (meta == null) return axe;

        meta.displayName(Component.text("Sign Axe", NamedTextColor.GRAY));
        meta.lore(List.of(Component.text("Use signs to chat", NamedTextColor.DARK_GRAY)));
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        meta.getPersistentDataContainer().set(signModeItemKey, PersistentDataType.BYTE, (byte) 1);

        axe.setItemMeta(meta);
        List<BlockType> breakableSigns = new java.util.ArrayList<>();
        for (Material mat : Material.values()) {
            if (mat.isBlock() && mat.name().contains("SIGN")) {
                breakableSigns.add(mat.asBlockType());
            }
        }

        axe.setData(
            DataComponentTypes.CAN_BREAK,
            ItemAdventurePredicate.itemAdventurePredicate()
                .addPredicate(
                    BlockPredicate.predicate()
                        .blocks(RegistrySet.keySetFromValues(RegistryKey.BLOCK, breakableSigns))
                        .build()
                )
                .build()
        );
        return axe;
    }

    /**
     * Creates a stack of 16 oak signs for use in sign mode.
     * Sets an adventure-mode placement predicate for all non-air block materials
     * so players can place them on any surface.
     */
    public ItemStack createSignStack() {
        ItemStack signs = new ItemStack(Material.OAK_SIGN, SIGN_STACK_SIZE);
        ItemMeta meta = signs.getItemMeta();
        if (meta == null) return signs;

        meta.displayName(Component.text("Sign (Chat)", NamedTextColor.GRAY));
        meta.lore(List.of(Component.text("Place a sign to send a message", NamedTextColor.DARK_GRAY)));
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_PLACED_ON);
        meta.getPersistentDataContainer().set(signModeItemKey, PersistentDataType.BYTE, (byte) 1);

        List<BlockType> placeableOn = new java.util.ArrayList<>();
        for (Material mat : Material.values()) {
            if (mat.isBlock() && !mat.isAir()) {
                placeableOn.add(mat.asBlockType());
            }
        }

        signs.setItemMeta(meta);
        signs.setData(
            DataComponentTypes.CAN_PLACE_ON,
            ItemAdventurePredicate.itemAdventurePredicate()
                .addPredicate(
                    BlockPredicate.predicate()
                        .blocks(RegistrySet.keySetFromValues(RegistryKey.BLOCK, placeableOn))
                        .build()
                )
                .build()
        );
        return signs;
    }

    /**
     * Returns {@code true} if the given item is a sign-mode item (axe or sign stack)
     * created by this manager.
     */
    public boolean isSignModeItem(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(signModeItemKey, PersistentDataType.BYTE);
    }

    // -------------------------------------------------------------------------
    // Item distribution
    // -------------------------------------------------------------------------

    /**
     * Gives each player the sign-mode kit: axe in slot 0, two stacks of 16
     * oak signs in slots 1 and 2.  Existing items in those hotbar slots are
     * overwritten; items that are displaced are silently discarded because
     * inventories are already cleared prior to this call.
     *
     * @param players the collection of alive players to receive sign items
     */
    public void giveSignItems(Collection<org.bukkit.entity.Player> players) {
        if (players == null || players.isEmpty()) return;

        ItemStack axe = createSignAxe();
        ItemStack signs1 = createSignStack();
        ItemStack signs2 = createSignStack();

        for (org.bukkit.entity.Player player : players) {
            if (player == null || !player.isOnline()) continue;
            try {
                player.getInventory().setItem(SIGN_AXE_SLOT, axe.clone());
                player.getInventory().setItem(SIGN_STACK_1_SLOT, signs1.clone());
                player.getInventory().setItem(SIGN_STACK_2_SLOT, signs2.clone());
                player.updateInventory();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to give sign items to " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Placement tracking
    // -------------------------------------------------------------------------

    /**
     * Registers a sign block that was placed by a player during an active session.
     *
     * @param sessionName the name of the session the sign belongs to
     * @param location    the world location of the placed sign block
     * @param placerId    the UUID of the player who placed the sign
     */
    public void registerSignPlacement(String sessionName, Location location, UUID placerId) {
        if (sessionName == null || location == null || placerId == null) return;
        Location key = toBlockKey(location);
        sessionSigns
            .computeIfAbsent(sessionName, k -> new ConcurrentHashMap<>())
            .put(key, placerId);
        plugin.getLogger().fine("Tracked sign at " + formatLoc(key) + " by " + placerId + " in session " + sessionName);
    }

    /**
     * Removes a sign from the tracking map (e.g. when it is broken).
     * This does NOT remove the block from the world; use
     * {@link #clearSessionSigns(String)} for that.
     *
     * @param sessionName the session the sign belongs to
     * @param location    the location of the sign block
     */
    public void removeSignTracking(String sessionName, Location location) {
        if (sessionName == null || location == null) return;
        Map<Location, UUID> signs = sessionSigns.get(sessionName);
        if (signs != null) {
            signs.remove(toBlockKey(location));
        }
    }

    /**
     * Returns {@code true} if the block at the given location was placed as a
     * sign-mode sign during the specified session.
     */
    public boolean isTrackedSign(String sessionName, Location location) {
        if (sessionName == null || location == null) return false;
        Map<Location, UUID> signs = sessionSigns.get(sessionName);
        return signs != null && signs.containsKey(toBlockKey(location));
    }

    /**
     * Returns the UUID of the player who placed the sign at the given location,
     * or {@code null} if the location is not tracked.
     */
    public UUID getSignPlacer(String sessionName, Location location) {
        if (sessionName == null || location == null) return null;
        Map<Location, UUID> signs = sessionSigns.get(sessionName);
        return signs != null ? signs.get(toBlockKey(location)) : null;
    }

    /**
     * Returns the number of signs currently tracked for the given session.
     */
    public int getSignCount(String sessionName) {
        if (sessionName == null) return 0;
        Map<Location, UUID> signs = sessionSigns.get(sessionName);
        return signs != null ? signs.size() : 0;
    }

    // -------------------------------------------------------------------------
    // World cleanup
    // -------------------------------------------------------------------------

    /**
     * Removes all signs that were placed during the given session from both the
     * world and the tracking map.  Called at the start of the discussion phase
     * so the map is clean before players are teleported to the discussion area.
     *
     * @param sessionName the session whose signs should be cleared
     */
    public void clearSessionSigns(String sessionName) {
        if (sessionName == null) return;

        Map<Location, UUID> signs = sessionSigns.remove(sessionName);
        if (signs == null || signs.isEmpty()) {
            plugin.getLogger().info("No signs to clear for session: " + sessionName);
            return;
        }

        int cleared = 0;
        int skipped = 0;

        for (Location loc : signs.keySet()) {
            if (loc == null) {
                skipped++;
                continue;
            }
            World world = loc.getWorld();
            if (world == null) {
                skipped++;
                continue;
            }

            try {
                Block block = world.getBlockAt(loc);
                String typeName = block.getType().name();
                // Match any sign variant (OAK_SIGN, SPRUCE_WALL_SIGN, etc.)
                if (typeName.contains("SIGN")) {
                    block.setType(Material.AIR);
                    cleared++;
                } else {
                    // Block is no longer a sign (already removed by other means)
                    skipped++;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to remove sign at " + formatLoc(loc) + " for session " + sessionName + ": " + e.getMessage());
                skipped++;
            }
        }

        plugin.getLogger().info("Sign cleanup for session '" + sessionName + "': removed=" + cleared + ", skipped=" + skipped);
    }

    /**
     * Cleans up all tracked sessions.  Called on emergency shutdown.
     */
    public void clearAll() {
        for (String sessionName : new HashSet<>(sessionSigns.keySet())) {
            clearSessionSigns(sessionName);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Normalises a {@link Location} to integer block coordinates with yaw=0,
     * pitch=0 so it can be used reliably as a map key.
     */
    private static Location toBlockKey(Location loc) {
        return new Location(
            loc.getWorld(),
            loc.getBlockX(),
            loc.getBlockY(),
            loc.getBlockZ()
        );
    }

    private static String formatLoc(Location loc) {
        if (loc == null) return "null";
        return (loc.getWorld() != null ? loc.getWorld().getName() : "?")
            + " [" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "]";
    }
}
