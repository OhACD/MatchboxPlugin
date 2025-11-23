package com.ohacd.matchbox;

import com.ohacd.matchbox.command.MatchboxCommand;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.chat.ChatListener;
import com.ohacd.matchbox.game.hologram.HologramManager;
import com.ohacd.matchbox.game.session.SessionManager;
import com.ohacd.matchbox.game.utils.HitRevealListener;
import com.ohacd.matchbox.game.utils.NameTagManager;
import com.ohacd.matchbox.game.utils.PlayerQuitListener;
import com.ohacd.matchbox.game.utils.VoteItemListener;
import org.bukkit.plugin.java.JavaPlugin;

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
        getServer().getPluginManager().registerEvents(new HitRevealListener(gameManager, hologramManager), this);
        getServer().getPluginManager().registerEvents(new VoteItemListener(), this);
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