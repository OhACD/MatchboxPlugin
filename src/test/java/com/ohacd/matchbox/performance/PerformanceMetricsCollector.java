package com.ohacd.matchbox.performance;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance metrics collector for session operations.
 * Captures and analyzes performance data from stress tests.
 */
public class PerformanceMetricsCollector {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConcurrentHashMap<String, AtomicLong> operationCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> totalTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> minTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> maxTimes = new ConcurrentHashMap<>();

    private final AtomicInteger activeOperations = new AtomicInteger(0);
    private final AtomicInteger totalOperations = new AtomicInteger(0);
    private final AtomicInteger failedOperations = new AtomicInteger(0);

    private final String testName;
    private final LocalDateTime testStart;

    public PerformanceMetricsCollector(String testName) {
        this.testName = testName;
        this.testStart = LocalDateTime.now();
    }

    /**
     * Records the timing of an operation.
     */
    public void recordOperation(String operationName, long durationMs, boolean success) {
        // Update counters
        activeOperations.incrementAndGet();
        totalOperations.incrementAndGet();
        if (!success) {
            failedOperations.incrementAndGet();
        }

        // Update timing metrics
        operationCounts.computeIfAbsent(operationName, k -> new AtomicLong(0)).incrementAndGet();
        totalTimes.computeIfAbsent(operationName, k -> new AtomicLong(0)).addAndGet(durationMs);

        // Update min/max times
        minTimes.compute(operationName, (k, v) -> {
            if (v == null) return new AtomicLong(durationMs);
            return new AtomicLong(Math.min(v.get(), durationMs));
        });

        maxTimes.compute(operationName, (k, v) -> {
            if (v == null) return new AtomicLong(durationMs);
            return new AtomicLong(Math.max(v.get(), durationMs));
        });
    }

    /**
     * Records a successful operation.
     */
    public void recordSuccess(String operationName, long durationMs) {
        recordOperation(operationName, durationMs, true);
    }

    /**
     * Records a failed operation.
     */
    public void recordFailure(String operationName, long durationMs) {
        recordOperation(operationName, durationMs, false);
    }

    /**
     * Gets the average time for an operation.
     */
    public double getAverageTime(String operationName) {
        AtomicLong count = operationCounts.get(operationName);
        AtomicLong total = totalTimes.get(operationName);

        if (count == null || total == null || count.get() == 0) {
            return 0.0;
        }

        return (double) total.get() / count.get();
    }

    /**
     * Gets the success rate as a percentage.
     */
    public double getSuccessRate() {
        int total = totalOperations.get();
        if (total == 0) return 100.0;

        int successful = total - failedOperations.get();
        return (double) successful / total * 100.0;
    }

    /**
     * Prints a comprehensive performance report.
     */
    public void printReport() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 PERFORMANCE REPORT: " + testName);
        System.out.println("⏰ Test Start: " + testStart.format(TIMESTAMP_FORMAT));
        System.out.println("⏱️  Test End: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        System.out.println("=".repeat(80));

        // Overall statistics
        System.out.println("📈 OVERALL STATISTICS:");
        System.out.printf("   Total Operations: %d%n", totalOperations.get());
        System.out.printf("   Successful Operations: %d%n", totalOperations.get() - failedOperations.get());
        System.out.printf("   Failed Operations: %d%n", failedOperations.get());
        System.out.printf("   Success Rate: %.2f%%%n", getSuccessRate());
        System.out.printf("   Active Operations: %d%n", activeOperations.get());

        // Per-operation statistics
        System.out.println("\n📋 PER-OPERATION STATISTICS:");
        for (String operationName : operationCounts.keySet()) {
            long count = operationCounts.get(operationName).get();
            double avgTime = getAverageTime(operationName);
            long minTime = minTimes.get(operationName).get();
            long maxTime = maxTimes.get(operationName).get();

            System.out.printf("   %s:%n", operationName);
            System.out.printf("     Count: %d%n", count);
            System.out.printf("     Average Time: %.2f ms%n", avgTime);
            System.out.printf("     Min Time: %d ms%n", minTime);
            System.out.printf("     Max Time: %d ms%n", maxTime);
            System.out.printf("     Throughput: %.2f ops/sec%n", count / Math.max(1, (System.currentTimeMillis() - testStart.toInstant(java.time.ZoneOffset.UTC).toEpochMilli()) / 1000.0));
        }

        // Performance analysis
        System.out.println("\n🎯 PERFORMANCE ANALYSIS:");
        double overallSuccessRate = getSuccessRate();
        if (overallSuccessRate >= 99.0) {
            System.out.println("   ✅ Excellent: >99% success rate");
        } else if (overallSuccessRate >= 95.0) {
            System.out.println("   ⚠️  Good: 95-99% success rate");
        } else if (overallSuccessRate >= 90.0) {
            System.out.println("   ⚠️  Acceptable: 90-95% success rate");
        } else {
            System.out.println("   ❌ Poor: <90% success rate - investigate performance issues");
        }

        // Check for performance bottlenecks
        for (String operationName : operationCounts.keySet()) {
            double avgTime = getAverageTime(operationName);
            if (avgTime > 1000) { // More than 1 second
                System.out.printf("   🚨 WARNING: %s is slow (%.2f ms average)%n", operationName, avgTime);
            } else if (avgTime > 500) { // More than 500ms
                System.out.printf("   ⚠️  SLOW: %s is above acceptable latency (%.2f ms average)%n", operationName, avgTime);
            }
        }

        System.out.println("=".repeat(80) + "\n");
    }

    /**
     * Saves the report to a file.
     */
    public void saveReportToFile(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("PERFORMANCE REPORT: " + testName);
            writer.println("Test Start: " + testStart.format(TIMESTAMP_FORMAT));
            writer.println("Test End: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
            writer.println();

            writer.println("OVERALL STATISTICS:");
            writer.printf("Total Operations: %d%n", totalOperations.get());
            writer.printf("Successful Operations: %d%n", totalOperations.get() - failedOperations.get());
            writer.printf("Failed Operations: %d%n", failedOperations.get());
            writer.printf("Success Rate: %.2f%%%n", getSuccessRate());
            writer.println();

            writer.println("PER-OPERATION STATISTICS:");
            for (String operationName : operationCounts.keySet()) {
                long count = operationCounts.get(operationName).get();
                double avgTime = getAverageTime(operationName);
                long minTime = minTimes.get(operationName).get();
                long maxTime = maxTimes.get(operationName).get();

                writer.printf("%s:%n", operationName);
                writer.printf("  Count: %d%n", count);
                writer.printf("  Average Time: %.2f ms%n", avgTime);
                writer.printf("  Min Time: %d ms%n", minTime);
                writer.printf("  Max Time: %d ms%n", maxTime);
                writer.println();
            }

        } catch (IOException e) {
            System.err.println("Failed to save performance report: " + e.getMessage());
        }
    }

    /**
     * Resets all metrics.
     */
    public void reset() {
        operationCounts.clear();
        totalTimes.clear();
        minTimes.clear();
        maxTimes.clear();
        activeOperations.set(0);
        totalOperations.set(0);
        failedOperations.set(0);
    }
}
