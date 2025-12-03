package com.ohacd.matchbox;

import com.ohacd.matchbox.command.MatchboxCommand;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.chat.ChatListener;
import com.ohacd.matchbox.game.hologram.HologramManager;
import com.ohacd.matchbox.game.session.SessionManager;
import com.ohacd.matchbox.game.ability.SwipeActivationListener;
import com.ohacd.matchbox.game.ability.SparkVisionListener;
import com.ohacd.matchbox.game.ability.SwipeHitListener;
import com.ohacd.matchbox.game.ability.MedicAbilityListener;
import com.ohacd.matchbox.game.ability.MedicHitListener;
import com.ohacd.matchbox.game.ability.MedicSightListener;
import com.ohacd.matchbox.game.utils.CheckProjectVersion;
import com.ohacd.matchbox.game.utils.Managers.NameTagManager;
import com.ohacd.matchbox.game.utils.ProjectStatus;
import com.ohacd.matchbox.game.utils.listeners.BlockInteractionProtectionListener;
import com.ohacd.matchbox.game.utils.listeners.DamageProtectionListener;
import com.ohacd.matchbox.game.utils.listeners.GameItemProtectionListener;
import com.ohacd.matchbox.game.utils.listeners.HitRevealListener;
import com.ohacd.matchbox.game.utils.listeners.PlayerJoinListener;
import com.ohacd.matchbox.game.utils.listeners.PlayerQuitListener;
import com.ohacd.matchbox.game.utils.listeners.VoteItemListener;
import com.ohacd.matchbox.game.utils.listeners.VotePaperListener;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

/**
 * Main plugin class for Matchbox - a social deduction game for Minecraft.
 * Supports parallel game sessions with up to 7 players each.
 */
public final class Matchbox extends JavaPlugin {
    // Project status, versioning and update name
    private static final ProjectStatus projectStatus = ProjectStatus.DEVELOPMENT; // Main toggle for project status
    private String updateName = "It's all about the base";
    private String currentVersion;
    private CheckProjectVersion versionChecker;

    private static Matchbox instance;
    private HologramManager hologramManager;
    private GameManager gameManager;
    private SessionManager sessionManager;

    @Override
    public void onEnable() {
        instance = this;
        this.hologramManager = new HologramManager(this);
        this.gameManager = new GameManager(this, hologramManager);
        this.sessionManager = new SessionManager();
        this.versionChecker = new CheckProjectVersion(this);
        this.currentVersion = getInstance().getPluginMeta().getVersion();

        // Register event listeners
        getServer().getPluginManager().registerEvents(new ChatListener(hologramManager, gameManager), this);
        getServer().getPluginManager().registerEvents(
                new HitRevealListener(gameManager, hologramManager, gameManager.getInventoryManager()), this);
        getServer().getPluginManager().registerEvents(new GameItemProtectionListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new DamageProtectionListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new BlockInteractionProtectionListener(gameManager), this);

        // Register ability listeners
        getServer().getPluginManager().registerEvents(new SwipeActivationListener(gameManager, this), this);
        getServer().getPluginManager().registerEvents(new SwipeHitListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new SparkVisionListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new MedicAbilityListener(gameManager, this), this);
        getServer().getPluginManager().registerEvents(new MedicHitListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new MedicSightListener(gameManager), this);

        // Register voting listeners
        getServer().getPluginManager().registerEvents(new VoteItemListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new VotePaperListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(gameManager), this);
        
        // Register join listener for welcome messages
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, versionChecker), this);

        // Register command handler
        MatchboxCommand commandHandler = new MatchboxCommand(this, sessionManager, gameManager);
        getCommand("matchbox").setExecutor(commandHandler);
        getCommand("matchbox").setTabCompleter(commandHandler);

        getLogger().info("Matchbox" + currentVersion + "enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling Matchbox plugin...");

        // End all active games first (this cancels all tasks)
        if (gameManager != null) {
            try {
                Set<String> activeSessions = gameManager.getActiveSessionNames();
                getLogger().info("Ending " + activeSessions.size() + " active game session(s)...");
                for (String sessionName : activeSessions) {
                    try {
                        gameManager.endGame(sessionName);
                    } catch (Exception e) {
                        getLogger().warning("Error ending session " + sessionName + ": " + e.getMessage());
                    }
                }

                gameManager.emergencyCleanup();
            } catch (Exception e) {
                getLogger().severe("Error during plugin shutdown cleanup: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (hologramManager != null) {
            hologramManager.clearAll();
        }

        getLogger().info("Restoring all nametags...");
        NameTagManager.restoreAllNameTags();

        // Ensure no outstanding tasks continue after disable.
        getServer().getScheduler().cancelTasks(this);

        getLogger().info("Matchbox disabled");
    }

    public static Matchbox getInstance() {
        return instance;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public String getCurrentVersion() { return currentVersion; }

    public String getProjectStatus() { return projectStatus.getDisplayName();}

    public String getUpdateName() { return updateName; }
}