package com.ohacd.matchbox;

import com.ohacd.matchbox.command.MatchboxCommand;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.chat.ChatListener;
import com.ohacd.matchbox.game.hologram.HologramManager;
import com.ohacd.matchbox.game.session.SessionManager;
import com.ohacd.matchbox.game.utils.GameItemProtectionListener;
import com.ohacd.matchbox.game.utils.HitRevealListener;
import com.ohacd.matchbox.game.utils.NameTagManager;
import com.ohacd.matchbox.game.utils.PlayerQuitListener;
import com.ohacd.matchbox.game.utils.VoteItemListener;
import com.ohacd.matchbox.game.utils.VotePaperListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

public final class Matchbox extends JavaPlugin {
    private static Matchbox instance;
    private HologramManager hologramManager;
    private GameManager gameManager;
    private SessionManager sessionManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        this.hologramManager = new HologramManager(this);
        this.gameManager = new GameManager(this, hologramManager);
        this.sessionManager = new SessionManager();

        // Register events
        getServer().getPluginManager().registerEvents(new ChatListener(hologramManager, gameManager), this);
        getServer().getPluginManager().registerEvents(new HitRevealListener(gameManager, hologramManager, gameManager.getInventoryManager()), this);
        getServer().getPluginManager().registerEvents(new GameItemProtectionListener(), this);
        // Register swipe ability listeners
        getServer().getPluginManager().registerEvents(new com.ohacd.matchbox.game.ability.SwipeActivationListener(gameManager, this), this);
        getServer().getPluginManager().registerEvents(new com.ohacd.matchbox.game.ability.SwipeHitListener(gameManager), this);
        // Register spark vision listener
        getServer().getPluginManager().registerEvents(new com.ohacd.matchbox.game.ability.SparkVisionListener(gameManager), this);
        // Register medic ability listeners
        // Healing Touch (Cure) - slot 9
        getServer().getPluginManager().registerEvents(new com.ohacd.matchbox.game.ability.MedicAbilityListener(gameManager, this), this);
        getServer().getPluginManager().registerEvents(new com.ohacd.matchbox.game.ability.MedicHitListener(gameManager), this);
        // Healing Sight - slot 8
        getServer().getPluginManager().registerEvents(new com.ohacd.matchbox.game.ability.MedicSightListener(gameManager), this);
        // Voting listeners
        getServer().getPluginManager().registerEvents(new VoteItemListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new VotePaperListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(gameManager), this);
        // Register command
        MatchboxCommand commandHandler = new MatchboxCommand(this, sessionManager, gameManager);
        getCommand("matchbox").setExecutor(commandHandler);
        getCommand("matchbox").setTabCompleter(commandHandler);

        getLogger().info("Matchbox enabled");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
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
                
                // Emergency cleanup in case anything was missed
                gameManager.emergencyCleanup();
            } catch (Exception e) {
                getLogger().severe("Error during plugin shutdown cleanup: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Clear all holograms
        if (hologramManager != null) {
            hologramManager.clearAll();
        }

        // Restore all nametags before shutdown
        getLogger().info("Restoring all nametags...");
        NameTagManager.restoreAllNameTags();

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
}