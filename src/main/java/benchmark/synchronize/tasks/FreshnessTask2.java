package benchmark.synchronize.tasks;

import java.sql.Timestamp;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.math3.util.Pair;

import static benchmark.oltp.OLTPClient.*;
import benchmark.synchronize.components.HTAPCheckInfo;

public class FreshnessTask2 extends Task {
    private ExecutorService executor;
    private Timestamp currentTime;
    private static AtomicLong maxFreshness = new AtomicLong(0);
    private static AtomicLong maxSyncTime = new AtomicLong(0);
    private FreshnessExecutor freshnessExecutor;
    private long freshnessTimeBound;


    public FreshnessTask2(int dbType, HTAPCheckInfo htapCheckInfo, Timestamp currentTime) {
        this.executor = Executors.newFixedThreadPool(120);
        this.currentTime = currentTime;
        this.freshnessTimeBound = htapCheckInfo.htapCheckFreshnessDataBound;
    }

    @Override
    public TaskResult runTask(ArrayList<Connection> conns, int threadId) {
        int tryNum = 0;
        boolean pass = true;
        boolean isApConnErr = false;
        Connection apConnection = conns.get(0);
        System.out.printf("Remain #%d Real-time query\n", htapCheckQueryNumber.decrementAndGet());
        long startTime = System.currentTimeMillis(), endTime = 0;
        long hatFreshness = 0, vodkaFreshness = 0;
        Timestamp lTimeBound = new Timestamp(currentTime.getTime() - freshnessTimeBound * 1000);
        this.freshnessExecutor = new FreshnessExecutor(apConnection, lTimeBound, currentTime);
        try {
            List<Future<?>> futures = new ArrayList<>();
            // Future<Long> hatFreshnessFuture = executor.submit(() -> {
            //     return haTtrickFreshness.computeFreshness(currentTime.getTime());
            // });
            Future<Long> vodkaFuture = executor.submit(() -> {
                return freshnessExecutor.computeFreshness();
            });
            for (Future<?> f : futures) {
                f.get();
            }

            // Get the result of the HATtrickFreshness task
            // hatFreshness = hatFreshnessFuture.get();
            vodkaFreshness = vodkaFuture.get();

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
                hatFreshness, vodkaFreshness,
                0, 0, 0);
    }

    public static void updateMaxValues(long syncTime, long freshness) {
        maxSyncTime.updateAndGet(x -> Math.max(x, syncTime));
        maxFreshness.updateAndGet(x -> Math.max(x, freshness));
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
}