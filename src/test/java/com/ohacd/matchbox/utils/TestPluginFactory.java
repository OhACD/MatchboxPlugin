package com.ohacd.matchbox.utils;

import com.ohacd.matchbox.Matchbox;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.hologram.HologramManager;
import com.ohacd.matchbox.game.session.GameSession;
import com.ohacd.matchbox.game.session.SessionManager;
import com.ohacd.matchbox.game.utils.PlayerBackup;
import com.ohacd.matchbox.game.utils.Managers.InventoryManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Factory for creating mock Matchbox plugin instances for testing.
 * This ensures that Matchbox.getInstance() returns a properly mocked plugin
 * with all necessary dependencies initialized.
 */
public class TestPluginFactory {
    
    private static Matchbox mockPlugin;
    
    /**
     * Creates and sets up a mock Matchbox plugin instance for testing.
     * This should be called in @BeforeEach setup methods.
     */
    public static void setUpMockPlugin() {
        // CRITICAL: Mock Registry BEFORE any other initialization to prevent "No RegistryAccess implementation found" errors
        // Use a different approach - try to prevent Registry initialization by setting system properties or using a custom classloader
        try {
            // Try to set the RegistryAccess before Registry class loads
            System.setProperty("paper.registry.access.implementation", "mock");
            // Force load RegistryAccess first
            Class.forName("io.papermc.paper.registry.RegistryAccess");
        } catch (Exception e) {
            // Silently ignore if registry setup fails
        }

        // Note: This sets up Bukkit mocks first
        MockBukkitFactory.setUpBukkitMocks();

        // Initialize player registry
        MockBukkitFactory.initializePlayerRegistry();

        // Create mock plugin instance with minimal real methods to avoid initialization issues
        mockPlugin = mock(Matchbox.class);
        
        // Mock plugin metadata FIRST before any other mocking
        PluginMeta mockPluginMeta = mock(PluginMeta.class);
        when(mockPluginMeta.getName()).thenReturn("Matchbox");
        when(mockPluginMeta.getVersion()).thenReturn("0.9.5-test");
        
        // Use reflection to set the 'description' field in JavaPlugin since getName() is final
        try {
            // Find the 'description' field in class hierarchy
            java.lang.reflect.Field descriptionField = null;
            Class<?> clazz = JavaPlugin.class;
            while (clazz != null && descriptionField == null) {
                try {
                    descriptionField = clazz.getDeclaredField("description");
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            
            if (descriptionField != null) {
                descriptionField.setAccessible(true);
                
                // Mock PluginDescriptionFile
                org.bukkit.plugin.PluginDescriptionFile mockDescription = mock(org.bukkit.plugin.PluginDescriptionFile.class);
                when(mockDescription.getName()).thenReturn("Matchbox");
                when(mockDescription.getVersion()).thenReturn("0.9.5-test");
                
                descriptionField.set(mockPlugin, mockDescription);
            } else {
                // Try 'pluginMeta' field (Newer Paper versions)
                try {
                    java.lang.reflect.Field metaField = null;
                    clazz = JavaPlugin.class;
                    while (clazz != null && metaField == null) {
                        try {
                             metaField = clazz.getDeclaredField("pluginMeta");
                        } catch (NoSuchFieldException e) {
                             clazz = clazz.getSuperclass();
                        }
                    }
                    
                    if (metaField != null) {
                        metaField.setAccessible(true);
                        metaField.set(mockPlugin, mockPluginMeta);
                    } else {
                        System.err.println("Could not find 'description' or 'pluginMeta' field in JavaPlugin hierarchy.");
                    }
                } catch (Exception ex) {
                    System.err.println("Failed to set pluginMeta via reflection: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        } catch (Exception e) {
             System.err.println("Failed to set plugin description via reflection: " + e.getMessage());
             e.printStackTrace();
        }

        // Mock the static getInstance() method to return our mock
        try {
            var instanceField = Matchbox.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, mockPlugin);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up mock plugin instance", e);
        }
        
        // Create server mock separately
        var mockServer = MockBukkitFactory.createMockServer();
        
        // Set up basic plugin behavior (avoid calling getName() to prevent plugin meta access)
        when(mockPlugin.getServer()).thenReturn(mockServer);
        when(mockPlugin.isEnabled()).thenReturn(true);
        when(mockPlugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        
        // Mock data folder to prevent NullPointerException in ConfigManager
        java.io.File mockDataFolder = new java.io.File(System.getProperty("java.io.tmpdir"), "matchbox-test");
        when(mockPlugin.getDataFolder()).thenReturn(mockDataFolder);
        
        // Mock plugin namespace to prevent NamespacedKey issues in InventoryManager
        when(mockPlugin.getName()).thenReturn("matchbox");

        // PRE-INITIALIZE InventoryManager.VOTE_TARGET_KEY to avoid NPE in tests
        try {
            java.lang.reflect.Field voteKeyField = InventoryManager.class.getDeclaredField("VOTE_TARGET_KEY");
            voteKeyField.setAccessible(true);
            if (voteKeyField.get(null) == null) {
                // Create a NamespacedKey manually without using the plugin instance if possible
                // or use a dummy constructor. 
                // Since NamespacedKey(Plugin, String) is crashing, let's use the deprecated one for tests
                // or construct it such that it doesn't fail.
                // We'll try to use the deprecated constructor NamespacedKey(String namespace, String key)
                // which avoids accessing plugin.getName() entirely.
                @SuppressWarnings("deprecation")
                NamespacedKey key = new NamespacedKey("matchbox", "vote-target");
                voteKeyField.set(null, key);
            }
        } catch (Exception e) {
            System.err.println("Failed to inject VOTE_TARGET_KEY: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Create real SessionManager
        SessionManager realSessionManager = new SessionManager();
        when(mockPlugin.getSessionManager()).thenReturn(realSessionManager);
        
        // Create real GameManager with proper initialization
        // Use a real HologramManager mock
        var mockHologramManager = mock(HologramManager.class);
        GameManager realGameManager = new GameManager(mockPlugin, mockHologramManager);
        when(mockPlugin.getGameManager()).thenReturn(realGameManager);

        // Mock PluginManager
        PluginManager mockPluginManager = MockBukkitFactory.createMockPluginManager();
        when(mockPlugin.getServer().getPluginManager()).thenReturn(mockPluginManager);
    }
    
    /**
     * Cleans up the mock plugin instance after testing.
     * This should be called in @AfterEach cleanup methods.
     */
    public static void tearDownMockPlugin() {
        try {
            var instanceField = Matchbox.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to tear down mock plugin instance", e);
        }
        mockPlugin = null;
        
        // Clean up Bukkit mocks
        MockBukkitFactory.tearDownBukkitMocks();
    }
    
    /**
     * Gets the current mock plugin instance.
     * 
     * @return the mock plugin instance, or null if not set up
     */
    public static Matchbox getMockPlugin() {
        return mockPlugin;
    }
    
    /**
     * Gets the mock SessionManager from the current plugin instance.
     * 
     * @return the mock SessionManager
     */
    public static SessionManager getMockSessionManager() {
        if (mockPlugin == null) {
            throw new IllegalStateException("Mock plugin not set up. Call setUpMockPlugin() first.");
        }
        return mockPlugin.getSessionManager();
    }
    
    /**
     * Gets the mock GameManager from the current plugin instance.
     * 
     * @return the mock GameManager
     */
    public static GameManager getMockGameManager() {
        if (mockPlugin == null) {
            throw new IllegalStateException("Mock plugin not set up. Call setUpMockPlugin() first.");
        }
        return mockPlugin.getGameManager();
    }
}
