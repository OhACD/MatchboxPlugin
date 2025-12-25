package com.ohacd.matchbox.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.any;

/**
 * Factory for creating mock Bukkit objects for testing.
 * Provides consistent mocking across all test classes.
 */
public class MockBukkitFactory {
    
    private static final String TEST_WORLD_NAME = "test-world";
    private static final UUID TEST_UUID = UUID.randomUUID();
    private static final String TEST_PLAYER_NAME = "TestPlayer";
    
    /**
     * Creates a mock Player with basic setup.
     */
    public static Player createMockPlayer() {
        Player player = mock(Player.class);
        
        // Basic player properties
        when(player.getUniqueId()).thenReturn(TEST_UUID);
        when(player.getName()).thenReturn(TEST_PLAYER_NAME);
        when(player.getDisplayName()).thenReturn(TEST_PLAYER_NAME);
        when(player.isOnline()).thenReturn(true);
        
        // Inventory mock
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getContents()).thenReturn(new ItemStack[0]);
        
        // Location mock - create world first, then location
        World world = createMockWorld();
        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.getX()).thenReturn(0.0);
        when(location.getY()).thenReturn(64.0);
        when(location.getZ()).thenReturn(0.0);
        when(location.getYaw()).thenReturn(0.0f);
        when(location.getPitch()).thenReturn(0.0f);
        
        // Set up player location with proper chaining
        when(player.getLocation()).thenReturn(location);
        when(player.getWorld()).thenReturn(world);
        
        // Health and game state
        when(player.getHealth()).thenReturn(20.0);
        when(player.isDead()).thenReturn(false);
        when(player.getAllowFlight()).thenReturn(false);
        
        return player;
    }
    
    /**
     * Creates a mock Player with custom UUID and name.
     */
    public static Player createMockPlayer(UUID uuid, String name) {
        Player player = createMockPlayer();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn(name);
        when(player.getDisplayName()).thenReturn(name);
        return player;
    }
    
    /**
     * Creates a list of mock players for testing.
     */
    public static List<Player> createMockPlayers(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> createMockPlayer(
                UUID.randomUUID(),
                "TestPlayer" + i
            ))
            .toList();
    }
    
    /**
     * Creates a mock Location with test world.
     */
    public static Location createMockLocation() {
        return createMockLocation(0, 64, 0, 0, 0);
    }
    
    /**
     * Creates a mock Location with custom coordinates.
     */
    public static Location createMockLocation(double x, double y, double z, float yaw, float pitch) {
        World world = createMockWorld();
        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.getX()).thenReturn(x);
        when(location.getY()).thenReturn(y);
        when(location.getZ()).thenReturn(z);
        when(location.getYaw()).thenReturn(yaw);
        when(location.getPitch()).thenReturn(pitch);
        return location;
    }
    
    /**
     * Creates a mock World.
     */
    public static World createMockWorld() {
        World world = mock(World.class);
        when(world.getName()).thenReturn(TEST_WORLD_NAME);
        when(world.getUID()).thenReturn(UUID.randomUUID());
        return world;
    }
    
    /**
     * Creates a mock Server.
     */
    public static Server createMockServer() {
        Server server = mock(Server.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getBukkitVersion()).thenReturn("1.21.10-R0.1-SNAPSHOT");

        // Mock Scheduler
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(server.getScheduler()).thenReturn(scheduler);

        return server;
    }

    /**
     * Creates a mock Server with player registry support.
     * This version supports player lookups for testing.
     */
    public static Server createMockServerWithPlayerRegistry() {
        Server server = createMockServer();

        // Create a static player registry that can be accessed globally
        if (globalPlayerRegistry == null) {
            globalPlayerRegistry = new java.util.HashMap<>();
        }

        // Mock getPlayer methods to use the global registry
        when(server.getPlayer(any(UUID.class))).thenAnswer(invocation -> {
            UUID uuid = invocation.getArgument(0);
            return globalPlayerRegistry.get(uuid);
        });

        when(server.getPlayer(any(String.class))).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            return globalPlayerRegistry.values().stream()
                .filter(p -> name != null && name.equals(p.getName()))
                .findFirst()
                .orElse(null);
        });

        when(server.getOnlinePlayers()).thenAnswer(invocation ->
            java.util.Collections.unmodifiableCollection(globalPlayerRegistry.values()));

        return server;
    }

    // Global registry for all mock servers
    private static Map<UUID, Player> globalPlayerRegistry;

    /**
     * Registers a mock player with the current mock server.
     * This allows GameSession.getPlayers() to work properly in tests.
     */
    public static void registerMockPlayer(Player player) {
        if (player == null || globalPlayerRegistry == null) return;

        globalPlayerRegistry.put(player.getUniqueId(), player);
    }
    
    /**
     * Creates a mock Plugin.
     */
    public static Plugin createMockPlugin() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("Matchbox");
        when(plugin.isEnabled()).thenReturn(true);
        return plugin;
    }
    
    /**
     * Creates a mock PluginManager.
     */
    public static PluginManager createMockPluginManager() {
        PluginManager pluginManager = mock(PluginManager.class);
        doNothing().when(pluginManager).registerEvents(any(), any());
        return pluginManager;
    }
    
    /**
     * Creates a mock ItemStack.
     */
    public static ItemStack createMockItemStack(Material material, int amount) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        when(item.getAmount()).thenReturn(amount);
        return item;
    }
    
    /**
     * Creates a mock ItemStack with default material.
     */
    public static ItemStack createMockItemStack() {
        return createMockItemStack(Material.PAPER, 1);
    }
    
    /**
     * Initializes the global player registry for testing.
     * This should be called before creating mock players.
     */
    public static void initializePlayerRegistry() {
        if (globalPlayerRegistry == null) {
            globalPlayerRegistry = new java.util.HashMap<>();
        }
    }

    /**
     * Sets up static Bukkit mocks for testing.
     * Call this method in @BeforeEach setup methods.
     */
    public static void setUpBukkitMocks() {
        try {
            // Use reflection to set static Bukkit mocks with player registry
            var serverField = Bukkit.class.getDeclaredField("server");
            serverField.setAccessible(true);
            serverField.set(null, createMockServerWithPlayerRegistry());
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up Bukkit mocks", e);
        }
    }
    
    /**
     * Cleans up static Bukkit mocks after testing.
     * Call this method in @AfterEach cleanup methods.
     */
    public static void tearDownBukkitMocks() {
        try {
            // Use reflection to clear static Bukkit mocks
            var serverField = Bukkit.class.getDeclaredField("server");
            serverField.setAccessible(true);
            serverField.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to tear down Bukkit mocks", e);
        }
    }
    
    /**
     * Creates a test configuration object.
     */
    public static TestGameConfig createTestConfig() {
        return new TestGameConfig();
    }
    
    /**
     * Creates a test configuration with custom values.
     */
    public static TestGameConfig createTestConfig(int minPlayers, int maxPlayers, 
                                          int swipeDuration, int discussionDuration, int votingDuration) {
        TestGameConfig config = new TestGameConfig();
        config.setMinPlayers(minPlayers);
        config.setMaxPlayers(maxPlayers);
        config.setSwipeDuration(swipeDuration);
        config.setDiscussionDuration(discussionDuration);
        config.setVotingDuration(votingDuration);
        return config;
    }
}
