package benchmark.synchronize.tasks;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CollectMetrics {
    private AtomicLong totalFreshness = new AtomicLong();
    private AtomicLong totalSyncTime = new AtomicLong();
    private AtomicLong totalExecutionTime = new AtomicLong();
    private AtomicInteger failCount = new AtomicInteger();
    private AtomicInteger taskCount = new AtomicInteger();
    private static final int MAX_TASKS = 20; // 最大任务次数
    private double freshnessThreshold;

    public CollectMetrics(double freshnessThreshold) {
        this.freshnessThreshold = freshnessThreshold;
    }

    public synchronized void addMetrics(long freshness, long syncTime, long executionTime) {
        totalFreshness.addAndGet(freshness);
        totalSyncTime.addAndGet(syncTime);
        totalExecutionTime.addAndGet(executionTime);
        taskCount.incrementAndGet();
        
        if (freshness > freshnessThreshold) {
            failCount.incrementAndGet();
        }
    }

    public boolean shouldStop() {
        return taskCount.get() >= MAX_TASKS;
    }

    public void printMetrics() {
        System.out.println("Total Freshness: " + totalFreshness.get());
        System.out.println("Total Sync Time: " + totalSyncTime.get());
        System.out.println("Total Execution Time: " + totalExecutionTime.get());
        System.out.println("Total Combined Time: " + (totalSyncTime.get() + totalExecutionTime.get()));
        double errorRate = (double) failCount.get() / taskCount.get();
        System.out.println("Error Rate: " + errorRate);
    }

    public static void main(String[] args) {
        // Example of setting up and using CollectMetrics
        CollectMetrics collector = new CollectMetrics(50); // Suppose the freshness threshold is 50
        // Simulation of multiple tasks
        for (int i = 0; i < 25; i++) {
            if (collector.shouldStop()) {
                break;
            }
            // Simulation: Add random metrics
            collector.addMetrics((long) (Math.random() * 100), (long) (Math.random() * 1000), (long) (Math.random() * 500));
        }
        collector.printMetrics();
    }
}