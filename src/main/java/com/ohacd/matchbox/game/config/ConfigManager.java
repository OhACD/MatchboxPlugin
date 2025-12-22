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
import java.util.List;
import java.util.Map;

/**
 * Manages the plugin configuration file.
 */
public class ConfigManager {
    private final Plugin plugin;
    private FileConfiguration config;
    private File configFile;

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
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Gets the list of valid seat numbers for discussion phase spawns.
     * Returns a list of integers representing seat numbers (1-indexed).
     */
    public List<Integer> getDiscussionSeatSpawns() {
        List<?> rawList = config.getList("discussion.seat-spawns");
        if (rawList == null) {
            return new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7)); // Default
        }

        List<Integer> seatSpawns = new ArrayList<>();
        for (Object obj : rawList) {
            if (obj instanceof Number) {
                seatSpawns.add(((Number) obj).intValue());
            } else if (obj instanceof String) {
                try {
                    seatSpawns.add(Integer.parseInt((String) obj));
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid seat number in config: " + obj);
                }
            }
        }
        return seatSpawns;
    }

    /**
     * Gets the discussion phase duration in seconds.
     * Validates and clamps to reasonable range (5-300 seconds).
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
     * Validates and clamps to reasonable range (2-7).
     */
    public int getMinPlayers() {
        int min = config.getInt("session.min-players", 2);
        if (min < 2) {
            plugin.getLogger().warning("Min players too low (" + min + "), using minimum 2");
            return 2;
        }
        if (min > 7) {
            plugin.getLogger().warning("Min players too high (" + min + "), using maximum 7");
            return 7;
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
        int min = Math.max(2, Math.min(minRaw, 7));
        if (max < min) {
            plugin.getLogger().warning("Max players (" + max + ") is less than min players (" + min + "), adjusting max to " + min);
            return Math.max(min, 2);
        }
        return max;
    }

    /**
     * Gets the minimum number of spawn locations required before starting a game.
     * Validates and clamps to reasonable range (1-50).
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
     */
    public boolean isRandomSkinsEnabled() {
        return config.getBoolean("cosmetics.random-skins-enabled", true);
    }

    /**
     * Gets whether Steve skins should be used for all players.
     * When enabled, all players will have the default Steve skin regardless of random-skins-enabled setting.
     */
    public boolean isUseSteveSkins() {
        return config.getBoolean("cosmetics.use-steve-skins", false);
    }
    
    /**
     * Gets the voting threshold percentage at 20 players.
     * Validates and clamps to reasonable range (0.05-1.0).
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
     */
    public Map<Integer, Location> loadSeatLocations() {
        Map<Integer, Location> seatLocations = new HashMap<>();
        
        if (!config.contains("discussion.seat-locations")) {
            return seatLocations;
        }
        
        org.bukkit.configuration.ConfigurationSection seatSection = config.getConfigurationSection("discussion.seat-locations");
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
     */
    public void saveSeatLocation(int seatNumber, Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        
        String path = "discussion.seat-locations." + seatNumber;
        saveLocationToSection(path, location);
        saveConfig();
    }

    /**
     * Removes a seat location from config.
     */
    public void removeSeatLocation(int seatNumber) {
        config.set("discussion.seat-locations." + seatNumber, null);
        saveConfig();
    }

    /**
     * Loads spawn locations from config.
     * Returns a list of locations.
     */
    public List<Location> loadSpawnLocations() {
        List<Location> spawnLocations = new ArrayList<>();
        
        if (!config.contains("session.spawn-locations")) {
            return spawnLocations;
        }
        
        List<?> rawList = config.getList("session.spawn-locations");
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
     * Adds a spawn location to config.
     */
    public void addSpawnLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        
        List<Map<String, Object>> spawnList = new ArrayList<>();
        if (config.contains("session.spawn-locations")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> existing = (List<Map<String, Object>>) config.getList("session.spawn-locations");
            if (existing != null) {
                spawnList = new ArrayList<>(existing);
            }
        }
        
        Map<String, Object> locMap = locationToMap(location);
        spawnList.add(locMap);
        config.set("session.spawn-locations", spawnList);
        saveConfig();
    }

    /**
     * Clears all spawn locations from config.
     */
    public void clearSpawnLocations() {
        config.set("session.spawn-locations", new ArrayList<>());
        saveConfig();
    }

    /**
     * Removes a spawn location from config by index.
     */
    public boolean removeSpawnLocation(int index) {
        List<?> rawList = config.getList("session.spawn-locations");
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
            config.set("session.spawn-locations", spawnList);
            saveConfig();
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
        List<String> display = new ArrayList<>();
        org.bukkit.configuration.ConfigurationSection seatSection = config.getConfigurationSection("discussion.seat-locations");
        
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
        List<String> display = new ArrayList<>();
        List<?> rawList = config.getList("session.spawn-locations");
        
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
    private void saveLocationToSection(String path, Location location) {
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("Cannot save null location or location with null world");
            return;
        }
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
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

