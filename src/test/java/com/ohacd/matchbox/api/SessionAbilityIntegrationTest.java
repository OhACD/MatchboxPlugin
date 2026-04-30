package com.ohacd.matchbox.api;

import com.ohacd.matchbox.Matchbox;
import com.ohacd.matchbox.utils.MockBukkitFactory;
import com.ohacd.matchbox.utils.TestPluginFactory;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class SessionAbilityIntegrationTest {

    @BeforeEach
    void setUp() {
        TestPluginFactory.setUpMockPlugin();
    }

    @AfterEach
    void tearDown() {
        TestPluginFactory.tearDownMockPlugin();
    }

    @Test
    @DisplayName("Should store session ability handlers registered through session builder")
    void shouldStoreSessionAbilityHandlersRegisteredThroughSessionBuilder() {
        Player player = MockBukkitFactory.createMockPlayer(UUID.randomUUID(), "AbilityPlayer");
        MockBukkitFactory.registerMockPlayer(player);
        List<Location> spawnPoints = List.of(MockBukkitFactory.createMockLocation(0, 64, 0, 0, 0));
        AtomicBoolean invoked = new AtomicBoolean(false);

        MatchboxAPI.createSessionBuilder("ability-session")
            .withPlayers(List.of(player))
            .withSpawnPoints(spawnPoints)
            .withAbilityHandlers(new SessionAbilityHandler() {
                @Override
                public void handleInventoryClick(InventoryClickEvent event, ApiGameSession session) {
                    invoked.set(true);
                }
            })
            .createSessionOnly();

        Matchbox plugin = Matchbox.getInstance();
        assertThat(plugin.getGameManager().getSessionAbilityHandlers("ability-session")).hasSize(1);
        assertThat(invoked.get()).isFalse();
    }
}
