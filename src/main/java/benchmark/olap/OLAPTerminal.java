package benchmark.olap;

import benchmark.olap.query.*;
import benchmark.oltp.OLTPClient;
import config.CommonConfig;
import org.apache.log4j.Logger;

import java.util.concurrent.atomic.AtomicLong;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class OLAPTerminal implements Runnable {
    public static AtomicLong oorderTableSize = new AtomicLong(benchmark.olap.query.baseQuery.orderOriginSize); // 通过查询获得的oorder表的实时大小
    public static AtomicLong orderLineTableSize = new AtomicLong(benchmark.olap.query.baseQuery.olOriginSize); // 通过查询获得的orderline表的实时大小
    public static AtomicLong orderlineTableNotNullSize = new AtomicLong(benchmark.olap.query.baseQuery.olNotnullSize);
    public static AtomicLong orderlineTableRecipDateNotNullSize = new AtomicLong(
            benchmark.olap.query.baseQuery.olNotnullSize);
    public static boolean filterRateCheck = false;  // 为 TRUE 时获取过滤比分母查询
    public static boolean countPlan = false;        // 为 TRUE 时记查询计划
    public static boolean detailedPlan = true;      // 为 TRUE 以 json 格式保存查询计划
    private static final Logger log = Logger.getLogger(OLAPTerminal.class);
    private final int interval;
    private final int dynamicParam;
    private Connection conn;
    private final int dbType;
    private boolean stopRunningSignal = false;
    private String terminalName;
    private final String resultDirName;
    private static final int queryNumber = 2;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final String parallel_sql;
    private static final String origin_sql = "set _force_parallel_query_dop = 1;";
    private final boolean parallelSwitch;
    private static final String[] sqlPath = { "tpchSQL/5.sql", "tpchSQL/8.sql" };
    // private static final String[] sqlPath = { "tpchSQL/1.sql", "tpchSQL/2.sql",
    // "tpchSQL/3.sql", "tpchSQL/4.sql",
    // "tpchSQL/5.sql",
    // "tpchSQL/6.sql", "tpchSQL/7.sql", "tpchSQL/8.sql", "tpchSQL/9.sql",
    // "tpchSQL/10.sql",
    // "tpchSQL/11.sql", "tpchSQL/13.sql", "tpchSQL/14.sql", "tpchSQL/15.sql",
    // "tpchSQL/16.sql", "tpchSQL/17.sql", "tpchSQL/19.sql", "tpchSQL/20.sql",
    // "tpchSQL/21.sql", "tpchSQL/22.sql" };

    private final TxnNumRecord txnNumRecord;

    public OLAPTerminal(String database, Properties dbProps, int dbType, int interval, OLTPClient parent,
            int dynamicParam, boolean parallelSwitch, int isolation_level, int parallel_degree, String iresultDirName)
            throws SQLException {
        this.dbType = dbType;
        this.resultDirName = iresultDirName;
        this.interval = interval;
        this.conn = DriverManager.getConnection(database, dbProps);
        this.conn.setAutoCommit(false);
        this.dynamicParam = dynamicParam;
        this.txnNumRecord = new TxnNumRecord(conn, dbType);
        this.parallelSwitch = parallelSwitch;
        this.parallel_sql = setQueryParalleDegreelByDBType(dbType, parallel_degree);
        isolation_level = 2; // default configuration
        switch (isolation_level) {
            case 0 -> conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            case 2 -> conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            case 3 -> conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            default -> conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        }
    }

    public static String setQueryParalleDegreelByDBType(int dbType, int parallel_degree) {
        String result = "";
        switch (dbType) {
            case CommonConfig.DB_OCEANBASE -> result = "set _force_parallel_query_dop = " + parallel_degree + " ; ";
            case CommonConfig.DB_TIDB -> result = "SET @@global.tidb_max_tiflash_threads = " + parallel_degree + " ; ";
            case CommonConfig.DB_POSTGRES ->
                result = "SET max_parallel_workers_per_gather =   " + parallel_degree + " ; ";
            case CommonConfig.DB_POLARDB -> result = "set polar_px_dop_per_node =   " + parallel_degree + " ; ";
            default -> log.error("vodka is not yet compatible with the database");
        }
        return result;
    }

    @Override
    public void run() {
        try {
            log.info("AP Workloads Starting");
            executeTPCHWorkload();
            log.info("AP execute end, pass end signal");
            this.conn.close();
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void executeTPCHWorkload() throws IOException {
        PreparedStatement stmt, stmtCountCheck, stmtPlan, stmtJsonCheck;
        Statement paralleStmt;
        String[] queryName = new String[queryNumber];
        String[] filterRateLine = new String[queryNumber];
        String[] txnNum = new String[queryNumber];
        double[] lantency = new double[queryNumber];
        long[] queryStartTime = new long[queryNumber];
        ArrayList<String> queryPlan = new ArrayList<>();
        ArrayList<String> queryPlanJson = new ArrayList<>();
        boolean recordFileSignal = true;
        try {
            // paralleStmt = conn.createStatement();
            // paralleStmt.execute("SET enable_indexscan TO off;");
            // paralleStmt.close();
            if (parallelSwitch) {
                if (dbType == CommonConfig.DB_TIDB || dbType == CommonConfig.DB_POSTGRES) { // set global parallel
                    paralleStmt = conn.createStatement();
                    paralleStmt.execute(parallel_sql);
                    log.info("Executing parallel session sql for global" + parallel_sql);
                    paralleStmt.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // vector initialize
        Vector<baseQuery> queryVector = new Vector<>(queryNumber);
        try {
            if (dynamicParam == 1) {
                long startTime = System.nanoTime();
                queryVector.clear();
                // Q1 q1 = new Q1(dbType);
                // queryVector.add(q1);
                // Q2 q2 = new Q2(dbType);
                // queryVector.add(q2);
                // Q3 q3 = new Q3(dbType);
                // queryVector.add(q3);
                // Q4 q4 = new Q4(dbType);
                // queryVector.add(q4);
                Q5 q5 = new Q5(dbType);
                queryVector.add(q5);
                // Q6 q6 = new Q6(dbType);
                // queryVector.add(q6);
                // Q7 q7 = new Q7(dbType);
                // queryVector.add(q7);
                Q8 q8 = new Q8(dbType);
                queryVector.add(q8);
                // Q9 q9 = new Q9(dbType);
                // queryVector.add(q9);
                // Q15 q15 = new Q15(dbType);
                // queryVector.add(q15);
                // Q21 q21 = new Q21(dbType);
                // queryVector.add(q21);
                // Q10 q10 = new Q10(dbType);
                // queryVector.add(q10);
                // Q11 q11 = new Q11(dbType);
                // queryVector.add(q11);
                // Q13 q13 = new Q13(dbType);
                // queryVector.add(q13);
                // Q14 q14 = new Q14(dbType);
                // queryVector.add(q14);

                // Q16 q16 = new Q16(dbType);
                // queryVector.add(q16);
                // Q17 q17 = new Q17(dbType);
                // queryVector.add(q17);
                // Q18 q18 = new Q18(dbType);
                // queryVector.add(q18);
                // Q19 q19 = new Q19(dbType);
                // queryVector.add(q19);

                // Q22 q22 = new Q22(dbType);
                // queryVector.add(q22);

                while (!stopRunningSignal) {
                    long currentLoopTime = System.nanoTime(); // 单位为ns
                    if (Double.parseDouble(Long.toString(currentLoopTime - startTime)) / 1_000_000_000 > interval) { // s
                        recordFileSignal = true;
                        startTime = currentLoopTime;
                    }

                    File file1 = new File(resultDirName + "/query.csv"); // 存放数组数据的文件
                    FileWriter out1;
                    File file2 = new File(resultDirName + "/queryJson.csv"); // 存放数组数据的文件
                    FileWriter out2;

                    for (int i = 0; i < queryVector.size(); i++) {
                        if (stopRunningSignal)
                            break;
                        queryName[i] = "Vodka-AP-Query-" + (i + 1);
                        txnNum[i] = txnNumRecord.getCurrentTxnNum();
                        baseQuery obj = queryVector.get(i);

                        long startClick, endClick;
                        // check query filter rate

                        // 在查询前获取统计数据的快照
                        int snapshotOrderLineTableSize1 = orderLineTableSize.intValue();
                        int snapshotOOrderTableSize1 = oorderTableSize.intValue();
                        int snapshotOrderlineNotNullSize1 = orderlineTableNotNullSize.intValue();
                        int snapshotOrderlineRecieveNotNullSize1 = orderlineTableRecipDateNotNullSize.intValue();
                        stmt = conn.prepareStatement(obj.updateQuery());
                        int snapshotOrderLineTableSize2 = orderLineTableSize.intValue();
                        int snapshotOOrderTableSize2 = oorderTableSize.intValue();
                        int snapshotOrderlineNotNullSize2 = orderlineTableNotNullSize.intValue();
                        int snapshotOrderlineRecieveNotNullSize2 = orderlineTableRecipDateNotNullSize.intValue();

                        // 执行查询
                        startClick = System.currentTimeMillis();
                        ResultSet rs = stmt.executeQuery();
                        endClick = System.currentTimeMillis();
                        double latency = Double.parseDouble(Long.toString(endClick - startClick));

                        queryName[i] = sqlPath[i];
                        queryStartTime[i] = startClick;
                        lantency[i] = latency;

                        out1 = new FileWriter(file1, true); // 文件写入流
                        out1.write("query " + (i + 1) + ":" + obj.getQuery() + "\r");
                        out1.close();

                        if (detailedPlan) {
                            System.out.println(obj.getQuery());
                            stmtJsonCheck = conn.prepareStatement(
                                    "explain (analyze, costs false, timing false, summary false, format json) "
                                            + obj.getQuery());
                            ResultSet rsPlan = stmtJsonCheck.executeQuery();
                            out2 = new FileWriter(file2, true); // 文件写入流
                            out2.write(sdf.format(new java.util.Date()) + "\n");
                            if (rsPlan.next()) {
                                out2.write(queryStartTime[i] + ", " + lantency[i]);
                                out2.write(queryName[i] + " \n " + rsPlan.getString(1) + "\n");
                            }
                            out2.write("Before sort: orderline table size: " + snapshotOrderLineTableSize1
                                    + ", order table size: "
                                    + snapshotOOrderTableSize1 + ", orderline not null size: "
                                    + snapshotOrderlineNotNullSize1 + ", orderline receivedates not null size: "
                                    + snapshotOrderlineRecieveNotNullSize1 + "\n");
                            out2.write("After sort: orderline table size: " + snapshotOrderLineTableSize2
                                    + ", order table size: "
                                    + snapshotOOrderTableSize2 + ", orderline not null size: "
                                    + snapshotOrderlineNotNullSize2 + ", orderline receivedates not null size: "
                                    + snapshotOrderlineRecieveNotNullSize2 + "\n");
                            out2.close();
                        }

                        // if (detailedPlan) {
                        // System.out.println(obj.getQuery());
                        // stmtJsonCheck = conn.prepareStatement(
                        // "explain (analyze, costs false, timing false, summary false, format json) "
                        // + obj.getQuery());
                        // ResultSet rsPlan = stmtJsonCheck.executeQuery();
                        // if (rsPlan.next()) {
                        // queryPlanJson.add(queryName[i] + " \n " + rsPlan.getString(1) + "\n");
                        // }
                        // }

                        if (recordFileSignal)
                            log.info(sqlPath[i] + "--" + latency + "ms");
                    }
                    log.info("Complete a bunch of TPC-H queries");
                    // conn.close();
                    // conn = DriverManager.getConnection(database, dbProps);
                    // conn.setAutoCommit(false);
                    // conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                    // if (recordFileSignal) {
                    // writeFile(queryName, queryStartTime, lantency, txnNum);
                    // if (detailedPlan)
                    // writePlanJsonToFile(queryPlanJson);
                    // if (filterRateCheck)
                    // writeLineCountCheckToFile(queryName, filterRateLine);
                    // if (countPlan)
                    // writePlanToFile(queryPlan);
                    // recordFileSignal = false;
                    // }
                    log.info("check stopRunningSignal status: " + stopRunningSignal);
                }
            }
            log.info("Quit AP threads running");
        } catch (SQLException | IOException | ParseException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void stopRunningWhenPossible() {
        stopRunningSignal = true;
        printMessage("Terminal received stop signal!");
        printMessage("Finishing current transaction before exit...");
    }

    public void writePlanToFile(ArrayList<String> queryPlan) throws IOException {
        File file = new File(resultDirName + "/planResult-" + dbType + ".csv"); // 存放数组数据的文件
        FileWriter out = new FileWriter(file, true);
        for (String s : queryPlan)
            out.write(s);
        out.close();
    }

    public void writePlanJsonToFile(ArrayList<String> queryPlan) throws IOException {
        System.out.println("Write query plan in json layout to file.");
        File file = new File(resultDirName + "/planJsonResult-" + dbType + ".csv"); // 存放数组数据的文件
        FileWriter out = new FileWriter(file, true);
        out.write(sdf.format(new java.util.Date()) + "\n");
        for (String s : queryPlan)
            out.write(s);
        out.write("orderline table size: " + orderLineTableSize.intValue() + ", order table size: "
                + oorderTableSize.intValue() + ", orderline not null size: " + orderlineTableNotNullSize.intValue()
                + "\n");
        out.close();
    }

    public void writeLineCountCheckToFile(String[] queryName, String[] linesCountCheck) throws IOException {
        System.out.println("Write selectivity to file.");
        File file = new File(resultDirName + "/lineCountResult-" + dbType + ".csv"); // 存放数组数据的文件
        FileWriter out = new FileWriter(file, true);
        for (int i = 0; i < queryNumber; i++)
            out.write((i + 1) + "," + queryName[i] + "," + linesCountCheck[i] + "," + "countCheck" + "\r");
        out.close();
    }

    private void writeFile(String[] queryName, long[] queryStartTime, double[] lantency, String[] txnNum)
            throws IOException {
        log.info("write file");
        File file = new File(resultDirName + "/tpchresult-" + dbType + ".csv"); // 存放数组数据的文件
        FileWriter out = new FileWriter(file, true); // 文件写入流
        for (int i = 0; i < queryNumber; i++)
            out.write(longToDate(queryStartTime[i]) + "," + (i + 1) + "," + queryName[i] + "," + lantency[i] + ","
                    + txnNum[i] + "\r");
        out.close();
    }

    private void printMessage(String message) {
        log.trace(terminalName + ", " + message);
    }

    public static String longToDate(long lo) {
        Date date = new Date(lo);
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sd.format(date);
    }

}

class TxnNumRecord {
    private Connection conn;
    private String getFinishedTxnNumSql = "select new_order, payment from vodka_time;";
    private PreparedStatement stmt = null;
    protected ResultSet result;

    TxnNumRecord(Connection conn, int dbType) {
        this.conn = conn;
        if (CommonConfig.DB_TIDB == dbType)
            getFinishedTxnNumSql = "select /*+ read_from_storage(tiflash[vodka_time]) */ new_order, payment from vodka_time;";
    }

    String getCurrentTxnNum() {
        long new_order = -1;
        long payment = -1;
        try {
            stmt = conn.prepareStatement(getFinishedTxnNumSql);
            result = stmt.executeQuery();
            if (result.next()) {
                new_order = result.getLong(1);
                payment = result.getLong(2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new_order + "," + payment;
    }
}
