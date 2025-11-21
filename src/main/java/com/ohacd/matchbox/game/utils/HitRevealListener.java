package com.ohacd.matchbox.game.utils;

import com.ohacd.matchbox.game.hologram.HologramManager;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class HitRevealListener implements Listener {
    private final GameManager gameManager;
    private final HologramManager hologramManager;

    public HitRevealListener(GameManager gameManager, HologramManager hologramManager) {
        this.gameManager = gameManager;
        this.hologramManager = hologramManager;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getDamager() instanceof Arrow arrow) {
            if (arrow.getShooter() instanceof Player) {
                Player shooter = (Player) arrow.getShooter();
                Player target = (Player) event.getEntity();
                // For now will assume that any arrow shot during the game by the player is valid
                // Might add a check to make sure that the player used the given crossbow
                // Reveal player identity for 10 seconds
                hologramManager.showTextAbove(target, target.getName(), 200);

                // if shooter is the spark mark swiped
                gameManager.handleSwipe(shooter, target);
            }
        }
    }
}
