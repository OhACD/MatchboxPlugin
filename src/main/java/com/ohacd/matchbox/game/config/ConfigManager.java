package com.ohacd.matchbox.game.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages the plugin configuration file.
 */
public class ConfigManager {
    private final Plugin plugin;
    private FileConfiguration config;
    private File configFile;
    private static final String WORLD_MAP_CONFIG_FILE = "matchbox-map.yml";

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        loadConfig();
    }

    /**
     * Loads the config file, creating it with defaults if it doesn't exist.
     */
    public void loadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!configFile.exists()) {
            plugin.getLogger().info("Config file not found, creating default config.yml...");
            try (InputStream defaultConfig = plugin.getResource("config.yml")) {
                if (defaultConfig != null) {
                    Files.copy(defaultConfig, configFile.toPath());
                    plugin.getLogger().info("Default config.yml created successfully.");
                } else {
                    plugin.getLogger().warning("Default config.yml not found in resources, creating empty config.");
                    configFile.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create config file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        setDefaults();
        saveConfig();
    }

    /**
     * Sets default values for config options.
     */
    private void setDefaults() {
        // Session settings
        if (!config.contains("session.min-players")) {
            config.set("session.min-players", 2);
        }
        if (!config.contains("session.max-players")) {
            config.set("session.max-players", 7);
        }
        if (!config.contains("session.min-spawn-locations")) {
            config.set("session.min-spawn-locations", 1);
        }

        // Swipe phase settings
        if (!config.contains("swipe.duration")) {
            config.set("swipe.duration", 180);
        }

        // Discussion phase settings
        if (!config.contains("discussion.seat-spawns")) {
            config.set("discussion.seat-spawns", new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7)));
        }
        if (!config.contains("discussion.duration")) {
            config.set("discussion.duration", 60);
        }

        // Voting phase settings
        if (!config.contains("voting.duration")) {
            config.set("voting.duration", 30);
        }
        
        // Dynamic voting threshold settings
        if (!config.contains("voting.threshold.at-20-players")) {
            config.set("voting.threshold.at-20-players", 0.20); // 20%
        }
        if (!config.contains("voting.threshold.at-7-players")) {
            config.set("voting.threshold.at-7-players", 0.30); // 30%
        }
        if (!config.contains("voting.threshold.at-3-players")) {
            config.set("voting.threshold.at-3-players", 0.50); // 50%
        }
        
        // Voting penalty settings (for phases without elimination)
        if (!config.contains("voting.penalty.per-phase")) {
            config.set("voting.penalty.per-phase", 0.0333); // ~3.33% per phase
        }
        if (!config.contains("voting.penalty.max-phases")) {
            config.set("voting.penalty.max-phases", 3); // Max 3 phases
        }
        if (!config.contains("voting.penalty.max-reduction")) {
            config.set("voting.penalty.max-reduction", 0.10); // 10% max reduction
        }
        
        // Spark ability settings
        if (!config.contains("spark.secondary-ability")) {
            config.set("spark.secondary-ability", "random"); // Default: random selection
        }
        
        // Medic ability settings
        if (!config.contains("medic.secondary-ability")) {
            config.set("medic.secondary-ability", "random"); // Default: random selection
        }

        // Cosmetic settings
        if (!config.contains("cosmetics.random-skins-enabled")) {
            config.set("cosmetics.random-skins-enabled", false);
        }
        if (!config.contains("cosmetics.use-steve-skins")) {
            config.set("cosmetics.use-steve-skins", true);
        }

        // Sign mode settings
        if (!config.contains("sign-mode.enabled")) {
            config.set("sign-mode.enabled", true);
        }
    }

    /**
     * Saves the config file.
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save config file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reloads the config file from disk.
     */
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        setDefaults();
        saveConfig();
    }

    /**
     * Gets the FileConfiguration object.
     *
     * @return the loaded {@link FileConfiguration}
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Returns true if the given world has a local Matchbox map config file.
     */
    public boolean hasWorldMapConfig(World world) {
        if (world == null) {
            return false;
        }
        return getWorldMapConfigFile(world).exists();
    }

    private File getWorldMapConfigFile(World world) {
        return new File(world.getWorldFolder(), WORLD_MAP_CONFIG_FILE);
    }

    private FileConfiguration loadWorldMapConfig(World world) {
        if (world == null) {
            return null;
        }
        File worldConfigFile = getWorldMapConfigFile(world);
        if (!worldConfigFile.exists()) {
            return null;
        }
        return YamlConfiguration.loadConfiguration(worldConfigFile);
    }

    private FileConfiguration getOrCreateWorldMapConfig(World world) {
        if (world == null) {
            return null;
        }

        File worldConfigFile = getWorldMapConfigFile(world);
        if (!worldConfigFile.exists()) {
            try {
                File parent = worldConfigFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                worldConfigFile.createNewFile();
                plugin.getLogger().info("Created world-local Matchbox map config: " + worldConfigFile.getPath());
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create world-local map config for world '" + world.getName() + "': " + e.getMessage());
                return null;
            }
        }

        FileConfiguration worldConfig = YamlConfiguration.loadConfiguration(worldConfigFile);
        if (!worldConfig.contains("discussion.seat-spawns")) {
            worldConfig.set("discussion.seat-spawns", new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7)));
        }
        if (!worldConfig.contains("session.spawn-locations")) {
            worldConfig.set("session.spawn-locations", new ArrayList<>());
        }

        saveWorldMapConfig(world, worldConfig);
        return worldConfig;
    }

    private void saveWorldMapConfig(World world, FileConfiguration worldConfig) {
        if (world == null || worldConfig == null) {
            return;
        }

        File worldConfigFile = getWorldMapConfigFile(world);
        try {
            worldConfig.save(worldConfigFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save world-local map config for world '" + world.getName() + "': " + e.getMessage());
        }
    }

    /**
     * Gets the list of valid seat numbers for discussion phase spawns.
     * Returns a list of integers representing seat numbers (1-indexed).
     *
     * @return a list of valid seat numbers
     */
    public List<Integer> getDiscussionSeatSpawns() {
        return parseSeatSpawns(config, "global config");
    }

    /**
     * Gets valid discussion seat numbers for a world, falling back to global config.
     */
    public List<Integer> getDiscussionSeatSpawns(World world) {
        FileConfiguration worldConfig = loadWorldMapConfig(world);
        if (worldConfig != null && worldConfig.contains("discussion.seat-spawns")) {
            return parseSeatSpawns(worldConfig, "world map config '" + world.getName() + "'");
        }
        return getDiscussionSeatSpawns();
    }

    private List<Integer> parseSeatSpawns(FileConfiguration source, String sourceName) {
        List<?> rawList = source.getList("discussion.seat-spawns");
        if (rawList == null) {
            return new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7));
        }

        List<Integer> seatSpawns = new ArrayList<>();
        for (Object obj : rawList) {
            if (obj instanceof Number) {
                seatSpawns.add(((Number) obj).intValue());
            } else if (obj instanceof String) {
                try {
                    seatSpawns.add(Integer.parseInt((String) obj));
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid seat number in " + sourceName + ": " + obj);
                }
            }
        }
        return seatSpawns;
    }

    /**
     * Gets the world-local map config path for display.
     */
    public String getWorldMapConfigPath(World world) {
        if (world == null) {
            return WORLD_MAP_CONFIG_FILE;
        }
        return getWorldMapConfigFile(world).getPath();
    }

    /**
     * Initializes (or updates) metadata for a world-local map config.
     */
    public void initializeWorldMapMetadata(World world, String mapId, String displayName, String creator) {
        if (world == null) {
            return;
        }

        String normalizedMapId = mapId == null || mapId.isBlank() ? world.getName() : mapId.trim().toLowerCase();
        String normalizedDisplayName = displayName == null || displayName.isBlank() ? world.getName() : displayName.trim();
        String normalizedCreator = creator == null || creator.isBlank() ? "unknown" : creator.trim();

        FileConfiguration worldConfig = getOrCreateWorldMapConfig(world);
        if (worldConfig == null) {
            return;
        }

        worldConfig.set("map.id", normalizedMapId);
        worldConfig.set("map.display-name", normalizedDisplayName);
        worldConfig.set("map.creator", normalizedCreator);
        worldConfig.set("map.schema-version", 1);
        worldConfig.set("map.plugin-version", plugin.getPluginMeta().getVersion());

        saveWorldMapConfig(world, worldConfig);
    }

    /**
     * Returns world-local map metadata for display.
     */
    public Map<String, String> getWorldMapMetadata(World world) {
        Map<String, String> metadata = new HashMap<>();
        if (world == null) {
            return metadata;
        }

        FileConfiguration worldConfig = loadWorldMapConfig(world);
        if (worldConfig == null) {
            return metadata;
        }

        metadata.put("id", worldConfig.getString("map.id", ""));
        metadata.put("display-name", worldConfig.getString("map.display-name", ""));
        metadata.put("creator", worldConfig.getString("map.creator", ""));
        metadata.put("schema-version", String.valueOf(worldConfig.getInt("map.schema-version", 0)));
        metadata.put("plugin-version", worldConfig.getString("map.plugin-version", ""));
        return metadata;
    }

    /**
     * Validates required keys for a world-local map config.
     */
    public List<String> validateWorldMapConfig(World world) {
        List<String> issues = new ArrayList<>();
        if (world == null) {
            issues.add("World is null.");
            return issues;
        }

        FileConfiguration worldConfig = loadWorldMapConfig(world);
        if (worldConfig == null) {
            issues.add("Missing world map config file: " + getWorldMapConfigPath(world));
            return issues;
        }

        if (!worldConfig.contains("map.id") || worldConfig.getString("map.id", "").isBlank()) {
            issues.add("Missing required key: map.id");
        }
        if (!worldConfig.contains("map.display-name") || worldConfig.getString("map.display-name", "").isBlank()) {
            issues.add("Missing required key: map.display-name");
        }
        if (!worldConfig.contains("map.creator") || worldConfig.getString("map.creator", "").isBlank()) {
            issues.add("Missing required key: map.creator");
        }
        if (!worldConfig.contains("map.schema-version")) {
            issues.add("Missing required key: map.schema-version");
        }
        if (!worldConfig.contains("map.plugin-version") || worldConfig.getString("map.plugin-version", "").isBlank()) {
            issues.add("Missing required key: map.plugin-version");
        }

        List<Integer> seatSpawns = getDiscussionSeatSpawns(world);
        if (seatSpawns.isEmpty()) {
            issues.add("discussion.seat-spawns is empty.");
        }

        Map<Integer, Location> seats = loadSeatLocations(world);
        List<Location> spawns = loadSpawnLocations(world);

        if (seats.isEmpty()) {
            issues.add("No discussion.seat-locations configured.");
        }
        if (spawns.isEmpty()) {
            issues.add("No session.spawn-locations configured.");
        }

        for (Integer seat : seatSpawns) {
            if (!seats.containsKey(seat)) {
                issues.add("Seat " + seat + " is listed in discussion.seat-spawns but has no location.");
            }
        }

        return issues;
    }

    /**
     * Adds a seat number to discussion.seat-spawns for a world-local map config.
     */
    public void addDiscussionSeatSpawn(World world, int seatNumber) {
        if (world == null || seatNumber < 1) {
            return;
        }

        FileConfiguration worldConfig = getOrCreateWorldMapConfig(world);
        if (worldConfig == null) {
            return;
        }

        Set<Integer> uniqueSeats = new LinkedHashSet<>(getDiscussionSeatSpawns(world));
        uniqueSeats.add(seatNumber);

        List<Integer> updated = new ArrayList<>(uniqueSeats);
        updated.sort(Integer::compareTo);
        worldConfig.set("discussion.seat-spawns", updated);
        saveWorldMapConfig(world, worldConfig);
    }

    /**
     * Removes a seat number from discussion.seat-spawns for a world-local map config.
     */
    public void removeDiscussionSeatSpawn(World world, int seatNumber) {
        if (world == null || seatNumber < 1) {
            return;
        }

        FileConfiguration worldConfig = getOrCreateWorldMapConfig(world);
        if (worldConfig == null) {
            return;
        }

        List<Integer> seats = new ArrayList<>(getDiscussionSeatSpawns(world));
        seats.removeIf(value -> value == seatNumber);
        worldConfig.set("discussion.seat-spawns", seats);
        saveWorldMapConfig(world, worldConfig);
    }

    /**
     * Replaces discussion.seat-spawns for a world-local map config.
     */
    public void setDiscussionSeatSpawns(World world, List<Integer> seatNumbers) {
        if (world == null) {
            return;
        }

        FileConfiguration worldConfig = getOrCreateWorldMapConfig(world);
        if (worldConfig == null) {
            return;
        }

        List<Integer> safeSeatNumbers = new ArrayList<>();
        if (seatNumbers != null) {
            Set<Integer> uniqueSeats = new LinkedHashSet<>();
            for (Integer seatNumber : seatNumbers) {
                if (seatNumber != null && seatNumber >= 1) {
                    uniqueSeats.add(seatNumber);
                }
            }
            safeSeatNumbers.addAll(uniqueSeats);
            safeSeatNumbers.sort(Integer::compareTo);
        }

        worldConfig.set("discussion.seat-spawns", safeSeatNumbers);
        saveWorldMapConfig(world, worldConfig);
    }

    /**
     * Imports legacy global seat/spawn config into a world-local map config.
     *
     * @param world target world
     * @param overwrite whether existing world-local values should be replaced
     * @return true if world config was updated, false otherwise
     */
    public boolean importLegacyGlobalConfigToWorld(World world, boolean overwrite) {
        if (world == null) {
            return false;
        }

        FileConfiguration worldConfig = getOrCreateWorldMapConfig(world);
        if (worldConfig == null) {
            return false;
        }

        boolean changed = false;

        boolean hasSeatSpawns = worldConfig.isList("discussion.seat-spawns")
                && !worldConfig.getIntegerList("discussion.seat-spawns").isEmpty();
        if (overwrite || !hasSeatSpawns) {
            List<Integer> legacySeatSpawns = getDiscussionSeatSpawns();
            worldConfig.set("discussion.seat-spawns", new ArrayList<>(legacySeatSpawns));
            changed = true;
        }

        org.bukkit.configuration.ConfigurationSection seatLocationsSection =
                worldConfig.getConfigurationSection("discussion.seat-locations");
        boolean hasSeatLocations = seatLocationsSection != null && !seatLocationsSection.getKeys(false).isEmpty();
        if (overwrite || !hasSeatLocations) {
            worldConfig.set("discussion.seat-locations", null);
            Map<Integer, Location> legacySeatLocations = loadSeatLocations();
            for (Map.Entry<Integer, Location> entry : legacySeatLocations.entrySet()) {
                saveLocationToSection(worldConfig, "discussion.seat-locations." + entry.getKey(), entry.getValue());
            }
            changed = true;
        }

        boolean hasSpawnLocations = worldConfig.isList("session.spawn-locations")
                && !worldConfig.getMapList("session.spawn-locations").isEmpty();
        if (overwrite || !hasSpawnLocations) {
            List<Map<String, Object>> serializedSpawns = new ArrayList<>();
            for (Location spawn : loadSpawnLocations()) {
                if (spawn != null && spawn.getWorld() != null) {
                    serializedSpawns.add(locationToMap(spawn));
                }
            }
            worldConfig.set("session.spawn-locations", serializedSpawns);
            changed = true;
        }

        if (changed) {
            saveWorldMapConfig(world, worldConfig);
        }

        return changed;
    }

    /**
     * Automatically migrates legacy global config into loaded world-local map configs when needed.
     * Uses non-overwrite import behavior so existing world-local values remain unchanged.
     *
     * @return number of worlds whose map config was updated
     */
    public int autoMigrateLegacyConfigsForLoadedWorlds() {
        List<World> loadedWorlds = Bukkit.getWorlds();
        if (loadedWorlds == null || loadedWorlds.isEmpty()) {
            plugin.getLogger().info("Auto legacy migration skipped: no loaded worlds found.");
            return 0;
        }

        int scanned = 0;
        int migrated = 0;
        int skippedWithoutMapConfig = 0;

        for (World world : loadedWorlds) {
            if (world == null) {
                continue;
            }

            if (!hasWorldMapConfig(world)) {
                skippedWithoutMapConfig++;
                continue;
            }

            FileConfiguration worldConfig = loadWorldMapConfig(world);
            if (worldConfig == null) {
                skippedWithoutMapConfig++;
                continue;
            }

            scanned++;
            boolean linkedSeatReferences = hasLinkedLegacySeatReferences(worldConfig);
            boolean shouldMigrate = shouldAutoMigrateLegacyConfig(worldConfig);

            if (!shouldMigrate) {
                continue;
            }

            if (linkedSeatReferences) {
                plugin.getLogger().info("Detected linked legacy seat references for world '" + world.getName()
                        + "' (seat-spawns present, seat-locations missing). Migrating legacy global config...");
            }

            boolean changed = importLegacyGlobalConfigToWorld(world, false);
            if (changed) {
                migrated++;
                plugin.getLogger().info("Auto-migrated legacy global config into world map config for '"
                        + world.getName() + "'.");
            }
        }

        plugin.getLogger().info("Auto legacy migration complete: scanned=" + scanned
                + ", migrated=" + migrated
                + ", skippedWithoutMapConfig=" + skippedWithoutMapConfig + ".");

        return migrated;
    }

    private boolean shouldAutoMigrateLegacyConfig(FileConfiguration worldConfig) {
        if (worldConfig == null) {
            return false;
        }

        boolean hasSeatSpawns = worldConfig.isList("discussion.seat-spawns")
                && !worldConfig.getIntegerList("discussion.seat-spawns").isEmpty();
        org.bukkit.configuration.ConfigurationSection seatLocationsSection =
                worldConfig.getConfigurationSection("discussion.seat-locations");
        boolean hasSeatLocations = seatLocationsSection != null && !seatLocationsSection.getKeys(false).isEmpty();
        boolean hasSpawnLocations = worldConfig.isList("session.spawn-locations")
                && !worldConfig.getMapList("session.spawn-locations").isEmpty();

        boolean legacyHasSeatSpawns = !getDiscussionSeatSpawns().isEmpty();
        boolean legacyHasSeatLocations = !loadSeatLocations().isEmpty();
        boolean legacyHasSpawnLocations = !loadSpawnLocations().isEmpty();

        return (!hasSeatSpawns && legacyHasSeatSpawns)
                || (!hasSeatLocations && legacyHasSeatLocations)
                || (!hasSpawnLocations && legacyHasSpawnLocations);
    }

    private boolean hasLinkedLegacySeatReferences(FileConfiguration worldConfig) {
        if (worldConfig == null) {
            return false;
        }

        boolean hasSeatSpawns = worldConfig.isList("discussion.seat-spawns")
                && !worldConfig.getIntegerList("discussion.seat-spawns").isEmpty();
        org.bukkit.configuration.ConfigurationSection seatLocationsSection =
                worldConfig.getConfigurationSection("discussion.seat-locations");
        boolean hasSeatLocations = seatLocationsSection != null && !seatLocationsSection.getKeys(false).isEmpty();

        return hasSeatSpawns && !hasSeatLocations && !loadSeatLocations().isEmpty();
    }

    /**
     * Gets the discussion phase duration in seconds.
     * Validates and clamps to reasonable range (5-300 seconds).
     *
     * @return discussion duration in seconds
     */
    public int getDiscussionDuration() {
        int duration = config.getInt("discussion.duration", 30);
        if (duration < 5) {
            plugin.getLogger().warning("Discussion duration too low (" + duration + "), using minimum 5 seconds");
            return 5;
        }
        if (duration > 300) {
            plugin.getLogger().warning("Discussion duration too high (" + duration + "), using maximum 300 seconds");
            return 300;
        }
        return duration;
    }

    /**
     * Gets the swipe phase duration in seconds.
     * Validates and clamps to reasonable range (30-600 seconds).
     *
     * @return swipe duration in seconds
     */
    public int getSwipeDuration() {
        int duration = config.getInt("swipe.duration", 180);
        if (duration < 30) {
            plugin.getLogger().warning("Swipe duration too low (" + duration + "), using minimum 30 seconds");
            return 30;
        }
        if (duration > 600) {
            plugin.getLogger().warning("Swipe duration too high (" + duration + "), using maximum 600 seconds");
            return 600;
        }
        return duration;
    }

    /**
     * Gets the voting phase duration in seconds.
     * Validates and clamps to reasonable range (5-120 seconds).
     *
     * @return voting duration in seconds
     */
    public int getVotingDuration() {
        int duration = config.getInt("voting.duration", 15);
        if (duration < 5) {
            plugin.getLogger().warning("Voting duration too low (" + duration + "), using minimum 5 seconds");
            return 5;
        }
        if (duration > 120) {
            plugin.getLogger().warning("Voting duration too high (" + duration + "), using maximum 120 seconds");
            return 120;
        }
        return duration;
    }

    /**
     * Gets the minimum number of players required to start a game.
     * Validates and clamps to reasonable range (2-20).
     *
     * @return minimum number of players
     */
    public int getMinPlayers() {
        int min = config.getInt("session.min-players", 2);
        if (min < 2) {
            plugin.getLogger().warning("Min players too low (" + min + "), using minimum 2");
            return 2;
        }
        if (min > 20) {
            plugin.getLogger().warning("Min players too high (" + min + "), using maximum 20");
            return 20;
        }
        // Ensure min doesn't exceed max (read max directly to avoid recursion)
        int maxRaw = config.getInt("session.max-players", 7);
        int max = Math.max(2, Math.min(maxRaw, 20));
        if (min > max) {
            plugin.getLogger().warning("Min players (" + min + ") exceeds max players (" + max + "), adjusting min to " + max);
            return max;
        }
        return min;
    }

    /**
     * Gets the maximum number of players allowed per session.
     * Validates and clamps to reasonable range (2-20).
     *
     * @return maximum number of players
     */
    public int getMaxPlayers() {
        int max = config.getInt("session.max-players", 7);
        if (max < 2) {
            plugin.getLogger().warning("Max players too low (" + max + "), using minimum 2");
            return 2;
        }
        if (max > 20) {
            plugin.getLogger().warning("Max players too high (" + max + "), using maximum 20");
            return 20;
        }
        // Ensure max is at least equal to min (read min directly to avoid recursion)
        int minRaw = config.getInt("session.min-players", 2);
        int min = Math.max(2, Math.min(minRaw, 20));
        if (max < min) {
            plugin.getLogger().warning("Max players (" + max + ") is less than min players (" + min + "), adjusting max to " + min);
            return Math.max(min, 2);
        }
        return max;
    }

    /**
     * Gets the minimum number of spawn locations required before starting a game.
     * Validates and clamps to reasonable range (1-50).
     *
     * @return minimum number of spawn locations
     */
    public int getMinSpawnLocations() {
        int min = config.getInt("session.min-spawn-locations", 1);
        if (min < 1) {
            plugin.getLogger().warning("Min spawn locations too low (" + min + "), using minimum 1");
            return 1;
        }
        if (min > 50) {
            plugin.getLogger().warning("Min spawn locations too high (" + min + "), using maximum 50");
            return 50;
        }
        return min;
    }

    /**
     * Gets whether random skins are enabled.
     *
     * @return true if random skins are enabled
     */
    public boolean isRandomSkinsEnabled() {
        return config.getBoolean("cosmetics.random-skins-enabled", true);
    }

    /**
     * Gets whether Steve skins should be used for all players.
     * When enabled, all players will have the default Steve skin regardless of random-skins-enabled setting.
     *
     * @return true if Steve skins should be used
     */
    public boolean isUseSteveSkins() {
        return config.getBoolean("cosmetics.use-steve-skins", false);
    }

    /**
     * Gets whether sign mode is enabled.
     * When enabled, players receive a wooden axe and two stacks of oak signs at the start
     * of the swipe phase and can use signs to chat instead of normal text chat.
     * All signs placed during swipe phase are removed when discussion starts.
     *
     * @return true if sign mode is enabled
     */
    public boolean isSignModeEnabled() {
        return config.getBoolean("sign-mode.enabled", true);
    }
    
    /**
     * Gets the voting threshold percentage at 20 players.
     * Validates and clamps to reasonable range (0.05-1.0).
     *
     * @return threshold percentage for 20 players (0.0 - 1.0)
     */
    public double getVotingThresholdAt20Players() {
        double threshold = config.getDouble("voting.threshold.at-20-players", 0.20);
        if (threshold < 0.05) {
            plugin.getLogger().warning("Voting threshold at 20 players too low (" + threshold + "), using minimum 0.05");
            return 0.05;
        }
        if (threshold > 1.0) {
            plugin.getLogger().warning("Voting threshold at 20 players too high (" + threshold + "), using maximum 1.0");
            return 1.0;
        }
        return threshold;
    }
    
    /**
     * Gets the voting threshold percentage at 7 players.
     * Validates and clamps to reasonable range (0.05-1.0).
     *
     * @return threshold percentage for 7 players (0.0 - 1.0)
     */
    public double getVotingThresholdAt7Players() {
        double threshold = config.getDouble("voting.threshold.at-7-players", 0.30);
        if (threshold < 0.05) {
            plugin.getLogger().warning("Voting threshold at 7 players too low (" + threshold + "), using minimum 0.05");
            return 0.05;
        }
        if (threshold > 1.0) {
            plugin.getLogger().warning("Voting threshold at 7 players too high (" + threshold + "), using maximum 1.0");
            return 1.0;
        }
        return threshold;
    }
    
    /**
     * Gets the voting threshold percentage at 3 players and below.
     * Validates and clamps to reasonable range (0.05-1.0).
     *
     * @return threshold percentage for 3 players (0.0 - 1.0)
     */
    public double getVotingThresholdAt3Players() {
        double threshold = config.getDouble("voting.threshold.at-3-players", 0.50);
        if (threshold < 0.05) {
            plugin.getLogger().warning("Voting threshold at 3 players too low (" + threshold + "), using minimum 0.05");
            return 0.05;
        }
        if (threshold > 1.0) {
            plugin.getLogger().warning("Voting threshold at 3 players too high (" + threshold + "), using maximum 1.0");
            return 1.0;
        }
        return threshold;
    }
    
    /**
     * Gets the penalty percentage applied per voting phase without elimination.
     * Validates and clamps to reasonable range (0.0-0.5).
     *
     * @return penalty percentage applied per phase (0.0 - 1.0)
     */
    public double getVotingPenaltyPerPhase() {
        double penalty = config.getDouble("voting.penalty.per-phase", 0.0333);
        if (penalty < 0.0) {
            plugin.getLogger().warning("Voting penalty per phase too low (" + penalty + "), using minimum 0.0");
            return 0.0;
        }
        if (penalty > 0.5) {
            plugin.getLogger().warning("Voting penalty per phase too high (" + penalty + "), using maximum 0.5");
            return 0.5;
        }
        return penalty;
    }
    
    /**
     * Gets the maximum number of phases that can accumulate penalty.
     * Validates and clamps to reasonable range (1-10).
     *
     * @return maximum penalty phases
     */
    public int getVotingMaxPenaltyPhases() {
        int maxPhases = config.getInt("voting.penalty.max-phases", 3);
        if (maxPhases < 1) {
            plugin.getLogger().warning("Voting max penalty phases too low (" + maxPhases + "), using minimum 1");
            return 1;
        }
        if (maxPhases > 10) {
            plugin.getLogger().warning("Voting max penalty phases too high (" + maxPhases + "), using maximum 10");
            return 10;
        }
        return maxPhases;
    }
    
    /**
     * Gets the maximum penalty reduction percentage.
     * Validates and clamps to reasonable range (0.0-0.5).
     *
     * @return maximum penalty reduction (0.0 - 1.0)
     */
    public double getVotingMaxPenalty() {
        double maxPenalty = config.getDouble("voting.penalty.max-reduction", 0.10);
        if (maxPenalty < 0.0) {
            plugin.getLogger().warning("Voting max penalty too low (" + maxPenalty + "), using minimum 0.0");
            return 0.0;
        }
        if (maxPenalty > 0.5) {
            plugin.getLogger().warning("Voting max penalty too high (" + maxPenalty + "), using maximum 0.5");
            return 0.5;
        }
        return maxPenalty;
    }
    
    /**
     * Gets the configured Spark secondary ability selection mode.
     * Returns "random" for random selection, or a specific ability name.
     * Valid values: "random", "hunter_vision", "spark_swap"
     */
    public String getSparkSecondaryAbility() {
        String ability = config.getString("spark.secondary-ability", "random");
        if (ability == null) {
            return "random";
        }
        String lowerAbility = ability.toLowerCase().trim();
        if (lowerAbility.equals("random") || lowerAbility.equals("hunter_vision") || lowerAbility.equals("spark_swap") || lowerAbility.equals("delusion")) {
            return lowerAbility;
        }
        plugin.getLogger().warning("Invalid Spark secondary ability setting: " + ability + ". Valid options: random, hunter_vision, spark_swap, delusion. Using default: random");
        return "random";
    }
    
    /**
     * Gets the configured Medic secondary ability selection mode.
     * Returns "random" for random selection, or a specific ability name.
     * Valid values: "random", "healing_sight"
     */
    public String getMedicSecondaryAbility() {
        String ability = config.getString("medic.secondary-ability", "random");
        if (ability == null) {
            return "random";
        }
        String lowerAbility = ability.toLowerCase().trim();
        if (lowerAbility.equals("random") || lowerAbility.equals("healing_sight")) {
            return lowerAbility;
        }
        plugin.getLogger().warning("Invalid Medic secondary ability setting: " + ability + ". Valid options: random, healing_sight. Using default: random");
        return "random";
    }

    /**
     * Loads seat locations from config.
     * Returns a map of seat numbers to locations.
     *
     * @return map of seat number -> {@link Location}
     */
    public Map<Integer, Location> loadSeatLocations() {
        return loadSeatLocations(null);
    }

    /**
     * Loads seat locations from world-local config when available, otherwise global config.
     */
    public Map<Integer, Location> loadSeatLocations(World world) {
        FileConfiguration source = config;
        if (world != null) {
            FileConfiguration worldConfig = loadWorldMapConfig(world);
            if (worldConfig != null && worldConfig.contains("discussion.seat-locations")) {
                source = worldConfig;
            }
        }

        Map<Integer, Location> seatLocations = new HashMap<>();
        if (!source.contains("discussion.seat-locations")) {
            return seatLocations;
        }

        org.bukkit.configuration.ConfigurationSection seatSection = source.getConfigurationSection("discussion.seat-locations");
        if (seatSection == null) {
            return seatLocations;
        }
        
        for (String key : seatSection.getKeys(false)) {
            try {
                int seatNumber = Integer.parseInt(key);
                org.bukkit.configuration.ConfigurationSection locSection = seatSection.getConfigurationSection(key);
                if (locSection != null) {
                    Location loc = loadLocationFromSection(locSection);
                    if (loc != null) {
                        seatLocations.put(seatNumber, loc);
                    }
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid seat number in config: " + key);
            }
        }
        
        return seatLocations;
    }

    /**
     * Saves a seat location to config.
     *
     * @param seatNumber the seat number to save
     * @param location the location to store for the seat
     */
    public void saveSeatLocation(int seatNumber, Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        World world = location.getWorld();
        FileConfiguration worldConfig = getOrCreateWorldMapConfig(world);
        if (worldConfig == null) {
            return;
        }

        String path = "discussion.seat-locations." + seatNumber;
        saveLocationToSection(worldConfig, path, location);

        List<Integer> seatSpawns = getDiscussionSeatSpawns(world);
        if (!seatSpawns.contains(seatNumber)) {
            seatSpawns.add(seatNumber);
            seatSpawns.sort(Integer::compareTo);
            worldConfig.set("discussion.seat-spawns", seatSpawns);
        }

        saveWorldMapConfig(world, worldConfig);
    }

    /**
     * Removes a seat location from config.
     *
     * @param seatNumber the seat number to remove
     */
    public void removeSeatLocation(int seatNumber) {
        config.set("discussion.seat-locations." + seatNumber, null);
        saveConfig();
    }

    /**
     * Removes a seat location from world-local config when available, otherwise global config.
     */
    public void removeSeatLocation(int seatNumber, World world) {
        FileConfiguration worldConfig = loadWorldMapConfig(world);
        if (worldConfig != null) {
            worldConfig.set("discussion.seat-locations." + seatNumber, null);
            saveWorldMapConfig(world, worldConfig);
            return;
        }
        removeSeatLocation(seatNumber);
    }

    /**
     * Loads spawn locations from config.
     * Returns a list of locations.
     *
     * @return list of spawn {@link Location}s
     */
    public List<Location> loadSpawnLocations() {
        return loadSpawnLocations(null);
    }

    /**
     * Loads spawn locations from world-local config when available, otherwise global config.
     */
    public List<Location> loadSpawnLocations(World world) {
        FileConfiguration source = config;
        if (world != null) {
            FileConfiguration worldConfig = loadWorldMapConfig(world);
            if (worldConfig != null && worldConfig.contains("session.spawn-locations")) {
                source = worldConfig;
            }
        }

        List<Location> spawnLocations = new ArrayList<>();

        if (!source.contains("session.spawn-locations")) {
            return spawnLocations;
        }

        List<?> rawList = source.getList("session.spawn-locations");
        if (rawList == null) {
            return spawnLocations;
        }
        
        for (Object obj : rawList) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> locMap = (Map<String, Object>) obj;
                Location loc = loadLocationFromMap(locMap);
                if (loc != null) {
                    spawnLocations.add(loc);
                }
            }
        }
        
        return spawnLocations;
    }

    /**
     * Adds a spawn location to config.     *
     * @param location location to add to the spawn list     */
    public void addSpawnLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        World world = location.getWorld();
        FileConfiguration worldConfig = getOrCreateWorldMapConfig(world);
        if (worldConfig == null) {
            return;
        }

        List<Map<String, Object>> spawnList = new ArrayList<>();
        if (worldConfig.contains("session.spawn-locations")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> existing = (List<Map<String, Object>>) worldConfig.getList("session.spawn-locations");
            if (existing != null) {
                spawnList = new ArrayList<>(existing);
            }
        }

        Map<String, Object> locMap = locationToMap(location);
        spawnList.add(locMap);
        worldConfig.set("session.spawn-locations", spawnList);
        saveWorldMapConfig(world, worldConfig);
    }

    /**
     * Clears all spawn locations from config.
     */
    public void clearSpawnLocations() {
        config.set("session.spawn-locations", new ArrayList<>());
        saveConfig();
    }

    /**
     * Clears world-local spawn locations when available, otherwise global config.
     */
    public void clearSpawnLocations(World world) {
        FileConfiguration worldConfig = loadWorldMapConfig(world);
        if (worldConfig != null) {
            worldConfig.set("session.spawn-locations", new ArrayList<>());
            saveWorldMapConfig(world, worldConfig);
            return;
        }
        clearSpawnLocations();
    }

    /**
     * Removes a spawn location from config by index.     *
     * @param index index of the spawn location to remove
     * @return true if the location was removed, false otherwise     */
    public boolean removeSpawnLocation(int index) {
        return removeSpawnLocation(index, null);
    }

    /**
     * Removes a spawn location by index from world-local config when available, otherwise global config.
     */
    public boolean removeSpawnLocation(int index, World world) {
        FileConfiguration source = config;
        boolean worldScoped = false;
        if (world != null) {
            FileConfiguration worldConfig = loadWorldMapConfig(world);
            if (worldConfig != null) {
                source = worldConfig;
                worldScoped = true;
            }
        }

        List<?> rawList = source.getList("session.spawn-locations");
        if (rawList == null || rawList.isEmpty()) {
            return false;
        }
        
        if (index < 0 || index >= rawList.size()) {
            return false;
        }
        
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> spawnList = new ArrayList<>((List<Map<String, Object>>) rawList);
            spawnList.remove(index);
            source.set("session.spawn-locations", spawnList);
            if (worldScoped) {
                saveWorldMapConfig(world, source);
            } else {
                saveConfig();
            }
            return true;
        } catch (ClassCastException e) {
            plugin.getLogger().warning("Invalid spawn location format in config: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets all seat locations from config as a formatted string for display.
     */
    public List<String> getSeatLocationsDisplay() {
        return getSeatLocationsDisplay(null);
    }

    /**
     * Gets seat locations for display from world-local config when available, otherwise global config.
     */
    public List<String> getSeatLocationsDisplay(World world) {
        FileConfiguration source = config;
        if (world != null) {
            FileConfiguration worldConfig = loadWorldMapConfig(world);
            if (worldConfig != null && worldConfig.contains("discussion.seat-locations")) {
                source = worldConfig;
            }
        }

        List<String> display = new ArrayList<>();
        org.bukkit.configuration.ConfigurationSection seatSection = source.getConfigurationSection("discussion.seat-locations");
        
        if (seatSection == null || seatSection.getKeys(false).isEmpty()) {
            display.add("§7No seat locations configured.");
            return display;
        }
        
        display.add("§6Seat Locations:");
        List<String> seatKeys = new ArrayList<>(seatSection.getKeys(false));
        seatKeys.sort((a, b) -> {
            try {
                return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
            } catch (NumberFormatException e) {
                return a.compareTo(b);
            }
        });
        
        for (String key : seatKeys) {
            org.bukkit.configuration.ConfigurationSection locSection = seatSection.getConfigurationSection(key);
            if (locSection == null) {
                display.add("§7  Seat " + key + ": §cInvalid configuration (missing location section)");
                continue;
            }
            
            String worldName = locSection.getString("world");
            boolean worldMissing = (worldName == null || worldName.isEmpty());
            boolean worldLoaded = !worldMissing && Bukkit.getWorld(worldName) != null;
            
            double x = locSection.getDouble("x", 0.0);
            double y = locSection.getDouble("y", 64.0);
            double z = locSection.getDouble("z", 0.0);
            
            String status = worldMissing ? " §c(missing world)" : (!worldLoaded ? " §e(world not loaded)" : "");
            display.add(String.format("§7  Seat %s: §e%s §7(%.1f, %.1f, %.1f)%s",
                    key,
                    worldName != null ? worldName : "unknown",
                    x, y, z,
                    status));
        }
        
        return display;
    }

    /**
     * Gets all spawn locations from config as a formatted string for display.
     */
    public List<String> getSpawnLocationsDisplay() {
        return getSpawnLocationsDisplay(null);
    }

    /**
     * Gets spawn locations for display from world-local config when available, otherwise global config.
     */
    public List<String> getSpawnLocationsDisplay(World world) {
        FileConfiguration source = config;
        if (world != null) {
            FileConfiguration worldConfig = loadWorldMapConfig(world);
            if (worldConfig != null && worldConfig.contains("session.spawn-locations")) {
                source = worldConfig;
            }
        }

        List<String> display = new ArrayList<>();
        List<?> rawList = source.getList("session.spawn-locations");
        
        if (rawList == null || rawList.isEmpty()) {
            display.add("§7No spawn locations configured.");
            return display;
        }
        
        display.add("§6Spawn Locations:");
        for (int i = 0; i < rawList.size(); i++) {
            Object obj = rawList.get(i);
            if (!(obj instanceof Map)) {
                display.add("§7  #" + (i + 1) + ": §cInvalid entry (not a location map)");
                continue;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> locMap = (Map<String, Object>) obj;
            String worldName = (String) locMap.get("world");
            boolean worldMissing = (worldName == null || worldName.isEmpty());
            boolean worldLoaded = !worldMissing && Bukkit.getWorld(worldName) != null;
            
            double x = getDoubleValue(locMap.get("x"), 0.0);
            double y = getDoubleValue(locMap.get("y"), 64.0);
            double z = getDoubleValue(locMap.get("z"), 0.0);
            
            String status = worldMissing ? " §c(missing world)" : (!worldLoaded ? " §e(world not loaded)" : "");
            display.add(String.format("§7  #%d: §e%s §7(%.1f, %.1f, %.1f)%s",
                    i + 1,
                    worldName != null ? worldName : "unknown",
                    x, y, z,
                    status));
        }
        
        return display;
    }

    /**
     * Helper method to load a location from a configuration section.
     */
    private Location loadLocationFromSection(org.bukkit.configuration.ConfigurationSection section) {
        String worldName = section.getString("world");
        if (worldName == null) {
            return null;
        }
        
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' not found when loading location from config");
            return null;
        }
        
        double x = section.getDouble("x", 0.0);
        double y = section.getDouble("y", 64.0);
        double z = section.getDouble("z", 0.0);
        float yaw = (float) section.getDouble("yaw", 0.0);
        float pitch = (float) section.getDouble("pitch", 0.0);
        
        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Helper method to load a location from a map.
     */
    private Location loadLocationFromMap(Map<String, Object> locMap) {
        if (locMap == null) {
            return null;
        }
        
        String worldName = (String) locMap.get("world");
        if (worldName == null) {
            plugin.getLogger().warning("Location in config missing world name");
            return null;
        }
        
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' not found when loading location from config");
            return null;
        }
        
        try {
            double x = getDoubleValue(locMap.get("x"), 0.0);
            double y = getDoubleValue(locMap.get("y"), 64.0);
            double z = getDoubleValue(locMap.get("z"), 0.0);
            float yaw = (float) getDoubleValue(locMap.get("yaw"), 0.0);
            float pitch = (float) getDoubleValue(locMap.get("pitch"), 0.0);
            
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse location from config: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Safely converts an object to a double value.
     */
    private double getDoubleValue(Object obj, double defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid number format in config: " + obj);
            return defaultValue;
        }
    }

    /**
     * Helper method to save a location to a configuration section path.
     */
    private void saveLocationToSection(FileConfiguration targetConfig, String path, Location location) {
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("Cannot save null location or location with null world");
            return;
        }
        targetConfig.set(path + ".world", location.getWorld().getName());
        targetConfig.set(path + ".x", location.getX());
        targetConfig.set(path + ".y", location.getY());
        targetConfig.set(path + ".z", location.getZ());
        targetConfig.set(path + ".yaw", location.getYaw());
        targetConfig.set(path + ".pitch", location.getPitch());
    }

    /**
     * Helper method to convert a location to a map.
     */
    private Map<String, Object> locationToMap(Location location) {
        Map<String, Object> locMap = new HashMap<>();
        locMap.put("world", location.getWorld().getName());
        locMap.put("x", location.getX());
        locMap.put("y", location.getY());
        locMap.put("z", location.getZ());
        locMap.put("yaw", location.getYaw());
        locMap.put("pitch", location.getPitch());
        return locMap;
    }
}

