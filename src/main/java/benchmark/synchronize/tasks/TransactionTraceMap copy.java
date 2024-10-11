// package benchmark.synchronize.tasks;

// import java.sql.Timestamp;
// import java.text.SimpleDateFormat;
// import java.util.*;
// import java.util.concurrent.*;

// import java.util.logging.Logger;

// import lombok.Getter;

// import java.util.logging.Level;

// class VersionRange {
//     int startVersion;
//     int endVersion;
//     Timestamp commitTime;

//     public VersionRange(int startVersion, int endVersion, Timestamp commitTime) {
//         this.startVersion = startVersion;
//         this.endVersion = endVersion;
//         this.commitTime = commitTime;
//     }
// }

// class ColumnVersion {
//     List<Integer> primaryKeys; // 支持复合主键
//     String columnName;
//     int version;

//     public ColumnVersion(List<Integer> primaryKeys, String columnName, int version) {
//         this.primaryKeys = primaryKeys;
//         this.columnName = columnName;
//         this.version = version;
//     }
// }

// class ColumnTrace {
//     Map<String, List<VersionRange>> columnVersions; // Map column names to their version ranges

//     public ColumnTrace() {
//         this.columnVersions = new HashMap<>();
//     }

//     public void addVersionRange(String columnName, int startVersion, int endVersion, Timestamp commitTime) {
//         this.columnVersions.computeIfAbsent(columnName, k -> new ArrayList<>())
//                 .add(new VersionRange(startVersion, endVersion, commitTime));
//     }
// }

// class TrxTrace {
//     Map<List<Integer>, ColumnTrace> primaryKeyToColumnsMap;

//     public TrxTrace() {
//         this.primaryKeyToColumnsMap = new ConcurrentHashMap<>();
//     }

//     public void updateColumn(List<Integer> primaryKeys, String columnName, int currentVersion, Timestamp commitTime) {
//         ColumnTrace columnTrace = primaryKeyToColumnsMap.computeIfAbsent(primaryKeys, k -> new ColumnTrace());
//         if (columnName.equalsIgnoreCase("ol_delivery_d")) {
//             columnTrace.addVersionRange("ol_delivery_d", 2, 2, commitTime);
//             columnTrace.addVersionRange("ol_receipdate", 2, 2, commitTime);
//         } else if (columnName.equalsIgnoreCase("ol_receipdate")) {
//             columnTrace.addVersionRange("ol_delivery_d", 3, 3, commitTime);
//             columnTrace.addVersionRange("ol_receipdate", 3, 3, commitTime);
//         } else {
//             System.out.println("fail");
//         }
//     }

//     public void printTrxTrace() {
//         SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
//         primaryKeyToColumnsMap.forEach((primaryKeys, columnTrace) -> {
//             System.out.println("Primary Keys: " + primaryKeys);
//             columnTrace.columnVersions.forEach((columnName, versionRanges) -> {
//                 versionRanges.forEach(range -> {
//                     System.out.println("Column: " + columnName + ", Start Version: " + range.startVersion +
//                             ", End Version: " + range.endVersion + ", Commit Time: "
//                             + dateFormat.format(range.commitTime));
//                 });
//             });
//             System.out.println();
//         });
//     }
// }

// @Getter
// public class TransactionTraceMap {
//     private static final Logger LOGGER = Logger.getLogger(TransactionTraceMap.class.getName());
//     Map<String, TrxTrace> tableTraces;
//     ExecutorService executorService;

//     public TransactionTraceMap() {
//         tableTraces = new ConcurrentHashMap<>();
//         executorService = Executors.newSingleThreadExecutor();
//         createTrace();
//     }

//     public void createTrace() {
//         String tableName = "vodka_order_line";
//         tableTraces.put(tableName, new TrxTrace());
//     }

//     public void addTransaction(List<Integer> primaryKeys, String columnName, int currentVersion, Timestamp commitTime) {
//         String tableName = "vodka_order_line";
//         TrxTrace trxTrace = tableTraces.get(tableName);
//         trxTrace.updateColumn(primaryKeys, columnName, currentVersion, commitTime);
//     }

//     public long findMaxTimeDifference(String tableName, List<ColumnVersion> columnVersionList) {
//         long maxDiff = 0;
//         Map<List<Integer>, List<ColumnVersion>> primaryKeyToColumnsMap = new HashMap<>();

//         for (ColumnVersion cv : columnVersionList) {
//             primaryKeyToColumnsMap.computeIfAbsent(cv.primaryKeys, k -> new ArrayList<>()).add(cv);
//         }

//         TrxTrace trxTrace = tableTraces.get(tableName);
//         if (trxTrace != null) {
//             for (Map.Entry<List<Integer>, List<ColumnVersion>> entry : primaryKeyToColumnsMap.entrySet()) {
//                 List<Integer> primaryKeys = entry.getKey();
//                 List<ColumnVersion> columns = entry.getValue();

//                 ColumnTrace columnTrace = trxTrace.primaryKeyToColumnsMap.get(primaryKeys);
//                 if (columnTrace != null) {
//                     for (ColumnVersion cv : columns) {
//                         Timestamp commitTime = getCommitTime(columnTrace, cv.columnName, cv.version);
//                         Timestamp latestCommitTime = getLatestCommitTime(columnTrace, cv.columnName);
//                         if (commitTime != null && latestCommitTime != null) {
//                             long diff = latestCommitTime.getTime() - commitTime.getTime();
//                             maxDiff = Math.max(maxDiff, diff);
//                         }
//                     }
//                 }
//             }
//         } else {
//             LOGGER.log(Level.WARNING, "No TrxTrace found for table: {0}", tableName);
//         }
//         LOGGER.log(Level.INFO, "Maximum time difference found: {0}ms", maxDiff);
//         return maxDiff;
//     }

//     private Timestamp getCommitTime(ColumnTrace columnTrace, String columnName, int version) {
//         List<VersionRange> ranges = columnTrace.columnVersions.get(columnName);
//         if (ranges != null) {
//             for (VersionRange range : ranges) {
//                 if (version >= range.startVersion && version <= range.endVersion) {
//                     return range.commitTime;
//                 }
//             }
//         }
//         return null;
//     }

//     private Timestamp getLatestCommitTime(ColumnTrace columnTrace, String columnName) {
//         List<VersionRange> ranges = columnTrace.columnVersions.get(columnName);
//         if (ranges != null && !ranges.isEmpty()) {
//             return ranges.get(ranges.size() - 1).commitTime;
//         }
//         System.out.println("Fail to ger latest commit time");
//         return null;
//     }

//     private int getLatestVersion(ColumnTrace columnTrace, String columnName) {
//         List<VersionRange> ranges = columnTrace.columnVersions.get(columnName);
//         if (ranges != null && !ranges.isEmpty()) {
//             // System.out.println(ranges.get(ranges.size() - 1).endVersion);
//             return ranges.get(ranges.size() - 1).endVersion;
//         }
//         System.out.println("Fail to ger version");
//         return 0;
//     }

//     public void shutdown() {
//         executorService.shutdown();
//         try {
//             if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
//                 executorService.shutdownNow();
//             }
//         } catch (InterruptedException e) {
//             Thread.currentThread().interrupt();
//             executorService.shutdownNow();
//         }
//     }
// }