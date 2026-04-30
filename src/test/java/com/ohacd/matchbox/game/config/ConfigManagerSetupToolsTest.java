package com.ohacd.matchbox.game.config;

import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ohacd.matchbox.utils.MockBukkitFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigManagerSetupToolsTest {

    @TempDir
    Path tempDir;

    private Plugin plugin;
    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        MockBukkitFactory.setUpBukkitMocks();
        plugin = mock(Plugin.class);
        PluginMeta pluginMeta = mock(PluginMeta.class);

        when(plugin.getDataFolder()).thenReturn(tempDir.resolve("plugin-data").toFile());
        when(plugin.getResource("config.yml")).thenReturn(null);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getPluginMeta()).thenReturn(pluginMeta);
        when(pluginMeta.getVersion()).thenReturn("0.9.7-test");

        configManager = new ConfigManager(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkitFactory.tearDownBukkitMocks();
    }

    @Test
    @DisplayName("Should initialize and read world map metadata")
    void shouldInitializeAndReadWorldMapMetadata() {
        World world = mock(World.class);
        File worldFolder = tempDir.resolve("worlds").resolve("creator-map").toFile();
        worldFolder.mkdirs();

        when(world.getName()).thenReturn("creator-map");
        when(world.getWorldFolder()).thenReturn(worldFolder);

        configManager.initializeWorldMapMetadata(world, "creator_map", "Creator Map", "MapMaker");

        Map<String, String> metadata = configManager.getWorldMapMetadata(world);

        assertThat(configManager.hasWorldMapConfig(world)).isTrue();
        assertThat(metadata.get("id")).isEqualTo("creator_map");
        assertThat(metadata.get("display-name")).isEqualTo("Creator Map");
        assertThat(metadata.get("creator")).isEqualTo("MapMaker");
        assertThat(metadata.get("schema-version")).isEqualTo("1");
        assertThat(metadata.get("plugin-version")).isEqualTo("0.9.7-test");
    }

    @Test
    @DisplayName("Should manage seat spawn order in world map config")
    void shouldManageSeatSpawnOrderInWorldMapConfig() {
        World world = mock(World.class);
        File worldFolder = tempDir.resolve("worlds").resolve("seat-world").toFile();
        worldFolder.mkdirs();

        when(world.getName()).thenReturn("seat-world");
        when(world.getWorldFolder()).thenReturn(worldFolder);

        configManager.setDiscussionSeatSpawns(world, List.of(4, 2, 2, 1));
        assertThat(configManager.getDiscussionSeatSpawns(world)).containsExactly(1, 2, 4);

        configManager.addDiscussionSeatSpawn(world, 3);
        assertThat(configManager.getDiscussionSeatSpawns(world)).containsExactly(1, 2, 3, 4);

        configManager.removeDiscussionSeatSpawn(world, 2);
        assertThat(configManager.getDiscussionSeatSpawns(world)).containsExactly(1, 3, 4);
    }

    @Test
    @DisplayName("Should report validation issues for incomplete world map config")
    void shouldReportValidationIssuesForIncompleteWorldMapConfig() {
        World world = mock(World.class);
        File worldFolder = tempDir.resolve("worlds").resolve("validate-world").toFile();
        worldFolder.mkdirs();

        when(world.getName()).thenReturn("validate-world");
        when(world.getWorldFolder()).thenReturn(worldFolder);

        configManager.initializeWorldMapMetadata(world, "validate_world", "Validate World", "Tester");

        List<String> issues = configManager.validateWorldMapConfig(world);

        assertThat(issues).anyMatch(issue -> issue.contains("No discussion.seat-locations configured"));
        assertThat(issues).anyMatch(issue -> issue.contains("No session.spawn-locations configured"));
    }

    @Test
    @DisplayName("Should import legacy global seat spawns into world map config with overwrite")
    void shouldImportLegacyGlobalSeatSpawnsIntoWorldMapConfigWithOverwrite() {
        World world = mock(World.class);
        File worldFolder = tempDir.resolve("worlds").resolve("legacy-world").toFile();
        worldFolder.mkdirs();

        when(world.getName()).thenReturn("legacy-world");
        when(world.getWorldFolder()).thenReturn(worldFolder);

        configManager.setDiscussionSeatSpawns(world, List.of(1, 2, 3));

        configManager.getConfig().set("discussion.seat-spawns", List.of(9, 10));
        configManager.saveConfig();

        boolean changed = configManager.importLegacyGlobalConfigToWorld(world, true);

        assertThat(changed).isTrue();
        assertThat(configManager.getDiscussionSeatSpawns(world)).containsExactly(9, 10);
    }

    @Test
    @DisplayName("Should import legacy spawn locations when world-local list exists but is empty")
    void shouldImportLegacySpawnLocationsWhenWorldListIsEmpty() {
        World globalWorld = mock(World.class);
        World targetWorld = mock(World.class);

        File globalFolder = tempDir.resolve("worlds").resolve("global-world").toFile();
        File targetFolder = tempDir.resolve("worlds").resolve("target-world").toFile();
        globalFolder.mkdirs();
        targetFolder.mkdirs();

        when(globalWorld.getName()).thenReturn("global-world");
        when(globalWorld.getWorldFolder()).thenReturn(globalFolder);
        when(targetWorld.getName()).thenReturn("target-world");
        when(targetWorld.getWorldFolder()).thenReturn(targetFolder);
        when(Bukkit.getServer().getWorld("global-world")).thenReturn(globalWorld);

        configManager.initializeWorldMapMetadata(targetWorld, "target_map", "Target Map", "Tester");

        Location globalSpawn = new Location(globalWorld, 100.5, 64.0, 200.5, 180.0f, 0.0f);
        configManager.getConfig().set("session.spawn-locations", List.of(Map.of(
            "world", "global-world",
            "x", globalSpawn.getX(),
            "y", globalSpawn.getY(),
            "z", globalSpawn.getZ(),
            "yaw", (double) globalSpawn.getYaw(),
            "pitch", (double) globalSpawn.getPitch()
        )));
        configManager.saveConfig();

        boolean changed = configManager.importLegacyGlobalConfigToWorld(targetWorld, false);

        List<Location> importedSpawns = configManager.loadSpawnLocations(targetWorld);
        assertThat(changed).isTrue();
        assertThat(importedSpawns).hasSize(1);
        assertThat(importedSpawns.get(0).getWorld().getName()).isEqualTo("global-world");
        assertThat(importedSpawns.get(0).getX()).isEqualTo(100.5);
    }

    @Test
    @DisplayName("Should not overwrite existing world-local spawn locations when importlegacy is used without overwrite")
    void shouldNotOverwriteExistingWorldLocalSpawnLocationsWithoutOverwrite() {
        World globalWorld = mock(World.class);
        World targetWorld = mock(World.class);

        File globalFolder = tempDir.resolve("worlds").resolve("legacy-global").toFile();
        File targetFolder = tempDir.resolve("worlds").resolve("existing-world").toFile();
        globalFolder.mkdirs();
        targetFolder.mkdirs();

        when(globalWorld.getName()).thenReturn("legacy-global");
        when(globalWorld.getWorldFolder()).thenReturn(globalFolder);
        when(targetWorld.getName()).thenReturn("existing-world");
        when(targetWorld.getWorldFolder()).thenReturn(targetFolder);
        when(Bukkit.getServer().getWorld("legacy-global")).thenReturn(globalWorld);
        when(Bukkit.getServer().getWorld("existing-world")).thenReturn(targetWorld);

        configManager.initializeWorldMapMetadata(targetWorld, "existing_world", "Existing World", "Tester");

        Location existingSpawn = new Location(targetWorld, 12.5, 64.0, -8.5, 0.0f, 0.0f);
        configManager.addSpawnLocation(existingSpawn);

        Location legacyGlobalSpawn = new Location(globalWorld, 300.5, 70.0, 400.5, 180.0f, 0.0f);
        configManager.getConfig().set("session.spawn-locations", List.of(Map.of(
                "world", "legacy-global",
                "x", legacyGlobalSpawn.getX(),
                "y", legacyGlobalSpawn.getY(),
                "z", legacyGlobalSpawn.getZ(),
                "yaw", (double) legacyGlobalSpawn.getYaw(),
                "pitch", (double) legacyGlobalSpawn.getPitch()
        )));
        configManager.saveConfig();

        configManager.importLegacyGlobalConfigToWorld(targetWorld, false);

        List<Location> worldSpawnsAfterImport = configManager.loadSpawnLocations(targetWorld);
        assertThat(worldSpawnsAfterImport).hasSize(1);
        assertThat(worldSpawnsAfterImport.get(0).getWorld().getName()).isEqualTo("existing-world");
        assertThat(worldSpawnsAfterImport.get(0).getX()).isEqualTo(12.5);
        assertThat(worldSpawnsAfterImport.get(0).getZ()).isEqualTo(-8.5);
    }

    @Test
    @DisplayName("Should auto migrate linked legacy seat locations for loaded worlds")
    void shouldAutoMigrateLinkedLegacySeatLocationsForLoadedWorlds() {
        World globalWorld = mock(World.class);
        World targetWorld = mock(World.class);

        File globalFolder = tempDir.resolve("worlds").resolve("legacy-global-seats").toFile();
        File targetFolder = tempDir.resolve("worlds").resolve("linked-seat-world").toFile();
        globalFolder.mkdirs();
        targetFolder.mkdirs();

        when(globalWorld.getName()).thenReturn("legacy-global-seats");
        when(globalWorld.getWorldFolder()).thenReturn(globalFolder);
        when(targetWorld.getName()).thenReturn("linked-seat-world");
        when(targetWorld.getWorldFolder()).thenReturn(targetFolder);
        when(Bukkit.getServer().getWorld("legacy-global-seats")).thenReturn(globalWorld);
        when(Bukkit.getServer().getWorld("linked-seat-world")).thenReturn(targetWorld);
        when(Bukkit.getServer().getWorlds()).thenReturn(List.of(targetWorld));

        configManager.initializeWorldMapMetadata(targetWorld, "linked_world", "Linked World", "Tester");
        configManager.setDiscussionSeatSpawns(targetWorld, List.of(1, 2));

        configManager.getConfig().set("discussion.seat-locations.1.world", "legacy-global-seats");
        configManager.getConfig().set("discussion.seat-locations.1.x", 21.0);
        configManager.getConfig().set("discussion.seat-locations.1.y", 64.0);
        configManager.getConfig().set("discussion.seat-locations.1.z", -12.0);
        configManager.getConfig().set("discussion.seat-locations.1.yaw", 180.0);
        configManager.getConfig().set("discussion.seat-locations.1.pitch", 0.0);
        configManager.saveConfig();

        int migratedCount = configManager.autoMigrateLegacyConfigsForLoadedWorlds();
        Map<Integer, Location> seatLocations = configManager.loadSeatLocations(targetWorld);

        assertThat(migratedCount).isEqualTo(1);
        assertThat(seatLocations).containsKey(1);
        assertThat(seatLocations.get(1).getWorld().getName()).isEqualTo("legacy-global-seats");
        assertThat(seatLocations.get(1).getX()).isEqualTo(21.0);
    }

    @Test
    @DisplayName("Should preserve existing world-local seats during startup auto migration")
    void shouldPreserveExistingWorldLocalSeatsDuringStartupAutoMigration() {
        World globalWorld = mock(World.class);
        World targetWorld = mock(World.class);

        File globalFolder = tempDir.resolve("worlds").resolve("legacy-full-import").toFile();
        File targetFolder = tempDir.resolve("worlds").resolve("existing-seat-world").toFile();
        globalFolder.mkdirs();
        targetFolder.mkdirs();

        when(globalWorld.getName()).thenReturn("legacy-full-import");
        when(globalWorld.getWorldFolder()).thenReturn(globalFolder);
        when(targetWorld.getName()).thenReturn("existing-seat-world");
        when(targetWorld.getWorldFolder()).thenReturn(targetFolder);
        when(Bukkit.getServer().getWorld("legacy-full-import")).thenReturn(globalWorld);
        when(Bukkit.getServer().getWorld("existing-seat-world")).thenReturn(targetWorld);
        when(Bukkit.getServer().getWorlds()).thenReturn(List.of(targetWorld));

        configManager.initializeWorldMapMetadata(targetWorld, "existing_seat_world", "Existing Seat World", "Tester");

        Location existingSeat = new Location(targetWorld, 5.0, 70.0, 5.0, 90.0f, 0.0f);
        configManager.saveSeatLocation(1, existingSeat);

        configManager.getConfig().set("discussion.seat-locations.1.world", "legacy-full-import");
        configManager.getConfig().set("discussion.seat-locations.1.x", 99.0);
        configManager.getConfig().set("discussion.seat-locations.1.y", 65.0);
        configManager.getConfig().set("discussion.seat-locations.1.z", 99.0);
        configManager.getConfig().set("discussion.seat-locations.1.yaw", 0.0);
        configManager.getConfig().set("discussion.seat-locations.1.pitch", 0.0);
        configManager.getConfig().set("session.spawn-locations", List.of(Map.of(
                "world", "legacy-full-import",
                "x", 101.0,
                "y", 64.0,
                "z", 101.0,
                "yaw", 0.0,
                "pitch", 0.0
        )));
        configManager.saveConfig();

        int migratedCount = configManager.autoMigrateLegacyConfigsForLoadedWorlds();
        Map<Integer, Location> seatLocations = configManager.loadSeatLocations(targetWorld);
        List<Location> spawnLocations = configManager.loadSpawnLocations(targetWorld);

        assertThat(migratedCount).isEqualTo(1);
        assertThat(seatLocations).containsKey(1);
        assertThat(seatLocations.get(1).getWorld().getName()).isEqualTo("existing-seat-world");
        assertThat(seatLocations.get(1).getX()).isEqualTo(5.0);
        assertThat(spawnLocations).hasSize(1);
        assertThat(spawnLocations.get(0).getWorld().getName()).isEqualTo("legacy-full-import");
    }

    @Test
    @DisplayName("Should skip startup auto migration when no legacy data can be imported")
    void shouldSkipStartupAutoMigrationWhenNoLegacyDataCanBeImported() {
        World targetWorld = mock(World.class);

        File targetFolder = tempDir.resolve("worlds").resolve("no-legacy-world").toFile();
        targetFolder.mkdirs();

        when(targetWorld.getName()).thenReturn("no-legacy-world");
        when(targetWorld.getWorldFolder()).thenReturn(targetFolder);
        when(Bukkit.getServer().getWorld("no-legacy-world")).thenReturn(targetWorld);
        when(Bukkit.getServer().getWorlds()).thenReturn(List.of(targetWorld));

        configManager.initializeWorldMapMetadata(targetWorld, "no_legacy_world", "No Legacy World", "Tester");

        int migratedCount = configManager.autoMigrateLegacyConfigsForLoadedWorlds();

        assertThat(migratedCount).isZero();
        assertThat(configManager.loadSeatLocations(targetWorld)).isEmpty();
        assertThat(configManager.loadSpawnLocations(targetWorld)).isEmpty();
    }

    @Test
    @DisplayName("Should auto migrate linked seat locations only once across repeated startup runs")
    void shouldAutoMigrateLinkedSeatLocationsOnlyOnceAcrossRepeatedStartupRuns() {
        World globalWorld = mock(World.class);
        World targetWorld = mock(World.class);

        File globalFolder = tempDir.resolve("worlds").resolve("legacy-repeat-world").toFile();
        File targetFolder = tempDir.resolve("worlds").resolve("repeat-target-world").toFile();
        globalFolder.mkdirs();
        targetFolder.mkdirs();

        when(globalWorld.getName()).thenReturn("legacy-repeat-world");
        when(globalWorld.getWorldFolder()).thenReturn(globalFolder);
        when(targetWorld.getName()).thenReturn("repeat-target-world");
        when(targetWorld.getWorldFolder()).thenReturn(targetFolder);
        when(Bukkit.getServer().getWorld("legacy-repeat-world")).thenReturn(globalWorld);
        when(Bukkit.getServer().getWorld("repeat-target-world")).thenReturn(targetWorld);
        when(Bukkit.getServer().getWorlds()).thenReturn(List.of(targetWorld));

        configManager.initializeWorldMapMetadata(targetWorld, "repeat_target", "Repeat Target", "Tester");
        configManager.setDiscussionSeatSpawns(targetWorld, List.of(1));

        configManager.getConfig().set("discussion.seat-locations.1.world", "legacy-repeat-world");
        configManager.getConfig().set("discussion.seat-locations.1.x", 7.0);
        configManager.getConfig().set("discussion.seat-locations.1.y", 64.0);
        configManager.getConfig().set("discussion.seat-locations.1.z", 7.0);
        configManager.getConfig().set("discussion.seat-locations.1.yaw", 0.0);
        configManager.getConfig().set("discussion.seat-locations.1.pitch", 0.0);
        configManager.saveConfig();

        int firstRun = configManager.autoMigrateLegacyConfigsForLoadedWorlds();
        int secondRun = configManager.autoMigrateLegacyConfigsForLoadedWorlds();

        assertThat(firstRun).isEqualTo(1);
        assertThat(secondRun).isZero();
    }
}
