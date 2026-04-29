package com.ohacd.matchbox.game.nick;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Manages Matchbox player nicks.
 *
 * <p>Nicks are stored persistently in {@code plugins/Matchbox/nicks.yml} (keyed by UUID)
 * and are applied only when a player enters an active game session.  They affect:
 * <ul>
 *   <li>Chat display name ({@link Player#setDisplayName})</li>
 *   <li>Tab-list name ({@link Player#setPlayerListName})</li>
 *   <li>Above-head custom name ({@link Player#customName} + {@link Player#setCustomNameVisible})</li>
 * </ul>
 * All three are restored to the real username when the session ends.
 */
public final class NickManager {

    // --- Result enum ---

    public enum NickResult {
        SUCCESS, TOO_SHORT, TOO_LONG, INVALID_CHARS
    }

    // --- Constants ---

    private static final int MIN_LEN = 3;
    private static final int MAX_LEN = 16;
    /** Allowed characters for non-admin nicks. */
    private static final Pattern SAFE = Pattern.compile("^[a-zA-Z0-9_\\-]+$");

    // --- State ---

    private final Plugin plugin;
    private final Map<UUID, String> nicks = new ConcurrentHashMap<>();
    private File nickFile;
    private YamlConfiguration nickConfig;

    // --- Constructor ---

    public NickManager(Plugin plugin) {
        this.plugin = plugin;
        load();
    }

    // =========================================================
    // Persistence
    // =========================================================

    private void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        nickFile = new File(plugin.getDataFolder(), "nicks.yml");
        nickConfig = YamlConfiguration.loadConfiguration(nickFile);

        int loaded = 0;
        for (String key : nickConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String nick = nickConfig.getString(key);
                if (nick != null && !nick.isBlank()) {
                    nicks.put(uuid, nick);
                    loaded++;
                }
            } catch (IllegalArgumentException ignored) {
                // skip malformed keys
            }
        }
        plugin.getLogger().info("[NickManager] Loaded " + loaded + " nicks.");
    }

    private void save() {
        for (Map.Entry<UUID, String> entry : nicks.entrySet()) {
            nickConfig.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            nickConfig.save(nickFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[NickManager] Failed to save nicks.yml: " + e.getMessage());
        }
    }

    // =========================================================
    // Core API
    // =========================================================

    /**
     * Stores and persists a nick for the given player.
     *
     * @param uuid    target player UUID
     * @param nick    desired nick string (may contain §-colour codes if admin)
     * @param isAdmin whether the caller holds {@code matchbox.admin} — allows colour codes
     * @return the validation result; only {@link NickResult#SUCCESS} means the nick was saved
     */
    public NickResult setNick(UUID uuid, String nick, boolean isAdmin) {
        // Strip colour codes for length/char validation
        String stripped = nick.replaceAll("§[0-9a-fA-Fk-orK-OR]", "");

        if (stripped.length() < MIN_LEN) return NickResult.TOO_SHORT;
        if (stripped.length() > MAX_LEN) return NickResult.TOO_LONG;
        if (!SAFE.matcher(stripped).matches()) return NickResult.INVALID_CHARS;

        // Non-admins may not include colour codes
        if (!isAdmin && !nick.equals(stripped)) return NickResult.INVALID_CHARS;

        nicks.put(uuid, nick);
        nickConfig.set(uuid.toString(), nick);
        save();
        return NickResult.SUCCESS;
    }

    /**
     * Removes a stored nick and persists the change.
     */
    public void removeNick(UUID uuid) {
        nicks.remove(uuid);
        nickConfig.set(uuid.toString(), null);
        save();
    }

    /**
     * Returns the stored nick for {@code uuid}, or {@code null} if none is set.
     */
    public String getNick(UUID uuid) {
        return nicks.get(uuid);
    }

    // =========================================================
    // Session helpers
    // =========================================================

    /**
     * Builds the set of lower-case nicks currently stored for all players in a session.
     * Used to detect collisions before applying nicks at game-start.
     */
    public Set<String> getTakenNicksInSession(Collection<UUID> sessionPlayerIds) {
        Set<String> taken = new HashSet<>();
        for (UUID id : sessionPlayerIds) {
            String nick = nicks.get(id);
            if (nick != null) taken.add(nick.toLowerCase());
        }
        return taken;
    }

    // =========================================================
    // Apply / Restore
    // =========================================================

    /**
     * Applies the player's stored nick if it is not already taken by another player
     * in the same session.
     *
     * <p>Sets the display name, tab-list name, and above-head custom name.
     * The above-head custom name is only visible because {@link com.ohacd.matchbox.game.utils.Managers.NameTagManager}
     * hides the real username via a scoreboard team during the game.
     *
     * @param player     the player to nick
     * @param takenLower lower-case nicks already active in the session (must NOT include this player's own nick)
     * @return {@code true} if the nick was applied; {@code false} on collision or no nick stored
     */
    public boolean applyNick(Player player, Set<String> takenLower) {
        String nick = nicks.get(player.getUniqueId());
        if (nick == null) return false;
        if (takenLower.contains(nick.toLowerCase())) return false;

        player.setDisplayName(nick);
        player.setPlayerListName(nick);

        // Above-head custom name — visible while the real nametag is hidden by the scoreboard team
        Component component = LegacyComponentSerializer.legacySection().deserialize(nick);
        player.customName(component);
        player.setCustomNameVisible(true);

        return true;
    }

    /**
     * Resets the player's display name, tab-list name, and above-head custom name
     * back to their real username.
     */
    public void restoreNick(Player player) {
        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
        player.customName(null);
        player.setCustomNameVisible(false);
    }
}
