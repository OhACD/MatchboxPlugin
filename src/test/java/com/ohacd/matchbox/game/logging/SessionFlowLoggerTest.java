package com.ohacd.matchbox.game.logging;

import com.ohacd.matchbox.api.GameSessionLog;
import com.ohacd.matchbox.api.GameStatistics;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionFlowLoggerTest {

    private SessionFlowLogger logger;

    @BeforeEach
    void setUp() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        logger = new SessionFlowLogger(plugin);
    }

    @Test
    @DisplayName("Should record chat and sign events into session timeline")
    void shouldRecordChatAndSignEventsIntoSessionTimeline() {
        String session = "logger-session";
        UUID playerId = UUID.randomUUID();

        logger.recordChat(session, playerId, "PlayerA", "GAME", "hello world");
        logger.recordSignMessage(session, playerId, "PlayerA", "line one | line two");

        GameSessionLog snapshot = logger.getSessionLog(session);
        assertThat(snapshot.size()).isEqualTo(2);
        assertThat(snapshot.getEntries().get(0).category()).isEqualTo("CHAT");
        assertThat(snapshot.getEntries().get(1).category()).isEqualTo("SIGN");
    }

    @Test
    @DisplayName("Should aggregate vote swipe cure and elimination stats")
    void shouldAggregateVoteSwipeCureAndEliminationStats() {
        String session = "stats-session";
        UUID spark = UUID.randomUUID();
        UUID medic = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        logger.incrementRound(session);
        logger.recordVote(session, spark, target, "Spark", "Target");
        logger.recordSwipe(session, spark, target, "Spark", "Target");
        logger.recordCure(session, medic, target, "Medic", "Target");
        logger.recordElimination(session, target, "Target");

        GameStatistics stats = logger.getSessionStatistics(session);
        assertThat(stats.getRoundsPlayed()).isEqualTo(1);
        assertThat(stats.getStats(spark).votesCast()).isEqualTo(1);
        assertThat(stats.getStats(spark).swipes()).isEqualTo(1);
        assertThat(stats.getStats(medic).cures()).isEqualTo(1);
        assertThat(stats.getStats(target).votesReceived()).isEqualTo(1);
        assertThat(stats.getStats(target).eliminations()).isEqualTo(1);
    }
}
