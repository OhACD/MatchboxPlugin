package com.ohacd.matchbox;

import com.ohacd.matchbox.command.MatchboxCommand;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.chat.ChatListener;
import com.ohacd.matchbox.game.hologram.HologramManager;
import com.ohacd.matchbox.game.session.SessionManager;
import com.ohacd.matchbox.game.ability.AbilityEventListener;
import com.ohacd.matchbox.game.ability.AbilityManager;
import com.ohacd.matchbox.game.ability.MedicAbilityListener;
import com.ohacd.matchbox.game.ability.MedicHitListener;
import com.ohacd.matchbox.game.ability.MedicSightListener;
import com.ohacd.matchbox.game.ability.DelusionActivationListener;
import com.ohacd.matchbox.game.ability.DelusionHitListener;
import com.ohacd.matchbox.game.ability.SparkSwapAbility;
import com.ohacd.matchbox.game.ability.SparkVisionListener;
import com.ohacd.matchbox.game.ability.SwipeActivationListener;
import com.ohacd.matchbox.game.ability.SwipeHitListener;
import com.ohacd.matchbox.game.utils.CheckProjectVersion;
import com.ohacd.matchbox.game.utils.Managers.NameTagManager;
import com.ohacd.matchbox.game.utils.ProjectStatus;
import com.ohacd.matchbox.game.utils.listeners.BlockInteractionProtectionListener;
import com.ohacd.matchbox.game.utils.listeners.DamageProtectionListener;
import com.ohacd.matchbox.game.utils.listeners.GameItemProtectionListener;
import com.ohacd.matchbox.game.utils.listeners.HitRevealListener;
import com.ohacd.matchbox.game.utils.listeners.PotBreakProtectionListener;
import com.ohacd.matchbox.game.utils.listeners.PlayerJoinListener;
import com.ohacd.matchbox.game.utils.listeners.PlayerQuitListener;
import com.ohacd.matchbox.game.utils.listeners.VoteItemListener;
import com.ohacd.matchbox.game.utils.listeners.VotePaperListener;
import com.ohacd.matchbox.game.nick.NickManager;
import com.ohacd.matchbox.game.sign.SignModeManager;
import com.ohacd.matchbox.game.sign.SignModeListener;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

/**
 * Main plugin class for Matchbox - a social deduction game for Minecraft.
 * Supports parallel game sessions with up to 7 players each.
 */
public final class Matchbox extends JavaPlugin {
    // Project status, versioning and update name
    private static final ProjectStatus projectStatus = ProjectStatus.STABLE; // Main toggle for project status
    private String updateName = "Community Edition!"; 
    private String currentVersion;
    private CheckProjectVersion versionChecker;

    private static Matchbox instance;
    private HologramManager hologramManager;
    private GameManager gameManager;
    private SessionManager sessionManager;
    private AbilityManager abilityManager;

    @Override
    public void onEnable() {
        instance = this;
        this.hologramManager = new HologramManager(this);
        this.gameManager = new GameManager(this, hologramManager);
        this.sessionManager = new SessionManager();
        this.abilityManager = new AbilityManager(gameManager);
        this.versionChecker = new CheckProjectVersion(this);
        this.currentVersion = getInstance().getPluginMeta().getVersion();

        int migratedWorlds = gameManager.getConfigManager().autoMigrateLegacyConfigsForLoadedWorlds();
        if (migratedWorlds > 0) {
            getLogger().info("Applied automatic legacy config migration for " + migratedWorlds + " world(s).");
        }

        // Initialise sign mode (inject into GameManager so it can use it)
        SignModeManager signModeManager = new SignModeManager(this);
        gameManager.setSignModeManager(signModeManager);

        // Initialise nick system (inject into GameManager so it can apply/restore nicks)
        NickManager nickManager = new NickManager(this);
        gameManager.setNickManager(nickManager);

        // Repeating task: show action bar reminder to any player who has a nick stored.
        // Suppressed while the player is inside an active game session (phases have their own action bar).
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (org.bukkit.entity.Player p : getServer().getOnlinePlayers()) {
                String nick = nickManager.getNick(p.getUniqueId());
                if (nick == null) continue;
                // Don't show while a game session is active for this player
                com.ohacd.matchbox.game.SessionGameContext ctx =
                        gameManager.getContextForPlayer(p.getUniqueId());
                if (ctx != null && ctx.getGameState().isGameActive()) continue;
                p.sendActionBar(net.kyori.adventure.text.Component.text(
                        "§7Currently Nicked as: §a" + nick));
            }
        }, 40L, 40L); // start after 2 s, repeat every 2 s (action bar fades after ~3 s)

        // Register event listeners
        getServer().getPluginManager().registerEvents(new ChatListener(hologramManager, gameManager), this);
        getServer().getPluginManager().registerEvents(
                new HitRevealListener(gameManager, hologramManager, gameManager.getInventoryManager()), this);
        getServer().getPluginManager().registerEvents(new GameItemProtectionListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new DamageProtectionListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new BlockInteractionProtectionListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new PotBreakProtectionListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new SignModeListener(gameManager, signModeManager), this);

        // Register abilities through a single event router
        abilityManager.registerAbility(new SwipeActivationListener(gameManager, this));
        abilityManager.registerAbility(new SwipeHitListener(gameManager));
        abilityManager.registerAbility(new SparkVisionListener(gameManager));
        abilityManager.registerAbility(new MedicAbilityListener(gameManager, this));
        abilityManager.registerAbility(new MedicHitListener(gameManager));
        abilityManager.registerAbility(new MedicSightListener(gameManager));
        abilityManager.registerAbility(new SparkSwapAbility(this));
        abilityManager.registerAbility(new DelusionActivationListener(gameManager, this));
        abilityManager.registerAbility(new DelusionHitListener(gameManager));
        getServer().getPluginManager().registerEvents(new AbilityEventListener(abilityManager), this);

        // Register voting listeners
        getServer().getPluginManager().registerEvents(new VoteItemListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new VotePaperListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(gameManager), this);
        
        // Register join listener for welcome messages
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, versionChecker), this);

        // Register command handler
        MatchboxCommand commandHandler = new MatchboxCommand(this, sessionManager, gameManager, nickManager);
        getCommand("matchbox").setExecutor(commandHandler);
        getCommand("matchbox").setTabCompleter(commandHandler);

        getLogger().info("Matchbox v" + currentVersion + " enabled");
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

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public String getCurrentVersion() { return currentVersion; }

    public String getProjectStatus() { return projectStatus.getDisplayName();}

    public String getUpdateName() { return updateName; }
}
