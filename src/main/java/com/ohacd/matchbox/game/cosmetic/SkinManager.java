package com.ohacd.matchbox.game.cosmetic;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies temporary random skins to players while a game is active.
 * Skins are fetched asynchronously from Mojang's profile service via PlayerProfile#complete
 * and cached for reuse. Original player skins are restored when the game ends or the player leaves.
 */
public class SkinManager {
    private static final List<String> DEFAULT_SKIN_NAMES = List.of(
        "Notch",
        "Technoblade",
        "Dream",
        "Grian",
        "GeminiTay",
        "fWhip",
        "Green",
        "CaptainSparklez",
        "PearlescentMoon",
        "Pixlriffs",
        "Sausage",
        "MumboJumbo"
    );

    private final Plugin plugin;
    private final List<SkinData> cachedSkins = new CopyOnWriteArrayList<>();
    private final Map<UUID, SkinData> originalSkins = new ConcurrentHashMap<>();
    private final Map<UUID, SkinData> assignedSkins = new ConcurrentHashMap<>();
    private volatile boolean preloading = false;

    public SkinManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Preloads a curated skin pool asynchronously to avoid runtime stalls.
     */
    public void preloadDefaultSkins() {
        if (preloading) {
            return;
        }
        preloading = true;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (String name : DEFAULT_SKIN_NAMES) {
                fetchSkinByName(name).ifPresent(cachedSkins::add);
            }
            if (cachedSkins.isEmpty()) {
                plugin.getLogger().warning("[SkinManager] Could not cache any random skins. Falling back to player defaults.");
            } else {
                plugin.getLogger().info("[SkinManager] Cached " + cachedSkins.size() + " random skins for upcoming games.");
            }
        });
    }

    /**
     * Applies random skins to every player in the supplied collection.
     */
    public void applyRandomSkins(Collection<Player> players) {
        if (players == null || players.isEmpty()) {
            return;
        }
        for (Player player : players) {
            applyRandomSkin(player);
        }
    }

    /**
     * Applies a random skin to the provided player. No-op if no cached skins exist.
     */
    public void applyRandomSkin(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (cachedSkins.isEmpty()) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (!originalSkins.containsKey(playerId)) {
            captureCurrentSkin(player).ifPresent(skin -> originalSkins.put(playerId, skin));
        }
        SkinData chosen = pickRandomSkin(playerId);
        if (chosen == null) {
            return;
        }
        assignedSkins.put(playerId, chosen);
        setSkin(player, chosen);
        refreshAppearance(player);
    }

    /**
     * Restores a player's original skin if one was cached.
     */
    public void restoreOriginalSkin(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        SkinData original = originalSkins.remove(playerId);
        assignedSkins.remove(playerId);
        if (original == null) {
            return;
        }
        if (!player.isOnline()) {
            return;
        }
        setSkin(player, original);
        refreshAppearance(player);
    }

    /**
     * Restores skins for a set of UUIDs, typically when a game session ends.
     */
    public void restoreOriginalSkins(Set<UUID> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return;
        }
        for (UUID uuid : new ArrayList<>(playerIds)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                restoreOriginalSkin(player);
            } else {
                originalSkins.remove(uuid);
                assignedSkins.remove(uuid);
            }
        }
    }

    private SkinData pickRandomSkin(UUID playerId) {
        if (cachedSkins.isEmpty()) {
            return null;
        }
        SkinData currentlyAssigned = assignedSkins.get(playerId);
        SkinData candidate = cachedSkins.get(ThreadLocalRandom.current().nextInt(cachedSkins.size()));
        // if the candidate matches the currently assigned skin, try once more for variety
        if (currentlyAssigned != null && candidate.equals(currentlyAssigned) && cachedSkins.size() > 1) {
            candidate = cachedSkins.get(ThreadLocalRandom.current().nextInt(cachedSkins.size()));
        }
        return candidate;
    }

    private void setSkin(Player player, SkinData skinData) {
        try {
            PlayerProfile profile = Bukkit.createProfile(player.getUniqueId(), player.getName());
            Collection<ProfileProperty> properties = profile.getProperties();
            properties.clear();
            profile.setProperty(new ProfileProperty("textures", skinData.value(), skinData.signature()));
            player.setPlayerProfile(profile);
        } catch (Exception e) {
            plugin.getLogger().warning("[SkinManager] Failed to apply skin to " + player.getName() + ": " + e.getMessage());
        }
    }

    private void refreshAppearance(Player player) {
        if (!player.isOnline()) {
            return;
        }
        Runnable task = () -> {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(player)) {
                    continue;
                }
                viewer.hidePlayer(plugin, player);
                viewer.showPlayer(plugin, player);
            }
        };
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    private Optional<SkinData> captureCurrentSkin(Player player) {
        PlayerProfile profile = player.getPlayerProfile();
        for (ProfileProperty property : profile.getProperties()) {
            if ("textures".equalsIgnoreCase(property.getName())) {
                return Optional.of(new SkinData(property.getValue(), property.getSignature()));
            }
        }
        return Optional.empty();
    }

    private Optional<SkinData> fetchSkinByName(String skinName) {
        try {
            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + skinName.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
            PlayerProfile profile = Bukkit.createProfile(uuid, skinName);
            if (!profile.complete(true)) {
                return Optional.empty();
            }
            for (ProfileProperty property : profile.getProperties()) {
                if ("textures".equalsIgnoreCase(property.getName())) {
                    return Optional.of(new SkinData(property.getValue(), property.getSignature()));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[SkinManager] Failed to fetch skin '" + skinName + "': " + e.getMessage());
        }
        return Optional.empty();
    }

    private record SkinData(String value, String signature) {}
}

