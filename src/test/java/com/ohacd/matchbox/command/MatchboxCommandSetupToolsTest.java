package com.ohacd.matchbox.command;

import com.ohacd.matchbox.Matchbox;
import com.ohacd.matchbox.game.GameManager;
import com.ohacd.matchbox.game.config.ConfigManager;
import com.ohacd.matchbox.game.nick.NickManager;
import com.ohacd.matchbox.game.session.SessionManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchboxCommandSetupToolsTest {

    private MatchboxCommand commandExecutor;
    private ConfigManager configManager;
    private Player player;
    private Command command;
    private World world;

    @BeforeEach
    void setUp() {
        Matchbox plugin = mock(Matchbox.class);
        SessionManager sessionManager = mock(SessionManager.class);
        GameManager gameManager = mock(GameManager.class);
        NickManager nickManager = mock(NickManager.class);

        configManager = mock(ConfigManager.class);
        when(gameManager.getConfigManager()).thenReturn(configManager);

        commandExecutor = new MatchboxCommand(plugin, sessionManager, gameManager, nickManager);

        player = mock(Player.class);
        command = mock(Command.class);
        world = mock(World.class);

        when(player.hasPermission("matchbox.admin")).thenReturn(true);
        when(player.getWorld()).thenReturn(world);
        when(player.getName()).thenReturn("MapMaker");
        when(world.getName()).thenReturn("creator-world");

        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);

        when(configManager.getWorldMapConfigPath(world)).thenReturn("worlds/creator-world/matchbox-map.yml");
        when(configManager.loadSpawnLocations(world)).thenReturn(List.of(location));
        when(configManager.getSpawnLocationsDisplay(world)).thenReturn(List.of("§7Spawn #1"));
        when(configManager.getSeatLocationsDisplay(world)).thenReturn(List.of("§7Seat 1"));
        when(configManager.getDiscussionSeatSpawns(world)).thenReturn(List.of(1, 2, 3, 4, 5, 6, 7));
        when(configManager.getWorldMapMetadata(world)).thenReturn(java.util.Map.of(
                "id", "creator_map",
                "display-name", "Creator Map",
                "creator", "MapMaker",
                "schema-version", "1",
                "plugin-version", "0.9.7-test"
        ));
    }

    @Test
    @DisplayName("Should route setup init to map metadata initializer")
    void shouldRouteSetupInitToMetadataInitializer() {
        boolean handled = commandExecutor.onCommand(player, command, "mb", new String[]{"setup", "init", "creator_map", "Creator", "Map"});

        verify(configManager).initializeWorldMapMetadata(world, "creator_map", "Creator Map", "MapMaker");
        verify(configManager).getWorldMapConfigPath(world);
        org.assertj.core.api.Assertions.assertThat(handled).isTrue();
    }

    @Test
    @DisplayName("Should route setup seatspawns set to ConfigManager with parsed values")
    void shouldRouteSetupSeatSpawnsSetWithParsedValues() {
        boolean handled = commandExecutor.onCommand(player, command, "mb", new String[]{"setup", "seatspawns", "set", "1,2,3,7"});

        verify(configManager).setDiscussionSeatSpawns(world, List.of(1, 2, 3, 7));
        org.assertj.core.api.Assertions.assertThat(handled).isTrue();
    }

    @Test
    @DisplayName("Should route setup setseat through existing setseat flow")
    void shouldRouteSetupSetSeatThroughExistingFlow() {
        boolean handled = commandExecutor.onCommand(player, command, "mb", new String[]{"setup", "setseat", "4"});

        verify(configManager).saveSeatLocation(eq(4), any(Location.class));
        org.assertj.core.api.Assertions.assertThat(handled).isTrue();
    }

    @Test
    @DisplayName("Should route setup importlegacy overwrite to ConfigManager")
    void shouldRouteSetupImportLegacyOverwrite() {
        when(configManager.importLegacyGlobalConfigToWorld(world, true)).thenReturn(true);

        boolean handled = commandExecutor.onCommand(player, command, "mb", new String[]{"setup", "importlegacy", "overwrite"});

        verify(configManager).importLegacyGlobalConfigToWorld(world, true);
        org.assertj.core.api.Assertions.assertThat(handled).isTrue();
    }

    @Test
    @DisplayName("Should expose setup subcommands in tab completion")
    void shouldExposeSetupSubcommandsInTabCompletion() {
        List<String> suggestions = commandExecutor.onTabComplete(player, command, "mb", new String[]{"setup", "i"});

        org.assertj.core.api.Assertions.assertThat(suggestions).contains("init", "info", "importlegacy");
    }

    @Test
    @DisplayName("Should not expose removed legacy location commands in top-level tab completion")
    void shouldNotExposeRemovedLegacyLocationCommandsInTopLevelTabCompletion() {
        List<String> suggestions = commandExecutor.onTabComplete(player, command, "mb", new String[]{"s"});

        org.assertj.core.api.Assertions.assertThat(suggestions).contains("setup", "start", "stop", "skip");
        org.assertj.core.api.Assertions.assertThat(suggestions)
                .doesNotContain("setspawn", "setseat", "listspawns", "listseatspawns", "removespawn", "removeseat", "clearspawns", "clearseats");
    }

    @Test
    @DisplayName("Should treat removed legacy setspawn as unknown command")
    void shouldTreatRemovedLegacySetspawnAsUnknownCommand() {
        boolean handled = commandExecutor.onCommand(player, command, "mb", new String[]{"setspawn"});

        verify(configManager, never()).addSpawnLocation(any(Location.class));
        org.assertj.core.api.Assertions.assertThat(handled).isTrue();
    }

    @Test
    @DisplayName("Should deny setup command for non-player senders")
    void shouldDenySetupForNonPlayerSender() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("matchbox.admin")).thenReturn(true);

        boolean handled = commandExecutor.onCommand(sender, command, "mb", new String[]{"setup", "info"});

        verify(sender).sendMessage("§cSetup tools can only be used by players.");
        org.assertj.core.api.Assertions.assertThat(handled).isTrue();
    }

    @Test
    @DisplayName("Should deny setup command without admin permission")
    void shouldDenySetupWithoutPermission() {
        when(player.hasPermission("matchbox.admin")).thenReturn(false);

        boolean handled = commandExecutor.onCommand(player, command, "mb", new String[]{"setup", "info"});

        verify(player).sendMessage("§cYou don't have permission to use setup tools.");
        org.assertj.core.api.Assertions.assertThat(handled).isTrue();
    }

    @Test
    @DisplayName("Should route setup listspawns to world-scoped config display")
    void shouldRouteSetupListSpawnsToWorldScopedConfigDisplay() {
        boolean handled = commandExecutor.onCommand(player, command, "mb", new String[]{"setup", "listspawns"});

        verify(configManager).getSpawnLocationsDisplay(world);
        org.assertj.core.api.Assertions.assertThat(handled).isTrue();
    }

    @Test
    @DisplayName("Should route setup listseats to world-scoped config display")
    void shouldRouteSetupListSeatsToWorldScopedConfigDisplay() {
        boolean handled = commandExecutor.onCommand(player, command, "mb", new String[]{"setup", "listseats"});

        verify(configManager).getSeatLocationsDisplay(world);
        verify(configManager).getDiscussionSeatSpawns(world);
        org.assertj.core.api.Assertions.assertThat(handled).isTrue();
    }
}
