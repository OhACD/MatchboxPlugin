package com.ohacd.matchbox.game;

import com.ohacd.matchbox.game.hologram.HologramManager;
import com.ohacd.matchbox.game.utils.GamePhase;
import com.ohacd.matchbox.game.utils.Role;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.bukkit.Bukkit.broadcast;

public class GameManager {
    private final Plugin plugin;
    private final HologramManager hologramManager;

    private GamePhase phase = GamePhase.WAITING;
    private BukkitRunnable swipeTask = null;
    private final int DEFAULT_SWIPE_SECONDS = 30;


    // Per round state
    private final Map<UUID, Role> roles = new HashMap<>(); // Holds player UUIDs and roles
    private final Set<UUID> players = new HashSet<>(); // Holds all players
    private final Set<UUID> swipedThisRound = new HashSet<>(); // Holds players that swiped other players this round (cleaned every round)
    private final Set<UUID> curedThisRound = new HashSet<>(); // Holds players that cured other players this round (cleaned every round)

    public GameManager(Plugin plugin, HologramManager hologramManager) {
        this.plugin = plugin;
        this.hologramManager = hologramManager;
    }

    public void startRound(Collection<Player> players) {
        players.clear();
        roles.clear();
        swipedThisRound.clear();
        curedThisRound.clear();
        // Assign roles (randomly choose one spark, one medic and rest innocent)
        List<Player> list = new ArrayList<>(players);
        Collections.shuffle(list);
        if (!list.isEmpty()) {
            roles.put(list.get(0).getUniqueId(), Role.SPARK);
            // TODO: Find a way to keep track of players
        }
        if (list.size() > 1) {
            roles.put(list.get(1).getUniqueId(), Role.MEDIC);
        }
        for (int i = 2; i < list.size(); i++) {
            roles.put(list.get(i).getUniqueId(), Role.INNOCENT);
        }
        // TODO:Give items, set role paper to the top right slot in the inventory.

        // Starts the swipe phase
        startSwipePhase(DEFAULT_SWIPE_SECONDS);
    }

    public void startSwipePhase(int seconds) {
        cancelSwipeTask();

        this.phase = GamePhase.SWIPE;
        plugin.getServer().sendPlainMessage("Swipe phase started! You have " + seconds + " seconds to swipe.");
        AtomicInteger remaining = new AtomicInteger(seconds);

        swipeTask = new BukkitRunnable() {
            @Override
            public void run() {
                int secs = remaining.getAndDecrement();
                if (secs <= 0) {
                    cancel();
                    swipeTask = null;
                    endSwipePhase();
                    return;
                }
                // Updates actionbar for all alive players
                for (Player player : getAlivePlayerObjects()) {
                    sendActionBar(player, "Swipe: " + secs + "s");
                }
                // Broadcast at specific times
                if (secs == 10 || secs == 5 || secs <= 3) {
                    plugin.getServer().sendPlainMessage("Swipe phase ends in " + secs + " seconds!");
                }
            }
        };
        swipeTask.runTaskTimer(plugin, 0L, 20L);
    }

    public void cancelSwipeTask() {
        if (swipeTask != null) {
            try {
                swipeTask.cancel();
            } catch (IllegalStateException ignored) {}
            swipeTask = null;
        }
    }

    public void endSwipePhase() {
        this.phase = GamePhase.DISCUSSION;
        plugin.getServer().sendPlainMessage("Swipe phase ended!");
        startDiscussionPhase(); // TODO: Death happens hear
    }

    public void sendActionBar(Player player, String message) {
        try {
            player.sendActionBar(Component.text(message));
        } catch (NoSuchMethodError | NoClassDefFoundError error) {
            player.sendActionBar(message);
        }
    }

    public void broadcast(String message) {
        plugin.getServer().broadcastMessage(message);
    }

    public void handleSwipe(Player shooter, Player target) {
        // verify phases
        if (phase != GamePhase.SWIPE) {
            shooter.sendMessage("You cannot swipe right now");
            return;
        }
        UUID shooterid = shooter.getUniqueId();
        if (roles.get(shooterid) == Role.SPARK) {
            // Spark can only swipe once per round
            if (swipedThisRound.contains(shooterid)) return;
            swipedThisRound.add(shooterid);
        }
    }
}
