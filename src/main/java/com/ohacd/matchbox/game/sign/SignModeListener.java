package com.ohacd.matchbox.game.sign;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.utils.GamePhase;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.UUID;

/**
 * Handles sign-mode gameplay events:
 *
 * <ul>
 *   <li>{@link BlockPlaceEvent} — tracks every sign placed by an in-game player.</li>
 *   <li>{@link BlockBreakEvent} — allows owners to break their own tracked signs with the
 *       sign-mode axe; prevents all other player breaks; cleans up the tracking entry on
 *       non-player destruction (pistons, explosions, etc.).</li>
 * </ul>
 *
 * <p>Sign placement is only permitted during the SWIPE phase. Players read signs directly
 * in the world; no chat broadcast is performed.</p>
 */
public class SignModeListener implements Listener {

    private final GameManager gameManager;
    private final SignModeManager signModeManager;

    public SignModeListener(GameManager gameManager, SignModeManager signModeManager) {
        if (gameManager == null || signModeManager == null) {
            throw new IllegalArgumentException("GameManager and SignModeManager cannot be null");
        }
        this.gameManager = gameManager;
        this.signModeManager = signModeManager;
    }

    // -------------------------------------------------------------------------
    // Block placement — track sign positions
    // -------------------------------------------------------------------------

    /**
     * Tracks a sign placed by an in-game player during the SWIPE phase.
     *
     * <p>Runs at LOW priority so it executes before any NORMAL/HIGH-priority
     * cancellations (but after LOWEST, which the adventure-mode CanPlaceOn
     * check uses).  If the placement has already been cancelled upstream we
     * skip tracking.</p>
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        Block placed = event.getBlockPlaced();
        if (placed == null || !placed.getType().name().contains("SIGN")) return;

        SessionGameContext context = gameManager.getContextForPlayer(player.getUniqueId());
        if (context == null || !context.getGameState().isGameActive()) return;

        // Only track during SWIPE phase
        if (context.getPhaseManager().getCurrentPhase() != GamePhase.SWIPE) {
            // Cancel the placement outside of SWIPE phase as a safety measure
            event.setCancelled(true);
            return;
        }

        // Only track if sign mode is enabled for this session
        if (!gameManager.isSignModeEnabled()) return;

        // Only count players with sign-mode sign items (not random signs from outside)
        if (!signModeManager.isSignModeItem(event.getItemInHand())) return;

        String sessionName = context.getSessionName();
        UUID placerId = player.getUniqueId();

        signModeManager.registerSignPlacement(sessionName, placed.getLocation(), placerId);
    }

    // -------------------------------------------------------------------------
    // Block break — protect tracked signs
    // -------------------------------------------------------------------------

    /**
     * Handles sign breaking during an active game.
     *
     * <ul>
     *   <li>Players may break their own tracked signs with the sign-mode axe during SWIPE phase.</li>
     *   <li>Players cannot break signs placed by other players.</li>
     *   <li>Non-player break causes (pistons, explosions) silently remove the entry from tracking.</li>
     * </ul>
     *
     * <p>Runs at HIGHEST priority to ensure the cancellation takes effect
     * before other listeners process the event.</p>
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (block == null) return;

        if (!block.getType().name().contains("SIGN")) return;

        // Find which session (if any) this sign belongs to
        String sessionName = findSessionForSign(block);
        if (sessionName == null) return;

        if (player != null) {
            SessionGameContext context = gameManager.getContextForPlayer(player.getUniqueId());

            // Allow the owner to break their own sign with the axe during SWIPE phase
            if (context != null
                    && context.getGameState().isGameActive()
                    && context.getPhaseManager().getCurrentPhase() == GamePhase.SWIPE
                    && gameManager.isSignModeEnabled()
                    && isHoldingSignAxe(player)
                    && player.getUniqueId().equals(signModeManager.getSignPlacer(sessionName, block.getLocation()))) {
                // Force server-side removal and broadcast block update to avoid client desync in adventure mode.
                signModeManager.removeSignTracking(sessionName, block.getLocation());
                removeSignBlockForEveryone(block);
                event.setCancelled(true);
                return;
            }

            // All other player break attempts on tracked signs are blocked
            event.setCancelled(true);
            return;
        }

        // Non-player break (piston, explosion, etc.) — remove from tracking and make sure clients see air.
        signModeManager.removeSignTracking(sessionName, block.getLocation());
        removeSignBlockForEveryone(block);
        event.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Searches all active session contexts to find whether the given block's
     * location is tracked as a sign-mode sign.  Returns the session name or
     * {@code null} if not found.
     */
    private String findSessionForSign(Block block) {
        // We need to check all sessions — gameManager exposes active session names
        for (String sName : gameManager.getActiveSessionNames()) {
            if (signModeManager.isTrackedSign(sName, block.getLocation())) {
                return sName;
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if the player is holding the sign-mode wooden axe in their main hand.
     */
    private boolean isHoldingSignAxe(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() != Material.WOODEN_AXE) return false;
        return signModeManager.isSignModeItem(held);
    }

    private void removeSignBlockForEveryone(Block block) {
        if (block == null) return;

        block.setType(Material.AIR, false);
        if (block.getWorld() == null) return;

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendBlockChange(block.getLocation(), Material.AIR.createBlockData());
        }
    }
}
