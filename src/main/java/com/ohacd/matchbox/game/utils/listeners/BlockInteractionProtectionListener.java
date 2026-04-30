package com.ohacd.matchbox.game.utils.listeners;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.utils.GamePhase;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents players from interacting with blocks during active games.
 * Only allows interactions with items (for abilities and voting).
 */
public class BlockInteractionProtectionListener implements Listener {
    private final GameManager gameManager;

    /**
     * Creates a listener that prevents block interactions during active games.
     *
     * @param gameManager the game manager used to check active sessions
     */
    public BlockInteractionProtectionListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Prevents block interactions during active games.
     * Allows item interactions (for abilities and voting).
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in an active game
        if (!isPlayerInActiveGame(player)) {
            return;
        }

        // Allow item interactions (abilities, voting papers, etc.)
        // These are handled by other listeners
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_AIR) {
            return;
        }

        // Block all block interactions (right-click on blocks, left-click on blocks)
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();

            // Allow right-click sign placement in sign mode during SWIPE phase
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                    && gameManager.isSignModeEnabled()
                    && isInSwipePhase(player)
                    && isHoldingSignModeSignItem(player)
                    && (clickedBlock == null || !isFlowerPot(clickedBlock.getType()))) {
                return;
            }

            // Allow left-click sign breaking in sign mode during SWIPE phase
            if (event.getAction() == Action.LEFT_CLICK_BLOCK
                    && gameManager.isSignModeEnabled()
                    && isInSwipePhase(player)
                    && clickedBlock != null
                    && clickedBlock.getType().name().contains("SIGN")
                    && isHoldingSignModeAxe(player)) {
                return;
            }

            if (clickedBlock != null) {
                // Specifically prevent flower pot interactions to avoid duplication bug
                Material blockType = clickedBlock.getType();
                if (isFlowerPot(blockType)) {
                    event.setCancelled(true);
                    return;
                }
            }
            event.setCancelled(true);
        }
    }

    /**
     * Checks if a player is in an active game.
     */
    private boolean isPlayerInActiveGame(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        SessionGameContext context = gameManager.getContextForPlayer(player.getUniqueId());
        if (context == null) {
            return false;
        }

        return context.getGameState().isGameActive();
    }

    /**
     * Returns {@code true} if the player is currently in the SWIPE phase.
     */
    private boolean isInSwipePhase(Player player) {
        SessionGameContext context = gameManager.getContextForPlayer(player.getUniqueId());
        if (context == null) return false;
        return context.getPhaseManager().getCurrentPhase() == GamePhase.SWIPE;
    }

    /**
     * Returns {@code true} if the item held in the player's main hand is a
     * sign-mode sign item (i.e. an OAK_SIGN marked as a game item via the
     * sign-mode PDC tag).
     */
    private boolean isHoldingSignModeSignItem(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() == Material.AIR) return false;
        // Delegate to SignModeManager via GameManager
        return gameManager.isSignModeItem(held);
    }

    /**
     * Returns {@code true} if the item held in the player's main hand is the
     * sign-mode wooden axe.
     */
    private boolean isHoldingSignModeAxe(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() != Material.WOODEN_AXE) return false;
        return gameManager.isSignModeItem(held);
    }

    /**
     * Matches empty and occupied flower pot variants across supported versions.
     */
    private boolean isFlowerPot(Material material) {
        if (material == null) {
            return false;
        }
        return material == Material.FLOWER_POT || material.name().startsWith("POTTED_");
    }
}

