package com.ohacd.matchbox.game.cosmetic;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.google.common.collect.Multimap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies temporary skins during active games using ProtocolLib packet rewriting.
 *
 * This approach avoids mutating the server-side PlayerProfile and only rewrites
 * player info packets seen by clients, which is more resilient across phase changes.
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
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private static final Duration PROFILE_TIMEOUT = Duration.ofSeconds(5);
    private static final Pattern PROFILE_ID_PATTERN = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([0-9a-fA-F]{32})\\\"");
    private static final SkinData DEFAULT_STEVE = new SkinData(
        "ewogICJ0aW1lc3RhbXAiIDogMTc3NDUwMDQzMjk2OCwKICAicHJvZmlsZUlkIiA6ICJiMTM1MDRmMjMxOGI0OWNjYWFkZDcyYWVhYmMyNTQ1MCIsCiAgInByb2ZpbGVOYW1lIiA6ICJUeXBrZW4iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWE5YjQwMGRmZDUyOTUyYzM1Y2UzZmY1MzcwNTY1MWQ0MjVkMjMwYjBmOGM4M2YxNjg5ZTFkZTgyNzFjZDM1MiIKICAgIH0KICB9Cn0=",
        "wYbFAH3ejP+MO1YNAoe8RlhwYc2v3G6oz4v4e2uH/YvENmqPqnKzerKhIVq69vyvndmLIJd5XHyiKJhuiYpeMwBtzo5D54nLiVdCGG+MXdkEmTyJtuna1CzIFnYnx8gT6JMK0wztgR6IFa/WwGQU4pfYI0+xXutmwqKqsKMUkrutalHkS5FUSovPTGDSKz66JDxpxSo8a9GVxNJIUrbUszg50GbuYURQ3l4D6vKR4IufH3Q4Z9oDUcD5f8EYI3EhNmfytUHRa51r0hmrNGDvFSYaix97PrPASgBDSy9m0xCNTR5Sno+TyYb+QyJfh1nT7MpGMtClHi3OzBT4vaQG9A8+vu+ISuYcwWvswcHisYCq+8AulaPfd//+oWjnZEDISwBvHq26jyOs4ct56qXTReRf0zgWELlJIs/YM/n8Jpgs1lEdXcBUZZPLsyAeanKL3GwApq9GTlPMAanihYeaWM5HeonxUPUZKD1Z4Xwd0gdB6VQHRCPVHtyN4/DHP4idQOdF62izUKrNXODNbHXnElEgpVmy8Zgawaval8vo9GkRRxXMPh4SQ88RlVnStQdQbSPAwnVSVOBlLlidy9tJA71a4ktQJTqlIEFciLhryOdyxhPI/UiKW1TRYOcMhfVCI8RprMTeisEeE1hsqZCgNRySva8XmmqxY2uvzvlnyy8="
    );

    private final Plugin plugin;
    private final List<SkinData> cachedSkins = new CopyOnWriteArrayList<>();
    private final Map<UUID, SkinData> originalSkins = new ConcurrentHashMap<>();
    private final Map<UUID, SkinData> assignedSkins = new ConcurrentHashMap<>();
    private final Set<UUID> discussionOriginalView = ConcurrentHashMap.newKeySet();
    private final ProtocolManager protocolManager;
    private final boolean packetMode;

    private volatile boolean preloading = false;
    private volatile boolean loggedOfflineModeFallback = false;

    public SkinManager(Plugin plugin) {
        this.plugin = plugin;
        ProtocolManager manager = null;
        boolean available = false;

        try {
            if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
                manager = ProtocolLibrary.getProtocolManager();
                available = manager != null;
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[SkinManager] ProtocolLib not available for packet skins: " + t.getMessage());
        }

        this.protocolManager = manager;
        this.packetMode = available;

        if (packetMode) {
            registerPacketListener();
            plugin.getLogger().info("[SkinManager] Using ProtocolLib packet-based skin overrides.");
        } else {
            plugin.getLogger().warning("[SkinManager] ProtocolLib unavailable. Falling back to direct profile skin updates.");
        }
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
            try {
                if (!Bukkit.getServer().getOnlineMode()) {
                    if (!loggedOfflineModeFallback) {
                        plugin.getLogger().info("[SkinManager] Server is in offline mode. Using Steve-only skin fallback.");
                        loggedOfflineModeFallback = true;
                    }
                    cachedSkins.clear();
                    ensureSteveFallbackCached();
                    return;
                }

                for (String name : DEFAULT_SKIN_NAMES) {
                    fetchSkinByName(name).ifPresent(cachedSkins::add);
                }
                if (cachedSkins.isEmpty()) {
                    plugin.getLogger().warning("[SkinManager] Could not cache any random skins. Falling back to Steve-only skins.");
                    ensureSteveFallbackCached();
                } else {
                    plugin.getLogger().info("[SkinManager] Cached " + cachedSkins.size() + " random skins for upcoming games.");
                }
            } finally {
                preloading = false;
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
     * Applies Steve skin to every player in the supplied collection.
     */
    public void applySteveSkins(Collection<Player> players) {
        if (players == null || players.isEmpty()) {
            return;
        }
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                applySteveSkin(player);
            }
        }
    }

    /**
     * Applies Steve skin to a single player.
     */
    public void applySteveSkin(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        rememberOriginalIfNeeded(player);
        assignedSkins.put(playerId, DEFAULT_STEVE);
        discussionOriginalView.remove(playerId);
        setSkinDirect(player, DEFAULT_STEVE);
        refreshAppearance(player);
    }

    /**
     * Applies a random skin to the provided player.
     */
    public void applyRandomSkin(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (!Bukkit.getServer().getOnlineMode()) {
            applySteveSkin(player);
            return;
        }
        if (cachedSkins.isEmpty()) {
            applySteveSkin(player);
            return;
        }

        UUID playerId = player.getUniqueId();
        rememberOriginalIfNeeded(player);
        SkinData chosen = pickRandomSkin(playerId);
        if (chosen == null) {
            return;
        }

        assignedSkins.put(playerId, chosen);
        discussionOriginalView.remove(playerId);
        setSkinDirect(player, chosen);
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
        discussionOriginalView.remove(playerId);

        if (!player.isOnline()) {
            return;
        }

        if (original != null) {
            setSkinDirect(player, original);
        }
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
                discussionOriginalView.remove(uuid);
            }
        }
    }

    /**
     * Temporarily shows original skins during discussion, without losing assigned skins.
     */
    public void restoreOriginalSkinsForDiscussion(Collection<Player> players) {
        if (players == null || players.isEmpty()) {
            return;
        }

        for (Player player : players) {
            if (player == null || !player.isOnline()) {
                continue;
            }
            UUID playerId = player.getUniqueId();
            discussionOriginalView.add(playerId);

            SkinData original = originalSkins.get(playerId);
            if (original != null) {
                setSkinDirect(player, original);
            }

            refreshAppearance(player);
        }
    }

    /**
     * Re-applies assigned game skins after discussion ends.
     */
    public void restoreAssignedSkinsAfterDiscussion(Collection<Player> players) {
        if (players == null || players.isEmpty()) {
            return;
        }

        for (Player player : players) {
            if (player == null || !player.isOnline()) {
                continue;
            }
            UUID playerId = player.getUniqueId();
            discussionOriginalView.remove(playerId);

            SkinData assigned = assignedSkins.get(playerId);
            if (assigned != null) {
                setSkinDirect(player, assigned);
            }

            refreshAppearance(player);
        }
    }

    private SkinData pickRandomSkin(UUID playerId) {
        if (cachedSkins.isEmpty()) {
            return null;
        }

        SkinData currentlyAssigned = assignedSkins.get(playerId);
        SkinData candidate = cachedSkins.get(ThreadLocalRandom.current().nextInt(cachedSkins.size()));
        if (currentlyAssigned != null && candidate.equals(currentlyAssigned) && cachedSkins.size() > 1) {
            candidate = cachedSkins.get(ThreadLocalRandom.current().nextInt(cachedSkins.size()));
        }
        return candidate;
    }

    private void registerPacketListener() {
        PacketType[] packetTypes = resolvePlayerInfoPacketTypes();
        if (packetTypes.length == 0) {
            plugin.getLogger().warning("[SkinManager] No supported player-info packet types found. Skin packet overrides disabled.");
            return;
        }

        PacketAdapter adapter = new PacketAdapter(plugin, ListenerPriority.NORMAL, packetTypes) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event == null || event.getPacket() == null) {
                    return;
                }
                try {
                    rewritePlayerInfoPacket(event.getPacket());
                } catch (Exception e) {
                    plugin.getLogger().warning("[SkinManager] Failed to rewrite PLAYER_INFO packet: " + e.getMessage());
                }
            }
        };
        protocolManager.addPacketListener(adapter);
        plugin.getLogger().info("[SkinManager] Listening for packet skin rewrites on " + packetTypes.length + " player-info packet type(s).");
    }

    private PacketType[] resolvePlayerInfoPacketTypes() {
        List<PacketType> types = new ArrayList<>(2);

        PacketType playerInfo = resolvePlayerInfoPacketType("PLAYER_INFO");
        if (playerInfo != null) {
            types.add(playerInfo);
        }

        PacketType playerInfoUpdate = resolvePlayerInfoPacketType("PLAYER_INFO_UPDATE");
        if (playerInfoUpdate != null && !types.contains(playerInfoUpdate)) {
            types.add(playerInfoUpdate);
        }

        return types.toArray(new PacketType[0]);
    }

    private PacketType resolvePlayerInfoPacketType(String fieldName) {
        try {
            Field field = PacketType.Play.Server.class.getField(fieldName);
            Object value = field.get(null);
            if (value instanceof PacketType packetType) {
                return packetType;
            }
        } catch (NoSuchFieldException ignored) {
            // Packet not present on this ProtocolLib version.
        } catch (Exception e) {
            plugin.getLogger().warning("[SkinManager] Failed to resolve packet type '" + fieldName + "': " + e.getMessage());
        }
        return null;
    }

    private void rewritePlayerInfoPacket(PacketContainer packet) {
        if (!isAddPlayerAction(packet)) {
            return;
        }

        if (packet.getPlayerInfoDataLists().size() <= 0) {
            return;
        }

        List<PlayerInfoData> entries = packet.getPlayerInfoDataLists().read(0);
        if (entries == null || entries.isEmpty()) {
            return;
        }

        boolean changed = false;
        List<PlayerInfoData> rewritten = new ArrayList<>(entries.size());

        for (PlayerInfoData entry : entries) {
            PlayerInfoData updated = rewriteEntry(entry);
            rewritten.add(updated);
            if (updated != entry) {
                changed = true;
            }
        }

        if (changed) {
            packet.getPlayerInfoDataLists().write(0, rewritten);
        }
    }

    private boolean isAddPlayerAction(PacketContainer packet) {
        if (packet.getPlayerInfoActions().size() > 0) {
            Set<EnumWrappers.PlayerInfoAction> actions = packet.getPlayerInfoActions().read(0);
            return actions != null && actions.contains(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        }
        if (packet.getPlayerInfoAction().size() > 0) {
            EnumWrappers.PlayerInfoAction action = packet.getPlayerInfoAction().read(0);
            return action == EnumWrappers.PlayerInfoAction.ADD_PLAYER;
        }
        return false;
    }

    private PlayerInfoData rewriteEntry(PlayerInfoData entry) {
        if (entry == null) {
            return entry;
        }

        WrappedGameProfile sourceProfile = entry.getProfile();
        if (sourceProfile == null) {
            return entry;
        }

        UUID profileId = entry.getProfileId();
        if (profileId == null) {
            profileId = sourceProfile.getUUID();
        }
        if (profileId == null || discussionOriginalView.contains(profileId)) {
            return entry;
        }

        SkinData skinData = assignedSkins.get(profileId);
        if (skinData == null) {
            return entry;
        }

        WrappedGameProfile updatedProfile = cloneWithSkin(sourceProfile, skinData);
        return new PlayerInfoData(
            profileId,
            entry.getLatency(),
            entry.isListed(),
            entry.getGameMode(),
            updatedProfile,
            entry.getDisplayName(),
            entry.isShowHat(),
            entry.getListOrder(),
            entry.getRemoteChatSessionData()
        );
    }

    private WrappedGameProfile cloneWithSkin(WrappedGameProfile source, SkinData skinData) {
        WrappedGameProfile clone = new WrappedGameProfile(source.getUUID(), source.getName());
        Multimap<String, WrappedSignedProperty> sourceProperties = source.getProperties();
        Multimap<String, WrappedSignedProperty> cloneProperties = clone.getProperties();

        for (Map.Entry<String, WrappedSignedProperty> property : sourceProperties.entries()) {
            if ("textures".equalsIgnoreCase(property.getKey())) {
                continue;
            }
            cloneProperties.put(property.getKey(), property.getValue());
        }

        if (skinData.value() != null && !skinData.value().isBlank()) {
            cloneProperties.put(
                "textures",
                new WrappedSignedProperty("textures", skinData.value(), skinData.signature() != null ? skinData.signature() : "")
            );
        }

        return clone;
    }

    private void rememberOriginalIfNeeded(Player player) {
        originalSkins.computeIfAbsent(player.getUniqueId(), id -> captureCurrentSkin(player).orElse(new SkinData("", "")));
    }

    private void setSkinDirect(Player player, SkinData skinData) {
        try {
            if (skinData == null) {
                plugin.getLogger().warning("[SkinManager] Cannot apply null skin data to " + player.getName());
                return;
            }
            if (player == null || !player.isOnline()) {
                return;
            }

            com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit.createProfile(player.getUniqueId(), player.getName());
            if (profile == null) {
                plugin.getLogger().warning("[SkinManager] Failed to create profile for " + player.getName());
                return;
            }

            Collection<com.destroystokyo.paper.profile.ProfileProperty> properties = profile.getProperties();
            properties.clear();
            if (skinData.value() != null && !skinData.value().isBlank()) {
                String signature = skinData.signature() != null ? skinData.signature() : "";
                profile.setProperty(new com.destroystokyo.paper.profile.ProfileProperty("textures", skinData.value(), signature));
            }
            player.setPlayerProfile(profile);
        } catch (Exception e) {
            plugin.getLogger().warning("[SkinManager] Failed to apply skin to " + player.getName() + " (offline mode?): " + e.getMessage());
        }
    }

    private void refreshAppearance(Player player) {
        if (player == null || !player.isOnline()) {
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
        try {
            com.destroystokyo.paper.profile.PlayerProfile profile = player.getPlayerProfile();
            if (profile == null) {
                return Optional.empty();
            }
            for (com.destroystokyo.paper.profile.ProfileProperty property : profile.getProperties()) {
                if ("textures".equalsIgnoreCase(property.getName())) {
                    String value = property.getValue();
                    String signature = property.getSignature();
                    if (value != null && !value.isEmpty()) {
                        return Optional.of(new SkinData(value, signature != null ? signature : ""));
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[SkinManager] Failed to capture skin for " + player.getName() + " (offline mode?): " + e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<SkinData> fetchSkinByName(String skinName) {
        try {
            if (!Bukkit.getServer().getOnlineMode()) {
                return Optional.empty();
            }

            Optional<UUID> onlineUuid = resolveOnlineUuid(skinName);
            if (onlineUuid.isEmpty()) {
                plugin.getLogger().warning("[SkinManager] Unable to resolve UUID for '" + skinName + "'. Skipping.");
                return Optional.empty();
            }

            com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit.createProfile(onlineUuid.get(), skinName);
            if (!profile.complete(true)) {
                plugin.getLogger().warning("[SkinManager] Mojang session server rejected skin '" + skinName + "'.");
                return Optional.empty();
            }

            for (com.destroystokyo.paper.profile.ProfileProperty property : profile.getProperties()) {
                if ("textures".equalsIgnoreCase(property.getName())) {
                    return Optional.of(new SkinData(property.getValue(), property.getSignature()));
                }
            }
            plugin.getLogger().warning("[SkinManager] No texture properties returned for '" + skinName + "'.");
        } catch (Exception e) {
            plugin.getLogger().warning("[SkinManager] Failed to fetch skin '" + skinName + "': " + e.getMessage());
        }
        return Optional.empty();
    }

    private void ensureSteveFallbackCached() {
        if (!cachedSkins.contains(DEFAULT_STEVE)) {
            cachedSkins.add(DEFAULT_STEVE);
        }
    }

    private Optional<UUID> resolveOnlineUuid(String skinName) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + skinName))
                .timeout(PROFILE_TIMEOUT)
                .GET()
                .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                return Optional.empty();
            }

            Matcher matcher = PROFILE_ID_PATTERN.matcher(response.body());
            if (matcher.find()) {
                String rawId = matcher.group(1);
                return Optional.of(UUID.fromString(formatUuid(rawId)));
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            plugin.getLogger().warning("[SkinManager] Failed to resolve UUID for '" + skinName + "': " + e.getMessage());
        }
        return Optional.empty();
    }

    private static String formatUuid(String raw) {
        String normalized = raw.replace("-", "").toLowerCase(Locale.ROOT);
        return normalized.replaceFirst(
            "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
            "$1-$2-$3-$4-$5"
        );
    }

    private record SkinData(String value, String signature) {}
}
