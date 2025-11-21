package com.ohacd.matchbox;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.chat.ChatListener;
import com.ohacd.matchbox.game.hologram.HologramManager;
import com.ohacd.matchbox.game.utils.HitRevealListener;
import com.ohacd.matchbox.game.utils.VoteItemListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Matchbox extends JavaPlugin {
    private static Matchbox instance;
    private HologramManager hologramManager;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        this.gameManager = new GameManager(this, hologramManager);

        getServer().getPluginManager().registerEvent(new ChatListener(hologramManager), this);
        getServer().getPluginManager().registerEvent(new HitRevealListener(gameManager), this);
        getServer().getPluginManager().registerEvent(new VoteItemListener(gameManager), this);

        getLogger().info("Matchbox enabled");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        hologramManager.clearAll();
    }

    public static Matchbox getInstance() {return instance; }
    public HologramManager getHologramManager() {return hologramManager; }
    public GameManager getGameManager() {return gameManager; }
}
