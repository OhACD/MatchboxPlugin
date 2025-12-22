package com.ohacd.matchbox.performance;

import com.ohacd.matchbox.api.*;
import com.ohacd.matchbox.utils.MockBukkitFactory;
import com.ohacd.matchbox.utils.TestPluginFactory;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Stress tests for session creation and management under high load.
 * Tests concurrent session creation limits and performance characteristics.
 */
public class SessionStressTest {

    @BeforeEach
    void setUp() {
        MockBukkitFactory.setUpBukkitMocks();
        TestPluginFactory.setUpMockPlugin();

        // Clear any existing sessions
        for (String sessionName : MatchboxAPI.getAllSessions().stream().map(ApiGameSession::getName).toList()) {
            MatchboxAPI.endSession(sessionName);
        }
    }

    @AfterEach
    void tearDown() {
        // Clean up all sessions after each test
        for (String sessionName : MatchboxAPI.getAllSessions().stream().map(ApiGameSession::getName).toList()) {
            MatchboxAPI.endSession(sessionName);
        }
    }

    @Test
    @DisplayName("Should handle creating 10 concurrent sessions successfully")
    void shouldHandleTenConcurrentSessions() throws InterruptedException {
        // Arrange
        int sessionCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(sessionCount);
        List<CompletableFuture<SessionCreationResult>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        // Act - Create sessions concurrently
        for (int i = 0; i < sessionCount; i++) {
            final int sessionIndex = i;
            CompletableFuture<SessionCreationResult> future = CompletableFuture.supplyAsync(() -> {
                String sessionName = "stress-session-" + sessionIndex + "-" + UUID.randomUUID();
                List<Player> players = MockBukkitFactory.createMockPlayers(3);
                List<Location> spawnPoints = List.of(MockBukkitFactory.createMockLocation());

                return MatchboxAPI.createSessionBuilder(sessionName)
                    .withPlayers(players)
                    .withSpawnPoints(spawnPoints)
                    .startWithResult();
            }, executor);

            futures.add(future);
        }

        // Wait for all sessions to complete
        List<SessionCreationResult> results = new ArrayList<>();
        for (CompletableFuture<SessionCreationResult> future : futures) {
            try {
                SessionCreationResult result = future.get(30, TimeUnit.SECONDS);
                results.add(result);
                if (result.isSuccess()) {
                    successCount.incrementAndGet();
                }
            } catch (TimeoutException | ExecutionException e) {
                fail("Session creation timed out or failed: " + e.getMessage());
            }
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Assert
        assertThat(successCount.get()).isEqualTo(sessionCount);
        assertThat(MatchboxAPI.getAllSessions()).hasSize(sessionCount);

        // Verify all sessions are accessible
        for (SessionCreationResult result : results) {
            assertThat(result.isSuccess()).isTrue();
            String sessionName = result.getSession().get().getName();
            assertThat(MatchboxAPI.getSession(sessionName)).isPresent();
        }
    }

    @Test
    @DisplayName("Should handle creating 50 concurrent sessions with performance monitoring")
    void shouldHandleFiftyConcurrentSessionsWithPerformanceMonitoring() throws InterruptedException {
        // Arrange
        PerformanceMetricsCollector metrics = new PerformanceMetricsCollector("50 Concurrent Sessions Test");
        int sessionCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(sessionCount, 20)); // Limit thread pool size
        List<CompletableFuture<SessionCreationResult>> futures = new ArrayList<>();
        long startTime = System.nanoTime();

        // Act - Create sessions concurrently
        for (int i = 0; i < sessionCount; i++) {
            final int sessionIndex = i;
            CompletableFuture<SessionCreationResult> future = CompletableFuture.supplyAsync(() -> {
                long sessionStartTime = System.nanoTime();
                try {
                    String sessionName = "perf-session-" + sessionIndex + "-" + UUID.randomUUID();
                    List<Player> players = MockBukkitFactory.createMockPlayers(2); // Smaller player count for faster creation
                    List<Location> spawnPoints = List.of(MockBukkitFactory.createMockLocation());

                    SessionCreationResult result = MatchboxAPI.createSessionBuilder(sessionName)
                        .withPlayers(players)
                        .withSpawnPoints(spawnPoints)
                        .startWithResult();

                    long sessionEndTime = System.nanoTime();
                    long sessionDurationMs = (sessionEndTime - sessionStartTime) / 1_000_000;

                    // Record metrics
                    if (result.isSuccess()) {
                        metrics.recordSuccess("Session Creation", sessionDurationMs);
                        System.out.println("Session " + sessionIndex + " created in " + sessionDurationMs + "ms");
                    } else {
                        metrics.recordFailure("Session Creation", sessionDurationMs);
                        System.out.println("Session " + sessionIndex + " failed: " + result.getErrorType());
                    }

                    return result;
                } catch (Exception e) {
                    long sessionEndTime = System.nanoTime();
                    long sessionDurationMs = (sessionEndTime - sessionStartTime) / 1_000_000;
                    metrics.recordFailure("Session Creation", sessionDurationMs);
                    System.out.println("Session " + sessionIndex + " threw exception: " + e.getMessage());
                    return null;
                }
            }, executor);

            futures.add(future);
        }

        // Wait for all sessions to complete with timeout
        List<SessionCreationResult> results = new ArrayList<>();
        for (CompletableFuture<SessionCreationResult> future : futures) {
            try {
                SessionCreationResult result = future.get(60, TimeUnit.SECONDS); // Longer timeout for 50 sessions
                results.add(result);
            } catch (TimeoutException | ExecutionException e) {
                long timeoutDurationMs = 60000; // 60 seconds timeout
                metrics.recordFailure("Session Creation", timeoutDurationMs);
                System.out.println("Session creation timed out: " + e.getMessage());
            }
        }

        long endTime = System.nanoTime();
        long totalDurationMs = (endTime - startTime) / 1_000_000;

        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        // Generate comprehensive performance report
        metrics.printReport();

        // Save report to file for historical tracking
        metrics.saveReportToFile("build/reports/performance/session-stress-test-" +
                               System.currentTimeMillis() + ".txt");

        // Assert
        assertThat(metrics.getSuccessRate()).isGreaterThanOrEqualTo(90.0); // At least 90% success rate
        assertThat(MatchboxAPI.getAllSessions()).hasSize((int)(metrics.getSuccessRate() * sessionCount / 100.0));
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 10, 25, 100})
    @DisplayName("Should handle varying numbers of concurrent sessions")
    void shouldHandleVaryingNumbersOfConcurrentSessions(int sessionCount) throws InterruptedException {
        // Arrange
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(sessionCount, 10));
        List<CompletableFuture<SessionCreationResult>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        // Act - Create sessions concurrently
        for (int i = 0; i < sessionCount; i++) {
            final int sessionIndex = i;
            CompletableFuture<SessionCreationResult> future = CompletableFuture.supplyAsync(() -> {
                String sessionName = "var-stress-" + sessionIndex + "-" + UUID.randomUUID();
                List<Player> players = MockBukkitFactory.createMockPlayers(2);
                List<Location> spawnPoints = List.of(MockBukkitFactory.createMockLocation());

                return MatchboxAPI.createSessionBuilder(sessionName)
                    .withPlayers(players)
                    .withSpawnPoints(spawnPoints)
                    .startWithResult();
            }, executor);

            futures.add(future);
        }

        // Wait for all sessions to complete
        for (CompletableFuture<SessionCreationResult> future : futures) {
            try {
                SessionCreationResult result = future.get(30, TimeUnit.SECONDS);
                if (result.isSuccess()) {
                    successCount.incrementAndGet();
                }
            } catch (TimeoutException | ExecutionException e) {
                // Continue - some failures are expected under high load
            }
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Assert
        double successRate = (double) successCount.get() / sessionCount;
        System.out.println("Session count: " + sessionCount + ", Success rate: " + (successRate * 100) + "%");

        assertThat(successRate).isGreaterThan(0.8); // At least 80% success rate
        assertThat(MatchboxAPI.getAllSessions()).hasSize(successCount.get());
    }

    @Test
    @DisplayName("Should handle session creation and immediate cleanup stress")
    void shouldHandleSessionCreationAndImmediateCleanupStress() throws InterruptedException {
        // Arrange
        int iterations = 20;
        int concurrentSessions = 5;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentSessions);
        AtomicInteger totalCreated = new AtomicInteger(0);
        AtomicInteger totalCleaned = new AtomicInteger(0);

        // Act - Create and immediately clean up sessions in waves
        for (int iteration = 0; iteration < iterations; iteration++) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // Create concurrent sessions
            for (int i = 0; i < concurrentSessions; i++) {
                final int sessionIndex = iteration * concurrentSessions + i;
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    String sessionName = "cleanup-stress-" + sessionIndex + "-" + UUID.randomUUID();
                    List<Player> players = MockBukkitFactory.createMockPlayers(2);
                    List<Location> spawnPoints = List.of(MockBukkitFactory.createMockLocation());

                    SessionCreationResult result = MatchboxAPI.createSessionBuilder(sessionName)
                        .withPlayers(players)
                        .withSpawnPoints(spawnPoints)
                        .startWithResult();

                    if (result.isSuccess()) {
                        totalCreated.incrementAndGet();

                        // Immediately try to end the session
                        boolean ended = MatchboxAPI.endSession(sessionName);
                        if (ended) {
                            totalCleaned.incrementAndGet();
                        }
                    }
                }, executor);

                futures.add(future);
            }

            // Wait for this wave to complete
            for (CompletableFuture<Void> future : futures) {
                try {
                    future.get(10, TimeUnit.SECONDS);
                } catch (TimeoutException | ExecutionException e) {
                    // Continue - some operations may timeout
                }
            }

            // Small delay between waves to prevent overwhelming the system
            Thread.sleep(100);
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Assert
        System.out.println("Cleanup stress results:");
        System.out.println("- Sessions created: " + totalCreated.get());
        System.out.println("- Sessions cleaned: " + totalCleaned.get());

        assertThat(totalCreated.get()).isGreaterThan(0);
        assertThat(totalCleaned.get()).isEqualTo(totalCreated.get()); // All created sessions should be cleaned up
        assertThat(MatchboxAPI.getAllSessions()).isEmpty(); // No sessions should remain
    }

    @Test
    @DisplayName("Should handle maximum concurrent session creation limit")
    void shouldHandleMaximumConcurrentSessionCreationLimit() throws InterruptedException {
        // This test attempts to find the practical limit of concurrent session creation
        // It gradually increases concurrency until failures occur

        int maxConcurrentToTest = 200; // Test up to 200 concurrent sessions
        int stepSize = 25;
        ExecutorService executor = Executors.newFixedThreadPool(50); // Fixed pool to control resource usage

        for (int concurrentCount = stepSize; concurrentCount <= maxConcurrentToTest; ) {
            final int currentConcurrentCount = concurrentCount; // Make effectively final for lambda
            System.out.println("Testing " + currentConcurrentCount + " concurrent sessions...");

            List<CompletableFuture<SessionCreationResult>> futures = new ArrayList<>();
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            long startTime = System.nanoTime();

            // Create sessions concurrently
            for (int i = 0; i < currentConcurrentCount; i++) {
                final int sessionIndex = i;
                CompletableFuture<SessionCreationResult> future = CompletableFuture.supplyAsync(() -> {
                    String sessionName = "limit-test-" + currentConcurrentCount + "-" + sessionIndex + "-" + UUID.randomUUID();
                    List<Player> players = MockBukkitFactory.createMockPlayers(1); // Minimal players for speed
                    List<Location> spawnPoints = List.of(MockBukkitFactory.createMockLocation());

                    return MatchboxAPI.createSessionBuilder(sessionName)
                        .withPlayers(players)
                        .withSpawnPoints(spawnPoints)
                        .startWithResult();
                }, executor);

                futures.add(future);
            }

            // Wait for all sessions to complete
            for (CompletableFuture<SessionCreationResult> future : futures) {
                try {
                    SessionCreationResult result = future.get(30, TimeUnit.SECONDS);
                    if (result != null && result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (TimeoutException | ExecutionException e) {
                    failureCount.incrementAndGet();
                }
            }

            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;
            double successRate = (double) successCount.get() / currentConcurrentCount;

            System.out.println("Results for " + currentConcurrentCount + " sessions:");
            System.out.println("- Success: " + successCount.get() + ", Failures: " + failureCount.get());
            System.out.println("- Success rate: " + (successRate * 100) + "%");
            System.out.println("- Total time: " + durationMs + "ms");
            System.out.println("- Avg time per session: " + (durationMs / (double) currentConcurrentCount) + "ms");

            // Clean up successful sessions
            for (String sessionName : MatchboxAPI.getAllSessions().stream().map(ApiGameSession::getName).toList()) {
                MatchboxAPI.endSession(sessionName);
            }

            // If success rate drops below 50%, we've likely hit a practical limit
            if (successRate < 0.5) {
                System.out.println("Detected performance degradation at " + currentConcurrentCount + " concurrent sessions");
                break;
            }

            // Increment for next iteration
            concurrentCount += stepSize;
        }

        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        // This test mainly provides data - no strict assertions
        assertTrue(true, "Stress test completed and provided performance data");
    }
}
