package benchmark.synchronize.tasks;

import java.sql.Timestamp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import bean.Triple;

import static benchmark.oltp.OLTPClient.*;
import benchmark.oltp.DeliveryTracker;
import benchmark.oltp.entity.OLTPData;
import benchmark.synchronize.components.HTAPCheckInfo;

public class SynchronizeTask extends Task {
    private ExecutorService executor;
    private int partitionCount;
    private double partitionRatio;
    private long freshnessTimeBound;
    private Timestamp currentTime;
    private static AtomicLong maxFreshness = new AtomicLong(0);
    private static AtomicLong maxSyncTime = new AtomicLong(0);
    private long execution_time;
    private long sync_time;
    private DeliveryTracker deliveryTracker;
    private int dbType;
    private static ArrayList<Integer> validWIds = new ArrayList<>();
    private int lFreshLagBound;
    private int rFreshLagBound;
    private int checkThreshold = 0;
    private boolean isWeakRead;
    private int weakReadTime;

    public SynchronizeTask(int dbType, HTAPCheckInfo htapCheckInfo, Timestamp currentTime) {
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
        Connection tpConnection;
        if (conns.size() > 1) {
            tpConnection = conns.get(1);
        } else {
            tpConnection = conns.get(0);
        }

        Timestamp lTimeBound = new Timestamp(currentTime.getTime() - freshnessTimeBound * 1000);
        System.out.printf("Remain #%d Real-time query\n", htapCheckQueryNumber.decrementAndGet());
        try {
            AtomicInteger accessedCount = new AtomicInteger(1);
            LinkedHashMap<Integer, DeliveryTracker.Transaction> transactions = deliveryTracker.getTransactionMap();
            List<Future<?>> futures = new ArrayList<>();
            // for (DeliveryTracker.Transaction transaction : transactions.values()) {
            // boolean condition1 = transaction.current_ts.getTime() < lTimeBound.getTime();
            // boolean condition2 = accessedCount.get() > (int) (partitionCount *
            // partitionRatio);
            // if (condition1 || condition2) {
            // System.out.println("condition1: " + condition1 + ", condition2: " +
            // condition2 + "break out!");
            // break;
            // }
            // validWIds.add(transaction.w_id);
            // // if (accessedCount.get() <= checkThreshold) {
            // System.out.println("Executing for Task: " + accessedCount.get());
            // // futures.add(executor.submit(() -> {
            // // try {
            // // new VersionCheckTask(transaction, tpConnection, apConnection,
            // freshnessTimeBound,
            // // currentTime, accessedCount.get(), dbType, isWeakRead, weakReadTime)
            // // .call();
            // // } catch (Exception e) {
            // // e.printStackTrace();
            // // } finally {
            // // System.out.println("Task completed");
            // // }
            // // }));
            // // }
            // accessedCount.incrementAndGet();
            // }

            // for (Future<?> f : futures) {
            // f.get();
            // }
            executor.shutdown(); // 关闭执行器
            String secondsql = constructSecondSQL();
            // 所有任务完成后执行第二个查询
            System.out.println("Executing for second query!");

            long startTime = System.currentTimeMillis();
            long endTime = 0;
            try (PreparedStatement stmt = apConnection.prepareStatement(secondsql)) {
                stmt.setTimestamp(1, lTimeBound);
                stmt.setTimestamp(2, currentTime);
                System.out.println(secondsql + "leftBound:" + lTimeBound + ", RightBound: " + currentTime);
                try (ResultSet rs = stmt.executeQuery()) {
                    endTime = System.currentTimeMillis();
                    execution_time = endTime - startTime;
                    if (rs.next()) {
                        System.out.println(rs.getString(1) + ", " + execution_time);
                    }
                }
            }
            sync_time = maxSyncTime.get();
            Triple<Long, Long, Long> freshnessItem = new Triple<>(maxFreshness.get(), sync_time,
                    execution_time);
            // System.out.println("current time is" + maxSyncTime.get());
            freshnessTime.add(freshnessItem);
            maxFreshness.set(0);
            maxSyncTime.set(0);
        } catch (Exception e) {
            e.printStackTrace();
            pass = false;
        }
        if (htapCheckQueryNumber.get() < 0)
            printFreshnessReport();
        return new TaskResult(taskType, txnCompleteTime, gapTime, startTime, endTime, tryNum, pass, isApConnErr, 0, 0,
                sync_time, execution_time, sync_time + execution_time);
    }

    private String constructSecondSQL() {
        Set<Integer> uniqueWIds = new LinkedHashSet<>(validWIds);
        StringBuilder inClause = new StringBuilder();
        for (Integer wId : uniqueWIds) {
            if (inClause.length() > 0)
                inClause.append(",");
            inClause.append(wId);
        }
        // return String.format(
        // "SELECT /*+ read_from_storage(tiflash[vodka_order_line]) */ ol_delivery_d,
        // ol_receipdate FROM vodka_order_line WHERE vodka_order_line.current_ts Between
        // ? AND ? and ol_w_id IN (%s)",
        // inClause);
        return String.format(
                "SELECT /*+ read_from_storage(tiflash[vodka_order_line]) */ count(*) FROM vodka_order_line WHERE vodka_order_line.current_ts Between ? AND ? and ol_w_id between 1 and 120",
                inClause);
    }

    public static void updateMaxValues(long syncTime) {
        maxSyncTime.updateAndGet(x -> Math.max(x, syncTime));
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
        freshnessTime.remove(0);

        for (Triple<Long, Long, Long> item : freshnessTime) {
            double freshness = item.getFirst();
            double sync_latency = item.getSecond();
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

        System.out.printf("Average Synchronizatino Latency is: %.2f ms\n", averageSyncLatency / 1_000_000);
        System.out.printf("Average Execution Latency is: %.2f ms\n", averageExecLatency);
        System.out.printf("Average Unified Latency is: %.2f ms\n", averageUnifiedLatency);
        // After the loop, print out the maximum synchronization latency found
        System.out.println("Maximum Synchronization Latency: " + maxSyncLatency);
        signalTerminalsRequestEnd(false);
    }

}