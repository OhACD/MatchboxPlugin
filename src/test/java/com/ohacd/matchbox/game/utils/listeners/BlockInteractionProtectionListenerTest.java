package com.ohacd.matchbox.game.utils.listeners;

import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.state.GameState;
import com.ohacd.matchbox.game.phase.PhaseManager;
import com.ohacd.matchbox.game.utils.GamePhase;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regressions for the flower-pot duplication bug. Right-clicking a flower pot
 * during an active game must always be cancelled, even when sign mode is on
 * and the player is in the SWIPE phase holding a sign-mode sign.
 */
class BlockInteractionProtectionListenerTest {

    private PlayerInteractEvent buildRightClickPot(Player player, Block pot) {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getClickedBlock()).thenReturn(pot);
        return event;
    }

    @Test
    @DisplayName("Right-clicking a flower pot during active game is cancelled (even with sign in hand)")
    void rightClickFlowerPotWithSignIsCancelled() {
        GameManager gameManager = mock(GameManager.class);
        SessionGameContext context = mock(SessionGameContext.class);
        GameState gameState = mock(GameState.class);
        PhaseManager phaseManager = mock(PhaseManager.class);

        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(player.isOnline()).thenReturn(true);

        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack sign = mock(ItemStack.class);
        when(sign.getType()).thenReturn(Material.OAK_SIGN);
        when(inv.getItemInMainHand()).thenReturn(sign);
        when(player.getInventory()).thenReturn(inv);

        when(gameManager.getContextForPlayer(id)).thenReturn(context);
        when(gameManager.isSignModeEnabled()).thenReturn(true);
        when(gameManager.isSignModeItem(sign)).thenReturn(true);
        when(context.getGameState()).thenReturn(gameState);
        when(context.getPhaseManager()).thenReturn(phaseManager);
        when(gameState.isGameActive()).thenReturn(true);
        when(phaseManager.getCurrentPhase()).thenReturn(GamePhase.SWIPE);

        Block pot = mock(Block.class);
        when(pot.getType()).thenReturn(Material.FLOWER_POT);

        PlayerInteractEvent event = buildRightClickPot(player, pot);

        new BlockInteractionProtectionListener(gameManager).onPlayerInteract(event);

        verify(event).setCancelled(true);
    }

    @Test
    @DisplayName("Right-clicking a potted flower variant during active game is cancelled")
    void rightClickPottedVariantIsCancelled() {
        GameManager gameManager = mock(GameManager.class);
        SessionGameContext context = mock(SessionGameContext.class);
        GameState gameState = mock(GameState.class);
        PhaseManager phaseManager = mock(PhaseManager.class);

        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(player.isOnline()).thenReturn(true);

        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack air = mock(ItemStack.class);
        when(air.getType()).thenReturn(Material.AIR);
        when(inv.getItemInMainHand()).thenReturn(air);
        when(player.getInventory()).thenReturn(inv);

        when(gameManager.getContextForPlayer(id)).thenReturn(context);
        when(gameManager.isSignModeEnabled()).thenReturn(false);
        when(context.getGameState()).thenReturn(gameState);
        when(context.getPhaseManager()).thenReturn(phaseManager);
        when(gameState.isGameActive()).thenReturn(true);
        when(phaseManager.getCurrentPhase()).thenReturn(GamePhase.DISCUSSION);

        Block pot = mock(Block.class);
        when(pot.getType()).thenReturn(Material.POTTED_DANDELION);

        PlayerInteractEvent event = buildRightClickPot(player, pot);

        new BlockInteractionProtectionListener(gameManager).onPlayerInteract(event);

        verify(event).setCancelled(true);
    }

    @Test
    @DisplayName("Right-clicking blocks outside any active game is not cancelled")
    void rightClickWhenNotInGameIsAllowed() {
        GameManager gameManager = mock(GameManager.class);

        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(player.isOnline()).thenReturn(true);
        when(gameManager.getContextForPlayer(id)).thenReturn(null);

        Block pot = mock(Block.class);
        PlayerInteractEvent event = buildRightClickPot(player, pot);

        new BlockInteractionProtectionListener(gameManager).onPlayerInteract(event);

        verify(event, never()).setCancelled(true);
    }
}
