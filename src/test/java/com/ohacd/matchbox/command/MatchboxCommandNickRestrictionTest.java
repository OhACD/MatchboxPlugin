package com.ohacd.matchbox.command;

import com.ohacd.matchbox.Matchbox;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.SessionGameContext;
import com.ohacd.matchbox.game.config.ConfigManager;
import com.ohacd.matchbox.game.nick.NickManager;
import com.ohacd.matchbox.game.session.SessionManager;
import com.ohacd.matchbox.game.state.GameState;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression: non-admin players cannot change their nick while their session
 * has an active game in progress. Admin override is exempt.
 */
class MatchboxCommandNickRestrictionTest {

    private MatchboxCommand executor;
    private Player player;
    private Command command;
    private GameManager gameManager;
    private NickManager nickManager;
    private SessionGameContext context;
    private GameState gameState;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        Matchbox plugin = mock(Matchbox.class);
        SessionManager sessionManager = mock(SessionManager.class);
        gameManager = mock(GameManager.class);
        nickManager = mock(NickManager.class);
        ConfigManager configManager = mock(ConfigManager.class);
        when(gameManager.getConfigManager()).thenReturn(configManager);

        executor = new MatchboxCommand(plugin, sessionManager, gameManager, nickManager);

        player = mock(Player.class);
        command = mock(Command.class);
        playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Bob");

        context = mock(SessionGameContext.class);
        gameState = mock(GameState.class);
        when(context.getGameState()).thenReturn(gameState);
        when(gameManager.getContextForPlayer(playerId)).thenReturn(context);
    }

    @Test
    @DisplayName("Non-admin cannot set a nick during an active game")
    void nonAdminBlockedDuringActiveGame() {
        when(player.hasPermission("matchbox.admin")).thenReturn(false);
        when(gameState.isGameActive()).thenReturn(true);

        executor.onCommand(player, command, "mb", new String[]{"nick", "Phantom"});

        verify(player).sendMessage(contains("cannot change your nick during an active game"));
        verify(nickManager, never()).setNick(any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("Admin can set a nick during an active game")
    void adminBypassesActiveGameRestriction() {
        when(player.hasPermission("matchbox.admin")).thenReturn(true);
        when(gameState.isGameActive()).thenReturn(true);
        when(nickManager.setNick(eq(playerId), eq("Phantom"), eq(true)))
            .thenReturn(NickManager.NickResult.SUCCESS);

        executor.onCommand(player, command, "mb", new String[]{"nick", "Phantom"});

        verify(nickManager).setNick(eq(playerId), eq("Phantom"), eq(true));
    }

    @Test
    @DisplayName("Non-admin can set a nick when not in an active game")
    void nonAdminAllowedOutsideActiveGame() {
        when(player.hasPermission("matchbox.admin")).thenReturn(false);
        when(gameManager.getContextForPlayer(playerId)).thenReturn(null);
        when(nickManager.setNick(eq(playerId), eq("Phantom"), eq(false)))
            .thenReturn(NickManager.NickResult.SUCCESS);

        executor.onCommand(player, command, "mb", new String[]{"nick", "Phantom"});

        verify(nickManager).setNick(eq(playerId), eq("Phantom"), eq(false));
    }
}
