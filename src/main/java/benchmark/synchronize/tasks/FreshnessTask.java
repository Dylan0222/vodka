package benchmark.synchronize.tasks;

import java.sql.Timestamp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.math3.util.Pair;

import bean.Triple;

import static benchmark.oltp.OLTPClient.*;
import benchmark.oltp.DeliveryTracker;
import benchmark.oltp.OLTPClient;
import benchmark.oltp.entity.OLTPData;
import benchmark.synchronize.components.HTAPCheckInfo;
import benchmark.synchronize.related_work.HATtrickFreshness;

public class FreshnessTask extends Task {
    private ExecutorService executor;
    private int partitionCount;
    private double partitionRatio;
    private long freshnessTimeBound;
    private Timestamp currentTime;
    private static AtomicLong maxFreshness = new AtomicLong(0);
    private static AtomicLong maxSyncTime = new AtomicLong(0);
    private long execution_time;
    private DeliveryTracker deliveryTracker;
    private int dbType;
    private static ArrayList<Integer> validWIds = new ArrayList<>();
    private int lFreshLagBound;
    private int rFreshLagBound;
    private int checkThreshold = 0;
    private boolean isWeakRead;
    private int weakReadTime;

    public FreshnessTask(int dbType, HTAPCheckInfo htapCheckInfo, Timestamp currentTime) {
        this.dbType = dbType;
        this.executor = Executors.newFixedThreadPool(120);
        this.partitionCount = htapCheckInfo.warehouseNum;
        this.partitionRatio = (double) htapCheckInfo.htapCheckCrossQuantity / 100;
        this.freshnessTimeBound = htapCheckInfo.htapCheckFreshnessDataBound;
        this.lFreshLagBound = htapCheckInfo.lFreshLagBound;
        this.rFreshLagBound = htapCheckInfo.rFreshLagBound;
        this.deliveryTracker = OLTPData.taskTrack;
        this.currentTime = currentTime;
        this.checkThreshold = 100;
        this.isWeakRead = htapCheckInfo.isWeakRead;
        this.weakReadTime = htapCheckInfo.weakReadTime;
    }

    @Override
    public TaskResult runTask(ArrayList<Connection> conns, int threadId) {
        int tryNum = 0;
        boolean pass = true;
        boolean isApConnErr = false;
        Connection apConnection = conns.get(0);
        Connection tpConnection = conns.size() > 1 ? conns.get(1) : conns.get(0);

        Timestamp lTimeBound = new Timestamp(currentTime.getTime() - freshnessTimeBound * 1000);
        System.out.printf("Remain #%d Real-time query\n", htapCheckQueryNumber.decrementAndGet());
        long startTime = System.currentTimeMillis(), endTime = 0;
        long hatFreshness = 0, vodkaFreshness = 0;
        try {
            AtomicInteger accessedCount = new AtomicInteger(1);
            LinkedHashMap<Integer, DeliveryTracker.Transaction> transactions = deliveryTracker.getTransactionMap();
            List<Future<?>> futures = new ArrayList<>();

            // Submit a task for HATtrickFreshness computeFreshness
            Future<Long> hatFreshnessFuture = executor.submit(() -> {
                return haTtrickFreshness.computeFreshness(currentTime.getTime());
            });

            for (DeliveryTracker.Transaction transaction : transactions.values()) {
                boolean condition1 = transaction.current_ts.getTime() < lTimeBound.getTime();
                boolean condition2 = accessedCount.get() > (int) (partitionCount * partitionRatio);
                if (condition1 || condition2) {
                    System.out.println("condition1: " + condition1 + ", condition2: " + condition2 + "break out!");
                    break;
                }
                validWIds.add(transaction.w_id);
                System.out.println("Executing for Task: " + accessedCount.get());
                futures.add(executor.submit(() -> {
                    try {
                        new VersionCheckTask(transaction, tpConnection, apConnection, freshnessTimeBound,
                                currentTime, accessedCount.get(), dbType, isWeakRead, weakReadTime)
                                .call();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        System.out.println("Task completed");
                    }
                }));
                accessedCount.incrementAndGet();
            }

            for (Future<?> f : futures) {
                f.get();
            }

            // Get the result of the HATtrickFreshness task
            hatFreshness = hatFreshnessFuture.get();
            vodkaFreshness = maxFreshness.get();

            executor.shutdown(); // 关闭执行器

            maxFreshness.set(0);
            maxSyncTime.set(0);

            // 新鲜度结果存储或处理
            System.out.println("Vodka Freshness: " + vodkaFreshness);
            System.out.println("HATtrick Freshness: " + hatFreshness);
            freshnessHistory.add(new Pair<Long, Long>(hatFreshness, vodkaFreshness));
        } catch (Exception e) {
            e.printStackTrace();
            pass = false;
        }
        if (htapCheckQueryNumber.get() <= 0)
            printFreshnessHistory();
        return new TaskResult(taskType, txnCompleteTime, gapTime, startTime, endTime, tryNum, pass, isApConnErr,
                hatFreshness, 0,
                0, 0, 0);
    }

    private void printFreshnessHistory() {
        System.out.println("Complete Freshness History, Deviations, and Deviation Ratios:");
        for (Pair<Long, Long> results : freshnessHistory) {
            long hatFreshness = results.getFirst();
            long vodkaFreshness = results.getSecond();
            long deviation = Math.abs(hatFreshness - vodkaFreshness);
            double deviationRatio = vodkaFreshness != 0 ? (double) deviation / vodkaFreshness : 0; // 防止除以零

            System.out.println("HATtrick Freshness: " + hatFreshness + "ms" +
                    ", Vodka Freshness: " + vodkaFreshness + "ms" +
                    ", Deviation: " + deviation +
                    ", Deviation Ratio: " + String.format("%.2f", deviationRatio));
        }
        signalTerminalsRequestEnd(false);
    }

    private void printFreshnessReport() {
        int failNumber = 0;
        double totalUnifiedLatency = 0.0;
        double totalFreshness = 0.0;
        double totalSyncLatency = 0.0;
        double totalExecLatency = 0.0;
        double maxSyncLatency = Double.MIN_VALUE;
        double minFreshness = Double.MAX_VALUE;
        double maxFreshness = Double.MIN_VALUE;

        System.out.println("Freshness | Synchronization Latency | Execution Latency | Unified Latency");
        // freshnessTime.remove(0);

        for (Triple<Long, Long, Long> item : freshnessTime) {
            long freshness = item.getFirst();
            long sync_latency = item.getSecond();
            double exec_latency = item.getThird();
            double unifiedLatency = sync_latency + exec_latency;
            System.out.println(freshness + ", " + sync_latency + ", " + exec_latency + ", " + unifiedLatency);
            totalUnifiedLatency += unifiedLatency;
            totalFreshness += freshness;
            totalSyncLatency += sync_latency;
            totalExecLatency += exec_latency;
            minFreshness = Math.min(minFreshness, freshness);
            maxFreshness = Math.max(maxFreshness, freshness);
            if (sync_latency > maxSyncLatency) {
                maxSyncLatency = sync_latency;
            }

            if (freshness > rFreshLagBound)
                failNumber++;
        }

        double averageUnifiedLatency = totalUnifiedLatency / freshnessTime.size();
        double averageFreshness = totalFreshness / freshnessTime.size();
        double averageSyncLatency = totalSyncLatency / freshnessTime.size();
        double averageExecLatency = totalExecLatency / freshnessTime.size();
        double failRate = (double) failNumber / freshnessTime.size();

        System.out.printf("Freshness Fail Rate is: %.2f%%\n", failRate * 100);
        System.out.printf("Min Freshness is: %.2f\n ms", minFreshness);
        System.out.printf("Max Freshness is: %.2f\n ms", maxFreshness);
        System.out.printf("Average Freshness is: %.2f ms\n", averageFreshness);
        System.out.printf("Average Synchronizatino Latency is: %.2f ms\n", averageSyncLatency);
        System.out.printf("Average Execution Latency is: %.2f ms\n", averageExecLatency);
        System.out.printf("Average Unified Latency is: %.2f ms\n", averageUnifiedLatency);
        // After the loop, print out the maximum synchronization latency found
        System.out.println("Maximum Synchronization Latency: " + maxSyncLatency);
        signalTerminalsRequestEnd(false);
    }

}